package dk.statsbiblioteket.summa.storage.database.derby;

import java.io.File;
import java.rmi.RemoteException;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.database.DatabaseControl;
import dk.statsbiblioteket.util.Files;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * ControlDerby Tester.
 *
 * @author <Authors name>
 * @since <pre>02/11/2008</pre>
 * @version 1.0
 */
public class ControlDerbyTest extends TestCase {
    public ControlDerbyTest(String name) {
        super(name);
    }

    private static final File location =
            new File(System.getProperty("java.io.tmpdir"), "kablam");

    public void setUp() throws Exception {
        super.setUp();
        if (location.exists()) {
            Files.delete(location);
        }
    }

    public void tearDown() throws Exception {
        super.tearDown();
        if (location.exists()) {
            Files.delete(location);
        }
    }

    public void testCreateDatabase() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        //noinspection DuplicateStringLiteralInspection
        conf.set(DatabaseControl.PROP_LOCATION, location.toString());
        DatabaseControl control = new ControlDerby(conf);
        control.close();
        assertTrue("The location '" + location + "' should exist",
                   location.exists());
    }

    public void testReconnect() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        //noinspection DuplicateStringLiteralInspection
        conf.set(DatabaseControl.PROP_LOCATION, location.toString());
        DatabaseControl control = new ControlDerby(conf);
        control.close();
        assertTrue("The location '" + location + "' should exist",
                   location.exists());
        try {
            control = new ControlDerby(conf);
        } catch (RemoteException e) {
            fail("It should be possible to reopen access to database");
            e.printStackTrace();
        }
        control.close();
    }

    public void testFailedConnection() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        //noinspection DuplicateStringLiteralInspection
        conf.set(DatabaseControl.PROP_LOCATION, location.toString());
        conf.set(DatabaseControl.PROP_CREATENEW, false);
        try {
            DatabaseControl control = new ControlDerby(conf);
            fail("createNew was false, so construction of ControlDerby "
                 + "should fail");
            control.close();
        } catch (Exception e) {
            // Expected
        }
    }

    public void testProtectedAccess() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        //noinspection DuplicateStringLiteralInspection
        conf.set(DatabaseControl.PROP_LOCATION, location.toString());
        conf.set(DatabaseControl.PROP_USERNAME, "foo");
        conf.set(DatabaseControl.PROP_PASSWORD, "bar");
        DatabaseControl control = new ControlDerby(conf);
        control.close();
        assertTrue("The location '" + location + "' should exist",
                   location.exists());

        try {
            control = new ControlDerby(conf);
        } catch (RemoteException e) {
            fail("It should be possible to access a protected database");
            e.printStackTrace();
        }
        control.close();

        conf.set(DatabaseControl.PROP_PASSWORD, "zoo");
        try {
            control = new ControlDerby(conf);

            assertFalse("It should not be possible to access a protected"
                        + " database with the wrong credidentials", 
                        control.getDatabaseInfo().length() > 0);
        } catch (RemoteException e) {
            // Expected
        }
        control.close();


        // Authentication-failed test disabled at ControlDerby does not
        // currently support authentication
 /*       conf = Configuration.newMemoryBased();
         //noinspection DuplicateStringLiteralInspection
        conf.set(DatabaseControl.PROP_LOCATION, location.toString());
        try {
            control = new ControlDerby(conf);
            fail("It should not be possible to access a protected database " 
                 + "without any credidentials");
        } catch (RemoteException e) {
            // Expected
        }
        control.close();*/

    }

    public void testGetDatabaseInfo() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        //noinspection DuplicateStringLiteralInspection
        conf.set(DatabaseControl.PROP_LOCATION, location.toString());
        DatabaseControl control = new ControlDerby(conf);
        assertTrue("getDatabaseInfo should return something",
                   control.getDatabaseInfo().length() > 0);
    }


    public static Test suite() {
        return new TestSuite(ControlDerbyTest.class);
    }
}
