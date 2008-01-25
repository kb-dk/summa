package dk.statsbiblioteket.summa.score.server;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.watch.FolderWatcher;
import dk.statsbiblioteket.util.watch.FolderListener;
import dk.statsbiblioteket.util.watch.FolderEvent;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;
import dk.statsbiblioteket.summa.score.bundle.BundleRepository;
import dk.statsbiblioteket.summa.score.bundle.Bundle;
import dk.statsbiblioteket.summa.score.bundle.URLRepository;
import dk.statsbiblioteket.summa.score.api.ClientConnection;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper class for the {@link ScoreCore} to manage the resources associated
 * with its repository of client- and service bundles.
 *
 * @see ScoreCore
 * @see BundleRepository
 * @see dk.statsbiblioteket.summa.score.client.Client
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Unfinished")
public class RepositoryManager implements Configurable,
                                          Iterable<String> {

    /**
     * Configuration property defining the path to the root directory of the
     * Score server's {@link BundleRepository}. The default value is
     * <code>${user.home}/public_html/score/repo</code>.
     */
    public static final String BASE_PATH_PROPERTY = "summa.score.repository.dir";

    /**
     * Configuration property defining the class clients should use to download
     * bundles from the Score's repository.
     */
    public static final String CLIENT_REPO_PROPERTY =
                                           "summa.score.repository.clientClass";

    /**
     * Configuration property defining the directory used for incoming bundles
     * to be stored in the repository.
     */
    public static final String INCOMING_DIR_PROPERTY =
                                          "summa.score.repository.incoming.dir";

    /**
     * Configuration property defining the gracetime for the
     * {@link FolderWatcher} on the incoming directory. Ie the number of
     * milliseconds a newly added file should be unchanged before it is
     * transfered to the repository. Default is 3000.
     */
    public static final String WATCHER_GRACETIME_PROPERTY =
                                     "summa.score.repository.watcher.graceTime";

    /**
     * Configuration property defining the number of seconds between each
     * scan of the incoming directory, when watching for incoming bundles.
     * Default is 5.
     */
    public static final String WATCHER_POLL_INTERVAL_PROPERTY =
                                  "summa.score.repository.watcher.pollInterval";

    private File baseDir;
    private String address;
    private Class<? extends BundleRepository> clientRepoClass;
    private String clientDownloadDir;
    private Log log;
    private File incomingDir;
    private FolderWatcher incomingWatcher;

    private class IncomingListener implements FolderListener {

        private RepositoryManager repo;

        public IncomingListener (RepositoryManager repo) {
            this.repo = repo;
        }

        public void folderChanged(FolderEvent event) {
            if (event.getEventType() != FolderEvent.EventType.added) {
                    log.debug("Ignoring folder event: " + event.getEventType()
                              + ", for files: "
                              + Logs.expand(event.getChangeList(), 5));
                return;
             }

            for (File changed : event.getChangeList()) {
                if (changed.getName().endsWith(Bundle.BUNDLE_EXT)) {
                    log.debug ("Detected incoming .bundle file '"
                             + changed + "'");
                    repo.importBundle(changed);
                } else {
                    log.debug ("Ignoring changed non-.bundle file in '"
                              + event.getWatchedFolder() + "': "
                              + changed);
                }
            }
        }
    }

    public RepositoryManager (Configuration conf) {
        log = LogFactory.getLog (RepositoryManager.class);

        /* Configure base path */
        baseDir = ScoreUtils.getRepositoryBaseDir(conf);
        log.debug ("Using repository base dir: '" + baseDir + "'");

        /* Configure Client Repo Class */
        clientRepoClass = conf.getClass (CLIENT_REPO_PROPERTY,
                                         BundleRepository.class,
                                         URLRepository.class);
        log.debug ("Using client repository class: "
                   + clientRepoClass.getName());

        /* Configure public address */
        address = ScoreUtils.getRepositoryAddress(conf);
        log.debug ("Using repository address: '" + address + "'");

        clientDownloadDir = conf.getString(BundleRepository.DOWNLOAD_DIR_PROPERTY,
                                           "tmp");

        incomingDir = ScoreUtils.getIncomingDir(conf);
        try {
            incomingWatcher =
               new FolderWatcher(incomingDir,
                                 conf.getInt(WATCHER_POLL_INTERVAL_PROPERTY, 5),
                                 conf.getInt(WATCHER_GRACETIME_PROPERTY, 3000));

        } catch (IOException e) {
            log.error ("Failed to initialize monitor for incming files on '"
                       + incomingDir + "'. Will not be able to pick up any new " +
                       "bundles from the incoming folder.");
        }
    }

    /**
     * Return true <i>iff</i> the bundle with the given id exists in the
     * repository.
     * @param bundleId id of the bundle to look up
     * @return whether or not the bundle is present in the repository
     */
    public boolean hasBundle (String bundleId) {
        return new File (baseDir, bundleId + Bundle.BUNDLE_EXT).exists();
    }

    /**
     * Return a file handle for the given bundle.
     * @param bundleId the id of the bundle to look up
     * @return a File handle for the bundle or {@code null} if the bundle does
     *         not exist.
     */
    public File getBundle (String bundleId) {
        File bundleFile = new File (baseDir, bundleId + Bundle.BUNDLE_EXT);
        if (bundleFile.exists()) {
            return bundleFile;
        }
        return null;
    }

    /**
     * Return the class Clients should use to obtain packages from this
     * repository.
     * @return a class implementing {@link BundleRepository}
     */
    public Class<? extends BundleRepository> getClientRepositoryClass () {
        return clientRepoClass;
    }

    /**
     * Iterate through the bundle ids of all bundles present in this repository.
     * @return and iterator over all bundle ids
     */
    public Iterator<String> iterator() {
        return getBundles().iterator();
    }

    /**
     * Get a list of all available bundle ids.
     * @return list of bundle ids
     */
    public List<String> getBundles () {
        log.trace ("Got getBundles() request");
        final FilenameFilter filter = new FilenameFilter () {

            public boolean accept(File file, String s) {
                return baseDir.equals(file) && s.endsWith(Bundle.BUNDLE_EXT);
            }
        };
        List<String> ids = Arrays.asList (baseDir.list(filter));
        log.trace ("Returning " + Logs.expand(ids, 5) + " for getBundles()"
                   + "request");
        return ids;
    }

    public Configuration getClientRepositoryConfig () {
        Configuration conf = Configuration.newMemoryBased();

        conf.set (BundleRepository.DOWNLOAD_DIR_PROPERTY, clientDownloadDir);
        conf.set (ClientConnection.REPOSITORY_CLASS, clientRepoClass.getName());
        conf.set (BundleRepository.REPO_ADDRESS_PROPERTY, address);

        return conf;
    }

    /**
     * Add a bundle to the repository.
     * @param prospectBundle the bundle to import into the repository
     */
    public void importBundle (File prospectBundle) {
        log.info ("Preparing to import bundle file '" + prospectBundle + "'");
        throw new UnsupportedOperationException();
    }
}
