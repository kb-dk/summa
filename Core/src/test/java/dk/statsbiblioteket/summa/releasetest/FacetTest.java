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
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.unittest.ExtraAsserts;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.facetbrowser.api.IndexKeys;
import dk.statsbiblioteket.summa.search.IndexWatcher;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.exposed.ExposedSettings;
import org.apache.lucene.search.exposed.facet.CollectorPoolFactory;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tests a ingest => storage => index => search chain with facets.
 * </p><p>
 * IMPORTANT: Due to problems with releasing JDBC, the tests cannot be run
 * in succession, but must be started one at a time in their own JVM.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection", "ObjectToString"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FacetTest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(FacetTest.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        cleanup();
        assertTrue("The " + SearchTest.INDEX_ROOT + " should be created", SearchTest.INDEX_ROOT.mkdirs());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        cleanup();
        ReleaseHelper.cleanup();
    }

    private void cleanup() throws Exception {
        ReleaseHelper.cleanup();
        if (SearchTest.INDEX_ROOT == null) {
            throw new RuntimeException("SearchTest.INDEX_ROOT is null, which should not be possible as"
                                       + " the property 'java.io.tmpdir' should always exist");
        }
        if (SearchTest.INDEX_ROOT.exists()) {
            log.debug("Deleting " + SearchTest.INDEX_ROOT);
            Files.delete(SearchTest.INDEX_ROOT);
        }
        if (SearchTest.INDEX_ROOT.exists()) {
            throw new IllegalStateException(
                    "The folder " + SearchTest.INDEX_ROOT + " still exists. Cleanup incomplete");
        }
    }

    public static Configuration getSearcherConfiguration() throws Exception {
        URL descriptorLocation = Resolver.getURL("integration/search/SearchTest_IndexDescriptor.xml");
        assertNotNull("The descriptor location should not be null", descriptorLocation);

        Configuration searcherConf = IndexTest.loadFagrefProperties("no_storage",
                                                                    "integration/search/FacetTest_SearchConfiguration"
                                                                    + ".xml");
        assertNotNull("The Facet configuration should not be empty", searcherConf);
        searcherConf.getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION, descriptorLocation.getFile());
                /*
        List<Configuration> subSearcherConfs =
                searcherConf.getSubConfigurations(SearchNodeFactory.CONF_NODES);
        for (Configuration subSearcherConf: subSearcherConfs) {
            if (subSearcherConf.getString(
                    SearchNodeFactory.CONF_NODE_CLASS).equals(
                    LuceneSearchNode.class.getName())) {
                Configuration descConf = subSearcherConf.getSubConfiguration(
                        IndexDescriptor.CONF_DESCRIPTOR);
                log.debug("Updating the location of the IndexDescriptor to "
                          + descriptorLocation.getFile());
                assertTrue("The descriptorlocation should be present",
                           descConf.valueExists(
                                   IndexDescriptor.CONF_ABSOLUTE_LOCATION));
                descConf.set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                             descriptorLocation.getFile());
            }
        }         */
        searcherConf.set(IndexWatcher.CONF_INDEX_WATCHER_INDEX_ROOT, SearchTest.INDEX_ROOT.toString());
        return searcherConf;
    }

