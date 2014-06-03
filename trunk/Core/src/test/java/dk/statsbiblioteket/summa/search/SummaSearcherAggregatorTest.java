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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.dummy.SummaSearcherDummy;
import dk.statsbiblioteket.summa.search.rmi.RMISearcherProxy;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.*;
import org.apache.lucene.util.InPlaceMergeSorter;
import org.apache.lucene.util.PriorityQueue;

import java.io.IOException;
import java.util.*;

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

    public void testTopFieldCollectorPerformance() throws IOException {
        final int[] TOPS = new int[]{
                1_000_000, 10_000_000, 20_000_000, 40_000_000, 60_000_000};
        final SortField[] FIELDS =
                new SortField[]{SortField.FIELD_SCORE};

        for (int top: TOPS) {
            long collectAndSortTime = -System.nanoTime();
            PriorityQueue<ScoreDoc> queue = new PriorityQueue<ScoreDoc>(top) {
                @Override
                protected ScoreDoc getSentinelObject() {
                  // Always set the doc Id to MAX_VALUE so that it won't be favored by
                  // lessThan. This generally should not happen since if score is not NEG_INF,
                  // TopScoreDocCollector will always add the object to the queue.
                  return new ScoreDoc(Integer.MAX_VALUE, Float.NEGATIVE_INFINITY);
                }

                @Override
                protected final boolean lessThan(ScoreDoc hitA, ScoreDoc hitB) {
                  if (hitA.score == hitB.score)
                    return hitA.doc > hitB.doc;
                  else
                    return hitA.score < hitB.score;
                }
            };

            Random r = new Random(87);
            for (int i = 0 ; i < top ; i++) {
                queue.insertWithOverflow(new ScoreDoc(i, r.nextFloat()));
            }
            while (queue.size() > 0) {
                queue.pop();
            }
            collectAndSortTime += System.nanoTime();

            long randomTime = -System.nanoTime();
            r = new Random(87);
            for (int i = 0 ; i < top ; i++) {
                if (r.nextFloat() == 0.1f && i == 0) {
                    throw new RuntimeException("Never thrown");
                }
            }
            randomTime += System.nanoTime();

            System.out.println(String.format(
                    "Queue size %,10d entries in %,7d ms",
                    top, (collectAndSortTime - randomTime) / 1000000));
        }
    }

    public void testMergeSortPerformance() throws IOException {
        final int[] TOPS = new int[]{
                1_000_000, 10_000_000, 20_000_000, 40_000_000, 60_000_000};
        for (int top: TOPS) {
            long collectAndSortTime = -System.nanoTime();
            final int[] docs = new int[top];
            final float[] scores = new float[top];
            Random r = new Random(87);
            for (int i = 0 ; i < top ; i++) {
                docs[i] = i;
                scores[i] = r.nextFloat();
            }
            new InPlaceMergeSorter() {
                @Override
                protected int compare(int i, int j) {
                    return scores[i] == scores[j] ? 0 :
                            scores[i] < scores[j] ? -1 : 1;
                }

                @Override
                protected void swap(int i, int j) {
                    float ts = scores[j];
                    scores[j] = scores[i];
                    scores[i] = ts;
                    int td = docs[j];
                    docs[j] = docs[i];
                    docs[i] = td;
                }
            }.sort(0, top);
            for (int i = 0 ; i < top ; i++) {
                if (docs[i] == 1 && scores[i] == 0.1f) {
                    throw new RuntimeException("Never thrown");
                }
            }
            collectAndSortTime += System.nanoTime();

            long randomTime = -System.nanoTime();
            r = new Random(87);
            for (int i = 0 ; i < top ; i++) {
                if (r.nextFloat() == 0.1f && i == 0) {
                    throw new RuntimeException("Never thrown");
                }
            }
            randomTime += System.nanoTime();

            System.out.println(String.format(
                    "Direct arrays size %,10d entries in %,7d ms",
                    top, (collectAndSortTime-randomTime)/1000000));
        }
    }

    public void testMergeSortPerformance2() throws IOException {
        final int[] TOPS = new int[]{
                1_000_000, 10_000_000, 20_000_000, 40_000_000, 60_000_000};
        for (int top: TOPS) {
            long collectAndSortTime = -System.nanoTime();
            final ScoreDoc[] docs = new ScoreDoc[top];
            Random r = new Random(87);
            for (int i = 0 ; i < top ; i++) {
                docs[i] = new ScoreDoc(i, r.nextFloat());
            }
            Arrays.sort(docs, new Comparator<ScoreDoc>() {
                @Override
                public int compare(ScoreDoc hitA, ScoreDoc hitB) {
                    if (hitA.score == hitB.score) {
                      return hitB.doc - hitA.doc;
                    }
                    return hitA.score < hitB.score ? -1 : 1;
                  }
            });
            for (int i = 0 ; i < top ; i++) {
                if (docs[i].doc == 1 && docs[i].score == 0.1f) {
                    throw new RuntimeException("Never thrown");
                }
            }
            collectAndSortTime += System.nanoTime();

            long randomTime = -System.nanoTime();
            r = new Random(87);
            for (int i = 0 ; i < top ; i++) {
                if (r.nextFloat() == 0.1f && i == 0) {
                    throw new RuntimeException("Never thrown");
                }
            }
            randomTime += System.nanoTime();

            System.out.println(String.format(
                    "ScoreDoc arrays size %,10d entries in %,7d ms",
                    top, (collectAndSortTime-randomTime)/1000000));
        }
    }

    private List<SummaSearcher> getSearchers() throws IOException {
        List<SummaSearcher> searchers = new ArrayList<>(SHARDS.length);
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

