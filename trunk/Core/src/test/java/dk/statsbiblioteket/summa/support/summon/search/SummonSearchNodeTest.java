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
package dk.statsbiblioteket.summa.support.summon.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.unittest.ExtraAsserts;
import dk.statsbiblioteket.summa.common.util.SimplePair;
import dk.statsbiblioteket.summa.common.util.StringExtraction;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultExternal;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultImpl;
import dk.statsbiblioteket.summa.search.PagingSearchNode;
import dk.statsbiblioteket.summa.search.SearchNode;
import dk.statsbiblioteket.summa.search.SearchNodeFactory;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.search.tools.QueryRewriter;
import dk.statsbiblioteket.summa.support.api.LuceneKeys;
import dk.statsbiblioteket.summa.support.harmonise.AdjustingSearchNode;
import dk.statsbiblioteket.summa.support.harmonise.HarmoniseTestHelper;
import dk.statsbiblioteket.summa.support.harmonise.InteractionAdjuster;
import dk.statsbiblioteket.summa.support.solr.SolrSearchNode;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.DOM;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummonSearchNodeTest extends TestCase {
    private static Log log = LogFactory.getLog(SummonSearchNodeTest.class);

    public SummonSearchNodeTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(SummonSearchNodeTest.class);
    }

    public void testMoreLikeThis() throws RemoteException {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        long standard = getHits(summon, DocumentKeys.SEARCH_QUERY, "foo");
        assertTrue("A search for 'foo' should give hits", standard > 0);

        long mlt = getHits(summon, DocumentKeys.SEARCH_QUERY, "foo", LuceneKeys.SEARCH_MORELIKETHIS_RECORDID, "bar");
        assertEquals("A search with a MoreLikeThis ID should not give hits", 0, mlt);
    }

    public void testPageFault() throws RemoteException {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        try {
            summon.search(new Request(
                    DocumentKeys.SEARCH_QUERY, "book", DocumentKeys.SEARCH_START_INDEX, 10000), responses);
        } catch (RemoteException e) {
            log.debug("Received RemoteException as expected");
        }
        fail("Search with large page number was expected to fail. Received response:\n" + responses.toXML());
    }

    public void testRecordBaseQueryRewrite() {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        assertNull("Queries for recordBase:summon should be reduced to null (~match all)",
                   summon.convertQuery("recordBase:summon", null));
        assertEquals("recordBase:summon should be removed with implicit AND",
                     "\"foo\"", summon.convertQuery("recordBase:summon foo", null));
        assertEquals("recordBase:summon should be removed with explicit AND",
                     "\"foo\"", summon.convertQuery("recordBase:summon AND foo", null));
        assertEquals("OR with recordBase:summon and another recordBase should match",
                     null, summon.convertQuery("recordBase:summon OR recordBase:blah", null));
        assertEquals("OR with recordBase:summon should leave the rest of the query",
                     "\"foo\"", summon.convertQuery("recordBase:summon OR foo", null));
        assertEquals("recordBase:summon AND recordBase:nonexisting should not match anything",
                     SummonSearchNode.DEFAULT_NONMATCHING_QUERY.replace(":", ":\"")
                     + "\"", summon.convertQuery("recordBase:summon AND recordBase:nonexisting", null));
    }

    /*
    Trying to discover why some phrase searches return more results than non-phrase searches for summon.
    No luck so fas as it seems that neither the ?-wildcard, nor escaping of space works.
     */
