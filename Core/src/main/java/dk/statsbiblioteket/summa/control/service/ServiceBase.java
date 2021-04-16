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
package dk.statsbiblioteket.summa.control.service;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;
import dk.statsbiblioteket.summa.common.util.DeferredSystemExit;
import dk.statsbiblioteket.summa.common.util.Security;
import dk.statsbiblioteket.summa.control.api.BadConfigurationException;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Locale;

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
    private static final Log log = LogFactory.getLog(ServiceBase.class);

    private int registryPort;
    private String id;
    private int servicePort;

    public ServiceBase(Configuration conf) throws RemoteException {
        super (getServicePort(conf));

        /* Make sure that the codebase appears valid */
        String codeBase = System.getProperty ("java.rmi.server.codebase");
        if (codeBase != null) {
            log.debug ("Validating codebase: " + codeBase);
            String[] urls = codeBase.split(" ");
            try {
                RemoteHelper.testCodeBase(urls);
            } catch (RemoteHelper.InvalidCodeBaseException e) {
                throw new RemoteException ("Invalid codebase: "
                                           + e.getMessage(), e);
            }
        } else {
            log.debug ("Codebase not set");
        }

        id = System.getProperty(Service.CONF_SERVICE_ID);
        if (id == null) {
            id = conf.getString(Service.CONF_SERVICE_ID, null);
        }
        if (id == null) {
            throw new ConfigurationException(String.format(
                    Locale.ROOT, "The property '%s' was not present and no service id was specified in system " +
                                 "properties '%s'. id could not be determined",
                    Service.CONF_SERVICE_ID, Service.CONF_SERVICE_ID));
        }

        Security.checkSecurityManager();

        registryPort = conf.getInt(Service.CONF_REGISTRY_PORT, 27000);
        servicePort = conf.getInt(Service.CONF_SERVICE_PORT, 28003);
        log.info("ServiceBase constructor finished with registryPort "
                 + registryPort + ", servicePort " + servicePort
                 + " and id '" + id + "'");
    }

    @Override
    public Status getStatus() throws RemoteException {
        log.trace("getStatus called, returning '" + status + "'");
        return status;
    }

    @Override
    public String getId() throws RemoteException {
        log.trace("getID called, returning '" + id + "'");
        return id;
    }

    public String getClientId() {
        return System.getProperty(ClientConnection.CONF_CLIENT_ID);
    }

    /**
     * Read the port on which the service should expose its rmi service.
     *
     * This method is mainly here to be able to retrieve the service
     * port in the super() call in the constructor.
     *
     * @param conf the configuration from which to read {@link #CONF_SERVICE_PORT}
     * @return the port
     * @throws BadConfigurationException if {@link #CONF_SERVICE_PORT} cannot be read
     */
    protected static int getServicePort(Configuration conf) {
        try {
            return conf.getInt(Service.CONF_SERVICE_PORT);
        } catch (Exception e) {
            log.warn ("No service port specified in " + Service.CONF_SERVICE_PORT + ". "
                      + "Defaulting to anonymous service port");
            return 0;
        }
    }

    protected String getRMIAddress() {
        return "//localhost:" + registryPort + "/" + id;
    }

    /**
     * Expose the {@link Service} interface as a remote service over rmi,
     * as well as trying to export it as an MBean. If the MBean registration
     * fails it will be ignored.
     *
     * @throws RemoteException if there is an error exporting the
                               {@link Service} interface
     */
    protected void exportRemoteInterfaces() throws IOException {
        log.trace("exportRemoteInterfaces called");
        
        RemoteHelper.exportRemoteInterface(this, registryPort, id);

        try {
            RemoteHelper.exportMBean(this);
        } catch (Exception e) {
            String msg = "Failed to register MBean, going on without it. "
                         + "Error was: " + e.getMessage();
            if (log.isTraceEnabled()) {
                log.warn(msg, e);
            } else {
                log.warn(msg);
            }
        }
    }

    /**
     * Retract remote {@code Service} interface as well as MBean.
     * If the MBean unregistration fails a warning will be logged, but nothing
     * more.
     * <p></p>
     * Note that under normal operations services will not need to do this.
     * After a {@link #stop()} command the remote interfaces still needs to be
     * up.
     *
     * @throws IOException on communication errors with the RPC mechanism (rmi)
     */
    protected void unexportRemoteInterfaces() throws IOException {
        log.info("Retracting remote interfaces");

        RemoteHelper.unExportRemoteInterface(id, registryPort);

        try {
            RemoteHelper.unExportMBean(this);
        } catch (Exception e) {
            String msg = "Failed to unregister MBean. Going on anyway. "
                         + "Error was: " + e.getMessage();
            if (log.isTraceEnabled()) {
                log.warn (msg, e);
            } else {
                log.warn(msg);
            }
        }
    }

    /**
     * Set the status and log a message.
     * @param code the status code to set.
     * @param msg a message for the status.
     * @param level the logging level to log on.
     */
    protected void setStatus(Status.CODE code, String msg,
                             Logging.LogLevel level) {
        status = new Status(code, msg);
        Logging.log (this +" status: "+ status, log, level);
    }

    /**
     * Set the status and log a message.
     * @param code the status code to set.
     * @param msg a message for the status.
     * @param level the logging level to log on.
     * @param cause the cause of the change in status
     */
    protected void setStatus(Status.CODE code, String msg,
                             Logging.LogLevel level, Throwable cause) {
        status = new Status(code, msg);
        Logging.log(this +" status: "+ status, log, level, cause);
    }

    /**
     * Convenience method to set an idle status with a default message.
     * The status change will be logged on debug level.
     */
    protected void setStatusIdle() {
        log.trace("setStatusIdle called");
        setStatus (Status.CODE.idle, "ready", Logging.LogLevel.DEBUG);
    }

    /**
     * Convenience method to set the status to
     * {@link dk.statsbiblioteket.summa.control.api.Status.CODE#running} with a given message.
     * The status change will be logged on info level.
     * @param msg the message to set in the status.
     */
    protected void setStatusRunning(String msg) {
        log.trace("setStatusRunning called");
        setStatus (Status.CODE.running, msg, Logging.LogLevel.INFO);
    }

    public String toString() {
        return "[service:" + id + "@" + getRMIAddress() + "]";
    }

    @Override
    public void kill () throws RemoteException {
        log.info("Got kill command. Preparing for JVM shutdown");
        int exitCode = 0;
        try {
            if (status.getCode() != Status.CODE.stopped &&
                    status.getCode() != Status.CODE.stopping) {
                log.debug ("Stopping service before killing it");
                stop();
            } else {
                log.debug ("Service is already stopped or stopping. Commencing "
                           + "kill");
            }

            unexportRemoteInterfaces();

        } catch (Throwable t) {
            log.warn ("Caught error when shutting down. Commencing shutdown: "
                      + t.getMessage(), t);
            exitCode = 1;
        }

        log.warn ("Killed. The JVM is shutting down in "
                  + DeferredSystemExit.DEFAULT_DELAY/1000 + "s, with exit code "
                  + exitCode);
        new DeferredSystemExit(exitCode);
    }
}




