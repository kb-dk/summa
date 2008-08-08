/* $Id: StorageBase.java,v 1.9 2007/12/04 09:08:19 te Exp $
 * $Revision: 1.9 $
 * $Date: 2007/12/04 09:08:19 $
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
package dk.statsbiblioteket.summa.storage;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.WritableStorage;
import dk.statsbiblioteket.summa.storage.rmi.RemoteStorage;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * StorageBase is an abstract class, which implements both the Storage and Schedulable interface.
 * There is no choice of storage in StorageBase. This choice is made in the subclasses.
 * Created by IntelliJ IDEA. User: hal. Date: Jan 9, 2006.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal, te")
public abstract class StorageBase extends UnicastRemoteObject
                                  implements RemoteStorage {
    private static Log log = LogFactory.getLog(StorageBase.class);

    public StorageBase(Configuration conf) throws IOException {
        super (getServicePort(conf));

        log.trace("Exporting Storage interface");
        RemoteHelper.exportRemoteInterface(this,
                                           conf.getInt(Storage.DEFAULT_REGISTRY_PORT, 27000),
                                           "summa-storage");

        try {
            RemoteHelper.exportMBean(this);
        } catch (Exception e) {
            log.warn ("Failed to register MBean, going on without it. "
                      + "Error was", e);
        }
    }

    /**
     * Create the storage base with an empty configuration. This means that
     * default values will be used throughout.
     */
    public StorageBase() throws IOException {
        this(Configuration.newMemoryBased());
    }

    private static int getServicePort(Configuration configuration) {
        return configuration.getInt(Storage.DEFAULT_SERVICE_PORT, 27027);
    }

    /**
     * Default implementation that uses {@link #next(Long)} to create the list
     * of RecordAndNext. As this happens server-side, this should be fast
     * enough.
     */
    public List<RecordAndNext> next(Long iteratorKey, int maxRecords) throws
                                                               RemoteException {
        List<RecordAndNext> records = new ArrayList<RecordAndNext>(maxRecords);
        int added = 0;
        while (added++ < maxRecords) {
            RecordAndNext ran = next(iteratorKey);
            records.add(ran);
            if (!ran.getNext()) {
                break;
            }
        }
        return records;
    }

    protected void updateMultiVolume(Record record) {
        log.warn("updateMultiVolume not implemented yet");
        // TODO: Implement multi-volume as described in the pseudo-code
        /* Pseudo-code for new or modified (self = new or modified record):
        if parent exists
          mark self as not indexable
          add self to parent-children
          touch parent upwards (to trigger update)
        foreach child
          mark child as not indexable
          set self as child parent
        foreach child in each record
          if child equals self
            touch record

        Idea: Keep a cache of ghost-childs (child-id's without corresponding
              record) and their Record-id, for faster lookup.

        Pseudo-code for deleted:
          if parent exists
            touch parent upwards
          foreach child
            mark child as indexable
         */
    }

    /**
     * Is a Record is active, it should be indexed. This method returns
     * !DELETED & INDEXABLE.
     */
    public boolean recordActive(String id) throws RemoteException {
        Record record = getRecord(id);
        return record != null && !record.isDeleted() && record.isIndexable();
    }

    public boolean recordExists(String name) throws RemoteException {
        return getRecord(name) != null;
    }

    /**
     * <p>Convenience implementation of {@link WritableStorage#flushAll}
     * simply iterating through the list and calling
     * {@link WritableStorage#flush} on each record.</p>
     *
     * <p>Subclasses of {@code StorageBase} may choose to overwrite this method
     * for optimization purposes.</p>
     *
     * @param records the records to store or update
     * @throws IOException on comminication errors
     */
    public void flushAll (List<Record> records) throws IOException {
        for (Record rec : records) {
            flush(rec);
        }
    }
}
