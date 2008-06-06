/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.search;

import java.io.File;

import dk.statsbiblioteket.util.watch.Observable;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Watches for changes to a Summa index, triggering an update to listeners.
 */
// TODO: Consider moving this to Common as Facets can also use it
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexWatcher extends Observable implements Configurable, Runnable {
    private static Log log = LogFactory.getLog(IndexWatcher.class);

    /**
     * How often, in milliseconds, the watcher should check for a new index.
     * </p><p>
     * This is optional. Default is 30 seconds (30,000 milliseconds).
     * @see {@link #CONF_INDEX_WATCHER_MIN_RETENTION}.
     */
    public static final String CONF_INDEX_WATCHER_CHECK_INTERVAL =
            "summa.index-watcher.check-interval";
    public static final int DEFAULT_CHECK_INTERVAL = 1000 * 30; // Every 30 sec.

    /**
     * The root for the index. This will be resolved using
     * {@link Resolver#getPersistentFile(File)}.
     * </p><p>
     * This is optional. Default is "index".
     */
    public static final String CONF_INDEX_WATCHER_INDEX_ROOT =
            "summa.index-watcher.index-root";
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
     *
     */
    public static final String CONF_INDEX_WATCHER_MIN_RETENTION =
            "summa.index-watcher.min-retention";
    public static final int DEFAULT_MIN_RETENTION = 1000 * 60 * 5; // 5 min.

    private int indexCheckInterval = DEFAULT_CHECK_INTERVAL;
    private int indexMinRetention = DEFAULT_MIN_RETENTION;
    private String indexRoot = DEFAULT_INDEX_ROOT;
    private File absoluteIndexRoot;

    private boolean continueWatching = false;
    private Thread thisThread = null;

    /**
     * Set up an IndexWatcher based on the given configuration. The watcher
     * won't start before {@link #startWatching} is called.
     * @param conf the setup for the watcher.
     */
    public IndexWatcher(Configuration conf) {
        indexCheckInterval = conf.getInt(CONF_INDEX_WATCHER_CHECK_INTERVAL,
                                         indexCheckInterval);
        indexMinRetention = conf.getInt(CONF_INDEX_WATCHER_MIN_RETENTION,
                                        indexMinRetention);
        indexRoot = conf.getString(CONF_INDEX_WATCHER_INDEX_ROOT, indexRoot);
        absoluteIndexRoot = Resolver.getPersistentFile(new File(indexRoot));
        log.debug(String.format(
                "Constructing with %s=%d, %s=%d, %s='%s', "
                + "absoluteIndexRoot='%s'",
                CONF_INDEX_WATCHER_CHECK_INTERVAL, indexCheckInterval,
                CONF_INDEX_WATCHER_MIN_RETENTION, indexMinRetention,
                CONF_INDEX_WATCHER_INDEX_ROOT, indexRoot,
                absoluteIndexRoot.toString()
                ));
    }

    public synchronized void startWatching() {
        // TODO: Implement this

    }

    public synchronized void stopWatching() {
        // TODO: Implement this

    }

    public void run() {
        // TODO: Implement this
    }
}
