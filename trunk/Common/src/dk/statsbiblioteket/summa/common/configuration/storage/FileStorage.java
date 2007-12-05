/* $Id: FileStorage.java,v 1.3 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.3 $
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
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorageException;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.*;
import java.io.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLConnection;

/**
 * A {@link ConfigurationStorage} backed by a file.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment = "Class and methods needs better docs, especially for throws")
public class FileStorage implements ConfigurationStorage {

    public static final String DEFAULT_RESOURCE = "configuration.xml";

    private HashMap<String,Serializable> map;
    private String filename;

    /**
     * Create a file storage backed by the next file of the form configuration.N.xml
     * that doesn't already exist for some natural number N.
     * @throws IOException
     */
    public FileStorage () throws IOException {
        this(nextAvailableConfigurationFile());
    }

    /**
     * Create a new {@code FileStorage} importing all properties from
     * the provided {@link Configuration}.
     * @param configuration
     * @throws IOException
     */
    public FileStorage (Configuration configuration) throws IOException {
        this();
        new Configuration(this).importConfiguration(configuration);
    }

    /**
     * Create a new {@code FileStorage} loading properties from a file or creating
     * it if it doesn't exist already.
     * @param configuration the file to read and write properties in
     * @throws IOException
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
     * @param filename name of <i>resource</i> to use as hard storage
     * @throws IOException
     */
    public FileStorage (String filename) throws IOException {
        this (new File(Thread.currentThread().getContextClassLoader().getResource(filename).getFile()));
        
        reloadConfig();
    }

    /**
     * Load initial config from an URL and store in a local file
     * @param initialConfig the localtion from which to import all configuration data, pointing to a usual properties.xml file
     * @param persistentConfig the absolute path name to store the configuration in
     * @throws IOException
     */
    public FileStorage (URL initialConfig, String persistentConfig) throws IOException {
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
        throw new UnsupportedOperationException("Not capable of handling sub"
                                                + " storages");
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
}
