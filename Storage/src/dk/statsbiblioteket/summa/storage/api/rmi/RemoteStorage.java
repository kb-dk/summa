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

import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.common.Record;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Utility wrapper of the {@link Storage} interface to make RMI work
 */
public interface RemoteStorage extends Storage, Remote {

    /* Reader methods */
    long getRecordsModifiedAfter(long time, String base, QueryOptions options) throws RemoteException;

    long getModificationTime (String base) throws RemoteException;

    List<Record> getRecords(List<String> ids, QueryOptions options) throws RemoteException;

    Record getRecord(String id, QueryOptions options) throws RemoteException;

    Record next(long iteratorKey) throws RemoteException;

    List<Record> next(long iteratorKey, int maxRecords) throws RemoteException;

    /* Writer methods */
    void flush(Record record) throws RemoteException;

    void flushAll(List<Record> records) throws RemoteException;

    void close() throws RemoteException;

    void clearBase (String base) throws RemoteException;

}




