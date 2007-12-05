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
import java.util.Random;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.qa.QAInfo;

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
    }

    public void testSetGetValue() throws Exception {
        MemoryPoolTest.testSetGetValue(new DiskStringPool());
    }

    public void testIO() throws Exception {
        MemoryStringPoolTest.testIO(new DiskStringPool(), new DiskStringPool());
    }

    public void dumpSpeed() throws Exception {
        MemoryPoolTest.dumpSpeed(new DiskStringPool());
        System.gc();
        MemoryPoolTest.dumpSpeed(new DiskStringPool());
    }

    public void makeSmallSample() throws Exception {
        MemoryPoolTest.createSample(new DiskStringPool(), 1000,
                                    new File("/tmp"), "test_1K");
    }

    public void makeMediumSample() throws Exception {
        MemoryPoolTest.createSample(new DiskStringPool(), 1000000,
                                    new File("/tmp"), "test_1M");
    }

    public void makeBigSample() throws Exception {
        MemoryPoolTest.createSample(new DiskStringPool(), 10000000,
                                    new File("/tmp"), "test_10M");
    }

    public void dumpDirty() throws Exception {
        MemoryPoolTest.dumpDirty(new DiskStringPool(), 1000000);
    }
    public void testDirty() throws Exception {
        MemoryPoolTest.testDirty(new DiskStringPool());
    }
    public void testDirtyRandom() throws Exception {
        MemoryPoolTest.testDirtyRandom(new DiskStringPool(),
                                       new DiskStringPool(), 1000);
    }

    public void testDirtyOffByOne() throws Exception {
        int RUNS = 100;
        int samples = 1000;
        for (int r = 0 ; r < RUNS ; r++) {
            DiskPool<String> pool = new DiskStringPool();
            Random random = new Random();
            for (int i = 0 ; i < samples ; i++) {
                String adder = Integer.toString(random.nextInt(samples / 4));
                pool.dirtyAdd(adder);
            }
            pool.cleanup();
            for (int i = 0 ; i < samples ; i++) {
                assertFalse("No values should contain line breaks",
                     pool.getValue(random.nextInt(pool.size())).contains("\n"));
            }
        }
    }

    public void testPosition() throws Exception {
        MemoryPoolTest.testPosition(new DiskStringPool());
    }


    public static Test suite() {
        return new TestSuite(DiskPoolTest.class);
    }
}
