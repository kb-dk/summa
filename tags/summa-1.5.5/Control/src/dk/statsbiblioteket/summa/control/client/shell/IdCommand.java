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
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;

/**
 * Print the client id
 */
public class IdCommand extends RemoteCommand<ClientConnection> {

    private ConnectionManager<ClientConnection> cm;
    private String clientAddress;

    public IdCommand(ConnectionManager<ClientConnection> cm,
                        String clientAddress) {
        super("id", "Print the instance id of the client", cm);
        this.clientAddress = clientAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {
        ClientConnection client = getConnection(clientAddress);

        /* Get and print the client id  */
        try {
            String id = client.getId();
            ctx.info(id);
        } finally {
            releaseConnection();
        }
    }

}



