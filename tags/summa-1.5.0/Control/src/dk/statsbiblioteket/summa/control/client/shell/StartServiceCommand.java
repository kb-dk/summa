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
import dk.statsbiblioteket.summa.control.api.*;
import dk.statsbiblioteket.summa.control.client.Client;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A shell command to launch a {@link Service} deployed in a {@link Client}.
 */
public class StartServiceCommand extends RemoteCommand<ClientConnection> {
    private Log log = LogFactory.getLog(StartServiceCommand.class);

    private String clientAddress;

    public StartServiceCommand(ConnectionManager<ClientConnection> connMgr,
                         String clientAddress) {
        super("start", "Start a service given by id", connMgr);
        this.clientAddress = clientAddress;

        setUsage("start [options] <service-id>");

        installOption("c", "configuration", true, "Location of configuration. "
                                                  + "Url, file, or RMI server");
        installOption("w", "wait", false, "Wait for the service to stop before "
                                          + "returning from this command");
    }

    public void invoke(ShellContext ctx) throws Exception {
        String confLocation = getOption("c");
        log.debug("Invoke called with confLocation '" + confLocation + "'");        

        if (getArguments().length != 1) {
            ctx.error ("Exactly one package id should be specified. Found "
                       + getArguments().length + ".");
            return;
        }

        String pkgId = getArguments()[0];

        ctx.prompt ("Starting service '" + pkgId + "' "
                    + (confLocation != null ?
                                 " with configuration " + confLocation
                               : " with default configuration ")
                    + "... ");

        ClientConnection client = getConnection(clientAddress);

        String baseMeta = "client '" + client.getId()+ "' with pkgId '"
                          + pkgId + "' and confLocation '" + confLocation
                          + "' in invoke";

        // Wait for the service to leave the not_instantiated status
        StatusMonitor mon = new StatusMonitor(
                                     new ServiceConnectionFactory(clientAddress,
                                                                  client),
                                     pkgId, 5, ctx,
                                     Status.CODE.not_instantiated);

        try {

            log.debug("calling startService for " + baseMeta);
            client.startService(pkgId, confLocation);
            log.debug("Attaching ServiceMonitor to " + baseMeta);

            // Block until we have a response
            mon.run();
        } catch (Exception e) {
            connectionError(e);
            throw new RuntimeException ("Start for service failed: "
                                        + e.getMessage(), e);
        } finally {
            releaseConnection();
        }

        if (mon.getLastStatus() == null) {
            ctx.warn("Timed out");
            log.info("End invoke for " + baseMeta + ", timed out");
            return;
        } else {
            ctx.info(mon.getLastStatus().toString());
        }

        if (hasOption("wait")) {
            ctx.prompt("Waiting for '" + pkgId + "' to stop ... ");

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
            mon = new StatusMonitor(
                                     new ServiceConnectionFactory(clientAddress,
                                                                  client),
                                     pkgId, timeout, null, ignoreStatuses);
            mon.run();

            if (mon.getLastStatus() == null) {
                ctx.warn("Timed out");
                log.info("End invoke for " + baseMeta + ", timed out when " +
                         "waiting for stop");
            } else {
                ctx.info(mon.getLastStatus().toString());
            }
        }
    }
}




