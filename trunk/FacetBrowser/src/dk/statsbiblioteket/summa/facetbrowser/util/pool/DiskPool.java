/* $Id: DiskPool.java,v 1.5 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.5 $
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
 * CVS:  $Id: DiskPool.java,v 1.5 2007/10/04 13:28:21 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.util.pool;

import dk.statsbiblioteket.summa.common.util.ListSorter;
import dk.statsbiblioteket.util.LineReader;
import dk.statsbiblioteket.util.Logs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.Comparator;
import java.util.List;
import java.util.Arrays;

/**
 * The DiskPool uses the local filesystem for the storage of values. An index
 * of the value positions is kept in RAM. This takes up 8*valueCount bytes.
 * </p><p>
 * New values are appended to the values-file, but never removed. As long as
 * the underlying storage system allows for readers to access a file, while a
 * writer updates it, concurrent access should not be a problem.
 * </p><p>
 * While care has been taken to optimize the speed of this pool, it is
 * significantly slower than the equivalent {@link MemoryPool}.
 */
public class DiskPool<E extends Comparable<E>> extends SortedPoolImpl<E> {
    private Log log = LogFactory.getLog(DiskPool.class);
    private static final int DEFAULT_SIZE = 1000;
    private static final int MAX_INCREMENT = 1000000;

    private ListSorter sorter;

    /**
     * The index for the DiskPool. This is kept as an array in order to minimize
     * memory usage. The index can be longer than the amount of values.
     * The amount of values is given by {@link #valueCount}.
     * </p><p>
     * Note that each long in the array are made up of length and position as
     * described in {@link SortedPool}.
     */
    private long[] indexes;
    private int valueCount = 0;

    /**
     * The file containing values. This can be null, in case on a newly created
     * pool. The file is only updated upon storing.
     */
//    private RandomAccessFile values;
    private LineReader values;
    /**
     * The buffer for LineReader. This should be aproximately the length (bytes)
     * of the largest value.
     */
    private static final int VALUE_BUFFER_SIZE = 100;

    public DiskPool(ValueConverter<E> valueConverter,
                          Comparator comparator) {
        super(valueConverter, comparator);
        sorter = new ListSorter() {
            protected <T> void swap(List<T> list, int pos1, int pos2) {
                long temp = indexes[pos1];
                indexes[pos2] = indexes[pos1];
                indexes[pos1] = temp;
            }
        };
    }

    public boolean open(File location, String poolName, boolean readOnly,
                        boolean forceNew) throws IOException {
        if (values != null) {
            log.warn(String.format(
                    "Opening new pool without previous close of '%s'. "
                    + "Attempting close", location));
            try {
                values.close();
            } catch (IOException e) {
                log.warn(String.format(
                        "IOException closing previous pool for '%s'",
                         location), e);
            }
        }
        setBaseData(location, poolName, readOnly);
        if (!getIndexFile().exists() || !getValueFile().exists()) {
            if (!forceNew) {
                log.trace(String.format(
                        "Index data: %spresent, value data: %spresent. A new "
                        + "pool '%s' will be created at '%s'",
                        getIndexFile().exists() ? "" : "not ",
                        getValueFile().exists() ? "" : "not ",
                        poolName, location));
                forceNew = true;
            }
        }
        if (forceNew) {
            log.debug(String.format("creating new pool '%s' at '%s'",
                                    poolName, location));
            remove(getValueFile(), "existing values");
            getValueFile().createNewFile();
            indexes = new long[DEFAULT_SIZE];
        } else {
            log.trace("Loading index");
            indexes = loadIndex();
        }
        connectToValues();
        return !forceNew;
    }

    public void store() throws IOException {
        log.debug(String.format("Storing pool '%s' to location '%s'",
                                poolName, location));
        File tmpIndex = new File(getIndexFile().toString() + ".tmp");
        remove(tmpIndex, "previously stored index");

        FileOutputStream indexOut = new FileOutputStream(tmpIndex);
        BufferedOutputStream indexBuf = new BufferedOutputStream(indexOut);
        ObjectOutputStream index = new ObjectOutputStream(indexBuf);

        index.writeInt(VERSION);
        index.writeInt(size());

        for (int i = 0 ; i < size() ; i++) {
            index.writeLong(indexes[i]);
        }
        log.trace("Stored all values for '" + poolName + "', closing streams");
        index.flush();
        index.close();
        indexBuf.close();
        indexOut.close();
        log.trace(String.format("store: Renaming index '%s' to '%s'",
                                tmpIndex, getIndexFile()));
        remove(getIndexFile(), "old index");
        tmpIndex.renameTo(getIndexFile());
        log.debug("Finished storing pool '" + poolName + "' to location '"
                  + location + "'");

    }

