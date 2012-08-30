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
package dk.statsbiblioteket.summa.common.marc;

import dk.statsbiblioteket.summa.common.xml.XMLStepper;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Iterates through MARC21Slim XML and makes callbacks for all parts from the standard. Comments and unexpected elements
 * are ignored. This class uses the XMLStream framework, which means that it is fast and with low memory overhead,
 * compared to a full DOM build.
 * </p><p>
 * This class is Thread safe.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MARCStepper {
    private static Log log = LogFactory.getLog(MARCStepper.class);

    private XMLInputFactory inputFactory;

    public MARCStepper() {
        inputFactory = XMLInputFactory.newInstance();
    }

    /**
     * Parse through the provided MARC21Slim XML and performs callbacks for the different elements.
     * @param source MARC21Slim XML. Closed after processing.
     * @return the number of records processed fron the source.
     * @throws XMLStreamException if non-valid XML is encountered.
     * @throws ParseException upon unexpected elements.
     * @throws IOException if the source could not be read or closed.
     */
    public int parse(
        InputStream source, final MarcCallback callback) throws XMLStreamException, ParseException, IOException {
        XMLStreamReader xml = inputFactory.createXMLStreamReader(source, "utf-8");
        // Positioned at startDocument
        int eventType = xml.getEventType();
        if (eventType != XMLEvent.START_DOCUMENT) {
            throw new ParseException(String.format("The first element should be start, it was %s",
                                                   XMLUtil.eventID2String(eventType)), 0);
        }
        final AtomicInteger processed = new AtomicInteger(0);
        XMLStepper.iterateTags(xml, new XMLStepper.Callback() {
            @Override
            public boolean elementStart(XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                if (MARC.TAG_RECORD.equals(current)) {
                    try {
                        processInRecord(xml, callback);
                    } catch (InterruptedException e) {
                        throw new XMLStreamException("Interrupted while processing", e);
                    }
                    processed.incrementAndGet();
                    return true;
                }
                // We probably encountered a <collection>, but we just skip past that as it does not have attributes
                return false;
            }
        });
        source.close();
        return processed.get();
    }

    /**
     * Collect record information and ultimately build a Record and add it to
     * the queue before returning.
     * @param xml a reader positioned at the start of a record.
     * @throws XMLStreamException   if a parse error occured.
     * @throws InterruptedException if the process was interrupted while adding
     *                              to the queue.
     */
    private void processInRecord(XMLStreamReader xml, MarcCallback callback)
        throws XMLStreamException, InterruptedException {
        callback.startRecord(XMLStepper.getAttribute(xml, MARC.ATTRIBUTE_ID, null),
                             XMLStepper.getAttribute(xml, MARC.TAG_RECORD_ATTRIBUTE_TYPE, null));

        boolean encounteredUnexpectedStart = false;
        while (callback.running() && xml.hasNext()) {
            int eventType = xml.next();
            if (eventType == XMLEvent.END_ELEMENT && MARC.TAG_RECORD.equals(xml.getLocalName())) {
                break;
            }

            switch(eventType) {
                case XMLEvent.START_ELEMENT :
                    if (MARC.TAG_DATAFIELD.equals(xml.getLocalName())) {
                        processDataField(xml, callback);
                    } else if (MARC.TAG_CONTROLFIELD.equals(xml.getLocalName())) {
                        processControlField(
                            xml, XMLStepper.getAttribute(xml, MARC.ATTRIBUTE_ID, null), callback);
                    } else if (MARC.TAG_LEADER.equals(xml.getLocalName())) {
                        callback.leader(
                            XMLStepper.getAttribute(xml, MARC.ATTRIBUTE_ID, null), xml.getElementText());
                    } else {
                        if (!encounteredUnexpectedStart) {
                            encounteredUnexpectedStart = true;
                            log.warn(String.format(
                                    "Unexpected start-tag '%s' while parsing MARC. This is the first time this"
                                    + " has been encountered. Further encounters will be logged on debug",
                                    xml.getLocalName()));
                        } else {
                            log.debug("Unexpected start-tag '" + xml.getLocalName() + "' while parsing MARC");
                        }
                    }
                    break;
                case XMLEvent.END_ELEMENT :
                    log.warn(String.format("Unexpected end-tag '%s' while parsing MARC", xml.getLocalName()));
                    break;
                case XMLEvent.CHARACTERS :
                    if (!isBlank(xml.getText())) {
                        log.warn(String.format("Unexpected text '%s' while parsing MARC", xml.getText()));
                    }
                    callback.strayCharacters(xml.getText());
                    break;
                case XMLEvent.COMMENT:
                    callback.comment(xml.getText());
                    break;
                default:
                    log.warn(String.format("Unexpended event %s", XMLUtil.eventID2String(eventType)));
            }
        }
        if (!callback.running()) {
            log.debug("processInRecord stopped as running was false");
        } else {
            callback.endRecord();
        }
    }

    private void processDataField(XMLStreamReader reader, final MarcCallback callback) throws XMLStreamException {
        log.trace("Reached datafield start-tag");
        final String tag = XMLStepper.getAttribute(reader, MARC.TAG_DATAFIELD_ATTRIBUTE_TAG, null);
        final String id = XMLStepper.getAttribute(reader, MARC.ATTRIBUTE_ID, null);
        final String ind1 = XMLStepper.getAttribute(reader, MARC.TAG_DATAFIELD_ATTRIBUTE_IND1, null);
        final String ind2 = XMLStepper.getAttribute(reader, MARC.TAG_DATAFIELD_ATTRIBUTE_IND2, null);
        callback.startDataField(tag, id, ind1, ind2);

        XMLStepper.iterateElements(reader, MARC.TAG_DATAFIELD, MARC.TAG_SUBFIELD, new XMLStepper.XMLCallback() {
            @Override
            public void execute(XMLStreamReader xml) throws XMLStreamException {
                String code = XMLStepper.getAttribute(xml, MARC.TAG_SUBFIELD_ATTRIBUTE_CODE, null);
                String content = xml.getElementText();
                callback.subField(tag, id, code, content);
            }
        });
        callback.endDataField();
    }

    // NOTE: id is optional
    private void processControlField(XMLStreamReader xml, String id, MarcCallback callback) throws XMLStreamException {
        String tag = XMLStepper.getAttribute(xml, MARC.TAG_CONTROLFIELD_ATTRIBUTE_TAG, null);
        String tid= XMLStepper.getAttribute(xml, MARC.ATTRIBUTE_ID, null);
        String content = xml.getElementText();
        callback.controlField(tag, id, content);
    }


    public abstract static class MarcCallback {
        /**
         * @return true if processing of the input is to continue, else false. Used for premature halting.
         */
        public boolean running() {
            return true;
        }

        public void strayCharacters(String text) { }

        public void comment(String text) { }

        // Note: id & type are optional
        public void startRecord(String id, String type) { }

        /* {@code <leader>.....cmm  22.....0  45032</leader>}.*/
        // Note: id is optional
        public void leader(String id, String content) { }

        public void controlField(String tag, String id, String content) { }

        public void startDataField(String tag, String id, String ind1, String ind2) { }

        public void subField(String fieldTag, String fieldId, String subCode, String subContent) { }

        public void endDataField() { }

        public void endRecord() { }
    }

    private static final Pattern BLANKS = Pattern.compile("( |\n|\t)*");
    /**
     * Test whether text is made up of ignorable blanks, which translates to line-breaks, space and tab.
     * @param text the String to analyze for blanks.
     * @return true if the text consists solely of blanks.
     */
    protected static boolean isBlank(String text) {
        return BLANKS.matcher(text).matches();
    }
}
