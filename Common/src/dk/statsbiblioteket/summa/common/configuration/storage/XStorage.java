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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: XStorage.java,v 1.3 2007/10/04 13:28:21 te Exp $
 */
package dk.statsbiblioteket.summa.common.configuration.storage;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.util.XProperties;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * XStorage class, is a configuration storage, where it is possible to on-the
 * -fly is possible to attach sub storage and configurations. 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te, hbk")
public class XStorage implements ConfigurationStorage {
    public static final long serialVersionUID = 81293421421L;
    public static final String DEFAULT_RESOURCE = "xconfiguration.xml";

    private static boolean unclearSemanticsWarned = false;

    private File storageFile;
    /* The Storage properties. */
    private XProperties xprops;
    private static Log log = LogFactory.getLog(XStorage.class);

    /*
     * Creates a XStorage around a XProperties.
     * @paramproperties the properties to wrap around.
     */
    /*protected XStorage(XProperties properties) throws IOException {
        this (nextAvailableConfigurationFile());
        assignFrom(properties);
        syncStorageFile();
    }*/

    /**
     * Creates a XStorage with the {@link this@nextAvailableConfigurationFile}
     * configuration file.
     *
     * @throws IOException if error occur while fetching next available
     * configuration file.
     */
    public XStorage() throws IOException {
        this(nextAvailableConfigurationFile());
    }

    /**
     * Creates a XStorage with the given configuration.
     *
     * @param configuration The configuration for this XStorage.
     * @throws IOException if error occur while manipulating files on disc.
     */
    public XStorage(Configuration configuration) throws IOException {
        xprops = new XProperties();
        storageFile = nextAvailableConfigurationFile();
        new Configuration(this).importConfiguration(configuration);
        syncStorageFile();
    }

    // FIXME: Very ad-hoc
    private XStorage(XProperties properties) throws IOException {
        xprops = properties;
        storageFile = nextAvailableConfigurationFile();
    }

    /**
     * Creates a XStorage with the given configuration file on disc.
     *
     * @param configurationFile The configuration file.
     * @throws IOException if error occur while reading file from disc.
     */
    public XStorage(File configurationFile) throws IOException {
        storageFile = configurationFile;
        xprops = new XProperties();
        if (!configurationFile.exists()) {
            log.warn("Property file '" + configurationFile + "' does not exist."
                     + " Creating new file");
            syncStorageFile();
        } else {
            xprops.load(configurationFile.getAbsoluteFile().toString(),
                 false, false);
        }
    }

    public XStorage(String resource) throws IOException {
        xprops = new XProperties();
        xprops.load(resource, false, false);
    }

    /**
     * Return the absolute path of the file backing this storage.
     *
     * @return Absolute file path.
     */
    public String getFilename () {
        return storageFile.getAbsolutePath();
    }

    /**
     * Put a key-value pair into this configuration.
     *
     * @param key The name used to access the stored object.
     * @param value The actual value to store.
     * @throws IOException if error occur while manipulating on disc file.
     */
    public void put(String key, Serializable value) throws IOException {
        xprops.put(key, value);
        syncStorageFile();
    }

