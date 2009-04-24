package dk.statsbiblioteket.summa.control.server.shell;

import dk.statsbiblioteket.summa.control.api.ControlConnection;
import dk.statsbiblioteket.summa.control.api.rmi.ControlRMIConnection;
import dk.statsbiblioteket.summa.control.server.shell.StatusCommand;
import dk.statsbiblioteket.summa.common.shell.Core;
import dk.statsbiblioteket.summa.common.shell.Script;
import dk.statsbiblioteket.summa.common.rpc.SummaRMIConnectionFactory;
import dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.Arrays;

/**
 * Command line UI for managing the Control server.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke")
public class ControlShell {

    private ConnectionManager<ControlConnection> connManager;
    private Core shell;
    private String rmiAddress;

    public ControlShell(Configuration conf) throws Exception {
        shell = new Core ();
        shell.setPrompt ("control-shell> ");

        rmiAddress = conf.getString(ConnectionConsumer.CONF_RPC_TARGET,
                                    "//localhost:27000/summa-control");

        connManager = new ConnectionManager<ControlConnection> (
                         new GenericConnectionFactory<ControlConnection>(conf));

        shell.installCommand(new PingCommand(connManager, rmiAddress));
        shell.installCommand(new DeployCommand(connManager, rmiAddress));
        shell.installCommand(new RepositoryCommand(connManager, rmiAddress));
        shell.installCommand(new StartCommand(connManager, rmiAddress));
        shell.installCommand(new ClientsCommand(connManager, rmiAddress));
        shell.installCommand(new StatusCommand(connManager, rmiAddress));
        shell.installCommand(new ControlCommand(connManager, rmiAddress));
    }

    public int run (Script script) {
        int returnVal = shell.run(script);
        connManager.close();

        return returnVal;
    }

    public static void printUsage () {
        System.err.println ("USAGE:\n\tcontrol-shell [script commands]\n");
    }

    public static void main (String[] args) {
        Script script = null;
        Configuration conf = Configuration.getSystemConfiguration(true);

        if (args.length > 0) {
            script = new Script(args);
        }

        try {
            ControlShell shell = new ControlShell(conf);
            System.exit(shell.run(script));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}



