/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
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
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.*;

/**
 * Treats the Record.content or Payload.stream as UTF-8 and ensures that only
 * valid unicode characters pass.
 * </p><p>
 * Note: The current implementation only checks for invalid characters below
 * code point 0x7F.
 * </p><p>
 * @see <a href="http://www.w3.org/TR/REC-xml/#charsets">W3C definition</a>.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SanitiseUnicodeFilter extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(SanitiseUnicodeFilter.class);

    /**
     * The replacement character when an invalid unicode character is
     * encountered. This must be a single character below unicode 0x7F.
     * It is possible to specify the empty String as replacement.
     * </p><p>
     * Optional. Default is {@code ?} (codepoint 0x3F).
     */
    public static final String CONF_REPLACEMENT_CHAR = "sanitise.replacement";
    public static final String DEFAULT_REPLACEMENT_CHAR = "?";

    private char replacement = '?';
    private boolean replace = true;

    public SanitiseUnicodeFilter(Configuration conf) {
        super(conf);
        feedback = false;
        String re = conf.getString(
            CONF_REPLACEMENT_CHAR, DEFAULT_REPLACEMENT_CHAR);
        if ("".equals(re)) {
            replace = false;
        } else if (re.length() > 1 || re.charAt(0) > 0x7F) {
            throw new ConfigurationException(String.format(
                "The replacement character with key %s was '%s'. It must be of"
                + " length 1 or less and must be at code point 0x7F or less",
                CONF_REPLACEMENT_CHAR, re));
        } else {
            replacement = re.charAt(0);
        }
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (payload.getRecord() != null) {
            byte[] content = payload.getRecord().getContent();
            if (content == null || content.length == 0) {
                Logging.logProcess("SanitiseContent", "Content was empty",
                                   Logging.LogLevel.TRACE, payload);
                return true;
            }
            InputStream is = new ByteArrayInputStream(content);
            InputStream fixed = new FixUnicodeStream(is);
            ByteArrayOutputStream out =
                new ByteArrayOutputStream(content.length);
            try {
                Streams.pipe(fixed, out);
            } catch (IOException e) {
                throw new PayloadException(
                    "Unable to pipe content through FixUnicodeStream",
                    e, payload);
            }

            payload.getRecord().setContent(out.toByteArray(), false);
            Logging.logProcess("SanitiseContent", "Sanitised content",
                               Logging.LogLevel.TRACE, payload);
            return true;
        }
        if (payload.getStream() == null) {
            throw new PayloadException(
                "Neither Record nor Stream in Payload", payload);
        }
        payload.setStream(new FixUnicodeStream(payload.getStream()));
        Logging.logProcess("SanitiseContent",
                           "Wrapped Payload.stream in FixUnicodeStream",
                           Logging.LogLevel.TRACE, payload);
        return true;
    }

    private class FixUnicodeStream extends InputStream {
        private InputStream source;

        private FixUnicodeStream(InputStream source) {
            this.source = source;
        }

        @Override
        public int read() throws IOException {
            int c;
            while ((c = source.read()) != -1 && c <= 0x7F) {
                if (!isValid(c)) {
                    if (replace) {
                        return replacement;
                    }
                    continue;
                }
                break;
            }
            return c;
        }

        private boolean isValid(int i) {
            return i == 0x09 || i == 0x0A || i == 0x0D || i >= 0x20;
        }

        // Delegation

        @Override
        public void close() throws IOException {
            source.close();
        }

        @Override
        public void mark(int readlimit) {
            source.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            source.reset();
        }

        @Override
        public boolean markSupported() {
            return source.markSupported();
        }

        @Override
        public long skip(long n) throws IOException {
            return source.skip(n);
        }

        @Override
        public int available() throws IOException {
            return source.available();
        }
    }
}
