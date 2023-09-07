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
package dk.statsbiblioteket.summa.support.solr;

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
import dk.statsbiblioteket.summa.support.alto.AltoBoxResponse;
import dk.statsbiblioteket.summa.support.alto.AltoBoxSearcher;
import dk.statsbiblioteket.util.Strings;
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
import java.util.stream.Collectors;

/**
 * A very poor test as it relies on instances running at Statsbiblioteket's test machine.
 */
public class Solr9SearcherTest extends TestCase {
    private static Log log = LogFactory.getLog(Solr9SearcherTest.class);


    // Tests the SolrSearchNode.solr9QueryHack
    public void disabledtestStageSolr9() throws IOException {
        final String query = "(+\"vestindien\")";
        SummaSearcher searcher = getSolr9ShadowProdSearcher();
        if (searcher == null) {
            return;
        }

        ResponseCollection responses = searcher.search(new Request(
                DocumentKeys.SEARCH_QUERY, query,
                DocumentKeys.SEARCH_RESULT_FIELDS, "recordID",
                "solrparam.op", "AND" // Will always be AND due to Solr setup
        ));

        DocumentResponse dr = (DocumentResponse)responses.iterator().next();
        assertTrue("There should be at least one hit for '" + query + "'", dr.getHitCount() >= 1);
    }


    private SummaSearcher getSolr9ShadowProdSearcher() throws IOException {
        // http://rhea:50006/solr/#/aviser.1.prod/query?q=strudsehest&q.op=OR&indent=true

        // ssh -L 127.0.0.1:50100:rhea.statsbiblioteket.dk:50100 -L 127.0.0.1:50110:rhea.statsbiblioteket.dk:50110 develro@rhea.statsbiblioteket.dk
        return getFullStackWithTest("rhea.statsbiblioteket.dk", 50006, "/solr/aviser.1.prod/select",
                                    "localhost", 50100, "aviser-storage");
//        return getFullStackWithTest("rhea", 50001, "/solr/aviser.1.stage/select",
//                                    "rhea", 56700, "aviser-storage");
    }
    private SummaSearcher getSolr9MixServer() throws IOException {
        // http://rhea:50006/solr/#/aviser.1.prod/query?q=strudsehest&q.op=OR&indent=true
        return getFullStackWithTest("rhea.statsbiblioteket.dk", 50006, "/solr/aviser.1.prod/select",
                                    "rhea.statsbiblioteket.dk", 56700, "aviser-storage");
    }
    private SummaSearcher getSolr9LocalSearcher() throws IOException {
        // http://localhost:50001/solr/aviser.2.devel/select?indent=true&q.op=OR&q=falk%20daniel
        return getFullStackWithTest("localhost", 50001, "/solr/aviser.2.devel/select",
                                    "localhost", 56700, "aviser-storage");
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
