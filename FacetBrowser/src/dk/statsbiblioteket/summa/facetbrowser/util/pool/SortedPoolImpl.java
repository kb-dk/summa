/* $Id: SortedPoolImpl.java,v 1.2 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:21 $
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: SortedPoolImpl.java,v 1.2 2007/10/04 13:28:21 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.util.pool;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.Profiler;

/**
 * Partial implementation of SortedPool to provide load and save.
 */
public abstract class SortedPoolImpl<E extends Comparable>
        implements SortedPool<E> {
    private Log log = LogFactory.getLog(SortedPool.class);
    private static final int VERSION = 1;

    /**
     * Load the index data. Note that the array is one element longer than the
     * number of values. The last entry is the index for the next logical value.
     * @param location the folder for the index data.
     * @param poolName the name of the pool. The index data should be in a
     *                 file with the name [poolName].index.
     * @return the indexes for the stored pool plus the index for the next
     *         logical value.
     * @throws IOException if the index could not be loaded.
     */
    // FIXME: The use of an Object-Stream pollutes the raw bytes
    protected long[] loadIndex(File location, String poolName) throws
                                                               IOException {
        log.debug("Loading indexes for pool '" + poolName + "' at location '"
                 + location + "'");
        if (!location.exists()) {
            throw new IOException("The location '" + location
                                  + "' for pool '" + poolName
                                  + "' does not exist");
        }

        FileInputStream indexIn =
                new FileInputStream(new File(location, poolName + ".index"));
        BufferedInputStream indexBuf = new BufferedInputStream(indexIn);
        ObjectInputStream index = new ObjectInputStream(indexBuf);
        int version = index.readInt();
        if (version != VERSION) {
            throw new IOException("The version for the pool '" + poolName
                                  + "' at location '" + location + "' was "
                                  + version + ". This loader only supports "
                                  + "version " + VERSION);
        }
        int size = index.readInt();
        log.debug("Starting load of " + size + " index data (longs)");
        long[] indexData = new long[size+1];
        long feedback = Math.max(size() / 100, 1);
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(size);
        for (int i = 0 ; i <= size ; i++) {
            if (i % feedback == 0) {
                log.debug("Loaded " + i + "/" + size + " index values. ETA: "
                          + profiler.getETAAsString(true));
            }
            indexData[i] = index.readLong();
            profiler.beat();
        }
        log.trace("Loaded " +  size + " index values for '" + poolName
                  + "', closing streams");
        index.close();
        indexBuf.close();
        indexIn.close();
        log.debug("Finished loading " + size + " index data from pool '"
                  + poolName + "' at location '" + location + "'");
        return indexData;
    }

    // TODO: Handle storing on existing file: Store with new name, then replace?

    public void store(File location, String poolName) throws IOException {
        log.debug("Storing pool '" + poolName + "' to location '"
                 + location + "'");
        if (!location.exists()) {
            throw new IOException("The location '" + location
                                  + "' for pool '" + poolName
                                  + "' does not exist");
        }

        FileOutputStream dataOut =
                new FileOutputStream(new File(location, poolName + ".dat"));
        BufferedOutputStream dataBuf = new BufferedOutputStream(dataOut);
        //ObjectOutputStream data = new ObjectOutputStream(dataBuf);

        FileOutputStream indexOut =
                new FileOutputStream(new File(location, poolName + ".index"));
        BufferedOutputStream indexBuf = new BufferedOutputStream(indexOut);
        ObjectOutputStream index = new ObjectOutputStream(indexBuf);

        index.writeInt(VERSION);
        index.writeInt(size());

        long pos = 0;
        long feedback = Math.max(size() / 100, 1);
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(size());
        for (int i = 0 ; i < size() ; i++) {
            if (i % feedback == 0) {
                log.debug("Stored " + i + "/" + size() + " values. ETA: "
                          + profiler.getETAAsString(true));
            }
            index.writeLong(pos);
            pos += writeValue(dataBuf, getValue(i));
            profiler.beat();
        }
        index.writeLong(pos); // Index for next logical, but non-existing, value
        log.trace("Stored all values for '" + poolName + "', closing streams");
//        data.flush();
//        data.close();
        dataBuf.close();
        dataOut.close();
        index.flush();
        index.close();
        indexBuf.close();
        indexOut.close();
        log.debug("Finished storing pool '" + poolName + "' to location '"
                  + location + "'");
    }

    /**
     * Removes duplicates from the index. This requires the index to be sorted.
     * It is not guaranteed that the duplicates are removed from memory or
     * storage, only that they appear to the caller as removed.
     * </p><p>
     * Note: Implementations should avoid populating with duplicates during
     *       normal adds. This method is only meant for cleanup after bulk
     *       operations, that does not guarantee consistency.
     */
    protected void removeDuplicates() {
        log.trace("Removing duplicated");
        int initial = size();
        E last = null;
        int index = 0;
        while (index < size()) {
            E current = getValue(index);
            if (last != null && last.equals(current)) {
                if (log.isTraceEnabled()) {
                    log.trace("Removing duplicate '" + current + "'");
                }
                remove(index);
            } else {
                index++;
            }
            last = current;
        }
        log.debug("Removed " + (initial - size())
                  + " duplicates from a total of " + initial + " values");

    }


    /**
     * Write the given value to the stream and return the number of bytes that
     * was written.
     * @param out   the stream to write to.
     * @param value the value to write.
     * @return the number of bytes that was written.
     * @throws IOException is the value could not be written.
     */
    protected int writeValue(BufferedOutputStream out, E value) throws
                                                              IOException {
        try {
            byte[] buffer = valueToBytes(value);
            out.write(buffer);
            return buffer.length;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding problem in writeValue(out, '"
                                       + value + "')", e);
        }
    }

    /**
     * Converts the given value to an array of bytes, suitable for storage.
     * The conversion should mirror {@link #bytesToValue}.
     * @param value the value to converto to bytes.
     * @return bytes based on values.
     */
    protected abstract byte[] valueToBytes(E value);

    /**
     * Converts the given bytes to a value. The conversion should mirror
     * {@link #valueToBytes}.
     * @param buffer the bytes to convert.
     * @param length the number of bytes to convert.
     * @return a value constructed from the given bytes.
     */
    protected abstract E bytesToValue(byte[] buffer, int length);
}
