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

import junit.framework.TestCase;

import java.util.Arrays;

import dk.statsbiblioteket.summa.common.util.UniqueTimestampGenerator;

/**
 *
 */
public class UniqueTimestampGeneratorTest extends TestCase {

    UniqueTimestampGenerator gen;
    final long maxSalt = UniqueTimestampGenerator.MAX_SALT;
    final long maxTime = UniqueTimestampGenerator.MAX_TIME;

    public void setUp() throws Exception {
        gen = new UniqueTimestampGenerator();
    }

    public void testFixedNow() {
        long now = System.currentTimeMillis();
        long lastStamp = 0;

        for (int i = 0; i < UniqueTimestampGenerator.MAX_SALT; i++) {
            long next = gen.next(now);
            assertFalse(lastStamp == next);
            assertEquals("System time mismatch on iteration " + i
                         +". Delta was: " + (now - gen.systemTime(next)),
                         now, gen.systemTime(next));

            lastStamp = next;
        }
    }

    public void testExtendSalt () {
        long now = System.currentTimeMillis();
        long lastStamp = 0;

        for (long i = 0; i < 2*UniqueTimestampGenerator.MAX_SALT; i++) {
            if (i == UniqueTimestampGenerator.MAX_SALT) {
                now++;
            }

            long next = gen.next(now);
            assertFalse(lastStamp == next);
            assertEquals("System time mismatch on iteration " + i
                         +". Delta was: " + (now - gen.systemTime(next)),
                         now, gen.systemTime(next));

            lastStamp = next;
        }
    }

    public void testIncSalt () {
        long now =System.currentTimeMillis();

        for (long i = 0; i < UniqueTimestampGenerator.MAX_SALT; i++) {
            long next = gen.next(now);
            assertEquals("Salt not incremented as expected",
                         i, gen.salt(next));
            assertEquals("System time mismatch on iteration " + i
                         +". Delta was: " + (now - gen.systemTime(next)),
                         now, gen.systemTime(next));
        }
    }

    public void testSort() {
        int numSalts = 1000;
        int numTimeSteps = 10;

        long[] stamps = new long[numSalts*numTimeSteps];

        for (int timeStep = 0; timeStep < numTimeSteps; timeStep++) {
            for (int salt = 0; salt < numSalts; salt++) {
                long stamp = gen.next(timeStep);
                assertEquals("System time should match timeStep",
                             timeStep, gen.systemTime(stamp));
                assertEquals("Salt should match precomputed value",
                             salt, gen.salt(stamp));

                stamps[timeStep*numSalts + salt] = stamp;
            }
        }

        long[] stampsSorted = Arrays.copyOf(stamps, stamps.length);
        Arrays.sort(stampsSorted);
        
        assertTrue("Generated timestamps should come out sorted",
                   Arrays.equals(stamps, stampsSorted));
    }

    public void testStringFormats() throws Exception {
        long testTime = 1258448660520L;
        String testTimeString = "2009-11-17T10:04:20.520";

        assertEquals(testTimeString, gen.formatSystemTime(testTime));
        assertEquals(testTime, gen.parseSystemTime(testTimeString));
        assertEquals(testTimeString,
                     gen.formatTimestamp(gen.baseTimestamp(testTime)));
    }

    public void testMaxTimestamp() {
        long i1 = UniqueTimestampGenerator.MAX_TIME;
        long i2 = i1 - 1;
        long i3 = i1 - 100;
        long i0 = 0;
        System.out.println(" " + gen.next());
        System.out.println(gen.baseTimestamp(i0) + " " + gen.baseTimestamp(i1) + " " + gen.baseTimestamp(i2) + " " + gen.baseTimestamp(i3));
        assertTrue(gen.baseTimestamp(i1) > gen.baseTimestamp(i2));
        assertTrue(gen.baseTimestamp(i2) > gen.baseTimestamp(i3));
        assertTrue(gen.baseTimestamp(i3) > gen.baseTimestamp(i0));
    }

    static double log2(double d) {
        return Math.log10(d)/ Math.log10(2);
    }

}

