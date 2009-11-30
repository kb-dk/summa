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

/*    public void testBitSetMath() {
        int elementBits = 3;
        int index = 0;
        int[] elements = new int[2];

        // Trivial
        int value = 7; // 111
        int[] expected = new int[2];
        expected[0] = (value) << (BitsArrayPacked.BLOCK_SIZE-3);

        testBitSetMath(elementBits, index, elements, value, expected);

        elements = new int[2];
        value = 7; // 111
        index = 20;
        expected = new int[2];
        expected[0] = ((int)value) << (1);
        testBitSetMath(elementBits, index, elements, value, expected);

        elements = new int[2];
        value = 7; // 111
        index = 20;
        expected = new int[2];
        expected[0] = ((int)value) << (1);
        testBitSetMath(elementBits, index, elements, value, expected);

        elements = new int[2];
        value = 7; // 111
        index = 21;
        expected = new int[2];
        expected[0] = ((int)value) >>> (2);
        expected[1] = ((int)value) << (BitsArrayPacked.BLOCK_SIZE - 2);
        testBitSetMath(elementBits, index, elements, value, expected);

        elements = new int[2];
        elements[0] = 1730000;
        elements[1] = 8787;
        value = 7; // 111
        index = 21;
        expected = new int[2];
        expected[0] = ((int)value) >>> (2) | elements[0];
        expected[1] = ((int)value) << (BitsArrayPacked.BLOCK_SIZE - 2) | elements[1];
        testBitSetMath(elementBits, index, elements, value, expected);
    }
  */
    public void testBitSetMath(int elementBits, int index, int[] elements,
                               int value, int[] expected) {
        int bitPos = index * elementBits;

        // Samples: 3 bits/value (A, B and C) stored in bytes
        // Case A: ???ABC??
        // Case B: ???????A BC??????

        int bytePos = (int)(bitPos / BitsArrayPacked.BLOCK_SIZE);    // Position in bytes
        int subPosLeft = (int)(bitPos % BitsArrayPacked.BLOCK_SIZE); // Position in the bits at bytePos
        // Case A: subPosLeft == 3, Case B: subPosLeft == 7

        // The number of remaining bits at bytePos+1
        int subRemainingBits = elementBits - (BitsArrayPacked.BLOCK_SIZE - subPosLeft);
        // Case A: -2, Case B: 2
        log.debug("subRemainingBits=" + subRemainingBits);
        if (subRemainingBits > 0) {
            log.debug("Modifying elements[" + bytePos + "] and next ="
                      + toBinary(elements[bytePos]) + " "
                      + toBinary(elements[bytePos+1]));
            // Case B: ???????A BC??????
            elements[bytePos] =
                    ((elements[bytePos] & (~0 << (elementBits - subPosLeft))))
                    | ((int)value >>> subRemainingBits);
            elements[bytePos+1] =
                    ((elements[bytePos+1] & (~0 >>> subRemainingBits)))
                    | ((int)value << (BitsArrayPacked.BLOCK_SIZE - subRemainingBits));
        } else {
            // Case A: ???ABC??, subPosLeft == 3, subRemainingBits == -2
            log.debug("Modifying elements[" + bytePos + "]="
                      + toBinary(elements[bytePos]));
            elements[bytePos] =
                    (elements[bytePos]
                     & ((subPosLeft == 0 ? 0
                         : ~0 << (BitsArrayPacked.BLOCK_SIZE - subPosLeft))
                        | (~0 >>> (elementBits - -subRemainingBits))))
                    | ((int)value <<
                       (BitsArrayPacked.BLOCK_SIZE - subPosLeft - elementBits));
        }

        String message = String.format(
                "elementBits=%d, index=%d, value=%d, expected, actual:\n%s\n%s",
                elementBits, index, value, Strings.join(toList(expected), ", "), 
                Strings.join(toList(elements), ", "));
        assertTrue(message, Arrays.equals(elements, expected));
        log.debug(message);
    }

    @SuppressWarnings({"unchecked"})
    public List<Object> toList(int[] elements) {
        List result = new ArrayList(elements.length);
        for (int element: elements) {
            result.add(toBinary(element));
        }
        return result;
    }

    public String toBinary(int l) {
        String s = Long.toBinaryString(l);
        while (s.length() < BitsArrayPacked.BLOCK_SIZE) {
            s = "0" + s;
        }
        return s;
    }

    public void testPlainGetSet() throws Exception {
        BitsArrayPacked ba = new BitsArrayPacked();
        ba.set(4, 3);
        ba.set(1, 1);
        ba.set(2, 5);
        assertContains("Simple addition", ba, Arrays.asList(0, 1, 5, 0, 3));
    }

    public void testSpanningGetSet() throws Exception {
        BitsArrayPacked ba = new BitsArrayPacked();
        ba.set(0, 256);
        ba.set(7, 287);
        ba.set(8, 288);
        assertContains("Spanning ints",
                       ba, Arrays.asList(256, 0, 0, 0, 0, 0, 0, 287, 288));
    }

    public void testSpanningGetSetSingle() throws Exception {
        BitsArrayPacked ba = new BitsArrayPacked();
        ba.set(7, 287);
        assertContains("Spanning int",
                       ba, Arrays.asList(0, 0, 0, 0, 0, 0, 0, 287));
    }

    public void testHighGetSet() throws Exception {
        BitsArrayPacked ba = new BitsArrayPacked();
        ba.set(0, 256);
        assertContains("0, 256",
                       ba, Arrays.asList(256));
    }

    public void testTrivialGetSet() throws Exception {
        BitsArrayPacked ba = new BitsArrayPacked();
        ba.set(0, 1);
        assertContains("Trivial case", ba, Arrays.asList(1));
    }

    public void testNearlyTrivialGetSet() throws Exception {
        BitsArrayPacked ba = new BitsArrayPacked();
        ba.set(0, 1);
        ba.set(1, 2);
        assertContains("Nearly trivial case", ba, Arrays.asList(1, 2));
    }

    private void assertContains(
            String message, BitsArrayPacked ba, List<Integer> expected) {
        assertEquals(message + ". The BitsArrayPacked.size() should be as expected",
                     expected.size(), ba.size());
        assertEquals(message + ". The BitsArrayPacked content should be as expected",
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
            long baiTime = testPerformanceBAI(
                    MAX, INITIAL_MAX_LENGTH, INITIAL_MAX_VALUE);
            long plainTime = testPerformancePlain(MAX);
            System.out.println(String.format(
                    "Write %d: BAP=%dms, BAI=%dms, int[]=%dms",
                    MAX, baTime, baiTime, plainTime));
        }
    }

    public void testReadPerformance() {
        int MAX = 1000000;
        int READS = MAX * 10;
        int INITIAL_MAX_LENGTH = MAX;
        int INITIAL_MAX_VALUE = 240;
        int WARMUP = 2;
        int RUNS = 5;

        BitsArrayPacked ba = makeBA(MAX, INITIAL_MAX_LENGTH, INITIAL_MAX_VALUE);
        BitsArrayInt bai = makeBAI(MAX, INITIAL_MAX_LENGTH, INITIAL_MAX_VALUE);
        log.debug("Created " + ba);
        int[] a = makePlain(MAX, INITIAL_MAX_VALUE);
        System.out.println(String.format(
                "Memory usage: BAP~=%dKB, BAI~=%dKB, int[]~=%dKB -> %s",
                ba.getMemSize() / 1024, bai.getMemSize() / 1024,
                a.length*4 / 1024, ba));

        for (int i = 0 ; i < WARMUP ; i++) {
            testReadBA(ba, READS);
            testReadBA(bai, READS);
            testReadPlain(a, READS);
        }
        for (int i = 0 ; i < RUNS ; i++) {
            long baTime = testReadBA(ba, READS);
            long baiTime = testReadBA(bai, READS);
            long plainTime = testReadPlain(a, READS);
            long baseTime = testReadCalibrate(a, READS);
            System.out.println(String.format(
                    "read %d (of %d max): "
                    + "BAP=%dms, BAI=%dms, int[]=%dms, null=%dms",
                    READS, MAX, baTime-baseTime, baiTime-baseTime,
                    plainTime-baseTime, baseTime));
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

    private long testReadCalibrate(int[] a, int reads) {
        System.gc();
        int MAX = a.length;
        long startTime = System.currentTimeMillis();
        Random random = new Random(88);
        int dummy = 0;
        for (int i = 0 ; i < reads ; i++) {
            random.nextInt(MAX);
        }
        return System.currentTimeMillis() - startTime;
    }

    private long testReadBA(BitsArray ba, int reads) {
        System.gc();
        int MAX = ba.size();
        long startTime = System.currentTimeMillis();
        Random random = new Random(88);
        for (int i = 0 ; i < reads ; i++) {
            ba.getAtomic(random.nextInt(MAX));
        }
        return System.currentTimeMillis() - startTime;
    }

/*    private long testReadBAI(final BitsArrayInt ba, final int reads) {
        System.gc();
        final int MAX = ba.size();
        long startTime = System.currentTimeMillis();
        Random random = new Random(88);
        for (int i = 0 ; i < reads ; i++) {
            ba.getAtomic(random.nextInt(MAX));
        }
        return System.currentTimeMillis() - startTime;
    }*/

    public long testPerformanceBA(
            int max, int initialMaxLength, int initialMaxValue) {
        System.gc();
        long startTime = System.currentTimeMillis();
        makeBA(max, initialMaxLength, initialMaxValue);
        return System.currentTimeMillis() - startTime;
    }

    public long testPerformanceBAI(
            int max, int initialMaxLength, int initialMaxValue) {
        System.gc();
        long startTime = System.currentTimeMillis();
        makeBAI(max, initialMaxLength, initialMaxValue);
        return System.currentTimeMillis() - startTime;
    }

    private BitsArrayPacked makeBA(
            int elements, int initialMaxLength, int initialMaxValue) {
        Random random = new Random(87);
        BitsArrayPacked ba = new BitsArrayPacked(initialMaxLength, initialMaxValue);
        for (int i = 0 ; i < elements ; i++) {
            ba.set(i, random.nextInt(initialMaxValue));
        }
        return ba;
    }

    private BitsArrayInt makeBAI(
            int elements, int initialMaxLength, int initialMaxValue) {
        Random random = new Random(87);
        BitsArrayInt ba = new BitsArrayInt(initialMaxLength);
        for (int i = 0 ; i < elements ; i++) {
            ba.set(i, random.nextInt(initialMaxValue));
        }
        return ba;
    }

    public long testPerformancePlain(int max) {
        System.gc();
        long startTime = System.currentTimeMillis();
        makePlain(max);
        return System.currentTimeMillis() - startTime;
    }

    private int[] makePlain(int length) {
        return makePlain(length, length);
    }
    private int[] makePlain(int length, int max) {
        Random random = new Random(87);
        int[] a = new int[length];
        for (int i = 0 ; i < length ; i++) {
            a[random.nextInt(max)] = i;
        }
        return a;
    }
}
