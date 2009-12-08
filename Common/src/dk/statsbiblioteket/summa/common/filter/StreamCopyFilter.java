/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2009  The State and University Library
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
package dk.statsbiblioteket.summa.common.filter;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Copies the content if received Streams into memory, closes the source Stream
 * and creates a new Stream from the content in memory.
 * The standard use case is a source that needs Streams to be closed, before it
 * cal deliver more Streams.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_OK,
        author = "te",
        reviewers = "mke")
public class StreamCopyFilter extends ObjectFilterImpl {

    /**
     * If true, a warning is logged when a Payload with no Stream is
     * encountered. If false, nothing happens.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_WARN_ON_NO_STREAM =
            "common.streamcopy.warnonnostream";
    public static final boolean DEFAULT_WARN_ON_NO_STREAM = true;

    private boolean warnOnNoStream = DEFAULT_WARN_ON_NO_STREAM;

    public StreamCopyFilter(Configuration conf) {
        super(conf);
        warnOnNoStream = conf.getBoolean(
                CONF_WARN_ON_NO_STREAM, warnOnNoStream);
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (payload.getStream() == null) {
            if (warnOnNoStream) {
                Logging.logProcess("StreamCopyFilter", "No Stream in Payload",
                                   Logging.LogLevel.WARN, payload);
            }
            return true;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(1000);
        try {
            Streams.pipe(payload.getStream(), out); // Auto-closes both Streams
        } catch (IOException e) {
            throw new PayloadException("Unable to copy Stream", e, payload);
        }
        // TODO: Consider reusing the internal byte array from out
        payload.setStream(new ByteArrayInputStream(out.toByteArray()));
        return true;
    }

}
