/* $Id: SourceTest.java,v 1.3 2007/10/11 12:56:24 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/11 12:56:24 $
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: SourceTest.java,v 1.3 2007/10/11 12:56:24 te Exp $
 */
package dk.statsbiblioteket.summa.score.rmid;

import java.io.File;

import junit.framework.TestCase;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * The rmid package contains testing of remove invocation of services.
 * These tests does not have any direct relation to the code in the Score
 * module.
 * See http://java.sun.com/j2se/1.3/docs/guide/rmi/activation/activation.1.html
 *
 * NOTE: In order to use this test, the rmid needs to be started:
 * Navigate to test/dk/statatsbiblioteket/summa/score/rmid and execute
 * rmid -J-Djava.security.policy=rmid.policy
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class SourceTest extends TestCase {
    public void testLocations() throws Exception {
        File location = new File(SetupServices.LOCATION);

        assertTrue("The policy should exist", SetupServices.POLICY.exists());
        assertTrue("The policy should be readable", SetupServices.POLICY.canRead());
        assertTrue("The location should be readable", location.canRead());
        assertTrue("The policy should be a file", SetupServices.POLICY.isFile());
        assertTrue("The location should be a folder", location.isDirectory());
    }

    public void testSetup() throws Exception {
        new SetupServices();
    }
    
}
