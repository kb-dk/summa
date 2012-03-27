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

import java.util.*;

import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te, hbk")
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

    private PriorityQueue<Long> setUpPriorityQueue(long[] input) {
        PriorityQueue<Long> queue = new PriorityQueue<Long>(input.length);
        for (long element: input) {
            queue.insert(element);
        }

        return queue;
    }

    public void testPriorityQueue() throws Exception {
        long[] input = new long[] { 5, 7, 3, 10, 12, 2, 5, 2 };
        long[] expected = input.clone();
        Arrays.sort(expected);
        expected = PriorityQueueLongTest.reverseArray(expected);
        PriorityQueue<Long> queue = setUpPriorityQueue(input);
        for (long element: expected) {
            assertEquals("The order of the output should be as expected",
                         element, (long)queue.removeMin());
        }
    }

    public void testPriorityQueueRandom() throws Exception {
        int maxLength = 100;
        int maxValue = 100;
        int reRuns = 100;
        Random random = new Random();
        for (int i = 0 ; i < reRuns ; i++) {
            Long[] input = new Long[random.nextInt(maxLength)];
            for (int j = 0 ; j < input.length ; j++) {
                input[j] = new Long(random.nextInt(maxValue));
            }
            Long[] expected = input.clone();
            Arrays.sort(expected);
            expected = PriorityQueueLongTest.reverseArray(expected);
            PriorityQueue<Long> queue = new PriorityQueue<Long>(input.length);
            for (long element: input) {
                queue.insert(element);
            }
            for (long element: expected) {
                assertEquals("The order of the output should be as expected "
                             + "by using insert",
                             element, (long)queue.removeMin());
            }

            queue = new PriorityQueue<Long>();
            queue.setValues(input, input.length, false, input.length);
            assertEquals(expected.length, queue.getSize());
            for (long element: expected) {
                //noinspection DuplicateStringLiteralInspection
                assertEquals("The order of the output should be as expected "
                             + "in rerun '" + i + "', by using set",
                             element, (long)queue.removeMin());
            }
        }

    }

    public void testSetValues() throws Exception {
        long[] input = new long[] { 5, 7, 3, 10, 12, 2, 5, 2 };
        PriorityQueue<Long> queue = setUpPriorityQueue(input);
        long[] inputLong = new long[] { 13, 4, 6, 8, 9, 10, 1, 13 };
        Long[] input2 = new Long[8];
        for(int i=0; i<inputLong.length; i++) {
            input2[i] = inputLong[i];
        }

        Long[] expected = input2.clone();
        Arrays.sort(expected);
        expected = PriorityQueueLongTest.reverseArray(expected);
        queue.setValues(input2, 8, true, 16);
        for (long element: expected) {
            assertEquals("The order of the output should be as expected",
                         element, (long)queue.removeMin());
        }
    }

    public void testGetMin() throws Exception {
        long[] input = new long[] { 5, 7, 3, 10, 12, 2, 5, 2 };
        long[] expected = input.clone();
        Arrays.sort(expected);
        expected = PriorityQueueLongTest.reverseArray(expected);
        PriorityQueue<Long> queue = setUpPriorityQueue(input);
        for (long element: expected) {
            assertEquals("The getMin should return the smallest element "
                    +"without removing it", element, (long)queue.getMin());
            assertEquals("The order of the output should be as expected",
                         element, (long)queue.removeMin());
        }
    }

    public void testGetSize() throws Exception {
        long[] input = new long[] { 5, 7, 3, 10, 12, 2, 5, 2 };
        long[] expected = input.clone();
        Arrays.sort(expected);
        expected = PriorityQueueLongTest.reverseArray(expected);
        PriorityQueue<Long> queue = setUpPriorityQueue(input);
        for (int expectedSize = 0; expectedSize < expected.length;
             expectedSize++) {
            assertEquals(expected.length-expectedSize, queue.getSize());
            queue.removeMin();
        }
        assertEquals(0, queue.getSize());
    }

    public static Test suite() {
        return new TestSuite(PriorityQueueTest.class);
    }
}




