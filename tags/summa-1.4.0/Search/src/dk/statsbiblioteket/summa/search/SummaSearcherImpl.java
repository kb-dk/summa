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
package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.util.ChangingSemaphore;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import dk.statsbiblioteket.summa.search.dummy.SearchNodeDummy;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Convenience base class for SummaSearchers, taking care of basic setup.
 * The default Impl is oriented towards watching for changes to the file system,
 * normally initiated by index manipulators.
 * Relevant properties from {@link SearchNodeFactory},
 * {@link dk.statsbiblioteket.summa.search.api.SummaSearcher},
 * {@link IndexWatcher}, {@link SearchNodeLoadBalancer} and
 * {@link LuceneIndexUtils} needs to be specified in the configuration.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummaSearcherImpl implements SummaSearcherMBean, SummaSearcher,
                                          IndexListener {
    private static Log log = LogFactory.getLog(SummaSearcherImpl.class);

    /**
     * If a new search is requested and there is no free slots, the search
     * is queued. If the queue reaches its max-size, the request is not queued
     * and an exception is thrown.
     * </p><p>
     * This is optional. Default is 50.
     */
    public static final String CONF_SEARCH_QUEUE_MAX_SIZE =
            "summa.search.searchqueue.maxsize";
    public static final int DEFAULT_SEARCH_QUEUE_MAX_SIZE = 50;

    /**
     * If no searchers are ready upon search, wait up to this number of
     * milliseconds for a searcher to become ready. If no searchers are ready
     * at that time, an exception will be thrown.
     */
    public static final String CONF_SEARCHER_AVAILABILITY_TIMEOUT =
            "summa.search.searcheravailability.timeout";
    public static final int DEFAULT_SEARCHER_AVAILABILITY_TIMEOUT = 5 * 60000;

    /**
     * If specified, watching for index changes will be disabled and an open
     * will be called with the stated root when the SummaSearcher is created.
     * </p><p>
     * Optional. Default is null (disabled).
     */
    public static final String CONF_STATIC_ROOT = "summa.search.staticroot";
    public static final String DEFAULT_STATIC_ROOT = null;

    private int searcherAvailabilityTimeout =
            DEFAULT_SEARCHER_AVAILABILITY_TIMEOUT;

    private ChangingSemaphore searchQueue;
    private ChangingSemaphore freeSlots = new ChangingSemaphore(0);
    private SearchNode searchNode;
    private IndexWatcher watcher;

    private File indexFolder;
    private long lastResponseTime = -1;
    private AtomicLong queryCount = new AtomicLong(0);
    private AtomicLong totalResponseTime = new AtomicLong(0);

    /**
     * Extracts basic settings from the configuration and constructs the
     * underlying tree of SearchNodes.
     * <p></p>
     * The searcher will instantiate a {@link SearchNode} based on the
     * property {@link SearchNodeFactory#CONF_NODE_CLASS}. If this property
     * is not defined the searcher will fall back to using a
     * {@link dk.statsbiblioteket.summa.search.dummy.SearchNodeDummy}
     * @param conf the configuration for the searcher.
     * @throws RemoteException if the underlying SearchNode could not be
     *                         constructed.
     */
    public SummaSearcherImpl(Configuration conf) throws RemoteException {
        log.info("Constructing SummaSearcherImpl");
        int searchQueueMaxSize = conf.getInt(CONF_SEARCH_QUEUE_MAX_SIZE,
                                             DEFAULT_SEARCH_QUEUE_MAX_SIZE);
        searcherAvailabilityTimeout =
                conf.getInt(CONF_SEARCHER_AVAILABILITY_TIMEOUT,
                            searcherAvailabilityTimeout);
        log.trace("searcherAvailabilityTimeout=" + searcherAvailabilityTimeout 
                  + " ms");

        searchQueue = new ChangingSemaphore(searchQueueMaxSize, true);
        log.trace("Constructing search node");
        searchNode = SearchNodeFactory.createSearchNode(conf,
                                                        SearchNodeDummy.class);

        // Ready for open
        String staticRoot =
                conf.getString(CONF_STATIC_ROOT, DEFAULT_STATIC_ROOT);
        if (staticRoot == null || "".equals(staticRoot)) {
            log.trace("Starting watcher");
            watcher = new IndexWatcher(conf);
            watcher.addIndexListener(this);
            watcher.startWatching(); // This fires an open to the indexes
        } else {
            indexChanged(new File(staticRoot));
        }
        log.debug("Finished constructing SummaSearcherImpl");
    }

    public ResponseCollection search(Request request) throws RemoteException {
        log.trace("Search called");
        long fullStartTime = System.nanoTime();
        if (searchQueue.availablePermits() == 0) {
            throw new RemoteException(
                    "Could not perform search as the queue of requests exceed "
                    + searchQueue.getOverallPermits());
        }
        try {
            log.trace("Acquiring token from searchQueue");
            searchQueue.acquire();
        } catch (InterruptedException e) {
            throw new RemoteException(
                    "Interrupted while waiting for search queue access", e);
        }
        ResponseCollection responses = new ResponseCollection();
        try {
            try {
                if (freeSlots.getOverallPermits() == 0) {
                    // To kickstart in case of lockout due to open or crashes
                    log.trace("search: getting free slots");
                    freeSlots.setOverallPermits(searchNode.getFreeSlots());
                }
                if (log.isTraceEnabled() && freeSlots.availablePermits() <= 0) {
                    log.trace("No free slots. Entering wait-mode with timeout "
                              + searcherAvailabilityTimeout + " ms");
                }
                log.trace("Acquiring free searcher slot");
                if (!freeSlots.tryAcquire(1, searcherAvailabilityTimeout,
                                          TimeUnit.MILLISECONDS)) {
                    throw new RemoteException(
                            "Timeout in search: The limit of "
                            + searcherAvailabilityTimeout
                            + " milliseconds was exceeded");
                }
            } catch (InterruptedException e) {
                throw new RemoteException(
                        "Interrupted while waiting for free slot", e);
            }
            try {
                searchNode.search(request, responses);
                long responseTime = System.nanoTime() - fullStartTime;
                lastResponseTime = responseTime;
                //noinspection DuplicateStringLiteralInspection
                log.trace("Query performed in " + responseTime / 1000000.0
                          + " milliseconds");
                queryCount.incrementAndGet();
                totalResponseTime.addAndGet(responseTime);
                return responses;
            } finally {
                try {
                    // Checks for crashed SearchNodes
                    freeSlots.setOverallPermits(searchNode.getFreeSlots());
                } finally {
                    freeSlots.release();
                }
            }
        } finally {
            searchQueue.release();
            // TODO: Make this cleaner with no explicit dependency
            if (responses.getTransient().containsKey(DocumentSearcher.DOCIDS)) {
                Object o =
                        responses.getTransient().get(DocumentSearcher.DOCIDS);
                if (o instanceof DocIDCollector) {
                    ((DocIDCollector)o).close();
                }
            }
        }
    }

    /**
     * Shut down the searcher and free all resources. The searcher cannot be
     * used after close() has been called.
     * @throws RemoteException is an exception happened when closing the
     *                         underlying SearchNode.
     */
    public synchronized void close() throws RemoteException {
        if (watcher != null) {
            watcher.stopWatching();
        }
        searchNode.close();
        freeSlots.setOverallPermits(0);
//        freeSlots.setPermits(searchNode.getFreeSlots());
    }

    /**
     * Inform all underlying search nodes that they should open indexes at the
     * given location.
     * @param indexFolder where the index is located.
     */
    public synchronized void indexChanged(File indexFolder) {
        this.indexFolder = indexFolder;
        long startTime = System.currentTimeMillis();
        //noinspection DuplicateStringLiteralInspection
        log.debug("indexChanged(" + indexFolder + ") called");
        try {
            searchNode.open(indexFolder == null
                            ? null
                            : indexFolder.getAbsolutePath());
        } catch (RemoteException e) {
            // TODO: Consider making this a fatal
            log.error("Exception received while opening '" + indexFolder + "'",
                      e);
        }
        freeSlots.setOverallPermits(searchNode.getFreeSlots());
        log.debug("Finished indexChanged(" + indexFolder + ") in " +
                  (System.currentTimeMillis() - startTime) + " ms");
    }

    /* MBean implementations */

    /**
     * Reloads the index from the last known location. This method blocks until
     * the reload has been performed.
     * </p><p>
     * Note: The reloadIndex() does not check for new indexes under the root.
     * @throws RemoteException if the index could not be reloaded.
     * @see {@link #checkIndex()}.
     */
    public void reloadIndex() throws RemoteException {
        try {
            indexChanged(indexFolder);
        } catch (Exception e) {
            throw new RemoteException(
                    "Exception while forcing reload of index", e);
        }
    }

    /**
     * Forces a check for new or updated index. If a change is detected, the
     * searcher connectes to the index.
     * This method blocks until checking and potentially reloading has been
     * performed.
     * @throws RemoteException if the check or reload could not be performed.
     */
    public void checkIndex() throws RemoteException {
        try {
            watcher.updateAndReturnCurrentState();
        } catch (Exception e) {
            throw new RemoteException(
                    "Exception while checking for new index", e);
        }
    }

    /* Mutators */

    public int getSearchQueueMaxSize() {
        return searchQueue.getOverallPermits();
    }
    public void setSearchQueueMaxSize(int searchQueueMaxSize) {
        log.debug(String.format("setSearchQueueMaxSize(%s) called",
                                searchQueueMaxSize));
        searchQueue.setOverallPermits(searchQueueMaxSize);
    }

    public int getSearcherAvailabilityTimeout() {
        return searcherAvailabilityTimeout;
    }
    public void setSearcherAvailabilityTimeout(
            int searcherAvailabilityTimeout) {
        log.debug(String.format("setSearcherAvailabilityTimeout(%d) called",
                                searcherAvailabilityTimeout));
        this.searcherAvailabilityTimeout = searcherAvailabilityTimeout;
    }

    public String getIndexLocation() throws RemoteException {
        return indexFolder == null ? null : indexFolder.toString();
    }

    /* Statistics */

    public long getQueryCount() throws RemoteException {
        return queryCount.get();
    }
    public long getLastResponseTime() {
        return lastResponseTime;
    }
    public long getTotalResponseTime() {
        return totalResponseTime.get();
    }
    public double getAverageResponseTime() throws RemoteException {
        long qc = queryCount.get();
        return qc == 0 ? Double.NaN : (double)totalResponseTime.get() / qc;
    }
    public int getQueueLength() throws RemoteException {
        return searchQueue.getQueueLength();
    }
    public int getCurrentSearches() throws RemoteException {
        throw new UnsupportedOperationException("Not supported yet");
//        return searchActiveSemaphore.getQueueLength();
    }
    // Not really thread-safe, but statistically this should work ;-)
    public void clearStatistics() throws RemoteException {
        //noinspection AssignmentToNull
        lastResponseTime = -1;
        queryCount.set(0);
        totalResponseTime.set(0);
    }

}



