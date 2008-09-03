package dk.statsbiblioteket.summa.control.bundle;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.SummaRMIConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link BundleRepository} talking to a RemoteURLRepositoryServer}.
 * The primary purpose of this is to allow repository listing.
 */
public class RemoteURLRepositoryClient implements BundleRepository {

    private static final Log log = LogFactory.getLog (RemoteURLRepositoryClient.class);

    /**
     * The value of this property will be passed in as
     * {@link URLRepository#REPO_ADDRESS_PROPERTY} to the underlying
     * {@link URLRepository}.
     */
    public static final String PROP_REMOTE_URL =
                                      "summa.control.repository.remote.address";

    public static final String DEFAULT_REMOTE_URL =
                                        URLRepository.DEFAULT_REPO_URL;

    public static final String DEFAULT_REPO_ADDRESS =
                                "//localhost:"
                              + RemoteURLRepositoryServer.DEFAULT_REGISTRY_PORT
                              + "/"
                              + RemoteURLRepositoryServer.DEFAULT_SERVICE_NAME;

    private String repoAddress;
    private String remoteUrl;
    private String downloadDir;
    private BundleRepository localRepo;
    private ConnectionManager<BundleRepository> connMgr;
    private ConnectionContext<BundleRepository> connCtx;

    public RemoteURLRepositoryClient (Configuration conf) {
        downloadDir = conf.getString(BundleRepository.DOWNLOAD_DIR_PROPERTY,
                                     DEFAULT_DOWNLOAD_DIR);
        repoAddress = conf.getString(BundleRepository.REPO_ADDRESS_PROPERTY,
                                     DEFAULT_REPO_ADDRESS);

        /* Sanity check properties */
        if (!repoAddress.startsWith("//")) {
            throw new ConfigurationException("The value of " + REPO_ADDRESS_PROPERTY
                                             +": '" + repoAddress + "' does "
                                             + "not look like an RMI address");
        }

        /* Create a connection manager */
        connMgr = new ConnectionManager<BundleRepository> (
               new SummaRMIConnectionFactory<RemoteRepository> (conf));

        /* Prepare a configuration for the underlying URLRepo */
        Configuration localConf = Configuration.newMemoryBased(
                   RemoteURLRepositoryServer.DOWNLOAD_DIR_PROPERTY, downloadDir);

        /* If there is no explicit remote URL supplied use the URLRepo's default
         * Ie. don't specify it */
        if (conf.valueExists(PROP_REMOTE_URL)) {
                localConf.set (REPO_ADDRESS_PROPERTY,
                               conf.getString(PROP_REMOTE_URL));
        }        

        /* Create the URLRepo with the custom config */
        localRepo = new URLRepository(localConf);
    }

    public File get(String bundleId) throws IOException {
        return localRepo.get (bundleId);
    }

    public List<String> list(String regex) throws IOException {
        BundleRepository repo = getConnection();
        try {
            return repo.list(regex);
        } finally {
            releaseConnection();
        }
    }

    public String expandApiUrl (String jarFileName) throws IOException {
        BundleRepository repo = getConnection();
        try {
            String url = repo.expandApiUrl(jarFileName);
            log.trace ("Expanded API url of " + jarFileName + ": "
                       +url);
            return url;
        } finally {
            releaseConnection();
        }
    }

    /**
     * Return a connection to the remote repository used by this
     * repository client.
     * @return a connection to the remote repository
     * @throws IOException on communication errors
     */
    public BundleRepository getRepositoryConnection () throws IOException {
        try {
            return getConnection();
        } finally {
            releaseConnection();
        }
    }

    private synchronized BundleRepository getConnection() throws IOException {
        if (connCtx == null) {
            connCtx = connMgr.get(repoAddress);
        }

        if (connCtx == null) {
            throw new IOException ("Failed to connect to remote repository: "
                                   + repoAddress);
        }

        return connCtx.getConnection();
    }

    private synchronized void releaseConnection () {
        if (connCtx != null) {
            connCtx.unref();
            connCtx = null;
        }
    }

}
