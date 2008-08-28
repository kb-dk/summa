package dk.statsbiblioteket.summa.control.server.shell;

import dk.statsbiblioteket.summa.control.api.ControlConnection;
import dk.statsbiblioteket.summa.control.api.rmi.ControlRMIConnection;
import dk.statsbiblioteket.summa.control.server.shell.StatusCommand;
import dk.statsbiblioteket.summa.common.shell.Core;
import dk.statsbiblioteket.summa.common.rpc.SummaRMIConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Command line UI for managing the Control server.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Unfinished")
public class ControlShell {

    private ConnectionManager<ControlConnection> connManager;
    private Core shell;

    public ControlShell(String rmiAddress) throws Exception {
        shell = new Core ();
        shell.setPrompt ("control-shell> ");

        connManager = new ConnectionManager<ControlConnection> (
                                    new SummaRMIConnectionFactory<ControlRMIConnection>(null));

        shell.installCommand(new PingCommand(connManager, rmiAddress));
        shell.installCommand(new DeployCommand(connManager, rmiAddress));
        shell.installCommand(new RepositoryCommand(connManager, rmiAddress));
        shell.installCommand(new StartCommand(connManager, rmiAddress));
        shell.installCommand(new ClientsCommand(connManager, rmiAddress));
        shell.installCommand(new StatusCommand(connManager, rmiAddress));
    }

    public void run () {
        // FIXME pass command line args to shell core
        shell.run(new String[0]);
        connManager.close();
    }

    public static void printUsage () {
        System.err.println ("USAGE:\n\tcontrol-shell <control-rmi-address>\n");
        System.err.println ("For example:\n\tcontrol-shell //localhost:2768/summa-control");
    }

    public static void main (String[] args) {
        try {
            if (args.length != 1) {
            printUsage ();
            System.exit (1);
        }

        ControlShell shell = new ControlShell(args[0]);
        shell.run ();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
