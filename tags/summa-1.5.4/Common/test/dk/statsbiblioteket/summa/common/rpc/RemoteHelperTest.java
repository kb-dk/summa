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
package dk.statsbiblioteket.summa.common.rpc;

import junit.framework.TestCase;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.Naming;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.io.IOException;

/**
 *
 */
public class RemoteHelperTest extends TestCase {

    public void testTestNotUrlCodePath () {
        String[] codePath = new String[] {"goo.jar"};

        try {
            RemoteHelper.testCodeBase(codePath);
            fail ("Codepath is not a URL");
        } catch (RemoteHelper.InvalidCodeBaseException e) {
            System.out.println ("Error in codepath as expected: "
                                + e.getMessage());
        }
    }

    public void testTestNotJarCodePath () {
        String[] codePath = new String[] {"http://goo"};

        try {
            RemoteHelper.testCodeBase(codePath);
            fail ("Codepath is a .jar");
        } catch (RemoteHelper.InvalidCodeBaseException e) {
            System.out.println ("Error in codepath as expected: "
                                + e.getMessage());
        }
    }

    public void testTestNonExistingCodePath () {
        String[] codePath = new String[] {"http://example.com/foobar.jar"};

        try {
            RemoteHelper.testCodeBase(codePath);
            fail ("Codepath does not represent an existing file");
        } catch (RemoteHelper.InvalidCodeBaseException e) {
            System.out.println ("Error in codepath as expected: "
                                + e.getMessage());
        }
    }

    interface Pingable extends Remote {
        public String ping() throws RemoteException;
        public void throwException() throws RemoteException;
        public void throwOOM() throws RemoteException;
    }

    static class PingServer extends UnicastRemoteObject implements Pingable {

        public static final String SERVICE_NAME = "PingServer";
        public static final int REGISTRY_PORT = 12345;

        public PingServer() throws IOException {
            super();
            RemoteHelper.exportRemoteInterface(this,
                                               REGISTRY_PORT, SERVICE_NAME);
        }

        public String ping() throws RemoteException {
            return "Pong";
        }

        public void throwException() throws RemoteException {
            throw new RuntimeException("Here you go - an exception!");
        }

        public void throwOOM() throws RemoteException {
            throw new OutOfMemoryError("Fake serverside OOM");
        }

        public void close() {
            try {
                RemoteHelper.unExportRemoteInterface(SERVICE_NAME,
                                                     REGISTRY_PORT);
            } catch (IOException e) {
                throw new RuntimeException("Failed to unexport service", e);
            }
        }
    }

    public void testRemoteInteractions() throws Exception {
        PingServer p = new PingServer();
        try {
            Pingable conn = (Pingable)Naming.lookup(

                                    "//localhost:" + PingServer.REGISTRY_PORT
                                    + "/" + PingServer.SERVICE_NAME);
            assertEquals("Pong", conn.ping());
            try {
                p.throwException();
                fail("We should have a runtime exception here");
            } catch (RuntimeException e) {
                // Succes
            }
            try {
                p.throwOOM();
                fail("We should have an OOM here");
            } catch (OutOfMemoryError e) {
                // Succes
                System.err.println("Bloody scary shit! The OOM passed "
                                   + "from the server to the client");
            }
        } finally {
            p.close();
        }

    }

}




