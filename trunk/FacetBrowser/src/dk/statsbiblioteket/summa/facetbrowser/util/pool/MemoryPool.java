/* $Id: MemoryPool.java,v 1.4 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.4 $
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
 * CVS:  $Id: MemoryPool.java,v 1.4 2007/10/04 13:28:21 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.util.pool;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * The MemoryPool stores all values in RAM. It uses binary search for efficient
 * value-based lookups and direct array indexing for position-based lookups.
 * </p><p>
 * Note that MemoryPool isn't synchronised.
 */
public abstract class MemoryPool<E extends Comparable<? super E>> extends
                                                       SortedPoolImpl<E> {
    private Log log = LogFactory.getLog(MemoryPool.class);
    private static final int DEFAULT_SIZE = 1000;
    private static final int MAX_INCREMENT = 1000000;

    protected E[] values;
    protected int valueCount = 0;

    public MemoryPool() {
        log.info("Creating empty pool");
        values = getArray(DEFAULT_SIZE);
    }

    // Remove assumption of sorted strings and use index instead
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public void load(File location, String poolName) throws IOException {
        log.debug("Loading indexes and data for pool '" + poolName
                  + "' at location '" + location + "'");
        long[] index = loadIndex(location, poolName);
        log.trace("Index data loaded for pool '" + poolName
                  + "' at location '" + location + "'");
               
        FileInputStream dataIn =
                new FileInputStream(new File(location, poolName + ".dat"));
        BufferedInputStream dataBuf = new BufferedInputStream(dataIn);
        valueCount = index.length-1;
        log.debug("Starting load of " + valueCount + " values");
        values = getArray(valueCount);

        long feedback = Math.max(valueCount / 100, 1);
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(size());
        for (int i = 0 ; i < size() ; i++) {
            if (log.isTraceEnabled() && i % feedback == 0) {
                log.trace("Loaded " + i + "/" + valueCount + " values. ETA: "
                          + profiler.getETAAsString(true));
            }
            values[i] = readValue(dataBuf, (int)(index[i+1]-index[i]));
            profiler.beat();
        }
        log.trace("Loaded " +  valueCount + " values for '" + poolName
                  + "', closing streams");
        dataBuf.close();
        dataIn.close();
        log.debug("Finished loading " + valueCount + " values from pool '"
                  + poolName + "' at location '" + location + "'");
    }

    private byte[] buffer = new byte[1024];
    /**
     * Read a value from the given stream. The length indicated the number of
     * bytes that the value occupies. It is the responsibility of the
     * implementing class to read exactly length bytes.
     * @param in     the stream to read from.
     * @param length the number of bytes to read.
     * @return a value deserialised from the bytes read from the stream.
     * @throws IOException if the value could not be read from the stream.
     */
    @QAInfo(state=QAInfo.State.QA_NEEDED, level=QAInfo.Level.PEDANTIC,
            author="te", reviewers={"mke"},
            comment="Check that in.read works as expected")
    protected E readValue(BufferedInputStream in, int length) throws IOException {
        log.trace("Reading value data from stream with length " + length);
        if (buffer.length < length) {
            buffer = new byte[length];
        }
        length = in.read(buffer, 0, length);
        return bytesToValue(buffer, length);
    }


    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public int add(E value) {
        log.trace("Adding '" + value + "' to the pool");
        int insertPos = binarySearch(value);
        if (insertPos < valueCount && values[insertPos].equals(value)) {
            log.debug("The value '" + value + "' already exists in the pool");
            return -1;
        }
        expandIfNeeded();
        /* Make room */
        if (insertPos < valueCount) {
            System.arraycopy(values, insertPos, values, insertPos + 1,
                             valueCount - insertPos);
        }
        /* Assign */
        log.trace("Inserting at position " + insertPos);
        values[insertPos] = value;
        valueCount++;
        return insertPos;
    }

    protected void expandIfNeeded() {
        /* Check that there is enough room */
        if (valueCount == values.length) {
            int newSize = Math.min(valueCount + MAX_INCREMENT, valueCount * 2);
            log.debug("Expanding internal array of values to length "
                      + newSize);
            E[] newValues = getArray(newSize);
            System.arraycopy(values, 0, newValues, 0, valueCount);
            values = newValues;
        }
    }

    public void dirtyAdd(E value) {
        expandIfNeeded();
        values[valueCount++] = value;
    }

    public void cleanup() {
        //noinspection DuplicateStringLiteralInspection
        log.debug("Cleaning up dirty added values (" + valueCount + ")");
        Arrays.sort(values, 0, valueCount);
        removeDuplicates();
        log.debug("Finished cleanup. Resulting values: " + valueCount);
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public void remove(int position) {
        log.trace("Removing value at position " + position);
         if (position < 0 || position >= valueCount) {
            throw new ArrayIndexOutOfBoundsException("The position " + position
                                                     + " was not between 0"
                                                     + " and " + valueCount);
        }
        System.arraycopy(values, position + 1, values, position,
                         valueCount - position + 1);
        valueCount--;
    }

    public int getPosition(E value) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("Getting position for  '" + value + "' in the pool");
        int insertPos = binarySearch(value);
        return insertPos == valueCount
               || valueCount == 0
               || !value.equals(getValue(insertPos)) ? -1 : insertPos;
    }

    public E getValue(int position) {
        // No trace here, as it needs to be FAST
        return values[position];
    }

    public int size() {
        return valueCount;
    }

    public void clear() {
        valueCount = 0;
        values = getArray(DEFAULT_SIZE);
    }

    /**
     * This hack is taken from ArrayList and demonstrates one of the problems
     * with generics.
     * @param arraysize the size of the wanted array.
     * @return an array which can hold values of the type specified in the
     *         creation of the generified MemoryPool.
     */
    protected  E[] getArray(int arraysize) {
        //noinspection unchecked
        return (E[])new Comparable[arraysize];
    }

    /**
     * A simple binary searcher which recognizes valueCount. Other than that,
     * it behaves as the implementation from Sun.<br/>
     * http://en.wikipedia.org/wiki/Binary_search
     * @param value the value to search for.
     * @return the insertion point for value.
     */
    protected int binarySearch(E value) {
        int low = 0;
        int high = valueCount-1;
        while (low <= high) {
            int mid = (low + high) / 2;
            int diff = compare(values[mid], value);
            if (diff > 0) {
                high = mid - 1;
            } else if (diff < 0) {
                low = mid + 1;
            } else {
                return mid;
            }
        }
        return low;
    }
}