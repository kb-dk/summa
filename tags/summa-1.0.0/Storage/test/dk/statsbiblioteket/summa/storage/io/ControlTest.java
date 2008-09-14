package dk.statsbiblioteket.summa.storage.io;

import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.StringMap;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.RecordIterator;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.util.Files;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * This JUnit-test is written with ControlDerby testing in mind. However, it
 * is kept fairly generic so later evolvement to tests for other implementations
 * shouldn't be hard.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class ControlTest extends TestCase {
    public ControlTest(String name) {
        super(name);
    }
    // TODO: Test iterating deleted and not indexable records
    static int testdirCounter = 0;
    Storage control;
    File location;
    private Configuration conf;

    public void setUp() throws Exception {
        super.setUp();
        location = new File(System.getProperty("java.io.tmpdir"), "controltest"
                                                            + testdirCounter++);
        conf = Configuration.newMemoryBased();
        conf.set(DatabaseStorage.PROP_LOCATION, location.toString());

        if (location.exists()) {
            Files.delete(location);
            if (location.exists()) {
                fail("Failed to remove '" + location + "' on setUp");
            }
        }
        control = StorageFactory.createStorage(conf);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        control.close();
        // We don't delete the files as the Derby JDBC-driver maintains a lock
    }

    public static Test suite() {
        return new TestSuite(ControlTest.class);
    }

    public void testFlush() throws Exception {
        Record record = new Record("foo", "bar", new byte[0]);
        control.flush(record);

        Record storedRecord = control.getRecord("foo");
        assertEquals("The base for the stored record should be correct", 
                     "bar", storedRecord.getBase());
    }

    public void testErrorHandlingOnFaultyNew() throws Exception {
        Record record = new Record("foo", "bar", new byte[0]);
        control.flush(record);
        try {
            control.flush(record);
            fail("An exception should be thrown when reflushing a record marked"
                 + " as new");
        } catch (RemoteException e) {
            System.out.println("Expected exception thrown: " + e.getMessage());
        }
    }

    public void testGet() throws Exception {
        long time = System.currentTimeMillis();
        List<String> children = new ArrayList<String>(2);
        StringMap meta = new StringMap(10);
        meta.put("", "=/e/n//");
        meta.put("hello world", "");
        meta.put("plain", "case");
        children.add("ping1");
        children.add("ping2");
        Record original = new Record("foos", "bary", false, false,
                                     new byte[]{(byte)2}, time, time, "boo2",
                                     children, meta);
        // Create new
        control.flush(original);

        Record retrieved = control.getRecord("foos");
        assertEquals("The retrieved record should be equal to the stored one",
                     original, retrieved);

        retrieved.setIndexable(true);
        retrieved.getMeta().put("new one", "here");
        retrieved.touch();
        // Update existing
        control.flush(retrieved);
        Record updatedRetrieved = control.getRecord("foos");
        assertEquals("The updated Record should be intact after storing",
                     retrieved, updatedRetrieved);
    }

    public void testModified() throws Exception {
        long time = System.currentTimeMillis();
        Record record = new Record("foo", "bar", false, true, 
                                   new byte[]{(byte)1}, time, time, "boo",
                                   null, null);
        control.flush(record);
        assertEquals("Requesting the new stored record shouldn't change "
                     + "anything", record, control.getRecord("foo"));

        time++;
        List<String> children = new ArrayList<String>(2);
        children.add("ping1");
        children.add("ping2");
        Record modified = new Record("foo", "bar2", false, false,
                                   new byte[]{(byte)2}, time, time, "boo2",
                                   children, null);
        modified.setModificationTime(time + 5000);

        assertTrue("The record should be classified as modified",
                   modified.isModified());
        try {
            control.flush(modified);
        } catch (RemoteException e) {
            e.printStackTrace();
            fail("Flushing a modified record should not give an exception");
        }
        Record extracted = control.getRecord("foo");
        assertEquals("The modified base should be reflected",
                     modified, extracted);
    }

    public void testModifiedFail() throws Exception {
        Record record = new Record("foo", "bar", new byte[0]);
        record.setModificationTime(System.currentTimeMillis() + 5000);
        try {
            control.flush(record);
        } catch (RemoteException e) {
            fail("Flushing a non-existing modified record should not give an "
                 + "exception");
        }
    }

    public void testDeleted() throws Exception {
        Record record = new Record("foo", "bar", new byte[0]);
        control.flush(record);
        record.setDeleted(true);
        try {
            control.flush(record);
        } catch (RemoteException e) {
            e.printStackTrace();
            fail("Flushing a deleted record should not give an exception");
        }

        // deleting non-existing record does not necessarily means exception
/*        Record nonExisting = new Record("spax", "ghj", new byte[0]);
        try {
            control.flush(nonExisting);
        } catch (RemoteException e) {
            fail("Flushing a non-existing deleted record should give an"
                 + " exception");
        }
  */
    }

    public void testGetRecords() throws Exception {
        control.flush(new Record("foo1", "bar", new byte[0]));
        control.flush(new Record("foo2", "bar", new byte[0]));
        control.flush(new Record("zoo1", "baz", new byte[0]));
        control.flush(new Record("zoo2", "baz", new byte[0]));

        for (String base: new String[]{"bar", "baz"}) {
            RecordIterator i = control.getRecords(base);
            int counter = 0;
            while (i.hasNext()) {
                assertEquals("All records should be from the right base",
                             base, i.next().getBase());
                counter++;
            }
            assertEquals("There should be the correct number of records in base" 
                         + " " + base, 2, counter);
        }
    }

    public void testGetRecordsModifiedAfter() throws Exception {
        long now = System.currentTimeMillis();
        for (int i = 0 ; i < 10 ; i++) {
            Record record = new Record("bar" + i, "foo", new byte[0]);
            record.setModificationTime(now + 10000 * i);
            control.flush(record);
        }
        RecordIterator i = control.getRecordsModifiedAfter(now + 40000, "foo");
        assertEquals("The number of extracted Records from the middle should "
                     + "match",
                     5, countRecords(i));

        i = control.getRecordsModifiedAfter(now - 40000, "foo");
        assertEquals("The number of extracted Records from before start",
                     10, countRecords(i));

        i = control.getRecordsModifiedAfter(now + 900000, "foo");
        assertEquals("The number of extracted Records from after end should "
                     + "match",
                     0, countRecords(i));

        i = control.getRecordsModifiedAfter(now + 900000, "baons");
        assertEquals("The number of extracted Records from an empty base should"
                     + " match", 
                     0, countRecords(i));
    }

    public void testGetRecordsFrom() throws Exception {
        String[] ids = new String[]{"b1", "a1", "a0", "b7", "b2"};
        String from = "a1";
        for (String id: ids) {
            control.flush(new Record(id, "bar", new byte[0]));
        }

        String[] expected = Arrays.copyOf(ids, ids.length);
        Arrays.sort(expected);
        expected = Arrays.copyOfRange(expected,
                                      Arrays.binarySearch(expected, from)+1,
                                      expected.length);

        List<String> result = new ArrayList<String>(expected.length);
        RecordIterator i = control.getRecordsFrom(from, "bar");
        while (i.hasNext()) {
            Record record = i.next();
            result.add(record.getId());
        }
        assertEquals("The length of expected and result should be equal",
                     expected.length, result.size());
        for (int j = 0 ; j < expected.length ; j++) {
            assertEquals("The id at position " + j + " should be the same for " 
                         + "expected and result", expected[j], result.get(j));
        }
    }

    public void testGetRecord() throws Exception {
        control.flush(new Record("foo", "bar", new byte[0]));
        control.flush(new Record("baz", "bar", new byte[0]));
        assertEquals("The extracted record should have the right base",
                     "bar", control.getRecord("foo").getBase());
        assertNull("Requesting a non-existing Record should return null",
                   control.getRecord("bada-boom"));
    }

    public void testRecordExists() throws Exception {
        control.flush(new Record("foo", "bar", new byte[0]));
        control.flush(new Record("baz", "bar", new byte[0]));
        assertTrue("The record foo should exist", control.recordExists("foo"));
        assertFalse("The record w3 should not exist",
                    control.recordExists("w3"));
    }

    @SuppressWarnings({"deprecation"})
    public void testRecordActive() throws Exception {
        control.flush(new Record("foo", "bar", new byte[0]));
        Record deleted = new Record("baz", "bar", new byte[0]);
        control.flush(deleted);
        deleted.setDeleted(true);
        deleted.touch();
        control.flush(deleted);
        Record notactive = new Record("zoo", "bar", new byte[0]);
        control.flush(notactive);
        notactive.setIndexable(false);
        notactive.touch();
        control.flush(notactive);

        assertTrue("The plain record foo should be active",
                   control.recordActive("foo"));
        assertFalse("The deleted record baz should not be active",
                   control.recordActive("baz"));
        assertFalse("The non-indexable record zoo should not be active",
                   control.recordActive("zoo"));
        assertFalse("The non-existing record nada should not be active", 
                   control.recordActive("nada"));
    }

    private int countRecords(RecordIterator iterator) {
        int counter = 0;
        while (iterator.hasNext()) {
            iterator.next();
            counter++;
        }
        return counter;
    }
}



