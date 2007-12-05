/* $Id: GenericCacheClient.java,v 1.2 2007/10/04 13:28:17 te Exp $
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

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Generic class to interface with a {@link Cache}, the <code>Cache</code>
 * being a {@link RemoteCacheService}, {@link BlockingCacheService},
 * or normal {@link CacheService}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment = "Needs Javadoc")
public class GenericCacheClient<E> implements CacheClient<E> {
    Cache<E> cache;

    private class CacheItemProxy implements Iterator<E> {
        E nextPart;
        long handle;

        public CacheItemProxy (long handle) throws IOException {
            this.handle = handle;
            nextPart = readPart(handle);
        }

        public boolean hasNext() {
            return nextPart != null;
        }

        public E next() {
            E tmp = nextPart;
            try {
                nextPart = readPart(handle);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return tmp;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }


    public GenericCacheClient (Cache<E> cache) {
        this.cache = cache;
    }

    public long put(Iterable<E> item) throws IOException {
        long id = cache.startPut();
        Iterator<E> parts = item.iterator();
        while (parts.hasNext()) {
            cache.putPart(parts.next(), id);
        }
        cache.endPut(id);
        return id;
    }

    public Iterator get (long id) throws IOException {
        long handle = cache.lookup (id);

        if (handle == Cache.NO_SUCH_ITEM) {
            return null;
        }

        return new CacheItemProxy(handle);
    }

    public String getPath (long id) throws IOException {
        return cache.getDataURL(id);
    }

    private E readPart(long handle) throws IOException {
           E part = cache.readPart(handle);
            if (part == null) {
                cache.close (handle);
            }
            return part;
        }
}
