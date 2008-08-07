package dk.statsbiblioteket.summa.storage.rmi;

import dk.statsbiblioteket.summa.storage.api.WritableStorage;
import dk.statsbiblioteket.summa.common.Record;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: mikkel Date: Aug 5, 2008 Time: 1:04:03 PM To
 * change this template use File | Settings | File Templates.
 */
public interface RemoteWritableStorage extends WritableStorage, Remote {

    void flush(Record record) throws RemoteException;

    void flushAll(List<Record> records) throws RemoteException;

    void close() throws RemoteException;

}
