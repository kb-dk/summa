package dk.statsbiblioteket.summa.storage.api.watch;

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;
import java.io.File;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.derby.DerbyStorage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.util.Files;

/**
 *
 */
public class NotificationTest extends TestCase {

    static class Event {
        public StorageWatcher watcher;
        public String base;
        public long timeStamp;
        public Object userData;

        public Event (StorageWatcher w, String b, long t, Object ud) {
            watcher = w;
            base = b;
            timeStamp = t;
            userData = ud;
        }
    }

    static class Listener implements StorageChangeListener {

        public List<Event> events;

        public Listener () {
            events = new ArrayList<Event>();
        }

        public void storageChanged(StorageWatcher watch, String base, long timeStamp, Object userData) {
            events.add (new Event(watch, base, timeStamp, userData));
        }
    }

    static final String testDBLocation = "test_db";
    static int storageCounter = 0;

    static final String testId1 = "testId1";
    static final String base1 = "base1";
    static final byte[] testContent1 = "Summa rocks your socks!".getBytes();

    long testStartTime;
    Storage storage;

    StorageWatcher w;
    Listener l;

    public static Configuration createConf () throws Exception {

        Configuration conf = Configuration.newMemoryBased(
                Storage.CONF_CLASS,
                DerbyStorage.class,
                DatabaseStorage.CONF_LOCATION,
                testDBLocation + (storageCounter++),
                DatabaseStorage.CONF_FORCENEW,
                true
        );

        return conf;
    }

    public void setUp () throws Exception {
        if (new File(testDBLocation + storageCounter).exists()) {
            Files.delete (testDBLocation + storageCounter);
        }

        storage = StorageFactory.createStorage(createConf());
        testStartTime = System.currentTimeMillis();

        w = new StorageWatcher(storage, 1000);
        l = new Listener();

        w.start();
        /* Make sure that the poller is running */
        Thread.sleep(w.getPollInterval()+1000);
    }

    public void tearDown () throws Exception {
        w.stop();
        storage.close();
    }

    public void testNotifyAll () throws Exception {
        w.addListener(l, null, "userData");

        storage.flush(new Record(testId1, base1, testContent1));

        Thread.sleep(w.getPollInterval()+1000);

        assertEquals(1, l.events.size());
        assertEquals("userData", l.events.get(0).userData);
        assertEquals(null, l.events.get(0).base);
    }

}
