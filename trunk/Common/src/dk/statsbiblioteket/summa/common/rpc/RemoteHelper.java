package dk.statsbiblioteket.summa.common.rpc;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.lang.management.ManagementFactory;
import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Utility class to help export remote interfaces
 */
public class RemoteHelper {

    static private final Log log = LogFactory.getLog (RemoteHelper.class);

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

        if (System.getSecurityManager() == null) {
            log.info ("No security manager set. Using RMISecurityManager");
            System.setSecurityManager(new RMISecurityManager());
        }

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

        reg.rebind(serviceName, remote);        

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
            log.debug("Succesfully unexported service '" + serviceName + "'");
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
            log.warn("Failed to unregister JMX interface for "
                     + obj.getClass() + ". Continuing.", e);
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

}
