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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides an OutputStream that writes to an internal buffer, which can be
 * read as an InputStream. The internal buffer automatically expands to
 * accommodate input. Reading from the InputStream is blocking.
 * </p><p>
 * Warning: As the internal buffer automatically expands, external checks for
 * memory overflow should be done.
 * </p><p>
 * Optionally the method {@link #addBytesRequest(int)} can be implemented.
 * It is a call-back from the reader when bytes are requested.
 * </p><p>
 * Note: The feeder should call {@link #isClosed()} before writing to the
 *       stream, as the stream can be closed from both ends.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class PipedStream extends InputStream {
    private static Log log = LogFactory.getLog(PipedStream.class);

    private final OutputStream feeder = new InnerOutputStream();
    private static final double EXPAND_FACTOR = 1.2;

    private byte[] buffer = new byte[1000];
    private int pos = 0;    // Read-position
    private int length = 0; // Last valid buffer-entry + 1
    private boolean sourceEOF = false; // Set to true after last source byte
    // or if close() has been called

    /**
     * A request for wanted bytes to be added to the outputstream.
     * @param wanted the wanted number of bytes.
     * @return the number of added bytes or -1 if EOF is reached.
     * @throws java.io.IOException if bytes could not be retrieved or added.
     */
    protected int addBytesRequest(int wanted) throws IOException {
        log.trace("Default addBytesRequest(" + wanted
                  + ") called. No action taken");
        return 0;
    }

    /**
     * @return an OutputStream where writes will be piped to PipedStream.
     */
    public OutputStream getOutputStream() {
        return feeder;
    }

    @Override
    public int read() throws IOException {
        while (true) {
            synchronized (feeder) {
                if (length - pos > 0) { // Buffer has content
                    return buffer[pos++];
                }
                if (sourceEOF) {
                    return -1;
                }
            }
            addBytesRequest(1);
            synchronized (feeder) {
                if (length - pos > 0) { // Buffer has content
                    return buffer[pos++];
                }
                if (sourceEOF) {
                    return -1;
                }
            }
            synchronized (feeder) {
                try {
                    feeder.wait();
                } catch (InterruptedException e) {
                    log.debug("Received interrupted while ");
                }
/*                if (closed || !source.hasNext()) { // No more content
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
                }*/
            }
        }
    }

    @Override
    public void close() throws IOException {
        feeder.close();
        synchronized (feeder) {
            feeder.notifyAll();
        }
    }

    public boolean isClosed() {
        return sourceEOF;
    }

    // TODO: Performance-optimize with array-read

    private class InnerOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            if (sourceEOF) {
                throw new IllegalStateException(
                        "The Stream has been closed");
            }
            synchronized (feeder) {
                if (length == buffer.length) { // We need to make room
                    if (pos > 0) {// Shift
                        System.arraycopy(buffer, pos, buffer, 0, length-pos);
                        length -= pos;
                        pos = 0;
                    } else { // Expand
                        byte[] newBuffer =
                                new byte[(int)(buffer.length * EXPAND_FACTOR)];
                        System.arraycopy(
                                buffer, 0, newBuffer, 0, buffer.length);
                        buffer = newBuffer;
                    }
                }
                buffer[length++] = (byte)(0xFF & b);
                feeder.notifyAll();
            }
        }

        @Override
        public void close() throws IOException {
            sourceEOF = true;
        }
    }
}

