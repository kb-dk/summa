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
package dk.statsbiblioteket.summa.storage.rmi;

import dk.statsbiblioteket.summa.common.Logging;
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
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Locale;

/**
 * A {@link Storage} implementation capable of wrapping an underlying backend
 * {@code Storage} and expose it over RMI.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke, te, hbk")
public class RMIStorageProxy extends UnicastRemoteObject implements RemoteStorage {
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 23485L;

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
     * Default class for the storage backend implementation.
     */
    public static final Class<? extends Storage> DEFAULT_BACKEND = H2Storage.class;

    /**
     * Configuration property specifying which port the registry used by
     * the Storage can be found on. Default value is
     * {@link #DEFAULT_REGISTRY_PORT}.
     */
    public static final String CONF_REGISTRY_PORT = "summa.storage.rmi.registry.port";

    /**
     * Default value for the {@link #CONF_REGISTRY_PORT} property.
     */
    public static final int DEFAULT_REGISTRY_PORT = 28000;

    /**
     * Configuration property specifying the service name of the Storage
     * service.
     * Default is {@link #DEFAULT_SERVICE_NAME}.
     */
    public static final String CONF_SERVICE_NAME = "summa.storage.rmi.service.name";
    /**
     * Default value for {@link RMIStorageProxy#CONF_SERVICE_NAME}.
     */
    public static final String DEFAULT_SERVICE_NAME = "summa-storage";

    /**
     * If true, all received exception trees are flattened to a single
     * RemoteException containing the printed stack trace.
     * </p><p>
     * Although the best setting is theoretically false, allowing external
     * clients to handle the exception by drilling down and analyzing, this
     * requires the caller to have all relevant Exception implementations.
     * For real world use, true is nearly always the right choice.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_FLATTEN_EXCEPTIONS = "rmi.exceptions.flatten";
    public static final boolean DEFAULT_FLATTEN_EXCEPTIONS = true;

    /**
     * Private logger instance.
     */
    private static Log log = LogFactory.getLog(RMIStorageProxy.class);
    /**
     * Storage backend.
     */
    private Storage backend;
    /**
     * RMI service name.
     */
    private String serviceName;
    /**
     * Registry port.
     */
    private int registryPort;
    /**
     * Log expand size.
     */
    private final int logExpand = 5;

    private final boolean flattenExceptions;

    private boolean mbeanExported = false;

    /**
     * Constructs a RMI Storage proxy with a given configuration.
     *
     * @param conf The configuration.
     * @throws IOException If error occur construction storage.
     */
    public RMIStorageProxy(Configuration conf) throws IOException {
        super(getServicePort(conf));
        log.info("Creating RMIStorageProxy from configuration only");
        flattenExceptions = conf.getBoolean(CONF_FLATTEN_EXCEPTIONS, DEFAULT_FLATTEN_EXCEPTIONS);

        /* Create configuration for the backend, based on our own,
         * rewriting the class property if necessary */
        // FIXME: The below config should really be kept entirely in memory,
        //        but we can't use a memory-based config because of bug:
        //https://gforge.statsbiblioteket.dk/tracker/index.php?func=detail&aid=1453&group_id=8&atid=109
        Configuration backendConf = new Configuration(new XStorage(false));
        backendConf.importConfiguration(conf);
        if (conf.valueExists(CONF_BACKEND)) {
            backendConf.set(CONF_CLASS, conf.getString(CONF_BACKEND));
        } else {
            log.info(CONF_BACKEND + " not set, using " + DEFAULT_BACKEND + " for backend");
            backendConf.set(CONF_CLASS, DEFAULT_BACKEND);
        }

        /* If the backend is set to be another RMIStorageProxy then avoid
         * infinite recursion by forcing it into a DerbyStorage. */
        if (backendConf.valueExists(CONF_CLASS)) {
            if (this.getClass().getName().equals(backendConf.getString(CONF_CLASS))) {
                log.warn("Backend set to RMIStorageProxy. Forcing backend class to " + DEFAULT_BACKEND.getName()
                         + " to avoid infinite recursion");
                backendConf.set(CONF_CLASS, DEFAULT_BACKEND.getName());
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("Backend conf:\n" + backendConf.dumpString());
        }

        log.trace("Creating storage backend");
        try {
            backend = StorageFactory.createStorage(backendConf);
        } catch (Exception e) {
            String error = "Error creating storage backend";
            Logging.fatal(log, "RMIStorageProxy", error, e);
            throw new ConfigurationException(error, e);
        }
        log.trace("Created storage: " + backend.getClass().getName());

        serviceName = conf.getString(CONF_SERVICE_NAME, DEFAULT_SERVICE_NAME);
        registryPort = conf.getInt(CONF_REGISTRY_PORT, DEFAULT_REGISTRY_PORT);

        RemoteHelper.exportRemoteInterface(this, registryPort, serviceName);
        try {
            RemoteHelper.exportMBean (this);
            mbeanExported = true;
        } catch (Exception e) {
            String msg = "Unable to export MBean for '" + this;
//            if (log.isTraceEnabled()) {
                //log.warn(msg, e);
//            } else {
                log.warn(msg);
  //          }
        }
/*        try {
            RemoteHelper.exportMBean(this);
        } catch (Exception e) {
            String msg = "Error exporting MBean of '" + this + "'. Going on without it: " + e.getMessage();
            if (log.isTraceEnabled()) {
                log.warn(msg, e);
            } else {
                log.warn(msg);
            }
        }*/
    }

    /**
     * Constructs a RMI Storage proxy with a given configuration, backed by the given Storage.
     * Note: This will ignore the {@link #CONF_BACKEND} parameter.
     * @param conf The configuration.
     * @param backend the backing Storage to expose over RMI.
     * @throws IOException if the wrapping failed.
     */
    public RMIStorageProxy(Configuration conf, Storage backend) throws IOException {
        super(getServicePort(conf));
        log.info("Creating RMIStorageProxy from configuration and backing Storage " + backend);
        this.backend = backend;
        flattenExceptions = conf.getBoolean(CONF_FLATTEN_EXCEPTIONS, DEFAULT_FLATTEN_EXCEPTIONS);

        serviceName = conf.getString(CONF_SERVICE_NAME, DEFAULT_SERVICE_NAME);
        registryPort = conf.getInt(CONF_REGISTRY_PORT, DEFAULT_REGISTRY_PORT);

        RemoteHelper.exportRemoteInterface(this, registryPort, serviceName);

        try {
            RemoteHelper.exportMBean(this);
        } catch (Exception e) {
            String msg = "Error exporting MBean of '" + this + "'. Going on without it: " + e.getMessage();
            if (log.isTraceEnabled()) {
                log.warn(msg, e);
            } else {
                log.warn(msg);
            }
        }

    }

    /**
     * Return this proxy's service port.
     *
     * @param configuration The configuration.
     * @return This proxy's services port.
     */
    private static int getServicePort(Configuration configuration) {
        try {
            return configuration.getInt(Storage.CONF_SERVICE_PORT);
        } catch (NullPointerException e) {
            log.warn(String.format(Locale.ROOT, "Service port not defined in %s. Falling back to anonymous port 0",
                                   Storage.CONF_SERVICE_PORT));
            return 0;
        }
    }

    /**
     * Return iterator key for records modified after input time, from the
     * backend storage.
     *
     * @param time    Time stamp records should be modified after.
     * @param base    The base to look in.
     * @param options The query options.
     * @return Iterator key for the result set.
     * @throws RemoteException If error occurred while doing RMI call.
     */
    @Override
    public long getRecordsModifiedAfter(long time, String base, QueryOptions options) throws RemoteException {
        log.debug("getRecordsModifiedAfter(" + time + ", '" + base + "', " + options + ")");

        if (log.isTraceEnabled()) {
            log.trace("getRecordsModifiedAfter(" + time + ", '" + base + "', " + options + ").");
        }
        try {
            return backend.getRecordsModifiedAfter(time, base, options);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(Locale.ROOT,
                    "getRecordsModifiedAfter(time=%d, base='%s', options=%s) for %d:%s",
                    time, base, options, registryPort, serviceName), t, flattenExceptions);
            return -1;
        }
    }


    /**
     * Return iterator key for records modified after input time, from the
     * backend storage.
     *
     * @param base The base to look in.
     * @return Iterator ey for the result set.
     * @throws RemoteException If error occurred while doing RMI call.
     */
    @Override
    public long getModificationTime(String base) throws RemoteException {
        try {
            return backend.getModificationTime(base);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(Locale.ROOT,
                    "getModificationTime(base='%s') for %d:%s",
                    base, registryPort, serviceName), t, flattenExceptions);
            return -1;
        }
    }

    /**
     * Return a list of records, given a list of id's and query options, from
     * the backend storage.
     *
     * @param ids     A list of string id's.
     * @param options The query options.
     * @return Return a list of records, given the id's and query options.
     * @throws RemoteException if error occurred doing RMI.
     */
    @Override
    public List<Record> getRecords(List<String> ids, QueryOptions options) throws RemoteException {
        try {
            return backend.getRecords(ids, options);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(Locale.ROOT,
                    "getRecord(ids=%s, queryOptions=%s) for %d:%s",
                    Logs.expand(ids, logExpand), options, registryPort, serviceName), t);
            return null;
        }
    }

    /**
     * Return a single Record based on the id and query options, from the
     * backend storage.
     *
     * @param id      A single id string.
     * @param options The query options.
     * @return Return a single record, given the id and query options.
     * @throws RemoteException If error occurred doing RMI.
     */
    @Override
    public Record getRecord(String id, QueryOptions options) throws RemoteException {
        try {
            return backend.getRecord(id, options);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(Locale.ROOT,
                    "getRecord(id='%s', queryOptions=%s) for %d:%s",
                    id, options, registryPort, serviceName), t, flattenExceptions);
            return null;
        }
    }

    /**
     * Return next record of the iterator based on the iterator key, from the
     * backend storage.
     *
     * @param iteratorKey The iterator key
     * @return Next record based on the iterator key.
     * @throws RemoteException If error occurred doing RMI.
     */
    @Override
    public Record next(long iteratorKey) throws RemoteException {
        try {
            return backend.next(iteratorKey);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(Locale.ROOT,
                    "next(iteratorKey=%d) for %d:%s",
                    iteratorKey, registryPort, serviceName), t, flattenExceptions);
            return null;
        }
    }

    /**
     * Return a list of record of maximum maxRecords, given the iterator key,
     * from the backend storage.
     *
     * @param iteratorKey The iterator key.
     * @param maxRecords  maximum number of records.
     * @return a list of records based on iterator key from backend storage.
     * @throws RemoteException If error occurred doing RMI.
     */
    @Override
    public List<Record> next(long iteratorKey, int maxRecords) throws RemoteException {
        try {
            return backend.next(iteratorKey, maxRecords);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(Locale.ROOT,
                    "next(iteratorKey=%d, maxRecords=%d) for %d:%s",
                    iteratorKey, maxRecords, registryPort, serviceName), t, flattenExceptions);
            return null;
        }
    }

    /**
     * Flush the record into backend storage, based on query options.
     *
     * @param record  The record to store or update.
     * @param options A set of arguments to modify how the record is inserted
     *                or updated.
     * @throws RemoteException If error occurred doing RMI.
     */
    @Override
    public void flush(Record record, QueryOptions options) throws RemoteException {
        try {
            backend.flush(record, options);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(
                    log, String.format(Locale.ROOT, "flush(%s) for %d:%s", record, registryPort, serviceName), t, flattenExceptions);
        }
    }

    /**
     * Flush the record into backend storage, with query options equal 'null'.
     *
     * @param record The record to store or update.
     * @throws RemoteException If error occurred doing RMI.
     */
    @Override
    public void flush(Record record) throws RemoteException {
        flush(record, null);
    }

    /**
     * Flush a list of records into backend storage, based on the query options.
     *
     * @param records a list of records to store or update. On duplicate ids
     *                only the last of the duplicated records are stored.
     * @param options A set of arguments to modify how records are inserted
     *                or updated.
     * @throws RemoteException If error occurred doing RMI.
     */
    @Override
    public void flushAll(List<Record> records, QueryOptions options) throws RemoteException {
        try {
            backend.flushAll(records, options);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(Locale.ROOT,
                    "flushAll(%s) for %d:%s",
                    Logs.expand(records, logExpand), registryPort, serviceName), t, flattenExceptions);
        }
    }

    /**
     * Flush a list of records into backend storage, with query options equal
     * to 'null'.
     *
     * @param records a list of records to store or update. On duplicate ids
     *                only the last of the duplicated records are stored.
     * @throws RemoteException If error occurred doing RMI.
     */
    @Override
    public void flushAll(List<Record> records) throws RemoteException {
        flushAll(records, null);
    }

    /**
     * Closes the RMI proxy storage.
     *
     * @throws RemoteException If error occurred doing RMI.
     */
    @Override
    public void close() throws RemoteException {
        try {
            log.info("Unexporting " + this);
            UnicastRemoteObject.unexportObject(this, true);
        } catch (NoSuchObjectException e) {
            log.warn("Attempted unexport of " + this + " but it was not registered", e);
        }
        try {
            RemoteHelper.unExportRemoteInterface(serviceName, registryPort);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(Locale.ROOT,
                    "close().unExportRemoteInterface(serviceName='%s', registryPort=%d)",
                    serviceName, registryPort), t, flattenExceptions);
        } finally {
            // If an exception was throws above, it was also logged, so we
            // accept that it might be eaten by an exception from the backend
            try {
                backend.close();
            } catch (Throwable t) {
                RemoteHelper.exitOnThrowable(log, String.format(Locale.ROOT,
                        "close() for %d:%s",
                        registryPort, serviceName), t, flattenExceptions);
            }

            try {
                if (mbeanExported) {
                    log.info("Closing down MBean for " + this);
                    RemoteHelper.unExportMBean(this);
                } else {
                    log.debug("Skipping unexport of MBean for " + this + " and binding failed");
                }
            } catch (Throwable t) {
                RemoteHelper.exitOnThrowable(log, String.format(Locale.ROOT, "close().unExportMBean() for %d:%s",
                                                                registryPort, serviceName), t, flattenExceptions);
            }
        }
    }

    /**
     * Clear the base in the underlaying backend storage.
     *
     * @param base the base to clear.
     * @throws RemoteException If error occurred doing RMI.
     */
    @Override
    public void clearBase(String base) throws RemoteException {
        final String call = String.format(Locale.ROOT, "clearBase(%s) for %d:%s", base, registryPort, serviceName);
        //noinspection DuplicateStringLiteralInspection
        log.debug(call + " called");
        try {
            backend.clearBase(base);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, call, t, flattenExceptions);
        }
    }

    /**
     * Run a batch job on the backend storage.
     *
     * @param jobName  The name of the job to instantiate.
     *                 The job name must match the regular expression
     *                 {@code [a-zA-z_-]+.job.[a-zA-z_-]+} and correspond to a
     *                 resource in the classpath of the storage process.
     *                 Fx {@code count.job.js}.
     * @param base     Restrict the batch jobs to records in this base. If
     *                 {@code base} is {@code null} the records from all bases will
     *                 be included in the batch job.
     * @param minMtime Only records with modification times strictly greater
     *                 than {@code minMtime} will be included in the batch job.
     * @param maxMtime Only records with modification times strictly less than
     *                 {@code maxMtime} will be included in the batch job.
     * @param options  Restrict to records for which
     *                 {@link QueryOptions#allowsRecord} returns true.
     * @return a result string.
     * @throws IOException if error occurred.
     */
    @Override
    public String batchJob(String jobName, String base, long minMtime, long maxMtime, QueryOptions options)
                                                                                                    throws IOException {
        final String call = String.format(Locale.ROOT, "batchJob(%s, %s) for %d:%s",
                                          jobName, base, registryPort, serviceName);
        //noinspection DuplicateStringLiteralInspection
        log.debug(call + " called");
        try {
            return backend.batchJob(jobName, base, minMtime, maxMtime, options);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, call, t, flattenExceptions);
            return "ERROR: " + t.getMessage();
        }
    }
}
