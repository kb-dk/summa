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
package dk.statsbiblioteket.summa.storage.api.filter;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.summa.storage.rmi.RMIStorageProxy;
import dk.statsbiblioteket.util.Files;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

public class StorageValidationFilterTest extends TestCase {
    /** Local storage instance. */
    private Storage storage = null;
    /** Storage location. */
    private File dbRoot = new File("target/test_result/storagevalidation/");
    /** The used storage configuration. */
    private Configuration conf;
    /** storage counter. */
    private int storageCounter = 0;

    @Override
    public void tearDown() throws Exception {
        if (storage != null && storage instanceof DatabaseStorage) {
            ((DatabaseStorage) storage).destroyDatabase();
        }
        Files.delete(dbRoot);
    }
    
    @Override
    public void setUp() throws Exception {
        if (dbRoot.exists()) {
            Files.delete(dbRoot);
        }
        String lastStorageLocation = dbRoot + File.separator
                                   + "storage_validataion" + (storageCounter++);
        conf = Configuration.newMemoryBased(
                Storage.CONF_CLASS, H2Storage.class,
                DatabaseStorage.CONF_LOCATION, lastStorageLocation,
                "summa.rpc.connections.retries", 1,
                "summa.rpc.connections.gracetime", 2
        );
    }
    
    /**
     * Test construction of ingest chain with and without storage.
     */
    public void testConstrution() {
        try {
            // No storage running should result in an exception
            new StorageValidationFilter(conf);
            fail("Exception is expected");
        } catch (IOException e) {
            // expected
        } catch (Exception e) {
            e.printStackTrace();
            fail("Not expected to throw exception when creating configuration");
        }
        try {
            storage = new RMIStorageProxy(conf);
            assertNotNull(storage);
            StorageValidationFilter storageValidationFilter = 
                                              new StorageValidationFilter(conf);
            // Expected
            assertNotNull(storageValidationFilter);
        } catch (IOException e) {
            e.printStackTrace();
            fail("No exception is expected here");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Not expected to fail when creating configuratino");
        }
    }
}
