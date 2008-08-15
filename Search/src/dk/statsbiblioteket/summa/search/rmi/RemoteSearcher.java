package dk.statsbiblioteket.summa.search.rmi;

import dk.statsbiblioteket.summa.search.SummaSearcher;
import dk.statsbiblioteket.summa.search.ResponseCollection;
import dk.statsbiblioteket.summa.search.Request;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface needed for RMI on {@link RMISearcherProxy} to work
 */
public interface RemoteSearcher extends Remote, SummaSearcher {

    public ResponseCollection search(Request request) throws RemoteException;

    public void close() throws RemoteException;

}
