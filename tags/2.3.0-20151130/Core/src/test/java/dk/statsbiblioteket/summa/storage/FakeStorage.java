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
package dk.statsbiblioteket.summa.storage;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple in-memory Storage implementation used for testing.
 * </p><p>
 * Limitations: Query Options are ignored as well as explicit parent/child-handling.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FakeStorage implements Storage {
    private static Log log = LogFactory.getLog(FakeStorage.class);

    private Map<String, Record> records;
    private int maxBatchSize;
    private Map<Long, List<Record>> iterators = new HashMap<>();
    private AtomicLong iteratorCount = new AtomicLong(0);

    /**
     * @param records starting records.
     */
    public FakeStorage(List<Record> records) {
        this(records, Integer.MAX_VALUE);
    }

    /**
     * @param records   starting records.
     * @param maxBatchSize the maximim number of records to deliver from {@link #next(long)}.
     */
    public FakeStorage(List<Record> records, int maxBatchSize) {
        this.records = new HashMap<>(records.size());
        for (Record record: records) {
            this.records.put(record.getId(), record);
        }
        this.maxBatchSize = maxBatchSize;
        log.info("Created " + this);
    }

    @Override
    public synchronized long getRecordsModifiedAfter(long time, String base, QueryOptions options) throws IOException {
        log.debug("getRecordsModifiedAfter(" + time + ", " + base + ", " + options + ") called");
        long iteratorID = iteratorCount.getAndIncrement();
        List<Record> result = new ArrayList<>();
        for (Map.Entry<String, Record> pair: records.entrySet()) {
            Record record = pair.getValue();
            if ((base == null || record.getBase().equals(base)) && record.getModificationTime() > time) {
                result.add(record);
            }
        }
        iterators.put(iteratorID, result);
        log.debug("Created iterator " + iteratorID + " with " + result.size() + " Records");
        return iteratorID;
    }

    @Override
    public synchronized long getModificationTime(String base) throws IOException {
        long last = 0;
        for (Map.Entry<String, Record> pair: records.entrySet()) {
            Record record = pair.getValue();
            if (record.getBase().equals(base)) {
                last = Math.max(last, record.getLastModified());
            }
        }
        return last;
    }

    @Override
    public synchronized List<Record> getRecords(List<String> ids, QueryOptions options) throws IOException {
        List<Record> records = new ArrayList<>();
        for (String id: ids) {
            Record record = getRecord(id, options);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    @Override
    public synchronized Record getRecord(String id, QueryOptions options) throws IOException {
        return records.get(id);
    }

    @Override
    public Record next(long iteratorKey) throws IOException {
        List<Record> records = iterators.get(iteratorKey);
        if (records == null || records.isEmpty()) {
            throw new NoSuchElementException("No record for iterator key " + iteratorKey);
        }
        return records.remove(0);
    }

    @Override
    public List<Record> next(long iteratorKey, int maxRecords) throws IOException {
        int realMax = Math.min(maxRecords, maxBatchSize);
        List<Record> results = new ArrayList<>(realMax);

        for (int i = 0 ; i < realMax ; i++) {
            try {
                results.add(next(iteratorKey));
            } catch (NoSuchElementException e) {
                if (records.isEmpty()) {
                    iterators.remove(iteratorKey);
                    throw new NoSuchElementException("No more Records for iterator key " + iteratorKey);
                }
                break;
            }
        }
        return results;
    }

    @Override
    public synchronized void flush(Record record, QueryOptions options) throws IOException {
        flush(record);
    }

    @Override
    public void flush(Record record) throws IOException {
        records.put(record.getId(), record);
    }

    @Override
    public void flushAll(List<Record> records, QueryOptions options) throws IOException {
        flushAll(records);
    }

    @Override
    public void flushAll(List<Record> records) throws IOException {
        for (Record record: records) {
            flush(record);
        }
    }

    @Override
    public void close() throws IOException {
        // Do nothing
    }

    @Override
    public void clearBase(String base) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public String batchJob(String jobName, String base, long minMtime, long maxMtime, QueryOptions options)
            throws IOException {
        throw new UnsupportedOperationException("No batch jobs in FakeStorage (yet)");
    }

    @Override
    public String toString() {
        return "FakeStorage(#records=" + records.size() + ", #iterators=" + iterators.size() + ")";
    }
}
