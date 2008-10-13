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

import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link Storage} proxying requests onto a collection of sub-storages. The
 * matching is done on {@code base} level.
 * <p/>
 * Configuration instructions for the aggregating storage can be found under
 * {@link #CONF_SUB_STORAGES}.
 */
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

    private HashMap<String,StorageReaderClient> readers;
    private HashMap<String,StorageWriterClient> writers;

    private Log log;

    public AggregatingStorage (Configuration conf) throws IOException {
        super (conf);

        log = LogFactory.getLog(this.getClass().getName());
        log.debug ("Creating aggregating storage");

        List<Configuration> subConfs =
                                   conf.getSubConfigurations(CONF_SUB_STORAGES);

        if (subConfs.size() == 0) {
            log.warn ("No sub storages configured");
        }

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
                log.info("Configuring sub storage for base: " + base);
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
                log.info ("Configuring aggregated storage");
                StorageFactory.createStorage(storageConf);
            }
        }
    }

    public Iterator<Record> getRecordsFromBase(String base) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace ("getRecordsFromBase('"+base+"')");
        }

        StorageReaderClient reader = getSubStorageReader(base);

        if (reader == null) {
            log.warn("No sub storage configured for base '" + base + "'");
            return new ArrayList<Record>(0).iterator();
        }

        return reader.getRecordsFromBase(base);
    }

    public Iterator<Record> getRecordsModifiedAfter(long time, String base) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace ("getRecordsModifiedAfter("+time+", '"+base+"')");
        }

        StorageReaderClient reader = getSubStorageReader(base);

        if (reader == null) {
            log.warn("No sub storage configured for base '" + base + "'");
            return new ArrayList<Record>(0).iterator();
        }

        return reader.getRecordsModifiedAfter(time, base);
    }

    public boolean isModifiedAfter(long time, String base) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace ("isModifiedAfter("+time+", '"+base+"')");
        }

        StorageReaderClient reader = getSubStorageReader(base);

        if (reader == null) {
            log.warn("No sub storage configured for base '" + base + "'");
            return false;
        }

        return reader.isModifiedAfter(time, base);
    }

    public Iterator<Record> getRecordsFrom(String id, String base) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace ("getRecordsFrom('"+id+"', '"+base+"')");
        }

        StorageReaderClient reader = getSubStorageReader(base);

        if (reader == null) {
            log.warn("No sub storage configured for base '" + base + "'");
            return new ArrayList<Record>(0).iterator();
        }

        return reader.getRecordsFrom(id, base);
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

    public Record next(Long iteratorKey) throws IOException {
        /* This method is not needed. RecordIterator will
        *  call directly back to owning storage  */
        throw new UnsupportedOperationException();
    }

    public List<Record> next(Long iteratorKey, int maxRecords)
                                                            throws IOException {
        /* This method is not needed. RecordIterator will
        *  call directly back to owning storage  */
        throw new UnsupportedOperationException();
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

    public void close() throws IOException {
        /* FIXME: Should close() sub storages ? */
        log.info ("Closed. FIXME: Should close() sub storages?");
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
