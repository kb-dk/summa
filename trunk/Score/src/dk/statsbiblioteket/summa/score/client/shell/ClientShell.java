/* $Id: ClientShell.java,v 1.4 2007/10/04 13:28:18 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/04 13:28:18 $
 * $Author: te $
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
package dk.statsbiblioteket.summa.score.client.shell;

import dk.statsbiblioteket.summa.common.shell.Core;
import dk.statsbiblioteket.summa.score.api.ClientConnection;

import java.rmi.Naming;

/**
 *
 */
public class ClientShell {

    private ClientConnection client;
    private Core shell;

    public ClientShell (String rmiAddress) throws Exception {
        shell = new Core ();
        shell.setPrompt ("score-client> ");

        shell.getShellContext().info ("Looking up client on " + rmiAddress);
        client = (ClientConnection) Naming.lookup (rmiAddress);

        shell.installCommand(new GetStatusCommand(client));
        shell.installCommand(new DeployCommand(client));
        shell.installCommand(new StartServiceCommand(client));
    }

    public void run () {
        // FIXME pass command line args to shell core
        shell.run(new String[0]);
    }

    public static void printUsage () {
        System.err.println ("USAGE:\n\tclient-shell <client-rmi-address>\n");
        System.err.println ("For example:\n\tclient-shell //localhost:2768/score-client-2");
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
