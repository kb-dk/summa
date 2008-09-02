package dk.statsbiblioteket.summa.control.server.shell;

import dk.statsbiblioteket.summa.control.api.*;
import dk.statsbiblioteket.summa.common.shell.RemoteCommand;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.util.rpc.ConnectionManager;

/**
 *
 */
public class StatusCommand extends RemoteCommand<ControlConnection> {

    private String controlAddress;

    public StatusCommand(ConnectionManager<ControlConnection> connMgr,
                         String controlAddress) {
        super("status", "Print the status of the control server, or query the " +
              "status of deployed clients",
              connMgr);

        setUsage("status [client_id]...");

        this.controlAddress = controlAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {
        ControlConnection control = getConnection(controlAddress);

        String[] targets = getArguments();

        try {
            if (targets.length == 0) {
                Status status = control.getStatus();
                ctx.info("Control status: " + status.toString());
            } else {
                ctx.info ("Status of clients:");
                for (String target : targets) {
                    String status;
                    try {
                        ClientConnection client = control.getClient(target);
                        status = "" + client.getStatus();
                    } catch (InvalidClientStateException e) {
                        status = e.getMessage();
                    } catch (NoSuchClientException e) {
                        status = "No such client";
                    }
                    ctx.info("\t" + target + ": " + status);
                }
            }
        } finally {
            releaseConnection();
        }
    }
}
