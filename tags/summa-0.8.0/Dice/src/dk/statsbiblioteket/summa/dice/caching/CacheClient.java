/* $Id: CacheClient.java,v 1.2 2007/10/04 13:28:17 te Exp $
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
import java.io.FileNotFoundException;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 11/09/2006
 * Time: 12:55:15
 * To change this template use File | Settings | File Templates.
 */
public interface CacheClient<E> {

    /**
     * Write an iterable to the cache and return an id
     * that can be used to retrieve a handle
     * to the item via the {@link #get} method.
     */
    public long put (Iterable<E> item) throws IOException;

    /**
     * Get an iterator over the parts of an item in the cache
     * @param id id for the item to lookup
     * @return iterator over the parts of the cached item or null if there's no item corresponding to <code>id</code> in the cache
     * @throws IOException if the item was found, but there was an error reading it from the cache 
     */
    public Iterator<E> get (long id) throws IOException;

    /**
     * get the full path to a cached item
     * @param id
     * @return
     * @throws FileNotFoundException
     */
    public String getPath (long id) throws IOException;

}