/*    public void testQuoting() throws IOException, TransformerException {
        String[] TESTS = new String[] {
                "dogs myasthenia gravis",
                "dogs myasthenia\\ gravis*",
                "dogs myasthenia*gravis",
                "dogs myasthenia?gravis",
                "dogs \"myasthenia gravis\"",
                "dogs myasthenia\\ gravis"
        };

        log.debug("Creating SummonSearchNode");
        String s = "";
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        for (String query: TESTS) {
            long hits = getHits(summon,
                    DocumentKeys.SEARCH_QUERY, query,
                    SolrSearchNode.SEARCH_PASSTHROUGH_QUERY, "true",
                    DocumentKeys.SEARCH_COLLECT_DOCIDS, "false");
            s += "'" + query + "' gave " + hits + " hits\n";
        }
        summon.close();

        System.out.print(s);
    }
  */


    public void testIDResponse() throws IOException, TransformerException {
        String QUERY = "gene and protein evolution";

        log.debug("Creating SummonSearchNode");
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        Request req = new Request(
                DocumentKeys.SEARCH_QUERY, QUERY,
                DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        List<String> ids = getAttributes(summon, req, "id", false);
        assertTrue("There should be at least 1 result", ids.size() >= 1);

        final Pattern EMBEDDED_ID_PATTERN = Pattern.compile("<field name=\"recordID\">(.+?)</field>", Pattern.DOTALL);
        List<String> embeddedIDs = getPattern(summon, req, EMBEDDED_ID_PATTERN, false);
        ExtraAsserts.assertEquals("The embedded IDs should match the plain IDs", ids, embeddedIDs);
        System.out.println("Received IDs: " + Strings.join(ids, ", "));

        summon.close();
    }

    public void testNegativeFacet() throws RemoteException {
        final String JSON =
                "{\"search.document.query\":\"darkmans barker\"," +
                "\"search.document.collectdocids\":\"true\","
                + "\"solr.filterisfacet\":\"true\"," +
                "\"solrparam.s.ho\":\"true\","
                + "\"search.document.filter\":\" NOT ContentType:\\\"Newspaper Article\\\"\","
                + "\"search.document.filter.purenegative\":\"true\"}";
        SearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.addJSON(JSON);
        summon.search(request, responses);
        assertFalse("The response should not have Newspaper Article as facet. total response was\n" + responses.toXML(),
                    responses.toXML().contains("<tag name=\"Newspaper Article\""));
        summon.close();
    }

    public void testPagedFacet() throws RemoteException {
        final String JSON =
                "{\"search.document.query\":\"peter\"," +
                "\"search.document.startindex\":0," +
                "\"search.document.maxrecords\":60," +
                "\"search.document.collectdocids\":true," +
                "\"solr.filterisfacet\":\"true\"," +
                "\"search.document.filter\":\"SubjectTerms:\\\"athletes\\\"\"}";

        SearchNode summon = SummonTestHelper.createSummonSearchNode();
        PagingSearchNode pager = new PagingSearchNode(Configuration.newMemoryBased(
                PagingSearchNode.CONF_SEQUENTIAL, true,
                PagingSearchNode.CONF_GUIPAGESIZE, 20,
                PagingSearchNode.CONF_MAXPAGESIZE, 50
        ), summon);
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.addJSON(JSON);
        pager.search(request, responses);
        assertTrue("The response should contain some facets\n" + responses.toXML(),
                    responses.toXML().contains("facet name"));
        summon.close();
    }

    public void testSpacedEscapedFacetFilter() throws RemoteException {
        final String JSON =
                "{\"search.document.query\":\"nature\", " +
                "\"search.document.filter\":" +
                "\"ContentType:Magazine\\\\ Article OR ContentType:Journal\\\\ Article\", "  +
                "\"solr.filterisfacet\":\"true\"}";
        SearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.addJSON(JSON);
        log.info("Searching with query='" + request.getString(DocumentKeys.SEARCH_QUERY)
                 + "', filter='" + request.getString(DocumentKeys.SEARCH_FILTER) + "'");
        summon.search(request, responses);
        final long numHits = ((DocumentResponse)responses.iterator().next()).getHitCount();
        assertTrue("The number of hits should exceed 1 but was " + numHits, numHits > 1);
        summon.close();
    }

    public void testSpacedEscapedFacetQuery() throws RemoteException {
        final String JSON =
                "{\"search.document.query\":" +
                "\"nature AND (ContentType:Magazine\\\\ Article OR ContentType:Journal\\\\ Article)\", " +
                "\"solr.filterisfacet\":\"true\"}";
        SearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.addJSON(JSON);
        log.info("Searching with query='" + request.getString(DocumentKeys.SEARCH_QUERY) + "'");
        summon.search(request, responses);
        final long numHits = ((DocumentResponse)responses.iterator().next()).getHitCount();
        assertTrue("The number of hits should exceed 1 but was " + numHits, numHits > 1);
        summon.close();
    }

    public void testSpacedQuotedFacetFilter() throws RemoteException {
        final String JSON =
                "{\"search.document.query\":\"nature\", " +
                "\"search.document.filter\":" +
                "\"ContentType:\\\"Magazine Article\\\" OR ContentType:\\\"Journal Article\\\"\", " +
                "\"solr.filterisfacet\":\"true\"}";
        SearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.addJSON(JSON);
        log.info("Searching with query='" + request.getString(DocumentKeys.SEARCH_QUERY)
                 + "', filter='" + request.getString(DocumentKeys.SEARCH_FILTER) + "'");
        summon.search(request, responses);
        final long numHits = ((DocumentResponse)responses.iterator().next()).getHitCount();
        assertTrue("The number of hits should exceed 1 but was " + numHits, numHits > 1);
        summon.close();
    }

    public void testFacetSizeSmall() throws RemoteException {
        assertFacetSize(3);
        assertFacetSize(25);
    }

    private void assertFacetSize(int tagCount) throws RemoteException {
        final String JSON = "{\"search.document.query\":\"thinking\",\"search.document.collectdocids\":\"true\"}";
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(SummonSearchNode.CONF_SOLR_FACETS, "ContentType (" + tagCount + " ALPHA)");
        SearchNode summon = new SummonSearchNode(conf);

        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.addJSON(JSON);
        summon.search(request, responses);
        List<String> tags = StringExtraction.getStrings(responses.toXML(), "<tag.+?>");
        assertEquals("The number of returned tags should be " + tagCount + "+1. The returned Tags were\n"
                     + Strings.join(tags, "\n"), tagCount + 1, tags.size());
        summon.close();
    }

    public void testFacetSizeQuery() throws RemoteException {
        int tagCount = 3;
        final String JSON = "{\"search.document.query\":\"thinking\","
                            + "\"search.document.collectdocids\":\"true\","
                            + "\"search.facet.facets\":\"ContentType (" + tagCount + " ALPHA)\"}";
        SearchNode summon = SummonTestHelper.createSummonSearchNode();

        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.addJSON(JSON);
        summon.search(request, responses);
        List<String> tags = StringExtraction.getStrings(responses.toXML(), "<tag.+?>");
        assertEquals("The number of returned tags should be " + tagCount + "+1. The returned Tags were\n"
                     + Strings.join(tags, "\n"), tagCount + 1, tags.size());
        summon.close();
    }

    public void testRecordBaseFacet() throws RemoteException {
        final String JSON =
                "{\"search.document.query\":\"darkmans barker\",\"search.document.collectdocids\":\"true\","
                + "\"solr.filterisfacet\":\"true\",\"solrparam.s.ho\":\"true\","
                + "\"search.document.filter\":\" recordBase:summon\"}";
        SearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.addJSON(JSON);
        summon.search(request, responses);
        assertTrue("The response should contain a summon tag from faceting\n"
                   + responses.toXML(), responses.toXML().contains("<tag name=\"summon\" addedobjects=\""));
        summon.close();
    }

    public void testRecordBaseFacetWithOR() throws RemoteException {
        final String JSON =
                "{\"search.document.query\":\"darkmans barker\",\"search.document.collectdocids\":\"true\","
                + "\"solr.filterisfacet\":\"true\",\"solrparam.s.ho\":\"true\","
                + "\"search.document.filter\":\" recordBase:summon OR recordBase:sb_aleph\"}";
        SearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.addJSON(JSON);
        summon.search(request, responses);
        assertTrue("The response should contain a summon tag from faceting\n"
                   + responses.toXML(), responses.toXML().contains("<tag name=\"summon\" addedobjects=\""));
        summon.close();
    }

    public void testBasicSearch() throws RemoteException {
        log.debug("Creating SummonSearchNode");
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
//        summon.open(""); // Fake open for setting permits
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        summon.search(request, responses);
        log.debug("Finished searching");
        //System.out.println(responses.toXML());
        assertTrue("The result should contain at least one record", responses.toXML().contains("<record score"));
        assertTrue("The result should contain at least one tag", responses.toXML().contains("<tag name"));
        System.out.println(responses.getTransient().get(SummonResponseBuilder.SUMMON_RESPONSE));
    }

    public void testXMLTree() throws RemoteException {
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(SummonResponseBuilder.CONF_XML_FIELD_HANDLING, SummonResponseBuilder.XML_MODE.mixed);
        SummonSearchNode summon = new SummonSearchNode(conf);

        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);

        final Pattern XML_FIELDS = Pattern.compile("(<field name=\"[^\"]*_xml\">.+?</field>)", Pattern.DOTALL);
        List<String> xmlFields = getPattern(summon, request, XML_FIELDS, false);
        log.info("Got XML-fields\n" + Strings.join(xmlFields, "\n"));
    }

    public void testMultiID() throws RemoteException {
        List<String> IDs = Arrays.asList(
                "FETCH-proquest_dll_11531932811",
                "FETCH-proquest_dll_6357072911",
                "FETCH-proquest_dll_15622214411"
        );
        SummonSearchNode searcher = SummonTestHelper.createSummonSearchNode(true);

        for (String id: IDs) {
            assertEquals("The number of hits for ID '" + id + "' should match", 1, getAttributes(searcher, new Request(
                    DocumentKeys.SEARCH_QUERY, "ID:\"" + id + "\"",
                    SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, true
            ), "id", false).size());
        }

        String IDS_QUERY = "(ID:\"" + Strings.join(IDs, "\" OR ID:\"") + "\")";

        Request req = new Request(
                DocumentKeys.SEARCH_QUERY, IDS_QUERY,
                SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, true
        );
        List<String> returnedIDs = getAttributes(searcher, req, "id", false);
        if (IDs.size() != returnedIDs.size()) {
            ResponseCollection responses = new ResponseCollection();
            searcher.search(req, responses);
            System.out.println("Returned IDs: " + Strings.join(returnedIDs, ", "));
//            System.out.println(responses.toXML());
        }

        assertEquals(
                "Pre 2013-12-13 Serial Solutions only returned 1 hit for multiple ID searches. That has now changed " +
                "for query '" + IDS_QUERY + "'", 1, returnedIDs.size());
    }

    public void testShortFormat() throws RemoteException {
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(SummonResponseBuilder.CONF_SHORT_DATE, true);

        log.debug("Creating SummonSearchNode");
        SummonSearchNode summon = new SummonSearchNode(conf);
//        summon.open(""); // Fake open for setting permits
        final Pattern DATEPATTERN = Pattern.compile("<dc:date>(.+?)</dc:date>", Pattern.DOTALL);
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo");
        request.put(DocumentKeys.SEARCH_RESULT_FIELDS, "shortformat");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        List<String> dates= getPattern(summon, request, DATEPATTERN, false);
        assertTrue("There should be at least 1 extracted date", !dates.isEmpty());
        for (String date: dates) {
            assertTrue("the returned dates should be of length 4 or less, got '" + date + "'", date.length() <= 4);
        }
//        System.out.println("Got dates:\n" + Strings.join(dates, ", "));
    }

    public void testIDSearch() throws IOException, TransformerException {
        List<String> sampleIDs = getSampleIDs();
        assertFalse("There should be at least 1 sample ID", sampleIDs.isEmpty());
        String ID = sampleIDs.get(0);
//        System.out.println("Got IDs " + Strings.join(sampleIDs, ", "));
//        String ID = "summon_FETCH-gale_primary_2105957371";

        /*
        From Summon-provided test site
        From Summon-provided test site
        queryString="
        s.rec.qs.max=
        &amp;s.mr=
        &amp;s.ho=t
        &amp;s.rec.db.max=
        &amp;s.ps=10
        &amp;s.q=ID%3A%22FETCH-LOGICAL-a8990-b5a26f60b0093e6474a5a91213bf9fccef1af6e41cbc4c3456d008ae2e43f7e61%22
        &amp;s.pn=1
         */

        String query = "recordID:\"" + ID + "\"";
        //String query = "ID:\"" + ID + "\"";
        log.info("Creating SummonSearchNode and performing search for " + query);
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        Request req = new Request(
                DocumentKeys.SEARCH_QUERY, query,
                DocumentKeys.SEARCH_MAX_RECORDS, 10,
//                SolrSearchNode.CONF_SOLR_PARAM_PREFIX + "ps", "10",
                DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        List<String> ids = getAttributes(summon, req, "id", false);
        assertTrue("There should be at least 1 result", ids.size() >= 1);
    }

    private List<String> getSampleIDs() throws IOException, TransformerException {
        String QUERY = "gene and protein evolution";

        log.debug("Creating SummonSearchNode");
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        try {
            Request req = new Request(
                    DocumentKeys.SEARCH_QUERY, QUERY,
                    DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
            List<String> ids = getAttributes(summon, req, "id", false);
            assertTrue("getSampleIDs(): There should be at least 1 result", ids.size() >= 1);

            final Pattern EMBEDDED_ID_PATTERN =
                    Pattern.compile("<field name=\"recordID\">(.+?)</field>", Pattern.DOTALL);
            List<String> embeddedIDs = getPattern(summon, req, EMBEDDED_ID_PATTERN, false);
            ExtraAsserts.assertEquals("getSampleIDs(): The embedded IDs should match the plain IDs", ids, embeddedIDs);
            return embeddedIDs;
        } finally {
            summon.close();
        }
    }

    public void testTruncation() throws IOException, TransformerException {
        String PLAIN = "Author:andersen";
        String ESCAPED = "Author:andersen\\ christian";
        String TRUNCATED = "Author:andersen*";
        String ESCAPED_TRUNCATED = "Author:andersen\\ c*";
        String ESCAPED_TRUNCATED2 = "lfo:andersen\\ h\\ c*";

        List<String> QUERIES = Arrays.asList(PLAIN, ESCAPED, TRUNCATED, ESCAPED_TRUNCATED, ESCAPED_TRUNCATED2);

        log.debug("Creating SummonSearchNode");
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(SolrSearchNode.CONF_SOLR_READ_TIMEOUT, 20*1000);
        SearchNode summon = new SummonSearchNode(conf);
        for (String query: QUERIES) {
            Request req = new Request(
                    DocumentKeys.SEARCH_QUERY, query,
                    DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
            long searchTime = -System.currentTimeMillis();
            List<String> ids = getAttributes(summon, req, "id", false);
            searchTime += System.currentTimeMillis();
            assertFalse("There should be at least 1 result for " + query, ids.isEmpty());
            log.info("Got " + ids.size() + " from query '" + query + "' in " + searchTime + " ms");
        }
    }

    /*
      * At the end of june and the start of july 2013, 5-15% of the summon-requests in production exceeded the
      * connection timeout of 2000 ms. This unit test was made to verify this and is left (although disabled)
      * if the problem should arise again.
     */
    public void disabledtestTimeout() throws Exception {
        final int CONNECT_TIMEOUT = 20000;
        final int READ_TIMEOUT = 20000;
        final int RUNS = 20;
        final int DELAY_MS = 5000;
        final int VARIANCE_MS = 2234;

/*        List<String> QUERIES = new ArrayList<String>();
        BufferedReader in = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(
                "/home/te/tmp/sumfresh/common/performancetest/testQueries.txt")), "utf-8"));
        String line;
        while ((line = in.readLine()) != null) {
            if (!line.isEmpty()) {
                QUERIES.add(line);
            }
        }
        log.info("Loaded " + QUERIES.size() + " queries");
  */
        List<String> QUERIES = Arrays.asList("foo", "heat", "heat beads", "heat pans", "dolphin calls",
                                             "fresh water supply", "fresh water", "fresh water irrigation");

        log.debug("Creating SummonSearchNode");
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(SolrSearchNode.CONF_SOLR_CONNECTION_TIMEOUT, CONNECT_TIMEOUT);
        conf.set(SolrSearchNode.CONF_SOLR_READ_TIMEOUT, READ_TIMEOUT);
        SummonSearchNode summon = new SummonSearchNode(conf);

        long maxConnectTime = -1;
        int success = 0;
        Random random = new Random();
        for (int run = 0 ; run < RUNS ; run++) {
            String query = QUERIES.get(random.nextInt(QUERIES.size()));
            Request req = new Request(
                    DocumentKeys.SEARCH_QUERY, query,
                    DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
            long searchTime = -System.currentTimeMillis();
            try {
                List<String> ids = getAttributes(summon, req, "id", false);
                searchTime += System.currentTimeMillis();
                log.info(String.format("Test %d/%d. Got %d hits with connect time %dms for '%s' in %dms",
                                       run+1, RUNS, ids.size(), summon.getLastConnectTime(), query, searchTime));
                success++;
            } catch (Exception e) {
                searchTime += System.currentTimeMillis();
                if (e instanceof IllegalArgumentException) {
                    log.warn(String.format("Test %d/%d. Unable to get a result from '%s' in %d ms with connect " +
                                           "time %d due to illegal argument (probably a faulty query)",
                                           run+1, RUNS, query, searchTime, summon.getLastConnectTime()));
                } else if (e.getMessage().contains("java.net.SocketTimeoutException: connect timed out")) {
                    log.warn(String.format("Test %d/%d. Unable to get a result from '%s' in %d ms with connect " +
                                           "time %d due to connect timeout",
                                           run+1, RUNS, query, searchTime, summon.getLastConnectTime()));
                } else {
                    log.error(String.format("Test %d/%d. Unable to get a result from '%s' in %d ms with connect " +
                                            "time %d due to unexpected exception",
                                            run+1, RUNS, query, searchTime, summon.getLastConnectTime()), e);
                }
            }

            maxConnectTime = Math.max(maxConnectTime, summon.getLastConnectTime());
            if (run != RUNS-1) {
                int delay = DELAY_MS - VARIANCE_MS/2 + random.nextInt(VARIANCE_MS);
                synchronized (this) {
                    this.wait(delay);
                }
            }
        }
        log.info(String.format("Successfully performed %d/%d queries with max connect time %dms",
                               success, RUNS, maxConnectTime));
        summon.close();
    }

    public void testGetField() throws IOException, TransformerException {
        String ID = "summon_FETCH-gale_primary_2105957371";

        log.debug("Creating SummonSearchNode");
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        String fieldName = "shortformat";
        String field = getField(summon, ID, fieldName);
        assertTrue("The field '" + fieldName + "' from ID '" + ID + "' should have content",
                   field != null && !"".equals(field));
//        System.out.println("'" + field + "'");
    }

    /* This is equivalent to SearchWS#getField */
    private String getField(SearchNode searcher, String id, String fieldName) throws IOException, TransformerException {
        String retXML;

        Request req = new Request();
        req.put(DocumentKeys.SEARCH_QUERY, "ID:\"" + id + "\"");
        req.put(DocumentKeys.SEARCH_MAX_RECORDS, 1);
        req.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);

        ResponseCollection res = new ResponseCollection();
        searcher.search(req, res);
//        System.out.println(res.toXML());
        Document dom = DOM.stringToDOM(res.toXML());
        Node subDom = DOM.selectNode(
                dom, "/responsecollection/response/documentresult/record/field[@name='" + fieldName + "']");
        retXML = DOM.domToString(subDom);
        return retXML;
    }

    public void testNonExistingFacet() throws RemoteException {
        final Request request = new Request(
                "search.document.query", "foo",
                "search.document.filter", "Language:abcde32542f",
                "search.document.collectdocids", "true",
                "solr.filterisfacet", "true"
        );
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        log.debug("Searching");
        summon.search(request, responses);
        log.debug("Finished searching");
        for (Response response : responses) {
            if (response instanceof FacetResultExternal) {
                FacetResultExternal facets = (FacetResultExternal)response;
                for (Map.Entry<String, List<FacetResultImpl.Tag<String>>> entry: facets.getMap().entrySet()) {
                    assertEquals("The number of tags for facet '" + entry.getKey()
                                 + "' should be 0 as there should be no hits. First tag was '"
                                 + (entry.getValue().isEmpty() ? "N/A" : entry.getValue().get(0).getKey()) + "',",
                                 0, entry.getValue().size());
                }
            }
        }
    }

    // TODO: Test search for term with colon, quoted and unquoted
    public void testColonSearch() throws RemoteException {
        final String OK = "FETCH-proquest_dll_14482952011";
        final String PROBLEM = "FETCH-doaj_primary_oai:doaj-articles:932b6445ce452a2b2a544189863c472e1";
        performSearch("ID:\"" + OK + "\"");
        performSearch("ID:\"" + PROBLEM + "\"");
    }

    public void testColonNameSearch() throws RemoteException {
        performSearch("Gillis\\ P\\:son\\ Wetter");
    }

    public void testColonNameWeightedSearch() throws RemoteException {
        performSearch("P\\:son^1.2");
    }

    public void testColonFieldShortNameSearch() throws RemoteException {
        performSearch("AuthorCombined:(Gillis Wetter)");
    }

    public void testColonFieldLongNameSearch() throws RemoteException {
        performSearch("AuthorCombined:(Gillis P\\:son Wetter)");
    }

    public void testColonPhrasedNameSearch() throws RemoteException {
        performSearch("AuthorCombined:\"Gillis P:son Wetter\"");
    }

    private void performSearch(String query) throws RemoteException {
        log.debug("Creating SummonSearchNode");
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, query);
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        summon.search(request, responses);
        log.debug("Finished searching");
//        System.out.println(responses.toXML());
        assertTrue("The result should contain at least one record for query '" + query + "'",
                   responses.toXML().contains("<record score"));
    }

    public void testFacetOrder() throws RemoteException {
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        //SummonSearchNode.CONF_SOLR_FACETS, ""

        log.debug("Creating SummonSearchNode");
        SummonSearchNode summon = new SummonSearchNode(conf);
//        summon.open(""); // Fake open for setting permits
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        summon.search(request, responses);
        log.debug("Finished searching");
        List<String> facets = getFacetNames(responses);
        List<String> expected = new ArrayList<String>(Arrays.asList(SummonSearchNode.DEFAULT_SUMMON_FACETS.split(" ?, ?")));
        expected.add("recordBase"); // We always add this when we're doing faceting
        for (int i = expected.size()-1 ; i >= 0 ; i--) {
            if (!facets.contains(expected.get(i))) {
                expected.remove(i);
            }
        }
        assertEquals("The order of the facets should be correct",
                     Strings.join(expected, ", "), Strings.join(facets, ", "));

//        System.out.println(responses.toXML());
//        System.out.println(Strings.join(facets, ", "));
    }

    public void testSpecificFacets() throws RemoteException {
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        conf.set(SummonSearchNode.CONF_SOLR_FACETS, "SubjectTerms");

        log.debug("Creating SummonSearchNode");
        SummonSearchNode summon = new SummonSearchNode(conf);
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        summon.search(request, responses);
        log.debug("Finished searching");
        List<String> facets = getFacetNames(responses);
        assertEquals("The number of facets should be correct", 2, facets.size()); // 2 because of recordBase
        assertEquals("The returned facet should be correct",
                     "SubjectTerms, recordBase", Strings.join(facets, ", "));
    }

    public void testFacetSortingCount() throws RemoteException {
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        conf.set(SummonSearchNode.CONF_SOLR_FACETS, "SubjectTerms");
        assertFacetOrder(conf, false);
    }

    // Summon does not support index ordering of facets so we must cheat by over-requesting and post-processing
    public void testFacetSortingAlpha() throws RemoteException {
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        conf.set(SummonSearchNode.CONF_SOLR_FACETS, "SubjectTerms(ALPHA)");
        assertFacetOrder(conf, true);
    }

    private void assertFacetOrder(Configuration summonConf, boolean alpha) throws RemoteException {
        log.debug("Creating SummonSearchNode");
        SummonSearchNode summon = new SummonSearchNode(summonConf);
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        summon.search(request, responses);
        log.debug("Finished searching");
        List<String> tags = getTags(responses, "SubjectTerms");
        List<String> tagsAlpha = new ArrayList<String>(tags);
        Collections.sort(tagsAlpha);
        if (alpha) {
            ExtraAsserts.assertEquals(
                    "The order should be alphanumeric\nExp: "
                    + Strings.join(tagsAlpha, " ") + "\nAct: " + Strings.join(tags, " "),
                    tagsAlpha, tags);
        } else {
            boolean misMatch = false;
            for (int i = 0 ; i < tags.size() ; i++) {
                if (!tags.get(i).equals(tagsAlpha.get(i))) {
                    misMatch = true;
                    break;
                }
            }
            if (!misMatch) {
                fail("The order should not be alphanumeric but it was");
            }
        }
        log.debug("Received facets with alpha=" + alpha + ": " + Strings.join(tags, ", "));
    }

    private List<String> getTags(ResponseCollection responses, String facet) {
        List<String> result = new ArrayList<String>();
        for (Response response: responses) {
            if (response instanceof FacetResultExternal) {
                FacetResultExternal facetResult = (FacetResultExternal)response;
                List<FacetResultImpl.Tag<String>> tags = facetResult.getMap().get(facet);
                for (FacetResultImpl.Tag<String> tag: tags) {
                    result.add(tag.getKey() + "(" + tag.getCount() + ")");
                }
                return result;
            }
        }
        fail("Unable to locate a FacetResponse in the ResponseCollection");
        return null;
    }

    public void testNegativeFacets() throws RemoteException {
        final String QUERY = "foo fighters NOT limits NOT (boo OR bam)";
        final String FACET = "SubjectTerms:\"united states\"";
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        assertHits("There should be at least one hit for positive faceting",
                   summon,
                   DocumentKeys.SEARCH_QUERY, QUERY,
                   DocumentKeys.SEARCH_FILTER, FACET);
        assertHits("There should be at least one hit for parenthesized positive faceting", summon,
                   DocumentKeys.SEARCH_QUERY, QUERY,
                   DocumentKeys.SEARCH_FILTER, "(" + FACET + ")");
        assertHits("There should be at least one hit for filter with pure negative faceting", summon,
                   DocumentKeys.SEARCH_QUERY, QUERY,
                   DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE, "true",
                   DocumentKeys.SEARCH_FILTER, "NOT " + FACET);
        summon.close();
    }

    public void testFilterFacets() throws RemoteException {
        final String QUERY = "foo fighters";
        final String FACET = "SubjectTerms:\"united states\"";
        final String FACET_NEG = "-SubjectTerms:\"united states\"";
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        assertHits("There should be at least one hit for standard positive faceting",
                   summon,
                   DocumentKeys.SEARCH_QUERY, QUERY,
                   DocumentKeys.SEARCH_FILTER, FACET);
        assertHits("There should be at least one hit for facet filter positive faceting",
                   summon,
                   DocumentKeys.SEARCH_QUERY, QUERY,
                   DocumentKeys.SEARCH_FILTER, FACET,
                   SummonSearchNode.SEARCH_SOLR_FILTER_IS_FACET, "true");
        assertHits("There should be at least one hit for standard negative faceting",
                   summon,
                   DocumentKeys.SEARCH_QUERY, QUERY,
                   DocumentKeys.SEARCH_FILTER, FACET_NEG,
                   DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE, "true");
        assertHits("There should be at least one hit for facet filter negative faceting",
                   summon,
                   DocumentKeys.SEARCH_QUERY, QUERY,
                   DocumentKeys.SEARCH_FILTER, FACET_NEG,
                   SummonSearchNode.SEARCH_SOLR_FILTER_IS_FACET, "true");
        summon.close();
    }

    // summon used to support pure negative filters (in 2011) but apparently does not with the 2.0.0-API.
    // If they change their stance on the issue, we want to switch back to using pure negative filters, as it
    // does not affect ranking.
    public void testNegativeFacetsSupport() throws RemoteException {
        final String QUERY = "foo fighters NOT limits NOT (boo OR bam)";
        final String FACET = "SubjectTerms:\"united states\"";
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(SummonSearchNode.CONF_SUPPORTS_PURE_NEGATIVE_FILTERS, true);
        SummonSearchNode summon = new SummonSearchNode(conf);
        assertEquals("There should be zero hits for filter with assumed pure negative faceting support", 0,
                     getHits(summon, DocumentKeys.SEARCH_QUERY, QUERY, DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE,
                             "true", DocumentKeys.SEARCH_FILTER,
                             "NOT "
                             + FACET));
        summon.close();
    }

    public void testQueryWithNegativeFacets() throws RemoteException {
        final String QUERY = "foo";
        final String FACET = "SubjectTerms:\"analysis\"";
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        assertHits("There should be at least one hit for positive faceting", summon, DocumentKeys.SEARCH_QUERY,
                   QUERY, DocumentKeys.SEARCH_FILTER, FACET);
        assertHits("There should be at least one hit for query with negative facet", summon,
                   DocumentKeys.SEARCH_QUERY, QUERY, DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE, Boolean.TRUE.toString(), DocumentKeys.SEARCH_FILTER,
                   "NOT " + FACET);
        summon.close();
    }

    public void testSortedSearch() throws RemoteException {
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(InteractionAdjuster.CONF_ADJUST_DOCUMENT_FIELDS, "sort_year_asc - PublicationDate");

        log.debug("Creating SummonSearchNode");
        SummonSearchNode summon = new SummonSearchNode(conf);
//        summon.open(""); // Fake open for setting permits
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "sb");
        request.put(DocumentKeys.SEARCH_SORTKEY, "PublicationDate");
//        request.put(DocumentKeys.SEARCH_REVERSE, true);
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        summon.search(request, responses);
        log.debug("Finished searching");
        List<String> sortValues = getAttributes(summon, request, "sortValue", true);
        String lastValue = null;
        for (String sortValue: sortValues) {
            assertTrue("The sort values should be in unicode order but was " + Strings.join(sortValues, ", "),
                       lastValue == null || lastValue.compareTo(sortValue) <= 0);
//            System.out.println(lastValue + " vs " + sortValue + ": " + (lastValue == null ? 0 : lastValue.compareTo(sortValue)));
            lastValue = sortValue;
        }
        log.debug("Test passed with sort values\n" + Strings.join(sortValues, "\n"));
    }

    public void testSortedDate() throws RemoteException {
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(InteractionAdjuster.CONF_ADJUST_DOCUMENT_FIELDS, "sort_year_asc - PublicationDate");

        log.debug("Creating SummonSearchNode");
        SummonSearchNode summon = new SummonSearchNode(conf);
//        summon.open(""); // Fake open for setting permits
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "dolphin whale");
        request.put(DocumentKeys.SEARCH_SORTKEY, "PublicationDate");
//        request.put(DocumentKeys.SEARCH_REVERSE, true);
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        List<String> fields = getField(summon, request, "PublicationDate_xml_iso");
        log.debug("Finished searching");
        String lastValue = null;
        for (String sortValue: fields) {
            assertTrue("The field values should be in unicode order but was " + Strings.join(fields, ", "),
                       lastValue == null || lastValue.compareTo(sortValue) <= 0);
//            System.out.println(lastValue + " vs " + sortValue + ": " + (lastValue == null ? 0 : lastValue.compareTo(sortValue)));
            lastValue = sortValue;
        }
        log.debug("Test passed with field values\n" + Strings.join(fields, "\n"));
    }

    public void testFacetedSearchNoFaceting() throws Exception {
        assertSomeHits(new Request(
                DocumentKeys.SEARCH_QUERY, "first"
        ));
    }

    public void testFacetedSearchNoHits() throws Exception {
        Request request = new Request(
                DocumentKeys.SEARCH_FILTER, "recordBase:nothere",
                DocumentKeys.SEARCH_QUERY, "first"
        );
        ResponseCollection responses = search(request);
        assertTrue("There should be a response", responses.iterator().hasNext());
        assertEquals("There should be no hits. Response was\n"
                     + responses.toXML(), 0, ((DocumentResponse) responses.iterator().next()).getHitCount());
    }

    public void testFacetedSearchSomeHits() throws Exception {
        assertSomeHits(new Request(DocumentKeys.SEARCH_FILTER, "recordBase:summon", DocumentKeys.SEARCH_QUERY, "first"));
    }

    public void testDashSomeHits() throws Exception {
        assertSomeHits(new Request(DocumentKeys.SEARCH_QUERY, "merrian - webster"));
    }

    public void testDashWeightSomeHits() throws Exception {
        assertSomeHits(new Request(DocumentKeys.SEARCH_QUERY, "merrian \\-^12.2 webster"));
    }

    public void testAmpersandWeightSomeHits() throws Exception {
        assertSomeHits(new Request(DocumentKeys.SEARCH_QUERY, "merrian &^12.2 webster"));
    }

    public void testDashWeightQuotedSomeHits() throws Exception {
        assertSomeHits(new Request(
                DocumentKeys.SEARCH_QUERY, "merrian \"-\"^12.2 webster"
        ));
    }

    private void assertSomeHits(Request request) throws RemoteException {
        ResponseCollection responses = search(request);
        assertTrue("There should be a response", responses.iterator().hasNext());
        assertTrue("There should be some hits. Response was\n" + responses.toXML(),
                   ((DocumentResponse) responses.iterator().next()).getHitCount() > 0);
    }

    private ResponseCollection search(Request request) throws RemoteException {
        SearchNode searcher = SummonTestHelper.createSummonSearchNode();
        try {
            ResponseCollection responses = new ResponseCollection();
            searcher.search(request, responses);
            return responses;
        } finally {
            searcher.close();
        }
    }

    public void testSortedSearchRelevance() throws RemoteException {
        Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
        conf.set(InteractionAdjuster.CONF_ADJUST_DOCUMENT_FIELDS, "sort_year_asc - PublicationDate");

        log.debug("Creating SummonSearchNode");
        SummonSearchNode summon = new SummonSearchNode(conf);
//        summon.open(""); // Fake open for setting permits
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo");
        request.put(DocumentKeys.SEARCH_SORTKEY, DocumentKeys.SORT_ON_SCORE);
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        summon.search(request, responses);
        log.debug("Finished searching");
        List<String> ids = getAttributes(summon, request, "id", false);
        assertTrue("There should be some hits", !ids.isEmpty());
    }

    public void testPaging() throws RemoteException {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
//        summon.open(""); // Fake open for setting permits
        List<String> ids0 = getAttributes(
                summon, new Request(
                DocumentKeys.SEARCH_QUERY, "foo",
                DocumentKeys.SEARCH_MAX_RECORDS, 20,
                DocumentKeys.SEARCH_START_INDEX, 0),
                "id", false);
        List<String> ids1 = getAttributes(
                summon, new Request(
                DocumentKeys.SEARCH_QUERY, "foo",
                DocumentKeys.SEARCH_MAX_RECORDS, 20,
                DocumentKeys.SEARCH_START_INDEX, 20),
                "id", false);
        List<String> ids2 = getAttributes(
                summon, new Request(
                DocumentKeys.SEARCH_QUERY, "foo",
                DocumentKeys.SEARCH_MAX_RECORDS, 20,
                DocumentKeys.SEARCH_START_INDEX, 40),
                "id", false);

        assertNotEquals("The hits should differ from page 0 and 1",
                        Strings.join(ids0, ", "), Strings.join(ids1, ", "));
        assertNotEquals("The hits should differ from page 1 and 2",
                        Strings.join(ids1, ", "), Strings.join(ids2, ", "));
    }

    public void testPingFromSummaSearcher() throws IOException {
        Configuration conf = Configuration.newMemoryBased();
        SimplePair<String, String> credentials = SummonTestHelper.getCredentials();
        conf.set(SearchNodeFactory.CONF_NODE_CLASS, SummonSearchNode.class);
        conf.set(SummonSearchNode.CONF_SUMMON_ACCESSID, credentials.getKey());
        conf.set(SummonSearchNode.CONF_SUMMON_ACCESSKEY, credentials.getValue());

        SummaSearcher searcher = new SummaSearcherImpl(conf);
        ResponseCollection responses = searcher.search(new Request());
        assertTrue("The response collection should be empty", responses.isEmpty());
        assertTrue("The timing should contain summon.pingtime", responses.getTiming().contains("summon.pingtime"));
    }

    private void assertNotEquals(String message, String expected, String actual) {
        assertFalse(message + ".\nExpected: " + expected + "\nActual:   " + actual,
                    expected.equals(actual));
    }

    private List<String> getAttributes(
            SearchNode searcher, Request request, String attributeName, boolean explicitMerge) throws RemoteException {
        final Pattern IDPATTERN = Pattern.compile("<record.*?" + attributeName + "=\"(.+?)\".*?>", Pattern.DOTALL);
        return getPattern(searcher, request, IDPATTERN, explicitMerge);
/*        ResponseCollection responses = new ResponseCollection();
        searcher.search(request, responses);
        responses.iterator().next().merge(responses.iterator().next());
        String[] lines = responses.toXML().split("\n");
        List<String> result = new ArrayList<String>();
        for (String line: lines) {
            Matcher matcher = IDPATTERN.matcher(line);
            if (matcher.matches()) {
                result.add(matcher.group(1));
            }
        }
        return result;*/
    }

    private List<String> getField(SearchNode searcher, Request request, String fieldName) throws RemoteException {
        final Pattern IDPATTERN = Pattern.compile(
                "<field name=\"" + fieldName + "\">(.+?)</field>", Pattern.DOTALL);
        return getPattern(searcher, request, IDPATTERN, false);
    }

    private List<String> getFacetNames(ResponseCollection responses) {
        Pattern FACET = Pattern.compile(".*<facet name=\"(.+?)\">");
        List<String> result = new ArrayList<String>();
        String[] lines = responses.toXML().split("\n");
        for (String line : lines) {
            Matcher matcher = FACET.matcher(line);
            if (matcher.matches()) {
                result.add(matcher.group(1));
            }
        }
        return result;
    }

    private List<String> getPattern(
            SearchNode searcher, Request request, Pattern pattern, boolean explicitMerge) throws RemoteException {
        ResponseCollection responses = new ResponseCollection();
        searcher.search(request, responses);
        if (explicitMerge) {
            responses.iterator().next().merge(responses.iterator().next());
        }
        String xml = responses.toXML();
        Matcher matcher = pattern.matcher(xml);
        List<String> result = new ArrayList<String>();
        while (matcher.find()) {
            result.add(Strings.join(matcher.group(1).split("\n"), ", "));
        }
        return result;
    }

    public void testRecommendations() throws RemoteException {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "PublicationTitle:jama");
