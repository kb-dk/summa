/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.common.xml;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import javax.xml.transform.*;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;

import dk.statsbiblioteket.summa.common.configuration.Resolver;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderFactory;

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
                    Resolver.getFile("data/webpage_xhtml-1.0-strict.xml"))), result);
            fail("Transformation of XHTML 1.0 content without DTD-resolving"
                 + " should fail");
        } catch (TransformerException e) {
            // Expected
        }
        System.out.println("Transformation result:\n" + out.toString("utf-8"));
    }

    public void testEscapingTransformation() throws Exception {
        testEscapingTransformation("data/webpage_xhtml-1.0-strict.xml");
    }

    public void testEscapingTransformationNondeclared() throws Exception {
        testEscapingTransformation("data/webpage_html-nondeclared.html");
    }

    public static void testEscapingTransformation(String webpage)
                                                              throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(new StreamSource(
                new FileInputStream(Resolver.getFile("data/identity.xslt"))));

//        XMLReader reader = XMLReaderFactory.createXMLReader();
//        reader.setEntityResolver(new XHTMLEntityResolver(null));

        XMLInputFactory xif = XMLInputFactory.newInstance();
        xif.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, true);
        xif.setProperty(XMLInputFactory.IS_VALIDATING, false);
        xif.setProperty(XMLInputFactory.SUPPORT_DTD, true);
        xif.setXMLResolver(new XMLResolver() {
            public Object resolveEntity(String publicID, String systemID, String baseURI, String namespace) throws XMLStreamException {
                System.out.println("public: " + publicID);
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        XMLStreamReader streamReader = xif.createXMLStreamReader(new FileInputStream(
                    Resolver.getFile(webpage)));

        InputSource is = new InputSource(new FileInputStream(
                    Resolver.getFile(webpage)));
//        Source source = new SAXSource(reader, streamReader);
        StAXSource ax = new StAXSource(streamReader);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Result result = new StreamResult(out);
        transformer.transform(ax, result);
        System.out.println("Transformation result:\n" + out.toString("utf-8"));
    }

    public static Test suite() {
        return new TestSuite(XHTMLEntityResolverTest.class);
    }
}
