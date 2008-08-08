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
package dk.statsbiblioteket.summa.control.service;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.storage.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.StorageBase;
import dk.statsbiblioteket.summa.storage.RecordAndNext;
import dk.statsbiblioteket.summa.storage.RecordIterator;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.RemoteException;
import java.util.List;
import java.io.IOException;

/**
 * Wrapper for Metadata Storage. The underlying type of storage (Lucene,
 * PostgreSQL, JavaDB, Fedora...) is specified in the bundle configuration.
 * </p><p>
 * The setup-configuration should conform to the standard defined by
 * {@link StorageFactory} and requirements specific for the chosen storage.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class StorageService extends ServiceBase {
    private Log log = LogFactory.getLog(StorageService.class);

    // TODO: This should probably be in a module separate from Control,
    // so that the Control is not dependend on the modules it can start.

    /**
     * The Storage implementation wrapped in this service.
     */
    private Storage storage;

    /**
     * Standard setup for Services. This does not start the Metadata Storage.
     * @param conf             the configuration to use.
     * @throws RemoteException if the setup could not be performed.
     */
    public StorageService(Configuration conf) throws IOException {
        super(conf);
        setStatus(Status.CODE.constructed, "Created StorageService object",
                  Logging.LogLevel.DEBUG);
        // TODO: It is bad style to store this. Find an alternative
        this.conf = conf;
        exportRemoteInterfaces();

        log.trace ("Creating storage instance");
        storage = StorageFactory.createStorage(conf);

        setStatus(Status.CODE.constructed, "Remote interfaces are up",
                  Logging.LogLevel.DEBUG);
    }

    private boolean stopped = true;
    private Configuration conf; // Is it okay to keep this? (NO -- mke)
    public void start() throws RemoteException {
        if (!stopped) {
            log.warn("start called when already running");
            return;
        }
        setStatus(Status.CODE.startingUp, "Starting StorageServiceThread",
                  Logging.LogLevel.INFO);
        stopped = false;
        log.info ("StorageService started");
        log.debug("Getting storage from StorageFactory");
        try {
            storage = StorageFactory.createStorage(conf);
        } catch (IOException t) {
            setStatus(Status.CODE.crashed,
                      "Crashed due to RemoteException when requesting storage",
                      Logging.LogLevel.FATAL, t);
            stopped = true;
            throw new RemoteException("Crashed during startup", t);
        } catch (Throwable t) {
            //noinspection DuplicateStringLiteralInspection
            setStatus(Status.CODE.crashed,
                      "Crashed due to unexpected Throwable when requesting "
                      + "storage", Logging.LogLevel.FATAL, t);
            stopped = true;
            throw new RemoteException("Crashed with throwable during startup",
                                      t);
        }
        setStatusRunning("The Storage service is running");
    }

    public void stop() throws RemoteException {
        log.trace("Recieved request to stop Storage service");
        if (stopped) {
            log.warn("Attempting to stop when not started");
            return;
        }
        try {
            setStatus(Status.CODE.stopping, "Stopping storage control",
                      Logging.LogLevel.DEBUG);
            storage.close();
            setStatus(Status.CODE.stopped,
                      "Storage control stopped successfully",
                      Logging.LogLevel.INFO);
        } catch (IOException e) {
            setStatus(Status.CODE.crashed,
                      "Storage control crashed while stopping",
                      Logging.LogLevel.ERROR, e);
            throw new RemoteException("RemoteException when closing storage "
                                      + "control", e);
        }
        setStatus(Status.CODE.stopped,
                  "StorageService down, all lights green, performing clean-up",
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
