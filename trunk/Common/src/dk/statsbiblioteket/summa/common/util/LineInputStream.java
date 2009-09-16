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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

/**
 * Wrapper that adds readLine-capabilities to an InputStream. Handles LG, CR
 * and LF+CR line breaks.
 * </p><p>
 * Note that the underlying InputStream might be positioned 1 character further
 * than the LineInputStream due to the need for look-ahead for the LF+CR
 * combination.
 * </p><p>
 * This implementation is not thread-safe.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class LineInputStream extends InputStream {
    private static Log log = LogFactory.getLog(LineInputStream.class);

    public static final String DEFAULT_CHARSET = "utf-8";
    public static final int LF = 0x0A; // 10 dec
    public static final int CR = 0x0D; // 13 dec

    private InputStream source;
    /* If -1, no characters are pending. If >= 0, the corresponding character is
       waiting to be delivered. pending will never be CR.
     */
    private int pending = -1;
    /* If a byte was pending while mark was called, it is remembered for later
       calls to reset().
     */
    private int markedPending = -1;
    private String charset = DEFAULT_CHARSET;

    /**
     * Wraps the source stream and expects the content to be UTF-8.
     * @param source where to read data.
     */
    public LineInputStream(InputStream source) {
        this.source = source;
        log.debug(
                "Constructed LineInputStream with default charset " + charset);
    }

    public LineInputStream(InputStream source, String charset) {
        this.source = source;
        this.charset = charset;
        log.debug("Constructed LineInputStream with charset " + charset);
    }

    private ByteArrayOutputStream buffer = new ByteArrayOutputStream(100);
    /**
     * Read the next line from the source stream. Lines are defined as bytes
     * delimited by LF, CR or LF+CR.
     * @return the next line or null if EOF is reached.
     * @throws IOException if the source stream threw an I/O-exception.
     */
    public String readLine() throws IOException {
        buffer.reset();
        if (pending != -1) {
            if (pending == LF) { // Pending was empty line, check for CR
                pending = source.read();
                if (pending == CR) {
                    pending = -1; // Discard CR
                }
                return "";
            }
            // Pending was something, so we add it and continue reading
            buffer.write(pending);
        }
        int read = 0;
        while ((pending = source.read()) != -1) {
            read++;
            if (pending == LF) { // Reached EOL, check for CR
                pending = source.read();
                if (pending == CR) {
                    pending = -1; // Discard CR
                }
                break;
            } else if (pending == CR) {
                pending = -1;
                break;
            }
            buffer.write(pending);
        }
        return read == 0 && buffer.size() == 0 ? null :
               buffer.toString(charset);
    }

    @Override
    public int read() throws IOException {
        if (pending == -1) {
            return source.read();
        }
        int toDeliver = pending;
        pending = -1;
        return toDeliver;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        if (pending != -1) {
            // TODO: Check if this conversion is correct
            b[0] = (byte)pending;
            pending = -1;
            len--;
            off++;
            return source.read(b, off, len) + 1;
        }
        return source.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        if (n == 0) {
            return 0;
        }
        if (pending != -1) {
            n--;
            pending = -1;
        }
        return source.skip(n);
    }

    @Override
    public int available() throws IOException {
        return pending == -1 ? source.available() : source.available() + 1;
    }

    @Override
    public void close() throws IOException {
        source.close();
        pending = -1;
    }

    @Override
    public void mark(int readlimit) {
        markedPending = pending;
        source.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        pending = markedPending;
        source.reset();
    }

    @Override
    public boolean markSupported() {
        return source.markSupported();
    }
}