/* Does not work as the updateIndex-method for SerachTest does not handle facets
   public void testPlainSearch() throws Exception {
        SummaSearcher searcher =
                new SummaSearcherImpl(getSearcherConfiguration());
        SearchTest.testFullSearcher(searcher);
    }*/

    public void testCreateSearcher() throws Exception {
        SummaSearcherImpl searcher = new SummaSearcherImpl(getSearcherConfiguration());
        log.debug("Searcher created: " + searcher);
        searcher.close();
    }

    public void testUpdateBlankIndex() throws Exception {
        final String STORAGE = "blank_update_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);
        try {
            log.debug("Storage started");
            updateIndex(STORAGE);
        } finally {
            storage.close();
        }
    }

    public void testIngest() throws Exception {
        final String STORAGE = "test_ingest_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);
        try {
            log.debug("Storage started");
            SearchTest.ingestFagref(STORAGE, SearchTest.fagref_hj);
            assertEquals("Hans Jensen data should be ingested", 1, storage.getRecords(Arrays.asList
                    ("fagref:hj@example.com"), null).size());
        } finally {
            storage.close();
        }
    }

    public void testTransliteration() throws Exception {
        final String STORAGE = "transliteraye_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);
        log.debug("Storage started");
        SearchTest.ingestFagref(STORAGE, Resolver.getURL("integration/transliteration/transliterate.xml").getFile());
        assertEquals("Háns Jensén data should be ingested", 1, storage.getRecords(Arrays.asList("fagref:haje@example"
                                                                                                + ".com"),
                                                                                  null).size());
        updateIndex(STORAGE);
        SummaSearcherImpl searcher = new SummaSearcherImpl(getSearcherConfiguration());
        log.debug("Searcher created. Verifying searches");
        SearchTest.verifySearch(searcher, "*", 1);
        SearchTest.verifySearch(searcher, "Fagekspert", 1);
        SearchTest.verifySearch(searcher, "Indenfor", 1);
        SearchTest.verifySearch(searcher, "Häns Erik Jensén", 1);
        SearchTest.verifySearch(searcher, "Erik", 1);
        SearchTest.verifySearch(searcher, "kapacitet", 1);
        SearchTest.verifySearch(searcher, "Häns", 1); // Häns
        SearchTest.verifySearch(searcher, "Hæns", 1); // From freetext
        SearchTest.verifySearch(searcher, "areni", 1); // main_titel is text
        SearchTest.verifySearch(searcher, "árënì", 1); // Direct
        searcher.close();
        storage.close();
    }

    public void testSimpleSearch() throws Exception {
        final String STORAGE = "simple_search_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);

        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_hj);
        Record hansRecord = storage.getRecord("fagref:hj@example.com", null);
        assertNotNull("The fagref Hans should exist in storage", hansRecord);
        assertEquals("The Records-count should be correct after first ingest", 1, countRecords(storage, "fagref"));

        updateIndex(STORAGE);
        log.debug("Index updated. Creating searcher");
        SummaSearcherImpl searcher = new SummaSearcherImpl(getSearcherConfiguration());
        log.debug("Searcher created. Verifying existence of Hans Jensen data");
        SearchTest.verifySearch(searcher, "Hans", 1);
        log.debug("Sample output from search: " + searcher.search(SearchTest.simpleRequest("Hans")).toXML());
        SearchTest.verifySearch(searcher, "Hans", 1);
        log.debug("Second test-search performed with success");
        verifyFacetResult(searcher, "Hans");
        log.debug("All OK. Closing searcher, storage and returning");
        searcher.close();
        storage.close();
    }

    public void testSortValue() throws Exception {
        final String STORAGE = "sort_value_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);
        try {
            SearchTest.ingestFagref(STORAGE, SearchTest.fagref_hj);
            Record hansRecord = storage.getRecord("fagref:hj@example.com", null);
            assertNotNull("The fagref Hans should exist in storage", hansRecord);
            assertEquals("The Records-count should be correct after first ingest", 1, countRecords(storage, "fagref"));

            updateIndex(STORAGE);
            log.debug("Index updated. Creating searcher");
            SummaSearcherImpl searcher = new SummaSearcherImpl(getSearcherConfiguration());
            log.debug("Searcher created. Verifying existence of Hans Jensen " + "data");
            SearchTest.verifySearch(searcher, "Hans", 1);
            Request request = new Request();
            request.put(DocumentKeys.SEARCH_QUERY, "hans");
            request.put(DocumentKeys.SEARCH_SORTKEY, "author_person");

            String xml = searcher.search(request).toXML();
            Pattern sortValuePattern = Pattern.compile(".*sortValue=\"(.+?)\".*", Pattern.DOTALL);
            Matcher matcher = sortValuePattern.matcher(xml);
            assertTrue("There should be a sortValue in the result\n" + xml, matcher.matches());
            assertEquals("The sortValue should be as expected in\n" + xml, "Hans Jensen", matcher.group(1));
            searcher.close();
//            System.out.println(xml);
        } finally {
            storage.close();
        }
    }

    public void testScoreExtraction() throws Exception {
        final String STORAGE = "score_test_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);
        try {
            SearchTest.ingestFagref(STORAGE, SearchTest.fagref_hj);
            SearchTest.ingestFagref(STORAGE, SearchTest.fagref_jh_gm);
            Record hansRecord = storage.getRecord("fagref:hj@example.com", null);
            assertNotNull("The fagref Hans should exist in storage", hansRecord);
            assertEquals("The Records-count should be correct after first ingest", 3, countRecords(storage, "fagref"));

            updateIndex(STORAGE);
            log.debug("Index updated. Creating searcher");
            SummaSearcherImpl searcher = new SummaSearcherImpl(getSearcherConfiguration());
            log.debug("Searcher created. Verifying existence of Hans Jensen data");
            SearchTest.verifySearch(searcher, "Hans", 1);
            Request request = new Request();
            request.put(DocumentKeys.SEARCH_QUERY, "hans");
            request.put(DocumentKeys.SEARCH_SORTKEY, "author_person");

            String xml = searcher.search(request).toXML();
            final String SNIPPET = "<record score=\"0.0067765927\"";
            assertTrue("The result should contain the string '" + SNIPPET + "'\n" + xml, xml.contains(SNIPPET));
            searcher.close();
        } finally {
            storage.close();
        }
    }

    public void testSortValues() throws Exception {
        final String STORAGE = "sortvalues_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);

        try {
            SearchTest.ingestFagref(STORAGE, SearchTest.fagref_hj);
            SearchTest.ingestFagref(STORAGE, SearchTest.fagref_jh_gm);

            assertNotNull("The fagref Hans should exist in storage", storage.getRecord("fagref:hj@example.com", null));
            assertNotNull("The fagref Gurli should exist in storage", storage.getRecord("fagref:gm@example.com", null));

            updateIndex(STORAGE);
            log.debug("Index updated. Creating searcher");
            SummaSearcherImpl searcher = new SummaSearcherImpl(getSearcherConfiguration());
            log.debug("Searcher created. Verifying existence of data");
            SearchTest.verifySearch(searcher, "*", 3);
            Request request = new Request();
            request.put(DocumentKeys.SEARCH_QUERY, "*");
            request.put(DocumentKeys.SEARCH_SORTKEY, "author_person");

            List<String> sortValues = extractSortValues(searcher, request);
            final List<String> EXPECTED = Arrays.asList("Gurli Margrethe", "Hans Jensen", "Jens Hansen");
            ExtraAsserts.assertEquals("The returned sort values should be as expected", EXPECTED, sortValues);
            searcher.close();
        } finally {
            storage.close();
        }
    }

    public void testNegativeFaceting() throws Exception {
        final String STORAGE = "negative_faceting_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);

        try {
            SearchTest.ingestFagref(STORAGE, SearchTest.fagref_hj);
            SearchTest.ingestFagref(STORAGE, SearchTest.fagref_jh_gm);
            updateIndex(STORAGE);
            log.debug("Index updated. Creating searcher");
            SummaSearcherImpl searcher = new SummaSearcherImpl(getSearcherConfiguration());
            log.debug("Searcher created. Verifying existence of Hans Jensen " + "data");
            SearchTest.verifySearch(searcher, "Hans", 1);

            {
                Request request = new Request(DocumentKeys.SEARCH_QUERY, "*", DocumentKeys.SEARCH_FILTER, "hans");
                assertEquals("Regular query + filter",
                             1, ((DocumentResponse) searcher.search(request).iterator().next()).getHitCount());
            }

            {
                Request request = new Request(DocumentKeys.SEARCH_QUERY, "*", DocumentKeys.SEARCH_FILTER, "NOT hans");
                assertEquals("Regular query + negative filter without flag",
                             0, ((DocumentResponse) searcher.search(request).iterator().next()).getHitCount());
            }

            {
                Request request = new Request(DocumentKeys.SEARCH_QUERY, "*",
                                              DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE, "true",
                                              DocumentKeys.SEARCH_FILTER, "NOT hans");
                assertEquals("Regular query + negative filter with flag",
                             2, ((DocumentResponse) searcher.search(request).iterator().next()).getHitCount());
            }

            searcher.close();
        } finally {
            storage.close();
        }
    }

    private List<String> extractSortValues(SummaSearcherImpl searcher, Request request) throws RemoteException {
        final Pattern SORT_VALUE = Pattern.compile("sortValue=\"(.+?)\"", Pattern.DOTALL);
        final List<String> result = new ArrayList<String>();
        String xml = searcher.search(request).toXML();
        Matcher matcher = SORT_VALUE.matcher(xml);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }


    public void testIndexLookup() throws Exception {
        final String STORAGE = "index_lookup_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);

        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_hj);
        Record hansRecord = storage.getRecord("fagref:hj@example.com", null);
        assertNotNull("The fagref Hans should exist in storage", hansRecord);
        assertEquals("The Records-count should be correct after first ingest", 1, countRecords(storage, "fagref"));
        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_jh_gm);

        updateIndex(STORAGE);
        log.debug("Index updated. Creating searcher");
        SummaSearcherImpl searcher = new SummaSearcherImpl(getSearcherConfiguration());

        { // Query, minCount 1
            Request request = new Request();
            request.put(IndexKeys.SEARCH_INDEX_QUERY, "jensen");
            request.put(IndexKeys.SEARCH_INDEX_FIELD, "author_person");
            request.put(IndexKeys.SEARCH_INDEX_TERM, "H");
            request.put(IndexKeys.SEARCH_INDEX_DELTA, 0);
            request.put(IndexKeys.SEARCH_INDEX_MINCOUNT, 1);
            ResponseCollection responses = searcher.search(request);
//            System.out.println(responses.toXML());
            assertFalse("The index lookup should have no Jens Hansen entry but" + " had\n"
                        + responses.toXML(), responses.toXML().contains("<term count=\"0\">Jens Hansen</term>"));
            assertTrue("The index lookup should return 1 for Hans Jensen but " + "got\n"
                       + responses.toXML(), responses.toXML().contains("<term count=\"1\">Hans Jensen</term>"));
        }

        { // Query, minCount 0
            Request request = new Request();
            request.put(IndexKeys.SEARCH_INDEX_QUERY, "jensen");
            request.put(IndexKeys.SEARCH_INDEX_FIELD, "author_person");
            request.put(IndexKeys.SEARCH_INDEX_TERM, "J");
            request.put(IndexKeys.SEARCH_INDEX_MINCOUNT, 0);
            ResponseCollection responses = searcher.search(request);
            assertTrue("The index lookup should return 0 for Jens Hansen but " + "got\n"
                       + responses.toXML(), responses.toXML().contains("<term count=\"0\">Jens Hansen</term>"));
            //          System.out.println(responses.toXML());
        }

        { // No query, hit all
            Request request = new Request();
//        request.put(DocumentKeys.SEARCH_QUERY, "*");
            //    request.put(IndexKeys.SEARCH_INDEX_QUERY, "");
            request.put(IndexKeys.SEARCH_INDEX_FIELD, "author_person");
            request.put(IndexKeys.SEARCH_INDEX_TERM, "J");
            ResponseCollection responses = searcher.search(request);
            assertTrue("The index lookup should return at least one term",
                       responses.toXML().contains("<term count=\"1\">Hans Jensen</term>"));
            //    System.out.println(responses.toXML());
        }

        // TODO: IndexDescriptor-load?
        searcher.close();
        storage.close();
    }

    public void testIndexLookupNonFacetField() throws Exception {
        final String STORAGE = "index_lookup_storage2";
        Storage storage = ReleaseHelper.startStorage(STORAGE);

        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_hj);
        Record hansRecord = storage.getRecord("fagref:hj@example.com", null);
        assertNotNull("The fagref Hans should exist in storage", hansRecord);
        assertEquals("The Records-count should be correct after first ingest", 1, countRecords(storage, "fagref"));
        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_jh_gm);

        updateIndex(STORAGE);
        log.debug("Index updated. Creating searcher");
        SummaSearcherImpl searcher = new SummaSearcherImpl(getSearcherConfiguration());

        { // Query, minCount 1
            Request request = new Request();
            request.put(IndexKeys.SEARCH_INDEX_QUERY, "jensen");
            request.put(IndexKeys.SEARCH_INDEX_FIELD, "stilling");
            request.put(IndexKeys.SEARCH_INDEX_SORT, IndexKeys.INDEX_SORTBYLOCALE);
            request.put(IndexKeys.SEARCH_INDEX_LOCALE, "da");
/*            request.put(IndexKeys.SEARCH_INDEX_TERM,     "H");
            request.put(IndexKeys.SEARCH_INDEX_DELTA,    0);
            request.put(IndexKeys.SEARCH_INDEX_MINCOUNT, 1);
            */
            ResponseCollection responses = searcher.search(request);
            System.out.println(responses.toXML());
            assertFalse("The index lookup should have no Jens Hansen entry but" + " had\n"
                        + responses.toXML(), responses.toXML().contains("<term count=\"0\">Jens Hansen</term>"));
            assertTrue("The index lookup should return 1 for 'omnilogi' but " + "got\n"
                       + responses.toXML(), responses.toXML().contains("<term count=\"0\">omnilogi</term>"));
        }
        // TODO: IndexDescriptor-load?
        searcher.close();
        storage.close();
    }


    public static void verifyFacetResult(SummaSearcher searcher, String query) throws IOException {
        verifyFacetResult(searcher, query, true);
    }

    public static void verifyFacetResult(SummaSearcher searcher, String query, boolean shouldExist) throws IOException {
        String res = searcher.search(SearchTest.simpleRequest(query)).toXML();
        boolean contains = res.contains("<facet name=\"author\">");
        if ((shouldExist && !contains)) {
            fail("Search for '" + query + "' did not produce any facets. Result was:\n" + res);
        }

        if ((!shouldExist && contains)) {
            fail("Search for '" + query + "' did produce facets when it should " + "not. Result was:\n" + res);
        }
    }

    public void testTwoDocumentsOneHitSearch() throws Exception {
        final String STORAGE = "2doc1hit_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);

        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_jh_gm);
        Record gurliRecord = storage.getRecord("fagref:gm@example.com", null);
        assertNotNull("There should be a Gurli Record", gurliRecord);
        assertEquals("The Records-count should be correct after first ingest", 2, countRecords(storage, "fagref"));

        updateIndex(STORAGE);
        log.debug("Index updated. Creating searcher");
        SummaSearcherImpl searcher = new SummaSearcherImpl(getSearcherConfiguration());
        log.debug("Searcher created. Verifying existence of Gurli data");
        SearchTest.verifySearch(searcher, "Gurli", 1);
        log.debug("Sample output from search: " + searcher.search(SearchTest.simpleRequest("Gurli")).toXML());
        SearchTest.verifySearch(searcher, "Gurli", 1);
        SearchTest.verifySearch(searcher, "Jens", 1);
        log.debug("Second test-search performed with success");
        verifyFacetResult(searcher, "Jens");
        verifyFacetResult(searcher, "Gurli");
        log.debug("All OK. Closing searcher, storage and returning");
        searcher.close();
        storage.close();
    }

    public void testThreeFileIngest() throws Exception {
        final String STORAGE = "2doc1hit_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);
        try {
            SearchTest.ingestFagref(STORAGE, SearchTest.fagref_1plus2);
            assertEquals("The Records-count should be correct after first ingest", 3, countRecords(storage, "fagref"));

            updateIndex(STORAGE);
            Thread.sleep(5000); // Why do we need to do this?
            log.debug("Index updated. Creating searcher");
            SummaSearcherImpl searcher = new SummaSearcherImpl(getSearcherConfiguration());
            Thread.sleep(5000); // Why do we need to do this?
            searcher.checkIndex(); // Make double sure
            log.debug("Searcher created");
            for (String name : "Jens Gurli Hans".split(" ")) {
                log.debug(String.format("Verifying existence of %s data", name));
                SearchTest.verifySearch(searcher, name, 1);
                verifyFacetResult(searcher, name);
            }
            searcher.close();
        } finally {
            storage.close();
        }
    }

    public void testTagCounting() throws Exception {
        final String STORAGE = "tagcounting_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);
        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_clone);

        assertEquals("The Records-count should be correct after first ingest", 5, countRecords(storage, "fagref"));

        updateIndex(STORAGE);
        log.debug("Index updated. Creating searcher");
        SummaSearcherImpl searcher = new SummaSearcherImpl(getSearcherConfiguration());
        searcher.checkIndex(); // Make double sure
        log.debug("Searcher created");
        for (String name : "Jens Gurli Hans".split(" ")) {
            log.debug(String.format("Verifying existence of %s data", name));
            SearchTest.verifySearch(searcher, name, "Hans".equals(name) ? 3 : 1);
            verifyFacetResult(searcher, name);
        }
        log.debug("Result for search for fagref " + searcher.search(SearchTest.simpleRequest("fagekspert")).toXML());

        log.debug("Repeating tests to check for facet structure re-use");
        for (String name : "Jens Gurli Hans".split(" ")) {
            log.debug(String.format("Verifying existence of %s data", name));
            SearchTest.verifySearch(searcher, name, "Hans".equals(name) ? 3 : 1);
            verifyFacetResult(searcher, name);
        }
        searcher.close();
        storage.close();
    }

    public void testDualIngest() throws Exception {
        final String STORAGE = "dualingest_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);

        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_hj);
        assertEquals("The Records-count should be correct after first ingest", 1, countRecords(storage, "fagref"));
        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_jh_gm);
        assertEquals("The Records-count should be correct after second ingest", 3, countRecords(storage, "fagref"));

        Record hansRecord = storage.getRecord("fagref:hj@example.com", null);
        assertNotNull("There should be a Hans Record", hansRecord);
        Record gurliRecord = storage.getRecord("fagref:gm@example.com", null);
        assertNotNull("There should be a gurli Record", gurliRecord);

        updateIndex(STORAGE);
        Thread.sleep(5000); // Why do we need to do this?
        log.debug("Index updated. Creating searcher");
        SummaSearcherImpl searcher = new SummaSearcherImpl(getSearcherConfiguration());
        Thread.sleep(5000); // Why do we need to do this?
        searcher.checkIndex(); // Make double sure
        log.debug("Searcher created");
        for (String name : "Jens Gurli Hans".split(" ")) {
            log.debug(String.format("Verifying existence of %s data", name));
            SearchTest.verifySearch(searcher, name, 1);
            verifyFacetResult(searcher, name);
        }
        searcher.close();
        storage.close();
    }

    public void testIndexUpdate() throws Exception {
        final String STORAGE = "indexupdate_storage";
        ExposedSettings.debug = true;

        Storage storage = ReleaseHelper.startStorage(STORAGE);

        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_hj);
        assertEquals("The Records-count should be correct after first ingest", 1, countRecords(storage, "fagref"));
        log.info("Initial index build");
        updateIndex(STORAGE);

        log.info("Initial index open");
        SummaSearcherImpl searcher = new SummaSearcherImpl(getSearcherConfiguration());
        assertEquals("There should be 0 heap used by cached facet structures before initial search",
                     0, CollectorPoolFactory.getLastFactory().getMem());
        log.info("Initial search");
        SearchTest.verifySearch(searcher, "Hans", 1);
        assertTrue("There should be > 0 heap used by cached facet structures after first search",
                   CollectorPoolFactory.getLastFactory().getMem() > 0);

        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_jh_gm);
        assertEquals("The Records-count should be correct after second ingest",
                     3, countRecords(storage, "fagref"));
        log.info("Index update");
        updateIndex(STORAGE);

        Thread.sleep(5000); // Index discovery is done each second, so we wait 5 to be reasonably sure is is reopened
        assertEquals("There should be 0 heap used by cached facet structures after index reopen",
                     0, CollectorPoolFactory.getLastFactory().getMem());
        log.info("Second search (index should have been re-opened)");
        SearchTest.verifySearch(searcher, "Gurli", 1);
        assertTrue("There should be > 0 heap used by cached facet structures after second search",
                   CollectorPoolFactory.getLastFactory().getMem() > 0);

        searcher.close();
        storage.close();
    }

