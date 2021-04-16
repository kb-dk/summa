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

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.search.rmi.RMISearcherProxy;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageReaderClient;
import dk.statsbiblioteket.summa.support.summon.search.SummonSearchNode;
import dk.statsbiblioteket.summa.support.summon.search.SummonTestHelper;
import junit.framework.TestCase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class SearchStorageTest extends TestCase {
    private SummonSearchNode summonSearchNode;
    private Storage summonSearchStorage;

    private SummaSearcher plainSearcher;
    private SummaSearcher rmiSearcher;
    private Storage fakeStorage;
    private Storage searchStorage;

    @Override
    public void setUp() throws Exception {
        plainSearcher = new FakeSearcher();
        rmiSearcher = new RMISearcherProxy(Configuration.newMemoryBased(
                RMISearcherProxy.CONF_BACKEND, FakeSearcher.class.getCanonicalName()
        ));
        fakeStorage = new FakeStorage(Arrays.asList(
                new Record("someRecord", "fakeBase", "<myXML>content</myXML>".getBytes(StandardCharsets.UTF_8))));
        searchStorage = new SearchStorage(Configuration.newMemoryBased(
                SearchStorage.CONF_ID_FIELD, "recordID",
                ConnectionConsumer.CONF_RPC_TARGET,
                "//localhost:" + RMISearcherProxy.DEFAULT_REGISTRY_PORT + "/" + RMISearcherProxy.DEFAULT_SERVICE_NAME
        ));

        summonSearchNode = SummonTestHelper.createSummonSearchNode();
        summonSearchStorage = new SearchStorage(Configuration.newMemoryBased(), summonSearchNode);
    }

    @Override
    public void tearDown() throws Exception {
        rmiSearcher.close();
    }

    public void testSummonStorageSOAPProblem() throws IOException {
        Record record = summonSearchStorage.getRecord("summon_FETCH-ceeol_journals_4571843", null);
        assertNotNull("There should be a record", record);
        System.out.println(record.getContentAsUTF8());
    }

    public void testSearcherStorage() throws IOException {
        testSearcherStorage(searchStorage);
    }

    public void testFakeStorage() throws IOException {
        testSearcherStorage(fakeStorage);
    }

    public void disabledtestSBSpecificStorages() throws IOException {
        final String HUB = "//mars:47500/hub-storage";
        StorageReaderClient storage = new StorageReaderClient(Configuration.newMemoryBased(
                ConnectionConsumer.CONF_RPC_TARGET, HUB

        ));
        Record record = storage.getRecord("ebog_ssj0000325811", null);
        assertNotNull("There should be an ebog Record", record);
    }

    private void testSearcherStorage(Storage storage) throws IOException {
        Record record = storage.getRecord("someRecord", null);
        assertNotNull("There should be a record", record);
        System.out.println(record.getContentAsUTF8());
    }

    public void testFakeSearchPlain() throws IOException {
        testFakeSearch(plainSearcher);
    }

    public void testFakeSearchRMI() throws IOException {
        testFakeSearch(rmiSearcher);
    }

    public void testFakeSearch(SummaSearcher searcher) throws IOException {
        ResponseCollection responses = searcher.search(new Request(DocumentKeys.SEARCH_QUERY, "foo"));
        assertEquals("There should be a single response", 1, responses.size());
        DocumentResponse docs = (DocumentResponse)responses.iterator().next();
        assertEquals("The query should be delivered back", "foo", docs.getQuery());
        assertEquals("There should be a single document", 1, docs.size());
    }

}
