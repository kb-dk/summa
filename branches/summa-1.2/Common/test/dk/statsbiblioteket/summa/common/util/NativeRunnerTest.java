/* $Id: NativeRunnerTest.java,v 1.2 2007/10/04 13:28:20 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:20 $
 * $Author: te $
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
package dk.statsbiblioteket.summa.common.util;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * NativeRunner Tester.
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

    public void testSetParameters() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetProcessOutput() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetProcessError() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetProcessOutputAsString() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetProcessErrorAsString() throws Exception {
        //TODO: Test goes here...
    }

    public void testSimpleCall() throws Exception {
        NativeRunner runner = new NativeRunner("true");
        assertEquals("The execution of true should work fine",
                     0, runner.executeNoCollect());
        runner = new NativeRunner("false");
        assertEquals("The execution of false should give 1",
                     1, runner.executeNoCollect());
    }

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

    public static Test suite() {
        return new TestSuite(NativeRunnerTest.class);
    }
}



