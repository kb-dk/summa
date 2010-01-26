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
package dk.statsbiblioteket.summa.common.filter.object;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.io.IOException;

/**
 * Testing delays are subject to flux as garbage collection et al kicks in.
 * The values used in the test are therefore conservative. This means that the
 * speed of the test is fairly low.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class DelayFilterTest extends TestCase implements ObjectFilter {
    public DelayFilterTest(String name) {
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

    private static final long MS = 1000000; // Expressed in ns
    private static final long DELAY = 500 * MS; // More than GC + processing
    private static final long PROCESSING = 500 * MS; // Processing < this

    public void testPreDelay() {
        Configuration conf = Configuration.newMemoryBased(
                DelayFilter.CONF_FIXED_DELAY_PREREQUEST, DELAY);
        DelayFilter delayer = new DelayFilter(conf);
        delayer.setSource(this);
        long startTime = System.nanoTime();
        delayer.next();
        assertBetween("There should be a pause before the first next()",
                      lastNext - startTime, DELAY, DELAY + PROCESSING);
        assertBetween("There should be no pause after next()",
                      System.nanoTime() - lastNext, 0, PROCESSING);
    }

    public void testPostDelay() {
        Configuration conf = Configuration.newMemoryBased(
                DelayFilter.CONF_FIXED_DELAY_POSTREQUEST, DELAY);
        DelayFilter delayer = new DelayFilter(conf);
        delayer.setSource(this);
        long startTime = System.nanoTime();
        delayer.next();
        assertBetween("There should be no pause before the first next()",
                      lastNext - startTime, 0, PROCESSING);
        assertBetween("There should be a pause after the first next()",
                      System.nanoTime() - lastNext, DELAY, DELAY + PROCESSING);
    }

    public void testMin() throws Exception {
        Configuration conf = Configuration.newMemoryBased(
                DelayFilter.CONF_MIN_DELAY_BETWEEN_PAYLOADS, DELAY);
        DelayFilter delayer = new DelayFilter(conf);
        delayer.setSource(this);
        long startTime = System.nanoTime();
        delayer.next();
        assertBetween("There should be no pause for the first next()",
                      System.nanoTime() - startTime, 0, PROCESSING);
        delayer.next();
        assertBetween("There should be a pause for the second next()",
                      System.nanoTime() - lastNext, DELAY, DELAY + PROCESSING);
        Thread.sleep(DELAY / MS);

        long middleTime = System.nanoTime();
        delayer.next();
        assertBetween("There should be no pause for the third next()",
                      System.nanoTime() - middleTime, 0, PROCESSING);
    }

    public void testNonCummulative() {
        Configuration conf = Configuration.newMemoryBased(
                DelayFilter.CONF_FIXED_DELAY_PREREQUEST, DELAY,
                DelayFilter.CONF_MIN_DELAY_BETWEEN_PAYLOADS, DELAY);
        DelayFilter delayer = new DelayFilter(conf);
        delayer.setSource(this);
        long startTime = System.nanoTime();
        delayer.next();
        assertBetween("Overlapping waits should overlap, not sum",
                   System.nanoTime() - startTime, DELAY, DELAY + PROCESSING);
    }

    public void testCummulative() throws InterruptedException {
        Configuration conf = Configuration.newMemoryBased(
                DelayFilter.CONF_FIXED_DELAY_PREREQUEST, DELAY,
                DelayFilter.CONF_MIN_DELAY_BETWEEN_PAYLOADS, 2 * DELAY);
        DelayFilter delayer = new DelayFilter(conf);
        delayer.setSource(this);
        long startTime = System.nanoTime();
        delayer.next();
        assertBetween("The first next() should ignore minDelayBetween",
                      System.nanoTime() - startTime,
                      DELAY, DELAY + PROCESSING);

        long middleTime = System.nanoTime();
        delayer.next();
        assertBetween("The second next() should hit minDelayBetween",
                      System.nanoTime() - middleTime,
                      2 * DELAY, 2 * DELAY + PROCESSING);

        Thread.sleep(DELAY / MS);

        middleTime = System.nanoTime();
        delayer.next();
        assertBetween("The third next() should miss minDelayBetween",
                      System.nanoTime() - middleTime,
                      DELAY, DELAY + PROCESSING);
    }
    
    private void assertBetween(String message, long ns,
                               long minNS, long maxNS) {
        if (ns < minNS || ns > maxNS) {
            fail(message + ". " + ns / 1000000D + " did not satisfy "
                 + minNS / 1000000D + " <= x <= " + maxNS / 1000000D);
        }
    }

    public static Test suite() {
        return new TestSuite(DelayFilterTest.class);
    }

    /* ObjectFilter dummy */

    private long lastNext = 0;
    private int counter = 0;
    public boolean hasNext() {
        return true;
    }
    public void setSource(Filter filter) {
        // Nada
    }
    public boolean pump() throws IOException {
        next();
        return true;
    }
    public void close(boolean success) {
        // Nada
    }
    public Payload next() {
        lastNext = System.nanoTime();
        return new Payload(new Record(
                "Dummy_" + counter++, "foo", new byte[0]));
    }
    public void remove() {
        // Nada
    }
}

