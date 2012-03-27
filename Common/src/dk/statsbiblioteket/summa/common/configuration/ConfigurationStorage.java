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
package dk.statsbiblioteket.summa.common.configuration;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
     * @param key The name used to access the stored object.
     * @param value The actual value to store.
     * @throws IOException if there is an error performing the operation.
     */
    public void put (String key, Serializable value) throws IOException;

    /**
     * Retrieve a stored item.
     * @param key Name of object to look up.
     * @return The object or null if the object is unknown.
     * @throws IOException if there was an error performing the operation.
     */
    public Serializable get (String key) throws IOException;

    /**
     * Get an {@code Iterator} over all key-value pairs.
     * @return An Iterator over all stored key-value pairs.
     * @throws IOException if there was an error performing the operation.
     */
    public Iterator<Map.Entry<String,Serializable>> iterator ()
                                                             throws IOException;

    /**
     * Remove a property from the storage.
     * @param key The name of the value to remove from the storage.
     * @throws IOException if there was an error performing the operation.
     */
    public void purge (String key) throws IOException;

    /**
     * Get the number of properties in the configuration.
     * @return Number of properties stored.
     * @throws IOException if there was an error performing the operation.
     */
    public int size () throws IOException;

    /**
     * Tells if this storage supports multiple sub storage.
     *
     * @return True if the storage supports a hierarchical structure. I false,
     * the methods {@link #getSubStorage} and {@link #createSubStorage} should
     * throw an UnsupportedOperationException.
     * @throws IOException if there was an error performing the operation.
     */
    public boolean supportsSubStorage() throws IOException;

    public static final String NOT_SUBSTORAGE_CAPABLE =
            "Not capable of handling sub storage";
    /**
     * Creates a sub storage, adds it to the current storage and returns it.
     * Sub storage are fully capable storage and are typically used to order
     * settings in a hierarchy.
     * </p><p>
     * Note: This might not be supported by all storage. Check for availability
     * with {@link #supportsSubStorage} in case of doubt.
     * @param key The name of the sub storage.
     * @return The storage that was created.
     * @throws IOException if there was an error performing the operation.
     */
    public ConfigurationStorage createSubStorage(String key) throws IOException;

    /**
     * Note: This might not be supported by all storage. Check for availability
     * with {@link #supportsSubStorage} in case of doubt.
     * @param key The name of the sub storage.
     * @return A sub storage.
     * @throws IOException if there was an error performing the operation.
     */
    public ConfigurationStorage getSubStorage(String key) throws IOException;

    /**
     * Creates a list of sub storage, adds it to the current storage and
     * returns it. Sub storage are fully capable storage.
     * </p><p>
     * Note: This might not be supported by all storage. Check for availability
     * with {@link #supportsSubStorage} in case of doubt.
     * @param key The key for the list of storage.
     * @param count The number of storage to create.
     * @return A list of count sub storage.
     * @throws IOException if the storage could not be created.
     */
    public List<ConfigurationStorage> createSubStorages(String key, int count)
                                                             throws IOException;

    /**
     * Note: This might not be supported by all storage. Check for availability
     * with {@link #supportsSubStorage} in case of doubt.
     * @param key the key for the list of storage.
     * @return A list of storage.
     * @throws IOException if the list could not be extracted.
     */
    public List<ConfigurationStorage> getSubStorages(String key) throws
                                                                 IOException;
}




