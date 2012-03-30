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
package dk.statsbiblioteket.summa.control;

import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.ControlConnection;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.shell.VoidShellContext;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper class exposing a {@link ConnectionFactory} that establish
 * {@link ClientConnection}s by proxying through a {@link ControlConnection}.
 * <p/>
 * The key point is that this class only need the client id and not the
 * whole RMI address to look up a client
 */
public class ClientConnectionFactory
        extends ConnectionFactory<ClientConnection> {

    private static final Log log = LogFactory.getLog(
                                                 ClientConnectionFactory.class);

    private ShellContext ctx;
    private ConnectionFactory<ControlConnection> controlConnFact;
    private String controlAddress;

    public ClientConnectionFactory (Configuration conf,
                                    ShellContext ctx) {
        this.ctx = ctx != null ? ctx : new VoidShellContext();
        controlConnFact =
                new GenericConnectionFactory<ControlConnection>(conf);
        controlAddress = conf.getString(ConnectionConsumer.CONF_RPC_TARGET,
                                        "//localhost:27000/summa-control");
    }

    public ClientConnectionFactory (String controlAddress,
                                    ShellContext ctx) {
        this.ctx = ctx != null ? ctx : new VoidShellContext();
        controlConnFact =
                new GenericConnectionFactory<ControlConnection>(
                                                Configuration.newMemoryBased());
        this.controlAddress = controlAddress;
    }

    public ClientConnection createConnection(String connectionId) {
        log.debug("Looking up Control server at " + controlAddress + " ... ");
        ControlConnection control =
                controlConnFact.createConnection(controlAddress);

        if (control == null) {
            ctx.error("No connection to Control");
            return null;
        } else {
            try {
                log.debug("Control reports "
                          + control.getStatus().toString());
            } catch (IOException e) {
                // Yeah we eat the stack trace. Shoot me
                ctx.error("Connection error: " + e.getMessage());
                return null;
            }
        }

        ctx.prompt(".");
        for (int retries = 0; retries < getNumRetries(); retries++) {
            try {
                ClientConnection client = control.getClient(connectionId);
                ctx.prompt(".");

                if (client == null) {
                    try {
                        Thread.sleep(getGraceTime());
                    } catch (InterruptedException e) {
                        String msg = "Interrupted while waiting for client "
                                     + connectionId;
                        log.warn(msg);
                        ctx.warn(msg);
                    }
                    continue;
                }

                try {
                    Status status = client.getStatus();
                    log.debug("Client reports " + status);
                } catch (Exception e) {
                    log.debug("Error requesting status from " + connectionId
                              + ": " + e.getMessage(), e);
                    try {
                        Thread.sleep(getGraceTime());
                    } catch (InterruptedException ee) {
                        String msg = "Interrupted while waiting for client "
                                     + connectionId;
                        log.warn(msg);
                        ctx.warn(msg);
                    }
                    continue;
                }


                // Do a 'noia check that the client ids match up
                String clientId = client.getId();
                if (!connectionId.equals(clientId)) {
                    ctx.warn("Client reports id '" + clientId
                             + "'. Expected '" + connectionId + "'");
                }

                return client;

            } catch (IOException e) {
                // Yeah we eat the stack trace (again). Shoot me
                log.debug("Connection error, retrying: " + e.getMessage(), e);
            }
        }

        log.info("Giving up on client '" + connectionId + "'");
        return null;
    }
}

