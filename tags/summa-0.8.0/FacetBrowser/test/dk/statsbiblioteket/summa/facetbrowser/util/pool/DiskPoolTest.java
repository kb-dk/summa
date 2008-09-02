/* $Id: DiskPoolTest.java,v 1.5 2007/10/05 10:20:23 te Exp $
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

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.text.Collator;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.CachedCollator;

/**
 * DiskPool Tester.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DiskPoolTest extends TestCase {
    public DiskPoolTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        for (SortedPool pool: openedPools) {
            pool.close();
        }
        openedPools.clear();
    }

    static File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));

    static Collator defaultCollator = new CachedCollator(new Locale("da"));
    static List<SortedPool> openedPools = new ArrayList<SortedPool>(10);
    static Random random = new Random();
    public static DiskStringPool getPool() throws IOException {
        DiskStringPool pool = new DiskStringPool(defaultCollator);
        pool.open(new File(TMP_DIR, "facetTestDisk"),
                  "testpool" + random.nextInt(), false, true);
        openedPools.add(pool);
        return pool;
    }
    
    public void testSetGetValue() throws Exception {
        MemoryPoolTest.testSetGetValue(getPool());
    }

    public void testIO() throws Exception {
        MemoryStringPoolTest.testIO(getPool(), getPool());
    }

    public void dumpSpeed() throws Exception {
        System.gc();
        MemoryPoolTest.dumpSpeed(getPool());
    }

    public void makeSmallSample() throws Exception {
        MemoryPoolTest.createSample(getPool(), 1000,
                                    TMP_DIR, "test_1K");
    }

    public void makeMediumSample() throws Exception {
        MemoryPoolTest.createSample(getPool(), 1000000,
                                    TMP_DIR, "test_1M");
    }

    public void makeBigSample() throws Exception {
        MemoryPoolTest.createSample(getPool(), 5000000,
                                    TMP_DIR, "test_10M");
    }

    public void dumpDirty() throws Exception {
        MemoryPoolTest.dumpDirty(getPool(), 100000);
    }
    public void testDirty() throws Exception {
        MemoryPoolTest.testDirty(getPool());
    }
    public void testDirtyRandom() throws Exception {
        MemoryPoolTest.testDirtyRandom(getPool(),
                                       getPool(), 100);
    }

    public void testDirtyOffByOne() throws Exception {
        int RUNS = 10;
        int samples = 1000;
        for (int r = 0 ; r < RUNS ; r++) {
            DiskPool<String> pool = getPool();
            Random random = new Random();
            for (int i = 0 ; i < samples ; i++) {
                String adder = Integer.toString(random.nextInt(samples / 4));
                pool.add(adder);
            }
            pool.cleanup();
            for (int i = 0 ; i < samples ; i++) {
                assertFalse("No values should contain line breaks",
                     pool.get(random.nextInt(pool.size())).contains("\n"));
            }
        }
    }

    public void testPosition() throws Exception {
        MemoryPoolTest.testPosition(getPool());
    }


    public static Test suite() {
        return new TestSuite(DiskPoolTest.class);
    }
}
