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

import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.commons.io.input.CharSequenceReader;

import javax.xml.stream.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for stream oriented processing of XML.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class XMLStepper {
    /**
     * Iterates through the start tags in the stream until the current sub tree in the DOM is depleted
     * Leaves the cursor after END_ELEMENT.
     * @param xml the stream to iterate.
     * @param callback called for each start element.
     */
    public static void iterateTags(XMLStreamReader xml, Callback callback) throws XMLStreamException {
        List<String> tagStack = new ArrayList<String>(10);
        while (true) {
            if (xml.getEventType() == XMLStreamReader.START_ELEMENT) {
                String currentTag = xml.getLocalName();
                tagStack.add(currentTag);
                if (!callback.elementStart(xml, tagStack, currentTag)) {
                    xml.next();
                }
                continue;
            }
            if (xml.getEventType() == XMLStreamReader.END_ELEMENT) {
                String currentTag = xml.getLocalName();
                if (tagStack.isEmpty()) {
                    callback.end();
                    return;
                }
                if (!currentTag.equals(tagStack.get(tagStack.size()-1))) {
                    throw new IllegalStateException(String.format(
                            "Encountered end tag '%s' where '%s' from the stack %s were expected",
                            currentTag, tagStack.get(tagStack.size()-1), Strings.join(tagStack, ", ")));
                }
                callback.elementEnd(tagStack.remove(tagStack.size()-1));
            } else if (xml.getEventType() == XMLStreamReader.END_DOCUMENT) {
                callback.end();
                return;
            }
            xml.next();
        }
    }

    /**
     * Traverses all parts in the given element, including sub elements etc., and pipes the parts to out.
     * Used for copying snippets verbatim from one XML structure to another.
     * Leaves in positioned immediately after the END_ELEMENT matching the START_ELEMENT.
     * </p><p>
     * Note: The piper does not repair namespaces. If in uses namespaces defined previously in the XML and out does
     * not have these definitions, they will not be transferred.
     * @param in must be positioned at START_ELEMENT and be coalescing.
     * @param out the destination for the traversed XML.
     * @param failOnError if true, unrecognized elements will result in an UnsupportedOperationException.
     *                    if false, unrecognized elements will be ignored.
     * @throws XMLStreamException if in was faulty.
     */
    public static void pipeXML(XMLStreamReader in, XMLStreamWriter out, boolean failOnError) throws XMLStreamException {
        if (in.getProperty(XMLInputFactory.IS_COALESCING) == null ||
            Boolean.TRUE != in.getProperty(XMLInputFactory.IS_COALESCING)) {
            throw new IllegalArgumentException("The XMLInputStream must be coalescing but was not");
        }
        pipeXML(in, out, 0, failOnError);
    }
    private static void pipeXML(XMLStreamReader in, XMLStreamWriter out, final int level, boolean ignoreErrors)
            throws XMLStreamException {
        if (XMLStreamReader.START_ELEMENT != in.getEventType()) {
            throw new IllegalStateException(
                    "Must be positioned at START_ELEMENT but was at " + XMLUtil.eventID2String(in.getEventType()));
        }
        // TODO: Add better namespace support by matching NameSpaceContexts for in and out

        out.writeStartElement(in.getPrefix(), in.getLocalName(), in.getNamespaceURI());
        for (int i = 0 ; i < in.getNamespaceCount() ; i++) {
            out.writeNamespace(in.getNamespacePrefix(i), in.getNamespaceURI(i));
        }
        for (int i = 0 ; i < in.getAttributeCount() ; i++) {
            out.writeAttribute(in.getAttributeNamespace(i), in.getAttributeLocalName(i), in.getAttributeValue(i));
        }
        out:
        while (in.getEventType() != XMLStreamConstants.END_DOCUMENT) {
            in.next();
            switch (in.getEventType()) {
                case XMLStreamReader.END_DOCUMENT: {
                    out.writeEndDocument();
                    break;
                }
                case XMLStreamReader.START_ELEMENT: {
                    pipeXML(in, out, level + 1, ignoreErrors);
                    break;
                }
                case XMLStreamReader.END_ELEMENT: {
                    out.writeEndElement();
                    break out;
                }
                case XMLStreamReader.SPACE:
                case XMLStreamReader.CHARACTERS: {
                    out.writeCharacters(in.getText());
                    break;
                }
                case XMLStreamReader.CDATA: {
                    out.writeCData(in.getText());
                    break;
                }
                case XMLStreamReader.COMMENT: {
                    out.writeComment(in.getText());
                    break;
                }
                default: if (!ignoreErrors) {
                    throw new UnsupportedOperationException(
                            "pipeXML does not support event type " + XMLUtil.eventID2String(in.getEventType()));
                }
            }
        }
        if (level == 0) {
            if (in.getEventType() == XMLStreamConstants.END_ELEMENT) {
                in.next();
            }
            out.flush();
        }
    }

    private static final XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    /**
     * Steps through the provided XML and returns the text content of the first element with the given tag.
     * @param xml the XML to extract text from.
     * @param tag the designation of the element to extract text from.
     * @return the text of the element with the given tag or null if the tag could not be found. If the tag is empty,
     *         the empty String will be returned.
     */
    public static String getFirstElementText(CharSequence xml, String tag) throws XMLStreamException {
        final Pair<Boolean, String> result = new Pair<Boolean, String>(false, null);
        XMLStepper.iterateElements(xmlFactory.createXMLStreamReader(new CharSequenceReader(xml)),
                                   "", tag, new XMLStepper.XMLCallback() {
            @Override
            public void execute(XMLStreamReader xml) throws XMLStreamException {
                if (result.getKey().equals(false)) { // We only want the first one
                    result.setKey(true);
                    result.setValue(xml.getElementText());
                }
            }
        });
        return result.getValue();
    }

    /**
     * Skips everything until a start tag is reacted or the readers is depleted.
     * @param xml the stream to iterate over.
     * @return the name of the start tag or null if EOD.
     * @throws javax.xml.stream.XMLStreamException if there was an error
     * accessing the xml stream.
     */
    public static String jumpToNextTagStart(XMLStreamReader xml)
            throws XMLStreamException {
        if (xml.getEventType() == XMLStreamReader.START_ELEMENT) {
            xml.next(); // Skip if already located at a start
        }
        while (true) {
            if (xml.getEventType() == XMLStreamReader.START_ELEMENT) {
                return xml.getLocalName();
            }
            if (xml.getEventType() == XMLStreamReader.END_DOCUMENT) {
                return null;
            }
            xml.next();
        }
    }

    /**
     *
     * @param xml stream positioned at a start tag.
     * @param attributeName the wanted attribute
     * @param defaultValue the value to return if the attributes is not present.
     * @return the attribute content og the default value.
     */
    public static String getAttribute(XMLStreamReader xml, String attributeName, String defaultValue) {
        for (int i = 0 ; i < xml.getAttributeCount() ; i++) {
            if (xml.getAttributeLocalName(i).equals(attributeName)) {
                return xml.getAttributeValue(i);
            }
        }
        return defaultValue;
    }

    /**
     * Iterates over the xml until a start tag with startTagName is reached.
     * @param xml          the stream to iterate over.
     * @param startTagName the name of the tag to locate.
     * @return true if the tag was found, else false.
     * @throws javax.xml.stream.XMLStreamException if there were an error
     * seeking the xml stream.
     */
    public static boolean findTagStart(XMLStreamReader xml, String startTagName) throws XMLStreamException {
        while (true)  {
            if (xml.getEventType() == XMLStreamReader.START_ELEMENT && startTagName.equals(xml.getLocalName())) {
                return true;
            }
            if (xml.getEventType() == XMLStreamReader.END_DOCUMENT) {
                return false;
            }
            try {
                xml.next();
            } catch (XMLStreamException e) {
                throw new XMLStreamException("Error seeking to start tag for element '" + startTagName + "'", e);
            }
        }
    }

    public static boolean findTagEnd(XMLStreamReader xml, String endTagName) throws XMLStreamException {
        while (true)  {
            if (xml.getEventType() == XMLStreamReader.END_ELEMENT && endTagName.equals(xml.getLocalName())) {
                return true;
            }
            if (xml.getEventType() == XMLStreamReader.END_DOCUMENT) {
                return false;
            }
            try {
                xml.next();
            } catch (XMLStreamException e) {
                throw new XMLStreamException("Error seeking to end tag for element '" + endTagName + "'", e);
            }
        }
    }

    /**
     * Iterates over elements in the stream until end element is encountered
     * or end of document is reached. For each element matching actionElement,
     * callback is called.
     * @param xml        the stream to iterate.
     * @param endElement the stopping element.
     * @param actionElement callback is activated when encountering elements
     *                   with this name.
     * @param callback   called for each encountered element.
     * @throws javax.xml.stream.XMLStreamException if the stream could not
     * be iterated or an error occured during callback.
     */
    public static void iterateElements(
            XMLStreamReader xml, String endElement, String actionElement, XMLCallback callback) throws XMLStreamException {
        while (true) {
            if (xml.getEventType() == XMLStreamReader.END_DOCUMENT ||
                (xml.getEventType() == XMLStreamReader.END_ELEMENT && xml.getLocalName().equals(endElement))) {
                break;
            }
            if (xml.getEventType() == XMLStreamReader.START_ELEMENT && xml.getLocalName().equals(actionElement)) {
                callback.execute(xml);
            }
            xml.next();
        }
    }

    public static void skipSubTree(XMLStreamReader xml) throws XMLStreamException {
        iterateTags(xml, new Callback() {
            @Override
            public boolean elementStart(
                    XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                return false; // Ignore everything until end of sub tree
            }
        });
    }

    public abstract static class Callback {
        /**
         * Called for each encountered START_ELEMENT in the part of the xml that is within scope. If the implementation
         * calls {@code xml.next()} or otherwise advances the position in the stream, it must ensure that the list of
         * tags is consistent with the position in the DOM.
         *
         * @param xml        the Stream.
         * @param tags       the start tags encountered in the current sub tree.
         * @param current    the local name of the current tag.
         * @return true if the implementation called {@code xml.next()} one or more times, else false.
         */
        // TODO: Can we avoid the return value by using Location?
        public abstract boolean elementStart(
                XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException;

        /**
         * Called for each encountered ELEMENT_END in the part of the XML that is within scope.
         * @param element the name of the element that ends.
         */
        @SuppressWarnings("UnusedParameters")
        public void elementEnd(String element) { }

        /**
         * Called when the last END_ELEMENT is encountered.
         */
        public void end() { }

    }

    public abstract static class XMLCallback {
        public abstract void execute(XMLStreamReader xml) throws XMLStreamException;
        public void close() { } // Called when iteration has finished
    }
}
