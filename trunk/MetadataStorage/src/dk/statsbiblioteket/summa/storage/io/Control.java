/* $Id: Control.java,v 1.9 2007/12/04 09:08:19 te Exp $
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
package dk.statsbiblioteket.summa.storage.io;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.schedule.Schedulable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Control is an abstract class, which implements both the Access and Schedulable interface.
 * There is no choice of storage in Control. This choice is made in the subclasses.
 * Created by IntelliJ IDEA. User: hal. Date: Jan 9, 2006.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal, te")
public abstract class Control extends UnicastRemoteObject implements Access,
                                                                     Schedulable {
    private static Log log = LogFactory.getLog(Control.class);

    /**
     * Using the default constructor means that the Control will not be
     * exposed as a RMI service by Control itself.
     * @throws RemoteException in case of any error.
     */
    public Control() throws RemoteException {
        super();
    }

    /**
     * The recommended constructor as it allows for RMI exposure.
     * @param configuration
     * @throws RemoteException
     */
    public Control(Configuration configuration) throws RemoteException {

    }

    /**
     * Control constructor.
     * @param port serviceport
     * @throws RemoteException
     */
    public Control(int port) throws RemoteException{
        super(port);
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
     * The underlying status for records in the database has changed since
     * Stable Summa. This method returns !DELETED & INDEXABLE.
     * @deprecated the status field has been removed. See DELETED and INDEXABLE
     *             in the package description.
     */
    public boolean recordActive(String id) throws RemoteException {
        Record record = getRecord(id);
        return record != null && !record.isDeleted() && record.isIndexable();
    }

    public boolean recordExists(String name) throws RemoteException {
        return getRecord(name) != null;
    }

    /**
     * Close the connection to the underlying storage. The state of the Control
     * after close is undefined.
     * @throws RemoteException if the connection could not be closed properly.
     */
    public abstract void close() throws RemoteException;
}
