/* $Id: ClientTest.java,v 1.5 2007/10/11 12:56:24 te Exp $
 * $Revision: 1.5 $
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
package dk.statsbiblioteket.summa.score.client;

import junit.framework.TestCase;

import java.net.URL;
import java.io.File;
import java.rmi.Naming;

import dk.statsbiblioteket.summa.score.api.ClientConnection;
import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class ClientTest extends TestCase {

    public void testHello () throws Exception {
        URL url = new URL("http://java.sun.com/javase/6/docs/api/java/net/HttpCookie.html");
        System.out.println (url.getQuery());
        System.out.println (url.getPath());

        System.out.println (new File("/home/mikkel/tmp/hugabuga").mkdirs());

        String filename = url.getPath();
        filename = filename.substring (filename.lastIndexOf("/") + 1);
        System.out.println(filename);          
    }

    public void testDeploy () throws Exception {
        ClientConnection client = (ClientConnection) Naming.lookup ("//localhost:2767/test-client-1");
        URL url = new URL("http://java.sun.com/javase/6/docs/api/java/net/HttpCookie.html");

        client.deployService("test-1", "test-1-1", null);

    }
    

}
