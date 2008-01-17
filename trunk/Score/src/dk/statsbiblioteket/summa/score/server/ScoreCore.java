package dk.statsbiblioteket.summa.score.server;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;
import dk.statsbiblioteket.summa.score.bundle.BundleRepository;
import dk.statsbiblioteket.summa.score.api.ClientConnection;

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

        RemoteHelper.exportRemoteInterfaces(this,
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

    public ClientConnection getClient(String instanceId) {
        throw new UnsupportedOperationException();
    }

    public void deployClient(Configuration conf) {
        //FIXME: If CLIENT_CONF_PROPERTY is not set make it point at the Score's conf server
        log.info ("Got deploy reqeust: \n" + conf.dumpString());

    }

    public void startClient(Configuration conf) {
        //FIXME: If CLIENT_CONF_PROPERTY is not set make it point at the Score's conf server

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
