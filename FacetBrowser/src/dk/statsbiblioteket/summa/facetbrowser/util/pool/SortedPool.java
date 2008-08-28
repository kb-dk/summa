/* $Id: SortedPool.java,v 1.3 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.3 $
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
package dk.statsbiblioteket.summa.facetbrowser.util.pool;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.RandomAccess;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
* Maintains an ordered list of values, providing fast lookup based on position
* and lookup based on value. Additions and removals from the pool shifts
* positions in a deterministic manner.
* </p><p>
* SortedPools are not guaranteed to accept input before {@link #open} has been
* called.
* </p><p>
* null is not a permitted value for SortedPools. All values are unique, with
* regard to equals().
* </p><p>
* For persistence, the format of the pool is as follows:
* </p><p>
* [poolname].dat contains the values in implementation-specific format
* directly appended after each other.
* </p><p>
* [poolname].index contains the index-entries for the values in [poolname].dat.
* The entries are represented af longs, made up of length of the value followed
* by its position. {@link #POSITION_BITS} specify the amount of bits for position.
* The format of the file is as follows:<br />
* version (4 bytes)<br />
* entry-count (4 bytes)<br />
* index-entries (8 bytes * entry-count)<br />
* </p><p>
* Note: The position in the entries does not need to be increasing. It is up
*       to the implementation to be capable of determining when one value
*       ends and the next begins (e.g. use \n as delimiter for Strings).
*/
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface SortedPool<E extends Comparable<E>> extends List<E>,
                                                          RandomAccess {
    public static final String VALUES_POSTFIX = ".dat";
    public static final String INDEX_POSTFIX = ".index";

    /**
     * When data are stored, an entry in index-data consists of 64 bits split in
     * [length][index] where [length] is 64 - POSITION_BITS bits long and [index]
     * is POSITION_BITS bits long.
     * </p><p>
     * This limits the maximum file size to 2^POSITION_BITS-1 and the maximum
     * entry-size to 2^(64-POSITION_BITS)-1 bytes in length.
     */
    public static final byte POSITION_BITS = 40; // 1TB index, 16MB values
    public static final long POSITION_MASK =
            Math.round(StrictMath.pow(2, POSITION_BITS))-1;

    /**
     * Opens an existing pool or creates a new pool at the given location.
     * If forceNew is true, a new pool is always created. If forceNew is false
     * and no pool-data exists, a new pool is created and false is returned.
     * </p><p>
     * @param location the folder with the persistent data.
     * @param poolName the name of the pool.
     * @param readOnly if true, the index is opened as read-only.
     * @param forceNew ignore any existing data and create a new pool.
     * @throws IOException if the pool could not be loaded.
     * @return true if existing data could be retrieved from Storage.
     */
    public boolean open(File location, String poolName, boolean readOnly,
                        boolean forceNew) throws IOException;

    /**
     * Ensure that the persistent files for the pool are stored. The location
     * will be the one used in {@link #open} or similar. Implementations are
     * adviced to ensure that users of existing persistence files can still
     * access those during save (e.g. save under another name, then move with
     * overwrite).
     * The storage format is described under {@link #open}.
     * @throws IOException if the pool could not be stored.
     */
    // TODO: Consider how to do this properly on systems with file locking.
    public void store() throws IOException;

    /**
     * Construct a File object based on the location, poolname and stated
     * postfix. This is the recommended method for calculating Files for
     * implementation-specific persistence.
     * </p><p>
     * Example: {@code getPoolPersistenceFile(".meta");} =>
     *          File(".../mylocation/mypool.meta).
     * @param postfix the postfix for the pool-specific persistent file.
     * @return a File for the pool with the given postfix.
     */
    public File getPoolPersistenceFile(String postfix);

    /**
     * Close down any connections to persistent files and call remove.
     * In order to use the Pool for further work, open must be called.
     * </p><p>
     * Note: It is legal to call close befor the first open.
     */
    public void close();

    /**
     * @return the name of the pool as specified in {@link #open}.
     */
    public String getName();

    /**
     * Add the given value to the pool and return the position of the newly
     * inserted value if it does not already exist. The insertion point will be
     * determined by the implementation. The position of all following values
     * are incremented by one if the value is not already existing.
     * </p><p>
     * If the value is already existing, no change is done to the list.
     * @param value the value to insert in the pool.
     * @return the position of the newly added value. If the value already
     *         exists in the pool, (-position)-1 is returned.
     * @see {@link #add}.
     */
    public int insert(E value);

    /**
     * Ensures that the state of the pool is consistent. Used after a
     * series of calls to {@link #add}. Note that this method will most
     * probably change the position of the values.
     */
    public void cleanup();

    /* List interface notes */

    /**
     * Add a value to the pool. This is a wrapper for {@link #insert}.
     * @param value the value to add to the pool.
     * @return true if the value was added.
     * @see {@link #insert}.
     */
    public boolean add(E value); // Defined in List

    /**
     * Add a value to the pool as fast as possible. The state of the pool
     * will be inconsistent until {@link #cleanup} has been called.
     * The method intended use is for initial builds of large pools.
     * @param value the value to add to the pool.
     * @return true if the value was added.
     * @see {@link #insert}.
     */
    public boolean dirtyAdd(E value);

    /**
     * Adding an element at a specific position from outside of the
     * pool-framework should be avoided, as it makes the state of the pool
     * inconsistent.
     * @param index   where to add the element.
     * @param element the element to add.
     */
    public void add(int index, E element); // Defined in List

    /**
     * Remove the value at the given position. The position of all following
     * values are decremented by one.
     * @param position the position in the pool.
     */
    public E remove(int position);

    /**
     * Return the value at the given position. Implementations on the SortedPool
     * interface should optimise this method over {@link #indexOf} in terms
     * of lookup speed.
     * @param position the position of the value in the pool.
     * @return the value at the given position. Requesting values outside of
     *         0..size() returns an undefined value or an exception. This is
     *         due to speed optimisation.
     */
    //public E get(int position); // Defined in list

    /**
     * Search the pool for the given value and return the position. This method
     * is not guaranteed to be fast, as it is secondary to {@link #get}.
     * @param value the value to search for.
     * @return the position of the value, -1 if it does not exist.
     */
    public int indexOf(E value); // Defined in List

    /**
     * Setting the element from outside of the pool-framework should be avoided,
     * as it makes the state of the pool inconsistent.
     * @param index   where to set the element.
     * @param element the element to set.
     * @return the old element, if any, else null.
     */
    public E set(int index, E element); // Defined in List

}
