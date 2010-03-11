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
package dk.statsbiblioteket.summa.common.pool;

import dk.statsbiblioteket.util.CachedCollator;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.StringWriter;
import java.text.Collator;
import java.util.*;

/**
 * A super-class with tests for String Pools.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public abstract class StringPoolSuper extends TestCase {
    private Log log = LogFactory.getLog(StringPoolSuper.class);

    /**
     * Construct a new pool with the given parameters.
     * @param location where the Pool-data should be stored.
     * @param name     the name of the pool.
     * @return a fresh pool at the given location.
     * @throws Exception if the pool could not be constructed.
     */
    public abstract SortedPool<String> getPool(File location, String name)
                                                               throws Exception;

    public StringPoolSuper(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        poolDir.mkdirs();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        for (SortedPool<String> pool: pools) {
            pool.close();
        }
        pools.clear();
        Files.delete(poolDir);
    }

    protected Collator defaultCollator = new CachedCollator(
            new Locale("da"), CachedCollator.COMMON_SUMMA_EXTRACTED, true);
    protected static File poolDir = new File(
            System.getProperty("java.io.tmpdir"), "pooltest");
    private List<SortedPool<String>> pools =
            new ArrayList<SortedPool<String>>(10);
    protected SortedPool<String> getPool() throws Exception {
        return getPool(pools.size());
    }
    protected SortedPool<String> getPool(int poolID) throws Exception {
        SortedPool<String> pool = getPool(poolDir, "testpool_" + poolID);
        pools.add(pool);
        return pool;
    }

    public void testClean() throws Exception {
        SortedPool<String> pool = getPool();
        pool.add("A");
        pool.add("B");
        assertEquals("The size of the pool should be correct", 2, pool.size());
        pool.clear();
        assertEquals("Clearing should result in empty", 0, pool.size());
        pool.add("D");
        assertEquals("Adding a single element should increase size",
                     1, pool.size());
        pool.add("C");
        assertEquals("Adding a second element should increase size",
                     2, pool.size());
        pool.add("E");
        assertEquals("The size of the pool should be correct after new add",
                     3, pool.size());
        assertEquals("The first element should be correct", "C", pool.get(0));
        assertEquals("The second element should be correct", "D", pool.get(1));
        assertEquals("The third element should be correct", "E", pool.get(2));
    }

    public void testDirtyABC() throws Exception {
        SortedPool<String> pool = getPool();
        testDirtyHelper(pool,
                        new String[]{"a", "b", "c"},
                        new String[]{"a", "b", "c"});
    }

    public void testDirty() throws Exception {
        SortedPool<String> pool = getPool();
        testDirtyHelper(pool,
                        new String[]{},
                        new String[]{});
        testDirtyHelper(pool,
                        new String[]{"a", "a"},
                        new String[]{"a"});
        testDirtyHelper(pool,
                        new String[]{"a", "b", "c"},
                        new String[]{"a", "b", "c"});
        testDirtyHelper(pool,
                        new String[]{"c", "b", "a"},
                        new String[]{"a", "b", "c"});
        testDirtyHelper(pool,
                        new String[]{"a", "a", "b"},
                        new String[]{"a", "b"});
        testDirtyHelper(pool,
                        new String[]{"a", "b", "b"},
                        new String[]{"a", "b"});
        testDirtyHelper(pool,
                        new String[]{"b", "a", "b"},
                        new String[]{"a", "b"});
        testDirtyHelper(pool,
                        new String[]{"b", "b", "a", "b"},
                        new String[]{"a", "b"});
        testDirtyHelper(pool,
                        new String[]{"a", "a"},
                        new String[]{"a"});
        testDirtyHelper(pool,
                        new String[]{"a", "a", "a"},
                        new String[]{"a"});
        testDirtyHelper(pool,
                        new String[]{"0", "-1", "1", "1"},
                        new String[]{"-1", "0", "1"});
        testDirtyHelper(pool,
                        new String[]{"elk", "flam", "elke", "elke", "1"},
                        new String[]{"1", "elk", "elke", "flam"});
    }
    public void testDirtyHelper(SortedPool<String> pool, String[] input,
                                String[] expected) throws Exception {
        pool.clear();
        StringWriter sw = new StringWriter(1000);
        for (int i = 0 ; i < input.length ; i++) {
            sw.append(input[i]);
            if (i < input.length-1) {
                sw.append(", ");
            }
            pool.dirtyAdd(input[i]);
            assertEquals("Dirty add should increment the size by 1", 
                         i+1, pool.size());
        }
        pool.cleanup();
        log.debug("Expected " + expected.length + " Strings, pool has " 
                  + pool.size());
        compareOrder(sw.toString(), pool, expected);
    }

    public void testSetGetValue() throws Exception {
        testSetGetValue(getPool());
    }
    public static void testSetGetValue(SortedPool<String> pool) throws
                                                                Exception {
        assertEquals("The initial size should be zero", 0, pool.size());
        assertEquals("The position of a should be zero", 0, pool.insert("a"));
        compareOrder("a added", pool, new String[]{"a"});
        assertEquals("The position of d should be one", 1, pool.insert("d"));
        compareOrder("d added", pool, new String[]{"a", "d"});
        assertEquals("The position of b should be one", 1, pool.insert("b"));
        compareOrder("b added", pool, new String[]{"a", "b", "d"});

        assertEquals("The position of d after b should be two",
                     2, pool.indexOf("d"));
        pool.insert("a");
        assertEquals("Reinserting a shouldn't change anything", 3, pool.size());
        compareOrder("Reinserting", pool, new String[]{"a", "b", "d"});
        assertEquals("The position of 1 should be zero", 0, pool.insert("1"));
        compareOrder("1 is 0", pool, new String[]{"1", "a", "b", "d"});
        assertEquals("The position of e should be four", 4, pool.insert("e"));
        compareOrder("e is 4", pool, new String[]{"1", "a", "b", "d", "e"});

        assertEquals("Position of 1 check", 0, pool.indexOf("1"));
        assertEquals("Position of b check", 2, pool.indexOf("b"));
        assertEquals("Position of e check", 4, pool.indexOf("e"));
        assertEquals("Position of unknown check", -1, pool.indexOf("x"));
    }

    public void testModifyWithJumps() throws Exception {
        testModifyWithJumps(getPool());
    }

    public static void testModifyWithJumps(SortedPool<String> pool)
            throws Exception {
        pool.add("Foo");
        pool.add(20000, "Bar");
    }

    private SortedPool<String> samplePool() throws Exception {
        SortedPool<String> pool = getPool();
        pool.insert("Flim");
        pool.insert("Flam");
        pool.insert("Flum");
        return pool;
    }

    public void testRemoveValue() throws Exception {
        SortedPool<String> pool = samplePool();
        compareOrder("Basic sample", pool,
                     new String[]{"Flam", "Flim", "Flum"});
        pool.remove(0);
        assertEquals("After removal, the size should shrink by one",
                     2, pool.size());
        compareOrder("Removal of 0", pool, new String[]{"Flim", "Flum"});

        pool = samplePool();
        pool.remove(2);
        compareOrder("Removal of 2", pool, new String[]{"Flam", "Flim"});

        try {
            pool.remove(pool.size());
            fail("Trying to remove from a non-existing position should throw"
                 + " an error");
        } catch (Exception e) {
            // Expected
        }

        pool = samplePool();
        pool.remove(1);
        compareOrder("Removed 1", pool, new String[]{"Flam", "Flum"});
        pool.remove(1);
        compareOrder("Remove 1 again", pool, new String[]{"Flam"});
        pool.remove(0);
        compareOrder("Remove 0", pool, new String[]{});

        try {
            pool.remove(0);
            fail("Trying to remove from an empty pool should throw an error");
        } catch (Exception e) {
            // Expected
        }
    }

    public void dumpSpeed() throws Exception {
        dumpSpeed(getPool());
        System.gc();
        dumpSpeed(getPool());
    }
    public static void dumpSpeed(SortedPool<String> pool) throws Exception {
        int ADD_RUNS = 50000;
        int GET_RUNS = 500000;
        Random random = new Random();
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(ADD_RUNS);
        for (int i = 0 ; i < ADD_RUNS ; i++) {
            Integer.toString(random.nextInt());
            profiler.beat();
        }
/*        System.out.println("Raw: " + Math.round(profiler.getBps(false))
                           + " operations/second in "
                           + profiler.getSpendTime());
  */
        profiler = new Profiler();
        profiler.setExpectedTotal(ADD_RUNS);
        for (int i = 0 ; i < ADD_RUNS ; i++) {
            pool.insert(Integer.toString(random.nextInt()));
            profiler.beat();
        }
        System.out.println("Pool add: " + Math.round(profiler.getBps(false))
                           + " operations/second in "
                           + profiler.getSpendTime());

        profiler = new Profiler();
        profiler.setExpectedTotal(GET_RUNS);
        int size = pool.size();
        for (int i = 0 ; i < GET_RUNS ; i++) {
            pool.get(random.nextInt(size));
            profiler.beat();
        }
        System.out.println("Pool get: " + Math.round(profiler.getBps(false))
                           + " operations/second in "
                           + profiler.getSpendTime());

    }
    public void makeSmallSample() throws Exception {
        createSample(getPool(), 1000);
    }
    public void makeMediumSample() throws Exception {
        createSample(getPool(), 1000000);
    }

    public static void createSample(SortedPool<String> pool, int samples)
            throws Exception {
        createSample(pool, samples, false);
    }
    public static Profiler createSample(SortedPool<String> pool, int samples,
                                    boolean dirty) throws Exception {
        Random random = new Random(samples);
        Profiler profiler = new Profiler();
        profiler.setBpsSpan(1000);
        profiler.setExpectedTotal(samples);
        int feedback = Math.max(1, samples / 100);
        for (int i = 0 ; i < samples ; i++) {
            if (dirty) {
                pool.dirtyAdd(Integer.toString(random.nextInt()));
            } else {
                pool.insert(Integer.toString(random.nextInt()));
            }
            profiler.beat();
            if (i % feedback == 0) {
                System.out.println("Added " + i + "/" + samples
                                   + ". Speed: "
                                   + Math.round(profiler.getBps(true))
                                   + " adds/second. ETA: "
                                   + profiler.getETAAsString(true));
            }
        }
        if (dirty) {
            System.out.println("Dirty adding took "+ profiler.getSpendTime());
            pool.cleanup();
            System.out.println("After cleanup the total time was "
                               + profiler.getSpendTime());
        }

        System.out.println("Construction took "+ profiler.getSpendTime());

        Profiler store = new Profiler();
        pool.store();
        System.out.println("Storing took "+ store.getSpendTime());
        profiler.pause();
        return profiler;
    }

    public void dumpDirty() throws Exception {
        dumpDirty(getPool(), 1000000);
    }

    public void testDirtyRandom() throws Exception {
        testDirtyRandom(getPool(), getPool(), 1000);
    }

    public static void testDirtyRandom(SortedPool<String> pool1,
                                       SortedPool<String> pool2, int samples) {
        Random random = new Random();
        for (int i = 0 ; i < samples ; i++) {
            String adder = Integer.toString(random.nextInt(samples / 4));
            pool1.dirtyAdd(adder);
            pool2.insert(adder);
        }
        pool1.cleanup();
        if (!compareOrderBool("dirtyAdds and normal adds should give the "
                              + "same result after cleanup", pool1, pool2)) {
            System.out.println("Dumping values:");
            for (int i = 0 ; i < Math.max(pool1.size(), pool2.size()) ; i++) {
                System.out.print("(" +
                                   (i < pool1.size() ? pool1.get(i) : "NA")
                                   + ", "
                                 + (i < pool2.size() ? pool2.get(i) : "NA")
                                   + ") ");
            }
            System.out.println("");
        }
    }


    public static void dumpDirty(SortedPool<String> pool, int samples) {
        Profiler profiler = new Profiler();
        profiler.setBpsSpan(1000);
        profiler.setExpectedTotal(samples);
        Random random = new Random();
        int feedback = Math.max(1, samples / 100);
        for (int i = 0 ; i < samples ; i++) {
            pool.add(Integer.toString(random.nextInt()));
            profiler.beat();
            if (i % feedback == 0) {
                System.out.println("Dirty added " + i + "/" + samples
                                   + ". Speed: "
                                   + Math.round(profiler.getBps(true))
                                   + " adds/second. ETA: "
                                   + profiler.getETAAsString(true));
            }
        }
        System.out.println("\nAddition took "+ profiler.getSpendTime()
                           + " - Cleaning up...");
        profiler = new Profiler();
        pool.cleanup();
        System.out.println("Cleaning up took "+ profiler.getSpendTime());
    }

    public static void testPosition(SortedPool<String> pool) throws Exception {
        pool.add("Flim");
        pool.add("Flam");
        pool.add("Flum");
        assertEquals("The position of Flem should be -1 as it does not exist",
                     -1, pool.indexOf("Flem"));
        assertEquals("The position of Flim should be correct",
                     1, pool.indexOf("Flim"));
        assertEquals("The position of Flem should still be -1",
                     -1, pool.indexOf("Flem"));
        assertEquals("The position of Flzm should also be -1",
                     -1, pool.indexOf("Flzm"));
    }
    public void testPosition() throws Exception {
        testPosition(getPool());
    }


    protected static void compareOrder(String message, SortedPool pool,
                                       Object[] expected) {
        assertEquals("(" + message + ") The arrays should have the same size",
                     expected.length, pool.size());
        for (int i = 0 ; i < expected.length ; i++) {
            //noinspection DuplicateStringLiteralInspection
            assertEquals("(" + message + ") The objects at position " + i
                         + " should be equal (expected "
                         + Logs.expand(Arrays.asList(expected), 10)
                         + " actual " + Logs.expand(pool, 10) + ")",
                         expected[i], pool.get(i));
        }
    }
    protected static void compareOrder(String message, SortedPool pool1,
                                       SortedPool pool2) {
        assertEquals("(" + message + ") The pools should have the same size",
                     pool1.size(), pool2.size());
        for (int i = 0 ; i < pool1.size() ; i++) {
            //noinspection DuplicateStringLiteralInspection
            assertEquals("(" + message + ") The objects at position " + i
                         + " should be equal",
                         pool1.get(i), pool2.get(i));
        }
    }
    protected static boolean compareOrderBool(String message, SortedPool pool1,
                                              SortedPool pool2) {
        if (pool1.size() != pool2.size()) {
            return false;
        }
        for (int i = 0 ; i < pool1.size() ; i++) {
            if (!pool1.get(i).equals(pool2.get(i))) {
                System.out.println(message);
                return false;
            }
        }
        return true;
    }

    public void testSorting() throws Exception {
        testSorting(getPool());
    }

    public void testSorting(SortedPool<String> pool) {
        String[] words = new String[]{
                "Duksedreng", "Daskelars", "Dumrian", "Drøbel",
                //"Dræbel", "Drible",
                "Drillenisse"};
        String[] sorted = new String[words.length];
        System.arraycopy(words, 0, sorted, 0, words.length);
        Arrays.sort(sorted, defaultCollator);

        //noinspection ManualArrayToCollectionCopy
        for (String word: words) {
            pool.add(word);
        }
        compareOrder("Sort with add should work",
                                    pool, sorted);
        pool.clear();
        assertTrue("Pool should be empty after clear", pool.size() == 0);

        pool.addAll(Arrays.asList(words));
        compareOrder("Sort with addAll should work",
                                    pool, sorted);
        pool.clear();

        for (String word: words) {
            pool.dirtyAdd(word);
        }
        pool.cleanup();
        compareOrder("Sort with cleanup should work",
                                    pool, sorted);
    }

    public void testComparator() throws Exception {
        assertTrue("i and a should be sorted correctly with natural",
                     "Drillenisse".compareTo("Drabant") > 0);
        assertTrue("i and a should be sorted correctly with collator",
                     defaultCollator.compare("Drillenisse", "Drabant") > 0);

        assertTrue("i and ø should be sorted correctly with natural",
                     "Drøbel".compareTo("Drillenisse") > 0);
        assertTrue("i and ø should be sorted correctly with collator",
                     defaultCollator.compare("Drøbel", "Drillenisse") > 0);
    }
    public void testSortBasic() throws Exception {
        testSortBasic(getPool());
    }
    public static void testSortBasic(SortedPool<String> pool)
            throws Exception {
        String[] BASE = new String[]{"Daskelars", "Drillenisse",
                                     "Dræsine", "Drøbel", "Duksedreng",
                                     "Dumrian"};
        pool.add("Duksedreng");
        pool.add("Daskelars");
        pool.add("Dumrian");
        pool.add("Drøbel");
        pool.add("Dræsine");
        pool.add("Drillenisse");
        compareOrder("Plain addition should work", pool, BASE);
    }
    public void testSortAfterStore() throws Exception {
        testSortAddition(getPool(), false);
        testSortAddition(getPool(), true);
    }
    public static void testSortAddition(SortedPool<String> pool, boolean store)
            throws Exception {
        String[] BASE = new String[]{"Daskelars", "Drøbel", "Duksedreng",
                                     "Dumrian"};
        pool.add("Duksedreng");
        pool.add("Daskelars");
        pool.add("Dumrian");
        pool.add("Drøbel");
        compareOrder(
                "The constructed pool should be as expected", pool, BASE);
        if (store) {
            pool.store();
            compareOrder(
                    "The stored pool should be unchanged", pool, BASE);
        }
        pool.add("Drillenisse");
        compareOrder(
                "Addition of Drillenisse with store " + store + " should work",
                pool,
                new String[]{"Daskelars", "Drillenisse", "Drøbel", "Duksedreng",
                             "Dumrian"});
    }

    public void testDirtyOffByOne() throws Exception {
        int RUNS = 5;
        int samples = 100;
        Random random = new Random();
        for (int r = 0 ; r < RUNS ; r++) {
            SortedPool<String> pool = getPool();
            for (int i = 0 ; i < samples ; i++) {
                String adder = Integer.toString(random.nextInt(samples / 4));
                pool.dirtyAdd(adder);
            }
            pool.cleanup();
            for (int i = 0 ; i < samples ; i++) {
                assertFalse("No values should contain line breaks",
                     pool.get(random.nextInt(pool.size())).contains("\n"));
            }
        }
    }

    public void testResourceFreeing() throws Exception {
        SortedPool<String> pool = getPool(87);
        pool.add("Duksedreng");
        pool.store();
        pool.close();
        File indexFile = new File(poolDir, pool.getName() + ".index");
        assertTrue("An index-file should be created", indexFile.exists());
        assertTrue("The index-file should be deleted", indexFile.delete());
        File datFile = new File(poolDir, pool.getName() + ".dat");
        assertTrue("An data-file should be created", datFile.exists());
        assertTrue("The data-file should be deleted", datFile.delete());

        pool = getPool(88);
        pool.add("Duksedreng");
        pool.store();
        datFile = new File(poolDir, pool.getName() + ".dat");
        assertTrue("An data-file should be created", datFile.exists());
        try {
            datFile.delete();
            System.out.println(
                    "The datFile could be deleted even though it was in use. "
                    + "This is probably on a system without file-locking");
        } catch (Exception e) {
            System.out.println(
                    "The datFile could not be deleted. This is probably "
                    + "running under Windows with a file-locking file-system, "
                    + "such as NTFS");
        }
    }
}

