package dk.statsbiblioteket.summa.common.rpc;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.lang.management.ManagementFactory;
import java.io.IOException;
import java.io.File;
import java.net.UnknownHostException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Zips;
import dk.statsbiblioteket.summa.common.util.Security;

/**
 * Utility class to help export remote interfaces
 */
public class RemoteHelper {

    static private final Log log = LogFactory.getLog (RemoteHelper.class);
    static private final RemoteHelperShutdownHook shutdownHook;

    // Create and install the shutdown hook to clear all non-freed services
    static {
        shutdownHook = new RemoteHelperShutdownHook();
        Thread hookThread = new Thread(shutdownHook,
                                       "RemoteHelperShutdownHook");
        Runtime.getRuntime().addShutdownHook(hookThread);
    }

    /**
     * Expose the an object as a remote service. Currently this implementation
     * only works for RMI and {@link UnicastRemoteObject}s, but that might be
     * extended in the future.
     *
     * @param obj Object to bind
     * @param registryPort the port on which the registry should run. If no
     *                     registry is found here, one will be created
     * @param serviceName the name of the service to export
     * @throws IOException if there is an error exporting the interface
     */
    public synchronized static void exportRemoteInterface(Object obj,
                                              int registryPort,
                                              String serviceName)
                                                            throws IOException {
        log.trace ("Preparing to export remote interfaces of " + obj
                   + "as '" + serviceName + "' with registry on port "
                   + registryPort);

        Security.checkSecurityManager();

        UnicastRemoteObject remote = (UnicastRemoteObject) obj;
        Registry reg = null;

        try {
            reg = LocateRegistry.createRegistry(registryPort);
            log.debug("Created registry on port " + registryPort);
        } catch (RemoteException e) {
            reg = LocateRegistry.getRegistry("localhost", registryPort);
            log.debug ("Found registry localhost:" + registryPort);
        }


        if (reg == null) {
            throw new RemoteException ("Failed to locate or create registry on "
                                        + "localhost:" + registryPort);
        }

        try {
            reg.rebind(serviceName, remote);
            shutdownHook.registerService(registryPort, serviceName);
        } catch (NullPointerException e) {
            throw new NullPointerException(String.format(
                    "NullPointerException while calling rebind(%s, %s",
                    serviceName, remote));
        }

        log.info(remote.getClass().getSimpleName()
                + " bound in registry on //localhost:" + registryPort + "/"
                 + serviceName);
    }

    public synchronized static void unExportRemoteInterface (String serviceName,
                                                             int registryPort)
                                                            throws IOException {
        log.trace ("Preparing to unexport '" + serviceName + "' with registry on"
                   + " port " + registryPort);
        Registry reg = null;

        /* We should not try and create the registry when we want to
         * unregister a service. */

        reg = LocateRegistry.getRegistry("localhost", registryPort);
        log.debug ("Found registry localhost:" + registryPort);

        if (reg == null) {
            log.error ("Can not unbind service '" + serviceName + "'. No "
                       + "registry running on port " + registryPort);
            return;
        }

        try {
            reg.unbind(serviceName);
            shutdownHook.unregisterService(registryPort, serviceName);
            log.info("Unexported service '" + serviceName + "' on port "
                      + registryPort);
        } catch (NotBoundException e) {
            log.error ("Service '" + serviceName + "' not bound in registry on "
                       + "port " + registryPort);
        }
    }

    /**
     * Export an object as a JMX MBean. Unregister the object with
     * {@link #unExportMBean(Object)}
     *  
     * @param obj the object to expose as an MBean
     * @throws IOException on communication errors with the JMX subsystem
     */
    public synchronized static void exportMBean (Object obj)
                                                            throws IOException {
        ObjectName name = null;

        try {
            log.debug ("Registering " + obj.getClass().getName()
                       + " at mbean server");

            MBeanServer mbserver = ManagementFactory.getPlatformMBeanServer();
            name = new ObjectName(obj.getClass().getName()
                                  + ":type=" + obj.getClass().getSimpleName());
            mbserver.registerMBean(obj, name);

            log.info ("Registered " + obj.getClass().getName()
                      + " at mbean server as " + name);
        } catch (Exception e) {
            throw new IOException("Failed to bind MBean '" + obj + "' "
                                  + "with '" + name + "'", e);
        }
    }

