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

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.storage.api.Storage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.*;

/**
 * Take a fulldump and treat it as a update, where non-existing records should
 * be deleted, all other records are inserted.
 * Note: Existing records should not have there
 * {@link dk.statsbiblioteket.summa.common.Record#modificationTime} updated.
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
public class UpdateFromFulldumpFilter extends ObjectFilterImpl{
    private Log log = LogFactory.getLog(UpdateFromFulldumpFilter.class);
    // storage to manipulate.
    private Storage storage = null;

    /**
     * Maxium number of records to delete from storage, without going down with
     * an error.
     */
    public static final String CONF_MAX_NUMBER_DELETES =
                 "summa.ingest.stream.updatefromfulldumpfiler.maxnumberdeletes";
    /**
     * Default value of {@link dk.statsbiblioteket.summa.ingest.stream.UpdateFromFulldumpFilter#CONF_MAX_NUMBER_DELETES}.
     */
    public static final int DEFAULT_MAX_NUMBER_DELETES = 100;

    /**
     * Value of {@link this#CONF_MAX_NUMBER_DELETES}
     * if set otherwise {@link this#DEFAULT_MAX_NUMBER_DELETES }.
     */
    private int maxNumberDeletes = 0;

    /**
     * Maximum number of records to get from storage at each
     * {@link Storage#next(long, int)} records.
     */
    public static final String CONF_NUMBER_OF_RECORDS_FROM_STORAGE =
    "summa.ingest.stream.updatefromfulldumpfiler.numberofrecordsfromstorage";
    /**
     * Default value {@link this#CONF_NUMBER_OF_RECORDS_FROM_STORAGE}.
     */
    public static final int DEFAULT_NUMBER_OF_RECORDS_FROM_STORAGE = 100;

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
     * @param storage the storage, where we should insert and possibly delete
     * records from.
     */
    public UpdateFromFulldumpFilter(Storage storage, Configuration config) {
        super(config);

        maxNumberDeletes = config.getInt(CONF_MAX_NUMBER_DELETES
                                                  , DEFAULT_MAX_NUMBER_DELETES);
        numberOfRecordsFromStorage =
                          config.getInt(CONF_NUMBER_OF_RECORDS_FROM_STORAGE,
                                   DEFAULT_NUMBER_OF_RECORDS_FROM_STORAGE);

        ids = new HashMap<String, Record>();

        this.storage = storage;
        log.info("Get all records id from storage.");
        getRecords();
    }

    private void getRecords() {
        /*  -  Print number of records in storage.
        StringMap meta = new StringMap();
        meta.put("ALLOW_PRIVATE", "true");
        QueryOptions opts = new QueryOptions(null, null, 0, 0, meta);
        Record holdings = storage.getRecord("__holdings__", opts);
        log.info("Number of records in storage: " + storage.);
        */
        // get a local copy of all records id.
        try {
            long iteratorKey = storage.getRecordsModifiedAfter(0, null, null);
            List<Record> records = null;
            do {
                records = storage.next(iteratorKey, numberOfRecordsFromStorage);
                for(Record r: records) {
                    ids.put(r.getId(), r);
                }
            }
            while(records.size() == numberOfRecordsFromStorage);

        } catch (NoSuchElementException e) {
            // last element   
        } catch (IOException e) {
            log.warn("IOException on communication with storage.", e);    
        }
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        return false;
    }

    /**
     * Overrided from Filter. Delete non inserted records, if less than
     * {@link }
     * 
     * @param ok true if everything was okay, false on dirty closure.
     */
    @Override
    public void close(boolean ok) {
        // Clean closure
        if(ok) {
            if(ids.size() < maxNumberDeletes) {
                try {
                    storage.flushAll(new ArrayList(ids.values()));
                } catch(IOException e) {
                    // TODO throw error
                }
            } else {
                // TODO throw error.
            }
        } else {
            log.error("Dirty closure of UpdateFromFulldumpFilter, are not "
                + "removing any records from storage. There should have been "
                + "removed: " + ids.size() + " records");
        }
    }
}