/*    private int countFacetCaches(SummaSearcherImpl searcher) {
        CollectorPoolFactory.getLastFactory().
        SearchNode inner = searcher.getSearchNode();
        if (!(inner instanceof SearchNodeAggregator)) {
            throw new IllegalStateException(
                    "Expected the inner SearchNode to be a SearchNodeAggregator, but found " + inner.getClass());
        }
        SearchNodeAggregator aggregator = (SearchNodeAggregator)inner;
        for (SearchNode node: aggregator) {
            if (node instanceof FacetSearchNode) {

            }
        }
    }    */

    private int countRecords(Storage storage, String base) throws IOException {
        long iterKey = storage.getRecordsModifiedAfter(0, base, null);
        Iterator<Record> i = new StorageIterator(storage, iterKey);
        int counter = 0;
        while (i.hasNext()) {
            counter++;
            i.next();
        }
        return counter;
    }

    public void testFacetSearch() throws Exception {
        final String STORAGE = "facetsearch_storage";

//        ExposedSettings.debug = true;
        log.debug("Getting configuration for searcher");
        Configuration conf = getSearcherConfiguration();
        log.debug("Creating Searcher");
        SummaSearcherImpl searcher = new SummaSearcherImpl(conf);
        log.debug("Searcher created");
        Storage storage = ReleaseHelper.startStorage(STORAGE);
        log.debug("Storage started");
        updateIndex(STORAGE);
        log.debug("Update 1 performed");
        searcher.checkIndex();
        // An empty index should return 0 hits
/*        log.debug("CheckIndex called");
        try {
            String searchResult =
                    searcher.search(SearchTest.simpleRequest("dummy")).toXML();
            // TODO: Check if this is what we want
            fail("An timeout-Exceptions should be thrown with empty index. "
                 + "Instead we got the result\n" + searchResult);
        } catch (RemoteException e) {
            // Expected
        }*/
        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_hj);
        log.debug("Ingest 1 performed");
        updateIndex(STORAGE);
        log.debug("Update 2 performed");
        searcher.checkIndex();
        log.debug("Checkindex after Update 2 performed, verifying...");
        SearchTest.verifySearch(searcher, "Hans", 1);
        verifyFacetResult(searcher, "Hans");
        log.debug("Sample output after initial ingest: "
                  + searcher.search(SearchTest.simpleRequest("fagekspert")).toXML());
        log.debug("Adding new material");
        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_jh_gm);
        updateIndex(STORAGE);
        log.debug("Waiting for the searcher to discover the new index");
        searcher.checkIndex(); // Make double sure
        log.debug("Verify final index");
        SearchTest.verifySearch(searcher, "Gurli", 1);
        SearchTest.verifySearch(searcher, "Gurli", 1); // Yes, we try again
        SearchTest.verifySearch(searcher, "Hans", 1);
        verifyFacetResult(searcher, "Gurli");
        verifyFacetResult(searcher, "Hans"); // Why is Hans especially a problem?
        log.debug(
                "Sample output from large search: " + searcher.search(SearchTest.simpleRequest("fagekspert")).toXML());

        searcher.close();
        storage.close();
    }

    public void testFacetConcurrency() throws Exception {
        final int MAX_CONCURRENT = 10;
        final int THREADS = 10;
        final int HAMMER_RUNS = 20;
        final Random random = new Random(87);
        final String STORAGE = "facetsearch_concurrent";

//        ExposedSettings.debug = true;
        Configuration conf = getSearcherConfiguration();
        final SummaSearcherImpl searcher = new SummaSearcherImpl(conf);
        Storage storage = ReleaseHelper.startStorage(STORAGE);
        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_hj);
        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_jh_gm);
        updateIndex(STORAGE);
        searcher.checkIndex(); // Make double sure

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Future> tasks = new ArrayList<Future>(THREADS);
        log.info("Starting " + THREADS + " search threads");
        for (int t = 0; t < THREADS; t++) {
            FutureTask task = new FutureTask(new Callable() {
                @Override
                public Object call() throws Exception {
                    for (int h = 0; h < HAMMER_RUNS; h++) {
                        verifyFacetResult(searcher, "Gurli");
//                        SearchTest.verifySearch(searcher, "Gurli", 1);
                        if (h < HAMMER_RUNS - 1) {
                            Thread.sleep(random.nextInt(10));
                        }
                    }
                    return null;
                }
            });
            tasks.add(task);
            executor.submit(task);
        }
        log.info("Waiting for search threads to finish");
        int counter = 0;
        for (Future task : tasks) {
            int concurrent = searcher.getConcurrentSearches();
            log.debug("Waiting for task " + counter++ + ". Concurrent searchers: " + concurrent);
            task.get();
        }
        log.info("Finished with maximum concurrent searches " + searcher.getMaxConcurrent()
                 + " with a theoretical max of " + MAX_CONCURRENT);
        assertTrue(
                "The maximum concurrent searchers should be > 1, but was " + searcher.getMaxConcurrent(),
                searcher.getMaxConcurrent() > 1);

        log.debug(
                "Sample output from large search: " + searcher.search(SearchTest.simpleRequest("fagekspert")).toXML());
        searcher.close();
        storage.close();
    }

