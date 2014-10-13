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
import dk.statsbiblioteket.summa.common.filter.Filter;
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
import dk.statsbiblioteket.summa.support.api.LuceneKeys;
import dk.statsbiblioteket.summa.support.embeddedsolr.EmbeddedJettyWithSolrServer;
import dk.statsbiblioteket.summa.support.summon.search.SummonSearchNode;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SolrSearchNodeTest extends TestCase {
    private static Log log = LogFactory.getLog(SolrSearchNodeTest.class);

    public static final String SOLR_HOME = "support/solr_home_default"; //data-dir (index) will be created here.

    private EmbeddedJettyWithSolrServer server = null;

    public SolrSearchNodeTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("basedir", ".");
        server = new EmbeddedJettyWithSolrServer(SOLR_HOME);
        server.run();

        // Clear existing
        SolrSearchNode searcher = getSearcher();
        searcher.clear();
        searcher.close();
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
        ResponseCollection responses = search(new Request(
                DocumentKeys.SEARCH_QUERY, "fulltext:first",
                SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "recordId score title_org fulltext"
        ));
        assertTrue("There should be a response", responses.iterator().hasNext());
        assertEquals("There should be the right number of hits. Response was\n" + responses.toXML(),
                     1, ((DocumentResponse)responses.iterator().next()).getHitCount());

        String PHRASE = "Solr sample document";
        assertTrue("The result should contain the phrase '" + PHRASE + "'", responses.toXML().contains(PHRASE));
    }

    public void testSolrParamPrefix() throws Exception {
        performBasicIngest();
        ResponseCollection responses = search(new Request(
                "foosearch." + SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "q", "fulltext:first",
                SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "recordId score title_org fulltext"
        ));
        assertTrue("There should be a response", responses.iterator().hasNext());
        assertEquals("There should be the right number of hits. Response was\n" + responses.toXML(),
                     1, ((DocumentResponse)responses.iterator().next()).getHitCount());

        String PHRASE = "Solr sample document";
        assertTrue("The result should contain the phrase '" + PHRASE + "'", responses.toXML().contains(PHRASE));
    }

    public void testSolrMultiFilter() throws Exception {
        performBasicIngest();
        {
            ResponseCollection responses = search(new Request(
                    DocumentKeys.SEARCH_QUERY, "*:*"
            ));
            assertEquals("There should be the right number of hits for match all.\n" + responses.toXML(),
                         2, ((DocumentResponse)responses.iterator().next()).getHitCount());
        }
        {
            ResponseCollection responses = search(new Request(
                    DocumentKeys.SEARCH_QUERY, "fulltext:first"
            ));
            assertEquals("There should be the right number of hits for plain query. Response was\n" + responses.toXML(),
                         1, ((DocumentResponse)responses.iterator().next()).getHitCount());
        }
        {
            ResponseCollection responses = search(new Request(
                    DocumentKeys.SEARCH_QUERY, "*:*",
                    DocumentKeys.SEARCH_FILTER, "recordID:doc1"
            ));
            assertEquals("There should be the right number of hits for single filter query 1.\n" + responses.toXML(),
                         1, ((DocumentResponse)responses.iterator().next()).getHitCount());
        }
        {
            ResponseCollection responses = search(new Request(
                    DocumentKeys.SEARCH_QUERY, "*:*",
                    DocumentKeys.SEARCH_FILTER, "recordID:doc2"
            ));
            assertEquals("There should be the right number of hits for single filter query 2.\n" + responses.toXML(),
                         1, ((DocumentResponse)responses.iterator().next()).getHitCount());
        }
        {
            ResponseCollection responses = search(new Request(
                    DocumentKeys.SEARCH_QUERY, "*:*",
                    DocumentKeys.SEARCH_FILTER, new ArrayList<>(Arrays.asList(new String[]{
                    "recordID:doc1", "recordID:doc2"}))
            ));
            assertEquals("There should be the right number of hits for multi filter query.\n" + responses.toXML(),
                         0, ((DocumentResponse)responses.iterator().next()).getHitCount());
        }
    }

    public void testSolrParamPlainFilter() throws Exception {
        performBasicIngest();
        {
            ResponseCollection responses = search(new Request(
                    DocumentKeys.SEARCH_FILTER, "fulltext:nomatch",
                    "foosearch." + SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "q", "fulltext:first",
                    SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "recordId score title_org fulltext"
            ));
            assertTrue("There should be a response for nomatch", responses.iterator().hasNext());
            assertEquals("There should be the right number of hits for nomatch. Response was\n" + responses.toXML(),
                         0, ((DocumentResponse)responses.iterator().next()).getHitCount());
        }
    }

    // Filters are additive so the nomatch should continue being in effect
    public void testSolrParamPrefixFilterAddition() throws Exception {
        performBasicIngest();
        {
            ResponseCollection responses = search(new Request(
                    DocumentKeys.SEARCH_FILTER, "fulltext:nomatch",
                    "foosearch." + SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "q", "fulltext:first",
                    SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "recordId score title_org fulltext",
                    "foosearch." + SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fq", "fulltext:first"
            ));
            assertTrue("There should be a response for nomatch AND match", responses.iterator().hasNext());
            assertEquals("There should be the right number of hits for nomatch AND match. Response was\n"
                         + responses.toXML(),
                         0, ((DocumentResponse)responses.iterator().next()).getHitCount());
        }
    }

    private ResponseCollection search(Request request) throws RemoteException {
        SearchNode searcher = getSearcher();
        ResponseCollection responses = new ResponseCollection();
        try {
            searcher.search(request, responses);
        } finally {
            searcher.close();
        }
        return responses;
    }

    public void testNoQueryNoResult() throws Exception {
        performBasicIngest();
        SearchNode searcher = getSearcher();
        try {
            ResponseCollection responses = new ResponseCollection();
            searcher.search(new Request(
                    SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "recordId score title_org fulltext"
            ), responses);
            assertFalse("There should not be a response", responses.iterator().hasNext());
        } finally {
            searcher.close();
        }
    }

    public void testSorting() throws Exception {
        final int RECORDS = 100;
        ingestFacets(RECORDS);
        SearchNode searcher = getSearcher();
        {
            ResponseCollection responses = new ResponseCollection();
            searcher.search(new Request(
                    DocumentKeys.SEARCH_QUERY, "recordBase:dummy",
//                    DocumentKeys.SEARCH_SORTKEY, "sort_title",
                    DocumentKeys.SEARCH_SORTKEY, "sort_year_asc",
                    //DocumentKeys.SEARCH_REVERSE, "true",
                    DocumentKeys.SEARCH_MAX_RECORDS, 20
//                    DocumentKeys.SEARCH_RESULT_FIELDS, "recordID, sort_title"
            ), responses);
            DocumentResponse docs = (DocumentResponse)responses.iterator().next();
            // TODO: Grep for sortKey != null
            for (DocumentResponse.Record record: docs.getRecords()) {
                System.out.print("Record: " + record + ":");
                for (DocumentResponse.Field field: record.getFields()) {
                    System.out.print(" " + field.getName() + "=\"" + field.getContent() + "\"");
                }
                System.out.println();
            }
        }
    }

    public void testPaging() throws Exception {
        final int RECORDS = 100;
        final int PAGE_SIZE = 20;

        ingestFacets(RECORDS);
        SearchNode searcher = getSearcher();
        {
            ResponseCollection responses = new ResponseCollection();
            searcher.search(new Request(
                    DocumentKeys.SEARCH_QUERY, "recordBase:dummy",
                    DocumentKeys.SEARCH_SORTKEY, "recordID",
                    DocumentKeys.SEARCH_MAX_RECORDS, 20,
                    DocumentKeys.SEARCH_RESULT_FIELDS, "recordID"
            ), responses);
            DocumentResponse docs = (DocumentResponse)responses.iterator().next();
            assertPaging("Result for no start index value", docs, 0, PAGE_SIZE);
        }
        {
            ResponseCollection responses = new ResponseCollection();
            searcher.search(new Request(
                    DocumentKeys.SEARCH_QUERY, "recordBase:dummy",
                    DocumentKeys.SEARCH_SORTKEY, "recordID",
                    DocumentKeys.SEARCH_START_INDEX, 0,
                    DocumentKeys.SEARCH_MAX_RECORDS, 20,
                    DocumentKeys.SEARCH_RESULT_FIELDS, "recordID"
            ), responses);
            DocumentResponse docs = (DocumentResponse)responses.iterator().next();
            assertPaging("Result for start index 0", docs, 0, PAGE_SIZE);
        }
        {
            ResponseCollection responses = new ResponseCollection();
            searcher.search(new Request(
                    DocumentKeys.SEARCH_QUERY, "recordBase:dummy",
                    DocumentKeys.SEARCH_SORTKEY, "recordID",
                    DocumentKeys.SEARCH_START_INDEX, PAGE_SIZE,
                    DocumentKeys.SEARCH_MAX_RECORDS, PAGE_SIZE,
                    DocumentKeys.SEARCH_RESULT_FIELDS, "recordID"
            ), responses);
            DocumentResponse docs = (DocumentResponse)responses.iterator().next();
            assertPaging("Result for start index 20", docs, PAGE_SIZE, PAGE_SIZE);
        }
        searcher.close();
    }

    public void testQueryNoFilter() throws Exception {
        assertResponse(new Request(DocumentKeys.SEARCH_QUERY, "fulltext:first"), true);
    }

    public void testFilterNoQuery() throws Exception {
        assertResponse(new Request(DocumentKeys.SEARCH_FILTER, "fulltext:first"), true);
    }

    public void testFilterEmptyQuery() throws Exception {
        assertResponse(new Request(DocumentKeys.SEARCH_FILTER, "fulltext:first", DocumentKeys.SEARCH_QUERY, ""), false);
    }

    public void testFilterAndQuery() throws Exception {
        assertResponse(new Request(
                DocumentKeys.SEARCH_QUERY, "fulltext:first",
                DocumentKeys.SEARCH_FILTER, "fulltext:first"
        ), true);
    }

    public void testNoFilterNoQuery() throws Exception {
        assertResponse(new Request("foo", "bar"), false);
    }

    private void assertResponse(Request request, boolean responseExpected) throws Exception {
        performBasicIngest();
        SearchNode searcher = getSearcher();
        ResponseCollection responses = new ResponseCollection();
        searcher.search(request, responses);
        if (responseExpected) {
            assertTrue("There should be a response for " + request, responses.iterator().hasNext());
        } else {
            assertFalse("There should not be a response for " + request, responses.iterator().hasNext());
        }
    }

    private void assertPaging(String message, DocumentResponse docs, int start, int pageSize) {
        List<String> expected = new ArrayList<>(pageSize);
        for (int page = start ; page < start + pageSize ; page++) {
            expected.add("doc" + String.format("%07d", page));
        }
        assertEquals(message, Strings.join(expected), Strings.join(getContent(docs, "recordID")));
    }

    private List<String> getContent(DocumentResponse docs, String fieldName) {
        List<String> terms = new ArrayList<>();
        for (DocumentResponse.Record records: docs.getRecords()) {
            for (DocumentResponse.Field field: records.getFields()) {
                if (fieldName.equals(field.getName())) {
                    terms.add(field.getContent());
                }
            }
        }
        return terms;
    }

    public void testFilterFacets() throws Exception {
        performBasicIngest();
        final String QUERY = "recordID:doc1";
        final String FACET = "title_org:Document";
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

    public void testDocIDRequest() throws Exception {
        performBasicIngest();
        SearchNode searcher = getSearcher();
        try {
            assertEquals("There should be 2 hits for request for 2 IDs",
                         2, getHits(searcher, DocumentKeys.SEARCH_IDS, "doc1, doc2"));
        } finally {
            searcher.close();
        }
    }

    public void testFilterFacetsNoHits() throws Exception {
        performBasicIngest();
        final String QUERY = "recordID:doc1";
        final String FACET = "title_org:nonexisting";
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
                SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "recordId score title_org fulltext"
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
                SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "recordId score title_org fulltext"
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
                    SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "recordId score title_org fulltext"
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

  public void testFacetedFieldsOverrideSearch() throws Exception {
    performBasicIngest();
    SearchNode searcher = getSearcher();
    ResponseCollection responses = new ResponseCollection();
      searcher.search(new Request(
              DocumentKeys.SEARCH_QUERY, "first",
              //SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "recordID score title fulltext",
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

    public void testFacetedSearchSummaDefinedSort() throws Exception {
        int DOC_COUNT= 100;
        String[][] EXPECTED_ALPHA = new String[][] {
                {"01 quart", Integer.toString(DOC_COUNT/4)},
                {"02 half", Integer.toString(DOC_COUNT/2)},
                {"03 all", Integer.toString(DOC_COUNT)},
                {"solr", Integer.toString(DOC_COUNT)}
        };
        String[][] EXPECTED_ALPHA_DESC = new String[][] {
                {"03 all", Integer.toString(DOC_COUNT)},
                {"02 half", Integer.toString(DOC_COUNT/2)},
                {"01 quart", Integer.toString(DOC_COUNT/4)},
                {"solr", Integer.toString(DOC_COUNT)} // From the recordBase-facet so it will always be last
        };
        String[][] EXPECTED_COUNT = new String[][] {
                {"03 all", Integer.toString(DOC_COUNT)},
                {"02 half", Integer.toString(DOC_COUNT/2)},
                {"01 quart", Integer.toString(DOC_COUNT/4)},
                {"solr", Integer.toString(DOC_COUNT)}
        };

        ingestFacets(DOC_COUNT);
        {
            SearchNode searcher = new SolrSearchNode(Configuration.newMemoryBased(
                    SolrSearchNode.CONF_SOLR_FACETS, "lma_long(ALPHA)"
            ));
            try {
                assertFacetOrder("ALPHA", searcher, EXPECTED_ALPHA);
            } finally {
                searcher.close();
            }
        }
        ingestFacets(DOC_COUNT);
        {
            SearchNode searcher = new SolrSearchNode(Configuration.newMemoryBased(
                    SolrSearchNode.CONF_SOLR_FACETS, "lma_long(ALPHA_DESC)"
            ));
            try {
                assertFacetOrder("ALPHA_DESC", searcher, EXPECTED_ALPHA_DESC);
            } finally {
                searcher.close();
            }
        }
        {
            SearchNode searcher = new SolrSearchNode(Configuration.newMemoryBased(
                    SolrSearchNode.CONF_SOLR_FACETS, "lma_long(COUNT)"
            ));
            try {
                assertFacetOrder("COUNT", searcher, EXPECTED_COUNT);
            } finally {
                searcher.close();
            }
        }
    }

    public void testFacetLimit() throws Exception {
        int DOC_COUNT= 100;
        String[][] EXPECTED_ALPHA = new String[][] {
                {"01 quart", Integer.toString(DOC_COUNT/4)},
                {"02 half", Integer.toString(DOC_COUNT/2)},
//            {"03 all", Integer.toString(DOC_COUNT)},
                {"solr", Integer.toString(DOC_COUNT)} // Added explicit
        };

        ingestFacets(DOC_COUNT);
        {
            SearchNode searcher = new SolrSearchNode(Configuration.newMemoryBased(
                    SolrSearchNode.CONF_SOLR_FACETS, "lma_long(2 ALPHA)"
            ));
            try {
                assertFacetOrder("ALPHA", searcher, EXPECTED_ALPHA);
            } finally {
                searcher.close();
            }
        }
    }

    public void testFacetLimitMedium() throws Exception {
        testFacetLimit(60);
    }

    public void testFacetLimitHigher() throws Exception {
        testFacetLimit(80);
    }

    public void testFacetLimitHigh() throws Exception {
        testFacetLimit(199);
    }

    // TODO: 200 is the limit. How do we raise it?
    public void testFacetLimitExtraHigh() throws Exception {
        testFacetLimit(201);
    }

    public void testFacetLimit(int docCount) throws Exception {
        String[][] EXPECTED_ALPHA = new String[docCount+1][2];
        for (int i = 0 ; i < docCount ; i++) {
            EXPECTED_ALPHA[i][0] = "document" + String.format("%07d", i);
            EXPECTED_ALPHA[i][1] = "1";
        }
        // 200 is the hard limit and we ask for 201, so the solr-part is for the next facet in the result
        EXPECTED_ALPHA[docCount][0] = "solr";
        EXPECTED_ALPHA[docCount][1] = Integer.toString(docCount);

        log.info("Ingesting " + docCount + " documents");
        ingestFacets(docCount);
        log.info("Finished ingesting " + docCount + " documents");
        {
            SearchNode searcher = new SolrSearchNode(Configuration.newMemoryBased(
                    SolrSearchNode.CONF_SOLR_FACETS, "title_org(" + docCount + " ALPHA)",
                    SolrSearchNode.CONF_SOLR_FACETS_DEFAULTPAGESIZE, "1" // To ensure that overriding works
            ));
            try {
                for (int i = 0 ; i < docCount ; i++) {
                    String docID = "Document_" + String.format("%07d", i);
                    assertHits("There should be a hit for 'title_org:" + docID + "'",
                               searcher, DocumentKeys.SEARCH_QUERY, "title_org:" + docID);
                }
                assertFacetOrder("ALPHA", searcher, EXPECTED_ALPHA);
            } finally {
                searcher.close();
            }
        }
    }

    private void assertFacetOrder(String designation, SearchNode searcher, String[][] expected) throws RemoteException {
        Pattern TAGS = Pattern.compile("<tag name=\"([^\"]+)\" addedobjects=\"([^\"]+)\"[^>]*>", Pattern.DOTALL);
        ResponseCollection responses = new ResponseCollection();
        searcher.search(new Request(
                DocumentKeys.SEARCH_QUERY, "*:*",
                DocumentKeys.SEARCH_COLLECT_DOCIDS, true
        ), responses);

        int count = 0;
        Matcher matcher = TAGS.matcher(responses.toXML());
        while (matcher.find()) {
            if (count >= expected.length) {
                fail("Did not expect pattern match #" + count + ". Tag designation '" + matcher.group(1)
                     + "', count '" + matcher.group(2) + "'");
            }
            assertEquals(designation + ": The tag designation should match at position " + count,
                         expected[count][0], matcher.group(1));
            assertEquals(designation + ": The tag count should match at position " + count,
                         expected[count][1], matcher.group(2));
            count++;
        }
        if (count < expected.length) {
            String missing = "";
            for (int i = count ; i < expected.length ; i++) {
                missing += " " + expected[i][0] + "(" + expected[i][1] + ")";
            }
            fail("Missed " + (expected.length - count) + " expected values:" + missing);
        }
    }

    public void testFacetedSearchMatchAll() throws Exception {
        performBasicIngest();
        SearchNode searcher = new SolrSearchNode(Configuration.newMemoryBased(

        ));
        ResponseCollection responses = new ResponseCollection();
        searcher.search(new Request(
                DocumentKeys.SEARCH_QUERY, "*:*",
                SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "recordID score title_org fulltext",
                DocumentKeys.SEARCH_COLLECT_DOCIDS, true,
                FacetKeys.SEARCH_FACET_FACETS, "fulltext"
        ), responses);

        assertTrue("There should be a response", responses.iterator().hasNext());
        assertEquals("There should be the right number of hits. Response was\n" + responses.toXML(),
                     2, ((DocumentResponse) responses.iterator().next()).getHitCount());
        assertTrue("The result should contain tag 'solr' with count 2\n" + responses.toXML(),
                   responses.toXML().contains("<tag name=\"solr\" addedobjects=\"2\" reliability=\"PRECISE\">"));
//        System.out.println(responses.toXML());
    }

    private void testFacetedSearch(SearchNode searcher) throws Exception {
        ResponseCollection responses = new ResponseCollection();
        searcher.search(new Request(
                DocumentKeys.SEARCH_QUERY, "first",
                SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "recordID score title_org fulltext",
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
                SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "recordId score title_org fulltext",
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
                    SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "fl", "recordId score title_org fulltext",
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

    public void testFuzzySearch() throws IOException {
        testFuzzySearch(5000000);
    }
    private void testFuzzySearch(int samples) throws IOException {
        final String QUERIES[] = new String[] {
                "title_org:mxyzptll~",
                "title_org:mxyzptlk", // Non-fuzzy
                "title_org:mxyzpelk~",
                "title_org:xmyzpelk~",
                "title_org:mxyzptl~"
        };
        ObjectFilter feeder = getFuzzyFeeder(samples, "squid", "mxyzptlk");
        ObjectFilter indexer = getIndexer();
        indexer.setSource(feeder);
        //noinspection StatementWithEmptyBody
        while (indexer.pump());
        indexer.close(true);
        SearchNode searcher = new SBSolrSearchNode(Configuration.newMemoryBased());
        warmup(searcher, 1000);

        ResponseCollection responses = new ResponseCollection();
        Request request = new Request(
                DocumentKeys.SEARCH_QUERY, "foo"
        );
        for (String query: QUERIES) {
            request.put(DocumentKeys.SEARCH_QUERY, query);
            responses.clear();
            long searchTime = -System.nanoTime();
            searcher.search(request, responses);
            searchTime += System.nanoTime();
            assertTrue("There should at least be 1 hit for '" + query + "'",
                       ((DocumentResponse) (responses.iterator().next())).getHitCount() > 0);
            System.out.println("Search time for '" + query + "' was " + searchTime + "ns ~= "
                               + (1000000000L / searchTime) + " searches/s");
        }
    }

    private void warmup(SearchNode searcher, int searches) throws RemoteException {
        log.debug("Performing " + searcher + " searches to warm up searcher");
        final Random random = getDeterministicRandom();
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request(
                DocumentKeys.SEARCH_QUERY, "foo"
        );
        for (int i = 0 ; i < searches ; i++) {
            responses.clear();
            request.put(DocumentKeys.SEARCH_QUERY, "title_org:" + getFuzzyWord(random));
            searcher.search(request, responses);
            assertTrue("There should be at least 1 response in the collection", responses.iterator().hasNext());
        }
    }

    private Random getDeterministicRandom() {
        return new Random(87);
    }


    private ObjectFilter getFuzzyFeeder(final int numDocs, final String... specificEntries) {
        return new ObjectFilter() {
            private Random random = getDeterministicRandom();
            private Profiler profiler = new Profiler(numDocs + specificEntries.length, 10000);
            private int count = 0;
            @Override
            public boolean hasNext() {
                return count < numDocs + specificEntries.length;
            }

            @Override
            public Payload next() {
                if (!hasNext()) {
                    return null;
                }
                String fuzzy = count >= numDocs ? specificEntries[count - numDocs] : getFuzzyWord(random);
                count++;
                profiler.beat();
                if (profiler.getBeats() % 100000 == 0) {
                    log.info("FuzzyFeeder produced Payload #" + profiler.getBeats() + " at "
                             + (int)profiler.getBps(true) + " Payloads/sec. Ready in "
                             + profiler.getTimeLeftAsString(true));
                }
                try {
                    return new Payload(new Record(
                            "doc" + count, "Dummy",
                            ("<doc>\n"
                             + "<field name=\"recordID\">doc" + count + "</field>\n"
                             + "<field name=\"recordBase\">dummy</field>\n"
                             + "<field name=\"title_org\">" + fuzzy + "</field>\n"
                             + "</doc>\n").getBytes("utf-8")));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("UTF-8 should be supported", e);
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not implemented");
            }

            @Override
            public void setSource(Filter filter) {
                throw new IllegalAccessError("Cannot have source");
            }

            @Override
            public boolean pump() throws IOException {
                if (hasNext()) {
                    next();
                }
                return hasNext();
            }

            @Override
            public void close(boolean success) { }
        };
    }

    private StringBuffer sb = new StringBuffer(50);
    private String getFuzzyWord(Random random) {
        int chars = 2 + random.nextInt(20);
        sb.setLength(0);
        for (int i = 0 ; i < chars ; i++) {
            sb.append((char)(97 + random.nextInt(25)));
        }
        return sb.toString();
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

    // TODO: Create a test that actually have MoreLikeThis-hits
    public void disabledtestMoreLikeThis() throws Exception {
        performBasicIngest();
        SearchNode searcher = new SBSolrSearchNode(Configuration.newMemoryBased());
        try {
            Request request = new Request(
                    //DocumentKeys.SEARCH_QUERY, "recordID:doc1"
                    LuceneKeys.SEARCH_MORELIKETHIS_RECORDID, "doc1",
                    SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "mlt.fl", "recordBase,fulltext"
            );

            ResponseCollection responses = new ResponseCollection();
            log.info("Issuing MLT request");
            searcher.search(request, responses);
            assertTrue("There should be a response", responses.iterator().hasNext());
            assertEquals("There should be the right number of hits. Response was\n" + responses.toXML(),
                         1, ((DocumentResponse) responses.iterator().next()).getHitCount());
//            assertTrue("The result should contain tag 'solr' with count 1\n" + responses.toXML(),
//                       responses.toXML().contains("<tag name=\"solr\" addedobjects=\"2\" reliability=\"PRECISE\">"));
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

    private void ingestFacets(int docCount) throws IOException {
        ObjectFilter data = getFacetSamples(docCount);
        ObjectFilter indexer = getIndexer();
        indexer.setSource(data);
        for (int i = 0 ; i < docCount ; i++) {
            assertTrue("Check " + i + "/" + docCount + ". There should be a next for the indexer",
                       indexer.hasNext());
            indexer.next();
        }
        assertFalse("After " + docCount + " nexts, there should be no more Payloads", indexer.hasNext());
        indexer.close(true);
        log.debug("Finished basic ingest");
    }

    final static int SAMPLES = 2;
    private ObjectFilter getDataProvider(boolean deleted) throws IOException {
        List<Payload> samples = new ArrayList<>(SAMPLES);
        for (int i = 1 ; i <= SAMPLES ; i++) {
            Payload payload = new Payload(new Record(
                    "doc" + i, "dummy", Resolver.getUTF8Content(
                    "integration/solr/SolrSampleDocument" + i + ".xml").getBytes("utf-8")));
            payload.getRecord().setDeleted(deleted);
            samples.add(payload);
        }
        return new PayloadFeederHelper(samples);
    }

    private ObjectFilter getFacetSamples(int docCount) throws IOException {
        Random random = new Random(87);
        List<Payload> samples = new ArrayList<>(docCount);
        StringBuilder sb = new StringBuilder(1000);
        for (int i = 0 ; i < docCount ; i++) {
            sb.setLength(0);
            sb.append("<doc>\n");
            sb.append("<field name=\"recordID\">doc").append(String.format("%07d", i)).append("</field>\n");
            sb.append("<field name=\"recordBase\">dummy</field>\n");
            sb.append("<field name=\"sort_year_asc\">").
                    append(Integer.toString(random.nextInt(2014))).append("</field>\n");
            sb.append("<field name=\"title_org\">Document_").append(String.format("%07d", i)).append("</field>\n");
            //            sb.append("<field name=\"sort_title\">Document_").append(String.format("%07d", i)).append("</field>\n");
//            sb.append("<field name=\"lma\">sort_").append(String.format("%07d", i)).append("</field>\n");
            sb.append("<field name=\"fulltext\">Some very simple Solr sample document.</field>\n");
            sb.append("<field name=\"lma_long\">03_all</field>\n");
            if ((i & 0x01) == 0) {
                sb.append("<field name=\"lma_long\">02_half</field>\n");
            }
            if ((i & 0x03) == 0) {
                sb.append("<field name=\"lma_long\">01_quart</field>\n");
            }
            sb.append("</doc>\n");
            samples.add(new Payload(new Record("doc" + i, "dummy", sb.toString().getBytes("utf-8"))));
        }
        return new PayloadFeederHelper(samples);
    }

    private IndexController getIndexer() throws IOException {
        Configuration controllerConf = Configuration.newMemoryBased(
                IndexController.CONF_FILTER_NAME, "testcontroller");
        controllerConf.set(IndexControllerImpl.CONF_COMMIT_MAX_DOCUMENTS, -1);
        Configuration manipulatorConf = controllerConf.createSubConfigurations(
                IndexControllerImpl.CONF_MANIPULATORS, 1).get(0);
        manipulatorConf.set(IndexControllerImpl.CONF_MANIPULATOR_CLASS, SolrManipulator.class.getCanonicalName());
        manipulatorConf.set(SolrManipulator.CONF_ID_FIELD, IndexUtils.RECORD_FIELD); // 'id' is the default ID field for Solr
        return new IndexControllerImpl(controllerConf);
    }

    private SolrSearchNode getSearcher() throws RemoteException {
        return new SolrSearchNode(Configuration.newMemoryBased(
                SolrSearchNode.CONF_ID, "foosearch"
        ));
    }

    protected long getHits(SearchNode searcher, String... arguments) throws RemoteException {
        String HITS_PATTERN = "(?s).*hitCount=\"([0-9]*)\".*";
        ResponseCollection responses = new ResponseCollection();
        searcher.search(new Request(arguments), responses);
        if (!Pattern.compile(HITS_PATTERN).matcher(responses.toXML()).matches()) {
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
