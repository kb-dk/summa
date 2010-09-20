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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;
import dk.statsbiblioteket.summa.common.util.ThreadInterrupt;
import junit.framework.TestCase;

/**
 * Test suite for {@link WorkflowManager}
 */
public class WorkflowManagerTest extends TestCase {

    WorkflowManager man;

    static class GoodStep implements WorkflowStep {

        public int numRuns = 0;

        public GoodStep(Configuration conf) {

        }

        @Override
        public void run() {
            numRuns++;
        }
    }

    static class BadStep extends GoodStep {

        public static final String message = "I'm bad! I'm bad!";

        public BadStep(Configuration conf) {
            super(conf);
        }

        @Override
        public void run() {
            super.run();
            throw new RuntimeException(message);
        }
    }

    @Override
    public void setUp() throws Exception {

    }

    @Override
    public void tearDown() throws Exception {

    }

    public void testManualNoSteps() {
        man = new WorkflowManager(false, false, 5);
        man.run();
    }

    public void testManualGoodNoFail() {
        man = new WorkflowManager(false, false, 5);
        man.addStep(new GoodStep(null));
        man.run();
    }

    public void testManualBadNoFail() {
        man = new WorkflowManager(false, false, 5);
        man.addStep(new BadStep(null));
        try {
            man.run();
            fail("Should throw a runtime exception");
        } catch (RuntimeException e) {
            assertEquals(BadStep.message, e.getMessage());
        }
    }

    public void testManualBadAcceptFail() {
        man = new WorkflowManager(false, true, 1);

        BadStep step = new BadStep(null);
        man.addStep(step);
        
        // The man.run() call will loop in an endless retry, so schedule an
        // interrupt on it in 3s
        new ThreadInterrupt(Thread.currentThread(), 3100);

        long deltaTime = System.currentTimeMillis();
        man.run();

        assertTrue("Should have three retries before interruption",
                   4 == step.numRuns);

        // Assert that three iterations of the retry has been run
        assertTrue("Workflow should retry until interrupted",
                   deltaTime >= 3100);
    }

     public void testManualGoodBadGoodAcceptFail() {
         man = new WorkflowManager(false, true, 1);

         GoodStep good1 = new GoodStep(null);
         BadStep bad = new BadStep(null);
         GoodStep good2 = new GoodStep(null);

         man.addStep(good1);
         man.addStep(bad);
         man.addStep(good2);

         // The man.run() call will loop in an endless retry, so schedule an
         // interrupt on it in 3s
         new ThreadInterrupt(Thread.currentThread(), 3100);

         long deltaTime = System.currentTimeMillis();
         man.run();

         assertTrue("Should have runs before interruption",
                    4 == good1.numRuns);

         assertTrue("Should have three retries before interruption",
                    4 == bad.numRuns);

         assertTrue("Should have zero invocations. Found " + good2.numRuns,
                    0 == good2.numRuns);

         // Assert that three iterations of the retry has been run
         assertTrue("Workflow should retry until interrupted",
                    deltaTime >= 3100);
     }

    public void testConfigWait() throws Exception {
        Configuration conf = new Configuration(
                                        new XStorage("data/wait-workflow.xml"));
        man = new WorkflowManager(conf);

        long delta = System.currentTimeMillis();
        man.run();
        delta = System.currentTimeMillis() -delta;

        assertTrue(delta < 5005 && delta > 4995);
    }
}