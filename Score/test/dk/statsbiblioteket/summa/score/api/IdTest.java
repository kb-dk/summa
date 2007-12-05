/* $Id: IdTest.java,v 1.5 2007/10/11 12:56:25 te Exp $
 * $Revision: 1.5 $
 * $Date: 2007/10/11 12:56:25 $
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
package dk.statsbiblioteket.summa.score.api;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.net.InetAddress;

import dk.statsbiblioteket.util.qa.QAInfo;

//import sun.reflect.Reflection;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class IdTest extends TestCase {
    // TODO: Investigate what happened to Id
     /*
    Id myId;

    public IdTest(String name) {
        super(name);

    }

    public void setUp() throws Exception {
        super.setUp();
        myId = new Id("IdUnitTest");
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetHostname() throws Exception {
        // hostname shold be on the same host as code is running
        String myHostName = InetAddress.getLocalHost().getHostName();
        Id i = new Id("testHostNameComponent");
        assertEquals(i.getHostname(),myHostName);
    }

    public void testGetWork_dir() throws Exception {
        //workdir need to be the same for all Id generated within the same jvm

        assertEquals(new Id("bla").getWork_dir(), new Id("foo").getWork_dir());
        assertFalse(new Id("gsdh").getWork_dir() == null || "".equals(new Id("gsdh").getWork_dir()));

    }

    public void testGetName() throws Exception {
        String name = "testGetName";
        Id i = new Id(name);
        assertEquals(i.getName(), name);
    }

    public void testGetId() throws Exception {
        String name = "testGetId";
        Id t = new Id(name);
        assertFalse(t.getId().equals(myId.getId()));
        String name2 = "testGetId";
        assertEquals(t,new Id(name2));
    }

    public static Test suite() {
        return new TestSuite(IdTest.class);
    }
*/
}
