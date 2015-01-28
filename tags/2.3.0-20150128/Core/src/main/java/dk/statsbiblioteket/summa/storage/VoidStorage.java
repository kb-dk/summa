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
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.Storage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * A {@link Storage} implementation that immediately forgets everything added
 * to it. Used mainly for debugging ingest chains.
 */
@SuppressWarnings("UnusedParameters")
public class VoidStorage extends StorageBase {

    private static final Log log = LogFactory.getLog(VoidStorage.class);

    public VoidStorage(Configuration conf) {
        log.info("Created VoidStorage");
    }

    @Override
    public long getRecordsModifiedAfter(long time, String base, QueryOptions options) throws IOException {
        return 0;
    }

    public long getRecordsModifiedAfterLoadData(long time, String base, QueryOptions options) throws IOException {
        return 0;
    }

    @Override
    public Record getRecord(String id, QueryOptions options) throws IOException {
        log.debug("Get record: " + id);
        return null;
    }

    @Override
    public Record next(long iteratorKey) throws IOException {
        throw new NoSuchElementException("VoidStorage contains no elements");
    }

    @Override
    public void flush(Record record, QueryOptions options) throws IOException {
        updateModificationTime(record.getBase());
        log.debug("Flushed: " + record + ", with options " + options);
    }

    @Override
    public void close() throws IOException {
        log.info("Closed");
    }

    @Override
    public void clearBase(String base) throws IOException {
        log.info("Clearing base: " + base);
    }

    @Override
    public String batchJob(String jobName, String base, long minMtime, long maxMtime, QueryOptions options) {
        log.info("Batch job: " + jobName + " on base " + base);
        return "";
    }
}

