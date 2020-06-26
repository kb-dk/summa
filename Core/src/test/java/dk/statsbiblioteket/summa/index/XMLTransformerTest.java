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
package dk.statsbiblioteket.summa.index;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.summa.common.xml.XHTMLEntityResolver;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.SequenceType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class XMLTransformerTest extends TestCase {
    private static Log log = LogFactory.getLog(XMLTransformerTest.class);

    public XMLTransformerTest(String name) {
        super(name);
    }

    public static final String FAGREF_XSLT_ENTRY = "index/fagref/fagref_index.xsl";
    public static final URL xsltFagrefEntryURL = getURL(FAGREF_XSLT_ENTRY);

    // 2.6MB dobundle containing ALTO from the newspaper search project at kb.dk
    // The dobundle is large due to an update script running amok and is useful for
    // testing processing speed
    public static final String ALTO = "support/alto/ba7b39e9-1773-498e-84d0-bd6ecfb61b76.xml.gz";
    public static final String ALTO_XSLT = "support/alto/doms_aviser.xsl";

    public static URL getURL(String resource) {
        URL url = Resolver.getURL(resource);
        assertNotNull("The resource " + resource + " must be present", url);
        return url;
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
        return new TestSuite(XMLTransformerTest.class);
    }

    public void testTransformerSetup() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(XMLTransformer.CONF_XSLT, xsltFagrefEntryURL);
        new XMLTransformer(conf);
        // Throws exception by itself in case of error
    }

    public void testTransformerSetupWithResource() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(XMLTransformer.CONF_XSLT, FAGREF_XSLT_ENTRY);
        new XMLTransformer(conf);
        // Throws exception by itself in case of error
    }

    public void testURL() throws Exception {
        try {
            new URL("foo.bar");
            fail("The URL 'foo.bar' should not be valid");
        } catch (MalformedURLException e) {
            // Expected
        }
    }

    // Experimental!
    // Special setup to get Saxon to work. Stable XSLT is Xalan, but Saxon is under consideration due to speed
    public void testSetupSaxonCallcack() throws SaxonApiException, IOException, TransformerConfigurationException {
/*        Processor proc = new Processor(false);
        ExtensionFunction sqrt = new ExtensionFunction() {
            public QName getName() {
                return new QName("http://math.com/", "sqrt");
            }
            public SequenceType getResultType() {
                return SequenceType.makeSequenceType(ItemType.DOUBLE, OccurrenceIndicator.ONE );
            }
            public net.sf.saxon.s9api.SequenceType[] getArgumentTypes() {
                return new SequenceType[]{
                        SequenceType.makeSequenceType(ItemType.DOUBLE, OccurrenceIndicator.ONE)};
            }
            public XdmValue call(XdmValue[] arguments) throws SaxonApiException {
                double arg = ((XdmAtomicValue)arguments[0].itemAt(0)).getDoubleValue();
                double result = Math.sqrt(arg); return new XdmAtomicValue(result);
            }
        };
        proc.registerExtensionFunction(sqrt);*/

        net.sf.saxon.Configuration saxonConf = new net.sf.saxon.Configuration();
        saxonConf.registerExtensionFunction(new ShiftLeft());
        byte[] content = Streams.getUTF8Resource(ALTO).getBytes(StandardCharsets.UTF_8);

                TransformerFactory tfactory = TransformerFactory.newInstance();
        
        InputStream in = Resolver.getURL(ALTO_XSLT).openStream();
        Transformer transformer = tfactory.newTransformer(new StreamSource(in, ALTO_XSLT));

/*        XPathCompiler comp = proc.newXPathCompiler();
        comp.declareNamespace("mf", "http://math.com/");
        comp.declareVariable(new QName("arg"));
        XPathExecutable exp = comp.compile("mf:sqrt($arg)");
        XPathSelector ev = exp.load();
        ev.setVariable(new QName("arg"), new XdmAtomicValue(2.0));
        XdmValue val = ev.evaluate();
        System.out.println(val.toString());*/
    }
    private static class ShiftLeft extends ExtensionFunctionDefinition {
        @Override
        public StructuredQName getFunctionQName() {
            return new StructuredQName("eg", "http://example.com/saxon-extension", "shift-left");
        }
        @Override
        public SequenceType[] getArgumentTypes() {
            return new SequenceType[]{SequenceType.SINGLE_INTEGER, SequenceType.SINGLE_INTEGER};
        }
        @Override
        public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
            return SequenceType.SINGLE_INTEGER;
        }
        @Override
        public ExtensionFunctionCall makeCallExpression() {
            return new ExtensionFunctionCall() {
                @Override public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
                    long v0 = ((IntegerValue)arguments[0]).longValue();
                    long v1 = ((IntegerValue)arguments[1]).longValue();
                    long result = v0<<v1;
                    return Int64Value.makeIntegerValue(result);
                }
            };
        }
    }


    // Performance test of XML transformation
    public void testALTOPerformace() throws IOException, PayloadException {
        final int WARM = 3;
        final int RUNS = 20;
        Configuration conf = Configuration.newMemoryBased(
                XMLTransformer.CONF_XSLT, ALTO_XSLT
        );
        XMLTransformer transformer = new XMLTransformer(conf);
        String content = Strings.flush(new GZIPInputStream(Resolver.getURL(ALTO).openStream()));
        final byte[] incoming = content.getBytes(StandardCharsets.UTF_8);
        Record record = new Record("alto", "xml", incoming);

        for (int i = 0 ; i < WARM ; i++) {
            record.setContent(incoming, false);
            transformer.processRecord(record, true, null);
        }

        Profiler profiler = new Profiler(RUNS);
        for (int i = 0 ; i < RUNS ; i++) {
            record.setContent(incoming, false);
            transformer.processRecord(record, true, null);
            profiler.beat();
        }
        log.info(String.format(Locale.ROOT, "Finished stress testing XMLTransformer with %d runs at %.1fms/run",
                               RUNS, 1000/profiler.getBps(false)));
//        System.out.println(record.getContentAsUTF8());
    }

    public void testSpecificXSLTProblem() {
        final String PROBLEM = "/home/te/tmp/sumfresh/sites/sbsolr/xslt/index/sb_aleph/aleph_index.xsl";
        if (Resolver.getFile(PROBLEM) == null) {
            log.debug("Sorry, testSpecificXSLTProblem() only runs locally on Toke's computer");
            return;
        }
        Configuration conf = Configuration.newMemoryBased(
                XMLTransformer.CONF_XSLT, PROBLEM
        );
        new XMLTransformer(conf);
    }


    /*
    Iterating comma separated values in XSLT is done with recursion. Too many values blows the stack with a
    StackOverflowError, which previously meant a shutdown of the JVM. As this is hard to guard against and
    disrupts the processing flow, the shutdown was made optional.
     */
    // Disabled as the stack problem has yet to be demonstrated with Saxon (as opposed to Xalan)
    public void disabledtestStackOverflowHandling() throws Exception {
        final String SAMPLE = "index/stackoverflow/authors.xml";

        Configuration conf = Configuration.newMemoryBased();
        conf.set(XMLTransformer.CONF_XSLT, "index/stackoverflow/iterate.xslt");
        OpenTransformer transformer = new OpenTransformer(conf);

        String content = Streams.getUTF8Resource(SAMPLE);
        Record record = new Record("4K", "xml", content.getBytes(StandardCharsets.UTF_8));
        Payload payload = new Payload(record);

        try  {
            transformer.process(payload);
            fail("Transformation of " + SAMPLE + " should always fail, but produced\n"
                 + payload.getRecord().getContentAsUTF8());
        } catch (StackOverflowError e) {
            fail("Default XMLTransformer stack overflow Throwable should be a PayloadException."
                 + " Got StackOverflowError");
        } catch (PayloadException e) {
            // Expected
        }
    }

    // Temporary test. Delete at will or leave it - if the files does not exist it will exit quietly
    public void testMissingOutput() throws Exception {
        final String SAMPLE = "/home/te/tmp/sumfresh/sites/doms/dump/index_1_premux/pvica_tv_oai_du_8dc41330-24b5-436a-82c0-814185351981.xml";
        final String XSLT = "/home/te/tmp/sumfresh/sites/doms/xslt/index/doms/pvica_tv.xsl";
        if (!new File(SAMPLE).exists()) {
            log.info("Unable to run testMissingInput() as it requires a local file");
            return;
        }

        Configuration conf = Configuration.newMemoryBased();
        conf.set(XMLTransformer.CONF_XSLT, XSLT);
        conf.set(XMLTransformer.CONF_STRIP_XML_NAMESPACES, false);
        OpenTransformer transformer = new OpenTransformer(conf);

        String content = Files.loadString(new File(SAMPLE));
        Record record = new Record("empty", "xml", content.getBytes(StandardCharsets.UTF_8));
        Payload payload = new Payload(record);

        transformer.process(payload);
        transformer.close(true);
        System.out.println("Transformed output:\n" + payload.getRecord().getContentAsUTF8());
    }

    // A switch from Xalan to Saxon broke the stack overflow trigger
    public void disabledtestStackOverflowNonHandling() throws Exception {
        final String SAMPLE = "index/stackoverflow/authors.xml";

        Configuration conf = Configuration.newMemoryBased();
        conf.set(XMLTransformer.CONF_XSLT, "index/stackoverflow/iterate.xslt");
        conf.set(XMLTransformer.CONF_CATCH_STACK_OVERFLOW, false);
        OpenTransformer transformer = new OpenTransformer(conf);

        String content = Streams.getUTF8Resource(SAMPLE);
        Record record = new Record("4K", "xml", content.getBytes(StandardCharsets.UTF_8));
        Payload payload = new Payload(record);

        try  {
            transformer.process(payload);
            transformer.close(true);
            fail("Transformation of " + SAMPLE + " should always fail, but produced\n"
                 + payload.getRecord().getContentAsUTF8());
        } catch (StackOverflowError e) {
            transformer.close(false);
            // Expected
        } catch (PayloadException e) {
            transformer.close(false);
            fail("XMLTransformer stack overflow Throwable should be a StackOverflowError when "
                 + XMLTransformer.CONF_CATCH_STACK_OVERFLOW + " is false");
        }
    }

    public void testEntityResolver() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(XMLTransformer.CONF_XSLT, "index/identity.xslt");
        conf.set(XMLTransformer.CONF_ENTITY_RESOLVER, XHTMLEntityResolver.class);
        OpenTransformer transformer = new OpenTransformer(conf);

        String content = Streams.getUTF8Resource("index/webpage_xhtml-1.0-strict.xml");
        Record record = new Record("validwebpage", "xhtml", content.getBytes(StandardCharsets.UTF_8));
        Payload payload = new Payload(record);
        transformer.process(payload);
        String transformed = payload.getRecord().getContentAsUTF8();
        String expected = "Rødgrød med fløde → mæthed";
        assertTrue("The transformed content '" + transformed + "' should contain '" + expected + "'",
                   transformed.contains(expected));
    }

    public void testEntitiesXML() throws Exception {
        assertEntities("index/webpage_xhtml-1.0-strict.xml");
    }
    public void testNotEntitiesXML() throws Exception {
        assertEntities("index/webpage_xhtml-no_entities.xml");
    }

    private void assertEntities(String source) throws IOException, PayloadException {
        Configuration conf = Configuration.newMemoryBased(
                XMLTransformer.CONF_XSLT, "index/identity.xslt",
                XMLTransformer.CONF_ENTITY_RESOLVER, XHTMLEntityResolver.class,
                XMLTransformer.CONF_SOURCE, RecordUtil.PART.content.toString());
        OpenTransformer transformer = new OpenTransformer(conf);

        String content = Streams.getUTF8Resource(source);
        Record record = new Record("validwebpage", "xhtml", content.getBytes(StandardCharsets.UTF_8));
        Payload payload = new Payload(record);
        transformer.process(payload);
        String transformed = payload.getRecord().getContentAsUTF8();
        System.out.println(transformed);
        String expected = "Rødgrød med fløde → mæthed";
        String expectedAlternative = "Rødgrød med fløde → mæthed".replace("æ", "&aelig;").replace("ø", "oelig;");
        assertTrue("The transformed content '" + transformed + "' should contain '" + expected + "' or '"
                   + expectedAlternative + "'",
                   transformed.contains(expected) || transformed.contains(expectedAlternative));
        log.info("Transformed content:\n" + transformed);
    }

    public static final String GURLI = "index/fagref/gurli.margrethe.xml";
    public void testTransformation() throws Exception {
        String content = Streams.getUTF8Resource(GURLI);
        Record record = new Record("fagref:gurli_margrethe", "fagref", content.getBytes(StandardCharsets.UTF_8));
        Payload payload = new Payload(record);

        Configuration conf = Configuration.newMemoryBased();
        conf.set(XMLTransformer.CONF_XSLT, xsltFagrefEntryURL);
        OpenTransformer transformer = new OpenTransformer(conf);

        transformer.process(payload);
        String transformed = payload.getRecord().getContentAsUTF8();
        //System.out.println(transformed);
        String[] MUST_CONTAIN = new String[]{
            "sortLocale", "Yetitæmning</Index:field>", "<shortrecord>", "boost"};
        for (String must: MUST_CONTAIN) {
            assertTrue("The result must contain " + must, transformed.contains(must));
        }
    }
    private class OpenTransformer extends XMLTransformer {
        private OpenTransformer(Configuration conf) {
            super(conf);
        }
        public boolean process(Payload payload) throws PayloadException {
            return processPayload(payload);
        }
    }

    public static final String HANS = "index/fagref/hans.jensen.xml";
    public void testTraversal() throws IOException, PayloadException {
        String gurli = Streams.getUTF8Resource(GURLI);
        String hans = Streams.getUTF8Resource(HANS);
        Record gRecord = new Record("fagref:gurli_margrethe", "fagref", gurli.getBytes(StandardCharsets.UTF_8));
        Record hRecord = new Record("fagref:hans_jensen", "fagref", hans.getBytes(StandardCharsets.UTF_8));
        gRecord.setChildren(Arrays.asList(hRecord));
        Payload payload = new Payload(gRecord);
        Configuration conf = Configuration.newMemoryBased();
        conf.set(XMLTransformer.CONF_XSLT, xsltFagrefEntryURL);
        conf.set(XMLTransformer.CONF_VISIT_CHILDREN, true);
        conf.set(XMLTransformer.CONF_SUCCESS_REQUIREMENT, XMLTransformer.REQUIREMENT.all);

        OpenTransformer transformer = new OpenTransformer(conf);
        transformer.process(payload);

        String transformedChild =
            payload.getRecord().getChildren().get(0).getContentAsUTF8();
        String[] MUST_CONTAIN = new String[]{
            "Python</Index:field>"};
        for (String must: MUST_CONTAIN) {
            assertTrue("The result must contain " + must, transformedChild.contains(must));
        }
    }
}