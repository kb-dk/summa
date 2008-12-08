package dk.statsbiblioteket.summa.storage.api.rmi;

import dk.statsbiblioteket.summa.storage.api.ReadableStorage;
import dk.statsbiblioteket.summa.common.Record;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: mikkel Date: Aug 5, 2008 Time: 1:02:08 PM To
 * change this template use File | Settings | File Templates.
 */
public interface RemoteReadableStorage extends ReadableStorage, Remote {

    long getRecordsFromBase(String base) throws RemoteException;

    long getRecordsModifiedAfter(long time, String base) throws RemoteException;

    long getModificationTime (String base) throws RemoteException;

    long getRecordsFrom(String id, String base) throws RemoteException;

    List<Record> getRecords(List<String> ids, int expansionDepth) throws RemoteException;

    Record getRecord(String id, int expansionDepth) throws RemoteException;

    Record next(long iteratorKey) throws RemoteException;

    List<Record> next(long iteratorKey, int maxRecords) throws RemoteException;

}



