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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.stream.events.XMLEvent;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Locale;

/**
 * convenience methods for parsing using XPath.
 * @deprecated use {@link dk.statsbiblioteket.util.xml.DOM} and
 * {@link dk.statsbiblioteket.util.xml.XMLUtil} instead.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
@Deprecated
public class ParseUtil {
    private static final Log log = LogFactory.getLog(ParseUtil.class);
    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

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
    public static Float getValue(XPath xPath, Node node, String path, Float defaultValue) throws ParseException {
        String sVal = getValue(xPath, node, path, (String)null);
        try {
            return sVal == null ? defaultValue : Float.valueOf(sVal);
        } catch (NumberFormatException e) {
            log.warn("Expected a float for path '" + path + "' but got '" + sVal + "'");
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
    public static Boolean getValue(XPath xPath, Node node, String path, Boolean defaultValue) throws ParseException {
        return Boolean.valueOf(getValue(xPath, node, path, Boolean.toString(defaultValue)));

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
    public static String getValue(XPath xPath, Node node, String path, String defaultValue) throws ParseException {
        if (log.isTraceEnabled()) {
            log.trace("getSingleValue: Extracting path '" + path + "'");
        }
        String nodeValue;
        try {
            if (!((Boolean) xPath.evaluate(path, node,
                                           XPathConstants.BOOLEAN))) {
                //noinspection DuplicateStringLiteralInspection
                log.trace("No value defined for path '" + path + "'. Returning default value '" + defaultValue + "'");
                return defaultValue;
        }
            nodeValue = xPath.evaluate(path, node);
        } catch (XPathExpressionException e) {
            throw (ParseException) new ParseException(String.format(
                    Locale.ROOT, "Invalid expression '%s'", path), -1).initCause(e);
        }
        if (nodeValue == null) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("Got null value from expression '" + path + "'. Returning default value '" + defaultValue + "'");
            return defaultValue;
        }
        log.trace("Got value '" + nodeValue + "' from expression '" + path + "'");
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
                 if (all.item(i).getNodeType() == Node.TEXT_NODE
                     || all.item(i).getNodeType() == Node.CDATA_SECTION_NODE) {
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
            return input.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
        }

        /**
         * Converts an XMLEvent-id to String. Used for primarily debugging and error
         * messages.
         * @param eventType the XMLEvent-id.
         * @return the event as human redable String.
         */
        public static String eventID2String(int eventType) {
            switch (eventType) {
                case XMLEvent.START_ELEMENT:  return "START_ELEMENT";
                case XMLEvent.END_ELEMENT:    return "END_ELEMENT";
                case XMLEvent.PROCESSING_INSTRUCTION: return "PROCESSING_INSTRUCTION";
                case XMLEvent.CHARACTERS: return "CHARACTERS";
                case XMLEvent.COMMENT: return "COMMENT";
                case XMLEvent.START_DOCUMENT: return "START_DOCUMENT";
                case XMLEvent.END_DOCUMENT: return "END_DOCUMENT";
                case XMLEvent.ENTITY_REFERENCE: return "ENTITY_REFERENCE";
                case XMLEvent.ATTRIBUTE: return "ATTRIBUTE";
                case XMLEvent.DTD: return "DTD";
                case XMLEvent.CDATA: return "CDATA";
                case XMLEvent.SPACE: return "SPACE";
                default: return "UNKNOWN_EVENT_TYPE ," + eventType;
            }
        }
}
