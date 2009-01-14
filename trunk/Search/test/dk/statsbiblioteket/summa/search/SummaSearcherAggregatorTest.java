/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.search;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.summa.search.rmi.RMISearcherProxy;
import dk.statsbiblioteket.summa.search.dummy.SummaSearcherDummy;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummaSearcherAggregatorTest extends TestCase {
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
        for (SummaSearcher searcher : searchers) {
            searcher.close();
        }
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
    }

    private SummaSearcher createAggregator(String[] shards) throws IOException {
        Configuration conf = Configuration.newMemoryBased();
        List<Configuration> connections = conf.createSubConfigurations(
                SummaSearcherAggregator.CONF_SEARCHERS, shards.length);
        for (int shardNumber = 0 ; shardNumber < shards.length ; shardNumber++){
            //connections.get(shardNumber).set(SearchClient.CONF_RPC_TARGET());
        }
        // TODO: Implement this
        return null;
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
