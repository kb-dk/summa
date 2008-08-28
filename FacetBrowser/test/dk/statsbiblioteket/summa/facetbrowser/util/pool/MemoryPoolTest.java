/* $Id: MemoryPoolTest.java,v 1.5 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.5 $
 * $Date: 2007/10/05 10:20:23 $
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
package dk.statsbiblioteket.summa.facetbrowser.util.pool;

import dk.statsbiblioteket.util.CachedCollator;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.text.Collator;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

/**
 * MemoryPool Tester.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MemoryPoolTest extends TestCase {
    public MemoryPoolTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    static Collator defaultCollator = new CachedCollator(new Locale("da"));
    public static  MemoryStringPool getPool() throws IOException {
        MemoryStringPool pool = new MemoryStringPool(defaultCollator);
        pool.open(new File(System.getProperty("java.io.tmpdir"),
                           "facetTestDisk"),
                  "testpool", false, true);
        return pool;
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

    private SortedPool<String> samplePool() throws IOException {
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
        createSample(getPool(), 1000, new File("/tmp"), "test_1K");
    }
    public void makeMediumSample() throws Exception {
        createSample(getPool(), 1000000,
                     new File("/tmp"), "test_1M");
    }

    public static void createSample(SortedPool<String> pool, int samples,
                                    File location, String name)
            throws Exception {
        Random random = new Random();
        Profiler profiler = new Profiler();
        profiler.setBpsSpan(1000);
        profiler.setExpectedTotal(samples);
        int feedback = Math.max(1, samples / 100);
        for (int i = 0 ; i < samples ; i++) {
            pool.insert(Integer.toString(random.nextInt()));
            profiler.beat();
            if (i % feedback == 0) {
                System.out.println("Added " + i + "/" + samples
                                   + ". Speed: "
                                   + Math.round(profiler.getBps(true))
                                   + " adds/second. ETA: "
                                   + profiler.getETAAsString(true));
            }
        }
        System.out.println("\nConstruction took "+ profiler.getSpendTime());

        profiler = new Profiler();
        pool.store();
        System.out.println("Storing took "+ profiler.getSpendTime());
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
            pool1.add(adder);
            pool2.insert(adder);
        }
        pool1.cleanup();
        if (!compareOrderBool("dirtyAdds and normal adds should give the "
                              + "same result", pool1, pool2)) {
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

    public void testDirty() throws Exception {
        testDirty(getPool());
    }

    public static void testDirty(SortedPool<String> pool) throws Exception {
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
    public static void testDirtyHelper(SortedPool<String> pool, String[] input,
                                       String[] expected) throws Exception {
        pool.clear();
        StringWriter sw = new StringWriter(1000);
        for (int i = 0 ; i < input.length ; i++) {
            sw.append(input[i]);
            if (i < input.length-1) {
                sw.append(", ");
            }
            pool.add(input[i]);
        }
        pool.cleanup();
        compareOrder(sw.toString(), pool, expected);
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
}