    /**
     * Returns the value to the given key, from the configuration.
     *
     * @param key Name of object to look up.
     * @return serializable object, which is the value of the given key, from
     * the configuration.
     */
    public Serializable get(String key) {
        try {
            return (Serializable)xprops.getObject(key);
        } catch (ClassCastException e) {
            throw new ClassCastException(String.format(
                    "Unable to cast the value for key '%s' to Serializable, "
                    + "got object of class %s",
                    key, xprops.getObject(key).getClass().getName()));
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * Returns an iterator over String, Serializable pairs, or key, value from
     * the configuration.
     *
     * @return An iterator over String, Serializable pairs, or key, value from
     * the configuration.
     * @throws IOException if error occur while serializing objects.
     */
    public Iterator<Map.Entry<String, Serializable>> iterator() throws
                                                                IOException {
        log.warn("Iterators are not fully supported by XStorage. "
                 + "This won't work well with nesting");
        Map<String, Serializable> tempMap =
                new HashMap<String, Serializable>(size());
        for (Map.Entry<Object, Object> entry: xprops.entrySet()) {
        try {
                tempMap.put((String)entry.getKey(),
                            (Serializable)entry.getValue());
        } catch (ClassCastException e) {
            throw new ClassCastException(String.format(
                    "Unable to cast '%s' of class %s to String and '%s' of "
                    + "class %s to Serializable",
                    entry.getKey(), entry.getKey().getClass(),
                    entry.getValue(), entry.getValue().getClass()));
        }
        }
        log.trace("Created shallow copy of storage with " + size()
                  + " elements. Returning iterator");
        return tempMap.entrySet().iterator();
    }

    /**
     * Remove a given key-value pair for the storage configuration.
     *
     * @param key The name of the value to remove from the storage.
     * @throws IOException if error occurs while updating storage file.
     */
    public void purge(String key) throws IOException {
        xprops.remove(key);
        syncStorageFile();
    }

    /**
     * Return the number of properties for this storage.
     *
     * @return The number of properties for this storage.
     */
    public int size() {
        return xprops.size();
    }

    /**
     * Return true if this configuration makes sub storages possible.
     * TODO: Should this always return true?
     *
     * @return Always true.
     */
    public boolean supportsSubStorage() {
        return true;
    }

    /**
     * Create a sub storage configuration and associate it with the given key.
     *
     * @param key The name of the sub storage.
     * @return The sub storage created.
     * @throws IOException if error occur while creating or retrieving sub
     * storage.
     */
    public ConfigurationStorage createSubStorage(String key) throws
                                                             IOException {
        put(key, new XProperties());
        return getSubStorage(key);
    }

    /**
     * Return a sub storage given a key that matches a sub storage name.
     *
     * @param key The name of the sub storage.
     * @return the sub storage.
     * @throws IOException if error occur while returning sub strorage.
     */
    public ConfigurationStorage getSubStorage(String key) throws IOException {
        try {
            Object sub = get(key);
            if (!(sub instanceof XProperties)) {
                throw new IOException("The value for '" + key
                                      + "' was of class '"
                                      + sub.getClass()
                                      + "'. Expected XStorage");
            }

            // TODO: Solve https://gforge.statsbiblioteket.dk/tracker/index.php?aid=1186
            if (!unclearSemanticsWarned) {
                log.warn("Unclear semantics. "
                         + "See https://gforge.statsbiblioteket.dk/"
                         + "tracker/index.php?aid=1186");
                unclearSemanticsWarned = true;
            }
            return new XStorage((XProperties)sub);
        } catch (NullPointerException e) {
            throw new IOException("Could not locate value for key '" + key
                                  + "':", e);
        }
    }

    /**
     * Create 'count' number of sub storages and associated these with the
     * given 'key'.
     *
     * @param key   The key for the list of storages.
     * @param count The number of storages to create.
     * @return A list of sub storages.
     * @throws IOException if error occur while creating sub storages.
     */
    public List<ConfigurationStorage> createSubStorages(String key, int count)
                                                            throws IOException {
        ArrayList<XProperties> subProperties =
                new ArrayList<XProperties>(count);
        List<ConfigurationStorage> storages =
                new ArrayList<ConfigurationStorage>(count);
        for (int i = 0 ; i < count ; i++) {
            subProperties.add(new XProperties());
            storages.add(new XStorage(subProperties.get(i)));
        }
        put(key, subProperties);
        return storages;
    }

    /**
     * Get a list of sub storage associated with a the 'key'.
     *
     * @param key The key for the list of storages.
     * @return list of sub storage associated with the given key.
     * @throws IOException if error occur while getting storages.
     */
    public List<ConfigurationStorage> getSubStorages(String key) throws
                                                                 IOException {
        Object sub = get(key);
        if (!(sub instanceof List)) {
            throw new IOException(String.format(
                    "The value for '%s' was of class '%s'. Expected List",
                    key, sub.getClass()));
        }
        List list = (List)sub;
        List<ConfigurationStorage> storages =
                new ArrayList<ConfigurationStorage>(list.size());
        for (Object o: list) {
            if (!(o instanceof XProperties)) {
                throw new IOException(String.format(
                        "A class in the list for '%s' was '%s'. Expected "
                        + "XProperties", key, o.getClass()));
            }
            storages.add(new XStorage((XProperties)o));
        }
        return storages;
    }

    /**
     * Get next available configuration file.
     *
     * @return The configuration file.
     * @throws IOException if IOException is cast while doing IO operations on
     * disc.
     */
    private static File nextAvailableConfigurationFile () throws IOException {
        final String XCONFIGURATION = "xconfiguration.";
        int count = 0;
        File f = new File (XCONFIGURATION + count +".xml");
        while (f.exists()) {
            count++;
            f = new File (XCONFIGURATION + count +".xml");
        }
        return f;
    }

    /**
     * Sync in memory storage with file on disc.
     *
     * @throws IOException if error occur while doing IO operations.
     */
    private void syncStorageFile () throws IOException {
        if (storageFile == null) {
            log.trace("No sync as the XStorage is memory-based");
            return;
        }
        log.trace("Syncing XStorage to '" + storageFile + "'");
        xprops.store (new BufferedOutputStream(
                              new FileOutputStream (storageFile)), null);
    }
}




