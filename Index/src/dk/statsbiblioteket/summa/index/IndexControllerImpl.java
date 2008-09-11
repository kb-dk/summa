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
import java.io.File;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.util.StateThread;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.index.IndexCommon;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The default implementation of IndexController. It is basically an aggregator
 * for IndexManipulators, which contains a timer that calls commit and
 * consolidate at configurable intervals. It is also a standard ObjectFilter,
 * so further chaining is possible.
 */
// TODO: Mark update on eof, meta-key-value-pattern
// TODO: Consider write-lock
// TODO: Handle deletions without documents
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexControllerImpl extends StateThread implements ObjectFilter,
                                                            IndexManipulator,
                                                            IndexController {
    private static Log log = LogFactory.getLog(IndexControllerImpl.class);

    /**
     * A comma-delimited string of the keys for the manipulators. For each
     * of these names, a substorage will be extracted and used as the setup
     * for a manipulator.
     * </p><p>
     * This property is mandatory. No default.
     */
    public static final String CONF_MANIPULATORS = "summa.index.manipulators";

    /**
     * The index root location defines the top-level for the index.
     * If the location is not an absolute path, it will be appended to the
     * System property "summa.control.client.persistent.dir". If that system
     * property does not exist, the location will be relative to the current
     * dir.
     * </p><p>
     * This property is optional. Default is "index".
     */
    public static final String CONF_INDEX_ROOT_LOCATION =
            "summa.index.indexrootlocation";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String DEFAULT_INDEX_ROOT_LOCATION = "index";

    /**
     * Whether or not a new index should be created upon start. If false, the
     * loading of an existing older index will be attempted.
     * </p><p>
     * This property is optional. Default is false.
     */
    public static final String CONF_CREATE_NEW_INDEX =
            "summa.index.createnewindex";
    public static final boolean DEFAULT_CREATE_NEW_INDEX = false;


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
            "summa.index.commitmaxdocuments";
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
            "summa.index.consolidatetimeout";
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
            "summa.index.consolidatemaxdocuments";
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
            "summa.index.consolidatemaxcommits";
    public static final int DEFAULT_CONSOLIDATE_MAX_COMMITS = 100;

    /**
     * The fully qualified class-name for a manipulator. Used to create an
     * IndexManipulator through reflection. This property must be present in
     * all subconfigurations listed by {@link #CONF_MANIPULATORS}.
     * </p><p>
     * This property is mandatory. No default.
     */
    public static final String CONF_MANIPULATOR_CLASS =
            "summa.index.manipulatorclass";

    private static final int PROFILER_SPAN = 1000;

    /* The indexRoot is the main root. Indexes will always be in sub-folders */
    @SuppressWarnings({"FieldCanBeLocal"}) // Saved for auto-change of location
    private File indexRoot;
    /* The indexLocation is where the current index is. See getConcreteRoot */
    @SuppressWarnings({"FieldCanBeLocal"}) // Saved for auto-change of location
    private File indexLocation;
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

    private boolean indexIsOpen = false;
    private ObjectFilter source;

    public IndexControllerImpl(Configuration conf) {
        log.debug("Creating IndexControllerImpl");
        String indexRootCandidate = conf.getString(CONF_INDEX_ROOT_LOCATION,
                                                   DEFAULT_INDEX_ROOT_LOCATION);
        try {
            indexRoot = new File(indexRootCandidate);
        } catch (Exception e) {
            log.error("Could not construct File from '" + indexRootCandidate
                      + "'. Defaulting to '" + DEFAULT_INDEX_ROOT_LOCATION
                      + "'");
            indexRoot = new File(DEFAULT_INDEX_ROOT_LOCATION);
        }
        try {
            indexLocation = Resolver.getPersistentFile(indexRoot);
        } catch (Exception e) {
            throw new ConfigurationException("Exception resolving '"
                                             + indexRoot
                                             + "' to absolute path");
        }
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
        boolean createNewIndex = conf.getBoolean(CONF_CREATE_NEW_INDEX,
                                                 DEFAULT_CREATE_NEW_INDEX);
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
        log.debug("Manipulators created, opening index");
        profiler = new Profiler();
        profiler.setBpsSpan(PROFILER_SPAN);
        try {
            open(indexLocation, createNewIndex);
        } catch (IOException e) {
            throw new ConfigurationException("Could not open index at '"
                                             + indexLocation.getPath() + "'",
                                             e);
        }
        log.debug("Index opened, starting Watchdog");
        start();
        log.debug("Creation of IndexControllerImpl finished. Ready for Payloads");
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
        if (!indexIsOpen) {
            log.trace("triggerCheck: No index open");
            lastCommit = System.currentTimeMillis();
            lastConsolidate = System.currentTimeMillis();
            return;
        }
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
            long wakeupTime = commitTimeout == -1 ? Long.MAX_VALUE :
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
            } else {
                log.trace("No sleep-time in Watchdog. This eats a lot of "
                          + "resources");
            }
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

    /**
     * @return the index updates per second, averaged over the last
     *         {@link #PROFILER_SPAN} milliseconds.
     */
    public double getCurrentUpdatesPerSecond() {
        return profiler.getBps(true);
    }

    /**
     * @return the current location of the index.
     */
    public File getIndexLocation() {
        return indexLocation;
    }

    /* The IndexManipulator interface aggregates the underlying manipulators */

    public synchronized void open(File indexRoot) throws IOException {
        open(indexRoot, false);
    }
    public synchronized void open(File indexRoot, boolean createNewIndex) throws
                                                                   IOException {
        if (indexRoot == null) {
            throw new IllegalArgumentException("indexRoot must not be null");
        }
        //noinspection DuplicateStringLiteralInspection
        log.info("open(" + indexRoot + ") called");
        if (indexIsOpen) {
            log.debug("Calling close() on previously opened index");
            try {
                close();
            } catch (IOException e) {
                log.error("IOExceptin closing previously opened index", e);
            } catch (Exception e) {
                log.error("Error closing previously opened index", e);
            }
        }
        indexLocation = getConcreteRoot(indexRoot, createNewIndex);
        log.debug("Using '" + indexLocation + "' as concrete root");
        indexIsOpen = true;
        lastCommit =                  System.currentTimeMillis();
        updatesSinceLastCommit =      0;
        lastConsolidate =             System.currentTimeMillis();
        updatesSinceLastConsolidate = 0;
        commitsSinceLastConsolidate = 0;
        profiler.reset();
        for (IndexManipulator manipulator: manipulators) {
            manipulator.open(indexLocation);
        }
        log.trace("Finished open()");
    }

    /*
    * The location of the index files is a subfolder to indexRoot.
    * The name of the subfolder is a timestamp for the construction time of the
    * index with the format YYYYMMDD-HHMM. If subfolders matching the pattern
    * are present in indexRoot, the last (sorted alphanumerically) folder
    * is used. If no such folder exists, a new one is created.
    */
    private File getConcreteRoot(File indexRoot, boolean createNewIndex) throws
                                                                   IOException {
        if (!indexRoot.exists()) {
            log.debug("Creating non-existing indexRoot '" + indexRoot + "'");
            try {
                if (!indexRoot.mkdirs()) {
                    throw new IOException("Unable to create indexRoot '"
                                          + indexRoot + "'");
                }
            } catch (SecurityException e) {
                throw new IOException("Not allowed to create indexRoot '"
                                      + indexRoot + "'");
            }
        }
        // Locate existing folders
        if (!createNewIndex) {
            log.trace("Attempting to locate existing index root");
            File concreteRoot =
                    IndexCommon.getCurrentIndexLocation(indexRoot, true);
            if (concreteRoot != null) {
                log.debug("Located index root '" + concreteRoot + "'");
                return concreteRoot;
            }
        }
        // Create new folder
        log.trace("Attempting to create new index root");
        String folderName = IndexCommon.getTimestamp();
        File concreteRoot = new File(indexRoot, folderName);
        log.debug("Got new root '" + concreteRoot + "'. Creating folder");
        if (!concreteRoot.mkdirs()) {
            throw new IOException("Could not create index folder '"
                                  + concreteRoot + "'");
        }
        return concreteRoot;
    }

    public synchronized void clear() throws IOException {
        //noinspection DuplicateStringLiteralInspection
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
    public synchronized boolean update(Payload payload) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("update(" + payload + ") called");
        }
        if (source == null) {
            throw new IOException("No source defined, cannot update");
        }
        updatesSinceLastCommit++;
        updatesSinceLastConsolidate++;
        boolean requestCommit = false;
        for (IndexManipulator manipulator: manipulators) {
            // TODO: Handle IllegalArgumentexception gracefully for first manipulator
            requestCommit = requestCommit | manipulator.update(payload);
        }
        // TODO: Keep received Payloads and close them on commit?
        profiler.beat();
        if (requestCommit) {
            log.debug("Commit requested during update. Calling commit()");
            commit();
        }
        triggerCheck();
        if (log.isTraceEnabled()) {
            log.trace("update(" + payload + ") finished");
        }
        return requestCommit;
    }

    public synchronized void commit() throws IOException {
        long startTime = System.currentTimeMillis();
        //noinspection DuplicateStringLiteralInspection
        log.trace("commit() called");
        if (updatesSinceLastCommit == 0) {
            log.trace("No updates since last commit");
            lastCommit = System.currentTimeMillis();
            return;
        }
        log.debug("Performing commit");
        for (IndexManipulator manipulator: manipulators) {
            manipulator.commit();
        }
        commitsSinceLastConsolidate++;
        lastCommit = System.currentTimeMillis();
        markAsUpdated(lastCommit);
        updatesSinceLastCommit =      0;
        log.trace("commit() finished in "
                  + (System.currentTimeMillis() - startTime) + " ms");
    }

    public synchronized void consolidate() throws IOException {
        long startTime = System.currentTimeMillis();
        log.trace("consolidate() called");
        if (updatesSinceLastConsolidate == 0) {
            log.trace("No updates since last Consolidate");
            lastConsolidate = System.currentTimeMillis();
            lastCommit = System.currentTimeMillis(); // Consolidate includes commit
            return;
        }
        log.debug("Performing consolidate");
        for (IndexManipulator manipulator: manipulators) {
            manipulator.consolidate();
        }
        lastCommit = System.currentTimeMillis(); // Consolidate includes commit
        markAsUpdated(lastCommit);
        updatesSinceLastCommit =      0;
        lastConsolidate =             System.currentTimeMillis();
        updatesSinceLastConsolidate = 0;
        commitsSinceLastConsolidate = 0;
        log.trace("consolidate() finished in "
                  + (System.currentTimeMillis() - startTime) + " ms");
    }

    /**
     * Mark the underlying index as being updated at the given timestamp. Marks
     * are used to inform readers of changes.
     * @param timestamp the time the update occured.
     */
    private void markAsUpdated(long timestamp) {
        File currentFile = new File(indexLocation, IndexCommon.VERSION_FILE);
        try {
            Files.saveString(Long.toString(timestamp), currentFile);
        } catch (IOException e) {
            log.error("Could not mark '" + currentFile + "' with timestamp " +
                      timestamp + ". Index-watchers will not recognize the"
                      + " indexes at '" + indexLocation + "' as being updated",
                      e);
        }
    }

    public synchronized void close() throws IOException {
        if (!indexIsOpen) {
            log.trace("close() called on already closed");
            return;
        }
        indexIsOpen = false;
        consolidate(); // TODO: Always do this upon close?
        log.debug("Closing down IndexControllerImpl");
        for (IndexManipulator manipulator: manipulators) {
            manipulator.close();
        }
        log.trace("Close finished");
    }

    /* ObjectFilter interface */

    public synchronized void setSource(Filter filter) {
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
        if (!source.hasNext()) { // The big shutdown
            close();
            stop();
            return false;
        }
        Payload payload = source.next();
        try {
            update(payload);
        } catch (IOException e) {
            // Exceptions while indexing is a serious offense, so we escalate
            throw new IOException("IOException when calling update("
                                  + payload + ")", e);
        }
        try {
            payload.close();
        } catch (Exception e) {
            log.warn("Exception while calling close() on payload '" + payload
                     + "' in pump()");
        }
        return source.hasNext();
    }

    public void close(boolean success) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("close(" + success + ") called");
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

    /* IndexController interface */

    public synchronized void addManipulator(IndexManipulator manipulator) {
        manipulators.add(manipulator);
    }

    public synchronized boolean removeManipulator(IndexManipulator
            manipulator) {
        return manipulators.remove(manipulator);
    }
}



