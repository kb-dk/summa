package dk.statsbiblioteket.summa.control.service.shell;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;

/**
 * Created by IntelliJ IDEA. User: mikkel Date: Aug 8, 2008 Time: 8:49:41 AM To
 * change this template use File | Settings | File Templates.
 */
public class StartCommand  extends Command {

    private ConnectionManager<Service> cm;
    private String address;

    public StartCommand (ConnectionManager<Service> cm,
                        String serviceAddress) {
        super("start", "Start the service");
        this.cm = cm;
        this.address = serviceAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {
        ConnectionContext<Service> connCtx = null;

        ctx.prompt ("Starting service at '" + address + "'...");

        /* Get a connection */
        try {
            connCtx = cm.get (address);
        } catch (Exception e){
            ctx.error ("Failed to connect to '" + address + "'. Error was: "
                       + e.getMessage());
            throw new RuntimeException("Failed to connect to '" + address + "'",
                                       e);
        }

        /* Start the service */
        try {
            Service service = connCtx.getConnection();
            service.start();
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




