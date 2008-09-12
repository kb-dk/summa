/* $Id: BlockingCacheService.java,v 1.2 2007/10/04 13:28:17 te Exp $
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
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;


/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 8/09/2006
 * Time: 10:39:06
 * Implementation of a {@link Cache} with a blocking queue-like behavior.
 * Since the queue has a disk based backend it is assumed that there is no maximum
 * size of the queue.
 *
 * This class is implemented using a decorator type of pattern.
 * It does the basic book keeping but forwards any real IO operations
 * to another <code>Cache</code>.
 *
 * This {@link Cache} is thread safe in the following way: Multiple threads each with
 * their own {@link CacheClient}s can write to the cache using their
 * {@link CacheClient#put} methods.
 * One thread may read from the cache with its own {@link CacheClient} using
 * {@link CacheClient#get}. The <code>get</code> method of that client
 * will block until the requested <code>id</code> is available in the
 * the cache.
 */
public class BlockingCacheService<E> implements Cache<E> {

    private Cache<E> cache;
    private final Set<Long> transactions;

    /**
     * Create a new BlockingCacheService using the given {@link Cache}
     * for IO operations.
     * @param backend
     */
    public BlockingCacheService(Cache<E> backend) {
        this.cache = backend;
        transactions = Collections.synchronizedSet(new HashSet<Long>());
    }

    public long startPut () throws IOException {
        long id = cache.startPut();
        transactions.add(id);
        return id;
    }

    public void putPart(E part, long id) throws IOException {
        if (!transactions.contains(id)) {
            throw new IOException("Id " + id + " not marked as in transaction");
        }
        cache.putPart (part, id);
    }

    public void endPut(long id) throws IOException {
        if (!transactions.contains(id)) {
            throw new IOException("Id " + id + " not marked as in transaction");
        }
        synchronized(transactions) {
            transactions.remove (id);
            transactions.notifyAll();
        }
        cache.endPut (id);
    }

    /**
     * Blocks until item with <code>id</code> becomes available.
     * See {@link Cache#lookup} for more info.
     * @param id
     * @return
     * @throws IOException
     */
    public long lookup(long id) throws IOException {
        // Block if requested item is in transaction
        if (transactions.contains(id)) {
            synchronized(transactions) {
                try {
                    System.out.println ("Wait trabns");
                    transactions.wait();
                } catch (InterruptedException e) {
                    // Do nothing
                }
                // something has changed in transactions, try looking up again
                return lookup (id);
            }
        }

        long handle = cache.lookup(id);

        if (handle == Cache.NO_SUCH_ITEM) {
            // block until item becomes available
            synchronized(transactions) {
                try {
                    System.out.println ("Wait no found");
                    transactions.wait();
                } catch (InterruptedException e) {
                    // Do nothing
                }
                // something has changed in transactions, try looking up again
                return lookup (id);
            }
        }
        return handle;

    }

    public E readPart(long handle) throws IOException {
        return cache.readPart(handle);
    }

    public void close(long handle) throws IOException {
        cache.close(handle);
    }

    public String getDataURL(long id) throws IOException {
        return cache.getDataURL(id);
    }
}





