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
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.support.solr.SBSolrSearchNode;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.List;

/**
 * A very poor test as it relies on instances running at Statsbiblioteket's test machine.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AltoBoxSearcherTest extends TestCase {
    private static Log log = LogFactory.getLog(AltoBoxSearcherTest.class);

    public void testFullPackage() throws IOException {
        SummaSearcher searcher = getAvailableSearcher();
        if (searcher == null) {
            return;
        }
        ResponseCollection responses = getFullStack().search(new Request(
                DocumentKeys.SEARCH_QUERY, "hest",
                AltoBoxSearcher.CONF_BOX, true
        ));
        System.out.println(responses.toXML());
    }

    private SummaSearcher getAvailableSearcher() throws IOException {
        SummaSearcher searcher = getFullStack();
        try {
            searcher.search(new Request(DocumentKeys.SEARCH_QUERY, "ddsdffsfss"));
        } catch (IOException e) {
            log.warn("This test only runs on Statsbiblioteket, sorry");
            return null;
        }
        return searcher;
    }

    private SummaSearcherImpl getFullStack() throws IOException {
        Configuration searcherConf = Configuration.newMemoryBased(
                SummaSearcherImpl.CONF_USE_LOCAL_INDEX, false
        );
        searcherConf.set(SearchNodeFactory.CONF_NODE_CLASS, SearchNodeAggregator.class.getCanonicalName());
        searcherConf.set(SearchNodeAggregator.CONF_SEQUENTIAL, true);
        List<Configuration> nodeConfs = searcherConf.createSubConfigurations(SearchNodeFactory.CONF_NODES, 2);

        nodeConfs.get(0).set(SearchNodeFactory.CONF_NODE_CLASS, SBSolrSearchNode.class.getCanonicalName());
        nodeConfs.get(0).set(SBSolrSearchNode.CONF_SOLR_HOST, "mars:57008");
        nodeConfs.get(0).set(SBSolrSearchNode.CONF_SOLR_RESTCALL, "/sb/sbsolr/select");
        nodeConfs.get(0).set(SBSolrSearchNode.CONF_SOLR_CONNECTION_TIMEOUT, 500);
        nodeConfs.get(0).set(SBSolrSearchNode.CONF_SOLR_READ_TIMEOUT, 5000);

        nodeConfs.get(1).set(SearchNodeFactory.CONF_NODE_CLASS, AltoBoxSearcher.class.getCanonicalName());
        nodeConfs.get(1).set(ConnectionConsumer.CONF_RPC_TARGET, "//mars:57008/sb-storage");
        return new SummaSearcherImpl(searcherConf);
    }
}
