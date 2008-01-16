package dk.statsbiblioteket.summa.score.server.shell;

import dk.statsbiblioteket.summa.score.api.ClientConnection;
import dk.statsbiblioteket.summa.score.api.ScoreConnection;
import dk.statsbiblioteket.summa.score.client.ClientRMIConnection;
import dk.statsbiblioteket.summa.score.client.shell.*;
import dk.statsbiblioteket.summa.score.server.ScoreRMIConnection;
import dk.statsbiblioteket.summa.common.shell.Core;
import dk.statsbiblioteket.summa.common.rpc.SummaRMIConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Command line UI for managing the Score server.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Unfinished")
public class ScoreShell {

    private ConnectionManager<ScoreConnection> connManager;
    private Core shell;

    public ScoreShell (String rmiAddress) throws Exception {
        shell = new Core ();
        shell.setPrompt ("score-shell> ");

        connManager = new ConnectionManager<ScoreConnection> (
                                    new SummaRMIConnectionFactory<ScoreRMIConnection>(null));

        shell.installCommand(new PingCommand(connManager, rmiAddress));
        shell.installCommand(new DeployCommand(connManager, rmiAddress));

    }

    public void run () {
        // FIXME pass command line args to shell core
        shell.run(new String[0]);
        connManager.close();
    }

    public static void printUsage () {
        System.err.println ("USAGE:\n\tscore-shell <score-rmi-address>\n");
        System.err.println ("For example:\n\tscore-shell //localhost:2768/summa-score");
    }

    public static void main (String[] args) {
        try {
            if (args.length != 1) {
            printUsage ();
            System.exit (1);
        }

        ScoreShell shell = new ScoreShell (args[0]);
        shell.run ();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
