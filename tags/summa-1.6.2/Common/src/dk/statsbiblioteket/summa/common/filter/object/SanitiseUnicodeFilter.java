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
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.util.ConvenientMap;
import dk.statsbiblioteket.summa.common.util.StringMap;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Treats the Record.content or Payload.stream as UTF-8 and ensures that only
 * valid unicode characters pass.
 * </p><p>
 * Note: The current implementation only checks for invalid characters below
 * code point 0x7F.
 * </p><p>
 * @see <a href="http://www.w3.org/TR/REC-xml/#charsets">W3C definition</a>
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

    /**
     * If true, meta-data for Payload and Record is also sanitised.
     * </p><p>
     * Optional. Default is true;
     */
    public static final String CONF_FIX_META = "sanitise.fixmeta";
    public static final boolean DEFAULT_FIXMETA = true;

    private char replacement = '?';
    private boolean replace = true;
    private boolean fixMeta = DEFAULT_FIXMETA;
    private long processed = 0;
    private long fixed = 0;

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
        fixMeta = conf.getBoolean(CONF_FIX_META, fixMeta);
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (fixMeta) {
            fixMeta(payload);
        }
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

    @Override
    public void close(boolean success) {
        super.close(success);
        log.info("Closing down. Fixed invalid code points: "
                 + fixed + "/" + processed);
    }

    private void fixMeta(Payload payload) {
        if (payload.getRecord() != null) {
            StringMap map = payload.getRecord().getMeta();
            Set<String> keys = new HashSet<String>(map.keySet());
            for (String key: keys) {
                map.put(key, fixText(map.get(key)));
            }
        }
        ConvenientMap map = payload.getData();
        Set<String> keys = new HashSet<String>(map.keySet());
        for (String key: keys) {
            Object value = map.get(key);
            if (value instanceof String) {
                map.put(key, fixText((String)value));
            }
        }
    }

    private StringBuffer buffer = new StringBuffer(100);
    // Only illegal chars, no handling of space and tags
    private String fixText(String input) {
        buffer.setLength(0);
        for (char c: input.toCharArray()) {
            if (isValid(c)) {
                buffer.append(c);
            } else if (replace) {
                buffer.append(replacement);
            }
        }
        return buffer.toString();
    }

    private boolean isValid(int i) {
        processed++;
        boolean valid = i == 0x09 || i == 0x0A || i == 0x0D || i >= 0x20;
        if (!valid) {
            fixed++;
        }
        return valid;
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
