/* $Id: CachingQueue.java,v 1.2 2007/10/04 13:28:17 te Exp $
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
package dk.statsbiblioteket.summa.dice.caching;

import java.io.IOException;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 12/09/2006
 * Time: 14:33:33
 * Bare bones queue implementation using a {@link Cache} a to store elements.
 * Only supports {@link #add} and {@link #poll} operations.
 */
public class CachingQueue<E> {

    private CacheClient<E> cache;
    private int lastItem;

    public CachingQueue (Cache<E> cacheService) {
        this.cache = new GenericCacheClient<E>(cacheService);
        lastItem = 0;
    }

    /**
     * Put an item on the queue.
     * @param item
     */
    public void add (Iterable<E> item) {
        try {
            cache.put (item);
        } catch (IOException e) {
            throw new RuntimeException("Failed to put item in cache", e);
        }
    }

    /**
     * Remove and return the first element of the queue.
     * This call may block if the underlying cache is blocking
     * on {@link Cache#lookup} - like a {@link BlockingCacheService} does.
     * @return an iterator iterating over the parts of the next item in the queue, or null if there are no items in the queue
     */
    public Iterator<E> poll () {
        try {
            Iterator<E> parts = cache.get(lastItem);
            if (parts != null){
                lastItem++;
            }
            return parts;
        } catch (IOException e) {
            throw new RuntimeException("Failed to get item " + lastItem, e);
        }
    }

}