    private void connectToValues() throws IOException {
        values = new LineReader(getValueFile(), readOnly ? "r" : "rw");
        values.setBufferSize(VALUE_BUFFER_SIZE);
    }

    public void close() {
        log.trace("Close called");
        if (values != null) {
            try {
                values.close();
            } catch (IOException e) {
                log.warn(String.format(
                        "Exception while attempting close "
                        + "for pool '%s' at '%s'",
                        poolName, location), e);
            }
        }
        values = null;
        valueCount = 0;
    }

    public String getName() {
        return "Generic Disk Pool";
    }

    protected void sort() {
        sorter.sort(this, this);
    }

    public E set(int index, E element) {
        if (index < 0 || index >= size()) {
            throw new ArrayIndexOutOfBoundsException(String.format(
                    "The index %d for %s was not between 0 and %d",
                    index, element, valueCount));
        }
        E e = get(index);
        indexes[index] = storeValue(element);
        return e;
    }

    private long storeValue(E element) {
        byte[] setBytes = getValueConverter().valueToBytes(element);
        long pos = values.length();
        if (setBytes.length > 0) {
            try {
                values.seek(pos);
                values.write(setBytes);
            } catch (IOException e1) {
                throw new RuntimeException(String.format(
                        "Unable to store value '%s' at file-position %d for "
                        + "'%s' in '%s'",
                        element, pos, poolName, location), e1);
            }
        }
        return getIndexEntry(pos, setBytes.length);
    }

    public void add(int insertPos, E value) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("Adding '" + value + "' to the pool at index " + insertPos);

        /* Check that there is enough room */
        expandIfNeeded();
        /* Make room */
        if (insertPos < size()) {
            System.arraycopy(indexes, insertPos, indexes, insertPos + 1,
                             size() - insertPos);
        }
        indexes[insertPos] = storeValue(value);
/*        System.out.println("Insertpos " + insertPos
                           + " valuelength " + getValueLength(indexes[insertPos])
                           + " valueposition " + getValuePosition(indexes[insertPos]));
                           */
        valueCount++;
    }

    protected void expandIfNeeded() {
        if (size() == indexes.length) {
            int newSize = Math.min(size() + MAX_INCREMENT, size() * 2);
            log.debug("Expanding internal arrays of indexes to length "
                      + newSize);
            long[] newIndexes = new long[newSize];
            System.arraycopy(indexes, 0, newIndexes, 0, size());
            indexes = newIndexes;
        }
    }

    public E remove(int position) {
        log.trace("Removing value at position " + position);
        E e = get(position);
        System.arraycopy(indexes, position + 1, indexes, position,
                         size() - position + 1);
        valueCount--;
        return e;
     }

    public E get(int position) {
        if (position < 0 || position >= size()) {
            throw new ArrayIndexOutOfBoundsException(String.format(
                    "The position %d was not between 0 and %d",
                    position, size()));
        }
        try {
            return readValue(values, indexes[position]);
        } catch (IOException e) {
            throw new IllegalStateException(String.format(
                    "No value present at position %d with length %d in file "
                    + "'%s' for pool '%s'",
                    getValuePosition(indexes[position]),
                    getValueLength(indexes[position]), location, poolName), e);
        }
    }

    public int size() {
        return valueCount;
    }

    public void clear() {
        log.debug(String.format("Clear called for pool '%s' at '%s'",
                                poolName, location));
        valueCount = 0;
        indexes = new long[DEFAULT_SIZE];
        if (values != null) {
            log.debug("Closing old value reader");
            try {
                values.close();
            } catch (IOException e) {
                log.warn(String.format(
                        "Could not close reader for pool '%s' at '%s'",
                        poolName, location));
            }
        }
        try {
            remove(getValueFile(), "existing values");
        } catch (IOException e) {
            log.warn(String.format("As '%s' could not be removed, the clearing "
                                   + "of '%s' might fail",
                                   getValueFile(), poolName));
        }
        try {
            getValueFile().createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(String.format(
                    "Unable to create file '%s' for pool '%s",
                    getValueFile(), poolName), e);
        }
        try {
            connectToValues();
        } catch (IOException e) {
            throw new RuntimeException(String.format(
                    "Unable to connect to file '%s' for pool '%s",
                    getValueFile(), poolName), e);
        }
    }

    /*
     * A indirect binary searcher which recognizes valueCount and performs
     * lookups for its values. Other than that, it behaves as the implementation
     * from Sun.<br/>
     * http://en.wikipedia.org/wiki/Binary_search
     * @param value the value to search for.
     * @return the insertion point for value.
     */
/*    protected int binarySearch(E value) {
        int low = 0;
        int high = valueCount-1;
        while (low <= high) {
            int mid = (low + high) / 2;
            int diff = compare(getValue(mid), value);
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
  */
}
