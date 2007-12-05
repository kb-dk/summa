/* $Id: ClusterMapCompleteArrayTest.java,v 1.5 2007/10/04 13:28:17 te Exp $
 * $Revision: 1.5 $
 * $Date: 2007/10/04 13:28:17 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
/*
 * ClusterMapCompleteArray Tester.
 *
 * CVS:  $Id: ClusterMapCompleteArrayTest.java,v 1.5 2007/10/04 13:28:17 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.core;

import java.util.Random;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.summa.facetbrowser.util.ClusterCommon;
import dk.statsbiblioteket.summa.facetbrowser.command.ClustermapTest;
import org.apache.log4j.Logger;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class ClusterMapCompleteArrayTest extends TestCase {
    private static Logger log = Logger.getLogger(ClusterMapCompleteArrayTest.class);

    public ClusterMapCompleteArrayTest(String name) {
        super(name);
    }

    private Integer[] intArray;

    public void setUp() throws Exception {
        super.setUp();
        intArray = new Integer[5];
        for (int i = 0 ; i < 5; i++){
            intArray[i] = 1;
        }
    }
    protected Integer[] getArray() {
        for (int i = 0 ; i < 5; i++){
            intArray[i] += 1;
        }
        return intArray;
    }
    public void testForLoop() {
        for (int i: getArray()) {
            System.out.print(i + " ");
        }
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testLimitedSpeed() throws Exception {
        testGetFirstX(2000);
    }

    public void testLimitedSpeedFull() throws Exception {
        testGetFirstX(Integer.MAX_VALUE);
    }

/*    public void testStructure() throws Exception {
        int limit = 200;
        ClusterMapCompleteArray raw = new ClusterMapCompleteArray(limit);
        raw.fillMap();
        raw.logFacetNames();

        for (SortedHash<String> expandedFacet: raw.expandedFacets) {
            for (int i = 0 ; i < expandedFacet.size() ; i++) {
                assertTrue("Lookup should give id in the array",
                           expandedFacet.get(expandedFacet.get(i)) <
                           expandedFacet.size());
            }
        }

       for (int facetID = 0 ; facetID < raw.expandedFacets.size() ; facetID++) {
            for (int docID = 0 ; docID < limit ; docID++) {
                for (int lookupID: raw.map.get(docID, facetID)) {
                    try {
                        assertTrue("TagID should be inside limits",
                                   raw.expandedFacets.get(facetID).get(lookupID) != null);
                    } catch (Exception e) {
                        System.out.println(facetID + " " +
                                           raw.facetNames.get(facetID) + " " +
                                           docID + " " + lookupID + " failed");
                    }
                }
            }

        }

    }*/

    public void testPrint() throws Exception {
        ClusterMapComplete map = new ClusterMapCompleteArray(200);
        map.fillMap();
        map.setLimits(20, 10, 10);
//        int[] hits = getRandomHits(500, 200);
        System.out.println("\n\n");
//        System.out.println(map.resultToString(
//            map.getFirstX(hits, ClusterMapCompleteArray.TagSortOrder.ALPHA)));
        System.out.println("\n\n");
//        System.out.println(map.resultToString(
//            map.getFirstX(hits, ClusterMapCompleteArray.TagSortOrder.POPULARITY)));
    }

    public int[] getRandomHits(int hitCount, int maxDocID) {
        Random random = new Random();
        int[] result = new int[hitCount];
        for (int i = 0 ; i < hitCount ; i++) {
            result[i] = random.nextInt(maxDocID-1);
        }
        return result;
    }

    public void testThreadEquility() throws IOException, ClusterException {
        int limit = 200;
        ClusterMapCompleteArray map = new ClusterMapCompleteArray(limit);
        map.fillMap();
        map.setLimits(20, 10, 10);
        log.info("Filled Map");
        int[] hits = getRandomHits(500, limit);
        String res1 = map.resultToString(
            map.getFirstXThreaded(hits, ClusterMapComplete.SortOrder.ALPHA, 1));
        String res2 = map.resultToString(
            map.getFirstXThreaded(hits, ClusterMapComplete.SortOrder.ALPHA, 2));
        String res3 = map.resultToString(
            map.getFirstXThreaded(hits, ClusterMapComplete.SortOrder.ALPHA, 3));
        assertEquals("The number of threads (1, 2) shouldn't affect the result",
                     res1, res2);
        assertEquals("The number of threads (2, 3) shouldn't affect the result",
                     res2, res3);

    }

    public void prepareDumpFull() throws IOException, ClusterException {
        prepareDump(Integer.MAX_VALUE);
    }

    public void prepareDump100K() throws IOException, ClusterException {
        prepareDump(1000000);
    }

    public void prepareDump() throws IOException, ClusterException {
        prepareDump(200);
    }

    public void prepareDump(int limit) throws IOException, ClusterException {
        ClusterMapCompleteArray map = new ClusterMapCompleteArray(limit);
        map.fillMap();
        map.storeMap();
        log.info("Map created and stored");
    }

    public void dumpResult() throws IOException, ClusterException {
        int limit = 2500000;
        ClusterMapCompleteArray map = new ClusterMapCompleteArray(limit);
        map.loadMap();
        map.setLimits(20, 10, 10);
        int[] hits = getRandomHits(50000, limit);
        System.out.println("\nTesting\n\n");
        System.out.println(map.resultToXML(
                map.getFirstXThreaded(hits,
                                      ClusterMapComplete.SortOrder.ALPHA, 1)));
        System.out.println("\n\n");
        Profiler pf = new Profiler();
        System.out.println(map.resultToXML(
                map.getFirstXThreaded(hits,
                                      ClusterMapComplete.SortOrder.ALPHA, 2)));
        System.out.println("Searched in " + pf.getSpendTime());

    }

    public void dumpResultPop() throws IOException, ClusterException {
        int limit = 200;
        ClusterMapCompleteArray map = new ClusterMapCompleteArray(limit);
        map.loadMap();
        map.setLimits(20, 10, 10);
        int[] hits = getRandomHits(500, limit);
        System.out.println("\nTesting\n\n");
        map.resultToXML(
                map.getFirstXThreaded(hits,
                                   ClusterMapComplete.SortOrder.POPULARITY, 1));
        System.out.println("\n\n");
        Profiler pf = new Profiler();
        System.out.println(map.resultToString(
                map.getFirstXThreaded(hits,
                                  ClusterMapComplete.SortOrder.POPULARITY, 2)));
        System.out.println("Searched in " + pf.getSpendTime());

    }

    public void dumpTiming() throws IOException, ClusterException {
        int limit = 200;
        ClusterMapCompleteArray map = new ClusterMapCompleteArray(limit);
        map.loadMap();
        map.setLimits(20, 10, 10);
        int[] hits = getRandomHits(500, limit);
        System.gc();
        Profiler pf = new Profiler();
        map.getFirstXThreaded(hits, ClusterMapComplete.SortOrder.ALPHA, 1);
        System.out.println("Performed getFirstXThreaded 1 in " +
                           pf.getSpendTime());

        hits = getRandomHits(500, limit);
        pf.reset();
        map.getFirstXThreaded(hits, ClusterMapComplete.SortOrder.ALPHA, 1);
        System.out.println("Performed getFirstXThreaded 2 in " +
                           pf.getSpendTime());

        hits = getRandomHits(500, limit);
        System.gc();
        System.out.println("\n\n");
        pf = new Profiler();
        map.resultToXML(
            map.getFirstXThreaded(hits, ClusterMapComplete.SortOrder.ALPHA, 2));
        System.out.println("With XML in " + pf.getSpendTime());
    }
        /*
    public void dumpSearchTiming() throws Exception {
        System.out.println(CompleteClusterEngineServer.getInstance().
                getClusters("Petersen", Integer.MAX_VALUE / 2 - 1,
                            10000000,
                            20, -1,
                            ClusterMapCompleteArray.TagSortOrder.ALPHA));

    }
          */

    public void dumpFeedback() throws IOException, ClusterException {
        ClusterMapCompleteArray raw = new ClusterMapCompleteArray(10000);
        raw.fillMap();
        System.out.println(raw.getStats());
        System.gc();
        //noinspection DuplicateStringLiteralInspection
        System.out.println("Memory usage incl. Strings: "
                           + ClusterCommon.getMem());
        //noinspection AssignmentToNull
        raw.expandedFacets = null;
        System.gc();
        System.out.println("Memory usage excl. Strings: "
                           + ClusterCommon.getMem());
    }

    public void testGetFirstX(int limit) throws Exception {
        new ClustermapTest().testGetFirstX(limit, Integer.MAX_VALUE, false);
    }

    public static Test suite() {
        return new TestSuite(ClusterMapCompleteArrayTest.class);
    }
}
