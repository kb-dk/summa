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
package dk.statsbiblioteket.summa.ingest.split;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * Splits a XML document into pieces, validates and handles namespaces.
 * The parser is a helper-class for {@link XMLSplitterFilter} and outputs
 * Records, ready for passing through the chain. 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class XMLSplitterParser extends ThreadedStreamParser {
    private static Log log = LogFactory.getLog(XMLSplitterParser.class);

    // TODO: Purge double declarations

    private SAXParserFactory factory;
    private XMLSplitterHandler handler;
    XMLSplitterParserTarget target;
    private long lastRecordStart = System.nanoTime();

    public XMLSplitterParser(Configuration conf) {
        super(conf);
        target = new XMLSplitterParserTarget(conf);
        factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        handler = new XMLSplitterHandler(conf, this, target);
    }

    @Override
    protected void protectedRun() throws Exception {
        String LEXICAL_HANDLER =
                "http://xml.org/sax/properties/lexical-handler";
        SAXParser parser;
        try {
            // TODO: Can we reuse a SAXParser? Reset?
            log.trace("Constructing new SAXParser");
            parser = factory.newSAXParser();
            handler.resetForNextStream();
            // Enable comment preservation
            parser.setProperty(LEXICAL_HANDLER, handler);

            // TODO: Handle non-namespaceaware saxparsers better
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Could not instantiate SAXParser due to "
                                       + "configuration exception", e);
        } catch (SAXNotRecognizedException e) {
            throw new IllegalArgumentException(String.format(
                    "SAXProperty %s not recognized", LEXICAL_HANDLER), e);
        } catch (SAXNotSupportedException e) {
            throw new IllegalArgumentException(String.format(
                    "SAXProperty %s not supported", LEXICAL_HANDLER), e);
        } catch (SAXException e) {
            throw new RuntimeException("Could not instantiate SAXParser", e);
        }
        log.trace("Ready to parse");
        handler.resetForNextRecord();
        lastRecordStart = System.nanoTime();
        parser.parse(sourcePayload.getStream(), handler);
        log.debug("Finished parsing " + sourcePayload);
    }

    private static final SAXException stopped =
            new SAXException("Parser stop requested");
    void checkRunning() throws SAXException {
        if (!running) {
            throw stopped;
        }
    }

    @Override
    protected void postProcess(Payload payload) {
        target.adjustID(payload);
    }

    /**
     * Insert the given Record into the queue. This will block if the queue is
     * full.
     * @param record the Record to insert.
     */
    void queueRecord(Record record) {
        try {
            if (log.isTraceEnabled()) {
                //noinspection DuplicateStringLiteralInspection
                log.trace(String.format(
                        "Produced record in %.5f ms: %s. queueing",
                        (System.nanoTime() - lastRecordStart) / 1000000.0,
                        record));
            }
            queue.put(record);
            lastRecordStart = System.nanoTime();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while adding to queue", e);
        }
    }
}
