/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.common.unittest;

import java.security.Permission;

import junit.framework.TestCase;
import dk.statsbiblioteket.util.qa.QAInfo;

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
    public void setUp () throws Exception {
        super.setUp();
        allowExit =false;
        checkSecurityManager();
    }

    /**
     * Each call to tearDown enables exitVM.
     * @throws Exception if the super-class encountered an exception.
     */
    public void tearDown() throws Exception {
        super.tearDown();
        allowExit = true;
    }

    /**
     * Checks to see is a SecurityManager is assigned to the running VM. If not,
     * a new SecurityManager is created. The new manager selectively enables or
     * disables exitVM.
     */
    private void checkSecurityManager() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager() {
                public void checkPermission(Permission perm) {
                    checkPermission(perm, null);
                }
                public void checkPermission(Permission perm, Object context) {
                    if (perm.getName().startsWith("exitVM")) {
                        if (!allowExit) {
                            throw new SecurityException(EXIT_MESSAGE);
                        }
                    }
                }
            });
        }
    }

}
