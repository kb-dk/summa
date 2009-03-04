package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.Files;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * FileWatcher Tester.
 *
 * @author <Authors name>
 * @since <pre>09/11/2008</pre>
 * @version 1.0
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class FileWatcherTest extends TestCase {
    private static Log log = LogFactory.getLog(FileWatcherTest.class);

    public FileWatcherTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        if (FileReaderTest.root.exists()) {
            Files.delete(FileReaderTest.root);
        }
        FileReaderTest.root.mkdirs();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(FileWatcherTest.class);
    }

    public void testDiscovery() throws Exception {
        int ST = 500;

        Configuration conf = Configuration.newMemoryBased();
        conf.set(FileReader.CONF_ROOT_FOLDER, FileReaderTest.root.toString());
        conf.set(FileReader.CONF_RECURSIVE, true);
        conf.set(FileReader.CONF_FILE_PATTERN, ".*\\.xml");
        conf.set(FileReader.CONF_COMPLETED_POSTFIX, ".fin");
        conf.set(FileWatcher.CONF_POLL_INTERVAL, 100);
        FileWatcher reader = new FileWatcher(conf);
        new Poller(reader).start();

        Thread.sleep(100);
        assertEquals("There should be no files to start with",
                     0, received.size());

        log.debug("Creating test file foo.bar");
        new File(FileReaderTest.root, "foo.bar").createNewFile();
        Thread.sleep(ST);
        assertEquals("There should be no valid files",
                     0, received.size());

        log.debug("Creating test file foo.xml");
        new File(FileReaderTest.root, "foo.xml").createNewFile();
        Thread.sleep(ST);
        assertEquals("There should be a valid file",
                     1, received.size());
        assertTrue("The file should exist", exists(0, null));

        received.get(0).close();
        assertTrue("The file should be renamed", exists(0, ".fin"));

        log.debug("Touching 2 new files");
        new File(FileReaderTest.root, "bar.xml").createNewFile();
        new File(FileReaderTest.root, "zoo.xml").createNewFile();
        Thread.sleep(ST);
        assertEquals("There should be two extra files",
                     3, received.size());

        log.debug("Closing reader");
        reader.close(false);
        log.debug("Reader closed");
        assertTrue("The files should not be renamed on close(false)",
                   exists(1, null));

        doRun = false;
        log.debug("Test ending");
    }

    public void testReverse() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(FileReader.CONF_ROOT_FOLDER, FileReaderTest.root.toString());
        conf.set(FileReader.CONF_RECURSIVE, true);
        conf.set(FileReader.CONF_FILE_PATTERN, ".*\\.xml");
        conf.set(FileReader.CONF_COMPLETED_POSTFIX, ".fin");
        conf.set(FileWatcher.CONF_POLL_INTERVAL, 100);
        new File(FileReaderTest.root, "fooA.xml").createNewFile();
        new File(FileReaderTest.root, "fooB.xml").createNewFile();
        FileWatcher reader = new FileWatcher(conf);
        new Poller(reader).start();

        Thread.sleep(100);
        reader.close(true);

        assertEquals("There should be 2 files received", 2, received.size());
        assertEquals("The first file should be as expected",
                     "fooA.xml", getOriginFile(0).getName());

        received.clear();

        conf.set(FileReader.CONF_REVERSE_SORT, true);
        new File(FileReaderTest.root, "zooA.xml").createNewFile();
        new File(FileReaderTest.root, "zooB.xml").createNewFile();
        reader = new FileWatcher(conf);
        new Poller(reader).start();

        Thread.sleep(500);
        reader.close(true);
        assertEquals("There should be 2 new files received",
                     2, received.size());
        assertEquals("The first file should be as expected (reverse order)",
                     "zooB.xml", getOriginFile(0).getName());

    }

    private boolean exists(int index, String prefix) {
        File file = getOriginFile(index);
        return prefix == null ? file.exists() :
               new File(file.toString() +  prefix).exists();
    }

    private File getOriginFile(int index) {
        return ((FileReader.RenamingFileStream)received.get(index).
                getStream()).getFile();
    }

    private List<Payload> received = new ArrayList<Payload>(10);
    private boolean doRun = true;
    public class Poller extends Thread {
        private FileWatcher reader;
        public Poller(FileWatcher reader) {
            this.reader = reader;
            doRun = true;
        }
        @Override
        public void run() {
            while (doRun && reader.hasNext()) {
                received.add(reader.next());
            }
            doRun = false;
        }
    }
}
