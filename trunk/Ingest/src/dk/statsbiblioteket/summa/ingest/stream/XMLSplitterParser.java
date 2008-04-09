/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
package dk.statsbiblioteket.summa.ingest.stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.ext.DefaultHandler2;

/**
 * Splits a XML document into pieces, validates and handles namespaces.
 * The parser is a helper-class for {@link XMLSplitterFilter} and outputs
 * Records, ready for passing through the chain. 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class XMLSplitterParser extends DefaultHandler2 implements
                                                       Iterator<Payload>,
                                                       Runnable {
    private static Log log = LogFactory.getLog(XMLSplitterParser.class);

    private static final int DEFAULT_MAX_QUEUE = 50;
    private static final long QUEUE_TIMEOUT = 120*1000; // Timeout in ms
    private static final long HASNEXT_SLEEP = 100; // Sleep-ms between polls
    /* We feed this to the queue to signal interrupted parsing */
    private static final Record interruptor =
            new Record("dummyID", "dummyBase", new byte[0]);
    // TODO: Extract this from the stream instead of hardcoding
    private static final String HEADER =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    // TODO: Purge double declarations

    private XMLSplitterFilter.Target target;
    private Payload payload;
    private ArrayBlockingQueue<Record> queue;
    private boolean running = false;
    private boolean finished = false;

    private SAXParserFactory factory;
    private SAXParser parser;

    public XMLSplitterParser(XMLSplitterFilter.Target target) {
        this.target = target;
        factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
    }

    /**
     * Shortcut for {@link #openPayload(Payload, int)} with
     * {@link #DEFAULT_MAX_QUEUE}.
     * @param payload  the container for the stream to be parsed.
     * @throws IllegalStateException if a parsing is already underway.
     */
    public synchronized void openPayload(Payload payload) throws
                                                         IllegalStateException {
        openPayload(payload, DEFAULT_MAX_QUEUE);
    }
    /**
     * Opens the stream embedded in the payload and starts the parsing of the
     * content. After this method has been called, Records can be extracted
     * with {@link #next()}.
     * @param payload  the container for the stream to be parsed.
     * @param maxQueue the maximum size for the internal queue of parsed
     *                 Records.
     * @throws IllegalStateException if a parsing is already underway.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public synchronized void openPayload(Payload payload, int maxQueue) throws
                                                         IllegalStateException {
        String LEXICAL_HANDLER =
                "http://xml.org/sax/properties/lexical-handler";
        log.trace("openPayload(" + payload + ") called");
        if (payload.getStream() == null) {
            throw new IllegalArgumentException("No stream in payload '"
                                               + payload + "'");
        }
        if (running) {
            throw new IllegalStateException("Already parsing payload '"
                                            + payload + "'");
        }
        this.payload = payload;
        queue = new ArrayBlockingQueue<Record>(maxQueue);
        try {
            // TODO: Can we reuse a SAXParser?
            parser = factory.newSAXParser();
            // Enable comment preservation
            parser.setProperty (LEXICAL_HANDLER,
                                this);

            // TODO: Handle non-namespaceaware saxparsers better
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Could not instantiate SAXParser due to "
                                       + "configuration exception", e);
        } catch (SAXNotRecognizedException e) {
            throw new IllegalArgumentException("SAXProperty " + LEXICAL_HANDLER
                                               + " not recognized", e);
        } catch (SAXNotSupportedException e) {
            throw new IllegalArgumentException("SAXProperty " + LEXICAL_HANDLER
                                               + " not supported", e);
        } catch (SAXException e) {
            throw new RuntimeException("Could not instantiate SAXParser", e);
        }
        log.trace("Ready to parse");
        finished = false;
        Thread runner = new Thread(this);
        runner.start();
    }

    /**
     * If a parsing is underway, it is stopped and queued Records are discarded.
     */
    public synchronized void stopParsing() {
        log.trace("stopParsing called");
        running = false;
        queue.clear();
        queue.add(interruptor);
        // TODO: More aggressive abort of SAXParser?
    }

    /**
     * This method is controlled by {@link #openPayload}.
     * Do not call this method outside of XMLSplitterParser!
     */
    public void run() {
        log.trace("run called");
        running = true;
        try {
            prepareScanForNextRecord();
            parser.parse(payload.getStream(), this);
        } catch (IOException e) {
            //noinspection DuplicateStringLiteralInspection
            log.warn("IOException during parse of payload '" + payload
                      + "'. Closing stream and exiting", e);
        } catch (SAXException e) {
            //noinspection ObjectEquality
            if (e == stopped) {
                log.info("Aborting parser as stopParsing was called");
            } else {
                //noinspection DuplicateStringLiteralInspection
                log.warn("SAXException during parse of payload '" + payload
                          + "'. Closing stream and exiting", e);
            }
        } catch (Exception e) {
            //noinspection DuplicateStringLiteralInspection
            log.error("Unexpected Exception during parse of payload '" + payload
                      + "'. Closing stream and exiting", e);
        }
        finished = true;
        payload.close();
        running = false;
        if (queue.isEmpty()) {
            // Add the interruptor to abort any waiting next()-calls
            queue.add(interruptor);
        }
        log.trace("run finished");
    }

    /* Iterator interface */

    public synchronized boolean hasNext() {
        log.trace("hasNext() called");
        long endTime = System.currentTimeMillis() + QUEUE_TIMEOUT;
        while (System.currentTimeMillis() < endTime) {
            if (finished) {
                //noinspection ObjectEquality
                return queue.size() > 0 && queue.peek() != interruptor;
            }

            if (queue.size() > 0) {
                //noinspection ObjectEquality
                return queue.peek() != interruptor;
            }

            Record record = queue.peek();
            if (record != null) {
                //noinspection ObjectEquality
                return record != interruptor;
            }

            try {
                Thread.sleep(HASNEXT_SLEEP);
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for Record in hasNext(). "
                          + "Returning false", e);
                return false;
            }
        }
        log.warn("hasNext taited more than '" + QUEUE_TIMEOUT
                  + "' ms for status and got none. Returning false");
        return false;
    }
    public synchronized Payload next() {
        log.trace("next() called");
        if (!hasNext()) {
            throw new NoSuchElementException("No more Records for the current "
                                             + "stream");
        }
        while (!(finished && queue.size() == 0)) {
            try {
                log.trace("next: Polling for Record with timeout of "
                          + QUEUE_TIMEOUT + " ms");
                Record record =
                        queue.poll(QUEUE_TIMEOUT, TimeUnit.MILLISECONDS);
                if (record == null) {
                    throw new NoSuchElementException("Waited more than '"
                                                     + QUEUE_TIMEOUT
                                                     + "' ms for Record and"
                                                     + " got none");
                }
                //noinspection ObjectEquality
                if (record == interruptor) { // Hack
                    throw new NoSuchElementException("Parsing interrupted, no "
                                                     + "more elements");
                }
                log.trace("Got record. Constructing and returning Payload");
                Payload newPayload = payload.clone();
                newPayload.setRecord(record);
                return newPayload;
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for Record. Retrying");
            }
        }
        throw new NoSuchElementException("Expected more Records, but got none");
    }
    public void remove() {
        log.warn("Remove not supported by XMLSplitterParser");
    }

    /* DefaultHandler overrides */
    private List<String> insideRecordElementStack = new ArrayList<String>(20);
    private List<String> outsideRecordPrefixStack = new ArrayList<String>(20);
    private List<String> insideRecordPrefixStack = new ArrayList<String>(20);

    /* Inside a record-block */
    private boolean inRecord;
    /* Inside an id-block */
    private boolean inId;
    /* Record content */
    private StringWriter sw;
    private StringWriter id;

    private void prepareScanForNextRecord() {
        inRecord = false;
        inId = false;
        sw = new StringWriter(10000);
        id = new StringWriter(100);
        insideRecordPrefixStack.clear();
    }


    public void startPrefixMapping (String prefix, String uri) throws
                                                                  SAXException {
        checkRunning();
        String expanded = ("".equals(prefix) ? "xmlns" : "xmlsn:" + prefix)
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
            prefixes = outsideRecordPrefixStack;
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
        if (equalsAny(target.idElement, qName, local)) {
            // We have an ID
            inId = true;
        }
        for (int i = 0 ; i < atts.getLength() ; i++) {
            sw.append(" ").append(atts.getQName(i)).append("=\"");
            sw.append(atts.getValue(i)).append("\"");
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
                // TODO: Handle target.collapsePrefix
                Record record = new Record(target.idPrefix + id.toString(),
                                           target.base,
                                           (HEADER + sw.toString()).
                                                   getBytes("utf-8"));
                log.debug("Produced record " + record);
                queue.add(record);
                prepareScanForNextRecord();
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Unable to convert string to utf-8 "
                                           + "bytes: '" + sw.toString() + "'",
                                           e);
            }
        }
    }

    public void characters (char ch[], int start, int length) throws
                                                              SAXException {
        checkRunning();
        if (!inRecord) {
            return;
        }
        String chars = new String(ch, start, length);
        if (inId) {
            id.append(chars);
        }
        sw.append(chars);
    }


    public void ignorableWhitespace (char ch[], int start, int length) throws
                                                                  SAXException {
        characters(ch, start, length);
    }

    public void comment (char ch [], int start, int length) throws
                                                           SAXException {
        checkRunning();
        if (!inRecord) {
            return;
        }
        String chars = new String(ch, start, length);
        sw.append("<!--").append(chars).append("-->");
    }

    public void startCDATA () throws SAXException {
        checkRunning();
        if (!inRecord) {
            return;
        }
        sw.append("<![CDATA[");
    }
    public void endCDATA () throws SAXException {
        checkRunning();
        if (!inRecord) {
            return;
        }
        sw.append("]]>");
    }

    private static final SAXException stopped = new SAXException("Stopped");
    private void checkRunning() throws SAXException {
        if (!running) {
            throw stopped;
        }
    }
}
