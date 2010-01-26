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

import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.StatusMonitor;
import dk.statsbiblioteket.summa.control.api.ServiceConnectionFactory;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.common.shell.RemoteCommand;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link dk.statsbiblioteket.summa.common.shell.Command} used for waiting for
 * a service to stop.
 */
public class WaitCommand extends RemoteCommand<ClientConnection> {
    private Log log = LogFactory.getLog(StartServiceCommand.class);

    private String clientAddress;

    public WaitCommand(ConnectionManager<ClientConnection> connMgr,
                         String clientAddress) {
        super("wait", "Wait for a service to stop", connMgr);
        this.clientAddress = clientAddress;

        setUsage("wait [options] <service-id>");

        installOption("t", "timeout", true, "Number of seconds to wait. " +
                                            "Default timeout is 24h");
    }

    public void invoke(ShellContext ctx) throws Exception {
        if (getArguments().length != 1) {
            ctx.error("Only one service id may be specified, found "
                      + getArguments().length);
            return;
        }

        String serviceId = getArguments()[0];
        ClientConnection client = getConnection(clientAddress);

        // Wait for not_instantiated, stopped, or crashed
        Status.CODE[] ignoreStatuses = new Status.CODE[]{
                Status.CODE.constructed,
                Status.CODE.idle,
                Status.CODE.recovering,
                Status.CODE.running,
                Status.CODE.startingUp,
                Status.CODE.stopping
        };

        int timeout = 60*60*24; // 24h
        if (hasOption("timeout")) {
            timeout = Integer.parseInt(getOption("timeout"));
        }

        ctx.prompt("Waiting for '" + serviceId + "' to stop (timeout "
                   +timeout+"s) ... ");

        StatusMonitor mon = new StatusMonitor(
                new ServiceConnectionFactory(clientAddress, client),
                                             serviceId, timeout,
                                             null, ignoreStatuses);
        mon.run();

        if (mon.isTimedOut()) {
            ctx.warn("Timed out");
            log.info("Timed out when waiting for stop on " + serviceId);
        } else {
            ctx.info(mon.getLastStatus().toString());
        }

    }
}