/*
    public void testFacetSearchExisting() throws Exception {
        File INDEX = new File("/home/te/tmp/sumfresh/sites/sb/index/sb/");
//        File DESCRIPTOR = new File("/home/te/tmp/sumfresh/sites/sb/index/sb/"
//                                   + "20110830-142012/IndexDescriptor.xml");
        File SEARCHER = new File(
            "/home/te/tmp/sumfresh/sites/sb/config/searcher_sb.xml");
//        ExposedSettings.debug = true;
        log.debug("Getting configuration for searcher");

//        URL descriptorLocation = DESCRIPTOR.toURI().toURL();

        Configuration searcherConf = Configuration.load(
            SEARCHER.getAbsolutePath());
//        searcherConf.getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).
//                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
//                     descriptorLocation.getFile());
        searcherConf.set(
            IndexWatcher.CONF_INDEX_WATCHER_INDEX_ROOT, INDEX.toString());
//        searcherConf.set(SummaSearcherImpl.CONF_STATIC_ROOT, INDEX.toString());


        log.debug("Creating Searcher");
        SummaSearcherImpl searcher = new SummaSearcherImpl(searcherConf);
        log.debug("Triggering index open");
        searcher.checkIndex();
        log.debug("Searcher created");
        searcher.checkIndex();
        ResponseCollection responses =
            searcher.search(SearchTest.simpleRequest("freetext:bog"));
        log.debug("Sample output after initial ingest: " + responses.toXML());
        Pattern problem = Pattern.compile(
            "(?s).+<tag name=\"grd kre\" addedobjects=\"([0-9]+)\">.+");
        Matcher matcher = problem.matcher(responses.toXML());
        assertTrue("The name 'grd kre' should be present",
                   matcher.matches());
        assertEquals("The count for 'grd kre' should be 1 but was "
                     + matcher.group(1), "1", matcher.group(1));
        searcher.close();
    }
  */


    // does not work atm. See comment below: ***
    public void testFacetSearchDelete() throws Exception {
        final String STORAGE = "facetsearch_delete_storage";
        final String HANS = "fagref:hj@example.com";

//        ExposedSettings.debug = true;
        log.debug("Getting configuration for searcher");
        Configuration conf = getSearcherConfiguration();
        log.debug("Creating Searcher");
        SummaSearcherImpl searcher = new SummaSearcherImpl(conf);
        log.debug("Searcher created");
        Storage storage = ReleaseHelper.startStorage(STORAGE);
        log.debug("Storage started");
        updateIndex(STORAGE);
        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_hj);
        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_jh_gm);
        log.debug("Ingest 1+2 performed");
        updateIndex(STORAGE);
        log.debug("Waiting for the searcher to discover the new index");
        searcher.checkIndex(); // Make double sure
        log.debug("Verify index with 3 fagref Records");
        SearchTest.verifySearch(searcher, "Gurli", 1);
        SearchTest.verifySearch(searcher, "Hans", 1);
        verifyFacetResult(searcher, "Gurli");
        verifyFacetResult(searcher, "Hans"); // Why is Hans especially a problem?
        Record hans = storage.getRecord(HANS, null);
        assertNotNull("The Hans Record should exist", hans);
        long originalModTime = hans.getModificationTime();

        log.debug("Sleeping to ensure that progress is properly updated");
        Thread.sleep(1000);

        log.debug("Deleting document");
        hans.setDeleted(true);
        //***
        //This update does not work for some reason. Storage-record is not changed
        // and program does not even enter the storage-code to update/insert records.
        //Hard to figure out what happens.
        SearchTest.update(STORAGE, hans);

        Thread.sleep(200);


        hans = storage.getRecord(HANS, null);
        long deleteModTime = hans.getModificationTime();
        assertTrue("The Hans-Record should be marked as deleted but was " + hans, hans.isDeleted());
        assertFalse("The modification time for the Hans Records should differ", originalModTime == deleteModTime);
        log.debug("Extracted deleted hans record " + hans.toString(true));
        log.debug("Updating index after update of " + HANS);
        updateIndex(STORAGE);
        searcher.checkIndex(); // Make double sure

        log.debug("Verifying index after update of Hans Jensen");
        SearchTest.verifySearch(searcher, "Gurli", 1);
        SearchTest.verifySearch(searcher, "Hans", 0);

        log.debug("Sample output from large search: "
                  + searcher.search(SearchTest.simpleRequest("fagekspert")).toXML());

        searcher.close();
        storage.close();
    }

    // TODO: Move this to SearchTest
    public void testFastHitCount() throws Exception {
        final String STORAGE = "fasthitcount_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);

        Configuration conf = getSearcherConfiguration();
        SummaSearcherImpl searcher = new SummaSearcherImpl(conf);
        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_hj);
        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_jh_gm);
        updateIndex(STORAGE);
        updateIndex(STORAGE);
        searcher.checkIndex();
        SearchTest.verifySearch(searcher, "Gurli", 1);
        SearchTest.verifySearch(searcher, "Hans", 1);

        {
            Request request = new Request();
            request.put(DocumentKeys.SEARCH_QUERY, "Gurli");
            Response response = searcher.search(request).iterator().next();
            assertTrue("There should be at least one hit for standard search", SearchTest.getHits(response) > 0);
        }

        {
            Request request = new Request();
            request.put(DocumentKeys.SEARCH_QUERY, "Gurli");
            request.put(DocumentKeys.SEARCH_MAX_RECORDS, 0);
            request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
            Response response = searcher.search(request).iterator().next();
            assertTrue("There should be at least one hit for 0 max result", SearchTest.getHits(response) > 0);
        }

        {
            Request request = new Request();
            request.put(DocumentKeys.SEARCH_QUERY, "Invalid¤%ffd");
            request.put(DocumentKeys.SEARCH_MAX_RECORDS, 0);
            request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
            Response response = searcher.search(request).iterator().next();
            assertEquals("There should be zero hits for non-existing search term", 0, SearchTest.getHits(response));
        }

        {
            Request request = new Request();
            request.put(DocumentKeys.SEARCH_QUERY, "*");
            request.put(DocumentKeys.SEARCH_MAX_RECORDS, 0);
            request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
            Response response = searcher.search(request).iterator().next();
            assertTrue("There should be at least one hit for * search", SearchTest.getHits(response) > 0);
        }

        searcher.close();
        storage.close();
    }


    public void testExplain() throws Exception {
        final String STORAGE = "explain_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);
        Configuration conf = getSearcherConfiguration();
        SummaSearcherImpl searcher = new SummaSearcherImpl(conf);
        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_hj);
        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_jh_gm);
        assertNotNull("Gurli should exist in Storage", ReleaseHelper.getRecord(STORAGE, "fagref:gm@example.com"));
        updateIndex(STORAGE);
        searcher.checkIndex();
        SearchTest.verifySearch(searcher, "Gurli", 1); // Just checking

        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "Gurli");
        request.put(DocumentKeys.SEARCH_EXPLAIN, true);
        log.debug("Sample output from explain search: " + searcher.search(request).toXML());

        searcher.close();
        storage.close();
    }

    public void testSort() throws Exception {
        final String STORAGE = "sort_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);
        try {
            Configuration conf = getSearcherConfiguration();
            SummaSearcherImpl searcher = new SummaSearcherImpl(conf);
            SearchTest.ingestFagref(STORAGE, SearchTest.fagref_hj);
            SearchTest.ingestFagref(STORAGE, SearchTest.fagref_jh_gm);
            updateIndex(STORAGE);
            searcher.checkIndex();

            Request request = new Request();
            request.put(DocumentKeys.SEARCH_QUERY, "fagekspert");
            request.put(DocumentKeys.SEARCH_SORTKEY, "sort_title");
            String result = searcher.search(request).toXML();
            String first = Strings.join(getIDs(result), ", ");
            log.debug("IDs from sort_title sort: " + first);

            request.put(DocumentKeys.SEARCH_REVERSE, true);
            result = searcher.search(request).toXML();
            String second = Strings.join(getIDs(result), ", ");
            log.debug("IDs from sort_title reverse: " + second);

            assertFalse(String.format("The first IDs '%s' should be in reverse order of the second IDs '%s'", first, second), first.equals(second));
            searcher.close();
        } finally {
            storage.close();
        }
    }

    public void testTiming() throws Exception {
        final String STORAGE = "timing_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);

        Configuration conf = getSearcherConfiguration();
        SummaSearcherImpl searcher = new SummaSearcherImpl(conf);
        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_hj);
        SearchTest.ingestFagref(STORAGE, SearchTest.fagref_jh_gm);
        updateIndex(STORAGE);
        searcher.checkIndex();
        storage.close();

        Request request = new Request(DocumentKeys.SEARCH_QUERY, "fagekspert", DocumentKeys.SEARCH_SORTKEY, "sort_title", DocumentKeys.SEARCH_COLLECT_DOCIDS, "true");
        final Pattern TIMING = Pattern.compile(" timing=\"(.+?)\"", Pattern.DOTALL);

        String timing = getPattern(searcher, request, TIMING).get(0);
        String[] timings = timing.split("[|]");
        assertTrue("There should be some timing information", timings.length > 0);
        Arrays.sort(timings);
        String last = null;
        for (String t : timings) {
            if (last != null) {
                assertFalse("The timing '" + t + "' should differ from the previous timing", last.equals(t));
            }
            last = t;
            log.debug(t);
        }
        searcher.close();
    }

    private List<String> getAttributes(SummaSearcher searcher, Request request, String attributeName) throws IOException {
        final Pattern IDPATTERN = Pattern.compile("<record.*?" + attributeName + "=\"(.+?)\".*?>", Pattern.DOTALL);
        return getPattern(searcher, request, IDPATTERN);
    }

    private List<String> getPattern(SummaSearcher searcher, Request request, Pattern pattern) throws IOException {
        ResponseCollection responses = searcher.search(request);
        String xml = responses.toXML();
        Matcher matcher = pattern.matcher(xml);
        List<String> result = new ArrayList<String>();
        while (matcher.find()) {
            result.add(Strings.join(matcher.group(1).split("\n"), ", "));
        }
        return result;
    }


    private Pattern idPattern = Pattern.compile("fagref\\:[a-z]+\\@example\\.com");

    private List<String> getIDs(String xml) {
        Matcher matcher = idPattern.matcher(xml);
        List<String> matches = new ArrayList<String>(10);
        while (matcher.find()) {
            matches.add(matcher.group(0));
        }
        return matches;
    }

    public void testFacetBuild() throws Exception {
        final String STORAGE = "facetbuild_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);
        try {
            updateIndex(STORAGE);
        } finally {
            storage.close();
        }
    }

    public static void updateIndex(String storageID) throws Exception {
        Configuration indexConf = IndexTest.loadFagrefProperties(storageID, "integration/search/FacetTest_IndexConfiguration.xml");
        IndexTest.updateIndex(indexConf);
    }
}
