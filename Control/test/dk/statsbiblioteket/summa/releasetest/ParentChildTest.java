/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.releasetest;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.filter.FilterControl;
import dk.statsbiblioteket.summa.control.service.StorageService;
import dk.statsbiblioteket.summa.control.service.SearchService;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.ingest.stream.FileReader;
import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.io.File;

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
        ReleaseTestCommon.tearDown();
    }

    public void testFull() throws Exception {
        Profiler storageProfiler = new Profiler();
        StorageService storage = OAITest.getStorageService();
        log.info("Finished starting Storage in "
                 + storageProfiler.getSpendTime());

        performIngest();
        testMultiExistence(storage);

        Profiler indexProfiler = new Profiler();
        performIndex();
        String indexTime = indexProfiler.getSpendTime();

        SearchService search = OAITest.getSearchService();
        testSearch();

        search.stop();
        storage.stop();

        log.info("Finished indexing in " + indexTime);
    }

    public void testParentExistence() throws Exception {
        StorageService storage = OAITest.getStorageService();
        performIngest();
        QueryOptions options = new QueryOptions(null, null, -1, 0);
        Record record =
                storage.getStorage().getRecord("horizon:parent1", options);
        assertNotNull("The record should exist", record);
        assertTrue("The record chould have child-IDs",
                   record.getChildIds().size() > 0);
        assertTrue("The record chould have children",
                   record.getChildren().size() > 0);
    }

    public void testDump() throws Exception {
        StorageService storage = OAITest.getStorageService();
        performIngest();
        dumpMultiRecord(storage, "horizon:parent1");

    }

/*    public void testDumpHorizon() throws Exception {
        StorageService storage = OAITest.getStorageService();
        performIngest();
        dumpMultiRecord(storage, "horizon_2114623");
    }*/

    private void dumpMultiRecord(StorageService storage, String recordID)
            throws IOException {
        QueryOptions options = new QueryOptions(null, null, -1, 0);
        Record record = storage.getStorage().getRecord(recordID, options);
        if (record == null) {
            System.out.println("Record with id '" + recordID + "' is null");
        }
        System.out.print("Start ");
        printRecord(record, 0);
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
    private void testMultiExistence(StorageService storageService) throws
                                                                   Exception {
        Storage storage = storageService.getStorage();
        StorageIterator iterator = new StorageIterator(
                storage, storage.getRecordsModifiedAfter(0, "horizon", null));
        int counter = 0;
        while (iterator.hasNext()) {
            counter++;
            iterator.next();
        }
        assertEquals("There should be the correct number of records in storage",
                     6, counter);
    }

    private void testSearch() throws IOException {
        log.debug("Testing searching");
        SearchClient searchClient =
                new SearchClient(Configuration.newMemoryBased(
                        ConnectionConsumer.CONF_RPC_TARGET,
                        "//localhost:28000/summa-searcher"));

        assertcount(searchClient, "basic test", "Kaoskyllingen", 2);

        assertcount(searchClient,
                    "child1 only parent", "Parental Mitzy Stardust", 1);
        assertcount(searchClient,
                    "child2 only parent", "Parental uundgåelige", 1);
        assertcount(searchClient,
                    "child2 all", "uundgåelige", 1);
        assertcount(searchClient,
                    "child4 only parent", "Parental reign", 1);
        assertcount(searchClient,
                    "child4 all", "reign", 1);
    }

    private void assertcount(SearchClient searchClient, String testCase,
                             String query, int expectedHits) throws IOException{
        String result = performSearch(searchClient, query);
        int count = MultipleSourcesTest.getSearchResultCount(result);
        assertEquals("The query '" + query + "' for the case '" + testCase
                     + "' should yield the expected number of hits",
                     expectedHits, count);
    }

    private String performSearch(SearchClient searchClient, String query)
                                                            throws IOException {
        Request request = new Request();
        request.put(DocumentSearcher.SEARCH_QUERY, query);
        String result = searchClient.search(request).toXML();
        log.trace(String.format(
                "Search result for query '%s' was:\n%s",
                query, result));
        return result;
    }

    private void performIngest()  throws IOException, InterruptedException {
        Configuration ingestConf =Configuration.load(
                "data/parent-child/horizon_ingest_configuration.xml");
        ingestConf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfiguration("Reader").
                set(FileReader.CONF_ROOT_FOLDER,
                    new File(ReleaseTestCommon.DATA_ROOT,
                             "parent-child/horizondump").getAbsolutePath());
        FilterService ingestService = new FilterService(ingestConf);
        try {
            ingestService.start();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Got exception while ingesting horizon dump", e);
        }
        while (ingestService.getStatus().getCode() == Status.CODE.running) {
            log.trace("Waiting for ingest of horizon ½ a second");
            Thread.sleep(500);
        }
        ingestService.stop();
        log.debug("Finished ingesting horizon");
    }

    private void performIndex() throws IOException, InterruptedException {
        log.info("Starting index");
        Configuration indexConf = getIndexConfiguration();

        FilterService index = new FilterService(indexConf);
        index.start();
        while (index.getStatus().getCode() == Status.CODE.running) {
            log.trace("Waiting for index ½ a second");
            Thread.sleep(500);
        }
        index.stop();
    }

    public Configuration getIndexConfiguration() throws IOException {
        Configuration indexConf = Configuration.load(Resolver.getURL(
                        "data/multiple/index_configuration.xml").
                getFile());
        indexConf.set(Service.CONF_SERVICE_ID, "IndexService");

        log.trace("Updating index conf with source horizon");

        String indexDescriptorLocation = new File(
                ReleaseTestCommon.DATA_ROOT,
                "parent-child/index_descriptor.xml").toString();
        log.debug("indexDescriptorLocation: " + indexDescriptorLocation);

        indexConf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfiguration("DocumentCreator").
                getSubConfiguration(LuceneIndexUtils.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    indexDescriptorLocation);

        indexConf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfiguration("IndexUpdate").
                getSubConfiguration("LuceneUpdater").
                getSubConfiguration(LuceneIndexUtils.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    indexDescriptorLocation);
        return indexConf;
    }
}
