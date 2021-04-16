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

import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
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
        state = QAInfo.State.QA_OK,
        author = "te, mke")
public class ProgressTracker {
    private static Log log = LogFactory.getLog(ProgressTracker.class);

    private static final String TAG = "lastRecordTimestamp";
    public static final Pattern TIMESTAMP_PATTERN =
            Pattern.compile(".*<" + TAG + ">.*"
                            + "<iso>([-.0-9]+)</iso>.*"
                            + "</" + TAG + ">.*", Pattern.DOTALL);
    public static final Pattern EPOCH_TIMESTAMP_PATTERN =
            Pattern.compile(".*<" + TAG + ">.*"
                            + "<epoch>([0-9]+)</epoch>.*"
                            + "</" + TAG + ">.*", Pattern.DOTALL);

    /*    public static final Pattern TIMESTAMP_PATTERN =
            Pattern.compile(".*<" + TAG + ">.*"
                            + "<epoch>"
                            + "([0-9]{4})([0-9]{2})([0-9]{2})-"
                            + "([0-9]{2})([0-9]{2})([0-9]{2})"
                            + "</" + TAG + ">.*", Pattern.DOTALL);*/
//    public static final String ISO_TIME = "%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS";
    public static final String ISO_TIME = "%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS.%1$tL";
    public static final String TIMESTAMP_FORMAT =
            "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n"
            +"<" + TAG + ">\n"
            + "<epoch>%1$tQ</epoch>\n"
            + "<!-- As of 2013-05-01, iso is the authoritative timestamp and epoch is deprecated -->\n"
            + "<iso>" + ISO_TIME + "</iso>\n"
            + "</" + TAG + ">\n";
    // <iso>20120718-103035</iso> or <iso>20120718-103035.123</iso>
    private static final SimpleDateFormat timeParser = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss");
    private static final SimpleDateFormat timeParserMS = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss.SSS");

    private long lastExternalUpdate;
    private long lastInternalUpdate;
    private long batchSize;
    private long graceTime;
    private long numUpdates;
    private long numUpdatesLastFlush;
    private File progressFile;

    public ProgressTracker (File progressFile, long batchSize, long graceTime) {
        this.batchSize = batchSize;
        this.graceTime = graceTime;
        this.progressFile = progressFile;

        log.debug("Created " + this);
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

        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    Locale.ROOT, "Storing progress in '%s' (%d records has been extracted so far, last timestamp: %s)",
                    progressFile, numUpdates-1, String.format(Locale.ROOT, ISO_TIME, lastExternalUpdate)));
        }
        try {
            Files.saveString(String.format(Locale.ROOT, TIMESTAMP_FORMAT, lastExternalUpdate), progressFile);
        } catch (IOException e) {
            log.error("close(true): Unable to store progress in file '" + progressFile + "': " + e.getMessage(), e);

        }
        numUpdatesLastFlush = numUpdates;
    }

    /**
     * Read the last modification time in from the progress file
     */
    public void loadProgress () {
        loadProgress(0);
    }

    /**
     * Read the last modification time in from the progress file with an offset
     * @param offset offset in milliseconda. Can be negative.
     */
    public void loadProgress (final long offset) {
        log.debug("Attempting to get previous progress stored in file " + progressFile + " with offset " + offset);

        if (progressFile.exists() && progressFile.isFile() && progressFile.canRead()) {
            log.trace("getStartTime has persistence file");
            try {
                long startTime = getTimestamp(progressFile, Files.loadString(progressFile));
                try {
                    log.info(String.format(Locale.ROOT,
                            "Extracted timestamp " + ISO_TIME + " from '%2$s'. This will be adjusted with %3$dms",
                            startTime, progressFile, offset));
                } catch (Exception e) {
                    log.warn("Could not output properly formatted timestamp for " + startTime + " ms");
                }
                startTime += offset;
                if (startTime < 0) {
                    startTime = 0;
                }
                lastExternalUpdate = startTime;
                lastInternalUpdate = System.currentTimeMillis();
            } catch (IOException e) {
                //noinspection DuplicateStringLiteralInspection
                log.error("getStartTime: Unable to open existing file '" + progressFile + "'. Returning 0");
            }
        } else {
            log.warn("Unable to read progress file " + progressFile);
        }
    }

    public long getLastUpdate() {
        return lastExternalUpdate;
    }
    public String getLastUpdateStr() {
        try {
            return String.format(Locale.ROOT, ISO_TIME, lastExternalUpdate);
        } catch (Exception e) {
            log.warn("Could not output properly formatted timestamp for " + lastExternalUpdate + " ms");
            return "N/A";
        }
    }

    public void clearProgressFile() {
        if (progressFile.exists()) {
            if (!progressFile.delete()) {
                log.warn("Unable to delete progress file '" + progressFile.getAbsolutePath() + "'");
            }
        }
    }

    static synchronized long getTimestamp(File progressFile, String xml) {
        Matcher matcher = TIMESTAMP_PATTERN.matcher(xml);
        if (!matcher.matches() || matcher.groupCount() != 1) {
            //noinspection DuplicateStringLiteralInspection
            log.error("getTimestamp: Could not locate iso timestamp in file '" + progressFile + "' containing '" + xml
                      + "'. Returning 0");
            return 0;
        }
        final String iso = matcher.group(1);
        if (iso.length() == 15) { // Old second-granularity timestamp, so use epoch if available
            log.debug("Old iso-time '" + iso + "' detected in progress-file. Attempting epoch parsing");
            Matcher epochMatcher = EPOCH_TIMESTAMP_PATTERN.matcher(xml);
            if (!epochMatcher.matches() || epochMatcher.groupCount() != 1) {
                log.warn("Old iso-time '" + iso + "' but no epoch. Using second-granularity iso time");
                try {
                    return timeParser.parse(iso).getTime();
                } catch (ParseException e) {
                    log.error("Unable to parse iso datetime '" + iso + "'. Returning epoch 0 (1970-01-01)");
                    return 0;
                }
            }
            return Long.parseLong(epochMatcher.group(1));
        }
        if (iso.length() != 19) {
            log.warn("Unsupported iso-time character count of " + iso.length() + " (expected 19). " +
                     "Attempting parsing anyway");
        }
        try {
            return timeParserMS.parse(iso).getTime();
        } catch (ParseException e) {
            log.error("Unable to parse iso datetime '" + iso + "'. Returning epoch 0 (1970-01-01)");
        }

        return 0;
/*        return new GregorianCalendar(Integer.parseInt(matcher.group(1)),
                                     // Months in Calendar starts at 0
                                     Integer.parseInt(matcher.group(2))-1,
                                     Integer.parseInt(matcher.group(3)),
                                     Integer.parseInt(matcher.group(4)),
                                     Integer.parseInt(matcher.group(5)),
                                     Integer.parseInt(matcher.group(6))).
                getTimeInMillis();*/
    }

    public File getProgressFile() {
        return progressFile;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT,
                "ProgressTracker(batchSize=%d, graceTime=%d, progressFile='%s', updates=%d, lastExternalUpdate=%s)",
                batchSize, graceTime, progressFile, numUpdates, getLastUpdateStr());
    }
}
