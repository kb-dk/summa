package dk.statsbiblioteket.summa.control.api.bundle.rmi;

import dk.statsbiblioteket.summa.control.api.bundle.BundleRepository;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.File;
import java.util.List;

/**
 * Stub interface for RMI implementations of {@link dk.statsbiblioteket.summa.control.api.bundle.BundleRepository}
 */
public interface RemoteRepository extends BundleRepository, Remote {

    @Override
    public File get (String bundleId) throws RemoteException;

    @Override
    public List<String> list (String regex) throws RemoteException;

    @Override
    public String expandApiUrl (String jarFileName) throws RemoteException;
}



