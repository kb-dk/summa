package dk.statsbiblioteket.summa.storage.api;

import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;
import java.util.List;
import java.net.NoRouteToHostException;

/**
 * A helper class utilizing a stateless connection to a storage service exposing
 * a {@link WritableStorage} interface. Unless your needs are very advanced
 * or you must do manual connection management, this is by far the
 * easiest way to use a remote {@link WritableStorage}.
 * <p></p>
 * This class is modelled as a {@link ConnectionConsumer} meaning that you can
 * tweak its behavior by changing the configuration parameters
 * {@link dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory#CONF_RETRIES},
 * {@link dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory#CONF_GRACE_TIME},
 * {@link dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory#CONF_FACTORY}, and
 * {@link ConnectionConsumer#CONF_RPC_TARGET}.
 * <p/>
 * The property {@link ConnectionConsumer#CONF_RPC_TARGET} <i>must</i> be
 * defined all others are optional.
 *
 * @see StorageReaderClient 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke")
public class StorageWriterClient extends ConnectionConsumer<WritableStorage>
                                 implements WritableStorage {
    private static final String UNABLE_TO_CONNECT = "Unable to connect to ";

    /**
     * Create a new storage writer. If no RPC vendor is defined in the
     * {@link #CONF_RPC_TARGET} property of {@code conf} then the writer will
     * default to {@code //localhost:28000/summa-storage}
     *
     * @param conf configuration used to instantiate the underlying
     *             {@link ConnectionConsumer}
     */
    public StorageWriterClient(Configuration conf) {
        super(conf, "//localhost:28000/summa-storage");
    }

    public void flush(Record record) throws IOException {
        WritableStorage storage = getConnection();

        if (storage == null) {
            throw new NoRouteToHostException(UNABLE_TO_CONNECT + getVendorId());
        }

        try {
            storage.flush (record);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException(String.format(
                    "flush(%s) failed: %s", record, t.getMessage()), t);
        } finally {
            releaseConnection();
        }
    }

    public void flushAll(List<Record> records) throws IOException {
        WritableStorage storage = getConnection();

        if (storage == null) {
            throw new NoRouteToHostException(UNABLE_TO_CONNECT + getVendorId());
        }
        
        try {
            storage.flushAll (records);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException(String.format(
                    "flushAll(%s) failed: %s", records, t.getMessage()), t);
        } finally {
            releaseConnection();
        }
    }

    public void clearBase(String base) throws IOException {
        WritableStorage storage = getConnection();

        if (storage == null) {
            throw new NoRouteToHostException(UNABLE_TO_CONNECT + getVendorId());
        }

        try {
            storage.clearBase (base);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException(String.format(
                    "clearBase(%s) failed: %s", base, t.getMessage()), t);
        } finally {
            releaseConnection();
        }
    }

    public void close() throws IOException {
        WritableStorage storage = getConnection();

        if (storage == null) {
            throw new NoRouteToHostException(UNABLE_TO_CONNECT + getVendorId());
        }

        try {
            storage.close ();
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("close() failed: " + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }
}
