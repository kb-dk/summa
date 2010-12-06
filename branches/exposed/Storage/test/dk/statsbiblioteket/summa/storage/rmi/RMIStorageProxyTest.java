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
package dk.statsbiblioteket.summa.storage.rmi;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.util.Files;

import java.io.File;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests {@link RMIStorageProxy}.
 */
@SuppressWarnings("duplicatestringliteralinspection")
public class RMIStorageProxyTest extends TestCase {
    /** Test root. */
    private File testRoot = new File("target/test_result", "storagetest");

    /**
     * Constructs a RMI Storage proxy.
     * @param name The name.
     */
    public RMIStorageProxyTest(String name) {
        super(name);
    }

    @Override
    public final void setUp() throws Exception {
        super.setUp();
        if (testRoot.exists()) {
            Files.delete(testRoot);
        }
        testRoot.mkdirs();        
    }

    @Override
    public final void tearDown() throws Exception {
        super.tearDown();
        testRoot.mkdirs();
        Files.delete(testRoot);
    }

    /**
     * @return A test suite.
     */
    public static Test suite() {
        return new TestSuite(RMIStorageProxyTest.class);
    }

    /**
     * Returns a RMI storage.
     * @return A RMI Storage.
     * @throws Exception If error.
     */
    private Storage getRMIStorage() throws Exception {
        Configuration conf = Configuration.newMemoryBased(
                DatabaseStorage.CONF_LOCATION, testRoot.toString());
        return new RMIStorageProxy(conf);
    }

    /**
     * Test get record.
     */
    public final void testGetRecord() {
        try {
            Storage storage = getRMIStorage();
            storage.flush(new Record("Dummy1", "SomeBase", new byte[0]));
            assertFalse("The added record should exist",
                        storage.getRecord("Dummy1", null).isDeleted());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Not expected here");
        }
    }

    /**
     * Test direct clear.
     */
    public final void testDirectClear() {
        try {
            Configuration conf = Configuration.newMemoryBased(
                    DatabaseStorage.CONF_LOCATION,
                    testRoot.toString());
            Storage storage = StorageFactory.createStorage(conf);
            testClearBase(storage);
        } catch (IOException e) {
            fail("Not expected here");
        }
    }

    /**
     * Test clear base.
     */
    public final void testClearBase() {
        try {
            Storage storage = getRMIStorage();
            testClearBase(storage);
        } catch (Exception e) {
            fail("Not expected here");
        }
    }

    /**
     * Test clear base.
     * @param storage The storage.
     * @throws Exception
     */
    public static void testClearBase(Storage storage) {
        try {
            storage.flush(new Record("Dummy1", "SomeBase", new byte[0]));
            assertFalse("The added record should exist",
                       storage.getRecord("Dummy1", null).isDeleted());
            storage.clearBase("SomeBase");
            assertTrue("The added record should not exist anymore",
                       storage.getRecord("Dummy1", null).isDeleted());
        } catch (IOException e) {
            fail("Not expected here");
        }
    }
}
