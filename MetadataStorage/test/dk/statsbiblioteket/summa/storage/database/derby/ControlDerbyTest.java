package dk.statsbiblioteket.summa.storage.database.derby;

import java.io.File;

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

    public void testGetConnection() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        //noinspection DuplicateStringLiteralInspection
        conf.set(DatabaseControl.PROP_LOCATION, location.toString());
        DatabaseControl control = new ControlDerby(conf);
        control.close();
        assertTrue("The location '" + location + "' should exist",
                   location.exists());
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

    public void testGetLastAccess() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetKey() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetRecord() throws Exception {
        //TODO: Test goes here...
    }

    public static Test suite() {
        return new TestSuite(ControlDerbyTest.class);
    }
}
