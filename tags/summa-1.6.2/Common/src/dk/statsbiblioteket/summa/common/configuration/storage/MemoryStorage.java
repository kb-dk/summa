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
package dk.statsbiblioteket.summa.common.configuration.storage;

import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.Serializable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.net.URL;

/**
 * A {@link ConfigurationStorage} implemetation backed by an in-memory
 * {@link HashMap}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment = "Class needs better Javadoc")
public class MemoryStorage implements ConfigurationStorage {
    public static final long serialVersionUID = 342390483L;
    private Map<String,Serializable> map;

    /**
     * Create a new empty {@code ConfigurationStorage} backed by an in-memeory
     * {@link HashMap}.
     */
    public MemoryStorage () {
        map = new HashMap<String,Serializable> (100);
    }

    public MemoryStorage (Configuration config) {
        this();
        new Configuration(this).importConfiguration(config);
    }

    /**
     * Create a new MemoryStorage loading default values from an
     * input stream.
     * @param in Inputstream from which to load XML data.
     * @throws IOException if error  occours when loading XML from input stream.
     */
    public MemoryStorage (InputStream in) throws IOException {
        this();
        Properties p = new Properties();

        p.loadFromXML(in);

        for (Map.Entry entry : p.entrySet()) {
            map.put ((String)entry.getKey(), (Serializable)entry.getValue());
        }
    }

    /**
     * Create a new memory backed {@code ConfigurationStorage} importing
     * an initial set of properties from an {@link URL} (with content in
     * Java property xml format).
     *
     * @param initialConfig location of the initial resource
     * @throws IOException if there are errors fetching the external resource
     */
    public MemoryStorage (URL initialConfig) throws IOException {
        this(initialConfig.openConnection().getInputStream());
    }

    /**
     * Create a new memory backed {@code ConfigurationStorage} importing
     * an initial set of properties from a system resource (with content in
     * Java property xml format).
     *
     * @param initialResource the name of the resource to import
     * @throws IOException if there is an error reading the resource
     */
    public MemoryStorage (String initialResource) throws IOException {
        this(ClassLoader.getSystemResource(initialResource));

    }

    public void put(String key, Serializable value) {
        map.put (key, value);
    }

    public Serializable get(String key) {
        return map.get(key);
    }

    public Iterator<Map.Entry<String, Serializable>> iterator() {
        return map.entrySet().iterator();
    }

    public void purge(String key) {
        map.remove(key);
    }

    public int size () {
        return map.size();
    }

    public boolean supportsSubStorage() {
        return true;
    }

    public ConfigurationStorage getSubStorage(String key) throws IOException {
        try {
            Object sub = get(key);
            if (!(sub instanceof MemoryStorage)) {
                throw new IOException(String.format(
                        "The value for '%s' was of class '%s'. "
                        + "Expected MemoryStorage", key, sub.getClass()));
            }

            return (MemoryStorage)sub;
        } catch (NullPointerException e) {
            throw new IOException(String.format(
                    "Could not locate value for key '%s'", key));
        }
    }

    public ConfigurationStorage createSubStorage(String key) throws
                                                                   IOException {
        put(key, new MemoryStorage());
        return getSubStorage(key);
    }
    public List<ConfigurationStorage> createSubStorages(String key, int count)
                                                            throws IOException {
        ArrayList<MemoryStorage> subProperties =
                new ArrayList<MemoryStorage>(count);
        List<ConfigurationStorage> storages =
                new ArrayList<ConfigurationStorage>(count);
        for (int i = 0 ; i < count ; i++) {
            subProperties.add(new MemoryStorage());
            storages.add(subProperties.get(i));
        }
        put(key, subProperties);
        return storages;
    }
    public List<ConfigurationStorage> getSubStorages(String key) throws
                                                                 IOException {
        Object sub = get(key);
        if (!(sub instanceof List)) {
            //noinspection DuplicateStringLiteralInspection
            throw new IOException(String.format(
                    "The value for '%s' was of class '%s'. Expected List",
                    key, sub.getClass()));
        }
        List list = (List)sub;
        List<ConfigurationStorage> storages =
                new ArrayList<ConfigurationStorage>(list.size());
        for (Object o: list) {
            if (!(o instanceof MemoryStorage)) {
                //noinspection DuplicateStringLiteralInspection
                throw new IOException(String.format(
                        "A class in the list for '%s' was '%s'. Expected "
                        + "MemoryStorage", key, o.getClass()));
            }
            storages.add((MemoryStorage)o);
        }
        return storages;
    }
}




