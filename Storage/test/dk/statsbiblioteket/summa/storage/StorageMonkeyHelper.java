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
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.api.*;
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
public class StorageMonkeyHelper {
    private static Log log = LogFactory.getLog(StorageMonkeyHelper.class);

    private List<Integer> existingIDs = new ArrayList<Integer>(10000);
    private int idCounter = 0; // For new IDs
    private Random random = new Random(87);

    private int minContentSize = 0;
    private int maxContentSize = 100000;
    private double parentChance = 0.01;
    private double childChance = 0.02;

    // Meta data
    private String validChars =
            "abcdefghijklmnopqrstuvwxyzæøå ABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅ123456"
            + "7890!\"#¤%&/()=?+^~*'";
    private List<String> validKeys = Arrays.asList("foo", "foo.sub", "bar");
    private int minMetaEntries;
    private int maxMetaEntries;
    private int minMetaLength;
    private int maxMetaLength;

    private boolean checkForExistingOnDelete = true;

    /**
     * Performs a monkey-test on the given Storage, using multiple Threads with
     * varying payloads.
     * @param minMetaEntries the minimum number of meta-entries/record.
     * @param maxMetaEntries the maximum number of meta-entries/record.
     * @param minMetaLength  the minimum length of the values for meta-entries.
     * @param maxMetaLength  the maximum length of the values for meta-entries.
     * @param minContentSize the minimum size of the content for records.
     * @param maxContentSize the maximum size of the content for records.
     * @param parentChance   the chance that a parent relation is added to new
     *                       records.
     * @param childChance the chance that a child relation is added to records.
     * @param validChars the characters used for meta values.
     *                       If null, a fairly conservative dafault is used.
     * @param validKeys the valid meta-keys. If null, the defaults are used.
     */
    public StorageMonkeyHelper(int minContentSize, int maxContentSize,
                             double parentChance, double childChance,
                             String validChars, List<String> validKeys,
                             int minMetaEntries, int maxMetaEntries,
                             int minMetaLength, int maxMetaLength) {
        this.minContentSize = minContentSize;
        this.maxContentSize = maxContentSize;
        this.parentChance = parentChance;
        this.childChance = childChance;
        this.validChars = validChars == null ? this.validChars : validChars;
        this.validKeys = validKeys == null ? this.validKeys : validKeys;
        this.minMetaEntries = minMetaEntries;
        this.maxMetaEntries = maxMetaEntries;
        this.minMetaLength = minMetaLength;
        this.maxMetaLength = maxMetaLength;
    }

    /**
     * Performs a monkey-test on the given Storage, using multiple Threads with
     * varying payloads. This is a shortcut for calling {@link #createJobs}
     * followed by {@link #doJobs}.
     * @param news         the number of new records to create.
     * @param updates      the number of existing records to update.
     * @param deletes      the number of existing records to delete.
     * @param jobSize      the number of records in each job.
     * @param threads      the number of threads to use for concurrent access.
     * @param minFlushSize the maximum number of records to flush at a time.
     * @param maxFlushSize the maximum number of records to flush at a time.
     * @throws Exception if the test failed.
     */
    public synchronized void monkey(
            int news, int updates, int deletes, int jobSize, int threads,
            int minFlushSize, int maxFlushSize)
            throws Exception {
        List<Job> jobs = createJobs(news, updates, deletes,
                                    jobSize, minFlushSize, maxFlushSize);
        doJobs(jobs, threads);
    }

    /**
     * Creates single job.
     * @param news         the number of new records to create.
     * @param updates      the number of existing records to update.
     * @param deletes      the number of existing records to delete.
     * @return the created job.
     */
    public Job createJob(int news, int updates, int deletes) {
        return createJobs(news, updates, deletes, Integer.MAX_VALUE,
                          Integer.MAX_VALUE, Integer.MAX_VALUE).get(0);
    }

