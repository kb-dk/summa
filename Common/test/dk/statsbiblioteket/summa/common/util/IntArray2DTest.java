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

import java.util.Random;

/**
 * IntArray2D Tester.
 *
 * @author <Authors name>
 * @since <pre>12/10/2009</pre>
 * @version 1.0
 */
public class IntArray2DTest extends TestCase {
    public IntArray2DTest(String name) {
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

    public static Test suite() {
        return new TestSuite(IntArray2DTest.class);
    }

    public void testSetGetValues() throws Exception {
        IntArray2D ia = new IntArray2D();
        ia.set(0, new int[]{9});
        ExtraAsserts.assertEqualsNoSort("Array at pos 0 should match (A)",
                                        new int[]{9}, ia.get(0));

        ia.set(10, new int[]{10, 11});
        ExtraAsserts.assertEqualsNoSort("Array at pos 0 should match (B)",
                                        new int[]{9}, ia.get(0));
        ExtraAsserts.assertEqualsNoSort("Array at pos 10 should match (A)",
                                        new int[]{10, 11}, ia.get(10));
        ia.set(9, new int[]{12});
        ExtraAsserts.assertEqualsNoSort("Array at pos 0 should match (C)",
                                        new int[]{9}, ia.get(0));
        ExtraAsserts.assertEqualsNoSort("Array at pos 9 should match",
                                        new int[]{12}, ia.get(9));
        ExtraAsserts.assertEqualsNoSort("Array at pos 10 should match (B)",
                                        new int[]{10, 11}, ia.get(10));
    }

    public void testMonkey() throws Exception {
        final int POS_MAX = 100;
        final int ARRAY_LENGTH_MAX = 10;
        final int ASSIGNMENTS = 75;
        int[][] base = createBase(
                POS_MAX, ASSIGNMENTS, ARRAY_LENGTH_MAX);
        IntArray2D ia = createIntArray2D(
                POS_MAX, ASSIGNMENTS, ARRAY_LENGTH_MAX);
        for (int pos = 0 ; pos <= POS_MAX ; pos++) {
            int[] b = base[pos] == null ? new int[0] : base[pos];
            int[] i = ia.get(pos);
            ExtraAsserts.assertEqualsNoSort(
                    "The arrays at position " + pos + " shold be equal", b, i);
        }

        boolean someCleared = false;
        for (int i = 10 ; i < POS_MAX ; i += 5) {
            if (base[i] != null) {
                base[i] = null;
                ia.dirtyClear(i);
                someCleared = true;
            }
        }
        assertTrue("Some values should be cleared", someCleared);

        boolean someTrulyCleared = false;
        for (int pos = 0 ; pos <= POS_MAX ; pos++) {
            int[] b = base[pos] == null ? new int[0] : base[pos];
            int[] i = ia.get(pos);
            ExtraAsserts.assertEqualsNoSort(
                    "The arrays at position " + pos
                    + " shold still be equal", b, i);
            someTrulyCleared = someTrulyCleared || ia.isCleared(pos);
        }
        assertTrue("Verification of clear should pass", someTrulyCleared);
    }

    private IntArray2D createIntArray2D(
            int POS_MAX, int ASSIGNMENTS, int ARRAY_LENGTH_MAX) {
        Random random = new Random(87);
        IntArray2D ia = new IntArray2D();
        for (int run = 0 ; run < ASSIGNMENTS ; run++) {
            int pos = random.nextInt(POS_MAX+1);
            int[] values = new int[random.nextInt(ARRAY_LENGTH_MAX+1)];
            for (int i = 0 ; i < values.length ; i++) {
                values[i] = random.nextInt();
            }
            ia.set(pos, values);
        }
        // Assign at end to avoid out of bounds
        ia.set(POS_MAX, new int[]{87});
        return ia;
    }

    private int[][] createBase(
            int POS_MAX, int ASSIGNMENTS, int ARRAY_LENGTH_MAX) {
        Random random = new Random(87);
        int[][] base = new int[POS_MAX+1][];
        for (int run = 0 ; run < ASSIGNMENTS ; run++) {
            int pos = random.nextInt(POS_MAX+1);
            int[] values = new int[random.nextInt(ARRAY_LENGTH_MAX+1)];
            for (int i = 0 ; i < values.length ; i++) {
                values[i] = random.nextInt();
            }
            base[pos] = values;
        }
        // Assign at end to avoid out of bounds
        base[POS_MAX] = new int[]{87};
        return base;
    }

    public void testAppend() throws Exception {
        IntArray2D ia = new IntArray2D();
        ia.set(10, new int[]{10, 11});
        ia.set(11, new int[]{12});
        ia.append(11, 13);
        ia.append(10, 14);
        ia.append(9, 15);
        ia.append(20, 16);
        ExtraAsserts.assertEqualsNoSort("Array at pos 9 should match",
                                        new int[]{15}, ia.get(9));
        ExtraAsserts.assertEqualsNoSort("Array at pos 10 should match",
                                        new int[]{15}, ia.get(9));
        ExtraAsserts.assertEqualsNoSort("Array at pos 11 should match",
                                        new int[]{15}, ia.get(9));
        ExtraAsserts.assertEqualsNoSort("Array at pos 20 should match",
                                        new int[]{15}, ia.get(9));
        ExtraAsserts.assertEqualsNoSort("Array at pos 9 should match",
                                        new int[]{15}, ia.get(9));
    }
}

