/* $Id: MemoryStringPoolTest.java,v 1.5 2007/10/05 10:20:23 te Exp $
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

import dk.statsbiblioteket.summa.facetbrowser.BaseObjects;
import dk.statsbiblioteket.util.CachedCollator;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.Arrays;
import java.util.Locale;

/**
 * MemoryStringPool Tester.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MemoryStringPoolTest extends TestCase {
    public MemoryStringPoolTest(String name) {
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

    public static Test suite() {
        return new TestSuite(MemoryStringPoolTest.class);
    }

    static Collator defaultCollator = new CachedCollator(new Locale("da"));
    public static MemoryStringPool getPool(int id) throws IOException {
        MemoryStringPool pool = new MemoryStringPool(defaultCollator);
        pool.open(poolFolder, "testpool" + id, false, true);
        return pool;
    }
    static File poolFolder = new File(System.getProperty("java.io.tmpdir"),
                           "facetTestMemory");


    public void testSorting() throws IOException {
        testSorting(getPool(3));
    }
    public static void testSorting(SortedPool<String> pool) {
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
        MemoryPoolTest.compareOrder("Sort with add should work",
                                    pool, sorted);
        pool.clear();
        assertTrue("Pool should be empty after clear", pool.size() == 0);

        pool.addAll(Arrays.asList(words));
        MemoryPoolTest.compareOrder("Sort with addAll should work",
                                    pool, sorted);
        pool.clear();

        for (String word: words) {
            pool.dirtyAdd(word);
        }
        pool.cleanup();
        MemoryPoolTest.compareOrder("Sort with cleanup should work",
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
        testSortBasic(getPool(7));
    }
    public static void testSortBasic(SortedPool<String> pool)
            throws Exception {
        String[] BASE = new String[]{"Daskelars", "Drillenisse",
                                     "Drøbel", "Duksedreng",
                                     "Dumrian"};
        pool.add("Duksedreng");
        pool.add("Daskelars");
        pool.add("Dumrian");
        pool.add("Drøbel");
        pool.add("Drillenisse");
        MemoryPoolTest.compareOrder("Plain addition should work", pool, BASE);
    }

    public static void testSortAfterStore() throws Exception {
        testSortAddition(getPool(6), false);
        testSortAddition(getPool(6), true);
    }
    public static void testSortAddition(SortedPool<String> pool, boolean store)
            throws Exception {
        String[] BASE = new String[]{"Daskelars", "Drøbel", "Duksedreng",
                                     "Dumrian"};
        pool.add("Duksedreng");
        pool.add("Daskelars");
        pool.add("Dumrian");
        pool.add("Drøbel");
        MemoryPoolTest.compareOrder(
                "The constructed pool should be as expected", pool, BASE);
        if (store) {
            pool.store();
            MemoryPoolTest.compareOrder(
                    "The stored pool should be unchanged", pool, BASE);
        }
        pool.add("Drillenisse");
        MemoryPoolTest.compareOrder(
                "Addition of Drillenisse with store " + store + " should work",
                pool,
                new String[]{"Daskelars", "Drillenisse", "Drøbel", "Duksedreng",
                             "Dumrian"});
    }

    public void testIO() throws Exception {
        testIO(getPool(1), getPool(1));
    }
    public static void testIO(SortedPool<String> pool1,
                              SortedPool<String> pool2) throws Exception {
        pool1.add("Duksedreng");
        pool1.add("Daskelars");
        pool1.add("Dumrian");
        pool1.add("Drøbel");
        MemoryPoolTest.compareOrder(
                "The constructed pool should be as expected",
                pool1, 
                new String[]{"Daskelars", "Drøbel", "Duksedreng", "Dumrian"});
        pool1.store();
        pool2.open(poolFolder, pool1.getName(), false, false);
        MemoryPoolTest.compareOrder("The loaded pool should be equal to the"
                                    + " saved", pool1, pool2);
        MemoryPoolTest.compareOrder(
                "The loaded pool should be as expected",
                pool2,
                new String[]{"Daskelars", "Drøbel", "Duksedreng", "Dumrian"});

        pool1.add("Drillenisse");
        MemoryPoolTest.compareOrder(
                "Adding after store",
                pool1,
                new String[]{"Daskelars", "Drillenisse", "Drøbel", "Duksedreng",
                             "Dumrian"});

        pool2.add("Drillenisse");
        MemoryPoolTest.compareOrder(
                "Adding after load",
                pool2,
                new String[]{"Daskelars", "Drillenisse", "Drøbel", "Duksedreng",
                             "Dumrian"});
    }

    public void dumpDirty() throws Exception {
        MemoryPoolTest.dumpDirty(getPool(3), 1000000);
    }
}
