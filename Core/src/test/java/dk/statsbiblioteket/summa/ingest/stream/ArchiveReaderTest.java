package dk.statsbiblioteket.summa.ingest.stream;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.file.TVFS;
import de.schlichtherle.truezip.fs.FsSyncException;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.ExtraAsserts;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ArchiveReaderTest extends TestCase {
    private static Log log = LogFactory.getLog(ArchiveReaderTest.class);

    public ArchiveReaderTest(String name) {
        super(name);
    }

    File TMP;
    @Override
    public void setUp() throws Exception {
        super.setUp();
        File t = Resolver.getFile("ingest/zip");
        assertTrue("The test data source 'zip' should exist", t.exists());
        TMP = new File(t.getParent(), "ZIPTMP").getAbsoluteFile();
        if (TMP.exists()) {
            Files.delete(TMP);
        }
        Files.copy(t, TMP, false);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        Files.delete(TMP);
    }

    public static Test suite() {
        return new TestSuite(ArchiveReaderTest.class);
    }

    private void createRecursiveZIP(TFile outer, long innerZIPs, long entryCount, long entrySize) throws IOException {
        TFile innerZIP = new TFile("innerarchive.zip");
        try {
            log.debug("Creating inner ZIP " + innerZIP);
            createTestFile(innerZIP, entryCount, entryCount, entrySize);
            assertTrue("The zip should be marked as an archive", innerZIP.isArchive());
            log.debug("Created " + innerZIP + " of size " + innerZIP.length()/1048576 + "MB");
            TVFS.umount(innerZIP);

            log.debug("Creating outer zip " + outer);
            TFile outerZIP = outer.mkdir(false);
//            TFile.cp(getContent(entrySize), new TFile(outerZIP, "singlefile.dat"));
            for (int i = 0 ; i < innerZIPs ; i++) {
                TFile dest = new TFile(outerZIP, "inner_" + i + ".zip");
                log.debug("Adding " + (i+1) + "/" + innerZIPs + " as " + dest);
                TFile.cp_r(innerZIP, dest, null, TArchiveDetector.ALL);
            }
            TVFS.umount(outerZIP);
        } finally {
            Files.delete(new File(innerZIP.getPath()));
        }
    }

    private void createTestFile(TFile zip, long entryCount, long batch, long entrySize) throws IOException {
        TFile archive = zip.mkdir(false);
        for (int i = 0 ; i < entryCount ; i++) {
            if (i % 100000 == 0) {
                log.debug("Compressed " + i + "/" + entryCount + " entries");
            }
            if (entryCount == batch) {
                TFile.cp(getContent(entrySize), new TFile(archive, "file" + i + ".dat"));
            } else {
                TFile.cp(getContent(entrySize), new TFile(archive, "folder" + (i / batch) + "/file" + i + ".dat"));
            }
        }
        TVFS.umount(archive);
        log.debug("Finished creating " + zip);
    }

    private InputStream getContent(final long contentSize) {
        return new InputStream() {
            long count = 0;
            @Override
            public int read() throws IOException {
                if (count == contentSize) {
                    return -1;
                }
                count++;
                return (int)(count & 0xFF);
            }
        };
    }

    @SuppressWarnings("ConstantConditions")
    public void testTestFileCreation() throws IOException {
        TFile zip = new TFile("testarchive.zip");
        try {
            createTestFile(zip, 10, 2, 100);
            assertTrue("The zip should be marked as an archive", zip.isArchive());
            assertEquals("The zip should contain 5 entries (folders)",
                         5, zip.listFiles() == null ? 0 : zip.listFiles().length);
            TVFS.umount();
        } finally {
            Files.delete(new File(zip.getPath()));
        }
    }

    public void testCreateRecursiveZIP() throws IOException {
        TFile zip = new TFile("testarchive.zip");
        try {
            createRecursiveZIP(zip, 5, 10, 20);
            assertTrue("The zip should be marked as an archive", zip.isArchive());
            TFile[] inners = zip.listFiles();
            assertNotNull(zip + " should have content", inners);
            assertTrue(zip + " should have at least 1 inner element", inners.length > 0);
            for (TFile inner: inners) {
                assertTrue("The inner element " + inner + " in " + zip + " should be an archive", inner.isArchive());
            }
            TVFS.umount(zip);
        } finally {
            Files.delete(new File(zip.getPath()));
        }
    }

    // TrueZIP does not scale well to many entries in a ZIP file
    public void testDirectoryScalingTiny() throws IOException {
        testScaling(10);
    }
    public void testDirectoryScalingSmall() throws IOException {
        testScaling(10000);
    }
    public void testDirectoryScalingSmallBatch() throws IOException {
        testScaling(10000, 1000, false);
    }
    public void testDirectoryScalingSmallBatchNested() throws IOException {
        testScaling(10000, 1000, true);
    }
    public void testDirectoryScalingMedium() throws IOException {
        testScaling(100000);
    }
    public void testDirectoryScalingMediumBatch() throws IOException {
        testScaling(100000, 10000, false);
    }
    public void testDirectoryScalingMediumBatchNested() throws IOException {
        testScaling(100000, 10000, true);
    }
    // Upper practical limit as of 20121120
    public void testDirectoryScalingLarge() throws IOException {
        testScaling(500000);
    }
    // Semi-hange (used > 30 minutes to list files)
    public void disabledtestDirectoryScalingHuge() throws IOException {
        testScaling(3500000); // Approximate number of Records in the Aleph system at Statsbiblioteket as of 2012
    }
    private void testScaling(long entryCount) throws IOException {
        testScaling(entryCount, entryCount, false);
    }
    private void testScaling(long entryCount, long batch, boolean nestedZIPs) throws IOException {
        batch = Math.max(1, Math.min(entryCount, batch));
        TFile zip = new TFile("testarchive_" + entryCount + "_" + batch + (nestedZIPs ? "_nested" : "") + ".zip");
        try {
            createTestArchive(zip, entryCount, batch, nestedZIPs);
            log.debug(String.format(
                    "Iterating with %s of size %dMB, containing %d entries (batch size %d), %susing nested archives",
                    zip.getPath(), zip.length() / 1048576, entryCount, batch, nestedZIPs ? "" : "not "));
            checkArchive(zip, entryCount, batch, nestedZIPs);
            checkArchiveReader(zip, entryCount);
            displayMem();
            log.debug("Finished testing, cleaning up");
        } finally {
//            Files.delete(new File(zip.getPath()));
        }
    }

    // Introduced to test a real-world aleph dump with 35 ~100MB nested ZIP archives
    public void disabledtestSpecificArchive() throws IOException {
        //TFile zip = new TFile("/home/te/tmp/aleph_repack/repacked.zip");
        TFile zip = new TFile("/home/te/tmp/aleph_repack/tst.zip");
        int expected = 3500000;

        log.debug("Creating ArchiveReader for " + zip);
        ArchiveReader reader = new ArchiveReader(Configuration.newMemoryBased(
                ArchiveReader.CONF_FILE_PATTERN, ".*xml",
                ArchiveReader.CONF_RECURSIVE, true,
                ArchiveReader.CONF_ROOT_FOLDER, zip.getPath(),
                ArchiveReader.CONF_COMPLETED_POSTFIX, ""
        ));
        long feedback = expected < 100 ? 10 : expected / 100;
        Profiler profiler = new Profiler(expected, 10000);
        log.debug("Extracting XML-files from " + zip);
        while (reader.hasNext()) {
            Strings.flush(reader.next().getStream());
            if (profiler.getBeats() % feedback == 0) {
                log.debug("Extracted " + profiler.getBeats() + "/" + profiler.getExpectedTotal() +  " files at "
                          + (int)profiler.getBps(true) + " records/sec. ETA at " + profiler.getETAAsString(true));
                displayMem();
            }
            profiler.beat();
        }
        log.debug("Finished processing. Extracted " + profiler.getBeats() + "/" + profiler.getExpectedTotal()
                  +  " files at " + (int)profiler.getBps(true) + " records/sec");
        displayMem();
        log.debug("nulling reader to detect if TrueZIP has any lingering resident structures");
        //noinspection UnusedAssignment
        reader = null;
        displayMem();
    }

    private void checkArchiveReader(TFile zip, long expected) throws IOException {
        ArchiveReader reader = new ArchiveReader(Configuration.newMemoryBased(
                ArchiveReader.CONF_FILE_PATTERN, ".*dat",
                ArchiveReader.CONF_RECURSIVE, true,
                ArchiveReader.CONF_ROOT_FOLDER, zip.getPath(),
                ArchiveReader.CONF_COMPLETED_POSTFIX, ""
        ));
        long feedback = expected < 100 ? 10 : expected / 20;
        Profiler profiler = new Profiler((int)expected, 1000);
        while (reader.hasNext()) {
            Strings.flush(reader.next().getStream());
            profiler.beat();
            if (profiler.getBeats() % feedback == 0) {
                log.debug("Extracted " + profiler.getBeats() + "/" + profiler.getExpectedTotal() +  " files at "
                          + (int)profiler.getBps(true) + " records/sec. ETA at " + profiler.getETAAsString(true));
                displayMem();
            }
        }
        assertEquals("The number of extracted streams should match", expected, profiler.getBeats());
        log.debug("Extracted " + profiler.getBeats() + " records from " + zip + " in " + profiler.getSpendTime());
    }

    private void checkArchive(TFile zip, long entryCount, long batch, boolean nestedZIPs) throws FsSyncException {
        displayMem();
        log.debug("Listing entries in " + zip);
        TFile[] entries = zip.listFiles();
        assertNotNull("There should be entries in the zip " + zip, entries);
        log.debug("Finished listing entries");
        displayMem();
        long expected = entryCount == batch ? entryCount : entryCount/batch;
        assertEquals("The zip " + zip + " should contain " + expected + " entries",
                     expected, entries.length);
        log.debug("Iterating " + entries.length + " entries in " + zip + " and calling isDirectory on each");
        for (TFile entry: entries) {
            if (nestedZIPs || entryCount != batch) {
                assertTrue("The entry " + entry + " should be a Directory", entry.isDirectory());
            } else {
                assertFalse("The entry " + entry + " should not be a Directory", entry.isDirectory());
            }
            TVFS.umount(entry);
        }
        log.debug("Finished isDirectory iteration");
        displayMem();
        log.debug("Iterating " + entries.length + " entries and calling isArchive on each");
        for (TFile entry: entries) {
            if (nestedZIPs) {
                assertTrue("The entry " + entry + " should be an Archive", entry.isArchive());
            } else {
                assertFalse("The entry " + entry + " should not be an Archive", entry.isArchive());
            }
        }
        log.debug("Finished isArchive iteration");
        displayMem();
    }

    private void createTestArchive(TFile zip, long entryCount, long batch, boolean nestedZIPs) throws IOException {
        displayMem();
//        log.debug("Checking for existence of " + zip);
        if (!zip.exists()) {
            log.debug("Creating test file " + zip);
            if (nestedZIPs) {
                createRecursiveZIP(zip, entryCount / batch == 0 ? 1 : entryCount / batch, batch, 1);
            } else {
                createTestFile(zip, entryCount, batch, 1);
            }
        } else {
            log.debug("Test file " + zip + " already exists. Reusing existing file");
        }
        assertTrue("The zip should be marked as an archive", zip.isArchive());
    }

    private void displayMem() {
        long before = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576;
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting for GC to finish");
        }
        long after = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576;
        log.debug("Heap size before and after GC: " + before + "MB / " + after + "MB");
    }

    public void testZIPFilenames() throws Exception {
        testZIPDetection(
            new File(TMP, "zip32.zip").getAbsolutePath());
        // Colon is not supported by TrueZIP 7.0pr1
        //testZIPDetection("data/zip/zip:colon.zip");
        testZIPDetection(
            new File(TMP, "double_stuffed2.zip").getAbsolutePath());
    }

    public void testBasicZIPRename() throws FsSyncException {
        File ZIP = new File(TMP, "zip32.zip");
        TFile TZIP = new TFile(TMP, "zip32.zip");
        assertTrue("The rename should succeed",
                   ZIP.renameTo(new File(TMP, "zip32.zip.finito")));
        TFile.umount(TZIP);
        assertFalse("The old ZIP file should not exist anymore",
                    TZIP.exists());
    }

    public void testBasicZIPRenameWithStream() throws IOException, InterruptedException {
        File ZIP = new File(TMP, "zip32.zip");
        TFile TZIP = new TFile(TMP, "zip32.zip");
        TFileInputStream tin =
            new TFileInputStream(new TFile(TZIP, "flam.xml"));
        //noinspection StatementWithEmptyBody
        while (tin.read() != -1) {
            ;
        }
        tin.close();
        TFile.umount(TZIP);

        assertTrue("The rename should succeed",
                   ZIP.renameTo(new File(TMP, "zip32.zip.finito")));
        assertFalse("The old ZIP file should not exist anymore",
                    TZIP.exists());
    }

    public void testVerifyTrueZIPCapabilities() throws Exception {
        TArchiveDetector detector = TFile.getDefaultArchiveDetector();
        assertTrue("TrueZIP should support ZIP",
                   detector.toString().contains("zip"));
    }

    public void testZIPDetection(String zipString) throws Exception {
        File ZIP = Resolver.getFile(zipString);

        assertNotNull(
            "Resolver.getFile(" + zipString + ") should not return null", ZIP);
        assertTrue("File '" + ZIP + "' should exist using standard Java API",
                   ZIP.exists());

/*        System.out.println("Resolver.getFile(" + zipString + ") == " + ZIP);
        System.out.println("Resolver.getFile(" + zipString + ").toURI() == "
                           + ZIP.toURI());
  */
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
        testRecursive(TMP.getAbsolutePath(), null);
    }

    public void testSimple() throws Exception {
        testRecursive(new File(TMP, "subfolder/").getAbsolutePath(),
                      Arrays.asList("kaboom.xml",
                                    "zoo.xml",
                                    "zoo2.xml"));
    }

    public void testDoubleZIP() {
        testRecursive(new File(TMP, "subdouble/").getAbsolutePath(),
                      Arrays.asList("foo.xml",
                                    "double_zipped_foo2.xml",
                                    "kaboom.xml",
                                    "zoo.xml",
                                    "zoo2.xml",
                                    "non_zipped_foo.xml"));
    }

    public void testRenamePlainFile() {
        assertFile(new File(TMP, "subfolder/zoo.xml"));
        assertNotFile(new File(TMP, "subfolder/zoo.xml.finito"));
        testRecursive(new File(TMP, "subfolder").getAbsolutePath(), null);
        assertFile(new File(TMP, "subfolder/zoo.xml.finito"));
        assertFalse("The renamed file should be a file and not a directory",
                    new File(TMP, "subfolder/zoo.xml.finito").isDirectory());
    }

    public void testRenameZIPSimple() {
        File TST = new File(TMP, "large200.zip");
        File TSTFINISHED = new File(TMP, "large200.zip.finito");

        assertFile(TST);
        assertNotFile(TSTFINISHED);
        testRecursive(TST.getAbsolutePath(), null);
        assertFile(TSTFINISHED);
        assertNotFile(TST);
    }

    public void testRenameEmbedded() {
        assertFile(new File(TMP, "subdouble/sub2/non_zipped_foo.xml"));
        assertFile(new File(
            TMP, "subdouble/sub2/double_stuffed.zip").getAbsolutePath());
        assertFile(
            new File(TMP, "subdouble/sub2/double_stuffed.zip").getAbsolutePath()
            + "/foo.xml");
        testRecursive(new File(TMP, "subdouble/").getAbsolutePath(), null);
        assertFile(new File(TMP, "subdouble/sub2/non_zipped_foo.xml.finito"));
        assertFile(new File(TMP, "subdouble/sub2/double_stuffed.zip.finito"));
        assertNotFile(
            new File(TMP, "subdouble/sub2/double_stuffed.zip").getAbsolutePath()
            + "/foo.xml.finito");
    }

    public void testFolderCreation() throws IOException {
        final File F = new File(TMP, "subdouble/sub2/double_stuffed.zip");
        assertTrue(
            "The file '" + F + "' should exist", F.exists());
        assertFalse(
            "The file '" + F + "' should not be a directory", F.isDirectory());

        TFile tf = new TFile(F);
        assertTrue(
            "The tfile '" + tf + "' should exist", tf.exists());
        assertTrue(
            "The tfile '" + tf + "' should be an archive", tf.isArchive());
        //noinspection ConstantConditions
        assertTrue(
            "The tfile '" + tf.getParentFile() + "' should be a directory",
            tf.getParentFile().isDirectory());

        TFile tif = new TFile(tf, "foo.xml");
        assertTrue(
            "The embedded tfile '" + tif + "' should exist", tif.exists());

        TFileInputStream is = new TFileInputStream(tif);
        OutputStream out = new ByteArrayOutputStream(100);
        Streams.pipe(is, out);
        is.close();

        assertTrue(
            "After streaming, the tfile '" + tf + "' should exist",
            tf.exists());
        assertTrue(
            "After streaming, the tfile '" + tf + "' should be an archive",
            tf.isArchive());
    }

    /* Fails due to curious handling of rename in TFile */
    /*
    public void testBasicRename() throws IOException {
        TFile F = new TFile(TMP, "subdouble/sub2/double_stuffed.zip");
        TFile FR = new TFile(F.getPath() + "completed");
        assertTrue("Rename should work", F.renameTo(FR));
        assertFalse(
            "The old file '" + F + "' should not exist", F.exists());
        assertTrue(
            "The renamed file '" + FR + "' should exist", FR.exists());
        assertFalse(
            "The renamed file '" + FR + "' should be an archive",
            FR.isArchive());
        assertFalse(
            "The renamed file '" + FR + "' should not be a directory",
            FR.isDirectory());
    }
      */
    private void assertFile(File file) {
        assertTrue("The file '" + file.getAbsolutePath() + "' should exist",
                   new TFile(file).exists());
        assertFalse("The file '" + file + "' should not be a directory",
                    file.isDirectory());
    }
    private void assertFile(String path) {
        assertTrue("The file '" + path + "' should exist",
                   new TFile(path).exists());
        assertFalse("The file '" + path + "' should not be a directory",
                    new File(path).isDirectory());
    }
    private void assertNotFile(String path) {
        assertTrue("The file '" + path + "' should not exist",
                   !new TFile(path).exists());
    }
    private void assertNotFile(File file) {
        assertTrue("The file '" + file + "' should not exist",
                   !file.exists());
    }

    public void testRecursive(String source, List<String> expected) {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(FileReader.CONF_ROOT_FOLDER, source);
        conf.set(FileReader.CONF_RECURSIVE, true);
        conf.set(FileReader.CONF_FILE_PATTERN, ".*\\.xml");
        conf.set(FileReader.CONF_COMPLETED_POSTFIX, ".finito");
        ArchiveReader reader = new ArchiveReader(conf);

        ArrayList<String> actual = new ArrayList<String>();
        while (reader.hasNext()) {
            Payload payload = reader.next();
            actual.add(new File((String)
                    payload.getData(Payload.ORIGIN)).getName());
            payload.close();
        }
        if (expected == null) {
/*            for (String a: actual) {
                System.out.println(a);
            }*/
        } else {
            ExtraAsserts.assertEquals(
                "The resulting files should be as expected", expected, actual);
        }
    }

}
