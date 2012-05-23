package dk.statsbiblioteket.summa.control.server;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.control.api.*;
import dk.statsbiblioteket.summa.control.api.feedback.Feedback;
import dk.statsbiblioteket.summa.control.api.feedback.FeedbackFactory;
import dk.statsbiblioteket.summa.control.api.rmi.ControlRMIConnection;
import dk.statsbiblioteket.summa.control.bundle.BundleUtils;
import dk.statsbiblioteket.summa.control.bundle.BundleSpecBuilder;
import dk.statsbiblioteket.summa.control.bundle.Bundle;

import java.io.IOException;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Unfinished")
public class ControlCore extends UnicastRemoteObject
               implements ControlRMIConnection, ControlCoreMBean, Configurable {

    /**
     * Configuration property defining which port the
     * {@link ControlCore} should communicate. Default is 27001.
     */
    public static final String CONF_CONTROL_CORE_PORT = "summa.control.core.port";

    /**
     * Configuration property defining which port the
     * {@link ControlCore}s registry should run. Default is 27000.
     */
    public static final String CONF_CONTROL_REGISTRY_PORT = "summa.control.core.registryport";

    /**
     * Configuration property defining the base directory for the ControlCore.
     * Default is <code>${user.home}/summa-control</code>.
     */
    public static final String CONF_CONTROL_BASE_DIR = "summa.control.core.dir";

    private static final Log log = LogFactory.getLog (ControlCore.class);

    private File baseDir;
    private Status status;
    private ClientManager clientManager;
    private RepositoryManager repoManager;
    private ConfigurationManager confManager;

    public ControlCore(Configuration conf) throws IOException {
        super (getServicePort(conf));

        setStatus(Status.CODE.not_instantiated,
                  "Setting up", Logging.LogLevel.DEBUG);

        clientManager = new ClientManager(conf);
        repoManager = new RepositoryManager(conf);
        confManager = new ConfigurationManager(conf);

        baseDir = ControlUtils.getControlBaseDir(conf);
        log.debug ("Using base dir '" + baseDir + "'");

        BundleUtils.prepareCodeBase(conf, repoManager.getRepository(),
                                    "summa-common", "summa-control-api");
        setStatus(Status.CODE.not_instantiated,
                  "Exporting remote interfaces", Logging.LogLevel.DEBUG);
        RemoteHelper.exportRemoteInterface(
                                 this,
                                 conf.getInt(CONF_CONTROL_REGISTRY_PORT, 27000),
                                 "summa-control");

        try {
            RemoteHelper.exportMBean(this);
        } catch (Exception e) {
            String msg = "Failed to register MBean, going on without it. "
                         + "Error was: " + e.getMessage();
            if (log.isTraceEnabled()) {
                log.warn (msg, e);
            } else {
                log.warn(msg);
            }
        }

        runAutoStartClients ();

        setStatus(Status.CODE.idle,
                  "Set up complete. Remote interfaces up",
                  Logging.LogLevel.DEBUG);
    }

    private void runAutoStartClients () {
        setStatusRunning("Starting auto-start clients");

        for (String instanceId : clientManager) {
            Configuration conf =
                               clientManager.getDeployConfiguration(instanceId);

            if (conf.getBoolean(Bundle.CONF_AUTO_START,
                                Bundle.DEFAULT_AUTO_START)) {
                log.info("Auto-starting client '" + instanceId + "'");
                try {
                    startClient (conf);
                } catch (Exception e) {
                    log.error("Error auto-starting client '"
                              + instanceId + "': " + e.getMessage(), e);
                }
            } else {
                log.debug("Client '" + instanceId + "' not scheduled for " +
                          "auto-start");
            }
        }

        setStatusIdle();
    }

    private static int getServicePort(Configuration conf) {
        return conf.getInt(CONF_CONTROL_CORE_PORT, 27001);
    }

    public ClientConnection getClient(String instanceId) {
        setStatus(Status.CODE.running,
                  "Looking up client '" + instanceId + "'",
                  Logging.LogLevel.DEBUG);

        if (!clientManager.knowsClient(instanceId)) {
            setStatusIdle();
            throw new NoSuchClientException("Unknown client: " + instanceId);
        }

        ConnectionContext<ClientConnection> conn =
                                                 clientManager.get (instanceId);

        if (conn == null) {
            setStatusIdle();
            throw new InvalidClientStateException (instanceId, "Not running");
        }

        ClientConnection client = conn.getConnection();

        /* validate the connection */
        try {
            String lookupId = client.getId();

            if (!instanceId.equals(lookupId)) {
                log.warn ("Client reports illegal id '" + lookupId
                          + "'. Expected '" + instanceId + "'");
                throw new InvalidClientStateException(instanceId,
                                                      "Reports illegal id '"
                                                      + lookupId + "'");
            }
        } catch (Exception e) {
            clientManager.reportError(conn, e);
            throw new InvalidClientStateException(instanceId,
                                                  "Broken connection", e);
        } finally {
            clientManager.release (conn);
        }

        setStatusIdle();

        return client;
    }

    public void deployClient(Configuration conf) {
        setStatusRunning ("Deploying client");

        validateClientConf(conf);
        log.info ("Preparing to deploy client with deployer config: \n"
                   + conf.dumpString());

        ClientDeployer deployer = DeployerFactory.createClientDeployer(conf);
        Feedback feedback = FeedbackFactory.createFeedback(conf);

        String instanceId = conf.getString(ClientDeployer.CONF_INSTANCE_ID);

        /* Make sure that a client with the given id isn't already deployed */
        if (clientManager.knowsClient (instanceId)) {
            throw new ClientDeploymentException ("A client with instance id '"
                                                 +instanceId
                                                 + "' already exists");
        }

        setStatusRunning ("Deploying client '" + instanceId + "'. "
                          + "Waiting for deployer");
        try {
            deployer.deploy(feedback);
        } catch (Exception e) {
            setStatusIdle();
            throw new ClientDeploymentException("Error when deploying client '"
                                                + instanceId + "': "
                                                + e.getMessage(), e);
        }

        /* Client is deployed. Store everything we need to know about this
         * deployment */

        // First extract the bundle spec
        String bundleId = conf.getString(ClientDeployer.CONF_DEPLOYER_BUNDLE);
        byte[] bundleSpecContent = repoManager.getBundleSpec(bundleId);
        BundleSpecBuilder spec = BundleSpecBuilder.open(
                                   new ByteArrayInputStream(bundleSpecContent));

        // Write client meta file
        clientManager.register(instanceId,
                               deployer.getTargetHost(),
                               conf,
                               spec);

        log.info ("Client '" + instanceId + "' deployed");
        setStatusIdle();
    }

    public void startClient(Configuration conf) {
        setStatusRunning("Starting client");

        String instanceId = conf.getString(ClientDeployer.CONF_INSTANCE_ID);
        String bundleId = clientManager.getBundleId(instanceId);
        log.trace("startClient: got bundleId '" + bundleId + "'");

        if (bundleId == null) {
            setStatusIdle();
            throw new ClientDeploymentException("Unknown instance '"
                                                 + instanceId + "'");
        }

        if (!clientManager.knowsClient(instanceId)) {
            setStatusIdle();
            throw new BadConfigurationException("Unknown client instance id '"
                                                + instanceId + "'");
        }

        log.info ("Preparing to start client '"
                  + instanceId
                  + "' with deployment configuration:\n" + conf.dumpString());

        conf.set(ClientDeployer.CONF_DEPLOYER_TARGET,
                 clientManager.getDeployTarget(instanceId));

        conf.set (ClientDeployer.CONF_DEPLOYER_BUNDLE,
                  bundleId);

        conf.set (ClientDeployer.CONF_DEPLOYER_BUNDLE_FILE,
                  repoManager.getBundle(bundleId).getAbsolutePath());

        log.debug ("Modified deployment configuration:\n" + conf.dumpString());

        ClientDeployer deployer = DeployerFactory.createClientDeployer (conf);
        Feedback feedback = FeedbackFactory.createFeedback (conf);

        try {
            setStatusRunning("Starting '" + instanceId
                             + "'. Waiting for deployer");
            StatusMonitor mon = new StatusMonitor(clientManager,
                                                  instanceId,
                                                  8,
                                                  feedback,
                                                  Status.CODE.not_instantiated);
            Thread statusThread = new Thread (mon);
            statusThread.setDaemon (true); // Allow JVM to exit
            statusThread.start();
            deployer.start(feedback);
        } catch (Exception e) {
            setStatusIdle();
            throw new ClientDeploymentException("Error when starting client '"
                                                + instanceId + "': "
                                                + e.getMessage(), e);
        }

        setStatusIdle();
        log.info ("Client '" + instanceId + "' deployed");
    }

    public void stopClient(String instanceId) {
        setStatusRunning("Stopping '" + instanceId + "'");

        ConnectionContext<ClientConnection> conn = null;
        try {
            conn = clientManager.get(instanceId);

            if (conn == null) {
                setStatusIdle();
                throw new NoSuchClientException(instanceId);
            }

            ClientConnection client = conn.getConnection();
            client.stop ();
        } catch (IOException e) {
            log.error("Error stopping client '" + instanceId + "'", e);
        } finally {
            if (conn != null) {
                clientManager.release(conn);
            }
            setStatusIdle();
        }

    }

    /**
     * <p>Return true <i>iff</i> the provided {@link Configuration} contains the
     * required properties listed in {@link ClientDeployer}.</p>
     *
     * <p>If the {@link ClientDeployer#CONF_CLIENT_CONF} is not set,
     * it will be set to point at the configuration of this Control instance.</p>
     *
     * <p>If the {@link ClientDeployer#CONF_DEPLOYER_BUNDLE_FILE} is not
     * set it will be calculated from the
     * {@link ClientDeployer#CONF_DEPLOYER_BUNDLE} and set in the
     * configuration.</p>
     *
     * @param conf the configuration to validate
     * @throws BadConfigurationException if any one of  the required properties
     *                                   listed in {@link ClientDeployer} is not
     *                                   present
     */
    private void validateClientConf(Configuration conf) {
        log.trace("validateClientConf called");
        String bdl =  conf.getString(ClientDeployer.CONF_DEPLOYER_BUNDLE,
                                     null);

        if (bdl == null) {
            throw new BadConfigurationException ("No bundle id specified in "
                                                 + "deployment "
                                                 + "configuration");
        }

        File bdlFile = repoManager.getBundle(bdl);

        if (bdlFile == null) {
            throw new BadConfigurationException ("No such bundle '"
                                                 + bdl + "'");
        }

        if (!bdlFile.exists()) {
            throw new BadConfigurationException("Bundle file '" + bdlFile
                                                + "' to deploy does not "
                                                + "exist");
        }

        /* Set bundle file prop if it is not already set */
        String bdlFileProp =
                conf.getString (ClientDeployer.CONF_DEPLOYER_BUNDLE_FILE,
                                bdlFile.getAbsolutePath());
        log.trace ("Setting " + ClientDeployer.CONF_DEPLOYER_BUNDLE_FILE
                   + " = " + bdlFileProp);
        conf.set(ClientDeployer.CONF_DEPLOYER_BUNDLE_FILE,
                 bdlFileProp);



        try { conf.getString(ClientDeployer.CONF_INSTANCE_ID); }
        catch (NullPointerException e) {
            throw new BadConfigurationException("No instance id defined in "
                                                + "deployment configuration");
        }

        try { conf.getString(ClientDeployer.CONF_DEPLOYER_CLASS); }
        catch (NullPointerException e) {
            throw new BadConfigurationException("Required property: "
                                                + ClientDeployer.CONF_DEPLOYER_CLASS
                                                + " not set ");
        }

        try { conf.getString(ClientDeployer.CONF_DEPLOYER_TARGET); }
        catch (NullPointerException e) {
            throw new BadConfigurationException("Required property: "
                                                + ClientDeployer.CONF_DEPLOYER_TARGET
                                                + " not set ");
        }

        try { conf.getString(ClientDeployer.CONF_DEPLOYER_FEEDBACK); }
        catch (NullPointerException e) {
            throw new BadConfigurationException("Required property: "
                                                + ClientDeployer.CONF_DEPLOYER_FEEDBACK
                                                + " not set ");
        }

        try {
            conf.getString(ClientDeployer.CONF_CLIENT_CONF);
        } catch (NullPointerException e) {
            // The ClientDeployer.CONF_CLIENT_CONF is not set, set it as
            // specified by our contract (see javadoc for said property)
            log.debug (ClientDeployer.CONF_CLIENT_CONF + " not set, "
                       + "setting to 'configuration.xml'");
            conf.set(ClientDeployer.CONF_CLIENT_CONF,
                     "configuration.xml");
        }
    }

    public List<String> getClients() {
        setStatus(Status.CODE.running, "Getting client list",
                  Logging.LogLevel.TRACE);
        List<String> list = clientManager.getClients();
        setStatusIdle();
        return list;
    }

    public List<String> getBundles() {
        setStatus(Status.CODE.running, "Getting bundle list",
                  Logging.LogLevel.TRACE);
        List<String> list = repoManager.getBundles();
        setStatusIdle();
        return list;

    }

    public Configuration getDeployConfiguration(String instanceId)
            throws RemoteException {
        return clientManager.getDeployConfiguration(instanceId);
    }

    public Status getStatus() throws RemoteException {
        return status;
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

    public static void main (String[] args) {
        Configuration conf = Configuration.getSystemConfiguration();
        try {
            ControlCore control = new ControlCore(conf);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit (1);
        }
    }
}


