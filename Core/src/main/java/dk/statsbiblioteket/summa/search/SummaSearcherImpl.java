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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.util.ChangingSemaphore;
import dk.statsbiblioteket.summa.common.util.MachineStats;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import dk.statsbiblioteket.summa.search.dummy.SearchNodeDummy;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.rmi.RemoteException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Convenience base class for SummaSearchers, taking care of basic setup.
 * The default Impl is oriented towards watching for changes to the file system,
 * normally initiated by index manipulators.
 * Relevant properties from {@link SearchNodeFactory},
 * {@link dk.statsbiblioteket.summa.search.api.SummaSearcher},
 * {@link IndexWatcher}, {@link SearchNodeLoadBalancer} and
 * {@link LuceneIndexUtils} needs to be specified in the configuration.
 * </p><p>
 * It is recommended to include setup of an IndexDescriptor in the configuration
 * for the SummaSearcher as it will be copied to the configuration for the
 * underlying SearchNodes. See IndexDescriptor#CONF_DESCRIPTOR.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummaSearcherImpl implements SummaSearcherMBean, SummaSearcher, IndexListener {
    private static Log log = LogFactory.getLog(SummaSearcherImpl.class);

    /**
     * If a new search is requested and there is no free slots, the search
     * is queued. If the queue reaches its max-size, the request is not queued
     * and an exception is thrown.
     * </p><p>
     * This is optional. Default is 50.
     */
    public static final String CONF_SEARCH_QUEUE_MAX_SIZE = "summa.search.searchqueue.maxsize";
    /** Default valeu for {@link #CONF_SEARCH_QUEUE_MAX_SIZE}. */
    public static final int DEFAULT_SEARCH_QUEUE_MAX_SIZE = 50;

    /**
     * If no searchers are ready upon search, wait up to this number of
     * milliseconds for a searcher to become ready. If no searchers are ready
     * at that time, an exception will be thrown.
     */
    public static final String CONF_SEARCHER_AVAILABILITY_TIMEOUT = "summa.search.searcheravailability.timeout";
    /** Default value for {@link #CONF_SEARCHER_AVAILABILITY_TIMEOUT}. */
    public static final int DEFAULT_SEARCHER_AVAILABILITY_TIMEOUT = 5 * 60000;

    /**
     * If specified, watching for index changes will be disabled and an open
     * will be called with the stated root when the SummaSearcher is created.
     * </p><p>
     * Optional. Default is null (disabled).
     */
    public static final String CONF_STATIC_ROOT = "summa.search.staticroot";
    /** Default value for {@link #CONF_STATIC_ROOT}. */
    public static final String DEFAULT_STATIC_ROOT = null;

    /**
     * If true, a local index is assumed. This will activate IndexWatcher if
     * a static root is not defined.
     * </p><p>
     * Optional. Default is true;
     */
    public static final String CONF_USE_LOCAL_INDEX = "summa.search.uselocalindex";
    public static final boolean DEFAULT_USE_LOCAL_INDEX = true;

    /**
     * If true, empty searches are processed. If false, the searcher returns immediately.
     */
    public static final String CONF_ALLOW_EMPTY_SEARCH = "summa.search.allowempty";
    public static final boolean DEFAULT_ALLOW_EMPTY_SEARCH = false;

    private int searcherAvailabilityTimeout = DEFAULT_SEARCHER_AVAILABILITY_TIMEOUT;

    private ChangingSemaphore searchQueue;
    private ChangingSemaphore freeSlots = new ChangingSemaphore(0);
    private SearchNode searchNode;
    private IndexWatcher watcher;

    private File indexFolder;
    private long lastResponseTime = -1;
    private AtomicLong queryCount = new AtomicLong(0);
    private AtomicLong totalResponseTime = new AtomicLong(0);
    private AtomicInteger concurrentSearches = new AtomicInteger(0);
    private final boolean emptySearchAllowed;
    private final MachineStats machineStats;

    private int maxConcurrent = 0; // Non-authoritative, used for loose inspection only
    private final Profiler profiler = new Profiler(Integer.MAX_VALUE, 100);

    /**
     * Extracts basic settings from the configuration and constructs the
     * underlying tree of SearchNodes.
     * <p></p>
     * The searcher will instantiate a {@link SearchNode} based on the
     * property {@link SearchNodeFactory#CONF_NODE_CLASS}. If this property
     * is not defined the searcher will fall back to using a
     * {@link dk.statsbiblioteket.summa.search.dummy.SearchNodeDummy}
     * @param conf the configuration for the searcher.
     * @throws RemoteException if the underlying SearchNode could not be constructed.
     */
    public SummaSearcherImpl(Configuration conf) throws RemoteException {
        this(conf, SearchNodeFactory.createSearchNode(conf, SearchNodeDummy.class));
    }
    /**
     * Extracts basic settings from the configuration and uses the given searchNode directly.
     * @param conf the configuration for the searcher.
     */
    public SummaSearcherImpl(Configuration conf, SearchNode searchNode) {
        log.info("Constructing SummaSearcherImpl");
        int searchQueueMaxSize = conf.getInt(CONF_SEARCH_QUEUE_MAX_SIZE, DEFAULT_SEARCH_QUEUE_MAX_SIZE);
        searcherAvailabilityTimeout = conf.getInt(CONF_SEARCHER_AVAILABILITY_TIMEOUT, searcherAvailabilityTimeout);
        log.trace("searcherAvailabilityTimeout=" + searcherAvailabilityTimeout + " ms");

        searchQueue = new ChangingSemaphore(searchQueueMaxSize, true);
        log.trace("Constructing search node");
        this.searchNode = searchNode;
        emptySearchAllowed = conf.getBoolean(CONF_ALLOW_EMPTY_SEARCH, DEFAULT_ALLOW_EMPTY_SEARCH);

        // Ready for open
        if (conf.getBoolean(CONF_USE_LOCAL_INDEX, DEFAULT_USE_LOCAL_INDEX)) {
            String staticRoot = conf.getString(CONF_STATIC_ROOT, DEFAULT_STATIC_ROOT);
            if (staticRoot == null || "".equals(staticRoot)) {
                log.debug("Starting index watcher");
                watcher = new IndexWatcher(conf);
                watcher.addIndexListener(this);
                watcher.startWatching(); // This fires an open to the indexes
            } else {
                log.debug("Using static index '" + staticRoot + "'");
                indexChanged(Resolver.getPersistentFile(new File(staticRoot)));
            }
        } else {
            log.debug("Not using local index. No index watcher, no open");
        }
        machineStats = conf.getBoolean(MachineStats.CONF_ACTIVE, true) ? new MachineStats(conf) : null;
        log.debug("Finished constructing SummaSearcherImpl");
    }

    /**
     * Make a search request. And return the appropriate response collection.
     *
     * @param request Contains SearchNode-specific request-data.
     * @return Response collection based on the search request.
     * @throws RemoteException if error occur connection to a remote searcher.
     */
    @Override
    public ResponseCollection search(Request request) throws RemoteException {
        ResponseCollection responses = new ResponseCollection();
        if (machineStats != null) {
            machineStats.ping();
        }
        if (!emptySearchAllowed && request.isEmpty()) {
            log.debug("search: No content in request and empty search not allowed. No search is performed");
            responses.addTiming("summasearcher.emptysearch", 0);
        }
        if (log.isTraceEnabled()) {
            log.trace("Search called with parameters\n" + request.toString());
        }
        final long fullStartTime = System.nanoTime();
        final String originalRequest = request.toString();
        if (searchQueue.availablePermits() == 0) {
            throw new RemoteException(
                    "Could not perform search as the queue of requests exceed " + searchQueue.getOverallPermits());
        }
        try {
            log.trace("Acquiring token from searchQueue");
            searchQueue.acquire();
        } catch (InterruptedException e) {
            throw new RemoteException("Interrupted while waiting for search queue access", e);
        }
        boolean success = false;
        try {
            try {
                if (freeSlots.getOverallPermits() == 0) {
                    // To kickstart in case of lockout due to open or crashes
                    log.trace("search: getting free slots");
                    int fs = searchNode.getFreeSlots();
                    if (log.isDebugEnabled()) {
                        log.debug("Setting free slots to " + fs + ", acquired from " + searchNode);
                    }
                    freeSlots.setOverallPermits(fs);
                }
                if (log.isTraceEnabled() && freeSlots.availablePermits() <= 0) {
                    log.trace("No free slots. Entering wait-mode with timeout " + searcherAvailabilityTimeout + " ms");
                }
                log.trace("Acquiring free searcher slot");
//                System.out.println("Acquiring with overall " + freeSlots.getOverallPermits() + " with " + freeSlots.availablePermits() + " available: " + concurrentSearches.get());
                if (!freeSlots.tryAcquire(1, searcherAvailabilityTimeout, TimeUnit.MILLISECONDS)) {
                    throw new RemoteException(
                            "Timeout in search: The limit of " + searcherAvailabilityTimeout + " ms was exceeded while "
                            + "waiting for a free slot");
                }
            } catch (InterruptedException e) {
                throw new RemoteException("Interrupted while waiting for free slot", e);
            }
            try {
                int concurrent = concurrentSearches.addAndGet(1);
                if (maxConcurrent < concurrent) {
                    maxConcurrent = concurrent;
                }
                log.debug("Concurrent searches: " + concurrent);
                searchNode.search(request, responses);
                long responseTime = System.nanoTime() - fullStartTime;
                lastResponseTime = responseTime;
                //noinspection DuplicateStringLiteralInspection
                log.trace("Query performed in " + responseTime / 1000000.0 + " milliseconds");
                queryCount.incrementAndGet();
                totalResponseTime.addAndGet(responseTime);
                success = true;
                return responses;
            } finally {
                concurrentSearches.addAndGet(-1);
                try {
                    // Checks for crashed SearchNodes
                    freeSlots.setOverallPermits(searchNode.getFreeSlots());
                } finally {
                    freeSlots.release();
                }
            }
        } finally {
            searchQueue.release();
            profiler.beat();
            // TODO: Make this cleaner with no explicit dependency
            if (responses.isEmpty()) {
                queries.info(this.getClass().getSimpleName() + " finished "
                             + (success ? "successfully" : "unsuccessfully (see logs for errors)")
                             + " without responses in " + (System.nanoTime() - fullStartTime) / 1000000
                             + "ms. Request was " + originalRequest + ". " + getStats());
            } else {
                if (responses.getTransient() != null && responses.getTransient().containsKey(DocumentSearcher.DOCIDS)) {
                    Object o = responses.getTransient().get(DocumentSearcher.DOCIDS);
                    if (o instanceof DocIDCollector) {
                        ((DocIDCollector)o).close();
                    }
                }
                if (queries.isInfoEnabled()) {
                    String hits = "N/A";
                    for (Response response: responses) {
                        if (response instanceof DocumentResponse) {  // If it's there, we might as well get some stats
                            hits = Long.toString(((DocumentResponse)response).getHitCount());
                            break;
                        }
                    }
                    queries.info(this.getClass().getSimpleName() + " finished "
                                 + (success ? "successfully" : "unsuccessfully (see logs for errors)")
                                 + " in " + (System.nanoTime() - fullStartTime) / 1000000
                                 + "ms with " + hits + " hits. " + "Request was " + originalRequest
                                 + " with Timing(" + responses.getTiming() + "). " + getStats());
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
    @Override
    public synchronized void close() throws RemoteException {
        if (watcher != null) {
            watcher.stopWatching();
        }
        searchNode.close();
        freeSlots.setOverallPermits(0);
        //freeSlots.setPermits(searchNode.getFreeSlots());
    }

    /**
     * Inform all underlying search nodes that they should open indexes at the
     * given location.
     * @param indexFolder where the index is located.
     */
    @Override
    public synchronized void indexChanged(File indexFolder) {
        this.indexFolder = indexFolder;
        long startTime = System.currentTimeMillis();
        //noinspection DuplicateStringLiteralInspection
        log.info("indexChanged(" + indexFolder + ") called");
        try {
            searchNode.open(indexFolder == null ? null : indexFolder.getAbsolutePath());
        } catch (RemoteException e) {
            // TODO: Consider making this a fatal
            log.error("Exception received while opening '" + indexFolder + "'", e);
        }
        freeSlots.setOverallPermits(searchNode.getFreeSlots());
        log.info("Finished indexChanged(" + indexFolder + ") in " + (System.currentTimeMillis() - startTime) + " ms");
    }

    /* MBean implementations */

    /**
     * Reloads the index from the last known location. This method blocks until
     * the reload has been performed.
     * </p><p>
     * Note: The reloadIndex() does not check for new indexes under the root.
     * @throws RemoteException if the index could not be reloaded.
     * @see #checkIndex()
     */
    public void reloadIndex() throws RemoteException {
        try {
            indexChanged(indexFolder);
        } catch (Exception e) {
            throw new RemoteException("Exception while forcing reload of index", e);
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
            throw new RemoteException("Exception while checking for new index", e);
        }
    }

    /********************** Mutators section *****************/

    /**
     * Set the maximum size of search queries. Accessor
     * {@link #getSearcherAvailabilityTimeout()}.
     *
     * @param searchQueueMaxSize Maximum size of search queries.
     */
    @Override
    public void setSearchQueueMaxSize(int searchQueueMaxSize) {
        log.debug(String.format(Locale.ROOT, "setSearchQueueMaxSize(%s) called", searchQueueMaxSize));
        searchQueue.setOverallPermits(searchQueueMaxSize);
    }

    /**
     * Sets this searcher availability timeout. Accessor {@link #getSearcherAvailabilityTimeout()}.
     *
     * @param searcherAvailabilityTimeout The availability timeout for this searcher.
     */
    @Override
    public void setSearcherAvailabilityTimeout(
            int searcherAvailabilityTimeout) {
        log.debug(String.format(Locale.ROOT, "setSearcherAvailabilityTimeout(%d) called", searcherAvailabilityTimeout));
        this.searcherAvailabilityTimeout = searcherAvailabilityTimeout;
    }

    /************** Accessors section ****************/

    /**
     * Return the availability timeout for this searhcer. Mutator
     * {@link #setSearcherAvailabilityTimeout(int)}.
     *
     * @return This searcher availability timeout.
     */
    @Override
    public int getSearcherAvailabilityTimeout() {
        return searcherAvailabilityTimeout;
    }

    /**
     * Get the maximum size of search queries for this searcher. \
     * Mutator {@link #setSearchQueueMaxSize(int)}.
     *
     * @return The maximum size of search queries for this searcer.
     */
    @Override
    public int getSearchQueueMaxSize() {
        return searchQueue.getOverallPermits();
    }

    /**
     * Return the index location for this searcher.
     * FIXME why does this method throw RemoteException?
     *
     * @return The index location.
     * @throws RemoteException Should not happen.
     */
    public String getIndexLocation() throws RemoteException {
        return indexFolder == null ? null : indexFolder.toString();
    }

    /*************** Statistics section ***************/

    /**
     * Return the query count from searcher.
     * @return The query count.
     * @throws RemoteException if error occur while fetching the Query count remotely.
     */
    @Override
    public long getQueryCount() throws RemoteException {
        return queryCount.get();
    }

    /**
     * Return the latest response time from searcher.
     * @return Latest response time.
     */
    @Override
    public long getLastResponseTime() {
        return lastResponseTime;
    }

    /**
     * Return the total repsonse time from searcher.
     * @return Total response time.
     */
    @Override
    public long getTotalResponseTime() {
        return totalResponseTime.get();
    }

    /**
     * Return the average response time from searcher.
     * @return  Average response time.
     * @throws RemoteException if error occur while fetching query count remotely.
     */
    @Override
    public double getAverageResponseTime() throws RemoteException {
        long qc = queryCount.get();
        return qc == 0 ? Double.NaN : (double)totalResponseTime.get() / qc;
    }

    /**
     * Return the length of query for this searcher.
     * @return Query length.
     * @throws RemoteException if error occur while fetching query length remotely.
     */
    @Override
    public int getQueueLength() throws RemoteException {
        return searchQueue.getQueueLength();
    }

    /**
     * Return the number of current searchers.
     * TODO implement
     *
     * @return Nothing yet, only throws a {@link UnsupportedOperationException} if called.
     * @throws RemoteException Throws a {@link UnsupportedOperationException} if called.
     */
    @Override
    public int getCurrentSearches() throws RemoteException {
        throw new UnsupportedOperationException("Not supported yet");
        //return searchActiveSemaphore.getQueueLength();
    }

    //

    private String getStats() {
        return "Stats(#queries=" + profiler.getBeats()
               + ", q/s(last " + profiler.getBpsSpan() + ")=" + profiler.getBps(true);
    }

    /**
     * Clear the statistic numbers from this searcher. This means set query count to zero, total
     * response to zero and last response time to -1.
     * FIXME Not really thread-safe, but statistically this should work ;-)
     *
     * @throws RemoteException if error occur while fetching query count remotely.
     */
    @Override
    public void clearStatistics() throws RemoteException {
        //noinspection AssignmentToNull
        lastResponseTime = -1;
        queryCount.set(0);
        totalResponseTime.set(0);
    }

    /**
     * @return the current number of active searches.
     */
    public int getConcurrentSearches() {
        return concurrentSearches.get();
    }

    /**
     * @return the historically maximum for concurrent searches. Not an authoritative number!
     */
    public int getMaxConcurrent() {
        return maxConcurrent;
    }

    /**
     * @return the inner SearchNode.
     */
    public SearchNode getSearchNode() {
        return searchNode;
    }

    @Override
    public String toString() {
        return "SummaSearcherImpl(searcherAvailabilityTimeout=" + searcherAvailabilityTimeout +
               ", searchQueue.size=" + searchQueue.getQueueLength() + ", freeSlots=" + freeSlots.getQueueLength() +
               ", searchNode=" + searchNode + ", ..., indexFolder=" + indexFolder +
               ", lastResponseTime=" + lastResponseTime + ", stats=" + getStats() +
               ", concurrentSearches=" + concurrentSearches + ", emptySearchAllowed=" + emptySearchAllowed +
               ", maxConcurrent=" + maxConcurrent + ")";
    }
}
