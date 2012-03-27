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

import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;

/**
 * Unit tests for {@link DeferredSystemExit}.
 * <p></p>
 * These unit tests are not suited for being run in a batched manner since
 * they need to call {@link System#exit} and Java does not support forking
 * and trapping of sub processes 
 */
public class DeferredSystemExitTest extends NoExitTestCase {
    private static final String NO_ATTEMPT_YET =
            "System.exit should not have been attempted by now";

    /**
     * Schedule a system exit 0 and wait for it to happen. If it
     * doesn't exit with code 1. This test should exit with code 0.
     * @throws Exception on frobnication
     */
    public void testExit() throws Exception {
        assertFalse(NO_ATTEMPT_YET, this.exitHasBeenRequested);

        new DeferredSystemExit(0, 1000);
        Thread.sleep(2000);
        assertTrue("System.exit should have been attempted by now",
                   this.exitHasBeenRequested);
        assertEquals("The exit code should be as expected",
                     0, this.exitCode);
    }

    /**
     * Schedule a system exit with code 1 and abort it. This test should exit
     * with exit code 0.
     * @throws Exception If the wizzlebizzle has been twizzled
     */
    public void testAbortExit () throws Exception {
        DeferredSystemExit exit = new DeferredSystemExit(1, 1000);

        assertFalse(NO_ATTEMPT_YET + " (1)", this.exitHasBeenRequested);
        Thread.sleep(500);
        exit.abortExit();
        assertFalse(NO_ATTEMPT_YET + " (2)", this.exitHasBeenRequested);
        Thread.sleep(1500);
        assertFalse(NO_ATTEMPT_YET + " (3)", this.exitHasBeenRequested);
    }
}