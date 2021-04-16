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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: DiskPool.java,v 1.5 2007/10/04 13:28:21 te Exp $
 */
package dk.statsbiblioteket.summa.common.pool;

import dk.statsbiblioteket.summa.common.util.ArrayUtil;
import dk.statsbiblioteket.summa.common.util.IndirectLongSorter;
import dk.statsbiblioteket.summa.common.util.ListSorter;
import dk.statsbiblioteket.util.LineReader;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.WeakHashMap;

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
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class DiskPool<E extends Comparable<E>> extends SortedPoolImpl<E> {
    private Log log = LogFactory.getLog(DiskPool.class);
    private static final int DEFAULT_SIZE = 1000;
    private static final int MAX_INCREMENT = 1000000;
    private static final double GROWTH_FACTOR = 2.0;

    private ListSorter sorter;
    private IndirectLongSorter<E> mergeSorter;
    // If false, ListSorter is used
    private boolean USE_MERGE_SORT = true;

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
    private static final int VALUE_BUFFER_SIZE = 500;

    /**
     * The cache facilitates faster look ups. It caches the mapping from
     * {@link #indexes} to external values.
     */
    private final WeakHashMap<Long, E> cache = new WeakHashMap<>();
    private long cacheHits = 0;
    private long cacheMisses = 0;

    public DiskPool(ValueConverter<E> valueConverter, Comparator comparator) {
        super(valueConverter, comparator);
        sorter = new ListSorter() {
            @Override
            protected <T> void swap(List<T> list, int pos1, int pos2) {
                long temp = indexes[pos1];
                indexes[pos1] = indexes[pos2];
                indexes[pos2] = temp;
            }
        };
        mergeSorter = new IndirectLongSorter<E>() {
            @Override
            protected E getValue(long reference) {
                return DiskPool.this.getValue(reference);
            }
        };
    }

    @Override
    @QAInfo(level = QAInfo.Level.FINE,
            state = QAInfo.State.QA_NEEDED,
            author = "te",
            comment = "Verify that valueCount is correct after load")
    public boolean open(File location, String poolName, boolean readOnly, boolean forceNew) throws IOException {
        log.debug(String.format(Locale.ROOT, "open(%s, %s, %b, %b) called", location, poolName, readOnly, forceNew));
        if (values != null) {
            log.warn(String.format(Locale.ROOT, "Opening new pool without previous close of '%s'. Attempting close", location));
            try {
                values.close();
            } catch (IOException e) {
                log.warn(String.format(Locale.ROOT, "IOException closing previous pool for '%s'", location), e);
            }
        }
        setBaseData(location, poolName, readOnly);
        if (!getIndexFile().exists() || !getValueFile().exists()) {
            if (!forceNew) {
                log.trace(String.format(Locale.ROOT,
                        "Index data: %spresent, value data: %spresent. A new pool '%s' will be created at '%s'",
                        getIndexFile().exists() ?
                        "" :
                        "not ", getValueFile().exists() ? "" : "not ", poolName, location));
                forceNew = true;
            }
        }
        if (forceNew) {
            log.debug(String.format(Locale.ROOT, "creating new pool '%s' at '%s'", poolName, location));
            //noinspection DuplicateStringLiteralInspection
            remove(getValueFile(), "existing values");
            if (!getValueFile().createNewFile()) {
                log.warn("The file '" + getValueFile()  + "' already existed");
            }
            indexes = new long[DEFAULT_SIZE];
        } else {
            log.trace("Loading index");
            indexes = loadIndex();
            valueCount = indexes.length;
            log.trace("Index loaded with indexes.length " + indexes.length);
            indexes = ArrayUtil.makeRoom(indexes, size(), GROWTH_FACTOR, MAX_INCREMENT, 1);
        }
        connectToValues();
        return !forceNew;
    }

    @Override
    public void store() throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.debug(String.format(Locale.ROOT, "Storing pool '%s' to location '%s'", poolName, location));
        File tmpIndex = new File(getIndexFile().toString() + ".tmp");
        remove(tmpIndex, "previously stored temporary index");

        FileOutputStream indexOut = new FileOutputStream(tmpIndex);
        BufferedOutputStream indexBuf = new BufferedOutputStream(indexOut);
        ObjectOutputStream index = new ObjectOutputStream(indexBuf);

        index.writeInt(VERSION);
        index.writeInt(size());

        for (int i = 0; i < size(); i++) {
            index.writeLong(indexes[i]);
        }
        log.trace(String.format(Locale.ROOT, "Stored all indexes for '%s', closing streams", poolName));
        index.flush();
        index.close();
        indexBuf.close();
        indexOut.close();
        //noinspection DuplicateStringLiteralInspection
        log.trace(String.format(Locale.ROOT, "store: Renaming index '%s' to '%s'", tmpIndex, getIndexFile()));
        //noinspection DuplicateStringLiteralInspection
        remove(getIndexFile(), "old index");
        if (!tmpIndex.renameTo(getIndexFile())) {
            log.warn("Unable to rename '" + tmpIndex + "' to '" + getIndexFile() + "'");
        }

        log.debug(String.format(Locale.ROOT,
                "Finished storing pool '%s' with %d values to location '%s'",
                poolName, size(), location));
//        log.debug("Values file: " + values.getFile() + ": " + values.getFile().length() + " bytes"); // TODO: Remove
        logStats();
    }

    private void logStats() {
        log.debug("Cache hits: " + cacheHits + ", cache misses: " + cacheMisses);
    }

    private void connectToValues() throws IOException {
        log.trace(String.format(Locale.ROOT, "connecToValues(%s) called", getValueFile()));
        if (values != null) {
            log.debug(String.format(Locale.ROOT,
                    "connecToValues(%s) observed that values are already assigned", getValueFile()));
        }
        values = new LineReader(getValueFile(), readOnly ? "r" : "rw");
        values.setBufferSize(VALUE_BUFFER_SIZE);
    }

    @Override
    public void close() {
        log.debug("Close called on " + getName() + " for file " + (values == null ? "N/A" : values.getFile()));
        if (values != null) {
            //noinspection DuplicateStringLiteralInspection
            log.debug("Before close: " + values.getFile() + ": " + values.getFile().length() + " bytes");
            try {
/*                if (values.length() > 4) { // TODO: Remove this
                    values.seek(values.length() / 2);
                    log.debug("Reading int from position "
                              + values.getPosition() + ": "
                              + values.readInt());
                    log.debug("Reading line 1 from position "
                              + values.getPosition() + ": "
                              + values.readLine());
                    log.debug("Reading line 2 from position "
                              + values.getPosition() + ": "
                              + values.readLine());
                }
  */
                log.trace("Calling values.close() on " + getName());
//                values.seek(values.length()-1); // TODO: Remove this
//                values.read();
//                values.seek(values.length()); // TODO: Remove this
                //              values.write(87); // Hack to ensure writer is at EOF
                values.close();
            } catch (IOException e) {
                log.warn(String.format(Locale.ROOT, "Exception while attempting close for pool '%s' at '%s'",
                                       poolName, location), e);
            }
        }
/*        File f = values.getFile();
        log.debug("After close: " + f + ": " + f.length() + " bytes");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();  //TODO: Implement this
        }
        log.debug("After close and sleep: " + f + ": " + f.length() + " bytes");
  */
        values = null;
        valueCount = 0;
        cache.clear();
        logStats();
    }

    @Override
    protected void sort() {
        if (!USE_MERGE_SORT) {
            sorter.sort(this, this);
        } else {
            mergeSorter.sort(indexes, 0, size(), this);
        }
    }

    @Override
    public E set(int index, E element) {
        if (index < 0 || index >= size()) {
            throw new ArrayIndexOutOfBoundsException(String.format(Locale.ROOT, "The index %d for %s was not between 0 and %d",
                                                                   index, element, valueCount));
        }
        E e = get(index);
        indexes[index] = storeValue(element);
        cache.put(indexes[index], element);
        return e;
    }

    private long storeValue(E element) {
        byte[] setBytes = getValueConverter().valueToBytes(element);
        long pos = values.length();
        if (log.isTraceEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("Storing " + element + " at file position " + pos);
        }
/*
        // Temporary debug start
        try {
            values.flush();
        } catch (IOException e) {
            log.error("Could not flush values!");
        }
        log.debug(values.getFile() + ": " + values.getFile().length()
                  + " bytes");
        // Temporary debug end
        */
        if (setBytes.length > 0) {
            try {
                values.seek(pos);
                values.write(setBytes);
            } catch (IOException e1) {
                throw new RuntimeException(String.format(Locale.ROOT,
                        "Unable to store value '%s' at file-position %d for '%s' in '%s'",
                        element, pos, poolName, location), e1);
            }
        }
        return getIndexEntry(pos, setBytes.length);
    }

    @Override
    public void add(int insertPos, E value) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("Adding '" + value + "' to the pool at index " + insertPos);

        /* Check that there is enough room */
        indexes = ArrayUtil.makeRoom(indexes, Math.max(insertPos, size()), GROWTH_FACTOR, MAX_INCREMENT, 1);
        /* Make room */
        if (insertPos < size()) {
            log.trace("Shuffling part of the index array to make room for add");
            System.arraycopy(indexes, insertPos, indexes, insertPos + 1, size() - insertPos);
        }
        indexes[insertPos] = storeValue(value);
        cache.put(indexes[insertPos], value);
