package dk.statsbiblioteket.summa.storage.rmi;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.rmi.RemoteStorage;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

/**
 * A {@link Storage} implementation capable of wrapping an underlying backend
 * {@code Storage} and expose it over RMI.
 * </p><p>
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke, te")
public class RMIStorageProxy extends UnicastRemoteObject
                             implements RemoteStorage {

    /**
     * The class used for the storage backend. If this is set it will be
     * written to {@link #CONF_CLASS} before submitting
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
                                                             H2Storage.class;

    /**
     * Configuration property specifying which port the registry used by
     * the Storage can be found on. Default value is
     * {@link #DEFAULT_REGISTRY_PORT}.
     */
    public static final String CONF_REGISTRY_PORT =
                                              "summa.storage.rmi.registry.port";

    /**
     * Default value for the {@link #CONF_REGISTRY_PORT} property.
     */
    public static final int DEFAULT_REGISTRY_PORT = 28000;

    /**
     * Configuration property specifying the service name of the Storage service.
     * Default is {@link #DEFAULT_SERVICE_NAME}.
     */
    public static final String CONF_SERVICE_NAME =
                                               "summa.storage.rmi.service.name";

    /**
     *
     */
    public static final String DEFAULT_SERVICE_NAME = "summa-storage";

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
            log.info (CONF_BACKEND + " not set, using " + DEFAULT_BACKEND
                      + " for backend");
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

        serviceName = conf.getString (CONF_SERVICE_NAME, DEFAULT_SERVICE_NAME);
        registryPort = conf.getInt(CONF_REGISTRY_PORT, DEFAULT_REGISTRY_PORT);

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
            log.warn(String.format(
                    "Service port not defined in %s. Falling back to anonymous "
                    + "port 0", Storage.CONF_SERVICE_PORT));
            return 0;
        }
    }

    /* Reader methods */
    @Override
    public long getRecordsModifiedAfter(
            long time, String base, QueryOptions options)
                                                        throws RemoteException {
        try {
            return backend.getRecordsModifiedAfter(time, base, options);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(
                    "getRecordsModifiedAfter(time=%d, base='%s', options=%s) "
                    + "for %d:%s",
                    time, base, options, registryPort, serviceName), t);
            return -1;
        }
    }

    @Override
    public long getModificationTime(String base) throws RemoteException {
        try {
            return backend.getModificationTime (base);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(
                    "getModificationTime(base='%s') for %d:%s",
                    base, registryPort, serviceName), t);
            return -1;
        }
    }

    @Override
    public List<Record> getRecords(List<String> ids, QueryOptions options)
                                                        throws RemoteException {
        try {
            return backend.getRecords(ids, options);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(
                    "getRecord(ids=%s, queryOptions=%s) for %d:%s",
                    Logs.expand(ids, 5), options,
                    registryPort, serviceName), t);
            return null;
        }
    }

    @Override
    public Record getRecord(String id, QueryOptions options)
                                                        throws RemoteException {
        try {
            return backend.getRecord(id, options);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(
                    "getRecord(id='%s', queryOptions=%s) for %d:%s",
                    id, options, registryPort, serviceName), t);
            return null;
        }
    }

    @Override
    public Record next(long iteratorKey) throws RemoteException {
        try {
            return backend.next(iteratorKey);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(
                    "next(iteratorKey=%d) for %d:%s",
                    iteratorKey, registryPort, serviceName), t);
            return null;
        }
    }

    @Override
    public List<Record> next(long iteratorKey, int maxRecords)
                                                        throws RemoteException {
        try {
            return backend.next(iteratorKey, maxRecords);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(
                    "next(iteratorKey=%d, maxRecords=%d) for %d:%s",
                    iteratorKey, maxRecords, registryPort, serviceName), t);
            return null;
        }
    }

    @Override
    public void flush(Record record, QueryOptions options)
                                                        throws RemoteException {
        try {
            backend.flush(record, options);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(
                    "flush(%s) for %d:%s",
                    record, registryPort, serviceName), t);
        }
    }

    @Override
    public void flush(Record record) throws RemoteException {
        flush(record, null);
    }

    @Override
    public void flushAll(List<Record> records, QueryOptions options)
                                                        throws RemoteException {
        try {
            backend.flushAll(records, options);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(
                    "flushAll(%s) for %d:%s",
                    Logs.expand(records, 5), registryPort, serviceName), t);
        }
    }

    @Override
    public void flushAll(List<Record> records) throws RemoteException {
        flushAll(records, null);
    }

    @Override
    public void close() throws RemoteException {
        try {
            RemoteHelper.unExportRemoteInterface (serviceName, registryPort);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(
                    "close().unExportRemoteInterface(serviceName='%s', "
                    + "registryPort=%d)", serviceName, registryPort), t);
        } finally {
            // If an exception was throws above, it was also logged, so we
            // accept that it might be eaten by an exception from the backend
            try {
                backend.close();
            } catch (Throwable t) {
                RemoteHelper.exitOnThrowable(log, String.format(
                        "close() for %d:%s", registryPort, serviceName), t);
            }

            try {
                RemoteHelper.unExportMBean(this);
            } catch (Throwable t) {
                RemoteHelper.exitOnThrowable(log, String.format(
                        "close().unExportMBean() for %d:%s",
                        registryPort, serviceName), t);
            }
        }
    }

    @Override
    public void clearBase(String base) throws RemoteException {
        final String CALL = String.format("clearBase(%s) for %d:%s",
                                          base, registryPort, serviceName);
        //noinspection DuplicateStringLiteralInspection
        log.debug(CALL + " called");
        try {
            backend.clearBase(base);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, CALL, t);
        }
    }

    @Override
    public String batchJob(String jobName, String base,
                           long minMtime, long maxMtime, QueryOptions options)
                                                            throws IOException {
        final String CALL = String.format(
                "batchJob(%s, %s) for %d:%s",
                jobName, base, registryPort, serviceName);
        //noinspection DuplicateStringLiteralInspection
        log.debug(CALL + " called");
        try {
            return backend.batchJob(jobName, base, minMtime, maxMtime, options);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, CALL, t);
            return "ERROR: " + t.getMessage();
        }
    }
}
