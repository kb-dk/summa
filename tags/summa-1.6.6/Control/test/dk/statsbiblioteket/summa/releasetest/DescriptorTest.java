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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.FilterControl;
import dk.statsbiblioteket.summa.common.filter.object.FilterSequence;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.index.XMLTransformer;
import dk.statsbiblioteket.summa.search.IndexWatcher;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Arrays;


/**
 * Tests a ingest => storage => index => search chain with facets.
 * </p><p>
 * IMPORTANT: Due to problems with releasing JDBC, the tests cannot be run
 * in succession, but must be started one at a time in their own JVM.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DescriptorTest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(DescriptorTest.class);

    @Override
    public void setUp () throws Exception {
        super.setUp();
        cleanup();
        SearchTest.INDEX_ROOT.mkdirs();
    }
                                            
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
//        cleanup();
    }

    private void cleanup() throws Exception {
        IngestTest.deleteOldStorages();
        if (SearchTest.INDEX_ROOT.exists()) {
            Files.delete(SearchTest.INDEX_ROOT);
        }
    }

    private Configuration getStaticSearcherConfiguration() throws Exception {
        URL descriptorLocation = Resolver.getURL(
                "data/descriptor/DescriptorTest_IndexDescriptor.xml");
        assertNotNull("The descriptor location should not be null",
                      descriptorLocation);

        Configuration searcherConf = Configuration.load(
                "data/descriptor/StaticDescriptor_SearchConfiguration.xml");
        assertNotNull("The Facet configuration should not be empty",
                      searcherConf);
        searcherConf.getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    descriptorLocation.getFile());
        searcherConf.set(IndexWatcher.CONF_INDEX_WATCHER_INDEX_ROOT,
                         SearchTest.INDEX_ROOT.toString());
        return searcherConf;
    }

    private Configuration getCopySearcherConfiguration() throws IOException {
        Configuration searcherConf = Configuration.load(
                "data/descriptor/CopyDescriptor_SearchConfiguration.xml");
        assertNotNull("The Facet configuration should not be empty",
                      searcherConf);
        searcherConf.set(IndexWatcher.CONF_INDEX_WATCHER_INDEX_ROOT,
                         SearchTest.INDEX_ROOT.toString());
        return searcherConf;
    }

    public void testCreateCopySearcher() throws Exception {
        SummaSearcherImpl searcher =
                new SummaSearcherImpl(getCopySearcherConfiguration());
        log.debug("Searcher created: " + searcher);
        searcher.close();
    }

    public void testCreateStaticSearcher() throws Exception {
        SummaSearcherImpl searcher =
                new SummaSearcherImpl(getStaticSearcherConfiguration());
        log.debug("Searcher created: " + searcher);
        searcher.close();
    }

    public void testUpdateBlankIndex() throws Exception {
        Storage storage = SearchTest.startStorage();
        log.debug("Storage started");
        updateIndex();
        storage.close();
    }

    public void testIngest() throws Exception {
        Storage storage = SearchTest.startStorage();
        log.debug("Storage started");
        SearchTest.ingest(new File(
                Resolver.getURL("data/search/input/part1").getFile()));
        assertTrue("Hans Jensen data should be ingested",
                   storage.getRecords(
                           Arrays.asList("fagref:hj@example.com"), null).size() == 1);
        storage.close();
    }

    public void testSimpleSearch() throws Exception {
        Storage storage = SearchTest.startStorage();
        SearchTest.ingest(new File(
                Resolver.getURL("data/search/input/part1").getFile()));
        updateIndex();
        log.debug("Index updated. Creating searcher");
        SummaSearcherImpl searcher =
                new SummaSearcherImpl(getCopySearcherConfiguration());
        log.debug("Searcher created. Verifying existence of Hans Jensen data");
        SearchTest.verifySearch(searcher, "Hans", 1);
        log.debug("Sample output from search: "
                  + searcher.search(SearchTest.simpleRequest("Hans")).toXML());
        SearchTest.verifySearch(searcher, "Hans", 1);
        log.debug("Second test-search performed with success");
        verifyFacetResult(searcher, "Hans");
        log.debug("All OK. Closing searcher, storage and returning");
        searcher.close();
        storage.close();
    }

    private void verifyFacetResult(SummaSearcher searcher,
                                   String query) throws IOException {
        verifyFacetResult(searcher, "author", query);
    }

    private void verifyFacetResult(SummaSearcher searcher,
                                   String facet, String query) throws IOException {
        String res = searcher.search(SearchTest.simpleRequest(query)).toXML();
        if (!res.contains("<facet name=\"" + facet + "\">")) {
            fail("Search for '" + query + "' with facet '" + facet
                 + "' did not produce any facets. Result was:\n" + res);
        }
        log.debug("Search for '" + query + "' gave:\n" + res);
    }

    public void testUpdateCopySearch() throws Exception {
        Configuration conf = Configuration.load(
                "data/descriptor/DescriptorTest_IndexUpdateConfiguration.xml");
        testFacetSearch(conf, getCopySearcherConfiguration(), "author");
    }

    public void testUpdateStaticSearch() throws Exception {
        Configuration conf = Configuration.load(
                "data/descriptor/DescriptorTest_IndexUpdateConfiguration.xml");
        testFacetSearch(conf, getStaticSearcherConfiguration(), "author");
    }

    public void testFullStaticSearch() throws Exception {
        Configuration conf = Configuration.load(
                "data/descriptor/DescriptorTest_IndexFullConfiguration.xml");
        testFacetSearch(conf, getCopySearcherConfiguration(), "author");
    }

    public void testFullCopySearch() throws Exception {
        Configuration conf = Configuration.load(
                "data/descriptor/DescriptorTest_IndexFullConfiguration.xml");
        testFacetSearch(conf, getStaticSearcherConfiguration(), "author");
    }

    public void testChangingFacets() throws Exception {

        Storage storage = SearchTest.startStorage();
        log.debug("Ingesting data");
        SearchTest.ingest(new File(
                Resolver.getURL("data/search/input/part1").getFile()));
        SearchTest.ingest(new File(
                Resolver.getURL("data/search/input/part2").getFile()));

        log.debug("Creating initial index");
        Configuration indexFullConf = Configuration.load(
                "data/descriptor/DescriptorTest_IndexFullConfiguration.xml");
        URL basicDescriptor = Resolver.getURL(
                "data/descriptor/DescriptorTest_IndexDescriptor.xml");
        index(indexFullConf, basicDescriptor);

        log.debug("Testing initial index, copy searcher");
        SummaSearcherImpl searcher =
                new SummaSearcherImpl(getCopySearcherConfiguration());
        searcher.checkIndex();
        verifyFacetResult(searcher, "author", "Hans");

        log.debug("Re-creating index to verify index change");
        String oldLocation = searcher.getIndexLocation();
        index(indexFullConf, basicDescriptor);
        searcher.checkIndex();
        assertFalse(String.format(
                "The old index location '%s' should differ from the new index " 
                + "location", oldLocation),
                oldLocation.equals(searcher.getIndexLocation()));
        verifyFacetResult(searcher, "author", "Hans");

        log.debug("Creating new index with new facets");
        indexFullConf = Configuration.load(
                "data/descriptor/DescriptorTest_IndexFullConfiguration.xml");
        oldLocation = searcher.getIndexLocation();
        URL extraDescriptor = Resolver.getURL(
                "data/descriptor/DescriptorTest_IndexDescriptorExtra.xml");
        index(indexFullConf, extraDescriptor);
        log.debug("Verifying that new index contains new facet");
        searcher.checkIndex();
        assertFalse(String.format(
                "The old index location from basic descriptor '%s' should "
                + "differ from the new index location", oldLocation),
                oldLocation.equals(searcher.getIndexLocation()));
        verifyFacetResult(searcher, "author2", "Hans");

        searcher.close();
        storage.close();
    }


    public void testFacetSearch(Configuration indexConf,
                                Configuration searchConf,
                                String checkFacet) throws Exception {
        log.debug("Creating Searcher");
        SummaSearcherImpl searcher = new SummaSearcherImpl(searchConf);
        log.debug("Searcher created");
        Storage storage = SearchTest.startStorage();
        log.debug("Storage started");
        index(indexConf);
        log.debug("Update 1 performed");
        searcher.checkIndex();
        log.debug("CheckIndex called");
        try {
            searcher.search(SearchTest.simpleRequest("dummy"));
            // TODO: Check if this is what we want
            fail("An timeout-Exceptions hould be thrown with empty index");
        } catch (RemoteException e) {
            // Expected
        }
        SearchTest.ingest(new File(
                Resolver.getURL("data/search/input/part1").getFile()));
        assertTrue("Hans Jensen data should be ingested",
                   storage.getRecords(Arrays.asList(
                           "fagref:hj@example.com"), null).size() == 1);
        log.debug("Ingest 1 performed");
        index(indexConf);
        log.debug("Update 2 performed");
        searcher.checkIndex();
        log.debug("Checkindex after Update 2 performed, verifying...");
        SearchTest.verifySearch(searcher, "Hans", 1);
        verifyFacetResult(searcher, checkFacet, "Hans");
        log.debug("Sample output after initial ingest: "
                  + searcher.search(SearchTest.simpleRequest("fagekspert")).
                toXML());
        log.debug("Adding new material");
        SearchTest.ingest(new File(
                Resolver.getURL("data/search/input/part2").getFile()));
        index(indexConf);
        log.debug("Waiting for the searcher to discover the new index");
        searcher.checkIndex(); // Make double sure
        log.debug("Verify final index");
        SearchTest.verifySearch(searcher, "Gurli", 1);
        SearchTest.verifySearch(searcher, "Gurli", 1);
        SearchTest.verifySearch(searcher, "Hans", 1);
        verifyFacetResult(searcher, checkFacet, "Gurli");
        verifyFacetResult(searcher, checkFacet, "Hans");
        log.debug("Sample output from large search: "
                  + searcher.search(SearchTest.simpleRequest("fagekspert")).
                toXML());

        searcher.close();
        storage.close();
    }

    public void testFacetBuild() throws Exception {
        Storage storage = SearchTest.startStorage();
        updateIndex();
        storage.close();
    }

    public void fullIndex() throws Exception {
        Configuration conf = Configuration.load(
                "data/descriptor/DescriptorTest_IndexFullConfiguration.xml");
        index(conf);
    }

    public void updateIndex() throws Exception {
        Configuration conf = Configuration.load(
                "data/descriptor/DescriptorTest_IndexUpdateConfiguration.xml");
        index(conf);
    }

    public void index(Configuration conf) throws Exception {
        URL descriptorLocation = Resolver.getURL(
                "data/descriptor/DescriptorTest_IndexDescriptor.xml");
        index(conf, descriptorLocation);
    }

    public void index(Configuration indexConf, URL descriptorLocation)
                                                              throws Exception {
        updateIndexConfiguration(indexConf, descriptorLocation);

        FilterService indexService = new FilterService(indexConf);
        indexService.start();
        IndexTest.waitForService(indexService, Integer.MAX_VALUE);
        indexService.stop();
    }

    private void updateIndexConfiguration(
            Configuration conf, URL descriptorLocation) throws Exception {
        URL xsltLocation = Resolver.getURL(
                "data/search/fagref_xslt/fagref_index.xsl");
        assertNotNull("The fagref xslt location should not be null",
                      xsltLocation);
        assertNotNull("The descriptor location should not be null",
                      descriptorLocation);

        Configuration chain = conf.getSubConfigurations(
                FilterControl.CONF_CHAINS).get(0);
        chain.getSubConfigurations(FilterSequence.CONF_FILTERS).get(1).
//        chain.getSubConfiguration("FagrefTransformer").
        set(XMLTransformer.CONF_XSLT, xsltLocation.getFile());

        // Document Creator
        chain.getSubConfigurations(FilterSequence.CONF_FILTERS).get(3).
                getSubConfiguration(
                        IndexDescriptor.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    descriptorLocation.getFile());

        // Index Update
        chain.getSubConfigurations(FilterSequence.CONF_FILTERS).get(4).
        set(IndexControllerImpl.CONF_INDEX_ROOT_LOCATION,
            SearchTest.INDEX_ROOT.toString());
        chain.getSubConfigurations(FilterSequence.CONF_FILTERS).get(4).
        getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    descriptorLocation.getFile());
    }
}
