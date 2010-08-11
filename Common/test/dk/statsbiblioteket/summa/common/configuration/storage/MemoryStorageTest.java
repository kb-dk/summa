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
    public MemoryStorageTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
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
                     7, xs.get("seven"));
        ConfigurationStorage sub = xs.getSubStorage("submarine");
        assertNotNull("There should be a sub storage named submarine", sub);
        assertEquals("The sub storage should contain 8", 8, sub.get("eight"));
    }

    public static Test suite() {
        return new TestSuite(MemoryStorageTest.class);
    }

    public void testMemoryStorageWithConf() throws Exception {
        // TODO Recreate error described in 
        // '#6 	Importing Sub Configurations is Broken'
        try {
            XStorage xs = new XStorage(XStorageTest.subLocation);
            Configuration c = new Configuration(xs);

            c.importConfiguration(new Configuration(xs));

            MemoryStorage ms = new MemoryStorage(c);
            ms.createSubStorage("storage");
            ConfigurationStorage cs = ms.getSubStorage("storage");
        } catch(Exception e) {
            e.printStackTrace();
            fail("Exception thrown");
        }
    }
}