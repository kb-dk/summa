package dk.statsbiblioteket.summa.control.client.shell;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.shell.RemoteCommand;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.ClientException;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.client.Client;
import dk.statsbiblioteket.util.rpc.ConnectionManager;

/**
 * A shell command to launch a {@link Service} deployed in a {@link Client}.
 */
public class StopServiceCommand extends RemoteCommand<ClientConnection> {

    private String clientAddress;

    public StopServiceCommand(ConnectionManager<ClientConnection> connMgr,
                              String clientAddress) {
        super("stop", "Stop a service given by id", connMgr);
        this.clientAddress = clientAddress;

        setUsage("stop <service-id> [service-id] ...");
    }

    public void invoke(ShellContext ctx) throws Exception {

        if (getArguments().length == 0) {
            ctx.error ("At least one package id should be specified.");
            return;
        }

        ClientConnection client = getConnection(clientAddress);

        try {
            for (String id : getArguments()) {

                ctx.prompt ("Stopping service '" + id + "' ... ");

                try {
                    client.stopService(id);
                } catch (ClientException e){
                    // A ClientException is a controlled exception, we don't print
                    // the whole stack trace
                    ctx.info ("FAILED");
                    ctx.error(e.getMessage());
                    continue;
                } catch (Exception e) {
                    throw new RuntimeException ("Stopping of service failed: "
                                                + e.getMessage(), e);
                }

                ctx.info("OK");
            }
        } finally {
            releaseConnection();
        }
    }
}
