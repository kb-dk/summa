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
package dk.statsbiblioteket.summa.storage.api.filter;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.storage.api.*;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.*;

/**
 * Take a fulldump and treat it as a update, where non-existing records should
 * be deleted, all other records are inserted.
 * Note: Existing records should not have their {@link Record#modificationTime}
 * updated.
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
 * </p><p>
 * Note: As the ids are sent through RMI and kept in RAM, this filter is very
 * resource intensive for bases with a large number of records. A rule of thumb
 * is that bases with 20+ million records should be cleared with
 * {@link ClearBaseFilter}.
 * </p><p>
 * In order to use this, the property
*{@link dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer#CONF_RPC_TARGET}
 * must set to a Storage.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te, hbk",
        reviewers = "te")
public class UpdateFromFulldumpFilter extends ObjectFilterImpl{
    private Log log = LogFactory.getLog(UpdateFromFulldumpFilter.class);
    // storage to manipulate.
    private ReadableStorage readableStorage = null;
    private WritableStorage writableStorage = null;

    /**
     * The target base.
     * </p><p>
     * Mandatory. If '*' is specified, all bases are used. Warning: This is
     *            normally not the right thing to do.
     */
    public static final String CONF_BASE = "summa.ingest.stream.updatefromfulldumpfiler.base";

    /**
     * Maximum number of records to delete from storage, without going down with
     * an error.
     * </p><p>
     * Optional, but highly recommended. Default is 10.
     */
    public static final String CONF_MAX_NUMBER_DELETES = "summa.ingest.stream.updatefromfulldumpfiler.maxnumberdeletes";
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
     * Default value {@link #CONF_NUMBER_OF_RECORDS_FROM_STORAGE}.
     */
    public static final int DEFAULT_NUMBER_OF_RECORDS_FROM_STORAGE = 100;


    private String base = null;
    /**
    * Value of {@link #CONF_MAX_NUMBER_DELETES}
    * if set otherwise {@link #DEFAULT_MAX_NUMBER_DELETES }.
    */
   private int maxNumberDeletes = 0;

    /**
     * Value of {@link #CONF_MAX_NUMBER_DELETES} if set otherwise
     * {@link #DEFAULT_MAX_NUMBER_DELETES }.
     */
    private int numberOfRecordsFromStorage = 0;

    /**
     * Set containing ids for records in storage.
     */
    protected Set<String> ids = null;

    private boolean recordsGotten = false;
    /**
     * Constructor
     * SideEffect: Fetch a copy of storage ID's for local storage.
     *
     * @param config configuration for the running version.
     */
    public UpdateFromFulldumpFilter(Configuration config) {
        super(config);
        feedback = false;
        init(config, new StorageWriterClient(config), new StorageReaderClient(config));
    }

    protected void init(Configuration config, WritableStorage writableStorage, ReadableStorage readableStorage) {
        this.writableStorage = writableStorage;
        this.readableStorage = readableStorage;
        
        if (!config.valueExists(CONF_BASE)) {
            throw new ConfigurationException("The property " + CONF_BASE + " must be defined");
        }
        base = config.getString(CONF_BASE);
        if ("*".equals(base)) {
            base = null;
            log.info("The base was specified as *, which matches all records in storage. Note that this only makes "
                     + "sense when the storage contains only a single base or when the current ingest-chain performs "
                     + "full ingests from all bases ");
        }

        maxNumberDeletes = config.getInt(CONF_MAX_NUMBER_DELETES, DEFAULT_MAX_NUMBER_DELETES);
        numberOfRecordsFromStorage = config.getInt(CONF_NUMBER_OF_RECORDS_FROM_STORAGE,
                                                   DEFAULT_NUMBER_OF_RECORDS_FROM_STORAGE);

        ids = new HashSet<>();
        log.debug(String.format(Locale.ROOT, "Initialized with base '%s', maxNumberDeletes %d, numberOfRecordsFromStorage %d",
                                base, maxNumberDeletes, numberOfRecordsFromStorage));
    }

    /**
     * Get all record id's from storage. These ids are place in a local
     * Map for later usage. 
     */
    private void getRecords() {
        // get a local copy of all records id.
        QueryOptions.ATTRIBUTES[] attributesNeeded = new QueryOptions.ATTRIBUTES[] {
            QueryOptions.ATTRIBUTES.ID, QueryOptions.ATTRIBUTES.BASE, QueryOptions.ATTRIBUTES.MODIFICATIONTIME};
        QueryOptions queryOptions = new QueryOptions(false, null, 0, 0, null, attributesNeeded);
        Profiler profiler = new Profiler();
        try {
            long iteratorKey = readableStorage.getRecordsModifiedAfter(0, base, queryOptions);
            log.debug("Got iterator key for base '" + base + "'");
            List<Record> tmpRecords;
            int recieved = 0;
            do {
                tmpRecords = readableStorage.next(iteratorKey, numberOfRecordsFromStorage);
                for(Record r: tmpRecords) {
                    ids.add(r.getId());
                    recieved++;
                    if (log.isDebugEnabled() && (recieved & 0x00000FFF) == 0) { // Every 4096
                        log.debug("Received " + recieved + " IDs for base '" + base + "'. Continuing...");
                    }
                }
            } while (tmpRecords.size() == numberOfRecordsFromStorage);
            log.info("IDs for all '" + recieved + "' records from storage base '" + base + "' has been locally stored");
        } catch (NoSuchElementException e) {
            log.info("Got NoSuchElementException in getRecords for base '" + base + "'. This is ok at it is the last "
                     + "element. Stored a total of " + ids.size() + "IDs in " + profiler.getSpendTime());
            // last element ok not to report this error.   
        } catch (IOException e) {
            log.warn("IOException on communication with Storage. ID-list is not guaranteed to be completed: This might"
                     + " leave records after ingest that should have been marked as deleted", e);
        }
    }

    /**
     * For each record received this filter is un-marking the record in the local
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
            throw new PayloadException("null received in Payload in next(). This should not happen");
        }

        if(!recordsGotten) {
            log.info("Getting all records id from storage for base '" + base + "'");
            getRecords();
            recordsGotten = true;
        }

        ids.remove(r.getId());
        Logging.logProcess("UpdateFromFulldumpFilter", "Marking as existing", Logging.LogLevel.TRACE, payload);
        return true;
    }

    /**
     * Overridden from Filter. Delete non inserted records, if less than
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
            log.info("Closing update from fulldump, means deleting non-matched records.");
            if(ids.size() < maxNumberDeletes) {
                Profiler profiler = new Profiler();
                try {
                    // TODO: We can do this a lot faster with a delete job that takes a list of IDs
                    for (String id: ids) { // TODO: We could do this in bulk but beware of OOM
                        Record tmp = readableStorage.getRecord(id, null);
                        if (tmp != null) {
                            tmp.setDeleted(true);
                            tmp.setIndexable(false);
                            Logging.logProcess("UpdateFromFulldumpFilter", "Marking as deleted and not idexable",
                                               Logging.LogLevel.DEBUG, id);
                            writableStorage.flush(tmp);
                        } else {
                            Logging.logProcess("UpdateFromFulldumpFilter",
                                               "Attempted marking as deleted, but the Record did not exist",
                                               Logging.LogLevel.DEBUG, id);
                        }
                    }
                    log.info("Marked " + ids.size() + " records as deleted and not indexable from base " + base
                             + " in " + profiler.getSpendTime());
                } catch(IOException e) {
                    log.error("IOException when deleting records from storage. Storage now contains deleted records");
                }
            } else {
                log.error("The number of records to delete from storage for base " + base + " is too great: "
                        + ids.size() + " > " + maxNumberDeletes + ". "
                        + "No records are deleted. Storage probably contains records that should have been deleted");
            }
        } else {
            log.error("Dirty closure of UpdateFromFulldumpFilter. No Records are removed from Storage. There should "
                      + "have been removed: " + ids.size() + " records");
        }
        // Note: Do not close writableStorage, as is closes the server Storage
        log.info("Closed UpdateFromFulldumpFilter");
    }
}
