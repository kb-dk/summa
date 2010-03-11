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
public class XMLSplitterParser extends ThreadedStreamParser implements
                                                           XMLSplitterReceiver {
    private static Log log = LogFactory.getLog(XMLSplitterParser.class);

    public static final String LEXICAL_HANDLER =
            "http://xml.org/sax/properties/lexical-handler";

    // TODO: Purge double declarations

    private SAXParserFactory factory;
    private XMLSplitterHandler handler;
    XMLSplitterParserTarget target;
    private long lastRecordStart = System.nanoTime();
    private long thisRunQueued = 0;

    public XMLSplitterParser(Configuration conf) {
        super(conf);
        target = new XMLSplitterParserTarget(conf);
        factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        handler = new XMLSplitterHandler(conf, this, target);
    }

    @Override
    protected void protectedRun() throws Exception {
        thisRunQueued = 0;
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
        log.debug("Finished parsing " + sourcePayload + " with " + thisRunQueued
                  + " records produced");
    }

    public boolean isTerminated() {
        return !running;
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
    public void queueRecord(Record record) {
        thisRunQueued++;
//        try {
            if (log.isTraceEnabled()) {
                //noinspection DuplicateStringLiteralInspection
                log.trace(String.format(
                        "Produced record in %.5f ms: %s. queueing",
                        (System.nanoTime() - lastRecordStart) / 1000000.0,
                        record));
            }
            addToQueue(record);
            lastRecordStart = System.nanoTime();
//        } catch (InterruptedException e) {
//            throw new RuntimeException("Interrupted while adding to queue", e);
//        }
    }
}

