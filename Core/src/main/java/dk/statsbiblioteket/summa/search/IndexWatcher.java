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
package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.index.IndexCommon;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.watch.Observable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Watches for changes to a Summa index, triggering an update to listeners.
 * Indexes are placed under a root, in folders made up of timestamps. A file
 * with the name {@link IndexCommon#VERSION_FILE}, containing the time the
 * index was consolidated, must be present in the folder. The folders are used
 * in natural sorted order, with preference for the last matching folder.
 */
// TODO: Consider moving this to Common as Facets can also use it
// TODO: Support changes versions
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexWatcher extends Observable<IndexListener> implements Configurable, Runnable {
    private static Log log = LogFactory.getLog(IndexWatcher.class);

    /**
     * How often, in milliseconds, the watcher should check for a new index.
     * </p><p>
     * This is optional. Default is 30 seconds (30,000 milliseconds).
     *
     * @see #CONF_INDEX_WATCHER_MIN_RETENTION
     */
    public static final String CONF_INDEX_WATCHER_CHECK_INTERVAL = "summa.indexwatcher.checkinterval";
    public static final int DEFAULT_CHECK_INTERVAL = 1000 * 30; // Every 30 sec.

    /**
     * The root for the index. This will be resolved using
     * {@link Resolver#getPersistentFile(File)}.
     * </p><p>
     * This is optional. Default is "index".
     */
    public static final String CONF_INDEX_WATCHER_INDEX_ROOT = "summa.indexwatcher.indexroot";
    // TODO: Consider creating a class with Summa constants
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String DEFAULT_INDEX_ROOT = "index";

    /**
     * The minimum retention time for notifying about updates. Depending on
     * hardware and index complexity, this can range from a few seconds to
     * hours or days.
     * Setting this value very low on an often-changing index can lead to low
     * performance as (re)opening indexes normally take a non-trivial amount of
     * resources, especially if warm-up is specified.
     * </p><p>
     * This is optional. Default is 5 minutes (300,000 milliseconds).
     */
    public static final String CONF_INDEX_WATCHER_MIN_RETENTION = "summa.indexwatcher.minretention";
    public static final int DEFAULT_MIN_RETENTION = 1000 * 60 * 5; // 5 min.

    private int indexCheckInterval = DEFAULT_CHECK_INTERVAL;
    private int indexMinRetention = DEFAULT_MIN_RETENTION;
    private File absoluteIndexRoot;
    private File lastCheckedLocation = null;
    private long lastCheckedTimestamp = -1;

    private boolean checkHasBeenPerformed = false;
    private boolean continueWatching = false;
    private long lastNotification = 0;

    /**
     * Set up an IndexWatcher based on the given configuration. The watcher
     * won't start before {@link #startWatching} is called.
     *
     * @param conf the setup for the watcher.
     */
    public IndexWatcher(Configuration conf) {
        indexCheckInterval = conf.getInt(CONF_INDEX_WATCHER_CHECK_INTERVAL, indexCheckInterval);
        indexMinRetention = conf.getInt(CONF_INDEX_WATCHER_MIN_RETENTION, indexMinRetention);
        String indexRoot = conf.getString(CONF_INDEX_WATCHER_INDEX_ROOT, DEFAULT_INDEX_ROOT);
        absoluteIndexRoot = Resolver.getPersistentFile(new File(indexRoot));
        log.debug(String.format(Locale.ROOT, "Constructing with %s=%d, %s=%d, %s='%s', absoluteIndexRoot='%s'",
                                CONF_INDEX_WATCHER_CHECK_INTERVAL, indexCheckInterval,
                                CONF_INDEX_WATCHER_MIN_RETENTION, indexMinRetention, CONF_INDEX_WATCHER_INDEX_ROOT,
                                indexRoot, absoluteIndexRoot.toString()));

    }

    /**
     * Start watching for changes to the watched index. Note that a call to this
     * method will always result in an immediate notification of indexChanged,
     * except in the case where a watch is already running. If this method is
     * called on an already running watcher, nothing will happen.
     */
    public synchronized void startWatching() {
        log.trace("startWatching called");
        if (continueWatching) {
            log.trace("Already watching");
            return;
        }
        checkHasBeenPerformed = false;
        updateAndReturnCurrentState();
        Thread thisThread = new Thread(this, "IndexWatcher daemon");
        thisThread.setDaemon(true);
        thisThread.start();
    }

    /**
     * Stop watching for changes to the index. This method can be called safely
     * multiple times.
     */
    public synchronized void stopWatching() {
        log.trace("Stopping watch");
        continueWatching = false;
    }

    @Override
    public void run() {
        log.debug("Starting watch for index changes with max sleep-time " + indexCheckInterval + " ms");
        continueWatching = true;
        while (continueWatching) {
            updateAndReturnCurrentState();
            long sleepTime = Math.max(indexCheckInterval,
                                      lastNotification + indexMinRetention - System.currentTimeMillis());
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                try  {
                    log.warn("Received InterruptedException while sleeping between index-checks. Ignoring");
                } catch (NullPointerException ne) {
                    // Ignore as this means we're shutting down
                }
            }
        }
        try {
            log.debug("Stopping watch for index changes");
        } catch (NullPointerException ne) {
            // Ignore as this means we're shutting down
        }
    }

    /**
     * Check for indexes and notify all listeners is a change is discovered.
     * This method blocks until the job is done.
     *
     * @return the location of the current index after the check.
     */
    public synchronized File updateAndReturnCurrentState() {
//        log.trace("updateAndReturnCurrentState called");
        File newChecked = getCurrentIndexLocation();
        long timestamp = getTimestamp(newChecked);
        if (checkHasBeenPerformed && equals(lastCheckedLocation, newChecked) && lastCheckedTimestamp == timestamp) {
            return lastCheckedLocation;
        }
        checkHasBeenPerformed = true;
        lastCheckedLocation = newChecked;
        lastCheckedTimestamp = timestamp;
        notifyListeners();
        return lastCheckedLocation;
    }

    private long getTimestamp(File newChecked) {
        if (newChecked == null) {
            return -1;
        }
        File vFile = new File(newChecked, IndexCommon.VERSION_FILE);
        if (!vFile.exists()) {
            return -1;
        }
        String content = null;
        try {
            content = Files.loadString(vFile);
            long v = Long.parseLong(content);
            if (log.isTraceEnabled()) {
                log.trace("Got version " + v + " from '" + vFile + "'");
            }
            return v;
        } catch (IOException e) {
            log.warn(String.format(Locale.ROOT, "Unable to load content of '%s'", vFile), e);
            return -1;
        } catch (NumberFormatException e) {
            log.warn(String.format(Locale.ROOT, "Unable to parse content '%s' of '%s' as a long", content, vFile), e);
            return -1;
        }
    }

    private boolean equals(File f1, File f2) {
        return f1 == null && f2 == null || f1 != null && f1.equals(f2);
    }

    /**
     * @return the current index location, as described in the JavaDoc for the
     *         class or null, if no index could be located.
     */
    protected File getCurrentIndexLocation() {
        return IndexCommon.getCurrentIndexLocation(absoluteIndexRoot, false);
    }

    /* Observer pattern */

    private void notifyListeners() {
        log.trace("notifying listeners with index location '" + lastCheckedLocation
                  + "' and timestamp " + lastCheckedTimestamp);
        for (IndexListener listener : getListeners()) {
            try {
                listener.indexChanged(lastCheckedLocation);
            } catch (Exception e) {
                log.error("Encountered exception during notify with location '" + lastCheckedLocation + "'", e);
            }
        }
        lastNotification = System.currentTimeMillis();
    }

    public void addIndexListener(IndexListener listener) {
        addListener(listener);
    }

    public void removeIndexListener(IndexListener listener) {
        removeListener(listener);
    }
}
