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
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.api.InvalidServiceStateException;
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

        setUsage("status [service_id]...");

        this.clientAddress = clientAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {
        ClientConnection client = getConnection(clientAddress);

        String[] services = getArguments();

        try {
            if (services.length == 0) {
                Status status = client.getStatus();
                ctx.info("Client status: " + status.toString());
            } else {
                ctx.info ("Status of services:");
                for (String service : services) {
                    String status;
                    try {
                        status = "" + client.getServiceStatus(service);
                    } catch (InvalidServiceStateException e) {
                        status = "Not running";
                    }
                    ctx.info("\t" + service + ": " + status);
                }
            }
        } finally {
            releaseConnection();
        }
    }
}
