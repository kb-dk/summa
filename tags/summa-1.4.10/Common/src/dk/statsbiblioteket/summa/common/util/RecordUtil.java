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
import dk.statsbiblioteket.util.xml.XMLUtil;
import dk.statsbiblioteket.summa.common.Record;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.*;
import java.io.*;
import java.util.*;
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

    /**
     * If true, XML generated by {@link #toXML} makes no assumptions about the
     * content of the Records being XML and entity-escaped it all. If false,
     * Record content is assumed to be valid XML, XML-declarations are removed
     * and the XML inserted verbatim.
     * </p><p>
     * As RecordUtil is a utility-class, it does not have an explicit setup.
     * As such, this configuration-property is expected to be used by callers
     * of the {@link #toXML}-method.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_ESCAPE_CONTENT =
            "summa.common.recordutil.escapecontent";
    public static final boolean DEFAULT_ESCAPE_CONTENT = true;


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
    private static String CONTENT_TYPE = "type";
    private static final String CONTENT_TYPE_XML = "xml";
    private static final String CONTENT_TYPE_STRING = "string";

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
        return toXML(record, DEFAULT_ESCAPE_CONTENT);
    }

    /**
     * Represents the given Record as XML, dumping all data in
     * Record.xsd-format. This includes traversing parents and children.
     * While the XML conforms to the xsd, there is no header (aka declaration)
     * as it is expected that the XML will be further wrapped. To include a
     * header, prepend {@link ParseUtil#XML_HEADER}.
     * </p><p>
     * If escapeContent is true, the content of the Record is expected to be an
     * UTF-8 String, which will be entity-escaped. Thus there is no requirements
     * for the content being proper XML.
     * </p><p>
     * If escapeContent is false, the content is expected to be proper XML and
     * will not be entity-escaped. XML-declarations in the content from the
     * Records will be removed.
     * @param record the Record to represent as XML.
     * @param escapeContent if true, XML-content from the Records will be
     *                      entity-escaped.
     * @return the Record as UTF-8 XML, without header.
     * @throws javax.xml.stream.XMLStreamException if an error occured during
     *         XML creation.
     * @see {@link #fromXML}.
     * @see {@link RecordUtil#toXML}.
     */
    public static String toXML(Record record, boolean escapeContent) throws
                                                            XMLStreamException {
        log.trace("Creating XML for Record '" + record.getId() + "'");
        StringWriter sw = new StringWriter(5000);
        XMLStreamWriter xmlOut = xmlOutputFactory.createXMLStreamWriter(sw);
        xmlOut.setDefaultNamespace(RECORD_NAMESPACE);
        toXML(xmlOut, 0, record, escapeContent);
        log.debug("Created an XML representation of '" + record.getId() + "'");
        return sw.toString();
    }

    // http://www.w3.org/TR/xmlschema-2/#dateTime
    // 2002-10-10T17:00:00.000
    private static SimpleDateFormat schemaTimestampFormatter =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");
    // synchronized due to schemaTimestampFormatter
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private synchronized static void toXML(
            XMLStreamWriter out, int level,
            Record record, boolean escapeContent) throws XMLStreamException {
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
        try {
            writeContent(out, record, escapeContent);
        } catch (IOException e) {
            throw new XMLStreamException(String.format(
                    "Unable to write XML for content from %s", record), e);
        }
        out.writeEndElement();

        if (record.getParents() != null && record.getParents().size() > 0) {
            out.writeCharacters("\n");
            out.writeStartElement(PARENTS);
            for (Record parent: record.getParents()) {
                toXML(out, level+1, parent, escapeContent);
            }
            out.writeCharacters("\n");
            out.writeEndElement();
        }

        if (record.getChildren() != null && record.getChildren().size() > 0) {
            out.writeCharacters("\n");
            out.writeStartElement(CHILDREN);
            for (Record child: record.getChildren()) {
                if (log.isTraceEnabled()) {
                    log.trace("Calling toXML on " + child);
                }
                toXML(out, level+1, child, escapeContent);
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
                out.writeEndElement();
            }
            out.writeCharacters("\n");
            out.writeEndElement();
        }

        out.writeCharacters("\n");
        out.writeEndElement(); // record
    }

    /*
     * The underlyiongWriter-hack is necessary to write XML directly.
     */
    private static void writeContent(XMLStreamWriter out, Record record,
            boolean escapeContent) throws XMLStreamException, IOException {
        if (escapeContent) {
            out.writeCharacters(record.getContentAsUTF8());
            return;
        }
        out.writeAttribute(CONTENT_TYPE, CONTENT_TYPE_XML);
        XMLStreamReader content = xmlInputFactory.createXMLStreamReader(
                new StringReader(record.getContentAsUTF8()));
        int eventType = content.getEventType();
        if (eventType != XMLEvent.START_DOCUMENT) {
            String snippet = record.getContent() == null
                             ? "[no content in record]"
                             : record.getContentAsUTF8();
            snippet = snippet.substring(0, Math.min(20, snippet.length()));
            throw new XMLStreamException(String.format(
                    "First event was not START_DOCUMENT for '%s...' from %s",
                    snippet, record));
        }
        content.next();
        copyContent(content, out, record, true, -1);
    }

    /**
     * Strips the XML-declaration, if present, from the given XML. This makes it
     * possible to include the XML directly inside another XML structure.
     * @param xml the XML which should have the declaration removed.
     * @return the XML without XML-declaration.
     */
    public static String removeDeclaration(String xml) {
        int start = xml.indexOf("<?xml");
        int end = xml.indexOf(">");
        if (start == -1 || start > 8 || end < start) {
            // At the beginning, compensating for BOM
            return xml;
        }
        return xml.substring(end + 1);
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
                    || reader.getEventType() == XMLStreamReader.COMMENT
                    || reader.getEventType() == XMLStreamReader.SPACE) {
                    // Ignore text and comments in between fields
                } else {
                    log.debug("Expected start-elementor chars but got "
                              + ParseUtil.eventID2String(
                            reader.getEventType()) + " in " + record.getId());
                }
                continue;
            }

            if (reader.getEventType() != XMLStreamReader.START_ELEMENT) {
                log.debug(String.format(
                        "processRecord: Expected START_ELEMENT but got %s",
                        XMLUtil.eventID2String(reader.getEventType())));
                continue;
            }

            if (CONTENT.equals(reader.getLocalName()) &&
                RECORD_NAMESPACE.equals(reader.getName().getNamespaceURI())) {
                log.trace("Found content for Record '" + record.getId() + "'");
                try {
                    record.setContent(getContent(reader, record), false);
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

    private static byte[] getContent(XMLStreamReader reader, Record record)
                       throws XMLStreamException, UnsupportedEncodingException {
        log.info(XMLUtil.eventID2String(reader.getEventType()));
        for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
            if (reader.getAttributeLocalName(i).equals(CONTENT_TYPE)) {
                String type = reader.getAttributeValue(i);
                if (type.equals(CONTENT_TYPE_XML)) {
                    StringWriter sw = new StringWriter(5000);
                    XMLStreamWriter writer =
                            xmlOutputFactory.createXMLStreamWriter(sw);
                    reader.next();
                    writer.writeStartDocument("UTF-8", "1.0");
                    copyContent(reader, writer, record, true, -1);
                    writer.writeEndDocument();
                    return sw.toString().getBytes("utf-8");
                } else if (!type.equals(CONTENT_TYPE_STRING)) {
                    log.warn(String.format(
                            "Encountered unknown content type '%s' for %s. "
                            + "Parsing as string", type, record));
                }
            }
        }
        return reader.getElementText().getBytes("utf-8");
    }

    /**
     * Copies the content of the current sub-part from reader to writer.
     * By current sub-part we mean everything at the current level and down
     * in the DOM. Copying is stopped as soon as an end-tag above the current
     * level is encountered. The reader is returned positioned at this end tag.
     * @param reader the source.
     * @param writer the destination.
     * @param skipDeclarations if true, declarations, DTDs and notations are not
     *                         copied.
     * @param maxElements the maximum number of elements to copy. Only top-level
     *                    elements are counted. -1 means all elements.
     * @throws javax.xml.stream.XMLStreamException if the reader could not
     *         parse its content.
     */
    public static void copyContent(
            XMLStreamReader reader, XMLStreamWriter writer,
            boolean skipDeclarations, int maxElements)
                                                     throws XMLStreamException {
        copyContent(reader, writer, null, skipDeclarations, maxElements);
    }

    /* Like above but with nice debug message on error is record != null */
    private static void copyContent(
            XMLStreamReader reader, XMLStreamWriter writer, Record record,
            boolean skipDeclarations, int maxElements)
                                                     throws XMLStreamException {
        int depth = 0;
        int elementcount = 0;
        while (reader.getEventType() != XMLStreamReader.END_DOCUMENT) {
            switch (reader.getEventType()) {
                case XMLStreamReader.END_ELEMENT: {
                    depth--;
                    if (depth < 0) {
                        return; // Too far up
                    }
                    writer.writeEndElement();
                    break;
                }
                case XMLStreamReader.CHARACTERS:
                case XMLStreamReader.SPACE: {
                    writer.writeCharacters(reader.getText());
                    break;
                }
                case XMLStreamReader.CDATA: {
                    writer.writeCData(reader.getText());
                    break;
                }
                case XMLStreamReader.COMMENT: {
                    writer.writeComment(reader.getText());
                    break;
                }
                case XMLStreamReader.PROCESSING_INSTRUCTION: {
                    writer.writeProcessingInstruction(
                            reader.getPITarget(), reader.getPIData());
                    break;
                }
                case XMLStreamReader.ENTITY_REFERENCE: {
                    // TODO: Verify that this part is correct
                    writer.writeEntityRef(reader.getLocalName());
                    break;
                }
                case XMLStreamReader.DTD: {
                    if (!skipDeclarations) {
                        writer.writeDTD(reader.getText());
                    }
                    break;
                }
                case XMLStreamReader.START_DOCUMENT: {
                    if (!skipDeclarations) {
                        // TODO: Don't we need to transfer more information?
                        writer.writeStartDocument(reader.getVersion());
                    }
                    break;
                }
                case XMLStreamReader.NAMESPACE: {
                    for (int i = 0 ; i < reader.getNamespaceCount() ; i++) {
                        writer.writeNamespace(reader.getNamespacePrefix(i), 
                                              reader.getNamespaceURI(i));
                    }
                    break;
                }
                case XMLStreamReader.START_ELEMENT: {
                    if (elementcount == maxElements) {
                        return;
                    }
                    elementcount++;
                    String namespaceURI = reader.getNamespaceURI();
                    if (namespaceURI != null) {
                        writer.writeStartElement(
                                reader.getPrefix(), reader.getLocalName(),
                                reader.getNamespaceURI());
                    } else {
                        writer.writeStartElement(reader.getLocalName());
                    }
                    // TODO: What about default namespace?
                    for (int i = 0 ; i < reader.getNamespaceCount() ; i++) {
                        writer.writeNamespace(reader.getNamespacePrefix(i),
                                              reader.getNamespaceURI(i));
                    }
                    for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
                        if (reader.getAttributeNamespace(i) == null) {
                            writer.writeAttribute(
                                    reader.getAttributeLocalName(i),
                                    reader.getAttributeValue(i));
                            continue;
                        }
                        writer.writeAttribute(reader.getAttributePrefix(i),
                                              reader.getAttributeNamespace(i),
                                              reader.getAttributeLocalName(i),
                                              reader.getAttributeValue(i));
                    }
                    depth++;
                    break;
                }
                default: {
                    throw new XMLStreamException(String.format(
                            "Unknown event type %d from reader",
                            reader.getEventType()));
                }
            }
            try {
                reader.next();
            } catch (XMLStreamException e) {
                if (record == null) {
                    throw new XMLStreamException(
                            "Parse error (content could not be extracted "
                            + "for debugging)", e.getLocation(), e);
                }
                String content = record.getContent() == null
                                 ? "[no content]"
                                 : record.getContentAsUTF8();
                throw new XMLStreamException(String.format(
                        "Parse error. First 20 characters of %s was '%s'. "
                        + "Error was '%s'",
                        record, content.substring(
                                0, Math.min(20, content.length())), 
                        e.getMessage()),
                                             e.getLocation(), e);
            }
        }
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

    /**
     * Calculates the approximate memory usage of the Record.
     * </p><p>
     * This method is fairly light-weight and aims for approximations in the
     * kilobyte range. Thus the length of String attributes et al are ignored.
     * The sizes is primarily determined by content and meta-data.
     * @param record      the Record to calculate memory usage for.
     * @param followLinks if true, a transitive traversal of parents and
     *                    children is performed, summing the sizes.
     * @return the approximate memory usage of the Record.
     */
    public static int calculateRecordSize(Record record, boolean followLinks) {
        return calculateRecordSize(record, followLinks, null);
    }
    private static int calculateRecordSize(Record record, boolean followLinks,
                                           Set<Record> visited) {
        if (visited != null) {
            if (visited.contains(record)) {
                return 0;
            }
            visited.add(record);
        }

        int size = record.getContent(false) == null ? 0
                   : record.getContent(false).length;
        if (record.getMeta() != null) {
            for (Map.Entry<String, String> entry: record.getMeta().entrySet()) {
                try {
                    size += entry.getKey().length() + entry.getValue().length();
                } catch (NullPointerException e) {
                    log.trace("NPE while requesting key and value size from "
                              + "entry " + entry.getKey() + " in " + record, e);
                }
            }
        }
        if (!followLinks ||
            (record.getParents() == null && record.getChildren() == null)) {
            return size;
        }
        if (visited == null) {
            visited = new HashSet<Record>(10);
            visited.add(record);
        }
        if (record.getParents() != null) {
            for (Record parent: record.getParents()) {
                size += calculateRecordSize(parent, followLinks, visited);
            }
        }
        if (record.getChildren() != null) {
            for (Record child: record.getChildren()) {
                size += calculateRecordSize(child, followLinks, visited);
            }
        }
        return size;
    }
}
