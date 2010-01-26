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
package dk.statsbiblioteket.summa.ingest.split;

import java.util.*;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.StringReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLUtil;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.summa.common.util.UniqueTimestampGenerator;
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
    private static final String XMLNS = "xmlns";

    private XMLSplitterReceiver receiver;
    private XMLSplitterParserTarget target;

    @SuppressWarnings({"UnusedDeclaration"})
    public XMLSplitterHandler(Configuration conf,
                              XMLSplitterReceiver receiver,
                              XMLSplitterParserTarget target) {
        this.receiver = receiver;
        this.target = target;
    }

    private void checkRunning() throws SAXException {
        if (receiver.isTerminated()) {
            throw new SAXException("Parser stop requested");
        }
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


    @Override
    public void startPrefixMapping (String prefix, String uri) throws
                                                               SAXException {
        checkRunning();
        String expanded = ("".equals(prefix) ? XMLNS : XMLNS + ":" + prefix)
                          + "=\"" + uri + "\"";
        if (log.isTraceEnabled()) {
            log.trace("Prefix: " + expanded);
        }
//        System.out.println("Prefix: " + prefix + ", uri " + uri);
        if (inRecord) {
            overwriteOrAdd(insideRecordPrefixStack, expanded);
        } else {
            overwriteOrAdd(outsideRecordPrefixStack, expanded);
        }
    }

    private void overwriteOrAdd(Collection<String> existing, String n) {
        try {
            String newPrefix = n.split("=", 2)[0];
            for (String e: existing) {
                if (e.split("=", 2)[0].equals(newPrefix)) {
                    if (log.isTraceEnabled()) {
                        log.trace(String.format(
                                "Overwriting namespace %s with %s", e, n));
                    }
                    existing.remove(e);
                    break;
                }
            }
            existing.add(n);
        } catch (Exception e) {
            log.warn(String.format("Exception in overwriteOrAdd(%s, %s)",
                                   Strings.join(existing, ", "), n), e);
        }
    }


    @Override
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

    @Override
    public void startElement (String uri, String local, String qName,
                              Attributes atts) throws SAXException {
        checkRunning();
        List<String> prefixes;
        boolean rootRecordElement;
        if (!inRecord && equalsAny(target.recordElement, qName, local)
            && (target.recordNamespace == null
                || target.recordNamespace.equals(uri))) {
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
        sw.append("<").append(target.preserveNamespaces ? qName : local);
        if (target.preserveNamespaces) {
            for (String prefix: prefixes) {
                sw.append(" ").append(prefix);
                // xmlns, xmlns:foo etc.
            }
        }
        insideRecordElementStack.add(qName);
        if (!rootRecordElement) { // Only clear inside prefixes
            prefixes.clear();
        }

        // Check if this is an id element if the idElement from target is not
        // an empty string. If we already have an id just move along
        if (!"".equals(target.idElement) && id.getBuffer().length() == 0) {
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

        // Append namespaces and attributes for the current element
        for (int i = 0 ; i < atts.getLength() ; i++) {
            if (atts.getLocalName(i).startsWith(XMLNS)) {
                // We skip already added name spaces
            }
            sw.append(" ").append(target.preserveNamespaces ?
                                  atts.getQName(i) :
                                  atts.getLocalName(i)).append("=\"");
            sw.append(XMLUtil.encode(atts.getValue(i))).append("\"");
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


    @Override
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
        sw.append("</").append(target.preserveNamespaces ? qName : localName);
        sw.append(">");
        if (equalsAny(target.recordElement, qName, localName)
            && (target.recordNamespace == null
                || target.recordNamespace.equals(uri))) {
            // Record XML end reached
            log.debug("Record XML collected, creating Record");
            if ("".equals(id.toString())) {
                if ("".equals(target.idElement)) {
                    makeRandomID(id);
                } else {
                    log.warn("Record found, but no id could be located. "
                             + "Skipping Record");
                    if (log.isTraceEnabled()) {
                        log.trace(String.format(
                                "Dumping id-less Record-XML (expected "
                                + "id-element %s#%s):\n%s",
                                target.idElement, target.idTag, sw.toString()));
                    }
                    prepareScanForNextRecord();
                    return;
                }
            }
            try {
                Record record = new Record(
                        id.toString(), // ID-modification is handled by parser
                        target.base,
                        (HEADER + sw.toString()).getBytes("utf-8"));
                //noinspection DuplicateStringLiteralInspection
                log.debug("Produced " + record);
                receiver.queueRecord(record);
                prepareScanForNextRecord();
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Unable to convert string to utf-8 "
                                           + "bytes: '" + sw.toString() + "'",
                                           e);
            }
        }
    }

    /**
     * Created a semi-random ID, guaranteed to be unique within the current
     * XMLSplitterHandler instance.
     * @param id where to append the ID.
     */
    private void makeRandomID(StringWriter id) {
        id.append("randomID_").append(Long.toString(utg.next()));
    }
    private UniqueTimestampGenerator utg = new UniqueTimestampGenerator();

    @Override
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
        sw.append(XMLUtil.encode(chars));
    }


    @Override
    public void ignorableWhitespace(char ch[], int start, int length) throws
                                                                      SAXException {
        characters(ch, start, length);
    }

    @Override
    public void comment(char ch [], int start, int length) throws SAXException {
        checkRunning();
        if (!inRecord) {
            return;
        }
        String chars = new String(ch, start, length);
        sw.append("<!--").append(chars).append("-->");
    }

    @Override
    public void startCDATA() throws SAXException {
        checkRunning();
        if (!inRecord) {
            return;
        }
        sw.append("<![CDATA[");
    }
    @Override
    public void endCDATA() throws SAXException {
        checkRunning();
        if (!inRecord) {
            return;
        }
        sw.append("]]>");
    }

    /*
     * Forcefully - not resolve DTDs
     */
    @Override
    public InputSource resolveEntity(String publicId, String systemId) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("Ignoring request to resolve entity '" + publicId + "'");
        return new InputSource(new StringReader(""));
    }

    /*
     * Forcefully - not resolve DTDs
     */
    @Override
    public InputSource resolveEntity(String name, String publicId,
                                     String baseURI, String systemId) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("Ignoring request to resolve entity '" + publicId + "'");
        return new InputSource(new StringReader(""));
    }

}

