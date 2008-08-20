package dk.statsbiblioteket.summa.control.service.shell;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;

/**
 * Send a kill command to the service
 */
public class KillCommand extends Command {

    private ConnectionManager<Service> cm;
    private String address;

    public KillCommand(ConnectionManager<Service> cm,
                        String serviceAddress) {
        super("kill", "Kill the services JVM");
        this.cm = cm;
        this.address = serviceAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {
        ConnectionContext<Service> connCtx = null;

        /* Get a connection */
        try {
            connCtx = cm.get (address);
        } catch (Exception e){
            ctx.error ("Failed to connect to '" + address + "'. Error was: "
                       + e.getMessage());
            throw new RuntimeException("Failed to connect to '" + address + "'",
                                       e);
        }

        /* Kill the service  */
        try {
            Service service = connCtx.getConnection();
            String id = service.getId();
            service.kill();
            ctx.info("Killed service'" + id + "'");
        } catch (Exception e) {
            cm.reportError(connCtx, e);
            connCtx = null;
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (connCtx != null) {
                cm.release (connCtx);
            } else {
                ctx.error ("Failed to connect, unknown error");
            }
        }
    }

}
