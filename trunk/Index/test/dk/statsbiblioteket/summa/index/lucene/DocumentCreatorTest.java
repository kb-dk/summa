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

import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayInputStream;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.xml.DefaultNamespaceContext;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.index.lucene.DocumentCreator;
import org.apache.lucene.document.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DocumentCreatorTest extends TestCase implements ObjectFilter {
    public DocumentCreatorTest(String name) {
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
        return new TestSuite(DocumentCreatorTest.class);
    }

    public static final String SIMPLE_DESCRIPTOR =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
            + "<IndexDescriptor version=\"1.0\">\n"
            + "    <fields>\n"
            + "        <field name=\"mystored\" parent=\"stored\" "
            + "indexed=\"true\"/>\n"
            + "    </fields>\n"
            + "    <defaultSearchFields>freetext mystored"
            + "</defaultSearchFields>\n"
            + "</IndexDescriptor>";

    public static final String SIMPLE_RECORD =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
            + "<SummaDocument version=\"1.0\" boost=\"1.5\" "
            + "id=\"mybase:grimme_aellinger\" xmlns=\""
            + StreamingDocumentCreator.SUMMA_NAMESPACE + "\">\n"
            + "    <fields>\n"
            + "        <field name=\"mystored\" boost=\"2.0\">Foo bar</field>\n"
            + "        <field name=\"mystored\" boost=\"2.0\">Kazam</field>\n"
            + "        <field name=\"keyword\">Flim flam</field>\n"
            + "        <field name=\"nonexisting\">Should be default</field>\n"
            + "    </fields>\n"
            + "</SummaDocument>";

    public static final String NAMESPACED_RECORD =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
            + "<SummaDocument version=\"1.0\" boost=\"1.5\" "
            + "id=\"mybase:grimme_aellinger\""
            + " xmlns:Index=\"" + StreamingDocumentCreator.SUMMA_NAMESPACE
            + "\""
            + " xmlns=\"" + StreamingDocumentCreator.SUMMA_NAMESPACE + "\">\n"
            + "    <fields>\n"
            + "        <field name=\"mystored\" boost=\"2.0\">Foo bar</field>\n"
            + "        <field name=\"mystored\" boost=\"2.0\">Kazam</field>\n"
            + "        <Index:field name=\"keyword\">Flim flam</Index:field>\n"
            + "        <field name=\"nonexisting\">Should be default"
            + "</field>\n"
            + "    </fields>\n"
            + "</SummaDocument>";

    public static final String CREATOR_SETUP =
            "<xproperties>\n"
            + "    <xproperties>\n"
            + "        <entry>\n"
            + "            <key>" + IndexDescriptor.CONF_DESCRIPTOR
            + "</key>\n"
            + "            <value class=\"xproperties\">\n"
            + "                <entry>\n"
            + "                    <key>"
            + IndexDescriptor.CONF_ABSOLUTE_LOCATION + "</key>\n"
            + "                    <value class=\"string\">%s</value>\n"
            + "                </entry>\n"
            + "            </value>\n"
            + "        </entry>\n"
            + "    </xproperties>\n"
            + "</xproperties>";

    public void testEntityHandling() throws Exception {
        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder domBuilder = builderFactory.newDocumentBuilder();

        String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                     + "<outer>\n"
                     + "<entitied>&lt;</entitied>\n"
                     + "<cdataed><![CDATA[<]]></cdataed>\n"
                     + "</outer>";
        org.w3c.dom.Document dom = domBuilder.parse(
                new ByteArrayInputStream(XML.getBytes("utf-8")));

        XPath xPath = XPathFactory.newInstance().newXPath();
        assertEquals("The content of entitied should be unencoded",
                     "<", ((Node) xPath.compile("outer/entitied").
                evaluate(dom, XPathConstants.NODE)).getTextContent());
        assertEquals("The content of cdataed should be unencoded",
                     "<", ((Node) xPath.compile("outer/cdataed").
                evaluate(dom, XPathConstants.NODE)).getTextContent());
    }

    public void testXPath() throws Exception {
        DefaultNamespaceContext nsCon = new DefaultNamespaceContext();
        nsCon.setNameSpace(StreamingDocumentCreator.SUMMA_NAMESPACE,
                           DocumentCreator.SUMMA_NAMESPACE_PREFIX);
        XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(nsCon);
        XPathExpression singleFieldXPathExpression =
                xPath.compile("/Index:SummaDocument/Index:fields/Index:field");

        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();

        builderFactory.setNamespaceAware(true);
        builderFactory.setValidating(false);
        DocumentBuilder domBuilder = builderFactory.newDocumentBuilder();

        org.w3c.dom.Document dom = domBuilder.parse(
                new ByteArrayInputStream(SIMPLE_RECORD.getBytes("utf-8")));

        NodeList singleFields = (NodeList)
                singleFieldXPathExpression.evaluate(dom,
                                                    XPathConstants.NODESET);
        assertEquals("There should be the correct number of fields",
                     4, singleFields.getLength());
    }

    public void testsimpletransformation() throws Exception {
        Configuration conf = getCreatorConf();
        testSimpleTransformation(new StreamingDocumentCreator(conf));
    }

    public void testStreamTransformation() throws Exception {
        Configuration conf = getCreatorConf();
        testSimpleTransformation(new StreamingDocumentCreator(conf));
    }

    public void testTransformationSpeed() throws Exception {
        int WARM = 10;
        int RUNS = 10;
        int SUBRUNS = 10;

        System.out.println("Creating creators");
        Configuration conf = getCreatorConf();
        ObjectFilter dom = new StreamingDocumentCreator(conf);
        ObjectFilter stream = new StreamingDocumentCreator(conf);

        System.out.println("Performing warm up");
        for (int i = 0 ; i < WARM ; i++) {
            speed(dom, SUBRUNS);
            speed(stream, SUBRUNS);
        }

        System.out.println("\nNumbers are transformation/s");
        System.out.println("DOM\tStream");
        for (int i = 0 ; i < RUNS ; i++) {
            System.out.println(speed(dom, SUBRUNS) + "\t" 
                               + speed(stream, SUBRUNS));
        }
    }

    public double speed(ObjectFilter creator, int runs) throws Exception {
        long startTime = System.currentTimeMillis();
        for (int i = 0 ; i < runs ; i++) {
            testSimpleTransformation(creator);
        }
        return runs * 1000 / (System.currentTimeMillis() - startTime);
    }

    private Configuration getCreatorConf() throws IOException {
        File descriptorLocation = File.createTempFile("descriptor", ".xml");
        descriptorLocation.deleteOnExit();
        Files.saveString(SIMPLE_DESCRIPTOR, descriptorLocation);
        File confLocation = File.createTempFile("configuration", ".xml");
        confLocation.deleteOnExit();
        Files.saveString(String.format(
               CREATOR_SETUP,
               "file://" + descriptorLocation.getAbsoluteFile().toString()),
               confLocation);
        assertTrue("The configuration should exist",
                   descriptorLocation.getAbsoluteFile().exists());

        Configuration conf = new Configuration(new XStorage(confLocation));
        LuceneIndexDescriptor id = new LuceneIndexDescriptor(
                    conf.getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR));
        assertNotNull("A descriptor should be created", id);
        return conf;
    }

    public void testSimpleTransformation(ObjectFilter creator) throws
                                                                  Exception {

        creator.setSource(this);
        Payload processed = creator.next();
        assertNotNull("Payload should have a document", 
                      processed.getData(Payload.LUCENE_DOCUMENT));
        Document doc = (Document)processed.getData(Payload.LUCENE_DOCUMENT);
        assertTrue("The document should have some fields",
                   doc.getFields().size() > 0);
        for (String fieldName: new String[]{"mystored", "freetext",
                                            "nonexisting",
                                            IndexUtils.RECORD_FIELD}) {
            assertNotNull("The document should contain the field " + fieldName,
                          doc.getField(fieldName));
        }
        assertEquals("The document boost should be correct",
                     1.5f, doc.getBoost());
    }

    /* ObjectFilter implementation */

    public boolean hasNext() {
        return true;
    }

    public Payload next() {
        try {
            return new Payload(new Record("dummy", "fooBase",
//                                          SIMPLE_RECORD.getBytes("utf-8")));
            NAMESPACED_RECORD.getBytes("utf-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void remove() {
    }

    public void setSource(Filter filter) {
    }

    public boolean pump() throws IOException {
        return next() != null;
    }

    public void close(boolean success) {
    }
}




