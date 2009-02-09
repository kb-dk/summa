/* $Id$
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
package dk.statsbiblioteket.summa.common.util;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.unittest.ExtraAsserts;

/**
 * ArrayUtil Tester.
 *
 * @author <Authors name>
 * @since <pre>02/06/2009</pre>
 * @version 1.0
 */
public class ArrayUtilTest extends TestCase {
    public ArrayUtilTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testMergeArrays() throws Exception {
        assertMerge("Trivial",  new int[]{1, 2, 3},
                    new int[]{1, 2}, new int[]{3}, false, false);
        assertMerge("Doublets",  new int[]{1, 2, 2, 3},
                    new int[]{1, 2}, new int[]{2, 3}, false, false);
        assertMerge("Doublets remove",  new int[]{1, 2, 3},
                    new int[]{1, 2}, new int[]{2, 3}, true, false);
        assertMerge("Sort",  new int[]{1, 2, 3},
                    new int[]{2, 3}, new int[]{1}, false, true);
        assertMerge("Non-sort",  new int[]{2, 3, 1},
                    new int[]{2, 3}, new int[]{1}, false, true);
    }

    private void assertMerge(String message, int[] expected,
                             int[] primary, int[] secondary,
                             boolean removeduplicates, boolean sort) {
        ExtraAsserts.assertEquals(
                "Merge([" + ExtraAsserts.dump(primary) + "], "
                + ExtraAsserts.dump(secondary) + "), remove duplicates: "
                + removeduplicates + ", sort: " + sort + ". " + message,
                expected,
                ArrayUtil.mergeArrays(primary, secondary,
                                      removeduplicates, sort));

    }



    public static Test suite() {
        return new TestSuite(ArrayUtilTest.class);
    }
}
