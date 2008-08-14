/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.control.client;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;
import dk.statsbiblioteket.summa.common.configuration.Configurable.ConfigurationException;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.control.api.*;
import dk.statsbiblioteket.summa.control.bundle.BundleLoader;
import dk.statsbiblioteket.summa.control.bundle.BundleLoadingException;
import dk.statsbiblioteket.summa.control.bundle.BundleRepository;
import dk.statsbiblioteket.summa.control.bundle.BundleStub;
import dk.statsbiblioteket.summa.control.bundle.BundleSpecBuilder;
import dk.statsbiblioteket.summa.control.bundle.URLRepository;
import dk.statsbiblioteket.util.*;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.net.MalformedURLException;

/**
 * <p>Core class for running ClientManager clients.</p>
 *
 * <p>The client talks to the ClientManager server via a {@link ControlConnection}.
 * Itself exposes a {@link ClientConnection} over RMI.</p>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="The class and some methods needs Javadoc")
public class Client extends UnicastRemoteObject implements ClientMBean {
    private static Log log = LogFactory.getLog(Client.class);

    /** Extension to use for old packages, used for rollback purposes.
     * Package ids are not allowed to end with this reserved string. */
    public static final String OLD_PKG_EXTENSION = ".old";

    /** <p>Integer property defining the timeout when waiting for services
     * to come up. The value is in seconds.</p>
     * <p><b>Important:</b> The service will be monitored synchronously
     * so don't set this value to high. 5-10 ought to do it. 30 should be
     * absolute max.</p>
     */
    public static final String SERVICE_TIMEOUT =
            "summa.control.client.serviceTimeout";

    private Status status;
    private BundleRepository repository;
    private BundleLoader loader;
    private ServiceManager serviceMan;
    private String id;
    private String hostname;
    private String basePath;
    private String tmpPath;
    private String servicePath;
    private String artifactPath; // Removed service packages
    private String persistentPath;
    private int serviceTimeout;


    // RMI-related data
    private String registryHost, clientId;
    private int registryPort, clientPort;

    /**
     * Create a client from a {@link Configuration} and expose it over rmi.
     * The various RMI properties needed are read from the {@code Configuraion}
     * as defined in {@link ClientConnection}.
     * @param conf the conf from which to extract rmi  properties
     * @throws RemoteException if there is an error exposing the rmi service
     */
    public Client(Configuration conf) throws IOException {
        super (getServicePort (conf));
        log.debug("Constructing client");
        log.trace("Home dir: " + new File(".").getAbsolutePath());

        if (System.getSecurityManager() == null) {
            log.info ("No security manager found. "
                      + "Setting RMI security manager");
            System.setSecurityManager(new RMISecurityManager());
        } else {
            log.info("SecurityManager '" + System.getSecurityManager()
                     + "' present");
        }

        registryHost = conf.getString(REGISTRY_HOST_PROPERTY,
                                               "localhost");
        registryPort = conf.getInt(REGISTRY_PORT_PROPERTY, 27000);
        clientId = System.getProperty(CLIENT_ID);
        clientPort = conf.getInt(CLIENT_PORT_PROPERTY, 27002);
        id = clientId;

        if (clientId == null) {
            throw new BadConfigurationException("System property '" + CLIENT_ID
                                                + "' not set");
        }

        basePath = System.getProperty("user.home") + File.separator
                                     + conf.getString(
                                      CLIENT_BASEPATH_PROPERTY, "summa-control")
                                     + File.separator + clientId;
        log.debug ("Client '" + id + "' using basePath '" + basePath + "'");

        tmpPath = basePath + File.separator + "tmp";
        servicePath = basePath + File.separator + "services";
        artifactPath = basePath + File.separator + "artifacts";
        persistentPath = new File(basePath + File.separator
                                  + ".." + File.separator
                                  + "persistent").getCanonicalPath();

        /* Create repository */
        Class<? extends BundleRepository> repositoryClass =
                                    conf.getClass(
                                            REPOSITORY_CLASS_PROPERTY,
                                            BundleRepository.class,
                                            URLRepository.class);
        repository = Configuration.create(repositoryClass, conf);

        /* Create bundle loader */
        loader = Configuration.create(BundleLoader.class, conf);

        validateConfiguration();

        /* Service related setup */
        serviceTimeout = conf.getInt(SERVICE_TIMEOUT, 5);
        serviceMan = new ServiceManager(conf);

        /* Find client hostname */
        hostname = RemoteHelper.getHostname();
        log.debug ("Found hostname: '" + hostname + "'");

        setStatus(Status.CODE.constructed,
                  "Setting up remote interfaces (rmi,jmx)",
                  Logging.LogLevel.DEBUG);

        RemoteHelper.exportRemoteInterface(this, registryPort, clientId);
        RemoteHelper.exportMBean(this);

        setStatus(Status.CODE.constructed, "Remote interfaces up",
                  Logging.LogLevel.DEBUG);

        // Make sure all needed directories are created
        String[] dirs = new String[]{tmpPath, servicePath, artifactPath,
                                     persistentPath};
        for (String dir: dirs) {
            File dirFile = new File(dir);
            dirFile.mkdirs();
            if (!dirFile.exists()) {
                throw new IOException("Could not create directory '"
                                      + dirFile.getAbsoluteFile() + "'");
            }
        }
    }

    /**
     * Check that the states set by the configuration are good.
     * Throw an BadConfigurationException if something is ascrew.
     * @throws dk.statsbiblioteket.summa.control.api.BadConfigurationException if
     *         the configuration provided to the client is insufficient
     */
    private void validateConfiguration() throws BadConfigurationException {
        if (registryHost.equals("")) {
            throw new BadConfigurationException (this + ", "
                                                 + REGISTRY_HOST_PROPERTY
                                                 + " is empty");
        } else if (registryPort < 0) {
            throw new BadConfigurationException (this + ", "
                                                 + REGISTRY_PORT_PROPERTY
                                                + " < 0. Value "
                                                + registryPort);
        } else if (clientId.equals("")) {
            throw new BadConfigurationException (this + ", " + CLIENT_ID
                                                 + " is empty");
        } else if (clientPort < 0) {
            throw new BadConfigurationException (this + ", " + clientPort
                                                + " < 0. Value " + clientPort);
        } else if (id.equals("")) {
            throw new BadConfigurationException (this + ", " + CLIENT_ID
                                                 + " is empty");
        }  else if (basePath.equals("")) {
            throw new BadConfigurationException (this + ", "
                                                 + CLIENT_BASEPATH_PROPERTY
                                                 + " is empty");
        }
    }

    private String getRMIAddress () {
        return "//"+registryHost+":"+registryPort+"/"+ clientId;
    }

    /**
     * Read the port on which the client should expose its rmi service.
     *
     * This method is mainly here to be able to retrieve the service
     * port in the super() call in the constructor.
     *
     * @param conf the configuration from which to read {@link #CLIENT_PORT_PROPERTY}
     * @return the port
     * @throws ConfigurationException if {@link # CLIENT_PORT_PROPERTY} cannot be read
     */
    private static int getServicePort (Configuration conf) {
        try {
            return conf.getInt(CLIENT_PORT_PROPERTY);
        } catch (Exception e) {
            log.fatal("Unable to read " + CLIENT_PORT_PROPERTY
                      + "from configuration", e);
            throw new ConfigurationException("Unable to read "
                                             + CLIENT_PORT_PROPERTY
                                             + "from configuration", e);
        }

    }

    public void stop() {
        ConnectionContext<Service> connCtx = null;

        setStatus(Status.CODE.stopping, "Stopping all services",
                  Logging.LogLevel.INFO);
        for (String serviceId : serviceMan) {
            try {
                log.trace("Trying to stop service " + serviceId);
                connCtx = serviceMan.get(serviceId);
                connCtx.getConnection().stop();
                log.debug("Service " + serviceId + " was stopped");
            } catch (Exception e) {
                log.error("Could not stop service " + serviceId);
            } finally {
                if (connCtx != null) {
                    connCtx.unref();
                }
            }
        }
        setStatus(Status.CODE.stopped, "All services down. Stopping",
                Logging.LogLevel.INFO);
        System.exit(0);
    }

    public Status getStatus() {
        if (log.isTraceEnabled()) {
            log.trace("Getting status for client");
        }
        return status;
    }

    public String deployService(String bundleId,
                                String instanceId,
                                String configLocation) {
        setStatusRunning("Deploying service '" + bundleId + "' with config "
                        + configLocation + ", and instanceId '"
                        + instanceId + "'");
        File tmpBundleFile;

        try {
            tmpBundleFile = repository.get(bundleId);
        } catch (IOException e) {
            setStatusIdle ();
            throw new BundleLoadingException ("Failed to retrieve " + bundleId
                                            + "from repository", e);
        }

        deployServiceFromLocalFile(instanceId, tmpBundleFile, configLocation);
        
        setStatusIdle();
        return instanceId;
    }

    /**
     * <p>If this call completes it is guaranteed that
     * {@link ServiceManager#getServiceDir} returns an existing bundle file.</p>
     *
     * <p>The local file is unpacked to {@code servicePath/bundleId}</p>
     *
     * @param instanceId the instanceId under which to deploy the service
     * @param localFile the file to deploy
     * @param configLocation location for configuration, either an URL,
     *                       rmi address, or file path
     * @return the instance id of the deployed service or null on error
     */
    public void deployServiceFromLocalFile(String instanceId, File localFile,
                                           String configLocation) {

        if (servicePath.equals(localFile.getParent())) {
            throw new BundleLoadingException ("Trying to deploy " + localFile
                                              + " which is already"
                                              + " in the service directory "
                                              + servicePath +"."
                                              + " Aborting deploy.");
        } else if (!localFile.exists()) {
            throw new BundleLoadingException ("Trying to deploy non-existing"
                                              + " file " + localFile  + ", "
                                              + "aborting deploy.");
        }

        setStatusRunning ("Deploying '" + instanceId + "' from " + localFile);

        File tmpPkg = new File(tmpPath, instanceId);

        // Assert that we don't have collisions in the tmp dir
        if (tmpPkg.exists()) {
            try {
                log.debug ("Deleting temporary file '" + tmpPkg + "' to avoid"
                         + " collisions");
                Files.delete (tmpPkg);
            } catch (IOException e) {
                throw new BundleLoadingException("Failed to delete temporary "
                                                 + "file '" + tmpPkg + "'"
                                                 + "' blocking the way. "
                                                 + "Bailing out on deploy.", e);
            }
        }

        // Unzip the file into the tmp directory, and set the instance id
        // in the spec file
        try {
            Zips.unzip (localFile.toString(), tmpPkg.toString(), false);
            File specFile = new File(tmpPkg, "service.xml");
            BundleSpecBuilder builder = BundleSpecBuilder.open (specFile);
            builder.setInstanceId(instanceId);
            builder.write (specFile);

        } catch (IOException e) {
            try {
                Files.delete(tmpPkg);
                throw new BundleLoadingException ("Error deploying "
                                                  + localFile + ". "
                                                  + "Purged " + tmpPkg
                                                  + " from tmp dir", e);
            } catch (IOException ee) {
                log.error ("Failed to clean up after buggy deploy", ee);
                throw new BundleLoadingException ("Error deleting file '"
                                                  + tmpPkg + "' when cleaning "
                                                  + "up buggy deploy", e);
            }
        }

        // Check if the service is already deployed, ie if there already
        // is a service with the same instanceId
        File pkgFile = serviceMan.getServiceDir(instanceId);
        if (pkgFile.exists()) {
            reDeployService(instanceId, localFile);
        }

        // Move service bundle in place in services/<instanceid>
        log.trace("Moving '" + tmpPkg + "' to '" + pkgFile + "'");
        tmpPkg.renameTo(pkgFile);

        // FIXME: There is a race condition here, where the JMX files are
        //        readable after unpacking, but before we set read-only
        //        permissions
        log.debug ("Setting file permissions for service " + instanceId);
        checkPermissions(instanceId);
        
        setStatusIdle();
    }

    public void startService(String instanceId, String configLocation)
                                                        throws RemoteException {
        if (!serviceMan.knows(instanceId)) {
            throw new NoSuchServiceException(this, instanceId, "startService");
        }

        ConnectionContext<Service> connCtx = null;

        setStatusRunning ("Starting service " + instanceId);
        File serviceFile = serviceMan.getServiceDir(instanceId);
        BundleStub stub;

        connCtx = serviceMan.get(instanceId);

        /* If the service is running but start() has not been called
         * on it, just call Service.start() */
        if (connCtx != null) {
            Service service = connCtx.getConnection();
            connCtx.unref();
            if (service.getStatus().getCode() == Status.CODE.stopped) {
                log.debug("Calling start() on stopped service '" + instanceId
                          + "'");
                service.start();
                log.debug("Stopped service started");
            } else {
                log.warn("Trying to start service '" + instanceId
                        + "', but it is already running. Ignoring request.");

                throw new InvalidServiceStateException(this, instanceId,
                                                       "start",
                                                        "Already running");
            }
            setStatusIdle();
            return;
        }

        try {
            log.debug("Calling load for serviceFile '" + serviceFile + "'");
            stub = loader.load (serviceFile);
        } catch (IOException e) {
            setStatusIdle();
            throw new ServicePackageException (this, instanceId,
                                              "Error loading service '"
                                              + instanceId
                                              + "', from file " + serviceFile,
                                              e);
        }

        stub.addSystemProperty(CLIENT_PERSISTENT_DIR_PROPERTY, persistentPath);
        stub.addSystemProperty(CLIENT_ID, id);
        stub.addSystemProperty(Service.SERVICE_ID, instanceId);
        stub.addSystemProperty(Service.SERVICE_BASEPATH,
                               serviceFile.getParent());
        stub.addSystemProperty("summa.configuration", configLocation);


        connCtx = serviceMan.get (instanceId);
        if (connCtx != null) {
        try {
            Service service = connCtx.getConnection();

            if (log.isDebugEnabled()) {
                log.debug ("Launching '" + instanceId + "' with command line:\n"
                           + Strings.join(stub.buildCommandLine(), " "));
            }
            final Process p = stub.start();
            log.trace("startService: Process started");
            // Flush output stream
            new Thread (new Runnable () {

                public void run() {
                    log.info("Flushing output of child " + p);
                    try {
                        Streams.pipe(p.getInputStream(), System.out);
                        Streams.pipe(p.getErrorStream(), System.err);
                        log.info("Waiting for process");
                        p.waitFor();
                    } catch (Exception e) {
                        log.error ("Error flushing subprocess pipe", e);
                    }
                    log.info ("Child process exited with " + p.exitValue());
                }

            }).start();

            // FIXME: Should care about the returned Process?

            log.trace("Registering service");
            registerService (stub, configLocation);

            log.debug("Calling start() on service '" + instanceId +"'");
            service.start();
            log.debug("Start called without errors");

        } catch (IOException e) {
            log.error ("Failed to start service '" + instanceId
                       + "' with command line:\n"
                       + Logs.expand(stub.buildCommandLine(), 100), e);
        } finally {
            connCtx.unref();
        }
        } else {
            log.warn ("Failed to connect to service '" + instanceId + "'");
        }

        setStatusIdle();
    }

    private void registerService(BundleStub stub, String configLocation) {
        log.debug ("Registering service '" + stub.getInstanceId() + "'");

        String instanceId = stub.getInstanceId();

        if (serviceMan.knows(instanceId)) {
            log.warn ("Trying to register service '" + instanceId + "', but it"
                    + " is already registered. Ignoring request.");
            return;
        }

        // If this is a relative path, expand it to the absolute path
        // so we can load it as a file
        if (!configLocation.startsWith("/") &&
            !configLocation.contains("://")) {

            File configFile = stub.findResource(configLocation);
            if (configFile == null) {
                log.error ("Failed to find config file '" + configLocation
                           + "' in service '" + instanceId + "'s"
                           + " classpath. Failed registration.\n"
                           + "Bundle dir was: " + stub.getBundleDir() + "\n"
                           + "Bundle classpath was: "
                           + Strings.join(stub.getClassPath(), ":"));
                return;
            }
            configLocation = configFile.getAbsolutePath();
        }

        log.trace ("Absolute service config location: " + configLocation);

        Configuration serviceConf = Configuration.load(configLocation);
        int registryPort = serviceConf.getInt(Service.REGISTRY_PORT);
        String serviceName = stub.getInstanceId();
        String serviceUrl = "//localhost:" + registryPort + "/" + serviceName;

        log.trace ("Pinging service '" + instanceId +"' at '"
                   + serviceUrl + "'");
        Service service = null;
        Status status = null;
        for (int tick = 0; tick < serviceTimeout; tick++) {
            try {
                service = (Service) Naming.lookup (serviceUrl);
                status = service.getStatus();
            } catch (NotBoundException e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    log.warn ("Interrupted while waiting for service '"
                              + stub.getInstanceId() + "' to come up.");
                    break;
                }
                // keep waiting on interface
                continue;
            } catch (MalformedURLException e) {
                log.error ("Malformed URL for service '" + instanceId
                           + "'. Not registering", e);
            } catch (RemoteException e) {
                log.error ("Error connecting to '" + instanceId
                           + "'. Not registering", e);
            }
        }
        if (service == null){
            log.error ("Service '" + instanceId + "' on '" + serviceUrl
                    + "' never came up. It probably crashed.");
        } else {
            log.info ("Service '" + instanceId
                      + "' registered. Status was " + status);
            serviceMan.register(instanceId);
        }

    }

    public void stopService(String id) throws RemoteException {
        setStatusRunning ("Stopping service " + id);

        if (!serviceMan.knows(id)) {
            log.warn ("Can not stop service '" + id + "'. Service not "
                      + "known");
            throw new NoSuchServiceException(this, id, "stopService");
        }

        ConnectionContext<Service> connCtx = null;
        connCtx = serviceMan.get (id);

        if (connCtx == null) {
            if (serviceMan.getServiceDir(id).exists()) {
                log.error ("Cannot stop service. Service '" + id
                           + "' not running");
                throw new InvalidServiceStateException(this, id, "stopService",
                        "Not running");
            }
        } else {
            log.trace ("Calling stop() method on service '" + id + "'");
            boolean serviceIsDead = false;
            try {
                Service s = connCtx.getConnection();
                s.stop();


                // Wait for service to die
                for (int tick = 0; tick < serviceTimeout; tick++) {
                    try {
                        Thread.sleep(1000);
                        Status status = s.getStatus();
                        log.debug("Waiting for '" + id + "' to die. Service"
                                  + " status " + status);
                        if (Status.CODE.stopped == status.getCode()) {
                            // The service is stopped, but the RMI
                            // connection is still alive. Keep the
                            // connection around
                            setStatusIdle ();
                            return;
                        }
                    } catch (InterruptedException e) {
                        // Stop waiting for service to die
                        break;
                    } catch (RemoteException e) {
                        log.info("Service ping to '" + id + "'failed. It is"
                                 + " probably down.");
                        serviceIsDead = true;
                    }
                }
            } finally {
                connCtx.unref();
            }

            if (!serviceIsDead) {
                throw new InvalidServiceStateException(this, id, "stop",
                                                       "Service should be dead,"
                                                       + " but is still"
                                                       + " responding");
            }

        }

        setStatusIdle ();
    }

    public Status getServiceStatus(String id) throws RemoteException {
        log.trace("Getting service status for " + id);

        if (id == null) {
            throw new NullPointerException("id is null");
        }

        if (!serviceMan.knows(id)) {
            throw new NoSuchServiceException(this, id, "getStatus");
        }

        ConnectionContext<Service> connCtx = serviceMan.get (id);

        if (connCtx == null) {
            throw new InvalidServiceStateException(this, id,
                                                   "getStatus", "not running");
        } else {
            Service s = connCtx.getConnection();
            try {
                Status status = s.getStatus();
                return status;
            } finally {
                connCtx.unref();
            }
        }
    }

    public List<String> getServices() {
        log.trace("Getting list of services");

        List<String> serviceList = serviceMan.getServices();

        log.trace("Found services: "
                  + Logs.expand(serviceList, serviceList.size()));

        return serviceList;
    }

    public String getId() {
        log.trace ("Getting id");
        return id;
    }

    public BundleRepository getRepository() {
        return repository;
    }

    private void setStatus (Status.CODE code, String msg,
                            Logging.LogLevel level) {
        status = new Status(code, msg);
        Logging.log (this +" status: "+ status, log, level);        
    }

    private void setStatusIdle () {
        setStatus (Status.CODE.idle, "ready", Logging.LogLevel.DEBUG);
    }

    private void setStatusRunning (String msg) {
        setStatus (Status.CODE.running, msg, Logging.LogLevel.INFO);
    }

    public String toString () {
        return "["+id+"@"+getRMIAddress()+"]";
    }

    /**
     * Deploy a local package over another (possibly running) service
     * @param instanceId if of the package to redeploy
     * @param tmpPkgFile the downloaded package which to deploy instead of the
     *                   existing service
     */
    private void reDeployService (String instanceId, File tmpPkgFile) {
        File pkgFile = new File (servicePath, instanceId);

        setStatusRunning("Redeploying service '" + instanceId
                         + "' from " + tmpPkgFile);


        try {
            removeService (instanceId);
            Files.copy(tmpPkgFile, pkgFile, false);
            //FIXME: We should really unzip to the location instead
        } catch (RemoteException re){
            log.error ("Error removing service '" + instanceId
                       + "', aborting redeploy", re);
        } catch (IOException e) {
            log.error ("Error redeploying service '" + instanceId + "' from "
                                    + tmpPkgFile + " to " + pkgFile, e);
        }

        setStatusIdle();
    }

    /**
     * Stop a service and move its package file to artifacts/
     * @param id the service to stop and remove
     * @throws RemoteException upon communication errors with the service
     */
    private void removeService(String id) throws RemoteException {
        ConnectionContext<Service> connCtx = null;
        File pkgFile = serviceMan.getServiceDir(id);
        String artifactPkgPath;

        if (!serviceMan.knows(id)) {
            throw new NoSuchServiceException(this, id, "removeService");
        }

        connCtx = serviceMan.get (id);

        /* Close the service if it is running */
        if (connCtx != null) {
            try {
                connCtx.getConnection().stop ();
            } finally {
                connCtx.unref();
            }
        }

        /* Find an available file name in the artifacts dir */
        int availNum = 1;
        artifactPkgPath = artifactPath +File.separator
                               + Files.baseName(pkgFile) + ".old.0";
        while (new File (artifactPkgPath).exists()) {
            artifactPkgPath =  artifactPath +File.separator
                               + Files.baseName(pkgFile) + ".old." + availNum;
        }

        pkgFile.renameTo(new File(artifactPkgPath));
    }

    /**
     * Set the file permissions correctly for various know trouble makers.
     * For instance JMX access and passwrod files need read-only permissions.
     * @param id the id of the service to set permissions for
     */
    private void checkPermissions(String id) {
        File bundleDir = serviceMan.getServiceDir(id);
        File policy = new File(bundleDir, BundleStub.POLICY_FILE);
        File password = new File(bundleDir, BundleStub.JMX_PASSWORD_FILE);
        File access = new File(bundleDir, BundleStub.JMX_ACCESS_FILE);

        if (policy.exists()) {
            log.trace("Setting " + policy + " read only");
            policy.setReadable(false, false); // disallow all reading
            policy.setReadable(true); // allow user reading
            policy.setWritable(false, false); // disallow all writing
        }

        if (password.exists()) {
            log.trace("Setting " + password + " read only");
            password.setReadable(false, false); // disallow all reading
            password.setReadable(true); // allow user reading
            password.setWritable(false, false); // disallow all writing
        }

        if (access.exists()) {
            log.trace("Setting " + access + " read only");
            access.setReadable(false, false); // disallow all reading
            access.setReadable(true); // allow user reading
            access.setWritable(false, false); // disallow all writing
        }
    }

    public static void main(String[] args) {
        try {
            Configuration conf = Configuration.getSystemConfiguration();
            Client client = new Client(conf);
            // The spawned server thread for RMI will cause the JVM to not exit
        } catch (Throwable e) {
            log.fatal("Caught toplevel exception, bailing out.", e);
            System.err.println ("Client caught toplevel exception. "
                                + "Bailing out: " + e.getMessage());
            System.exit (1);
        }

    }
}
