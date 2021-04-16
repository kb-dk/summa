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
package dk.statsbiblioteket.summa.releasetest;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.FilterControl;
import dk.statsbiblioteket.summa.common.filter.object.FilterSequence;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.control.service.SearchService;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.summa.storage.api.StorageReaderClient;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * The purpose of this test is to test handling of parent-child relations with
 * regard to indexing. The standard case is a parent (indexable) with some
 * children (not indexable) being indexed. The end-result should be a Record
 * representing the whole, with parts from both parent and children.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ParentChildTest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(ParentChildTest.class);

    @Override
    public void setUp () throws Exception {
        super.setUp();
        ReleaseTestCommon.setup();
        SearchTest.INDEX_ROOT.mkdirs();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        //ReleaseTestCommon.tearDown();
    }

    public void testFull() throws Exception {
        final String STORAGE = "pc_full_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);

        Profiler storageProfiler = new Profiler();
        log.info("Finished starting Storage in " + storageProfiler.getSpendTime());

        performIngest(STORAGE);
        testMultiExistence(STORAGE);

        Profiler indexProfiler = new Profiler();
        performIndex(STORAGE);
        String indexTime = indexProfiler.getSpendTime();

        SearchService search = OAITest.getSearchService();
        helpTestSearch();

        search.stop();
        storage.close();

        log.info("Finished indexing in " + indexTime);
    }

    public void testParentExistence() throws Exception {
        final String STORAGE = "pc_existence_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);

        performIngest(STORAGE);
        StorageReaderClient reader = ReleaseHelper.getReader(STORAGE);
        QueryOptions options = new QueryOptions(null, null, -1, 0);
        Record record = reader.getRecord("horizon:parent1", options);
        assertNotNull("The record should exist", record);
        assertTrue("The record chould have child-IDs", !record.getChildIds().isEmpty());
        assertTrue("The record chould have children", !record.getChildren().isEmpty());
        reader.releaseConnection();
        storage.close();
    }

    public void testDump() throws Exception {
        final String STORAGE = "pc_dump_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);
        performIngest(STORAGE);
        dumpMultiRecord(STORAGE, "horizon:parent1");
        storage.close();

    }

