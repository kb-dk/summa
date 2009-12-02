/* $Id:$
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
import dk.statsbiblioteket.summa.common.filter.Payload;

import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;
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
