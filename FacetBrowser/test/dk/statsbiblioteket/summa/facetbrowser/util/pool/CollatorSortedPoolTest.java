/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.facetbrowser.BaseObjects;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.Arrays;
import java.util.Locale;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class CollatorSortedPoolTest extends TestCase {
    public CollatorSortedPoolTest(String name) {
        super(name);
    }
    public void setUp() throws Exception {
        super.setUp();
    }
    public void tearDown() throws Exception {
        super.tearDown();
    }

    Collator defaultCollator = new CachedCollator(new Locale("da"));

    private void testSorting(CollatorSortedPool pool) {
        pool.clear();
        String[] terms = new String[]{"a", "å", "", "ø", "æ", "/"};
        pool.addAll(Arrays.asList(terms));
        Arrays.sort(terms, defaultCollator);
        assertEquals(terms, pool);
    }
    public void testMemoryBasedSorting() throws Exception {
        MemoryStringPool pool = new MemoryStringPool(defaultCollator);
        pool.open(new File(System.getProperty("java.io.tmpdir"),
                           "facetTestMem"),
                  "testpool", false, true);
        testSorting(pool);
    }
    public void testDiskBasedSorting() throws Exception {
        DiskStringPool pool = new DiskStringPool(defaultCollator);
        pool.open(new File(System.getProperty("java.io.tmpdir"),
                           "facetTestDisk"),
                  "testpool", false, true);
        testSorting(pool);
    }

    public void assertEquals(String[] expectedTerms, SortedPool pool) {
        assertEquals("The number of terms hould match",
                     expectedTerms.length, pool.size());
        for (int i = 0 ; i < pool.size() ; i++) {
            assertEquals("Terms at position " + i + " should match",
                         expectedTerms[i], pool.get(i));
        }
    }

    public void testMutation(CollatorSortedPool pool) throws IOException {
        pool.open(StringPoolSuperTest.poolDir, "foo", false, true);
        pool.clear();
        pool.setCollator(null);
        assertEquals(new String[0], pool);
        pool.add("b");
        pool.add("c");
        pool.add("a");
        assertEquals(new String[]{"a", "b", "c"}, pool);
        pool.remove(0);
        assertEquals(new String[]{"b", "c"}, pool);
        pool.remove(1);
        assertEquals(new String[]{"b"}, pool);
        pool.add("b");
        assertEquals(new String[]{"b"}, pool);
        pool.add("b");
        pool.add("c");
        assertEquals(new String[]{"b", "c"}, pool);
        pool.remove(1);
        assertEquals(new String[]{"b"}, pool);
        pool.remove(0);
        assertEquals(new String[0], pool);
        pool.add("b");
        pool.add("c");
        pool.add("a");
        try {
            pool.remove(10);
            fail("Removing a non-existing entry should throw an exception");
        } catch(IndexOutOfBoundsException e) {
            // Expected
        }
        pool.clear();
        assertEquals(new String[0], pool);
        pool.close();
    }
    public void testMemoryBasedMutation() throws Exception {
        testMutation(new MemoryStringPool(defaultCollator));
    }
    public void testDiskBasedMutation() throws Exception {
        testMutation(new DiskStringPool(defaultCollator));
    }

    // TODO: Save with disk, open with mem - check order
}



