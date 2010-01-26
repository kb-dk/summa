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

import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Low level methods, used for bit-twiddling et al.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class BitUtil {
    //private static Log log = LogFactory.getLog(BitUtil.class);

    /**
     * Read and return the next long from the InputStream. The long is read in
     * big-endian order.
     * @param in the Stream to read from.
     * @return a long constructed from the next 8 bytes in the stream.
     * @throws IOException if a long could not be extracted.
     */
    public static long readLong(InputStream in) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(8);
        for (int i = 0 ; i < 8 ; i++) {
            int value = in.read();
            if (value == Payload.EOF) {
                throw new EOFException("Attempting to read past EOF");
            }
            bb.put((byte)value);
        }
        return bb.getLong(0);
    }

    /**
     * Convertes the given long to an array of bytes of length 8.
     * @param value the long to convert to bytes.
     * @return the long as bytes in big-endian order.
     */
    public static byte[] longToBytes(long value) {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(value);
        return bb.array();
    }
}

