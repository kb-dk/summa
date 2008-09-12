/* $Id: GZipBlob.java,v 1.2 2007/10/04 13:28:17 te Exp $
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
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.io.*;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Utility class for storing a series of objects in a gzipped buffer, either
 * on-disk or in memory.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class GZipBlob<E> implements Iterable<E>, Serializable {

    private byte[] buffer;
    private boolean diskBased;
    private String filename;

    /**
     * Read objects stored in a GZipBlob
     */
    public class GZipBlobIterator<E> implements Iterator<E> {

        InputStream rawIn;
        GZIPInputStream zippedIn;
        ObjectInputStream in;

        E nextItem;

        public GZipBlobIterator (GZipBlob blob) throws IOException {
            rawIn = blob.getRawInputStream();

            try {
                zippedIn = new GZIPInputStream(rawIn);
                in = new ObjectInputStream (zippedIn);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create GZIPInputStream", e);
            }
            prefetch();
        }

        /**
         * set nextItem to the next available item or null,
         * if we are out of items
         */
        private void prefetch () {
            try {
                nextItem = (E) in.readObject();
            } catch (EOFException e){
                // No more elements
                nextItem = null;
            } catch (IOException e) {
                throw new RuntimeException("Broken input stream when reading next object", e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unknow class deteected", e);
            }
        }

        public boolean hasNext() {
                return nextItem != null;
        }

        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException ();
            }

            E res = nextItem;
            prefetch ();

            if (!hasNext()) {
                try {
                    in.close();
                    zippedIn.close();
                    rawIn.close();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to close streams", e);
                }
            }

            return res;
        }

        /**
         * Not supported
         */
        public void remove() {
            throw new UnsupportedOperationException("remove method not available");
        }
    }

    /**
     * Create a new GZipBlob reading all objects from <code>iter</code> into
     * memory.
     *
     * If you wan't to store the gzipped contents of a given file in memory
     * you can use the iterator provided by a {@link Splitter}.
     * @param inData
     * @throws IOException
     */
    public GZipBlob (Iterator<E> inData) throws IOException {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream ();
        GZIPOutputStream zippedOut =  new GZIPOutputStream(outBytes);
        ObjectOutputStream out = new ObjectOutputStream (zippedOut);

        while (inData.hasNext()) {
            out.writeObject(inData.next());
        }

        out.flush();
        zippedOut.finish();
        out.close();
        zippedOut.close();
        outBytes.close();

        buffer = outBytes.toByteArray();
        filename = null;
        diskBased = false;
    }

    /**
     * Create a new GZipBlob reading all objects from an on-disk
     * gzipped file into memory. You can use this constructor
     * to reload a file created by {@link #dump}.
     * @param filename
     * @throws IOException
     */
    public GZipBlob (String filename) throws IOException {
        buffer = null;
        diskBased = true;
        this.filename = filename;
    }

    /**
     * Create a new GZipBlob reading objects from a gzipped data buffer.
     * A data buffer like this can be obtained via the {@link #getBuffer} method.
     * @param gzippedData byte array containing gzipped java objects
     */
    public GZipBlob (byte[] gzippedData) {
        buffer = gzippedData;
    }

    /**
     * Read input data from an iterator and store the gzipped data in a file.
     * Using this method no more than one E will be kept in memory at any given time.
     * @param inData
     * @param storageFile
     * @param overwrite if false throw a FileAlreadyExistException if the output file already exist
     * @throws FileAlreadyExistException if overwrite is false and the specified output file already exist
     */
    public GZipBlob (Iterator<E> inData, String storageFile, boolean overwrite) throws IOException {
        if (!overwrite) {
            if (new File(storageFile).exists()) {
                throw new FileAlreadyExistException(storageFile);
            }
        }

        FileOutputStream outBytes = new FileOutputStream (storageFile);
        GZIPOutputStream zippedOut =  new GZIPOutputStream(outBytes);
        ObjectOutputStream out = new ObjectOutputStream (zippedOut);

        while (inData.hasNext()) {
            out.writeObject(inData.next());
        }

        out.flush();
        zippedOut.finish();
        out.close();
        zippedOut.close();
        outBytes.close();

        buffer = null;
        filename = storageFile;
        diskBased = true;
    }

    /**
     * Return a list containing uncompressed copies of the items
     * in this blob. The list is an {@link ArrayList} to ensure
     * that it is serializable.
     * @return a list containing the original objects
     */
    public ArrayList<E> expand () {
        ArrayList<E> list = new ArrayList<E>();
        for (E item : this) {
            list.add (item);
        }
        return list;
    }

    /**
     * If the underlying gzipped items are read from disk or memory
     */
    public boolean isDiskBased () {
        return diskBased;
    }

    /**
     * Get an input stream for reading the contents of this blob
     */
    public InputStream getRawInputStream () throws IOException {
        if (diskBased) {
            return new FileInputStream (filename);
        } else {
            return new ByteArrayInputStream (buffer);
        }
    }

    /**
     * Get an array containing the raw gzipped data.
     * If this GZipBlob is reading data from a file,
     * the whole file contents is read into a buffer
     * and returned.
     */
    public byte[] getBuffer () throws IOException {
        if (!diskBased) {
            return buffer;
        } else {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream ();
            FileInputStream file = new FileInputStream(filename);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = file.read(buffer)) > 0) {
                bytes.write (buffer, 0, len);
            }
            return bytes.toByteArray();
        }
    }

    /**
     * Write the raw gzipped contents of the blob to a given file.
     * You can retrieve them by constructing a new GZipBlob passing
     * the filename string as parameter to the constructor.
     * @param filename
     * @param overwrite overwrite the given file if it exist
     * @throws FileAlreadyExistException if not allowed to overwrite and the file already exists
     */
    public void dump (String filename, boolean overwrite) throws IOException {
        File outputFile = new File (filename);

        if (!overwrite) {
            if (outputFile.exists()) {
                throw new FileAlreadyExistException(filename);
            }
        }

        FileOutputStream file = new FileOutputStream(outputFile);
        InputStream contents = getRawInputStream();

        byte[] buffer = new byte[1024];
        int len;
        while ((len = contents.read(buffer)) > 0) {
            file.write (buffer, 0, len);
        }

        contents.close();
        file.flush();
        file.close();
    }

    /**
     * Get an iterator over all stored objects.
     */
    public Iterator<E> iterator() {
        try {
            return new GZipBlobIterator<E>(this);
        } catch (IOException e) {
            throw new RuntimeException("Error creating iterator for GZipBlob", e);
        }
    }

}



