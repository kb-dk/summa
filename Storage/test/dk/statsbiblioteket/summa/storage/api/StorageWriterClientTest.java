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
package dk.statsbiblioteket.summa.storage.api;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.rmi.RMIStorageProxy;
import dk.statsbiblioteket.summa.storage.rmi.RMIStorageProxyTest;
import dk.statsbiblioteket.util.Files;

import java.io.File;

/**
 * StorageWriterClient Tester.
 *
 * @author <Authors name>
 * @since <pre>08/13/2009</pre>
 * @version 1.0
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class StorageWriterClientTest extends TestCase {
    public StorageWriterClientTest(String name) {
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
        return new TestSuite(StorageWriterClientTest.class);
    }

    private File testRoot = new File(System.getProperty(
            "java.io.tmpdir", "storagetest"));
    private Storage getRMIStorage() throws Exception {
        Configuration conf = Configuration.newMemoryBased(
                DatabaseStorage.CONF_LOCATION,
                testRoot.toString());
        return new RMIStorageProxy(conf);
    }

    public void testLocalWrite() throws Exception {
        Storage storage = getRMIStorage();
        storage.flush(new Record("Dummy1", "SomeBase", new byte[0]));
        assertNotNull("The added record should exist",
                      storage.getRecord("Dummy1", null));
    }

    public void testRemoteWriteGetClear() throws Exception {
        Storage localStorage = getRMIStorage();
        Configuration conf = Configuration.newMemoryBased(
                ConnectionConsumer.CONF_RPC_TARGET,
                "//localhost:28000/summa-storage");
        StorageWriterClient remoteStorage = new StorageWriterClient(conf);
        StorageWriterClient remoteStorage2 = new StorageWriterClient(conf);

        remoteStorage.flush(new Record("Dummy1", "SomeBase", new byte[0]));
        remoteStorage2.flush(new Record("Dummy2", "SomeBase", new byte[0]));
        assertFalse("The added record should exist",
                    localStorage.getRecord("Dummy1", null).isDeleted());
        remoteStorage.clearBase("SomeBase");
        assertTrue("The added record should not exist anymore",
                   localStorage.getRecord("Dummy1", null).isDeleted());
        remoteStorage.close();
        remoteStorage2.close();
        localStorage.close();
    }

    public void testRemoteClear() throws Exception {
        Storage storage = getRMIStorage();
        RMIStorageProxyTest.testClearBase(storage);
    }
}

