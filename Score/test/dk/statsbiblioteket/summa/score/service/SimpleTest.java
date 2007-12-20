/* $Id: SimpleTest.java,v 1.5 2007/10/29 14:38:15 mke Exp $
 * $Revision: 1.5 $
 * $Date: 2007/10/29 14:38:15 $
 * $Author: mke $
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
package dk.statsbiblioteket.summa.score.service;

import dk.statsbiblioteket.summa.score.api.Status;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.RemoteException;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class SimpleTest extends ServiceBase {

    private class ActualService implements Runnable {

        public boolean stopped = true;

        public void run() {
            stopped = false;
            log.info ("service started");
            while (!stopped) {
                try {
                    Thread.sleep(2000);
                    log.info ("service running... ");
                } catch (InterruptedException e) {
                    log.info ("service interrupted!");
                    stopped = true;
                }
            }
            log.info ("service stopped");
        }
    }

    private Status status;
    private ActualService service;

    public SimpleTest (Configuration conf) throws RemoteException {
        super(conf);

        setStatus(Status.CODE.constructed, "Created SimpleTest object",
                  Logging.LogLevel.DEBUG);

        service = new ActualService();

        exportRemoteInterfaces();

        setStatus(Status.CODE.constructed, "Remote interfaces up",
                  Logging.LogLevel.DEBUG);
    }

    public void start() throws RemoteException {
        System.out.println ("START");
        setStatus(Status.CODE.startingUp, "Starting SimpleTest service" + this,
                  Logging.LogLevel.DEBUG);

        if (service.stopped) {
            new Thread (service).start();
        } else {
            log.warn ("Trying to start service, but it is already running");
        }

        setStatusRunning("The service is running");
    }

    public void stop() throws RemoteException {
        System.out.println ("STOP");
        if (service.stopped) {
            log.warn ("Trying to stop service but it is already stopped");
            return;
        }

        setStatus(Status.CODE.stopping, "Shutting down SimpleTest service" + this,
                Logging.LogLevel.DEBUG);

        service.stopped = true;

        setStatus(Status.CODE.stopped, "SimpleTest service " + this + " down.",
                Logging.LogLevel.INFO);
    }
}
