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

import java.util.Arrays;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * PriorityQueueLong Tester.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te, hbk")
public class PriorityQueueLongTest extends TestCase {
    public PriorityQueueLongTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    private PriorityQueueLong setUpPriorityQueue(long[] input) {
        PriorityQueueLong queue = new PriorityQueueLong();
        for (long element: input) {
            queue.insert(element);
        }

        return queue;
    }

    public void testGetMin() throws Exception {
        long[] input = new long[] { 5, 7, 3, 10, 12, 2, 5, 2 };
        long[] expected = input.clone();
        Arrays.sort(expected);
        PriorityQueueLong queue = setUpPriorityQueue(input);
        for (long element: expected) {
            assertEquals("The getMin should return the smallest element "
                    +"without removing it", element, queue.getMin());
            assertEquals("The order of the output should be as expected",
                         element, queue.removeMin());
        }
    }

    public static long[] reverseArray(long[] list) {
        long[] returnArray = new long[list.length];
        for(int i=0; i<list.length; i++) {
            returnArray[(returnArray.length-i-1)] = list[i];
        }
        return returnArray;
    }

    public static Long[] reverseArray(Long[] list) {
        Long[] returnArray = new Long[list.length];
        for(int i=0; i<list.length; i++) {
            returnArray[(returnArray.length-i-1)] = list[i];
        }
        return returnArray;
    }

    public void testSetValues() throws Exception {
        long[] input = new long[] { 5, 7, 3, 10, 12, 2, 5, 2 };
        PriorityQueueLong queue = setUpPriorityQueue(input);
        long[] input2 = new long[] { 13, 4, 6, 8, 9, 10, 1, 13};
        long[] expected = input2.clone();
        Arrays.sort(expected);
        queue.setValues(input2, 8, true, 16);
        for (long element: expected) {
            assertEquals("The order of the output should be as expected",
                         element, queue.removeMin());
        }
    }

    public void testOrder() throws Exception {
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

    public void testGetSize() throws Exception {
        long[] input = new long[] { 5, 7, 3, 10, 12, 2, 5, 2 };
        long[] expected = input.clone();
        Arrays.sort(expected);
        PriorityQueueLong queue = setUpPriorityQueue(input);
        for (int expectedSize = 0; expectedSize < expected.length;
             expectedSize++) {
            assertEquals(expected.length-expectedSize, queue.getSize());
            queue.removeMin();
        }
        assertEquals(0, queue.getSize());
    }

    public static Test suite() {
        return new TestSuite(PriorityQueueLongTest.class);
    }
}




