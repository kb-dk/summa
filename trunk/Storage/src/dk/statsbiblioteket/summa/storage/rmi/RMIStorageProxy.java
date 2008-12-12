package dk.statsbiblioteket.summa.storage.rmi;

import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.rmi.RemoteStorage;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.database.derby.DerbyStorage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.List;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link Storage} implementation capable of wrapping an underlying backend
 * {@code Storage} and expose it over RMI.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke")
public class RMIStorageProxy extends UnicastRemoteObject
                             implements RemoteStorage {

    /**
     * The class used for the storage backend. If this is set it will be
     * written to {@link Storage#CONF_CLASS} before submitting
     * the configuration to {@link StorageFactory#createStorage}.
     * <p/>
     * If this property is not set the proxy will fall back to
     * {@link #DEFAULT_BACKEND}.
     */
    public static final String CONF_BACKEND = "summa.storage.rmi.backend";

    /**
     * Default class for the storage backend implementation
     */
    public static final Class<? extends Storage> DEFAULT_BACKEND =
                                                             DerbyStorage.class;

    /**
     * Configuration property specifying which port the registry used by
     * the Storage can be found on. Default is 28000.
     */
    public static final String CONF_REGISTRY_PORT = "summa.storage.rmi.registry.port";

    /**
     * Configuration property specifying the service name of the Storage service.
     * Default is {@code summa-storage}.
     */
    public static final String CONF_SERVICE_NAME = "summa.storage.rmi.service.name";

    private static final Log log = LogFactory.getLog(RMIStorageProxy.class);

    private Storage backend;
    private String serviceName;
    private int registryPort;

    public RMIStorageProxy (Configuration conf) throws IOException {
        super (getServicePort(conf));

        /* Create configuration for the backend, based on our own,
         * rewriting the class property if necessary */
        // FIXME: The below config should really be kept entirely in memory,
        //        but we can't use a memorybased config because of bug:
        //        https://gforge.statsbiblioteket.dk/tracker/index.php?func=detail&aid=1453&group_id=8&atid=109
        Configuration backendConf = new Configuration (new XStorage());
        backendConf.importConfiguration (conf);
        if (conf.valueExists (CONF_BACKEND)) {
            backendConf.set (CONF_CLASS, conf.getString (CONF_BACKEND));
        } else {
            log.info (CONF_BACKEND + " not set, using " + DEFAULT_BACKEND + " for "
                      + "backend");
            backendConf.set (CONF_CLASS, DEFAULT_BACKEND);
        }

        /* If the backend is set to be another RMIStorageProxy then avoid
         * infinite recursion by forcing it into a DerbyStorage */
        if (backendConf.valueExists (CONF_CLASS)) {
            if (this.getClass().getName().equals(
                                          backendConf.getString (CONF_CLASS))) {
                log.warn ("Backend set to RMIStorageProxy. Forcing backend " +
                          "class to " + DEFAULT_BACKEND.getName()
                          + " to avoid infinite recursion");
                backendConf.set (CONF_CLASS, DEFAULT_BACKEND.getName());
            }
        }

        if (log.isTraceEnabled ()) {
            log.trace ("Backend conf:\n" + backendConf.dumpString ());
        }

        log.trace ("Creating storage backend");
        backend = StorageFactory.createStorage (backendConf);
        log.trace ("Created storage: " + backend.getClass().getName());

        serviceName = conf.getString (CONF_SERVICE_NAME, "summa-storage");
        registryPort = conf.getInt(CONF_REGISTRY_PORT, 28000);

        RemoteHelper.exportRemoteInterface (this, registryPort, serviceName);

        try {
            RemoteHelper.exportMBean (this);
        } catch (Exception e) {
            String msg = "Error exporting MBean of '" + this
                         + "'. Going on without it: " + e.getMessage ();
            if (log.isTraceEnabled()) {
                log.warn (msg, e);
            } else {
                log.warn(msg);
            }
        }
    }

     private static int getServicePort(Configuration configuration) {
        try {
            return configuration.getInt(Storage.CONF_SERVICE_PORT);
        } catch (NullPointerException e) {
            log.warn ("Service port not defined in "
                    + Storage.CONF_SERVICE_PORT + ". Falling back to "
                    + "anonymous port");
            return 0;
        }
    }

    /* Reader methods */
    @Override
    public long getRecordsModifiedAfter(long time, String base,
                                        QueryOptions options)
                                                        throws RemoteException {
        try {
            return backend.getRecordsModifiedAfter(time, base, options);
        } catch (IOException e) {
            throw new RemoteException("Failed to get records modified after "
                                      + time + " from base '"
                                      + base + "': " + e.getMessage(), e);
        }
    }

    @Override
    public long getModificationTime (String base) throws RemoteException {
        try {
            return backend.getModificationTime (base);
        } catch (IOException e) {
            throw new RemoteException("Failed to check modification state on "
                                      + "base '" + base + "': "
                                      + e.getMessage(), e);
        }
    }

    @Override
    public List<Record> getRecords(List<String> ids, QueryOptions options)
                                                        throws RemoteException {
        try {
            return backend.getRecords(ids, options);
        } catch (IOException e) {
            throw new RemoteException("Failed to get records "
                                      + Logs.expand(ids, 5) +": "
                                      + e.getMessage(), e);
        }
    }

    @Override
    public Record getRecord(String id, QueryOptions options)
                                                        throws RemoteException {
        try {
            return backend.getRecord(id, options);
        } catch (IOException e) {
            throw new RemoteException("Failed to get record '" + id + "': "
                                      + e.getMessage(), e);
        }
    }

    @Override
    public Record next(long iteratorKey) throws RemoteException {
        try {
            return backend.next(iteratorKey);
        } catch (IOException e) {
            throw new RemoteException("Failed to next() request on iterator '"
                                      + iteratorKey + "': " + e.getMessage(),
                                      e);
        }
    }

    @Override
    public List<Record> next(long iteratorKey, int maxRecords) throws RemoteException {
        try {
            return backend.next(iteratorKey, maxRecords);
        } catch (IOException e) {
            throw new RemoteException("Failed to next() request on iterator '"
                                      + iteratorKey + "' for " + maxRecords
                                      + " records: " + e.getMessage(),
                                      e);
        }
    }

    @Override
    public void flush(Record record) throws RemoteException {
        try {
            backend.flush(record);
        } catch (IOException e) {
            throw new RemoteException("Failed to flush " + record +": "
                                      + e.getMessage(), e);
        }
    }

    @Override
    public void flushAll(List<Record> records) throws RemoteException {
        try {
            backend.flushAll(records);
        } catch (IOException e) {
            throw new RemoteException("Failed to flush "
                                      + Logs.expand(records, 5) +": "
                                      + e.getMessage(), e);
        }
    }

    @Override
    public void close() throws RemoteException {
        try {
            RemoteHelper.unExportRemoteInterface (serviceName, registryPort);
        } catch (IOException e) {
            throw new RemoteException ("Failed to unexport RMI interface: "
                                       + e.getMessage (), e);
        }

        try {
            RemoteHelper.unExportMBean (this);
        } catch (IOException e) {
            // Don't bail out because of this...
            log.warn ("Failed to unexport MBean: " + e.getMessage (), e);
        }

        try {
            backend.close();
        } catch (IOException e) {
            throw new RemoteException("Failed to close backend: "
                                      + e.getMessage(), e);
        }
    }

    @Override
    public void clearBase(String base) throws RemoteException {
        try {
            backend.clearBase(base);
        } catch (IOException e) {
            throw new RemoteException("Failed to clear base '" + base +"': "
                                      + e.getMessage(), e);
        }
    }
}
