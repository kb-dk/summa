/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.common.util;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.text.ParseException;
import java.io.StringWriter;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * convenience methods for parsing using XPath.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ParseUtil {
    private static final Log log = LogFactory.getLog(ParseUtil.class);
    public static final String XML_HEADER =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    /**
     * Wrapper for {@link #getValue} that uses Floats.
     * @param xPath        an XPath instance (a new instance can be created with
     *                     XPathFactory.newInstance().newXPath()). The instance
     *                     should be reused for speed, but is not thread-safe!
     * @param node         the node with the wanted attribute.
     * @param path         the path to extract.
     * @param defaultValue the default value.
     * @return             the value of the path, if existing, else
     *                     defaultValue.
     * @throws ParseException if there was an error parsing.
     */
    public static Float getValue(
            XPath xPath, Node node, String path, Float defaultValue)
                                                         throws ParseException {
        String sVal = getValue(xPath, node, path, (String)null);
        try {
            return sVal == null ? defaultValue : Float.valueOf(sVal);
        } catch (NumberFormatException e) {
            log.warn("Expected a float for path '" + path
                     + "' but got '" + sVal + "'");
            return defaultValue;
        }
    }

    /**
     * Wrapper for {@link #getValue} that uses Booleans.
     * @param xPath        an XPath instance (a new instance can be created with
     *                     XPathFactory.newInstance().newXPath()). The instance
     *                     should be reused for speed, but is not thread-safe!
     * @param node         the node with the wanted attribute.
     * @param path         the path to extract.
     * @param defaultValue the default value.
     * @return             the value of the path, if existing, else
     *                     defaultValue.
     * @throws ParseException if there was an error parsing.
     */
    public static Boolean getValue(
            XPath xPath, Node node, String path, Boolean defaultValue)
                                                         throws ParseException {
        return Boolean.valueOf(getValue(
                xPath, node, path, Boolean.toString(defaultValue)));

    }

    /**
     * Extract the given value from the node as a String. If the value cannot be
     * extracted, defaultValue is returned.
     * </p><p>
     * Example: To get the value of the attribute "foo" in the node, specify
     *          "@foo" as the path.
     * </p><p>
     * Note: This method does not handle namespaces explicitely.
     * @param xPath        an XPath instance (a new instance can be created with
     *                     XPathFactory.newInstance().newXPath()). The instance
     *                     should be reused for speed, but is not thread-safe!
     * @param node         the node with the wanted attribute.
     * @param path         the path to extract.
     * @param defaultValue the default value.
     * @return             the value of the path, if existing, else
     *                     defaultValue.
     * @throws ParseException if there was an error parsing.
     */
    public static String getValue(
            XPath xPath, Node node, String path, String defaultValue)
                                                         throws ParseException {
        if (log.isTraceEnabled()) {
            log.trace("getSingleValue: Extracting path '" + path + "'");
        }
        String nodeValue;
        try {
            if (!((Boolean) xPath.evaluate(path, node,
                                           XPathConstants.BOOLEAN))) {
                //noinspection DuplicateStringLiteralInspection
                log.trace("No value defined for path '" + path
                          + "'. Returning default value '"
                          + defaultValue + "'");
                return defaultValue;
        }
            nodeValue = xPath.evaluate(path, node);
        } catch (XPathExpressionException e) {
            throw (ParseException) new ParseException(String.format(
                    "Invalid expression '%s'", path), -1).initCause(e);
        }
        if (nodeValue == null) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("Got null value from expression '" + path
                      + "'. Returning default value '" + defaultValue
                      + "'");
            return defaultValue;
        }
        log.trace("Got value '" + nodeValue + "' from expression '" 
                  + path + "'");
        return nodeValue;
    }

    /**
     * Extracts all textual and CDATA content from the given node and its
     * children.
     * @param node the node to get the content from.
     * @return the textual content of node.
     */
    public static String getElementNodeValue(Node node) {
        StringWriter sw = new StringWriter(2000);
         if (node.getNodeType() == Node.ELEMENT_NODE) {
             NodeList all = node.getChildNodes();
             for (int i = 0; i < all.getLength(); i++) {
                 if (all.item(i).getNodeType() == Node.TEXT_NODE ||
                     all.item(i).getNodeType() == Node.CDATA_SECTION_NODE) {
                     // TODO: Check if we exceed the limit for getNodeValue
                     sw.append(all.item(i).getNodeValue());
                 }
             }
         }
         return sw.toString();
     }

    /**
     * Performs a simple entity-encoding of input, maing it safe to include in
     * XML.
     * @param input the text to encode.
     * @return the text with &, ", < and > encoded.
     */
    public static String encode(String input) {
        input = input.replaceAll("&", "&amp;");
        input = input.replaceAll("\"", "&quot;");
        input = input.replaceAll("<", "&lt;");
        return input.replaceAll(">", "&gt;");
    }

}



