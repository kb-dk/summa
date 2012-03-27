/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.common.util;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.unittest.ExtraAsserts;
import dk.statsbiblioteket.util.Strings;

import java.util.Arrays;

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

    public void testReverse() {
        int[][][] tests = new int[][][] {
                {{}, {}},
                {{1}, {1}},
                {{1, 2}, {2, 1}},
                {{1, 2, 3}, {3, 2, 1}},
                {{1, 2, 3, 4}, {4, 3, 2, 1}}
        };
        for (int[][] test: tests) {
            ArrayUtil.reverse(test[0]);
            //noinspection DuplicateStringLiteralInspection
            assertTrue("Reversal to " + ExtraAsserts.dump(test[1]) 
                       + " should work",
                       Arrays.equals(test[1], test[0]));
        }
    }

    public static Test suite() {
        return new TestSuite(ArrayUtilTest.class);
    }

    public void testMakeRoom() throws Exception {
        int[] foo = new int[10];
        foo[9] = 87;
        int[] foo2 = ArrayUtil.makeRoom(foo, 10, 1.2, 100, 1);
        assertEquals("The length should be adjusted properly",
                     12, foo2.length);
        assertEquals("Content should be preserved", 87, foo2[9]);
    }
}

