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
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.api.StorageReaderClient;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A class containing methods meant to be exposed as a web service
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mv")
public class StorageWS {
    private Log log;

    StorageReaderClient storage;
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
            if (conf == null) {
                conf = Configuration.getSystemConfiguration(true);
            }
            storage = new StorageReaderClient(conf);
        }
        return storage;
    }

    /**
     * Get the contents of a record from storage.
     * @param id the record id.
     * @return A String with the contents of the record or null if unable to retrieve record.
     */
    public String getRecord(String id) {
        String retXML;

        Record record;

        try {
            List<Record> recs = getStorageClient().getRecords(Arrays.asList(id), 0);

            if (recs.size() == 0) {
                retXML = null;
            } else {
               retXML = recs.get(0).getContentAsUTF8();
            }
        } catch (IOException e) {
            log.error("Error while getting record with id: " + id + ". Error was: ", e);
            // an error occured while retrieving the record. We simply return null to indicate the record was not found.
            retXML = null;
        }

        return retXML;
    }
}



