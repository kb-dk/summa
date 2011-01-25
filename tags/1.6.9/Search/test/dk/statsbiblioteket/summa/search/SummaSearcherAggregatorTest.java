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
package dk.statsbiblioteket.summa.search;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.summa.search.rmi.RMISearcherProxy;
import dk.statsbiblioteket.summa.search.dummy.SummaSearcherDummy;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummaSearcherAggregatorTest extends TestCase {
    private static Log log =
            LogFactory.getLog(SummaSearcherAggregatorTest.class);

    public SummaSearcherAggregatorTest(String name) {
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
        return new TestSuite(SummaSearcherAggregatorTest.class);
    }

    private static final String[] SHARDS = {"shard1", "shard2"};

    /**
     * Sets up two separate summaSearchers, tests that they work on their own,
     * then createa an aggregator and tests if it aggregates properly.
     * @throws Exception if the test failed.
     */
    public void testMultiple() throws Exception {
        List<SummaSearcher> searchers = getSearchers();

        int counter = 0;
        for (String shard: SHARDS) {
            Request request = new Request();
            request.put("zoo", "baz" + counter);
            String result = searchers.get(counter++).search(request).toXML();
            assertTrue("Searcher for shard '" + shard
                       + "' should work. Got result " + result,
                       result.contains("<ids>" + shard + "</ids>"));
        }
        closeSearchers(searchers);
    }

    private List<SummaSearcher> getSearchers() throws IOException {
        List<SummaSearcher> searchers =
                new ArrayList<SummaSearcher>(SHARDS.length);
        for (String shard: SHARDS) {
            searchers.add(createSearcher(shard));
        }
        return searchers;
    }

    public void testAggregator() throws IOException {
        List<SummaSearcher> searchers = getSearchers();
        SummaSearcher aggregator = createAggregator(SHARDS);
        Request request = new Request();
        request.put("zoo", "baz");
        String result = aggregator.search(request).toXML();
        log.info("The result from the combined search was:\n" + result);
        String[] actual = result.substring(result.lastIndexOf("<ids>") + 5,
                                               result.lastIndexOf("</ids>")).
                split(", ");
        Arrays.sort(actual);
        String[] expected = Arrays.copyOf(SHARDS, SHARDS.length);
        Arrays.sort(expected);
        assertEquals("The result should contain elements from all shards",
                     Strings.join(expected, ", "),
                     Strings.join(actual, ", "));
        closeSearchers(searchers);
        aggregator.close();
    }

    private void closeSearchers(List<SummaSearcher> searchers) throws
                                                               IOException {
        for (SummaSearcher searcher : searchers) {
            searcher.close();
        }
    }

    private SummaSearcher createAggregator(String[] shards) throws IOException {
        Configuration conf = Configuration.newMemoryBased();
        List<Configuration> connections = conf.createSubConfigurations(
                SummaSearcherAggregator.CONF_SEARCHERS, shards.length);
        for (int shardNumber = 0 ; shardNumber < shards.length ; shardNumber++){
            connections.get(shardNumber).set(
                    SearchClient.CONF_RPC_TARGET,
                    "//localhost:28000/" + shards[shardNumber]);
        }
        return new SummaSearcherAggregator(conf);
    }

    private SummaSearcher createSearcher(String id) throws IOException {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(RMISearcherProxy.CONF_BACKEND,
                 SummaSearcherDummy.class.getCanonicalName());
        conf.set(RMISearcherProxy.CONF_SERVICE_NAME, id);
        conf.set(SummaSearcherDummy.CONF_ID, id);
        return new RMISearcherProxy(conf);
    }
}

