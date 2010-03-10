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
package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Take a fulldump and treat it as a update, where non-existing records should
 * be deleted, all other records are inserted.
 * Note: Existing records should not have there
 * {@link Record#modificationTime} updated.
 *
 * <ul>
 *  <li>Take the full storage and save a local copy of all ID's.</li>
 *  <li>For each record in input payloads (Fulldump).
 *      <ul>
 *          <li>Mark each record from dump as existing (locally).</li>
 *      </ul>
 *  </li>
 *  <li>Finally delete non-marked records from storage</li>
 * </ul>
 *
 * @author Henrik Kirk <hbk@statsbiblioteket.dk>
 * @since 2010-19-02
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_OK,
        author = "hbk",
        reviewers = "te")
public class UpdateFromFulldumpFilter extends ObjectFilterImpl{
    private Log log = LogFactory.getLog(UpdateFromFulldumpFilter.class);
    // storage to manipulate.
    private Storage storage = null;

    /**
     * Maximum number of records to delete from storage, without going down with
     * an error.
     * </p><p>
     * Optional, but highly recommended. Default is 10.
     */
    public static final String CONF_MAX_NUMBER_DELETES =
                 "summa.ingest.stream.updatefromfulldumpfiler.maxnumberdeletes";
    /**
     * Default value of {@link UpdateFromFulldumpFilter#CONF_MAX_NUMBER_DELETES}.
     */
    public static final int DEFAULT_MAX_NUMBER_DELETES = 10;

    /**
     * Maximum number of records to get from storage at each
     * {@link Storage#next(long, int)} records.
     * </p><p>
     * Optional. Default is 100.
     */
    public static final String CONF_NUMBER_OF_RECORDS_FROM_STORAGE =
    "summa.ingest.stream.updatefromfulldumpfiler.numberofrecordsfromstorage";
    /**
     * Default value {@link this#CONF_NUMBER_OF_RECORDS_FROM_STORAGE}.
     */
    public static final int DEFAULT_NUMBER_OF_RECORDS_FROM_STORAGE = 100;

    /**
    * Value of {@link this#CONF_MAX_NUMBER_DELETES}
    * if set otherwise {@link this#DEFAULT_MAX_NUMBER_DELETES }.
    */
   private int maxNumberDeletes = 0;

    /**
     * Value of {@link this#CONF_MAX_NUMBER_DELETES} if set otherwise
     * {@link this#DEFAULT_MAX_NUMBER_DELETES }.
     */
    private int numberOfRecordsFromStorage = 0;

    /**
     * Map containing ids for records in storage.
     */
    private Map<String, Record> ids = null;

    /**
     * Constructor
     * SideEffect: Fetch a copy of storage ID's for local storage.
     *
     * @param config configuration for the running version.
     */
    public UpdateFromFulldumpFilter(Configuration config) {
        super(config);
        try {
            storage = StorageFactory.createStorage(config);
        } catch (IOException e) {
            throw new ConfigurationException("Unable to connect to Storage", e);
        }

        init(config);
    }

    UpdateFromFulldumpFilter(Storage storage, Configuration config) {
        super(config);
        this.storage = storage;
        init(config);
    }

    private void init(Configuration config) {
        maxNumberDeletes = config.getInt(CONF_MAX_NUMBER_DELETES
                                                  , DEFAULT_MAX_NUMBER_DELETES);
        numberOfRecordsFromStorage =
                          config.getInt(CONF_NUMBER_OF_RECORDS_FROM_STORAGE,
                                   DEFAULT_NUMBER_OF_RECORDS_FROM_STORAGE);

        ids = new HashMap<String, Record>();

        log.info("Get all records id from storage.");
        getRecords();
    }

    /**
     * Get all record id's from storage. These ids are place in a local
     * Map for later usage. 
     */
    private void getRecords() {
        // get a local copy of all records id.
        try {
            long iteratorKey = storage.getRecordsModifiedAfter(0, null, null);
            List<Record> tmpRecords;
            int i = 0;
            do {
                tmpRecords = storage.next(iteratorKey,
                                                    numberOfRecordsFromStorage);
                for(Record r: tmpRecords) {
                    ids.put(r.getId(), null);
                    i++;
                }
            }
            while(tmpRecords.size() == numberOfRecordsFromStorage);
            log.info("All '" + i + "' records from storage has been locally "
                     + "stored");
        } catch (NoSuchElementException e) {
            // last element ok not to report this error.   
        } catch (IOException e) {
            log.warn("IOException on communication with storage.", e);    
        }
    }

    /**
     * For each record recieved this filter is unmarking the record in the local
     * storage copy.
     *
     * @param payload the Payload to process.
     * @return true if no error where detected, false otherwise. Eg. return
     * false, if {@link Payload#getRecord()} == null.
     * @throws PayloadException if payload is null.
     */
    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        Record r = payload.getRecord();
        if(r == null) {
            throw new PayloadException("null received in Payload in next()"
                                       + ". This should not happen");
        }
        log.info("Process record '" + r.getId() + "' ok.");
        ids.remove(r.getId());
        Logging.logProcess(
                "UpdateFromFulldumpFilter", "Marking as existing",
                Logging.LogLevel.TRACE, payload);
        return true;
    }

    /**
     * Overrided from Filter. Delete non inserted records, if less than
     * {@link Configuration#getInt(String, int)} with parameters
     * {@link UpdateFromFulldumpFilter#CONF_MAX_NUMBER_DELETES} and
     * {@link UpdateFromFulldumpFilter#DEFAULT_MAX_NUMBER_DELETES}. 
     * 
     * @param ok true if everything was okay, false on dirty closure.
     */
    @Override
    public void close(boolean ok) {
        super.close(ok);
        // Clean closure
        if(ok) {
            log.info("Closing update from fulldump, means deleting non-matched "
                + "records.");
            if(ids.size() < maxNumberDeletes) {
                try {
                    for(String id: new ArrayList<String>(ids.keySet())) {
                        Record tmp = storage.getRecord(id, null);
                        tmp.setDeleted(true); // TODO i think
                        Logging.logProcess(
                                "UpdateFromFulldumpFilter",
                                "Marking as deleted",
                                Logging.LogLevel.DEBUG, id);
                        storage.flush(tmp);
                    }
                    log.info("Marked '" + ids.size() + "' records as deleted.");
                } catch(IOException e) {
                    log.error("IOException when deleting records from storage. "
                              +"Storage now contains deleted records.");    
                }
            } else {
                log.error("Number of records to delete from storage is '"
                        + ids.size() + "' > '" + maxNumberDeletes + "', "
                        + "so no records are delete, storage is now "
                        + "containing delete records.");
            }
        } else {
            log.error("Dirty closure of UpdateFromFulldumpFilter, are not "
                + "removing any records from storage. There should have been "
                + "removed: " + ids.size() + " records");
        }
        log.info("Closed UpdateFromFulldumpFilter.");
    }
}
