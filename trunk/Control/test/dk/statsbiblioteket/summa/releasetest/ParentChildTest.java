/* $Id:$
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
import dk.statsbiblioteket.summa.control.service.StorageService;
import dk.statsbiblioteket.summa.control.service.SearchService;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.ingest.stream.FileReader;
import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.List;
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

    public void setUp () throws Exception {
        super.setUp();
        ReleaseTestCommon.setup();
        SearchTest.INDEX_ROOT.mkdirs();
    }

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

        Profiler indexProfiler = new Profiler();
        performIndex();
        String indexTime = indexProfiler.getSpendTime();

        SearchService search = OAITest.getSearchService();
        testSearch();

        search.stop();
        storage.stop();

        log.info("Finished indexing in " + indexTime);
    }

    private void testSearch() throws IOException {
        log.debug("Testing searching");
        SearchClient searchClient =
                new SearchClient(Configuration.newMemoryBased(
                        ConnectionConsumer.CONF_RPC_TARGET,
                        "//localhost:28000/summa-searcher"));
        String query = "Kaoskyllingen";
        Request request = new Request();
        request.put(DocumentSearcher.SEARCH_QUERY, query);
        String result = searchClient.search(request).toXML();
        log.trace(String.format(
                "Search result for query '%s' was:\n%s",
                query, result));
        int count = MultipleSourcesTest.getSearchResultCount(result);
        assertEquals("The query 'Kaoskyllingen' should yield a single hit",
                     1, count);
    }

    private void performIngest()  throws IOException, InterruptedException {
        Configuration ingestConf =Configuration.load(
                "data/parent-child/horizon_ingest_configuration.xml");
        ingestConf.getSubConfiguration("SingleChain").
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

        indexConf.getSubConfiguration("SingleChain").
                getSubConfiguration("DocumentCreator").
                getSubConfiguration(LuceneIndexUtils.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    indexDescriptorLocation);

        indexConf.getSubConfiguration("SingleChain").
                getSubConfiguration("IndexUpdate").
                getSubConfiguration("LuceneUpdater").
                getSubConfiguration(LuceneIndexUtils.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    indexDescriptorLocation);
        return indexConf;
    }
}
