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

import java.io.InputStream;
import java.io.Reader;
import java.io.IOException;

/**
 * Wrapper for a Reader that converts the characters into bytes. Note that this
 * involves a fair amount of small String and byte[] allocations which might tax
 * the garbage collector.
 * </p><p>
 * Important: This InputStream does not properly handle unicode characters above
 * 65535 (see the JUnit-test). TODO: Correct this shortcoming
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ReaderInputStream extends InputStream {
    private Reader reader;
    private boolean readerEmpty = false;

    private String encoding;

    private byte[] byteBuffer = null;
    private int bufferPos = 0;

    private int READ_AHEAD = 20; // Read minimum this many characters ahead
    private final char[] readAheadBuffer = new char[READ_AHEAD];
    private char[] customBuffer = readAheadBuffer;

    /**
     * Constructs the Reader wrapper.
     * @param reader   the Reader to wrap.
     * @param encoding the encoding to use when converting characters to bytes.
     */
    public ReaderInputStream(Reader reader, String encoding) {
        this.reader = reader;
        this.encoding = encoding;
    }

    @Override
    public int read() throws IOException {
        checkBuffer(1);
        if (byteBuffer != null && bufferPos < byteBuffer.length) {
            return byteBuffer[bufferPos++];
        }
        return -1; // EOF
    }

    /**
     * If possible, ensures that the buffer contains at least this number of
     * bytes. The only excuse for not having the wanted number of bytes in
     * the buffer after this operation is that the underlying Reader has reached
     * End Of File.
     * @param wantedSize the wanted minimum number of bytes in the buffer.
     * @throws java.io.IOException if an I/O error in the Reader occured.
     */
    private void checkBuffer(int wantedSize) throws IOException {
        if (readerEmpty ||
            (byteBuffer != null && byteBuffer.length - bufferPos >= wantedSize)) {
            return;
        }
        // We try to reuse buffers
        char[] readBuffer = wantedSize < READ_AHEAD ? readAheadBuffer :
                            wantedSize == customBuffer.length ? customBuffer :
                            new char[wantedSize];
        int charCount = reader.read(readBuffer);
        readerEmpty = charCount == -1 || charCount == 0;
        byte[] readByteBuffer = readerEmpty ? new byte[0] :
                                new String(readBuffer, 0, charCount).
                                        getBytes(encoding);
        if (byteBuffer != null && bufferPos < byteBuffer.length) {
            byte[] merged = new byte[byteBuffer.length - bufferPos
                                     + readByteBuffer.length];
            System.arraycopy(byteBuffer, 0, merged, 0, 
                             byteBuffer.length - bufferPos);
            System.arraycopy(readByteBuffer, 0, merged,
                             byteBuffer.length - bufferPos,
                             readByteBuffer.length);
            byteBuffer = merged;
        } else {
            byteBuffer = readByteBuffer;
        }
        bufferPos = 0;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
