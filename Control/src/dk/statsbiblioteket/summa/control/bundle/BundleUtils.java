package dk.statsbiblioteket.summa.control.bundle;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.control.api.bundle.BundleRepository;
import dk.statsbiblioteket.util.Strings;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helpful functions when dealing with {@link BundleRepository}s.
 */
public class BundleUtils {

    private static final Log log = LogFactory.getLog(BundleUtils.class);

    /**
     * Set the system property java.rmi.server.codebase to point at the relevant
     * jar files to make RMI work.
     * @param repo the {@link BundleRepository} to extract the URLs for the
     *             public jar-files.
     * @param conf configuration to extract {@link Configuration#CONF_API_VERSION}
     *             from
     * @param jarNames names of the {@code .jar} files to depend on for public
     *                 API. The names should be stripped of versioning information
     *                 and the {@link .jar} extension. For example
     *                 {@code summa-control-api-1.1.jar} would become
     *                 {@code summa-control-api}.
     */
    public static void prepareCodeBase(Configuration conf,
                                       BundleRepository repo,
                                       String... jarNames)
                                                            throws IOException {
        String summaVersion = conf.getString(
                                           Configuration.CONF_API_VERSION,
                                           Configuration.DEFAULT_API_VERSION);
        log.debug("Calculating codebase for public API, version: "
                  + summaVersion);

        String codeBase = System.getProperty("java.rmi.server.codebase");
        if (codeBase == null) {
            codeBase = "";
        }

        Set<String> codeSet = new TreeSet<String>(
                                            Arrays.asList(codeBase.split(" ")));

        for (String jar : jarNames) {
            String jarFile = jar+"-"+summaVersion+".jar";
            String jarUrl = repo.expandApiUrl(jarFile);

            if (jarUrl == null) {
                throw new Configurable.ConfigurationException("Failed to expand"
                                                              + " API URL for "
                                                              + jarFile);
            }

            codeSet.add(jarUrl);
        }

        codeBase = Strings.join(codeSet, " ");

        log.debug("Updating java.rmi.server.codebase: " + codeBase);
        System.setProperty("java.rmi.server.codebase", codeBase);
    }

}
