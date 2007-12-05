/* $Id: SortedHashTest.java,v 1.5 2007/10/05 10:20:23 te Exp $
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
package dk.statsbiblioteket.summa.facetbrowser.util;

import java.util.HashMap;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * SortedHash Tester.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SortedHashTest extends TestCase {
    SortedHash<String> basicMap;
    public SortedHashTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        basicMap = new SortedHash<String>();
        basicMap.add("b");
        basicMap.add("a");
        basicMap.add("c");
    }

    public void tearDown() throws Exception {
        super.tearDown();
        //noinspection AssignmentToNull
        basicMap = null;
    }

    public void testGet() throws Exception {
        assertEquals("a should be first", 0, basicMap.get("a"));
        assertEquals("b should be second", 1, basicMap.get("b"));
        assertEquals("c should be third", 2, basicMap.get("c"));
        assertEquals("d should be unknown", -1, basicMap.get("d"));
        assertEquals("Number 0 should yield a", "a", basicMap.get(0));
        assertEquals("Number 1 should yield b", "b", basicMap.get(1));
        assertEquals("Number 2 should yield c", "c", basicMap.get(2));
        assertEquals("a should still be first", 0, basicMap.get("a"));
        assertEquals("b should still be second", 1, basicMap.get("b"));
        assertEquals("c should still be third", 2, basicMap.get("c"));
    }

    public void testCreateVectorHack() throws Exception {
        basicMap.createVectorHack();
        assertEquals("Number 0 should yield a", "a", basicMap.get(0));
        assertEquals("Number 1 should yield b", "b", basicMap.get(1));
        assertEquals("Number 2 should yield c", "c", basicMap.get(2));
        //TODO: Test goes here...
    }

    public void testGetSorted() throws Exception {
        assertEquals("The sorted should contain 3 elements",
                     3, basicMap.getSorted().size());
    }

    public void dumpMemory() throws Exception {
        int size = 500000;
        SortedHash<String> stringMap = new SortedHash<String>(size-2);
        Profiler pf = new Profiler();
        System.gc();
        System.out.println("Before: " + ClusterCommon.getMem());

        pf.reset();
        for (int i = 0 ; i < size ; i++) {
            if (i % (size / 50) == 0) {
                System.out.print(".");
            }
            stringMap.add("Some string " + i);
        }
        System.out.println("");
        String t = pf.getSpendTime();
        System.gc();
        System.out.println("After fill: " + ClusterCommon.getMem() +
                           ", in " + t);

        pf.reset();
        stringMap.get("Some string " + 0);
        t = pf.getSpendTime();
        System.gc();
        System.out.println("After get: " + ClusterCommon.getMem() +
                           ", in " + t);

        HashMap<String, Integer> hash = new HashMap<String, Integer>(size);
        System.gc();
        System.out.println("Starting hash-test with " + ClusterCommon.getMem());
        pf.reset();
        for (int i = 0 ; i < size ; i++) {
            if (i % (size / 50) == 0) {
                System.out.print(".");
            }
            hash.put("Some string " + i, i);
        }
        System.out.println("");
        t = pf.getSpendTime();
        System.gc();
        System.out.println("After hash: " + ClusterCommon.getMem() +
                           ", in " + t);


    }

    public void testGetList() throws Exception {
        assertEquals("The list should contain 3 elements",
                     3, basicMap.getList().size());
    }

    public static Test suite() {
        return new TestSuite(SortedHashTest.class);
    }
}
