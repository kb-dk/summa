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
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.ClientException;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.client.Client;
import dk.statsbiblioteket.util.rpc.ConnectionManager;

/**
 * A shell command to launch a {@link Service} deployed in a {@link Client}.
 */
public class StopServiceCommand extends RemoteCommand<ClientConnection> {

    private String clientAddress;

    public StopServiceCommand(ConnectionManager<ClientConnection> connMgr,
                              String clientAddress) {
        super("stop", "Stop a service given by id", connMgr);
        this.clientAddress = clientAddress;

        setUsage("stop <service-id> [service-id] ...");
    }

    public void invoke(ShellContext ctx) throws Exception {

        if (getArguments().length == 0) {
            ctx.error ("At least one package id should be specified.");
            return;
        }

        ClientConnection client = getConnection(clientAddress);

        try {
            for (String id : getArguments()) {

                ctx.prompt ("Stopping service '" + id + "' ... ");

                try {
                    client.stopService(id);
                } catch (ClientException e){
                    // A ClientException is a controlled exception, we don't print
                    // the whole stack trace
                    ctx.info ("FAILED");
                    ctx.error(e.getMessage());
                    continue;
                } catch (Exception e) {
                    throw new RuntimeException ("Stopping of service failed: "
                                                + e.getMessage(), e);
                }

                ctx.info("OK");
            }
        } finally {
            releaseConnection();
        }
    }
}




