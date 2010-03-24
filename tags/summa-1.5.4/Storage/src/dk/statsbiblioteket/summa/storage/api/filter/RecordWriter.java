/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.storage.api.filter;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.util.LoggingExceptionHandler;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.StorageWriterClient;
import dk.statsbiblioteket.summa.storage.api.WritableStorage;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Connects to a Storage and ingests received Records into the storage.
 * The Storage is accessed on the address specified by
 * {@link ConnectionConsumer#CONF_RPC_TARGET}.
 * <p/>
 * Internally the RecordWriter will collect records into batches and submit
 * them to storage with {@link WritableStorage#flushAll}.
 * The policy on when to commit the batches is determined by the two
 * properties {@link #CONF_BATCH_SIZE} and {@link #CONF_BATCH_TIMEOUT}. 
 * <p/>
 * Note: This ObjectFilter can only be chained after another ObjectFilter.
 * </p><p>
 * Note: Only Record is stored. All other data in Payload is ignored.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class RecordWriter extends ObjectFilterImpl {
    private static final Log log = LogFactory.getLog(RecordWriter.class);

    /**
     * The Storage to connect to. This is a standard RMI address.
     * Example: //localhost:27000/summa-storage;
     * Deprecated: use {@link ConnectionConsumer#CONF_RPC_TARGET} instead.
     */
    private static final String DEPRECATED_CONF_STORAGE =
            "summa.storage.recordwriter.storage";

    /**
     * The writer groups records into chunks before commiting them to storage
     * via {@link WritableStorage#flushAll}. The number of records in the
     * chunks limited by CONF_BATCH_SIZE and {@link #CONF_BATCH_MAXMEMORY}.
     * </p><p>
     * If no records has been received during {@link #CONF_BATCH_TIMEOUT}
     * milliseconds the currently batched records will be committed.
     * <p/>
     * The default value for this property is 100.
     * </p><p>
     * @see {@link #CONF_BATCH_MAXMEMORY}.
     */
    public static final String CONF_BATCH_SIZE =
            "summa.storage.recordwriter.batchsize";

    /**
     * Default value for the {@link #CONF_BATCH_SIZE} property
     */
    public static final int DEFAULT_BATCH_SIZE = 100;

    /**
     * The maximum amount of memory in bytes used by grouped Records before they
     * are committed to Storage.
     * </p><p>
     * Note: The memory check is performed after the addition of each record.
     * If the limit is 10MB and 2 records of 9MB each are added, the queue will
     * contain both records before commit.
     * </p><p>
     * Optional. Default is 2000000 (~2MB).
     * @see {@link #CONF_BATCH_SIZE}.
     */
    public static final String CONF_BATCH_MAXMEMORY =
            "summa.storage.recordwriter.batchmaxmemory";
    public static final int DEFAULT_BATCH_MAXMEMORY = 2 * 1000 * 1000;

    /**
     * The number of milliseconds to wait without receiving any records before
     * committing the currently batched records to storage. The default value
     * is 1000ms (that would be 1s)
     * @see #CONF_BATCH_SIZE
     */
    public static final String CONF_BATCH_TIMEOUT =
                                      "summa.storage.recordwriter.batchtimeout";

    /**
     * Default value for the {@link #CONF_BATCH_TIMEOUT} property
     */
    public static final int DEFAULT_BATCH_TIMEOUT = 1000;

    /**
     * Boolean property for controlling the {@code TRY_UPDATE} flag on the
     * {@link QueryOptions} passed to {@code storage.flushAll()}. If this
     * property is {@code true} records that are already up to date in storage
     * will not be updated. Specifically the storage checks if the record
     * exists in the database with the exact same fields as the incoming record.
     * </p><p>
     * Warning: Do not use this with a deleting {@link FullDumpFilter} as the
     * combined effect is that all records for the base that are not changed
     * during ingest will be marked as deleted when the ingest has completed. 
     */
    public static final String CONF_TRY_UPDATE =
                                      "summa.storage.recordwriter.tryupdate";

    public static final boolean DEFAULT_TRY_UPDATE = false;

    private static class Batcher implements Runnable {
        /* CAVEAT: We log under the name of the RecordWriter !! */
        private static final Log log = LogFactory.getLog(RecordWriter.class);

        private boolean mayRun;
        long lastCommit;
        private long lastUpdate;
        private int batchSize;
        private int batchMaxMemory;
        private int batchTimeout;
        private List<Record> records;
        private Thread watcher;
        private WritableStorage storage;
        private QueryOptions qOptions;

        private long byteSize = 0;

        public Batcher (int batchSize, int batchMaxMemory, int batchTimeout,
                        WritableStorage storage, QueryOptions qOptions) {
            mayRun = true;
            records = new ArrayList<Record>(batchSize);
            this.batchSize = batchSize;
            this.batchMaxMemory = batchMaxMemory;
            this.batchTimeout = batchTimeout;
            lastUpdate = System.currentTimeMillis();
            lastCommit = System.nanoTime();
            this.storage = storage;
            this.qOptions = qOptions;

            log.debug("Starting batch job watcher");
            watcher = new Thread(this, "RecordBatcher");
            watcher.setDaemon(true); // Allow the JVM to exit
            watcher.setUncaughtExceptionHandler(new LoggingExceptionHandler());
            watcher.start();
        }

        public synchronized void add(Record r) {
            while (records.size() >= batchSize || byteSize > batchMaxMemory) {
                try {
                    log.debug("Waiting for batch queue to flush");
                    wait(batchTimeout);
                } catch (InterruptedException e) {
                    // Check our capacity again
                }
            }

            if (log.isTraceEnabled()) {
                //noinspection DuplicateStringLiteralInspection
                log.debug("Batching: " + r.toString(true));
            } else {
                //noinspection DuplicateStringLiteralInspection
                log.debug("Batching: " + r);
            }

            lastUpdate = System.currentTimeMillis();
            records.add(r);
            byteSize += RecordUtil.calculateRecordSize(r, true);
            notifyAll();
        }

        public void clear() {
            records.clear();
            byteSize = 0;
        }


        public boolean shouldCommit () {
            return records.size() >= batchSize
                   || System.currentTimeMillis() - lastUpdate >= batchTimeout
                   || !mayRun // Force commit if closing
                   || byteSize > batchMaxMemory;
        }

        private boolean checkCommit() {
            if (!shouldCommit()) {
                if (log.isTraceEnabled()) {
                    log.trace("Batch not ready for commit yet. Current size: "
                              + records.size());
                }
                return false;
            }

            forceCommit();
            return true;
        }

        private synchronized void forceCommit() {
            if (log.isDebugEnabled()) {
                for (Record r : records) {
                    log.debug("Committing: " + r.getId());
                }
            }
            if (records.isEmpty()) {
                log.debug("No records to commit");
                return;
            }

            try {
                String stats = records.size() + " records of total"
                         + " size " + byteSize/1024 + "KB";
                log.debug(String.format("Committing %s.", stats));
                long start = System.nanoTime();
                storage.flushAll(records, qOptions);
                log.info(String.format(
                        "Committed %s in %sms. Last commit was %sms ago",
                        stats, (System.nanoTime() - start)/1000000D,
                         (System.nanoTime() - lastCommit)/1000000D));
                lastCommit = System.nanoTime();
            } catch (Exception e) {
                log.error("Dropped " + records.size() + " records in commit: "
                          + e.getMessage(), e);
                for (Record r: records) {
                    log.warn("Dropped: " + r.getId());
                }
                // Fall through so that we clear the records list - this must
                // be done because it might not be the connection that is bad,
                // but the records that are broken
            }

            // Clear the batch queue and awake anyone waiting for us
            clear();
            log.trace("Batch queue cleared");
            notifyAll();
            log.trace("Notified");
        }

        public void stop() {
            log.debug("Stopping Batcher thread");
            mayRun = false;
            synchronized (this) {
                notifyAll();
            }
            try {
                watcher.join();
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for record "
                         + "batching thread");
            }
        }

        public void run () {
            log.debug("Batch job watcher is running");
            synchronized (this) {
                while (mayRun) {
                    try {
                        if (log.isTraceEnabled()) {
                            log.trace("Waiting for records");
                        }
                        wait(batchTimeout);
                    } catch (InterruptedException e) {
                        // We have been awoken!
                        log.trace("Interrupted while waiting for records");
                    }
                    checkCommit();
                }
            }
        }
    }

    private WritableStorage storage;
    private int batchSize;
    private int batchMaxMemory;
    private int batchTimeout;
    private Batcher batcher;
    private Profiler profiler = new Profiler();
    private boolean eofReached = false; // Set to true if hasNext() == false

    /**
     * Established an RMI connection to the Storage specified in configuration.
     * @param conf contains setup information.
     * @see {@link ConnectionConsumer#CONF_RPC_TARGET}.
     * @throws java.io.IOException if the RecordWriter could not be constructed.
     */
    public RecordWriter(Configuration conf) throws IOException {
        super(conf);
        log.trace("Constructing RecordWriter");
        if (conf.valueExists(DEPRECATED_CONF_STORAGE)) {
            log.warn(String.format(
                    "Old Storage address configuration detected."
                    + " The key %s has been replaced by %s",
                    DEPRECATED_CONF_STORAGE,
                    ConnectionConsumer.CONF_RPC_TARGET));
        }

        QueryOptions qOptions;
        if (conf.getBoolean(CONF_TRY_UPDATE, DEFAULT_TRY_UPDATE)) {
            qOptions = new QueryOptions();
            qOptions.meta("TRY_UPDATE", "true");
        } else {
            qOptions = null;
        }

        storage = new StorageWriterClient(conf);
        batchSize = conf.getInt(CONF_BATCH_SIZE, DEFAULT_BATCH_SIZE);
        batchMaxMemory = conf.getInt(
                CONF_BATCH_MAXMEMORY, DEFAULT_BATCH_MAXMEMORY);
        batchTimeout = conf.getInt(CONF_BATCH_TIMEOUT, DEFAULT_BATCH_TIMEOUT);
        batcher = new Batcher(
                    batchSize, batchMaxMemory, batchTimeout, storage, qOptions);

        // TODO: Perform a check to see if the Storage is alive
    }

    public RecordWriter(WritableStorage storage,
                        int batchSize, int batchTimeout) {
        this(storage, batchSize, DEFAULT_BATCH_MAXMEMORY, batchTimeout);
    }

    public RecordWriter(WritableStorage storage,
                        int batchSize, int batchMaxMemory, int batchTimeout) {
        super (Configuration.newMemoryBased());
        this.storage = storage;
        this.batchSize = batchSize;
        this.batchMaxMemory = batchMaxMemory;
        this.batchTimeout = batchTimeout;
        batcher = new Batcher(
                        batchSize, batchMaxMemory, batchTimeout, storage, null);
    }

    /**
     * Flushes Records to Storage.
     * @param payload the Payload containing the Record to flush.
     */
    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        Record record = payload.getRecord();

        if (record == null) {
            throw new PayloadException("null received in Payload in next()"
                                       + ". This should not happen");
        }

        batcher.add(record);
        profiler.beat();
        log.info("Record '" + record.getId() + "' ok through RecordWriter.");
        return true;
    }

    @Override
    public boolean hasNext() {
        if (!super.hasNext()) {
            eofReached = true;
            return false;
        }
        return true;
    }

    @Override
    public void close(boolean success) {
        log.debug(String.format("close(%s) with eofReached==%b called",
                                success, eofReached));
        boolean initialEofReached = eofReached;
        if (initialEofReached && success) {
            log.debug("close(true) with eofReached == true: Flushing and "
                      + "closing batcher before calling close on source");
            try {
                batcher.stop();
            } catch (Exception e) {
                log.warn("Exception while closing down batcher", e);
            }
        }
        try {
            log.debug(String.format(
                    "close(%s): Closing super (which closes source)", success));
            super.close(success);
        } finally {
            if (!(initialEofReached && success)) {
                log.info("Waiting for batch jobs to be committed");
                batcher.stop();
                log.info("Closed down RecordWriter. " + getProcessStats()
                         + ". Total time: " + profiler.getSpendTime());
            }
        }
    }

    
}



