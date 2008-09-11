package dk.statsbiblioteket.summa.storage.api;

import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;

import java.io.IOException;
import java.util.List;

/**
 * A helper class utilizing a stateless connection to a storage service exposing
 * a {@link ReadableStorage} interface. Unless your needs are very advanced
 * or you must do manual connection management, this is by far the
 * easiest way to use a remote {@link ReadableStorage}.
 * <p></p>
 * This class is modelled as a {@link ConnectionConsumer} meaning that you can
 * tweak its behavior by changing the configuration parameters
 * {@link dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory#CONF_RETRIES},
 * {@link dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory#CONF_GRACE_TIME},
 * {@link dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory#CONF_FACTORY}, and
 * {@link ConnectionConsumer#CONF_RPC_TARGET}
 */
public class StorageReaderClient extends ConnectionConsumer<ReadableStorage>
                                 implements Configurable {

    public StorageReaderClient(Configuration conf) {
        super (conf);
    }

    public RecordIterator getRecords(String base) throws IOException {
        ReadableStorage storage = getConnection();

        try {
            return storage.getRecords(base);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("getRecords("+base+") failed: "
                                  + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }


    public RecordIterator getRecordsModifiedAfter(long time, String base)
                                                            throws IOException {
        ReadableStorage storage = getConnection();

        try {
            return storage.getRecordsModifiedAfter(time, base);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("getRecordsModifiedAfter("+time+", "+base
                                  +") failed: " + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

    public RecordIterator getRecordsFrom(String id, String base)
                                                            throws IOException {
        ReadableStorage storage = getConnection();

        try {
            return storage.getRecordsFrom(id, base);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("getRecordsFrom("+id+", "+base
                                  +") failed: " + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

    public Record getRecord(String id) throws IOException {
        ReadableStorage storage = getConnection();

        try {
            return storage.getRecord(id);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("getRecord("+id+") failed: "
                                  + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

    public boolean recordExists(String id) throws IOException {
        ReadableStorage storage = getConnection();

        try {
            return storage.recordExists(id);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("recordExists("+id+") failed: "
                                  + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

    public boolean recordActive(String id) throws IOException {
        ReadableStorage storage = getConnection();

        try {
            return storage.recordActive(id);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("recordActive("+id+") failed: "
                                  + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

    public RecordAndNext next(Long iteratorKey) throws IOException {
        ReadableStorage storage = getConnection();

        try {
            return storage.next(iteratorKey);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("next("+iteratorKey+") failed: "
                                  + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

    public List<RecordAndNext> next(Long iteratorKey, int maxRecords)
                                                            throws IOException {
        ReadableStorage storage = getConnection();

        try {
            return storage.next(iteratorKey, maxRecords);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("next("+iteratorKey+", "+maxRecords
                                  +") failed: " + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

}



