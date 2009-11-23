/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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



