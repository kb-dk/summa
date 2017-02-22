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
package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.ingest.split.StreamController;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class ZIPParserTest extends TestCase {
    private static Log log = LogFactory.getLog(ZIPParserTest.class);

    public ZIPParserTest(String name) {
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
        return new TestSuite(ZIPParserTest.class);
    }

    // TODO: Test timeout with missing close

    // Provokes race condition
    public void testRepeatedBasics() throws Exception {
        for (int i = 0 ; i < 10 ; i++) {
            testBasics();
        }
    }

    // See what happens when the writer for a pipe closes the pipe, but the reader has not finished
    public void testPiping() throws IOException, InterruptedException {
        final int BLOCK_COUNT = 20;
        final int BLOCK_SIZE = 2024;
        final int RECEIVE_BLOCK_SIZE = 1234;
        final byte[] BLOCK = new byte[BLOCK_SIZE];
        final int EVERY = BLOCK_COUNT*BLOCK_SIZE/10;
        final AtomicInteger received = new AtomicInteger(0);

        final PipedInputStream pin = new PipedInputStream();
        PipedOutputStream pout = new PipedOutputStream();
        pout.connect(pin);

        Thread tout = new Thread() {
            byte[] buf = new byte[RECEIVE_BLOCK_SIZE];
            int next = EVERY;
            @Override
            public void run() {
                log.debug("Receiver polling with buffer size " + buf.length);
                try {
                    int read;
                    while ((read = pin.read(buf)) != -1) {
                        received.addAndGet(read);
                        if (received.get() >= next) {
                            log.debug("Received " + received.get() + "/" + BLOCK_COUNT * BLOCK_SIZE);
                            next += EVERY;
                        }
                        Thread.sleep(10);
                    }
                } catch (IOException e) {
                    log.error("Unexpected exception in testPiping", e);
                } catch (InterruptedException e) {
                    log.error("Interruped unexpectedly", e);
                }
            }
        };

        tout.start();

        log.debug(String.format("Feeder writing %dx%d bytes", BLOCK_COUNT, BLOCK_SIZE));
        int next = EVERY;
        for (int bi = 0 ; bi < BLOCK_COUNT ; bi++) {
            pout.write(BLOCK);
            if ((bi+1)*BLOCK_SIZE >= next) {
                log.debug("Delivered " + (bi+1)*BLOCK_SIZE + "/" + BLOCK_COUNT * BLOCK_SIZE);
                next += EVERY;
            }
        }

        log.debug("Feeder flushing");
        pout.flush();
        log.debug("Feeder closing");
        pout.close();
        log.debug("Feeder sleeping");
        Thread.sleep(500);
        log.debug("Verifying result");
        assertEquals("The amount of received content should match the send amount",
                     BLOCK_COUNT*BLOCK_SIZE, received.get());
        log.debug("All ok");
    }

    public void testArrayBlockingQueue() throws Exception {
        final ArrayBlockingQueue<String> queue =
                new ArrayBlockingQueue<>(2048);
        Thread feeder = new Thread() {
            @Override
            public void run() {
                for (int i = 0 ; i < 220 ; i++) {
                    try {
                        queue.put(Integer.toString(i));
                    } catch (InterruptedException e) {
                        log.error("Interrupted");
                    }
                }
                log.info("Finished feeding");
            }
        };

        Thread sucker = new Thread() {
            @Override
            public void run() {
                try {
                    //noinspection InfiniteLoopStatement
                    while (true) {
                        log.info(queue.take());
                    }
                } catch (InterruptedException e) {
                    log.error("Interrupted while taking");
                }
            }
        };

        Thread feeder2 = new Thread() {
            @Override
            public void run() {
                for (int i = 0 ; i < 20 ; i++) {
                    try {
                        queue.put("Take 2;" + Integer.toString(i));
                    } catch (InterruptedException e) {
                        log.error("Interrupted");
                    }
                }
                log.info("Finished feeding");
            }
        };

        feeder.start();
        feeder.join();
        sucker.start();
        log.info("Activating feeder 2") ;
        feeder2.start();
        Thread.sleep(1000);
    }

    public void testBasics() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(StreamController.CONF_PARSER, ZIPParser.class);
        StreamController unzipper = new StreamController(conf);

        PayloadFeederHelper feeder = new PayloadFeederHelper(
                Arrays.asList(new Payload(Resolver.getURL(
                        "ingest/zip/myzip.zip").openStream()),
                              new Payload(Resolver.getURL(
                        "ingest/zip/myzip2.zip").openStream())));
        unzipper.setSource(feeder);

        assertTrue("The unzipper should have at least one element",
                   unzipper.hasNext());

        List<String> expected = Arrays.asList(
                "foo", "flam", "kaboom", "zoo", "zoo2");
        assertContent(unzipper, expected);
    }

    public void testScale() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(StreamController.CONF_PARSER, ZIPParser.class);
        StreamController unzipper = new StreamController(conf);

        PayloadFeederHelper feeder = new PayloadFeederHelper(
                Arrays.asList(new Payload(Resolver.getURL(
                        "ingest/zip/large200.zip").openStream())));
        unzipper.setSource(feeder);

        assertTrue("The unzipper should have at least one element",
                   unzipper.hasNext());

        List<String> expected = new ArrayList<>(200);
        for (int i = 0 ; i < 200 ; i++) {
            expected.add("sample" + i);
        }
        assertContent(unzipper, expected);
    }

    public static void assertContent(ObjectFilter source,
                                    List<String> expected) throws IOException {
        int received = 0;
        while (source.hasNext()) {
            Payload payload = source.next();
            received++;
            Object originO = payload.getData(Payload.ORIGIN);
            String origin = originO == null ? "null.null!null.null" :
                            originO.toString();
            log.debug("Received Payload with origin " + origin);

            // Trim the an origin like 'foo/bar.zip!baz/myfile.xml'
            // down to 'myfile'
            String entryName = origin.contains("!") ?
                         origin.substring(origin.lastIndexOf("!") + 1) : origin;
            entryName = entryName.contains("/") ?
                 entryName.substring(entryName.lastIndexOf("/")+ 1) : entryName;
            entryName = entryName.substring(0, entryName.lastIndexOf("."));

            assertEquals("The entry name should match the content",
                         entryName,
                         PullParserTest.getStreamContent(payload).trim());
            assertTrue("Entry name " + entryName + "not found among "
                       + "expected entries", expected.contains(entryName));
            payload.close();
        }
        assertEquals("The number of processed Payloads should be correct",
                     expected.size(), received);
        source.close(true);
    }
}