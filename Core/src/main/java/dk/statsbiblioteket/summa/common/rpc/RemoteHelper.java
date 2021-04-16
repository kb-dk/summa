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
package dk.statsbiblioteket.summa.common.rpc;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.util.DeferredSystemExit;
import dk.statsbiblioteket.summa.common.util.Security;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.Zips;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

/**
 * Utility class foor remote invocation.
 */
public class RemoteHelper {
    /**
     * Logger for this class.
     */
    static private final Log log = LogFactory.getLog(RemoteHelper.class);
    static private final RemoteHelperShutdownHook shutdownHook;

    // Create and install the shutdown hook to clear all non-freed services
    static {
        shutdownHook = new RemoteHelperShutdownHook();
        Thread hookThread = new Thread(shutdownHook, "RemoteHelperShutdownHook");
        Runtime.getRuntime().addShutdownHook(hookThread);
    }

    /**
     * Expose the an object as a remote service. Currently this implementation
     * only works for RMI and {@link UnicastRemoteObject}s, but that might be
     * extended in the future.
     *
     * @param obj          Object to bind
     * @param registryPort the port on which the registry should run. If no
     *                     registry is found here, one will be created
     * @param serviceName  the name of the service to export
     * @throws IOException if there is an error exporting the interface
     */
    public synchronized static void exportRemoteInterface(
            Object obj, int registryPort, String serviceName) throws IOException {
        log.debug("Preparing to export remote interfaces of " + obj + "as '" + serviceName + "' with registry on port "
                  + registryPort);

        Security.checkSecurityManager();

        UnicastRemoteObject remote = (UnicastRemoteObject) obj;
        Registry reg;

        try {
            reg = LocateRegistry.createRegistry(registryPort);
            log.debug("Created registry on port " + registryPort);
        } catch (RemoteException e) {
            reg = LocateRegistry.getRegistry("localhost", registryPort);
            log.debug("Found registry localhost:" + registryPort);
        }


        if (reg == null) {
            throw new RemoteException("Failed to locate or create registry on localhost:" + registryPort);
        }

        try {
            reg.rebind(serviceName, remote);
            shutdownHook.registerService(registryPort, serviceName);
        } catch (NullPointerException e) {
            throw new NullPointerException(String.format(
                    Locale.ROOT, "NullPointerException while calling rebind(%s, %s", serviceName, remote));
        }

        log.info(remote.getClass().getSimpleName() + " bound in registry on //localhost:" + registryPort + "/"
                 + serviceName + ". Total services in registry: " + Strings.join(reg.list()));
    }

    /**
     * Un export a remote interface.
     *
     * @param serviceName  The service that should be unexported.
     * @param registryPort The port of the service.
     * @throws IOException If error occur while unexporting service.
     */
    public synchronized static void unExportRemoteInterface(String serviceName, int registryPort) throws IOException {
        try {
            log.trace("Preparing to unexport '" + serviceName + "' with registry on port " + registryPort);
        } catch (NullPointerException e) {
            // Ignore as that was just a trace and we are shutting down
        }
        Registry reg;

        /* We should not try and create the registry when we want to
         * unregister a service. */

        reg = LocateRegistry.getRegistry("localhost", registryPort);

        try {
            reg.list();
        } catch (java.rmi.ConnectException e) {
            log.warn("Can not unbind service '" + serviceName + "'. No registry running on port " + registryPort);
            return;
        }

        try {
            log.debug("Found registry localhost:" + registryPort + " with services " + Strings.join(reg.list()));
        } catch (NullPointerException e) {
            // Ignore as that was just a debug and we are shutting down
        }

        try {
            reg.unbind(serviceName);
            shutdownHook.unregisterService(registryPort, serviceName);
        } catch (NotBoundException e) {
            log.warn(String.format(Locale.ROOT, "Service '%s' not bound in registry on port %d", serviceName, registryPort), e);
        } catch (Exception e) {
            log.warn(String.format(Locale.ROOT, "Unable to unbind service '%s' on port %d", serviceName, registryPort), e);
        }
        try {
            log.info("Unexported service '" + serviceName + "' on port " + registryPort);
        } catch (NullPointerException e) {
            // Quite a shame to ignore, but Operations strongly dislike system out and it is not an error
        }
    }

    /**
     * Export an object as a JMX MBean. Unregister the object with
     * {@link #unExportMBean(Object)}
     *
     * @param obj the object to expose as an MBean
     * @throws IOException on communication errors with the JMX subsystem
     */
    public synchronized static void exportMBean(Object obj) throws IOException {
        ObjectName name = null;

        try {
            log.debug("Registering " + obj.getClass().getName() + " at mbean server");

            MBeanServer mbserver = ManagementFactory.getPlatformMBeanServer();
            name = new ObjectName(obj.getClass().getName() + ":type=" + obj.getClass().getSimpleName());
            mbserver.registerMBean(obj, name);

            log.info("Registered " + obj.getClass().getName() + " at mbean server as " + name);
        } catch (Exception e) {
            throw new IOException("Failed to bind MBean '" + obj + "' with '" + name + "'", e);
        }
    }

