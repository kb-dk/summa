package dk.statsbiblioteket.summa.control.bundle;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link BundleRepository} exposing an RMI interface. It wraps
 * a child {@link URLRepository} soring the bundles.
 */
public class RemoteURLRepositoryServer extends UnicastRemoteObject
                                       implements RemoteRepository {
    private static final Log log =
                             LogFactory.getLog(RemoteURLRepositoryServer.class);
    private BundleRepository localRepo;

    /**
     * Configuraion property defining the port on which RMI communications
     * with this repo should proceed. The default is 27045.
     */
    public static final String PROP_SERVICE_PORT =
                                          "summa.control.repository.service.port";

    /**
     * Configuraion property defining the name which the repository should
     * register itself on the RMI registry with. The default is
     * {@code remoteURLRepository}
     */
    public static final String PROP_SERVICE_NAME =
                                          "summa.control.repository.service.name";

    /**
     * Configuraion property defining the port on which RMI registry should run
     * or can be found. The default is 27000.
     */
    public static final String PROP_REGISTRY_PORT =
                                          "summa.control.repository.registry.port";

    /**
     * Create a new {@code RemoteURLRepository}. The passed in configuration
     * will be passed unmodified to an underlying {@link URLRepository}.
     *
     * @param conf configuration from which to read properties. This
     *             configuration will also be passed directly to the underlying
     *             {@link URLRepository}
     * @throws IOException if there is an error exporting the remote interface
     */
    public RemoteURLRepositoryServer (Configuration conf) throws IOException {
        super (getServicePort(conf));

        log.debug ("Creating RemoteURLRepositoryServer");

        localRepo = new URLRepository(conf);

        log.debug ("Exporting remote interfaces");
        RemoteHelper.exportRemoteInterface(this,
                                           conf.getInt(PROP_REGISTRY_PORT, 27000),
                                           conf.getString(PROP_SERVICE_NAME, "remoteURLRepository"));
    }

    private static int getServicePort(Configuration conf) {
        return conf.getInt (PROP_SERVICE_PORT, 27045);
    }

    /**
     * Warning: This method returns the File object for the 'local' file
     * on the server side. This works if you are the Control server.
     * However the  {@link RemoteURLRepositoryClient} does not invoke
     * this method, but bypasses it with a {@link URLRepository#get}
     */
    public File get(String bundleId) throws RemoteException {
        try {
            return localRepo.get(bundleId);
        } catch (IOException e) {
            throw new RemoteException("Error getting bundle file for '"
                                       + bundleId + "'", e);
        }
    }

    public List<String> list(String regex) throws RemoteException {
        log.trace ("Got list() request: '" + regex + "'");
        try {
            return localRepo.list (regex);
        } catch (IOException e) {
            throw new RemoteException("Failed to list local repository: "
                                      + e.getMessage(), e);
        }
    }
}
