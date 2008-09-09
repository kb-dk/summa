package dk.statsbiblioteket.summa.control.client.shell;

import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.shell.RemoteCommand;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.NoSuchServiceException;
import dk.statsbiblioteket.summa.control.api.InvalidServiceStateException;
import dk.statsbiblioteket.util.rpc.ConnectionManager;

/**
 * Send a kill command to the client or a service under the client
 */
public class KillCommand extends RemoteCommand<ClientConnection> {

    private ConnectionManager<ClientConnection> cm;
    private String clientAddress;

    public KillCommand(ConnectionManager<ClientConnection> cm,
                       String clientAddress) {
        super("kill", "Kill the client's or a service's JVM", cm);

        setUsage("kill [service_id]...");

        this.clientAddress = clientAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {

        /* Kill whoever needs to be killed  */
        try {
            ClientConnection client = getConnection(clientAddress);

            if (getArguments().length != 0) {
                ctx.info ("Killing service(s):");
                for (String serviceId : getArguments()) {
                    try {
                        Service service = client.getServiceConnection(serviceId);
                        ctx.prompt ("\t" + serviceId + "  ... ");
                        service.kill();
                        ctx.info("Killed");

                        /* Tell the client to reset all connections to
                         * the service */
                        client.reportError(serviceId);
                    } catch (NoSuchServiceException e) {
                        ctx.info ("\t" + serviceId + "  No such service");
                    } catch (InvalidServiceStateException e) {
                        ctx.info ("\t" + serviceId + "  Not running");
                    }
                }
            } else {
                // No services specified kill the client
                ctx.prompt ("Killing client '" + client.getId() + "'... ");
                client.stop ();
                connectionError("Client killed. Connection reset");
                ctx.info ("OK");
            }
        } catch (Exception e) {
            connectionError(e);
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            releaseConnection();
        }
    }

}