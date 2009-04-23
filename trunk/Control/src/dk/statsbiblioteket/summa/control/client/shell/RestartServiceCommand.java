package dk.statsbiblioteket.summa.control.client.shell;

import dk.statsbiblioteket.summa.control.api.*;
import dk.statsbiblioteket.summa.common.shell.RemoteCommand;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.util.DeferredSystemExit;
import dk.statsbiblioteket.util.rpc.ConnectionManager;

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

                    // Start the service(s)
                    // Wait the default JVM tear down gracetime
                    Thread.sleep(DeferredSystemExit.DEFAULT_DELAY);
                    try {
                        client.startService(serviceId, null);
                        StatusMonitor mon = new StatusMonitor(client.getServiceConnection(serviceId),
                                                              5, ctx,
                                                              Status.CODE.not_instantiated,
                                                              Status.CODE.stopped,
                                                              Status.CODE.stopping);
                        Thread monThread = new Thread (mon, "ServiceStatusMonitor");
                        monThread.setDaemon (true); // Allow the JVM to exit
                        monThread.start();
                        ctx.info("Started");
                    } catch (Exception e) {
                        client.reportError(serviceId);
                        ctx.error("Failed: " + e.getMessage());
                        throw new RuntimeException(e.getMessage(), e);
                    }

                } catch (NoSuchServiceException e) {
                    ctx.info ("No such service");
                }

            }

        } catch (Exception e) {
            connectionError(e);
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            releaseConnection();
        }

    }
}
