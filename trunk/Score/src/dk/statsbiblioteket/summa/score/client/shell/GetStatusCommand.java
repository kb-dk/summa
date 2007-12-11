/* $Id: GetStatusCommand.java,v 1.2 2007/10/04 13:28:18 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:18 $
 * $Author: te $
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
package dk.statsbiblioteket.summa.score.client.shell;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.score.client.Client;
import dk.statsbiblioteket.summa.score.api.Service;
import dk.statsbiblioteket.summa.score.api.ClientConnection;
import dk.statsbiblioteket.summa.score.api.Status;

/**
 * A {@link Command} for deploying a {@link Service} via a {@link ClientShell}.
 */
public class GetStatusCommand extends Command {

    ClientConnection client;

    public GetStatusCommand(ClientConnection client) {
        super("get-status", "print the status of the score client");
        this.client = client;

    }

    public void invoke(ShellContext ctx) throws Exception {
        Status status = client.getStatus();
        ctx.info(status.toString());
    }
}
