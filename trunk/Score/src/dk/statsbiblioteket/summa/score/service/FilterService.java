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
package dk.statsbiblioteket.summa.score.service;

import java.rmi.RemoteException;

import dk.statsbiblioteket.summa.storage.StorageFactory;
import dk.statsbiblioteket.summa.storage.io.Control;
import dk.statsbiblioteket.summa.storage.io.Access;
import dk.statsbiblioteket.summa.common.filter.FilterControl;
import dk.statsbiblioteket.summa.common.filter.FilterChainHandler;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.score.api.Status;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Wrapper for FilterControl. The setup of the concrete Filter chain handled by
 * the FilterControl is specified in the bundle configuration.
 *
 * The setup-configuration should conform to the standard defined by
 * {@link FilterControl} and requirements specific for the chosen filters.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FilterService extends ServiceBase implements FilterChainHandler {
    private Log log = LogFactory.getLog(FilterService.class);

    /**
     * The FilterControl wrapped in this service.
     */
    private FilterControl filterControl;

    /**
     * Standard setup for Services. This does not start the pump in
     * FilterControl.
     * @param conf             the configuration to use.
     * @throws RemoteException if the setup could not be performed.
     */
    public FilterService(Configuration conf) throws RemoteException {
        super(conf);
        setStatus(Status.CODE.constructed, "Created FilterService object",
                  Logging.LogLevel.DEBUG);
        filterControl = new FilterControl(conf) {
            protected void finishedCallback() {
                switch (getStatus()) {
                    case error:
                        setStatus(Status.CODE.crashed, "Crashed",
                                  Logging.LogLevel.ERROR);
                        break;
                    case ready:
                    case stopping:
                    case stopped:
                        setStatus(Status.CODE.stopped, "Stopped with substatus "
                                                       + getStatus(),
                                  Logging.LogLevel.DEBUG);
                        break;
                    default:
                        setStatus(Status.CODE.stopped,
                                  "Stopped with unknown state " + getStatus(),
                                  Logging.LogLevel.WARN);
                }
            }
        };
        exportRemoteInterfaces();

        setStatus(Status.CODE.constructed, "Remote interfaces are up",
                  Logging.LogLevel.DEBUG);
    }

    public void start() throws RemoteException {
        if (filterControl.isRunning()) {
            log.warn("start called when already running");
            return;
        }
        setStatus(Status.CODE.startingUp, "Starting FilterControl",
                  Logging.LogLevel.INFO);
        try {
            filterControl.start();
        } catch (Throwable t) {
            //noinspection DuplicateStringLiteralInspection
            setStatus(Status.CODE.crashed,
                      "Crashed due to unexpected Throwable when starting "
                      + "FilterControl", Logging.LogLevel.FATAL, t);
            throw new RemoteException("Crashed with throwable during startup",
                                      t);
        }
        setStatusRunning("The FilterControl service is running");
    }

    public void stop() throws RemoteException {
        log.trace("Recieved request to stop FilterControl service");
        if (!filterControl.isRunning()) {
            log.warn("Attempting to stop FilterControl when not started");
            return;
        }
        try {
            setStatus(Status.CODE.stopping, "Stopping FilterControl service",
                      Logging.LogLevel.DEBUG);
            filterControl.stop();
            setStatus(Status.CODE.stopped,
                      "Filtercontrol stopped successfully",
                      Logging.LogLevel.INFO);
        } catch (Throwable t) {
            setStatus(Status.CODE.crashed,
                      "FilterControl crashed while stopping",
                      Logging.LogLevel.ERROR, t);
            throw new RemoteException("Throwable when stopping FilterControl ",
                                      t);
        }
        setStatus(Status.CODE.stopped,
                  "Filtercontrol service down, all lights green, performing "
                  + "clean-up",
                  Logging.LogLevel.INFO);
        try {
            unexportRemoteInterfaces();
            log.info("Clean-up finished. Calling System.exit");
        } catch (RemoteException e) {
            log.warn("Failed to unexpose remote interfaces upon stop", e);
        }

        // Do we really need to do this? It cleans up any stray threads, yes,
        // but isn't that the responsibility of the StorageServiceThread?
        // TODO: Consider if System.exit is needed upon stop
        try {
            System.exit(0);
        } catch (SecurityException e) {
            log.warn("System.exit disabled");
        }
    }



}
