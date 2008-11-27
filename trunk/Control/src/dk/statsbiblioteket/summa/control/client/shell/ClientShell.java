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
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.client.Client;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.qa.QAInfo;

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

    public ClientShell (String rmiAddress) throws Exception {
        shell = new Core ();
        shell.setPrompt ("client-shell> ");

        Configuration conf = Configuration.getSystemConfiguration(true);

        connManager = new ConnectionManager<ClientConnection> (
                          new GenericConnectionFactory<ClientConnection>(conf));

        shell.getShellContext().info ("Looking up client on " + rmiAddress);
        ConnectionContext<ClientConnection> ctx = connManager.get(rmiAddress);
        client = ctx.getConnection();
        connManager.release (ctx);

        shell.installCommand(new StatusCommand(connManager, rmiAddress));
        shell.installCommand(new DeployCommand(connManager, rmiAddress));
        shell.installCommand(new StartServiceCommand(connManager, rmiAddress));
        shell.installCommand(new StopServiceCommand(connManager, rmiAddress));
        shell.installCommand(new ServicesCommand(connManager, rmiAddress));
        shell.installCommand(new PingCommand(connManager, rmiAddress));
        shell.installCommand(new IdCommand(connManager, rmiAddress));
        shell.installCommand(new RepositoryCommand(connManager, rmiAddress));
        shell.installCommand(new KillCommand(connManager, rmiAddress));
        shell.installCommand(new RemoveServiceCommand(connManager, rmiAddress));
        shell.installCommand(new SpecCommand(connManager, rmiAddress));
    }

    public int run (Script script) {
        int returnVal = shell.run(script);
        connManager.close();

        return returnVal;
    }

    public static void printUsage () {
        System.err.println ("USAGE:\n\tclient-shell <client-address> "
                            + "[script commands]\n");
        System.err.println ("For example:\n\tclient-shell //localhost:27000/c2");
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



