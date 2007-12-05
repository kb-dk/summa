/* $Id: RecordIteratorTest.java,v 1.3 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/05 10:20:22 $
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
package dk.statsbiblioteket.summa.storage.io;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * RecordIterator Tester.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class RecordIteratorTest extends TestCase {
    public RecordIteratorTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(RecordIteratorTest.class);
    }

    public void testBasicIteration() throws Exception {
        testExtendedIteration(10);
    }

    public void testExtendedIteration() throws Exception {
        testExtendedIteration(RecordIterator.MAX_QUEUE_SIZE * 3 + 7);
    }

    public void testExtendedIteration(int recordCount) throws Exception {
        FakeAccess access = new FakeAccess(recordCount);
        RecordIterator iterator = access.getRecords("foo");
        assertTrue("Access should have something", iterator.hasNext());
        for (int i = 0 ; i < recordCount ; i++) {
            try {
                Record record = iterator.next();
                assertEquals("The ID for record #" + i + " should be " + i,
                             Integer.toString(i), record.getId());
            } catch (Exception e) {
                fail("Could not request record with i = " + i + ": "
                     + e.getMessage());
            }
        }
        assertFalse("There should be no more records", iterator.hasNext());
        try {
            iterator.next();
            fail("Requesting more elements should throw an exception");
        } catch (Exception e) {
            // Expected
        }
    }


}