    /**
     * Creates a list of jobs.
     * @param news         the number of new records to create.
     * @param updates      the number of existing records to update.
     * @param deletes      the number of existing records to delete.
     * @param jobSize      the number of records in each job.
     * @param minFlushSize the maximum number of records to flush at a time.
     * @param maxFlushSize the maximum number of records to flush at a time.
     * @return the list of Jobs.
     */
    public List<Job> createJobs(int news, int updates, int deletes,
                                int jobSize,
                                int minFlushSize, int maxFlushSize) {
        if (deletes > news) {
            throw new IllegalArgumentException(String.format(
                    "The number of deletes was %d while the number of news was "
                    + "%d. the number of deletes must be less than or equal to "
                    + "news", deletes, news));
        }

        // create jobs
        List<Job> jobs = new ArrayList<Job>(100);
        while (news + updates + deletes > 0) {
            int records = Math.min(news + updates + deletes, jobSize);
            int flushSize = Math.max(1, nextInt(minFlushSize, maxFlushSize));
            Job job = new Job(flushSize);
            for (int i = 0 ; i < records ; i++) {
                if (existingIDs.size() == 0) {
                    job.add(new FutureRecord(idCounter++, false));
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
                                existingIDs.get(random.nextInt(
                                        existingIDs.size())), true));
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
                        job.add(new FutureRecord(idCounter++, false));
                        news--;
                        break;
                    }
                }
            }
            log.debug("Created " + job);
            jobs.add(job);
        }
        return jobs;
    }

    /**
     * Performs the given jobs against the default Storage.
     * @param jobs the records to commit to Storage.
     * @param threadCount the number of threads to use. One thread/job.
     */
    public void doJobs(List<Job> jobs, int threadCount) {
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
                synchronized (pool) {
                    pool.wait(50);
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for job completion. " 
                         + "Re-waiting");
            }
        }
        log.info("Finished processing " + jobs.size() + " jobs");
    }

    private static enum TYPE {n, u, d}

    private int nextInt(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    public Record makeRecord(Random random, int id, boolean delete,
                                    int minSize, int maxSize) {
        byte[] content = new byte[random.nextInt(maxSize - minSize) + minSize];
        random.nextBytes(content);
        Record record = new Record(Integer.toString(id), "simian", new byte[0]);
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
        if (random.nextDouble() < parentChance) {
            record.setParentIds(Arrays.asList(Integer.toString(
                    random.nextInt(Math.max(1, id)))));
        }
        if (random.nextDouble() < childChance) {
            record.setChildIds(Arrays.asList(
                    Integer.toString(random.nextInt(Math.max(1, id))),
                    Integer.toString(random.nextInt(Math.max(1, id)))));
        }
        record.setDeleted(delete);
        return record;
    }

    private class FutureRecord {
        private int id;
        private boolean delete;

        private FutureRecord(int id, boolean delete) {
            this.id = id;
            this.delete = delete;
            if (!delete && !existingIDs.contains(id)) {
                existingIDs.add(id);
            } else if (delete) {
                existingIDs.remove(Integer.valueOf(id));
            }
        }

        public Record getRecord() {
            return makeRecord(
                    random, id, delete, minContentSize, maxContentSize);
        }
    }

    public class Job implements Runnable {
        private List<FutureRecord> records = new ArrayList<FutureRecord>(100);
        private int flushSize;
        private int recordCount = 0;

        public Job(int flushSize) {
            this.flushSize = flushSize;
        }

        public void add(FutureRecord record) {
            records.add(record);
            recordCount++;
        }

        @Override
        public String toString() {
            return "Job(" + recordCount + " records)";
        }

        public int size() {
            return recordCount;
        }

        public void run() {
            log.debug("Starting Job thread");
            try {
                WritableStorage storageW = getStorageW();
                ReadableStorage storageR = getStorageR();
                while (records.size() > 0) {
                    int currentJobSize = Math.min(flushSize, records.size());
                    ArrayList<Record> summaRecords = new ArrayList<Record>(
                            currentJobSize);
                    List<FutureRecord> currentRecords =
                            new ArrayList<FutureRecord>(
                                    records.subList(0, currentJobSize));
                    if (records.size() - currentJobSize == 0) {
                        records.clear();
                    } else {
                        records = records.subList(
                                currentJobSize, records.size());
                    }

                    for (FutureRecord record: currentRecords) {
                        Record summaRecord = record.getRecord();
                        if (checkForExistingOnDelete &&
                            summaRecord.isDeleted() &&
                            storageR.getRecord(summaRecord.getId(), null)
                            == null) {
                            log.debug(
                                    "The Record with id " + summaRecord.getId()
                                    + " has not yet been added to the Storage");
                        }
                        summaRecords.add(summaRecord);
                    }
                    log.debug("Flushing " + summaRecords.size() + " records");
                    storageW.flushAll(summaRecords);
                }
            } catch (IOException e) {
                log.error("Failed to flush " + this, e);
            } catch (NullPointerException e) {
                log.error("NPE running " + this, e);
            }
            log.debug("Ending Job thread");
        }

        private WritableStorage getStorageW() throws IOException {
            Configuration conf = Configuration.newMemoryBased();

            return new StorageWriterClient(conf);
        }
        private ReadableStorage getStorageR() throws IOException {
            Configuration conf = Configuration.newMemoryBased();

            return new StorageReaderClient(conf);
        }
    }

    public boolean isCheckForExistingOnDelete() {
        return checkForExistingOnDelete;
    }

    public void setCheckForExistingOnDelete(boolean checkForExistingOnDelete) {
        this.checkForExistingOnDelete = checkForExistingOnDelete;
    }
}
