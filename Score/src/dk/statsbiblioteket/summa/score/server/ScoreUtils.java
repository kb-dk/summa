package dk.statsbiblioteket.summa.score.server;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;
import dk.statsbiblioteket.summa.score.bundle.BundleRepository;
import dk.statsbiblioteket.summa.score.api.BadConfigurationException;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Various utils for general use in the {@link ScoreCore} and related
 * classes.
 */
public class ScoreUtils {

    private static final Log log = LogFactory.getLog (ScoreUtils.class);

    /**
     * <p>Work out what base dir to use given a configuration. This is
     * extracted from the {@link ScoreCore#SCORE_BASE_DIR} property.</p>
     *
     * <p>If the directory does not exist it will be created.</p>
     *
     * @param conf configuration to extract the properties from
     * @return base directory for installation
     */
    public static File getScoreBaseDir (Configuration conf) {
        String basePath = conf.getString (ScoreCore.SCORE_BASE_DIR,
                                          System.getProperty("user.home")
                                        + File.separator
                                        + "summa-score");
        File baseDir = new File (basePath);
        if (!baseDir.exists()) {
            if (baseDir.mkdirs()) {
                log.debug("Created score base directory '" + baseDir + "'");
            }
        }

        log.trace ("Calculated score base dir '" + baseDir + "'");
        return baseDir;
    }

    /**
     * <p>Get the base directory for the repository - ie where bundles are stored.
     * This is calculated from the {@link RepositoryManager#BASE_PATH_PROPERTY}.
     * </p>
     * <p>If the directory does not exist it will be created.</p>
     *
     * @param conf configuration to extract the properties from
     * @return a file pointing at the base directory for the bundle repository
     */
    public static File getRepositoryBaseDir (Configuration conf) {
        String basePath = conf.getString (RepositoryManager.BASE_PATH_PROPERTY,
                                          System.getProperty("user.home")
                                        + File.separator
                                        + "public_html"
                                        + File.separator
                                        + "score"
                                        + File.separator
                                        + "repo");

        File baseDir = new File (basePath);
        if (!baseDir.exists()) {
            if (baseDir.mkdirs()) {
                log.debug ("Created repository base dir '" + baseDir + "'");
            }
        }
        log.trace ("Calculated repository base dir '" + baseDir + "'");
        return baseDir;
    }

    /**
     * <p>Get the directory used to store incoming bundles uploaded by the
     * score administrator(s).</p>
     *
     * <p>If the directory does not exist it will be created.</p>
     *
     * @param conf configuration to extract the properties from
     * @return file pointing at the directory for incoming bundles
     */
    public static File getIncomingDir (Configuration conf) {
        String incPath = conf.getString (RepositoryManager.INCOMING_DIR_PROPERTY,
                                          System.getProperty("user.home")
                                        + File.separator
                                        + "public_html"
                                        + File.separator
                                        + "score"
                                        + File.separator
                                        + "incoming");

        File incDir = new File (incPath);
        if (!incDir.exists()) {
            if (incDir.mkdirs()) {
                log.debug ("Created dir for incoming bundles '" + incDir + "'");
            }
        }
        return incDir;
    }

    /**
     * Get a File pointing at the
     * @param conf
     * @return
     */
    public static File getClientMetaDir (Configuration conf) {
        File scoreBase = getScoreBaseDir(conf);
        String metaPath = conf.getString (ClientManager.CLIENT_META_DIR_PROPERTY,
                                          "meta");

        File metaDir = new File (scoreBase, metaPath);
        if (!metaDir.exists()) {
            if (metaDir.mkdirs()) {
                log.debug ("Created dir for client metadata '" + metaDir + "'");
            }
        }
        return metaDir;
    }

    /**
     * Get the address the repository is exposed on. This is the address
     * clients will use to download bundles from.
     * @param conf
     * @return
     */
    public static String getRepositoryAddress (Configuration conf) {
        String address =
                  conf.getString(BundleRepository.REPO_ADDRESS_PROPERTY,
                                 "http://"
                                 + RemoteHelper.getHostname()
                                 + "/~"
                                 + System.getProperty("user.name")
                                 + "/score/repo");
        return address;
    }

    /**
     * Translate abbreviated deployer class name to fully qualified class name
     * @param shortDesc deployer abbrev.
     * @return the fulle qualified class name
     * @throws BadConfigurationException if the deployer is not known
     */
    public static String getDeployerClassName (String shortDesc) {
        if ("ssh".equals (shortDesc)) {
            return "dk.statsbiblioteket.summa.score.server.deploy.SSHDeployer";
        } else {
            throw new BadConfigurationException("Unknown deployment transport "
                                               + "'" + shortDesc + "'");
        }
    }

    /**
     * Read the (unzipped) contents of a single zip entry within a zip file
     * @param zipFile zip file to read from
     * @param entryName name of entry withing the zip file
     * @return a byte array with the unpacked data, or null if the entry is
     *         not found within the zip file
     * @throws IOException if there is an error reading the zip file
     */
    public static byte[] getZipEntry (File zipFile, String entryName)
            throws IOException {
        ZipInputStream zip = new ZipInputStream(new FileInputStream(zipFile));
        ByteArrayOutputStream out = new ByteArrayOutputStream ();
        byte[] buf = new byte[2048];
        int count = 0;


        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            if (entry.getName().equals(entryName)) {
                while ((count = zip.read(buf, 0, buf.length)) != -1) {
                    out.write(buf, 0, count);
                }
                return out.toByteArray();
            } else {
                zip.closeEntry();
            }
        }
        return null;
    }
}
