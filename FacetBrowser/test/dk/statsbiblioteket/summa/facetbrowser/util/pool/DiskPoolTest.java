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

import junit.framework.Test;
import junit.framework.TestSuite;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Profiler;

/**
 * DiskPool Tester.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DiskPoolTest extends StringPoolSuperTest {
    public DiskPoolTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(DiskPoolTest.class);
    }

    public SortedPool<String> getPool(File location,
                                      String name) throws Exception {
        DiskStringPool pool = new DiskStringPool(defaultCollator);
//        DiskStringPool pool = new DiskStringPool(null);
        pool.open(location, name, false, true);
        return pool;
    }

    public void testSpeed100() throws Exception {
        testSpeed(100);
    }

    public void testSpeed10K() throws Exception {
        testSpeed(10000);
    }

    public void atestSpeed100K() throws Exception {
        testSpeed(100000);
    }

    public void atestSpeed1M() throws Exception {
        testSpeed(1000000);
    }

    public void atestDirty1M() throws Exception {
        System.out.println("Dirty: " + createSample(getPool(), 1000000, true).
                getSpendTime());
    }

    public void testSpeed(int count) throws Exception {
        String plain = createSample(getPool(), count, false).getSpendTime();
        String dirty = createSample(getPool(), count, true).getSpendTime();
        System.out.println("Plain: " + plain);
        System.out.println("Dirty: " + dirty);
    }
}



