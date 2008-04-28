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
import dk.statsbiblioteket.summa.common.rpc.SummaRMIConnectionFactory;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.client.Client;
import dk.statsbiblioteket.summa.control.client.ClientRMIConnection;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;

/**
 * <p>A simple shell for communicating with a {@link Client}.</p>
 *
 * <p>The shell is built using a generic shell {@link Core} from the
 * {@link dk.statsbiblioteket.summa.common.shell} package.</p>
 *
 * <p>It can be run from the command line and can communicate with
 * any Client local or remotely via RMI.</p>
 */
public class ClientShell {

    private ConnectionManager<ClientConnection> connManager;
    private ClientConnection client;
    private Core shell;

    public ClientShell (String rmiAddress) throws Exception {
        shell = new Core ();
        shell.setPrompt ("client-shell> ");

        connManager = new ConnectionManager<ClientConnection> (
                      new SummaRMIConnectionFactory<ClientRMIConnection>(null));

        // Although the client shell use a connection manager
        // it does not currently use stateless connections. That would
        // require the individual commands to manage their connections
        // which is easy with a ConnectionManager instance , but requires
        // a bit of work
        shell.getShellContext().info ("Looking up client on " + rmiAddress);
        ConnectionContext<ClientConnection> ctx = connManager.get(rmiAddress);
        client = ctx.getConnection();
        connManager.release (ctx);

        shell.installCommand(new GetStatusCommand(client));
        shell.installCommand(new DeployCommand(client));
        shell.installCommand(new StartServiceCommand(client));
        shell.installCommand(new StopServiceCommand(client));
        shell.installCommand(new ServicesCommand(client));
    }

    public void run () {
        // FIXME pass command line args to shell core
        shell.run(new String[0]);
        connManager.close();
    }

    public static void printUsage () {
        System.err.println ("USAGE:\n\tclient-shell <client-rmi-address>\n");
        System.err.println ("For example:\n\tclient-shell "
                            + "//localhost:2768/control-client-2");
    }

    public static void main (String[] args) {
        try {
            if (args.length != 1) {
            printUsage ();
            System.exit (1);
        }

        ClientShell shell = new ClientShell (args[0]);
        shell.run ();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
