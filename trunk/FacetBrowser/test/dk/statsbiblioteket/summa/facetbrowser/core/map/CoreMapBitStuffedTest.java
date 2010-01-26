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

import java.util.Random;
import java.util.zip.Deflater;

import dk.statsbiblioteket.summa.facetbrowser.browse.TagCounter;
import dk.statsbiblioteket.summa.facetbrowser.browse.TagCounterArray;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.facetbrowser.BaseObjects;
import dk.statsbiblioteket.summa.common.unittest.ExtraAsserts;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Profiler;
import org.apache.log4j.Logger;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class CoreMapBitStuffedTest extends TestCase {
    private static Logger log = Logger.getLogger(CoreMapBitStuffedTest.class);

    public CoreMapBitStuffedTest(String name) {
        super(name);
    }

    BaseObjects bo;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        bo = new BaseObjects();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        bo.close();
    }

    public void testExpansion() throws Exception {
        CoreMap map = bo.getCoreMap();
        testExpansion(map);
    }

    public static void testExpansion(CoreMap map) {
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

    public void testExpansionWithZeros() throws Exception {
        CoreMap map = bo.getCoreMap();
        testExpansionWithZeroes(map);
    }

    public static void testExpansionWithZeroes(CoreMap map) {
        map.add(0, 0, new int[]{12, 23, 34});
        map.add(1, 1, new int[]{12, 34});
        map.add(3, 0, new int[]{23, 34});
        assertEquals("The map should contain 4 elements",
                     4, map.getDocCount());
    }

    public void testAddition() throws Exception {
        CoreMap map = bo.getCoreMap();
        testAddition(map);
    }

    public static void testAddition(CoreMap map) {
        map.add(0, 0, new int[]{23, 34});
        ExtraAsserts.assertEquals("Initial array should be correct",
                          new int[]{23, 34}, map.get(0, 0));
        log.debug("After initial array: " + map);
        map.add(0, 1, new int[]{87});
        ExtraAsserts.assertEquals(
                "Initial array should be correct after second add",
                new int[]{23, 34}, map.get(0, 0));
        ExtraAsserts.assertEquals("Second array should be correct",
                          new int[]{87}, map.get(0, 1));
        log.debug("After extra addition: " + map);
        map.add(0, 0, new int[]{45, 56, 67});
        ExtraAsserts.assertEquals("Array should be expanded",
                          new int[]{23, 34, 45, 56, 67}, map.get(0, 0));
        log.debug("After expansion: " + map);
        map.add(0, 1, new int[]{12, 89});
        ExtraAsserts.assertEquals("Array for facet 1 should be merged",
                          new int[]{12, 87, 89}, map.get(0, 1));
        log.debug("After merging: " + map);
        map.add(1, 0, new int[]{1});
        map.add(2, 0, new int[]{2});
        map.add(0, 1, new int[]{78, 56});
        ExtraAsserts.assertEquals("Array for facet 1 should be expanded again",
                          new int[]{12, 87, 89, 78, 56}, map.get(0, 1));
        ExtraAsserts.assertEquals("Array for doc 1 should be unchanged",
                          new int[]{1}, map.get(1, 0));
        assertEquals("The map should have 3 documents",
                     3, map.getDocCount());
        ExtraAsserts.assertEquals("The last tags should be [2]",
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
        ExtraAsserts.assertEquals("Doc 0, Facet 0 should be correct",
                          new int[]{23, 34, 4, 5}, map.get(0, 0));
        ExtraAsserts.assertEquals("Doc 0, Facet 1 should be correct",
                          new int[]{1}, map.get(0, 1));
        ExtraAsserts.assertEquals("Doc 2, Facet 0 should be correct",
                          new int[]{87}, map.get(2, 0));
        assertEquals("There should be the correct number of documents",
                     3, map.getDocCount());
    }

    public void testClear() throws Exception {
        CoreMap map = bo.getCoreMap();
        testClear(map);
    }

    public static void testClear(CoreMap map) {
        map.add(0, 0, new int[]{1, 2});
        map.add(0, 0, new int[]{3, 4});
        assertEquals("There should only be a single document",
                     1, map.getDocCount());
        map.clear();
        assertEquals("There should only be no documents after clear",
                     0, map.getDocCount());
    }

    public void testAddition2() throws Exception {
        CoreMap map = bo.getCoreMap();
        testAddition2(map);
    }

    public static void testAddition2(CoreMap map) {
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
        ExtraAsserts.assertEquals("Array for 0, 0 should be expanded",
                          new int[]{23, 34, 6, 7}, map.get(0, 0));
        ExtraAsserts.assertEquals("Array for doc 1 should be as expected",
                          new int[]{1}, map.get(1, 0));

        map.remove(1);
        // 0, 0: 6, 7, 23, 34
        // 1, 0: 2
        log.debug("After remove(1): " + map);
        assertEquals("The number of documents should be reduced",
                     2, map.getDocCount());
        ExtraAsserts.assertEquals(
                "Array 2 should have shifted down to position 1",
                new int[]{2}, map.get(1, 0));

        map.remove(1);
        log.debug("After second remove(1): " + map);
        assertEquals("The number of documents should be reduced again",
                     1, map.getDocCount());
        ExtraAsserts.assertEquals("The array left should be the first",
                          new int[]{23, 34, 6, 7}, map.get(0, 0));

        map.add(1, 0, new int[]{12, 23});
        log.debug("After additional add: " + map);
        map.remove(0);
        log.debug("After third remove (remove(0)): " + map);
        ExtraAsserts.assertEquals("The original first array should be replaced",
                          new int[]{12, 23}, map.get(0, 0));
        try {
            map.get(2, 0);
            fail("Requesting an unknown document should fail");
        } catch(Exception e) {
            // Expected
        }
    }

    public void testHugeJumpInDocID() throws Exception {
        CoreMap map = bo.getCoreMap();
        Random random = new Random();
        int maxFacet = bo.getStructure().getFacets().size();
        int MAX_TAG_ADDITIONS = 10;
        int MAX_TAG_ID = 10000;

        int[] tagIDs = new int[random.nextInt(MAX_TAG_ADDITIONS)];
        for (int j = 0 ; j < tagIDs.length ; j++) {
            tagIDs[j] = random.nextInt(MAX_TAG_ID);
        }
        map.add(0, random.nextInt(maxFacet), tagIDs);
        map.add(20000, random.nextInt(maxFacet), tagIDs);
    }

    /* Throws exceptions if it cannot scale to RUNS documents */
    public void testScale() throws Exception {
        CoreMap map = bo.getCoreMap();
        Random random = new Random();
        int maxFacet = bo.getStructure().getFacets().size();
        int MAX_TAG_ADDITIONS = 10;
        int MAX_TAG_ID = 10000;
        int RUNS = 20000;
        for (int i = 0 ; i< RUNS ; i++) {
            int[] tagIDs = new int[random.nextInt(MAX_TAG_ADDITIONS)];
            for (int j = 0 ; j < tagIDs.length ; j++) {
                tagIDs[j] = random.nextInt(MAX_TAG_ID);
            }
            map.add(i, random.nextInt(maxFacet),
                    tagIDs);
        }
    }

    public void testMonkey() throws Exception {
        int[] RUNS = {1000, 10000, 100000};
        for (int runs: RUNS) {
            testMonkey(runs);
        }
    }

    public void testMonkey(int runs) throws Exception {
        testMonkey(runs, bo.getStructure().getFacets().size(), bo.getCoreMap());
    }

    /*
    Note: This uses a seeded randomizer, so the finished map structure is
    deterministic.
     */
    public static void testMonkey(int runs, int maxFacet, CoreMap map)
            throws Exception {
        map.clear();
        Random random = new Random(87);
        int MAX_DOC = 5000;
        int MAX_TAG_ID = 10000;
        int MAX_TAG_ADDITIONS = 10;
        int feedback = Math.max(1, runs / 100);
        double removeChance = 0.01;

        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(runs);

        for (int i = 0 ; i< runs ; i++) {
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
            profiler.beat();
            if (i % feedback == 0) {
                log.debug(i / feedback + "% " + map.getDocCount() + " docs");
            }
        }
        //noinspection DuplicateStringLiteralInspection
        log.info("Finished monkey-test with " + runs
                 + " updates without crashing in "
                 + profiler.getSpendTime() + " at " + profiler.getBps(false)
                 + " updates/second");
    }

    public void testMarkPerformance() throws Exception {
        int RUNS = 5;
        int REDOS = 10;
        CoreMap map = getCoreMap();

        TagCounter tagCounter = bo.getTagCounter();
        tagCounter.verify();
        DocIDCollector docIDs = new DocIDCollector();
        for (int i = 0 ; i < map.getDocCount() ; i++) {
            docIDs.collect(i, 1.0f);
        }
        Profiler profiler = new Profiler();
        for (int run = 0 ; run < RUNS ; run++) {
            tagCounter.reset();
            tagCounter.increment(0, 0); // Wait for clean to finish
            profiler.reset();
            for (int redo = 0 ; redo < REDOS ; redo++) {
                map.markCounterLists(tagCounter, docIDs, 0, map.getDocCount());
            }
            System.out.println(String.format(
                    "Marked tags for %d documents %d times in %s",
                    map.getDocCount(), REDOS, profiler.getSpendTime()));
        }
    }
    public CoreMap getCoreMap() throws Exception {
        int MAX_DOC = 1000000;
        int MAX_TAG_ID = 10000;
        int MAX_TAG_ADDITIONS = 10;

        int maxFacet = bo.getStructure().getFacets().size();
        CoreMap map = bo.getCoreMap();

        map.clear();
        Random random = new Random(87);

        int feedback = Math.max(1, MAX_DOC / 100);
        long additions = 0;
        Profiler profiler = new Profiler();
        for (int i = 0 ; i< MAX_DOC; i++) {
            int[] tagIDs = new int[random.nextInt(MAX_TAG_ADDITIONS)];
            additions += tagIDs.length;
            for (int j = 0 ; j < tagIDs.length ; j++) {
                tagIDs[j] = random.nextInt(MAX_TAG_ID);
            }
            map.add(i, random.nextInt(maxFacet), tagIDs);
            if (i % feedback == 0) {
                System.out.print(".");
            }
        }
        System.out.println(String.format(
                "\nGenerated core map with length %d and approximate %d entries"
                + " in %s",
                map.getDocCount(), additions, profiler.getSpendTime()));
        return map;
    }

    public void testAdjustPositions() throws Exception {
        CoreMapBitStuffed map = (CoreMapBitStuffed)bo.getCoreMap();
        testAdjustPositions(map);
    }

    private void testAdjustPositions(CoreMapBitStuffed map) {
        map.add(0, 0, new int[]{23, 34});
        map.add(1, 0, new int[]{1});
        map.add(2, 0, new int[]{2});
        map.add(1, 1, new int[]{3, 7});
        map.add(0, 1, new int[]{6, 7});
        ExtraAsserts.assertEquals("0/0 should be unchanged",
                          new int[]{23, 34}, map.get(0, 0));
        map.adjustPositions(0, 34, 1);
        ExtraAsserts.assertEquals("0/0 should be slightly adjusted",
                          new int[]{23, 35}, map.get(0, 0));
        map.adjustPositions(1, 5, 2);
        ExtraAsserts.assertEquals("0/1 should be adjusted",
                          new int[]{8, 9}, map.get(0, 1));
        ExtraAsserts.assertEquals("1/1 should be slightly adjusted",
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

    // TODO: Move this or delete it as it is just general compression tests
    public void testCompress() throws Exception {
        String input = "IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII";
        byte[] inputBytes = input.getBytes("UTF-8");

        System.out.println("Original size in bytes: " + inputBytes.length);
        byte[] output = new byte[inputBytes.length * 2];
        Deflater compresser = new Deflater();
        compresser.setInput(inputBytes);
        compresser.finish();
        System.out.println("Packed size in bytes: " + compresser.deflate(output));

    }
}

