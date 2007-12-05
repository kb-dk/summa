/* $Id: PriorityQueueLongTest.java,v 1.2 2007/10/04 13:28:20 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:20 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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
        author = "te")
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

    public void testGetMin() throws Exception {
        //TODO: Test goes here...
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
        //TODO: Test goes here...
    }

    public static Test suite() {
        return new TestSuite(PriorityQueueLongTest.class);
    }
}
