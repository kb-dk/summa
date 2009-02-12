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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.Record;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.*;
import java.io.*;
import java.util.Map;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Helpers for processing Records.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class RecordUtil {
    private static Log log = LogFactory.getLog(RecordUtil.class);

    public static final String RECORD_NAMESPACE =
            "http://statsbiblioteket.dk/summa/2009/Record";

    private static XMLOutputFactory xmlOutputFactory =
            XMLOutputFactory.newInstance();
    private static XMLInputFactory xmlInputFactory =
            XMLInputFactory.newInstance();
    static {
        xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING,
                                    Boolean.TRUE);
        xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE,
                                    Boolean.TRUE);
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private static String RECORD = "record";

    private static String ID = "id";
    private static String BASE = "base";

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private static String DELETED = "deleted";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private static String INDEXABLE = "indexable";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private static String CTIME = "ctime";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private static String MTIME = "mtime";
    private static String CONTENT = "content";

    private static String PARENTS = "parents";
    private static String CHILDREN = "children";

    private static String META = "meta";
    private static String ELEMENT = "element";
    private static String KEY = "key";

    /**
     * Represents the given Record as XML, dumping all data in
     * Record.xsd-format. This includes traversing parents and children.
     * While the XML conforms to the xsd, there is no header (aka declaration)
     * as it is expected that the XML will be further wrapped. To include a
     * header, prepend {@link ParseUtil#XML_HEADER}.
     * </p><p>
     * The content of the Record is expected to be an UTF-8 String, which will
     * be entity-escaped. Thus there is no requirements for the content being
     * proper XML.
     * @param record the Record to represent as XML.
     * @return the Record as UTF-8 XML, without header.
     * @throws javax.xml.stream.XMLStreamException if an error occured during
     *         XML creation.
     * @see {@link #fromXML}
     */
    public static String toXML(Record record) throws XMLStreamException {
        log.trace("Creating XML for Record '" + record.getId() + "'");
        StringWriter sw = new StringWriter(5000);
        XMLStreamWriter xmlOut = xmlOutputFactory.createXMLStreamWriter(sw);
        xmlOut.setDefaultNamespace(RECORD_NAMESPACE);
        toXML(xmlOut, 0, record);
        log.debug("Created an XML representation of '" + record.getId() + "'");
        return sw.toString();
    }

    // http://www.w3.org/TR/xmlschema-2/#dateTime
    // 2002-10-10T17:00:00
    private static SimpleDateFormat schemaTimestampFormatter =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");
    // synchronized due to schemaTimestampFormatter
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private synchronized static void toXML(XMLStreamWriter out, int level,
                                           Record record)
                                                     throws XMLStreamException {
        log.trace("Constructing inner XML for Record '" + record.getId() + "'");
        String indent = "";
        while (indent.length() < level * 2) {
            indent += "  ";
        }

        if (level != 0) {
            out.writeCharacters("\n");
        }
        out.writeStartElement(RECORD);
        if (level == 0) {
            out.writeNamespace("", RECORD_NAMESPACE);
        }
        out.writeAttribute(ID, record.getId());
        out.writeAttribute(BASE, record.getBase());
        out.writeAttribute(DELETED, Boolean.toString(record.isDeleted()));
        out.writeAttribute(INDEXABLE, Boolean.toString(record.isIndexable()));
        out.writeAttribute(CTIME, schemaTimestampFormatter.format(
                new Date(record.getCreationTime())));
        out.writeAttribute(MTIME, schemaTimestampFormatter.format(
                new Date(record.getModificationTime())));
        out.writeCharacters("\n");

        out.writeStartElement(CONTENT);
        out.writeCharacters(record.getContentAsUTF8());
        out.writeEndElement();

        if (record.getParents() != null && record.getParents().size() > 0) {
            out.writeCharacters("\n");
            out.writeStartElement(PARENTS);
            for (Record parent: record.getParents()) {
                toXML(out, level+1, parent);
            }
            out.writeCharacters("\n");
            out.writeEndElement();
        }

        if (record.getChildren() != null && record.getChildren().size() > 0) {
            out.writeCharacters("\n");
            out.writeStartElement(CHILDREN);
            for (Record child: record.getChildren()) {
                toXML(out, level+1, child);
            }
            out.writeCharacters("\n");
            out.writeEndElement();
        }
             
        if (record.hasMeta()) {
            out.writeCharacters("\n");
            out.writeStartElement(META);
            for (Map.Entry<String, String> entry: record.getMeta().entrySet()) {
                out.writeCharacters("\n");
                out.writeStartElement(ELEMENT);
                out.writeAttribute(KEY, entry.getKey());
                out.writeCharacters(entry.getValue());
            }
            out.writeCharacters("\n");
            out.writeEndElement();
        }

        out.writeCharacters("\n");
        out.writeEndElement(); // record
    }

    /**
     * Parses the given XML and creates a Record from it, if possible.
     * @param xml an XML-representation of a Record.
     * @return a Record parsed from the XML.
     * @see {@link #toXML}
     * @throws IllegalArgumentException if the XML could not be parsed properly.
     */
    public static Record fromXML(String xml) throws IllegalArgumentException {
        return fromXML(new StringReader(xml));
    }

    /**
     * Parses the given XML and creates a Record from it, if possible.
     * This implementation is thread-safe and not synchronized.
     * @param xml an XML-representation of a Record, in UTF-8.
     * @return a Record parsed from the XML.
     * @see {@link #toXML}
     * @throws IllegalArgumentException if the XML could not be parsed properly.
     */
    public static Record fromXML(Reader xml) throws IllegalArgumentException {
        XMLStreamReader reader;
        try {
            reader = xmlInputFactory.createXMLStreamReader(xml);
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException(
                    "Unable to make an XMLStream from the given stream", e);
        }
        try {
            processHeader(reader);
            Record record = processRecord(reader);
            log.debug("Constructed Record '" + record.getId() + "' from XML");
            return record;
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    "Unable to parse XMLStream", e);
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException(
                    "Unable to extract content from XMLStream", e);
        }
    }

    private static void processHeader(XMLStreamReader reader)
                                     throws ParseException, XMLStreamException {
        int eventType = reader.getEventType();
        if (eventType != XMLEvent.START_DOCUMENT) {
            //noinspection DuplicateStringLiteralInspection
            throw new ParseException(String.format(
                    "The first event should be start, but it was %s",
                    ParseUtil.eventID2String(eventType)), 0);
        }
        reader.next();
        if (!reader.hasNext()) {
            throw new ParseException("The stream must have a element", 0);
        }
    }

    private static final byte[] DUMMY_CONTENT = new byte[0];
    // Expects the reader to be positioned at record start
    private static Record processRecord(XMLStreamReader reader)
                                     throws ParseException, XMLStreamException {
        int eventType = reader.getEventType();
        if (!(eventType == XMLEvent.START_ELEMENT
            && RECORD.equals(reader.getLocalName())
            && RECORD_NAMESPACE.equals(reader.getName().getNamespaceURI()))) {
            throw new ParseException(String.format(
                    "The element should be %s:%s, but was %s:%s",
                    RECORD_NAMESPACE, RECORD,
                    reader.getName().getNamespaceURI(), reader.getLocalName()),
                    reader.getLocation().getCharacterOffset());
        }

        Record record;
        long mtime;
        try {
            //noinspection DuplicateStringLiteralInspection
            String id = getAttributeValue(reader, RECORD_NAMESPACE, ID);
            String base = getAttributeValue(reader, RECORD_NAMESPACE, BASE);
            boolean deleted = Boolean.parseBoolean(getAttributeValue(
                    reader, RECORD_NAMESPACE, DELETED));
            boolean indexable = Boolean.parseBoolean(getAttributeValue(
                    reader, RECORD_NAMESPACE, INDEXABLE));
            long ctime = getDatetimeValue(reader, RECORD_NAMESPACE, CTIME);
            mtime = getDatetimeValue(reader, RECORD_NAMESPACE, MTIME);
            log.trace("Extracted record attributes for Record '" + id + "'");
            record = new Record(id, base, DUMMY_CONTENT);
            record.setDeleted(deleted);
            record.setIndexable(indexable);
            record.setCreationTime(ctime);
        } catch (Exception e) {
            throw new XMLStreamException("Exception extracting attributes", e);
        }

        log.trace("Looking for content, parents, children and meta for Record '"
                  + record.getId() + "'");
        while (reader.hasNext()) {
            reader.next();
            if (reader.getEventType() == XMLStreamReader.END_ELEMENT
                && RECORD.equals(reader.getLocalName())
                && RECORD_NAMESPACE.equals(
                    reader.getName().getNamespaceURI())) {
                break;
            }
            if (reader.getEventType() != XMLStreamReader.START_ELEMENT) {
                if (reader.getEventType() == XMLStreamReader.CHARACTERS
                    || reader.getEventType() == XMLStreamReader.COMMENT) {
                    // Ignore text and comments in between fields
                } else {
                    log.debug("Expected start-elementor chars but got "
                              + ParseUtil.eventID2String(
                            reader.getEventType()) + " in " + record.getId());
                }
                continue;
            }

            if (CONTENT.equals(reader.getLocalName()) &&
                RECORD_NAMESPACE.equals(reader.getName().getNamespaceURI())) {
                log.trace("Found content for Record '" + record.getId() + "'");
                try {
                    record.setContent(reader.getElementText().getBytes("utf-8"),
                                      false); // Optional compress?
                } catch (UnsupportedEncodingException e) {
                    //noinspection DuplicateStringLiteralInspection
                    throw new IllegalStateException("utf-8 not supported");
                }
                continue;
            }

            if (PARENTS.equals(reader.getLocalName()) &&
                RECORD_NAMESPACE.equals(reader.getName().getNamespaceURI())) {
                log.trace("Found parent element for Record '" + record.getId()
                          + "'");
                record.setParents(getRelatives(reader, PARENTS));
                continue;
            }

            if (CHILDREN.equals(reader.getLocalName()) &&
                RECORD_NAMESPACE.equals(reader.getName().getNamespaceURI())) {
                log.trace("Found children element for Record '" + record.getId()
                          + "'");
                record.setChildren(getRelatives(reader, CHILDREN));
                continue;
            }

            if (META.equals(reader.getLocalName()) &&
                RECORD_NAMESPACE.equals(reader.getName().getNamespaceURI())) {
                log.trace("Found meta element for Record '" + record.getId()
                          + "'");
                record.setMeta(getMeta(reader));
                continue;
            }

            log.debug("Encountered unexpected start element "
                      + reader.getName().getNamespaceURI() + ":"
                      + reader.getLocalName());
        }
        log.debug("Closing creation of Record '" + record.getId() + "'");
        record.setModificationTime(mtime);
        return record;
    }

    private static List<Record> getRelatives(XMLStreamReader reader,
                                             String relative)
                                                     throws XMLStreamException {
        List<Record> relatives = new ArrayList<Record>(5);
        while (reader.hasNext()) {
            reader.next();
            if (reader.getEventType() == XMLStreamReader.END_ELEMENT
                && relative.equals(reader.getLocalName())
                && RECORD_NAMESPACE.equals(
                    reader.getName().getNamespaceURI())) {
                break;
            }
            if (reader.getEventType() == XMLStreamReader.START_ELEMENT) {
                try {
                    relatives.add(processRecord(reader));
                } catch (Exception e) {
                    log.warn("Unable to expand relative Record of type "
                             + relative, e);
                }
                continue;
            }
            //noinspection DuplicateStringLiteralInspection
            log.trace("Skipping event " + ParseUtil.eventID2String(
                    reader.getEventType()) + " in getRelatives(..., "
                                           + relative + ")");
        }
        //noinspection DuplicateStringLiteralInspection
        log.trace("Expanded " + relatives.size() + " relatives of type "
                  + relative);
        return relatives;

    }

    private static StringMap getMeta(XMLStreamReader reader)
            throws XMLStreamException {
        StringMap map = new StringMap(10);
        while (reader.hasNext()) {
            reader.next();
            if (reader.getEventType() == XMLStreamReader.END_ELEMENT
                && META.equals(reader.getLocalName())
                && RECORD_NAMESPACE.equals(
                    reader.getName().getNamespaceURI())) {
                break;
            }
            if (reader.getEventType() == XMLStreamReader.START_ELEMENT
                    && ELEMENT.equals(reader.getLocalName())
                    && RECORD_NAMESPACE.equals(
                    reader.getName().getNamespaceURI())) {
                log.trace("Entered meta element");
                map.put(getAttributeValue(reader, KEY, RECORD_NAMESPACE),
                        reader.getElementText());
                continue;
            }
            //noinspection DuplicateStringLiteralInspection
            log.trace("Skipping event " + ParseUtil.eventID2String(
                    reader.getEventType()) + " in getMeta");
        }
        //noinspection DuplicateStringLiteralInspection
        log.trace("Expanded " + map.size() + " meta elements");
        return map;
    }

    private static long getDatetimeValue(XMLStreamReader reader,
                                         String namespace,
                                         String attribute)
                                                         throws ParseException {
        String sVal = getAttributeValue(reader, namespace, attribute);
        return schemaTimestampFormatter.parse(sVal).getTime();
    }

    // TODO: Make this namespace aware, sync with StreamingdocumentCreator then
    @SuppressWarnings({"UnusedDeclaration"})
    private static String getAttributeValue(XMLStreamReader reader,
                                            String namespace,
                                            String localName) {
        for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
            if (localName.equals(reader.getAttributeLocalName(i))) {
                return reader.getAttributeValue(i);
            }
        }
        return null;
    }
}
