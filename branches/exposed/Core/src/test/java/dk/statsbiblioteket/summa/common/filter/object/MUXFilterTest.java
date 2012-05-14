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
package dk.statsbiblioteket.summa.common.filter.object;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class MUXFilterTest extends TestCase implements ObjectFilter {
    private static Log log = LogFactory.getLog(MUXFilterTest.class);

    public MUXFilterTest(String name) {
        super(name);
    }

    private static final int PAYLOADS = 10;
    @Override
    public void setUp() throws Exception {
        super.setUp();
        recordsLeft = PAYLOADS;
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }


    public static Test suite() {
        return new TestSuite(MUXFilterTest.class);
    }

    /**
     * Sets up a muxer with slow processing (DelayFilter) and feeds it an EOF,
     * checking that all Payloads are processed.
     * https://gforge.statsbiblioteket.dk/tracker/?func=detail&atid=109&aid=1553&group_id=8}
     * @throws Exception if things go bonkers.
     */
    public void testEOF() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        List<Configuration> delays =
                conf.createSubConfigurations(MUXFilter.CONF_FILTERS, 1);
        delays.get(0).set(MUXFilterFeeder.CONF_FILTER_CLASS,
                          DelayFilter.class.getCanonicalName());
        delays.get(0).set(DelayFilter.CONF_FIXED_DELAY_PREREQUEST,
                          100 * 1000000);
        delays.get(0).set(DelayFilter.CONF_FIXED_DELAY_POSTREQUEST,
                          100 * 1000000);
        delays.get(0).set(MUXFilter.CONF_INSTANCES, 2);
//        delays.get(0).set(MUXFilterFeeder.CONF_QUEUE_OUT_LENGTH, 1);
        log.debug("Creating MUXFilter");
        MUXFilter muxer = new MUXFilter(conf);
        muxer.setSource(this);
        muxer.setSource(this);
        int counter = 0;
        log.debug("Emptying MUXFilter");
        while (muxer.hasNext()) {
            Thread.sleep(100);
            counter++;
            muxer.next();
        }
        assertEquals("The number of processed Payloads should be correct",
                     PAYLOADS, counter);
    }

    // TODO: Multiple sources test

    public void testClose() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        List<Configuration> delays =
                conf.createSubConfigurations(MUXFilter.CONF_FILTERS, 1);
        delays.get(0).set(MUXFilterFeeder.CONF_FILTER_CLASS,
                          DelayFilter.class.getCanonicalName());
        delays.get(0).set(DelayFilter.CONF_FIXED_DELAY_PREREQUEST,
                          100 * 1000000);
        delays.get(0).set(DelayFilter.CONF_FIXED_DELAY_POSTREQUEST,
                          100 * 1000000);
        delays.get(0).set(MUXFilter.CONF_INSTANCES, 2);
//        delays.get(0).set(MUXFilterFeeder.CONF_QUEUE_OUT_LENGTH, 1);
        log.debug("Creating MUXFilter");
        MUXFilter muxer = new MUXFilter(conf);
        muxer.setSource(this);
        assertTrue("Muxer should have next", muxer.hasNext());
        muxer.close(true);

        int counter = 0;
        log.debug("Emptying MUXFilter after close");
        while (muxer.hasNext()) {
            counter++;
            muxer.next();
        }
        assertEquals("The number of processed Payloads should be correct",
                     PAYLOADS, counter);
    }

    public void testQueues() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(MUXFilter.CONF_OUTQUEUE_MAXPAYLOADS, 2);
        List<Configuration> delays =
                conf.createSubConfigurations(MUXFilter.CONF_FILTERS, 2);
        delays.get(0).set(MUXFilterFeeder.CONF_FILTER_CLASS,
                          DelayFilter.class.getCanonicalName());
        delays.get(0).set(DelayFilter.CONF_FIXED_DELAY_PREREQUEST,
                          100 * 1000000);
        delays.get(0).set(DelayFilter.CONF_FIXED_DELAY_POSTREQUEST,
                          100 * 1000000);
        delays.get(0).set(MUXFilter.CONF_INSTANCES, 1);
        delays.get(0).set(MUXFilterFeeder.CONF_QUEUE_MAXPAYLOADS, 5);
        delays.get(1).set(MUXFilterFeeder.CONF_FILTER_CLASS,
                          DelayFilter.class.getCanonicalName());
        delays.get(1).set(DelayFilter.CONF_FIXED_DELAY_PREREQUEST,
                          10 * 1000000);
        delays.get(1).set(DelayFilter.CONF_FIXED_DELAY_POSTREQUEST,
                          50 * 1000000);
        delays.get(1).set(MUXFilter.CONF_INSTANCES, 1);
        delays.get(1).set(MUXFilterFeeder.CONF_QUEUE_MAXPAYLOADS, 1);
        log.debug("Creating MUXFilter");

        MUXFilter muxer = new MUXFilter(conf);
        muxer.setSource(this);
        muxer.close(true);

        assertPayloads("The number of processed Payloads should be correct",
                       PAYLOADS, muxer);
    }

    public void assertPayloads(String message, int expected,
                               ObjectFilter filter) throws Exception {
        int counter = 0;
        log.debug("Emptying MUXFilter after close");
        StringWriter sw = new StringWriter();
        while (filter.hasNext()) {
            counter++;
            sw.append(filter.next().getId()).append(" ");
        }
        assertEquals(message + ". Received Payloads was " + sw.toString(),
                     expected, counter);
    }

    /* ObjectFilter */
    private int recordsLeft = 0;
    public boolean hasNext() {
        return recordsLeft > 0;
    }
    public void setSource(Filter filter) {
        // Nada
    }
    public boolean pump() throws IOException {
        next();
        return true;
    }
    public void close(boolean success) {
        // Nada
    }
    public Payload next() {
        if (!hasNext()) {
            return null;
        }
        log.debug("Delivering Dummy_" + recordsLeft);
        return new Payload(new Record(
                "Dummy_" + recordsLeft--, "foo", new byte[0]));
    }
    public void remove() {
        recordsLeft--;
    }
}

