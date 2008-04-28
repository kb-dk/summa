package dk.statsbiblioteket.summa.storage;

import java.io.File;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.database.DatabaseControl;
import dk.statsbiblioteket.summa.storage.database.derby.ControlDerby;
import dk.statsbiblioteket.summa.storage.io.Control;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * StorageFactory Tester.
 *
 * @author <Authors name>
 * @since <pre>02/13/2008</pre>
 * @version 1.0
 */
public class StorageFactoryTest extends TestCase {
    public StorageFactoryTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(StorageFactoryTest.class);
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private static final File location =
            new File(System.getProperty("java.io.tmpdir"), "kabloey");

    public void testCreateDefaultController() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(DatabaseControl.PROP_LOCATION, location.toString());
        Control control = StorageFactory.createController(conf);
        assertNotNull("A controller should be created", control);
        control.close();
    }

    public void testCreateDerbyController() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(DatabaseControl.PROP_LOCATION, location.toString());
        conf.set(StorageFactory.PROP_CONTROLLER, ControlDerby.class.getName());
        Control control = StorageFactory.createController(conf);
        assertEquals("The controller should be a ControlDerby",
                     ControlDerby.class, control.getClass());
        control.close();
    }
}
