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
package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Simple wrapper for an InputStream that provides callback when the inner
 * stream is depleted or closed.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class CloseCallbackStream extends FilterInputStream {
    private static Log log = LogFactory.getLog(CloseCallbackStream.class);

    private boolean closed = false;
    private final boolean autoClose;

    public CloseCallbackStream(InputStream in) {
        super(in);
        autoClose = true;
    }

    /**
     * @param in        the stream to wrap.
     * @param autoClose if true. the inner stream is automatically closed when
     *                  it is depleted.
     */
    public CloseCallbackStream(InputStream in, boolean autoClose) {
        super(in);
        this.autoClose = autoClose;
    }

    /**
     * Called when the inner InputStream is depleted or close() is called.
     */
    public abstract void callback();

    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            return;
        }
        log.trace("Closing inner stream '" + in + "'");
        super.close();
        closed = true;
        callback();
    }

    @Override
    public int read() throws IOException {
        int result = super.read();
        if (autoClose && result == -1) {
            close();
        }
        return result;
    }

    @Override
    public int read(byte b[]) throws IOException {
        int result = super.read(b);
        if (autoClose && result == -1) {
            close();
        }
        return result;
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        int result = super.read(b, off, len);
        if (autoClose && result == -1) {
            close();
        }
        return result;
    }

    @Override
    public boolean markSupported() {
        // If the Stream is auto-closing, we cannot support marking
        return !autoClose && in.markSupported();
    }

    @Override
    public String toString() {
        return "CloseCallbackStream(" + in + ", autoClose=" + autoClose + ")";
    }

    public boolean isClosed() {
        return closed;
    }
}
