package dk.statsbiblioteket.summa.ingest.stream;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TDefaultArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.fs.FsCompositeDriver;
import de.schlichtherle.truezip.fs.FsDefaultDriver;
import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.sl.FsDriverLocator;
import de.schlichtherle.truezip.fs.spi.FsDriverProvider;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.ExtraAsserts;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SignedObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ArchiveReaderTest extends TestCase {
    public ArchiveReaderTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(ArchiveReaderTest.class);
    }

    public void testFileColon() throws IOException {
        File TMP = File.createTempFile("foo:bar", ".zip");
        TMP.deleteOnExit();
//        System.out.println("Testing existence of '" + TMP + "'");
//        System.out.println("'" + TMP + "'.toURI() == " + TMP.toURI());
        new TFile(TMP.toURI()).exists();
    }

    public void testZIPFilenames() throws Exception {
        testZIPDetection("data/zip/zip32.zip");
        // Colon is not supported by TrueZIP 7.0pr1
        //testZIPDetection("data/zip/zip:colon.zip");
        testZIPDetection("data/zip/double_stuffed2.zip");
    }

    public void testVerifyTrueZIPCapabilities() throws Exception {
        TArchiveDetector detector = TFile.getDefaultArchiveDetector();
        System.out.println(detector);
    }

    public void testZIPDetection(String zipString) throws Exception {
        File ZIP = Resolver.getFile(zipString);

        assertNotNull(
            "Resolver.getFile(" + zipString + ") should not return null", ZIP);
        assertTrue("File '" + ZIP + "' should exist using standard Java API",
                   ZIP.exists());

        System.out.println("Resolver.getFile(" + zipString + ") == " + ZIP);
        System.out.println("Resolver.getFile(" + zipString + ").toURI() == "
                           + ZIP.toURI());

        //TFile file = new TFile(ZIP);
        TFile file = new TFile(Resolver.getFile(zipString));
        try {
            assertTrue("File '" + ZIP + "' should exist", file.exists());
        } catch (NullPointerException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            fail("Calling TFile.exists() on '" + file + "' gave NPE");
        }
        assertTrue("File '" + ZIP + "' resolved to '" + file
                   + "' should be an archive",
                   file.isArchive());
    }

    public void testDeep() throws Exception {
        testRecursive(Resolver.getFile("data/zip/").getAbsolutePath());
    }

    public void testSimple() throws Exception {
        testRecursive(Resolver.getFile("data/zip/subfolder/").getAbsolutePath());
    }

    public void testDoubleZIP() throws Exception {
        testRecursive(Resolver.getFile("data/zip/subdouble/").getAbsolutePath());
    }

    public void testRecursive(String source) throws Exception {
        List<String> EXPECTED = Arrays.asList("foo", "bar");

        Configuration conf = Configuration.newMemoryBased();
        conf.set(FileReader.CONF_ROOT_FOLDER, source);
        conf.set(FileReader.CONF_RECURSIVE, true);
        conf.set(FileReader.CONF_FILE_PATTERN, ".*\\.xml");
        conf.set(FileReader.CONF_COMPLETED_POSTFIX, ".finito");
        ArchiveReader reader = new ArchiveReader(conf);

        ArrayList<String> actual = new ArrayList<String>(EXPECTED.size());
        while (reader.hasNext()) {
            Payload payload = reader.next();
            actual.add(new File((String)
                    payload.getData(Payload.ORIGIN)).getName());
            payload.close();
        }
        for (String a: actual) {
            System.out.println(a);
        }
//        ExtraAsserts.assertEquals(
//            "The resulting files should be as expected", EXPECTED, actual);
    }

}
