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
package dk.statsbiblioteket.summa.storage.api.filter;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for Recordreader, used to only flush the progress file when
 * needed.
 * <p/>
 * This class does NOT count the number of records read. It may receive
 * more or less updates than the number of records.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te, mke")
public class ProgressTracker {
    private static Log log = LogFactory.getLog(ProgressTracker.class);

    private static final String TAG = "lastRecordTimestamp";
    public static final Pattern TIMESTAMP_PATTERN =
            Pattern.compile(".*<" + TAG + ">.*"
                            + "<epoch>([0-9]+)</epoch>.*"
                            + "</" + TAG + ">.*", Pattern.DOTALL);
    /*    public static final Pattern TIMESTAMP_PATTERN =
            Pattern.compile(".*<" + TAG + ">.*"
                            + "<epoch>"
                            + "([0-9]{4})([0-9]{2})([0-9]{2})-"
                            + "([0-9]{2})([0-9]{2})([0-9]{2})"
                            + "</" + TAG + ">.*", Pattern.DOTALL);*/
    public static final String ISO_TIME = "%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS";
    public static final String TIMESTAMP_FORMAT =
            "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n"
            +"<" + TAG + ">\n"
            + "<epoch>%1$tQ</epoch>\n"
            + "<iso>" + ISO_TIME + "</iso>\n"
            + "</" + TAG + ">\n";

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
     * @param timestamp the time since Java Epoch (1972) in ms.
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
                    String.format(TIMESTAMP_FORMAT,
                                  lastExternalUpdate), progressFile);
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
                                "Extracted timestamp "
                                + ISO_TIME
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
        } else {
            log.warn("Unable to read progress file " + progressFile);
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
        if (!matcher.matches() || matcher.groupCount() != 1) {
            //noinspection DuplicateStringLiteralInspection
            log.error("getTimestamp: Could not locate timestamp in "
                      + "file '" + progressFile + "' containing '"
                      + xml + "'. Returning 0");
            return 0;
        }
        return Long.parseLong(matcher.group(1));
/*        return new GregorianCalendar(Integer.parseInt(matcher.group(1)),
                                     // Months in Calendar starts at 0
                                     Integer.parseInt(matcher.group(2))-1,
                                     Integer.parseInt(matcher.group(3)),
                                     Integer.parseInt(matcher.group(4)),
                                     Integer.parseInt(matcher.group(5)),
                                     Integer.parseInt(matcher.group(6))).
                getTimeInMillis();*/
    }
}
