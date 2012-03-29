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
package dk.statsbiblioteket.summa.facetbrowser.util;

import java.util.LinkedList;
import java.util.Random;
import java.util.Collections;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.util.FlexiblePair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * FlexiblePair Tester.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FlexiblePairTest extends TestCase {
    private Log log = LogFactory.getLog(FlexiblePairTest.class);

    public FlexiblePairTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testPerformance() throws Exception {
        int size = 100000;
        Random random = new Random();
        Profiler pf = new Profiler();
        LinkedList<FlexiblePair<String, Integer>> alphaResult =
                new LinkedList<FlexiblePair<String, Integer>>();
        for (int i = 0 ; i < size ; i++) {
            alphaResult.add(new FlexiblePair<String, Integer>(
                    Integer.toString(random.nextInt(10000000)),
                    random.nextInt(10000000),
                    FlexiblePair.SortType.PRIMARY_ASCENDING));
        }
        log.info("Finished adding " + size + " pairs " +
                           "in " + pf.getSpendTime());
        pf.reset();
        Collections.sort(alphaResult);
        log.info("Sorted in " + pf.getSpendTime());
    }

    public static Test suite() {
        return new TestSuite(FlexiblePairTest.class);
    }
}




