/* $Id: SearchWS.java,v 1.2 2007/10/04 13:28:21 mv Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:21 $
 * $Author: mv $
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
package dk.statsbiblioteket.summa.web.services;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.legacy.MarcMultiVolumeMerger;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.summa.common.util.StringMap;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.api.StorageReaderClient;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLStreamException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * A class containing methods meant to be exposed as a web service
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mv")
public class StorageWS {
    private Log log;

    static StorageReaderClient storage;
    Configuration conf;

    public StorageWS() {
        log = LogFactory.getLog(StorageWS.class);
    }

    /**
     * Get a single StorageReaderClient based on the system configuration.
     * @return A StorageReaderClient.
     */
    private synchronized StorageReaderClient getStorageClient() {
        if (storage == null) {
            Configuration conf = getConfiguration();
            storage = new StorageReaderClient(conf);
        }
        return storage;
    }

    /**
     * Get the a Configuration object. First trying to load the configuration from the location
     * specified in the JNDI property java:comp/env/confLocation, and if that fails, then the System
     * Configuration will be returned.
     * @return The Configuration object
     */
    private Configuration getConfiguration() {
        if (conf == null) {
            InitialContext context;
            try {
                context = new InitialContext();
                String paramValue = (String) context.lookup("java:comp/env/confLocation");
                log.debug("Trying to load configuration from: " + paramValue);
                conf = Configuration.load(paramValue);
            } catch (NamingException e) {
                log.error("Failed to lookup env-entry.", e);
                log.warn("Trying to load system configuration.");
                conf = Configuration.getSystemConfiguration(true);
            }
        }

        return conf;
    }

    /**
     * Get the contents of a record (including all parent/child relations) from storage.
     * @param id the record id.
     * @return A String with the contents of the record (and the parent/child relations)
     * or null if unable to retrieve record.
     */
    public String getRecord(String id) {
        return realGetRecord(id, true, false);
    }

    /**
     * Get the contents of a record (including all parent/child relations) from storage.
     * It will be returned in a format compatible with old Summa versions.
     * @param id the record id.
     * @return A String with the contents of the record (and the parent/child relations)
     * or null if unable to retrieve record.
     */
    public String getLegacyRecord(String id) {
        return realGetRecord(id, true, true);
    }

    /**
     * Get the contents of a record from storage.
     * @param id the record id.
     * @param expand whether or not to include all parent/child relations when getting the record
     * @param legacyMerge whether or not to return to record in a merged format sutiable for legacy use
     * @return A String with the contents of the record or null if unable to retrieve record.
     */
    private String realGetRecord(String id, boolean expand, boolean legacyMerge) {
        String retXML;
        Record record;
        QueryOptions q = null;

        try {
            if (expand) {
                q = new QueryOptions(null, null, -1, -1);
            }
            record = getStorageClient().getRecord(id, q);

            if (record == null) {
                retXML = null;
            } else {
                if (legacyMerge) {
                    MarcMultiVolumeMerger merger = new MarcMultiVolumeMerger(getConfiguration());
                    retXML = merger.getLegacyMergedXML(record);
                } else {
                    retXML = RecordUtil.toXML(record);
                }
            }
        } catch (IOException e) {
            log.error("Error while getting record with id: " + id + ". Error was: ", e);
            // an error occured while retrieving the record. We simply return null to indicate the record was not found.
            retXML = null;
        } catch (XMLStreamException e) {
            log.error("Error while converting record to XML: " + id + ". Error was: ", e);
            // an error occured while converting the record. We simply return null to indicate the error.
            retXML = null;
        }

        return retXML;
    }
}



