package dk.statsbiblioteket.summa.storage.rmi;

import dk.statsbiblioteket.summa.storage.api.ReadableStorage;
import dk.statsbiblioteket.summa.storage.RecordIterator;
import dk.statsbiblioteket.summa.storage.RecordAndNext;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: mikkel Date: Aug 5, 2008 Time: 1:02:08 PM To
 * change this template use File | Settings | File Templates.
 */
public interface RemoteReadableStorage extends ReadableStorage, Remote {

    RecordIterator getRecords(String base) throws RemoteException;

    RecordIterator getRecordsModifiedAfter(long time, String base) throws RemoteException;

    RecordIterator getRecordsFrom(String id, String base) throws RemoteException;

    dk.statsbiblioteket.summa.common.Record getRecord(String id) throws RemoteException;

    boolean recordExists(String id) throws RemoteException;

    boolean recordActive(String id) throws RemoteException;

    RecordAndNext next(Long iteratorKey) throws RemoteException;

    List<RecordAndNext> next(Long iteratorKey, int maxRecords) throws
                                                                RemoteException;

}
