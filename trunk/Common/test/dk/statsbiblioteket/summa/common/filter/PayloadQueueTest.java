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

