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
package dk.statsbiblioteket.summa.facetbrowser.browse;

import dk.statsbiblioteket.summa.facetbrowser.BaseObjects;
import dk.statsbiblioteket.summa.facetbrowser.IndexBuilder;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResult;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.index.IndexReader;
import org.apache.log4j.Logger;

import java.util.Random;
import java.io.IOException;

/**
 * TagCounterArray Tester.
 *
 * @author <Authors name>
 * @since <pre>03/22/2007</pre>
 * @version 1.0
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class TagCounterTest extends TestCase {
    private static Logger log = Logger.getLogger(TagCounterTest.class);

    IndexReader reader;

    public TagCounterTest(String name) {
        super(name);
    }

    BaseObjects bo;

    public void setUp() throws Exception {
        super.setUp();
        bo = new BaseObjects();
    }

    public void setupSample() throws Exception {
        IndexBuilder.checkIndex();
        reader = IndexBuilder.getReader();
    }

    @SuppressWarnings({"AssignmentToNull"})
    public void tearDown() throws Exception {
        super.tearDown();
        bo.close();
        reader = null;
    }

    public void testGetFirst() throws Exception {
        setupSample();
        TagCounter tc = bo.getTagCounter();
        tc.verify();
        for (int facetID = 0 ; facetID < bo.facetNames.length ; facetID++) {
            for (int tagID = 0 ;
                 tagID < bo.getTagHandler().getFacetSize(facetID) ;
                 tagID++) {
                tc.increment(facetID, tagID);
            }
        }
        //throw new UnsupportedOperationException("Update this to new API");
        FacetResult first = tc.getFirst(bo.getStructure());
        log.info(first.toXML());
        assertTrue("The result should be something",
                   !"".equals(first.toXML()));
    }

    public void dumpPerformance() throws Exception {
        setupSample();
        System.out.println("Testing fill performance");
        int retries = 20;
        int COMPLETERUNS = 50;
        int updates = COMPLETERUNS * bo.getTagHandler().getTagCount();
        System.out.println("Warming up...");
        for (int i = 0 ; i < 10 ; i++) {
            fill(10);
        }
        bo.getTagCounter().reset();
        System.out.println("Running...");
        Profiler profiler = new Profiler();
        for (int i = 0 ; i < retries ; i++) {
            fill(COMPLETERUNS);
            profiler.beat();
        }
        double speed = 1000 / profiler.getBps(true);
        System.out.println("Average speed: " + speed
                           + " ms for " + COMPLETERUNS + " complete fills of "
                           + bo.getTagHandler().getTagCount() + " tags ("
                           + updates / speed + " updates/ms)");
    }

    private void fill(int completeRuns) throws IOException {
        for (int complete = 0 ; complete < completeRuns ; complete++) {
            for (int facetID = 0 ; facetID < bo.facetNames.length ; facetID++) {
                for (int tagID = 0 ;
                     tagID < bo.getTagHandler().getFacetSize(facetID) ;
                     tagID++) {
                    bo.getTagCounter().increment(facetID, tagID);
                }
            }
        }
    }

    public void dumpGetFirstPerformance() throws Exception {
        setupSample();
        dumpPerformance();
        int retries = 2000;

        System.out.println("Testing getFirst performance for POPULARITY");
/*        Profiler profiler = new Profiler();
        FacetRequest popularityOrderRequest = new FacetRequest(
                null, 0, 0, Strings.join(bo.getFacetNames(), " (POPULARITY), "),
                bo.getStructure());
        for (int i = 0 ; i < retries ; i++) {
            bo.getTagCounter().getFirst(popularityOrderRequest);
            profiler.beat();
        }
        double speed = 1000 / profiler.getBps(true);
        System.out.println("Average speed: " + speed
                           + " ms for POPULARITY getFirst in "
                           + bo.getTagHandler().getTagCount() + " touched tags ("
                           + 1000 / speed + " fills/ms)");

        System.out.println("Testing getFirst performance for ALPHA");
        profiler = new Profiler();
        FacetRequest tagOrderRequest = new FacetRequest(
                null, 0, 0, Strings.join(bo.getFacetNames(), " (ALPHA), "),
                bo.getStructure());
        for (int i = 0 ; i < retries * 100; i++) {
            bo.getTagCounter().getFirst(tagOrderRequest);
            profiler.beat();
        }
        speed = 1000 / profiler.getBps(true);
        System.out.println("Average speed: " + speed
                           + " ms for ALPHA getFirst in "
                           + bo.getTagHandler().getTagCount() + " touched tags ("
                           + 1000 / speed + " fills/ms)");
  */  }

    public void dumpBigSpeed() throws Exception {
        System.out.println("Building bo.getTagHandler()...");
        int tagcount = 2000000;
        int runs = 5;
        int maxTagCount = 100;
        int chanceForHit = 50;

        Random random = new Random();
        String[][] tags = new String[bo.facetNames.length][];
        int facetPos = 0;
        for (String f: bo.facetNames) {
            tags[facetPos] = new String[tagcount];
            for (int i = 0 ; i < tagcount ; i++) {
                tags[facetPos][i] = Integer.toString(random.nextInt(1000000));
            }
            facetPos++;
        }

        TagCounter counter = bo.getTagCounter();

        int markedTags = 0;
        System.out.println("Filling bo.getTagCounter()...");
        for (int facetID = 0 ; facetID < bo.facetNames.length ; facetID++) {
            for (int tagID = 0 ; tagID < tagcount ; tagID++) {
                if (random.nextInt(100) < chanceForHit) {
                    markedTags++;
                    int hitcount = random.nextInt(maxTagCount);
                    for (int i = 0 ; i < hitcount ; i++) {
                        counter.increment(facetID, tagID);
                    }
                }
            }
        }
        System.out.println("Warming up bo.getTagCounter()...");
        for (int i = 0 ; i < 5 ; i++) {
            counter.getFirst(bo.getStructure());
        }
        System.out.println("Running getFirst tests...");
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(runs);
        for (int i = 0 ; i < runs ; i++) {
            counter.getFirst(null);
            profiler.beat();
        }
        double bps = profiler.getBps(true);
        System.out.println("Extracted first from " + markedTags
                           + " marked tags in "
                           + bo.facetNames.length + " facets "
                           + bps + " times/second, "
                           + 1000 / bps + " ms/extraction");
    }

    public static Test suite() {
        return new TestSuite(TagCounterTest.class);
    }
}




