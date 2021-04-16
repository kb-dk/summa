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
import dk.statsbiblioteket.summa.common.util.ReaderInputStream;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Extracts text sans XML-tags. The primary usage is for highlighting.
 * </p><p>
 * The source is either Scream or Record.content, depending on input.
 * </p><p>
 * The "parser" is forgiving and does not fail on invalid XML.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SnippetFilter extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(SnippetFilter.class);

    /**
     * The maximum number of characters to extract.
     * </p><p>
     * Optional. Default is 10000.
     */
    public static final String CONF_MAX_LENGTH = "snippet.maxlength";
    public static final int DEFAULT_MAX_LENGTH = 10000;

    /**
     * If defined, the first x characters (multiple spaces are collapsed and
     * count only as one) are skipped before collecting characters for the
     * snippet.
     */
    public static final String CONF_SKIP_FIRST = "snippet.skipfirst";
    public static final int DEFAULT_SKIP_FIRST = 0;

    /**
     * If defined, skipping is stopped if the given String is encountered.
     * Snippet-collection will commence after the given String has been
     * processed.
     * </p><p>
     * Optional. Default is not defined.
     */
    public static final String CONF_SKIP_BREAKER = "snippet.skipbreaker";
    public static final String DEFAULT_SKIP_BREAKER = null;

    /**
     * Where to store the snippet. This is either a key for meta-data (stored
     * in Payliad if the input is a Stream and Record if the input is
     * Record.content) or the special destinations {@code $CONTENT} or
     * {@code $STREAM} which will store the snippet as either Record.content
     * or Payload Stream. {@code $CONTENT} is only valid if the source is
     * Record.content.
     */
    public static final String CONF_DESTINATION = "snippet.destination";
    public static final String DEFAULT_DESTINATION = "snippet";

    /**
     * If true and the source is Payload Stream, the Stream is kept intact for
     * future use.
     * </p><p>
     * Note: This holds a copy of the data used for the snippet in memory for
     * each live Payload.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_PRESERVE_STREAM = "snippet.preservestream";
    public static final boolean DEFAULT_PRESERVE_STREAM = true;

    public static final String DESTINATION_CONTENT = "$CONTENT";
    public static final String DESTINATION_STREAM = "$STREAM";

    private int maxLength = DEFAULT_MAX_LENGTH;
    private String destination = DEFAULT_DESTINATION;
    private boolean preserve = DEFAULT_PRESERVE_STREAM;
    private int skipFirst = DEFAULT_SKIP_FIRST;
    private String skipBreaker = DEFAULT_SKIP_BREAKER;

    public SnippetFilter(Configuration conf) {
        super(conf);
        maxLength = conf.getInt(CONF_MAX_LENGTH, maxLength);
        destination = conf.getString(CONF_DESTINATION, destination);
        preserve = conf.getBoolean(CONF_PRESERVE_STREAM, preserve);
        skipFirst = conf.getInt(CONF_SKIP_FIRST, skipFirst);
        skipBreaker = conf.getString(CONF_SKIP_BREAKER, skipBreaker);
        if ("".equals(skipBreaker)) {
            log.warn(CONF_SKIP_BREAKER + " was empty. Skip break is disabled");
            skipBreaker = null;
        }
        buffer = new StringBuffer(Math.min(10000, maxLength));
        log.info(String.format(
                Locale.ROOT, "Constructed SnippetFilter with maxLength=%d, destination=%s", maxLength, destination));
    }

    // State for the snippet generator
    private StringBuffer buffer;
    private int skipsLeft = 0;
    private int skipBreakPos = 0;

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (DESTINATION_CONTENT.equals(destination) && payload.getRecord() == null) {
            throw new PayloadException("Destination " + DESTINATION_CONTENT + " requested, but the Payload does not "
                                       + "have a Record", payload);
        }


        CharArrayWriter forward = null;
        if (preserve && payload.getStream() != null) {
            forward = new CharArrayWriter(Math.min(10000, maxLength));
        }

        Reader in;
        in = payload.getStream() != null ?
             new InputStreamReader(payload.getStream(), StandardCharsets.UTF_8) :
             new StringReader(payload.getRecord().getContentAsUTF8());
        skipsLeft = skipFirst;
        skipBreakPos = 0;
        buffer.setLength(0);
        int last = 0;
        int current;
        try {
            while ((current = in.read()) != -1 && buffer.length() < maxLength) {
                preserveChar(forward, current);
                skipBreakCheck(current);
                if (((char) current == '\n') || ((char) current == '\r')) { // Newline to space
                    current = ' ';
                }
                if ((last == ' ' && current == ' ')) { // Skip multiple spaces
                    continue;
                }
                if (current == '<') {
                    if (buffer.length() > 0 && last != ' ') {
                        if (--skipsLeft < 0) {
                            buffer.append(" ");
                        }
                    }
                    //noinspection StatementWithEmptyBody
                    while ((current = in.read()) != -1 && current != '>') {
                        preserveChar(forward, current);
                        skipBreakCheck(current);
                    }
                    preserveChar(forward, current);
                    skipBreakCheck(current);
                    if (current == -1) {
                        break;
                    }
                    current = ' '; // Tags count as space
                } else if (--skipsLeft < 0) {
                    buffer.append((char) current);
                }
                last = current;
            }
            preserveChar(forward, current);
        } catch (IOException e) {
            throw new PayloadException("Unable to extract snippet", e, payload);
        }
        Logging.logProcess("SnippetFilter", "Assigning snippet of length " + buffer.length() + " to " + destination,
                           Logging.LogLevel.TRACE, payload);
        if (DESTINATION_CONTENT.equals(destination)) {
            payload.getRecord().setContent(buffer.toString().getBytes(StandardCharsets.UTF_8), false);
            return true;
        }
        if (DESTINATION_STREAM.equals(destination)) {
            payload.setStream(new ByteArrayInputStream(buffer.toString().getBytes(StandardCharsets.UTF_8)));
            return true;
        }
        if (payload.getStream() != null) {
            payload.getData().put(destination, buffer.toString());
        } else {
            payload.getRecord().getMeta().put(destination, buffer.toString());
        }
        if (forward != null) {
            forward.flush();
            Logging.logProcess("SnippetFilter", "Preserved Stream", Logging.LogLevel.TRACE, payload);
            payload.setStream(new SequenceInputStream(new ReaderInputStream(
                    new StringReader(forward.toString()), "utf-8"), new ReaderInputStream(in, "utf-8")));
        }
        return true;
    }

    private void skipBreakCheck(int ic) {
        if (skipsLeft <= 0 || skipBreaker == null) {
            return;
        }
        if (((char) ic) == skipBreaker.charAt(skipBreakPos)) {
            skipBreakPos++;
            if (skipBreakPos == skipBreaker.length()) {
                log.trace("SkipBreaker " + skipBreaker + " matched");
                skipsLeft = 0;
            }
        } else {
            skipBreakPos = 0; // Start over
        }
    }

    private void preserveChar(Writer forward, int ic) {
        if (forward != null && ic != -1) {
            try {
                forward.write(ic);
            } catch (IOException e) {
                throw new RuntimeException("IOException writing to internal structure", e);
            }
        }
    }
}
