/**  Licensed under the Apache License, Version 2.0 (the "License");
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
package dk.statsbiblioteket.summa.support.solr;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys;
import dk.statsbiblioteket.summa.index.IndexController;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.search.SearchNode;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.support.embeddedsolr.EmbeddedJettyWithSolrServer;
import dk.statsbiblioteket.summa.support.summon.search.SummonSearchNode;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SolrSearchNodeTest extends TestCase {
    private static Log log = LogFactory.getLog(SolrSearchNodeTest.class);

    public static final String SOLR_HOME = "support/solr_home1"; //data-dir (index) will be created here.

    private EmbeddedJettyWithSolrServer server = null;

    public SolrSearchNodeTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("basedir", ".");
        // TODO: Clear existing data
        server = new EmbeddedJettyWithSolrServer(SOLR_HOME);
        server.run();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        server.stopSolr();
    }

    public static Test suite() {
        return new TestSuite(SolrSearchNodeTest.class);
    }

    public void testBasicSearch() throws Exception {
        performBasicIngest();
        SearchNode searcher = getSearcher();
        try {
            ResponseCollection responses = new ResponseCollection();
            searcher.search(new Request(
                DocumentKeys.SEARCH_QUERY, "fulltext:first",
                SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "recordId score title fulltext"
            ), responses);
            assertTrue("There should be a response", responses.iterator().hasNext());
            assertEquals("There should be the right number of hits. Response was\n" + responses.toXML(),
                         1, ((DocumentResponse)responses.iterator().next()).getHitCount());

            String PHRASE = "Solr sample document";
            assertTrue("The result should contain the phrase '" + PHRASE + "'", responses.toXML().contains(PHRASE));
        } finally {
            searcher.close();
        }
    }

    public void testFilterFacets() throws Exception {
        performBasicIngest();
        final String QUERY = "recordID:doc1";
        final String FACET = "title:Document";
        SearchNode searcher = getSearcher();
        try {
            assertEquals("There should be at least one hit for standard positive faceting",
                         1, getHits(searcher,
                                    DocumentKeys.SEARCH_QUERY, QUERY,
                                    DocumentKeys.SEARCH_COLLECT_DOCIDS, "true",
                                    FacetKeys.SEARCH_FACET_FACETS, "fulltext",
                                    DocumentKeys.SEARCH_FILTER, FACET));
            assertEquals("There should be at least one hit for facet filter positive faceting",
                         1, getHits(searcher,
                                    DocumentKeys.SEARCH_QUERY, QUERY,
                                    DocumentKeys.SEARCH_COLLECT_DOCIDS, "true",
                                    FacetKeys.SEARCH_FACET_FACETS, "fulltext",
                                    DocumentKeys.SEARCH_FILTER, FACET,
                                    SummonSearchNode.SEARCH_SOLR_FILTER_IS_FACET, "true"));
        } finally {
            searcher.close();
        }
    }

    public void testFilterFacetsNoHits() throws Exception {
        performBasicIngest();
        final String QUERY = "recordID:doc1";
        final String FACET = "title:nonexisting";
        SearchNode searcher = getSearcher();
        try {
            assertEquals("There should be at least one hit for search with no filter",
                         1, getHits(searcher,
                                    DocumentKeys.SEARCH_QUERY, QUERY,
                                    DocumentKeys.SEARCH_COLLECT_DOCIDS, "true",
                                    FacetKeys.SEARCH_FACET_FACETS, "fulltext"));
            assertEquals("There should be zero hits for basic filter search",
                         0, getHits(searcher,
                                    DocumentKeys.SEARCH_QUERY, QUERY,
                                    DocumentKeys.SEARCH_COLLECT_DOCIDS, "true",
                                    FacetKeys.SEARCH_FACET_FACETS, "fulltext",
                                    DocumentKeys.SEARCH_FILTER, FACET));
            assertEquals("There should be zero hits for facet-is-filter faceting",
                         0, getHits(searcher,
                                    DocumentKeys.SEARCH_QUERY, QUERY,
                                    DocumentKeys.SEARCH_COLLECT_DOCIDS, "true",
                                    FacetKeys.SEARCH_FACET_FACETS, "fulltext",
                                    DocumentKeys.SEARCH_FILTER, FACET,
                                    SummonSearchNode.SEARCH_SOLR_FILTER_IS_FACET, "true"));
        } finally {
            searcher.close();
        }
    }

    public void testNonQualifiedSearch() throws Exception {
        performBasicIngest();
        SearchNode searcher = getSearcher();
        try {
            assertResult(searcher, new Request(
                DocumentKeys.SEARCH_QUERY, "first",
                SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "recordId score title fulltext"
            ), 1, "Solr sample document");
        } finally {
            searcher.close();
        }
    }

    // This test was for an old bug where queries with parenthesis had to be written with spaces after start parenthesis
    public void testParentheses() throws Exception {
        performBasicIngest();
        Request request = new Request(
            DocumentKeys.SEARCH_QUERY, "(+fulltext:first)",
            SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "recordId score title fulltext"
        );
/*        {
            SearchNode searcher = new SolrSearchNode(Configuration.newMemoryBased(
                SolrSearchNode.CONF_COMPENSATE_FOR_PARENTHESIS_BUG, false
            ));
            assertResult(searcher, request, 0, null);
            searcher.close();
        }*/
        {
            SearchNode searcher = new SolrSearchNode(Configuration.newMemoryBased());
            try {
                assertResult(searcher, request, 1, "Solr sample document");
            } finally {
                searcher.close();
            }
        }
    }

    public void testNonQualifiedEdismax() throws Exception {
        performBasicIngest();
        SearchNode searcher = new SolrSearchNode(Configuration.newMemoryBased(
            SolrSearchNode.CONF_SOLR_RESTCALL, "/solr/edismax"
        ));
        try {
            assertResult(searcher, new Request(
                DocumentKeys.SEARCH_QUERY, "first",
                SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "recordId score title fulltext"
            ), 1, "Solr sample document");
        } finally {
            searcher.close();
        }
    }

    private void assertResult(
        SearchNode searcher, Request request, int expectedResponses, String expectedContent) throws RemoteException {
        ResponseCollection responses = new ResponseCollection();
        searcher.search(request, responses);
        assertTrue("There should be a response", responses.iterator().hasNext());
        assertEquals("There should be the right number of hits. Response was\n" + responses.toXML(),
                     expectedResponses, ((DocumentResponse) responses.iterator().next()).getHitCount());
        if (expectedContent != null) {
            assertTrue("The result should contain '" + expectedContent + "'",
                       responses.toXML().contains(expectedContent));
        }
    }

    public void testFacetedSearch() throws Exception {
        performBasicIngest();
        SearchNode searcher = getSearcher();
        try {
            testFacetedSearch(searcher);
            testFacetedSearchFilter(searcher);
        } finally {
            searcher.close();
        }
    }

    private void testFacetedSearch(SearchNode searcher) throws Exception {
        ResponseCollection responses = new ResponseCollection();
        searcher.search(new Request(
            DocumentKeys.SEARCH_QUERY, "first",
            SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "recordID score title fulltext",
            DocumentKeys.SEARCH_COLLECT_DOCIDS, true,
            FacetKeys.SEARCH_FACET_FACETS, "fulltext"
        ), responses);

        assertTrue("There should be a response", responses.iterator().hasNext());
        assertEquals("There should be the right number of hits. Response was\n" + responses.toXML(),
                     1, ((DocumentResponse) responses.iterator().next()).getHitCount());
        assertTrue("The result should contain tag 'solr' with count 1\n" + responses.toXML(),
                   responses.toXML().contains("<tag name=\"solr\" addedobjects=\"1\" reliability=\"PRECISE\">"));
//        System.out.println(responses.toXML());
    }

    private void testFacetedSearchFilter(SearchNode searcher) throws Exception {
        ResponseCollection responses = new ResponseCollection();
        searcher.search(new Request(
            DocumentKeys.SEARCH_FILTER, "recordBase:dummy",
            DocumentKeys.SEARCH_QUERY, "first",
            SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "recordId score title fulltext",
            DocumentKeys.SEARCH_COLLECT_DOCIDS, true,
            FacetKeys.SEARCH_FACET_FACETS, "fulltext"
        ), responses);
        assertTrue("There should be a response with filter", responses.iterator().hasNext());
        assertEquals("There should be the right number of hits with filter. Response was\n" + responses.toXML(),
                     1, ((DocumentResponse) responses.iterator().next()).getHitCount());
        assertTrue("The result should contain tag 'solr' with count 1 with filter\n" + responses.toXML(),
                   responses.toXML().contains("<tag name=\"solr\" addedobjects=\"1\" reliability=\"PRECISE\">"));
//        System.out.println(responses.toXML());
    }

    public void testFacetedSearchWrongBase() throws Exception {
        SearchNode searcher = getSearcher();
        try {
            ResponseCollection responses = new ResponseCollection();
            searcher.search(new Request(
                DocumentKeys.SEARCH_FILTER, "recordBase:nothere",
                DocumentKeys.SEARCH_QUERY, "first",
                SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "recordId score title fulltext",
                DocumentKeys.SEARCH_COLLECT_DOCIDS, true,
                FacetKeys.SEARCH_FACET_FACETS, "fulltext"
            ), responses);
            assertTrue("There should be a response", responses.iterator().hasNext());
            assertEquals("There should be the right number of hits. Response was\n" + responses.toXML(),
                         0, ((DocumentResponse) responses.iterator().next()).getHitCount());
//        System.out.println(responses.toXML());
        } finally {
            searcher.close();
        }
    }

    public void testExposedFacetedSearchAuto() throws Exception {
        performBasicIngest();
        SearchNode searcher = new SBSolrSearchNode(Configuration.newMemoryBased(
            SBSolrSearchNode.CONF_USE_EFACET, "true"
        ));
        try {
            testFacetedSearch(searcher);
            testFacetedSearchFilter(searcher);
        } finally {
            searcher.close();
        }
    }

    public void testExposedFacetedSearchRest() throws Exception {
        performBasicIngest();
        SearchNode searcher = new SolrSearchNode(Configuration.newMemoryBased(
            SolrSearchNode.CONF_SOLR_RESTCALL, "/solr/exposed"
        ));
        try {
            // qt=exprh&efacet=true&efacet.field=path_ss&q=*%3A*&fl=id&version=2.2&start=0&rows=10&indent=on
            assertExposed(searcher, new Request(
                //SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "qt", "exprh",
                SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "efacet", "true",
                FacetKeys.SEARCH_FACET_FACETS, "fulltext", // TODO: Remove reliance on this for the SolrResponseBuilder
                SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "efacet.field", "fulltext",
                SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "q", "solr"
            ));
        } finally {
            searcher.close();
        }
    }

    public void testExposedFacetedSearchHandler() throws Exception {
        performBasicIngest();
        SearchNode searcher = new SBSolrSearchNode(Configuration.newMemoryBased(
//            SolrSearchNode.CONF_SOLR_RESTCALL, "/solr/exposed"
        ));
        // qt=exprh&efacet=true&efacet.field=path_ss&q=*%3A*&fl=id&version=2.2&start=0&rows=10&indent=on
        try {
            assertExposed(searcher, new Request(
                SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "qt", "/exposed",
                SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "efacet", "true",
                FacetKeys.SEARCH_FACET_FACETS, "fulltext", // TODO: Remove reliance on this for the SolrResponseBuilder
                SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "efacet.field", "fulltext",
                SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "q", "solr"
            ));
        } finally {
            searcher.close();
        }
    }

    private void assertExposed(SearchNode searcher, Request request) throws RemoteException {
        ResponseCollection responses = new ResponseCollection();
        searcher.search(request, responses);
//        System.out.println(responses.toXML());
        assertTrue("There should be a response", responses.iterator().hasNext());
        assertEquals("There should be the right number of hits. Response was\n" + responses.toXML(),
                     2, ((DocumentResponse) responses.iterator().next()).getHitCount());
        assertTrue("The result should contain tag 'solr' with count 1\n" + responses.toXML(),
                   responses.toXML().contains("<tag name=\"solr\" addedobjects=\"2\" reliability=\"PRECISE\">"));
    }

    private void performBasicIngest() throws Exception {
        ObjectFilter data = getDataProvider(false);
        ObjectFilter indexer = getIndexer();
        indexer.setSource(data);
        for (int i = 0 ; i < SAMPLES ; i++) {
            assertTrue("Check " + (i+1) + "/" + SAMPLES + ". There should be a next for the indexer",
                       indexer.hasNext());
            indexer.next();
        }
        assertFalse("After " + SAMPLES + " nexts, there should be no more Payloads", indexer.hasNext());
        indexer.close(true);
        log.debug("Finished basic ingest");
    }

    final int SAMPLES = 2;
    private ObjectFilter getDataProvider(boolean deleted) throws IOException {
        List<Payload> samples = new ArrayList<Payload>(SAMPLES);
        for (int i = 1 ; i <= SAMPLES ; i++) {
            Payload payload = new Payload(new Record(
                "doc" + i, "dummy", Resolver.getUTF8Content(
                "integration/solr/SolrSampleDocument" + i + ".xml").getBytes("utf-8")));
            payload.getRecord().setDeleted(deleted);
            samples.add(payload);
        }
        return new PayloadFeederHelper(samples);
    }

    private IndexController getIndexer() throws IOException {
        Configuration controllerConf = Configuration.newMemoryBased(
            IndexController.CONF_FILTER_NAME, "testcontroller");
        Configuration manipulatorConf = controllerConf.createSubConfigurations(
            IndexControllerImpl.CONF_MANIPULATORS, 1).get(0);
        manipulatorConf.set(IndexControllerImpl.CONF_MANIPULATOR_CLASS, SolrManipulator.class.getCanonicalName());
        manipulatorConf.set(SolrManipulator.CONF_ID_FIELD, IndexUtils.RECORD_FIELD); // 'id' is the default ID field for Solr
        return new IndexControllerImpl(controllerConf);
    }

    private SearchNode getSearcher() throws RemoteException {
        return new SolrSearchNode(Configuration.newMemoryBased());
    }

    protected long getHits(SearchNode searcher, String... arguments) throws RemoteException {
        String HITS_PATTERN = "(?s).*hitCount=\"([0-9]*)\".*";
        ResponseCollection responses = new ResponseCollection();
        searcher.search(new Request(arguments), responses);
        if (!Pattern.compile(HITS_PATTERN).matcher(responses.toXML()).
            matches()) {
            return 0;
        }
        String hitsS = responses.toXML().replaceAll(HITS_PATTERN, "$1");
        return "".equals(hitsS) ? 0L : Long.parseLong(hitsS);
    }

    protected void assertHits(
        String message, SearchNode searcher, String... queries)
        throws RemoteException {
        long hits = getHits(searcher, queries);
        assertTrue(message + ". Hits == " + hits, hits > 0);
    }
}
