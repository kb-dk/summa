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
import dk.statsbiblioteket.summa.storage.api.WritableStorage;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface of a remote writable storage, this interface have a sibling in
 * {@link RemoteReadableStorage}.
 * @see WritableStorage
 */
@QAInfo(author = "mke",
        state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL)
public interface RemoteWritableStorage extends WritableStorage, Remote {
    /**
     * Insert a record into storage.
     * @see WritableStorage
     * @param record Record to flush into storage.
     * @throws RemoteException If error occur when using the remote storage.
     */
    void flush(Record record) throws RemoteException;

    /**
     * Insert a list of records into storage.
     * @see WritableStorage
     * @param records A list of records to insert into storage.
     * @throws RemoteException If error occur when using the remote storage.
     */
    void flushAll(List<Record> records) throws RemoteException;

    /**
     * Closes the storage.
     * @see WritableStorage
     * @throws RemoteException If error occur when using the remote storage.
     */
    void close() throws RemoteException;

    /**
     * Clears a specific base from the storage.
     * @see WritableStorage
     * @param base The base to clear.
     * @throws RemoteException If error occur when using the remote storage.
     */
    void clearBase(String base) throws RemoteException;

    /**
     * Executes a batch job on a specific set of records in the storage. Records
     * can be specified by minimum and maximum MTime as well as base and a set
     * of {@link QueryOptions}.
     * @see WritableStorage
     * @param jobName The batch job name.
     * @param base The base to execute batch job on.
     * @param minMtime The minimum MTime to execute batch job on.
     * @param maxMtime The maximum MTime to execute batch job on.
     * @param options The query options.
     * @return Result of the batch job execution.
     * @throws RemoteException If error occur when using the remote storage.
     */
    String batchJob(String jobName, String base, long minMtime, long maxMtime,
                    QueryOptions options) throws RemoteException;
}
