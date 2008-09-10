/* $Id: SearchWS.java,v 1.2 2007/10/04 13:28:21 mv Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:21 $
 * $Author: mv $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
package dk.statsbiblioteket.commons;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.Iterator;
import java.util.Map;

// TODO: these methods probably belong in sbutil or someplace similar

public class XmlOperations {
    /**
     * Given a File handle for an XML document and a File handle for an XSLT, transform the XML document with the XSLT and return the results in a String.
     *
     * @param xml  the xml file handle
     * @param xslt the xslt file handle
     * @return a string with the transformed document
     * @throws TransformerException if there is an error transforming
     */
    public static String xsltTransform(File xml, File xslt) throws TransformerException {
        String retval = xsltTransform(xml, xslt, null);
        return retval;
    }

    /**
     * Given a File handle for an XML document and a File handle for an XSLT, transform the XML document with the XSLT and return the results in a String.
     * Parameters can be passed to the XSLT.
     *
     * @param xml  the xml file handle
     * @param xslt the xslt file handle
     * @param map  a map from parameter names to parameter values to be passed to the xslt
     * @return a string with the transformed document
     * @throws TransformerException if there is an error transforming
     */
    public static String xsltTransform(File xml, File xslt, Map map) throws TransformerException {
        String retval;

        ByteArrayOutputStream ba = new ByteArrayOutputStream();

        TransformerFactory tf = TransformerFactory.newInstance();
        Source source = new StreamSource(xslt);
        Transformer transformer = tf.newTransformer(source);

        if (map != null) {
            Iterator iter = map.keySet().iterator();
            while (iter.hasNext()) {
                String pname = (String) iter.next();
                transformer.setParameter(pname, map.get(pname));
            }
        }

        transformer.transform(new StreamSource(xml), new StreamResult(ba));
        try {
            retval = ba.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("This should never happen", e);
        }

        return retval;
    }

    /**
     * Given an XML document in a DOM and a File handle for an XSLT, transform the XML document with the XSLT and return the results in a String.
     *
     * @param dom  the xml DOM
     * @param xslt the xslt file handle
     * @return a string with the transformed document
     * @throws TransformerException if there is an error transforming
     */
    public static String xsltTransform(Document dom, File xslt) throws TransformerException {
        String retval = xsltTransform(dom, xslt, null);
        return retval;
    }

    /**
     * Given an XML document in a DOM and a File handle for an XSLT, transform the XML document with the XSLT and return the results in a String.
     * Parameters can be passed to the XSLT.
     *
     * @param dom  the xml DOM
     * @param xslt the xslt file handle
     * @param map  a map from parameter names to parameter values to be passed to the xslt
     * @return a string with the transformed document
     * @throws TransformerException if there is an error transforming
     */
    public static String xsltTransform(Document dom, File xslt, Map map) throws TransformerException {
        String retval;

        ByteArrayOutputStream ba = new ByteArrayOutputStream();

        TransformerFactory tf = TransformerFactory.newInstance();
        Source source = new StreamSource(xslt);
        Transformer transformer = tf.newTransformer(source);

        if (map != null) {
            Iterator iter = map.keySet().iterator();
            while (iter.hasNext()) {
                String pname = (String) iter.next();
                transformer.setParameter(pname, map.get(pname));
            }
        }

        transformer.transform(new DOMSource(dom), new StreamResult(ba));
        try {
            retval = ba.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("This should never happen", e);
        }

        return retval;
    }

    /**
     * Replaces the characters ' " < > and & with &apos; &quot; &lt; &gt; and &amp;.
     *
     * @param input String to convert
     * @return A String with characters entity encoded
     */
    public static String entityEncode(String input) {
        return input.replaceAll("&", "&amp;").replaceAll("'", "&apos;").replaceAll("\"", "&quot;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }

    /**
     * Parses an XML document from a String to a DOM.
     *
     * @param xmlString a String containing an XML document
     * @return The document in a DOM
     * @throws SAXException                 Of there is a problem parsing the document
     * @throws IOException                  Should not be thrown, indicates an error reading from the String
     * @throws ParserConfigurationException Should not happen, no special argumetns are passed to the parser.
     */
    public static Document stringToDOM(String xmlString) throws SAXException, IOException, ParserConfigurationException {
        InputSource in = new InputSource();
        in.setCharacterStream(new StringReader(xmlString));
        Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
        return dom;
    }

    /**
     * Parses an XML document from a stream to a DOM.
     *
     * @param xmlStream a stream containing an XML document
     * @return The document in a DOM
     * @throws SAXException                 Of there is a problem parsing the document
     * @throws IOException                  Should not be thrown, indicates an error reading from the String
     * @throws ParserConfigurationException Should not happen, no special argumetns are passed to the parser.
     */
    public static Document streamToDOM(InputStream xmlStream) throws SAXException, IOException, ParserConfigurationException {
        Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlStream);
        return dom;
    }

    public static String domToString(Node d) throws TransformerException {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
        t.setOutputProperty(OutputKeys.METHOD,"xml");

        /* Transformer */
        StringWriter sw = new StringWriter();
        t.transform(new DOMSource(d),new StreamResult(sw));

        return sw.toString();
    }

    public static String domToString(Node d, boolean withXmlDeclaration) throws TransformerException {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        if (withXmlDeclaration) {
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        }
        else {
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        }
        t.setOutputProperty(OutputKeys.METHOD, "xml");

        /* Transformer */
        StringWriter sw = new StringWriter();
        t.transform(new DOMSource(d), new StreamResult(sw));

        return sw.toString();
    }

    public static NodeList xpathSelectNodeList(Node dom, String xpath) {
        NodeList retval = null;

        try {
            XPath xp = XPathFactory.newInstance().newXPath();
            retval = (NodeList) xp.evaluate(xpath, dom, XPathConstants.NODESET);
        } catch (NullPointerException e) {
            //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (XPathExpressionException e) {
            //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return retval;
    }

    public static Node xpathSelectSingleNode(Node dom, String xpath) {
        Node retval = null;

        try {
            XPath xp = XPathFactory.newInstance().newXPath();
            retval = (Node) xp.evaluate(xpath, dom, XPathConstants.NODE);
        } catch (NullPointerException e) {
            //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (XPathExpressionException e) {
            //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return retval;
    }

    public static String xpathGetNodeValue(Node dom, String xpath) {
        String retval = null;

        try {
            XPath xp = XPathFactory.newInstance().newXPath();
            retval = (String) xp.evaluate(xpath, dom, XPathConstants.STRING);
        } catch (NullPointerException e) {
            //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (XPathExpressionException e) {
            //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return retval;
    }
}
