package dk.statsbiblioteket.summa.storage.database.derby;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.database.DatabaseControl;
import dk.statsbiblioteket.summa.storage.io.Control;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.RMIConnectionFactory;
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
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class ControlDerbyTest extends TestCase {
    public ControlDerbyTest(String name) {
        super(name);
    }

    /* Since the JDBC-driver maintains locks on all opened databases,
       regardless of close, we need to create a new database for each test.
    */
    private static File getLocation() {
        int baseCounter = 0;
        File location;
        //noinspection StatementWithEmptyBody
        while ((location = new File(System.getProperty("java.io.tmpdir"),
                                    "kablam" + baseCounter++)).exists());
        return location;
    }

    public void setUp() throws Exception {
        super.setUp();
        deleteFiles();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        // Deletion probably won't work here due to locks.
        // Proper clean-up will be performed on the next full run of the
        // tests in this TestCase.
        deleteFiles();
    }

    /* Attempts to delete databases until there are no databases left */
    private void deleteFiles() throws Exception {
        int baseCounter = 0;
        File location;
        while ((location = getLocation()).exists()) {
            try {
                Files.delete(location);
            } catch (IOException e) {
                System.err.println("Could not delete " + location
                                   + ". Probably due to the JDBC-driver holding"
                                   + " a lock");
            }
        }
    }

    public void testCreateDatabase() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        //noinspection DuplicateStringLiteralInspection
        File location = getLocation();
        conf.set(DatabaseControl.PROP_LOCATION, location.toString());
        DatabaseControl control = new ControlDerby(conf);
        control.close();
        assertTrue("The location '" + location + "' should exist",
                   location.exists());
    }

    public void testReconnect() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        //noinspection DuplicateStringLiteralInspection
        File location = getLocation();
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
        File location = getLocation();
        conf.set(DatabaseControl.PROP_LOCATION, location.toString());
        conf.set(DatabaseControl.PROP_CREATENEW, false);
        try {
            DatabaseControl control = new ControlDerby(conf);
            fail("createNew was false, so construction of ControlDerby "
                 + "should fail");
            control.close();
        } catch (RemoteException e) {
            // Expected
        }
    }

    public void testProtectedAccess() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        //noinspection DuplicateStringLiteralInspection
        File location = getLocation();
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

        // Authentication-failed test disabled at ControlDerby does not
        // currently support authentication
 /*        conf.set(DatabaseControl.PROP_PASSWORD, "zoo");
        try {
            control = new ControlDerby(conf);

            assertFalse("It should not be possible to access a protected"
                        + " database with the wrong credidentials", 
                        control.getDatabaseInfo().length() > 0);
        } catch (RemoteException e) {
            // Expected
        }
        control.close();


       conf = Configuration.newMemoryBased();
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
        File location = getLocation();
        conf.set(DatabaseControl.PROP_LOCATION, location.toString());
        DatabaseControl control = new ControlDerby(conf);
        assertTrue("getDatabaseInfo should return something",
                   control.getDatabaseInfo().length() > 0);
    }

    public void testRMI() throws Exception {
        /*
        Configuration conf = Configuration.newMemoryBased();
        //noinspection DuplicateStringLiteralInspection
        File location = getLocation();
        conf.set(DatabaseControl.PROP_LOCATION, location.toString());
        DatabaseControl control = new ControlDerby(conf);
        assertTrue("getDatabaseInfo should return something",
                   control.getDatabaseInfo().length() > 0);
        control.flush(new Record("foow", "bar", new byte[10]));

        // TODO: Move this to Control(?) in order to avoid MS-module dependency
        ConnectionFactory<Control> cf = new RMIConnectionFactory<Control>();
        ConnectionManager<Control> cm = new ConnectionManager<Control>(cf);
        ConnectionContext<Control> ctx = cm.get("//localhost:2767/ping_service");
        Control remoteControl = ctx.getConnection();
        assertEquals("getRecord should return the ingested Record",
                     "bar", remoteControl.getRecord("foow").getBase());
                     */
    }

    public static Test suite() {
        return new TestSuite(ControlDerbyTest.class);
    }
}
