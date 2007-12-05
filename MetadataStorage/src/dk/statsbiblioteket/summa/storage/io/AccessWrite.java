/* $Id: AccessWrite.java,v 1.6 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.6 $
 * $Date: 2007/10/05 10:20:22 $
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

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.RemoteException;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public interface AccessWrite {
    /**
     * Create a new record with the given name and base, the timestamp "now" and no data.
     * @param name record name (bib#/id)
     * @param base the name of the original record base
     * @return the created Record
     * @throws java.rmi.RemoteException
     * @deprecated use {@link dk.statsbiblioteket.summa.storage.io.AccessWrite#flush(dk.statsbiblioteket.summa.common.Record)}
     */
    //Record createNewRecord(String name, String base) throws RemoteException;

    /**
     * Create a new record with the given name and base, the timestamp "now" and the given data.
     * @param name record name (bib#/id)
     * @param data record data
     * @param base the name of the original record base
     * @return the created Record
     * @throws java.rmi.RemoteException
     * @deprecated use {@link dk.statsbiblioteket.summa.storage.io.AccessWrite#flush(dk.statsbiblioteket.summa.common.Record)}
     */
    //Record createNewRecord(String name, byte[] data, String base) throws RemoteException;

    /**
     * Delete the record of the given name (id).
     * The record is marked deleted, but kept in storage...
     * @param name record name (bib#/id)
     * @return true on success; false otherwise
     * @throws java.rmi.RemoteException
     * @deprecated use {@link dk.statsbiblioteket.summa.storage.io.AccessWrite#flush(dk.statsbiblioteket.summa.common.Record)}
     */
    //boolean deleteRecord(String name) throws RemoteException;

    /**
     * Remove from storage records with the given base and state set to deleted before given time.
     * I.e. remove records from given base with state deleted and time stamp before given time.
     * @param time upper bound time stamp
     * @param base name of the original record base
     * @throws java.rmi.RemoteException
     * @deprecated use {@link dk.statsbiblioteket.summa.storage.io.AccessWrite#flush(dk.statsbiblioteket.summa.common.Record)}
     */
    //void removeDeletedBefore(long time, String base) throws RemoteException;

    /**
     * Find the stored record by the name in the given record and update data and timestamp.
     * The data is updated to the data given in the record and the time stamp to "now".
     * @param record the record to be updated in storage
     * @throws java.rmi.RemoteException
     * @deprecated use {@link dk.statsbiblioteket.summa.storage.io.AccessWrite#flush(dk.statsbiblioteket.summa.common.Record)}
     */
    //void updateRecord(Record record) throws RemoteException;

    /**
     * Find the record by name/bib#/id and update data, and timestamp.
     * The data is updated to the given data and the time stamp to "now".
     * @param name record name (bib#/id)
     * @param data record data
     * @throws java.rmi.RemoteException
     * @deprecated use {@link dk.statsbiblioteket.summa.storage.io.AccessWrite#flush(dk.statsbiblioteket.summa.common.Record)}
     */
    //void updateRecord(String name, byte[] data) throws RemoteException;

    /**
     *
     * @param record
     * @throws RemoteException
     */
    void flush(Record record) throws RemoteException;
}
