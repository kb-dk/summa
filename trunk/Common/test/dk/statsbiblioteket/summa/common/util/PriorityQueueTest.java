package dk.statsbiblioteket.summa.common.util;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Random;

import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class PriorityQueueTest extends TestCase {
    public PriorityQueueTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testPriorityQueue() throws Exception {
        long[] input = new long[] { 5, 7, 3, 10, 12, 2, 5, 2 };
        long[] expected = input.clone();
        Arrays.sort(expected);
        PriorityQueueLong queue = new PriorityQueueLong();
        for (long element: input) {
            queue.insert(element);
        }
        for (long element: expected) {
            assertEquals("The order of the output should be as expected",
                         element, queue.removeMin());
        }

    }

    public void testPriorityQueueRandom() throws Exception {
        int maxLength = 100;
        int maxValue = 100;
        int reRuns = 100;
        Random random = new Random();
        for (int i = 0 ; i < reRuns ; i++) {
            long[] input = new long[random.nextInt(maxLength)];
            for (int j = 0 ; j < input.length ; j++) {
                input[j] = random.nextInt(maxValue);
            }
            long[] expected = input.clone();
            Arrays.sort(expected);
            PriorityQueueLong queue = new PriorityQueueLong();
            for (long element: input) {
                queue.insert(element);
            }
            for (long element: expected) {
                assertEquals("The order of the output should be as expected "
                             + "by using insert",
                             element, queue.removeMin());
            }

            queue = new PriorityQueueLong();
            queue.setValues(input, input.length, false, input.length);
            for (long element: expected) {
                //noinspection DuplicateStringLiteralInspection
                assertEquals("The order of the output should be as expected "
                             + "by using set",
                             element, queue.removeMin());
            }
        }

    }
    // TODO: Fill the TagHandler from the index
    public void testSetValues() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetMin() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetSize() throws Exception {
        //TODO: Test goes here...
    }

    public static Test suite() {
        return new TestSuite(PriorityQueueTest.class);
    }
}



