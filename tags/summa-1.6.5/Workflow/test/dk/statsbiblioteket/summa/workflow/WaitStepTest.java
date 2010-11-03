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
package dk.statsbiblioteket.summa.workflow;

import dk.statsbiblioteket.summa.common.util.ThreadInterrupt;

import junit.framework.TestCase;

/**
 * Test {@link WaitStep}.
 */
public class WaitStepTest extends TestCase {
    /** WaitStep pointer. */
    private WaitStep step;

    /**
     * Test wait.
     */
    public final void testWait() {
        final int deltaMax = 2000;
        step = new WaitStep(2);

        long delta = System.currentTimeMillis();
        step.run();
        delta = System.currentTimeMillis() - delta;

        assertTrue(delta >= deltaMax);
    }

    /**
     * Test interrupt.
     */
    public final void testInterrupt() {
        final int sleep = 1000;
        final int deltaMin = 980;
        final int deltaMax = 1200;
        step = new WaitStep(2);

        new ThreadInterrupt(Thread.currentThread(), sleep);

        long delta = System.currentTimeMillis();
        step.run();
        delta = System.currentTimeMillis() - delta;

        assertTrue("Should break out after a second. Break time was: "
                   + delta + "ms", delta > deltaMin && delta < deltaMax);
    }
}
