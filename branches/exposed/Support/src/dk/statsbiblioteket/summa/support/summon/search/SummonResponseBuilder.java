/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
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
package dk.statsbiblioteket.summa.support.summon.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.search.exposed.facet.FacetResponse;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * Takes a Summon response as per
 * http://api.summon.serialssolutions.com/help/api/search/response
 * and converts it to a {@link DocumentResponse} and a {@link FacetResponse}.
 * Mapping between field & facet names as well as score adjustments or tag
 * tweaking is not in the scope of this converter.
 * </p><p>
 * The Summon API does not return namespace-aware XML so the parser is equally
 * relaxed.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummonResponseBuilder {
    private static Log log = LogFactory.getLog(SummonResponseBuilder.class);

    private XMLInputFactory xmlFactory = XMLInputFactory.newFactory();
    {
        xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
    }

    public SummonResponseBuilder(Configuration conf) {
        // No current configuration for Summon response builder
    }


    public long buildResponses(Request request, ResponseCollection responses,
                              String summonResponse) throws RemoteException {
        XMLStreamReader xml;
        try {
            xml = xmlFactory.createXMLStreamReader(new StringReader(
                summonResponse));
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException(
                "Unable to construct a reader from input", e);
        }

        String query = request.getString(DocumentKeys.SEARCH_QUERY);
        String filter = request.getString(DocumentKeys.SEARCH_FILTER, null);
        int startIndex = request.getInt(DocumentKeys.SEARCH_START_INDEX, 0) + 1;

        if (!findTagStart(xml, "response")) {
            log.warn("Could not locate start tag 'response', exiting");
            return 0;
        }
        long searchTime = Long.parseLong(
            getAttribute(xml, "totalRequestTime", "0"));
        long hitCount = Long.parseLong(
            getAttribute(xml, "recordCount", "0"));
        if (!findTagStart(xml, "response")) {
            log.warn("Could not locate start tag 'response', exiting");
            return 0;
        }
        String summonQueryString = "N/A";
        int maxRecords = -1;
        List<DocumentResponse.Record> records = null;
        // Seek to queries, facets or documents
        String currentTag;
        while ((currentTag = jumpToNextTagStart(xml)) != null) {
            if ("query".equals(currentTag)) {
                maxRecords = Integer.parseInt(getAttribute(
                    xml, "pageSize", Integer.toString(maxRecords)));
                summonQueryString = getAttribute(
                    xml, "queryString", summonQueryString);
                continue;
            }
            if ("rangeFacetFields".equals(currentTag)) {
                // TODO: Implement this
            }
            if ("documents".equals(currentTag)) {
                records = extractRecords(xml);
                // TODO: Implement this
            }
        }
        DocumentResponse documentResponse = new DocumentResponse(
            filter, query, startIndex, maxRecords, null, false, new String[0],
            searchTime, hitCount);
        for (DocumentResponse.Record record: records) {
            documentResponse.addRecord(record);
        }
        responses.add(documentResponse);
        return documentResponse.getHitCount();
    }

    /**
     * Extracts all Summon documents and converts them to
     * {@link }DocumentResponse#Record}.
     * @param xml the stream to extract records from. Must be positioned at
     * ELEMENT_START for "documents".
     * @return an array of record or the empty list if no documents were found.
     * @throws java.rmi.RemoteException if there were an error advancing.
     */
    private List<DocumentResponse.Record> extractRecords(XMLStreamReader xml)
                                                        throws RemoteException {
        // Positioned at documents
        List<DocumentResponse.Record> records =
            new ArrayList<DocumentResponse.Record>(50);
        while (true) {
            if (xml.getEventType() == XMLStreamReader.END_DOCUMENT
                || (xml.getEventType() == XMLStreamReader.END_ELEMENT
                    && xml.getLocalName().equals("documents"))) {
                break;
            }
            if (xml.getEventType() == XMLStreamReader.START_ELEMENT
                && xml.getLocalName().equals("document")) {
                DocumentResponse.Record record = extractRecord(xml);
                if (record != null) {
                    records.add(record);
                }
            }
            try {
                xml.next();
            } catch (XMLStreamException e) {
                throw new RemoteException(
                    "Exception advancing through XMLStreamReader", e);
            }
        }
        return records;
    }


    /**
     * Extracts a Summon document and converts it to
     * {@link }DocumentResponse#Record}.
     * @param xml the stream to extract the record from. Must be positioned at
     * ELEMENT_START for "document".
     * @return a record or null if no document could be extracted.
     * @throws java.rmi.RemoteException if there were an error advancing.
     */
    private DocumentResponse.Record extractRecord(XMLStreamReader xml)
                                                        throws RemoteException {
    // http://api.summon.serialssolutions.com/help/api/search/response/documents
        String openUrl = getAttribute(xml, "openUrl", null);
        if (openUrl == null) {
            log.warn("Encountered a document without openUrl. Discarding");
            return null;
        }
        String availibilityToken = getAttribute(xml, "availabilityToken", null);
        String hasFullText =       getAttribute(xml, "hasFullText", "false");
        String inHoldings =        getAttribute(xml, "inHoldings", "false");
        float score = 0f;
        String id = null;
        
        List<DocumentResponse.Field> fields =
            new ArrayList<DocumentResponse.Field>(50);
        while (true) {
            if (xml.getEventType() == XMLStreamReader.END_DOCUMENT
                || (xml.getEventType() == XMLStreamReader.END_ELEMENT
                    && xml.getLocalName().equals("document"))) {
                break;
            }
            if (xml.getEventType() == XMLStreamReader.START_ELEMENT
                && xml.getLocalName().equals("field")) {
                DocumentResponse.Field field = extractField(xml);
                if (field!= null) {
                    if ("ID".equals(field.getName())) {
                        id = field.getContent();
                    }
                    if ("Score".equals(field.getName())) {
                        score = Float.parseFloat(field.getContent());
                    }
                    fields.add(field);
                }
            }
            try {
                xml.next();
            } catch (XMLStreamException e) {
                throw new RemoteException(
                    "Exception advancing through XMLStreamReader", e);
            }
        }
        if (id == null) {
            log.warn("Unable to locate field 'ID' in Summon document. "
                     + "Skipping document");
            return null;
        }
        fields.add(new DocumentResponse.Field(
            "availibilityToken", availibilityToken, true));
        fields.add(new DocumentResponse.Field(
            "hasFullText", hasFullText, true));
        fields.add(new DocumentResponse.Field(
            "inHoldings", inHoldings, true));
        DocumentResponse.Record record =
            new DocumentResponse.Record(id, "Summon", score, null);
        for (DocumentResponse.Field field: fields) {
            record.addField(field);
        }
        return record;
    }

    /**
     * Extracts a Summon document field and converts it to
     * {@link }DocumentResponse#Field}.
     * </p><p>
     * While this implementation tries to produce fields for all inputs,
     * it is not guaranteed that it will be usable as no authoritative list
     * of possible micro formats used by Summon has been found.
     * @param xml the stream to extract the field from. Must be positioned at
     * ELEMENT_START for "field".
     * @return a field or null if no field could be extracted.
     * @throws java.rmi.RemoteException if there were an error advancing.
     */
    private DocumentResponse.Field extractField(XMLStreamReader xml)
                                                        throws RemoteException {
        String name = getAttribute(xml, "name", null);
        if (name == null) {
            log.warn("Could not extract name for field. Skipping field");
            return null;
        }
        // TODO: Implement XML handling and multiple values properly
        if (name.endsWith("_xml")) {
            log.debug(
                "XML fields are not supported yet. Skipping '" + name + "'");
            return null;
        }
        String value = null;
        while (true) {
            try {
                if (xml.getEventType() == XMLStreamReader.END_DOCUMENT
                    || (xml.getEventType() == XMLStreamReader.END_ELEMENT
                        && xml.getLocalName().equals("field"))) {
                    break;
                }
                if (xml.getEventType() == XMLStreamReader.START_ELEMENT
                    && xml.getLocalName().equals("value")) {
                    value = value == null ? xml.getElementText() :
                            value + "\n" + xml.getElementText();
                }
                xml.next();
            } catch (XMLStreamException e) {
                throw new RemoteException(
                    "Exception advancing through XMLStreamReader", e);
            }
        }
        if (value == null) {
            log.debug("No value for field '" + name + "'");
        }
        return new DocumentResponse.Field(name, value, true);
    }

    /**
     * Skips everything until a start tag is reacted or the readers is depleted.
     * @param xml the stream to iterate over.
     * @return the name of the start tag or null if EOD.
     * @throws java.rmi.RemoteException if a Stream error occurred during seek.
     */
    private String jumpToNextTagStart(XMLStreamReader xml)
                                                        throws RemoteException {
        try {
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
        } catch (XMLStreamException e) {
            throw new RemoteException("Error seeking for start tag", e);
        }
    }

    private String getAttribute(
        XMLStreamReader xml, String attributeName, String defaultValue) {
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
     * @throws java.rmi.RemoteException if there was an error seeking.
     */
    private boolean findTagStart(
        XMLStreamReader xml, String startTagName) throws RemoteException {
        while (true) {
            if (xml.getEventType() == XMLStreamReader.START_ELEMENT
                && startTagName.equals(xml.getLocalName())) {
                return true;
            }
            if (xml.getEventType() == XMLStreamReader.END_DOCUMENT) {
                return false;
            }
            try {
                xml.next();
            } catch (XMLStreamException e) {
                throw new RemoteException(
                    "Error seeking to start tag for element '" + startTagName
                    + "'", e);
            }
        }
    }
}
