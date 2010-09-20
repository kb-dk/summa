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

import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.util.ThreadInterrupt;

/**
 *
 */
public class WaitStepTest extends TestCase {

    WaitStep step;

    public void testWait() throws Exception {
        step = new WaitStep(2);

        long delta = System.currentTimeMillis();
        step.run();
        delta = System.currentTimeMillis() - delta;

        assertTrue(delta >= 2000);
    }

    public void testInterrupt() throws Exception {
        step = new WaitStep(2);

        new ThreadInterrupt(Thread.currentThread(), 1000);

        long delta = System.currentTimeMillis();
        step.run();
        delta = System.currentTimeMillis() - delta;

        assertTrue("Should break out after a second. Break time was: "
                   + delta + "ms", delta > 995 && delta < 1005);
    }
}