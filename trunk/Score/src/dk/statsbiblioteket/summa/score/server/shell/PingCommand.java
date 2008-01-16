package dk.statsbiblioteket.summa.score.server.shell;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.score.api.ScoreConnection;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Simple command to test the connection to the score server
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Unfinished")
public class PingCommand extends Command {

    private ConnectionManager<ScoreConnection> cm;
    private String address;

    public PingCommand (ConnectionManager<ScoreConnection> cm,
                        String scoreAddress) {
        super("ping", "Test the connection to the Score server");
        this.cm = cm;
        this.address = scoreAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {
        ConnectionContext<ScoreConnection> connCtx = null;

        ctx.prompt ("Pinging Score server at '" + address + "'...");

        try {
            connCtx = cm.get (address);
        } catch (Exception e){
            ctx.error ("Failed to connect to '" + address + "'. Error was:\n\t" 
                       + Strings.getStackTrace(e));
        } finally {
            if (connCtx != null) {
                cm.release (connCtx);
                ctx.info("OK");
            } else {
                ctx.error ("Failed to connect");
            }
        }


    }
}
