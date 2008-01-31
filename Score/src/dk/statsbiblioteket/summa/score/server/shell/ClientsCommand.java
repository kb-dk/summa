package dk.statsbiblioteket.summa.score.server.shell;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.score.api.ScoreConnection;
import dk.statsbiblioteket.summa.score.api.ClientConnection;
import dk.statsbiblioteket.summa.score.api.Status;
import dk.statsbiblioteket.summa.score.feedback.RemoteConsoleFeedback;
import dk.statsbiblioteket.summa.score.server.ClientDeployer;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;

import java.util.List;

/**
 * Created by IntelliJ IDEA. User: mikkel Date: Jan 31, 2008 Time: 11:09:01 AM
 * To change this template use File | Settings | File Templates.
 */
public class ClientsCommand extends Command {

    private ConnectionManager<ScoreConnection> cm;
    private String scoreAddress;

    public ClientsCommand (ConnectionManager<ScoreConnection> cm,
                           String scoreAddress) {
        super ("clients", "list all deployed clients");

        installOption("s", "status", false, "Look up status for each client (slow)");
        installOption("e", "extended", false, "Get extended metadata about each client");

        this.cm = cm;
        this.scoreAddress = scoreAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {
         /* Connect to the Score and send getClients request */

        boolean doStatus = hasOption("s");
        boolean doExtended = hasOption("e");

        ConnectionContext<ScoreConnection> connCtx = null;
        try {
            connCtx = cm.get (scoreAddress);
            if (connCtx == null) {
                ctx.error ("Failed to connect to Score server at '"
                           + scoreAddress + "'");
                return;
            }

            ScoreConnection score = connCtx.getConnection();
            List<String> clients = score.getClients();

            /* Header */
            String header = "Deployed clients";
            if (doStatus) {
                header += "\tStatus";
            }
            if (doExtended) {
                header += "\tBundle, Configuration, Address";
            }
            ctx.info (header);

            /* Generate report */
            for (String client : clients) {
                String msg = "\t" + client;
                if (doStatus) {
                    try {
                        ClientConnection conn = score.getClient(client);

                        if (conn == null) {
                            msg += "\t" + new Status(Status.CODE.not_instantiated,
                                                     "No connection to client");
                        } else {
                            msg += "\t" + conn.getStatus();
                        }
                    } catch (Exception e) {
                        msg += "\tError";
                        ctx.error ("When contacting '" + client + "': "
                                   +e.getMessage());
                    }
                }
                if (doExtended) {
                    try {
                        Configuration conf = score.getDeployConfiguration(client);

                        String bundleId = conf.getString (ClientDeployer.DEPLOYER_BUNDLE_PROPERTY,
                                                          "ERROR");
                        String confLoc = conf.getString (ClientDeployer.CLIENT_CONF_PROPERTY,
                                                         "ERROR");
                        String host = conf.getString (ClientConnection.REGISTRY_HOST_PROPERTY,
                                                      "ERROR");
                        String port = conf.getString (ClientConnection.REGISTRY_PORT_PROPERTY,
                                                      "ERROR");
                        String address = "ERROR";
                        if (!"ERROR".equals(host) && !"ERROR".equals(port)) {
                            address = "//" + host + ":" + port + "/" + client;
                        }

                        msg += "\t" + bundleId + ", " + confLoc + ", " + address;
                    } catch (Exception e) {
                        msg += "\tError";
                        ctx.error ("When contacting '" + client + "': "
                                   +e.getMessage());
                    }
                }

                ctx.info(msg);
            }

        } finally {
            if (connCtx != null) {
                cm.release (connCtx);
            }
        }
    }
}
