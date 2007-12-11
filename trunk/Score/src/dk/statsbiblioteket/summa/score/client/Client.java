/* $Id: Client.java,v 1.24 2007/10/29 14:38:15 mke Exp $
 * $Revision: 1.24 $
 * $Date: 2007/10/29 14:38:15 $
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
package dk.statsbiblioteket.summa.score.client;

import dk.statsbiblioteket.summa.score.api.*;
import dk.statsbiblioteket.summa.score.bundle.BundleLoader;
import dk.statsbiblioteket.summa.score.bundle.BundleRepository;
import dk.statsbiblioteket.summa.score.bundle.BundleLoadingException;
import dk.statsbiblioteket.summa.score.bundle.BundleStub;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configurable.ConfigurationException;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Zips;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.management.ManagementFactory;
import java.io.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * <p>Core class for running Score clients.</p>
 *
 * <p>The client talks to the Score server via a {@link ScoreConnection}.
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

    private Map<String, Service> services = new HashMap<String, Service>(10);
    private Status status;
    private BundleRepository repository;
    private BundleLoader loader;
    private String id;
    private String hostname;
    private String basePath;
    private String tmpPath;
    private String servicePath;
    private String artifactPath; // Removed service packages
    private String persistentPath;


    // RMI-related data
    private String registryHost, serviceName;
    private int registryPort, servicePort;

    /**
     * Create a client from a {@link Configuration} and expose it over rmi.
     * The various RMI properties needed are read from the {@code Configuraion}
     * as defined in {@link ClientConnection}.
     * @param configuration the configuration from which to extract rmi  properties
     * @throws RemoteException if there is an error exposing the rmi service
     */
    public Client(Configuration configuration) throws RemoteException {
        super (getServicePort (configuration));
        log.debug("Constructing client");

        this.registryHost = configuration.getString(REGISTRY_HOST);
        this.registryPort = configuration.getInt(REGISTRY_PORT);
        this.serviceName = configuration.getString(CLIENT_ID);
        this.servicePort = configuration.getInt(REGISTRY_PORT);
        this.id = configuration.getString(CLIENT_ID);

        this.basePath = System.getProperty("user.home") + File.separator
                                     + configuration.getString(CLIENT_BASEPATH)
                                     + File.separator + serviceName;
        this.tmpPath = basePath + File.separator + "tmp";
        this.servicePath = basePath + File.separator + "services";
        this.artifactPath = basePath + File.separator + "artifacts";
        this.persistentPath = basePath + File.separator
                                       + ".." + File.separator +"persistent";

        setupServiceRepository(configuration);
        setupServiceLoader(configuration);

        validateConfiguration ();

        // Find client hostname
        try {
            java.net.InetAddress localMachine =
                    java.net.InetAddress.getLocalHost();
            hostname = localMachine.getHostName();
            log.debug ("Found hostname: " + hostname);
        } catch (Throwable t) {
            hostname = "localhost";
            log.warn ("Unable to get hostname for " + this
                      + ". Using 'localhost'", t);
        }


        setStatus(Status.CODE.constructed, "Setting up remote interfaces (rmi,jmx)",
                  Logging.LogLevel.DEBUG);
        exportRemoteInterfaces();
        setStatus(Status.CODE.constructed, "Remote interfaces up",
                  Logging.LogLevel.DEBUG);

        // Make sure all needed directories are created
        new File(tmpPath).mkdirs();
        new File(servicePath).mkdirs();
        new File(artifactPath).mkdirs();
        new File(persistentPath).mkdirs();
    }

    private void setupServiceRepository (Configuration conf) {
        Configuration repoConfig;
        if (conf.supportsSubConfiguration()) {
            try {
                repoConfig = conf.getSubConfiguration(REPOSITORY_CONFIG);
                log.debug ("Using embedded config for repository");
            } catch (IOException e) {
                throw new Configurable.ConfigurationException ("Unable to get "
                                                             + "sub config "
                                                             + REPOSITORY_CONFIG,
                                                             e);
            }
        } else {
            repoConfig = conf;
            log.debug ("Sharing config with repository");
        }

        Class<BundleRepository> repositoryClass =
                                    repoConfig.getClass(REPOSITORY_CLASS,
                                                        BundleRepository.class);
        repository = repoConfig.create (repositoryClass);
    }

    private void setupServiceLoader (Configuration conf) {
        Configuration loaderConfig;
        if (conf.supportsSubConfiguration()) {
            try {
                loaderConfig = conf.getSubConfiguration(LOADER_CONFIG);
                log.debug ("Using embedded config for bundle loader");
            } catch (IOException e) {
                throw new Configurable.ConfigurationException ("Unable to get "
                                                             + "sub config "
                                                             + LOADER_CONFIG,
                                                             e);
            }
        } else {
            loaderConfig = conf;
            log.debug ("Sharing config with bundle loader");
        }

        loader = loaderConfig.create (BundleLoader.class);
    }

    /**
     * Check that the states set by the configuration are good.
     * Throw an BadConfigurationException if something is ascrew.
     * @throws dk.statsbiblioteket.summa.score.api.BadConfigurationException if
     *         the configuration provided to the client is insufficient
     */
    private void validateConfiguration() throws BadConfigurationException {
        if (registryHost.equals("")) {
            throw new BadConfigurationException (this + ", " + REGISTRY_HOST
                                                 + " is empty");
        } else if (registryPort < 0) {
            throw new BadConfigurationException (this + ", " + REGISTRY_PORT
                                                + " < 0. Value " + registryPort);
        } else if (serviceName.equals("")) {
            throw new BadConfigurationException (this + ", " + CLIENT_ID
                                                 + " is empty");
        } else if (servicePort < 0) {
            throw new BadConfigurationException (this + ", " + servicePort
                                                + " < 0. Value " + servicePort);
        } else if (id.equals("")) {
            throw new BadConfigurationException (this +", " + CLIENT_ID
                                                 + " is empty");
        }  else if (basePath.equals("")) {
            throw new BadConfigurationException (this +", " + CLIENT_BASEPATH
                                                 + " is empty");
        }
    }

    private String getRMIAddress () {
        return "//"+registryHost+":"+registryPort+"/"+serviceName;
    }

    private File getServiceFile (String id) {
        return new File (servicePath, id);
    }

    public File getServicePath (String id) {
        return new File (servicePath, id);
    }

    /**
     * Read the port on which the client should expose its rmi service.
     *
     * This method is mainly here to be able to retrieve the service
     * port in the super() call in the constructor.
     *
     * @param conf the configuration from which to read {@link #SERVICE_PORT}
     * @return the port
     * @throws ConfigurationException if {@link #SERVICE_PORT} cannot be read
     */
    private static int getServicePort (Configuration conf) {
        try {
            return conf.getInt(SERVICE_PORT);
        } catch (Exception e) {
            log.fatal("Unable to read " + SERVICE_PORT + "from configuration", e);
            throw new ConfigurationException("Unable to read " + SERVICE_PORT
                                       + "from configuration", e);
        }

    }

    /**
     * Expose the Client as a remote service over rmi.
     * @throws RemoteException
     */
    private void exportRemoteInterfaces() throws RemoteException {
        Registry reg = null;

        if ("localhost".equals(registryHost)) {
            try {
                reg = LocateRegistry.createRegistry(registryPort);
                log.debug("Created registry on port " + servicePort);
            } catch (RemoteException e) {
                reg = LocateRegistry.getRegistry(registryHost, registryPort);
                log.debug ("Found registry " + registryHost + ":" + registryPort);
            }
        } else {
            reg = LocateRegistry.getRegistry(registryHost, registryPort);
            log.debug ("Found registry " + registryHost + ":" + registryPort);
        }

        if (reg == null) {
            throw new RemoteException ("Failed to locate or create registry on "
                                        + registryHost + ":" + registryPort);
        }

        try {
            reg.rebind(serviceName, this);
        } catch (RemoteException ee) {
            log.error("Failed to bind in registry", ee);
            throw ee;
        }

        log.info(this.getClass().getSimpleName()
                + " bound in registry "+ registryHost+":"+registryPort + " as '"
                 + serviceName + "' on port " + servicePort);

        try {
            log.debug ("Registering at mbean server");
            MBeanServer mbserver = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName(this.getClass().getName()+ ":type=Client");
            mbserver.registerMBean(this, name);
            log.info ("Registered at mbean server as " + name);
        } catch (Exception e) {
            log.error ("Failed to expose JMX interface. Going on without it.", e);
        }
    }

    public void stop() {
        setStatus(Status.CODE.stopping, "Stopping all services",
                  Logging.LogLevel.INFO);
        for (Map.Entry<String, Service> serviceEntry: services.entrySet()) {
            try {
                log.trace("Trying to stop service "
                        + serviceEntry.getKey());
                serviceEntry.getValue().stop();
                log.debug("Service " + serviceEntry.getKey() + " was stopped");
            } catch (Exception e) {
                log.error("Could not stop service " + serviceEntry.getKey());
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

    public void deployService(String id, String configLocation) {
        setStatusRunning("Deploying service '" + id + "' with config "
                        + configLocation);
        File tmpBundleFile;

        try {
            tmpBundleFile = repository.get (id);            
        } catch (IOException e) {
            throw new BundleLoadingException ("Failed to retrieve " + id
                                            + "from repository", e);
        }

        deployServiceFromLocalFile(id, tmpBundleFile, configLocation);
        
        setStatusIdle();
    }

    /**
     * <p>If this call completes it is guaranteed that
     * {@link #getServiceFile} returns an existing bundle file.</p>
     *
     * <p>The local file is unpacked to {@code servicePath/id}</p>
     *
     * @param id the id under which to deploy the file
     * @param localFile the file to deploy
     * @param configLocation location for configuration, either an URL,
     *                       rmi address, or file path
     */
    public void deployServiceFromLocalFile (String id, File localFile,
                                            String configLocation) {

        if (servicePath.equals(localFile.getParent())) {
            log.error ("Trying to deploy " + localFile + " which is already"
                       + " in the service directory " + servicePath +"."
                       + " Aborting deploy.");
            return;
        } else if (!localFile.exists()) {
            log.error ("Trying to deploy non-existing file " + localFile +
                       ", aborting deploy.");
            return;
        }

        setStatusRunning ("Deploying '" + id + "' from " + localFile);

        File pkgFile = getServiceFile(id);

        // Check if the service is already deployed
        if (services.get(id) != null || pkgFile.exists()) {
            reDeployService(id, localFile);
            return;
        }

        // make sure parent dir for service exists
        pkgFile.mkdirs();

        if (!pkgFile.isDirectory()) {
            throw new BundleLoadingException("Failed to create directory "
                                           + pkgFile + " for deployment of "
                                           + id);
        }

        // Unzip the file into the service directory
        try {
            Zips.unzip (localFile.toString(), pkgFile.toString(), false);
        } catch (IOException e) {
            log.error ("Error deploying " + localFile + ". "
                       + "Purging " + pkgFile + " from service dir", e);
            try {
                Files.delete(pkgFile);
            } catch (IOException ee) {
                log.error ("Error deleting file " + pkgFile
                           + " when cleaning up buggy deploy", e);
            }
        }

        // FIXME: There is a race condition here, where the JMX files are
        //        readable after unpacking, but before we set read-only
        //        permissions
        log.debug ("Setting file permissions for service " + id);
        checkPermissions(id);

        setStatusIdle();
    }

    public void startService(String id, String configLocation)
                                                        throws RemoteException {
        setStatusRunning ("Starting service " + id);
        Service service = services.get(id);
        File serviceFile = getServiceFile(id);
        BundleStub stub;

        if (service != null) {
            log.warn("Trying to start service '" + id
                     + "', but it is already running. Ignoring request.");
            setStatusIdle();
            return;
        } else if (!serviceFile.exists()) {
            log.error ("Trying to start service " + serviceFile + "; no such"
                     + " file or directory. Ignoring request.");
        }

        try {
            stub = loader.load (serviceFile);
        } catch (IOException e) {
            setStatusIdle();
            throw new ClientException (this,
                                       "Error loading service '" + id + "',"
                                      + " from file " + serviceFile, e);
        }

        stub.addSystemProperty(CLIENT_PERSISTENT_DIR, persistentPath);
        stub.addSystemProperty(CLIENT_ID, id);
        stub.addSystemProperty(Service.SERVICE_ID, id);
        stub.addSystemProperty(Service.SERVICE_BASEPATH, serviceFile.getParent());
        stub.addSystemProperty("summa.configuration", configLocation);

        try {
            if (log.isDebugEnabled()) {
                log.debug ("Launching '" + id + "' with command line:\n"
                          + Logs.expand(stub.buildCommandLine(), 100));
            }
            final Process p = stub.start();

            // Flush output stream
            new Thread (new Runnable () {

                public void run() {
                    log.info("Flushing output of child " + p);
                    try {
                        Streams.pipeStream(p.getInputStream(), System.out);
                        Streams.pipeStream(p.getErrorStream(), System.err);
                        log.info("Waiting for process");
                        p.waitFor();
                        p.wait(12);
                    } catch (Exception e) {
                        log.error ("Error flushing subprocess pipe", e);
                    }
                    log.info ("Child process exited with " + p.exitValue());
                }

            }).start();
            
            // FIXME: Should care about the returned Process?
            // FIXME: Connect to the Service interface of the process
        } catch (IOException e) {
            log.error ("Failed to start service '" + id
                       + "' with command line:\n"
                       + Logs.expand(stub.buildCommandLine(), 100), e);
        }

        setStatusIdle();
    }

    public void stopService(String id) throws RemoteException {
        setStatusRunning ("Stopping service " + id);

        Service s = services.get (id);

        if (id == null) {
            log.error ("Cannot stop service. No such service '" + id + "'");
            throw new NoSuchServiceException(this, id, "stopService");
        } else {
            s.stop();
        }

        setStatusIdle ();
    }

    public Status getServiceStatus(String id) throws RemoteException {
        log.trace("Getting service status for " + id);
        Service s = services.get (id);

        if (id == null) {
            log.error ("Cannot get status for service '" + id + "'. No such service");
            throw new NoSuchServiceException(this, id, "getServiceStatus");
        } else {
            return s.getStatus();
        }
    }

    public List<String> getServices() {
        log.trace("Getting list of services");
        List<String> serviceList = new ArrayList<String>(services.keySet());
        return serviceList;
    }

    public String getId() {
        log.trace ("Getting id");
        return id;
    }

    private void setStatus (Status.CODE code, String msg, Logging.LogLevel level) {
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
     * @param id if of the package to redeploy
     * @param tmpPkgFile the downloaded package which to deploy instead of the
     *                   existing service
     */
    private void reDeployService (String id, File tmpPkgFile) {
        File pkgFile = new File (servicePath, id);
        Service old = services.get (id);

        if (! pkgFile.exists()) {
            log.error ("Trying to redeploy unexisting service " + pkgFile
                       + ", skipping. ");
            return;
        }
        if (old == null) {
            throw new NullPointerException("Old service is null even though "
                                         + "package file " + getServicePath(id)
                                         + " exists");
        }

        setStatusRunning("Redeploying service '" + id + "' from " + tmpPkgFile);


        try {
            removeService (id);
            Files.copy(tmpPkgFile, pkgFile, false);
            //FIXME: We should really unzip to the location instead
        } catch (RemoteException re){
            log.error ("Error removing service '" + id + "', aborting redeploy",
                       re);
        } catch (IOException e) {
            log.error ("Error redeploying service '" + id + "' from "
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
        Service service = services.get (id);
        File pkgFile = getServicePath(id);
        String artifactPkgPath;

        if (!pkgFile.exists() && pkgFile.isDirectory()) {
            throw new ServicePackageException(this, id,
                                             "cannot remove service '" + id + "'"
                                           + ", " + pkgFile + " is not a directory");
        }
        if (service == null) {
            throw new NoSuchServiceException(this, id, "removeServide");
        }

        service.stop ();

        int availNum = 1;
        artifactPkgPath = artifactPath +File.separator
                               + Files.baseName(pkgFile) + ".old.0";
        while (new File (artifactPkgPath).exists()) {
            artifactPkgPath =  artifactPath +File.separator
                               + Files.baseName(pkgFile) + ".old." + availNum;
        }

        pkgFile.renameTo(new File(artifactPkgPath));
        services.remove(id);
    }

    /**
     * Set the file permissions correctly for various know trouble makers.
     * For instance JMX access and passwrod files need read-only permissions.
     * @param id the id of the service to set permissions for
     */
    private void checkPermissions(String id) {
        File bundleDir = getServicePath(id);
        File policy = new File(bundleDir, BundleStub.POLICY_FILE);
        File password = new File(bundleDir, BundleStub.JMX_PASSWORD_FILE);
        File access = new File(bundleDir, BundleStub.JMX_ACCESS_FILE);

        if (policy.exists()) {
            log.trace("Setting " + policy + " read only");
            policy.setReadable(false, false); // disallow all reading
            policy.setReadable(true); // allow user reading
        }

        if (password.exists()) {
            log.trace("Setting " + password + " read only");
            password.setReadable(false, false); // disallow all reading
            password.setReadable(true); // allow user reading
        }

        if (access.exists()) {
            log.trace("Setting " + access + " read only");
            access.setReadable(false, false); // disallow all reading
            access.setReadable(true); // allow user reading
        }
    }

    public static void main(String[] args) {
        try {
            Configuration conf = Configuration.getSystemConfiguration();
            Client client = new Client(conf);
            // The spawned server thread for RMI will cause the JVM to not exit
        } catch (Throwable e) {
            log.fatal("Caught toplevel exception, bailing out.", e);
            System.exit (1);
        }

    }
}
