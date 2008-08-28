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
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.CachedCollator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
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

    public void testIO() throws Exception {
        testIO(getPool(1), getPool(1));
    }
    public static void testIO(SortedPool<String> pool1,
                              SortedPool<String> pool2) throws Exception {
        pool1.add("Duksedreng");
        pool1.add("Daskelars");
        pool1.add("Dumrian");
        pool1.add("Drøbel");
        File temp = File.createTempFile("MemoryTest", "tmp");
        temp.deleteOnExit();
        File tempLocation = temp.getParentFile();
        pool1.store();
        assertTrue("Values should exist",
                   new File(tempLocation, "temp.dat").exists());
        assertTrue("Indexes should exist",
                   new File(tempLocation, "temp.index").exists());
        pool2.open(poolFolder, pool1.getName(), false, false);
        MemoryPoolTest.compareOrder("The loaded pool should be equal to the"
                                    + " saved", pool1, pool2);
        new File(tempLocation, "temp.dat").delete();
        new File(tempLocation, "temp.index").delete();
        pool2.add("Drillenisse");
        MemoryPoolTest.compareOrder("IOTesting", pool2,
                                    new String[]{"Daskelars", "Drillenisse",
                                                 "Drøbel", "Duksedreng",
                                                 "Dumrian"});
    }

    public void dumpDirty() throws Exception {
        MemoryPoolTest.dumpDirty(getPool(3), 1000000);
    }
}
