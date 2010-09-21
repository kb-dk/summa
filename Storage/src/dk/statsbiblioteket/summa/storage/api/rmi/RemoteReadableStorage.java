/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.storage.api.rmi;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.ReadableStorage;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Utility wrapper of the {@link ReadableStorage} interface to make RMI work.
 * @see ReadableStorage
 */
public interface RemoteReadableStorage extends ReadableStorage, Remote {
    /**
     * Return iterator key for records in a given base modified after
     * {@code time}.
     * @see ReadableStorage
     * @param time The time stamp.
     * @param base The base.
     * @param options The query options.
     * @return The iterator key (sorted by record id).
     * @throws RemoteException If error occur when using the remote storage.
     */
    long getRecordsModifiedAfter(long time, String base, QueryOptions options)
                                                         throws RemoteException;

    /**
     * Return the last modification time for a given base.
     * @see ReadableStorage
     * @param base The base.
     * @return The modification time.
     * @throws RemoteException If error occur when using the remote storage.
     */
    long getModificationTime(String base) throws RemoteException;

    /**
     * Return a list of records.
     * @see ReadableStorage
     * @param ids The records IDs.
     * @param options {@link QueryOptions} that adds extra control over which
     * records are returned.
     * @return List of record.
     * @throws RemoteException If error occur when using the remote storage.
     */
    List<Record> getRecords(List<String> ids, QueryOptions options)
                                                         throws RemoteException;

    /**
     * Return a single record.
     * @see ReadableStorage
     * @param id The record ID.
     * @param options {@link QueryOptions} that adds extra control over which
     * record are returned.
     * @return A record.
     * @throws RemoteException If error occur when using the remote storage.
     */
    Record getRecord(String id, QueryOptions options) throws RemoteException;

    /**
     * Return next record given a iterator key.
     * @see ReadableStorage
     * @param iteratorKey The iterator key.
     * @return The next record in the iterator.
     * @throws RemoteException If error occur when using the remote storage.
     */
    Record next(long iteratorKey) throws RemoteException;

    /**
     * Returns the next {@code maxRecords} records from the iterator.
     * @see ReadableStorage
     * @param iteratorKey The iterator key.;
     * @param maxRecords The maximum records there should be returned. If less
     * records are returned the iterator should be depleted.
     * @return A list of the next (up to {@code maxRecords}) records.
     * @throws RemoteException If error occur when using the remote storage.
     */
    List<Record> next(long iteratorKey, int maxRecords) throws RemoteException;
}
