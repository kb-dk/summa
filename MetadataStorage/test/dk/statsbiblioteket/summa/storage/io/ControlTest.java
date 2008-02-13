package dk.statsbiblioteket.summa.storage.io;

import java.io.File;
import java.rmi.RemoteException;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.StorageFactory;
import dk.statsbiblioteket.summa.storage.database.DatabaseControl;
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

    Control control;
    static final File location =
            new File(System.getProperty("java.io.tmpdir"), "controltest");
    private static Configuration conf;
    {
        conf = Configuration.newMemoryBased();
        conf.set(DatabaseControl.PROP_LOCATION, location.toString());
    }

    public void setUp() throws Exception {
        super.setUp();
        if (location.exists()) {
            Files.delete(location);
            if (location.exists()) {
                fail("Failed to remove '" + location + "' on setUp");
            }
        }
        control = StorageFactory.createController(conf);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        control.close();
        Files.delete(location);
        if (location.exists()) {
            fail("Failed to remove '" + location + "' on tearDown");
        }
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

    public void testModified() throws Exception {
        Record record = new Record("foo", "bar", new byte[0]);
        control.flush(record);
        record.setModificationTime(System.currentTimeMillis() + 1);
        try {
            control.flush(record);
        } catch (RemoteException e) {
            e.printStackTrace();
            fail("Flushing a modified record should not give an exception");
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
}
