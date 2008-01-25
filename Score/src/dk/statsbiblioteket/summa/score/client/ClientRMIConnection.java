package dk.statsbiblioteket.summa.score.client;

import dk.statsbiblioteket.summa.score.api.ClientConnection;
import dk.statsbiblioteket.summa.score.api.Status;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.IOException;
import java.util.List;

/**
 * Package private specification of a {@link ClientConnection} using RMI
 * as transport. This is used to abstract out RMI from the public API.
 */
public interface ClientRMIConnection extends Remote, ClientConnection {

    public void stop() throws RemoteException;


    public Status getStatus() throws RemoteException;


    public String deployService(String bundleId,
                                String instanceId,
                                String configLocation)
                                                         throws RemoteException;

    public void startService(String id, String configLocation)
                                                         throws RemoteException;

    public void stopService(String id) throws RemoteException;

    public Status getServiceStatus(String id) throws RemoteException;

    public List<String> getServices() throws RemoteException;

    public String getId() throws RemoteException;


}