/*    public void testDumpHorizon() throws Exception {
        StorageService storage = OAITest.getStorageService();
        performIngest();
        dumpMultiRecord(storage, "horizon_2114623");
    }*/

    private void dumpMultiRecord(String storageID, String recordID)
            throws IOException {
        StorageReaderClient storage = ReleaseHelper.getReader(storageID);
        QueryOptions options = new QueryOptions(null, null, -1, 0);
        Record record = storage.getRecord(recordID, options);
        if (record == null) {
            System.out.println("Record with id '" + recordID + "' is null");
        }
        System.out.print("Start ");
        printRecord(record, 0);
        storage.releaseConnection();
    }

    private void printRecord(Record record, int level) {
        System.out.println("record at level " + level);
        System.out.println(record.getContentAsUTF8());

        if (record.getParents() != null) {
            for (Record parent: record.getParents()) {
                System.out.print("\nParent ");
                printRecord(parent, level+1);
            }
        }

        if (record.getChildren() != null) {
            for (Record child: record.getChildren()) {
                System.out.print("\nChild ");
                printRecord(child, level+1);
            }
        }
    }

    /*
     * Verifies that the expected records are created and marked properly in
     * Storage. See the readme.txt in data/parent-child/horizondump for details.
     */
    private void testMultiExistence(String storageID) throws Exception {
        StorageReaderClient storage =  ReleaseHelper.getReader(storageID);
        StorageIterator iterator = new StorageIterator(storage, storage.getRecordsModifiedAfter(0, "horizon", null));
        int counter = 0;
        while (iterator.hasNext()) {
            counter++;
            iterator.next();
        }
        assertEquals("There should be the correct number of records in storage",
                     6, counter);
        storage.releaseConnection();
    }

    private void helpTestSearch() throws IOException {
        log.debug("Testing searching");
        SearchClient searchClient =
                new SearchClient(Configuration.newMemoryBased(
                        ConnectionConsumer.CONF_RPC_TARGET, "//localhost:28000/summa-searcher"));

        assertcount(searchClient, "basic test", "Kaoskyllingen", 2);
        assertcount(searchClient, "child1 only parent", "Parental Mitzy Stardust", 1);
        assertcount(searchClient, "child2 only parent", "Parental uundgåelige", 1);
        assertcount(searchClient, "child2 all", "uundgåelige", 1);
        assertcount(searchClient, "child4 only parent", "Parental reign", 1);
        assertcount(searchClient, "child4 all", "reign", 1);
    }

    private void assertcount(SearchClient searchClient, String testCase,
                             String query, int expectedHits) throws IOException{
        String result = performSearch(searchClient, query);
        int count = MultipleSourcesTest.getSearchResultCount(result);
        assertEquals("The query '" + query + "' for the case '" + testCase
                     + "' should yield the expected number of hits. The result "
                     + "was " + result,
                     expectedHits, count);
    }

    private String performSearch(SearchClient searchClient, String query)
                                                            throws IOException {
        Request request = new Request();
        request.put(DocumentSearcher.SEARCH_QUERY, query);
        String result = searchClient.search(request).toXML();
        log.trace(String.format(Locale.ROOT, "Search result for query '%s' was:\n%s", query, result));
        return result;
    }

    private void performIngest(String storage)  throws Exception {
        final String HORIZON_DATA = new File(ReleaseTestCommon.DATA_ROOT, "parent-child/horizondump").getAbsolutePath();
        System.setProperty("data", HORIZON_DATA);
        Configuration ingestConf = ReleaseHelper.loadGeneralConfiguration(
            storage, "integration/parent-child/horizon_ingest_configuration.xml");
        FilterService ingestService = new FilterService(ingestConf);
        try {
            ingestService.start();
        } catch (Exception e) {
            throw new RuntimeException("Got exception while ingesting horizon dump", e);
        }
        while (ingestService.getStatus().getCode() == Status.CODE.running) {
            log.trace("Waiting for ingest of horizon ½ a second");
            Thread.sleep(100);
        }
        ingestService.stop();
        log.debug("Finished ingesting horizon");
    }

    public void testIngest() throws Exception {
        final String STORAGE = "pc_ingest_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);

        String[] IDS = new String[]{"horizon:child1", "horizon:child2",
                "horizon:child4", "horizon:3319632", "horizon:parent1",
                "horizon:subchild1"};

        performIngest(STORAGE);

        StorageReaderClient reader = ReleaseHelper.getReader(STORAGE);
        for (String id: IDS) {
            assertNotNull("A Record with the id " + id + " should exist",
                          reader.getRecord(id, null));
        }
        QueryOptions qo = new QueryOptions(null, null, -1, -1);
        Record parent1 = reader.getRecord("horizon:parent1", qo);
        assertNotNull("parent1 should exist", parent1);
        assertTrue(parent1 + " should have children", 
                   parent1.getChildren() != null && !parent1.getChildren().isEmpty());
        storage.close();
    }

    private void performIndex(String storageID) throws Exception {
        log.info("Starting index");
        Configuration indexConf = getIndexConfiguration(storageID);

        FilterService index = new FilterService(indexConf);
        index.start();
        while (index.getStatus().getCode() == Status.CODE.running) {
            log.trace("Waiting for index ½ a second");
            Thread.sleep(500);
        }
        index.stop();
    }

    public Configuration getIndexConfiguration(String storageID) throws Exception {

        Configuration indexConf = ReleaseHelper.loadGeneralConfiguration(
            storageID, "integration/parent-child/index_configuration.xml");

        indexConf.set(Service.CONF_SERVICE_ID, "IndexService");

        log.trace("Updating index conf with source horizon");

        String indexDescriptorLocation = new File(
            ReleaseTestCommon.DATA_ROOT, "parent-child/index_descriptor.xml").toString();
        log.debug("indexDescriptorLocation: " + indexDescriptorLocation);

        indexConf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(5).
//                getSubConfiguration("DocumentCreator").
                getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    indexDescriptorLocation);

        indexConf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(6).
//                getSubConfiguration("IndexUpdate").
                getSubConfigurations(IndexControllerImpl.CONF_MANIPULATORS).
                get(0).
//                getSubConfiguration("LuceneUpdater").
                getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    indexDescriptorLocation);
        return indexConf;
    }
}

