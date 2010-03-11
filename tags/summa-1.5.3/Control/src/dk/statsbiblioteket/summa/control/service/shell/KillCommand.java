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
package dk.statsbiblioteket.summa.control.service.shell;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;

/**
 * Send a kill command to the service
 */
public class KillCommand extends Command {

    private ConnectionManager<Service> cm;
    private String address;

    public KillCommand(ConnectionManager<Service> cm,
                        String serviceAddress) {
        super("kill", "Kill the services JVM");
        this.cm = cm;
        this.address = serviceAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {
        ConnectionContext<Service> connCtx = null;

        /* Get a connection */
        try {
            connCtx = cm.get (address);
        } catch (Exception e){
            ctx.error ("Failed to connect to '" + address + "'. Error was: "
                       + e.getMessage());
            throw new RuntimeException("Failed to connect to '" + address + "'",
                                       e);
        }

        /* Kill the service  */
        try {
            Service service = connCtx.getConnection();
            String id = service.getId();
            service.kill();
            ctx.info("Killed service'" + id + "'");
        } catch (Exception e) {
            cm.reportError(connCtx, e);
            connCtx = null;
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (connCtx != null) {
                cm.release (connCtx);
            } else {
                ctx.error ("Failed to connect, unknown error");
            }
        }
    }

}