    /**
     * Unexport an object that has been registered as a JMX MBean via
     * {@link #exportMBean(Object)}.
     *
     * @param obj the object to unregsiter
     * @throws IOException on communication errors with the JMX subsystem
     */
    public synchronized static void unExportMBean(Object obj) throws IOException {
        ObjectName name;

        try {
            log.debug("Unregistering " + obj.getClass().getName() + " at mbean server");

            MBeanServer mbserver = ManagementFactory.getPlatformMBeanServer();
            name = new ObjectName(obj.getClass().getName() + ":type=" + obj.getClass().getSimpleName());
            mbserver.unregisterMBean(name);
        } catch (Exception e) {
            String msg = "Failed to unregister JMX interface for " + obj.getClass() + ". Continuing.";
            if (log.isTraceEnabled()) {
                log.warn(msg, e);
            } else {
                log.warn(msg);
            }
        }
    }

    /**
     * Get the host name of the running JVM
     *
     * @return the host name, or "localhost" if encountering an
     *         {@link UnknownHostException} from the Java runtime
     */
    public static String getHostname() {
        try {
            java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
            return localMachine.getHostName();
        } catch (UnknownHostException e) {
            log.warn("Failed to get host name. Returning 'localhost'", e);
            return "localhost";
        }
    }


    /**
     * Throws a {@link InvalidCodeBaseException} if one or more of the
     * URIs listed in {@code uris} does not point at a valid {@code .jar}
     * file.
     *
     * @param uris an array of uris to test for jar file contents
     * @throws dk.statsbiblioteket.summa.common.rpc.RemoteHelper.InvalidCodeBaseException
     *          if the uris could not be resolved to proper codebases.
     */
    public static void testCodeBase(String[] uris) throws InvalidCodeBaseException {
        log.trace("testCodeBase() called");

        File tmpDir = new File(System.getProperty("java.io.tmpdir"), "summa-RH-resolutions");

        try {
            /* Just try and create the dir to make sure we have something
             * to delete */
            tmpDir.mkdirs();
            Files.delete(tmpDir);
        } catch (IOException e) {
            log.error("Failed to create or delete temporary dir. Can not resolve code path", e);
            return;
        }

        for (String uri : uris) {
            log.trace("Testing codepath for " + uri);

            tmpDir.mkdirs();

            /* Check that it is a .jar file */
            if (!uri.endsWith(".jar")) {
                throw new InvalidCodeBaseException("Non .jar-file in codepath: " + uri);
            }

            /* Check that it is a valid url */
            URL url;
            try {
                url = new URL(uri);
            } catch (MalformedURLException e) {
                log.warn("Malformed URL in codepath", e);
                throw new InvalidCodeBaseException(String.format(Locale.ROOT, "Malformed url: %s, error was: %s", uri,
                                                                 e.getMessage()));
            }

            /* Try to download the url */
            File jar;
            try {
                jar = Files.download(url, tmpDir, true);
            } catch (IOException e) {
                log.warn("Unable to retrieve url", e);
                throw new InvalidCodeBaseException(String.format(Locale.ROOT, "Unable to retrieve url %s: %s", url, e.getMessage()));
            }

            /* validate that the contens looks like a .jar */
            try {
                Zips.unzip(jar.getAbsolutePath(), tmpDir.getAbsolutePath(), true);

                File metaInf = new File(tmpDir, "META-INF");
                if (!metaInf.exists()) {
                    throw new InvalidCodeBaseException(String.format(Locale.ROOT,
                            "The .jar-file %s does not contain a META-INF directory", url));
                }

            } catch (IOException e) {
                throw new InvalidCodeBaseException(String.format(Locale.ROOT,
                        "Failed to extract %s. The .jar file is possibly corrupt", url));
            }

            /* OK, it looks like a real .jar file there. Go on */
            log.debug("Validated .jar-file: " + url);

            try {
                Files.delete(tmpDir);
            } catch (IOException e) {
                log.error("Failed to delete temporary dir. Can not resolve code path", e);
                return;
            }
        }

    }

    /**
     * Send fatal error messages to the log and <code>stderr</code>, schedule
     * a system exit in 5s, and throw a <code>RemoteException</code>.
     * <p/>
     * The code style for Summa dictates that we respect Errors. This means
     * alerting the world that the JVM is in unstable state and shutting down
     * when they are encountered.
     *
     * @param log     the log to report FATAL to.
     * @param message human-readable description of what occured when the Error
     *                was encountered.
     * @param e       the Error.
     * @throws java.rmi.RemoteException wrapper for the Error to alert remote callers.
     */
    private static void fatality(Log log, String message, Throwable e) throws RemoteException {
        message += ". The JVM will be shut down in 5 seconds. Likely error causes include OutOfMemory, StackOverflows, "
                   + "or other critical problems in the JVM. Please note that this is an unclean shutdown and that "
                   + "file-based locks on databases and similar might persist in the environment and need manual "
                   + "cleanup";
        Logging.fatal(log, "RemoteHelper.fatality", message, e);
        System.err.println(message);
        e.printStackTrace(System.err);
        new DeferredSystemExit(1);
        throw new RemoteException(message, e);
    }