    /**
     * Unexport an object that has been registered as a JMX MBean via
     * {@link #exportMBean(Object)}.
     *
     * @param obj the object to unregsiter
     * @throws IOException on communication errors with the JMX subsystem
     */
    public synchronized static void unExportMBean (Object obj)
                                                            throws IOException {
        ObjectName name = null;

        try {
            log.debug ("Unregistering " + obj.getClass().getName()
                       + " at mbean server");

            MBeanServer mbserver = ManagementFactory.getPlatformMBeanServer();
            name = new ObjectName(obj.getClass().getName()
                                  + ":type=" + obj.getClass().getSimpleName());
            mbserver.unregisterMBean(name);
        } catch (Exception e) {
            String msg = "Failed to unregister JMX interface for "
                         + obj.getClass() + ". Continuing.";
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
    public static String getHostname () {
        try {
            java.net.InetAddress localMachine =
                    java.net.InetAddress.getLocalHost();
            return localMachine.getHostName();
        } catch (UnknownHostException e) {
            log.error ("Failed to get host name. Returning 'localhost'", e);
            return "localhost";
        }
    }

    /**
     * Throws a {@link InvalidCodeBaseException} if one or more of the
     * URIs listed in {@code uris} does not point at a valid {@code .jar}
     * file.
     * @param uris an array of uris to test for jar file contents
     */
    public static void testCodeBase(String[] uris)
                                               throws InvalidCodeBaseException {
        log.trace ("testCodeBase() called");

        File tmpDir = new File (System.getProperty("java.io.tmpdir"),
                                "summa-RH-resolutions");

        try {
            /* Just try and create the dir to make sure we have something
             * to delete */
            tmpDir.mkdirs();
            Files.delete(tmpDir);
        } catch (IOException e) {
            log.error ("Failed to create or delete temporary dir. Can not "
                       + "resolve code path", e);
            return;
        }

        for (String uri : uris) {
            log.trace ("Testing codepath for " + uri);

            tmpDir.mkdirs();

            /* Check that it is a .jar file */
            if (!uri.endsWith(".jar")) {
                throw new InvalidCodeBaseException("Non .jar-file in codepath: "
                                                   + uri);
            }

            /* Check that it is a valid url */
            URL url;
            try {
                url = new URL (uri);
            } catch (MalformedURLException e) {
                log.warn("Malformed URL in codepath", e);
                throw new InvalidCodeBaseException("Malformed url: " + uri
                                                    + ", error was: "
                                                    + e.getMessage());
            }

            /* Try to download the url */
            File jar;
            try {
                jar = Files.download(url, tmpDir, true);
            } catch (IOException e) {
                log.warn ("Unable to retrieve url", e);
                throw new InvalidCodeBaseException("Unable to retrieve url "
                                                   + url + ": "
                                                   + e.getMessage());
            }

            /* validate that the contens looks like a .jar */
            try {
                Zips.unzip(jar.getAbsolutePath(), tmpDir.getAbsolutePath(),
                           true);

                File metaInf = new File (tmpDir, "META-INF");
                if (!metaInf.exists()) {
                    throw new InvalidCodeBaseException("The .jar-file "+ url
                                                       + " does not contain "
                                                       + "a META-INF directory");
                }

            } catch (IOException e) {
                throw new InvalidCodeBaseException("Failed to extract "
                                                     + url + ". The .jar file "
                                                     + "is possibly corrupt");
            }

            /* OK, it looks like a real .jar file there. Go on */
            log.debug ("Validated .jar-file: " + url);

            try {
                Files.delete(tmpDir);
            } catch (IOException e) {
                log.error ("Failed to delete temporary dir. Can not "
                           + "resolve code path", e);
                return;
            }
        }

    }

    /**
     * Exception thrown when trying to resolve a URI not pointing at a valid
     * {@code .jar}-file, by calling {@link RemoteHelper#testCodeBase}.
     */
    public static class InvalidCodeBaseException extends Exception {

        public InvalidCodeBaseException(String msg) {
            super (msg);
        }
    }

    /**
     * Shutdown hook for the JVM to free all registered services when
     * it exits
     */
    private static class RemoteHelperShutdownHook implements Runnable {

        // Port -> List of service names
        private Map<Integer,List<String>> serviceRegistry;

        public RemoteHelperShutdownHook() {
            serviceRegistry = new HashMap<Integer, List<String>>();
        }

        public void registerService(int port, String name) {
            List<String> services = serviceRegistry.get(port);

            if (services == null) {
                services = new LinkedList<String>();
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

        public void run() {
            // We actually don't do conccurent modifications of the
            // serviceRegistry map here
            for (Map.Entry<Integer,List<String>> entry :
                                            serviceRegistry.entrySet()) {
                int registryPort = entry.getKey();

                // We clone the service name list to avoid
                // concurrent modifications
                for (String serviceName :
                        new LinkedList<String>(entry.getValue())) {
                    try {
                        unExportRemoteInterface(serviceName,registryPort);
                    } catch (IOException e) {
                        log.error("Failed to unexport remote interface '"
                                  + serviceName + "' on port " + registryPort);
                    }
                }
            }
        }
    }
}



