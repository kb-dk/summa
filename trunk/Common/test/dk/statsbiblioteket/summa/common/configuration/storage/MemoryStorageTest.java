/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.common.configuration.storage;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * MemoryStorage Tester.
 *
 * @author Toke <mailto:te@statsbblioteket.dk>
 * @since <pre>09/03/2008</pre>
 * @version 1.0
 */
public class MemoryStorageTest extends TestCase {
    /**
     * Constructor with name.
     * @param name The name.
     */
    public MemoryStorageTest(String name) {
        super(name);
    }

    @Override
    public final void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public final void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test get.
     */
    public void testGet() {
        //TODO Test goes here...
    }

    /**
     * Test get sub storages.
     */
    public void testGetSubStorages() {
        //TODO Test goes here...
    }

    /**
     * Test get sub storage.
     */
    public void testGetSubStorage() {
        final int seven = 7;
        final int eight = 8;
        try {
            MemoryStorage xs = new MemoryStorage();
            xs.put("seven", seven);
            xs.createSubStorage("submarine").put("eight", eight);
            assertEquals("The storage should contain the number 7",
                         seven, xs.get("seven"));
            ConfigurationStorage sub = xs.getSubStorage("submarine");
            assertNotNull("There should be a sub storage named submarine", sub);
            assertEquals("The sub storage should contain 8", eight,
                         sub.get("eight"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception thrown");
        }
    }

    /**
     * @return A test suite.
     */
    public static Test suite() {
        return new TestSuite(MemoryStorageTest.class);
    }

    /**
     * Test memory storage with configuration.
     */
    public void testMemoryStorageWithConf() {
        // TODO Recreate error described in
        // '#6 Importing Sub Configurations is Broken'
        try {
            XStorage xs = new XStorage(XStorageTest.SUBSTORAGELOCATION);
            Configuration c = new Configuration(xs);

            c.importConfiguration(new Configuration(xs));

            MemoryStorage ms = new MemoryStorage(c);
            ms.createSubStorage("storage");
            ConfigurationStorage cs = ms.getSubStorage("storage");
            assertNotNull(cs);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception thrown");
        }
    }
}
