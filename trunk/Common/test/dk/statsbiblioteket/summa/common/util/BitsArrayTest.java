package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.Strings;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class BitsArrayTest extends TestCase {
    private static Log log = LogFactory.getLog(BitsArrayTest.class);

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

    public void testBitSetMath() {
        int elementBits = 3;
        int index = 0;
        long[] elements = new long[2];

        // Trivial
        int value = 7; // 111
        long[] expected = new long[2];
        expected[0] = ((long)value) << (64-3);

        testBitSetMath(elementBits, index, elements, value, expected);

        elements = new long[2];
        value = 7; // 111
        index = 20;
        expected = new long[2];
        expected[0] = ((long)value) << (1);
        testBitSetMath(elementBits, index, elements, value, expected);

        elements = new long[2];
        value = 7; // 111
        index = 20;
        expected = new long[2];
        expected[0] = ((long)value) << (1);
        testBitSetMath(elementBits, index, elements, value, expected);

        elements = new long[2];
        value = 7; // 111
        index = 21;
        expected = new long[2];
        expected[0] = ((long)value) >>> (2);
        expected[1] = ((long)value) << (62);
        testBitSetMath(elementBits, index, elements, value, expected);

        elements = new long[2];
        elements[0] = 1730000;
        elements[1] = 878787;
        value = 7; // 111
        index = 21;
        expected = new long[2];
        expected[0] = ((long)value) >>> (2) | elements[0];
        expected[1] = ((long)value) << (62) | elements[1];
        testBitSetMath(elementBits, index, elements, value, expected);
    }

    public void testBitSetMath(int elementBits, int index, long[] elements,
                               int value, long[] expected) {
        long bitPos = index * elementBits;

        // Samples: 3 bits/value (A, B and C) stored in bytes
        // Case A: ???ABC??
        // Case B: ???????A BC??????

        int bytePos = (int)(bitPos / 64);    // Position in bytes
        int subPosLeft = (int)(bitPos % 64); // Position in the bits at bytePos
        // Case A: subPosLeft == 3, Case B: subPosLeft == 7

        // The number of remaining bits at bytePos+1
        int subRemainingBits = elementBits - (64 - subPosLeft);
        // Case A: -2, Case B: 2
        log.debug("subRemainingBits=" + subRemainingBits);
        if (subRemainingBits > 0) {
            log.debug("Modifying elements[" + bytePos + "] and next ="
                      + toBinary(elements[bytePos]) + " "
                      + toBinary(elements[bytePos+1]));
            // Case B: ???????A BC??????
            elements[bytePos] =
                    ((elements[bytePos] & (~0L << (elementBits - subPosLeft))))
                    | ((long)value >>> subRemainingBits);
            elements[bytePos+1] =
                    ((elements[bytePos+1] & (~0L >>> subRemainingBits)))
                    | ((long)value << (64 - subRemainingBits));
        } else {
            // Case A: ???ABC??, subPosLeft == 3, subRemainingBits == -2
            log.debug("Modifying elements[" + bytePos + "]="
                      + toBinary(elements[bytePos]));
            elements[bytePos] =
                    (elements[bytePos]
                     & ((subPosLeft == 0 ? 0 : ~0L << (64 - subPosLeft))
                        | (~0L >>> (elementBits - -subRemainingBits))))
                    | ((long)value << (64 - subPosLeft - elementBits));
        }

        String message = String.format(
                "elementBits=%d, index=%d, value=%d, expected, actual:\n%s\n%s",
                elementBits, index, value, Strings.join(toList(expected), ", "), 
                Strings.join(toList(elements), ", "));
        assertTrue(message, Arrays.equals(elements, expected));
        log.debug(message);
    }

    @SuppressWarnings({"unchecked"})
    public List<Object> toList(long[] elements) {
        List result = new ArrayList(elements.length);
        for (long element: elements) {
            result.add(toBinary(element));
        }
        return result;
    }

    public String toBinary(long l) {
        String s = Long.toBinaryString(l);
        while (s.length() < 64) {
            s = "0" + s;
        }
        return s;
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

    public void testSpanningGetSetSingle() throws Exception {
        BitsArray ba = new BitsArray();
        ba.set(7, 287);
        assertContains("Spanning long",
                       ba, Arrays.asList(0, 0, 0, 0, 0, 0, 0, 287));
    }

    public void testHighGetSet() throws Exception {
        BitsArray ba = new BitsArray();
        ba.set(0, 256);
        assertContains("0, 256",
                       ba, Arrays.asList(256));
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

    public void testWritePerformance() {
        int MAX = 100000;
        int INITIAL_MAX_VALUE = MAX;
        int INITIAL_MAX_LENGTH = MAX;
        int WARMUP = 2;
        int RUNS = 3;
        for (int i = 0 ; i < WARMUP ; i++) {
            testPerformanceBA(MAX, INITIAL_MAX_LENGTH, INITIAL_MAX_VALUE);
            testPerformancePlain(MAX);
        }
        for (int i = 0 ; i < RUNS ; i++) {
            long baTime = testPerformanceBA(
                    MAX, INITIAL_MAX_LENGTH, INITIAL_MAX_VALUE);
            long plainTime = testPerformancePlain(MAX);
            System.out.println(String.format(
                    "Write %d: BA=%dms, int[]=%dms", MAX, baTime, plainTime));
        }
    }

    public void testReadPerformance() {
        int MAX = 100000;
        int READS = 1000000;
        int INITIAL_MAX_VALUE = MAX;
        int INITIAL_MAX_LENGTH = MAX;
        int WARMUP = 2;
        int RUNS = 3;

        BitsArray ba = makeBA(MAX, INITIAL_MAX_LENGTH, INITIAL_MAX_VALUE);
        int[] a = makePlain(MAX);
        System.out.println(String.format(
                "Memory usage: BA~=%dKB, int[]~=%dKB",
                ba.getMemSize() / 1024, a.length*4 / 1024));

        for (int i = 0 ; i < WARMUP ; i++) {
            testReadBA(ba, READS);
            testReadPlain(a, READS);
        }
        for (int i = 0 ; i < RUNS ; i++) {
            long baTime = testReadBA(ba, READS);
            long plainTime = testReadPlain(a, READS);
            System.out.println(String.format(
                    "read %d (of %d max): BA=%dms, int[]=%dms",
                    READS, MAX, baTime, plainTime));
        }

    }

    private long testReadPlain(int[] a, int reads) {
        System.gc();
        int MAX = a.length;
        long startTime = System.currentTimeMillis();
        Random random = new Random(88);
        int dummy = 0;
        for (int i = 0 ; i < reads ; i++) {
            dummy = a[random.nextInt(MAX)];
        }
        if (dummy == -1) {
            log.warn("Supposedly undrechable dummy log");
        }
        return System.currentTimeMillis() - startTime;
    }

    private long testReadBA(BitsArray ba, int reads) {
        System.gc();
        int MAX = ba.size();
        long startTime = System.currentTimeMillis();
        Random random = new Random(88);
        for (int i = 0 ; i < reads ; i++) {
            ba.get(random.nextInt(MAX));
        }
        return System.currentTimeMillis() - startTime;
    }

    public long testPerformanceBA(
            int max, int initialMaxLength, int initialMaxValue) {
        System.gc();
        long startTime = System.currentTimeMillis();
        makeBA(max, initialMaxLength, initialMaxValue);
        return System.currentTimeMillis() - startTime;
    }

    private BitsArray makeBA(
            int elements, int initialMaxLength, int initialMaxValue) {
        Random random = new Random(87);
        BitsArray ba = new BitsArray(initialMaxLength, initialMaxValue);
        for (int i = 0 ; i < elements ; i++) {
            ba.set(random.nextInt(elements), i);
        }
        return ba;
    }

    public long testPerformancePlain(int max) {
        System.gc();
        long startTime = System.currentTimeMillis();
        makePlain(max);
        return System.currentTimeMillis() - startTime;
    }

    private int[] makePlain(int max) {
        Random random = new Random(87);
        int[] a = new int[max];
        for (int i = 0 ; i < max ; i++) {
            a[random.nextInt(max)] = i;
        }
        return a;
    }
}
