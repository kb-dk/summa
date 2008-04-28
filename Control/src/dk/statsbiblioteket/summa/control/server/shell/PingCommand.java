package dk.statsbiblioteket.summa.control.server.shell;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.control.api.ControlConnection;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Simple command to test the connection to the control server
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Unfinished")
public class PingCommand extends Command {

    private ConnectionManager<ControlConnection> cm;
    private String address;

    public PingCommand (ConnectionManager<ControlConnection> cm,
                        String controlAddress) {
        super("ping", "Test the connection to the Control server");
        this.cm = cm;
        this.address = controlAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {
        ConnectionContext<ControlConnection> connCtx = null;

        ctx.prompt ("Pinging Control server at '" + address + "'...");

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
