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

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexField;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.lucene.index.IndexServiceException;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

/**
 * Stream-based converter from SummaDocumentXML to Lucene Documents.
 * The generated Document is added to the Payload's objectData under the key
 * {@link Payload#LUCENE_DOCUMENT}.
 * </p><p>
 * see SummaDocumentSample.xml.
 * </p><p>
 * Note: The DocumentCreator needs an index-description. The setup for
 * retrieving the description must be stored in the sub-property
 * {@link dk.statsbiblioteket.summa.common.index.IndexDescriptor#CONF_DESCRIPTOR} with parameters from
 * {@link dk.statsbiblioteket.summa.common.index.IndexDescriptor}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class StreamingDocumentCreator extends DocumentCreatorBase<org.apache.lucene.document.Document> {
    private static Log log = LogFactory.getLog(StreamingDocumentCreator.class);

    // TODO: Entity-encode fields

    // TODO: Reconsider this namespace
    public static final String SUMMA_NAMESPACE = "http://statsbiblioteket.dk/summa/2008/Document";
//            "http://statsbiblioteket.dk/2008/Index";

    // TODO: Make DocumentCreator support namespace qualified attributes

    private static final String SUMMA_DOCUMENT = "SummaDocument";
    private static final String SUMMA_FIELD = "field";
    private static final String SUMMA_FIELDS = "fields";
    private static final String SUMMA_BOOST = "boost";
    private static final String SUMMA_NAME = "name";

    private XMLInputFactory inputFactory;

    private LuceneIndexDescriptor descriptor;

    /**
     * Initialize the underlying parser and set up internal structures.
     * {@link #setSource(dk.statsbiblioteket.summa.common.filter.Filter)} must
     * be called before further use.
     * @param conf the configuration for the document creator..
     * @throws Configurable.ConfigurationException if there was a problem with
     *         the configuration.
     */
    public StreamingDocumentCreator(Configuration conf) throws ConfigurationException {
        super(conf);
        inputFactory = XMLInputFactory.newInstance();
        // TODO: Check to see if we need to handle CData-events
//        inputFactory.setProperty("report-cdata-event", Boolean.TRUE);
        //inputFactory.setProperty(XMLInputFactory.IS_COALESCING,
        //                         Boolean.TRUE);
        inputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        // No resolving of external DTDs
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);

        descriptor = LuceneIndexUtils.getDescriptor(conf);
        log.info("StreamingDocumentCreator '" + getName() + "' created");
    }

    // TODO: Check whether resolver is used for anything
    @Override
    public Document createState(Payload payload) {
        // TODO: Use a pool of Documents so that they can be reused
        // TODO: Consider moving this to super class
        return new org.apache.lucene.document.Document();
    }

    @Override
    public boolean finish(Payload payload, org.apache.lucene.document.Document state, boolean success)
                                                                                               throws PayloadException {
        if (!success) {
            Logging.logProcess("StreamingDocumentCreator", "Unable to create Lucene document",
                               Logging.LogLevel.DEBUG, payload);
            throw new PayloadException("Unable to create Lucene document", payload);
        }
        payload.getObjectData().put(Payload.LUCENE_DOCUMENT, state);
        Logging.logProcess("StreamingDocumentCreator", "Added Lucene document as meta " + Payload.LUCENE_DOCUMENT,
                           Logging.LogLevel.DEBUG, payload);
        return true;
    }

    @Override
    public boolean processRecord(Record record, boolean origin, Document doc) throws PayloadException {
        XMLStreamReader reader;
        try {
            reader = inputFactory.createXMLStreamReader(RecordUtil.getReader(record, RecordUtil.PART_CONTENT));
        } catch (XMLStreamException e) {
            log.debug("Unable to make an XMLStream from " + record);
            if (origin) {
                throw new PayloadException("Unable to make an XMLStream from " + record, e);
            }
            return false;
        }
        try {
            if (origin) {
                log.trace("Processing top-level " + record.getId());
                float docBoost = processHeader(reader, record, origin);
                processBody(reader, doc, docBoost, record, origin);
            } else {
                log.trace("Skipping headers for non-toplevel " + record.getId());
                processHeader(reader, record, false);
                processBody(reader, doc, NEUTRAL_BOOST, record, false);
            }
        } catch (ParseException e) {
            String message = "Unable to parse XMLStream from " + record;
            log.debug(message, e);
            if (origin) {
                throw new PayloadException(message, e);
            }
            return false;
        } catch (XMLStreamException e) {
            String message = "Unable to extract content from XMLStream from " + record;
           
            if (log.isDebugEnabled()) {
                log.debug(message + ". Problematic content was:\n" + record.getContentAsUTF8(), e);
            }
            Logging.logProcess("StreamingDocumentcreator", message, Logging.LogLevel.WARN, record, e);
            if (origin) {
                throw new PayloadException(message, e);
            }
            return false;
        } catch (IndexServiceException e) {
            String message = "Exception while updating the Lucene document for " + record;
            log.debug(message, e);
            throw new PayloadException(message, e);
        }
        return true;
    }

    private static final float NEUTRAL_BOOST = 1.0f;
    private float processHeader(XMLStreamReader reader, Record record, boolean origin)
                                                                             throws ParseException, XMLStreamException {
        float boost = NEUTRAL_BOOST;
        long startTime = System.nanoTime();
        log.trace("Parsing header-information for " + record.getId());
        skipComments(reader);
        int eventType = reader.getEventType();
        if (eventType != XMLEvent.START_DOCUMENT) {
            //noinspection DuplicateStringLiteralInspection
            throw new ParseException(String.format("The first element was not start, it was %s",
                                                   XMLUtil.eventID2String(eventType)), 0);
        }

        if (!reader.hasNext()) {
            throw new ParseException("The stream must have at lest one element", 0);
        }
        reader.next();
        skipComments(reader);
        eventType = reader.getEventType();
        if (!(eventType == XMLEvent.START_ELEMENT
            && SUMMA_DOCUMENT.equals(reader.getLocalName())
            && SUMMA_NAMESPACE.equals(reader.getName().getNamespaceURI()))) {
            try {
                throw new ParseException(String.format(
                    "processHeader: The start element should be %s:%s, but was %s:%s",
                    SUMMA_NAMESPACE, SUMMA_DOCUMENT, reader.getName().getNamespaceURI(), reader.getLocalName()),
                                         reader.getLocation().getCharacterOffset());
            } catch (IllegalStateException e) {
                throw new IllegalStateException(String.format(
                    "processHeader: Unable to construct ParseException. Expected StartElement %s:%s, got %s",
                    SUMMA_NAMESPACE, SUMMA_DOCUMENT, XMLUtil.eventID2String(eventType)), e);
            }
        }
        try {
            //noinspection DuplicateStringLiteralInspection
            String boostString = getAttributeValue(reader, SUMMA_NAMESPACE, SUMMA_BOOST);
            if (boostString != null) {
                if (origin) {
                    boost = Float.parseFloat(boostString);
                } else {
                    Logging.log(log, Logging.LogLevel.TRACE,
                                "Skipping boost %s to document for %s as it is not origin",
                                boost, record.getId());
                }
            } else {
                log.trace("No explicit boost for document for " + record.getId());
            }
        } catch (Exception e) {
            Logging.log(log, Logging.LogLevel.DEBUG, e,
                        "Exception extracting and setting boost for %s", record.getId());
        }
        Logging.log(log, Logging.LogLevel.TRACE,
                    "Extracted header-information for %s in %d ns", record.getId(), System.nanoTime() - startTime);
        return boost;
    }

    private void skipComments(XMLStreamReader reader) throws XMLStreamException {
        while (reader.getEventType() == XMLStreamReader.COMMENT || reader.getEventType() == XMLStreamReader.SPACE) {
            if (!reader.hasNext()) {
                return;
            }
            reader.next();
        }
    }

    @SuppressWarnings({"UnusedParameters"})
    private void processBody(XMLStreamReader reader, Document luceneDoc, float docBoost, Record record, boolean origin)
                                                      throws ParseException, XMLStreamException, IndexServiceException {
        /* As the boost for multiple instances of the same field gets multiplied, we only assign field boosts once. */
        Set<String> boostedFields = null;

        long startTime = System.nanoTime();

        // Fields
        if (!reader.hasNext()) {
            throw new ParseException(String.format("Expected content in %s", record),
                                     reader.getLocation().getCharacterOffset());
        }
        while(reader.hasNext()) {
            reader.next();
            skipComments(reader);
            if (reader.getEventType() == XMLStreamReader.START_ELEMENT && SUMMA_FIELDS.equals(reader.getLocalName())
                && SUMMA_NAMESPACE.equals(reader.getName().getNamespaceURI())) {
                log.trace("Found <" + SUMMA_FIELDS + ">. Parsing fields");
                break;
            }
        }

        // TODO: Add check for field-count. If 0, then log a workflow warning
        log.trace("Adding fields to Lucene Document for " + record);
        while (reader.hasNext()) {
            reader.next();
            skipComments(reader);
            if (reader.getEventType() == XMLStreamReader.END_ELEMENT
                && SUMMA_FIELDS.equals(reader.getLocalName())
                && SUMMA_NAMESPACE.equals(reader.getName().getNamespaceURI())) {
                log.trace("Reached </" + SUMMA_FIELDS + ">. No more fields");
                break;
            }
            if (reader.getEventType() != XMLStreamReader.START_ELEMENT) {
                if (reader.getEventType() == XMLStreamReader.CHARACTERS
                    || reader.getEventType() == XMLStreamReader.COMMENT) {
                    // Ignore text and comments in between fields
                } else {
                    if (reader.getEventType() == XMLStreamReader.END_ELEMENT) {
                        log.debug("Expected start-element but got " + XMLUtil.eventID2String(reader.getEventType())
                                  + " with name '" + reader.getLocalName() + "' in " + record);
                    } else {
                        log.debug("Expected start-element but got " + XMLUtil.eventID2String(
                                reader.getEventType()) + " in " + record);
                    }
                    }
                continue;
            }
            if (!(SUMMA_FIELD.equals(reader.getLocalName()) &&
                  SUMMA_NAMESPACE.equals(reader.getName().getNamespaceURI()))) {
                log.debug(String.format(
                        "The expected element was %s:%s, but the received was %s:%s. Ignoring element",
                        SUMMA_NAMESPACE, SUMMA_FIELD, reader.getName().getNamespaceURI(), reader.getLocalName()));
                continue;
            }
            // <field name="author" boost="2.0">Jens Hansen</field>
            String fieldName = getAttributeValue(reader, SUMMA_NAMESPACE, SUMMA_NAME);
            if (fieldName == null) {
                throw new ParseException(String.format("Field without name-attribute in %s", record),
                                         reader.getLocation().getCharacterOffset());
            }

            Float boost = docBoost;
            try {
                //noinspection DuplicateStringLiteralInspection
                String boostString = getAttributeValue(reader, SUMMA_NAMESPACE, SUMMA_BOOST);
                if (boostString != null) {
                    boost *= Float.valueOf(boostString);
                }
            } catch (Exception e) {
                log.debug("Exception extracting boost for field " + fieldName + " from Record " + record.getId(), e);
            }
            // TODO: Verify how we handle embedded HTML
            String content = reader.getElementText();
            if (content != null) {
                // TODO: Perform a more complete trim (newline et al)
                content = content.trim();
                if ("".equals(content)) {
                    continue; // We do not want to store empty content
                }

                // As we explicitly assign to NEUTRAL_BOOST and as a false negative is okay, it is safe to compare directly
                //noinspection FloatingPointEquality
                if (boost == NEUTRAL_BOOST || (boostedFields != null && boostedFields.contains(fieldName))) {
                    boost = null;
                } else { // We have a boost and the field is not previously boosted
                    if (boostedFields == null) {
                        boostedFields = new HashSet<String>();
                    }
                    boostedFields.add(fieldName);
                }

                LuceneIndexField indexField = addFieldToDocument(descriptor, luceneDoc, fieldName, content, boost);
                if (indexField.isInFreetext()) {
                    addToFreetext(descriptor, luceneDoc, fieldName, content);
                }
            } else {
                log.debug("No content for field " + fieldName + " in " + record);
            }
        }
        log.trace("Extracted and added body-information for " + record + " in "
                  + (System.nanoTime() - startTime) + " ns");
    }

    // TODO: Make this namespace aware
    @SuppressWarnings({"UnusedParameters"})
    private String getAttributeValue(
        XMLStreamReader reader, String namespace, String localName) {
        for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
            if (localName.equals(reader.getAttributeLocalName(i))) {
                return reader.getAttributeValue(i);
            }
        }
        return null;
    }

    @Override
    public synchronized void close(boolean success) {
        super.close(success);
        log.info("Closing down StreamingDocumentcreator. " + getProcessStats());
    }
}
