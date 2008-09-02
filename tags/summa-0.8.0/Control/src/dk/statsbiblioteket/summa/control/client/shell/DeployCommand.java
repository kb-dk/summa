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

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.shell.RemoteCommand;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.util.rpc.ConnectionManager;

/**
 * A {@link Command} for deploying a {@link Service} via a {@link ClientShell}.
 */
public class DeployCommand extends RemoteCommand<ClientConnection> {

    ClientConnection client;
    private String clientAddress;

    public DeployCommand(ConnectionManager<ClientConnection> connMgr,
                         String clientAddress) {
        super("deploy", "Deploy a service given its bundle id", connMgr);
        this.clientAddress = clientAddress;

        setUsage ("deploy [options] <bundle-id> <instanceId>");

        installOption("c", "configuration", true, "The location of the "
                                                  + "configuration to use."
                                                  + " Defaults to "
                                                  + "'configuration.xml'");
    }

    public void invoke(ShellContext ctx) throws Exception {
        String confLocation = getOption("c");
        if (confLocation == null) {
            confLocation = "configuration.xml";
        }

        if (getArguments().length != 2) {
            ctx.error ("Exactly two arguments should be specified. Found "
                       + getArguments().length + ".");
            return;
        }

        String bundleId = getArguments()[0];
        String instanceId = getArguments()[1];

        ctx.prompt ("Deploying bundle '" + bundleId + "' as instance "
                    + "'" + instanceId + "'"
                    + " with configuration '" + confLocation + "'"
                    + "... ");


        ClientConnection client = getConnection(clientAddress);
        try {
            client.deployService(bundleId, instanceId, confLocation);
        } catch (Exception e) {
            throw new RuntimeException("Deployment failed: " + e.getMessage(),
                                       e);
        } finally {
            releaseConnection();
        }

        ctx.info("OK");
    }

}
