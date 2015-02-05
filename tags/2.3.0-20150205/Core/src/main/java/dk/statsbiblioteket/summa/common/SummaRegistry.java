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
package dk.statsbiblioteket.summa.common;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.LoggingExceptionHandler;
import dk.statsbiblioteket.summa.common.util.Security;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sets up an RMI registry, which should have access to all Summa JARs. If a registry is already running,
 * an exception will be thrown.
 * </p><p>
 * In a multi-headed setup, it is recommended to start this registry before things like Storages and Searchers,
 * so that they can be shut down and restarted without having to restart other services.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummaRegistry {
    private static Log log = LogFactory.getLog(SummaRegistry.class);

    /**
     * The port for the registry.
     * </p><p>
     * Mandatory.
     */
    public static final String CONF_PORT = "registry.port";

    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private Registry registry = null;
    private final int registryPort;

    public static void main (String[] args) {
        log.debug("main: Starting up SummaRegistry");
        new SummaRegistry(Configuration.getSystemConfiguration(true)).run();
    }

    public SummaRegistry(Configuration conf) {
        this(conf, true);
    }

    SummaRegistry(Configuration conf, boolean autoStart) {
        Security.checkSecurityManager();
        Thread.setDefaultUncaughtExceptionHandler(new LoggingExceptionHandler(log));

        if (!conf.containsKey(CONF_PORT)) {
            final String message = CONF_PORT + " must be specified";
            Logging.fatal(log, "SummaRegistry", message);
            throw new IllegalArgumentException(message);
        }
        registryPort = conf.getInt(CONF_PORT);
        if (autoStart) {
            run();
        } else {
            log.debug("Created SummaRegistry. Call run() to activate");
        }
    }

    /**
     * Starts the registry and keeps it running until {@link #shutdown} has been called.
     */
    public void run() {
        if (registry != null) {
            throw new IllegalArgumentException("Registry already running");
        }
        try {
            registry = LocateRegistry.createRegistry(registryPort);
        } catch (RemoteException e) {
            final String message = "A registry on port " + registryPort + " was already present";
            Logging.fatal(log, "SummaRegistry", message, e);
            throw new IllegalStateException(message, e);
        }

        log.info("Created registry on port " + registryPort + ". All OK.");

        // Block indefinitely (non-busy)
        while (!shutdown.get()) {
            try {
                synchronized (shutdown) {
                    shutdown.wait();
                }
            } catch (Exception e) {
                if (!shutdown.get()) {
                    log.error("Unexpected exception while waiting for shutdown. Continuing waiting");
                }
            }
        }
    }

    public void shutdown() {
        synchronized (shutdown) {
            this.shutdown.set(true);
            this.shutdown.notify();
        }
    }

    public Registry getRegistry() {
        return registry;
    }

    public int getRegistryPort() {
        return registryPort;
    }

}