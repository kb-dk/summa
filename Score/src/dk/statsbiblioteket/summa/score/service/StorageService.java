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

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.score.api.Status;
import dk.statsbiblioteket.summa.storage.StorageFactory;
import dk.statsbiblioteket.summa.storage.io.Access;
import dk.statsbiblioteket.summa.storage.io.Control;
import dk.statsbiblioteket.summa.storage.io.RecordAndNext;
import dk.statsbiblioteket.summa.storage.io.RecordIterator;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Wrapper for Metadata Storage. The underlying type of storage (Lucene,
 * PostgreSQL, JavaDB, Fedora...) is specified in the bundle configuration.
 *
 * The setup-configuration should conform to the standard defined by
 * {@link StorageFactory} and requirements specific for the chosen storage.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class StorageService extends ServiceBase implements Access {
    private Log log = LogFactory.getLog(StorageService.class);

    /**
     * The amount of ms that we will wait for the StorageServiceThread to
     * properly shut down, before forcing a shutdown.
     */
    private static final int SHUTDOWN_TIMEOUT = 10 * 1000;

    /**
     * The Storage implementation wrapped in this service.
     */
    private Control storageControl;

    /**
     * A simple thread that waits for a stop-signal, upon which it shuts down
     * the Metadata Storage.
     */
    // TODO: No need for threading here, so discard it
    private class StorageServiceThread extends Thread  {
        private Log log = LogFactory.getLog(StorageServiceThread.class);

        public boolean stopped = true;
        private Configuration conf; // Is it okay to keep this?

        public StorageServiceThread(Configuration conf) {
            this.conf = conf;
        }

        public void run() {
            stopped = false;
            log.info ("Service started");
            log.debug("Getting storage from StorageFactory");
            try {
                storageControl = StorageFactory.createController(conf);
            } catch (RemoteException t) {
                setStatus(Status.CODE.crashed,
                          "Crashed due to RemoteException when requesting "
                          + "storage", Logging.LogLevel.FATAL, t);
                stopped = true;
                return;
            } catch (Throwable t) {
                setStatus(Status.CODE.crashed,
                          "Crashed due to unexpected Throwable when requesting "
                          + "storage", Logging.LogLevel.FATAL, t);
                stopped = true;
                return;
            }
            setStatusRunning("The service is running");
            while (!stopped) {
                try {
                    Thread.sleep(2000);
                    log.trace("Service still running...");
                } catch (InterruptedException e) {
                    log.debug("Service interrupted!");
                    stopped = true;
                }
            }
            try {
                setStatus(Status.CODE.stopping, "Stopping storage control",
                          Logging.LogLevel.DEBUG);
                storageControl.close();
                setStatus(Status.CODE.stopped,
                          "Storage control stopped successfully",
                          Logging.LogLevel.INFO);
            } catch (RemoteException e) {
                setStatus(Status.CODE.crashed,
                          "Storage control crashed while stopping",
                          Logging.LogLevel.ERROR, e);
                log.error("RemoteException when closing storage control", e);
            }
        }
    }

    private StorageServiceThread service;
    /**
     * Standard setup for Services. This does not start the Metadata Storage.
     * @param conf             the configuration to use.
     * @throws RemoteException if the setup could not be performed.
     */
    public StorageService(Configuration conf) throws RemoteException {
        super(conf);
        setStatus(Status.CODE.constructed, "Created StorageService object",
                  Logging.LogLevel.DEBUG);

        service = new StorageServiceThread(conf);
        exportRemoteInterfaces();

        setStatus(Status.CODE.constructed, "Remote interfaces up",
                  Logging.LogLevel.DEBUG);
    }

    public void start() throws RemoteException {
        setStatus(Status.CODE.startingUp, "Starting StorageServiceThread",
                  Logging.LogLevel.INFO);

        if (service.stopped) {
            new Thread(service).start();
            // TODO: Wait for proper start before returning
        } else {
            log.warn("Trying to start service, but it is already running");
        }
    }

    public void stop() throws RemoteException {
        log.trace("Recieved request to stop.");
        if (service.stopped) {
            log.warn ("Trying to stop the service but it is already stopped");
            return;
        }

        setStatus(Status.CODE.stopping, "Shutting down StorageServicethread",
                Logging.LogLevel.DEBUG);
        service.stopped = true;

        try {
            service.join(SHUTDOWN_TIMEOUT);
        } catch (InterruptedException e) {
            setStatus(Status.CODE.crashed,
                      "Interrupted while waiting for the StorageServiceThread "
                      + "to shut down properly",
                      Logging.LogLevel.ERROR, e);
        }
        if (service.isAlive()) {
            log.error("A shutdown of StorageServiceThread has been attempted, "
                      + "but was unsuccessful. Shutdown will now be forced with"
                      + " System.exit");
            System.exit(-1);
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
        System.exit(0);
    }

    /* Interface send-through for Access */

    public RecordIterator getRecords(String base) throws RemoteException {
        return storageControl.getRecords(base);
    }

    public RecordIterator getRecordsModifiedAfter(long time, String base)
            throws RemoteException {
        return storageControl.getRecordsModifiedAfter(time, base);
    }

    public RecordIterator getRecordsFrom(String id, String base)
            throws RemoteException {
        return storageControl.getRecordsFrom(id, base);
    }

    public Record getRecord(String id) throws RemoteException {
        return storageControl.getRecord(id);
    }

    public boolean recordExists(String id) throws RemoteException {
        return storageControl.recordExists(id);
    }

    public boolean recordActive(String id) throws RemoteException {
        return storageControl.recordActive(id);
    }

    public RecordAndNext next(Long iteratorKey) throws RemoteException {
        return storageControl.next(iteratorKey);
    }

    public List<RecordAndNext> next(Long iteratorKey, int maxRecords)
            throws RemoteException {
        return storageControl.next(iteratorKey, maxRecords);
    }

    public void flush(Record record) throws RemoteException {
        storageControl.flush(record);
    }
}
