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
package dk.statsbiblioteket.summa.common.unittest;

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

import java.security.Permission;

/**
 * Sets up a security manager that allows everything, except exitXM
 * (signalled by System.exit) inside the individual unit-tests.
 * No explicit action is needed by the users of this class to enable and
 * disable exitVM.
 * </p><p>
 * This TestCase is suitable for unit-tests where the individual tests calls
 * code that exits the VM. Instead of aborting the whole test-run, an exception
 * will be thrown.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class NoExitTestCase extends TestCase {
    protected static final String EXIT_MESSAGE =
            "System.exit not allowed at this point";
    private boolean allowExit = false;
    protected boolean exitHasBeenRequested = false;
    protected int exitCode = -1;

    public NoExitTestCase() {
        super();
    }
    public NoExitTestCase(String s) {
        super(s);
    }

    /**
     * Each call to setUp disables exitVM.
     * @throws Exception if the super-class encountered an exception.
     */
    @Override
    public void setUp () throws Exception {
        super.setUp();
        allowExit = false;
        checkSecurityManager();
    }

    /**
     * Each call to tearDown enables exitVM.
     * @throws Exception if the super-class encountered an exception.
     */
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        allowExit = true;
    }

    public void testDummy() {
        try {
            System.exit(1);
            fail("Exit not allowed here");
        } catch(SecurityException e) {
            // ok
        }
    }

    /**
     * Checks to see is a SecurityManager is assigned to the running VM. If not,
     * a new SecurityManager is created. The new manager selectively enables or
     * disables exitVM.
     */
    private void checkSecurityManager() {
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkPermission(Permission perm) {
                checkPermission(perm, null);
            }
            @Override
            public void checkPermission(Permission perm, Object context) {
                if (perm.getName().startsWith("exitVM")) {
                    // exitVM.1
                    exitCode = Integer.valueOf(perm.getName().split("\\.")[1]);
                    exitHasBeenRequested = true;
                    if (!allowExit) {
                        throw new SecurityException(EXIT_MESSAGE);
                    }
                }
            }
        });
    }
}