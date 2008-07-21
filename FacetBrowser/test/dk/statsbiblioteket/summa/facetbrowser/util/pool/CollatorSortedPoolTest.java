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

import java.util.Locale;
import java.util.Collections;
import java.util.Arrays;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.CachedCollator;
import junit.framework.TestCase;

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

    private void testSorting(CollatorSortedPool pool) {
        CachedCollator collator = new CachedCollator(new Locale("da"));
        pool.setCollator(collator);
        pool.clear();
        String[] terms = new String[]{"a", "å", "", "ø", "æ", "/"};
        for (String term: terms) {
            pool.add(term);
        }
        assertEquals("The number of terms hould match",
                     terms.length, pool.size());
        Arrays.sort(terms, collator);
        for (int i = 0 ; i < pool.size() ; i++) {
            assertEquals("Terms at position " + i + " should match",
                         terms[i], pool.getValue(i));
        }
    }

    public void testMemoryBased() throws Exception {
        MemoryStringPool pool = new MemoryStringPool();
        testSorting(pool);
    }

    public void testDiskBased() throws Exception {
        DiskStringPool pool = new DiskStringPool();
        testSorting(pool);
    }
}
