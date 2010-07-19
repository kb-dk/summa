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
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexField;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.lucene.index.IndexServiceException;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayInputStream;
import java.text.ParseException;

/**
 * Stream-based converter from SummaDocumentXML to Lucene Documents.
 * The generated Document is added to the Payload's data under the key
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
public class StreamingDocumentCreator extends DocumentCreatorBase {
    private static Log log = LogFactory.getLog(StreamingDocumentCreator.class);

    // TODO: Entity-encode fields

    // TODO: Reconsider this namespace
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String SUMMA_NAMESPACE =
//            "http://statsbiblioteket.dk/2008/Index";
            "http://statsbiblioteket.dk/summa/2008/Document";
    // TODO: Make DocumentCreator support namespace qualified attributes

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private static final String SUMMA_DOCUMENT = "SummaDocument";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private static final String SUMMA_FIELD = "field";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private static final String SUMMA_FIELDS = "fields";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private static final String SUMMA_BOOST = "boost";
    private static final String SUMMA_NAME = "name";

    private XMLInputFactory inputFactory;

    private LuceneIndexDescriptor descriptor;

    /**
     * Initialize the underlying parser and set up internal structures.
     * {@link #setSource} must be called before further use.
     * @param conf the configuration for the document creator..
     * @throws Configurable.ConfigurationException if there was a problem with
     *         the configuration.
     */
    public StreamingDocumentCreator(Configuration conf) throws
                                                        ConfigurationException {
        super(conf);
        inputFactory = XMLInputFactory.newInstance();
        // TODO: Check to see if we need to handle CData-events
//        inputFactory.setProperty("report-cdata-event", Boolean.TRUE);
        //inputFactory.setProperty(XMLInputFactory.IS_COALESCING,
        //                         Boolean.TRUE);
        inputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE,
                                 Boolean.TRUE);

        descriptor = LuceneIndexUtils.getDescriptor(conf);
    }

