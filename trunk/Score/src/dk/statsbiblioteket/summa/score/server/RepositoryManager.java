package dk.statsbiblioteket.summa.score.server;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.Zips;
import dk.statsbiblioteket.util.Checksums;
import dk.statsbiblioteket.util.watch.FolderWatcher;
import dk.statsbiblioteket.util.watch.FolderListener;
import dk.statsbiblioteket.util.watch.FolderEvent;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.score.bundle.BundleRepository;
import dk.statsbiblioteket.summa.score.bundle.Bundle;
import dk.statsbiblioteket.summa.score.bundle.URLRepository;
import dk.statsbiblioteket.summa.score.bundle.BundleSpecBuilder;
import dk.statsbiblioteket.summa.score.bundle.BundleLoadingException;
import dk.statsbiblioteket.summa.score.bundle.BundleFormatException;
import dk.statsbiblioteket.summa.score.api.ClientConnection;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;

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

    private File repoBaseDir;
    private File scoreBaseDir;
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
                    try {
                        repo.importBundle(changed);
                    } catch (Exception e) {
                        log.error ("Failed to import bundle '" + changed + "'",
                                   e);
                    }
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
        repoBaseDir = ScoreUtils.getRepositoryBaseDir(conf);
        log.debug ("Using repository base dir: '" + repoBaseDir + "'");

        scoreBaseDir = ScoreUtils.getScoreBaseDir(conf);

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
        return new File (repoBaseDir, bundleId + Bundle.BUNDLE_EXT).exists();
    }

    /**
     * Return a file handle for the given bundle.
     * @param bundleId the id of the bundle to look up
     * @return a File handle for the bundle or {@code null} if the bundle does
     *         not exist.
     */
    public File getBundle (String bundleId) {
        File bundleFile = new File (repoBaseDir, bundleId + Bundle.BUNDLE_EXT);
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
                return repoBaseDir.equals(file) && s.endsWith(Bundle.BUNDLE_EXT);
            }
        };
        List<String> ids = Arrays.asList (repoBaseDir.list(filter));
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
     * @throws BundleLoadingException if the bundle is not accepted into the
     *                                repository
     */
    public void importBundle (File prospectBundle) throws IOException {
        log.info ("Preparing to import bundle file '" + prospectBundle + "'");
        BundleSpecBuilder builder;

        /* Make sure the file is a-ok */
        checkBundleFile(prospectBundle);

        /* Make sure bundle contents and spec is ok */
        checkBundle(prospectBundle, true);

        /* Clean up */
        log.debug ("Deleting '" + prospectBundle + "' (successfully imported)");
        Files.delete (prospectBundle);
        log.info ("'" + prospectBundle + "' imported successfully");
    }

    /**
     * Check that the contents of a bundle file are valid.
     * @param bundleFile the file to check
     * @param update if {@code true} {@code bundleFile} will be updated
     *               with any missing parameters from the Score server
     * @throws BundleLoadingException if there is an error reading the bundle
     *                                file
     * @throws BundleFormatException if the bundle is readable, but the spec
     *                               is bad
     * @throws java.io.IOException of there are errors handling the bundle file
     */
    public void checkBundle (File bundleFile, boolean update) throws IOException {
        /* Unzip bundle to staging area */
        File stagingDir = new File (scoreBaseDir, "tmp");
        stagingDir = new File (stagingDir,
                               new String (Checksums.md5(bundleFile)));
        log.debug ("Unpacking '" + bundleFile + "' to staging direcotry '"
                   + stagingDir + "'");
        if (stagingDir.exists()) {
            log.debug ("Staging dir already exists. Deleting.");
            Files.delete (stagingDir);
        }
        stagingDir.mkdirs();
        Zips.unzip(bundleFile.getAbsolutePath(),
                   stagingDir.getAbsolutePath(), true);

        /* Try and read the bundle spec */
        BundleSpecBuilder builder;
        try {
            builder = BundleSpecBuilder.open (bundleFile);
        } catch (IOException e) {
            String error = "Failed to read bundle '" + bundleFile
                         + "': " + e.getMessage()
                         + "\nRemoved from incoming queue.";
            try {
                Files.delete (bundleFile);
            } catch (Exception ee) {
                error += Strings.getStackTrace(e)
                         + "\n\nFailed to delete garbage file '"
                           + bundleFile + "': " + ee.getMessage();
            }

            /* We loose the stack trace of ee here, but it should
             * not be a biggie */
            throw new BundleLoadingException(error, e);
        }

        /* Run sanity checks */

        String bundleId = builder.getBundleId();
        if (bundleId == null) {
            throw new BundleFormatException("Bundle id for '"
                                            + bundleFile.getName()
                                            + "' not set");
        }

        String bundleName = bundleId + Bundle.BUNDLE_EXT;
        if (!bundleName.equals(bundleFile.getName())) {
            throw new BundleFormatException("Bundle filename and bundleId must"
                                            + " match. Found file '"
                                            + bundleFile.getName()
                                            +"' should have been '"
                                            + bundleName + "'");
        }

        if (builder.getInstanceId() != null) {
            throw new BundleFormatException("Bundle '" + bundleId + "' has hard"
                                            + " coded instanceId '"
                                            + builder.getInstanceId() + "'");
        }

        if (builder.getMainClass() == null) {
            throw new BundleFormatException("Bundle '" + bundleId + "' has no"
                                            + " mainClass defined");
        }

        if (builder.getMainJar() == null) {
            log.warn ("Bundle '" + bundleId + "' has no mainJar defined."
                      + " Things may still work if the mainClass is in the"
                      + " classpath");
        }

        if (builder.getDescription() == null) {
            log.warn ("Bundle '" + bundleId + "' has no description");
        }

        if (builder.getFiles().size() != 0) {
            log.debug ("Validating fileList for bundle '" + bundleId + "'");
            builder.checkFileList(stagingDir);
        } else if (update) {
            log.debug ("Building fileList for '" + bundleId +"'");
            builder.buildFileList(stagingDir);
        } else {
            throw new BundleFormatException("No, or empty,  fileList for"
                                            + " bundle '" + bundleId + "'");
        }

    }

    /**
     * <p>Assert that a given file meets our requirements for bundles to import.
     * This method does not check the contents of the bundle. Use
     * {@link #checkBundle} for that.</p>
     *
     * <p><i>Important:</i> This method should only be invoked on files
     * in the {@code incoming} directory, since it will delete any
     * violating files.</p>
     *
     * @param prospectBundle the file to check
     * @throws dk.statsbiblioteket.summa.score.bundle.BundleLoadingException
     *         if the bundle is not eligible for import
     */
    private void checkBundleFile (File prospectBundle) throws
                                                        BundleLoadingException {
        String error = null;
        Exception exc = null;

        if (!prospectBundle.exists()) {
            error = "Trying to import bundle '" + prospectBundle + "', but"
                     + " the file does not exist. Ignoring.";
        }

        else if (prospectBundle.isDirectory()) {
            error = "Can not import directory '" + prospectBundle + "'."
                     + " New bundles must be supplied in .bundle files."
                     + " Removing the directory from incoming dir '"
                     + incomingDir + "'";
            try {
                Files.delete (prospectBundle);
            } catch (Exception e) {
                error += "\n\nFailed to delete garbage file '"
                           + prospectBundle + "':";
                exc = e;
            }
        }

        else if (!(prospectBundle.canWrite() && prospectBundle.canRead())) {
            error = "Insufficient file permissions to import '"
                   + prospectBundle + "'. Ignoring.";
        }

        else if (!prospectBundle.getName().endsWith(Bundle.BUNDLE_EXT)) {
            log.error ("Invalid bundle file '" + prospectBundle + "'. Bundle"
                       + "files must have the '" + Bundle.BUNDLE_EXT + "'"
                       + " extension. Removing from incoming dir.");
            try {
                Files.delete (prospectBundle);
            } catch (Exception e) {
                error += "\n\nFailed to delete garbage file '"
                           + prospectBundle + "'";
                exc = e;
            }
        }

        /* Construct error message */
        if (error != null && exc == null) {
            throw new BundleLoadingException(error);
        } else if (error != null && exc != null) {
            throw new BundleLoadingException(error, exc);
        }

        /* Bundle is ok */
    }
}
