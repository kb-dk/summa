package dk.statsbiblioteket.summa.control.client.shell;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.shell.RemoteCommand;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.api.InvalidServiceStateException;
import dk.statsbiblioteket.summa.control.client.Client;
import dk.statsbiblioteket.util.rpc.ConnectionManager;

import java.util.List;

/**
 * A {@link Command} to list the services deployed in a {@link Client}.
 * Used in {@link ClientShell}.
 */
public class ServicesCommand extends RemoteCommand<ClientConnection> {

    private String clientAddress;

    public ServicesCommand(ConnectionManager<ClientConnection> connMgr,
                           String clientAddress) {
        super("services", "List and query all deployed services", connMgr);
        this.clientAddress = clientAddress;

        installOption("s", "status", false,
                      "Include service status for each service");

    }

    public void invoke(ShellContext ctx) throws Exception {
        ClientConnection client = getConnection(clientAddress);

        try {
            List<String> services = client.getServices();
            boolean listStatus = hasOption("s");

            ctx.info ("Known services:");

            for (String service : services) {
                String msg = "\t" + service;
                if (listStatus) {
                    String status;
                    try {
                         status = "" + client.getServiceStatus(service);
                    } catch (InvalidServiceStateException e) {
                        status = "Not running";
                    } catch (Exception e) {
                        status = "Error connecting: " + e.getMessage();
                        client.reportError(service);
                    }
                    msg += "  " + status;
                }
                ctx.info(msg);
            }
        } finally {
            releaseConnection();
        }
    }
}
