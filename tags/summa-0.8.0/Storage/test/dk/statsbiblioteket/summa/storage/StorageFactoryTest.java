package dk.statsbiblioteket.summa.storage;

import java.io.File;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.derby.DerbyStorage;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
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
        conf.set(DatabaseStorage.PROP_LOCATION, location.toString());
        Storage storage = StorageFactory.createStorage(conf);
        assertNotNull("A controller should be created", storage);
        storage.close();
    }

    public void testCreateDerbyController() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(DatabaseStorage.PROP_LOCATION, location.toString());
        conf.set(StorageFactory.PROP_STORAGE, DerbyStorage.class.getName());
        Storage storage = StorageFactory.createStorage(conf);
        assertEquals("The controller should be a ControlDerby",
                     DerbyStorage.class, storage.getClass());
        storage.close();
    }
}
