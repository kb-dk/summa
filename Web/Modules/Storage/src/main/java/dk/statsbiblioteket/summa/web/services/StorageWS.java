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
package dk.statsbiblioteket.summa.web.services;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.legacy.MarcMultiVolumeMerger;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.StorageReaderClient;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * A class containing methods meant to be exposed as a web service.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mv, hbk")
@WebService
public class StorageWS {
    /**
     * Logger for StorageWS.
     */
    private static Log log = LogFactory.getLog(StorageWS.class);

    /**
     * Records namespace, used in response.
     */
    public static final String RECORDS_NAMESPACE = "http://statsbiblioteket.dk/summa/2009/Records";

    /**
     * Record collection tag, used for returning multiple records with method
     * {@link #realGetRecords(java.util.List)}..
     */
    public static final String RECORDS = "Records";
    /**
     * Record collection tags attribute, for specifying time to get records from
     * storage, used in {@link #realGetRecords(java.util.List)}.
     */
    public static final String QUERYTIME = "querytime";

    /**
     * If not existing, the fallbacks in Configuration are tried.
     */
    public static final String DEFAULT_CONF_FILE = "configuration_storage.xml";

    /** A merger pool. */
    static ArrayBlockingQueue<MarcMultiVolumeMerger> mergers;

    /** Number of merges in the merger pool. */
    private static final int NUMBER_OF_MERGERS = 10;

    /** Storage reader client. */
    static StorageReaderClient storage;
    /** Used configuration. */
    static Configuration conf;
    /**
     * XML output factory, used for creating output stream when responding with
     * multiple records.
     */
    private XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
    /** True if content should be escaped. */
    static boolean escapeContent = RecordUtil.DEFAULT_ESCAPE_CONTENT;

    /**
     * Constructor for Storage WebService.
     */
    public StorageWS() {
        synchronized (this.getClass()) {
            if (mergers != null) {
                return;
            }
            log.debug("Creating " + NUMBER_OF_MERGERS + " multi volume mergers");
            mergers = new ArrayBlockingQueue<MarcMultiVolumeMerger>(NUMBER_OF_MERGERS);
            for (int i = 0; i < NUMBER_OF_MERGERS; i++) {
                try {
                    mergers.put(new MarcMultiVolumeMerger(getConfiguration()));
                } catch (InterruptedException e) {
                    throw new RuntimeException(
                            "Interrupted while trying to add MarcMultiVolumeMergers to the queue", e);
                }
            }
            log.info("StorageWS ready");
       }
    }

    /**
     * Get a single StorageReaderClient based on the system configuration.
     *
     * @return A StorageReaderClient.
     */
    private synchronized StorageReaderClient getStorageClient() {
        if (storage == null) {
            Configuration conf = getConfiguration();
            escapeContent = conf.getBoolean(RecordUtil.CONF_ESCAPE_CONTENT, escapeContent);
            storage = new StorageReaderClient(conf);
        }
        return storage;
    }

    /**
     * Get the a Configuration object. First trying to load the configuration
     * from the location specified in the JNDI property
     * java:comp/env/confLocation, and if that fails, then the System
     * Configuration will be returned.
     *
     * @return The Configuration object.
     */
    private Configuration getConfiguration() {
        final String LOCATION = "java:comp/env/confLocation";
        if (conf == null) {
            InitialContext context;
            try {
                context = new InitialContext();
                String paramValue = (String) context.lookup(LOCATION);
                log.debug("Trying to load configuration from: " + paramValue);
                conf = Configuration.load(paramValue);
                log.info("Configuration loaded from " + paramValue);
            } catch (NamingException e) {
                if (Resolver.getURL(DEFAULT_CONF_FILE) != null) {
                    log.info("Failed to lookup env-entry but '" + DEFAULT_CONF_FILE + "' exists. Attempting load");
                    //System.setProperty("StorageWS_config", DEFAULT_CONF_FILE);
                    conf = Configuration.getSystemConfiguration("StorageWS_config", true);
                } else {
                    log.warn("Failed to lookup env-entry. Attempting load of default system configuration");
                    conf = Configuration.getSystemConfiguration(true);
                }
            }
        }
        return conf;
    }

    /**
     * Get the contents of a record (including all parent/child relations) from
     * storage.
     *
     * @param id The record id.
     * @return A String with the contents of the record (and the parent/child
     * relations) or null if unable to retrieve record.
     */
    @WebMethod
    public String getRecord(String id) {
        return realGetRecord(id, true, false);
    }

    /**
     * Get the contents of a record (including all parent/child relations) from
     * storage.
     * It will be returned in a format compatible with old Summa versions.
     *
     * @param id The record id.
     * @return A String with the contents of the record (and the parent/child
     * relations) or null if unable to retrieve record.
     */
    @WebMethod
    public String getLegacyRecord(String id) {
        return realGetRecord(id, true, true);
    }

    /**
     * Get all records specified by the supplied set of id's. This method gives
     * no new functionality, but is intended to be faster.
     *
     * @param ids List of all record id's to fetch from storage.
     * @return XML block to return directly to web-service.
     */
    @WebMethod
    public String getRecords(String[] ids) {
        List<String> list = Arrays.asList(ids);
        log.info("getRecords, fetching " + list.size() + " records from storage.");
        return realGetRecords(list);
    }

