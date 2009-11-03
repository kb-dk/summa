package dk.statsbiblioteket.summa.control.api;

import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionContext;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper class for instantiating connections to {@link Service}s
 * through {@link ClientConnection}s.
 */
public class ServiceConnectionFactory extends ConnectionFactory<Service> {

    private static Log log = LogFactory.getLog(ServiceConnectionFactory.class);

    private String clientId;
    private ClientConnection staticClient;
    private ConnectionManager<ClientConnection> connMgr;

    public ServiceConnectionFactory(String clientId, ClientConnection client) {
        super();
        this.clientId = clientId;
        staticClient = client;
    }

    public ServiceConnectionFactory(String clientId,
                        ConnectionManager<ClientConnection> connMgr) {
        super();
        this.clientId = clientId;
        this.connMgr = connMgr;
    }

    public Service createConnection(String connectionId)  {
        ClientConnection conn;

        if (staticClient != null) {
            log.debug("Creating Service connection to " + connectionId
                      + " via static Client connection");
            conn = staticClient;
        } else {
            log.debug("Creating Service connection to " + connectionId
                      + " via connection manager");
            ConnectionContext<ClientConnection> connCtx =
                                                      connMgr.get(connectionId);
            if (connCtx != null) {
                conn = connCtx.getConnection();
                connCtx.unref();
            } else {
                throw new InvalidClientStateException(clientId,
                                                      "No connection");
            }
        }

        for (int retries = 0; retries < getNumRetries(); retries++) {
            try {
                return conn.getServiceConnection(connectionId);
            } catch (InvalidServiceStateException e) {
                // Fall through
            } catch (IOException e) {
                log.warn("Error creating connection to '" + connectionId + "': "
                         + e.getMessage(), e);
                return null;
            }

            try {
                log.debug("No connection to service " + connectionId
                          + ". Retrying in " + getGraceTime() + "ms");
                Thread.sleep(getGraceTime());
            } catch (InterruptedException e) {
                log.warn("Interrupted during connection retry grace period");
            }
        }

        // If we get here we timed out
        log.info("Connection to Service '" + connectionId + "' timed out");
        return null;
    }
}
