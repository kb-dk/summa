package dk.statsbiblioteket.summa.score.server;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.FileStorage;
import dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory;
import dk.statsbiblioteket.summa.common.rpc.SummaRMIConnectionFactory;
import dk.statsbiblioteket.summa.score.api.ClientConnection;
import dk.statsbiblioteket.summa.score.api.BadConfigurationException;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionContext;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class helps the {@link ScoreCore} manage its collection of clients.
 *
 * @see dk.statsbiblioteket.summa.score.client.Client
 * @see dk.statsbiblioteket.summa.score.api.ClientConnection
 * @see dk.statsbiblioteket.summa.score.api.ScoreConnection
 * @see ScoreCore
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Unfinished")
public class ClientManager extends ConnectionManager<ClientConnection>
                                                           implements Runnable {

    /**
     * Name of file used to store a list of clients. This file will
     * contain a {@code instanceId -> address} map.
     */
    public static final String CLIENT_MAP_FILE = "score.clients.xml";

    /**
     * Configuration property defining the class of the
     * {@link ConnectionFactory} to use for creating client connections.
     */
    public static final String CONNECTION_FACTORY_PROP =
                                               GenericConnectionFactory.FACTORY;

    /**
     * Configuration property defining in what directory to store
     * client deployment metadata
     */
    public static final String CLIENT_META_DIR_PROPERTY = "summa.score.meta.dir";

    /**
     * File extension for client metadata files
     */
    public static final String CLIENT_META_FILE_EXT = ".meta.xml";

    private Log log;
    private File metaDir;

    public ClientManager(Configuration conf) throws IOException {
        super (getConnectionFactory(conf));

        log = LogFactory.getLog (ClientManager.class);
        metaDir = ScoreUtils.getClientMetaDir(conf);
    }

    @SuppressWarnings("unchecked")
    private static ConnectionFactory<? extends ClientConnection>
                                     getConnectionFactory (Configuration conf) {
        Class<? extends ConnectionFactory> connFactClass =
                conf.getClass(CONNECTION_FACTORY_PROP, ConnectionFactory.class,
                              SummaRMIConnectionFactory.class);

        ConnectionFactory connFact = conf.create (connFactClass);
        return (ConnectionFactory<ClientConnection>) connFact;
    }

    /**
     * Return true <i>iff</i> the client metadata file for the given
     * instance id exists.
     * @param instanceId the instance id of the client to check for
     * @return true if the client is already registered
     */
    public boolean knowsClient (String instanceId) {
        return getClientMetaFile(instanceId).exists();
    }

    /**
     * Store connection parameters for the given Client instance.
     * @param instanceId unique instance id of the client
     * @param clientHost hostname of the machine where the client runs
     * @param deployConfig configuration passed to the {@link ClientDeployer}
     *                     responsible for deploying the given client
     * @throws BadConfigurationException if a client with instance id
     *                                   {@code instanceId} is already
     *                                   registered.
     */
    public void register (String instanceId, String clientHost,
                          Configuration deployConfig) {
        log.debug ("Registering Client instance '" + instanceId + "' on host '"
                   + clientHost + "'");

        File clientMeta = getClientMetaFile(instanceId);

        if (clientMeta.exists()) {
            throw new BadConfigurationException ("Client with instance id '"
                                                 + instanceId + "' is already"
                                                 + " registered");
        }

        try {
            Configuration conf = new Configuration (
                                                new FileStorage(clientMeta));
            conf.importConfiguration(deployConfig);
            conf.set (ClientConnection.REGISTRY_HOST_PROPERTY, clientHost);

            int regPort = getClientRegistryPort (instanceId, deployConfig);
            conf.set (ClientConnection.REGISTRY_PORT_PROPERTY, new Integer(regPort).toString());

        } catch (IOException e) {
            log.error ("Failed to write client registration for '"
                       + instanceId + "' in '" + clientMeta + "'");
        }

    }

    /**
     * Return the value of {@link ClientDeployer#DEPLOYER_TARGET_PROPERTY}
     * as set when the client was deployed.
     * @param instanceId the instance id of the client to look uo deployment
     *                   target for
     * @return the target property or null if it is not found
     */
    public String getDeployTarget (String instanceId) {
        try {
            File clientMeta = getClientMetaFile(instanceId);
            Configuration conf = new Configuration (new FileStorage(clientMeta));
            return conf.getString(ClientDeployer.DEPLOYER_TARGET_PROPERTY);
        } catch (IOException e) {
            log.error ("Error while looking up deployment target for '"
                       + instanceId + "'");
            return null;
        }
    }

    /**
     * Return the bundle id that a given instance is based on.
     * @param instanceId the instance id of the client to look up the bundkle id
     *                   for
     * @return the bundle id or null if the instance id is not known
     */
    public String getBundleId (String instanceId) {
        try {
            File clientMeta = getClientMetaFile(instanceId);
            Configuration conf = new Configuration (new FileStorage(clientMeta));
            return conf.getString(ClientDeployer.DEPLOYER_BUNDLE_PROPERTY);
        } catch (IOException e) {
            log.error ("Error while looking up bundle id for '"
                       + instanceId + "'");
            return null;
        }
    }

    /**
     * Find out what registry port a client uses
     * @param instanceId
     * @param deployConfig
     * @return
     */
    private int getClientRegistryPort(String instanceId,
                                      Configuration deployConfig) {
        String clientConfLocation = deployConfig.getString (
                                           ClientDeployer.CLIENT_CONF_PROPERTY);

       /* TODO:
        * if clientConfLocation starts with // create remote storage
        * else if clientConfLocation contains :// or starts with /
        *     create a resource/file storage
        * else if clientConfLocation  does not match any of the above
        *     it is a bundled resource (in the .bundle file)
        *     create a MemoryStorage on the zip-entry of the config
        *     by using ScoreUtils.getZipEntry
        *
        * Create a Configuration on the previsouly created storage.
        * Retrieve ClientConnection.REGISTRY_HOST_PROPERTY and return it
        * */

        log.error ("WARNING - HARDCODED CLIENT REG. PORT 2767");
        return 2767;
    }

    public ConnectionContext<ClientConnection>get (String clientId) {
        String address = getClientAddress (clientId);
        return super.get (address);        
    }

    public String getClientAddress (String instanceId) {
        throw new UnsupportedOperationException();
    }

    public void run() {
        throw new UnsupportedOperationException();
    }

    private File getClientMetaFile (String instanceId) {
        return new File (metaDir, instanceId + CLIENT_META_FILE_EXT);
    }
}