    /**
     * Private helper method to get all records, this is intended as a way to
     * get an XML block for web service, given a list of Strings (id's).
     * Sample:
       <pre>
<?xml version="1.0" ?>
<Records xmlns="http://statsbiblioteket.dk/summa/2009/Records" querytime="626">
    <record id="id1" base="base1" deleted="false" indexable="true"
            ctime="2010-03-31T09:54:53.395" mtime="2010-03-31T09:54:53.395">
        <content>data</content>
    </record>
    <record id="id2" base="base1" deleted="false" indexable="true"
            ctime="2010-03-31T09:54:53.437" mtime="2010-03-31T09:54:53.437">
        <content>data</content>
    </record>
</Records>
      </pre>
     *
     * @param ids Id's of records to fetch from storage.
     * @return List of Records specified by the given list of id's.
     */
    private String realGetRecords(List<String> ids) {
        StringWriter sw = new StringWriter(5000);
        long totalTime = 0;
        long time = 0;
        long xmlTime = 0;
        String retXML;
        XMLStreamWriter writer;

        log.debug("realGetRecords(ids[size: '" + ids.size() + "'])");

        try {
            QueryOptions queryOptions = null;
            totalTime = System.currentTimeMillis();
            List<Record> records = getStorageClient().getRecords(ids, queryOptions);
            time = System.currentTimeMillis() - totalTime;

            xmlTime = -System.currentTimeMillis();
            writer = xmlOutputFactory.createXMLStreamWriter(sw);
            writer.writeStartDocument();
            writer.setDefaultNamespace(RECORDS_NAMESPACE);
            writer.writeStartElement(RECORDS);
            writer.writeDefaultNamespace(RECORDS_NAMESPACE);
            writer.writeAttribute(QUERYTIME, String.valueOf(time));
            for (Record r : records) {
                RecordUtil.toXML(writer, 1, r, true);
            }
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            retXML = sw.toString();
            xmlTime += System.currentTimeMillis();
        } catch(IOException e) {
            log.error("Error getting #" + ids.size() + " records from storage. Total processing time was "
                    + (System.currentTimeMillis() - totalTime) + "ms. Error was: " + e, e);
            retXML = null;
        } catch(XMLStreamException e) {
            log.error("Error converting records to XML");
            retXML = null;
        }

        totalTime = System.currentTimeMillis() -totalTime;
        log.debug(String.format(Locale.ROOT,
            "Finished realGetRecords() in %dms (query: %d, xmlify: %dms)",
            totalTime, time, xmlTime));

        return retXML;
    }

    /**
     * Get the contents of a record from storage.
     *
     * @param id The record id.
     * @param expand Whether or not to include all parent/child relations when
     * getting the record.
     * @param legacyMerge Whether or not to return to record in a merged format
     * suitable for legacy use.
     * @return A String with the contents of the record or null if unable to
     * retrieve record.
     */
    private String realGetRecord(String id, boolean expand, boolean legacyMerge) {
        if (log.isTraceEnabled()) {
            log.trace(String.format(Locale.ROOT,
                    "realGetRecord('%s', expand=%b, legacyMerge=%b)",
                    id, expand, legacyMerge));
        }
        long startTime = System.currentTimeMillis();
        long xmlTime = 0;

        String retXML;
        QueryOptions q = null;
        String timing;

        try {
            if (expand) {
                q = new QueryOptions(null, null, -1, -1);
            }
            long getTime = System.currentTimeMillis();
            Record record = getStorageClient().getRecord(id, q);
            timing = "storage.getrecord.raw:" + (System.currentTimeMillis() - getTime);

            if (record == null) {
                retXML = null;
            } else {
                xmlTime = System.currentTimeMillis();
                if (legacyMerge) {
                    MarcMultiVolumeMerger merger = getMerger();
                    try {
                        retXML = merger.getLegacyMergedXML(record);
                    } finally {
                        releaseMerger(merger);
                    }
                } else {
                    retXML = RecordUtil.toXML(record, escapeContent, timing);
                }
                xmlTime = System.currentTimeMillis() - xmlTime;
            }
        } catch (IOException e) {
            log.error("Error while getting record with id: " + id, e);
            // an error occurred while retrieving the record. We simply return
            // null to indicate the record was not found.
            retXML = null;
        }

        log.debug(String.format(Locale.ROOT,
                "realGetRecord('%s', expand=%b, legacyMerge=%b) finished in %d ms (%dms spend on XML creation)",
                id, expand, legacyMerge, System.currentTimeMillis() - startTime, xmlTime));
        return retXML;
    }

    /**
     * Return a merger used by this web service.
     * @return A merger used by this web service.
     */
    private MarcMultiVolumeMerger getMerger() {
        try {
            return mergers.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(
                "Interrupted while trying to retrieve a MarcMultiVolumeMerger from the queue", e);
        }
    }

    /**
     * Release a used merger to the merger pool.
     * @param m The meger that should be released.
     */
    private synchronized void releaseMerger(MarcMultiVolumeMerger m) {
        try {
            mergers.put(m);
        } catch (InterruptedException e) {
            throw new RuntimeException(
                "Interrupted while trying to add a MarcMultiVolumeMerger to the queue", e);
        }
    }
}
