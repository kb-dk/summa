/* $Id: MemoryStorage.java,v 1.4 2007/10/04 13:28:21 te Exp $
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
package dk.statsbiblioteket.summa.common.configuration.storage;

import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.Serializable;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.net.URL;
import java.net.URLConnection;

/**
 * A {@link ConfigurationStorage} implemetation backed by an in-memory
 * {@link HashMap}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment = "Class needs better Javadoc")
public class MemoryStorage implements ConfigurationStorage {

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
     * @param in
     * @throws IOException
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
