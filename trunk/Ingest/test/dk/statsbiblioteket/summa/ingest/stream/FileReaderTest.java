package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.stream.StreamFilter;
import dk.statsbiblioteket.util.Files;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.Random;

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
    File rootFileFoo;

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
        rootFileFoo = makeFile(root, "file20.foo", 20);
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
        out.close();
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
        StreamFilter.MetaInfo meta = StreamFilter.MetaInfo.getMetaInfo(reader);
        log.debug("Got stream with id '" + meta.getId()
                  + "' and length " + meta.getContentLength()
                  + ". Available bytes: " + reader.available());
        for (int i = 0 ; i < meta.getContentLength() ; i++) {
            assertTrue("reader should not be empty at position " + i + "/"
                       + meta.getContentLength(),
                       reader.read() != StreamFilter.EOF);
        }
        assertEquals("The reader should be empty after "
                     + meta.getContentLength() + " bytes has been read",
                     StreamFilter.EOF, reader.read());
        reader.close(true);
        assertTrue("The file should be renamed after close(true)",
                   new File(rootFileFoo.getPath() + ".finito").exists());
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
        StreamFilter.MetaInfo meta;
        while ((meta = StreamFilter.MetaInfo.getMetaInfo(reader)) != null) {
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
