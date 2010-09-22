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
package dk.statsbiblioteket.summa.ingest.split;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test clases for {@link XMLSplitterFilter}.
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
public class XMLSplitterFilterTest extends TestCase implements ObjectFilter {
    /**
     * Constructor.
     * @param name The name.
     */
    public XMLSplitterFilterTest(String name) {
        super(name);
    }

    @Override
    public final void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public final void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * @return Return the test suite.
     */
    public static Test suite() {
        return new TestSuite(XMLSplitterFilterTest.class);
    }

    /**
     * Test basic split.
     * @throws Exception If error.
     */
    public final void testBasicSplit() throws Exception {
        final int magic = 4;
        Configuration conf = getBasicConfiguration();
        ObjectFilter parser = new XMLSplitterFilter(conf);
        parser.setSource(this);
        startProducer(2);
        for (int i = 0; i < magic; i++) {
            assertTrue("With " + i + " requested Payloads, more Payloads should"
                       + " be available", parser.hasNext());
            parser.next();
        }
        assertFalse("No more Payloads should be available", parser.hasNext());
    }

    /**
     * Test no id found.
     * @throws Exception If Error.
     */
    public final void testNoIDFound() throws Exception {
        Configuration conf = getBasicConfiguration();
        conf.set(XMLSplitterFilter.CONF_ID_ELEMENT, "nonexisting");
        ObjectFilter parser = new XMLSplitterFilter(conf);
        parser.setSource(this);
        startProducer(2);
        assertFalse("No records should be available as all has bad IDs",
                    parser.hasNext());
    }

    /**
     * Test random id.
     * @throws Exception If random.
     */
    public final void testRandomID() throws Exception {
        final int magic = 4;
        Configuration conf = getBasicConfiguration();
        conf.set(XMLSplitterFilter.CONF_ID_ELEMENT, "");
        ObjectFilter parser = new XMLSplitterFilter(conf);
        parser.setSource(this);
        startProducer(2);
        for (int i = 0; i < magic; i++) {
            String id = parser.next().getRecord().getId();
            assertTrue("The id '" + id + "' should contain the string "
                       + "'randomID'", id.contains("randomID"));
        }
    }

    /**
     * Test close call.
     * @throws Exception If error.
     */
    public final void testCloseCall() throws Exception {
        Configuration conf = getBasicConfiguration();
        ObjectFilter parser = new XMLSplitterFilter(conf);
        parser.setSource(this);
        startProducer(2);
        parser.next();
        parser.next(); // Hits EOF
        // TODO this tests is testing something in {@link ThreadedStreamParser}
        //assertEquals("Close should be performed at the end of first Payload",
        //            1, closeCount);
        parser.next();
        parser.next();
        assertEquals("Close should be performed at the end of second Payload",
                     2, closeCount);
        assertFalse("No more processed Payloads should be available",
                    parser.hasNext());
    }

    /**
     * Test entity.
     * Test for https://sourceforge.net/apps/trac/summa/ticket/96
     * @throws Exception If error.
     */
    public final void testEntity() throws Exception {
        Configuration conf = getBasicConfiguration();
        ObjectFilter parser = new XMLSplitterFilter(conf);
        parser.setSource(this);
        startProducer(2);
        Payload p = parser.next();
        assertTrue("Payload should stil contain a '&amp;'",
                  new String(p.getRecord().getContent()).contains("&amp;"));
    }

    /**
     * @return A baisc configuration.
     */
    private Configuration getBasicConfiguration() {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(XMLSplitterFilter.CONF_BASE, "testbase");
        conf.set(XMLSplitterFilter.CONF_COLLAPSE_PREFIX, "true");
        conf.set(XMLSplitterFilter.CONF_ID_ELEMENT, "identifier");
        conf.set(XMLSplitterFilter.CONF_ID_PREFIX, "myprefix");
        conf.set(XMLSplitterFilter.CONF_PRESERVE_NAMESPACES, "true");
        conf.set(XMLSplitterFilter.CONF_RECORD_ELEMENT, "record");
        conf.set(XMLSplitterFilter.CONF_REQUIRE_VALID, "true");
        return conf;
    }

    /**
     *  ObjectFilter implementation.
     */
    private void startProducer(int payloadCount) {
        this.payloadCount = payloadCount;
        closeCount = 0;
    }

    /** Number of payloads before close. */
    int closeCount = 0;
    /** Number of payloads. */
    private int payloadCount = 0;

    @Override
    public final boolean hasNext() {
        return payloadCount > 0;
    }

    @Override
    public final Payload next() {
        if (payloadCount <= 0) {
            return null;
        }
        ByteArrayInputStream stream = null;
        try {
            stream = new ByteArrayInputStream(
                    XMLSplitterParserTest.multiXML.getBytes("utf-8")) {
                @Override
                public void close() throws IOException {
                    super.close();
                    closeCount++;
                }
            };
        } catch (UnsupportedEncodingException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
        payloadCount--;
        return new Payload(stream);
    }

    @Override
    public final boolean pump() throws IOException {
        return hasNext() && next() != null;
    }

    @Override
    public void remove() {
        // not used
    }

    @Override
    public void setSource(Filter filter) {
        // not used
    }

    @Override
    public void close(boolean success) {
        // not used
    }
}
