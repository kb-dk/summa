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
package dk.statsbiblioteket.summa.common.xml;

import dk.statsbiblioteket.summa.common.configuration.Resolver;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.transform.*;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class XHTMLEntityResolverTest extends TestCase {
    public XHTMLEntityResolverTest(String name) {
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

    public void testNonescapingTransformation() throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(new StreamSource(
                new FileInputStream(Resolver.getFile("data/identity.xslt"))));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Result result = new StreamResult(out);
        try {
            transformer.transform(new StreamSource(new FileInputStream(
                    Resolver.getFile("data/webpage_xhtml-1.0-strict.xml"))),
                                  result);
            fail("Transformation of XHTML 1.0 content without DTD-resolving"
                 + " should fail");
        } catch (TransformerException e) {
            // Expected
        }
        //System.out.println("Transformation result:\n" + out.toString("utf-8"));
    }

    public void testEscapingTransformation() throws Exception {
        testEscapingTransformation("data/webpage_xhtml-1.0-strict.xml");
    }

    public void testEscapingTransformation2() throws Exception {
        testEscapingTransformation("data/tour_test.xml");
    }

    public void testEscapingTransformationNondeclared() throws Exception {
        try {
            testEscapingTransformation("data/webpage_html-nondeclared.html");
        } catch(Exception e) {
            e.printStackTrace();
            fail("Should be fixed");
        }
    }

    public static void testEscapingTransformation(String webpage)
                                                              throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(new StreamSource(
                new FileInputStream(Resolver.getFile("data/identity.xslt"))));

        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setEntityResolver(new XHTMLEntityResolver(null));

/*        XMLInputFactory xif = XMLInputFactory.newInstance();
        xif.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, true);
        xif.setProperty(XMLInputFactory.IS_VALIDATING, false);
        xif.setProperty(XMLInputFactory.SUPPORT_DTD, true);
        xif.setXMLResolver(new XMLResolver() {
            public Object resolveEntity(String publicID, String systemID, String baseURI, String namespace) throws XMLStreamException {
                System.out.println("public: " + publicID);
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });*/

/*        XMLStreamReader streamReader = xif.createXMLStreamReader(new FileInputStream(
                    Resolver.getFile(webpage)));
                                                */
        InputSource is = new InputSource(new FileInputStream(
                    Resolver.getFile(webpage)));
        Source source = new SAXSource(reader, is);
//        StAXSource ax = new StAXSource(streamReader);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Result result = new StreamResult(out);
        transformer.transform(source, result);
        //System.out.println("Transformation result:\n" + out.toString("utf-8"));
    }

    public static Test suite() {
        return new TestSuite(XHTMLEntityResolverTest.class);
    }
}

