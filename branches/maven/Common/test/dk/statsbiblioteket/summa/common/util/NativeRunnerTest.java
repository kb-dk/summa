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

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * NativeRunner Tester.
 *
 * @deprecated {@link dk.statsbiblioteket.summa.common.util.NativeRunner} is deprecated.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class NativeRunnerTest extends TestCase {
    public NativeRunnerTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void oldTestSimpleCall() throws Exception {
        NativeRunner runner = new NativeRunner("true");
        assertEquals("The execution of true should work fine",
                     0, runner.executeNoCollect());
        runner = new NativeRunner("false");
        assertEquals("The execution of false should give 1",
                     1, runner.executeNoCollect());
    }

/* NativeRunner is deprecated

    public void testTimeout() throws Exception {
        NativeRunner runner = new NativeRunner(new String[]{"sleep", "2"});
        try {
            runner.executeNoCollect(100);
            fail("The execution of sleep should time out");
        } catch(Exception e) {
            // Expected behaviour
        }
    }

    public void testEnvironment() throws Exception {
        NativeRunner runner =
                new NativeRunner(new String[]{"echo", "$FLAM"},
                                 new String[]{"FLAM=flim"});
        assertEquals("The execution of echo should work fine",
                     0, runner.executeNoCollect());
        assertEquals("The result of echo should be flim",
                     "flim", runner.getProcessOutputAsString());
    }
*/
    public static Test suite() {
        return new TestSuite(NativeRunnerTest.class);
    }
}




