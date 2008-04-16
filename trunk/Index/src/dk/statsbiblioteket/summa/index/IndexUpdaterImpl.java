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
package dk.statsbiblioteket.summa.index;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.StateThread;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The default implementation of IndexUpdater. It is basically an aggregator for
 * IndexManipulators, which also contains a timer that calls commit and
 * consolidate at configurable intervals. It is also a standard ObjectFilter,
 * so further chaining is possible.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexUpdaterImpl extends StateThread implements ObjectFilter,
                                                             IndexManipulator,
                                                             IndexUpdater {
    private static Log log = LogFactory.getLog(IndexUpdaterImpl.class);

    /**
     * A comma-delimited string of the keys for the manipulators. For each
     * of these names, a substorage will be extracted and used as the setup
     * for a manipulator.
     * </p><p>
     * This property is mandatory. No default.
     */
    public static final String CONF_MANIPULATORS = "summa.index.manipulators";

    /**
     * The maximum amount of seconds before a commit is called. Setting this
     * to 0 means that a commit is called for every document update.
     * </p><p>
     * It is highly recommended to set either CONF_COMMIT_TIMEOUT,
     * CONF_COMMIT_MAX_DOCUMENTS or both of these to avoid excessive memory
     * allocations.
     * </p><p>
     * This property is optional. Default is -1 (disabled).
     */
    public static final String CONF_COMMIT_TIMEOUT =
            "summa-index.commit-timeout";
    public static final int DEFAULT_COMMIT_TIMEOUT = -1;

    /**
     * The maximum amount of documents to update before a commit is called.
     * a value of -1 means that this trigger is disabled. A value of 0 or 1
     * means that a commit is called for every document update.
     * </p><p>
     * It is highly recommended to set either CONF_COMMIT_TIMEOUT,
     * CONF_COMMIT_MAX_DOCUMENTS or both of these to avoid excessive memory
     * allocations.
     * </p><p>
     * This property is optional. Default is 1000.
     */
    public static final String CONF_COMMIT_MAX_DOCUMENTS =
            "summa.index.commit-max-documents";
    public static final int DEFAULT_COMMIT_MAX_DOCUMENTS = 1000;

    /**
     * The maximum amount of seconds before a consolidate is called. Setting
     * this to 0 means that a consolidate is called for every document update.
     * </p><p>
     * It is highly recommended to set either CONF_CONSOLIDATE_TIMEOUT,
     * CONF_CONSOLIDATE_MAX_DOCUMENTS, CONF_CONSOLIDATE_MAX_COMMITS or any
     * combination of these to avoid running out of file handles.
     * </p><p>
     * This property is optional. Default is -1 (disabled).
     */
    public static final String CONF_CONSOLIDATE_TIMEOUT =
            "summa.index.consolidate-timeout";
    public static final int DEFAULT_CONSOLIDATE_TIMEOUT = -1;

    /**
     * The maximum amount of documents to update before a consolidate is called.
     * a value of -1 means that this trigger is disabled. A value of 0 or 1
     * means that a consolidate is called for every document update.
     * </p><p>
     * It is highly recommended to set either CONF_CONSOLIDATE_TIMEOUT,
     * CONF_CONSOLIDATE_MAX_DOCUMENTS, CONF_CONSOLIDATE_MAX_COMMITS or any
     * combination of these to avoid running out of file handles.
     * </p><p>
     * This property is optional. Default is -1 (disabled).
     */
    public static final String CONF_CONSOLIDATE_MAX_DOCUMENTS =
            "summa.index.consolidate-max-documents";
    public static final int DEFAULT_CONSOLIDATE_MAX_DOCUMENTS = -1;

    /**
     * The maximum amount of commits to perform before a consolidate is called.
     * a value of -1 means that this trigger is disabled. A value of 0 or 1
     * means that a consolidate is called for every commit.
     * </p><p>
     * It is highly recommended to set either CONF_CONSOLIDATE_TIMEOUT,
     * CONF_CONSOLIDATE_MAX_DOCUMENTS, CONF_CONSOLIDATE_MAX_COMMITS or any
     * combination of these to avoid running out of file handles.
     * </p><p>
     * This property is optional. Default is 100.
     */
    public static final String CONF_CONSOLIDATE_MAX_COMMITS =
            "summa.index.consolidate-max-commits";
    public static final int DEFAULT_CONSOLIDATE_MAX_COMMITS = 100;

    /**
     * The fully qualified class-name for a manipulator. Used to create an
     * IndexManipulator through reflection. This property must be present in
     * all subconfigurations listed by {@link #CONF_MANIPULATORS}.
     * </p><p>
     * This property is mandatory. No default.
     */
    public static final String CONF_MANIPULATOR_CLASS =
            "summa.index.manipulator-class";

    private static final int PROFILER_SPAN = 1000;

    private List<IndexManipulator> manipulators;
    private int commitTimeout =           DEFAULT_COMMIT_TIMEOUT;
    private int commitMaxDocuments =      DEFAULT_COMMIT_MAX_DOCUMENTS;
    private int consolidateTimeout =      DEFAULT_CONSOLIDATE_TIMEOUT;
    private int consolidateMaxDocuments = DEFAULT_CONSOLIDATE_MAX_DOCUMENTS;
    private int consolidateMaxCommits =   DEFAULT_CONSOLIDATE_MAX_COMMITS;

    private long lastCommit =                  System.currentTimeMillis();
    private long updatesSinceLastCommit =      0;
    private long lastConsolidate =             System.currentTimeMillis();
    private long updatesSinceLastConsolidate = 0;
    private long commitsSinceLastConsolidate = 0;

    private Profiler profiler;
    private ObjectFilter source;

    private boolean closed = false; // Whether close has been called

    public IndexUpdaterImpl(Configuration conf) {
        log.debug("Creating IndexUpdaterImpl");
        List<String> manipulatorKeys;
        try {
            manipulatorKeys = conf.getStrings(CONF_MANIPULATORS);
            if (manipulatorKeys.size() == 0) {
                log.warn("No manipulators specified in " + CONF_MANIPULATORS
                         +". This is probably an error");
            } else {
                log.debug("Got " + manipulatorKeys.size()
                          + " manipulator configuration keys");
            }
        } catch (Exception e) {
            throw new ConfigurationException("Could not get value for key "
                                             + CONF_MANIPULATORS);
        }
        log.trace("Extracting basic setup");
        commitTimeout =
                conf.getInt(CONF_COMMIT_TIMEOUT, commitTimeout);
        commitMaxDocuments =
                conf.getInt(CONF_COMMIT_MAX_DOCUMENTS, commitMaxDocuments);
        consolidateTimeout =
                conf.getInt(CONF_CONSOLIDATE_TIMEOUT, consolidateTimeout);
        consolidateMaxDocuments =
           conf.getInt(CONF_CONSOLIDATE_MAX_DOCUMENTS, consolidateMaxDocuments);
        consolidateMaxCommits =
               conf.getInt(CONF_CONSOLIDATE_MAX_COMMITS, consolidateMaxCommits);
        log.debug("Basic setup: commitTimeout: " + consolidateTimeout
                  + " seconds, commitMaxDocuments: " + commitMaxDocuments
                  + ", consolidateTimeout: " + consolidateTimeout + " seconds, "
                  + ", consolidateMaxDocuments: " + consolidateMaxDocuments
                  + ", consolidateMaxCommits: " + consolidateMaxCommits);
        //noinspection DuplicateStringLiteralInspection
        log.trace("Creating " + manipulatorKeys.size() + " manipulators");
        manipulators = new ArrayList<IndexManipulator>(manipulatorKeys.size());
        for (String manipulatorKey: manipulatorKeys) {
            //noinspection OverlyBroadCatchBlock
            try {
                log.trace("Creating manipulator for key '" + manipulatorKey
                          + "'");
                IndexManipulator manipulator =
                    createManipulator(conf.getSubConfiguration(manipulatorKey));
                log.trace("Manipulator created for key '" + manipulatorKey
                          + "'");
                manipulators.add(manipulator);
            } catch (Exception e) {
                throw new RuntimeException("Could not create manipulator '"
                                           + manipulatorKey + "'", e);
            }
        }
        log.trace("Manipulators created. Activating watchdog");
        profiler = new Profiler();
        profiler.setBpsSpan(PROFILER_SPAN);
        start();
        log.debug("Creation of IndexUpdaterImpl finished. Ready for Payloads");
    }

    private IndexManipulator createManipulator(Configuration conf) {
        String manipulatorClassName;
        try {
            manipulatorClassName = conf.getString(CONF_MANIPULATOR_CLASS);
        } catch (NullPointerException e) {
            throw new NullPointerException("Could not locate key '"
                                           + CONF_MANIPULATOR_CLASS + "'");
        }
        log.debug("Creating manipulator '" + manipulatorClassName + "'");
        Class<? extends IndexManipulator> manipulatorClass =
                Configuration.getClass(CONF_MANIPULATOR_CLASS,
                                       IndexManipulator.class, conf);
        log.debug("Got IndexManipulator class " + manipulatorClass
                  + ". Creating...");
        return Configuration.create(manipulatorClass, conf);
    }

    private synchronized void triggerCheck() throws IOException {
        log.trace("Triggercheck called");
        if (commitTimeout != -1 &&
            lastCommit + commitTimeout < System.currentTimeMillis()
            || commitMaxDocuments != -1 &&
               commitMaxDocuments <= updatesSinceLastCommit
            || commitTimeout == 0) {
            commit();
        }
        if (consolidateTimeout != -1 &&
            lastConsolidate + consolidateTimeout < System.currentTimeMillis()
            || consolidateMaxDocuments != -1 &&
               consolidateMaxDocuments <= updatesSinceLastConsolidate
            || consolidateMaxCommits != -1 &&
               consolidateMaxCommits <= commitsSinceLastConsolidate
            || consolidateTimeout == 0) {
            consolidate();
        }
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    protected void runMethod() {
        while (isRunning()) {
            if (commitTimeout == -1 && consolidateTimeout == -1) {
                log.debug("No time-based triggers for Watchdog. "
                          + "Exiting thread.");
                break;
            }
            if (commitTimeout == 0 || consolidateTimeout == 0) {
                log.debug("commitTimeout(" + commitTimeout
                          + ") or consolidateTimeout(" + consolidateTimeout
                          + ") is 0. This means triggering on all updates. "
                          + "No time-based triggers needed for Watchdog. "
                          + "Exiting thread.");
                break;
            }
            long wakeupTime = commitTimeout == -1 ? 0 :
                              lastCommit + commitTimeout;
            wakeupTime = consolidateTimeout == -1 ? wakeupTime :
                         Math.min(wakeupTime,
                                  lastConsolidate + consolidateTimeout);
            long sleepTime = wakeupTime - System.currentTimeMillis();
            if (sleepTime > 0) {
                log.trace("Watchdog sleeping for " + sleepTime + " ms");
                try {
                    Thread.sleep(wakeupTime);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while sleeping. "
                                               + "Stopping timer-based "
                                               + "watchdog", e);
                }
            }
            if (isRunning()) {
                try {
                    triggerCheck();
                } catch (IOException e) {
                    throw new RuntimeException("IOException during triggerCheck"
                                               + " from watchdog thread. "
                                               + "Stopping timer-based "
                                               + "watchdog");
                }
            }
        }
    }

    /**
     * @return the index updates per second, averaged over the last
     *         {@link #PROFILER_SPAN} milliseconds.
     */
    public double getCurrentUpdatesPerSecond() {
        return profiler.getBps(true);
    }

    /* The IndexManipulator interface aggregates the underlying manipulators */

    public synchronized void clear() throws IOException {
        log.debug("clear() called");
        lastCommit =                  System.currentTimeMillis();
        updatesSinceLastCommit =      0;
        lastConsolidate =             System.currentTimeMillis();
        updatesSinceLastConsolidate = 0;
        commitsSinceLastConsolidate = 0;
        profiler.reset();
        for (IndexManipulator manipulator: manipulators) {
            manipulator.clear();
        }
        log.trace("Finished clear()");
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public synchronized void update(Payload payload) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("update(" + payload + ") called");
        }
        if (source == null) {
            throw new IOException("No source defined, cannot update");
        }
        updatesSinceLastCommit++;
        updatesSinceLastConsolidate++;
        for (IndexManipulator manipulator: manipulators) {
            manipulator.update(payload);
        }
        profiler.beat();
        triggerCheck();
        if (log.isTraceEnabled()) {
            log.trace("update(" + payload + ") finished");
        }
    }

    public void commit() throws IOException {
        long startTime = System.currentTimeMillis();
        log.debug("commit() called");
        for (IndexManipulator manipulator: manipulators) {
            manipulator.commit();
        }
        commitsSinceLastConsolidate++;
        lastCommit = System.currentTimeMillis();
        updatesSinceLastCommit =      0;
        log.trace("commit() finished in "
                  + (System.currentTimeMillis() - startTime) + " ms");
    }

    public void consolidate() throws IOException {
        long startTime = System.currentTimeMillis();
        log.debug("consolidate() called");
        for (IndexManipulator manipulator: manipulators) {
            manipulator.consolidate();
        }
        lastCommit = System.currentTimeMillis(); // Consolidate includes commit
        updatesSinceLastCommit =      0;
        lastConsolidate =             System.currentTimeMillis();
        updatesSinceLastConsolidate = 0;
        commitsSinceLastConsolidate = 0;
        log.trace("consolidate() finished in "
                  + (System.currentTimeMillis() - startTime) + " ms");
    }

    public void close() {
        if (closed) {
            log.trace("close() called on already closed");
            return;
        }
        closed = true;
        log.debug("Closing down IndexUpdaterImpl");
        stop();
        for (IndexManipulator manipulator: manipulators) {
            manipulator.close();
        }
        log.trace("Close finished");
    }

    /* ObjectFilter interface */

    public void setSource(Filter filter) {
        if (filter instanceof ObjectFilter) {
            log.debug("Assigning source filter" + filter);
            source = (ObjectFilter)filter;
        } else {
            throw new IllegalArgumentException("Only ObjectFilters can be used "
                                               + "as source. The provided "
                                               + "filter was a "
                                               + filter.getClass());
        }
    }

    public boolean pump() throws IOException {
        if (source == null) {
            throw new IOException("No source defined, cannot pump");
        }
        if (!source.hasNext()) {
            close();
            return false;
        }
        Payload payload = source.next();
        try {
            update(payload);
        } catch (IOException e) {
            // Non-updates of indexes is a serious offense, so we escalate
            throw new IOException("IOException when calling update("
                                  + payload + ")", e);
        }
        return source.hasNext();
    }

    public void close(boolean success) {
        if (source == null) {
            log.error("No source defined, cannot close");
        }
        source.close(success);
    }

    public boolean hasNext() {
        if (source == null) {
            log.error("No source defined, cannot call source.hasNext");
            return false;
        }
        return source.hasNext();
    }

    public Payload next() {
        Payload payload = source.next();
        try {
            update(payload);
        } catch (IOException e) {
            // Non-updates of indexes is a serious offense, so we escalate
            throw new RuntimeException("IOException when calling next("
                                       + payload + ")", e);
        }
        return payload;
    }

    public void remove() {
        log.warn("remove() not supported");
    }

    /* IndexUpdater interface */

    public void addManipulator(IndexManipulator manipulator) {
        manipulators.add(manipulator);
    }

    public boolean removeManipulator(IndexManipulator manipulator) {
        return manipulators.remove(manipulator);
    }
}
