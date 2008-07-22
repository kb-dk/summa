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

import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.LineReader;

/**
 * The DiskPool uses the local filesystem for the storage of values. An index
 * of the value positions is kept in RAM. This takes up 8*valueCount bytes.
 * </p><p>
 * The internal position list packs pointers to value- and shadow-file together
 * with the size of the individual values. This means that the maximum size
 * of a given value is restricted to 2^15 bytes.
 * </p><p>
 * During use, a temporary shadow-file with added strings are kept on disk.
 * Upon storing, the shadow-file is removed.
 * </p><p>
 * While care has been taken to optimize the speed of this pool, it is
 * significantly slower than the equivalent {@link MemoryPool}.
 */
public abstract class DiskPool<E extends Comparable<? super E>>
                                                     extends SortedPoolImpl<E> {
    private Log log = LogFactory.getLog(DiskPool.class);
    private static final int DEFAULT_SIZE = 1000;
    private static final int MAX_INCREMENT = 1000000;

    private static final byte LENGTH_ROLL = 48; // Number of bits to roll
    private static final long OFFSET_MASK = 
            Math.round(StrictMath.pow(2, LENGTH_ROLL))-1;

    /**
     * The index for the DiskPool is special.
     * Positive values and 0 indicates offsets in the standard value data file.
     * Negative values indicates offsets in the shadow file.
     * The length of the given value in the value file is added to the offset by
     * length*2^48. For the shadow file, the length is subtracted.
     */
    private long[] indexes = new long[DEFAULT_SIZE];
    protected int valueCount = 0;

    /**
     * The file containing values. This can be null, in case on a newly created
     * pool. The file is only updated upon storing.
     */
//    private RandomAccessFile values;
    private LineReader values;

    /**
     * The file containing added values. This is changed every time a value is
     * added.
     */
    // TODO: When should we remove this file?
    private RandomAccessFile shadow;
    /**
     * The position for a new value.
     */
    private long newShadowPosition = 0;
    private File shadowFile;

    /**
     * Constructing a DiskPool without a location and a name will result in
     * temporary files being created in the temp-dir specified in Java
     * properties.
     * @throws IOException if the temporary files could not be created.
     */
    public DiskPool() throws IOException {
        //noinspection DuplicateStringLiteralInspection
        this(new File(System.getProperty("java.io.tmpdir")),
                      "TempPool_" + System.currentTimeMillis(), true);
    }

    /**
     * Constructing a DiskPool with newPool add to true will not change any
     * data on disk before storing.
     * @param location the folder with the persistent files.
     * @param poolName the name of the pool.
     * @param newPool  if true, a new pool is created, if false, an old pool
     *                 is loaded.
     * @throws IOException if old data could not be loaded or new data not
     *                     be created.
     */
    public DiskPool(File location, String poolName, boolean newPool)
            throws IOException {
        if (!newPool) {
            load(location, poolName);
        } else {
            log.info("Creating empty data file for '" + poolName + "' in '"
                     + location + "'");
            new File(location, poolName + ".dat").createNewFile();
            connectToValues(location, poolName);
        }
    }

    public void load(File location, String poolName) throws IOException {
        log.debug("Loading index and values for '" + poolName + "' in '"
                  + location + "'");
        if (values != null) {
            log.debug("Closing handles for values and shadow (" + shadowFile
                      + ")");
            values.close();
            shadow.close();
            shadowFile.delete();
            clear();
        }
        loadAndHandleIndex(location, poolName);
        connectToValues(location, poolName);
    }

    protected void connectToValues(File location, String poolName) throws
                                                                   IOException {
        log.debug("Connecting to values and shadow file for '" + poolName
                  + "' in '" + location + "'");
        values = new LineReader(new File(location, poolName + ".dat"),
                                      "r");
        values.setBufferSize(100);
//        values = new RandomAccessFile(new File(location, poolName + ".dat"),
//                                      "r");
        createAndConnectToShadow(location, poolName);
    }

    private void createAndConnectToShadow(File location, String poolName) throws
                                                                   IOException {
        shadowFile = new File(location,
                              poolName + "_" + System.currentTimeMillis()
                              + ".shadow");
        shadowFile.createNewFile();
        shadow = new RandomAccessFile(shadowFile, "rw");
    }

    /**
     * Loads the index into RAM and updates lengths to reflect it.
     * @param location where the index is stored.
     * @param poolName the name of the pool.
     * @throws IOException if the index data could not be loaded.
     */
    protected void loadAndHandleIndex(File location, String poolName) throws
                                                                      IOException {
        log.debug("Loading index for '" + poolName + "' in '" + location + "'");
        indexes = loadIndex(location, poolName);
        for (int i = 0 ; i < indexes.length - 1 ; i++) {
            indexes[i] = indexes[i] | indexes[i+1] - indexes[i] << LENGTH_ROLL;
        }
        valueCount = indexes.length - 1;
        //noinspection DuplicateStringLiteralInspection
        log.debug("Finished loading index and updating length for " + valueCount
                  + " values for '" + poolName + "' in '" + location + "'");
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public int add(E value) {
        log.trace("Adding '" + value + "' to the pool");
        int insertPos = binarySearch(value);
        if (insertPos < valueCount && getValue(insertPos).equals(value)) {
            log.debug("The value '" + value + "' already exists in the pool");
            return -1;
        }
        /* Check that there is enough room */
        expandIfNeeded();
        /* Make room */
        if (insertPos < valueCount) {
            System.arraycopy(indexes, insertPos, indexes, insertPos + 1,
                             valueCount - insertPos);
        }
        /* Assign */
        log.trace("Inserting at position " + insertPos);
        try {
            shadow.seek(newShadowPosition);
        } catch (IOException e) {
            throw new RuntimeException("Could not seek to position "
                                       + newShadowPosition
                                       + " in the shadow file '"
                                       + shadowFile + "'");
        }
        byte[] bytes = valueToBytes(value);
        try {
            shadow.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException("Could not store value '" + value + "'"
                                       + " to shadow file '" + shadowFile + "'",
                                       e);
        }
        long combined = newShadowPosition + ((long)bytes.length << LENGTH_ROLL);
        indexes[insertPos] = -combined; // Shadows are negative!
        newShadowPosition += bytes.length;
        valueCount++;
        return insertPos;
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public void dirtyAdd(E value) {
        if (log.isTraceEnabled()) {
            log.trace("Adding '" + value + "' to the pool");
        }
        /* Check that there is enough room */
        expandIfNeeded();
        try {
            shadow.seek(newShadowPosition);
        } catch (IOException e) {
            throw new RuntimeException("Could not seek to position "
                                       + newShadowPosition
                                       + " in the shadow file '"
                                       + shadowFile + "'");
        }
        byte[] bytes = valueToBytes(value);
        try {
            shadow.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException("Could not store value '" + value + "'"
                                       + " to shadow file '" + shadowFile + "'",
                                       e);
        }
        long combined = newShadowPosition + ((long)bytes.length << LENGTH_ROLL);
        indexes[valueCount] = -combined; // Shadows are negative!
        newShadowPosition += bytes.length;
        valueCount++;
    }

    public void cleanup() {
        //noinspection DuplicateStringLiteralInspection
        log.debug("Cleaning up dirty added values (" + valueCount + ")");
        sortIndexes();
        removeDuplicates();
    }

    /**
     * The sorter needs to look up the values from the indexes and should not
     * use too much memory, as the DiskPool are geared towards low memory usage.
     * A HeapSort is used, which takes O(n*log(n)) time and n space.
     * see http://en.wikibooks.org/wiki/Wikiversity:Data_Structures#Heaps
     * and http://www.personal.kent.edu/~rmuhamma/Algorithms/MyAlgorithms/Sorting/heapSort.htm
     */
    protected void sortIndexes() {
        /* This is O(n), although it looks like O(n*log(n)) */
        //noinspection DuplicateStringLiteralInspection
        log.debug("Sorting " + valueCount + " values");
        log.debug("Sifting down " + (valueCount / 2 - 1) + " entries");
        int every = valueCount / 2 / 100;
        Profiler siftProfiler = new Profiler();
        siftProfiler.setExpectedTotal(100);
        for (int position = valueCount / 2 - 1 ; position >= 0 ; position--) {
            if (log.isDebugEnabled() && position % every == 0) {
                siftProfiler.beat();
                if (log.isTraceEnabled()) {
                    log.trace("sortIndexes: sifted down "
                              + (valueCount / 2 - position) * 100 /
                                (valueCount / 2)
                              + "%. ETA: "
                              + siftProfiler.getETAAsString(true));
                }
            }
            siftDown(position, valueCount);
        }

        every = valueCount / 100;
        Profiler minProfiler = new Profiler();
        minProfiler.setExpectedTotal(100);
        log.debug("Removing minimum value " + valueCount + " times");
        for (int i = valueCount ; i > 0 ; i--) {
//            System.out.println("=> " + i);
            indexes[i-1] = removeMin(i);
            if (log.isDebugEnabled() && i % every == 0) {
                minProfiler.beat();
                log.debug("sortIndexes: removedMin "
                          + (valueCount - i) * 100 / valueCount + "%. ETA: "
                          + minProfiler.getETAAsString(true));
            }
//            System.out.println(i + " " + getValue(i-1));
/*            for (int j = 0 ; j < valueCount ; j++) {
                System.out.print(getValue(j) + " ");
            }
            System.out.println("");*/
        }
        log.debug("Finished sorting " + valueCount + " values in " 
                  + siftProfiler.getSpendTime());
    }
    protected void siftDown(int startPosition, int heapSize) {
        int position = startPosition;
        while (firstChild(position) < heapSize) {
            int kid = firstChild(position);
            if (kid < heapSize-1 &&
                compare(getValue(kid), getValue(kid+1)) < 0) {
                kid++;
            }
            if (compare(getValue(position), getValue(kid)) > 0) {
                break;
            } else {
                swap(kid, position);
                position = kid;
            }
        }
    }
    protected int firstChild(int element) {
        return 2*element+1;
    }
    private void swap(int element1, int element2) {
        long temp = indexes[element1];
        indexes[element1] = indexes[element2];
        indexes[element2] = temp;
    }
    public long removeMin(int heapSize) {
        long result = indexes[0];
        indexes[0] = indexes[heapSize-1];
        heapSize--;
        siftDown(0, heapSize);
        return result;
    }


    protected void expandIfNeeded() {
        if (valueCount == indexes.length) {
            int newSize = Math.min(valueCount + MAX_INCREMENT, valueCount * 2);
            log.debug("Expanding internal arrays of indexes to length "
                      + newSize);
            long[] newIndexes = new long[newSize];
            System.arraycopy(indexes, 0, newIndexes, 0, valueCount);
            indexes = newIndexes;
        }
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public void remove(int position) {
        log.trace("Removing value at position " + position);
         if (position < 0 || position >= valueCount) {
             throw new ArrayIndexOutOfBoundsException("The position " + position
                                                      + " was not between 0"
                                                      + " and " + valueCount);
         }
        System.arraycopy(indexes, position + 1, indexes, position,
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

    private byte[] buffer = new byte[1024];
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public E getValue(int position) {
        long combined = indexes[position];
        long corrected = combined < 0 ? -combined : combined;
        long index = corrected & OFFSET_MASK;
        int length = (int)(corrected >> LENGTH_ROLL);

        if (buffer.length < length) {
            buffer = new byte[length];
        }

        if (combined < 0) { // Shadow file
            try {
                shadow.seek(index);
            } catch (IOException e) {
                throw new RuntimeException("Could not seek to shadow position "
                                           + position + "(offset " + index
                                           + " in the file)", e);
            }
            try {
                shadow.readFully(buffer, 0, length);
                return bytesToValue(buffer, length);
            } catch (IOException e) {
                throw new RuntimeException("Could not read shadow at position "
                                           + position + "(offset " + index 
                                           + " in the file)", e);
            }
        }
        try {
            values.seek(index);
        } catch (IOException e) {
            throw new RuntimeException("Could not seek to value position "
                                       + position + "(offset " + index
                                       + " in the file)", e);
        }
        try {
            values.readFully(buffer, 0, length);
            return bytesToValue(buffer, length);
        } catch (IOException e) {
            throw new RuntimeException("Could not read value at position "
                                       + position + "(offset " + index
                                       + " in the file)", e);
        }
    }

    public int size() {
        return valueCount;
    }

    public void clear() {
        valueCount = 0;
        indexes = new long[DEFAULT_SIZE];
        newShadowPosition = 0;
    }

    /**
     * A indirect binary searcher which recognizes valueCount and performs
     * lookups for its values. Other than that, it behaves as the implementation
     * from Sun.<br/>
     * http://en.wikipedia.org/wiki/Binary_search
     * @param value the value to search for.
     * @return the insertion point for value.
     */
    protected int binarySearch(E value) {
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
}
