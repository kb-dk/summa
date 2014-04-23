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
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for stream oriented processing of XML.
 * @deprecated this class has been moved to sbutil and is to be removed from summa.
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

    private static final XMLOutputFactory xmlOutFactory = XMLOutputFactory.newInstance();

    /**
     * Equivalent to {@link #pipeXML(javax.xml.stream.XMLStreamReader, javax.xml.stream.XMLStreamWriter, boolean)} but
     * returns the sub XML as a String instead of piping the result. For performance, it is recommended to
     * @param in must be positioned at START_ELEMENT and be coalescing.
     * @param failOnError if true, unrecognized elements will result in an UnsupportedOperationException.
     *                    if false, unrecognized elements will be ignored.
     * @return the sub XML as a String.
     * @throws XMLStreamException if in was faulty.
     */
    public static String getSubXML(XMLStreamReader in, boolean failOnError) throws XMLStreamException {
        return getSubXML(in, failOnError, false);
/*        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter out = xmlOutFactory.createXMLStreamWriter(os);
        pipeXML(in, out, failOnError);
        return os.toString();*/
    }

    /**
     * Equivalent to
     * {@link #pipeXML(javax.xml.stream.XMLStreamReader, javax.xml.stream.XMLStreamWriter, boolean, boolean)} but
     * returns the sub XML as a String instead of piping the result.
     * </p><p>
     * Note: This methods is resilient against the multiple root-problem in pipeXML. This also means that the returned
     *       String is not necessarily valid XML.
     * @param in must be positioned at START_ELEMENT and be coalescing.
     * @param failOnError if true, unrecognized elements will result in an UnsupportedOperationException.
     *                    if false, unrecognized elements will be ignored.
     * @return the sub XML as a String.
     * @param onlyInner  if true, the start- and end-tag of the current element are not piped to out.
     * @throws XMLStreamException if in was faulty.
     */
    public static String getSubXML(XMLStreamReader in, boolean failOnError, boolean onlyInner)
            throws XMLStreamException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter out = xmlOutFactory.createXMLStreamWriter(os);
        out.writeStartElement("a");
        pipeXML(in, out, failOnError, onlyInner);
        out.flush();
        String xml = os.toString();
        // TODO: How can this ever be less than 3? A search for 'foo' against summon has this problem in the test gui
        return xml.length() < 3 ? "" : xml.substring(3); // We remove the start <a> from the String
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
        pipeXML(in, out, failOnError, false, null);
    }
    /**
     * Traverses all parts in the given element, including sub elements etc., and pipes the parts to out.
     * Used for copying snippets verbatim from one XML structure to another.
     * Leaves in positioned immediately after the END_ELEMENT matching the START_ELEMENT.
     * </p><p>
     * Note: The piper does not repair namespaces. If in uses namespaces defined previously in the XML and out does
     * not have these definitions, they will not be transferred.
     * </p><p>
     * Warning: Skipping the outer element is dangerous as the outer element can contain multiple inner elements.
     * If the destination (out) is empty and in contains multiple sub-elements, the piping will fail with an Exception
     * stating "Trying to output second root". In order to avoid that, the destination needs to have at least one
     * open element.
     * @param in must be positioned at START_ELEMENT and be coalescing.
     * @param out the destination for the traversed XML.
     * @param failOnError if true, unrecognized elements will result in an UnsupportedOperationException.
     *                    if false, unrecognized elements will be ignored.
     * @param onlyInner  if true, the start- and end-tag of the current element are not piped to out.
     * @throws XMLStreamException if in was faulty.
     */
    public static void pipeXML(XMLStreamReader in, XMLStreamWriter out, boolean failOnError, boolean onlyInner)
            throws XMLStreamException {
        pipeXML(in, out, failOnError, onlyInner, null);
    }

    private static boolean pipeXML(XMLStreamReader in, XMLStreamWriter out, boolean ignoreErrors, boolean onlyInner,
                                   Callback callback) throws XMLStreamException {
        if (in.getProperty(XMLInputFactory.IS_COALESCING) == null ||
            Boolean.TRUE != in.getProperty(XMLInputFactory.IS_COALESCING)) {
            throw new IllegalArgumentException("The XMLInputStream must be coalescing but was not");
        }
        if (!ignoreErrors) {
            return pipeXML(in, out, false, onlyInner, new ArrayList<String>(), callback);
        }
        try {
            return pipeXML(in, out, true, onlyInner, new ArrayList<String>(), callback);
        } catch (XMLStreamException e) {
            // Ignoring exception as ignoreErrors == true
            out.flush();
        }
        return true;
    }
    /**
     * @param in           XML, optionally positioned inside the stream.
     * @param out          the piped content will be send to this.
     * @param ignoreErrors if true, unknown element types will be ignored. If false, an exception will be thrown.
     * @param onlyInner    if true, in must be positioned at an elementStart, which will be skipped.
     * @param elementStack nested elements encountered so far.
     * @param callback     will be called for all elementStarts. If {@link Callback#elementStart} returns true,
     *                     piping will assume that the element has been processed and will skip it. In that case it is
     *                     the responsibility of the callback to leave {@code in} at the END_ELEMENT corresponding to
     *                     the START_ELEMENT.
     * @return true if piping has finished.
     * @throws XMLStreamException if processing failed due to an XML problem.
     */
    private static boolean pipeXML(XMLStreamReader in, XMLStreamWriter out, boolean ignoreErrors, boolean onlyInner,
                                   List<String> elementStack, Callback callback) throws XMLStreamException {
        if (onlyInner) {
            if (XMLStreamReader.START_ELEMENT != in.getEventType()) {
                throw new IllegalStateException(
                        "onlyInner == true, but the input was not positioned at START_ELEMENT. Current element is "
                        + XMLUtil.eventID2String(in.getEventType()));
            }
            in.next();
        }

        // TODO: Add better namespace support by matching NameSpaceContexts for in and out

        while (true) {
            switch (in.getEventType()) {
                case XMLStreamReader.START_DOCUMENT: {
                    if (in.getEncoding() != null || in.getVersion() != null) {
                        // Only write a declaration if the source has one
                        out.writeStartDocument(in.getEncoding(), in.getVersion());
                    }
                    break;
                }
                case XMLStreamReader.END_DOCUMENT: {
                    out.writeEndDocument();
                    out.flush();
                    return true;
                }

                case XMLStreamReader.START_ELEMENT: {
                    String element = in.getLocalName();
                    elementStack.add(element);
                    if (callback == null || !callback.elementStart(in, elementStack, element)) {
                        out.writeStartElement(in.getPrefix(), in.getLocalName(), in.getNamespaceURI());
                        for (int i = 0 ; i < in.getNamespaceCount() ; i++) {
                            out.writeNamespace(in.getNamespacePrefix(i), in.getNamespaceURI(i));
                        }
                        for (int i = 0 ; i < in.getAttributeCount() ; i++) {
                            if (in.getAttributeNamespace(i) == null) {
                                out.writeAttribute(in.getAttributeLocalName(i), in.getAttributeValue(i));
                            } else {
                                out.writeAttribute(in.getAttributeNamespace(i), in.getAttributeLocalName(i),
                                                   in.getAttributeValue(i));
                            }
                        }
                        in.next();
                        if (pipeXML(in, out, ignoreErrors, false, elementStack, callback)) {
                            out.flush();
                            return true;
                        }
                    } else { // callback handled the element so we do not pipe the END_ELEMENT
                        if (XMLStreamReader.END_ELEMENT != in.getEventType()) {
                            throw new IllegalStateException(String.format(
                                    "Callback for %s returned true, but did not position the XML stream at "
                                    + "END_ELEMENT. Current eventType is %s",
                                    Strings.join(elementStack, ", "), XMLUtil.eventID2String(in.getEventType())));
                        }
                        elementStack.remove(elementStack.size()-1);
                    }
                    break;
                }
                case XMLStreamReader.END_ELEMENT: {
                    if (elementStack.isEmpty()) {
                        if (callback != null) {
                            callback.end();
                        }
                        out.flush();
                        return true;
                    }
                    String element = in.getLocalName();
                    if (!element.equals(elementStack.get(elementStack.size()-1))) {
                        throw new IllegalStateException(String.format(
                                "Encountered end tag '%s' where '%s' from the stack %s were expected",
                                element, elementStack.get(elementStack.size()-1), Strings.join(elementStack, ", ")));
                    }
                    String popped = elementStack.remove(elementStack.size()-1);
                    if (callback != null) {
                        callback.elementEnd(popped);
                    }

                    if (!elementStack.isEmpty() || !onlyInner) {
                        out.writeEndElement();
                    }
                    if (elementStack.isEmpty()) {
                        in.next();
                    }
                    return elementStack.isEmpty();
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
            in.next();
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
     * Iterates over elements in the stream until end element is encountered or end of document is reached.
     * For each element matching actionElement, callback is called.
     * @param xml        the stream to iterate.
     * @param endElement the stopping element.
     * @param actionElement callback is activated when encountering elements with this name.
     * @param callback   called for each encountered element.
     * @throws javax.xml.stream.XMLStreamException if the stream could not be iterated or an error occurred during
     * callback.
     */
    public static void iterateElements(XMLStreamReader xml, String endElement, String actionElement,
                                       XMLCallback callback) throws XMLStreamException {
        iterateElements(xml, endElement, actionElement, true, callback);
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

    /**
     * Iterates over elements in the stream until end element is encountered
     * or end of document is reached. For each element matching actionElement,
     * callback is called.
     *
     * @param xml           the stream to iterate.
     * @param endElement    the stopping element.
     * @param actionElement callback is activated when encountering elements with this name.
     * @param advanceOnHit  if true, the iterator always calls {@code xml.next()}. If false, next is only called if
     *                      no callback has been issued.
     * @param callback   called for each encountered element.
     * @throws javax.xml.stream.XMLStreamException if the stream could not
     * be iterated or an error occured during callback.
     */
    public static void iterateElements(XMLStreamReader xml, String endElement, String actionElement,
                                       boolean advanceOnHit, XMLCallback callback) throws XMLStreamException {
        while (true) {
            if (xml.getEventType() == XMLStreamReader.END_DOCUMENT ||
                (xml.getEventType() == XMLStreamReader.END_ELEMENT && xml.getLocalName().equals(endElement))) {
                break;
            }
            if (xml.getEventType() == XMLStreamReader.START_ELEMENT && xml.getLocalName().equals(actionElement)) {
                callback.execute(xml);
                if (advanceOnHit) {
                    xml.next();
                }
            } else {
                xml.next();
            }
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

    public static void skipElement(XMLStreamReader xml) throws XMLStreamException {
        if (XMLStreamReader.START_ELEMENT != xml.getEventType()) {
            throw new IllegalStateException("The reader must be positioned at START_ELEMENT but was positioned at "
                                            + XMLUtil.eventID2String(xml.getEventType()));
        }
        xml.next();
        iterateTags(xml, new Callback() {
            @Override
            public boolean elementStart(
                    XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                return false; // Ignore everything until end of sub tree
            }
        });
    }

    /**
     * Iterates the given input, counting occurrences of limit-matches and skipping matching elements when the limits
     * are reached.
     * </p><p>
     * Every tag and every attribute (optional) is matched against the limits. Tags are represented as
     * {@code /rootelement/subelement}, attributes as {@code /rootelement/subelement#attributename=value}.
     * Namespaces are not part of the representation.
     * </p><p>
     * Sample: in={@code <foo><bar zoo="true"></bar><bar zoo="true"></bar><bar zoo="false"></bar><baz /></foo>}<br/>
     * Limits {@code "/foo/bar", 1} -> {@code <foo><bar zoo="true"></bar><baz /></foo>}<br/>
     * Limits {@code "bar", 1} -> {@code <foo><bar zoo="true"></bar><baz /></foo>}<br/>
     * Limits {@code "/foo/bar", 2} -> {@code <foo><bar zoo="true"></bar><bar zoo="true"></bar><baz /></foo>}<br/>
     * Limits {@code "/foo/bar", 0} -> {@code <foo><baz></baz></foo>}<br/>
     * Limits {@code "/foo/bar#zoo=true", 1} -> {@code <foo><bar zoo="true"></bar><bar zoo="false"></bar><baz /></foo>}
     * </p><p>
     * Example: limits={@code ["/foo$", -1], ["/foo/bar", 1]}, countPatterns=false, onlyCheckElementPaths=true,
                discardNonMatched=true} -> {@code "<foo><bar zoo=\"true\" /></foo>"}<br/>
     * Example: limits={@code ["/foo$", -1], ["/foo/bar", 1]}, countPatterns=false, onlyCheckElementPaths=true,
                discardNonMatched=false} -> {@code "<foo><bar zoo=\"true\" /><baz /></foo>"}<br/>
     * @param in     XML stream positioned at the point from which reduction should occur (normally the start).
     * @param out    the reduced XML.
     * @param limits patterns and max occurrences for entries. The limits are processed in entrySet order.
     *               If max occurrence is -1 there is no limit for the given pattern.
     * @param countPatterns if true, the limit applies to matched patterns. If false, the limit if for each regexp.
     *                      If the limit is {code ".*", 10}, only 10 elements in total is kept.
     * @param onlyCheckElementPaths if true, only element names are matched, not attributes.
     *                              Setting this to true speeds up processing.
     * @param discardNonMatched if true, paths that are not matched by any limit are discarded.
     * @throws javax.xml.stream.XMLStreamException if there was a problem reading (in) or writing (out) XML.
     */
    public static void limitXML(final XMLStreamReader in, XMLStreamWriter out, final Map<Pattern, Integer> limits,
                                final boolean countPatterns, final boolean onlyCheckElementPaths,
                                final boolean discardNonMatched) throws XMLStreamException {
        final Map<Object, Integer> counters = new HashMap<Object, Integer>();
        pipeXML(in, out, true, false, new Callback() {
            @Override
            public boolean elementStart(
                    XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                Set<RESULT> result = exceeded(counters, xml, tags);
                if (result.contains(RESULT.exceeded) || (discardNonMatched && !result.contains(RESULT.match))) {
                    skipElement(xml);
                    return true;
                }
                return false;
            }

            private Set<RESULT> exceeded(Map<Object, Integer> counters, XMLStreamReader xml, List<String> tags) {
                Set<RESULT> result = EnumSet.noneOf(RESULT.class);
                for (Map.Entry<Pattern, Integer> limit: limits.entrySet()) {
                    final Pattern pattern = limit.getKey();
                    final int max = limit.getValue();

                    final String element = "/" + Strings.join(tags, "/");
                    exceeded(result, counters, pattern, max, element, countPatterns);
                    if (result.contains(RESULT.exceeded)) {
                        return result;
                    }
                    if (!onlyCheckElementPaths) {
                        for (int i = 0 ; i < xml.getAttributeCount() ; i++) {
                            final String merged =
                                    element + "#" + xml.getAttributeLocalName(i) + "=" + xml.getAttributeValue(i);
                            exceeded(result, counters, pattern, max, merged, countPatterns);
                            if (result.contains(RESULT.exceeded)) {
                                return result;
                            }
                        }
                    }
                }
                return result;
            }

            private void exceeded(Set<RESULT> result, Map<Object, Integer> counters, Pattern pattern, int max,
                                  String path, boolean countPatterns) {
                Matcher elementMatch = pattern.matcher(path);
                if (elementMatch.matches()) {
                    result.add(RESULT.match);
                    Integer count = counters.get(countPatterns ? elementMatch.group() : pattern);
                    if (count == null) {
                        count = 0;
                    }
                    counters.put(countPatterns ? elementMatch.group() : pattern, ++count);
                    if (max != -1 && count > max) {
                        result.add(RESULT.exceeded);
                    }
                }
            }

        });
    }
    enum RESULT {match, exceeded}

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
