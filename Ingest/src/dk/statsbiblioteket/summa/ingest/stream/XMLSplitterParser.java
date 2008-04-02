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
    /* We feed this to the queue to signal interrupted parsing */
    private static final Record interruptor =
            new Record("dummyID", "dummyBase", new byte[0]);

    private XMLSplitterFilter.Target target;
    private Payload payload;
    private ArrayBlockingQueue<Record> queue;
    private boolean running = false;
    private boolean finished = false;
    private Thread runner;
    private SAXParser parser;

    public XMLSplitterParser(XMLSplitterFilter.Target target) {
        this.target = target;
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
    public synchronized void openPayload(Payload payload, int maxQueue) throws
                                                         IllegalStateException {
        //noinspection DuplicateStringLiteralInspection
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
            parser = SAXParserFactory.newInstance().newSAXParser();
            // TODO: Enable namespaceaware is stated in target
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Could not instantiate SAXParser due to "
                                       + "configuration exception", e);
        } catch (SAXException e) {
            throw new RuntimeException("Could not instantiate SAXParser", e);
        }
        log.trace("Ready to parse");
        finished = false;
        runner = new Thread(this);
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
            parser.parse(payload.getStream(), this);
        } catch (IOException e) {
            //noinspection DuplicateStringLiteralInspection
            log.error("IOException during parse of payload '" + payload
                      + "'. Closing stream and exiting", e);
        } catch (SAXException e) {
            //noinspection DuplicateStringLiteralInspection
            log.error("SAXException during parse of payload '" + payload
                      + "'. Closing stream and exiting", e);
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
        // TODO: Problem: This can return true on no Records with valid XML
        //noinspection ObjectEquality
        return !(finished && queue.isEmpty() || queue.peek() == interruptor);
    }
    public synchronized Payload next() {
        log.trace("next() called");
        if (!hasNext()) {
            throw new NoSuchElementException("No more Records for the current "
                                             + "stream");
        }
        while (!finished) {
            try {
                log.trace("next: Polling for Record with timeout of "
                          + QUEUE_TIMEOUT + " ms");
                Record record =
                        queue.poll(QUEUE_TIMEOUT, TimeUnit.MILLISECONDS);
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
    private List<String> stack = new ArrayList<String>(20);
    public void startElement (String uri, String local, String qName,
                              Attributes atts)  {
        stack.add(qName);
        System.out.println("Found element: '" + local + "', uri '" + uri
                           + "', qName '" + qName 
                           + "', attributes: " + atts.getLength());
    }
    public void endElement (String uri, String localName, String qName) {
        String expected = stack.remove(stack.size()-1);
        if (!expected.equals(qName)) {
            throw new RuntimeException("Miss");
        } else {
            System.out.println("OK " + qName);
        }
    }

    public void characters (char ch[], int start, int length) {
        String chars = new String(ch, start, length);
        if (!"".equals(chars.trim())) {
            System.out.println("'" + chars + "'");
        }
    }

}
