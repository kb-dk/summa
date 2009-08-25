/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.storage;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.storage.api.Storage;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ArrayBlockingQueue;
import java.io.IOException;
import java.io.StringWriter;

/**
 * Helper class for testing Storage.
 * </p><p>
 * This helper is not thread-safe.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class StorageMonkeyTest {
    private static Log log = LogFactory.getLog(StorageMonkeyTest.class);

    private List<String> existingIDs = new ArrayList<String>(10000);
    private int idCounter = 0; // For new IDs
    private Random random = new Random(87);
    private int minSize;
    private int maxSize;
    private Storage storage;


    // Meta data
    private String validChars =
            "abcdefghijklmnopqrstuvwxyzæøå ABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅ123456"
            + "7890!\"#¤%&/()=?+^~*'";
    private List<String> validKeys = Arrays.asList("foo", "foo.sub", "bar");
    private int minMetaEntries;
    private int maxMetaEntries;
    private int minMetaLength;
    private int maxMetaLength;

    /**
     * Performs a monkey-test on the given Storage, using multiple Threads with
     * varying payloads.
     * @param storage      the Storage to use for testing.
     * @param news         the number of new records to create.
     * @param updates      the number of existing records to update.
     * @param deletes      the number of existing records to delete.
     * @param threads      the number of threads to use for concurrent access.
     * @param minSize      the minimum content size for new records.
     * @param maxSize      the maximum content size for new records.
     * @param minFlushSize the maximum number of records to flush at a time.
     * @param maxFlushSize the maximum number of records to flush at a time.
     * @param minMetaEntries the minimum number of meta-entries/record.
     * @param maxMetaEntries the maximum number of meta-entries/record.
     * @param minMetaLength the minimum length of the values for meta-entries.
     * @param maxMetaLength the maximum length of the values for meta-entries.
     * @param validMetaChars the characters used for meta values. 
     *                       If null, a fairly conservative dafault is used.
     * @param validMetaKeys the valid meta-keys. If null, the defaults are used.
     * @throws Exception if the test failed.
     */
    public synchronized void monkey(
            Storage storage,
            int news, int updates, int deletes, int threads,
            int minSize, int maxSize, int minFlushSize, int maxFlushSize,
            int minMetaEntries, int maxMetaEntries,
            int minMetaLength, int maxMetaLength,
            String validMetaChars, List<String> validMetaKeys)
            throws Exception {
        if (deletes > news) {
            throw new IllegalArgumentException(String.format(
                    "The number of deletes was %d while the number of news was "
                    + "%d. the number of deletes must be less than or equal to "
                    + "news", deletes, news));
        }
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.storage = storage;
        this.validChars = validMetaChars == null ? validChars : validMetaChars;
        this.validKeys = validMetaKeys == null ? validKeys : validMetaKeys;
        this.minMetaEntries = minMetaEntries;
        this.maxMetaEntries = maxMetaEntries;
        this.minMetaLength = minMetaLength;
        this.maxMetaLength = maxMetaLength;
        
        // create jobs
        List<Job> jobs = new ArrayList<Job>(100);
        while (news + updates + deletes > 0) {
            int records = Math.min(news + updates + deletes,
                                   nextInt(minFlushSize, maxFlushSize));
            Job job = new Job();
            for (int i = 0 ; i < records ; i++) {
                if (existingIDs.size() == 0) {
                    job.add(new FutureRecord(
                            Integer.toString(idCounter++), false));
                    news--;
                    continue;
                }
                List<TYPE> types = new ArrayList<TYPE>(3);
                if (deletes > 0 && existingIDs.size() > 0) {
                    types.add(TYPE.d);
                }
                if (updates > 0 && existingIDs.size() > 0) {
                    types.add(TYPE.u);
                }
                if (news > 0) {
                    types.add(TYPE.n);
                }
                switch (types.get(random.nextInt(types.size()))) {
                    case d: {
                        job.add(new FutureRecord(
                                Integer.toString(idCounter++), true));
                        deletes--;
                        break;
                    }
                    case u: {
                        job.add(new FutureRecord(
                                existingIDs.get(random.nextInt(
                                        existingIDs.size())), false));
                        updates--;
                        break;
                    }
                    case n: {
                        job.add(new FutureRecord(
                                Integer.toString(idCounter++), false));
                        news--;
                        break;
                    }
                }
            }
            log.debug("Created " + job);
            jobs.add(job);
        }
        doJobs(jobs, threads);
    }

    private void doJobs(List<Job> jobs, int threadCount) {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                threadCount, threadCount, 10, TimeUnit.MINUTES,
                new ArrayBlockingQueue<Runnable>(jobs.size()));
        for (Job job: jobs) {
            log.debug("Queueing " + job);
            pool.execute(job);
        }
        log.debug("Waiting for jobs");
        while (pool.getCompletedTaskCount() < jobs.size()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for job completion. " 
                         + "Re-waiting");
            }
        }
        log.info("Finished processing " + jobs.size() + " jobs");
    }

    private static enum TYPE {n, u, d}

    private int nextInt(int min, int max) {
        return random.nextInt(max - min) + min;
    }

    public Record makeRecord(Random random, String id, boolean delete,
                                    int minSize, int maxSize) {
        byte[] content = new byte[random.nextInt(maxSize - minSize) + minSize];
        random.nextBytes(content);
        Record record = new Record(id, "simian", new byte[0]);
        record.setContent(content, true);
        int entries = nextInt(minMetaEntries, maxMetaEntries);
        for (int i = 0 ; i < entries ; i++) {
            int valueSize = nextInt(minMetaLength, maxMetaLength);
            StringWriter sw = new StringWriter(valueSize);
            for (int c = 0 ; c < valueSize ; c++) {
                sw.append(
                        validChars.charAt(random.nextInt(validChars.length())));
            }
            record.getMeta().put(
                    validKeys.get(random.nextInt(validKeys.size())),
                    sw.toString());
        }
        record.setDeleted(delete);
        return record;
    }

    private class FutureRecord {
        private String id;
        private boolean delete;

        private FutureRecord(String id, boolean delete) {
            this.id = id;
            this.delete = delete;
            if (!delete && !existingIDs.contains(id)) {
                existingIDs.add(id);
            } else if (delete) {
                existingIDs.remove(id);
            }
        }

        public Record getRecord() {
            return makeRecord(random, id, delete, minSize, maxSize);
        }
    }

    private class Job implements Runnable {
        private List<FutureRecord> records = new ArrayList<FutureRecord>(100);

        public void add(FutureRecord record) {
            records.add(record);
        }

        @Override
        public String toString() {
            return "Job(" + records + " records)";
        }

        public void run() {
            log.debug("Starting Job thread");
            ArrayList<Record> summaRecords =
                    new ArrayList<Record>(records.size());
            try {
                for (FutureRecord record: records) {
                    Record summaRecord = record.getRecord();
                    if (summaRecord.isDeleted() &&
                        storage.getRecord(summaRecord.getId(), null) == null) {
                        log.error("The Record with id " + summaRecord.getId() 
                                  + " should exist in the Storage");
                    }
                    summaRecords.add(summaRecord);

                }
                storage.flushAll(summaRecords);
            } catch (IOException e) {
                log.error("Failed to flush " + this, e);
            }
            log.debug("Ending Job thread");
        }
    }
}
