package dk.statsbiblioteket.summa.control.client.shell;

import dk.statsbiblioteket.summa.control.api.*;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.shell.RemoteCommand;
import dk.statsbiblioteket.util.rpc.ConnectionManager;

import java.io.IOException;

/**
 * {@link dk.statsbiblioteket.summa.common.shell.Command} for the client shell
 * to remove a service given by id.
 */
public class RemoveServiceCommand extends RemoteCommand<ClientConnection> {

    private String clientAddress;

    public RemoveServiceCommand(ConnectionManager<ClientConnection> connMgr,
                              String clientAddress) {
        super("remove", "Remove a service from the client, killing "
                        + "it first if necessary", connMgr);
        this.clientAddress = clientAddress;

        setUsage("remove [options] <service-id> [service-id] ...");

        installOption("f", "force", false, "Force removal of service even if it"
                                           + " fails to stop");
    }

    public void invoke(ShellContext ctx) throws Exception {

        if (getArguments().length == 0) {
            ctx.error ("At least one instance id to remove must be specified.");
            return;
        }

        String[] serviceIds = getArguments();
        boolean force = hasOption("force");

        ClientConnection client = getConnection(clientAddress);
        try {
            removeService(client, ctx, serviceIds, force);
        } finally {
            releaseConnection();
        }
    }

    public void removeService(ClientConnection client,
                              ShellContext ctx, String[] serviceIds,
                              boolean force) throws IOException {

        for (String id : serviceIds) {

                /* Make sure service is stopped */
                ctx.prompt ("Killing service '" + id + "' ... ");
                try {
                    try {
                        Status stat = client.getServiceStatus(id);
                        if (stat.getCode() != Status.CODE.not_instantiated) {
                            Service service = client.getServiceConnection(id);
                            service.kill();
                            ctx.info("OK");
                        }
                    } catch (InvalidServiceStateException e) {
                        ctx.info("Not running");
                    }
                } catch (ClientException e){
                    // A ClientException is a controlled exception, we don't print
                    // the whole stack trace
                    ctx.info ("FAILED");
                    ctx.error(e.getMessage());

                    /* Skip removal of this service unless we are forced to */
                    if (force) {
                        ctx.info("Forcing removal");
                    } else {
                        ctx.info("Service not removed. You can force removal of"
                                 + " a service by running 'remove' with the "
                                 +"-f option");
                        continue;
                    }
                }

                /* Remove the service */
                ctx.prompt("Removing service '" + id + "' ... ");
                try {
                    client.removeService(id);
                } catch (ClientException e) {
                    ctx.info ("FAILED");
                    ctx.error(e.getMessage());
                    continue;
                }

                ctx.info("OK"); // Removed

            }

    }

}
