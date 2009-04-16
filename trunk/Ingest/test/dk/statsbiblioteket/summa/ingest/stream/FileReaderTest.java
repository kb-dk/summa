package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.ExtraAsserts;
import dk.statsbiblioteket.util.Files;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class FileReaderTest extends TestCase {
    private static Log log = LogFactory.getLog(FileReader.class);

    public FileReaderTest(String name) {
        super(name);
    }

    public static final File root =
            new File(new File(System.getProperty("java.io.tmpdir")),
                     "loadertempA");
    File sub1 = new File(root, "1");
    File sub2 = new File(root, "2");
    File rootFile10;
    File rootFile20;
    File rootFileFoo20;

    public static final File rootDeep =
            new File(new File(System.getProperty("java.io.tmpdir")),
                     "rootDeep");

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (root.exists()) {
            Files.delete(root);
        }
        assertTrue("The test-folder '" + root + "' should be created",
                   root.mkdirs());
        sub1.mkdirs();
        sub2.mkdirs();
        rootFile10 = makeFile(root, "file10.xml", 10);
        rootFile20 = makeFile(root, "file20.xml", 20);
        rootFileFoo20 = makeFile(root, "file20.foo", 20);
        makeFile(sub1, "file0.xml", 0);
        File sub1File1000 = makeFile(sub1, "file1000.xml", 1000);
        assertEquals("The file size for '" + sub1File1000 + "' should match",
                     1000, sub1File1000.length());
        if (rootDeep.exists()) {
            Files.delete(rootDeep);
        }
        Files.copy(Resolver.getFile(
                "data/filereader/dummyA.xml").getParentFile(), rootDeep, false);
    }

    Random random = new Random();
    private File makeFile(File root, String name, int size) throws IOException {
        File file = new File(root, name);
        FileOutputStream out = new FileOutputStream(file);
        byte[] randoms = new byte[size];
        random.nextBytes(randoms);
        out.write(randoms);
        out.close();
        return file;
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (root.exists()) {
            Files.delete(root);
        }
        if (rootDeep.exists()) {
            Files.delete(rootDeep);
        }
    }

    public static Test suite() {
        return new TestSuite(FileReaderTest.class);
    }

    public void testPlainInputStream() throws Exception {
        InputStream in = new FileInputStream(rootFile10);
        for (int i = 0 ; i < 10 ; i++) {
            assertTrue("Some content should be loaded", in.read() > 0);
        }
        assertEquals("EOF should be reached", -1, in.read());
        in.close();
    }

    public void testSequenceInputStream() throws Exception {
        InputStream in1 = new FileInputStream(rootFile10);
        InputStream in2 = new FileInputStream(rootFile20);
        SequenceInputStream seq = new SequenceInputStream(in1, in2);
        for (int i = 0 ; i < 10+20 ; i++) {
            assertTrue("Some content should be loaded at pos " + i,
                       seq.read() >= 0);
        }
        assertEquals("EOF should be reached", -1, seq.read());
        seq.close();
    }

    public void testSingleFile() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(FileReader.CONF_ROOT_FOLDER, root.toString());
        conf.set(FileReader.CONF_RECURSIVE, true);
        conf.set(FileReader.CONF_FILE_PATTERN, ".*\\.foo");
        conf.set(FileReader.CONF_COMPLETED_POSTFIX, ".finito");
        FileReader reader = new FileReader(conf);
        Payload payload = reader.next();
        assertNotNull("The payload should have a stream", payload.getStream());
        assertEquals("The payload should have meta-info with file name",
                     rootFileFoo20.getPath(),
                     payload.getStringData(Payload.ORIGIN));
        for (int i = 0 ; i < 20 ; i++) {
            assertTrue("reader should not be empty at position " + i + "/"
                       + 20,
                       payload.getStream().read() != Payload.EOF);
        }
        assertEquals("The reader should be empty after "
                     + 20 + " bytes has been read",
                     Payload.EOF, payload.getStream().read());
        payload.close();
        reader.close(true);
        assertTrue("The file should be renamed after close(true)",
                   new File(rootFileFoo20.getPath() + ".finito").exists());
    }

    public void testEmptyThenFull() throws Exception {
        if (root.exists()) {
            Files.delete(root);
        }
        root.mkdirs();
        new File(root, "empty/moreempty").mkdirs();
        new File(root, "full").mkdir();
        new File(new File(root, "full"), "whatever.xml").createNewFile();

        /*
        empty
        empty/moreempty
        full/whatever.xml
         */

        Configuration conf = Configuration.newMemoryBased();
        conf.set(FileReader.CONF_ROOT_FOLDER, root.toString());
        conf.set(FileReader.CONF_RECURSIVE, true);
        conf.set(FileReader.CONF_FILE_PATTERN, ".*\\.xml");
        conf.set(FileReader.CONF_COMPLETED_POSTFIX, ".finito");
        FileReader reader = new FileReader(conf);
        assertTrue("There should be at least one file", reader.hasNext());
        Payload payload = reader.next();
        assertNotNull("A payload should be recoived", payload);
        payload.close();
        if (reader.hasNext()) {
            fail("There should be no more files. Got " + reader.next());
        }
        reader.close(true);
    }

    // TODO: Check for file rename, depending on success

    public void testMultipleFiles() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(FileReader.CONF_ROOT_FOLDER, root.toString());
        conf.set(FileReader.CONF_RECURSIVE, true);
        conf.set(FileReader.CONF_FILE_PATTERN, ".*\\.xml");
        conf.set(FileReader.CONF_COMPLETED_POSTFIX, ".fin");
        FileReader reader = new FileReader(conf);
        int filecount = 0;
        Payload payload;
        while (reader.hasNext()) {
            payload = reader.next();
            log.debug("Got payload with filename '"
                      + payload.getStringData(Payload.ORIGIN));
            filecount++;
        }
        assertEquals("The number of streams should match", 4, filecount);
//        reader.close(false);
//        assertTrue("No renaming should take place with close(false)",
//                   rootFile10.exists());
        reader.close(true);
        assertTrue("Renaming should take place with close(true)",
                   new File(rootFile10.getPath() + ".fin").exists());
    }

    private static final List<String> expectedOrder = Arrays.asList(
            "dummyA.xml", "dummyF.xml", "dummyE.xml", "dummyD.xml",
            "dummyC.xml");
    public void testDeep() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(FileReader.CONF_ROOT_FOLDER, rootDeep.toString());
        conf.set(FileReader.CONF_RECURSIVE, true);
        conf.set(FileReader.CONF_FILE_PATTERN, ".*\\.xml");
        conf.set(FileReader.CONF_COMPLETED_POSTFIX, ".finito");
        FileReader reader = new FileReader(conf);

        ArrayList<String> actual = new ArrayList<String>(expectedOrder.size());
        while (reader.hasNext()) {
            Payload payload = reader.next();
            actual.add(new File((String)
                    payload.getData(Payload.ORIGIN)).getName());
            payload.close();
        }
        ExtraAsserts.assertEquals("The resulting files should be as expected",
                                  expectedOrder, actual);
    }
}