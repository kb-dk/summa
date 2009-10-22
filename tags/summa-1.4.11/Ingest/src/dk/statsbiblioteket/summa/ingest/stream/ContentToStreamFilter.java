/* $Id:$
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

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.io.ByteArrayInputStream;

/**
 * Wraps the content of incoming Records in Payloads in an InputStream, which
 * is assigned to the Payload and passed on.
 * </p><p>
 * Note 1: Wrapping as stream implies a potential uncompression of the content,
 * which will take place in RAM. This can effectively inflate the memory-hit
 * taken from the Record.
 * </p><p>
 * Note 2: Existing Streams will be closed and removed.
 * </p><p>
 * This class is expected to be deprecated in Summa 2.0.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ContentToStreamFilter extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(ContentToStreamFilter.class);

    public ContentToStreamFilter(Configuration conf) {
        super(conf);
    }

    @Override
    public String getName() {
        return "ContentToStreamFilter";
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (payload.getRecord() == null) {
            Logging.logProcess(
                    getName(), "No Record, so no Stream can be created",
                    Logging.LogLevel.TRACE, payload);
            return true;
        }
        if (payload.getRecord().getContent(false) == null) {
            Logging.logProcess(
                    getName(), "No Record content, so no Stream can be created",
                    Logging.LogLevel.TRACE, payload);
            return true;
        }
        if (payload.getStream() != null) {
            Logging.logProcess(
                    getName(), "Closing and discarding existing Stream",
                    Logging.LogLevel.TRACE, payload);
            try {
                payload.getStream().close();
            } catch (IOException e) {
                throw new PayloadException(
                        "Exception closing existing stream", e, payload);
            }
        }
        Logging.logProcess(getName(), "Wrapping record content as stream",
                           Logging.LogLevel.TRACE, payload);
        payload.setStream(new ByteArrayInputStream(
                payload.getRecord().getContent(true)));
        if (log.isTraceEnabled()) {
            log.trace("Wrapped content to Stream in " + payload);
        }
        return true;
    }
}
