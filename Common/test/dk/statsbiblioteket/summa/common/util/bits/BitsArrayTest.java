package dk.statsbiblioteket.summa.common.util.bits;

import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.summa.common.unittest.ExtraAsserts;
import dk.statsbiblioteket.summa.common.util.bits.test.BitsArrayPerformance;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.io.StringWriter;

@SuppressWarnings({"UnnecessaryLocalVariable"})
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

    @SuppressWarnings({"PointlessBitwiseExpression"})
    public void testBitCalc() throws Exception {
        int[][] TESTS = new int[][]{ // maxValue, bits
                {1, 1}, {2, 2}, {3, 2}, {4, 3}, {7, 3}, {8, 4},
                {15, 4}, {16, 5}};
        for (int[] test: TESTS) {
            int bits = (int)Math.ceil(Math.log(test[0]+1)/Math.log(2));
            assertEquals("The calculated number of bits needed for " + test[0]
                         + " should be correct", test[1], bits);
        }
        int es = 87;
        if (es != 67) {
            es = 87;
        }
        assertEquals("SHL 0 should not change anything", es, es << 0);
        assertEquals("SHR 0 should not change anything", es, es >>> 0);
    }

    public void testAssignPrevious() throws Exception {
        BitsArray ba = new BitsArrayPacked(100, 10);
        ba.set(1, 1);
        ba.set(0, 1);
        assertEquals("The value at position 1 should be unchanged when the "
                     + "value at position 0 is modified", 1, ba.getAtomic(1));
    }

    public void testReadZero() throws Exception {
        BitsArray ba = new BitsArrayPacked(100, 10);
        ba.set(10, 1);
        ba.set(5, 1);
        for (int i = 0 ; i < 10 ; i++) {
            if (i == 5) {
                continue;
            }
            assertEquals(String.format(
                    "The value at position %d should be the initial value", i),
                    0, ba.getAtomic(i));
        }
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
        BitsArrayPacked ba = new BitsArrayPacked(100, 10);
        ba.set(0, 256);
        log.debug("After set(0,256): " + Strings.join(ba, ", "));
        ba.set(7, 287);
        log.debug("After set(7,288): " + Strings.join(ba, ", "));
        ba.set(8, 288);
        assertContains("Spanning ints",
                       ba, Arrays.asList(256, 0, 0, 0, 0, 0, 0, 287, 288));
    }

    public void testSingleSPanning() throws Exception {
        BitsArrayPacked ba = new BitsArrayPacked(100, 10);
        ba.set(7, 256);
        assertContains("Spanning ints",
                       ba, Arrays.asList(0, 0, 0, 0, 0, 0, 0, 256));
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
        int LENGTH = 10000000;
        int MAX_VALUE = 240;
        int INITIAL_MAX_LENGTH = LENGTH;
        int WARMUP = 2;
        int RUNS = 3;
        List<BitsArrayPerformance.BitsArrayGenerator> bags = BitsArrayPerformance.getGenerators();
        for (int i = 0 ; i < WARMUP ; i++) {
            for (BitsArrayPerformance.BitsArrayGenerator bag: bags) {
                testPerformanceBA(bag, LENGTH, INITIAL_MAX_LENGTH, MAX_VALUE);
            }
            testPerformancePlain(LENGTH);
        }
        
        System.out.println(String.format(
                "Measuring write-performance calibrated with null-speed "
                + "for %d random[0-%d] value assignments",
                LENGTH,  MAX_VALUE));
        List<Long> timings = new ArrayList<Long>(bags.size());
        for (int run = 0 ; run < RUNS ; run++) {
            for (BitsArrayPerformance.BitsArrayGenerator bag: bags) {
                timings.add(testPerformanceBA(
                        bag, LENGTH, INITIAL_MAX_LENGTH, MAX_VALUE));
            }
            long plainTime = testPerformancePlain(LENGTH);
            long baseTime = testPerformanceCalibrate(LENGTH);

            StringWriter sw = new StringWriter(1000);
            sw.append(String.format(
                    "Write %d (of %d values): ", LENGTH, LENGTH));
            sw.append("int[]=").append(Long.toString(plainTime-baseTime));
            sw.append("ms, null=").append(Long.toString(baseTime));
            sw.append("ms");
            for (int i = 0 ; i < bags.size() ; i++) {
                sw.append(", ");
                sw.append(bags.get(i).create(1, 1).getClass().getSimpleName());
                sw.append("=").append(Long.toString(timings.get(i)-baseTime));
                sw.append("ms");
            }
            System.out.println(sw.toString());
        }
    }

    public void testReadPerformance() {
        int LENGTH = 100000;
        int READS = LENGTH * 100;
        int INITIAL_MAX_LENGTH = LENGTH;
        int MAX_VALUE = 255;
        int WARMUP = 2;
        int RUNS = 5;

        List<BitsArray> bas = new ArrayList<BitsArray>();
        for (BitsArrayPerformance.BitsArrayGenerator generator: BitsArrayPerformance.getGenerators()) {
            bas.add(makeBA(generator, LENGTH, INITIAL_MAX_LENGTH, MAX_VALUE));
        }
        log.debug("Created " + bas.size() + " BitsArrays");
        int[] a = makePlain(LENGTH, MAX_VALUE);

        System.out.println(String.format(
                "Measuring read-performance calibrated with null-speed "
                + "for %d value reads from %d random[0-%d] values",
                READS, LENGTH, MAX_VALUE));

        StringWriter sw = new StringWriter(1000);
        sw.append("Memory usage: int[]=");
        sw.append(Integer.toString(a.length*4 / 1024));
        sw.append("KB");
        for (BitsArray ba: bas) {
            sw.append(", ");
            sw.append(ba.getClass().getSimpleName());
            sw.append("~=").append(Integer.toString(ba.getMemSize() / 1024));
            sw.append("KB");
        }

        System.out.println(sw.toString());

        for (int i = 0 ; i < WARMUP ; i++) {
            for (BitsArray ba: bas) {
                testReadBA(ba, READS);
            }
            testReadPlain(a, READS);
        }

        List<Long> timings = new ArrayList<Long>(bas.size());
        for (int run = 0 ; run < RUNS ; run++) {
            for (BitsArray ba: bas) {
                timings.add(testReadBA(ba, READS));
            }
            long plainTime = testReadPlain(a, READS);
            long baseTime = testReadCalibrate(a, READS);

            sw = new StringWriter(1000);
            sw.append(String.format("Read %d (of %d values): ", READS, LENGTH));
            sw.append("int[]=").append(Long.toString(plainTime-baseTime));
            sw.append("ms, null=").append(Long.toString(baseTime));
            sw.append("ms");
            for (int i = 0 ; i < bas.size() ; i++) {
                sw.append(", ");
                sw.append(bas.get(i).getClass().getSimpleName());
                sw.append("=").append(Long.toString(timings.get(i)-baseTime));
                sw.append("ms");
            }
            System.out.println(sw.toString());
        }

    }

    public void testMonkey() throws Exception {
        int LENGTH = 1000;
        int MAX = 1000;

        List<BitsArrayPerformance.BitsArrayGenerator> bags = BitsArrayPerformance.getGenerators();
        List<BitsArray> bas = new ArrayList<BitsArray>(bags.size());
        for (BitsArrayPerformance.BitsArrayGenerator bag: bags) {
            bas.add(makeBA(bag, LENGTH, LENGTH, MAX));
        }
        int[] plain = makePlain(LENGTH, MAX);

        for (BitsArray ba: bas) {
            int[] baArray = new int[ba.size()];
            for (int i = 0 ; i < ba.size() ; i++) {
                baArray[i] = ba.get(i);
            }
            ExtraAsserts.assertEqualsNoSort(String.format(
                    "The %s should match", ba.getClass().getSimpleName()),
                                            plain, baArray);
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
            dummy = random.nextInt(MAX);
        }
        if (dummy == -1) {
            throw new IllegalStateException("Dummy JIT-tricker was -1");
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
            a[i] = random.nextInt(max);
        }
        return a;
    }

    public long testPerformanceCalibrate(int max) {
        System.gc();
        long startTime = System.currentTimeMillis();
        Random random = new Random(87);
        int t = 0;
        for (int i = 0 ; i < max ; i++) {
            t = random.nextInt(max);
        }
        if (t == max+1) {
            System.err.println("Failed JIT-tricking sanity check");
        }
        return System.currentTimeMillis() - startTime;
    }

    public long testPerformanceBA(BitsArrayPerformance.BitsArrayGenerator generator,
            int assignments, int constructorLength, int maxValue) {
        System.gc();
        long startTime = System.currentTimeMillis();
        makeBA(generator, assignments, constructorLength, maxValue);
        return System.currentTimeMillis() - startTime;
    }

    private BitsArray makeBA(BitsArrayPerformance.BitsArrayGenerator generator,
            int assignments, int constructorLength, int maxValue) {
        Random random = new Random(87);
        BitsArray ba = generator.create(constructorLength, maxValue);
        for (int i = 0 ; i < assignments ; i++) {
            ba.set(i, random.nextInt(maxValue));
        }
        return ba;
    }
}
