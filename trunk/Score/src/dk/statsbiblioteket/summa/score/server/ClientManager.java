package dk.statsbiblioteket.summa.score.server;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.FileStorage;
import dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory;
import dk.statsbiblioteket.summa.common.rpc.SummaRMIConnectionFactory;
import dk.statsbiblioteket.summa.score.api.ClientConnection;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionContext;

import java.io.File;
import java.io.IOException;

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


    private Configuration clientMap;

    public ClientManager(Configuration conf) throws IOException {
        super (getConnectionFactory(conf));
        clientMap = new Configuration (
                          new FileStorage(new File("config", CLIENT_MAP_FILE)));
        
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
     * Store connection parameters for the given Client instance.
     * @param instanceId
     * @param clientHost
     */
    public void register (String instanceId, String clientHost,
                          int clientRegistryPort) {
        throw new UnsupportedOperationException();
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
}
