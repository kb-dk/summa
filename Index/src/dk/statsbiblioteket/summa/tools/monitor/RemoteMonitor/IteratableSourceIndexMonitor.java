/* $Id: IteratableSourceIndexMonitor.java,v 1.6 2007/10/05 10:20:24 te Exp $
 * $Revision: 1.6 $
 * $Date: 2007/10/05 10:20:24 $
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
package dk.statsbiblioteket.summa.tools.monitor.RemoteMonitor;

import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.index.IndexService;
import dk.statsbiblioteket.summa.tools.monitor.Monitor;
import dk.statsbiblioteket.summa.common.lucene.index.IndexServiceException;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.HashMap;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class IteratableSourceIndexMonitor extends Monitor {

    private long timestamp;
    private long persistentMapMaxSize = 100000;

    private String lastRecordID;
    private boolean completedFullIndex;
    private Properties p;
    private static final String prop = "iteratableSourceIndexMonitor.properties";
    private static final Log log = LogFactory.getLog(IteratableSourceIndexMonitor.class);

    private static final String TARGET = "horizon";


    private static String remoteStoreServer;
    private static String remoteStoreServiceName;
    private static String remoteStoreServicePort;

    private static String remoteIndexServer;
    private static String remoteIndexServiceName;
    private static String remoteIndexServicePort;

    private Storage store;
    private IndexService indexer;

    private String persistenDirectory;

    private URL prop_location;

    private Map currentMap;
    private String mapName;

    private IteratableSourceIndexMonitor() {
        super();
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        prop_location = loader.getResource(prop);
        final InputStream in = loader.getResourceAsStream(prop);
        currentMap = null;
        mapName = null;
        p = new Properties();

        try {
            p.loadFromXML(in);
            log.info("loading settings from iteratabelSourceIndexMonitor.properties");
            timestamp = Long.parseLong(p.getProperty("timestamp", "0"));
            lastRecordID = p.getProperty("lastRecordID", "0");
            completedFullIndex = Boolean.parseBoolean(p.getProperty("completedFullIndex"));
            persistenDirectory = p.getProperty("persistenDirectory");
            remoteStoreServer = p.getProperty("remoteStoreServer");
            remoteStoreServiceName = p.getProperty("remoteStoreServiceName");
            remoteStoreServicePort = p.getProperty("remoteStoreServicePort", "1099");
            remoteIndexServer = p.getProperty("remoteIndexServer");
            remoteIndexServiceName = p.getProperty("remoteIndexServiceName");
            remoteIndexServicePort = p.getProperty("remoteIndexServicePort", "1099");
            in.close();
        } catch (IOException e) {
            log.error("Cannot load properties", e);
        }

        try {
            super.init(persistenDirectory);
        } catch (IOException e) {
            log.error(e);
        } catch (ClassNotFoundException e) {
            log.error(e);
        }

        if (System.getSecurityManager() == null)
            System.setSecurityManager(new RMISecurityManager());
        try {
            store = (Storage) Naming.lookup("//" + remoteStoreServer + ":" + remoteStoreServicePort + "/" + remoteStoreServiceName);
            log.info("Got ref to remoteStore: //" + remoteStoreServer + ":" + remoteStoreServicePort + "/" + remoteStoreServiceName );
            indexer = (IndexService) Naming.lookup("//" + remoteIndexServer + ":" + remoteIndexServicePort + "/" + remoteIndexServiceName);
            log.info("Got ref to Indexing engine: //" + remoteIndexServer + ":" + remoteIndexServicePort + "/" + remoteIndexServiceName);
        } catch (MalformedURLException e) {
            log.fatal("Error getting remote store:", e);
            System.exit(-1);
        } catch (NotBoundException e) {
            log.fatal("Error getting remote store:", e);
            System.exit(-1);
        } catch (RemoteException e) {
            log.fatal("Error getting remote store:", e);
            System.exit(-1);
        }
    }

    public static Monitor getInstance() {
        if (_instance == null) {
            _instance = new IteratableSourceIndexMonitor();
        }
        return _instance;
    }

    public static Monitor getInstance(final String store) {
        if (_instance == null) {
            persist = store;
            _instance = new IteratableSourceIndexMonitor();
        }
        return _instance;
    }

    /**
     * Gets excecuted by the {@link dk.statsbiblioteket.summa.tools.schedule.Scheduler} where the component is registered.
     */
    public void perform() {

        synchronized (this) {
            log.debug("Starting scheduled task");
            Iterator iter = null;

            long tempTimestamp = 0;
            try {
                if (!completedFullIndex) {
                    if (!"".equals(lastRecordID)) {
                        log.debug("resuming iteration from:  " + lastRecordID);
                        iter = store.getRecordsFrom(lastRecordID, TARGET);
                    } else {
                        log.debug("Getting all records");
                        iter = store.getRecords(TARGET);
                    }
                } else {
                    log.debug("Getting records modified after: "  + timestamp);
                    iter = store.getRecordsModifiedAfter(timestamp, TARGET);
                }
            } catch (IOException e) {
                log.error("Error in getting remote iterator", e);
            }

            assert iter != null;
            int recordBlockCounter = 0;
            while (iter.hasNext()) {
                try {
                    log.debug("Getting next record");
                    Record record = (Record) iter.next();
                    log.debug("Got next remote record");
                    String id = record.getId();
                    int indexOF = id.indexOf("-");
                    String prefix = "";
                    if (indexOF > 0){
                        prefix = id.substring(0,id.indexOf("-"));
                    } else {
                        indexOF = 0;
                    }
                    String map_name;
                    if (id.substring(indexOF+1).matches("^d*$")){
                        map_name = prefix + ((int) Math.floor(Integer.parseInt(id.substring(indexOF)) / persistentMapMaxSize));
                    } else {
                        map_name = "alpha";
                    }
                    Map m = getMap(map_name);

                    log.debug("got record: " + id + " to be found in: " + map_name);

                    boolean delete = record.isDeleted();
                    if (m.containsKey(id)) {
                        if (delete) {
                            log.debug("removeing from index: " + id);
                            indexer.removeRecord(id);
                            m.remove(id);
                        } else if (((Long) m.get(id)).longValue() < record.getLastModified()) {
                            log.debug("updating in index: " + id);
                            indexer.updateXMLRecord(new String(record.getContent(), "UTF-8"), id, TARGET);
                            m.put(id, record.getLastModified());
                        }
                    } else {
                        String content = new String(record.getContent(), "UTF-8");
                        log.debug("adding to index: " +id);
                        log.debug(id + "has content:" + content);
                        indexer.addXMLRecord(content, id, TARGET);
                        m.put(id, record.getLastModified());
                    }

                    tempTimestamp = record.getLastModified() > tempTimestamp ? record.getLastModified() : tempTimestamp;
                    lastRecordID = record.getId();
                    timestamp = tempTimestamp;

                    p.setProperty("lastRecordID", record.getId());
                    p.setProperty("timestamp", Long.toString(timestamp));

                    // store properties once in a while
                    if (++recordBlockCounter >= 1000){
                        storeProperties();
                        recordBlockCounter = 0;
                    }

                } catch (IndexServiceException e) {
                    log.error("Indexing error", e);
                } catch (UnsupportedEncodingException e) {
                    log.error(e);
                } catch (IOException e) {
                    log.error(e);
                } 
            }

            completedFullIndex = true;
            p.setProperty("completedFullIndex", Boolean.toString(completedFullIndex));
            storeProperties();

            try {
                indexer.optimizeAll();
            } catch (IndexServiceException e) {
                log.error(e);
            } catch (RemoteException e) {
                log.error(e);
            }
        }

    }

    private void storeProperties() {

        String propFile;
        if ("".equals(propFile = prop_location.getFile())) {
            log.fatal("no place to store properties");
            System.exit(-1);
        }
        try {
            FileOutputStream fs = new FileOutputStream(propFile);
            p.storeToXML(fs, null);
            fs.close();
        } catch (FileNotFoundException e) {
            log.error("Store properties failed", e);
        } catch (IOException e) {
            log.error("Store properties failed", e);
        }

    }


    private Map getMap(String map_name) {

        if (map_name.equals(mapName)) {
            return currentMap;
        } else {
            if (mapName != null)super.recordIndexClusters.put(mapName, currentMap);
            currentMap = (HashMap)super.recordIndexClusters.get(map_name);
        }

        if (currentMap == null) {
                currentMap = new HashMap();
        }

        mapName = map_name;
        return currentMap;

    }
}
