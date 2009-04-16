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
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.api.StatusMonitor;
import dk.statsbiblioteket.summa.control.client.Client;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.SocketException;

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
                                                  + "Url, file, or RMI server."
                                                  + " Defaults to "
                                                  + "'configuration.xml'");
    }

    public void invoke(ShellContext ctx) throws Exception {
        String confLocation = getOption("c");
        log.debug("invoke called with confLocation '" + confLocation + "'");
        if (confLocation == null) {
            confLocation = "configuration.xml";
            log.debug("confLocation for invoke sat to " + confLocation);
        }

        if (getArguments().length != 1) {
            ctx.error ("Exactly one package id should be specified. Found "
                       + getArguments().length + ".");
            return;
        }

        String pkgId = getArguments()[0];

        ctx.prompt ("Starting service '" + pkgId + "' "
                    + (confLocation != null ?
                                 " with configuration " + confLocation
                               : " with no configuration ")
                    + "... ");

        ClientConnection client = getConnection(clientAddress);

        String baseMeta = "client '" + client.getId()+ "' with pkgId '"
                          + pkgId + "' and confLocation '" + confLocation
                          + "' in invoke";
        try {

            log.debug("calling startService for " + baseMeta);
            client.startService(pkgId, confLocation);
            log.debug("Attaching ServiceMonitor to " + baseMeta);
            StatusMonitor mon = new StatusMonitor(client.getServiceConnection(pkgId),
                                                  5, ctx,
                                                  Status.CODE.not_instantiated);
            Thread monThread = new Thread (mon, "ServiceStatusMonitor");
            monThread.setDaemon (true); // Allow the JVM to exit
            monThread.start();
        } catch (Exception e) {
            connectionError(e);
            throw new RuntimeException ("Start of service failed: "
                                        + e.getMessage(), e);
        } finally {
            releaseConnection();
        }
        log.debug("End if invoke for " + baseMeta);
        ctx.info("OK");
    }
}



