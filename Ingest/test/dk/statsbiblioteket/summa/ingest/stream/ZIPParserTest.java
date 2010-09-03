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
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.ingest.split.StreamController;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

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

    public void testArrayBlockingQueue() throws Exception {
        final ArrayBlockingQueue<String> queue =
                new ArrayBlockingQueue<String>(2048);
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
                        "data/zip/myzip.zip").openStream()),
                              new Payload(Resolver.getURL(
                        "data/zip/myzip2.zip").openStream())));
        unzipper.setSource(feeder);

        assertTrue("The unzipper should have at least one element",
                   unzipper.hasNext());

        List<String> expected = Arrays.asList(
                "foo", "flam", "kaboom", "zoo", "zoo2");
        assertUnzipContent(unzipper, expected);
    }

    public void testScale() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(StreamController.CONF_PARSER, ZIPParser.class);
        StreamController unzipper = new StreamController(conf);

        PayloadFeederHelper feeder = new PayloadFeederHelper(
                Arrays.asList(new Payload(Resolver.getURL(
                        "data/zip/large200.zip").openStream())));
        unzipper.setSource(feeder);

        assertTrue("The unzipper should have at least one element",
                   unzipper.hasNext());

        List<String> expected = new ArrayList<String>(200);
        for (int i = 0 ; i < 200 ; i++) {
            expected.add("sample" + i);
        }
        assertUnzipContent(unzipper, expected);
    }

    private void assertUnzipContent(StreamController unzipper,
                                    List<String> expected) throws IOException {
        int received = 0;
        while (unzipper.hasNext()) {
            Payload payload = unzipper.next();
            received++;
            String origin = payload.getData(Payload.ORIGIN).toString();
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
        unzipper.close(true);
    }
}