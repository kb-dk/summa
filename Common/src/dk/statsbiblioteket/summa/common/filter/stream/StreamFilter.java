/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
package dk.statsbiblioteket.summa.common.filter.stream;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;

/**
 * On an abstract level, ingesting is just a chain of filters. In the beginning
 * of the chain is a filter that extracts raw data from a source (such as XML
 * files on the local file system), then follows chains that transforms,
 * ingests and index the data. Normally the chain will consist of StreamFilters
 * followed by {@link ObjectFilter}s, but that is not a requirement.
 * </p><p>
 * The format for the top-level datastream is as follows:<br />
 * {@code
 * STREAM: (BODY)*EOF
 * BODY: Length(long) Content(Length bytes)
 * EOF: -1, as used in the standard InputStream.
 * }
 * </p><p>
 * An example of use could be a file loader that scans for multiple files,
 * reads the content sequentially and sends a stream made up of a body for
 * each file read. If the length of the content is unknown at load-time,
 * the filter must state Long.MAX_VALUE as the length. Readers must then
 * continue to read content until EOF is reached after which the filter is
 * considered depleted.
 * </p><p>
 * If EOF is reached and a read is performed, the implementation must continue
 * to return EOF.
 * </p><p>
 * An overall principle for filters is that they should only fail in case of
 * catastrophic events, such as out of memory. If the input is not as expected,
 * the filter should skip corrupt input (with appropriate logging of errors)
 * and attempt to continue processing further data.
 */
public abstract class StreamFilter extends InputStream implements Configurable,
                                                                  Filter {
    /**
     * EOF should be returned by read() when the filter is depleted.
     */
    public static final int EOF = -1;

    /**
     * @return the next long in the stream if present. If EOF was reached
     *         during read, an exception is thrown.
     * @throws IOException  if a fatal error occured during read.
     * @throws EOFException if EOF was reached during read.
     */
    public long readLong() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(8);
        for (int i = 0 ; i < 8 ; i++) {
            int value = read();
            if (value == EOF) {
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
    protected byte[] longToBytes(long value) {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(value);
        return bb.array();
    }
}