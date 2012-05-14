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
import dk.statsbiblioteket.summa.common.xml.XHTMLEntityResolver;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class XMLTransformerTest extends TestCase {
    public XMLTransformerTest(String name) {
        super(name);
    }

    public static final String FAGREF_XSLT_ENTRY = "index/fagref/fagref_index.xsl";
    public static final URL xsltFagrefEntryURL = getURL(FAGREF_XSLT_ENTRY);

    public static URL getURL(String resource) {
        URL url = Resolver.getURL(resource);
        assertNotNull("The resource " + resource + " must be present",
                      url);
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

    public void testEntityResolver() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(XMLTransformer.CONF_XSLT, "index/identity.xslt");
        conf.set(
                XMLTransformer.CONF_ENTITY_RESOLVER, XHTMLEntityResolver.class);
        OpenTransformer transformer = new OpenTransformer(conf);

        String content =
                Streams.getUTF8Resource("index/webpage_xhtml-1.0-strict.xml");
        Record record = new Record("validwebpage", "xhtml",
                                   content.getBytes("utf-8"));
        Payload payload = new Payload(record);
        transformer.process(payload);
        String transformed = payload.getRecord().getContentAsUTF8();
        String expected = "Rødgrød med fløde → mæthed";
        assertTrue("The transformed content '" + transformed
                   + "' should contain '" + expected + "'",
                   transformed.contains(expected));
    }

    public static final String GURLI = "index/fagref/gurli.margrethe.xml";
    public void testTransformation() throws Exception {
        String content = Streams.getUTF8Resource(GURLI);
        Record record = new Record("fagref:gurli_margrethe", "fagref",
                                   content.getBytes("utf-8"));
        Payload payload = new Payload(record);

        Configuration conf = Configuration.newMemoryBased();
        conf.set(XMLTransformer.CONF_XSLT, xsltFagrefEntryURL);
        OpenTransformer transformer = new OpenTransformer(conf);

        transformer.process(payload);
        String transformed = payload.getRecord().getContentAsUTF8();
        //System.out.println(transformed);
        String[] MUST_CONTAIN = new String[]{
            "sortLocale", "Yetitæmning</Index:field>", "<shortrecord>",
            "boost"};
        for (String must: MUST_CONTAIN) {
            assertTrue("The result must contain " + must,
                       transformed.contains(must));
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
        Record gRecord = new Record(
            "fagref:gurli_margrethe", "fagref", gurli.getBytes("utf-8"));
        Record hRecord = new Record(
            "fagref:hans_jensen", "fagref", hans.getBytes("utf-8"));
        gRecord.setChildren(Arrays.asList(hRecord));
        Payload payload = new Payload(gRecord);
        Configuration conf = Configuration.newMemoryBased();
        conf.set(XMLTransformer.CONF_XSLT, xsltFagrefEntryURL);
        conf.set(XMLTransformer.CONF_VISIT_CHILDREN, true);
        conf.set(XMLTransformer.CONF_SUCCESS_REQUIREMENT,
                 XMLTransformer.REQUIREMENT.all);

        OpenTransformer transformer = new OpenTransformer(conf);
        transformer.process(payload);

        String transformedChild =
            payload.getRecord().getChildren().get(0).getContentAsUTF8();
        String[] MUST_CONTAIN = new String[]{
            "Python</Index:field>"};
        for (String must: MUST_CONTAIN) {
            assertTrue("The result must contain " + must,
                       transformedChild.contains(must));
        }
    }
}