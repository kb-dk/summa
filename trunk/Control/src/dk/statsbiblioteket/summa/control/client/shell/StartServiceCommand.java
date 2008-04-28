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
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.client.Client;
import dk.statsbiblioteket.util.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A shell command to launch a {@link Service} deployed in a {@link Client}.
 */
public class StartServiceCommand extends Command {
    private Log log = LogFactory.getLog(StartServiceCommand.class);

    private class ServiceMonitor implements Runnable {

        private ClientConnection client;
        private String serviceId;
        private int timeout;
        private ShellContext ctx;

        /**
         * Print an error to the {@link ShellContext} if the specified
         * RMI server does not respond within {@code timeout} seconds.
         *
         * @param client The client controlling service to monitor
         * @param serviceId id of the service to monitor
         * @param timeout Number of seconds before the connection times out
         * @param ctx ShellContext to print to in case of errors
         */
        public ServiceMonitor (ClientConnection client,
                               String serviceId,
                               int timeout,
                               ShellContext ctx) {
            this.client = client;
            this.serviceId = serviceId;
            this.timeout = timeout;
            this.ctx = ctx;
        }

        public void run() {
            for (int tick = 0; tick < timeout; tick++) {

                try {
                    Thread.sleep (1000);
                } catch (InterruptedException e) {
                    // We should probably die if somebody interrupts us
                    return;
                }

                try {
                    Status s = client.getServiceStatus(serviceId);

                    if (Status.CODE.not_instantiated == s.getCode()) {
                        // Wait another interation
                        continue;
                    }

                    // If we reach this point we are good,
                    // and the monitor should die
                    ctx.debug ("Connection to service '" + serviceId
                             + "' up. Status: " + s);
                    return;
                } catch (Exception e) {
                    ctx.error ("Failed to ping service '" + serviceId
                             + "'. Error was:\n " + Strings.getStackTrace(e));
                }
            }
            ctx.error ("Service '" + serviceId + "' did not respond after "
                       + timeout + "s. It has probably crashed.");
        }
    }

    private ClientConnection client;

    public StartServiceCommand(ClientConnection client) {
        super("start", "Start a service given by id");
        this.client = client;

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

        String baseMeta = "client '" + client.getId()+ "' with pkgId '"
                          + pkgId + "' and confLocation '" + confLocation
                          + "' in invoke";
        try {

            log.debug("calling startService for " + baseMeta);
            client.startService(pkgId, confLocation);
            log.debug("Attaching ServiceMonitor to " + baseMeta);
            new Thread (new ServiceMonitor(client, pkgId, 5, ctx)).start();
        } catch (Exception e) {

            throw new RuntimeException ("Start of service failed: "
                                        + e.getMessage(), e);
        }
        log.debug("End if invoke for " + baseMeta);
        ctx.info("OK");
    }
}
