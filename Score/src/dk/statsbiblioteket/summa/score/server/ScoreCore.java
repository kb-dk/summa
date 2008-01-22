package dk.statsbiblioteket.summa.score.server;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;
import dk.statsbiblioteket.summa.score.bundle.BundleRepository;
import dk.statsbiblioteket.summa.score.api.ClientConnection;
import dk.statsbiblioteket.summa.score.api.BadConfigurationException;
import dk.statsbiblioteket.summa.score.api.Feedback;

import java.io.IOException;
import java.util.List;
import java.rmi.server.UnicastRemoteObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Unfinished")
public class ScoreCore extends UnicastRemoteObject
                  implements ScoreRMIConnection, ScoreCoreMBean, Configurable {

    /**
     * Configuration property defining which port the
     * {@link ScoreCore} should communicate. Default is 27001.
     */
    public static final String SCORE_CORE_PORT = "summa.score.core.port";

    /**
     * Configuration property defining which port the
     * {@link ScoreCore}s registry should run. Default is 27000.
     */
    public static final String SCORE_REGISTRY_PORT = "summa.score.core.registryPort";

    private Log log;
    private ClientManager clientManager;
    private RepositoryManager repoManager;
    private ConfigurationManager confManager;

    public ScoreCore (Configuration conf) throws IOException {
        super (getServicePort(conf));
        log = LogFactory.getLog (ScoreCore.class);
        clientManager = new ClientManager(conf);
        repoManager = new RepositoryManager(conf);
        confManager = new ConfigurationManager(conf);



        RemoteHelper.exportRemoteInterface(this,
                                            conf.getInt(SCORE_REGISTRY_PORT, 27000),
                                            "summa-score");

        try {
            RemoteHelper.exportMBean(this);
        } catch (Exception e) {
            log.warn ("Failed to register MBean, going on without it. "
                      + "Error was", e);
        }
    }

    private static int getServicePort(Configuration conf) {
        return conf.getInt(SCORE_CORE_PORT, 27001);
    }

    public ConfigurationStorage getConfigurationStorage() {
        return confManager.getExportedStorage();
    }

    public BundleRepository getRepository() {
        throw new UnsupportedOperationException();
    }

    /*public Configuration getClientConfiguration (String instanceId) {
        throw new UnsupportedOperationException();
    }*/

    public ClientConnection getClient(String instanceId) {
        ConnectionContext<ClientConnection> conn =
                                                 clientManager.get (instanceId);

        if (conn == null) {
            throw new NoSuchClientException(instanceId);
        }

        ClientConnection client = conn.getConnection();
        clientManager.release (conn);
        return client;
    }

    public void deployClient(Configuration conf) {
        log.trace ("Got deployClient request");
        validateClientConf(conf);
        log.info ("Preparing to start client with deployer config: \n"
                   + conf.dumpString());

        log.debug("Creating deployer from class: "
                  + conf.getString(ClientDeployer.DEPLOYER_CLASS_PROPERTY));
        Class<ClientDeployer> deployerClass =
                           conf.getClass(ClientDeployer.DEPLOYER_CLASS_PROPERTY,
                                         ClientDeployer.class);
        ClientDeployer deployer = conf.create (deployerClass);

        log.debug("Creating deployer feedback from class: "
                  + conf.getString(ClientDeployer.DEPLOYER_FEEDBACK_PROPERTY));
        Class<Feedback> feedbackClass =
                           conf.getClass(ClientDeployer.DEPLOYER_FEEDBACK_PROPERTY,
                                         Feedback.class);
        Feedback feedback = conf.create (feedbackClass);


        String instanceId = conf.getString(ClientDeployer.INSTANCE_ID_PROPERTY);

        try {
            deployer.deploy(feedback);
        } catch (Exception e) {
            throw new ClientDeploymentException("Error when deploying client '"
                                                + instanceId + "': "
                                                + e.getMessage(), e);
        }

        log.info ("Client '" + instanceId + "' deployed");
    }

    public void startClient(Configuration conf) {
        log.trace ("Got startClient request");
        validateClientConf(conf);
        log.info ("Preparing to start client : "
                  + conf.getString(ClientDeployer.INSTANCE_ID_PROPERTY));

        log.debug("Creating deployer from class: "
                  + conf.getString(ClientDeployer.DEPLOYER_CLASS_PROPERTY));
        Class<ClientDeployer> deployerClass =
                           conf.getClass(ClientDeployer.DEPLOYER_CLASS_PROPERTY,
                                         ClientDeployer.class);
        ClientDeployer deployer = conf.create (deployerClass);

        log.debug("Creating deployer feedback from class: "
                  + conf.getString(ClientDeployer.DEPLOYER_CLASS_PROPERTY));
        Class<Feedback> feedbackClass =
                           conf.getClass(ClientDeployer.DEPLOYER_FEEDBACK_PROPERTY,
                                         Feedback.class);
        Feedback feedback = conf.create (feedbackClass);

        String instanceId = conf.getString(ClientDeployer.INSTANCE_ID_PROPERTY);

        try {
            deployer.start(feedback);
        } catch (Exception e) {
            throw new ClientDeploymentException("Error when starting client '"
                                                + instanceId + "'", e);
        }

        log.info ("Client '" + instanceId + "' deployed");
    }

    public void stopClient(String instanceId) {
        ConnectionContext<ClientConnection> conn = null;
        try {
            conn = clientManager.get(instanceId);

            if (conn == null) {
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
        }

    }

    /**
     * <p>Return true <i>iff</i> the provided {@link Configuration} contains the
     * required properties listed in {@link ClientDeployer}.</p>
     *
     * <p>If the {@link ClientDeployer#CLIENT_CONF_PROPERTY} is not set,
     * it will be set to point at the configuration of this Score instance.</p>
     *
     * @param conf the configuration to validate
     * @throws BadConfigurationException if any one of  the required properties
     *                                   listed in {@link ClientDeployer} is not
     *                                   present
     */
    private void validateClientConf(Configuration conf) {
        try { conf.getString(ClientDeployer.DEPLOYER_BUNDLE_PROPERTY); }
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
        throw new UnsupportedOperationException();
    }

    public static void main (String[] args) {
        Configuration conf = Configuration.getSystemConfiguration();
        try {
            ScoreCore score = new ScoreCore(conf);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit (1);
        }
    }
}
