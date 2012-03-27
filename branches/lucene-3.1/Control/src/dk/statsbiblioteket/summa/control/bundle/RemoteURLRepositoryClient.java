/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.control.bundle;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.SummaRMIConnectionFactory;
import dk.statsbiblioteket.summa.control.api.bundle.BundleRepository;
import dk.statsbiblioteket.summa.control.api.bundle.rmi.RemoteRepository;
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
    private static final long serialVersionUID = 533894684189L;
    private static final Log log = LogFactory.getLog (RemoteURLRepositoryClient.class);

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

    /**
     * Create new repository client. The {@code conf} argument should set
     * the {@link URLRepository#CONF_BASE_URL} as this class utilizes
     * a {@link URLRepository} underneath to do bundle downloads.
     * @param conf  The configuration.
     */
    public RemoteURLRepositoryClient (Configuration conf) {
        downloadDir = conf.getString(CONF_DOWNLOAD_DIR,
                                     DEFAULT_DOWNLOAD_DIR);
        repoAddress = conf.getString(CONF_REPO_ADDRESS,
                                     DEFAULT_REPO_ADDRESS);

        /* Sanity check properties */
        if (!repoAddress.startsWith("//")) {
            throw new ConfigurationException("The value of " + CONF_REPO_ADDRESS
                                             +": '" + repoAddress + "' does "
                                             + "not look like an RMI address");
        }

        /* Create a connection manager */
        connMgr = new ConnectionManager<BundleRepository> (
               new SummaRMIConnectionFactory<RemoteRepository> (conf));

        /* Prepare a configuration for the underlying URLRepo */
        Configuration localConf = Configuration.newMemoryBased();
        localConf.importConfiguration(conf);
        localConf.purge(CONF_REPO_ADDRESS);        

        /* Create the URLRepo with the custom config */
        localRepo = new URLRepository(conf);
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




