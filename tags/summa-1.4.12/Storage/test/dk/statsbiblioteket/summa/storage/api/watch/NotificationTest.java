package dk.statsbiblioteket.summa.storage.api.watch;

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.File;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.summa.storage.database.derby.DerbyStorage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.rmi.RMIStorageProxy;
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
    static final String testId2 = "testId2";
    static final String base1 = "base1";
    static final String base2 = "base2";
    static final String base3 = "base3";
    static final byte[] testContent1 = "Summa rocks your socks!".getBytes();

    long testStartTime;
    Storage storage;

    StorageWatcher w;
    Listener l1;
    Listener l2, l3, l4;

    public static Configuration createConf () throws Exception {

        Configuration conf = Configuration.newMemoryBased(
                RMIStorageProxy.CONF_BACKEND,
                H2Storage.class,
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
        l1 = new Listener();
        l2 = new Listener();
        l3 = new Listener();
        l4 = new Listener();

        w.start();
        /* Make sure that the poller is running */
        Thread.sleep(w.getPollInterval()+1000);
    }

    public void tearDown () throws Exception {
        w.stop();
        storage.close();
    }

    public void testNotifyAll () throws Exception {
        w.addListener(l1, null, "userData");

        storage.flush(new Record(testId1, base1, testContent1));

        Thread.sleep(w.getPollInterval()+1000);

        assertEquals(1, l1.events.size());
        assertEquals("userData", l1.events.get(0).userData);
        assertEquals(null, l1.events.get(0).base);
    }

    public void testNotifyMany () throws Exception {
        w.addListener(l1, null, "userData1");
        w.addListener(l2, Arrays.asList(base1), "userData2");
        w.addListener(l3, Arrays.asList(base2), "userData3");
        w.addListener(l4, Arrays.asList(base3), "userData4");

        storage.flush(new Record(testId1, base1, testContent1));
        storage.flush(new Record(testId2, base2, testContent1));

        Thread.sleep(w.getPollInterval()+1000);

        assertEquals(1, l1.events.size());
        assertEquals("userData1", l1.events.get(0).userData);
        assertEquals(null, l1.events.get(0).base);

        assertEquals(1, l2.events.size());
        assertEquals("userData2", l2.events.get(0).userData);
        assertEquals(base1, l2.events.get(0).base);

        assertEquals(1, l3.events.size());
        assertEquals("userData3", l3.events.get(0).userData);
        assertEquals(base2, l3.events.get(0).base);

        assertEquals(0, l4.events.size());        
    }

    public void testNotifyMultipleEvents () throws Exception {
        w.addListener(l1, Arrays.asList(base1), "userData1");
        w.addListener(l2, Arrays.asList(base2), "userData2");

        storage.flush(new Record(testId1, base1, testContent1));

        Thread.sleep(w.getPollInterval()+1000);

        storage.flush(new Record(testId2, base1, testContent1));

        Thread.sleep(w.getPollInterval()+1000);

        assertEquals(2, l1.events.size());
        assertEquals("userData1", l1.events.get(0).userData);
        assertEquals("userData1", l1.events.get(1).userData);
        assertEquals(base1, l1.events.get(0).base);
        assertEquals(base1, l1.events.get(1).base);

        assertEquals(0, l2.events.size());        
    }
}