//            singleFieldXPathExpression = singleFields.compile(
//                    "/Index:SummaDocument/Index:fields/Index:field");

    /**
     * Convert the content of the Record embedded in the payload to a Lucene
     * Document. The content must be in the format SummaDocumentXML.
     * @param payload the container for the Record-content to convert.
     */
    // TODO: If not added, mark meta-data with unadded and continue gracefully
    @Override
    public boolean processPayload(Payload payload) throws PayloadException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("processPayload(" + payload + ") called");
        long startTime = System.nanoTime();
        if (payload.getRecord() == null) {
            //noinspection DuplicateStringLiteralInspection
            throw new PayloadException("No Record present", payload);
        }

        XMLStreamReader reader;
        try {
            reader = inputFactory.createXMLStreamReader(
                    new ByteArrayInputStream(payload.getRecord().getContent()),
                    "utf-8");
        } catch (XMLStreamException e) {
            log.debug("Unable to make an XMLStream from Payload");
            throw new PayloadException(
                    "Unable to make an XMLStream from Payload", e, payload);
        }

        // TODO: Use a pool of Documents so that they can be reused
        org.apache.lucene.document.Document luceneDoc =
                new org.apache.lucene.document.Document();
        // TODO: Check whether resolver is used for anything

        try {
            processHeader(reader, luceneDoc, payload);
            processBody(reader, luceneDoc, payload);
        } catch (ParseException e) {
            log.debug("Unable to parse XMLStream from Payload");
            throw new PayloadException(
                    "Unable to parse XMLStream from Payload", e, payload);
        } catch (XMLStreamException e) {
            String message =
                    "Unable to extract content from XMLStream from Payload";
            if (log.isDebugEnabled()) {
                log.debug(message + " " + payload
                          + ". Problematic content was:\n"
                          + payload.getRecord().getContentAsUTF8(), e);
            }
            Logging.logProcess("StreamingDocumentcreator", message,
                               Logging.LogLevel.WARN, payload, e);
            throw new PayloadException(message, e, payload);
        } catch (IndexServiceException e) {
            log.debug("Exception whle updating the Lucene document");
            throw new PayloadException(
                    "Exception whle updating the Lucene document", e, payload);
        }

        payload.getData().put(Payload.LUCENE_DOCUMENT, luceneDoc);
        //noinspection DuplicateStringLiteralInspection
        log.debug("Added Lucene Document payload "
                  + payload + ". Processing time was "
                  + (System.nanoTime() - startTime) / 1000000D + " ms");
        return true;
    }

    private void processHeader(XMLStreamReader reader,
                               org.apache.lucene.document.Document luceneDoc,
                               Payload payload)
            throws ParseException, XMLStreamException {
        long startTime = System.nanoTime();
        log.trace("Parsing header-information for " + payload);
        skipComments(reader);
        int eventType = reader.getEventType();
        if (eventType != XMLEvent.START_DOCUMENT) {
            //noinspection DuplicateStringLiteralInspection
            throw new ParseException(String.format(
                    "The first element was not start, it was %s",
                    XMLUtil.eventID2String(eventType)), 0);
        }

        if (!reader.hasNext()) {
            throw new ParseException(
                    "The stream must have at lest one element", 0);
        }
        reader.next();
        skipComments(reader);
        eventType = reader.getEventType();
        if (!(eventType == XMLEvent.START_ELEMENT
            && SUMMA_DOCUMENT.equals(reader.getLocalName())
            && SUMMA_NAMESPACE.equals(reader.getName().getNamespaceURI()))) {
            throw new ParseException(String.format(
                    "The start element should be %s:%s, but was %s:%s",
                    SUMMA_NAMESPACE, SUMMA_DOCUMENT,
                    reader.getName().getNamespaceURI(), reader.getLocalName()),
                    reader.getLocation().getCharacterOffset());
        }
        try {
            //noinspection DuplicateStringLiteralInspection
            String boostString = getAttributeValue(reader, SUMMA_NAMESPACE,
                                                   SUMMA_BOOST);
            if (boostString != null) {
                float boost = Float.parseFloat(boostString);
                log.trace("Assigning boost " + boost + " to document for "
                          + payload);
                luceneDoc.setBoost(boost);
            } else {
                log.trace("No explicit boost for document for " + payload);
            }
        } catch (Exception e) {
            log.debug("Exception extracting and setting boost for " + payload,
                      e);
        }
        log.trace("Extracted header-information for " + payload + " in " 
                  + (System.nanoTime() - startTime) + " ns");
    }

    private void skipComments(XMLStreamReader reader) throws
                                                            XMLStreamException {
        while (reader.getEventType() == XMLStreamReader.COMMENT) {
            if (!reader.hasNext()) {
                return;
            }
            reader.next();
        }
    }

    private void processBody(XMLStreamReader reader,
                               org.apache.lucene.document.Document luceneDoc,
                               Payload payload)
            throws ParseException, XMLStreamException, IndexServiceException {
        long startTime = System.nanoTime();

        // Fields
        if (!reader.hasNext()) {
            throw new ParseException(String.format(
                    "Expected content in %s", payload),
                    reader.getLocation().getCharacterOffset());
        }
        while(reader.hasNext()) {
            reader.next();
            skipComments(reader);
            if (reader.getEventType() == XMLStreamReader.START_ELEMENT
                && SUMMA_FIELDS.equals(reader.getLocalName())
                && SUMMA_NAMESPACE.equals(reader.getName().getNamespaceURI())) {
                log.trace("Found <" + SUMMA_FIELDS + ">. Parsing fields");
                break;
            }
        }

        // TODO: Add check for field-count. If 0, then log a workflow warning
        //noinspection DuplicateStringLiteralInspection
        log.trace("Adding fields to Lucene Document for " + payload);
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
                    log.debug("Expected start-element but got "
                              + XMLUtil.eventID2String(
                            reader.getEventType()) + " in " + payload);
                }
                continue;
            }
            if (!(SUMMA_FIELD.equals(reader.getLocalName()) &&
                  SUMMA_NAMESPACE.equals(reader.getName().getNamespaceURI()))) {
                log.debug(String.format(
                        "The expected element was %s:%s, but the received was "
                        + "%s:%s. Ignoring element",
                        SUMMA_NAMESPACE, SUMMA_FIELD,
                        reader.getName().getNamespaceURI(),
                        reader.getLocalName()));
                continue;
            }
            // <field name="author" boost="2.0">Jens Hansen</field>
            String fieldName = getAttributeValue(reader, SUMMA_NAMESPACE,
                                                 SUMMA_NAME);
            if (fieldName == null) {
                throw new ParseException(String.format(
                        "Field without name-attribute in %s", payload),
                        reader.getLocation().getCharacterOffset());
            }

            Float boost = null;
            try {
                //noinspection DuplicateStringLiteralInspection
                String boostString = getAttributeValue(reader, SUMMA_NAMESPACE,
                                                       SUMMA_BOOST);
                if (boostString != null) {
                    boost = Float.valueOf(boostString);
                }
            } catch (Exception e) {
                log.debug("Exception extracting boost for field " + fieldName,
                          e);
            }



            // TODO: Verify how we handle embedded HTML
            String content = reader.getElementText();
            if (content != null) {
                // TODO: Perform a more complete trim (newline et al)
                content = content.trim();
                LuceneIndexField indexField =
                        addFieldToDocument(descriptor, luceneDoc,
                                           fieldName, content, boost);
                if (indexField.isInFreetext()) {
                    addToFreetext(descriptor, luceneDoc, fieldName, content);
                }
            } else {
                log.debug("No content for field " + fieldName + " in "
                          + payload);
            }
        }
        log.trace("Extracted and added body-information for " + payload + " in "
                  + (System.nanoTime() - startTime) + " ns");
    }

    // TODO: Make this namespace aware
    private String getAttributeValue(XMLStreamReader reader,
                                     String namespace, String localName) {
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

