package dk.statsbiblioteket.summa.control.client.shell;

import dk.statsbiblioteket.summa.control.api.ControlConnection;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Created by IntelliJ IDEA. User: mikkel Date: Aug 5, 2008 Time: 8:13:20 AM To
 * change this template use File | Settings | File Templates.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Unfinished")
public class PingCommand extends Command {

    private ConnectionManager<ClientConnection> cm;
    private String address;

    public PingCommand (ConnectionManager<ClientConnection> cm,
                        String controlAddress) {
        super("ping", "Test the connection to the client server");
        this.cm = cm;
        this.address = controlAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {
        ConnectionContext<ClientConnection> connCtx = null;

        ctx.prompt ("Pinging client server at '" + address + "'...");

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
