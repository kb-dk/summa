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
package dk.statsbiblioteket.summa.common.index;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Methods and constants common for index-handling.
 */
public class IndexCommon {
    private static Log log = LogFactory.getLog(IndexCommon.class);

    /**
     * In order for a Summa-index to be valid, a file with the name version.txt
     * must be present in the index-folder. The file contains a timestamp for
     * the last consolidation of the index. The time is in milliseconds, as
     * returned by {@link System#currentTimeMillis()}.
     */
    public static final String VERSION_FILE = "version.txt";

    /**
     * The pattern for sub-folders under an index-root. The sub-folders contains
     * further index-files. Example {@code 20080815-152500}.
     */
    public static final Pattern TIMESTAMP_PATTERN =
            Pattern.compile("[0-9]{8}-[0-9]{6}");
    /**
     * The reverse of {@link #TIMESTAMP_PATTERN}. Used for creating sub-folders
     * to an index-root.
     */
    public static final String TIMESTAMP_FORMAT =
            "%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS";

    /**
     * @return the timestamp for the current time.
     */
    public static String getTimestamp() {
        return String.format(IndexCommon.TIMESTAMP_FORMAT,
                             Calendar.getInstance());
    }


    /**
     * A matcher for folders using the {@link #TIMESTAMP_PATTERN}.
     */
    public static final FilenameFilter SUBFOLDER_FILTER =
            new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    File full = new File(dir, name);
                    return full.isDirectory() && full.canRead() &&
                           TIMESTAMP_PATTERN.matcher(name).matches();
                }
            };

    /**
     * A matcher for folders using the {@link #TIMESTAMP_PATTERN}. The folders
     * must be writeable.
     */
    public static final FilenameFilter SUBFOLDER_FILTER_WRITE =
            new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    File full = new File(dir, name);
                    return full.isDirectory() && full.canWrite() &&
                           TIMESTAMP_PATTERN.matcher(name).matches();
                }
            };

    /**
     * Searches through sub-folders matching {@link #TIMESTAMP_PATTERN} and
     * returns the last matching folder, in natural order, that contains a file
     * with the name version.txt {@link #VERSION_FILE}.
     * @param root where to begin the search.
     * @param writable if true, write permission must be enabled for the folder.
     * @return the current index or null, if no index could be found.
     */
    public static File getCurrentIndexLocation(File root, boolean writable) {
        try {
            if (!root.exists()) {
                log.debug("getCurrentIndexLocation: Root '" + root
                          + "' does not exist, returning null");
                return null;
            }
            if (!root.canRead()) {
                log.debug("getCurrentIndexLocation: Cannot read '"
                          + root + "'");
                return null;
            }
            File[] subs = writable ?
                          root.listFiles(SUBFOLDER_FILTER_WRITE) :
                          root.listFiles(SUBFOLDER_FILTER);
            if (subs == null) {
                log.debug("getCurrentIndexLocation: listFiles returned null");
                return null;
            }
            Arrays.sort(subs);
            for (int i = subs.length-1 ; i >= 0 ; i--) {
                File version = new File(subs[i], VERSION_FILE);
                if (version.exists()) {
          //        log.trace("getCurrentIndexLocation: got '" + subs[i] + "'");
                    return subs[i];
                }
            }
            log.trace("getCurrentIndexLocation: No matching index-folders");
            return null;
        } catch (Exception e) {
            log.error("Exception calling getCurrentIndexLocation", e);
            return null;
        }
    }
}




