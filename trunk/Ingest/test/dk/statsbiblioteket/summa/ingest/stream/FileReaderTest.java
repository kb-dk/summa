package dk.statsbiblioteket.summa.ingest.stream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.stream.StreamFilter;
import dk.statsbiblioteket.util.Files;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileReaderTest extends TestCase {
    private static Log log = LogFactory.getLog(FileReader.class);

    public FileReaderTest(String name) {
        super(name);
    }

    public static final File root =
            new File(new File(System.getProperty("java.io.tmpdir")),
                     "loadertemp");
    File sub1 = new File(root, "1");
    File sub2 = new File(root, "2");

    public void setUp() throws Exception {
        super.setUp();
        if (root.exists()) {
            Files.delete(root);
        }
        assertTrue("The test-folder '" + root + "' should be created",
                   root.mkdirs());
        sub1.mkdirs();
        sub2.mkdirs();
        File rootFile10 = makeFile(root, "file10.xml", 10);
        File rootFile20 = makeFile(root, "file20.xml", 10);
        File rootFileSkip = makeFile(root, "file20.foo", 10);
        File sub1File0 = makeFile(sub1, "file0.xml", 0);
        File sub1File1000 = makeFile(sub1, "file1000.xml", 1000);
        assertEquals("The file size for '" + sub1File1000 + "' should match",
                     1000, sub1File1000.length());
    }

    Random random = new Random();
    private File makeFile(File root, String name, int size) throws IOException {
        File file = new File(root, name);
        FileOutputStream out = new FileOutputStream(file);
        byte[] randoms = new byte[size];
        random.nextBytes(randoms);
        out.write(randoms);
        return file;
    }

    public void tearDown() throws Exception {
        super.tearDown();
        if (root.exists()) {
            Files.delete(root);
        }
    }

    public static Test suite() {
        return new TestSuite(FileReaderTest.class);
    }

    public void testSingleFile() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(FileReader.CONF_ROOT_FOLDER, root.toString());
        conf.set(FileReader.CONF_RECURSIVE, true);
        conf.set(FileReader.CONF_FILE_PATTERN, ".*\\.foo");
        conf.set(FileReader.CONF_COMPLETED_POSTFIX, ".finito");
        FileReader reader = new FileReader(conf);
        log.debug("Available bytes in reader: " + reader.available());
        assertTrue("Something should be available", reader.available() > 0);
        StreamFilter.MetaInfo meta = new StreamFilter.MetaInfo(reader);
        log.debug("Got stream with id '" + meta.getId()
                  + "' and length " + meta.getContentLength()
                  + ". Available bytes: " + reader.available());
        for (int i = 0 ; i < meta.getContentLength() ; i++) {
            assertTrue("reader should not be empty yet",
                       reader.read() != StreamFilter.EOF);
        }
        assertEquals("The reader should be empty",
                     StreamFilter.EOF, reader.read());
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
        assertTrue("Something should be available", reader.available() > 0);
        int filecount = 0;
        while (reader.available() > 0) {
            StreamFilter.MetaInfo meta = new StreamFilter.MetaInfo(reader);
            log.debug("Got stream with id '" + meta.getId()
                      + "' and length '" + meta.getContentLength());
            for (int i = 0 ; i < meta.getContentLength() ; i++) {
                assertTrue("reader should not be empty yet",
                           reader.read() != StreamFilter.EOF);
            }
            filecount++;
        }
        assertEquals("The number of streams should match", 4, filecount);
        assertEquals("The reader should be empty", 
                     StreamFilter.EOF, reader.read());
        reader.close(true);
    }
}
