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

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * Retrieves payloads from a given source and processes the streams from the
 * payloads with a Configuration-specified StreamParser. The StreamParser
 * splits the given streams into Records, wrapped in new Payloads.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class StreamController implements ObjectFilter {
    private static Log log = LogFactory.getLog(StreamController.class);

    /**
     * The class of the {@link StreamParser} to use for the received Streams.
     * </p><p>
     * This property is mandatory.
     */
    public static final String CONF_PARSER =
            "summa.ingest.stream.controller.parser";

    private Payload payload = null;
    private ObjectFilter source;
    protected StreamParser parser;

    public StreamController(Configuration conf) {
        Class<? extends StreamParser> parserClass =
                Configuration.getClass(CONF_PARSER, StreamParser.class,
                                       getDefaultStreamParserClass(), conf);
        log.debug("Creating StreamParser '" + parserClass.getName() + "'");
        parser = Configuration.create(parserClass, conf);
    }

    /**
     * If {@link #payload} is not already assigned, this method tries to
     * generate the next payload, based on data from the source. If payload
     * is already assigned, this method does nothing.
     * </p><p>
     * Newly created payloads will have a reference to the stream and the
     * meta-info from the source, will have a newly created Record and will
     * have no Document assigned.
     */
    private void makePayload() {
        log.trace("makePayload() called");
        checkSource();
        if (payload != null) {
            if (log.isTraceEnabled()) {
                log.trace("makePayload: Payload already assigned with "
                          + payload);
            }
            return;
        }
        long startTime = System.nanoTime();
        try {
            while (payload == null) {
                try {
                    log.trace("makePayload(): Calling parser.hasNext()");
                    if (parser.hasNext()) {
                        log.trace("makePayload(): Calling parser.next()");
                        payload = parser.next();
                        return;
                    } else {
                        log.trace("makePayload(): parser.hasNext() == false");
                    }
                } catch (Exception e) {
                    log.warn("Exception requesting payload from parser, "
                             + "skipping to next stream payload", e);
                    parser.stop();
                }

                if (source.hasNext()) {
                    log.trace("makePayload(): Source hasNext, "
                              + "calling source.next()");
                    Payload streamPayload = source.next();
                    if (streamPayload == null) {
                        log.warn(String.format(
                                "Got null Payload from source %s after hasNext()"
                                + " == true", source));
                    }
                    log.debug("makePayload: Opening source stream payload "
                              + streamPayload);
                    parser.open(streamPayload);
                } else {
                    log.debug("makePayload: No more stream payloads available");
                    return;
                }
            }
        } finally {
            if (log.isTraceEnabled()) {
                log.trace("Requested payload from parser in "
                          + (System.nanoTime() - startTime) + " ns");
            }
        }
    }


    public boolean hasNext() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("hasNext() called");
        checkSource();
        if (payload == null) {
            makePayload();
        }
        log.trace("hasNext() resolved to " + (payload != null));
        return payload != null;
    }

    public Payload next() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("next() called");
        makePayload();
        Payload newPayload = payload;
        //noinspection AssignmentToNull
        payload = null;
        if (log.isTraceEnabled()) {
            try {
                log.trace("next() produced " + newPayload + " with content\n"
                          + newPayload.getRecord().getContentAsUTF8());
            } catch (NullPointerException e) {
                log.warn("NPE while dumping content of " + newPayload, e);
            }
        }
        //  We cannot calls stop yet, as newPayload hasn't been finished
        /*if (payload == null) {
            log.debug("hasNext() is false, calling stop on parser");
            parser.stop();
        } */
        return newPayload;
    }

    public boolean pump() throws IOException {
        if (!hasNext()) {
            return false;
        }
        Payload next = next();
        if (next == null) {
            log.warn("pump: Got null as next after hasNext() was true");
            return false;
        }
        //noinspection DuplicateStringLiteralInspection
        Logging.logProcess("StreamController",
                           "Calling close for Payload as part of pump()",
                           Logging.LogLevel.TRACE, payload);
        next.close();
        return true;
    }

    public void close(boolean success) {
        if (source == null) {
            log.warn(String.format(
                    "close(%b): Cannot close as no source is specified",
                    success));
        } else {
            parser.stop();
            source.close(success);
        }
    }

    public void remove() {
        log.warn("Remove not supported in StreamController");
    }

    private void checkSource() {
        if (source == null) {
            throw new NoSuchElementException(
                    "No source specified for StreamController");
        }
    }
    
    public void setSource(Filter filter) {
        if (!(filter instanceof ObjectFilter)) {
            throw new IllegalArgumentException(
                    "StreamController can only be chained to ObjectFilters");
        }
        log.debug("Assigning source " + source);
        source = (ObjectFilter)filter;
    }

    /**
     * Override this method to bypass the requirement of having to specify
     * the {@link #CONF_PARSER}.
     * @return the default StreamParser class for this controller.
     */
    protected Class<? extends StreamParser> getDefaultStreamParserClass() {
        log.trace("getDefaultStreamParserClass(): returning null");
        return null;
    }
}

