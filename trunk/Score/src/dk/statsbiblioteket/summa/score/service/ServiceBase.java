/* $Id: ServiceBase.java,v 1.5 2007/12/04 08:45:11 mke Exp $
 * $Revision: 1.5 $
 * $Date: 2007/12/04 08:45:11 $
 * $Author: mke $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.score.service;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.score.api.BadConfigurationException;
import dk.statsbiblioteket.summa.score.api.Service;
import dk.statsbiblioteket.summa.score.api.Status;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * <p>Helper class to ease implementation of Summa ClientManager {@link Service}s.
 * Deals with the basic RMI setup.</p>
 *
 * <p>All that is needed to implement a {@link Service} is to subclass
 * {@code ServiceBase} and implement the two missing methods of the Service
 * interface, {@link Service#start} and {@link Service#stop}.</p>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Some methods needs Javadoc")
public abstract class ServiceBase extends UnicastRemoteObject
                                  implements ServiceBaseMBean {

    /**
     *
     */
    protected Status status;

    /**
     *
     */
    protected Log log;

    private int registryPort;
    private String id;
    private int servicePort;

    public ServiceBase(Configuration conf) throws RemoteException {
        super (getServicePort(conf));

        log = LogFactory.getLog(ServiceBase.class);

        if (System.getSecurityManager() == null) {
            log.info ("No security manager found. Setting RMI security manager");
            System.setSecurityManager(new RMISecurityManager());
        }

        registryPort = conf.getInt(REGISTRY_PORT, 27000);
        servicePort = conf.getInt(SERVICE_PORT, 27003);
        id = System.getProperty(SERVICE_ID);
        
    }

    public Status getStatus() throws RemoteException {
        return status;
    }

    public String getId() throws RemoteException {
        return id;
    }

    /**
     * Read the port on which the service should expose its rmi service.
     *
     * This method is mainly here to be able to retrieve the service
     * port in the super() call in the constructor.
     *
     * @param conf the configuration from which to read {@link #SERVICE_PORT}
     * @return the port
     * @throws BadConfigurationException if {@link #SERVICE_PORT} cannot be read
     */
    protected static int getServicePort (Configuration conf) {
        try {
            return conf.getInt(SERVICE_PORT);
        } catch (Exception e) {
            throw new BadConfigurationException("Unable to read " + SERVICE_PORT
                                              + "from configuration", e);
        }
    }

    protected String getRMIAddress () {
        return "//localhost"+":"+registryPort+"/"+id;
    }

    /**
     * Expose the {@link Service} interface as a remote service over rmi.
     * @throws RemoteException if there is an error exporting the
                               {@link Service} interface
     */
    protected void exportRemoteInterfaces() throws RemoteException {
        Registry reg = null;

        try {
            reg = LocateRegistry.createRegistry(registryPort);
            log.debug("Created registry on port " + servicePort);
        } catch (RemoteException e) {
            reg = LocateRegistry.getRegistry("localhost", registryPort);
            log.debug ("Found registry localhost" + ":" + registryPort);
        }

        if (reg == null) {
            throw new RemoteException ("Failed to locate or create registry on "
                                     + "localhost:" + registryPort);
        }

        try {
            reg.rebind(id, this);
        } catch (RemoteException ee) {
            log.error("Failed to bind in registry", ee);
            throw ee;
        }

        log.info(this.getClass().getSimpleName()
                + " bound in registry on port: "+registryPort + " as '"
                 + id + "' on port " + servicePort);

        try {
            log.debug ("Registering at mbean server");
            MBeanServer mbserver = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName(this.getClass().getName()+ ":type=Service");
            mbserver.registerMBean(this, name);
            log.info ("Registered at mbean server as " + name);
        } catch (Exception e) {
            log.error ("Failed to expose JMX interface. Going on without it.", e);
        }
    }

    /**
     * Set the status and log a message
     * @param code the status code to set
     * @param msg a message for the status
     * @param level the logging level to log on
     */
    protected void setStatus (Status.CODE code, String msg, Logging.LogLevel level) {
        status = new Status(code, msg);
        Logging.log (this +" status: "+ status, log, level);
    }

    /**
     * Convenience method to set an idle status with a default message.
     * The status change will be logged on debug level.
     */
    protected void setStatusIdle () {
        setStatus (Status.CODE.idle, "ready", Logging.LogLevel.DEBUG);
    }

    /**
     * Convenience method to set the status to {@link Status.CODE#running}
     * with a given message. The status change will be logged on info level.
     * @param msg the message to set in the status
     */
    protected void setStatusRunning (String msg) {
        setStatus (Status.CODE.running, msg, Logging.LogLevel.INFO);
    }

    public String toString () {
        return "[service:"+id+"@"+getRMIAddress()+"]";
    }

}
