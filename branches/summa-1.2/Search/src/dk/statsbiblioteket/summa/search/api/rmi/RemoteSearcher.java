package dk.statsbiblioteket.summa.search.api.rmi;

import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.Request;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface needed for RMI on {@link RMISearcherProxy} to work
 */
public interface RemoteSearcher extends Remote, SummaSearcher {

    public ResponseCollection search(Request request) throws RemoteException;

    public void close() throws RemoteException;

}



