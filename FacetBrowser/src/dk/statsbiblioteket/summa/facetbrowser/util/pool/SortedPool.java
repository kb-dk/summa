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
import java.util.Comparator;

/*
* The State and University Library of Denmark
* CVS:  $Id: SortedPool.java,v 1.3 2007/10/04 13:28:21 te Exp $
*
* Maintains an ordered list of values, providing fast lookup based on position
* and lookup based on value. Additions and removals from the pool shifts
* positions in a deterministic manner.
*/
public interface SortedPool<E extends Comparable> extends Comparator<E> {
    /**
     * Load the pool from disk. The format of the pool is as follows:
     * </p><p>
     * [poolname].dat contains the values represented as Strings.
     * They are stored sequentially, delimited with linebreaks.
     * Line 0 contains the value with position 0, line 1 contains the value with
     * position 1 and so on.
     * </p><p>
     * [poolname].index contains the index of the values in [poolname].dat.
     * The indexes are represented af longs, directly stored as 8 bytes, no
     * delimiters. They are prepended with a version (4 bytes) and a count
     * of entries (4 bytes). They are appended with the index of the next
     * logical value, which also happens to be the file size of the values
     * file. The appended index does not could as an entry in the entry
     * count.<br>
     * There are exactly as many entries in [poolname].index as there are in
     * [poolname].dat.
     * @param location the folder with the persistent data.
     * @param poolName the name of the pool.
     * @throws IOException if the pool could not be loaded.
     */
    public void load(File location, String poolName) throws IOException;

    /**
     * Store the pool to disk. The format is described under {@link #load}.
     * @param location the folder with the persistent data.
     * @param poolName the name of the pool.
     * @throws IOException if the pool could not be stored..
     */
    public void store(File location, String poolName) throws IOException;

    /**
     * Add the given value to the pool and return the position of the newly
     * inserted value. The position of all following values are incremented
     * by one.
     * @param value the value to add to the pool.
     * @return the position of the newly added value.
     *         -1 is returned if the value already exists in the pool.
     */
    public int add(E value);

    /**
     * Add a value to the pool as fast as possible. The state of the pool
     * will be externally inconsistent until {@link #cleanup} has been called.
     * The methods intended use is for initial builds of large pools.
     * </p><p>
     * Important: Duplicates will only be removed during optimise, so adding
     *            a lot of duplocates with dirtyAdd will take up a lot of space.
     * @param value the value to add to the pool.
     */
    public void dirtyAdd(E value);

    /**
     * Ensures that the external state of the pool is consistent. Used after a
     * series of calls to {@link #dirtyAdd}. Note that this method will most
     * probably change the position of the values.
     * </p><p>
     * Calling optimise on an already cleaned pool will not change the position
     * of the values, but it might take some time.
     */
    public void cleanup();

    /**
     * Remove the value at the given position. The position of all following
     * values are decremented by one.
     * @param position the position in the pool.
     */
    public void remove(int position);

    /**
     * Search the pool for the given value and return the position. This method
     * are not guaranteed to be fast, as it is secondary to {@link #getValue}.
     * @param value the value to search for.
     * @return the position of the value, -1 if it does not exist.
     */
    public int getPosition(E value);

    /**
     * Return the value at the given position. Implementations on the SortedPool
     * interface should optimise this method over {@link #getPosition} in terms
     * of lookup speed.
     * @param position the position of the value in the pool.
     * @return the value at the given position. Requesting values outside of
     *         0..size() returns an undefined value or an exception. This is
     *         due to speed optimisation.
     */
    public E getValue(int position);

    /**
     * @return the number of values in the pool.
     */
    public int size();

    /**
     * Clear the pool of all values.
     */
    public void clear();

    // TODO: Add fast save for filesystem-based, use indexes upon open
}
