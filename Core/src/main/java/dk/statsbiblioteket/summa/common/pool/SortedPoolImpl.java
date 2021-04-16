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
package dk.statsbiblioteket.summa.common.pool;

import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.LineReader;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.AbstractList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

/**
 * Partial implementation of SortedPool to provide open and save.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
@SuppressWarnings({"unchecked"})
public abstract class SortedPoolImpl<E extends Comparable<E>> extends AbstractList<E> implements SortedPool<E>, 
                                                                                                 Comparator<E> {
    private Log log = LogFactory.getLog(SortedPoolImpl.class);
    protected static final int VERSION = 2;

    protected ValueConverter<E> valueConverter;
    protected Comparator comparator;

    protected File location = null;
    protected String poolName = null;
    protected Boolean readOnly = null;

    public SortedPoolImpl(ValueConverter<E> valueConverter, Comparator comparator) {
        if (valueConverter == null) {
            log.warn("No Valueconverter specified. This will go horribly wrong if persistence is attempted");
        }
        this.valueConverter = valueConverter;
        if (comparator == null) {
            log.debug("The comparator is null. The natural order of values will be used for sorting");
        }
        this.comparator = comparator;
    }

    /**
     * Checks basic data for validity and stores them.
     *
     * @param location the location of the pool data.
     * @param poolName the name of the pool.
     * @param readOnly if true, the pool is read-only.
     */
    protected void setBaseData(File location, String poolName, boolean readOnly) {
        if (poolName == null || "".equals(poolName)) {
            throw new IllegalArgumentException("The poolName must not be null or the empty string");
        }
        try {
            checkLocation(location, poolName);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format(
                    Locale.ROOT, "setBaseData: Unable to create folder '%s' for pool '%s'", location, poolName), e);
        }
        this.location = location;
        this.poolName = poolName;
        this.readOnly = readOnly;
        log.trace(String.format(Locale.ROOT,
                "Assigned base data location '%s', pool name '%s' and readOnly %s", location, poolName, readOnly));
    }

    protected void checkBase() {
        if (location == null) {
            throw new IllegalStateException("The base data (location, pool name, read only) must be "
                                            + "specified before further actions are taken");
        }
    }

    /**
     * @return the index-file for this pool.
     */
    protected File getIndexFile() {
        return getPoolPersistenceFile(INDEX_POSTFIX);
    }

    /**
     * @return the value-file for this pool.
     */
    protected File getValueFile() {
        return getPoolPersistenceFile(VALUES_POSTFIX);
    }

    @Override
    public File getPoolPersistenceFile(String postfix) {
        checkBase();
        return getPoolPersistenceFile(location, poolName, postfix);
    }

    @Override
    public File getPoolPersistenceFile(File location, String poolName, String postfix) {
        return new File(location, poolName + postfix);
    }

    /**
     * Load the index data.
     *
     * @return the indexes for the stored pool.
     * @throws IOException if the index could not be loaded.
     */
    // FIXME: The use of an Object-Stream pollutes the raw bytes
    protected long[] loadIndex() throws IOException {
        checkBase();
        log.debug(String.format(Locale.ROOT, "Loading indexes for pool '%s' at location '%s'", poolName, location));
        if (!location.exists()) {
            throw new IOException(String.format(Locale.ROOT, "The folder '%s' for pool '%s' does not exist", location, poolName));
        }

        FileInputStream indexIn = new FileInputStream(new File(location, poolName + INDEX_POSTFIX));
        BufferedInputStream indexBuf = new BufferedInputStream(indexIn);
        ObjectInputStream index = new ObjectInputStream(indexBuf);
        int version = index.readInt();
        if (version != VERSION) {
            throw new IOException(String.format(Locale.ROOT,
                    "The version for the pool '%s' at location '%s' was %d. This loader only supports version %d", 
                    poolName, location, version, VERSION));
        }
        int size = index.readInt();
        log.debug(String.format(Locale.ROOT, "Starting load of %d index data (longs)", size));
        long[] indexData = new long[size];
        long feedback = Math.max(size / 100, 1);
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(size);
        for (int i = 0; i < size; i++) {
            if (i % feedback == 0) {
                if (log.isTraceEnabled()) {
                    log.trace("Loaded " + i + "/" + size + " index values. ETA: " + profiler.getETAAsString(true));
                }
            }
            indexData[i] = index.readLong();
        }
        log.trace("loadIndex: Closing streams");
        index.close();
        indexBuf.close();
        indexIn.close();
        log.debug(String.format(Locale.ROOT, "Finished loading of %d index data from pool '%s' at location '%s' in %s",
                                size, poolName, location, profiler.getSpendTime()));
        return indexData;
    }

    protected void checkLocation(File location, String poolName) throws IOException {
        if (!location.exists()) {
            if (!location.mkdirs()) {
                throw new IOException(String.format(Locale.ROOT, "Unable to create folder '%s' for pool '%s'", location, poolName));
            }
            log.debug(String.format(Locale.ROOT, "Created folder '%s' for pool '%s'", location, poolName));
        }
    }

    /**
     * Stores index and values at the given location. If any of these files are
     * already present, they are overwritten.
     * </p><p>
     * This storage guarantees that the order of the stored values will be
     * the logical order and that the index-file will contain incrementing
     * pointers. It also guarantees that any old cruft in existing persistent
     * values are purged from the new persistent files.
     *
     * @param location the folder for the data.
     * @param poolName the name of the pool.
     * @throws IOException if the data could not be written.
     */
    @QAInfo(level = QAInfo.Level.NORMAL,
            state = QAInfo.State.IN_DEVELOPMENT,
            author = "te",
            comment = "Windows uses file locking, so this might work bad when overwriting existing files")
    public void store(File location, String poolName) throws IOException {
        log.debug(String.format(Locale.ROOT, "Storing pool '%s' to location '%s'", poolName, location));
        checkLocation(location, poolName);
        File tmpIndex = new File(getIndexFile().toString() + ".tmp");
        File tmpValues = new File(getValueFile().toString() + ".tmp");
        remove(tmpIndex, "previously stored index");
        remove(tmpValues, "previously stored values");

        FileOutputStream dataOut = new FileOutputStream(tmpValues);
        BufferedOutputStream dataBuf = new BufferedOutputStream(dataOut);

        FileOutputStream indexOut = new FileOutputStream(tmpIndex);
        BufferedOutputStream indexBuf = new BufferedOutputStream(indexOut);
        ObjectOutputStream index = new ObjectOutputStream(indexBuf);

        index.writeInt(VERSION);
        index.writeInt(size());

        long pos = 0;
        long feedback = Math.max(size() / 100, 1);
        if (!log.isTraceEnabled()) {
            feedback = Integer.MAX_VALUE; // Disable
        }
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(size());
        for (int i = 0; i < size(); i++) {
            int length = writeValue(dataBuf, get(i));
            index.writeLong(getIndexEntry(pos, length));
            pos += length;
            profiler.beat();
            if (i % feedback == 0) {
                log.trace("Stored " + i + "/" + size() + " values. ETA: " + profiler.getETAAsString(true));
            }
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
        remove(getIndexFile(), "old index");
        log.trace(String.format(Locale.ROOT, "store: Renaming index '%s' to '%s'", tmpIndex, getIndexFile()));
        Files.move(tmpIndex, getIndexFile(), true);

        remove(getValueFile(), "old values");
        log.trace(String.format(Locale.ROOT, "store: Renaming values '%s' to '%s'", tmpValues, getValueFile()));
        Files.move(tmpValues, getValueFile(), true);
        log.debug("Finished storing pool '" + poolName + "' to location '" + location + "'");
    }

    /**
     * Bit-fiddles an index-entry for persistence use.
     *
     * @param pos    the position of a value in the value-file.
     * @param length the length in bytes of the value
     * @return an index-entry encoding the position and the length.
     */
    @QAInfo(level = QAInfo.Level.NORMAL,
            state = QAInfo.State.IN_DEVELOPMENT,
            author = "te",
            comment = "Should we add checks for valid ranges?")
    protected static long getIndexEntry(long pos, long length) {
        return length << POSITION_BITS | pos;
    }

    protected static long getValueLength(long indexEntry) {
        return indexEntry >>> POSITION_BITS;
    }

    protected static long getValuePosition(long indexEntry) {
        return indexEntry & POSITION_MASK;
    }

    protected void remove(File file, String description) throws IOException {
        if (file.exists()) {
            log.debug(String.format(Locale.ROOT, "Removing %s file '%s'", description, file));
            if (!file.delete()) {
                throw new IOException(String.format(Locale.ROOT, "Unable to delete %s file '%s'", description, file));
            }
        }
    }

    /**
     * Removes duplicates from the index. This requires the index to be sorted.
     * It is not guaranteed that the duplicates are removed from memory or
     * storage, only that they appear to the caller as removed.
     * </p><p>
     * Note: Implementations should avoid populating with duplicates during
     * normal adds. This method is only meant for cleanup after bulk
     * operations, that does not guarantee consistency.
     */
    protected void removeDuplicates() {
        log.trace("Removing duplicated");
        long startTime = System.currentTimeMillis();
        int initial = size();
        E last = null;
        int index = 0;
        while (index < size()) {
            E current = get(index);
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
        log.debug(String.format(Locale.ROOT, "Removed %d duplicates from a total of %d values in %dms",
                                initial - size(), initial, System.currentTimeMillis() - startTime));
    }


    protected byte[] buffer = new byte[1000];

    /**
     * Read a value from an open file.
     *
     * @param reader       the reader wit access to the value-file.
     * @param indexElement the position and the length of the value to read.
     * @return the wanted value.
     * @throws java.io.IOException if the value could not be read.
     */
    protected E readValue(LineReader reader, long indexElement) throws IOException {
        int length = (int) getValueLength(indexElement);
        if (length == 0) {
            return getValueConverter().bytesToValue(buffer, 0);
        }
        long valPos = getValuePosition(indexElement);
        if (log.isTraceEnabled()) {
            log.trace("readValue: Retrieving value of length " + length + " from file at position " + valPos);
        }
        synchronized (this) {
            reader.seek(valPos);
            if (buffer.length < length) {
                buffer = new byte[length];
            }
            reader.readFully(buffer, 0, length);
        }
/*        for (int i = 0 ; i < length ; i++) {
            System.out.println(buffer[i]);
        }
                                          */
        return getValueConverter().bytesToValue(buffer, length);
    }

    /**
     * Write the given value to the stream and return the number of bytes that
     * was written.
     * </p><p>
     * Note: It must be possible to et the value back by using
     * {@link ValueConverter#bytesToValue}.
     *
     * @param out   the stream to write to.
     * @param value the value to write.
     * @return the number of bytes that was written.
     * @throws IOException is the value could not be written.
     */
    protected int writeValue(BufferedOutputStream out, E value) throws IOException {
        try {
            byte[] buffer = getValueConverter().valueToBytes(value);
            out.write(buffer);
            return buffer.length;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding problem in writeValue(out, '" + value + "')", e);
        }
    }

    /**
     * @return the ValueConverter to use for persistence.
     */
    protected ValueConverter<E> getValueConverter() {
        return valueConverter;
    }

    public Comparator<E> getComparator() {
        return this;
    }

    /**
     * The default implementation uses binary search to determine insertion
     * point, meaning O(log n) time complexity.
     *
     * @param value the value to insert in the pool.
     * @return the position of the newly added value. If the value already
     *         exists in the pool, (-position)-1 is returned.
     * @see SortedPool#insert
     */
    @Override
    public int insert(E value) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("Adding '" + value + "' to pool '" + poolName + "'");
        int insertPos = Collections.binarySearch(this, value, this);
        if (insertPos >= 0) {
            log.trace("Value '" + value + "' already exists in pool '" + poolName + "'");
            return -1 * insertPos - 1;
        }
        insertPos = -1 * (insertPos + 1);
        add(insertPos, value); // Positive position
        return insertPos;
    }

    /**
     * Sorts the values and removes duplicates.
     */
    @Override
    public void cleanup() {
        log.trace("cleanup called");
        long startTime = System.currentTimeMillis();

        long sortTime = System.currentTimeMillis();
        sort();
        sortTime = System.currentTimeMillis() - sortTime;

        long duplicateTime = System.currentTimeMillis();
        removeDuplicates();
        duplicateTime = System.currentTimeMillis() - duplicateTime;
        //noinspection DuplicateStringLiteralInspection
        log.debug(String.format(Locale.ROOT,
                "cleanup of %s finished for %d elements in %dms (%dms for sort, %dms for duplicate removal)",
                poolName, size(), System.currentTimeMillis() - startTime, sortTime, duplicateTime));
    }

    /**
     * Sorts the pool according to the comparator given in the constructor.
     * A trivial implementation is a call to
     * {@link Collections#sort}(this, this), but implementators should note
     * that this involves a toArray-call in Sun's Java up to at least v1.6.
     */
    protected abstract void sort();

    // Default comparator
    @Override
    public int compare(E o1, E o2) {
        //noinspection unchecked
        return comparator == null ? o1.compareTo(o2) : comparator.compare(o1, o2);
    }

    @Override
    public String getName() {
        return poolName;
    }

    @Override
    public boolean dirtyAdd(E value) {
        add(size(), value);
        return true;
    }

    /* List interface */

    @Override
    public boolean add(E e) {
        return insert(e) >= 0;
    }

    @Override
    public int indexOf(E value) {
        int index = Collections.binarySearch(this, value, this);
        return index >= 0 ? index : -1;
    }

    @Override
    public int indexOf(Object o) {
        try {
            //noinspection unchecked
            return indexOf((E) o);
        } catch (ClassCastException e) {
            return -1;
        }
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    // We only have uniques in a sorted pool
    @Override
    public int lastIndexOf(Object o) {
        return indexOf(o);
    }

    @Override
    public boolean remove(Object o) {
        try {
            //noinspection unchecked
            int index = indexOf((E) o);
            if (index < 0) {
                return false;
            }
            remove(index);
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }
}
