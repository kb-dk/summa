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
package dk.statsbiblioteket.summa.support.alto;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.search.SearchNodeAggregator;
import dk.statsbiblioteket.summa.search.SearchNodeFactory;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.support.solr.SBSolrSearchNode;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A very poor test as it relies on instances running at Statsbiblioteket's test machine.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AltoBoxSearcherTest extends TestCase {
    private static Log log = LogFactory.getLog(AltoBoxSearcherTest.class);

    public void testNoTermsNoHighlight() throws IOException {
        SummaSearcher searcher = getDevelSearcher();
        if (searcher == null) {
            return;
        }
        ResponseCollection responses = searcher.search(new Request(
                DocumentKeys.SEARCH_QUERY, "hest",
                DocumentKeys.SEARCH_RESULT_FIELDS, "recordID, fulltext_org, alto_box, pageUUID",
                AltoBoxSearcher.CONF_BOX, true,
                AltoBoxSearcher.CONF_ID_FIELD, "pageUUID"
        ));
        List<AltoBoxResponse.Box> boxes = getBoxes(responses);
        assertEquals("Without highlighting and no requestTerms there should be no boxes", 0, boxes.size());
//        System.out.println(responses.toXML());
    }

    // Does not highlight "Ninn-Hansen" although it exists in the record
    // http://rhea:56808/mediehub/storage/services/StorageWS?method=getRecord&id=doms_newspaperCollection:uuid:3350757c-25a7-4146-b464-2f99610e3303
    public void testHighlightSingleLineHyphenationWords() throws IOException {
        //final String QUERY = "shikoku AND pageUUID:\"doms_aviser_page:uuid:83c5e391-6cd5-4b52-8e91-dd1c686caf7c\"";
        final String query = "erik ninn-hansen AND pageUUID:\"doms_aviser_page:uuid:3350757c-25a7-4146-b464-2f99610e3303\"";
        SummaSearcher searcher = getStageSearcher();
        if (searcher == null) {
            return;
        }
        String[] expected = new String[]{"Ninn", "Hansen"};

        testHighlightHelper("Single-line",searcher, query, expected);
    }

    public void testHighlightMultiLineHyphenationWords() throws IOException {
        //final String QUERY = "shikoku AND pageUUID:\"doms_aviser_page:uuid:83c5e391-6cd5-4b52-8e91-dd1c686caf7c\"";
        final String query = "Jensen AND pageUUID:\"doms_aviser_page:uuid:9d35d33b-75b2-4912-b77f-5a9e4cc5179b\"";
        SummaSearcher searcher = getDevelSearcher();
        if (searcher == null) {
            return;
        }
        String[] expected = new String[]{"Jensen"};

        testHighlightHelper("Multi-line",searcher, query, expected);
    }

    private void testHighlightHelper(
            String designation, SummaSearcher searcher, String query, String[] expected) throws IOException {
        ResponseCollection responses = searcher.search(new Request(
                DocumentKeys.SEARCH_QUERY, query,
                DocumentKeys.SEARCH_RESULT_FIELDS, "recordID, fulltext_org, alto_box, pageUUID",
                AltoBoxSearcher.SEARCH_BOX, true,
                AltoBoxSearcher.SEARCH_ID_FIELD, "", // default
                AltoBoxSearcher.SEARCH_ID_REGEXP, "(doms_newspaperCollection:uuid:[0-9abcdef]{8}-[0-9abcdef]{4}-"
                                                  + "[0-9abcdef]{4}-[0-9abcdef]{4}-[0-9abcdef]{12}).*",
                AltoBoxSearcher.SEARCH_ID_TEMPLATE, "$1",
                "solrparam.hl", true,
                "solrparam.hl.fl", "fulltext_org",
                "solrparam.hl.snippets", 20
        ));
        for (Response response: responses) {
            if (response instanceof DocumentResponse) {
                if (((DocumentResponse)response).getHitCount() == 0) {
                    log.warn(designation + ": No hits for '" + query + "'. Skipping unit test");
                }
            }
        }
        List<AltoBoxResponse.Box> boxes = getBoxes(responses);
        assertFalse(designation + ": With highlighting there should be at least one box", boxes.isEmpty());

        boolean match = false;
        for (AltoBoxResponse.Box box: boxes) {
            boolean subMatch = true;
            for (String exp: expected) {
                if (!box.getContent().contains(exp)) {
                    subMatch = false;
                    break;
                }
            }
            if (subMatch) {
                match = true;
                break;
            }
        }
        assertTrue(designation + ": One of the boxes should contain 'Ninn' and 'Hansen'", match);
    }


    public void testHighlight() throws IOException {
        SummaSearcher searcher = getDevelSearcher();
        if (searcher == null) {
            return;
        }
        ResponseCollection responses = searcher.search(new Request(
                DocumentKeys.SEARCH_QUERY, "hest",
                DocumentKeys.SEARCH_RESULT_FIELDS, "recordID, fulltext_org, alto_box, pageUUID",
                AltoBoxSearcher.SEARCH_BOX, true,
                AltoBoxSearcher.SEARCH_ID_FIELD, "", // default
                //  doms_newspaperCollection:uuid:cced6bb3-96ed-45af-aeda-1bcab7a5b2ad-segment_1
                //  doms_newspaperCollection:uuid:cced6bb3-96ed-45af-aeda-1bcab7a5b2ad
                AltoBoxSearcher.SEARCH_ID_REGEXP, "(doms_newspaperCollection:uuid:[0-9abcdef]{8}-[0-9abcdef]{4}-"
                                                  + "[0-9abcdef]{4}-[0-9abcdef]{4}-[0-9abcdef]{12}).*",
                AltoBoxSearcher.SEARCH_ID_TEMPLATE, "$1",
                "solrparam.hl", true,
                "solrparam.hl.fl", "fulltext_org",
                "solrparam.hl.snippets", 20
        ));
        List<AltoBoxResponse.Box> boxes = getBoxes(responses);
        assertFalse("With highlighting there should be at least one box", boxes.isEmpty());
        //System.out.println(responses.toXML());
    }

    /*
    public void testWorkingProdAviserHighlight() throws IOException {
        assertSpecificProdAviserHighlight("doms_aviser_page:uuid:145c11a0-530b-4c66-9d47-7e321fa23805", "gedeost");
    }
    // This does not work as RMI is (understandably) blocked in the prod firewall
    public void testNonWorkingProdAviserHighlight() throws IOException {
        SummaSearcher searcher = getDevelSearcher(
                "prod-search03", 56700, "/aviser/sbsolr/select", "aviser-storage");
        assertHighlight(searcher, "doms_aviser_page:uuid:47aa35fc-6551-49b5-9c0c-4d323fd92f52", "titanic");
    }
      */

    public void testWorkingMarsAviserHighlight() throws IOException {
        SummaSearcher searcher = getDevelSearcher();
        assertHighlight(searcher, "doms_aviser_page:uuid:065527ab-d9dd-4ebe-a2bb-ca49ac8c7b7a", "overskrift");
    }

    /*
    This failed due to the highlighter returning 'Overskrift:' which did not match against the term 'overskrift' in
    the ALTO-XML. The fix was to also normalise the highlighter's result to 'Overskrift' before processing it further.
    This might lead to more false positive highlights, but that is preferable to the otherwise high amount of false
    negatives (aka missing highlights).
     */
    public void testNonWorkingMarsAviserHighlight() throws IOException {
        SummaSearcher searcher = getDevelSearcher();
        assertHighlight(searcher, "doms_aviser_page:uuid:e66d8304-2a90-4faa-beee-2f7660675306", "overskrift");
    }

    private void assertHighlight(SummaSearcher searcher, String pageUUID, String query) throws IOException {
        if (searcher == null) {
            return;
        }
        ResponseCollection responses = searcher.search(new Request(
                DocumentKeys.SEARCH_QUERY, query,
                DocumentKeys.SEARCH_RESULT_FIELDS, "recordID, fulltext_org, alto_box, pageUUID, recordBase",
                DocumentKeys.SEARCH_FILTER, "pageUUID:\"" + pageUUID + "\"",
                AltoBoxSearcher.SEARCH_BOX, true,
                AltoBoxSearcher.SEARCH_ID_FIELD, "", // default
                //  doms_newspaperCollection:uuid:cced6bb3-96ed-45af-aeda-1bcab7a5b2ad-segment_1
                //  doms_newspaperCollection:uuid:cced6bb3-96ed-45af-aeda-1bcab7a5b2ad
                AltoBoxSearcher.SEARCH_ID_REGEXP, "(doms_newspaperCollection:uuid:[0-9abcdef]{8}-[0-9abcdef]{4}-"
                                                  + "[0-9abcdef]{4}-[0-9abcdef]{4}-[0-9abcdef]{12}).*",
                AltoBoxSearcher.SEARCH_ID_TEMPLATE, "$1",
                "solrparam.hl", true,
                "solrparam.hl.fl", "fulltext_org",
                "solrparam.hl.snippets", 20
        ));
        List<AltoBoxResponse.Box> boxes = getBoxes(responses);
        assertFalse("With highlighting there should be at least one box", boxes.isEmpty());
        //System.out.println(responses.toXML());
    }

    // The ALTO-text for this was "Lykønskning," with highlight "Lykønskening".
    // The matcher did not ignore the comma at the end.
    public void testCommaHighlight() throws IOException {
        SummaSearcher searcher = getDevelSearcher();
        if (searcher == null) {
            return;
        }
        ResponseCollection responses = searcher.search(new Request(
                DocumentKeys.SEARCH_QUERY,
                "lykønskning pageUUID:\"doms_aviser_page:uuid:4ab71f1e-f5d4-4e64-ba26-1f62ef2503c2\"",
                DocumentKeys.SEARCH_RESULT_FIELDS, "recordID, fulltext_org, alto_box, pageUUID",
                AltoBoxSearcher.SEARCH_BOX, true,
                AltoBoxSearcher.SEARCH_ID_FIELD, "", // default
                AltoBoxSearcher.SEARCH_ID_REGEXP, "(doms_newspaperCollection:uuid:[0-9abcdef]{8}-[0-9abcdef]{4}-"
                                                  + "[0-9abcdef]{4}-[0-9abcdef]{4}-[0-9abcdef]{12}).*",
                AltoBoxSearcher.SEARCH_ID_TEMPLATE, "$1",
                "solrparam.hl", true,
                "solrparam.hl.fl", "fulltext_org",
                "solrparam.hl.snippets", 20
        ));
        List<AltoBoxResponse.Box> boxes = getBoxes(responses);
        assertFalse("With highlighting there should be at least one box", boxes.isEmpty());
        //System.out.println(responses.toXML());
    }

    public void testPhraseHighlight() throws IOException {
        SummaSearcher searcher = getDevelSearcher();
        if (searcher == null) {
            return;
        }
        ResponseCollection responses = getDevelSearcher().search(new Request(
                DocumentKeys.SEARCH_QUERY,
                "\"Jesper Tørring blev\"",
                DocumentKeys.SEARCH_RESULT_FIELDS, "recordID, fulltext_org, alto_box, pageUUID",
                AltoBoxSearcher.SEARCH_BOX, true,
                AltoBoxSearcher.SEARCH_ID_FIELD, "", // default
                AltoBoxSearcher.SEARCH_ID_REGEXP, "(doms_newspaperCollection:uuid:[0-9abcdef]{8}-[0-9abcdef]{4}-"
                                                  + "[0-9abcdef]{4}-[0-9abcdef]{4}-[0-9abcdef]{12}).*",
                AltoBoxSearcher.SEARCH_ID_TEMPLATE, "$1",
                "solrparam.hl", true,
                "solrparam.hl.fl", "fulltext_org",
                "solrparam.hl.snippets", 20
        ));
        System.out.println(responses.toXML());
        List<AltoBoxResponse.Box> boxes = getBoxes(responses);
        assertFalse("With highlighting there should be at least one box", boxes.isEmpty());
        //System.out.println(responses.toXML());
    }

    public void testSpecificHansJensen() throws IOException {
        SummaSearcher searcher = getDevelSearcher();
        if (searcher == null) {
            return;
        }
        ResponseCollection responses = getDevelSearcher().search(new Request(
                DocumentKeys.SEARCH_QUERY,
                "\"hans jensen\" editionUUID:\"doms_aviser_edition:uuid:6e8dff93-d59d-4bec-be95-321cb73b8c9c\"",
                DocumentKeys.SEARCH_RESULT_FIELDS, "recordID, fulltext_org, alto_box, pageUUID",
                AltoBoxSearcher.SEARCH_BOX, true,
                AltoBoxSearcher.SEARCH_ID_FIELD, "", // default
                AltoBoxSearcher.SEARCH_ID_REGEXP, "(doms_newspaperCollection:uuid:[0-9abcdef]{8}-[0-9abcdef]{4}-"
                                                  + "[0-9abcdef]{4}-[0-9abcdef]{4}-[0-9abcdef]{12}).*",
                AltoBoxSearcher.SEARCH_ID_TEMPLATE, "$1",
                "solrparam.hl", true,
                "solrparam.hl.fl", "fulltext_org",
                "solrparam.hl.snippets", 20
        ));
        System.out.println(responses.toXML());
        List<AltoBoxResponse.Box> boxes = getBoxes(responses);
        assertEquals("With highlighting there should be the right number of boxes", 2, boxes.size());
        //System.out.println(responses.toXML());
    }

    public void testHighlightRelative() throws IOException {
        SummaSearcher searcher = getDevelSearcher();
        if (searcher == null) {
            return;
        }
        ResponseCollection responses = getDevelSearcher().search(new Request(
                DocumentKeys.SEARCH_QUERY, "hest",
                DocumentKeys.SEARCH_RESULT_FIELDS, "recordID, fulltext_org, alto_box, pageUUID",
                AltoBoxSearcher.SEARCH_BOX, true,
                AltoBoxSearcher.SEARCH_ID_FIELD, "", // default
                //  doms_newspaperCollection:uuid:cced6bb3-96ed-45af-aeda-1bcab7a5b2ad-segment_1
                //  doms_newspaperCollection:uuid:cced6bb3-96ed-45af-aeda-1bcab7a5b2ad
                AltoBoxSearcher.SEARCH_ID_REGEXP, "(doms_newspaperCollection:uuid:[0-9abcdef]{8}-[0-9abcdef]{4}-"
                                                  + "[0-9abcdef]{4}-[0-9abcdef]{4}-[0-9abcdef]{12}).*",
                AltoBoxSearcher.SEARCH_ID_TEMPLATE, "$1",
                AltoBoxSearcher.SEARCH_COORDINATES_RELATIVE, "true",
                "solrparam.hl", true,
                "solrparam.hl.fl", "fulltext_org",
                "solrparam.hl.snippets", 20
        ));
        List<AltoBoxResponse.Box> boxes = getBoxes(responses);
        assertFalse("With highlighting there should be at least one box", boxes.isEmpty());
        int vposAbove = 0;
        int heightAbove = 0;
        for (AltoBoxResponse.Box box: boxes) {
            assertTrue("Expected relative for " + box.content, box.relative);
            assertTrue("Expected hpos <= 1.0 with relative. Got " + box.hpos + " for " + box.content, box.hpos <= 1.0);
            assertTrue("Expected vpos <= 2.0 with relative. Got " + box.vpos + " for " + box.content, box.vpos <= 2.0);
            assertTrue("Expected 0.0 <= width <= 1.0 with relative. Got " + box.width + " for " + box.content,
                       0.0 <= box.width && box.width <= 1.0);
            assertTrue("Expected 0.0 <= height <= 2.0 with relative. Got " + box.height + " for " + box.content,
                       0.0 <= box.height && box.height <= 2.0);
            vposAbove += box.vpos > 1.0 ? 1 : 0;
            heightAbove += box.height > 1.0 ? 1 : 0;
        }
        // Too much pure chance for a real test
        //assertTrue("There should be at least one box at relative vpos > 1.0 due to yisx=true", vposAbove != 0);
        //System.out.println(responses.toXML());
    }

    public void testPattern() {
        final String content = "men i øvrigt se ud som en <em>hest</em>. Dyret sk:l være 2 mete";
        Pattern highlightPattern = Pattern.compile(" ([^ ]*<em>[^<]+</em>[^ ]*) ");
        Matcher hlMatcher = highlightPattern.matcher(" " + content + " ");
        if (hlMatcher.find()) {
            assertEquals("hest.", hlMatcher.group(1).replace("<em>", "").replace("</em>", ""));
        }
    }

    public void testTrim() {
        final String[][] TESTS = new String[][] {
                {"foo", "foo"},
                {"foo", " foo"},
                {"foo bar", " foo bar"},
                {"foo/bar", " foo/bar!"},
                {"foo", "foo!"},
                {"foo", "foo!!"},
                {"foo", "foo!?"},
                {"foo", "foo!?,"},
                {"foo", "foo!?,/"},
                {"foo", "foo!?,/ "},
                {"foo", "foo!?,/ \""},
                {"foo", "foo!?,/ \"."},
                {"foo", "foo!?,/ \".-"},
                {"foo", "foo:"},
                {"foo", "foo;"},
                {"foo!?,/ \".-bar", "!?,/ \".-foo!?,/ \".-bar!?,/ \".-"},
                {"foo", "foo,"}
        };
        Pattern trim = Pattern.compile(AltoBoxSearcher.DEFAULT_ALTO_STRING_TRIM_REGEXP);
        for (String[] test: TESTS) {
            assertEquals("Trimming '" + test[1] + " should yield the correct result",
                         test[0], trim.matcher(test[1]).replaceAll("$1"));
        }
    }

    public void testEnabledNoHighlight() throws IOException {
        SummaSearcher searcher = getDevelSearcher();
        if (searcher == null) {
            return;
        }
        ResponseCollection responses = searcher.search(new Request(
                DocumentKeys.SEARCH_QUERY, "hest",
                DocumentKeys.SEARCH_RESULT_FIELDS, "recordID, fulltext_org, alto_box, pageUUID",
                AltoBoxSearcher.SEARCH_BOX, true,
                AltoBoxSearcher.SEARCH_ID_FIELD, "", // default
                //  doms_newspaperCollection:uuid:cced6bb3-96ed-45af-aeda-1bcab7a5b2ad-segment_1
                //  doms_newspaperCollection:uuid:cced6bb3-96ed-45af-aeda-1bcab7a5b2ad
                AltoBoxSearcher.SEARCH_ID_REGEXP, "(doms_newspaperCollection:uuid:[0-9abcdef]{8}-[0-9abcdef]{4}-"
                                                  + "[0-9abcdef]{4}-[0-9abcdef]{4}-[0-9abcdef]{12}).*",
                AltoBoxSearcher.SEARCH_ID_TEMPLATE, "$1",
                "solrparam.hl", false
        ));
        List<AltoBoxResponse.Box> boxes = getBoxes(responses);
        assertTrue("Without highlighting there should be no boxes", boxes.isEmpty());
        //System.out.println(responses.toXML());
    }

    private List<AltoBoxResponse.Box> getBoxes(ResponseCollection responses) {
        List<AltoBoxResponse.Box> boxes = new ArrayList<>();
        for (Response response: responses) {
            if (response instanceof AltoBoxResponse) {
                for (Map.Entry<String, List<AltoBoxResponse.Box>> entry:
                        ((AltoBoxResponse)response).getBoxes().entrySet()) {
                    boxes.addAll(entry.getValue());
                }
            }
        }
        return boxes;
    }

    private SummaSearcher getDevelSearcher() throws IOException {
        return getFullStackWithTest("mars", 50001, "/solr/aviser.2.devel/select",
                                    "mars", 56700, "aviser-storage");
        //return getDevelSearcher("mars", 56708, "/aviser/sbsolr/select", "aviser-storage");
    }
    private SummaSearcher getStageSearcher() throws IOException {
//        return getFullStackWithTest("rhea", 50001, "/solr/aviser.p.20170228/select",
//                                    "rhea", 56700, "aviser-storage");
        return getFullStackWithTest("rhea", 50001, "/solr/aviser.1.stage/select",
                                    "rhea", 56700, "aviser-storage");
    }

    // Not available due to tightened firewall rules
    private SummaSearcher getProd3Searcher() throws IOException {
        return getFullStackWithTest("prod-search03", 50001, "/solr/aviser.1.prod/select",
                                    "prod-search03", 56700, "aviser-storage");
    }
    private SummaSearcherImpl getFullStackWithTest(
            String solrHost, int solrPort, String solrRest, String storageHost, int storagePort, String storageID)
            throws IOException {
        SummaSearcherImpl searcher = getFullStack(solrHost, solrPort, solrRest, storageHost, storagePort, storageID);
        try {
            searcher.search(new Request(DocumentKeys.SEARCH_QUERY, "ddsdffsfss"));
        } catch (IOException e) {
            log.warn("This test only runs on Statsbiblioteket, sorry");
            return null;
        }
        return searcher;
    }

    private SummaSearcherImpl getFullStack(
            String solrHost, int solrPort, String solrRest, String storageHost, int storagePort, String storageID)
            throws IOException {
        Configuration searcherConf = Configuration.newMemoryBased(
                SummaSearcherImpl.CONF_USE_LOCAL_INDEX, false
        );
        searcherConf.set(SearchNodeFactory.CONF_NODE_CLASS, SearchNodeAggregator.class.getCanonicalName());
        searcherConf.set(SearchNodeAggregator.CONF_SEQUENTIAL, true);
        List<Configuration> nodeConfs = searcherConf.createSubConfigurations(SearchNodeFactory.CONF_NODES, 2);

        nodeConfs.get(0).set(SearchNodeFactory.CONF_NODE_CLASS, SBSolrSearchNode.class.getCanonicalName());
        nodeConfs.get(0).set(SBSolrSearchNode.CONF_SOLR_HOST, solrHost + ":" + solrPort); // "mars:56708"
        nodeConfs.get(0).set(SBSolrSearchNode.CONF_SOLR_RESTCALL, solrRest); // "/aviser/sbsolr/select"
        nodeConfs.get(0).set(SBSolrSearchNode.CONF_SOLR_CONNECTION_TIMEOUT, 500);
        nodeConfs.get(0).set(SBSolrSearchNode.CONF_SOLR_READ_TIMEOUT, 5000);

        nodeConfs.get(1).set(SearchNodeFactory.CONF_NODE_CLASS, AltoBoxSearcher.class.getCanonicalName());
        // "//mars:56700/aviser-storage"
        nodeConfs.get(1).set(ConnectionConsumer.CONF_RPC_TARGET, "//" + storageHost + ":" + storagePort + "/" + storageID);
        nodeConfs.get(1).set(ConnectionConsumer.CONF_INITIAL_GRACE_TIME, 500);
        nodeConfs.get(1).set(ConnectionConsumer.CONF_SUBSEQUENT_GRACE_TIME, 1000);
        return new SummaSearcherImpl(searcherConf);
    }
}
