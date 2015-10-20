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
package dk.statsbiblioteket.summa.common;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummaRegistryTest extends TestCase {
    private static Log log = LogFactory.getLog(SummaRegistryTest.class);

    public void testSingle() throws InterruptedException {
        RunnableRegistry rr = new RunnableRegistry();
        Thread t = new Thread(rr);

        t.start();
        synchronized (this) {
            this.wait(100);
        }
        assertTrue("The registry should be running", rr.isRunning());

        rr.shutdown();
        synchronized (this) {
            this.wait(100);
        }
        assertFalse("The registry should not be running", rr.isRunning());
    }

    public void testDual() throws InterruptedException {
        RunnableRegistry r1 = new RunnableRegistry();
        Thread t1 = new Thread(r1);
        t1.start();
        synchronized (this) {
            this.wait(100);
        }
        assertTrue("Registry 1 should be running", r1.isRunning());

        SummaRegistry r2 = new SummaRegistry(Configuration.newMemoryBased(SummaRegistry.CONF_PORT, 27000), false);
        try {
            r2.run();
            fail("Starting a registry on the same port as a running one should fail");
        } catch (IllegalStateException e) {
            // Expected
        }

        r1.shutdown();
        synchronized (this) {
            this.wait(100);
        }
        assertFalse("The registry should not be running", r1.isRunning());
    }


    private class RunnableRegistry implements Runnable {
        private SummaRegistry registry = null;
        private boolean running = false;

        @Override
        public void run() {
            log.info("Starting Registry");
            running = true;
            registry = new SummaRegistry(Configuration.newMemoryBased(SummaRegistry.CONF_PORT, 27000), false);
            registry.run();
            running = false;
            log.info("Closed down registry");
        }

        public void shutdown() {
            log.info("Shutdown called");
            registry.shutdown();
        }

        public boolean isRunning() {
            return running;
        }
    }
}
