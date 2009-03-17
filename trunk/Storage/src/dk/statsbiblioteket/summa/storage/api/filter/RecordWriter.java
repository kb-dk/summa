/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.storage.api.filter;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.storage.api.StorageWriterClient;
import dk.statsbiblioteket.summa.storage.api.WritableStorage;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Profiler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
     * The writer will try to group records into chunks of at least this size
     * and commit them to the storage via {@link WritableStorage#flushAll}.
     * If no records has been received during {@link #CONF_BATCH_TIMEOUT}
     * milliseconds the currently batched records will be committed.
     * <p/>
     * The default value for this property is 100.
     */
    public static final String CONF_BATCH_SIZE =
                                         "summa.storage.recordwriter.batchsize";

    /**
     * Default value for the {@link #CONF_BATCH_SIZE} property
     */
    public static final int DEFAULT_BATCH_SIZE = 100;

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

    private static class Batcher implements Runnable {
        /* CAVEAT: We log under the name of the RecordWriter !! */
        private static final Log log = LogFactory.getLog(RecordWriter.class);

        private boolean mayRun;
        long lastCommit;
        private long lastUpdate;
        private int batchSize;
        private int batchTimeout;
        private List<Record> records;
        private Thread watcher;
        private WritableStorage storage;

        public Batcher (int batchSize, int batchTimeout,
                        WritableStorage storage) {
            mayRun = true;
            records = new ArrayList<Record>(batchSize);
            this.batchSize = batchSize;
            this.batchTimeout = batchTimeout;
            lastUpdate = System.currentTimeMillis();
            lastCommit = System.nanoTime();
            this.storage = storage;

            log.debug("Starting batch job watcher");
            watcher = new Thread(this, "RecordBatcher");
            watcher.setDaemon(true); // Allow the JVM to exit
            watcher.start();
        }

        public synchronized void add(Record r) {
            while (records.size() >= batchSize) {
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
                log.debug("Batching: " + r);
            }

            lastUpdate = System.currentTimeMillis();
            records.add(r);
            notifyAll();
        }

        public void clear() {
            records.clear();
        }


        public boolean shouldCommit () {
            return (records.size() >= batchSize ||
                    System.currentTimeMillis() - lastUpdate >= batchTimeout ||
                    !mayRun); // Force commit if closing
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

            try {
                log.info("Committing " + records.size()
                         + "records. Time since last commit: "
                         + ((System.nanoTime() - lastCommit)/1000000D) + "ms");
                long start = System.nanoTime();
                storage.flushAll(records);
                log.info("Committed " + records.size() + " records in "
                          + ((System.nanoTime() - start)/1000000D) + "ms");
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
            records.clear();
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
                        log.trace("Interrupted");
                    }
                    checkCommit();
                }
            }
        }
    }

    private WritableStorage storage;
    private int batchSize;
    private int batchTimeout;
    private Batcher batcher;
    private Profiler profiler = new Profiler();

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
            log.warn(String.format("Old Storage address configuration detected."
                                   + " The key %s has been replaced by %s",
                                   DEPRECATED_CONF_STORAGE,
                                   ConnectionConsumer.CONF_RPC_TARGET));
        }

        storage = new StorageWriterClient(conf);
        batchSize = conf.getInt(CONF_BATCH_SIZE, DEFAULT_BATCH_SIZE);
        batchTimeout = conf.getInt(CONF_BATCH_TIMEOUT, DEFAULT_BATCH_TIMEOUT);
        batcher = new Batcher(batchSize, batchTimeout, storage);

        // TODO: Perform a check to see if the Storage is alive
    }

    public RecordWriter (WritableStorage storage,
                         int batchSize, int batchTimeout) {
        super (Configuration.newMemoryBased());
        this.storage = storage;
        this.batchSize = batchSize;
        this.batchTimeout = batchTimeout;
        batcher = new Batcher(batchSize, batchTimeout, storage);
    }

    /**
     * Flushes Records to Storage.
     * @param payload the Payload containing the Record to flush.
     */
    @Override
    protected void processPayload(Payload payload) {
        Record record = payload.getRecord();
        if (record == null) {
            throw new IllegalStateException("null received in Payload in next()"
                                            + ". This should not happen");
        }

        batcher.add(record);
        profiler.beat();
    }

    @Override
    public synchronized void close(boolean success) {
        super.close(success);
        log.info("Waiting for batch jobs to be committed");
        batcher.stop();
        log.info("Closing down RecordWriter. " + getProcessStats()
                 + ". Total time: " + profiler.getSpendTime());
    }

    
}


