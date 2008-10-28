package dk.statsbiblioteket.summa.control.bundle;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.control.api.bundle.WritableBundleRepository;
import dk.statsbiblioteket.util.Files;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An extension of {@link URLRepository} also implementing the
 * {@link dk.statsbiblioteket.summa.control.api.bundle.WritableBundleRepository} interface, thus allowing clients to
 * install files into the repository.
 */
public class LocalURLRepository extends URLRepository
                                implements WritableBundleRepository,
                                           Configurable {

    /**
     * Configuration property defining the directory in which to install
     * bundles. Given as a {@code file://} url.
     * <p></p>
     * Defaults to {@link #CONF_LOCAL_URL}{@code /api}.
     */
    public static final String CONF_API_LOCAL_URL =
                                         "summa.control.repository.api.localurl";

    /**
     * Configuration property defining the directory in which to install
     * bundles. Given as a {@code file://} url. This must point to a location
     * writable by the user running the repository.
     * <p></p>
     * Defaults to {@link #baseUrl} if this property unset.
     */
    public static final String CONF_LOCAL_URL =
                                         "summa.control.repository.localurl";

    private static final Log log = LogFactory.getLog(LocalURLRepository.class);

    private File bundleDir;
    private File apiDir;

    /**
     * <p>Create a new URLRepository. If the {@link #CONF_DOWNLOAD_DIR} is
     * not set {@code tmp/} relative to the working directory will be used.</p>
     * <p/>
     * <p>If {@link #CONF_LOCAL_URL} is not set in the configuration
     * the value {@link URLRepository#baseUrl} will be used.
     *
     * @param conf
     */
    public LocalURLRepository(Configuration conf) {
        super(conf);

        String bundlePath = conf.getString(CONF_LOCAL_URL,
                                           baseUrl);

        if (!bundlePath.endsWith("/")) {
            bundlePath = bundlePath + "/";
        }

        String apiPath = conf.getString(CONF_API_LOCAL_URL,
                                        bundlePath + "api");

        if (!apiPath.endsWith("/")) {
            apiPath = apiPath + "/";
        }

        /* We have file urls, remove the url scheme so we can use File on them */
        bundlePath = bundlePath.replace ("file://", "");
        apiPath = apiPath.replace ("file://", "");

        bundleDir = new File (bundlePath);
        apiDir = new File (apiPath);

        log.trace ("Install bundles in: " + bundleDir);
        log.trace ("Install API in    : " + apiDir);

    }

    public boolean installBundle(File bundle) throws IOException {
        bundleDir.mkdirs();

        File target = new File (bundleDir, bundle.getName());
        log.debug ("Installing bundle " + bundle + " into " + target);

        if (target.exists()) {
            return false;
        }

        Files.move (bundle, target);

        return true;
    }

    public boolean installApi(File apiFile) throws IOException {
        apiDir.mkdirs();

        File target = new File (apiDir, apiFile.getName());
        log.debug ("Installing API " + apiFile + " into " + target);

        if (target.exists()) {
            return false;
        }

        Files.copy (apiFile, target, false);

        return true;
    }
}



