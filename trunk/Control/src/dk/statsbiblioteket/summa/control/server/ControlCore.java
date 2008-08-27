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

import java.io.IOException;
import java.io.File;
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
    public static final String CONTROL_CORE_PORT = "summa.control.core.port";

    /**
     * Configuration property defining which port the
     * {@link ControlCore}s registry should run. Default is 27000.
     */
    public static final String CONTROL_REGISTRY_PORT = "summa.control.core.registryPort";

    /**
     * Configuration property defining the base directory for the ControlCore.
     * Default is <code>${user.home}/summa-control</code>.
     */
    public static final String CONTROL_BASE_DIR = "summa.control.core.dir";

    private Log log = LogFactory.getLog (ControlCore.class);
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

        setStatus(Status.CODE.not_instantiated,
                  "Exporting remote interfaces", Logging.LogLevel.DEBUG);
        RemoteHelper.exportRemoteInterface(this,
                                        conf.getInt(CONTROL_REGISTRY_PORT, 27000),
                                        "summa-control");

        try {
            RemoteHelper.exportMBean(this);
        } catch (Exception e) {
            log.warn ("Failed to register MBean, going on without it. "
                      + "Error was", e);
        }

        setStatus(Status.CODE.idle,
                  "Set up complete. Remote interfaces up",
                  Logging.LogLevel.DEBUG);
    }

    private static int getServicePort(Configuration conf) {
        return conf.getInt(CONTROL_CORE_PORT, 27001);
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
            return null;
        }

        ClientConnection client = conn.getConnection();
        clientManager.release (conn);

        setStatusIdle();

        return client;
    }

    public void deployClient(Configuration conf) {
        setStatusRunning ("Deploying client");

        validateClientConf(conf);
        log.info ("Preparing to start client with deployer config: \n"
                   + conf.dumpString());

        ClientDeployer deployer = DeployerFactory.createClientDeployer(conf);
        Feedback feedback = FeedbackFactory.createFeedback(conf);

        String instanceId = conf.getString(ClientDeployer.INSTANCE_ID_PROPERTY);

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

        clientManager.register(instanceId,
                               deployer.getTargetHost(),
                               conf);

        log.info ("Client '" + instanceId + "' deployed");
        setStatusIdle();
    }

    public void startClient(Configuration conf) {
        setStatusRunning("Starting client");

        String instanceId = conf.getString(ClientDeployer.INSTANCE_ID_PROPERTY);
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

        conf.set(ClientDeployer.DEPLOYER_TARGET_PROPERTY,
                 clientManager.getDeployTarget(instanceId));

        conf.set (ClientDeployer.DEPLOYER_BUNDLE_PROPERTY,
                  bundleId);

        conf.set (ClientDeployer.DEPLOYER_BUNDLE_FILE_PROPERTY,
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
     * <p>If the {@link ClientDeployer#CLIENT_CONF_PROPERTY} is not set,
     * it will be set to point at the configuration of this Control instance.</p>
     *
     * <p>If the {@link ClientDeployer#DEPLOYER_BUNDLE_FILE_PROPERTY} is not
     * set it will be calculated from the
     * {@link ClientDeployer#DEPLOYER_BUNDLE_PROPERTY} and set in the
     * configuration.</p>
     *
     * @param conf the configuration to validate
     * @throws BadConfigurationException if any one of  the required properties
     *                                   listed in {@link ClientDeployer} is not
     *                                   present
     */
    private void validateClientConf(Configuration conf) {
        log.trace("validateClientConf called");
        try {
            String bdl =  conf.getString(ClientDeployer.DEPLOYER_BUNDLE_PROPERTY);

            File bdlFile = repoManager.getBundle(bdl);

            if (!bdlFile.exists()) {
                throw new BadConfigurationException("Bundle file '" + bdlFile
                                                  + "' to deploy does not "
                                                  + "exist");
            }

            /* Set bundle file prop if it is not already set */
            String bdlFileProp =
                   conf.getString (ClientDeployer.DEPLOYER_BUNDLE_FILE_PROPERTY,
                                   bdlFile.getAbsolutePath());
            log.trace ("Setting " + ClientDeployer.DEPLOYER_BUNDLE_FILE_PROPERTY
                       + " = " + bdlFileProp);
            conf.set(ClientDeployer.DEPLOYER_BUNDLE_FILE_PROPERTY,
                     bdlFileProp);

        }
        catch (NullPointerException e) {
            throw new BadConfigurationException("Required property: "
                                                + ClientDeployer.DEPLOYER_BUNDLE_PROPERTY
                                                + " not set ");
        }

        try { conf.getString(ClientDeployer.INSTANCE_ID_PROPERTY); }
        catch (NullPointerException e) {
            throw new BadConfigurationException("Required property: "
                                                + ClientDeployer.INSTANCE_ID_PROPERTY
                                                + " not set ");
        }

        try { conf.getString(ClientDeployer.DEPLOYER_CLASS_PROPERTY); }
        catch (NullPointerException e) {
            throw new BadConfigurationException("Required property: "
                                                + ClientDeployer.DEPLOYER_CLASS_PROPERTY
                                                + " not set ");
        }

        try { conf.getString(ClientDeployer.DEPLOYER_TARGET_PROPERTY); }
        catch (NullPointerException e) {
            throw new BadConfigurationException("Required property: "
                                                + ClientDeployer.DEPLOYER_TARGET_PROPERTY
                                                + " not set ");
        }

        try { conf.getString(ClientDeployer.DEPLOYER_FEEDBACK_PROPERTY); }
        catch (NullPointerException e) {
            throw new BadConfigurationException("Required property: "
                                                + ClientDeployer.DEPLOYER_FEEDBACK_PROPERTY
                                                + " not set ");
        }

        try {
            conf.getString(ClientDeployer.CLIENT_CONF_PROPERTY);
        } catch (NullPointerException e) {
            // The ClientDeployer.CLIENT_CONF_PROPERTY is not set, set it as
            // specified by our contract (see javadoc for said property)
            log.debug (ClientDeployer.CLIENT_CONF_PROPERTY + " not set, "
                       + "setting to " + confManager.getPublicAddress());
            conf.set(ClientDeployer.CLIENT_CONF_PROPERTY,
                     confManager.getPublicAddress());
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
