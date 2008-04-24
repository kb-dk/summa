package dk.statsbiblioteket.summa.index;

import java.net.URL;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.util.Streams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class CreateDocumentTest extends TestCase implements ObjectFilter {
    private static Log log = LogFactory.getLog(CreateDocumentTest.class);

    public CreateDocumentTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(CreateDocumentTest.class);
    }

    private static final String GURLI = "data/fagref/gurli.margrethe.xml";
    private static final String HANS = "data/fagref/hans.jensen.xml";
    private static final String JENS = "data/fagref/jens.hansen.xml";

    public void testProcesspayload() throws Exception {
        initcontent(GURLI, HANS, JENS);

        Configuration conf = Configuration.newMemoryBased();
        conf.set(XMLTransformer.CONF_XSLT, XMLTransformerTest.xsltEntryURL);
        XMLTransformer transformer = new XMLTransformer(conf);
        transformer.setSource(this);

        CreateDocument creator =
                new CreateDocument(Configuration.newMemoryBased());
        creator.setSource(transformer);

        assertTrue("The chain must have an element", creator.hasNext());
        Payload gurli = creator.next();
        Document gurliDoc = (Document)gurli.getData(Payload.LUCENE_DOCUMENT);
        assertNotNull("The Gurli Payload should have a Document",
                      gurliDoc);
        assertNotNull("The Gurli Document should contain the Field "
                     + IndexUtils.RECORD_FIELD,
                     gurliDoc.getValues(IndexUtils.RECORD_FIELD));
        assertEquals("The Gurli Document should have the correct id",
                     "fagref:" + GURLI,
                     gurliDoc.getValues(IndexUtils.RECORD_FIELD)[0]);
        creator.close(true);
        //noinspection ControlFlowStatementWithoutBraces,StatementWithEmptyBody
        while (creator.pump());
    }

    // TODO: Test close(true/false), EOF et al.

    private List<Payload> payloads = new ArrayList<Payload>(10);
    private void initcontent(String... xmlFiles) throws Exception {
        payloads.clear();
        for (String xmlFile: xmlFiles) {
            log.debug("Creating payload for content at '" + xmlFile + "'");
            String content = Streams.getUTF8Resource(xmlFile);
            Record record = new Record("fagref:" + xmlFile, "fagref",
                                       content.getBytes("utf-8"));
            payloads.add(new Payload(record));
        }
    }

    /* ObjectFilter implementation */

    public boolean hasNext() {
        return payloads.size() > 0;
    }

    public Payload next() {
        if (!hasNext()) {
            throw new IndexOutOfBoundsException("No more payloads");
        }
        return payloads.remove(0);
    }

    public void remove() {
        // Nada
    }

    public void setSource(Filter filter) {
        throw new IllegalAccessError("Not defined");
    }

    public boolean pump() throws IOException {
        return hasNext() && next() != null;
    }

    public void close(boolean success) {
        payloads.clear();
    }
}
