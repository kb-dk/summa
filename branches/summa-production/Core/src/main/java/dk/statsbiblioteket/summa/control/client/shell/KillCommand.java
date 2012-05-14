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

import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.shell.RemoteCommand;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.NoSuchServiceException;
import dk.statsbiblioteket.summa.control.api.InvalidServiceStateException;
import dk.statsbiblioteket.util.rpc.ConnectionManager;

/**
 * Send a kill command to the client or a service under the client
 */
public class KillCommand extends RemoteCommand<ClientConnection> {

    private ConnectionManager<ClientConnection> cm;
    private String clientAddress;

    public KillCommand(ConnectionManager<ClientConnection> cm,
                       String clientAddress) {
        super("kill", "Kill the client's or a service's JVM", cm);

        setUsage("kill [service_id]...");

        this.clientAddress = clientAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {

        /* Kill whoever needs to be killed  */
        try {
            ClientConnection client = getConnection(clientAddress);

            if (getArguments().length != 0) {
                ctx.info ("Killing service(s):");
                for (String serviceId : getArguments()) {
                    try {
                        Service service = client.getServiceConnection(serviceId);
                        ctx.prompt ("\t" + serviceId + "  ... ");
                        service.kill();
                        ctx.info("Killed");

                        /* Tell the client to reset all connections to
                         * the service */
                        client.reportError(serviceId);
                    } catch (NoSuchServiceException e) {
                        ctx.info ("\t" + serviceId + "  No such service");
                    } catch (InvalidServiceStateException e) {
                        ctx.info ("\t" + serviceId + "  Not running");
                    }
                }
            } else {
                // No services specified kill the client
                ctx.prompt ("Killing client '" + client.getId() + "'... ");
                client.stop ();
                connectionError("Client killed. Connection reset");
                ctx.info ("OK");
            }
        } catch (Exception e) {
            connectionError(e);
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            releaseConnection();
        }
    }

}



