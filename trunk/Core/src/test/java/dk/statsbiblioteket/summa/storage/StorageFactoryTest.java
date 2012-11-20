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
package dk.statsbiblioteket.summa.storage;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.util.Files;

import java.io.File;

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

    /**
     * Setup.
     * @throws Exception if error.
     */
    public void setUp() throws Exception {
        super.setUp();
        if (location.exists()) {
            Files.delete(location);
        }
    }

    /**
     * Tear down.
     * @throws Exception If error.
     */
    public void tearDown() throws Exception {
        super.tearDown();
        Files.delete(location);
    }

    public static Test suite() {
        return new TestSuite(StorageFactoryTest.class);
    }

    /** Location. */
    private static final File location =
                                     new File("target/test_result/", "kabloey");

    /**
     * Test create default controller.
     * @throws Exception If error.
     */
    public void testCreateDefaultController() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(DatabaseStorage.CONF_LOCATION, location.toString());
        Storage storage = StorageFactory.createStorage(conf);
        assertNotNull("A controller should be created", storage);
        storage.close();
    }


}
