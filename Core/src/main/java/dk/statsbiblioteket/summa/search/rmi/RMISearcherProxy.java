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
package dk.statsbiblioteket.summa.search.rmi;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;
import dk.statsbiblioteket.summa.search.SummaSearcherFactory;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.summa.search.api.QueryException;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.rmi.RemoteSearcher;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Locale;

/**
 * A {@link SummaSearcher} implementation exposing an RMI interface,
 * proxying all method calls to a backend {@code SummaSearcher}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT)
public class RMISearcherProxy extends UnicastRemoteObject implements RemoteSearcher {
    private static final long serialVersionUID = 488468553187L;
    private static final Log log = LogFactory.getLog (RMISearcherProxy.class);

    /**
     * The class used for the searcher backend. If this is set it will be
     * written to {@link SummaSearcher#CONF_CLASS} before submitting
     * the configuration to {@link SummaSearcherFactory#createSearcher}.
     */
    public static final String CONF_BACKEND = "summa.searcher.rmi.backend";

    /**
     * Configuration property defining on which port the RMI registry can be
     * found or may be started. Default is 28000
     */
    public static final String CONF_REGISTRY_PORT = "summa.searcher.rmi.registry.port";
    public static final int DEFAULT_REGISTRY_PORT = 28000;

    /**
     * Configuration property defining the name under which the searcher should
     * run. Default is 'summa-searcher'.
     */
    public static final String CONF_SERVICE_NAME = "summa.searcher.rmi.name";
    public static final String DEFAULT_SERVICE_NAME = "summa-searcher";

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

    public static final Class<? extends SummaSearcher> DEFAULT_BACKEND = SummaSearcherImpl.class;

    private SummaSearcher backend;
    private String serviceName;
    private int registryPort;
    private final boolean flattenExceptions;
    private boolean mbeanExported = false;

    /**
     * Create a new searcher proxy. The configuration passed in must specify
     * {@link SummaSearcher#CONF_SERVICE_PORT} for the RMI service port to use,
     * as well as either {@link SummaSearcher#CONF_CLASS} or
     * {@link RMISearcherProxy#CONF_BACKEND} to define what backend searcher to
     * use.
     * <p></p>
     * The whole configuration will be copied before submission to the backend
     * implementation. Furthermore the value {@link #CONF_BACKEND} property will
     * be written into the {@link #CONF_CLASS} property of this new
     * configuration, before passing it to a {@link SummaSearcherFactory}.
     * <p></p>
     * If the value of {@link #CONF_CLASS} is
     * {@code dk.statsbiblioteket.summa.control.rmi.RMISearcherProxy} then this
     * class will avoid infinite recursion by forcing this property into
     * a {@link SummaSearcherImpl}.
     * @param conf the configuration for the proxy.
     * @throws RemoteException if the proxy could not be created.
     */
    public RMISearcherProxy (Configuration conf) throws IOException {
        this(conf, createBackend(conf));
    }

    /**
     * Create a new searcher proxy, wrapping the given searcher.
     * The configuration passed in must specify
     * {@link SummaSearcher#CONF_SERVICE_PORT} for the RMI service port to use,
     * as well as either {@link SummaSearcher#CONF_CLASS} or
     * {@link RMISearcherProxy#CONF_BACKEND} to define what backend searcher to
     * use.
     * @param conf the configuration for the proxy.
     * @throws RemoteException if the proxy could not be created.
     */
    public RMISearcherProxy (Configuration conf, SummaSearcher backend) throws IOException {
        super (getServicePort(conf));
        flattenExceptions = conf.getBoolean(CONF_FLATTEN_EXCEPTIONS, DEFAULT_FLATTEN_EXCEPTIONS);
        this.backend = backend;

        serviceName = conf.getString(CONF_SERVICE_NAME, DEFAULT_SERVICE_NAME);
        registryPort = conf.getInt(CONF_REGISTRY_PORT, DEFAULT_REGISTRY_PORT);

        RemoteHelper.exportRemoteInterface (this, registryPort, serviceName);

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
        log.info("Created " + this);
    }

    private static SummaSearcher createBackend(Configuration conf) throws IOException {
        Configuration backendConf = new Configuration (new XStorage(false));
        backendConf.importConfiguration (conf);
        if (conf.valueExists (CONF_BACKEND)) {
            backendConf.set (CONF_CLASS, conf.getString (CONF_BACKEND));
        } else {
            log.info (CONF_BACKEND + " not set, using " + DEFAULT_BACKEND + " for backend");
            backendConf.set (CONF_CLASS, DEFAULT_BACKEND);
        }

        /* If the backend is set to be another RMISeacherProxy then avoid
         * infinite recursion by forcing it into a SummaSearcherImpl */
        if (backendConf.valueExists (CONF_CLASS)) {
            if (RMISearcherProxy.class.getClass().getName().equals(backendConf.getString (CONF_CLASS))) {
                log.warn ("Backend set to RMISearcherProxy. Forcing backend class to " + DEFAULT_BACKEND.getName()
                          + " to avoid infinite recursion");
                backendConf.set (CONF_CLASS, DEFAULT_BACKEND.getName());
            }
        }

        if (log.isTraceEnabled ()) {
            log.trace ("Backend conf:\n" + backendConf.dumpString ());
        }

        log.debug("Creating searcher backend");
        return SummaSearcherFactory.createSearcher(backendConf);
    }

  /**
   * Get service port from a given configuration. Helper method.
   *
   * @param conf the configuration to retrieve service port from.
   * @return the service port.
   */
    private static int getServicePort (Configuration conf) {
        try {
            return conf.getInt(CONF_SERVICE_PORT);
        } catch (NullPointerException e) {
            log.warn ("Service port property " + CONF_SERVICE_PORT + " not defined in configuration. Falling back to "
                      + "anonymous port");
            return 0;
        }
    }

    @Override
    public ResponseCollection search(Request request) throws RemoteException {
        try {
            return backend.search (request);
        } catch (QueryException e) {
            throw new QueryException("RMISearcherProxy", e);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(
                    Locale.ROOT, "search(%s) for %d:%s", request, registryPort, serviceName), t, flattenExceptions);
            return null;
        }
    }

    @Override
    public void close() throws RemoteException {
        try {
            log.info("Unexporting " + this);
            UnicastRemoteObject.unexportObject(this, true);
        } catch (NoSuchObjectException e) {
            log.warn("Attempted unexport of " + this + " but it was not registered", e);
        }
        try {
            RemoteHelper.unExportRemoteInterface (serviceName, registryPort);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(Locale.ROOT,
                    "close().unExportRemoteInterface(serviceName='%s', registryPort=%d)",
                    serviceName, registryPort), t, flattenExceptions);
        } finally {
            // If an exception was thrown above, it was also logged, so we
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
                RemoteHelper.exitOnThrowable(log, String.format(Locale.ROOT,
                        "close().unExportMBean() for %d:%s",
                        registryPort, serviceName), t, flattenExceptions);
            }
        }
    }

    @Override
    public String toString() {
        return "RMISearcherProxy(serviceName='" + serviceName + "', registryPort=" + registryPort +
               ", flattenExceptions=" + flattenExceptions + ", backend=" + backend + ')';
    }
}
