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
package dk.statsbiblioteket.summa.support.summon.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.SearchNodeFactory;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.support.harmonise.AdjustingSearchNode;
import dk.statsbiblioteket.summa.support.harmonise.InteractionAdjuster;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.rmi.RemoteException;
import java.util.*;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummonSearchNodeTest extends TestCase {
    private static Log log = LogFactory.getLog(SummonSearchNodeTest.class);

    public SummonSearchNodeTest(String name) {
        super(name);
    }

    private static final File SECRET =
        new File(System.getProperty("user.home") + "/summon-credentials.dat");
    private String id;
    private String key;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (!SECRET.exists()) {
            throw new IllegalStateException(
                "The file '" + SECRET.getAbsolutePath() + "' must exist and "
                + "contain two lines, the first being access ID, the second"
                + "being access key for the Summon API");
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(
            new FileInputStream(SECRET), "utf-8"));
        id = br.readLine();
        key = br.readLine();
        br.close();
        log.debug("Loaded credentials from " + SECRET);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(SummonSearchNodeTest.class);
    }

    public void testBasicSearch() throws RemoteException {
        Configuration conf = Configuration.newMemoryBased(
            SummonSearchNode.CONF_SUMMON_ACCESSID, id,
            SummonSearchNode.CONF_SUMMON_ACCESSKEY, key
            //SummonSearchNode.CONF_SUMMON_FACETS, ""
        );

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
        //System.out.println(responses.toXML());
        assertTrue("The result should contain at least one record",
                   responses.toXML().contains("<record score"));
        assertTrue("The result should contain at least one tag",
                   responses.toXML().contains("<tag name"));

    }

    public void testRecommendations() throws RemoteException {
        Configuration conf = Configuration.newMemoryBased(
            SummonSearchNode.CONF_SUMMON_ACCESSID, id,
            SummonSearchNode.CONF_SUMMON_ACCESSKEY, key
            //SummonSearchNode.CONF_SUMMON_FACETS, ""
        );

        SummonSearchNode summon = new SummonSearchNode(conf);
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "cancer");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        summon.search(request, responses);
        System.out.println(responses.toXML());
        assertTrue("The result should contain at least one recommendation",
                   responses.toXML().contains("<recommendation "));

