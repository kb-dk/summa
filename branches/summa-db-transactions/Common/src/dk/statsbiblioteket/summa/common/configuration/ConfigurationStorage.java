/* $Id: ConfigurationStorage.java,v 1.4 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.4 $
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
package dk.statsbiblioteket.summa.common.configuration;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Iterator;
import java.util.List;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * An abstraction of a storage backend for a {@link Configuration}.
 *
 * <p><b>Important</b>: Any implementation of this interface should be
 * <i>threadsafe</i>.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public interface ConfigurationStorage extends Configurable, Serializable {

    /**
     * Store a key-value pair.
     * @param key the name used to access the stored object
     * @param value the actual value to store
     * @throws IOException if there is an error performing the operation
     */
    public void put (String key, Serializable value) throws IOException;

    /**
     * Retrieve a stored item.
     * @param key name of object to look up
     * @return the object or null if the object is unknown
     * @throws IOException if there was an error performing the operation
     */
    public Serializable get (String key) throws IOException;

    /**
     * Get an {@code Iterator} over all key-value pairs
     * @return an Iterator over all stored key-value pairs
     * @throws IOException if there was an error performing the operation
     */
    public Iterator<Map.Entry<String,Serializable>> iterator () throws IOException;

    /**
     * Remove a property from the storage
     * @param key the name of the value to remove from the storage
     * @throws IOException if there was an error performing the operation
     */
    public void purge (String key) throws IOException;

    /**
     * Get the number of properties in the configuration
     * @return number of properties stored
     * @throws IOException if there was an error performing the operation
     */
    public int size () throws IOException;

    /**
     * @return true if the storage supports a hierarchical structure. I false,
     * the methods {@link #getSubStorage} and {@link #createSubStorage} should
     * throw an UnsupportedOperationException.
     * @throws IOException if there was an error performing the operation
     */
    public boolean supportsSubStorage() throws IOException;

    public static final String NOT_SUBSTORAGE_CAPABLE =
            "Not capable of handling sub storages";
    /**
     * Creates a sub storage, adds it to the current storage and returns it.
     * Sub storages are fully capable storages and are typically used to order
     * settings in a hierarchy.
     * </p><p>
     * Note: This might not be supported by all storages. Check for availability
     * with {@link #supportsSubStorage} in case of doubt.
     * @param key the name of the sub storage.
     * @return the storage that was created.
     * @throws IOException if there was an error performing the operation
     */
    public ConfigurationStorage createSubStorage(String key) throws IOException;

    /**
     * Note: This might not be supported by all storages. Check for availability
     * with {@link #supportsSubStorage} in case of doubt.
     * @param key the name of the sub storage.
     * @return a sub storage.
     * @throws IOException if there was an error performing the operation
     */
    public ConfigurationStorage getSubStorage(String key) throws IOException;

    /**
     * Creates a list of sub storages, adds it to the current storage and
     * returns it. Sub storages are fully capable storages.
     * </p><p>
     * Note: This might not be supported by all storages. Check for availability
     * with {@link #supportsSubStorage} in case of doubt.
     * @param key   the key for the list of storages.
     * @param count the number of storages to create.
     * @return a list of count sub storages.
     * @throws IOException if the storages could not be created.
     */
    public List<ConfigurationStorage> createSubStorages(String key, int count)
                                                             throws IOException;

    /**
     * Note: This might not be supported by all storages. Check for availability
     * with {@link #supportsSubStorage} in case of doubt.
     * @param key the key for the list of storages.
     * @return a list of storages.
     * @throws IOException if the list could not be extracted.
     */
    public List<ConfigurationStorage> getSubStorages(String key) throws
                                                                 IOException;
}



