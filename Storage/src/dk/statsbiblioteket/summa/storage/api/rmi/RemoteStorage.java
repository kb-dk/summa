package dk.statsbiblioteket.summa.storage.api.rmi;

import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.RecordIterator;
import dk.statsbiblioteket.summa.storage.api.RecordAndNext;
import dk.statsbiblioteket.summa.common.Record;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: mikkel Date: Aug 5, 2008 Time: 1:04:57 PM To
 * change this template use File | Settings | File Templates.
 */
public interface RemoteStorage extends Storage, Remote {

    /* Reader methods */
    RecordIterator getRecords(String base) throws RemoteException;

    RecordIterator getRecordsModifiedAfter(long time, String base) throws RemoteException;

    RecordIterator getRecordsFrom(String id, String base) throws RemoteException;

    dk.statsbiblioteket.summa.common.Record getRecord(String id) throws RemoteException;

    boolean recordExists(String id) throws RemoteException;

    boolean recordActive(String id) throws RemoteException;

    RecordAndNext next(Long iteratorKey) throws RemoteException;

    List<RecordAndNext> next(Long iteratorKey, int maxRecords) throws
                                                                RemoteException;

    /* Writer methods */
    void flush(Record record) throws RemoteException;

}



