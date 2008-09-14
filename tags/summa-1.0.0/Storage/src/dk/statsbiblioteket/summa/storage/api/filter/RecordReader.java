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
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.RecordIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.GregorianCalendar;
import java.util.NoSuchElementException;
import java.net.ConnectException;

/**
 * Retrieves Records from storage based on the criteria given in the properties.
 * Supports stopping during retrieval and resuming by timestamp.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class RecordReader implements ObjectFilter {
    private static Log log = LogFactory.getLog(RecordReader.class);

    /**
     * The Storage to connect to. This is a standard RMI address.
     * Example: //localhost:6789/storage;
     * </p><p>
     * This property is mandatory.
     */
    public static final String CONF_STORAGE =
            "summa.storage.recordreader.storage";

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

    @SuppressWarnings({"FieldCanBeLocal"})
    private ConnectionContext<Storage> accessContext;
    private Storage storage;
    @SuppressWarnings({"FieldCanBeLocal"})
    private String base = DEFAULT_BASE;
    private File progressFile;
    private boolean usePersistence = DEFAULT_USE_PERSISTENCE;
    private boolean startFromScratch = DEFAULT_START_FROM_SCRATCH;
    private int maxReadRecords = DEFAULT_MAX_READ_RECORDS;
    private int maxReadSeconds = DEFAULT_MAX_READ_SECONDS;

    private boolean eooReached = false;
    private long recordCounter = 0;
    private long startTime;
    private long lastRecordTimestamp;

    RecordIterator recordIterator;

    /**
     * Connects to the Storage specified in the configuration and request an
     * iteration of the Records specified by the properties.
     * @param configuration contains setup information.
     * @see {@link #CONF_BASE}.
     * @see {@link #CONF_MAX_READ_RECORDS}.
     * @see {@link #CONF_MAX_READ_SECONDS}.
     * @see {@link #CONF_STORAGE}.
     * @see {@link #CONF_PROGRESS_FILE}.
     * @see {@link #CONF_START_FROM_SCRATCH}.
     * @see {@link #CONF_USE_PERSISTENCE}.
     */
    public RecordReader(Configuration configuration) throws IOException {
        log.trace("Constructing RecordReader");
        accessContext =
                FilterCommons.getAccess(configuration, CONF_STORAGE);
         // TODO: Consider if this should be requested for every call to Storage

        if (accessContext == null) {
            throw new ConnectException("Unnable to connect to storage at");
        }

        storage = accessContext.getConnection();
        base = configuration.getString(CONF_BASE, DEFAULT_BASE);
        String progressFileString =
                configuration.getString(CONF_PROGRESS_FILE, null);
        if (progressFileString == null || "".equals(progressFileString)) {
            progressFile = new File(("".equals(base) ? "" : base + ".")
                                    + DEFAULT_PROGRESS_FILE_POSTFIX);
            log.debug("No progress-file defined in key " + CONF_PROGRESS_FILE
                      + ". Constructing progress file '" + progressFile + "'");
        } else {
            log.debug("Progress.file is " + progressFile.getCanonicalFile());
        }
        progressFile = progressFile.getAbsoluteFile();
        usePersistence = configuration.getBoolean(CONF_USE_PERSISTENCE,
                                                  DEFAULT_USE_PERSISTENCE);
        startFromScratch = configuration.getBoolean(CONF_START_FROM_SCRATCH,
                                                    DEFAULT_START_FROM_SCRATCH);
        maxReadRecords = configuration.getInt(CONF_MAX_READ_RECORDS,
                                              DEFAULT_MAX_READ_RECORDS);
        maxReadSeconds = configuration.getInt(CONF_MAX_READ_SECONDS,
                                              DEFAULT_MAX_READ_SECONDS);
        // TODO: Support empty base in reader
        if ("".equals(base)) {
            throw new ConfigurationException("Empty base not supported yet");
        }

        try {
            long startTime = getStartTime();
            log.debug(String.format("Getting Records modified after "
                                    + ISO_TIME, startTime));
            recordIterator = storage.getRecordsModifiedAfter(startTime, base);
        } catch (IOException e) {
            FilterCommons.reportError(accessContext, e);
            throw new ConfigurationException("Exception while getting "
                                             + "recordIterator for time "
                                             + getStartTime() + " and base '"
                                             + base +"'", e);
        }
        startTime = 0;
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
            return 0;
        }
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
                return startTime;
            } catch (IOException e) {
                //noinspection DuplicateStringLiteralInspection
                log.error("getStartTime: Unable to open existing file '"
                          + progressFile + "'. Returning 0");
                return 0;
            }
        }
        //noinspection DuplicateStringLiteralInspection
        log.trace("getStartTime: Could not get storage progress file '"
                  + progressFile + "'. Returning 0 as lastTimestamp");
        return 0;
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
                                     Integer.parseInt(matcher.group(2))-1,
                                     Integer.parseInt(matcher.group(3)),
                                     Integer.parseInt(matcher.group(4)),
                                     Integer.parseInt(matcher.group(5)),
                                     Integer.parseInt(matcher.group(6))).
                getTimeInMillis();
    }

    /* ObjectFilter interface */

    public boolean hasNext() {
        if (eooReached) {
            return false;
        }
        if (!recordIterator.hasNext()) {
            eooReached = true;
            return false;
        }
        return true;
    }

    public Payload next() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("next() called");
        if (!hasNext()) {
            throw new NoSuchElementException("No more Records available");
        }
        Payload payload = new Payload(recordIterator.next());
        recordCounter++;
//        System.out.println("*** " + lastRecordTimestamp);
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
            eooReached = true;
        }
        if (maxReadSeconds != -1 &&
            maxReadSeconds * 1000 <= System.currentTimeMillis() - startTime) {
            log.debug("Reached maximum allow time usage ("
                      + maxReadSeconds + ") seconds");
            eooReached = true;
        }
        writeProgress(); // TODO: Is this too aggressive?
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
        eooReached = true;
        if (success) {
            writeProgress();
        }
        FilterCommons.releaseAccess(accessContext);
    }

    private void writeProgress() {
        if (usePersistence && recordCounter > 0) {
            log.debug("Storing progress in '" + progressFile + "'");
            try {
                Files.saveString(
                        String.format(TIMESTAMP_FORMAT, lastRecordTimestamp),
                        progressFile);
            } catch (IOException e) {
                log.error("close(true): Unable to store progress in file '"
                          + progressFile + "'");
            }
        }
    }

    /**
     * If a progress-files is existing, it is cleared.
     */
    public void clearProgressFile() {
        if (progressFile != null && progressFile.exists()) {
            progressFile.delete();
        }
    }
}



