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
package dk.statsbiblioteket.summa.facetbrowser.util;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * ClusterCommon Tester.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ClusterCommonTest extends TestCase {
    public ClusterCommonTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }


    public void testColonEscape() throws Exception {
        assertEquals("Colons should be escaped",
                     "foo\\:bar\\:\\:zoo\\:",
                     ClusterCommon.colonEscape("foo:bar::zoo:"));
    }


    public void testSetGetProperties() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetSearchProperties() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetIndexProperties() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetPropertyInt() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetPropertyDouble() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetProperty() throws Exception {
        //TODO: Test goes here...
    }

    public static Test suite() {
        return new TestSuite(ClusterCommonTest.class);
    }
}
