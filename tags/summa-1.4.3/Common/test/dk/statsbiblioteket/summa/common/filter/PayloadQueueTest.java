/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.common.filter;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.util.concurrent.Semaphore;

import dk.statsbiblioteket.summa.common.Record;

/**
 * PayloadQueue Tester.
 *
 * @author <Authors name>
 * @since <pre>11/05/2008</pre>
 * @version 1.0
 */
public class PayloadQueueTest extends TestCase {
    public PayloadQueueTest(String name) {
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

    public void deprecatedTestSemaphore() throws InterruptedException {
        Semaphore one = new Semaphore(1);
        one.release();
        one.release();
        assertEquals("1: Available permits should be the initial max",
                     1, one.availablePermits());
        one.acquire(1);
        assertEquals("2: Available permits should be none",
                     0, one.availablePermits());
        one.release();
        assertEquals("3: Available permits should be the initial max",
                     1, one.availablePermits());
        one.release();
        assertEquals("4: Available permits should be the initial max",
                     1, one.availablePermits());
    }

    public void testUninterruptibleOffer() throws Exception {
        PayloadQueue queue = new PayloadQueue(10, 1000);
        Payload payload = new Payload(new Record("D", "D", new byte[0]));
        queue.uninterruptablePut(payload);
    }

    public static Test suite() {
        return new TestSuite(PayloadQueueTest.class);
    }
}
