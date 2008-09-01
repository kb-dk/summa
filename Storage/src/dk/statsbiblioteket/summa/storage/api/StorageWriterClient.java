package dk.statsbiblioteket.summa.storage.api;

import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;

import java.io.IOException;
import java.util.List;

/**
 * A helper class utilizing a stateless connection to a storage service exposing
 * a {@link WritableStorage} interface. Unless your needs are very advanced
 * or you must do manual connection management, this is by far the
 * easiest way to use a remote {@link WritableStorage}.
 * <p></p>
 * This class is modelled as a {@link ConnectionConsumer} meaning that you can
 * tweak its behavior by changing the configuration parameters
 * {@link dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory#RETRIES},
 * {@link dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory#GRACE_TIME},
 * {@link dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory#FACTORY}, and
 * {@link ConnectionConsumer#PROP_RPC_TARGET}
 */
public class StorageWriterClient extends ConnectionConsumer<WritableStorage> {

    public StorageWriterClient(Configuration conf) {
        super(conf);
    }

    public void flush(Record record) throws IOException {
        WritableStorage storage = getConnection();

        try {
            storage.flush (record);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("flush("+record+") failed: "
                                  + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

    public void flushAll(List<Record> records) throws IOException {
        WritableStorage storage = getConnection();

        try {
            storage.flushAll (records);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("flushAll("+records+") failed: "
                                  + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

    public void close() throws IOException {
        WritableStorage storage = getConnection();

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
