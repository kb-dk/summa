package dk.statsbiblioteket.summa.storage;

import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageReaderClient;
import dk.statsbiblioteket.summa.storage.api.StorageWriterClient;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.*;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link Storage} proxying requests onto a collection of sub-storages. The
 * matching is done on {@code base} level. The actual requests are send through
 * {@link StorageReaderClient}s and {@link StorageWriterClient}s meaning that
 * all connections are stateless and will survive unstable connections to the
 * sub storages.
 * <p/>
 * Configuration instructions for the aggregating storage can be found under
 * {@link #CONF_SUB_STORAGES}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke")
public class AggregatingStorage extends StorageBase {

    /**
     * A list of sub configurations, one for each base name the aggregating
     * storage should map to a sub storage. Each sub configuration <i>must</i>
     * specify {@link #CONF_SUB_STORAGE_BASES} and
     * {@link ConnectionConsumer#CONF_RPC_TARGET}.
     * <p/>
     * Each sub configuration is passed to
     * a {@link StorageReaderClient} and a {@link StorageWriterClient} so
     * any configuration paramters applying to these classes may also be
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
    public static final String CONF_SUB_STORAGE_CONFIG = "summa.storage.substorage.config";

    public static final long UNKNOWN_BASE_KEY = -1;

    /**
     * Iterator keys time out after 24 hours of inactivity
     */
    public static final long ITERATOR_TIMEOUT = 86400000; // 24h

    private HashMap<String,StorageReaderClient> readers;
    private HashMap<String,StorageWriterClient> writers;
    private HashMap<Long,IteratorContext> iterators;
    private IteratorContextReaper reaper;

    private Log log;

    private static class IteratorContext {
        private long lastAccess;
        private long iterKey;
        private String base;
        private StorageReaderClient reader;

        public IteratorContext (StorageReaderClient reader, String base,
                                long iterKey, long lastAccess) {
            this.reader = reader;
            this.base = base;
            this.iterKey = iterKey;
            this.lastAccess = lastAccess;
        }

        public IteratorContext (StorageReaderClient reader, String base,
                                long iterKey) {
            this(reader, base, iterKey, System.currentTimeMillis());
        }

        public StorageReaderClient getReader () {
            accessed();
            return reader;
        }

        public String getBase () {
            accessed();
            return base;
        }

        public long getKey () {
            accessed();
            return iterKey;
        }

        public long getLastAccess () {
            return lastAccess;
        }

        public long accessed () {
            return (lastAccess = System.currentTimeMillis());
        }

        public boolean isTimedOut (long now) {
            return (now - lastAccess > ITERATOR_TIMEOUT);
        }

    }

    private static class IteratorContextReaper implements Runnable {

        private Map<Long,IteratorContext> iterators;
        private Log log;
        private boolean mayRun;
        private Thread thread;

        public static final int SLEEP_TIME = 1000*60;

        public IteratorContextReaper (Map<Long,IteratorContext> iterators) {
            log = LogFactory.getLog (IteratorContextReaper.class);
            this.iterators = iterators;
            mayRun = true;
            thread = new Thread(this);
            thread.setDaemon(true); // Allow JVM to exit when running
        }

        public void runInThread () {
            thread.start();
        }

        public void run() {
            log.debug ("Starting");
            while (mayRun) {
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    log.debug("Interrupted");
                }

                log.debug("Checking for timed out iterator keys");

                List<Long> timedOutKeys = new ArrayList<Long>();

                /* Detect timed out iterators */
                long now = System.currentTimeMillis();
                for (IteratorContext iter : iterators.values()) {
                    if (iter.isTimedOut(now)) {
                        timedOutKeys.add(iter.getKey());
                    }
                }

                /* Remove all timed out keys */
                for (Long iterKey : timedOutKeys) {
                    log.info (iterKey + " timed out");
                    iterators.remove(iterKey);
                }

            }
        }

        public void stop () {
            log.debug("Stopping");
            mayRun = false;

            if (thread.isAlive()) {
                thread.interrupt();
            }

            try {
                log.debug("Joining IteratorContextReaper thread");
                thread.join();
            } catch (InterruptedException e) {
                log.warn("Interrupted while joining " +
                         "IteratorContextReaper thread");
            }

            log.debug("Stopped");
        }
    }

    /**
     * List of storages to close when closing the aggregating storage.
     * See {@link #CONF_SUB_STORAGES}
     */
    private List<StorageWriterClient> toClose;

    public AggregatingStorage (Configuration conf) throws IOException {
        super (conf);

        log = LogFactory.getLog(this.getClass().getName());
        log.debug ("Creating aggregating storage");

        List<Configuration> subConfs =
                                   conf.getSubConfigurations(CONF_SUB_STORAGES);

        if (subConfs.size() == 0) {
            log.warn ("No sub storages configured");
        }

        readers = new HashMap<String,StorageReaderClient>();
        writers = new HashMap<String,StorageWriterClient>();
        iterators = new HashMap<Long,IteratorContext>();
        toClose = new ArrayList<StorageWriterClient>();

        reaper = new IteratorContextReaper(iterators);
        reaper.runInThread();

        for (Configuration subConf : subConfs) {
            List<String> bases;
            Configuration storageConf = null;

            try {
                bases = subConf.getStrings(CONF_SUB_STORAGE_BASES);
                if (bases.size () == 0) {
                    log.error ("No bases defined in sub configuration");
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
            } catch (IOException e) {
                log.warn ("No sub-sub-config for aggregated storage for bases '"
                          + Strings.join(bases, ", ") + "'. We can't tell if "
                          + "this is an error. See https://gforge.statsbiblioteket.dk/tracker/index.php?func=detail&aid=1487");
            }

            if (storageConf != null) {
                log.info ("Configuring aggregated storage: "
                          + writer.getVendorId());
                StorageFactory.createStorage(storageConf);

                /* Schedule this internally managed storage for closing when
                 * we close the aggregating storage */
                toClose.add(writer);
            }
        }
    }

    public long getRecordsFromBase(String base) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace ("getRecordsFromBase('"+base+"')");
        }

        StorageReaderClient reader = getSubStorageReader(base);

        if (reader == null) {
            log.warn("No sub storage configured for base '" + base + "'");
            return UNKNOWN_BASE_KEY;
        }

        long iterKey = reader.getRecordsFromBase(base);
        iterators.put(iterKey, new IteratorContext(reader, base, iterKey));
        return iterKey;
    }

    public long getRecordsModifiedAfter(long time, String base) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace ("getRecordsModifiedAfter("+time+", '"+base+"')");
        }

        StorageReaderClient reader = getSubStorageReader(base);

        if (reader == null) {
            log.warn("No sub storage configured for base '" + base + "'");
            return UNKNOWN_BASE_KEY;
        }

        long iterKey = reader.getRecordsModifiedAfter(time, base);
        iterators.put(iterKey, new IteratorContext(reader, base, iterKey));
        return iterKey;
    }

    public long getModificationTime (String base) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace ("getModificationTime("+base+")");
        }

        StorageReaderClient reader = getSubStorageReader(base);

        if (reader == null) {
            log.warn("No sub storage configured for base '" + base + "'");
            return -1;
        }

        return reader.getModificationTime (base);
    }

    public long getRecordsFrom(String id, String base) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace ("getRecordsFrom('"+id+"', '"+base+"')");
        }

        StorageReaderClient reader = getSubStorageReader(base);

        if (reader == null) {
            log.warn("No sub storage configured for base '" + base + "'");
            return UNKNOWN_BASE_KEY;
        }

        long iterKey = reader.getRecordsFrom(id, base);
        iterators.put(iterKey, new IteratorContext(reader, base, iterKey));
        return iterKey;
    }

    public List<Record> getRecords(List<String> ids, int expansionDepth) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace ("getRecords("+ Logs.expand(ids, 5)
                                    +", "+expansionDepth+")");
        }

        /* FIXME: This should be parallized*/
        List<Record> result = new ArrayList<Record>(ids.size());
        for (StorageReaderClient reader : readers.values()) {
            List<Record> recs = reader.getRecords(ids, expansionDepth);
            result.addAll(recs);
        }

        return result;
    }

    public Record getRecord(String id, int expansionDepth) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace ("getRecord('"+id+"', "+expansionDepth+")");
        }

        /* FIXME: This should be parallized*/
        Record r;
        for (StorageReaderClient reader : readers.values()) {
            r = reader.getRecord(id, expansionDepth);
            if (r != null) {
                return r;
            }
        }

        log.debug("No such record '" + id + "'");
        return null;
    }

    public Record next(long iteratorKey) throws IOException {
        if (iteratorKey == UNKNOWN_BASE_KEY) {
            throw new NoSuchElementException("Empty iterator " + iteratorKey);
        }

        IteratorContext iter = iterators.get(iteratorKey);

        if (iter == null) {
            throw new IllegalArgumentException("No such iterator: "
                                               + iteratorKey);
        }

        return iter.getReader().next(iteratorKey);
    }

    public List<Record> next(long iteratorKey, int maxRecords)
                                                            throws IOException {
        if (iteratorKey == UNKNOWN_BASE_KEY) {
            throw new NoSuchElementException("Empty iterator " + iteratorKey);
        }

        IteratorContext iter = iterators.get(iteratorKey);

        if (iter == null) {
            throw new IllegalArgumentException("No such iterator: "
                                               + iteratorKey);
        }

        return iter.getReader().next(iteratorKey, maxRecords);
    }

    public void flush(Record record) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace ("flush("+record+")");
        }

        StorageWriterClient writer = getSubStorageWriter(record.getBase());

        if (writer == null) {
            log.warn("No sub storage configured for base '"
                     + record.getBase() + "'");
            return;
        }

        writer.flush(record);
    }

    public void flushAll(List<Record> records) throws IOException {
        /* FIXME: Batch records into groups for each base and commit batches of records instead of singles */
        for (Record r : records) {
            flush (r);
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
    public void close() throws IOException {
        log.info ("Closing");

        reaper.stop();

        for (StorageWriterClient writer : toClose) {
            log.info ("Closing internally configured sub storage: "
                      + writer.getVendorId());
            writer.close();
        }

        log.info ("Closed");
    }

    public void clearBase(String base) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace ("clearBase("+base+")");
        }

        StorageWriterClient writer = getSubStorageWriter(base);

        if (writer == null) {
            log.warn("No sub storage configured for base '"
                     + base + "'");
            return;
        }

        writer.clearBase(base);
    }

    protected StorageReaderClient getSubStorageReader (String base) {
        return readers.get(base);
    }

    protected StorageWriterClient getSubStorageWriter (String base) {
        return writers.get(base);
    }
}
