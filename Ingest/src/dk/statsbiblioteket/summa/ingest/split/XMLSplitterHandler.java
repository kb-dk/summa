/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.ingest.split;

import java.util.*;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.util.ParseUtil;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

/**
 * SAX-handler used by XMLSplitterParser.
 */
// TODO: This is tightly coupled to XMLSplitterParser. Consider de-coupling it
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class XMLSplitterHandler extends DefaultHandler2 {
    private static Log log = LogFactory.getLog(XMLSplitterHandler.class);

    // TODO: Extract this from the stream instead of hardcoding
    private static final String HEADER =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    private XMLSplitterParser parser;
    private XMLSplitterParserTarget target;

    @SuppressWarnings({"UnusedDeclaration"})
    public XMLSplitterHandler(Configuration conf,
                              XMLSplitterParser parser,
                              XMLSplitterParserTarget target) {
        this.parser = parser;
        this.target = target;
    }

    private void checkRunning() throws SAXException {
        parser.checkRunning();
    }

    /* DefaultHandler overrides */
    private List<String> insideRecordElementStack = new ArrayList<String>(20);
    private Set<String> outsideRecordPrefixStack = new HashSet<String>(20);
    private List<String> insideRecordPrefixStack = new ArrayList<String>(20);

    /* Inside a record-block */
    private boolean inRecord;
    /* Inside an id-block */
    private boolean inId;
    /* Record content */
    private StringWriter sw;
    private StringWriter id;

    void resetForNextRecord() {
        prepareScanForNextRecord();
    }
    void resetForNextStream() {
        prepareScanForNextRecord();
        outsideRecordPrefixStack.clear();
    }

    void prepareScanForNextRecord() {
        //parser.reset(); // Don't reset, as it erases namespace context
        inRecord = false;
        inId = false;
        sw = new StringWriter(10000);
        id = new StringWriter(100);
        insideRecordPrefixStack.clear();
        //outsideRecordPrefixStack.clear();  // TODO: Should this be cleared?
        insideRecordElementStack.clear();
    }


    public void startPrefixMapping (String prefix, String uri) throws
                                                               SAXException {
        checkRunning();
        String expanded = ("".equals(prefix) ? "xmlns" : "xmlns:" + prefix)
                          + "=\"" + uri + "\"";
        if (log.isTraceEnabled()) {
            log.trace("Prefix: " + expanded);
        }
//        System.out.println("Prefix: " + prefix + ", uri " + uri);
        if (inRecord) {
            insideRecordPrefixStack.add(expanded);
        } else {
            outsideRecordPrefixStack.add(expanded);
        }
    }

    public void endPrefixMapping (String prefix) throws SAXException {
        checkRunning();
        // Ignore
/*        String expected = prefixStack.remove(prefixStack.size()-1);
        if (!expected.startsWith(prefix)) {
            System.err.println("Miss prefix pop. Expected " + expected
                               + ", got " + prefix);
        }
  */
    }

    public void startElement (String uri, String local, String qName,
                              Attributes atts) throws SAXException {
        checkRunning();
        List<String> prefixes;
        boolean rootRecordElement;
        if (!inRecord && equalsAny(target.recordElement, qName, local)) {
            // This is the Record root element
            inRecord = true;
            rootRecordElement = true;
            prefixes = new ArrayList<String>(outsideRecordPrefixStack);
            log.trace("Record start");
        } else {
            rootRecordElement = false;
            prefixes = insideRecordPrefixStack;
        }
        if (log.isTraceEnabled()) {
            log.trace((inRecord ? "Inside" : "Outside") + " element: " + qName);
        }
        if (!inRecord) {
            return;
        }

        // We're inside a Record
        sw.append("<").append(qName);
        for (String prefix: prefixes) {
            sw.append(" ").append(prefix);
        }
        insideRecordElementStack.add(qName);
        if (!rootRecordElement) { // Only clear inside prefixes
            prefixes.clear();
        }

        // Check if this is an id element. If we already have an id just
        // move along
        if (id.getBuffer().length() == 0) {
            if (target.idNamespace == null) {
                // Do sloppy id element extraction
                if (equalsAny(target.idElement, qName, local)) {
                    log.trace ("Found record ID by sloppy matching");
                    inId = true;
                }
            } else {
                // Me must match against the namespaced element
                if (uri.equals(target.idNamespace) &&
                    target.idElement.equals(local)) {
                    log.trace ("Found record ID by strict matching");
                    inId = true;
                }
            }
        }

        for (int i = 0 ; i < atts.getLength() ; i++) {
            sw.append(" ").append(atts.getQName(i)).append("=\"");
            sw.append(ParseUtil.encode(atts.getValue(i))).append("\"");
            if (inId && !"".equals(target.idTag) &&
                equalsAny(target.idTag,
                          atts.getQName(i), atts.getLocalName(i))) {
                // ID matches attribute
                id.append(atts.getValue(i));
                inId = false; // If attribute then !value
            }
        }
        sw.append(">");
    }

    private boolean equalsAny(String expected, String possible1,
                              String possible2) {
        return expected.equals(possible1) || expected.equals(possible2);
    }


    public void endElement (String uri, String localName, String qName) throws
                                                                  SAXException {
        checkRunning();
        if (!inRecord) {
            return;
        }
        inId = false; // ID is always a single element, so end-element clears id
        String expected = insideRecordElementStack.size() > 0 ?
                          insideRecordElementStack.
                                  remove(insideRecordElementStack.size()-1)
                          : "NA";
        if (!expected.equals(qName)) {
            log.warn("endElement: Expected '" + expected
                     + "', got '" + qName + "'");
        } else {
            log.trace("endElement: " + qName);
        }
        sw.append("</").append(qName).append(">");
        if (equalsAny(target.recordElement, qName, localName)) {
            // Record XML end reached
            log.debug("Record XML collected, creating Record");
            if ("".equals(id.toString())) {
                log.warn("Record found, but no id could be located. Skipping");
                if (log.isTraceEnabled()) {
                    log.trace("Dumping id-less Record-XML (expected id-element "
                              + target.idElement + " # " + target.idTag
                              + "):\n" + sw.toString());
                }
                prepareScanForNextRecord();
                return;
            }
            try {
                Record record = new Record(
                        id.toString(), // ID-modification is handled by parser
                        target.base,
                        (HEADER + sw.toString()).getBytes("utf-8"));
                log.debug("Produced " + record);
                parser.queueRecord(record);
                prepareScanForNextRecord();
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Unable to convert string to utf-8 "
                                           + "bytes: '" + sw.toString() + "'",
                                           e);
            }
        }
    }

    public void characters(char ch[], int start, int length) throws
                                                             SAXException {
        checkRunning();
        if (!inRecord) {
            return;
        }
        String chars = new String(ch, start, length);
        if (inId) {
            // Append the unescaped characters as the id
            id.append(chars);
        }

        // Append escaped characters to the body
        sw.append(ParseUtil.encode(chars));
    }


    public void ignorableWhitespace(char ch[], int start, int length) throws
                                                                      SAXException {
        characters(ch, start, length);
    }

    public void comment(char ch [], int start, int length) throws SAXException {
        checkRunning();
        if (!inRecord) {
            return;
        }
        String chars = new String(ch, start, length);
        sw.append("<!--").append(chars).append("-->");
    }

    public void startCDATA() throws SAXException {
        checkRunning();
        if (!inRecord) {
            return;
        }
        sw.append("<![CDATA[");
    }
    public void endCDATA() throws SAXException {
        checkRunning();
        if (!inRecord) {
            return;
        }
        sw.append("]]>");
    }

}
