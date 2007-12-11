/* $Id: DeployCommand.java,v 1.4 2007/10/04 13:28:18 te Exp $
 * $Revision: 1.4 $
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
import dk.statsbiblioteket.summa.score.api.ClientConnection;
import dk.statsbiblioteket.summa.score.api.Service;
import dk.statsbiblioteket.util.Strings;

/**
 * A {@link Command} for deploying a {@link Service} via a {@link ClientShell}.
 */
public class DeployCommand extends Command {

    ClientConnection client;

    public DeployCommand(ClientConnection client) {
        super("deploy", "Deploy a service given its bundle id");
        this.client = client;

        setUsage ("deploy [options] <service-id>");

        installOption("c", "configuration", true, "The location of the configuration to use");
    }

    public void invoke(ShellContext ctx) throws Exception {
        String confLocation = getOption("c");

        if (getArguments().length != 1) {
            ctx.error ("Exactly one package id should be specified. Found "
                       + getArguments().length + ".");
            return;
        }

        String pkgId = getArguments()[0];

        ctx.prompt ("Deploying package '" + pkgId + "' "
                    + (confLocation != null ?
                                 " with configuration " + confLocation
                               : " with no configuration ")
                    + "... ");

        try {
            client.deployService(pkgId, confLocation);
        } catch (Exception e) {
            ctx.info ("FAILED");
            ctx.error ("Deployment failed: " + Strings.getStackTrace(e));
            return;
        }

        ctx.info("OK");
    }

}
