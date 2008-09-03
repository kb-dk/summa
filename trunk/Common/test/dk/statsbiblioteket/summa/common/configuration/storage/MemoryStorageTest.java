package dk.statsbiblioteket.summa.common.configuration.storage;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;

/**
 * MemoryStorage Tester.
 *
 * @author <Authors name>
 * @since <pre>09/03/2008</pre>
 * @version 1.0
 */
public class MemoryStorageTest extends TestCase {
    public MemoryStorageTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGet() throws Exception {
        //TODO: Test goes here...
    }


    public void testGetSubStorages() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetSubStorage() throws Exception {
        MemoryStorage xs = new MemoryStorage();
        xs.put("seven", 7);
        xs.createSubStorage("submarine").put("eight", 8);
        assertEquals("The storage should contain the number 7",
                     new Integer(7), (Integer)xs.get("seven"));
        ConfigurationStorage sub = xs.getSubStorage("submarine");
        assertNotNull("There should be a sub storage named submarine", sub);
        assertEquals("The sub storage should contain 8", new Integer(8), (Integer)sub.get("eight"));
    }

    public static Test suite() {
        return new TestSuite(MemoryStorageTest.class);
    }
}