/*        System.out.println("Insertpos " + insertPos
                           + " valuelength " + getValueLength(indexes[insertPos])
                           + " valueposition " + getValuePosition(indexes[insertPos]));
                           */
        valueCount++;
//        log.debug("After add: " + values.getFile() + ": " + values.getFile().length() + " bytes"); // TODO: Remove
    }

    @Override
    public E remove(int position) {
        log.trace("Removing value at position " + position);
        E e = get(position);
        cache.put(indexes[position], null);
        try {
            System.arraycopy(indexes, position + 1, indexes, position, size() - position + 1);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new ArrayIndexOutOfBoundsException(String.format(Locale.ROOT,
                    "Exception performing System.arraycopy(indexes, %d, indexes"
                    + ", %d, %d) with indexes.length=%d and valueCount=%d",
                    position + 1, position, size() - position + 1, indexes.length, valueCount));
        }
        valueCount--;
        return e;
    }

    @Override
    public E get(int position) {
        if (position < 0 || position >= size()) {
            throw new ArrayIndexOutOfBoundsException(String.format(Locale.ROOT, "The position %d was not between 0 and %d",
                                                                   position, size()));
        }
        return getValue(indexes[position]);
    }

    private E getValue(long reference) {
        try {
            E value = cache.get(reference);
            if (value != null) {
                cacheHits++;
                return value;
            }
            cacheMisses++;
            value = readValue(values, reference);
            cache.put(reference, value);
            return value;
        } catch (IOException e) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "No value present at position %d with length %d in file '%s' for pool '%s'",
                    getValuePosition(reference), getValueLength(reference), location, poolName), e);
        }
    }

    @Override
    public int size() {
        return valueCount;
    }

    @Override
    public void clear() {
        log.debug(String.format(Locale.ROOT, "Clear called for pool '%s' at '%s'", poolName, location));
        valueCount = 0;
        indexes = new long[DEFAULT_SIZE];
        cache.clear();
        if (values != null) {
            log.debug("Closing old value reader");
            try {
                values.close();
            } catch (IOException e) {
                log.warn(String.format(Locale.ROOT, "Could not close reader for pool '%s' at '%s'", poolName, location));
            }
        }
        try {
            //noinspection DuplicateStringLiteralInspection
            remove(getValueFile(), "existing values");
        } catch (IOException e) {
            log.warn(String.format(Locale.ROOT,
                    "As '%s' could not be removed, the clearing of '%s' might fail", getValueFile(), poolName));
        }
        try {
            if (!getValueFile().createNewFile()) {
                log.warn("clear(): The file '" + getValueFile() + "' already existed");
            }
        } catch (IOException e) {
            throw new RuntimeException(String.format(Locale.ROOT, "Unable to create file '%s' for pool '%s",
                                                     getValueFile(), poolName), e);
        }
        try {
            connectToValues();
        } catch (IOException e) {
            throw new RuntimeException(String.format(Locale.ROOT, "Unable to connect to file '%s' for pool '%s", getValueFile(),
                                                     poolName), e);
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
