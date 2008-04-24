package dk.statsbiblioteket.summa.index;

import javax.xml.transform.Transformer;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.Streams;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class XMLTransformerTest extends TestCase {
    public XMLTransformerTest(String name) {
        super(name);
    }

    private static final String XSLT_ENTRY = "data/fagref/fagref_index.xsl";
    public static URL xsltEntryURL = getURL(XSLT_ENTRY);

    public static URL getURL(String resource) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(resource);
        assertNotNull("The resource " + resource + " must be present",
                      url);
        return url;
    }


    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(XMLTransformerTest.class);
    }

    public void testTransformerSetup() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(XMLTransformer.CONF_XSLT, xsltEntryURL);
        new XMLTransformer(conf);
        // Throws exception by itself in case of error
    }

    private static final String GURLI = "data/fagref/gurli.margrethe.xml";
    public void testTransformation() throws Exception {
        String content = Streams.getUTF8Resource(GURLI);
        Record record = new Record("fagref:gurli_margrethe", "fagref",
                                   content.getBytes("utf-8"));
        Payload payload = new Payload(record);

        Configuration conf = Configuration.newMemoryBased();
        conf.set(XMLTransformer.CONF_XSLT, xsltEntryURL);
        XMLTransformer transformer = new XMLTransformer(conf);

        transformer.processPayload(payload);
        String transformed = payload.getRecord().getContentAsUTF8();
        String[] MUST_CONTAIN = new String[]{"sortLocale", "Yetitæmning",
                                             "<shortrecord>", "boostFactor"};
        for (String must: MUST_CONTAIN) {
            assertTrue("The result must contain " + must,
                       transformed.contains(must));
        }
    }
}
