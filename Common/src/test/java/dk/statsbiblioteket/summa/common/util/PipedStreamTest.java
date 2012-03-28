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
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;

public class PipedStreamTest extends TestCase {
    public PipedStreamTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }


    public static Test suite() {
        return new TestSuite(PipedStreamTest.class);
    }

    public void testBasicPiping() throws Exception {
        PipedStream ps = new PipedStream();
        ps.getOutputStream().write(87);
        assertEquals("A single byte should pass", 87, ps.read());
        ps.close();
        assertEquals("EOF should pass through", -1, ps.read());
    }

    public void testThreadedWrite() throws Exception {
        final PipedStream ps = new PipedStream();
        Thread feeder = new Thread() {
            @Override
            public void run() {
                int counter = 0;
                while (counter++ < 5) {
                    try {
                        Thread.sleep(100);
                        ps.getOutputStream().write(counter);
                    } catch (InterruptedException e) {
                        System.err.println("Interrupted while sleeping");
                        //noinspection CallToPrintStackTrace
                        e.printStackTrace();
                    } catch (IOException e) {
                        System.err.println("IOException while writing");
                        //noinspection CallToPrintStackTrace
                        e.printStackTrace();
                    }
                }
                try {
                    ps.getOutputStream().close();
                } catch (IOException e) {
                    System.err.println("IOException while closing");
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace();
                }
            }
        };
        feeder.start();
        int b;
        int counter = 0;
        while ((b = ps.read()) != -1) {
            assertEquals("We should receive the right byte ", ++counter, b);
        }
        assertEquals("We should receive the right total number of bytes",
                     5, counter);
    }

    public void testCallback() throws Exception {
        final PipedStream ps = new PipedStream() {
            int counter = 0;

            @Override
            protected int addBytesRequest(int wanted) {
                try {
                    if (counter++ == 5) {
                        getOutputStream().close();
                        return 0;
                    } else {
                        getOutputStream().write(counter);
                        return 1;
                    }
                } catch (IOException e) {
                    System.err.println("Error in callback");
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace();
                }
                return 0;
            }
        };
        int b;
        int counter = 0;
        while ((b = ps.read()) != -1) {
            assertEquals("We should receive the right byte ", ++counter, b);
        }
        assertEquals("We should receive the right total number of bytes",
                     5, counter);
    }
}

