/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.control.server.shell;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory;
import dk.statsbiblioteket.summa.common.shell.Core;
import dk.statsbiblioteket.summa.common.shell.Script;
import dk.statsbiblioteket.summa.control.api.ControlConnection;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.rpc.ConnectionManager;

/**
 * Command line UI for managing the Control server.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_OK,
        author = "mke")
public class ControlShell {
    private ConnectionManager<ControlConnection> connManager;
    private Core shell;
    // The core shell object, used by this client shell.
    private String rmiAddress;

    /** Default value for {@link ConnectionConsumer#CONF_RPC_TARGET}. */
    public static final String DEFAULT_RPC_TARGET =
                                              "//localhost:27000/summa-control";

    /**
     * Constructs a Control shell, instantiate the core and install the commands
     * for this.
     * @param conf The configuration use for creation of this class.
     * @throws Exception If error occur while creating a
     * {@link ConnectionManager}.
     */
    public ControlShell(Configuration conf) throws Exception {
        shell = new Core();
        shell.setPrompt("control-shell> ");

        rmiAddress = conf.getString(ConnectionConsumer.CONF_RPC_TARGET,
                                    DEFAULT_RPC_TARGET);

        connManager = new ConnectionManager<ControlConnection>(
                         new GenericConnectionFactory<ControlConnection>(conf));
        installCommands();
    }

    /**
     * Private helper method for installing commands.
     */
    private void installCommands() {
        shell.installCommand(new PingCommand(connManager, rmiAddress));
        shell.installCommand(new DeployCommand(connManager, rmiAddress));
        shell.installCommand(new RepositoryCommand(connManager, rmiAddress));
        shell.installCommand(new StartCommand(connManager, rmiAddress));
        shell.installCommand(new StopCommand(connManager, rmiAddress));
        shell.installCommand(new ClientsCommand(connManager, rmiAddress));
        shell.installCommand(new StatusCommand(connManager, rmiAddress));
        shell.installCommand(new ServicesCommand(connManager, rmiAddress));
        shell.installCommand(new ControlCommand(connManager, rmiAddress));
    }
    
    /**
     * Run the given {@link dk.statsbiblioteket.summa.common.shell.Script}.
     * @param script The script to run.
     * @return return value of the script. 0 means that all commands are
     * executed without error, a non-zero value means some error occurred.
     */
    public int run(Script script) {
        int returnVal = shell.run(script);
        connManager.close();

        return returnVal;
    }

    /**
     * Print usage to {@link System#err}.
     */
    public static void printUsage() {
        System.err.println("USAGE:\n\tcontrol-shell [script commands]\n");
    }

    /**
     * Main method for the control shell, used to start the control shell from
     * a commanline. Arguments are optional
     * @param args Optional commandline arguments, if any these will be
     * translated into one or more commands and executed. 
     */
    public static void main(String[] args) {
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