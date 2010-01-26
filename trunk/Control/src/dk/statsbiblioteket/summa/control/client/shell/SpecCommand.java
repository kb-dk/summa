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
import dk.statsbiblioteket.summa.control.api.NoSuchServiceException;
import dk.statsbiblioteket.summa.control.bundle.BundleSpecBuilder;
import dk.statsbiblioteket.summa.common.shell.RemoteCommand;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.util.rpc.ConnectionManager;

import java.io.ByteArrayInputStream;
import java.util.Map;

import java.io.Serializable;

/**
 * {@link dk.statsbiblioteket.summa.common.shell.Command} implementation
 * for the client shell to print out the contents of either the client's
 * bundle spec, or the bundle spec of a deployed service
 */
public class SpecCommand extends RemoteCommand<ClientConnection> {

    private String clientAddress;

    public SpecCommand(ConnectionManager<ClientConnection> connMgr,
                         String clientAddress) {
        super("spec", "Print the bundle spec of the client or a deployed "
                      + "service", connMgr);

        setUsage("spec [options] [service_id]...");

        installOption("p", "pretty", false, "Pretty print the spec contents");
        installOption("f", "files", false, "Whether to print the file list in "
                                           + "the pretty-print mode");

        this.clientAddress = clientAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {
        ClientConnection client = getConnection(clientAddress);

        String[] services = getArguments();

        try {
            if (services.length == 0) {
                String spec = client.getBundleSpec(null);
                ctx.info("Client spec:");
                if (hasOption("pretty")) {
                    prettyPrint(spec, ctx);
                } else {
                    ctx.info(spec);
                }
            } else {
                for (String service : services) {
                    ctx.info("Service spec for '"+service+"':");
                    String spec;
                    try {
                        spec = client.getBundleSpec(service);
                        if (hasOption("pretty")) {
                            prettyPrint(spec, ctx);
                        } else {
                            ctx.info(spec);
                        }
                    } catch (NoSuchServiceException e) {
                        ctx.error("No such service '"+service+"'");
                    }
                }
            }
        } finally {
            releaseConnection();
        }
    }

    private void prettyPrint(String spec, ShellContext ctx) {
        BundleSpecBuilder builder = BundleSpecBuilder.open(
                                     new ByteArrayInputStream(spec.getBytes()));

        String msg = builder.getDisplayString (hasOption("files"));
        ctx.info(msg);
    }
}