/*        responses.clear();
        request.put(DocumentKeys.SEARCH_QUERY, "noobddd");
        summon.search(request, responses);
        System.out.println(responses.toXML());
  */
    }

    public void testCustomParams() throws RemoteException {
        final String QUERY = "reactive arthritis yersinia lassen";

        Configuration confInside = Configuration.newMemoryBased(
            SummonSearchNode.CONF_SUMMON_ACCESSID, id,
            SummonSearchNode.CONF_SUMMON_ACCESSKEY, key
            //SummonSearchNode.CONF_SUMMON_FACETS, ""
        );
        Configuration confOutside = Configuration.newMemoryBased(
            SummonSearchNode.CONF_SUMMON_ACCESSID, id,
            SummonSearchNode.CONF_SUMMON_ACCESSKEY, key,
            SummonSearchNode.CONF_SUMMON_PARAM_PREFIX + "s.ho",
            new ArrayList<String>(Arrays.asList("false"))
            //SummonSearchNode.CONF_SUMMON_FACETS, ""
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

        request.put(SummonSearchNode.CONF_SUMMON_PARAM_PREFIX + "s.ho",
                    new ArrayList<String>(Arrays.asList("false")));
        ResponseCollection responsesSearchTweak = new ResponseCollection();
        summonInside.search(request, responsesSearchTweak);

        int countInside = countResults(responsesInside);
        int countOutside = countResults(responsesOutside);
        assertTrue("The number of results for a search for '" + QUERY
                   + "' within holdings (" + confInside + ") should be less "
                   + "that outside holdings (" + confOutside + ")",
                   countInside < countOutside);
        log.info(String.format(
            "The search for '%s' gave %d hits within holdings and %d hits in"
            + " total", QUERY, countInside, countOutside));

        int countSearchTweak = countResults(responsesSearchTweak);
        assertEquals(
            "Query time specification of 's.ho=false' should give the same "
            + "result as configuration time specification of the same",
            countOutside, countSearchTweak);
    }

    public void testConvertRangeQueries() {
        final String QUERY = "foo bar:[10 TO 20] OR baz:[87 TO goa]";
        Map<String, List<String>> params = new HashMap<String, List<String>>();
        String stripped = SummonSearchNode.convertRangeQueries(QUERY, params);
        assertNotNull("RangeFilter should be defined", params.get("s.rf"));
        List<String> ranges = params.get("s.rf");
        assertEquals("The right number of ranges should be extracted",
                     2, ranges.size());
        assertEquals("Range #1 should be correct", "bar,10,20", ranges.get(0));
        assertEquals("Range #2 should be correct", "baz,87,goa", ranges.get(1));
        assertEquals("The resulting query should be stripped of ranges",
                     "(+foo)", stripped);
    }

    public void testConvertRangeQueriesEmpty() {
        final String QUERY = "bar:[10 TO 20]";
        Map<String, List<String>> params = new HashMap<String, List<String>>();
        String stripped = SummonSearchNode.convertRangeQueries(QUERY, params);
        assertNotNull("RangeFilter should be defined", params.get("s.rf"));
        List<String> ranges = params.get("s.rf");
        assertEquals("The right number of ranges should be extracted",
                     1, ranges.size());
        assertEquals("Range #1 should be correct", "bar,10,20", ranges.get(0));
        assertNull("The resulting query should be null", stripped);
    }

    // This fails, but as we are really testing Summon here, there is not much
    // we can do about it
    public void disabledtestCounts() throws RemoteException {
  //      final String QUERY = "reactive arthritis yersinia lassen";
        final String QUERY = "author:(Helweg Larsen) abuse";

        Configuration conf = Configuration.newMemoryBased(
            SummonSearchNode.CONF_SUMMON_ACCESSID, id,
            SummonSearchNode.CONF_SUMMON_ACCESSKEY, key
        );

        Request request = new Request();
        request.addJSON(
            "{search.document.query:\"" + QUERY + "\", "
            + "summonparam.s.ps:\"15\", summonparam.s.ho:\"false\"}");
        String r1 = request.toString(true);

        SummonSearchNode summon = new SummonSearchNode(conf);
        ResponseCollection responses = new ResponseCollection();
        summon.search(request, responses);
        int count15 = countResults(responses);

        request.clear();
        request.addJSON(
            "{search.document.query:\"" + QUERY + "\", "
            + "summonparam.s.ps:\"30\", summonparam.s.ho:\"false\"}");
        String r2 = request.toString(true);
        responses.clear();
        summon.search(request, responses);
        int count20 = countResults(responses);
/*
        request.clear();
        request.put(DocumentKeys.SEARCH_QUERY, QUERY);
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        request.put(SummonSearchNode.CONF_SUMMON_PARAM_PREFIX + "s.ho",
                    new ArrayList<String>(Arrays.asList("false")));
        request.put(DocumentKeys.SEARCH_MAX_RECORDS, 15);
        String rOld15 = request.toString(true);
        responses.clear();
        summon.search(request, responses);
        int countOld15 = countResults(responses);

        request.clear();
        request.put(DocumentKeys.SEARCH_QUERY, QUERY);
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        request.put(SummonSearchNode.CONF_SUMMON_PARAM_PREFIX + "s.ho",
                    new ArrayList<String>(Arrays.asList("false")));
        request.put(DocumentKeys.SEARCH_MAX_RECORDS, 20);
        String rOld20 = request.toString(true);
        responses.clear();
        summon.search(request, responses);
        int countOld20 = countResults(responses);
  */
        System.out.println("Request 15:  " + r1 + ": " + count15);
        System.out.println("Request 20:  " + r2 + ": " + count20);
//        System.out.println("Request O15: " + rOld15 + ": " + countOld15);
//        System.out.println("Request O20: " + rOld20 + ": " + countOld20);
        assertEquals("The number of hits should not be affected by page size",
                     count15, count20);
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
        Configuration conf = Configuration.newMemoryBased(
            InteractionAdjuster.CONF_IDENTIFIER, "summon",
            InteractionAdjuster.CONF_ADJUST_DOCUMENT_FIELDS, "recordID - ID");
        Configuration inner = conf.createSubConfiguration(
            AdjustingSearchNode.CONF_INNER_SEARCHNODE);
        inner.set(SearchNodeFactory.CONF_NODE_CLASS,
                  SummonSearchNode.class.getCanonicalName());
        inner.set(SummonSearchNode.CONF_SUMMON_ACCESSID, id);
        inner.set(SummonSearchNode.CONF_SUMMON_ACCESSKEY, key);

        log.debug("Creating adjusting SummonSearchNode");
        AdjustingSearchNode adjusting = new AdjustingSearchNode(conf);
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        //request.put(DocumentKeys.SEARCH_QUERY, "foo");
        request.put(DocumentKeys.SEARCH_QUERY, "recursion in string theory");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        log.debug("Searching");
        adjusting.search(request, responses);
        log.debug("Finished searching");
        System.out.println(responses.toXML());
    }
}
