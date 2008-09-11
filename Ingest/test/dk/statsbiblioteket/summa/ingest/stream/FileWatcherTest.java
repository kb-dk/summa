package dk.statsbiblioteket.summa.ingest.stream;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.Files;

/**
 * FileWatcher Tester.
 *
 * @author <Authors name>
 * @since <pre>09/11/2008</pre>
 * @version 1.0
 */
public class FileWatcherTest extends TestCase {
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
        int ST = 1500;

        Configuration conf = Configuration.newMemoryBased();
        conf.set(FileReader.CONF_ROOT_FOLDER, FileReaderTest.root.toString());
        conf.set(FileReader.CONF_RECURSIVE, true);
        conf.set(FileReader.CONF_FILE_PATTERN, ".*\\.xml");
        conf.set(FileReader.CONF_COMPLETED_POSTFIX, ".fin");
        conf.set(FileWatcher.CONF_POLL_INTERVAL, 500);
        FileWatcher reader = new FileWatcher(conf);
        new Poller(reader).start();

        Thread.sleep(100);
        assertEquals("There should be no files to start with",
                     0, received.size());

        new File(FileReaderTest.root, "foo.bar").createNewFile();
        Thread.sleep(ST);
        assertEquals("There should be no valid files",
                     0, received.size());

        new File(FileReaderTest.root, "foo.xml").createNewFile();
        Thread.sleep(ST);
        assertEquals("There should be a valid file",
                     1, received.size());
        doRun = false;
    }

    private List<Payload> received = new ArrayList<Payload>(10);
    private boolean doRun = true;
    public class Poller extends Thread {
        private FileWatcher reader;
        public Poller(FileWatcher reader) {
            this.reader = reader;
            doRun = true;
        }
        public void run() {
            while (doRun) {
                if (reader.hasNext()) {
                    received.add(reader.next());
                }
            }
        }
    }
}
