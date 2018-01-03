package dk.statsbiblioteket.summa.storage.api;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.summa.storage.rmi.RMIStorageProxy;
import dk.statsbiblioteket.util.Files;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

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
public class StorageReaderClientTest {
    private File testRoot = new File("target/test_result", "storagereaderclienttest");
    private DatabaseStorage dbStorage;
    private Storage rmiStorage;

    final String PVICA_PARENT = "pvica_tv_oai:du:1eadbaad-e294-4274-9d97-ac2861e87bc6";
    final String PVICA_CHILD = "pvica_tv_oai:man:2bdaa0c1-d24f-4136-bc80-5b7ba50f2bb1";

    @Before
    public void setup() throws IOException {
        if (testRoot.exists()) {
            Files.delete(testRoot);
        }
        testRoot.mkdirs();

        dbStorage = (DatabaseStorage) StorageFactory.createStorage(Configuration.newMemoryBased(
                Storage.CONF_CLASS, H2Storage.class,
                DatabaseStorage.CONF_LOCATION, testRoot,
                H2Storage.CONF_H2_SERVER_PORT, 8088
        ));
        addRecords(dbStorage);

        Configuration conf = Configuration.newMemoryBased(
                DatabaseStorage.CONF_LOCATION, testRoot.toString());
        rmiStorage = new RMIStorageProxy(conf, dbStorage);
    }

    @After
    public void tearDown() throws IOException {
        rmiStorage.close();
        dbStorage.close();
        testRoot.mkdirs();
        Files.delete(testRoot);
    }

    public void addRecords(Storage storage) throws IOException {
        Record parent = new Record(PVICA_PARENT, "doms", new byte[0]);
        parent.setChildren(Arrays.asList(
                new Record(PVICA_CHILD, "doms", new byte[0]),
                new Record("dummy2", "doms", new byte[0]),
                new Record("dummy3", "doms", new byte[0])
        ));
        storage.flush(parent);
    }

    // Mimick of problematic parent-child records
    @Test
    public void testProblematicChildParentLocal() throws IOException {
        testProblematicChildParent(
                "//localhost:" + RMIStorageProxy.DEFAULT_REGISTRY_PORT + "/" + RMIStorageProxy.DEFAULT_SERVICE_NAME);
    }

    // Debugging of problematic Record-relations on mars (SummaRise/doms devel machine). Enable for integration test
    public void testProblematicChildParentMars() throws IOException {
        testProblematicChildParent("//mars:57300/doms-storage");
    }

    public void testProblematicChildParent(String rmi) throws IOException {
        StorageReaderClient client = new StorageReaderClient(Configuration.newMemoryBased(
                ConnectionConsumer.CONF_RPC_TARGET, rmi
        ));

        { // Sanity check
            Record parent = client.getRecord(PVICA_PARENT, null);
            Record child = client.getRecord(PVICA_CHILD, null);

            pvicaCheck("none", PVICA_PARENT, PVICA_CHILD, parent, child);
        }

        { // default options
            final QueryOptions options = new QueryOptions();
            Record parent = client.getRecord(PVICA_PARENT, options);
            Record child = client.getRecord(PVICA_CHILD, options);

            pvicaCheck("default", PVICA_PARENT, PVICA_CHILD, parent, child);
        }

        { // height & depth options
            final QueryOptions options = new QueryOptions();
            options.parentHeight = 2;
            options.childDepth = 2;
            Record parent = client.getRecord(PVICA_PARENT, options);
            Record child = client.getRecord(PVICA_CHILD, options);

            pvicaCheck("height & depth", PVICA_PARENT, PVICA_CHILD, parent, child);
        }
    }

    private void pvicaCheck(String options, String parentID, String childID, Record parent, Record child) {
        final String pre = "options(" + options + "): ";
        assertNotNull(pre + "There should be a parent record for ID " + parentID, parent);
        assertNotNull(pre + "The parent Record should have children", parent.getChildren());
        assertEquals(pre + "The parent Record should have the right number of children",
                     3, parent.getChildren().size());
        boolean foundChild = false;
        for (Record candidate: parent.getChildren()) {
            if (childID.equals(candidate.getId())) {
                foundChild = true;
                break;
            }
        }
        assertTrue(pre + "One of the children to the parent should have ID " + childID, foundChild);

        assertNotNull(pre + "There should be a child record for ID " + childID, child);
        assertTrue(pre + "There should be a parent for the child record", child.hasParents());
        assertEquals(pre + "The parent for the child should be the right one",
                     parentID, child.getParents().get(0).getId());
    }
}