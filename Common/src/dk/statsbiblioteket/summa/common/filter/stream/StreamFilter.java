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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
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
 * STREAM: (HEAD BODY)*EOF
 * HEAD: ID-Size(long) ID(Size bytes of ID-String in utf-8)
 * BODY: Content-Size(long) Content(Size bytes)
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
 * @deprecated {@link ObjectFilter} has been extended to handle streams and
 *             provides a more flexible interface.
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
        return readLong(this);
    }

    private static long readLong(InputStream in) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(8);
        for (int i = 0 ; i < 8 ; i++) {
            int value = in.read();
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
    protected static byte[] longToBytes(long value) {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(value);
        return bb.array();
    }

    /**
     * Default implementation of pump(). Requests data from read() and returns
     * whether the result was EOF or not.
     * @return true if more data is available.
     * @throws IOException in case of read error.
     */
    public boolean pump() throws IOException {
        return read() != EOF;
    }


    /**
     * Helper class for handling construction and parsing of header-information
     * to and from bytes.
     */
    public static class MetaInfo {
        private String id;
        private long contentLength;

        /**
         * Create a MetaInfo, ready for appending header-information through
         * the method {@link #appendHeader}.
         * @param id            an id for a stream.
         * @param contentLength the length in bytes of the stream.
         */
        public MetaInfo(String id, long contentLength) {
            this.id = id;
            this.contentLength = contentLength;
        }

        /**
         * Extracts id and contentLength from the given stream.
         * @param stream a stream in the format specified by StreamFilter.
         * @throws IOException in case of read errors.
         * @return a MetaInfo with id and content length if possible. null is
         *         returned if the stream has reached EOF.
         */
        public static MetaInfo getMetaInfo(InputStream stream) throws IOException {
            try {
                long idLength = readLong(stream);
                ByteArrayOutputStream idStream =
                        new ByteArrayOutputStream((int)idLength);
                for (int i = 0 ; i < idLength ; i++) {
                    idStream.write(stream.read());
                }
                String id = idStream.toString("utf-8");
                long contentLength = readLong(stream);
                return new MetaInfo(id, contentLength);
            } catch (EOFException e) {
                // Consider making a more proper check for EOF at the beginning
                return null;
            }
        }

        /**
         * Convert the data in the header to a Stream, prepends the stream to
         * the given content-stream and returns a new stream made from the
         * coupling (a SequenceInputStream).
         * </p><p>
         * Note: The method available() of the returnes InputStream might
         *       return 0, eventhough there are still bytes to be read.
         * @param content the stream to prepend the header to.
         * @return a stream with meta-data prepended to content.
         */
        public InputStream appendHeader(InputStream content) {
            byte[] idBytes;
            try {
                idBytes = id.getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("utf-8 encoding not supported", e);
            }
            byte[] idSizeBytes = longToBytes(idBytes.length);
            byte[] contentSizeBytes = longToBytes(contentLength);

            byte[] buffer = new byte[idBytes.length + 2 * 8];
            System.arraycopy(idSizeBytes, 0,
                             buffer, 0, idSizeBytes.length);
            System.arraycopy(idBytes, 0,
                             buffer, idSizeBytes.length, idBytes.length);
            System.arraycopy(contentSizeBytes, 0,
                             buffer, idSizeBytes.length + idBytes.length,
                             contentSizeBytes.length);

            InputStream headerStream = new ByteArrayInputStream(buffer);
            return new SequenceInputStream(headerStream, content);
        }

        /* Accessors */
        public String getId() {
            return id;
        }
        public long getContentLength() {
            return contentLength;
        }

    }
}


