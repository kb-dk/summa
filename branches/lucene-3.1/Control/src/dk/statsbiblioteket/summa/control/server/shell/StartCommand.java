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
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;
import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.control.api.*;
import dk.statsbiblioteket.summa.control.api.feedback.rmi.RemoteConsoleFeedback;
import dk.statsbiblioteket.summa.control.api.feedback.rmi.RemoteFeedback;
import dk.statsbiblioteket.summa.control.server.ControlUtils;
import dk.statsbiblioteket.summa.control.ClientConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * Control shell command for starting a Client.
 */
public class StartCommand extends Command {
    private Log log = LogFactory.getLog(StartCommand.class);

    private ConnectionManager<ControlConnection> cm;
    private String controlAddress;
    private String hostname;

    public StartCommand (ConnectionManager<ControlConnection> cm,
                         String controlAddress) {
        super ("start", "Start a client instance");

        setUsage("start <client>");

        installOption ("t", "transport", true, "Which deployment transport to"
                                             + " use. Allowed values are 'ssh'."
                                             + "Default is ssh");

        installOption ("c", "configuration", true,
                       "Url, RMI address or file path where the client can"
                       + " find its configuration. Default points at the"
                       + " Control configuration server");

        this.cm = cm;
        this.controlAddress = controlAddress;

        hostname = RemoteHelper.getHostname();
    }

    public void invoke(ShellContext ctx) throws Exception {
        /* Extract and validate arguments */
        log.debug("Invoking StartCommand");
        String[] args = getArguments();
        if (args.length != 1) {
            ctx.error("You must provide exactly 1 argument. Found "
                      + args.length);
            return;
        }

        String instanceId = args[0];

        if (isClientRunning(instanceId)) {
            ctx.info("Client '" + instanceId + "' is already running");
            return;
        }

        String transport = getOption("t") != null ? getOption("t") : "ssh";
        transport = ControlUtils.getDeployerClassName(transport);

        String confLocation = getOption("c"); // This is allowed to be unset
                                              // - see ClientDeployer#CONF_CLIENT_CONF

        /* Set up a configuration for the startClient request */
        Configuration conf =
                Configuration.newMemoryBased(
                        ClientDeployer.CONF_CLIENT_CONF,
                        confLocation,
                        ClientDeployer.CONF_INSTANCE_ID,
                        instanceId,
                        ClientDeployer.CONF_DEPLOYER_CLASS,
                        transport);

        log.trace("Configuration initialized");
        /* Connect to the Control and send the deployment request */
        ctx.prompt ("Starting client '" + instanceId + "' using "
                    + "transport '" + transport + "'... ");
        ConnectionContext<ControlConnection> connCtx = null;

        try {
            log.trace("Looking up a Control connection on: " + controlAddress);
            connCtx = cm.get(controlAddress);
            if (connCtx == null) {
                ctx.error("Failed to connect to Control server at '"
                           + controlAddress + "'");
                return;
            }

            ControlConnection control = connCtx.getConnection();
            log.debug("Starting client '" + instanceId + "'");
            control.startClient(conf);

            // Wait for the service to leave the not_instantiated status
            StatusMonitor mon = new StatusMonitor(
                                     new ClientConnectionFactory(controlAddress,
                                                                 ctx),
                                     instanceId, 5, ctx,
                                     Status.CODE.not_instantiated);

            // Block until the Client's state leaves not_instantiated
            mon.run();

            log.trace("All OK");
            ctx.info("OK: " + mon.getLastStatus());
        } finally {
            if (connCtx != null) {
                cm.release (connCtx);
            }
        }

    }

    private boolean isClientRunning(String instanceId) {
        ConnectionContext<ControlConnection> connCtx = cm.get(controlAddress);
        ControlConnection control = connCtx.getConnection();
        try {
            ClientConnection client = control.getClient(instanceId);

            if (client != null) {
                log.debug("Client is already running");
                return true;
            }
        } catch (InvalidClientStateException e){
            return false;
        } catch (IOException e) {
            throw new InvalidClientStateException(instanceId,
                                                  "Connection to '" + instanceId
                                                  + "' is broken: "
                                                  + e.getMessage(), e);
        } finally {
            connCtx.unref();
        }
        return false;
    }
}




