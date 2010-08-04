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
package dk.statsbiblioteket.summa.control.client.shell;

import dk.statsbiblioteket.summa.common.shell.Core;
import dk.statsbiblioteket.summa.common.shell.Script;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.ControlConnection;
import dk.statsbiblioteket.summa.control.client.Client;
import dk.statsbiblioteket.summa.control.ClientConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;

/**
 * <p>A simple shell for communicating with a {@link Client}.</p>
 *
 * <p>The shell is built using a generic shell {@link Core} from the
 * {@link dk.statsbiblioteket.summa.common.shell} package.</p>
 *
 * <p>It can be run from the command line and can communicate with
 * any Client local or remotely via the configured RPC mechanism.</p>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_OK,
        author = "mke")
public class ClientShell {

    private ConnectionManager<ClientConnection> connManager;
    private ClientConnection client;
    // The core shell object, used by this client shell.
    private Core shell;

    /**
     * Constructs a {@link ClientShell} instance, sets up connection to the
     * client, instantiate the shell core, and install client commands.
     *
     * @param target Target client. This is the Id for the client to connect to.
     * @throws Exception If unable to connect to client.
     */
    public ClientShell(String target) throws Exception {
        shell = new Core();
        shell.setPrompt("client-shell> ");

        Configuration conf = Configuration.getSystemConfiguration(true);

        ConnectionFactory<ClientConnection> connFact;

        /**
         * If 'target' looks like an RMI address try that. Else try looking up
         * a client connection via the control server 
         */
        if (target.startsWith("//")) {
            connFact = new GenericConnectionFactory<ClientConnection>(conf);
        } else {
            connFact = new ClientConnectionFactory(conf, null);
        }

        connManager = new ConnectionManager<ClientConnection>(connFact);

        shell.getShellContext().info("Looking up client '" + target + "'");
        ConnectionContext<ClientConnection> ctx = connManager.get(target);
        client = ctx.getConnection();
        connManager.release(ctx);


        if (client == null) {
            throw new IOException("Unable to connect to client: " + target);
        }
        installCommands(target);
    }

    /**
     * Private helper method for installing commands
     * @param target Target client.
     */
    private void installCommands(String target) {
        // Instal client commands
        shell.installCommand(new StatusCommand(connManager, target));
        shell.installCommand(new DeployCommand(connManager, target));
        shell.installCommand(new StartServiceCommand(connManager, target));
        shell.installCommand(new StopServiceCommand(connManager, target));
        shell.installCommand(new ServicesCommand(connManager, target));
        shell.installCommand(new PingCommand(connManager, target));
        shell.installCommand(new IdCommand(connManager, target));
        shell.installCommand(new RepositoryCommand(connManager, target));
        shell.installCommand(new KillCommand(connManager, target));
        shell.installCommand(new RestartServiceCommand(connManager, target));
        shell.installCommand(new RemoveServiceCommand(connManager, target));
        shell.installCommand(new SpecCommand(connManager, target));
        shell.installCommand(new WaitCommand(connManager, target));
    }

    /**
     * Look up a {@link ClientConnection} given its instanceId.
     * This is done by looking it up via the Control server.
     * @param conf Configuration used to look up the address of the
     *             Control server.
     * @param target Instance id of the client.
     * @return a connection to the client.
     */
    private ClientConnection getClientConnection(Configuration conf,
                                                 String target,
                                                 ShellContext ctx)
                                                            throws IOException {
        ConnectionFactory<ControlConnection> connFact =
                         new GenericConnectionFactory<ControlConnection>(conf);
        String controlAddress = conf.getString(ConnectionConsumer.CONF_RPC_TARGET,
                                               "//localhost:27000/summa-control");

        ctx.prompt("Looking up Control server at " + controlAddress + " ... ");
        ControlConnection control = connFact.createConnection(controlAddress);

        if (control == null) {
            ctx.error("No connection to Control");
            return null;
        } else {
            ctx.info("Control reports " + control.getStatus().toString());
        }

        ctx.prompt("Looking up client '"
                   + target + "' via Control server ... ");
        ClientConnection client = control.getClient(target);

        if (client == null) {
            ctx.error("No connection to Client '" + target + "'");
            return null;
        } else {
            ctx.info("Client reports " + client.getStatus().toString());
        }

        // Do a 'noia check that the client ids match up
        String clientId = client.getId();
        if (!target.equals(clientId)) {
            ctx.warn("Client reports id '" + clientId
                     + "'. Expected '" + target + "'");
        }

        return client; 
    }

    /**
     * Run the supplied script.
     * @param script The script to run.
     * @return The return value of the scripts. 0 means no error occur executing
     * the script, a non-zero value means some error occured.
     */
    public int run(Script script) {
        int returnVal = shell.run(script);
        connManager.close();

        return returnVal;
    }

    /**
     * Prints the usage of the client shell to {@link System#err}.
     */
    public static void printUsage() {
        System.err.println("USAGE:\n\tclient-shell <instanceId|client-address> "
                           + "[script commands]\n");
        System.err.println("For example:\n\tclient-shell //localhost:27000/c2\n"
                           + "Or:\n\tclient-shell client-1 status");
    }

    /**
     * Main method, used to start Client shell as a standalone shell.
     * @param args Commandline arguments. The first argument is needed and
     * specify the client Id. Any arguments after this is optional and are
     * translated into a possible series of commands and executed. 
     */
    public static void main(String[] args) {
        Script script = null;

        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        if (args.length > 1) {
            script = new Script(args, 1);
        }

        try {
            ClientShell shell = new ClientShell(args[0]);
            System.exit(shell.run(script));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
