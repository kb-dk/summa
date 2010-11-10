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
package dk.statsbiblioteket.summa.facetbrowser.core.map;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.facetbrowser.BaseObjects;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.util.Arrays;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CoreMapBuilderTest extends TestCase {
    private static Log log = LogFactory.getLog(CoreMapBuilderTest.class);

    public CoreMapBuilderTest(String name) {
        super(name);
    }

    BaseObjects bo;
    CoreMapBuilder builder;
    @Override
    public void setUp() throws Exception {
        super.setUp();
        bo = new BaseObjects();
        builder = new CoreMapBuilder(
                Configuration.newMemoryBased(), bo.getStructure());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        bo.close();
    }

    public static Test suite() {
        return new TestSuite(CoreMapBuilderTest.class);
    }

    public void testAddition() throws Exception {
        CoreMapBitStuffedTest.testAddition(builder);

    }

    public void testAddition2() throws Exception {
        CoreMapBitStuffedTest.testAddition2(builder);
    }

    public void speedTestMonkey() throws Exception {
        int[] RUNS = {1000, 10000, 100000, 1000000, 10000000};
        CoreMap map = new CoreMapBuilder(
                Configuration.newMemoryBased(), bo.getStructure());
        for (int runs: RUNS) {
            testMonkey(runs, map);
        }
    }

    public void testMonkey(int runs, CoreMap map) throws Exception {
        CoreMapBitStuffedTest.testMonkey(
                runs, bo.getStructure().getFacets().size(), map);
    }

    public void testMonkeyValid() throws Exception {
        testMonkeyValid(10);
    }

    public void testMonkeyValid1000() throws Exception {
        testMonkeyValid(1000);
    }

    public void testMonkeyValid50000() throws Exception {
        testMonkeyValid(50000);
    }
    /*
     * Performs a monkey-test for a CoreMapBuilder and a CoreMapBitStuffed and
     * compares the results.
     */
    public void testMonkeyValid(int runs) throws Exception {
        CoreMapBuilder builderMap = new CoreMapBuilder(
                Configuration.newMemoryBased(), bo.getStructure());
        testMonkey(runs, builderMap);
        CoreMap bitMap = bo.getCoreMap();
        testMonkey(runs, bitMap);

        assertEquals("Builder vs. bit", builderMap, bitMap);
    }

    /*
     * Performs a monkey-test for a CoreMapBuilder and a CoreMapBitStuffed and
     * compares the results.
     */
    public void testCopyto() throws Exception {
        int runs = 100000;
        CoreMapBuilder expected = new CoreMapBuilder(
                Configuration.newMemoryBased(), bo.getStructure());
        testMonkey(runs, expected);
        CoreMap actual = bo.getCoreMap();
        long startTime = System.currentTimeMillis();
        expected.copyTo(actual);
        log.info("copyTo of " + runs + " runs finished in " 
                 + (System.currentTimeMillis() - startTime) + " ms");

        assertEquals("copyTo-bit vs. original-builder", expected, actual);
    }

    private void assertEquals(String message, CoreMap expected, CoreMap actual)
            throws IOException {
        assertEquals(message
                     + ". The highest document ID in the maps should match",
                     expected.getDocCount(), actual.getDocCount());
        for (int docID = 0 ; docID < expected.getDocCount() ; docID++) {
            for (String facet: bo.getStructure().getFacetNames()) {
                int facetID = bo.getStructure().getFacetID(facet);
                //noinspection DuplicateStringLiteralInspection
                assertEquals(message + ". The tags for doc " + docID
                             + " and facet '" + facet + "' should match",
                             Arrays.toString(actual.get(docID, facetID)),
                             Arrays.toString(expected.get(docID, facetID)));
            }
        }
    }
}

