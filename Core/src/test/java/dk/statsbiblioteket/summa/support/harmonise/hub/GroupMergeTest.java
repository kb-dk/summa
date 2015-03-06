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
package dk.statsbiblioteket.summa.support.harmonise.hub;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.unittest.ExtraAsserts;
import dk.statsbiblioteket.summa.search.SearchNode;
import dk.statsbiblioteket.summa.search.SearchNodeAggregator;
import dk.statsbiblioteket.summa.search.SummaSearcherAggregator;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.summa.search.api.*;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import dk.statsbiblioteket.summa.search.rmi.RMISearcherProxy;
import dk.statsbiblioteket.summa.support.embeddedsolr.EmbeddedJettyWithSolrServer;
import dk.statsbiblioteket.summa.support.harmonise.AdjustingSearcherAggregator;
import dk.statsbiblioteket.summa.support.harmonise.InteractionAdjuster;
import dk.statsbiblioteket.summa.support.solr.SolrSearchNode;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class GroupMergeTest extends SolrSearchDualTestBase {
    private static Log log = LogFactory.getLog(GroupMergeTest.class);

    public static final int DOCS = 200;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        basicIngest(DOCS);
    }

    public void testRunning() {
        assertTrue("server0 should be running", server0.isStarted());
        assertTrue("server1 should be running", server1.isStarted());
    }

    public void testBasicIngest() throws IOException, SolrServerException {
        {
            QueryResponse r = solrServer0.query(new SolrQuery("*:*"));
            assertEquals("There should be hits from Solr 0",
                         DOCS/2, r.getResults().getNumFound());
        }
        {
            QueryResponse r = solrServer0.query(new SolrQuery("*:*"));
            assertEquals("There should be hits from Solr 1",
                         DOCS/2, r.getResults().getNumFound());
        }
    }

    public void testSearchNode() throws RemoteException {
        SolrSearchNode searchNode = new SolrSearchNode(Configuration.newMemoryBased(
                SolrSearchNode.CONF_SOLR_HOST, "localhost:" + (EmbeddedJettyWithSolrServer.DEFAULT_PORT+1)
        ));
        ResponseCollection responses = new ResponseCollection();
        searchNode.search(new Request(DocumentKeys.SEARCH_QUERY, "fulltext:ra"), responses);
        assertTrue("There should be hits", ((DocumentResponse)responses.iterator().next()).getHitCount() > 0);
        searchNode.close();
    }

    public void testSinglePaging() throws RemoteException {
        SolrSearchNode searchNode = new SolrSearchNode(Configuration.newMemoryBased(
                SolrSearchNode.CONF_SOLR_HOST, "localhost:" + (EmbeddedJettyWithSolrServer.DEFAULT_PORT+1)
        ));
        testPaging(searchNode);

        searchNode.close();
    }

    // Invalid test at the SearchNodeAggregator is not responsible for adjusting start and max with
    // subsequent limiting of the search result
    public void testLocalNodeAggregatorPaging() throws RemoteException {
/*        SearchNodeAggregator aggregator = new SearchNodeAggregator(
                Configuration.newMemoryBased(),
                new ArrayList<SearchNode>(Arrays.asList(
                        new SolrSearchNode(Configuration.newMemoryBased(
                                SolrSearchNode.CONF_SOLR_HOST,
                                "localhost:" + (EmbeddedJettyWithSolrServer.DEFAULT_PORT+1))),
                        new SolrSearchNode(Configuration.newMemoryBased(
                                SolrSearchNode.CONF_SOLR_HOST,
                                "localhost:" + (EmbeddedJettyWithSolrServer.DEFAULT_PORT+2)))
                )));
        testPaging(aggregator);
        aggregator.close();*/
    }

    public void testLocalNodeAggregatorScoreOrder() throws RemoteException {
        SearchNodeAggregator aggregator = new SearchNodeAggregator(
                Configuration.newMemoryBased(),
                new ArrayList<SearchNode>(Arrays.asList(
                        new SolrSearchNode(Configuration.newMemoryBased(
                                SolrSearchNode.CONF_SOLR_HOST,
                                "localhost:" + (EmbeddedJettyWithSolrServer.DEFAULT_PORT+1))),
                        new SolrSearchNode(Configuration.newMemoryBased(
                                SolrSearchNode.CONF_SOLR_HOST,
                                "localhost:" + (EmbeddedJettyWithSolrServer.DEFAULT_PORT+2)))
                )));
        ResponseCollection responses = new ResponseCollection();
        aggregator.search(new Request(
                DocumentKeys.SEARCH_QUERY, "fulltext:ra"
        ), responses);
        assertTrue("There should be hits", ((DocumentResponse) responses.iterator().next()).getHitCount() > 0);
        assertOrdered("The score order should be descending for local aggregator", responses);
        aggregator.close();
    }

    private void assertOrdered(String message, ResponseCollection responses) {
        List<Double> scores = getScores(responses);
        List<Double> sortedScores = new ArrayList<>(scores);
        Collections.sort(sortedScores, new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return o2.compareTo(o1); // Reverse
            }
        });
        assertEquals(message, Strings.join(sortedScores), Strings.join(scores));
    }

    public void testLocalSingleSearcher() throws Exception {
        SummaSearcher searcher = getSearcher(1);
        testPaging(searcher, "fulltext:ra");
        searcher.close();
    }

    public void testLocalSearcherAggregator() throws Exception {
        Configuration aggConf = Configuration.newMemoryBased();
        List<Configuration> subConfs =  aggConf.createSubConfigurations(AdjustingSearcherAggregator.CONF_SEARCHERS, 2);
        for (int i = 0 ; i < subConfs.size() ; i++) {
            subConfs.get(i).set(SummaSearcherAggregator.CONF_SEARCHER_DESIGNATION, "searcher"+ (i+1));
            subConfs.get(i).set(ConnectionConsumer.CONF_RPC_TARGET, "//localhost:28000/searcher" + (i+1));
            subConfs.get(i).set(InteractionAdjuster.CONF_IDENTIFIER, "searcher" + (i+1));
        }
        try (SummaSearcher searcher1 = getSearcher(1); // Yes they are used!
             SummaSearcher searcher2 = getSearcher(2);
             AdjustingSearcherAggregator aggregator = new AdjustingSearcherAggregator(aggConf)) {
            testPaging(aggregator, "fulltext:ra NOT recordBase:backend1");
        }
    }

    private RMISearcherProxy getSearcher(int searcherID) throws IOException {
        SolrSearchNode searchNode = new SolrSearchNode(Configuration.newMemoryBased(
                SolrSearchNode.CONF_SOLR_HOST, "localhost:" + (EmbeddedJettyWithSolrServer.DEFAULT_PORT + searcherID)
        ));
        SummaSearcherImpl searcher = new SummaSearcherImpl(Configuration.newMemoryBased(), searchNode);
        return new RMISearcherProxy(Configuration.newMemoryBased(
                RMISearcherProxy.CONF_REGISTRY_PORT, 28000,
                RMISearcherProxy.CONF_SERVICE_NAME, "searcher" + searcherID
        ), searcher);
    }

    private void testPaging(SummaSearcher searcher, String query) throws IOException {
        final int PAGE = 3;
        assertPaging(PAGE,
                     getGroups(searcher, query, "group", 0, PAGE * 2),
                     getGroups(searcher, query, "group", 0, PAGE),
                     getGroups(searcher, query, "group", PAGE, PAGE)
        );
    }

    private void testPaging(SearchNode searchNode) throws RemoteException {
        final int PAGE = 3;

        assertPaging(PAGE,
                     getGroups(searchNode, "group", 0, PAGE * 2),
                     getGroups(searchNode, "group", 0, PAGE),
                     getGroups(searchNode, "group", PAGE, PAGE)
        );
    }

    private void assertPaging(int pageSize, List<String> groupsAll, List<String> groupsFirst, List<String> groupsSecond) {
        assertEquals("There should be the right number of all-groups", pageSize*2, groupsAll.size());
        ExtraAsserts.assertEquals("The first " + pageSize + " groups should match those from the all-group",
                                  groupsAll.subList(0, pageSize), groupsFirst);
        ExtraAsserts.assertEquals("The second " + pageSize + " groups should match those from the all-group",
                                  groupsAll.subList(pageSize, pageSize*2), groupsSecond);
    }

    private List<String> getGroups(
            SearchNode searchNode, String group, int start, int maxGroups) throws RemoteException {
        ResponseCollection responses = new ResponseCollection();
        searchNode.search(new Request(
                DocumentKeys.SEARCH_QUERY, "fulltext:ra",
                DocumentSearcher.GROUP, true,
                DocumentSearcher.GROUP_FIELD, group,
                DocumentSearcher.SEARCH_START_INDEX, start,
                DocumentSearcher.SEARCH_MAX_RECORDS, maxGroups
        ), responses);
        return getGroups(responses);
    }

    private List<String> getGroups(
            SummaSearcher searcher, String query, String group, int start, int maxGroups) throws IOException {
        ResponseCollection responses = searcher.search(new Request(
                DocumentKeys.SEARCH_QUERY, query,
                DocumentSearcher.GROUP, true,
                DocumentSearcher.GROUP_FIELD, group,
                DocumentSearcher.SEARCH_START_INDEX, start,
                DocumentSearcher.SEARCH_MAX_RECORDS, maxGroups
        ));
        return getGroups(responses);
    }

    private void basicIngest(int docs) throws IOException {
        List<String> groups = new ArrayList<>(docs /2);
        for (int i = 0 ; i < docs /2 ; i++) {
            groups.add("group_" + i);
        }
        ingest(0, "group", groups);
        ingest(1, "group", groups);
    }


    // Invalid test outside of SB
    public void disabledtestRemote() throws IOException {
        final String ADDRESS = "//mars:56800/mediehub-searcher";
        //final String ADDRESS = "//mars:57300/doms-searcher";
        //final String ADDRESS = "//mars:56700/aviser-searcher";
        SummaSearcher searcher = new SearchClient(Configuration.newMemoryBased(
                SearchClient.CONF_SERVER, ADDRESS));
        ResponseCollection responses = searcher.search(new Request(DocumentSearcher.SEARCH_QUERY, "hest"));
        assertFalse("There should be at least one response from " + ADDRESS, responses.isEmpty());

        List<String> groupsAll = getGroups(searcher.search(new Request(
                DocumentSearcher.SEARCH_QUERY, "hest",
                DocumentSearcher.GROUP, true,
                DocumentSearcher.GROUP_FIELD, "editionUUID",
                DocumentSearcher.SEARCH_MAX_RECORDS, 6
        )));
//        log.info("All\n" + Strings.join(groupsAll, "\n"));

        List<String> groupsFirst3 = getGroups(searcher.search(new Request(
                DocumentSearcher.SEARCH_QUERY, "hest",
                DocumentSearcher.GROUP, true,
                DocumentSearcher.GROUP_FIELD, "editionUUID",
                DocumentSearcher.SEARCH_START_INDEX, 0,
                DocumentSearcher.SEARCH_MAX_RECORDS, 3
        )));
//        log.info("First3\n" + Strings.join(groupsFirst3, "\n"));

        List<String> groupsSecond3 = getGroups(searcher.search(new Request(
                DocumentSearcher.SEARCH_QUERY, "hest",
                DocumentSearcher.GROUP, true,
                DocumentSearcher.GROUP_FIELD, "editionUUID",
                DocumentSearcher.SEARCH_START_INDEX, 3,
                DocumentSearcher.SEARCH_MAX_RECORDS, 3
        )));
//        log.info("Second3\n" + Strings.join(groupsSecond3, "\n"));

        assertEquals("All-group should contain the right number of groups", 6, groupsAll.size());
        ExtraAsserts.assertEquals("The first 3 groups should match those from the all-group",
                                  groupsAll.subList(0, 3), groupsFirst3.subList(0, 3));
        ExtraAsserts.assertEquals("The second 3 groups should match those from the all-group",
                                  groupsAll.subList(3, 6), groupsSecond3.subList(0, 3));

        searcher.close();
    }

    public void testRemoteConcreteProblem() throws IOException {
        final String ADDRESS = "//mars:56800/mediehub-searcher";
        //final String ADDRESS = "//mars:57300/doms-searcher";
        //final String ADDRESS = "//mars:56700/aviser-searcher";
        SummaSearcher searcher = new SearchClient(Configuration.newMemoryBased(
                SearchClient.CONF_SERVER, ADDRESS));
        ResponseCollection responses = searcher.search(new Request(DocumentSearcher.SEARCH_QUERY, "hest"));
        assertFalse("There should be at least one response from " + ADDRESS, responses.isEmpty());

        ResponseCollection responsesAll = searcher.search(getConcreteProblemBase(0, 20));
        List<String> groupsAll = getGroups(responsesAll);
        log.info("All\n" + Strings.join(groupsAll, "\n"));
        assertOrdered("Top-20 remote should be ordered", responsesAll);

        ResponseCollection responsesFirst = searcher.search(getConcreteProblemBase(0, 10));
        List<String> groupsFirst = getGroups(responsesFirst);
        log.info("Page 1\n" + Strings.join(groupsFirst, "\n"));
        assertOrdered("Top-10 remote should be ordered", responsesFirst);

        ResponseCollection responsesSecond = searcher.search(getConcreteProblemBase(10, 10));
        List<String> groupsSecond = getGroups(responsesSecond);
        log.info("Page 2\n" + Strings.join(groupsSecond, "\n"));
        assertOrdered("Page 2 @ 10 remote should be ordered", responsesSecond);

        assertEquals("All-group should contain the right number of groups", 20, groupsAll.size());
        ExtraAsserts.assertEquals("The first 10 groups should match those from the all-group",
                                  groupsAll.subList(0, 10), groupsFirst.subList(0, 10));
        ExtraAsserts.assertEquals("The second 10 groups should match those from the all-group",
                                  groupsAll.subList(10, 20), groupsSecond.subList(0, 10));

        searcher.close();
    }

    private Request getConcreteProblemBase(int start, int max) {
        return new Request(
                DocumentKeys.SEARCH_QUERY, "hest",
                DocumentKeys.SEARCH_START_INDEX, start,
                DocumentKeys.SEARCH_MAX_RECORDS, max,
                DocumentKeys.SEARCH_FILTER, "lma_long:avis",
                DocumentKeys.GROUP, true,
                DocumentKeys.GROUP_FIELD, "editionUUID",
                DocumentKeys.GROUP_LIMIT, 10
        );
    }

    private List<String> getGroups(ResponseCollection responses) {
        List<String> groups = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (Response response: responses) {
            if (response instanceof DocumentResponse) {
                for (DocumentResponse.Group group: ((DocumentResponse)response).getGroups()) {
                    sb.setLength(0);
                    for (DocumentResponse.Record record: group) {
                        if (sb.length() != 0) {
                            sb.append(", ");
                        }
                        sb.append(Float.toString(record.getScore())).
                                append("(").append(record.getId()).append(")");
                    }
                    groups.add(
                            group.getGroupValue() + "(" + group.getNumFound() + ": " + sb.toString() + ")");
                }
            }
        }
        return groups;
    }

    private List<Double> getScores(ResponseCollection responses) {
        List<Double> scores = new ArrayList<>();
        for (Response response: responses) {
            if (response instanceof DocumentResponse) {
                DocumentResponse docs = (DocumentResponse)response;
                if (docs.isGrouped()) {
                    for (DocumentResponse.Group group: docs.getGroups()) {
                        scores.add((double)group.get(0).getScore());
                    }
                } else {
                    for (DocumentResponse.Record record: docs.getRecords()) {
                        scores.add((double)record.getScore());
                    }
                }
            }
        }
        return scores;
    }

}
