/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.control.client.shell;

import dk.statsbiblioteket.summa.common.shell.Core;
import dk.statsbiblioteket.summa.common.shell.Script;
import dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.ControlConnection;
import dk.statsbiblioteket.summa.control.client.Client;
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
        state = QAInfo.State.QA_NEEDED,
        author = "mke")
public class ClientShell {

    private ConnectionManager<ClientConnection> connManager;
    private ClientConnection client;
    private Core shell;

    public ClientShell (String target) throws Exception {
        shell = new Core ();
        shell.setPrompt ("client-shell> ");

        Configuration conf = Configuration.getSystemConfiguration(true);

        connManager = new ConnectionManager<ClientConnection> (
                          new GenericConnectionFactory<ClientConnection>(conf));

        /**
         * If 'target' looks like an RMI address try that. Else try looking up
         * a client connection via the control server 
         */
        if (target.startsWith("//")) {
            shell.getShellContext().info ("Looking up client on address "
                                          + target);
            ConnectionContext<ClientConnection> ctx = connManager.get(target);
            client = ctx.getConnection();
            connManager.release (ctx);
        } else {
            shell.getShellContext().info ("Looking up client "
                                          + target);
            client = getClientConnection(conf, target);
        }

        if (client == null) {
            throw new IOException("Unable to connect to client: " + target);
        }

        shell.installCommand(new StatusCommand(connManager, target));
        shell.installCommand(new DeployCommand(connManager, target));
        shell.installCommand(new StartServiceCommand(connManager, target));
        shell.installCommand(new StopServiceCommand(connManager, target));
        shell.installCommand(new ServicesCommand(connManager, target));
        shell.installCommand(new PingCommand(connManager, target));
        shell.installCommand(new IdCommand(connManager, target));
        shell.installCommand(new RepositoryCommand(connManager, target));
        shell.installCommand(new KillCommand(connManager, target));
        shell.installCommand(new RemoveServiceCommand(connManager, target));
        shell.installCommand(new SpecCommand(connManager, target));
    }

    /**
     * Look up a {@link ClientConnection} given its instanceId.
     * This is done by looking it up via the Control server.
     * @param conf configuration used to look up the address of the
     *             Control server
     * @param target instance id of the client
     * @return a connection to the client
     */
    private ClientConnection getClientConnection(Configuration conf,
                                                 String target)
                                                            throws IOException {
        ConnectionFactory<ControlConnection> connFact =
                         new GenericConnectionFactory<ControlConnection>(conf);
        String controlAddress = conf.getString(ConnectionConsumer.CONF_RPC_TARGET,
                                               "//localhost:27000/summa-control");
        ControlConnection control = connFact.createConnection(controlAddress);

        if (control == null) {
            return null;
        }

        return control.getClient(target);
    }

    public int run (Script script) {
        int returnVal = shell.run(script);
        connManager.close();

        return returnVal;
    }

    public static void printUsage () {
        System.err.println ("USAGE:\n\tclient-shell <instanceId|client-address> "
                            + "[script commands]\n");
        System.err.println ("For example:\n\tclient-shell //localhost:27000/c2\nOr:\n\tclient-shell client-1 status");
    }

    public static void main (String[] args) {
        Script script = null;

        if (args.length == 0) {
                printUsage ();
                System.exit (1);
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



