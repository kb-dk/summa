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
import dk.statsbiblioteket.summa.common.shell.Layout;
import dk.statsbiblioteket.summa.control.api.*;
import dk.statsbiblioteket.util.rpc.ConnectionManager;

/**
 * A {@link Command} for deploying a {@link Service} via a {@link ClientShell}.
 */
public class StatusCommand extends RemoteCommand<ClientConnection> {

    private String clientAddress;

    public StatusCommand(ConnectionManager<ClientConnection> connMgr,
                         String clientAddress) {
        super("status", "Print the status of the control client, or query the " +
              "status of deployed services",
              connMgr);

        installOption("f", "formatted", false, "Strictly formatted output for" +
                                               "machine parsing. Line format " +
                                               "is: " +
                                               "'serviceId | statusCode | message'");

        setUsage("status [service_id]...");

        this.clientAddress = clientAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {
        ClientConnection client = getConnection(clientAddress);
        Layout layout = new Layout("Service");

        if (hasOption("formatted")) {
            // Always show all columns in formatted mode
            layout.appendColumns("StatusCode", "Message");
            layout.setPrintHeaders(false);
            layout.setDelimiter(" | ");
        } else {
            layout.appendColumns("Message");
        }

        String[] services = getArguments();

        try {
            if (services.length == 0) {
                Status status = client.getStatus();
                ctx.info("Client status: " + status.toString());
            } else {
                for (String service : services) {
                    String statusCode;
                    String msg;
                    try {
                        Status status = client.getServiceStatus(service);
                        statusCode = status.getCode().toString();
                        msg = "" + status;
                    } catch (InvalidServiceStateException e) {
                        statusCode = Status.CODE.not_instantiated.toString();
                        msg = "Not running";
                    } catch (NoSuchServiceException e) {
                        statusCode = Status.CODE.not_instantiated.toString();
                        msg = "No such service";
                    }
                    layout.appendRow("Service", service,
                                     "StatusCode", statusCode,
                                     "Message", msg);
                }

                ctx.info(layout.toString());
            }
        } finally {
            releaseConnection();
        }
    }
}




