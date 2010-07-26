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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorageException;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * A {@link ConfigurationStorage} backed by a file.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment = "Class and methods needs better docs, especially for throws")
public class FileStorage implements ConfigurationStorage {
    public static final long serialVersionUID = 789468461L;
    //public static final String DEFAULT_RESOURCE = "configuration.xml";

    private HashMap<String,Serializable> map;
    private String filename;

    /**
     * Create a file storage backed by the next file of the form configuration.N.xml
     * that doesn't already exist for some natural number N.
     * @throws IOException If an error occurs while loading the configuration.
     */
    public FileStorage () throws IOException {
        this(nextAvailableConfigurationFile());
    }

    /**
     * Create a new {@code FileStorage} importing all properties from
     * the provided {@link Configuration}.
     * @param configuration The configuration.
     * @throws IOException if error occurs during configuration import.
     */
    public FileStorage (Configuration configuration) throws IOException {
        this();
        new Configuration(this).importConfiguration(configuration);
    }

    /**
     * Create a new {@code FileStorage} loading properties from a file or creating
     * it if it doesn't exist already.
     * @param configuration the file to read and write properties in
     * @throws IOException If writing to the specified output stream results in an IOException.
     */
    public FileStorage (File configuration) throws IOException {
        map  = new HashMap<String,Serializable> ();
        this.filename = configuration.getAbsolutePath();

        if (! new File(filename).exists()) {
            new Properties().storeToXML (new FileOutputStream(filename), "Created: " + new Date().toString());
        }

        reloadConfig();
    }

    /**
     * Read and write to a resource
     * @param resource name of <i>resource</i> to use as hard storage
     * @throws IOException If an error occurs while loading the configuration.
     */
    public FileStorage (String resource) throws IOException {
        this (getResourceFile(resource));
        
        reloadConfig();
    }

    private static File getResourceFile(String resource) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);

        if (url == null) {
            if (new File(resource).exists()) {
                return new File(resource);
            }
            throw new ConfigurationStorageException("Unable to find resource '"
                                                  + resource + "'"); 
        }

        try {
            return new File(url.toURI());
        } catch (URISyntaxException e) {
            //noinspection DuplicateStringLiteralInspection
            throw new RuntimeException(String.format(
                    "Unable to convert URL '%s' to URI", url));
        }
    }

    /**
     * Load initial config from an URL and store in a local file.
     * @param initialConfig The localtion from which to import all configuration data, pointing to a
     * usual properties.xml file.
     * @param persistentConfig The absolute path name to store the configuration in.
     * @throws IOException If an error occurs while loading the configuration.
     */
    public FileStorage (URL initialConfig, String persistentConfig)
                                                            throws IOException {
        this.filename = persistentConfig;
        URLConnection con = initialConfig.openConnection();
        reloadConfig(new BufferedInputStream(con.getInputStream()));
    }

    public String getFilename () {
        return filename;
    }

    private void reloadConfig () throws IOException {
        reloadConfig(new BufferedInputStream(new FileInputStream(filename)));
    }

    private void reloadConfig (InputStream in) throws IOException {

        Properties p = new Properties();
        p.loadFromXML(in);

        map.clear();

        for (Object prop : p.keySet()) {
            map.put (prop.toString(), (Serializable)p.get(prop));
        }

    }

    private void writeConfig () {
        Properties p = new Properties();
        for (Map.Entry<String,Serializable> entry : map.entrySet()) {
            p.put(entry.getKey(), entry.getValue());
        }

        try {
            OutputStream out = new BufferedOutputStream (new FileOutputStream(filename));
            p.storeToXML (out, "Last updated: " + new Date().toString());
            out.close();
        } catch (IOException e) {
            throw new ConfigurationStorageException("Unable to write output file " + filename, e);
        }

    }

    public synchronized void put(String key, Serializable value) throws IOException {
        if (key == null){
	  throw new NullPointerException("Can not store value with 'null' key");
	}
	if (value == null) {
	    // We can not store a 'null', Properties.put() throws a NPE
	    value = "";
	}
        map.put (key, value);
        writeConfig();
    }

    public synchronized Serializable get(String key) throws IOException {
        return map.get(key);
    }

    public Iterator<Map.Entry<String, Serializable>> iterator() {
        return map.entrySet().iterator();
    }

    public synchronized void purge (String key) throws IOException {
        map.remove(key);
        writeConfig();
    }

    public int size () {
        return map.size();
    }

    public boolean supportsSubStorage() {
        return false;
    }


    public ConfigurationStorage getSubStorage(String key) {
        throw new UnsupportedOperationException(NOT_SUBSTORAGE_CAPABLE);
    }

    public ConfigurationStorage createSubStorage(String key) {
        throw new UnsupportedOperationException("Not capable of handling sub"
                                                + " storages");
    }

    private static File nextAvailableConfigurationFile () throws IOException {
        int count = 0;
        File f = new File ("configuration." + count +".xml");
        while (f.exists()) {
            count++;
            f = new File ("configuration." + count +".xml");
        }
        OutputStream out = new FileOutputStream(f);
        new Properties().storeToXML (out, "Created: " + new Date().toString());
        return f;
    }

    public List<ConfigurationStorage> createSubStorages(String key, int count)
                                                            throws IOException {
        throw new UnsupportedOperationException(NOT_SUBSTORAGE_CAPABLE);
    }

    public List<ConfigurationStorage> getSubStorages(String key) throws
                                                                 IOException {
        throw new UnsupportedOperationException(NOT_SUBSTORAGE_CAPABLE);
    }
}




