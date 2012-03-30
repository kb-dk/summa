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
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.api.InvalidServiceStateException;
import dk.statsbiblioteket.summa.control.client.Client;
import dk.statsbiblioteket.summa.control.bundle.BundleSpecBuilder;
import dk.statsbiblioteket.util.rpc.ConnectionManager;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.io.ByteArrayInputStream;

/**
 * A {@link Command} to list the services deployed in a {@link Client}.
 * Used in {@link ClientShell}.
 */
public class ServicesCommand extends RemoteCommand<ClientConnection> {

    private String clientAddress;

    public ServicesCommand(ConnectionManager<ClientConnection> connMgr,
                           String clientAddress) {
        super("services", "List and query all deployed services", connMgr);
        this.clientAddress = clientAddress;

        installOption("s", "status", false,
                      "Include service status for each service");
        installOption("b", "bundle", false,
                      "Display bundle version for each service");
        installOption("f", "formatted", false, "Strictly formatted output for" +
                                               "machine parsing. Format is: " +
                                               "'serviceId | bundleId | statusCode | message'");

    }

    public void invoke(ShellContext ctx) throws Exception {
        ClientConnection client = getConnection(clientAddress);

        Layout layout = new Layout("Service");

        try {
            List<String> services = client.getServices();
            boolean listStatus = hasOption("status");
            boolean listBundle = hasOption("bundle");

            if (hasOption("formatted")) {
                layout.setPrintHeaders(false);
                layout.setDelimiter(" | ");
                // Always show all rows in strict format
                layout.appendColumns("Bundle", "StatusCode","Message");
            } else {
                if (listBundle) {
                    layout.appendColumns("Bundle");
                }
                if (listStatus) {
                    layout.appendColumns("StatusCode","Message");
                }
            }

            /* List services sorted alphabetically */
            SortedSet<String> sortedServices = new TreeSet<String>(services);
            for (String service : sortedServices) {
                String bundleId = "", statusCode = "", statusString = "";
                if (listBundle) {
                    try {
                        String bdlSpec = client.getBundleSpec(service);
                        BundleSpecBuilder spec = BundleSpecBuilder.open(
                                  new ByteArrayInputStream(bdlSpec.getBytes()));
                        bundleId = spec.getBundleId();
                    } catch (Exception e) {
                        bundleId = "Unknown";
                    }
                }

                if (listStatus) {
                    try {
                        Status status = client.getServiceStatus(service);
                        statusString = "" + status;
                        statusCode = status.getCode().toString();
                    } catch (InvalidServiceStateException e) {
                        statusString = "Not running";
                        statusCode = Status.CODE.not_instantiated.toString();
                    } catch (Exception e) {
                        statusString = "Connection error";
                        statusCode = Status.CODE.crashed.toString();
                        client.reportError(service);
                    }
                }

                layout.appendRow("Service", service,
                                 "Bundle", bundleId,
                                 "StatusCode", statusCode,
                                 "Message", statusString);

            }
            ctx.info(layout.toString());
        } finally {
            releaseConnection();
        }
    }
}




