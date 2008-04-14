/* $Id: XStorage.java,v 1.3 2007/10/04 13:28:21 te Exp $
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

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te",
        comment = "Class and some methods needs Javadoc")
public class XStorage implements ConfigurationStorage {
    public static final String DEFAULT_RESOURCE = "xconfiguration.xml";

    private File storageFile;
    private XProperties xprops;
    private Log log = LogFactory.getLog(XStorage.class);

    /**
     * Creates a XStorage around a XProperties.
     * @param properties the properties to wrap around.
     */
    /*protected XStorage(XProperties properties) throws IOException {
        this (nextAvailableConfigurationFile());
        assignFrom(properties);
        syncStorageFile();
    }*/

    public XStorage() throws IOException {
        this(nextAvailableConfigurationFile());
    }

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

    public XStorage(File configurationFile) throws IOException {
        storageFile = configurationFile;
        xprops = new XProperties();
        if (!configurationFile.exists()) {
            syncStorageFile();
        } else {
            xprops.load(configurationFile.getAbsoluteFile().toString(),
                 false, false);
        }
    }

    /**
     * Return the absolute path of the file backing this storage.
     * @return absolute file path
     */
    public String getFilename () {
        return storageFile.getAbsolutePath();
    }

    public void put(String key, Serializable value) throws IOException {
        xprops.put(key, value);
        syncStorageFile();
    }

    public Serializable get(String key) {
        try {
            return (Serializable)xprops.getObject(key);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public Iterator<Map.Entry<String, Serializable>> iterator() throws
                                                                IOException {
        log.warn("Iterators are not fully supported by XStorage. "
                 + "This won't work well with nesting");
        Map<String, Serializable> tempMap =
                new HashMap<String, Serializable>(size());
            for (Map.Entry<Object, Object> entry: xprops.entrySet()) {
                tempMap.put((String)entry.getKey(),
                            (Serializable)entry.getValue());
            }
        log.trace("Created shallow copy of storage with " + size()
                  + " elements. Returning iterator");
        return tempMap.entrySet().iterator();
    }

    public void purge(String key) throws IOException {
        xprops.remove(key);
        syncStorageFile();
    }

    public int size() {
        return xprops.size();
    }

    public boolean supportsSubStorage() {
        return true;
    }

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
            log.warn("Unclear cemantics. " 
                     + "See https://gforge.statsbiblioteket.dk/"
                     + "tracker/index.php?aid=1186");
            return new XStorage((XProperties)sub);
        } catch (NullPointerException e) {
            throw new IOException("Could not locate value for key '" + key
                                  + "':", e);
        }
    }

    public ConfigurationStorage createSubStorage(
            String key) throws IOException {
        put(key, new XProperties());
        return getSubStorage(key);
    }

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

    private void syncStorageFile () throws IOException {
        xprops.store (new BufferedOutputStream(
                              new FileOutputStream (storageFile)), null);
    }
}
