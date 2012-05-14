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
package dk.statsbiblioteket.summa.storage;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.ReadableStorage;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.StorageReaderClient;
import dk.statsbiblioteket.summa.storage.api.StorageWriterClient;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link Storage} proxying requests onto a collection of sub-storages. The
 * matching is done on {@code base} level. The actual requests are send through
 * {@link StorageReaderClient}s and {@link StorageWriterClient}s meaning that
 * all connections are state less and will survive unstable connections to the
 * sub storages.
 * <p/>
 * Configuration instructions for the aggregating storage can be found under
 * {@link #CONF_SUB_STORAGES}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke, hbk")
public class AggregatingStorage extends StorageBase {

    /**
     * A list of sub configurations, one for each base name the aggregating
     * storage should map to a sub storage. Each sub configuration <i>must</i>
     * specify {@link #CONF_SUB_STORAGE_BASES} and
     * {@link ConnectionConsumer#CONF_RPC_TARGET}.
     * <p/>
     * Each sub configuration is passed to
     * a {@link StorageReaderClient} and a {@link StorageWriterClient} so
     * any configuration parameters applying to these classes may also be
     * supplied here.
     * <p/>
     * If the sub storage configuration contains another sub configuration
     * under the {@link #CONF_SUB_STORAGE_CONFIG} key the given sub storage
     * will be instantiated from that configuration.
     * <i>Any sub storages created this way will be closed when closing the
     * parent aggregating storage (ie. by calling
     * {@link AggregatingStorage#close})</i>
     */
    public static final String CONF_SUB_STORAGES = "summa.storage.substorages";

    /**
     * Configuration property defining a list of base names that a given sub
     * storage is responsible for.
     */
    public static final String CONF_SUB_STORAGE_BASES =
                                                "summa.storage.substorage.base";

    /**
     * Configuration property containing a sub configuration that, if present,
     * will cause the aggregating storage to create a {@link Storage} instance
     * using {@link StorageFactory#createStorage(Configuration)} with this
     * sub configuration.
     */
    public static final String CONF_SUB_STORAGE_CONFIG =
                                              "summa.storage.substorage.config";
    /** ID for unknown base keys. */
    public static final long UNKNOWN_BASE_KEY = -1;

    /**
     * Iterator keys time out after 24 hours of inactivity.
     */
    public static final long ITERATOR_TIMEOUT = 86400000; // 24h
    /** Map containing all readers. */
    private HashMap<String, StorageReaderClient> readers;
    /** Map containing all writers. */
    private HashMap<String, StorageWriterClient> writers;
    /** Map of iterator keys. */
    private HashMap<Long, IteratorContext> iterators;
    /** TODO . */
    private IteratorContextReaper reaper;
    /** Local log instance. */
    private Log log;

    /**
     * Merging context class.
     */
    private class MergingContext extends IteratorContext {
        /** Readable storage list. */
        private ReadableStorage[] readerList;
        /** Record buffer. */
        private Record[] recBuffer;
        /** List of iterator keys. */
        private long[] iterKeys;

        /**
         * Create a MergingContext.
         *
         * @param mtime modified time.
         * @param opts query options.
         * @param lastAccess lastAccess.
         * @throws IOException if error occurred.
         */
        public MergingContext(long mtime, QueryOptions opts,
                              long lastAccess) throws IOException {
            super(null, null, mtime, opts, lastAccess);

            log.debug("Creating merging iterator over all sub storages");

            /* The iterKey for the merging iterator is constructed as
             * the sum of all sub iterator keys. This is *almost* guaranteed
             * to be unique since we assume that all iterator keys
             * constructed from the sub storages are strictly increasing */
            iterKey = 0;

            readerList = new ReadableStorage[readers.size()];
            iterKeys = new long[readers.size()];

            int counter = 0;
            for (Map.Entry<String, StorageReaderClient> entry
                    : readers.entrySet()) {
                ReadableStorage reader = entry.getValue();
                String readerBase = entry.getKey();
                IteratorContext subIter = new IteratorContext(
                        reader, readerBase, mtime, opts, lastAccess);
                long subKey = subIter.getKey();
                log.debug("Iterkey for '" + reader.toString() + "' is '"
                                                                      + subKey);
                // TODO Better collision handling
                if (iterators.containsKey(subKey)) {
                    throw new RuntimeException(String.format(
                            "Internal error. Iterator key collision '%s'",
                            subKey));
                }

                // Calculate the merger's iterKey as the sum the children's
                iterKey += subKey;

                // We need to store the readers to guarantee the correct order
                readerList[counter] = reader;

                iterKeys[counter] = subKey;
                iterators.put(subKey, subIter);

                counter++;
            }
            log.trace("Merging iterator ready");
        }


        @Override
        public Record next() throws IOException {
            if (recBuffer == null) {
                initRecBuffer();
            }

            Record newest = null;
            int newestOffset = -1;
            for (int i = 0; i < recBuffer.length; i++) {
                if (recBuffer[i] == null) {
                    // This sub storage iter is depleted
                    continue;
                }

                if (newest == null) {
                    newest = recBuffer[i];
                    newestOffset = i;
                    continue;
                }

                if (newest.getModificationTime()
                        < recBuffer[i].getModificationTime()) {
                    newest = recBuffer[i];
                    newestOffset = i;
                }
            }

            if (newest == null) {
                throw new NoSuchElementException();
            }

            try {
                recBuffer[newestOffset] = nextFromSub(newestOffset);
            } catch (NoSuchElementException e) {
                recBuffer[newestOffset] = null;
            }
            return newest;
        }

        /**
         * Get a list of records.
         *
         * @param maxRecords Maximum number of records.
         * @return a list of records.
         * @throws IOException if connection error occur.
         */
        @Override
        public List<Record> next(int maxRecords) throws IOException {
            List<Record> result = new ArrayList<Record>(maxRecords);

            try {
                for (int i = 0; i < maxRecords; i++) {
                    result.add(next());
                }
            } catch (NoSuchElementException e) {
                // Iterator is depleated
                if (result.size() == 0) {
                    throw new NoSuchElementException();
                }
            }

            return result;
        }

        /**
         * Returns the next record from the readerOffset reader in the reader
         * list.
         * @param readerOffset The offset into the readable storage list.
         * @return The next record.
         * @throws IOException If error occur while fetching record.
         */
        private Record nextFromSub(int readerOffset) throws IOException {
            return readerList[readerOffset].next(iterKeys[readerOffset]);
        }

        /**
         * Initialize record buffer.
         * @throws IOException If error occur communicating with storage.
         */
        private void initRecBuffer() throws IOException {
            if (recBuffer != null) {
                log.error("Internal error. Double initialization of "
                          + "recBuffer for MergingContext");
                return;
            }

            log.debug("Filling record buffer for merging iterator");

            recBuffer = new Record[readers.size()];

            // Fill in recBuffer with the first record from each sub storage
            for (int i = 0; i < readerList.length; i++) {
                try {
                    recBuffer[i] = nextFromSub(i);
                } catch (NoSuchElementException e) {
                    recBuffer[i] = null;
                }
            }
        }
    }

    /**
     * Iterator context class.
     */
    private static class IteratorContext {
        /** Readable storage. */
        protected ReadableStorage reader;
        /** The base. */
        protected String base;
        /** Last modified time. */
        protected long mtime;
        /** Private logger instance. */
        private Log log;
        /** Query options used by this context. */
        protected QueryOptions opts;
        /** Last access time. */
        protected long lastAccess;
        /** The iterator key. */
        protected long iterKey=0;

        /**
         * Creates an iterator context.
         * @param reader The readable storage.
         * @param base The base in storage.
         * @param mtime The modification time.
         * @param opts The options.
         * @param lastAccess The last access time.
         * @throws IOException If error occur.
         */
        public IteratorContext(ReadableStorage reader, String base, long mtime,
                               QueryOptions opts, long lastAccess)
                                                            throws IOException {
            this.reader = reader;
            this.base = base;
            this.mtime = mtime;
            this.opts = opts;
            log = LogFactory.getLog(IteratorContext.class);
            this.lastAccess = lastAccess;

            log.debug("IteratorContext class created with mtime '" + mtime
                    + "', base '" + base + "'.");

            if (reader != null) {
                this.iterKey = reader.getRecordsModifiedAfter(mtime, base,
                                                              opts);
            }
        }

        /**
         * @return The next record.
         * @throws IOException If error occur while communicating with storage.
         */
        public Record next() throws IOException {
            return reader.next(iterKey);
        }

        /**
         * Return a list of {@code madRecords} records.
         * @param maxRecords Number of records.
         * @return A list of at most {@code maxRecords} records.
         * @throws IOException If error occur while communicating with storage.
         */
        public List<Record> next(int maxRecords) throws IOException {
            return reader.next(iterKey, maxRecords);
        }

        /**
         * Return the iterator key.
         * @return the iterator key.
         */
        public long getKey() {
            accessed();
            return iterKey;
        }

        /*public long getLastAccess () {
            return lastAccess;
        }*/

        /**
         * Return the last access time.
         * @return Last access time.
         */
        public long accessed() {
            return (lastAccess = System.currentTimeMillis());
        }

        /**
         * Return true if this iterator context is time out.
         * @param now Time stamp.
         * @return True if this iterator context is timed out, false otherwise.
         */
        public boolean isTimedOut(long now) {
            return (now - lastAccess > ITERATOR_TIMEOUT);
        }
    }

    /**
     * Iterator Context Reaper class.
     */
    private static class IteratorContextReaper implements Runnable {
        /** Map from iterator key to iterator contexts. */
        private Map<Long, IteratorContext> iterators;
        /** Local log instance. */
        private Log log;
        /** True if this may run. */
        private boolean mayRun;
        /** Private thread. */
        private Thread thread;
        /** Sleep time. */
        public static final int SLEEP_TIME = 1000 * 60;

        /**
         * Creates an iterator context.
         * @param iterators A map of iterator keys and iterator context.
         */
        public IteratorContextReaper(Map<Long, IteratorContext> iterators) {
            log = LogFactory.getLog(IteratorContextReaper.class);
            this.iterators = iterators;
            mayRun = true;
            thread = new Thread(this, "AggregatingStorage daemon");
            thread.setDaemon(true); // Allow JVM to exit when running
        }

        /**
         * If this method is called, this is runned in a thread.
         */
        public void runInThread() {
            thread.start();
        }

        /**
         * The main run method.
         */
        @Override
        public void run() {
            log.debug("Starting");
            while (mayRun) {
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    log.debug("Interrupted");
                }

                log.debug("Checking for timed out iterator keys");

                List<Long> timedOutKeys = new ArrayList<Long>();

                // Detect timed out iterators
                long now = System.currentTimeMillis();
                for (IteratorContext iter : iterators.values()) {
                    if (iter.isTimedOut(now)) {
                        timedOutKeys.add(iter.getKey());
                    }
                }

                // Remove all timed out keys
                for (Long iterKey : timedOutKeys) {
                    log.info(iterKey + " timed out");
                    iterators.remove(iterKey);
                }

            }
        }

        /**
         * Stop method for stopping running thread and cleaning up.
         */
        public void stop() {
            log.debug("Stopping");
            mayRun = false;

            if (thread.isAlive()) {
                thread.interrupt();
            }

            try {
                log.debug("Joining IteratorContextReaper thread");
                thread.join();
            } catch (InterruptedException e) {
                log.warn("Interrupted while joining "
                         + "IteratorContextReaper thread");
            }

            log.debug("Stopped");
        }
    }

    /**
     * List of storages to close when closing the aggregating storage.
     * See {@link #CONF_SUB_STORAGES}
     */
    private List<StorageWriterClient> toClose;

    /**
     * Aggregating storage constructor.
     *
     * @param conf The configuration.
     * @throws IOException if any errors are experienced during creation.
     */
    public AggregatingStorage(Configuration conf) throws IOException {
        super(conf);

        log = LogFactory.getLog(this.getClass().getName());
        log.debug("Creating aggregating storage");

        List<Configuration> subConfs;
        try {
            subConfs = conf.getSubConfigurations(CONF_SUB_STORAGES);
        } catch (Exception e) {
            throw new IOException("", e);
        }

        if (subConfs.size() == 0) {
            log.warn("No sub storages configured");
        }

        readers = new HashMap<String, StorageReaderClient>();
        writers = new HashMap<String, StorageWriterClient>();
        iterators = new HashMap<Long, IteratorContext>();
        toClose = new ArrayList<StorageWriterClient>();

        reaper = new IteratorContextReaper(iterators);
        reaper.runInThread();

        for (Configuration subConf : subConfs) {
            List<String> bases;
            Configuration storageConf = null;

            try {
                bases = subConf.getStrings(CONF_SUB_STORAGE_BASES);
                if (bases.size() == 0) {
                    log.error("No bases defined in sub configuration");
                    continue;
                }
            } catch (NullPointerException e) {
                throw new Configurable.ConfigurationException(
                                    CONF_SUB_STORAGE_BASES
                                    + " must be defined for each sub storage");
            }

            StorageReaderClient reader = new StorageReaderClient(subConf);
            StorageWriterClient writer = new StorageWriterClient(subConf);

            for (String base : bases) {
                log.info("Sub storage base map: " + writer.getVendorId()
                                                  + " -> " + base);
                readers.put(base, reader);
                writers.put(base, writer);
            }

            try {
                storageConf = subConf.getSubConfiguration(
                                                       CONF_SUB_STORAGE_CONFIG);
            } catch (SubConfigurationsNotSupportedException e) {
                log.warn("Storage doesn't support sub configurations");
            } catch (NullPointerException e) {
                log.warn("No sub-sub-config for aggregated storage for bases '"
                          + Strings.join(bases, ", ") + "'");
            }

            if (storageConf != null) {
                log.info("Configuring aggregated storage: "
                          + writer.getVendorId());
                StorageFactory.createStorage(storageConf);

                /* Schedule this internally managed storage for closing when
                 * we close the aggregating storage */
                toClose.add(writer);
            }
        }
    }

    /**
     * Returns a iterator key over records modified after a given time.
     * @param time Earliest time stamp we want records from.
     * @param base The base in storage
     * @param options The query options.
     * @return An iterator key.
     * @throws IOException If error occur while communicating with storage.
     */
    @Override
    public long getRecordsModifiedAfter(long time, String base,
                                      QueryOptions options) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("AggregatingStorage.getRecordsModifiedAfter("
                      + time + ", '" + base + "')");
        }

        IteratorContext ctx;
        long now = System.currentTimeMillis();

        if (base == null) {
            /* If the base is undefined the iterator must be merged based on all
             * sub storages */
            ctx = new MergingContext(time, options, now);
        } else {
            /* If the base is indeed defined then simply return the iterkey from
             * the relevant sub storage */
            StorageReaderClient reader = getSubStorageReader(base);
            if (reader == null) {
                log.warn("No sub storage configured for base '" + base + "'");
                return UNKNOWN_BASE_KEY;
            }
            ctx = new IteratorContext(reader, base, time, options, now);
        }

        long iterKey = ctx.getKey();
        log.debug("getModification iterKey '" + iterKey + "'.");
        // TODO Handle iterKey collisions in a sane way!
        if (iterators.containsKey(iterKey)) {
            throw new RuntimeException("Internal error. Iterator key "
                                       + "collision '" + iterKey + "'");
        }

        iterators.put(iterKey, ctx);
        if (log.isTraceEnabled()) {
            log.trace("getRecordsModifiedAfter returns: "
                    + iterKey + ".");
        }
        return iterKey;
    }

    
    
 
    @Override
    public long getRecordsModifiedAfterLoadData(long time, String base,
                                      QueryOptions options) throws IOException {
 
    	throw new RuntimeException("Not implemented");
    }

    /**
     * Return last modification time for a given base.
     * @param base The base in storage.
     * @return Last modification time.
     * @throws IOException If error occur while communicating with storage.
     */
    @Override
    public long getModificationTime(String base) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("getModificationTime(" + base + ")");
        }

        /* If the base is undefined return the maximal mtime from all
         * sub storages */
        if (base == null) {
            long mtime = 0;
            for (Map.Entry<String, StorageReaderClient> entry
                    : readers.entrySet()) {
                StorageReaderClient reader = entry.getValue();
                String readerBase = entry.getKey();
                mtime = Math.max(mtime, reader.getModificationTime(readerBase));
            }
            return mtime;
        }

        /* If the base is defined then simply return the mtime from the
         * corresponding sub storage */
        StorageReaderClient reader = getSubStorageReader(base);

        if (reader == null) {
            log.warn("No sub storage configured for base '" + base + "'");
            return -1;
        }

        return reader.getModificationTime(base);
    }

    /**
     * Returns a list of records.
     * @param ids A list of ID's on the records wanted.
     * @param options The query options to select records from.
     * @return A list of records.
     * @throws IOException If error occur while communicating with storage.
     */
    @Override
    public List<Record> getRecords(List<String> ids, QueryOptions options)
                                                            throws IOException {
        final int logExpands = 5;
        long startTime = System.currentTimeMillis();
        if (log.isTraceEnabled()) {
            log.trace("getRecords(" + Logs.expand(ids, logExpands)
                                  + ", " + options + ")");
        }

        // FIXME: This should be parallized
        List<Record> result = new ArrayList<Record>(ids.size());
        for (StorageReaderClient reader : readers.values()) {
            List<Record> recs = reader.getRecords(ids, options);
            result.addAll(recs);
        }
        //noinspection DuplicateStringLiteralInspection
        log.debug("Finished getRecords(" + ids.size() + " records ids, ...) -> "
                  + result.size() + "records in "
                  + (System.currentTimeMillis() - startTime));
        return result;
    }

    /**
     * Return a single record.
     * @param id The record id.
     * @param options The query options to select records from.
     * @return A record with the given id.
     * @throws IOException If error occur while communication with storage.
     */
    @Override
    public Record getRecord(String id, QueryOptions options)
                                                            throws IOException {
        long startTime = System.currentTimeMillis();
        if (log.isTraceEnabled()) {
            log.trace("getRecord('" + id + "', " + options + ")");
        }

        // FIXME: This should be parallized
        Record r;
        for (StorageReaderClient reader : readers.values()) {
            r = reader.getRecord(id, options);
            if (r != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Finished getRecord(" + id + ",...) -> " + r
                              +  " in "
                              + (System.currentTimeMillis() - startTime));
                }
                return r;
            }
        }

        log.debug("No such record '" + id + "'");
        return null;
    }

    /**
     * Returns the next record from the iterator.
     * @param iteratorKey The iterator key.
     * @return The next record from the iterator.
     * @throws IOException If error occur while communication with storage.
     */
    @Override
    public Record next(long iteratorKey) throws IOException {
        if (iteratorKey == UNKNOWN_BASE_KEY) {
            throw new NoSuchElementException("Empty iterator " + iteratorKey);
        }

        IteratorContext iter = iterators.get(iteratorKey);

        if (iter == null) {
            throw new IllegalArgumentException("No such iterator: "
                                               + iteratorKey);
        }

        Record r = iter.next();
        if (r == null) {
            log.debug("Iterator " + iteratorKey + " depleted");
            iterators.remove(iteratorKey);
        }

        return r;
    }

    /**
     * Get maxRecords records associated with given iterator.
     *
     * @param iteratorKey the key given by {@link ReadableStorage}.
     * @param maxRecords max number of records returned.
     * @return List containing max number of records, associated to the iterator
     * key.
     * @throws IOException if error occurred when fetching elements.
     */
    @Override
    public List<Record> next(long iteratorKey, int maxRecords)
                                                            throws IOException {
        if (iteratorKey == UNKNOWN_BASE_KEY) {
            throw new NoSuchElementException("Empty iterator " + iteratorKey);
        }

        if (log.isTraceEnabled()) {
            log.trace("AggregratingStorage.next(" + iteratorKey + ", "
                    + maxRecords + ") entered.");
        }

        IteratorContext iter = iterators.get(iteratorKey);

        if (iter == null) {
            throw new IllegalArgumentException("No such iterator: "
                                               + iteratorKey);
        }

        List<Record> recs = iter.next(maxRecords);
        if (recs == null || recs.isEmpty()) {
            log.debug("Iterator " + iteratorKey + " depleted");
            iterators.remove(iteratorKey);
        }

        return recs;
    }

    /**
     * Flushes a record into the storage, taking any options into consideration.
     * @param record The record to flush.
     * @param options The options to take into consideration.
     * @throws IOException If error occur while communication with storage.
     */
    @Override
    public void flush(Record record, QueryOptions options) throws IOException {
        long startTime = System.currentTimeMillis();
        if (log.isTraceEnabled()) {
            log.trace("flush(" + record + ")");
        }

        StorageWriterClient writer = getSubStorageWriter(record.getBase());

        if (writer == null) {
            log.warn("No sub storage configured for base '"
                     + record.getBase() + "'");
            return;
        }

        writer.flush(record, options);
        if (log.isDebugEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.debug("Flushed " + record + " in "
                      + (System.currentTimeMillis() - startTime) + "ms");
        }
    }

    /**
     * Flushes a list of records into the storage.
     * @param records A list of records to flush into the storage.
     * @throws IOException If error occur while communication with storage.
     */
    @Override
    public void flushAll(List<Record> records) throws IOException {
        // FIXME: Batch records into groups for each base and commit batches of
        // records instead of singles
        for (Record r : records) {
            flush(r);
        }
    }

    /**
     * Close the aggregating storage. This method will also close any sub
     * storages created from nested configurations in the
     * {@link #CONF_SUB_STORAGE_CONFIG} property.
     * <p/>
     * If you want to avoid having the aggregating storage close some specific
     * sub storages these storages should be configured and run externally.
     *
     * @throws IOException on communication errors with the storage or any
     *                     of the sub storages
     */
    @Override
    public void close() throws IOException {
        log.info("Closing");

        reaper.stop();

        for (StorageWriterClient writer : toClose) {
            log.info("Closing internally configured sub storage: "
                     + writer.getVendorId());
            writer.close();
        }

        log.info("Closed");
    }

    /**
     * Clears all records for a given base.
     * @param base The record base to clear.
     * @throws IOException If error occur while communication with storage.
     */
    @Override
    public void clearBase(String base) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("clearBase(" + base + ")");
        }

        StorageWriterClient writer = getSubStorageWriter(base);

        if (writer == null) {
            log.warn("No sub storage configured for base '"
                     + base + "'");
            return;
        }
        writer.clearBase(base);
    }

    /**
     * Runs a batch jobs on storage.
     * @param jobName The batch job name.
     * @param base The records base.
     * @param minMtime The minimum modification time on which batch job is
     * runned.
     * @param maxMtime The maximum modification time on which batch job is
     * runned.
     * @param options The query options.
     * @return The result of the batch job.
     * @throws IOException If error occur while communication with storage.
     */
    @Override
    public String batchJob(String jobName, String base, long minMtime,
                           long maxMtime, QueryOptions options)
                                                            throws IOException {
        log.debug(String.format("Batch job '%s' on '%s", jobName, base));

        if (base != null) {
            StorageWriterClient writer = getSubStorageWriter(base);

            if (writer == null) {
                log.warn("No sub storage configured for base '"
                         + base + "'");
                return "";
            }

            return writer.batchJob(jobName, base, minMtime, maxMtime, options);
        } else {
            List<String> results = new LinkedList<String>();
            for (StorageWriterClient sub : writers.values()) {
                String result;
                try {
                    result = sub.batchJob(
                            jobName, base, minMtime, maxMtime, options);
                } catch (Throwable t) {
                    result = String.format(
                            "ERROR(%s): %s", sub.getVendorId(), t.getMessage());
                }
                results.add(result);
            }
            return Strings.join(results, "\n");
        }
    }

    /**
     * Returns a storage reader client for a specific base.
     * @param base The records base.
     * @return A storage reader client.
     */
    protected StorageReaderClient getSubStorageReader(String base) {
        return readers.get(base);
    }

    /**
     * Returns a storage writer client for a specific base.
     * @param base The records base.
     * @return A storage writer client.
     */
    protected StorageWriterClient getSubStorageWriter(String base) {
        return writers.get(base);
    }
}
