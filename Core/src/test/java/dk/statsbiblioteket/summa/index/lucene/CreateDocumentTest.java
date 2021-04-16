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
package dk.statsbiblioteket.summa.index.lucene;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.index.XMLTransformer;
import dk.statsbiblioteket.summa.index.XMLTransformerTest;
import dk.statsbiblioteket.util.Streams;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class CreateDocumentTest extends TestCase implements ObjectFilter {
    /** Logger. */
    private static Log log = LogFactory.getLog(CreateDocumentTest.class);

    /**
     * Constructor.
     * @param name The name.
     */
    public CreateDocumentTest(String name) {
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

    /**
     * Return this test class.
     * @return This test class.
     */
    public static Test suite() {
        return new TestSuite(CreateDocumentTest.class);
    }

    /** Gurli test document. */
    private static final String GURLI = "index/fagref/gurli.margrethe.xml";
    /** Hans test document. */
    private static final String HANS = "index/fagref/hans.jensen.xml";
    /** Jens test document. */
    private static final String JENS = "index/fagref/jens.hansen.xml";

    public void testProcesspayload() throws Exception {
        initcontent(GURLI, HANS, JENS);

        Configuration conf = Configuration.newMemoryBased();
        conf.set(XMLTransformer.CONF_XSLT,
                 XMLTransformerTest.xsltFagrefEntryURL);
        XMLTransformer transformer = new XMLTransformer(conf);
        transformer.setSource(this);

        StreamingDocumentCreator creator =
                new StreamingDocumentCreator(Configuration.newMemoryBased());
        creator.setSource(transformer);
        log.debug("Ready for getting payload from chain");
        
        assertTrue("The chain must have an element", creator.hasNext());
        Payload gurli = creator.next();
        Document gurliDoc =
            (Document)gurli.getData(Payload.LUCENE_DOCUMENT);
        assertNotNull("The Gurli Payload should have a Document",
                      gurliDoc);
        assertNotNull("The Gurli Document should contain the Field "
                     + IndexUtils.RECORD_FIELD,
                     gurli.getRecord().getId());
        String id = null;        
        try {
            id = gurli.getRecord().getId();
        } catch(Exception e) {
            //e.printStackTrace();
            fail("Exception from 'gurliDoc.getValues("
                    + IndexUtils.RECORD_FIELD + ")[0]'");
        }
        // TODO should RecordID be present in gurilDoc as well?
        assertEquals("The Gurli Document should have the correct id",
                     "fagref:" + GURLI, id);
        creator.close(true);
        //noinspection ControlFlowStatementWithoutBraces,StatementWithEmptyBody
        while (creator.pump());
    }

    // TODO: Test close(true/false), EOF et al.

    private List<Payload> payloads = new ArrayList<>(10);
    /**
     * Initialize the content.
     * @param xmlFiles XML content files.
     * @throws Exception If error.
     */
    private void initcontent(String... xmlFiles) throws Exception {
        payloads.clear();
        for (String xmlFile: xmlFiles) {
            log.debug("Creating payload for content at '" + xmlFile + "'");
            String content = Streams.getUTF8Resource(xmlFile);
            Record record = new Record("fagref:" + xmlFile, "fagref",
                                       content.getBytes(StandardCharsets.UTF_8));
            payloads.add(new Payload(record));
        }
        log.debug("Number of payloads: " + payloads.size());
    }

    /* ObjectFilter implementation */
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        log.debug("payloads.size() = " + payloads.size());
        return !payloads.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Payload next() {
        if (!hasNext()) {
            throw new IndexOutOfBoundsException("No more payloads");
        }
        return payloads.remove(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() {
        // Nada
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSource(Filter filter) {
        throw new IllegalAccessError("Not defined");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean pump() throws IOException {
        return hasNext() && next() != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close(boolean success) {
        payloads.clear();
    }
}
