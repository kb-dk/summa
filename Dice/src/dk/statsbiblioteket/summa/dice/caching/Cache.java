/* $Id: Cache.java,v 1.3 2007/10/04 13:28:17 te Exp $
 * $Revision: 1.3 $
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
package dk.statsbiblioteket.summa.dice.caching;

import java.io.IOException;

/**
 * A cache is designed to hold objects that are too big to fit in memory.
 * The design approach is to assume that not even one of the cached objects
 * can be stored fully in memory.
 *
 * <p>Objects in the cache are assumed to consist of <code>parts</code> of equal
 * type, where an individual part can be held in memory without problems.
 * The part objects must implement the {@link java.io.Serializable} interface
 * (or {@link java.io.Externalizable} to optimize io).
 *
 * <p>The type of the generic <code>E</code> determines the type of
 * the parts (as returned by <code>readPart</code> calls) of the cached items.
 *
 * <p>To simplify communications with a {@link Cache} applications are
 * recommended to use a {@link CacheClient}.
 *
 * TODO: Add remove(id) and clear() methods and and update all implementations with these methods
 */
public interface Cache<E> {

    /**
     * Dummy handle returned when trying to {@link #lookup} a non existing id.
     */
    public final long NO_SUCH_ITEM = -1;


    /**
     * Prepare the cache to receive a data item.
     * Use the returned <code>id</code> to write an {@link Iterable}
     * with the {@link #putPart method}.
     * @return unique id used to identify the incomming {@link Iterable} in the {@link Cache}.
     * @throws IOException
     */
    public long startPut () throws IOException;

    /**
     * Write a part of a {@link Iterable} to the cache.
     * When done writing make sure to finalize the caching
     * with the {@link #endPut} method.
     * @param part a part of a {@link Iterable}.
     * @param id as returned by {@link #startPut}
     * @throws IOException
     */
    public void putPart (E part, long id) throws IOException;

    /**
     * Cleanly finalize the caching of the item given by <code>id</code>
     * @param id
     * @throws IOException
     */
    public void endPut (long id) throws IOException;

    /**
     * Obtain a handle to an item in the cache.
     *
     * Read parts of the item by calling {@link #readPart} on the handle.
     * You need to call {@link #close} to cleanly dispose of the handle.
     * If there's no item corresponding to <code>id</code> in the cache,
     * {@link #NO_SUCH_ITEM} is returned.
     * @param id
     * @return a handle to use in {@link #readPart} or {@link #NO_SUCH_ITEM} if there's no element corresponding to <code>id</code> in the cache.
     */
    public long lookup (long id) throws IOException;

    /**
     * Read the next part of a cached item.
     * You can obtain a handle to an item
     * by calling {@link #lookup} on the id identifying the
     * item in the cache.
     * @param handle
     * @return the next part or null if the item is fully read
     */
    public E readPart (long handle) throws IOException;

    /**
     * Cleanly  dispose of the handle.
     * @param handle
     */
    public void close (long handle) throws IOException;

    /**
     * Return the URL an item with <code>id</code> should use.
     * A return value does not gurantee existence of the item.
     * @param id
     * @return implementation specific url
     * @throws IOException
     */
    public String getDataURL (long id) throws IOException;
}
