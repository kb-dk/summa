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

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterBase;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.util.*;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.StorageWriterClient;
import dk.statsbiblioteket.summa.storage.api.WritableStorage;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Timing;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.NoRouteToHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    private static final String DEPRECATED_CONF_STORAGE = "summa.storage.recordwriter.storage";

    /**
     * The writer groups records into chunks before commiting them to storage
     * via {@link WritableStorage#flushAll}. The number of records in the
     * chunks limited by CONF_BATCH_SIZE and {@link #CONF_BATCH_MAXMEMORY}.
     * </p><p>
     * If no records has been received during {@link #CONF_BATCH_TIMEOUT}
     * milliseconds the currently batched records will be committed.
     * <p/>
     * The default value for this property is 1000.
     * </p><p>
     * @see #CONF_BATCH_MAXMEMORY
     */
    public static final String CONF_BATCH_SIZE = "summa.storage.recordwriter.batchsize";

    /**
     * Default value for the {@link #CONF_BATCH_SIZE} property
     */
    public static final int DEFAULT_BATCH_SIZE = 1000;

    /**
     * The maximum amount of memory in bytes used by grouped Records before they
     * are committed to Storage.
     * </p><p>
     * Note: The memory check is performed after the addition of each record.
     * If the limit is 10MB and 2 records of 9MB each are added, the queue will
     * contain both records before commit.
     * </p><p>
     * Optional. Default is 2000000 (~2MB).
     * @see #CONF_BATCH_SIZE
     */
    public static final String CONF_BATCH_MAXMEMORY = "summa.storage.recordwriter.batchmaxmemory";
    public static final int DEFAULT_BATCH_MAXMEMORY = 10 * 1000 * 1000;

    /**
     * The number of milliseconds to wait without receiving any records before
     * committing the currently batched records to storage. The default value
     * is 1000ms (that would be 1s)
     * @see #CONF_BATCH_SIZE
     */
    public static final String CONF_BATCH_TIMEOUT = "summa.storage.recordwriter.batchtimeout";

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
    public static final String CONF_TRY_UPDATE = "summa.storage.recordwriter.tryupdate";

    public static final boolean DEFAULT_TRY_UPDATE = false;

    private class Batcher implements Runnable {

        public static final String CONF_STATUS_EVERY = "batch." + ObjectFilterBase.CONF_STATUS_EVERY;
        public static final int DEFAULT_STATUS_EVERY = 0;

        /* CAVEAT: We log under the name of the RecordWriter !! */
        private final Log log = LogFactory.getLog(RecordWriter.class);

        private final int batchSize;
        private final int batchMaxMemory;
        private final int batchTimeout;
        private final List<Record> records;
        private final Thread watcher;
        private final WritableStorage storage;
        private final QueryOptions qOptions;

        private boolean mayRun;
        private long lastCommit;
        private long lastUpdate;

        private final int statusEvery;
        private long totalRecordsCommitted = 0;
        private long totalBatchesCommitted = 0;
        private long batchByteSize = 0;

        private final Timing timingSend;
        private final RecordStatsCollector sizeSend;

        public Batcher (Configuration conf, int batchSize, int batchMaxMemory, int batchTimeout,
                        WritableStorage storage, QueryOptions qOptions) {
            mayRun = true;
            records = new ArrayList<>(batchSize);
            this.batchSize = batchSize;
            this.batchMaxMemory = batchMaxMemory;
            this.batchTimeout = batchTimeout;
            lastUpdate = System.currentTimeMillis();
            lastCommit = System.nanoTime();
            this.storage = storage;
            this.qOptions = qOptions;
            statusEvery = conf.getInt(CONF_STATUS_EVERY, DEFAULT_STATUS_EVERY);
            timingSend = StatUtil.createTiming(conf, "batch", "batchsend", null, "RecordBatch", null);
            sizeSend = new RecordStatsCollector("batch", conf, null, false, "batches");

            log.debug("Starting batch job watcher");
            watcher = new Thread(this, "RecordBatcher daemon");
            watcher.setDaemon(true); // Allow the JVM to exit
            watcher.setUncaughtExceptionHandler(new LoggingExceptionHandler());
            watcher.start();
        }

        public synchronized void add(Record r) {
            while (records.size() >= batchSize || batchByteSize > batchMaxMemory) {
                try {
                    log.debug("Waiting for batch queue to flush");
                    wait(batchTimeout);
                } catch (InterruptedException e) {
                    // Check our capacity again
                }
            }

            if (log.isTraceEnabled()) {
                //noinspection DuplicateStringLiteralInspection
                log.debug("Batching" + profiler.getBeats() + ": " + r.toString(true));
            } else {
                //noinspection DuplicateStringLiteralInspection
                log.debug("Batching #" + profiler.getBeats() + ": " + r);
            }

            lastUpdate = System.currentTimeMillis();
            records.add(r);
            batchByteSize += RecordUtil.calculateRecordSize(r, true);
            notifyAll();
        }

        public void clear() {
            records.clear();
            batchByteSize = 0;
        }


        public boolean shouldCommit () {
            return !records.isEmpty()
                   && (records.size() >= batchSize
                       || System.currentTimeMillis() - lastUpdate >= batchTimeout
                       || !mayRun // Force commit if closing
                       || batchByteSize > batchMaxMemory
            );
        }

        private boolean checkCommit() {
            if (!shouldCommit()) {
                if (log.isTraceEnabled()) {
                    log.trace("Batch not ready for commit yet. Current size: " + records.size());
                }
                return false;
            }

            forceCommit();
            return true;
        }

        private synchronized void forceCommit() {
            if (log.isTraceEnabled()) {
                for (Record r : records) {
                    log.trace("Committing: " + r.getId());
                }
            }
            if (records.isEmpty()) {
                log.debug("No records to commit");
                return;
            }

            try {
                String stats = records.size() + " records of total size " + batchByteSize / 1024 + "KB, last recordID:'"
                               + (records.isEmpty() ? "N/A" : records.get(records.size()-1).getId()) + "'";
                if (log.isDebugEnabled()) {
                    log.debug(String.format(Locale.ROOT, "Committing %s.", stats));
                }
                long start = System.nanoTime();
                timingSend.start();
                storage.flushAll(records, qOptions);
                timingSend.stop();
                sizeSend.process("batch#" + totalBatchesCommitted, batchByteSize);
                totalBatchesCommitted++;
                totalRecordsCommitted += records.size();
                if (statusEvery != 0 && totalBatchesCommitted % statusEvery == 0 || log.isDebugEnabled()) {
                    final String message =String.format(Locale.ROOT,
                            "Committed %s in %.1fms. Last commit was %dms ago. %s",
                            stats, (System.nanoTime() - start) / 1000000D,
                            (System.nanoTime() - lastCommit) / 1000000, getBatchProcessStats());
                    if (statusEvery != 0 && totalBatchesCommitted % statusEvery == 0) {
                        log.info(message);
                    } else {
                        log.debug(message);
                    }
                }
                lastCommit = System.nanoTime();
            } catch (NoRouteToHostException e) {
                Logging.fatal(log, "RecordWriter.forceCommit",
                              "Unable to flush " + records.size() + " due to no Storage connection. " +
                              "System will be shut down in 1 second", e);
                new DeferredSystemExit(66, 1000);
            } catch (Exception e) {
                log.error("Dropped " + records.size() + " records in commit", e);
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

        private String getBatchProcessStats() {
            return "Timing=" + timingSend + ". SizeStats=" + sizeSend;
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
                log.warn("Interrupted while waiting for record batching thread");
            }
            log.info("Batcher closed. " + getBatchProcessStats());
        }

        @Override
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
     * @see ConnectionConsumer#CONF_RPC_TARGET
     * @throws java.io.IOException if the RecordWriter could not be constructed.
     */
    public RecordWriter(Configuration conf) throws IOException {
        super(conf);
        feedback = false;
        log.trace("Constructing RecordWriter");
        if (conf.valueExists(DEPRECATED_CONF_STORAGE)) {
            log.warn(String.format(Locale.ROOT,
                    "Old Storage address configuration detected. The key %s has been replaced by %s",
                    DEPRECATED_CONF_STORAGE, ConnectionConsumer.CONF_RPC_TARGET));
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
        batchMaxMemory = conf.getInt(CONF_BATCH_MAXMEMORY, DEFAULT_BATCH_MAXMEMORY);
        batchTimeout = conf.getInt(CONF_BATCH_TIMEOUT, DEFAULT_BATCH_TIMEOUT);
        batcher = new Batcher(conf, batchSize, batchMaxMemory, batchTimeout, storage, qOptions);
        setStatsDefaults(conf, false, false, true, true);

        // TODO: Perform a check to see if the Storage is alive
    }

    public RecordWriter(WritableStorage storage, int batchSize, int batchTimeout) {
        this(storage, batchSize, DEFAULT_BATCH_MAXMEMORY, batchTimeout);
    }

    public RecordWriter(WritableStorage storage, int batchSize, int batchMaxMemory, int batchTimeout) {
        super (Configuration.newMemoryBased());
        this.storage = storage;
        this.batchSize = batchSize;
        this.batchMaxMemory = batchMaxMemory;
        this.batchTimeout = batchTimeout;
        batcher = new Batcher(Configuration.newMemoryBased(), batchSize, batchMaxMemory, batchTimeout, storage, null);
    }

    /**
     * Flushes Records to the batch queue.
     * @param payload the Payload containing the Record to flush.
     */
    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        Record record = payload.getRecord();
        if (record == null) {
            throw new PayloadException("null received in Payload in next(). This should not happen");
        }

        processRecord(record);
        return true;
    }

    /**
     * Flushes Records to the batch queue.
     * @param record the Record to flush.
     */
    public void processRecord(Record record) {
        batcher.add(record);
        profiler.beat();
        log.trace("Record '" + record.getId() + "' ok through RecordWriter.");
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
    protected Timing createTimingProcess(Configuration conf) {
        return StatUtil.createTiming(conf, "process", "queue", null, "Payload", null);
    }

    @Override
    protected RecordStatsCollector createSizeProcess(Configuration conf) {
        return new RecordStatsCollector("out", conf, false);
    }

    /**
     * Explicit flush of queued Records to Storage.
     */
    public void flush() {
        batcher.forceCommit();
    }

    @Override
    public void close(boolean success) {
        log.info(String.format(Locale.ROOT, "close(%s) with eofReached == %b called", success, eofReached));
        boolean initialEofReached = eofReached;
        if (initialEofReached && success) {
            log.info(
                "close(true) with eofReached == true: Flushing and closing batcher before calling close on source");
            try {
                batcher.stop();
            } catch (Exception e) {
                log.warn("Exception while closing down batcher", e);
            }
        }
        try {
            log.info(String.format(Locale.ROOT, "close(%s): Closing super (which closes source)", success));
            super.close(success);
            log.info(String.format(Locale.ROOT, "close(%s): super closed without any known problems", success));
        } finally {
            if (!(initialEofReached && success)) {
                log.info("Waiting for batch jobs to be committed");
                batcher.stop();
                log.info("Closed down RecordWriter. " + getProcessStats() + ". Total time: " + profiler.getSpendTime());
            }
        }
    }
}
