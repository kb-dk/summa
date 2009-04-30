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
import dk.statsbiblioteket.summa.control.api.*;
import dk.statsbiblioteket.util.rpc.ConnectionManager;

import java.io.IOException;

/**
 * A {@link Command} for deploying a {@link Service} via a {@link ClientShell}.
 */
public class DeployCommand extends RemoteCommand<ClientConnection> {

    ClientConnection client;
    private String clientAddress;
    private RemoveServiceCommand removeCommand;
    private RestartServiceCommand restartCommand;

    public DeployCommand(ConnectionManager<ClientConnection> connMgr,
                         String clientAddress) {
        super("deploy", "Deploy or upgrade a service given its bundle id",
              connMgr);
        this.clientAddress = clientAddress;

        removeCommand = new RemoveServiceCommand(connMgr, clientAddress);
        restartCommand = new RestartServiceCommand(connMgr, clientAddress);

        setUsage ("deploy [options] <bundle-id> <instanceId>");

        installOption("c", "configuration", true, "The location of the "
                                                  + "configuration to use."
                                                  + " Defaults to "
                                                  + "'configuration.xml'");

        installOption("u", "upgrade", false, "Upgrade an existing bundle and "
                                             + "restart it, if it is already "
                                             + "running");
    }

    public void invoke(ShellContext ctx) throws Exception {
        String confLocation = getOption("c");
        if (confLocation == null) {
            confLocation = "configuration.xml";
        }

        boolean upgrade = hasOption("u");

        if (getArguments().length != 2) {
            ctx.error ("Exactly two arguments should be specified. Found "
                       + getArguments().length + ".");
            return;
        }

        String bundleId = getArguments()[0];
        String instanceId = getArguments()[1];
        ClientConnection client = getConnection(clientAddress);

        try {
            if (upgrade){
                upgradeService(client, ctx, confLocation, bundleId, instanceId);
            } else {
                deployService(client, ctx, confLocation, bundleId, instanceId);
            }
        } finally {
            releaseConnection();
        }
    }

    private void upgradeService(ClientConnection client,
                                ShellContext ctx, String confLocation,
                                String bundleId, String instanceId)
                                                            throws IOException {

        boolean restart = false;

        try {
            Status status = client.getServiceStatus(instanceId);
            if (status.getCode() != Status.CODE.not_instantiated) {
                restart = true;
            }
        } catch (InvalidServiceStateException e) {
            // Service is not running
            restart = false;
        }

        removeCommand.removeService(client, ctx,
                                    new String[]{instanceId}, true);

        deployService(client, ctx, confLocation, bundleId, instanceId);

        if (restart) {
            restartCommand.restartService(instanceId, client, ctx, true);
        } else {
            try {
                Status status = client.getServiceStatus(instanceId);

                if (status.getCode() != Status.CODE.not_instantiated) {
                    ctx.warn("Service '" + instanceId + "' has unexpectedly " +
                             "started up during upgrade, reporting: "
                             + status);
                }
            } catch (InvalidServiceStateException e) {
                // Expected, service is not running
            } catch (NoSuchServiceException e) {
                ctx.error("The service '" + instanceId + " was not deployed "
                          + "cleanly after removal");
            }
        }
    }

    private void deployService(ClientConnection client,
                               ShellContext ctx, String confLocation,
                               String bundleId, String instanceId)
                                                            throws IOException {
        ctx.prompt ("Deploying bundle '" + bundleId + "' as instance "
                    + "'" + instanceId + "'"
                    + " with configuration '" + confLocation + "'"
                    + "... ");

        client.deployService(bundleId, instanceId, confLocation);
        ctx.info("OK");
    }


}



