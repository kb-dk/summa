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
package dk.statsbiblioteket.summa.storage.database.cursors;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.util.SimplePair;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Cursor that requests & expands record data in chunks.
 */
public class ChunkedCursor implements Cursor {
    private static final Log log = LogFactory.getLog(ChunkedCursor.class);

    private final long key;
    private final DatabaseStorage storage;
    private final DatabaseStorage.OPTIMIZATION optimization;
    private final String base;
    private final QueryOptions queryOptions;

    private long lastAccess;
    private long cursor;

    private List<Record> records;
    private int recordIndex;

    public ChunkedCursor(DatabaseStorage storage, DatabaseStorage.OPTIMIZATION optimization, String base, long mTime,
                         QueryOptions queryOptions) throws Exception {
        this.storage = storage;
        this.optimization = optimization;
        this.base = base;
        this.cursor = mTime;
        this.queryOptions = queryOptions;
        this.key = storage.getTimestampGenerator().next();
        nextRecords();
    }

    @Override
    public boolean hasNext() {
        lastAccess = System.currentTimeMillis();
        return !records.isEmpty();
    }

    @Override
    public Record next() {
        lastAccess = System.currentTimeMillis();
        if (!hasNext()) {
            throw new NoSuchElementException("Iterator " + key + " depleted");
        }

        Record nextRecord = records.get(recordIndex++);
        if (recordIndex == records.size()) {
            try {
                nextRecords();
            } catch (Exception e) {
                log.warn("Error reading next record: " + e.getMessage(), e);
                nextRecord = null;
                records.clear();
            }
        }
        return nextRecord;
    }


    private void nextRecords() throws Exception {
        log.debug("Requesting next Records from base '" + base + "' with optimization " + optimization);
        SimplePair<List<Record>, Long> response =
                storage.getRecordsModifiedAfterOptimized(cursor, base, queryOptions, optimization);
        recordIndex = 0;
        records = response.getKey();
        cursor = response.getValue();
        lastAccess = System.currentTimeMillis();
    }

    @Override
    public void close() {
        records.clear();
        lastAccess = System.currentTimeMillis();
    }

    @Override
    public long getKey() {
        return key;
    }

    @Override
    public long getLastAccess() {
        return lastAccess;
    }

    @Override
    public QueryOptions getQueryOptions() {
        return queryOptions;
    }

    @Override
    public String getBase() {
        return base;
    }

    @Override
    public void remove() {
        recordIndex++;
    }

    @Override
    public boolean needsExpansion() {
        return false;
    }
}
