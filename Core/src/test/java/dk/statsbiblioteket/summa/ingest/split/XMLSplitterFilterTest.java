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

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterBase;
import dk.statsbiblioteket.summa.common.filter.object.XMLReplaceFilter;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test classes for {@link XMLSplitterFilter}.
 */
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

    public void testNestedRecords() throws IOException {
        XMLSplitterFilter splitter = new XMLSplitterFilter(Configuration.newMemoryBased(
                ObjectFilterBase.CONF_FILTER_NAME, "Splitter",
                XMLSplitterFilter.CONF_ID_PREFIX, "dummy",
                XMLSplitterFilter.CONF_COLLAPSE_PREFIX, true,
                XMLSplitterFilter.CONF_RECORD_ELEMENT, "record",
                XMLSplitterFilter.CONF_RECORD_NAMESPACE, "http://www.openarchives.org/OAI/2.0/",
                XMLSplitterFilter.CONF_ID_ELEMENT, "identifier",
                XMLSplitterFilter.CONF_ID_NAMESPACE, "http://www.openarchives.org/OAI/2.0/",
                XMLSplitterFilter.CONF_BASE, "dummybase",
                XMLSplitterFilter.CONF_PRESERVE_NAMESPACES, true,
                XMLSplitterFilter.CONF_REQUIRE_VALID, false
        ));
        splitter.setSource(new PayloadFeederHelper("ingest/oai_nested_record.xml"));
        assertTrue("There should be a Record available", splitter.hasNext());
        String produced = splitter.next().getRecord().getContentAsUTF8();
        assertEquals("There should be the right number of open- and close-tags for 'record'\n" + produced,
                     4, produced.split("record").length-1);
        //System.out.println(produced);
    }

    public void testNestedRecordsInner() throws IOException {
        XMLSplitterFilter splitter = new XMLSplitterFilter(Configuration.newMemoryBased(
                ObjectFilterBase.CONF_FILTER_NAME, "Splitter",
                XMLSplitterFilter.CONF_ID_PREFIX, "dummy",
                XMLSplitterFilter.CONF_COLLAPSE_PREFIX, true,
                XMLSplitterFilter.CONF_RECORD_ELEMENT, "record",
                XMLSplitterFilter.CONF_RECORD_NAMESPACE, "info:lc/xmlns/marcxchange-v1",
                XMLSplitterFilter.CONF_ID_ELEMENT, "leader",
                XMLSplitterFilter.CONF_ID_NAMESPACE, "info:lc/xmlns/marcxchange-v1",
                XMLSplitterFilter.CONF_BASE, "dummybase",
                XMLSplitterFilter.CONF_PRESERVE_NAMESPACES, true,
                XMLSplitterFilter.CONF_REQUIRE_VALID, false
        ));
        splitter.setSource(new PayloadFeederHelper("ingest/oai_nested_record.xml"));
        assertTrue("There should be a Record available", splitter.hasNext());
        String produced = splitter.next().getRecord().getContentAsUTF8();
        assertEquals("There should be the right number of open- and close-tags for 'record'\n" + produced,
                     2, produced.split("record").length-1);
        System.out.println(produced);
        /*
        XMLReplaceFilter replacer = new XMLReplaceFilter(Configuration.newMemoryBased(
                XMLReplaceFilter.CONF_ID_FIELDS, "001*a,002*a,002*c,011*a,013*a,014*a,015*a,016*a,017*a,018*a"
        ));
        replacer.setSource(splitter);
        StreamController marcSplitter = new StreamController(Configuration.newMemoryBased(
                StreamController.CONF_PARSER, SBMARCParser.class,
                MARCParser.CONF_BASE, "dummybase",
                MARCParser.CONF_ID_PREFIX, "dummyid_"
        ));
        marcSplitter.setSource(replacer);
        assertTrue("There should be a Record available", marcSplitter.hasNext());
        String produced = marcSplitter.next().getRecord().getContentAsUTF8();
        assertEquals("There should be the right number of open- and close-tags for 'record'\n" + produced,
                     2, produced.split("record").length-1);*/

    }

    public void testNestedRecordsInnerMarc() throws IOException {
        XMLSplitterFilter splitter = new XMLSplitterFilter(Configuration.newMemoryBased(
                ObjectFilterBase.CONF_FILTER_NAME, "Splitter",
                XMLSplitterFilter.CONF_ID_PREFIX, "dummy",
                XMLSplitterFilter.CONF_COLLAPSE_PREFIX, true,
                XMLSplitterFilter.CONF_RECORD_ELEMENT, "record",
                XMLSplitterFilter.CONF_RECORD_NAMESPACE, "info:lc/xmlns/marcxchange-v1",
                XMLSplitterFilter.CONF_ID_ELEMENT, "leader", // Will be overwritten later
                XMLSplitterFilter.CONF_ID_NAMESPACE, "info:lc/xmlns/marcxchange-v1",
                XMLSplitterFilter.CONF_BASE, "dummybase",
                XMLSplitterFilter.CONF_PRESERVE_NAMESPACES, true,
                XMLSplitterFilter.CONF_REQUIRE_VALID, false
        ));
        splitter.setSource(new PayloadFeederHelper("ingest/oai_nested_record.xml"));
        XMLReplaceFilter replacer = new XMLReplaceFilter(Configuration.newMemoryBased(
                XMLReplaceFilter.CONF_ID_FIELDS, "001*a,002*a,002*c,011*a,013*a,014*a,015*a,016*a,017*a,018*a"
        ));
        replacer.setSource(splitter);
        StreamController marcSplitter = new StreamController(Configuration.newMemoryBased(
                StreamController.CONF_PARSER, SBMARCParser.class,
                MARCParser.CONF_BASE, "dummybase",
                MARCParser.CONF_ID_PREFIX, "dummyid_"
        ));
        marcSplitter.setSource(replacer);
        assertTrue("There should be a Record available", marcSplitter.hasNext());
        Record record = marcSplitter.next().getRecord();
        String content = record.getContentAsUTF8();
        assertEquals("There should be the right number of open- and close-tags for 'record'\n" + content,
                     2, content.split("record").length-1);
        assertEquals("The Record should have the correct ID", "dummyid_12345678", record.getId());
        assertEquals("The Record should have the correct base", "dummybase", record.getBase());
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
        //noinspection StatementWithEmptyBody
        while (parser.pump());
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
     * @return A basic configuration.
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
        stream = new ByteArrayInputStream(
                XMLSplitterParserTest.multiXML.getBytes(StandardCharsets.UTF_8)) {
            private boolean closed = false;
            @Override
            public void close() throws IOException {
                if (closed) {
                    return; // TODO: Check why close is called twice
                }
                closed = true;
                super.close();
                closeCount++;
            }
        };
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
