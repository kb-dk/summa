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
package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Adds a Record to the Payload with the content from the given Stream.
 * </p><p>
 * This class is expected to be deprecated in Summa 2.0.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class StreamToContentFilter extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(StreamToContentFilter.class);

    /**
     * Assigns the given base to created Records.
     * </p><p>
     * Optional. Default is blank.
     */
    public static final String CONF_BASE = "summa.streamtocontent.base";
    public static final String DEFAULT_BASE = "";

    private String base = DEFAULT_BASE;

    public StreamToContentFilter(Configuration conf) {
        super(conf);
        base = conf.getString(CONF_BASE, base);
        log.debug(String.format("Filter created with base='%s'", base));
    }

    @Override
    public String getName() {
        return "StreamToContentFilter";
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (payload.getStream() == null) {
            Logging.logProcess(
                    getName(), "No Stream, so no Record can be created",
                    Logging.LogLevel.TRACE, payload);
            return true;
        }
        if (payload.getRecord() != null) {
            Logging.logProcess(
                    getName(), "Existing Record will be discarded",
                    Logging.LogLevel.TRACE, payload);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(1000);
        try {
            Streams.pipe(payload.getStream(), out); // Auto-closes both Streams
        } catch (IOException e) {
            throw new PayloadException("Unable to copy Stream to content",
                                       e, payload);
        }
        Record record = new Record(constructID(payload), base, out.toByteArray());
        payload.setRecord(record);
        payload.setStream(null);

        Logging.logProcess(
                getName(), "Assigned content of Stream to Record",
                Logging.LogLevel.TRACE, payload);
        return true;
    }

    private String constructID(Payload payload) {
        //noinspection DuplicateStringLiteralInspection
        return payload.getId() == null
               ? "Dummy_" + Long.toString(System.currentTimeMillis())
               : payload.getId();
    }
}