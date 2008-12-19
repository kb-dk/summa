/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.storage.api.filter;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.summa.storage.api.ReadableStorage;
import dk.statsbiblioteket.summa.storage.api.StorageReaderClient;
import dk.statsbiblioteket.summa.storage.api.watch.StorageWatcher;
import dk.statsbiblioteket.summa.storage.api.watch.StorageChangeListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.GregorianCalendar;
import java.util.NoSuchElementException;
import java.util.Iterator;
import java.util.Arrays;

/**
 * Retrieves Records from storage based on the criteria given in the properties.
 * Supports stopping during retrieval and resuming by timestamp.
 * </p><p>
 * Note: Besides the configurations stated below, the address for the
 * Storage must be specified with the key
 * {@link ConnectionConsumer#CONF_RPC_TARGET}. The address can be a standard
 * RMI address, such as {@code //localhost:6789/storage}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
// TODO: If keepalive, the state should be saved at regular intervals
// TODO: Change the + 1000 ms when the time granilarity of JavaDB improves
public class RecordReader implements ObjectFilter, StorageChangeListener {
    private static Log log = LogFactory.getLog(RecordReader.class);

    /**
     * The state of progress is stored in this file upon close. This allows
     * for a workflow where a Storage is harvested in parts.
     * </p><p>
     * The progress file is resolved to the default dir if it is not absolute.
     * </p><p>
     * This property is optional. Default is "<base>.progress.xml",
     * for example "horizon.progress.xml".
     * If no base is defined, the default value is "progress.xml".
     */
    public static final String CONF_PROGRESS_FILE =
            "summa.storage.recordreader.progressfile";
    public static final String DEFAULT_PROGRESS_FILE_POSTFIX = "progress.xml";

    /**
     * If true, the state of progress is stored in {@link #CONF_PROGRESS_FILE}.
     * This means that new runs will continue where the previous run left.
     * If no progress-file exists, a new one will be created.
     * </p><p>
     * Note that progress will only be stored in the event of a call to
     * {@link #close(boolean)} with a value of true.
     * </p><p>
     * This property is optional. Default is true.
     */
    public static final String CONF_USE_PERSISTENCE =
            "summa.storage.recordreader.usepersistence";
    public static final boolean DEFAULT_USE_PERSISTENCE = true;

    /**
     * If true, any existing progress is ignored and the harvest from the
     * Storage is restarted.
     * </p><p>
     * This property is optional. Default is false.
     */
    public static final String CONF_START_FROM_SCRATCH =
            "summa.storage.recordreader.startfromscratch";
    public static final boolean DEFAULT_START_FROM_SCRATCH = false;

    /**
     * The maximum number of Records to read before signalling EOF onwards in
     * the filter chain. Specifying -1 means no limit.
     * </p><p>
     * This property is optional. Default is -1 (disabled).
     */
    public static final String CONF_MAX_READ_RECORDS =
            "summa.storage.recordreader.maxread.records";
    public static final int DEFAULT_MAX_READ_RECORDS = -1;

    /**
     * The maximum number of seconds before signalling EOF onwards in the
     * filter chain. Specifying -1 means no limit.
     * </p><p>
     * This property is optional. Default is -1 (disabled).
     */
    public static final String CONF_MAX_READ_SECONDS =
            "summa.storage.recordreader.maxread.seconds";
    public static final int DEFAULT_MAX_READ_SECONDS = -1;

    /**
     * Only Records with matching base will be retrieved. Specifying the empty
     * string as base means all bases. No wildcards are allowed.
     * </p><p>
     * This property is optional. Default is "" (all records).  
     */
    public static final String CONF_BASE =
            "summa.storage.recordreader.base";
    public static final String DEFAULT_BASE = "";

    /**
     * Number of records to extract before storing a progress file.
     * The file will also be written if {@link #CONF_PROGRESS_GRACETIME} is
     * passed.
     * <p/>
     * If the batch size is negative then progress will never be written
     * no matter how many records are read, but surpassing the gracetime
     * will still trigger a write.
     * <p/>
     * This property is optional.
     * Default is {@link #DEFAULT_PROGRESS_BATCH_SIZE}
     */
    public static final String CONF_PROGRESS_BATCH_SIZE =
            "summa.storage.recordreader.progress.batchsize";
    public static final long DEFAULT_PROGRESS_BATCH_SIZE = 500;

    /**
     * Number of milliseconds to wait while writing a progress file.
     * The progress file will also be written if
     * {@link #CONF_PROGRESS_BATCH_SIZE} records has been extracted.
     * If the grace time is negative the progress file will never be written
     * because of time based rules, only by surpassing the batch size.
     * <p/>
     * This property is optional.
     * Default is {@link #DEFAULT_PROGRESS_GRACETIME}
     */
    public static final String CONF_PROGRESS_GRACETIME =
            "summa.storage.recordreader.progress.gracetime";
    public static final long DEFAULT_PROGRESS_GRACETIME = 5000;

    /**
     * If true, the connection should stay alive after the initial poll.
     * Calls to hasNext(), next() and pump() will block until new Records are
     * added to the Storage or close is called.
     * </p><p>
     * Note: The property {@link StorageWatcher#CONF_POLL_INTERVAL} controls the
     *       polling interval.
     * </p><p>
     * This property is optional. Default is false.
     */
    public static final String CONF_STAY_ALIVE =
            "summa.storage.recordreader.stayalive";
    public static final boolean DEFAULT_STAY_ALIVE = false;


    /**
     * Helper class used to only flush the progress file when needed.
     * <p/>
     * This class does NOT count the number of records read. It may receive
     * more or less updates than the number of records.
     */
    private static class ProgressTracker {

        private long lastExternalUpdate;
        private long lastInternalUpdate;
        private long batchSize;
        private long graceTime;
        private long numUpdates;
        private long numUpdatesLastFlush;
        private File progressFile;

        public ProgressTracker (File progressFile,
                                long batchSize, long graceTime) {
            this.batchSize = batchSize;
            this.graceTime = graceTime;
            this.progressFile = progressFile;

            log.debug(String.format(
                    "Created ProgressTracker with batchSize(%d), graceTime(%d),"
                    + " and progressFile(%s)",
                    batchSize, graceTime, progressFile));
        }

        /**
         * Register an update at time {@code timestamp}. The progress file
         * will be updated if needed
         * @param timestamp
         */
        public void updated (long timestamp) {
            lastExternalUpdate = timestamp;
            lastInternalUpdate = System.currentTimeMillis();
            numUpdates++;
            checkProgressFile();
        }

        /**
         * Check if the progress file needs updating and do it if that
         * is the case
         */
        private void checkProgressFile () {
            if (graceTime >= 0 &&
                System.currentTimeMillis() - lastInternalUpdate > graceTime) {
                updateProgressFile();
                return;
            }

            if (batchSize >= 0 &&
                numUpdates - numUpdatesLastFlush > batchSize) {
                updateProgressFile();
                return;
            }
        }

        /**
         * Force an update of the progress file. This will not respect
         * the batch size or gracetime settings in any way
         */
        public void updateProgressFile() {
            lastInternalUpdate = System.currentTimeMillis();

            log.debug("Storing progress in '" + progressFile + "' ("
                      + numUpdates + " records has been extracted so far)");
            try {
                Files.saveString(
                        String.format(TIMESTAMP_FORMAT, lastExternalUpdate),
                        progressFile);
            } catch (IOException e) {
                log.error("close(true): Unable to store progress in file '"
                          + progressFile + "': " + e.getMessage(), e);
            }
            numUpdatesLastFlush = numUpdates;
        }

        /**
         * Read the last modification time in from the progress file
         */
        public void loadProgress () {
            log.debug("Attempting to get previous progress stored in file "
                      + progressFile);

            if (progressFile.exists() && progressFile.isFile() &&
                progressFile.canRead()) {
                log.trace("getStartTime has persistence file");
                try {
                    long startTime = getTimestamp(progressFile,
                                                  Files.loadString(progressFile));
                    if (log.isDebugEnabled()) {
                        try {
                            log.debug(String.format(
                                    "Extracted timestamp " + ISO_TIME
                                    + " from '%2$s'", startTime, progressFile));
                        } catch (Exception e) {
                            log.warn("Could not output properly formatted timestamp"
                                     + " for " + startTime + " ms");
                        }
                    }
                    lastExternalUpdate = startTime;
                    lastInternalUpdate = System.currentTimeMillis();
                } catch (IOException e) {
                    //noinspection DuplicateStringLiteralInspection
                    log.error("getStartTime: Unable to open existing file '"
                              + progressFile + "'. Returning 0");
                }
            }
        }

        public long getLastUpdate() {
            return lastExternalUpdate;
        }

        public void clearProgressFile() {
            if (progressFile.exists()) {
                progressFile.delete();
            }
        }

        static long getTimestamp(File progressFile, String xml) {
            Matcher matcher = TIMESTAMP_PATTERN.matcher(xml);
            if (!matcher.matches() || matcher.groupCount() != 6) {
                //noinspection DuplicateStringLiteralInspection
                log.error("getTimestamp: Could not locate timestamp in "
                          + "file '" + progressFile + "' containing '"
                          + xml + "'. Returning 0");
                return 0;
            }
            return new GregorianCalendar(Integer.parseInt(matcher.group(1)),
                                         Integer.parseInt(matcher.group(2))-1, //WTF?
                                         Integer.parseInt(matcher.group(3)),
                                         Integer.parseInt(matcher.group(4)),
                                         Integer.parseInt(matcher.group(5)),
                                         Integer.parseInt(matcher.group(6))).
                    getTimeInMillis();
        }
    }

    @SuppressWarnings({"FieldCanBeLocal"})
    private ReadableStorage storage;
    @SuppressWarnings({"FieldCanBeLocal"})
    private String base = DEFAULT_BASE;
    private ProgressTracker progressTracker;
    private boolean usePersistence = DEFAULT_USE_PERSISTENCE;
    private boolean startFromScratch = DEFAULT_START_FROM_SCRATCH;
    private int maxReadRecords = DEFAULT_MAX_READ_RECORDS;
    private int maxReadSeconds = DEFAULT_MAX_READ_SECONDS;

    private StorageWatcher storageWatcher;
    private boolean eofReached = false;
    private long recordCounter = 0;
    private long startTime = System.currentTimeMillis();
    private long lastRecordTimestamp;
    private long lastIteratorUpdate;

    private Iterator<Record> recordIterator = null;

    /**
     * Connects to the Storage specified in the configuration and request an
     * iteration of the Records specified by the properties.
     * @param conf contains setup information.
     * @see {@link #CONF_BASE}.
     * @see {@link #CONF_MAX_READ_RECORDS}.
     * @see {@link #CONF_MAX_READ_SECONDS}.
     * @see {@link ConnectionConsumer#CONF_RPC_TARGET}.
     * @see {@link StorageWatcher#CONF_POLL_INTERVAL}.
     * @see {@link #CONF_PROGRESS_FILE}.
     * @see {@link #CONF_START_FROM_SCRATCH}.
     * @see {@link #CONF_USE_PERSISTENCE}.
     * @throws java.io.IOException if it was not possible to connect to the
     * Storage or if the filename for the progress file was illegal.
     */
    public RecordReader(Configuration conf) throws IOException {
        log.trace("Constructing RecordReader");
        storage = new StorageReaderClient(conf);
        base = conf.getString(CONF_BASE, DEFAULT_BASE);

        if ("*".equals(base) || "".equals(base)) {
            log.trace("Catch-all base '" + base + "' was specified");
            base = null;
        }

        String progressFileString =
                conf.getString(CONF_PROGRESS_FILE, null);
        File progressFile;
        if (progressFileString == null || "".equals(progressFileString)) {
            progressFile = new File((base == null ? "" : base + ".")
                                         + DEFAULT_PROGRESS_FILE_POSTFIX);
            log.debug("No progress-file defined in key " + CONF_PROGRESS_FILE
                      + ". Constructing progress file '" + progressFile + "'");
        } else {
            progressFile = new File(progressFileString);
            log.debug("Progress.file is " + progressFile.getCanonicalFile());
        }
        progressFile = progressFile.getAbsoluteFile();

        usePersistence = conf.getBoolean(CONF_USE_PERSISTENCE,
                                         DEFAULT_USE_PERSISTENCE);
        startFromScratch = conf.getBoolean(CONF_START_FROM_SCRATCH,
                                           DEFAULT_START_FROM_SCRATCH);
        maxReadRecords = conf.getInt(CONF_MAX_READ_RECORDS,
                                     DEFAULT_MAX_READ_RECORDS);
        maxReadSeconds = conf.getInt(CONF_MAX_READ_SECONDS,
                                     DEFAULT_MAX_READ_SECONDS);

        if (usePersistence) {
            log.debug("Enabling progress tracker");
            progressTracker =
                  new ProgressTracker(progressFile,
                                      conf.getLong(CONF_PROGRESS_BATCH_SIZE,
                                                   DEFAULT_PROGRESS_BATCH_SIZE),
                                      conf.getLong(CONF_PROGRESS_GRACETIME,
                                                   DEFAULT_PROGRESS_GRACETIME));
        } else {
            log.info("Progress tracking disabled");
            progressTracker = null;
        }

        if (conf.getBoolean(CONF_STAY_ALIVE, DEFAULT_STAY_ALIVE)) {
            storageWatcher = new StorageWatcher(conf);
            storageWatcher.addListener(this, Arrays.asList(base), null);
            storageWatcher.start();
            log.trace("Enabled storage watching for base " + base);
        } else {
            log.trace("No storage watching enabled as " + CONF_STAY_ALIVE
                      + " was false");
        }

        lastRecordTimestamp = getStartTime();
        lastIteratorUpdate = lastRecordTimestamp;
        log.trace("RecordReader constructed, ready for pumping");
    }

    private static final String TAG = "lastRecordTimestamp";
    public static final Pattern TIMESTAMP_PATTERN =
            Pattern.compile(".*<" + TAG + ">"
                            + "([0-9]{4})([0-9]{2})([0-9]{2})-"
                            + "([0-9]{2})([0-9]{2})([0-9]{2})"
                            + "</" + TAG + ">.*", Pattern.DOTALL);
    private static final String ISO_TIME = "%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS";
    public static final String TIMESTAMP_FORMAT =
            "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n"
            +"<" + TAG + ">" + ISO_TIME + "</" + TAG + ">\n";
    /**
     * If !START_FROM_SCRATCH && USE_PERSISTENCE then get last timestamp
     * from persistence file, else return 0.
     * @return the timestamp to continue harvesting from.
     */
    private long getStartTime() {
        if (startFromScratch || !usePersistence) {
            log.trace("getStartTime: Starttime set to 0");

            if (progressTracker != null) {
                progressTracker.updated(0);
            }

            return 0;
        }

        if (progressTracker != null) {
            progressTracker.loadProgress();
            log.info("Resuming from " + progressTracker.getLastUpdate());
            return progressTracker.getLastUpdate();
        } else {
            log.debug("Not set to resume or keep progress state. "
                      + "Starting from time 0");
            return 0;
        }
    }

    /**
     * If the iterator is null, a new iterator is requested. If the iterator
     * has reached the end, the method checks to see is the Storage has been
     * updated since last iterator creation. If so, a new iterator is created.
     * If not, the method waits for an update from StorageWatcher.
     *
     * @return {@code true} iff the iterator was good
     * @throws java.io.IOException if an iterator could not be created.
     */
    private boolean checkIterator() throws IOException {
        log.trace("checkIterator() called");
        if (isEof()) {
            return false;
        }
        if (recordIterator == null) {
            //noinspection DuplicateStringLiteralInspection
            log.debug(String.format("Creating initial record iterator for "
                                    + "Records modified after "
                                    + ISO_TIME, lastRecordTimestamp));
            long iterKey =
                    storage.getRecordsModifiedAfter(lastRecordTimestamp, base, null);

            lastIteratorUpdate = System.currentTimeMillis();
            recordIterator = new StorageIterator(storage, iterKey);

            return false;
        } else if (recordIterator.hasNext()) {
            return true;
        } else {
            if (storageWatcher == null) {
                log.trace("storageWatcher is null, so no renew of iterator");
                return false;
            }
            // We have an iterator but it is empty
            //noinspection DuplicateStringLiteralInspection
            log.debug(String.format("Updating record iterator for "
                                    + "Records modified after "
                                    + ISO_TIME, lastRecordTimestamp));
            long iterKey =
                    storage.getRecordsModifiedAfter(lastRecordTimestamp, base, null);

            lastIteratorUpdate = System.currentTimeMillis();
            recordIterator = new StorageIterator(storage, iterKey);

            return false;
        }
    }

    private void markEof() {
        eofReached = true;
        recordIterator = null; // Allow finalization of the recordIterator
    }

    private boolean isEof() {
        return eofReached;
    }

    /* ObjectFilter interface */

    public boolean hasNext() {
        if (isEof()) {
            return false;
        }

        try {
            log.trace("hasNext: Calling checkIterator()");
            checkIterator();
        } catch (IOException e) {
            log.warn("hasNext: An exception occured while checking for a new "
                     + "iterator. Returning false");
            return false;
        }

        if (isEof()) {
            return false;
        }
        while (!recordIterator.hasNext()) {
            log.trace("hasNext: RecordIterater does not have next. Waiting and "
                      + "checking");
            try {
                waitForStorageChange();
                checkIterator();
                if (storageWatcher == null || recordIterator == null
                    || !recordIterator.hasNext()) {
                    break;
                }
            } catch (IOException e) {
                log.warn("hasNext: An exception occured while checking for a"
                         + " new iterator. Returning false");
                return false;
            }
        }
        return !isEof();
    }

    private void waitForStorageChange() {
        log.trace("waitForStorageChange() called");
        try {
            if (checkIterator() && recordIterator.hasNext()) {
                return;
            }
            if (storageWatcher == null) { // We don't wait here
                log.trace("waitForStorageChange: No storageWatcher, no records:" 
                          + " Mark EOF");
                markEof();
                return;
            }
        } catch (IOException e) {
            log.error("Error prepraring iterator for wait-phase: "
                      + e.getMessage(), e);
            markEof();
        }


        // We have to check this in a loop. See Javadoc for Object.wait()
        while (true) {
            try {
                // Check if there has been changes since we last checked
                // Keep the monitor on the storageWatcher until we have
                // finished waiting
                // on it to make sure we don't drop any events
                synchronized (storageWatcher) {
                    if (storageWatcher.getLastNotify(base) >
                        lastIteratorUpdate) {
                        log.debug("Detected changes on base '"+base
                                  +"'since last check. Skipping wait");
                        break;
                    }
                    log.debug("No changes on base '"+base+"' since last check."
                              + " Waiting for storage watcher...");
                    storageWatcher.wait();
                }
            } catch (InterruptedException e) {
                log.debug("Interrupted");
            }
        }

        log.debug("Got notification from storage");
    }

    public Payload next() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("next() called");

        if (!hasNext()) {
            throw new NoSuchElementException("No more Records available");
        }

        Payload payload = new Payload(recordIterator.next());
        recordCounter++;
        lastRecordTimestamp = payload.getRecord().getLastModified();

        if (log.isTraceEnabled()) {
            log.trace("next(): Got lastModified timestamp "
                      + String.format(ISO_TIME,
                                      payload.getRecord().getLastModified())
                      + " for " + payload);
        }

        if (maxReadRecords != -1 && maxReadRecords <= recordCounter) {
            log.debug("Reached maximum number of Records to read ("
                      + maxReadRecords + ")");
            markEof();
        }

        if (maxReadSeconds != -1 &&
            maxReadSeconds * 1000 <= System.currentTimeMillis() - startTime) {
            log.debug("Reached maximum allow time usage ("
                      + maxReadSeconds + ") seconds");
            markEof();
        }

        if (progressTracker != null) {
            progressTracker.updated(lastRecordTimestamp);
        }

        return payload;
    }

    public void remove() {
        throw new UnsupportedOperationException(
                "No removal of Payloads for RecordReader");
    }

    public void setSource(Filter filter) {
        throw new UnsupportedOperationException(
                "RecordReader must be the first filter in the chain");
    }

    public boolean pump() throws IOException {
        if (!hasNext()) {
             return false;
         }
         Payload next = next();
         if (next == null) {
             return false;
         }
         next.close();
         return true;
     }

    /**
     * If success is true and persistence enabled, the current progress in the
     * harvest is stored. If success is false, no progress is stored.
     * @param success whether the while ingest has been successfull or not.
     */
    // TODO: Check why this is not called in FacetTest
    public void close(boolean success) {
        //noinspection DuplicateStringLiteralInspection
        log.debug("close(" + success + ") entered");
        markEof();
        if (success && progressTracker != null) {
            progressTracker.updated(lastRecordTimestamp);
            progressTracker.updateProgressFile(); // Force a flush of the progress
        }
    }

    /**
     * If a progress-file is existing, it is cleared.
     */
    public void clearProgressFile() {
        if (progressTracker != null) {
            progressTracker.clearProgressFile();
        }
    }

    public void storageChanged(StorageWatcher watch, String base,
                               long timeStamp, Object userData) {
        log.trace("Storage was changed for base " + base + " and timestamp "
                  + timeStamp);
        watch.notifyAll();
        // TODO : Update the Semaphore with at most 1   (remember syns)
    }
}



