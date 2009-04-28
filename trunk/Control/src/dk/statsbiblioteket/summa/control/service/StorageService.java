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
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.api.InvalidServiceStateException;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.RemoteException;
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

        setStatus(Status.CODE.constructed, "Remote interfaces are up",
                  Logging.LogLevel.DEBUG);
    }

    private Configuration conf; // Is it okay to keep this? (NO -- mke)

    public void start() throws RemoteException {
        if (storage != null) {
            throw new InvalidServiceStateException(getClientId(), getId(),
                                                   "start", "Already running");
        }

        setStatus(Status.CODE.startingUp, "Starting StorageServiceThread",
                  Logging.LogLevel.INFO);
        log.info ("StorageService started");
        log.debug("Creating Storage instance");
        try {
            storage = StorageFactory.createStorage(conf);
        } catch (IOException t) {
            setStatus(Status.CODE.crashed,
                      "Crashed due to RemoteException when requesting storage",
                      Logging.LogLevel.FATAL, t);
            throw new RemoteException("Crashed during startup", t);
        } catch (Throwable t) {
            //noinspection DuplicateStringLiteralInspection
            setStatus(Status.CODE.crashed,
                      "Crashed due to unexpected Throwable when requesting "
                      + "storage", Logging.LogLevel.FATAL, t);
            throw new RemoteException("Crashed with throwable during startup",
                                      t);
        }
        setStatusRunning("The Storage service is running");
    }

    public void stop() throws RemoteException {
        log.trace("Recieved request to stop Storage service");
        if (storage == null) {
            throw new InvalidServiceStateException(getClientId(), getId(),
                                                   "stop", "Already stopped");
        }
        closeStorage();
        setStatus(Status.CODE.stopped,
                  "StorageService down, all lights green, performing clean-up",
                  Logging.LogLevel.INFO);
    }

    private void closeStorage() throws RemoteException {
        try {
            setStatus(Status.CODE.stopping, "Stopping storage control",
                      Logging.LogLevel.DEBUG);
            storage.close();
            storage = null;
            setStatus(Status.CODE.stopped,
                      "Storage control closed successfully",
                      Logging.LogLevel.INFO);
        } catch (IOException e) {
            setStatus(Status.CODE.crashed,
                      "Storage control crashed while closing",
                      Logging.LogLevel.ERROR, e);
            throw new RemoteException("Error closing storage "
                                      + "control", e);
        }
    }

    /**
     * @return the underlying Storage. Primarily used for testing purposes.
     */
    public Storage getStorage() {
        return storage;
    }
}



