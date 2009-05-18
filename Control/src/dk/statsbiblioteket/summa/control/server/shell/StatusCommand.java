package dk.statsbiblioteket.summa.control.server.shell;

import dk.statsbiblioteket.summa.control.api.*;
import dk.statsbiblioteket.summa.common.shell.RemoteCommand;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.util.rpc.ConnectionManager;

import java.io.IOException;

/**
 *
 */
public class StatusCommand extends RemoteCommand<ControlConnection> {

    private String controlAddress;

    public StatusCommand(ConnectionManager<ControlConnection> connMgr,
                         String controlAddress) {
        super("status", "Print the status of the control server, query the " +
              "status of deployed clients, or query the status of deployed " +
              "services",
              connMgr);

        setUsage("status [clientId|clientId/serviceId]...");

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
                    String[] parsedTarget = target.split("/");

                    if (parsedTarget.length == 1) {
                        printClientStatus(ctx, control, parsedTarget[0]);
                    } else if (parsedTarget.length == 2){
                        printServiceStatus(ctx, control,
                                           parsedTarget[0], parsedTarget[1]);
                    } else {
                        ctx.warn("Malformed client or service id: " + target);
                    }
                }
            }
        } finally {
            releaseConnection();
        }
    }

    private void printClientStatus(ShellContext ctx, ControlConnection control,
                                   String clientId) throws IOException {
        String status;
        try {
            ClientConnection client = control.getClient(clientId);
            status = "" + client.getStatus();
        } catch (InvalidClientStateException e) {
            status = e.getMessage();
        } catch (NoSuchClientException e) {
            status = "No such client";
        }
        ctx.info("\t" + clientId + ": " + status);
    }

    private void printServiceStatus(ShellContext ctx, ControlConnection control,
                                    String clientId, String serviceId)
                                                            throws IOException {
        String status;
        try {
            ClientConnection client = control.getClient(clientId);
            Service service = client.getServiceConnection(serviceId);
            status = service.getStatus().toString();
        } catch (InvalidServiceStateException e){
            status = e.getMessage();
        } catch (NoSuchServiceException e) {
            status = "No such service '" + serviceId + "'";
        } catch (InvalidClientStateException e) {
            status = "Client '" + clientId + "' not running";
        } catch (NoSuchClientException e) {
            status = "No such client '" + clientId + "'";
        }
        ctx.info("\t" + clientId + "/" + serviceId + ": " + status);

    }
}



