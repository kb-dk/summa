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
package dk.statsbiblioteket.summa.control.client;

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class ClientTest extends TestCase {

    public void testHello () throws Exception {
        // Dummy
        /*URL url = new URL("http://java.sun.com/javase/6/docs/api/java/net/HttpCookie.html");
        System.out.println (url.getQuery());
        System.out.println (url.getPath());

        System.out.println (new File("/home/mikkel/tmp/hugabuga").mkdirs());

        String filename = url.getPath();
        filename = filename.substring (filename.lastIndexOf("/") + 1);
        System.out.println(filename);*/
    }

    /*public void testDeploy () throws Exception {
        ClientConnection client = (ClientConnection) Naming.lookup ("//localhost:27000/test-client-1");
        URL url = new URL("http://java.sun.com/javase/6/docs/api/java/net/HttpCookie.html");

        client.deployService("test-1", "test-1-1", null);

    }*/
}