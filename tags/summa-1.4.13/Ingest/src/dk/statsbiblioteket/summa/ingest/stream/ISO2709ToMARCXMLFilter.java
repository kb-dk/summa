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
package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc4j.MarcReader;
import org.marc4j.MarcWriter;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.Record;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wrapper for marc4j that takes an InputStream with MARC in ISO 2709 and
 * converts it to MARC21/Slim Stream.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ISO2709ToMARCXMLFilter extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(ISO2709ToMARCXMLFilter.class);

    /**
     * The charset to use when reading the InputStream. Supported values are
     * {@code utf-8}}}, {{{iso-8859-1}}} and {{{marc-8}}}.
     * </p><p>
     * Optional. If not defined, the charset is inferred by marc4j.
     */
    public static final String CONF_INPUT_CHARSET =
            "summa.iso2709.input.charset";

    private String inputcharset = null; // null = let marc4j handle this

    public ISO2709ToMARCXMLFilter(Configuration conf) {
        super(conf);
        inputcharset = conf.getString(CONF_INPUT_CHARSET, null);
        /* We go out of our way to parse the given charset as marc4j
           does not conform to the rules in Java Charset.
         */
        if ("".equals(inputcharset)) {
            inputcharset = null;
        }
        log.debug("Constructed ISO 2709 filter with charset "
                  + (inputcharset == null ? "inferred from the InputStream"
                     : "'" + inputcharset + "'"));
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (payload.getStream() == null) {
            throw new PayloadException("No Stream", payload);
        }
        Logging.logProcess(
                "ISO2709ToMARCXMLFilter", "Parsing Payload ISO 2709 Stream",
                Logging.LogLevel.TRACE, payload);
        payload.setStream(new ISO2MARCInputStream(payload.getStream()));
        log.trace("Wrapped in ISO2MARCInputStream and assigned to " + payload);
        return true;
    }

    // Not thread-safe!
    class ISO2MARCInputStream extends InputStream {
        private MarcReader source;
        private InputStream sourceStream;

        private ByteArrayOutputStream outStream =
                new ByteArrayOutputStream(4000);

        private byte[] buffer = new byte[0];
        private int pos = 0;
        private int length = 0;

        private boolean closed = false;

        /**
         *
         * @param stream the Stream with the ISO 2709 bytes.
         */
        ISO2MARCInputStream(InputStream stream) {
            source = inputcharset == null
                     ? new FlexibleMarcStreamReader(stream)
                     : new FlexibleMarcStreamReader(stream, inputcharset);
            log.trace("Constructed reader");
            sourceStream = stream;
        }

        @Override
        public int read() throws IOException {
            while (true) {
                if (length - pos > 0) { // Buffer has content
                    return buffer[pos++];
                }
                if (closed || !source.hasNext()) { // No more content
                    return -1; // EOF
                }
                try {
                    fillBuffer();
                } catch (Exception e) {
                    String message = "Exception while transforming ISO 2709 "
                                     + "into MARC21Slim";
                    log.warn(message, e);
                    sourceStream.close();
                    throw new IOException(message, e);
                }
            }
        }

        private MarcWriter out = new MarcXmlWriter(outStream, "UTF-8", true);
        // Assumes that the buffer has been depleted
        private void fillBuffer() throws IOException {
            pos = 0;
            while (outStream.size() == 0) {
                if (!source.hasNext()) {
                    sourceStream.close();
                    out.close();
                    closed = true;
                    break;
                }
                Record marcRecord = source.next();
                if (marcRecord == null) {
                    log.debug("fillBuffer(): Got null MARC Record from Stream");
                    continue;
                }
                out.write(marcRecord);
                if (outStream.size() == 0) {
                    log.trace("fillBuffer(): No content in outStream after "
                              + "producing XML for MARC Record (probably due to"
                              + " caching). Processing next Record");
                }
            }
            if (outStream.size() == 0) {
                log.trace("Depleted InputStream with no extra content");
                sourceStream.close();
                out.close();
                length = 0 ;
                closed = true;
                return;
            }

            log.trace("fillBuffer produced " + outStream.size() + " bytes");
            length = outStream.size();
            pos = 0;
            buffer = outStream.toByteArray();
            outStream.reset();
            try {
                log.trace("fillBuffer(): Dumping the first 100 bytes:\n"
                          + new String(buffer, 0, Math.min(100, length),
                                       "utf8"));
            } catch (Exception e) {
                log.debug("Exception performing trace", e);
            }
        }

        @Override
        public void close() throws IOException {
            log.debug("Closing Stream explicitly (this might result in loss of"
                      + " data)");
            sourceStream.close();
            closed = true;
        }

        // TODO: Implement buffer-read for better performance
    }
}
