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

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.WritableStorage;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.Files;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class RMIStorageProxyTest extends TestCase {
    public RMIStorageProxyTest(String name) {
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

    public static Test suite() {
        return new TestSuite(RMIStorageProxyTest.class);
    }

    private File testRoot = new File(System.getProperty(
            "java.io.tmpdir", "storagetest"));
    
    private Storage getRMIStorage() throws Exception {
        Configuration conf = Configuration.newMemoryBased(
                DatabaseStorage.CONF_LOCATION, testRoot.toString());
        return new RMIStorageProxy(conf);
    }

    public void testGetRecord() throws Exception {
        Storage storage = getRMIStorage();
        storage.flush(new Record("Dummy1", "SomeBase", new byte[0]));
        assertFalse("The added record should exist",
                    storage.getRecord("Dummy1", null).isDeleted());
    }

    public void testDirectClear() throws Exception {
        Configuration conf = Configuration.newMemoryBased(
                DatabaseStorage.CONF_LOCATION,
                testRoot.toString());
        Storage storage = StorageFactory.createStorage(conf);
        testClearBase(storage);
    }

    public void testClearBase() throws Exception {
        Storage storage = getRMIStorage();
        testClearBase(storage);
    }

    public static void testClearBase(Storage storage) throws Exception {
        storage.flush(new Record("Dummy1", "SomeBase", new byte[0]));
        assertFalse("The added record should exist",
                   storage.getRecord("Dummy1", null).isDeleted());
        storage.clearBase("SomeBase");
        assertTrue("The added record should not exist anymore",
                   storage.getRecord("Dummy1", null).isDeleted());
    }
}

