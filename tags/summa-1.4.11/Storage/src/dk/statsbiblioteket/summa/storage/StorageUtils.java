package dk.statsbiblioteket.summa.storage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A collection of utility methods handy for
 * {@link dk.statsbiblioteket.summa.storage.api.Storage} implementations.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke")
public class StorageUtils {

    private static final Log log = LogFactory.getLog (StorageUtils.class);

    /**
     * <p>Work out what root directory a service installed under. This is
     * extracted from the
     * {@link Storage#CONF_DATA_DIR} property.</p>
     *
     * <p>The service itself is installed under {@code <rootDir>/<service_id>}</p>
     *
     * <p>If the directory does not exist it will be created.</p>
     *
     * @param conf configuration to extract the properties from
     * @return base directory for installation
     */
    public static File getRootDir (Configuration conf) {
        String basePath = conf.getString (Storage.CONF_DATA_DIR,
                                          System.getProperty("user.home")
                                        + File.separator
                                        + "summa-control");
        File baseDir = new File (basePath);
        if (!baseDir.exists()) {
            if (baseDir.mkdirs()) {
                log.debug("Created storage root installation directory '"
                          + baseDir + "'");
            }
        }

        log.trace ("Calculated storage root dir '" + baseDir + "'");
        return baseDir;
    }

    /**
     * <p>Get the location to store globally persistent data. Here 'global'
     * is in the sense that it should be shared among different instances
     * of the same bundle.</p>
     *
     * <p>The location of the persistent dir is calculated as
     * {@code getRootDir()/persistent}</p>
     *
     * <p>The persistent directory will be created it needed</p>
     *
     * @param conf the configuration used to look up
     * @return a {@link File} object pointing to the requested directory
     */
    public static File getGlobalPersistentDir (Configuration conf) {
        File pers = getRootDir(conf);

        pers = new File (pers, "persistent");

        if (! pers.exists()) {
            if (pers.mkdirs()) {
                log.debug("Created globally persistent storage data dir '"
                          + pers + "'");
            }
        }

        log.trace ("Calculated storage persistent dir '" + pers + "'");
        return pers;
    }



}




