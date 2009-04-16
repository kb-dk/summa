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
package dk.statsbiblioteket.summa.common.pool;

import dk.statsbiblioteket.summa.common.pool.MemoryStringPool;
import dk.statsbiblioteket.summa.common.pool.SortedPool;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;

/**
 * MemoryStringPool Tester.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MemoryStringPoolTest extends StringPoolSuperTest {
    public MemoryStringPoolTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(MemoryStringPoolTest.class);
    }

    public SortedPool<String> getPool(File location,
                                      String name) throws Exception {
        SortedPool<String> pool = new MemoryStringPool(defaultCollator);
        pool.open(location, name, false, true);
        return pool;
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
        StringPoolSuperTest.compareOrder(
                "The constructed pool should be as expected",
                pool1, 
                new String[]{"Daskelars", "Drøbel", "Duksedreng", "Dumrian"});
        pool1.store();
        pool2.open(poolDir, pool1.getName(), false, false);
        StringPoolSuperTest.compareOrder("The loaded pool should be equal to the"
                                    + " saved", pool1, pool2);
        StringPoolSuperTest.compareOrder(
                "The loaded pool should be as expected",
                pool2,
                new String[]{"Daskelars", "Drøbel", "Duksedreng", "Dumrian"});

        pool1.add("Drillenisse");
        StringPoolSuperTest.compareOrder(
                "Adding after store",
                pool1,
                new String[]{"Daskelars", "Drillenisse", "Drøbel", "Duksedreng",
                             "Dumrian"});

        pool2.add("Drillenisse");
        StringPoolSuperTest.compareOrder(
                "Adding after load",
                pool2,
                new String[]{"Daskelars", "Drillenisse", "Drøbel", "Duksedreng",
                             "Dumrian"});
    }

}



