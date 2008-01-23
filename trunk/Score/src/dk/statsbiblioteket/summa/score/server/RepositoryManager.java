package dk.statsbiblioteket.summa.score.server;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.score.bundle.BundleRepository;
import dk.statsbiblioteket.summa.score.bundle.Bundle;
import dk.statsbiblioteket.summa.score.bundle.URLRepository;

import java.io.File;
import java.io.IOException;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.Collections;
import java.util.Arrays;

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


    private File baseDir;
    private Class<? extends BundleRepository> clientRepoClass;


    public RepositoryManager (Configuration conf) {

        String basePath = conf.getString (BASE_PATH_PROPERTY,
                                          System.getProperty("user.home")
                                        + File.separator
                                        + "public_html"
                                        + File.separator
                                        + "score"
                                        + File.separator
                                        + "repo");

        baseDir = new File (basePath);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }

        clientRepoClass = conf.getClass (CLIENT_REPO_PROPERTY,
                                         BundleRepository.class,
                                         URLRepository.class);
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
        final FilenameFilter filter = new FilenameFilter () {

            public boolean accept(File file, String s) {
                return baseDir.equals(file) && s.endsWith(Bundle.BUNDLE_EXT);
            }
        };

        return Arrays.asList (baseDir.list(filter));        
    }
}
