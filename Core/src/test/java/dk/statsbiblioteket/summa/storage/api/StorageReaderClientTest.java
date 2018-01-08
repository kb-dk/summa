package dk.statsbiblioteket.summa.storage.api;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.summa.storage.database.postgresql.PostGreSQLStorage;
import dk.statsbiblioteket.summa.storage.rmi.RMIStorageProxy;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

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
    private static final Log log = LogFactory.getLog(StorageReaderClientTest.class);

    private File testRoot = new File("target/test_result", "storagereaderclienttest");
    private DatabaseStorage dbStorage;
    private Storage rmiStorage;

    final String PVICA_PARENT = "pvica_tv_oai:du:1eadbaad-e294-4274-9d97-ac2861e87bc6";
    final String PVICA_CHILD = "pvica_tv_oai:man:2bdaa0c1-d24f-4136-bc80-5b7ba50f2bb1";
    final String PVICA_SECONDARY_CHILD = "pvica_tv_oai:man:80a7adaa-d58a-447b-b5b6-70290d4cab3b";

    // Mimick of problematic parent-child records
    @Test
    public void testProblematicChildParentLocal() throws IOException {
        setupLocal();
        try {
            testProblematicChildParent("local",
                    "//localhost:" + RMIStorageProxy.DEFAULT_REGISTRY_PORT + "/" + RMIStorageProxy.DEFAULT_SERVICE_NAME);
        } finally {
            tearDownLocal();
        }
    }

    @Test
    public void testCycleHandling() throws IOException {
        setupLocal();
        {
            Record r1 = new Record("r1", "dummy", new byte[0]);
            r1.setChildIds(Arrays.asList("r2", "r3"));
            dbStorage.flush(r1, null);

            Record r2 = new Record("r2", "dummy", new byte[0]);
            r2.setChildIds(Collections.singletonList("r1"));
            dbStorage.flush(r2, null);

            Record r3 = new Record("r3", "dummy", new byte[0]);
            dbStorage.flush(r3, null);
        }
        Record e1 = dbStorage.getRecord("r1", new QueryOptions(null, null, 3, 3));
        assertNotNull("Extracting a Record with child-cycle should work", e1);
        assertTrue("Root record should have children", e1.hasChildren());
        assertEquals("Root record should have the right number of children", 2, e1.getChildren().size());

        Record e2 = e1.getChildren().get(0);
        assertTrue("Sub-Record 1 should not have children", e2.hasChildren());
    }

    // Local PostgreSQL-proxy and remote PostgreSQL-server
    @Test
    public void testProblematicChildParentPostgreSQL() throws IOException {
        String PASS = "/home/te/tmp/mars_doms_pass";
        if (!new File(PASS).exists()) {
            log.debug("Skipping testProblematicChildParentPostgreSQL as there is no password file " + PASS);
            return;
        }
        final String password = Files.loadString(new File(PASS));
        rmiStorage = StorageFactory.createStorage(Configuration.newMemoryBased(
                Storage.CONF_CLASS, RMIStorageProxy.class,
                RMIStorageProxy.CONF_BACKEND, PostGreSQLStorage.class.getName(),
                PostGreSQLStorage.CONF_DRIVER_URL, "jdbc:postgresql://mars:5432/summa_doms_devel",
                DatabaseStorage.CONF_USERNAME, "summa",
                DatabaseStorage.CONF_PASSWORD, password,
                DatabaseStorage.CONF_EXPAND_RELATIVES_ID_LIST, true
        ));
        try {
            testProblematicChildParent("PostgreSQL",
                    "//localhost:" + RMIStorageProxy.DEFAULT_REGISTRY_PORT + "/" + RMIStorageProxy.DEFAULT_SERVICE_NAME);
        } finally {
            rmiStorage.close();
        }
    }

    // Debugging of problematic Record-relations on mars (SummaRise/doms devel machine). Enable for integration test
    public void testProblematicChildParentMars() throws IOException {
        testProblematicChildParent("mars", "//mars:57300/doms-storage");
    }

    public void testProblematicChildParent(String designation, String rmi) throws IOException {
        StorageReaderClient client = new StorageReaderClient(Configuration.newMemoryBased(
                ConnectionConsumer.CONF_RPC_TARGET, rmi
        ));

        { // height & depth 2
            final QueryOptions options = new QueryOptions();
            options.parentHeight = 2;
            options.childDepth = 2;
            Record parent = client.getRecord(PVICA_PARENT, options);
            Record child = client.getRecord(PVICA_CHILD, options);

            pvicaCheck(designation,"height & depth 2", PVICA_PARENT, PVICA_CHILD, parent, child);
        }

        { // Sanity check
            Record parent = client.getRecord(PVICA_PARENT, null);
            Record child = client.getRecord(PVICA_CHILD, null);

            pvicaCheck(designation,"none", PVICA_PARENT, PVICA_CHILD, parent, child);
        }

        /* The default has parentHeight==0 and childDepth==0
        { // default options
            final QueryOptions options = new QueryOptions();
            Record parent = client.getRecord(PVICA_PARENT, options);
            Record child = client.getRecord(PVICA_CHILD, options);

            pvicaCheck("default", PVICA_PARENT, PVICA_CHILD, parent, child);
        }
        */

        { // height & depth infinite
            final QueryOptions options = new QueryOptions();
            options.parentHeight = -1;
            options.childDepth = -1;
            Record parent = client.getRecord(PVICA_PARENT, options);
            Record child = client.getRecord(PVICA_CHILD, options);

            pvicaCheck(designation,"height & depth -1", PVICA_PARENT, PVICA_CHILD, parent, child);
        }

    }

    private void pvicaCheck(
            String designation, String options, String parentID, String childID, Record parent, Record child) {
        final String pre = designation + "(" + options + "): ";
        assertNotNull(pre + "There should be a parent record for ID " + parentID, parent);
        assertNotNull(pre + "The parent Record should have children", parent.getChildren());
        log.debug(pre + "Children IDs: [" + Strings.join(parent.getChildIds(), ", ") + "]");
        assertEquals(pre + "The parent Record should have the right number of children",
                     2, parent.getChildren().size());
        boolean foundChild = false;
        for (Record candidate: parent.getChildren()) {
            log.debug(pre + "Found child " + candidate.getId());
            if (childID.equals(candidate.getId())) {
                foundChild = true;
            }
        }
        assertTrue(pre + "One of the children to the parent should have ID " + childID, foundChild);

        assertNotNull(pre + "There should be a child record for ID " + childID, child);
        assertTrue(pre + "There should be a parent for the child record", child.hasParents());
        assertEquals(pre + "The parent for the child should be the right one",
                     parentID, child.getParents().get(0).getId());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void setupLocal() throws IOException {
        if (testRoot.exists()) {
            Files.delete(testRoot);
        }
        testRoot.mkdirs();

        dbStorage = (DatabaseStorage) StorageFactory.createStorage(Configuration.newMemoryBased(
                Storage.CONF_CLASS, H2Storage.class,
                DatabaseStorage.CONF_LOCATION, testRoot,
                H2Storage.CONF_H2_SERVER_PORT, 8088,
                DatabaseStorage.CONF_EXPAND_RELATIVES_ID_LIST, true
        ));
        addRecords(dbStorage);

        Configuration conf = Configuration.newMemoryBased(
                DatabaseStorage.CONF_LOCATION, testRoot.toString());
        rmiStorage = new RMIStorageProxy(conf, dbStorage);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void tearDownLocal() throws IOException {
        rmiStorage.close();
        dbStorage.close();
        testRoot.mkdirs();
        Files.delete(testRoot);
    }

    public void addRecords(Storage storage) throws IOException {
        Record parent = new Record(PVICA_PARENT, "doms", new byte[0]);
        parent.setChildren(Arrays.asList(
                new Record(PVICA_CHILD, "doms", new byte[0]),
                new Record(PVICA_SECONDARY_CHILD, "doms", new byte[0])
        ));
        storage.flush(parent);
    }

}