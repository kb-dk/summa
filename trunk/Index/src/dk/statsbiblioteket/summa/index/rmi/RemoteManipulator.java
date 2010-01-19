package dk.statsbiblioteket.summa.index.rmi;

import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.index.IndexManipulator;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Proxy interface to facilitate exposing {@link IndexManipulator}s over RMI.
 * To expose a manipulator over RMI use the wrapper class
 * {@link RMIManipulatorProxy}
 */
public interface RemoteManipulator extends IndexManipulator, Remote {

    @Override
    public void open(File indexRoot) throws RemoteException;

    @Override
    public void clear() throws RemoteException;

    @Override
    public boolean update(Payload payload) throws RemoteException;

    @Override
    public void commit() throws RemoteException;

    @Override
    public void consolidate() throws RemoteException;

    @Override
    public void close() throws RemoteException;

    @Override
    void orderChangedSinceLastCommit() throws RemoteException;

    @Override
    boolean isOrderChangedSinceLastCommit() throws RemoteException;
}
