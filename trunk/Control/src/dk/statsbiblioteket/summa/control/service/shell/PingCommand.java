package dk.statsbiblioteket.summa.control.service.shell;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.control.api.ControlConnection;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;

/**
 * Try to establish a connection to a service.
 */
public class PingCommand extends Command {

    private ConnectionManager<Service> cm;
    private String address;

    public PingCommand (ConnectionManager<Service> cm,
                        String serviceAddress) {
        super("ping", "Test the connection to the service");
        this.cm = cm;
        this.address = serviceAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {
        ConnectionContext<Service> connCtx = null;

        ctx.prompt ("Pinging service at '" + address + "'...");

        try {
            connCtx = cm.get (address);
        } catch (Exception e){
            ctx.error ("Failed to connect to '" + address + "'. Error was: "
                       + e.getMessage());
            throw new RuntimeException("Failed to connect to '" + address + "'",
                                       e);
        } finally {
            if (connCtx != null) {
                cm.release (connCtx);
                ctx.info("OK");
            } else {
                ctx.error ("Failed to connect, unknown error");
            }
        }


    }

}