//        request.put(DocumentKeys.SEARCH_QUERY, "+(PublicationTitle:jama OR PublicationTitle:jama) +(ContentType:Article OR ContentType:\"Book Chapter\" OR ContentType:\"Book Review\" OR ContentType:\"Journal Article\" OR ContentType:\"Magazine Article\" OR ContentType:Newsletter OR ContentType:\"Newspaper Article\" OR ContentType:\"Publication Article\" OR ContentType:\"Trade Publication Article\")");
      /*
 \+\(PublicationTitle:jama\ OR\ PublicationTitle:jama\)\ \+\(ContentType:Article\ OR\ ContentType:\"Book\ Chapter\"\ OR\ ContentType:\"Book\ Review\"\ OR\ ContentType:\"Journal\ Article\"\ OR\ ContentType:\"Magazine\ Article\"\ OR\ ContentType:Newsletter\ OR\ ContentType:\"Newspaper\ Article\"\ OR\ ContentType:\"Publication\ Article\"\ OR\ ContentType:\"Trade\ Publication\ Article\"\)
      */
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        summon.search(request, responses);
//        System.out.println(responses.toXML());
        assertTrue("The result should contain at least one recommendation",
                   responses.toXML().contains("<recommendation "));

/*        responses.clear();
        request.put(DocumentKeys.SEARCH_QUERY, "noobddd");
        summon.search(request, responses);
        System.out.println(responses.toXML());
  */
    }

    public void testReportedTiming() throws RemoteException {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo");
        summon.search(request, responses);
        Integer rawTime = getTiming(responses, "summon.rawcall");
        assertNotNull("There should be raw time", rawTime);
        Integer reportedTime = getTiming(responses, "summon.reportedtime");
        assertNotNull("There should be reported time", reportedTime);
        assertTrue("The reported time (" + reportedTime + ") should be lower than the raw time (" + rawTime + ")",
                   reportedTime <= rawTime);
        log.debug("Timing raw=" + rawTime + ", reported=" + reportedTime);
    }

    private Integer getTiming(ResponseCollection responses, String key) {
        String[] timings = responses.getTiming().split("[|]");
        for (String timing: timings) {
            String[] tokens = timing.split(":");
            if (tokens.length == 2 && key.equals(tokens[0])) {
                return Integer.parseInt(tokens[1]);
            }
        }
        return null;
    }

    public void testFilterVsQuery() throws RemoteException {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        long qHitCount = getHits(summon,
                                 DocumentKeys.SEARCH_QUERY, "PublicationTitle:jama",
                                 SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, "true");
        long fHitCount = getHits(summon,
                                 DocumentKeys.SEARCH_FILTER, "PublicationTitle:jama",
                                 SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, "true");

        assertTrue("The filter hit count " + fHitCount + " should differ from query hit count " + qHitCount
                   + " by less than 100",
                   Math.abs(fHitCount - qHitCount) < 100);
    }

    public void testFilterVsQuery2() throws RemoteException {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        long qHitCount = getHits(
                summon,
                DocumentKeys.SEARCH_QUERY, "PublicationTitle:jama",
                DocumentKeys.SEARCH_FILTER, "old");
        long fHitCount = getHits(
                summon,
                DocumentKeys.SEARCH_FILTER, "PublicationTitle:jama",
                DocumentKeys.SEARCH_QUERY, "old");

        assertTrue("The filter(old) hit count " + fHitCount + " should differ less than 100 from query(old) hit count "
                   + qHitCount + " as summon API 2.0.0 does field expansion on filters",
                   Math.abs(fHitCount - qHitCount) < 100);
        // This was only true in the pre-API 2.0.0. Apparently the new API does expands default fields for filters
//        assertTrue("The filter(old) hit count " + fHitCount + " should differ from query(old) hit count " + qHitCount
//                   + " by more than 100 as filter query apparently does not query parse with default fields",
//                   Math.abs(fHitCount - qHitCount) > 100);
    }

    public void testDismaxAnd() throws RemoteException {
        String QUERY1 = "public health policy";
        String QUERY2 = "alternative medicine";
        //String QUERY = "work and life balance";
        //String QUERY = "Small business and Ontario";
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        List<String> titlesLower = getField(summon, new Request(
                DocumentKeys.SEARCH_QUERY, QUERY1 + " and " + QUERY2,
                SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, true,
                SummonSearchNode.SEARCH_DISMAX_SABOTAGE, false
        ), "Title");
        String lower = Strings.join(titlesLower, "\n").replace("&lt;h&gt;", "").replace("&lt;/h&gt;", "");
        List<String> titlesUpper = getField(summon, new Request(
                DocumentKeys.SEARCH_QUERY, QUERY1 + " AND " + QUERY2,
                SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, true,
                SummonSearchNode.SEARCH_DISMAX_SABOTAGE, false
        ), "Title");
        String upper = Strings.join(titlesUpper, "\n").replace("&lt;h&gt;", "").replace("&lt;/h&gt;", "");

        summon.close();
        if (lower.equals(upper)) {
            fail("Using 'and' and 'AND' should not yield the same result\n" + lower);
        } else {
            System.out.println("Using 'and' and 'AND' gave different results:\nand: " +
                               lower.replace("\n", ", ") + "\nAND: " + upper.replace("\n", ", "));
        }
    }

    public void testDismaxWithQuoting() throws RemoteException {
        String QUERY = "public health policy and alternative medicine";
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();

        List<String> titlesRaw = getField(summon, new Request(
                DocumentKeys.SEARCH_QUERY, QUERY,
                SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, true,
                SummonSearchNode.SEARCH_DISMAX_SABOTAGE, false
        ), "Title");
        String raw = Strings.join(titlesRaw, "\n").replace("&lt;h&gt;", "").replace("&lt;/h&gt;", "");
        List<String> titlesQuoted = getField(summon, new Request(
                DocumentKeys.SEARCH_QUERY, QUERY,
                SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, false, // Adds quotes around individual terms
                SummonSearchNode.SEARCH_DISMAX_SABOTAGE, false
        ), "Title");
        String quoted = Strings.join(titlesQuoted, "\n").replace("&lt;h&gt;", "").replace("&lt;/h&gt;", "");
        List<String> titlesNonDismaxed = getField(summon, new Request(
                DocumentKeys.SEARCH_QUERY, "(" + QUERY + ")",
                SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, false, // Adds quotes around individual terms
                SummonSearchNode.SEARCH_DISMAX_SABOTAGE, false
        ), "Title");
        String nonDismaxed = Strings.join(titlesNonDismaxed, "\n").replace("&lt;h&gt;", "").replace("&lt;/h&gt;", "");

        summon.close();

        System.out.println("raw " + (raw.equals(quoted) ? "=" : "!") + "= quoted");
        System.out.println("raw " + (raw.equals(nonDismaxed) ? "=" : "!") + "= parenthesized");
        System.out.println("quoted " + (quoted.equals(nonDismaxed) ? "=" : "!") + "= parenthesized");
        System.out.println("raw =           " + raw.replace("\n", ", "));
        System.out.println("quoted =        " + quoted.replace("\n", ", "));
        System.out.println("parenthesized = " + nonDismaxed.replace("\n", ", "));

        assertEquals("The result from the raw (and thus dismaxed) query should match the result from "
                     + "the quoted terms query",
                     raw, quoted);
    }

    public void testFilterVsQuery3() throws RemoteException {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        long qCombinedHitCount = getHits(
                summon,
                DocumentKeys.SEARCH_QUERY,
                "PublicationTitle:jama AND Language:English");
        long qHitCount = getHits(
                summon,
                DocumentKeys.SEARCH_QUERY, "PublicationTitle:jama",
                DocumentKeys.SEARCH_FILTER, "(Language:English)");
        long fHitCount = getHits(
                summon,
                DocumentKeys.SEARCH_FILTER, "PublicationTitle:jama",
                DocumentKeys.SEARCH_QUERY, "Language:English");

        assertTrue("The filter(old) hit count " + fHitCount + " should differ"
                   + " from query(old) hit count " + qHitCount
                   + " by less than 100. Combined hit count for query is "
                   + qCombinedHitCount,
                   Math.abs(fHitCount - qHitCount) < 100);
    }

    public void testCustomParams() throws RemoteException {
        final String QUERY = "reactive arthritis yersinia lassen";

        Configuration confInside = SummonTestHelper.getDefaultSummonConfiguration();
        Configuration confOutside = SummonTestHelper.getDefaultSummonConfiguration();
        confOutside.set(SummonSearchNode.CONF_SOLR_PARAM_PREFIX + "s.ho",
                        new ArrayList<String>(Arrays.asList("false"))
        );

        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, QUERY);
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);

        SummonSearchNode summonInside = new SummonSearchNode(confInside);
        ResponseCollection responsesInside = new ResponseCollection();
        summonInside.search(request, responsesInside);

        SummonSearchNode summonOutside = new SummonSearchNode(confOutside);
        ResponseCollection responsesOutside = new ResponseCollection();
        summonOutside.search(request, responsesOutside);

        request.put(SummonSearchNode.CONF_SOLR_PARAM_PREFIX + "s.ho",
                    new ArrayList<String>(Arrays.asList("false")));
        ResponseCollection responsesSearchTweak = new ResponseCollection();
        summonInside.search(request, responsesSearchTweak);

        int countInside = countResults(responsesInside);
        int countOutside = countResults(responsesOutside);
        assertTrue("The number of results for a search for '" + QUERY + "' within holdings (" + confInside
                   + ") should be less that outside holdings (" + confOutside + ")",
                   countInside < countOutside);
        log.info(String.format("The search for '%s' gave %d hits within holdings and %d hits in total",
                               QUERY, countInside, countOutside));

        int countSearchTweak = countResults(responsesSearchTweak);
        assertEquals(
                "Query time specification of 's.ho=false' should give the same "
                + "result as configuration time specification of the same",
                countOutside, countSearchTweak);
    }

    public void testConvertRangeQueries() throws RemoteException {
        final String QUERY = "foo bar:[10 TO 20] OR baz:[87 TO goa]";
        Map<String, List<String>> params = new HashMap<String, List<String>>();
        String stripped = new SummonSearchNode(getSummonConfiguration()).convertQuery(QUERY, params);
        assertNotNull("RangeFilter should be defined", params.get("s.rf"));
        List<String> ranges = params.get("s.rf");
        assertEquals("The right number of ranges should be extracted", 2, ranges.size());
        assertEquals("Range #1 should be correct", "bar,10:20", ranges.get(0));
        assertEquals("Range #2 should be correct", "baz,87:goa", ranges.get(1));
        assertEquals("The resulting query should be stripped of ranges", "\"foo\"", stripped);
    }

    public void testConvertRangeQueriesEmpty() throws RemoteException {
        final String QUERY = "bar:[10 TO 20]";
        Map<String, List<String>> params = new HashMap<String, List<String>>();
        String stripped = new SummonSearchNode(getSummonConfiguration()).convertQuery(QUERY, params);
        assertNotNull("RangeFilter should be defined", params.get("s.rf"));
        List<String> ranges = params.get("s.rf");
        assertEquals("The right number of ranges should be extracted", 1, ranges.size());
        assertEquals("Range #1 should be correct", "bar,10:20", ranges.get(0));
        assertNull("The resulting query should be null", stripped);
    }

    private Configuration getSummonConfiguration() {
        return Configuration.newMemoryBased(
                SummonSearchNode.CONF_SUMMON_ACCESSID, "foo",
                SummonSearchNode.CONF_SUMMON_ACCESSKEY, "bar");
    }

    public void testFaultyQuoteRemoval() throws RemoteException {
        final String QUERY = "bar:\"foo:zoo\"";
        Map<String, List<String>> params = new HashMap<String, List<String>>();
        String stripped = new SummonSearchNode(getSummonConfiguration()).convertQuery(QUERY, params);
        assertNull("RangeFilter should not be defined", params.get("s.rf"));
        assertEquals("The resulting query should unchanged", QUERY, stripped);
    }

    // This fails, but as we are really testing Summon here, there is not much
    // we can do about it
    @SuppressWarnings({"UnusedDeclaration"})
    public void disabledtestCounts() throws RemoteException {
        //      final String QUERY = "reactive arthritis yersinia lassen";
        final String QUERY = "author:(Helweg Larsen) abuse";

        Request request = new Request();
        request.addJSON(
                "{search.document.query:\"" + QUERY + "\", summonparam.s.ps:\"15\", summonparam.s.ho:\"false\"}");
//        String r1 = request.toString(true);

        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        ResponseCollection responses = new ResponseCollection();
        summon.search(request, responses);
        int count15 = countResults(responses);

        request.clear();
        request.addJSON(
                "{search.document.query:\"" + QUERY + "\", summonparam.s.ps:\"30\", summonparam.s.ho:\"false\"}");
        //      String r2 = request.toString(true);
        responses.clear();
        summon.search(request, responses);
        int count20 = countResults(responses);
/*
        request.clear();
        request.put(DocumentKeys.SEARCH_QUERY, QUERY);
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        request.put(SummonSearchNode.CONF_SOLR_PARAM_PREFIX + "s.ho",
                    new ArrayList<String>(Arrays.asList("false")));
        request.put(DocumentKeys.SEARCH_MAX_RECORDS, 15);
        String rOld15 = request.toString(true);
        responses.clear();
        summon.search(request, responses);
        int countOld15 = countResults(responses);

        request.clear();
        request.put(DocumentKeys.SEARCH_QUERY, QUERY);
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        request.put(SummonSearchNode.CONF_SOLR_PARAM_PREFIX + "s.ho",
                    new ArrayList<String>(Arrays.asList("false")));
        request.put(DocumentKeys.SEARCH_MAX_RECORDS, 20);
        String rOld20 = request.toString(true);
        responses.clear();
        summon.search(request, responses);
        int countOld20 = countResults(responses);
  */
//        System.out.println("Request 15:  " + r1 + ": " + count15);
//        System.out.println("Request 20:  " + r2 + ": " + count20);
//        System.out.println("Request O15: " + rOld15 + ": " + countOld15);
//        System.out.println("Request O20: " + rOld20 + ": " + countOld20);
        assertEquals("The number of hits should not be affected by page size", count15, count20);
    }

    // Author can be returned in the field Author_xml (primary) and Author (secondary). If both fields are present,
    // Author should be ignored.
    public void testAuthorExtraction() throws IOException {

    }

    private int countResults(ResponseCollection responses) {
        for (Response response: responses) {
            if (response instanceof DocumentResponse) {
                return (int)((DocumentResponse)response).getHitCount();
            }
        }
        throw new IllegalArgumentException(
                "No documentResponse in ResponseCollection");
    }

    public void testAdjustingSearcher() throws IOException {
        SimplePair<String, String> credentials = SummonTestHelper.getCredentials();
        Configuration conf = Configuration.newMemoryBased(
                InteractionAdjuster.CONF_IDENTIFIER, "summon",
                InteractionAdjuster.CONF_ADJUST_DOCUMENT_FIELDS, "recordID - ID");
        Configuration inner = conf.createSubConfiguration(AdjustingSearchNode.CONF_INNER_SEARCHNODE);
        inner.set(SearchNodeFactory.CONF_NODE_CLASS, SummonSearchNode.class.getCanonicalName());
        inner.set(SummonSearchNode.CONF_SUMMON_ACCESSID, credentials.getKey());
        inner.set(SummonSearchNode.CONF_SUMMON_ACCESSKEY, credentials.getValue());

        log.debug("Creating adjusting SummonSearchNode");
        AdjustingSearchNode adjusting = new AdjustingSearchNode(conf);
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request(
                //request.put(DocumentKeys.SEARCH_QUERY, "foo");
                DocumentKeys.SEARCH_QUERY, "recursion in string theory",
                DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        adjusting.search(request, responses);
        log.debug("Finished searching");
        // TODO: Add proper test
//        System.out.println(responses.toXML());
    }

    public void testExplicitMust() throws IOException {
        final String QUERY = "miller genre as social action";
        ResponseCollection explicitMustResponses = new ResponseCollection();
        {
            Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
            conf.set(QueryRewriter.CONF_TERSE, false);
            SearchNode summon  = new SummonSearchNode(conf);
            Request request = new Request(
                    DocumentKeys.SEARCH_QUERY, QUERY
            );
            summon.search(request, explicitMustResponses);
            summon.close();
        }

        ResponseCollection implicitMustResponses = new ResponseCollection();
        {
            Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
            conf.set(QueryRewriter.CONF_TERSE, true);
            SearchNode summon  = new SummonSearchNode(conf);
            Request request = new Request(
                    DocumentKeys.SEARCH_QUERY, QUERY
            );
            summon.search(request, implicitMustResponses);
            summon.close();
        }
        HarmoniseTestHelper.compareHits(QUERY, false, explicitMustResponses, implicitMustResponses);
    }

    /*
    Tests if explicit weight-adjustment of terms influences the scores significantly.
     */
    public void testExplicitWeightScoring() throws RemoteException {
        assertScores("dolphin whales", "dolphin whales^1.000001", 10.0f);
    }

    /*
    Tests if explicit weight-adjustment of terms influences the order of documents.
     */
    public void testExplicitWeightOrder() throws RemoteException {
        assertOrder("dolphin whales", "dolphin whales^1.0");
    }

    public void testExplicitWeightOrderSingleTerm() throws RemoteException {
        assertOrder("whales", "whales^1.0");
    }

    public void testExplicitWeightOrderFoo() throws RemoteException {
        assertOrder("foo", "foo^1.0"); // By some funny coincidence, foo works when whales doesn't
    }

    private void assertOrder(String query1, String query2) throws RemoteException {
        SearchNode summon  = SummonTestHelper.createSummonSearchNode();
        try {
            List<String> ids1 = getAttributes(summon, new Request(
                    DocumentKeys.SEARCH_QUERY, query1,
                    SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, true
            ), "id", false);
            List<String> ids2 = getAttributes(summon, new Request(
                    DocumentKeys.SEARCH_QUERY, query2,
                    SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, true
            ), "id", false);
            ExtraAsserts.assertPermutations("Query '" + query1 + "' and '" + query2 + "'", ids1, ids2);
/*            assertEquals("The number of hits for '" + query1 + "' and '" + query2 + "' should be equal",
                         ids1.size(), ids2.size());
            assertEquals("The document order for '" + query1 + "' and '" + query2 + "' should be equal",
                         Strings.join(ids1, ", "), Strings.join(ids2, ", "));
                         */
        } finally {
            summon.close();
        }
    }

    public void testDisMaxDisabling() throws RemoteException {
        final String QUERY= "asian philosophy";
        SearchNode summon  = SummonTestHelper.createSummonSearchNode();
        try {
            long plainCount =
                    getHits(summon, DocumentKeys.SEARCH_QUERY, QUERY, SummonSearchNode.SEARCH_DISMAX_SABOTAGE, "false");
            long sabotagedCount =
                    getHits(summon, DocumentKeys.SEARCH_QUERY, QUERY, SummonSearchNode.SEARCH_DISMAX_SABOTAGE, "true");
            assertEquals("The number of hits for a DisMax-enabled and DisMax-sabotages query should match",
                         plainCount, sabotagedCount);

            List<String> plain = getAttributes(summon, new Request(
                    DocumentKeys.SEARCH_QUERY, QUERY, SummonSearchNode.SEARCH_DISMAX_SABOTAGE, false), "id", false);
            List<String> sabotaged = getAttributes(summon, new Request(
                    DocumentKeys.SEARCH_QUERY, QUERY, SummonSearchNode.SEARCH_DISMAX_SABOTAGE, true), "id", false);
            assertFalse("The ids returned by DisMax-enabled and DisMax-sabotaged query should differ",
                        Strings.join(plain, ", ").equals(Strings.join(sabotaged, ", ")));
        } finally {
            summon.close();
        }
    }

    public void testDismaxDisablingExperiment() throws RemoteException {
        assertOrder("foo bar", "(foo bar)");
    }


    /*
    Tests if quoting of terms influences the scores significantly.
     */
    public void testQuotingScoring() throws RemoteException {
        assertScores("dolphin whales", "\"dolphin\" \"whales\"", 10.0f);
    }

    private void assertScores(String query1, String query2, float maxDifference) throws RemoteException {
        SearchNode summon  = SummonTestHelper.createSummonSearchNode();

        ResponseCollection raw = new ResponseCollection();
        summon.search(new Request(
                DocumentKeys.SEARCH_QUERY, query1,
                SolrSearchNode.SEARCH_PASSTHROUGH_QUERY, true
        ), raw);

        ResponseCollection weighted = new ResponseCollection();
        summon.search(new Request(DocumentKeys.SEARCH_QUERY, query2), weighted);

        summon.close();

        List<Double> rawScores = getScores(raw);
        List<Double> weightedScores = getScores(weighted);

        assertEquals("The number of hits for '" + query1 + "' and '" + query2 + "' should be equal",
                     rawScores.size(), weightedScores.size());
        for (int i = 0 ; i < rawScores.size() ; i++) {
            assertTrue(String.format(
                    "The scores at position %d were %s and %s. Max difference allowed is %s. "
                    + "All scores for '%s' and '%s':\n%s\n%s",
                    i, rawScores.get(i), weightedScores.get(i), maxDifference,
                    query1, query2, Strings.join(rawScores, ", "), Strings.join(weightedScores, ", ")),
                       Math.abs(rawScores.get(i) - weightedScores.get(i)) <= maxDifference);
        }
    }

    private List<Double> getScores(ResponseCollection responses) {
        DocumentResponse docs = (DocumentResponse)responses.iterator().next();
        List<Double> result = new ArrayList<Double>(docs.size());
        for (DocumentResponse.Record record: docs.getRecords()) {
            result.add((double)record.getScore());
        }
        return result;
    }


    // Author_xml contains the authoritative order for the authors so it should override the non-XML-version
    public void testAuthor_xmlExtraction() throws RemoteException {
        String fieldName = "Author";
        String query = "PQID:821707502";

/*        {
            Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
            conf.set(SummonResponseBuilder.CONF_XML_OVERRIDES_NONXML, false);
            SearchNode summon = new SummonSearchNode(conf);
            ResponseCollection responses = new ResponseCollection();
            summon.search(new Request(DocumentKeys.SEARCH_QUERY, query), responses);

            DocumentResponse docs = (DocumentResponse)responses.iterator().next();
            for (DocumentResponse.Record record: docs.getRecords()) {
                System.out.println("\n" + record.getId());
                String author = "";
                String author_xml = "";
                for (DocumentResponse.Field field: record.getFields()) {
                    if ("Author".equals(field.getName())) {
                        author = field.getContent().replace("\n", ", ").replace("<h>", "").replace("</h>", "");
                        System.out.println("Author:     " + author);
                    } else if ("Author_xml".equals(field.getName())) {
                        author_xml = field.getContent().replace("\n", ", ");
                        System.out.println("Author_xml: " + author_xml);
                    } else if ("PQID".equals(field.getName())) {
                        System.out.println("PQID: " + field.getContent());
                    }
                }
                if (author.length() != author_xml.length()) {
                    fail("We finally found a difference between Author and Author_xml besides name ordering");
                }
            }
            summon.close();
        }
  */

        { // Old behaviour
            String expected = "Koetse, Willem\n"
                              + "Krebs, Christopher P\n"
                              + "Lindquist, Christine\n"
                              + "Lattimore, Pamela K\n"
                              + "Cowell, Alex J";
            Configuration conf = SummonTestHelper.getDefaultSummonConfiguration();
            conf.set(SummonResponseBuilder.CONF_XML_OVERRIDES_NONXML, false);
            SearchNode summonNonXML = new SummonSearchNode(conf);
            assertFieldContent("XML no override", summonNonXML, query, fieldName, expected, false);
            summonNonXML.close();
        }

        { // New behaviour
            String expected = "Lattimore, Pamela K\n"
                              + "Krebs, Christopher P\n"
                              + "Koetse, Willem\n"
                              + "Lindquist, Christine\n"
                              + "Cowell, Alex J";
            SearchNode summonXML = SummonTestHelper.createSummonSearchNode();
            assertFieldContent("XML override", summonXML, query, fieldName, expected, false);
            summonXML.close();
        }

    }

    public void testAuthor_xmlExtraction_shortformat() throws RemoteException {
        String query = "ID:FETCH-LOGICAL-c937-88b9adcf681a925445118c26ea8da2ed792f182b51857048dbb48b11a133ea321";
        { // sanity-check the Author-field
            String expected = "Ferlay, Jacques\n"
                              + "Shin, Hai-Rim\n"
                              + "Bray, Freddie\n"
                              + "Forman, David\n"
                              + "Mathers, Colin\n"
                              + "Parkin, Donald Maxwell";
            SearchNode summon = SummonTestHelper.createSummonSearchNode();
            assertFieldContent("author direct", summon, query, "Author", expected, false);
            summon.close();
        }

        { // shortformat should match Author
            String expected =
                    "  <shortrecord>\n"
                    + "    <rdf:RDF xmlns:dc=\"http://purl.org/dc/elements/1.1/\" "
                    + "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n"
                    + "      <rdf:Description>\n"
                    + "        <dc:title>Estimates of worldwide burden of cancer in 2008: GLOBOCAN 2008</dc:title>\n"
                    + "        <dc:creator>Ferlay, Jacques</dc:creator>\n"
                    + "        <dc:creator>Shin, Hai-Rim</dc:creator>\n"
                    + "        <dc:creator>Bray, Freddie</dc:creator>\n"
                    + "        <dc:creator>Forman, David</dc:creator>\n"
                    + "        <dc:creator>Mathers, Colin</dc:creator>\n"
                    + "        <dc:creator>Parkin, Donald Maxwell</dc:creator>\n"
                    + "        <dc:type xml:lang=\"da\">Journal Article</dc:type>\n"
                    + "        <dc:type xml:lang=\"en\">Journal Article</dc:type>\n"
                    + "        <dc:date>2010</dc:date>\n"
                    + "        <dc:format></dc:format>\n"
                    + "      </rdf:Description>\n"
                    + "    </rdf:RDF>\n"
                    + "  </shortrecord>\n";
            SearchNode summon = SummonTestHelper.createSummonSearchNode();
            assertFieldContent("shortformat", summon, query, "shortformat", expected, false);
            summon.close();
        }
    }

    public void testAuthor_xmlExtraction_shortformat2() throws RemoteException {
        String query = "ID:FETCH-LOGICAL-c1590-71216b8d44129eb55dba9244d0a7ad32261d9b5e7a00e7987e3aa5b33750b0dc1";
        { // sanity-check the Author-field
            String expected = "Fallah, Mahdi\n"
                              + "Kharazmi, Elham";
            SearchNode summon = SummonTestHelper.createSummonSearchNode();
            assertFieldContent("author direct", summon, query, "Author", expected, false);
            summon.close();
        }

        { // shortformat should match Author
            String expected =
                    "  <shortrecord>\n"
                    + "    <rdf:RDF xmlns:dc=\"http://purl.org/dc/elements/1.1/\" "
                    + "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n"
                    + "      <rdf:Description>\n"
                    + "        <dc:title>Substantial under-estimation in cancer incidence estimates for developing "
                    + "countries due to under-ascertainment in elderly cancer cases</dc:title>\n"
                    + "        <dc:creator>Fallah, Mahdi</dc:creator>\n"
                    + "        <dc:creator>Kharazmi, Elham</dc:creator>\n"
                    + "        <dc:type xml:lang=\"da\">Journal Article</dc:type>\n"
                    + "        <dc:type xml:lang=\"en\">Journal Article</dc:type>\n"
                    + "        <dc:date>2008</dc:date>\n"
                    + "        <dc:format></dc:format>\n"
                    + "      </rdf:Description>\n"
                    + "    </rdf:RDF>\n"
                    + "  </shortrecord>\n";
            SearchNode summon = SummonTestHelper.createSummonSearchNode();
            assertFieldContent("shortformat", summon, query, "shortformat", expected, false);
            summon.close();
        }
    }

    public void testScoreAssignment() throws RemoteException {
        String QUERY =
                "The effect of multimedia on perceived equivocality and perceived usefulness of information systems";
        String BAD =
                "<record score=\"0.0\" "
                + "id=\"summon_FETCH-LOGICAL-j865-7bb06e292771fe19b17b4f676a0939e693be812b38d8502735ffb8ab6e46b4d21\" "
                + "source=\"Summon\">";
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        Request req = new Request(
                DocumentKeys.SEARCH_QUERY, QUERY,
                DocumentKeys.SEARCH_MAX_RECORDS, 10,
                DocumentKeys.SEARCH_COLLECT_DOCIDS, false);

        ResponseCollection responses = new ResponseCollection();
        summon.search(req, responses);
        assertFalse("There should be a score != 0.0 for all records in\n" + responses.iterator().next().toXML(),
                    responses.iterator().next().toXML().contains(BAD));
        summon.close();

    }

    private void assertFieldContent(String message, SearchNode searchNode, String query, String fieldName,
                                    String expected, boolean sort) throws RemoteException {
        ResponseCollection responses = new ResponseCollection();
        searchNode.search(new Request(DocumentKeys.SEARCH_QUERY, query), responses);
        DocumentResponse docs = (DocumentResponse)responses.iterator().next();
        assertEquals(message + ". There should only be a single hit", 1, docs.getHitCount());
        boolean found = false;
        for (DocumentResponse.Record record: docs.getRecords()) {
            for (DocumentResponse.Field field: record.getFields()) {
                if (fieldName.equals(field.getName())) {
                    String content = field.getContent();
                    if (sort) {
                        String[] tokens = content.split("\n");
                        Arrays.sort(tokens);
                        content = Strings.join(tokens, "\n");

                    }
                    assertEquals(message + ".The field '" + fieldName + "' should have the right content",
                                 expected, content);
                    found = true;
                }
            }
        }
        if (!found) {
            fail("Unable to locate the field '" + fieldName + "'");
        }
    }

    public void testIDAdjustment() throws IOException {
        SimplePair<String, String> credentials = SummonTestHelper.getCredentials();
        Configuration conf = Configuration.newMemoryBased(
                InteractionAdjuster.CONF_IDENTIFIER, "summon",
                InteractionAdjuster.CONF_ADJUST_DOCUMENT_FIELDS, "recordID - ID");
        Configuration inner = conf.createSubConfiguration(
                AdjustingSearchNode.CONF_INNER_SEARCHNODE);
        inner.set(SearchNodeFactory.CONF_NODE_CLASS, SummonSearchNode.class.getCanonicalName());
        inner.set(SummonSearchNode.CONF_SUMMON_ACCESSID, credentials.getKey());
        inner.set(SummonSearchNode.CONF_SUMMON_ACCESSKEY, credentials.getValue());

        log.debug("Creating adjusting SummonSearchNode");
        AdjustingSearchNode adjusting = new AdjustingSearchNode(conf);
        Request request = new Request();
        //request.put(DocumentKeys.SEARCH_QUERY, "foo");
        request.put(DocumentKeys.SEARCH_QUERY, "recursion in string theory");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        List<String> ids = getAttributes(adjusting, request, "id", false);
        assertTrue("There should be at least one ID", !ids.isEmpty());

        request.clear();
        request.put(DocumentKeys.SEARCH_QUERY, IndexUtils.RECORD_FIELD + ":\"" + ids.get(0) + "\"");
        List<String> researchIDs = getAttributes(adjusting, request, "id", false);
        assertTrue("There should be at least one hit for a search for ID '"
                   + ids.get(0) + "'", !researchIDs.isEmpty());
    }

    // TODO: "foo:bar zoo"


    // It seems that "Book / eBook" is special and will be translated to s.fvgf (Book OR eBook) by summon
    // This is important as it means that we cannot use filter ContentType:"Book / eBook" to get the same
    // hits as a proper facet query
    public void testFacetTermWithDivider() throws RemoteException {
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);

        long filterCount = getHits(
                summon,
                DocumentKeys.SEARCH_QUERY, "foo",
                DocumentKeys.SEARCH_FILTER, "ContentType:\"Book / eBook\"",
                SolrSearchNode.SEARCH_SOLR_FILTER_IS_FACET, "true");
        long queryCount = getHits(
                summon,
                DocumentKeys.SEARCH_QUERY, "foo",
                DocumentKeys.SEARCH_FILTER, "ContentType:Book OR ContentType:eBook");

        assertTrue("There should be at least 1 hit for either query or filter request",
                   queryCount > 0 || filterCount > 0);
        assertEquals("The number of hits for filter and query based restrictions should be the same",
                     filterCount, queryCount);
        summon.close();
    }

    public void testFacetFieldValidity() throws RemoteException {
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        String[][] FACET_QUERIES = new String[][]{
                //{"Ferlay", "Author", "Ferlay\\, Jacques"}, // We need a sample from the Author facet
                {"foo", "Language", "German"},
                {"foo", "IsScholarly", "true"},
                {"foo", "IsFullText", "true"},
                {"foo", "ContentType", "Book / eBook"},
                {"foo", "SubjectTerms", "biology"}
        };
        for (String[] facetQuery: FACET_QUERIES) {
            String q = facetQuery[0];
            String ff = facetQuery[1] + ":\"" + facetQuery[2] + "\"";
            log.debug(String.format("Searching for query '%s' with facet filter '%s'", q, ff));
            long queryCount = getHits(
                    summon,
                    DocumentKeys.SEARCH_QUERY, q,
                    DocumentKeys.SEARCH_FILTER, ff,
                    SolrSearchNode.SEARCH_SOLR_FILTER_IS_FACET, "true");
            assertTrue(String.format("There should be at least 1 hit for query '%s' with facet filter '%s'", q, ff),
                       queryCount > 0);
        }
    }

    public void testFilterEffect() throws RemoteException {
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        String[][] FACET_QUERIES = new String[][]{
                //{"Ferlay", "Author", "Ferlay\\, Jacques"}, // We need a sample from the Author facet
                {"foo", "Language", "German"},
                {"foo", "IsScholarly", "true"},
                //  {"foo", "IsFullText", "true"},
                {"foo", "ContentType", "Book / eBook"},
                {"foo", "SubjectTerms", "biology"}
        };
        for (String[] facetQuery: FACET_QUERIES) {
            String q = facetQuery[0];
            String ff = facetQuery[1] + ":\"" + facetQuery[2] + "\"";

            long queryCount = getHits(
                    summon,
                    DocumentKeys.SEARCH_QUERY, q,
                    SolrSearchNode.SEARCH_SOLR_FILTER_IS_FACET, "true");
            log.debug(String.format("Searching for query '%s' with no facet filter gave %d hits'", q, queryCount));
            assertTrue(String.format("There should be at least 1 hit for query '%s' with no facet filter", q),
                       queryCount > 0);

            long filteredCount = getHits(
                    summon,
                    DocumentKeys.SEARCH_QUERY, q,
                    DocumentKeys.SEARCH_FILTER, ff,
                    SolrSearchNode.SEARCH_SOLR_FILTER_IS_FACET, "true");
            log.debug(String.format("Searching for query '%s' with facet filter '%s' gave %d hits",
                                    q, ff, filteredCount));

            assertTrue(String.format("There should not be the same number of hits with and without filtering for " +
                                     "query '%s' with facet filter '%s but there were %d hits'", q, ff, queryCount),
                       queryCount != filteredCount);
        }
    }

    public void testEmptyQuerySkip() throws RemoteException {
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        long hits = getHits(
                summon,
                DocumentKeys.SEARCH_QUERY, "",
                SolrSearchNode.SEARCH_SOLR_FILTER_IS_FACET, "true");
        summon.close();
        assertEquals("There should be zero hits", 0, hits);
    }

    public void testIsScholarly() throws RemoteException {
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        String q = "foo";
        long queryCount = getHits(
                summon,
                DocumentKeys.SEARCH_QUERY, "foo",
                SolrSearchNode.SEARCH_SOLR_FILTER_IS_FACET, "true");
        log.debug(String.format("Searching for query '%s' with no special params gave %d hits'", q, queryCount));
        assertTrue(String.format("There should be at least 1 hit for query '%s' with no special params", q),
                   queryCount > 0);

        long filteredCount = getHits(
                summon,
                DocumentKeys.SEARCH_QUERY, q,
                "summonparam.s.cmd", "addFacetValueFilters(IsScholarly,true)",
                SolrSearchNode.SEARCH_SOLR_FILTER_IS_FACET, "true");
        log.debug(String.format("Searching for query '%s' with special param " +
                                "summonparam.s.cmd=addFacetValueFilters(IsScholarly,true) gave %d hits",
                                q, filteredCount));

        assertTrue(String.format("There should not be the same number of hits for special param " +
                                 "summonparam.s.cmd=addFacetValueFilters(IsScholarly,true) with and without " +
                                 "filtering for query '%s' but there were %d hits'", q, queryCount),
                   queryCount != filteredCount);
    }

    public void testQueryNoFilter() throws Exception {
        assertResponse(new Request(DocumentKeys.SEARCH_QUERY, "foo"), true);
    }

    public void testFilterNoQuery() throws Exception {
        assertResponse(new Request(DocumentKeys.SEARCH_FILTER, "foo"), true);
    }

    public void testFilterAndQuery() throws Exception {
        assertResponse(new Request(
                DocumentKeys.SEARCH_QUERY, "foo",
                DocumentKeys.SEARCH_FILTER, "bar"
        ), true);
    }

    public void testNoFilterNoQuery() throws Exception {
        assertResponse(new Request("foo", "bar"), false);
    }

    private void assertResponse(Request request, boolean responseExpected) throws RemoteException {
        SearchNode summon = SummonTestHelper.createSummonSearchNode(true);
        ResponseCollection responses = new ResponseCollection();
        summon.search(request, responses);
        if (responseExpected) {
            assertTrue("There should be a response for " + request, responses.iterator().hasNext());
        } else {
            assertFalse("There should not be a response for " + request, responses.iterator().hasNext());
        }
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

    protected void assertHits(String message, SearchNode searcher, String... queries) throws RemoteException {
        long hits = getHits(searcher, queries);
        assertTrue(message + ". Hits == " + hits, hits > 0);
    }

    protected void assertHits(String message, String query, int expectedHits) throws RemoteException {
        SummonSearchNode summon = SummonTestHelper.createSummonSearchNode();
        long hits = getHits(summon, DocumentKeys.SEARCH_QUERY, query);
        assertEquals(message + ". Query='" + query + "'", expectedHits, hits);
    }
}
