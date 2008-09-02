/* $Id: Splitter.java,v 1.2 2007/10/04 13:28:17 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:17 $
 * $Author: te $
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
package dk.statsbiblioteket.summa.dice.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.lang.reflect.Array;
import java.io.*;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Utility class to help splitting files and byte arrays into smaller parts.
 * It also provides the option to reassemble the parts in a file via the
 * {@link #collect} method.
 * The splits created via this class does not reflect the internal
 * file structure in any way. It simply cuts the file in equally sized
 * chunks and provides acces through an iterator.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class Splitter implements Iterable<byte[]>, Serializable {

    private int splitSize;
    private String filename;
    byte[] buffer;
    boolean diskBased;

    /**
     * Iterator cutting up the file provided by a {@link Splitter}
     */
    public class SplitIterator implements Iterator<byte[]> {

        private Object buffer;
        private PushbackInputStream in;
        private int lastRead;

        public SplitIterator (Splitter splitter) {
            buffer = Array.newInstance (byte.class, splitter.getSplitSize());
            try {
                in = new PushbackInputStream(splitter.getInputStream());
            } catch (FileNotFoundException e) {
                throw new RuntimeException("File to split not found: " + splitter.getFileName() + ". This should never happen.", e);
            }
            lastRead = ((byte[])buffer).length;
        }

        public boolean hasNext() {
            try {
                // Peek the next byte of the input stream
                byte[] peek = new byte[1];
                boolean result = true;
                int numRead = in.read(peek);
                if (numRead == -1) {
                    result = false;
                }
                in.unread(peek);
                return result;
            } catch (IOException e) {
                throw new RuntimeException("Failed to peek stream", e);
            }
        }

        public byte[] next() {
            try {
                lastRead = in.read ((byte[])buffer);
            } catch (IOException e) {
                throw new RuntimeException("InputStream broken. Failed to read from file.");
            }

            if (lastRead == -1) {
                throw new NoSuchElementException("No more splits to be read, end of file.");
            }

            else if (lastRead < ((byte[])buffer).length) {
                // this is the last read, trim the buffer to avoid trailing garbage
                Object trim = Array.newInstance (byte.class, lastRead);
                System.arraycopy(buffer, 0, trim, 0, lastRead);
                return (byte[])trim;
            }

            return (byte[])buffer;
        }

        public void remove() {
            throw new UnsupportedOperationException("Cannot remove data from Splitter");
        }
    }

    /**
     * Create a splitter splitting the file given by filename.
     * @param filename file to slit
     * @throws FileNotFoundException
     */
    public Splitter (String filename, int splitSize) throws FileNotFoundException {
        this.splitSize = splitSize;
        this.filename = filename;

        if (! new File(filename).exists()) {
            throw new FileNotFoundException("File to split not found: " + filename);
        }

        buffer = null;
        diskBased = true;
    }

    /**
     * Create a splitter splitting an array of bytes.
     * @param bytes array to split
     */
    public Splitter (byte[] bytes, int splitSize) {
        this.splitSize = splitSize;
        filename = null;
        buffer = bytes;
        diskBased = false;
    }

    /**
     * Number of bytes per split
     */
    public int getSplitSize () {
        return splitSize;
    }

    public InputStream getInputStream () throws FileNotFoundException {
        if (diskBased) {
            return new FileInputStream(filename);
        } else {
            return new ByteArrayInputStream (buffer);
        }
    }

    /**
     * Filename of the file being split
     */
    public String getFileName () {
        return filename;
    }

    /**
     * Get an iterator splitting the file
     */
    public Iterator<byte[]> iterator() {
        return new SplitIterator (this);
    }

    /**
     * Merge a sequence of byte arrays into one file.
     * {@link #collect}({@link #iterator()}) is guranteed to
     * produce a file identical to the one split by {@link #iterator()}.
     * @param splits iterator returning byte arrays
     * @param outputFilename file to write collected data to
     */
    public static void collect (Iterator splits, String outputFilename) throws IOException {

        if (new File(outputFilename).exists()) {
            throw new FileAlreadyExistException(outputFilename);
        }

        OutputStream out = new FileOutputStream (outputFilename);

        while (splits.hasNext()) {
            byte[] bytes = (byte[]) splits.next();
            out.write (bytes);
        }

        out.flush();
        out.close();
    }

    /**
     * Merge a sequence of byte arrays into one array.
     * {@link #collect}({@link #iterator()}) is guranteed to
     * produce an array identical to the one split by {@link #iterator}.
     * @param splits iterator returning byte arrays
     * @return concatenation of all provided byte arrays
     */
    public static byte[] collect (Iterator splits) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream ();

        while (splits.hasNext()) {
            byte[] bytes = (byte[]) splits.next();
            out.write (bytes);
        }

        out.flush();
        out.close();
        return out.toByteArray();
    }
}
