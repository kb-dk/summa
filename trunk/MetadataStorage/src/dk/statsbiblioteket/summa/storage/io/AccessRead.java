/* $Id: AccessRead.java,v 1.7 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.7 $
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

import java.rmi.RemoteException;
import java.util.List;

import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public interface AccessRead {
    /**
     * Get an iterator over all records in the database from the given base sorted by name.
     * @param base the name of the original record base
     * @return a RecordIterator of all records (sorted by name)
     * @throws java.rmi.RemoteException
     */
    RecordIterator getRecords(String base) throws RemoteException;

    /**
     * Get an iterator over all records from the given base modified after the given time.
     * The iterator is sorted by name.
     * @param time a timestamp in milliseconds
     * @param base the name of the original record base
     * @return a RecordIterator of records modified after given time (sorted by name)
     * @throws java.rmi.RemoteException
     */
    RecordIterator getRecordsModifiedAfter(long time, String base) throws RemoteException;

    /**
     * Get an iterator over all records from the given base "from" the given id.
     * I.e. get all records with id "larger than" the given id.
     * Would we prefer "larger than or equal to"?
     * The iterator is sorted by id.
     * @param id record id (bib#/id)
     * @param base the name of the original record base
     * @return a RecordIterator of records "from" the given id (sorted by id)
     * @throws java.rmi.RemoteException
     */
    RecordIterator getRecordsFrom(String id, String base) throws RemoteException;

    /**
     * Get the record with the given id.
     * @param id record id (bib#/id)
     * @return the requested Record; null on record not found
     * @throws java.rmi.RemoteException
     */
    dk.statsbiblioteket.summa.common.Record getRecord(String id) throws RemoteException;

    /**
     * Check if a record of the given id exists.
     * @param id record id (bib#/id)
     * @return true if a record of the given id exists; false otherwise
     * @throws java.rmi.RemoteException
     */
    boolean recordExists(String id) throws RemoteException;

    /**
     * Check if a record of the given id exists AND is active.
     * I.e. the record exists and is NOT marked deleted.
     * @param id record id (bib#/id)
     * @return true if a record of the given id exists and is active; false otherwise
     * @throws java.rmi.RemoteException
     */
    boolean recordActive(String id) throws RemoteException;

    /**
     * Return the next record in the record iteration identified by the given iterator key.
     * Should only be used by RecordIterator Objects.
     * @param iteratorKey iterator key
     * @return a RecordAndNext object holding the next Record in the RecordIterator
     *         identified by the iteratorKey as well as the next next value;
     *         the Record in the RecordAndNext object can be null if the iteration
     *         has no more elements or the iterator is not found
     *         (does not throw a NoSuchElementException)
     * @throws java.rmi.RemoteException
     */
    RecordAndNext next(Long iteratorKey) throws RemoteException;

    /**
     * Return a maximum of maxRecords Records in the record iteration identified
     * by the given iterator key. Should only be used by RecordIterator Objects.
     * @param iteratorKey iterator key.
     * @param maxRecords  the maximum number of RecordAndNext objects to return.
     *                    The implementation should try to return as many
     *                    records as possible.
     * @return a list of RecordAndNext objects holding the Records in the
     *         RecordIterator identified by the iteratorKey as well as the next
     *         next value.
     * @throws java.rmi.RemoteException if there was a problem requesting
     *                                  Records.
     */
    List<RecordAndNext> next(Long iteratorKey, int maxRecords) throws
                                                                RemoteException;

}