    /**
     * If <code>t</code> is an <code>Error</code> send fatal error messages to
     * the log and <code>stderr</code>, schedule a system exit in 5s, and
     * throw a <code>RemoteException</code>.
     * <p/>
     * This method is intended as a Helper for general processing of Throwables.
     * This gives slightly worse stack traces, as the method handleThrowable
     * is inserted, but improves code readability tremendously.
     * </p><p>
     * This method always throws a RemoteException and shuts down the JVM in
     * case of Throwables.
     *
     * @param log  the log to use.
     * @param call the method, including parameters, that caused the Throwable.
     * @param t    the Throwable from the execution of the method.
     * @throws java.rmi.RemoteException thrown back with expanded info.
     */
    public static void exitOnThrowable(Log log, String call, Throwable t) throws RemoteException {
        if (t instanceof Exception) {
            String message = "Exception during " + call;
            log.warn(message, t);
            throw new RemoteException(message, t);
        } else {
            fatality(log, "Unhandled error during " + call, t);
        }
    }

    /**
     * If <code>t</code> is an <code>Error</code> send fatal error messages to
     * the log and <code>stderr</code>, schedule a system exit in 5s, and
     * throw a <code>RemoteException</code>.
     * <p/>
     * This method is intended as a Helper for general processing of Throwables.
     * This gives slightly worse stack traces, as the method handleThrowable
     * is inserted, but improves code readability tremendously.
     * </p><p>
     * This method always throws a RemoteException and shuts down the JVM in
     * case of Throwables.
     *
     * @param log              the log to use.
     * @param call             the method, including parameters, that caused the Throwable.
     * @param t                the Throwable from the execution of the method.
     * @param flattenException if true the received Throwable is flattened to a
     *                         string before being rethrown.
     * @throws java.rmi.RemoteException thrown back with expanded info.
     */
    public static void exitOnThrowable(Log log, String call, Throwable t, boolean flattenException) throws 
                                                                                                    RemoteException {
        if (t instanceof Exception) {
            String message = "Exception during " + call;
            log.warn(message, t);
            if (flattenException) {
                ByteArrayOutputStream out = new ByteArrayOutputStream(5000);
                PrintStream ps = new PrintStream(out);
                t.printStackTrace(ps);
                ps.flush();
                try {
                    throw new RemoteException(message + "\n" + out.toString("utf-8"), t);
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalArgumentException("Unable to perform toString with utf-8", e);
                }
            }
            throw new RemoteException(message, t);
        } else {
            fatality(log, "Unhandled error during " + call, t);
        }
    }

    /**
     * Exception thrown when trying to resolve a URI not pointing at a valid
     * {@code .jar}-file, by calling {@link RemoteHelper#testCodeBase}.
     */
    public static class InvalidCodeBaseException extends Exception {
        private static final long serialVersionUID = 786138469846L;

        public InvalidCodeBaseException(String msg) {
            super(msg);
        }
    }

    /**
     * Shutdown hook for the JVM to free all registered services when
     * it exits
     */
    private static class RemoteHelperShutdownHook implements Runnable {

        // Port -> List of service names
        private Map<Integer, List<String>> serviceRegistry;

        public RemoteHelperShutdownHook() {
            serviceRegistry = new HashMap<>();
        }

        public void registerService(int port, String name) {
            List<String> services = serviceRegistry.get(port);

            if (services == null) {
                services = new LinkedList<>();
                serviceRegistry.put(port, services);
            }

            services.add(name);
        }

        public void unregisterService(int port, String name) {
            List<String> services = serviceRegistry.get(port);

            if (services == null) {
                return;
            }

            services.remove(name);
        }

        @Override
        public void run() {
            // We actually don't do conccurent modifications of the
            // serviceRegistry map here
            safeLogInfo("Cleaning up all services on " + serviceRegistry.size() + " registry ports from shutdown hook");
            for (Map.Entry<Integer, List<String>> entry : serviceRegistry.entrySet()) {
                int registryPort = entry.getKey();
                if (entry.getValue().isEmpty()) {
                    safeLogInfo("No services registered under port " + registryPort);
                }
                // We clone the service name list to avoid
                // concurrent modifications
                for (String serviceName : new LinkedList<>(entry.getValue())) {
                    try {
                        safeLogInfo("Shutdown hook: Unexporting " + serviceName + " from port " + registryPort);
                        unExportRemoteInterface(serviceName, registryPort);
                    } catch (IOException e) {
                        safeLogWarn(String.format(Locale.ROOT,
                                "Failed to unexport remote interface '%s' on port %d", serviceName, registryPort));
                    }
                }
            }
        }
        private void safeLogInfo(String message) {
            try {
                log.info(message);
            } catch (NullPointerException e) {
                // The logger backend seems to have shut down. We ignore this as the whole JVM is shutting down
            }
        }
        @SuppressWarnings("UseOfSystemOutOrSystemErr")
        private void safeLogWarn(String message) {
            try {
                log.warn(message);
            } catch (NullPointerException e) {
                System.err.println("Unable to log: " + message);
            }
        }
    }
}
