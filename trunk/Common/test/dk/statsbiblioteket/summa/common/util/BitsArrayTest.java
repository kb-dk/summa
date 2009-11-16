package dk.statsbiblioteket.summa.common.util;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

import dk.statsbiblioteket.util.Strings;

public class BitsArrayTest extends TestCase {
    public BitsArrayTest(String name) {
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
        return new TestSuite(BitsArrayTest.class);
    }

    public void testBitCalc() throws Exception {
        int[][] TESTS = new int[][]{ // maxValue, bits
                {1, 1}, {2, 2}, {3, 2}, {4, 3}, {7, 3}, {8, 4},
                {15, 4}, {16, 5}};
        for (int[] test: TESTS) {
            int bits = (int)Math.ceil(Math.log(test[0]+1)/Math.log(2));
            assertEquals("The calculated number of bits needed for " + test[0]
                         + " should be correct", test[1], bits);
        }
    }

    public void testPlainGetSet() throws Exception {
        BitsArray ba = new BitsArray();
        ba.set(4, 3);
        ba.set(1, 1);
        ba.set(2, 5);
        assertContains("Simple addition", ba, Arrays.asList(0, 1, 5, 0, 3));
    }

    public void testSpanningGetSet() throws Exception {
        BitsArray ba = new BitsArray();
        ba.set(0, 256);
        ba.set(7, 287);
        ba.set(8, 288);
        assertContains("Spanning longs",
                       ba, Arrays.asList(256, 0, 0, 0, 0, 0, 0, 287, 288));
    }

    public void testTrivialGetSet() throws Exception {
        BitsArray ba = new BitsArray();
        ba.set(0, 1);
        assertContains("Trivial case", ba, Arrays.asList(1));
    }

    public void testNearlyTrivialGetSet() throws Exception {
        BitsArray ba = new BitsArray();
        ba.set(0, 1);
        ba.set(1, 2);
        assertContains("Nearly trivial case", ba, Arrays.asList(1, 2));
    }

    private void assertContains(
            String message, BitsArray ba, List<Integer> expected) {
        assertEquals(message + ". The BitsArray.size() should be as expected",
                     expected.size(), ba.size());
        assertEquals(message + ". The BitsArray content should be as expected",
                     Strings.join(expected, ", "), Strings.join(ba, ", "));
    }
}
