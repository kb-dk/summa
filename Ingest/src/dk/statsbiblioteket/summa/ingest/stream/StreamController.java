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
package dk.statsbiblioteket.summa.ingest.stream;

import java.util.NoSuchElementException;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

/**
 * Retrieves payloads from a given source and processes the streams from the
 * payloads with a Configuration-specified StreamParser. The StreamParser
 * splits the given streams into Records, wrapped in new Payloads.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
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
            log.trace("makePayload: Payload already assigned");
            return;
        }
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
    }


    public boolean hasNext() {
        checkSource();
        if (payload == null) {
            makePayload();
        }
        return payload != null;
    }

    public Payload next() {
        makePayload();
        Payload newPayload = payload;
        //noinspection AssignmentToNull
        payload = null;
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
        next.close();
        return true;
    }

    public void close(boolean success) {
        if (source == null) {
            log.warn(String.format(
                    "close(%b): Cannot close as no source is specified",
                    success));
        } else {
//            parser.stop(); // It should finish by itself
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
