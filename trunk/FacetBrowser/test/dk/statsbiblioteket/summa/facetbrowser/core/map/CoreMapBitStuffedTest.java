/* $Id: CoreMapBitStuffedTest.java,v 1.6 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.6 $
 * $Date: 2007/10/04 13:28:21 $
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
package dk.statsbiblioteket.summa.facetbrowser.core.map;

import java.util.Arrays;
import java.util.Random;
import java.io.StringWriter;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.facetbrowser.BaseObjects;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class CoreMapBitStuffedTest extends TestCase {
    private static Logger log = Logger.getLogger(CoreMapBitStuffedTest.class);

    public CoreMapBitStuffedTest(String name) {
        super(name);
    }

    BaseObjects bo;

    public void setUp() throws Exception {
        super.setUp();
        bo = new BaseObjects();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        bo.close();
    }

    public void testExpansion() throws Exception {
        CoreMap map = bo.getCoreMap();
        assertEquals("The map should contain no elements to start with",
                     0, map.getDocCount());
        map.add(0, 0, new int[]{12, 23, 34});
        assertEquals("The map should contain 1 element",
                     1, map.getDocCount());
        map.add(1, 1, new int[]{12, 34});
        assertEquals("The map should contain 2 elements",
                     2, map.getDocCount());
        map.add(2, 0, new int[]{23, 34});
        assertEquals("The map should contain 3 elements", 
                     3, map.getDocCount());
    }

    protected String dump(int[] ints) {
        StringWriter sw = new StringWriter(ints.length * 4);
        sw.append("[");
        for (int i = 0 ; i < ints.length ; i++) {
            sw.append(Integer.toString(ints[i]));
            if (i < ints.length - 1) {
                sw.append(", ");
            }
        }
        sw.append("]");
        return sw.toString();
    }

    protected void assertArrayEquals(String message, int[] expected,
                                     int[] actual) {
        Arrays.sort(expected);
        if (!Arrays.equals(expected, actual)) {
            fail(message + ". Expected " + dump(expected)
                 + " got " + dump(actual));
        }
    }

    public void testAddition() throws Exception {
        CoreMap map = bo.getCoreMap();
        map.add(0, 0, new int[]{23, 34});
        assertArrayEquals("Initial array should be correct",
                          new int[]{23, 34}, map.get(0, 0));
        log.debug("After initial array: " + map);
        map.add(0, 1, new int[]{87});
        assertArrayEquals("Initial array should still be correct",
                          new int[]{23, 34}, map.get(0, 0));
        assertArrayEquals("Second array should be correct",
                          new int[]{87}, map.get(0, 1));
        log.debug("After extra addition: " + map);
        map.add(0, 0, new int[]{45, 56, 67});
        assertArrayEquals("Array should be expanded",
                          new int[]{23, 34, 45, 56, 67}, map.get(0, 0));
        log.debug("After expansion: " + map);
        map.add(0, 1, new int[]{12, 89});
        assertArrayEquals("Array for facet 1 should be merged",
                          new int[]{12, 87, 89}, map.get(0, 1));
        log.debug("After merging: " + map);
        map.add(1, 0, new int[]{1});
        map.add(2, 0, new int[]{2});
        map.add(0, 1, new int[]{78, 56});
        assertArrayEquals("Array for facet 1 should be expanded again",
                          new int[]{12, 87, 89, 78, 56}, map.get(0, 1));
        assertArrayEquals("Array for doc 1 should be unchanged",
                          new int[]{1}, map.get(1, 0));
        assertEquals("The map should have 3 documents",
                     3, map.getDocCount());
        assertArrayEquals("The last tags should be [2]",
                          new int[]{2}, map.get(2, 0));
    }

    public void testPersistence() throws Exception {
        CoreMap map = bo.getCoreMap(1, true);
        map.add(0, 0, new int[]{23, 34, 4, 5});
        map.add(0, 1, new int[]{1});
        map.add(2, 0, new int[]{87});
        map.store();
        testPersistenceHelper(map);

        map = bo.getCoreMap(1, false);
        testPersistenceHelper(map);
    }

    public void testPersistenceHelper(CoreMap map) throws Exception {
        assertArrayEquals("Doc 0, Facet 0 should be correct",
                          new int[]{23, 34, 4, 5}, map.get(0, 0));
        assertArrayEquals("Doc 0, Facet 1 should be correct",
                          new int[]{1}, map.get(0, 1));
        assertArrayEquals("Doc 2, Facet 0 should be correct",
                          new int[]{87}, map.get(2, 0));
        assertEquals("There should be the correct number of documents",
                     3, map.getDocCount());
    }

    public void testAddition2() throws Exception {
        CoreMap map = bo.getCoreMap();
        map.add(0, 0, new int[]{1, 2});
        map.add(0, 0, new int[]{3, 4});
        log.debug("After initial additions: " + map);
        assertEquals("There should only be a single document",
                     1, map.getDocCount());

        map.add(0, 1, new int[]{5, 6});
        log.debug("After extra addition for docID 0: " + map);
        assertEquals("There should still only be a single document",
                     1, map.getDocCount());
        assertEquals("There should be the correct number of tags for facet 0",
                     4, map.get(0, 0).length);
        assertEquals("There should be the correct number of tags for facet 1",
                     2, map.get(0, 1).length);

        map.add(0, 0, new int[]{2, 1});
        log.debug("After redundant addition for docID 0: " + map);
        assertEquals("Adding duplicates for facet 0 should not change anything",
                     4, map.get(0, 0).length);

        map.add(1, 0, new int[]{7, 8});
        log.debug("After addition for docID 1: " + map);
        assertEquals("There should now be 2 documents",
                     2, map.getDocCount());
    }

    public void testRemove() throws Exception {
        // TODO: Make a new map with shift=true
        CoreMap map = bo.getCoreMap();
        ((CoreMapBitStuffed)map).setShift(true); // Hack
        map.add(0, 0, new int[]{23, 34});
        map.add(1, 0, new int[]{1});
        map.add(2, 0, new int[]{2});
        map.add(1, 1, new int[]{3, 7});
        map.add(0, 0, new int[]{6, 7});
        // 0, 0: 6, 7, 23, 34
        // 1, 0: 1
        // 1, 1: 3, 7
        // 2, 0: 2
        log.debug("After initial adds: " + map);
        assertArrayEquals("Array for 0, 0 should be expanded",
                          new int[]{23, 34, 6, 7}, map.get(0, 0));
        assertArrayEquals("Array for doc 1 should be as expected",
                          new int[]{1}, map.get(1, 0));

        map.remove(1);
        // 0, 0: 6, 7, 23, 34
        // 1, 0: 2
        log.debug("After remove(1): " + map);
        assertEquals("The number of documents should be reduced",
                     2, map.getDocCount());
        assertArrayEquals("Array 2 should have shifted down to position 1",
                          new int[]{2}, map.get(1, 0));

        map.remove(1);
        log.debug("After second remove(1): " + map);
        assertEquals("The number of documents should be reduced again",
                     1, map.getDocCount());
        assertArrayEquals("The array left should be the first",
                          new int[]{23, 34, 6, 7}, map.get(0, 0));

        map.add(1, 0, new int[]{12, 23});
        log.debug("After additional add: " + map);
        map.remove(0);
        log.debug("After third remove (remove(0)): " + map);
        assertArrayEquals("The original first array should be replaced",
                          new int[]{12, 23}, map.get(0, 0));
        try {
            map.get(2, 0);
            fail("Requesting an unknown document should fail");
        } catch(Exception e) {
            // Expected
        }
    }

    public void testMonkey() throws Exception {
        CoreMap map = bo.getCoreMap();
        Random random = new Random();
        int RUNS = 10000;
        int MAX_DOC = 5000;
        int MAX_TAG_ID = 10000;
        int MAX_TAG_ADDITIONS = 10;
        int maxFacet = bo.getStructure().getFacets().size();
        int feedback = RUNS / 100;
        double removeChance = 0.01;
        for (int i = 0 ; i< RUNS ; i++) {
            if (random.nextDouble() < removeChance && map.getDocCount() > 0) {
                map.remove(random.nextInt(map.getDocCount()));
            } else {
                int[] tagIDs = new int[random.nextInt(MAX_TAG_ADDITIONS)];
                for (int j = 0 ; j < tagIDs.length ; j++) {
                    tagIDs[j] = random.nextInt(MAX_TAG_ID);
                }
                map.add(random.nextInt(MAX_DOC), random.nextInt(maxFacet),
                        tagIDs);
            }
            if (i % feedback == 0) {
                log.debug(i / feedback + "% " + map.getDocCount() + " docs");
            }
        }
        log.info("Finished monkey-test without crashing");
    }

    public void testAdjustPositions() throws Exception {
        CoreMap map = bo.getCoreMap();
        map.add(0, 0, new int[]{23, 34});
        map.add(1, 0, new int[]{1});
        map.add(2, 0, new int[]{2});
        map.add(1, 1, new int[]{3, 7});
        map.add(0, 1, new int[]{6, 7});
        assertArrayEquals("0/0 should be unchanged",
                          new int[]{23, 34}, map.get(0, 0));
        map.adjustPositions(0, 34, 1);
        assertArrayEquals("0/0 should be slightly adjusted",
                          new int[]{23, 35}, map.get(0, 0));
        map.adjustPositions(1, 5, 2);
        assertArrayEquals("0/1 should be adjusted",
                          new int[]{8, 9}, map.get(0, 1));
        assertArrayEquals("1/1 should be slightly adjusted",
                          new int[]{3, 9}, map.get(1, 1));
    }

    public void testFillMap() throws Exception {
/*        TagHandler tagHandler = getTagHandler();
        IndexReader reader = IndexBuilder.getReader();
        CoreMapBitStuffed map =
                new CoreMapBitStuffed(reader.maxDoc(), getFacetNames().size());
                */
        // TODO: Make the fill test
//        map.markCounterLists();
    }

/*    public void testLoad() throws Exception {
        CoreMapBitStuffedLong map = new CoreMapBitStuffedLong(10, 10);
        map.open(ClusterMapCompleteArray.defaultPersistenceLocation);
        long[] values = map.getValues();

        int[] docs = new int[1000];
        Random r = new Random();
        for (int i = 0 ; i < 1000 ; i++) {
            docs[i] = r.nextInt(5000);
        }
        int[][] counterLists = new int[25][5000000];
        map.markCounterLists(counterLists, docs, 0, docs.length-1);
        
    }*/
    public static Test suite() {
        return new TestSuite(CoreMapBitStuffedTest.class);
    }
}
