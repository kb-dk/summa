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
package dk.statsbiblioteket.summa.index.rmi;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.index.IndexManipulator;
import dk.statsbiblioteket.summa.index.ManipulatorFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Helper class that exposes a {@link IndexManipulator} as an RMI interface.
 * The standard use case is to expose a {@link IndexControllerImpl} as a means
 * to remote control commits and consolidates of the index(es).
 */
public class RMIManipulatorProxy extends UnicastRemoteObject
                                 implements RemoteManipulator {
    private static final long serialVersionUID = 25681338871L;

    /**
     * The class used for the manipulator backend. If this is set it will be
     * written to {@link #CONF_MANIPULATOR_CLASS} before submitting
     * the configuration to {@link ManipulatorFactory#createManipulator}.
     * <p/>
     * If this property is not set the proxy will fall back to
     * {@link #DEFAULT_BACKEND}.
     */
    public static final String CONF_BACKEND = "summa.index.rmi.backend";

    /**
     * Default class for the manipulator backend implementation as defined
     * in the {@link #CONF_BACKEND} property.
     */
    public static final Class<? extends IndexManipulator> DEFAULT_BACKEND =
                                                      IndexControllerImpl.class;

    /**
     * Configuration property specifying which port the registry used by
     * the indexer can be found on. Default value is
     * {@link #DEFAULT_REGISTRY_PORT}.
     */
    public static final String CONF_REGISTRY_PORT =
                                              "summa.index.rmi.registry.port";

    /**
     * Default value for the {@link #CONF_REGISTRY_PORT} property.
     */
    public static final int DEFAULT_REGISTRY_PORT = 28000;

    /**
     * Configuration property specifying the service name of the indexer
     * service. Default is {@link #DEFAULT_SERVICE_NAME}.
     */
    public static final String CONF_SERVICE_NAME =
                                               "summa.index.rmi.service.name";

    /**
     * Default value for the {@link #CONF_SERVICE_NAME} property.
     */
    public static final String DEFAULT_SERVICE_NAME = "summa-indexer";

    /**
     * The port RMI communications should run over. The default value for this
     * property is {@link #DEFAULT_SERVICE_PORT}.
     */
    public static final String CONF_SERVICE_PORT =
                                                 "summa.index.rmi.servic.eport";

    /**
     * Default value for the {@link #CONF_SERVICE_PORT} property. Using port
     * 0 means that a random anonymous port will be used for communications.
     */
    public static final int DEFAULT_SERVICE_PORT = 0;

    private static final Log log = LogFactory.getLog(RMIManipulatorProxy.class);

    private IndexManipulator backend;
    private String serviceName;
    private int registryPort;

    public RMIManipulatorProxy (Configuration conf) throws IOException {
        super(getServicePort(conf));

        /* Create configuration for the backend, based on our own,
         * rewriting the class property if necessary */
        // FIXME: The below config should really be kept entirely in memory,
        //        but we can't use a memorybased config because of bug:
        //        https://gforge.statsbiblioteket.dk/tracker/index.php?func=detail&aid=1453&group_id=8&atid=109
        Configuration backendConf = new Configuration (new XStorage());
        backendConf.importConfiguration (conf);
        if (conf.valueExists (CONF_BACKEND)) {
            backendConf.set (CONF_MANIPULATOR_CLASS,
                             conf.getString (CONF_BACKEND));
        } else {
            log.info (CONF_BACKEND + " not set, using " + DEFAULT_BACKEND
                      + " for backend");
            backendConf.set (CONF_MANIPULATOR_CLASS, DEFAULT_BACKEND);
        }

        /* If the backend is set to be another RMIManipulatorProxy then avoid
         * infinite recursion */
        if (backendConf.valueExists (CONF_MANIPULATOR_CLASS)) {
            if (this.getClass().getName().equals(
                              backendConf.getString (CONF_MANIPULATOR_CLASS))) {

                throw new ConfigurationException("Nested RMIManipulatorProxy "
                                                 + "objects not allowed");
            }
        }

        if (log.isTraceEnabled ()) {
            log.trace ("Backend conf:\n" + backendConf.dumpString ());
        }

        log.trace ("Creating manipulator backend");
        backend = ManipulatorFactory.createManipulator(backendConf);
        log.trace ("Created manipulator: " + backend.getClass().getName());

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

    private static int getServicePort (Configuration conf) {
        try {
            return conf.getInt(CONF_SERVICE_PORT);
        } catch (NullPointerException e) {
            log.warn ("Service port not defined in "
                    + CONF_SERVICE_PORT + ". Falling back to "
                    + "anonymous port");
            return DEFAULT_SERVICE_PORT;
        }
    }

    @Override
    public void open(File indexRoot) throws RemoteException {
        try {
            backend.open(indexRoot);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(
                    "open(%s) for %d:%s",
                    indexRoot, registryPort, serviceName), t);
        }
    }

    @Override
    public void clear() throws RemoteException {
        try {
            backend.clear();
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(
                    "clear() for %d:%s", registryPort, serviceName), t);
        }
    }

    @Override
    public boolean update(Payload payload) throws RemoteException {
        try {
            return backend.update(payload);
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(
                    "update(%s) for %d:%s",
                    payload, registryPort, serviceName), t);
            return false; // exitOnThrowable always throws
        }
    }

    @Override
    public void commit() throws RemoteException {
        try {
            backend.commit();
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(
                    "commit() for %d:%s", registryPort, serviceName), t);
        }
    }

    @Override
    public void consolidate() throws RemoteException {
        try {
            backend.consolidate();
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(
                    "consolidate() for %d:%s", registryPort, serviceName), t);
        }
    }

    @Override
    public void close() throws RemoteException {
        try {
            backend.close();
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(
                    "close() for %d:%s", registryPort, serviceName), t);
        }
    }

    @Override
    public void orderChangedSinceLastCommit() throws RemoteException {
        try {
            backend.orderChangedSinceLastCommit();
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(
                    "orderChangedSinceLastCommit() for %d:%s",
                    registryPort, serviceName), t);
        }
    }

    @Override
    public boolean isOrderChangedSinceLastCommit() throws RemoteException {
        try {
            return backend.isOrderChangedSinceLastCommit();
        } catch (Throwable t) {
            RemoteHelper.exitOnThrowable(log, String.format(
                    "isOrderChangedSinceLastCommit() for %d:%s",
                    registryPort, serviceName), t);
            return true; // We bomb out in a few seconds, so the value is random
        }
    }
}

