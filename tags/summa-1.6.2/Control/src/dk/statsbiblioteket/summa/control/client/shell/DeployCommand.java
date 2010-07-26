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




