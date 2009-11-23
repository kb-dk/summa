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
 * than the LineInputStream due to the need for look-ahead for the CR+LF
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

    private boolean eofReached = false; // EOF for source. pending possible
    private InputStream source;
    /* If -1, no characters are pending. If >= 0, the corresponding character is
       waiting to be delivered. pending will never be LF.
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
     * delimited by LF, CR or CR+LF.
     * @return the next line or null if EOF is reached.
     * @throws IOException if the source stream threw an I/O-exception.
     */
    public String readLine() throws IOException {
        buffer.reset();
        int read = 0;
        int b;
        while ((b = read()) != -1) {
            read++;
            if (b == CR) { // Reached EOL, check for LF
                if ((b = read()) == -1) { // EOF
                    break;
                } else if (b != LF) {
                    pending = b; // The next is neither EOF or LF so remember it
                }
                break;
            } else if (b == LF) { // Reached EOL. Discard the LF
                break;
            }
            buffer.write(b);
        }
        return read == 0 ? null : buffer.toString(charset);
    }

    @Override
    public int read() throws IOException {
        if (pending != -1) { // pending
            int toDeliver = pending;
            pending = -1;
            return toDeliver;
        }
        if (eofReached) { // EOF
            return -1;
        }
        // read new
        int toDeliver = source.read();
        eofReached = toDeliver == -1;
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
            if (eofReached) {
                return -1;
            }
            int read = source.read(b, off, len) + 1;
            if (read == 0) {
                eofReached = true;
            }
            return read;
        }
        if (eofReached) {
            return -1;
        }
        int read = source.read(b, off, len);
        if (read == 0) {
            eofReached = true;
        }
        return read;
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
        log.trace("Close called");
        source.close();
        pending = -1;
        eofReached = true;
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
