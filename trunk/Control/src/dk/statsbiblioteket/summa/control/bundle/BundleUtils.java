package dk.statsbiblioteket.summa.control.bundle;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.control.api.bundle.BundleRepository;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.Streams;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Set;
import java.util.TreeSet;
import java.util.Arrays;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

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
     *                 API. If a jarName does not end in {@code .jar} the
     *                 current Summa version of the environment will be appended
     *                 together with the {@code .jar} extension. For example
     *                 {@code summa-control-api} might become
     *                 {@code summa-control-api-1.3.4.jar}.
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
            String jarFile = jar;
            if (!jarFile.endsWith(".jar")) {
                jarFile += "-"+summaVersion+".jar";
            }
            String jarUrl = repo.expandApiUrl(jarFile);

            if (jarUrl == null) {
                throw new Configurable.ConfigurationException("Failed to expand"
                                                              + " API URL for "
                                                              + jarFile);
            }

            codeSet.add(jarUrl);
        }

        codeBase = Strings.join(codeSet, " ");

        // Fixme: We also need sbutil and commons-logging in the codebase,
        //        but how should we include versioning info here without
        //        hard coding it..?
        log.debug("Updating java.rmi.server.codebase: " + codeBase);
        System.setProperty("java.rmi.server.codebase", codeBase);
    }

    /**
     * Extract the contents of a bundle spec file ({@code client.xml} or
     * {@code service.xml}) from a zipped bundle file.
     *
     * @param bundleFile a standard zipped bundle file
     * @return the raw contents of the bundle descriptor
     */
    public static byte[] extractBundleSpec (File bundleFile) {
        ZipInputStream zin;

        try {
            zin = new ZipInputStream (new FileInputStream (bundleFile));
        } catch (IOException e) {
            throw new BundleLoadingException("Failed to load bundle "
                                             + bundleFile, e);
        }

        try {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if ("client.xml".equals (entry.getName ()) ||
                    "service.xml".equals (entry.getName())) {
                    break;
                }
            }

            if (entry == null) {
                throw new BundleFormatException ("Bundle has no spec file: "
                                                 + bundleFile);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream ();
            Streams.pipe (zin, out, 2048);
            return out.toByteArray();

        } catch (IOException e) {
            throw new BundleLoadingException("Failed to load bundle "
                                             + bundleFile, e);
        } finally {
            try {
                zin.close();
            } catch (IOException e) {
                log.warn ("Failed to close zip stream for bundle file "
                          + bundleFile, e);
            }
        }
    }

}
