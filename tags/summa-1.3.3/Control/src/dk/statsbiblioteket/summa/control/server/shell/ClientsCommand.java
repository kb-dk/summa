package dk.statsbiblioteket.summa.control.server.shell;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.control.api.*;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;

import java.util.List;

/**
 * Created by IntelliJ IDEA. User: mikkel Date: Jan 31, 2008 Time: 11:09:01 AM
 * To change this template use File | Settings | File Templates.
 */
public class ClientsCommand extends Command {

    private ConnectionManager<ControlConnection> cm;
    private String controlAddress;

    public ClientsCommand (ConnectionManager<ControlConnection> cm,
                           String controlAddress) {
        super ("clients", "list all deployed clients");

        installOption("s", "status", false, "Look up status for each client (slow)");
        installOption("e", "extended", false, "Get extended metadata about each client");

        this.cm = cm;
        this.controlAddress = controlAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {
         /* Connect to the Control and send getClients request */

        boolean doStatus = hasOption("s");
        boolean doExtended = hasOption("e");

        ConnectionContext<ControlConnection> connCtx = null;
        try {
            connCtx = cm.get (controlAddress);
            if (connCtx == null) {
                ctx.error ("Failed to connect to Control server at '"
                           + controlAddress + "'");
                return;
            }

            ControlConnection control = connCtx.getConnection();
            List<String> clients = control.getClients();

            /* Header */
            String header = "Deployed clients";
            if (doStatus) {
                header += "\tStatus";
            }
            if (doExtended) {
                header += "\tBundle, Address";
            }
            ctx.info (header);

            /* Generate report */
            for (String client : clients) {
                String msg = "\t" + client;
                if (doStatus) {
                    try {
                        ClientConnection conn = control.getClient(client);

                        if (conn == null) {
                            msg += "\t" + new Status(Status.CODE.not_instantiated,
                                                     "No connection to client");
                        } else {
                            msg += "\t" + conn.getStatus();
                        }
                    } catch (InvalidClientStateException e) {
                        msg += "\t" + e.getMessage();
                    } catch (Exception e) {
                        msg += "\tConnection error: " + e.getMessage();                        
                    }
                }
                if (doExtended) {
                    try {
                        Configuration conf = control.getDeployConfiguration(client);

                        String bundleId = conf.getString (ClientDeployer.CONF_DEPLOYER_BUNDLE,
                                                          "ERROR");
                        String host = conf.getString (ClientConnection.CONF_REGISTRY_HOST,
                                                      "ERROR");
                        String port = conf.getString (ClientConnection.CONF_REGISTRY_PORT,
                                                      "ERROR");
                        String address = "ERROR";
                        if (!"ERROR".equals(host) && !"ERROR".equals(port)) {
                            address = "//" + host + ":" + port + "/" + client;
                        }

                        msg += "\t" + bundleId + ", " + address;
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



