package dk.statsbiblioteket.summa.control.client.shell;

import dk.statsbiblioteket.summa.control.api.*;
import dk.statsbiblioteket.summa.common.shell.RemoteCommand;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.util.DeferredSystemExit;
import dk.statsbiblioteket.util.rpc.ConnectionManager;

import java.io.IOException;

/**
 * A command for restarting a service
 */
public class RestartServiceCommand extends RemoteCommand<ClientConnection> {

    private ConnectionManager<ClientConnection> cm;
    private String clientAddress;

    public RestartServiceCommand(ConnectionManager<ClientConnection> cm,
                       String clientAddress) {
        super("restart", "Restart a deployed service", cm);

        installOption("k", "kill", false, "Kill the service instead of simply "
                                          +"stopping it. This will restart the "
                                          +"JVM of the service");

        setUsage("restart [options] <service_id>...");

        this.clientAddress = clientAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {

        boolean kill = hasOption("k");
        ClientConnection client;

        /* Kill whoever needs to be killed  */
        try {
             client = getConnection(clientAddress);

            if (getArguments().length == 0) {
                ctx.error("No service(s) specified");
                return;
            }

            for (String serviceId : getArguments()) {
                restartService(serviceId, client, ctx, kill);
            }

        } catch (Exception e) {
            connectionError(e);
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            releaseConnection();
        }

    }

    private void restartService(String serviceId, ClientConnection client,
                                ShellContext ctx, boolean kill)
                                      throws IOException, InterruptedException {
        ctx.prompt("Restarting '" + serviceId + "' ... ");
        try {
            // Stop or kill the service
            try {
                Service service = client.getServiceConnection(serviceId);
                if (kill) {
                    service.kill();
                    ctx.prompt("Killed ... ");
                } else {
                    service.stop();
                    ctx.prompt("Stopped ... ");
                }
            } catch (InvalidServiceStateException e) {
                ctx.prompt("Not running ... ");
            }

            Status.CODE[] ignoreStatuses;
            int timeout = DeferredSystemExit.DEFAULT_DELAY/1000;
            if (kill) {
                // Wait for not_instantiated
                ignoreStatuses = new Status.CODE[]{
                                     Status.CODE.constructed,
                                     Status.CODE.crashed,
                                     Status.CODE.idle,
                                     Status.CODE.recovering,
                                     Status.CODE.running,
                                     Status.CODE.startingUp,
                                     Status.CODE.stopped,
                                     Status.CODE.stopping};
                timeout = timeout*2;
            } else {
                // Wait for not_instantiated, stopped, or crashed
                ignoreStatuses = new Status.CODE[]{
                                     Status.CODE.constructed,
                                     Status.CODE.idle,
                                     Status.CODE.recovering,
                                     Status.CODE.running,
                                     Status.CODE.startingUp,
                                     Status.CODE.stopping};
            }

            // Await the no_instantiated status
            StatusMonitor mon = new StatusMonitor(
                                     new ServiceConnectionFactory(clientAddress,
                                                                  client),
                                     serviceId, timeout, ctx, ignoreStatuses);
            mon.run();

            Status.CODE code = mon.getLastStatus().getCode();
            if (kill) {
                if (code != Status.CODE.not_instantiated) {
                    ctx.error(serviceId + " is still responding, reports: "
                              + mon.getLastStatus());
                    return;
                }
            } else {                
                if (code != Status.CODE.not_instantiated
                    && code != Status.CODE.stopped
                    && code != Status.CODE.crashed) {
                    ctx.error(serviceId + " has not stopped, reports: "
                              +mon.getLastStatus());
                    return;
                }
            }

            // Start the service(s)
            try {
                ctx.prompt("Starting ... ");
                client.startService(serviceId, null);
                mon = new StatusMonitor(
                                     new ServiceConnectionFactory(clientAddress,
                                                                  client),
                                     serviceId, 5, ctx,
                                     Status.CODE.not_instantiated);
                // Block until we have an answer
                mon.run();
                ctx.info(mon.getLastStatus() == null ?
                         "Timed out" : mon.getLastStatus().toString());
            } catch (Exception e) {
                client.reportError(serviceId);
                ctx.error("Failed: " + e.getMessage());
                throw new RuntimeException(e.getMessage(), e);
            }

        } catch (NoSuchServiceException e) {
            ctx.info ("No such service");
        }
    }
}
