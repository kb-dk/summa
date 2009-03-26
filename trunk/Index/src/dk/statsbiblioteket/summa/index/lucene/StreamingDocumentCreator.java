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
package dk.statsbiblioteket.summa.index.lucene;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLUtil;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexField;
import dk.statsbiblioteket.summa.common.lucene.index.IndexServiceException;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Logging;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayInputStream;
import java.text.ParseException;

/**
 * Stream-based converter from SummaDocumentXML to Lucene Documents.
 * The generated Document is added to the Payload's data under the key
 * {@link Payload#LUCENE_DOCUMENT}.
 * </p><p>
 * see SummaDocumentXMLSample.xml.
 * </p><p>
 * Note: The DocumentCreator needs an index-description. The setup for
 * retrieving the description must be stored in the sub-property
 * {@link LuceneIndexUtils#CONF_DESCRIPTOR} with parameters from
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
        inputFactory.setProperty(XMLInputFactory.IS_COALESCING,
                                 Boolean.TRUE);
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
    public void processPayload(Payload payload) {
        //noinspection DuplicateStringLiteralInspection
        log.debug("processPayload(" + payload + ") called");
        long startTime = System.currentTimeMillis();
        if (payload.getRecord() == null) {
            //noinspection DuplicateStringLiteralInspection
            throw new IllegalArgumentException(payload + " has no Record");
        }

        XMLStreamReader reader;
        try {
            reader = inputFactory.createXMLStreamReader(
                    new ByteArrayInputStream(payload.getRecord().getContent()),
                    "utf-8");
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException(
                    "Unable to make an XMLStream from " + payload, e);
        }

        // TODO: Use a pool of Documents so that they can be reused
        org.apache.lucene.document.Document luceneDoc =
                new org.apache.lucene.document.Document();
        // TODO: Check whether resolver is used for anything

        try {
            processHeader(reader, luceneDoc, payload);
            processBody(reader, luceneDoc, payload);
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    "Unable to parse XMLStream from " + payload, e);
        } catch (XMLStreamException e) {
            String message = "Unable to extract content from XMLStream from "
                             + payload;
            if (log.isDebugEnabled()) {
                log.debug(message + ". Problematic content was:\n"
                          + payload.getRecord().getContentAsUTF8());
            }
            Logging.logProcess("StreamingDocumentcreator", message,
                               Logging.LogLevel.WARN, payload, e);
            throw new IllegalArgumentException(message, e);
        } catch (IndexServiceException e) {
            throw new IllegalArgumentException(
                    "exception whle updating the Lucene document for "
                    + payload, e);
        }
        //noinspection DuplicateStringLiteralInspection
        log.trace("Setting " + IndexUtils.RECORD_FIELD + " to '"
                  + payload.getId() + "'");
        IndexUtils.assignID(payload.getId(), luceneDoc);

        payload.getData().put(Payload.LUCENE_DOCUMENT, luceneDoc);
        //noinspection DuplicateStringLiteralInspection
        log.debug("Added Lucene Document payload "
                  + payload + ". Processing time was "
                  + (System.currentTimeMillis() - startTime) + " ms");
    }

    private void processHeader(XMLStreamReader reader,
                               org.apache.lucene.document.Document luceneDoc,
                               Payload payload)
            throws ParseException, XMLStreamException {
        long startTime = System.nanoTime();
        log.trace("Parsing header-information for " + payload);
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
        eventType = reader.next();
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
