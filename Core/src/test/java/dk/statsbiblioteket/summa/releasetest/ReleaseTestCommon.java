/**
 * Created: te 07-11-2008 20:46:59
 * CVS:     $Id: ReleaseTestCommon.java 857 2009-01-07 15:01:03Z toke $
 */
package dk.statsbiblioteket.summa.releasetest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Locale;

import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;

/**
 * Common methods usable for two or more release tests.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class ReleaseTestCommon {
    private final static Log log = LogFactory.getLog(ReleaseTestCommon.class);

    public static final File TEST_ROOT =
            new File(System.getProperty("java.io.tmpdir"), "summatest"); 
    public static final File PERSISTENT_FOLDER =
            new File(TEST_ROOT, "persistent");
    public static final File DATA_ROOT = new File(TEST_ROOT, "summatest");

    public static void setup() throws IOException {
        setupTestEnvironment();
        removeTestFiles();
        makeFolders();
        setupTestFiles();
    }

    public static void tearDown() throws IOException {
        removeTestFiles();
    }

    /**
     * Ensures that environment-settings for persistence et al points to a
     * valid folder under the system's temporary folder.
     */
    public static void setupTestEnvironment() {
        System.setProperty(Configuration.CONF_PERSISTENT_DIR,
                           PERSISTENT_FOLDER.toString());
        log.debug(String.format(Locale.ROOT, "Assigned %s to System property %s",
                                PERSISTENT_FOLDER, Configuration.CONF_PERSISTENT_DIR));
        System.setProperty(Resolver.SYSPROP_PERSISTENT_DIR,
                           PERSISTENT_FOLDER.toString());
    }

    /**
     * Clears TEST_ROOT.
     */
    public static void removeTestFiles() {
        try {
            if (TEST_ROOT.exists()) {
                Files.delete(TEST_ROOT);
            }
        } catch (IOException e) {
            log.warn("Unable to delete " + TEST_ROOT, e);
        }
    }

    public static void makeFolders() {
        if (!TEST_ROOT.exists()) {
            TEST_ROOT.mkdirs();
        }
        if (!PERSISTENT_FOLDER.exists()) {
            PERSISTENT_FOLDER.mkdirs();
        }
    }

    /**
     * Copies the data-folder under test in the Control-module to
     * {@link #DATA_ROOT}. If the data-folder already exists under the
     * destination-folder, it is deleted first.
     * @throws java.io.IOException if files could not be deleted or copied.
     */
    public static void setupTestFiles() throws IOException {
        if (new File(DATA_ROOT, "data").exists()) {
            Files.delete(new File(DATA_ROOT, "data"));
        }
        URL source = Resolver.getURL("integration/multiple");
        if (source == null) {
            throw new FileNotFoundException(
                    "Unable to resolve the folder 'data'");
        }
        File sourceRoot = new File(source.getFile()).getParentFile();
        log.debug("Copying test-files from " + sourceRoot + " to "
                  + DATA_ROOT);
        Files.copy(sourceRoot, DATA_ROOT, false);
    }
}
