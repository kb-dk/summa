package dk.statsbiblioteket.summa.common.util;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.util.Comparator;
import java.util.Random;
import java.util.Arrays;

import dk.statsbiblioteket.summa.common.unittest.ExtraAsserts;

public class IndirectLongSorterTest extends TestCase {
    public IndirectLongSorterTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(IndirectLongSorterTest.class);
    }

    public void testSimpleSort() {
        ExtraAsserts.assertEquals(
                "Super simple sorting should work",
                new long[]{1, 2},
                simpleSorter.sort(new long[]{2, 1}, myComparator));

        ExtraAsserts.assertEquals(
                "Simple sorting should work",
                new long[]{1, 6, 9},
                simpleSorter.sort(new long[]{6, 1, 9}, myComparator));

        ExtraAsserts.assertEquals(
                "More simple sorting should work",
                new long[]{1, 2, 6, 9},
                simpleSorter.sort(new long[]{1, 9, 6, 2}, myComparator));

        ExtraAsserts.assertEquals(
                "Two number sorting should work",
                new long[]{1, 2},
                simpleSorter.sort(new long[]{2, 1}, myComparator));

        ExtraAsserts.assertEquals(
                "No change number sorting should work",
                new long[]{1, 2, 3, 4, 5},
                simpleSorter.sort(new long[]{1, 2, 3, 4, 5}, myComparator));
    }

    public void testSubSorting() throws Exception {
        ExtraAsserts.assertEquals(
                "Partial sort should be partial",
                new long[]{5, 4, 2, 3, 1},
                simpleSorter.sort(new long[]{5, 4, 3, 2, 1}, 2, 4, 
                                  myComparator));
    }

    public void testSimpleMonkey() throws Exception {
        int RUNS = 1000;
        Random random = new Random(100);
        for (int i = 0 ; i < RUNS ; i++) {
            long[] data = new long[random.nextInt(100)];
            for (int j = 0 ; j < data.length ; j++) {
                data[j] = random.nextLong();
            }
            long[] expected = new long[data.length];
            System.arraycopy(data, 0, expected, 0, data.length);
            Arrays.sort(expected);
            ExtraAsserts.assertEquals(
                    "Monkey sort should work",
                    expected, simpleSorter.sort(data, myComparator));
        }
    }

    private IndirectLongSorter<String> mySorter =
            new IndirectLongSorter<String>() {
                protected String getValue(long reference) {
                    return Long.toString(Math.abs(5 - reference));
                }
            };

    private IndirectLongSorter<String> simpleSorter =
            new IndirectLongSorter<String>() {
                protected String getValue(long reference) {
                    return Long.toString(reference);
                }
            };

    private Comparator<String> myComparator = new Comparator<String>() {
        public int compare(String o1, String o2) {
            return Long.valueOf(o1).compareTo(Long.valueOf(o2));
        }
    };
}
