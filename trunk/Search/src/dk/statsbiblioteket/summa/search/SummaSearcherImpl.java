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

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.rmi.RemoteException;
import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Convenience base class for SummaSearchers, taking care of basic setup.
 * Relevant properties from {@link SummaSearcher}, {@link IndexWatcher},
 * {@link SearchNodeWrapper} and {@link LuceneIndexUtils} needs to be specified.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class SummaSearcherImpl implements SummaSearcherMBean,
                                                   Configurable,
                                                   IndexListener {
    private static Log log = LogFactory.getLog(SummaSearcherImpl.class);

    private int searchers = DEFAULT_NUMBER_OF_SEARCHERS;

    private int maxConcurrentSearches =
            DEFAULT_NUMBER_OF_CONCURRENT_SEARCHES;
    private int searchQueueMaxSize = DEFAULT_SEARCH_QUEUE_MAX_SIZE;
    private int searcherAvailabilityTimeout =
            DEFAULT_SEARCHER_AVAILABILITY_TIMEOUT;
    private String[] resultFields = DEFAULT_RESULT_FIELDS;
    private String[] fallbackValues = DEFAULT_FALLBACK_VALUES;
    private String sortKey = DEFAULT_DEFAULT_SORTKEY;
    private long maxRecords = DEFAULT_MAX_NUMBER_OF_RECORDS;
    private String warmupData = DEFAULT_WARMUP_DATA;
    private int warmupMaxTime = DEFAULT_WARMUP_MAXTIME;


    private Semaphore searchQueueSemaphore;
    private Semaphore searchActiveSemaphore;
    protected List<SearchNodeWrapper> searchNodes;
    private IndexWatcher watcher;
    // TODO: Are default resultFields extracted properly? 

    private File indexFolder;
    private String lastQuery = null;
    private long lastResponseTime = -1;
    private AtomicLong queryCount = new AtomicLong(0);
    private AtomicLong totalResponseTime = new AtomicLong(0);

    /**
     * Extracts basic settings from the configuration.
     * @param conf the configuration for the searcher.
     */
    public SummaSearcherImpl(Configuration conf) {
        log.trace("Constructor(Configuration) called");
        searchQueueMaxSize =
                conf.getInt(CONF_SEARCH_QUEUE_MAX_SIZE, searchQueueMaxSize);
        maxConcurrentSearches =
                conf.getInt(CONF_NUMBER_OF_CONCURRENT_SEARCHES,
                            maxConcurrentSearches);
        searchers = conf.getInt(CONF_NUMBER_OF_SEARCHERS,
                                searchers);
        if (searchers < 1) {
            throw new ConfigurationException(String.format(
                    "The number of searchers must be 1 or more. It was %s",
                    searchers));
        }
        searcherAvailabilityTimeout =
                conf.getInt(CONF_SEARCHER_AVAILABILITY_TIMEOUT,
                            searcherAvailabilityTimeout);

        resultFields = conf.getStrings(CONF_RESULT_FIELDS, resultFields);
        fallbackValues = conf.getStrings(CONF_FALLBACK_VALUES, fallbackValues);
        sortKey = conf.getString(CONF_DEFAULT_SORTKEY, sortKey);
        if (fallbackValues != null
            && resultFields.length != fallbackValues.length) {
            //noinspection DuplicateStringLiteralInspection
            throw new IllegalArgumentException(String.format(
                    "The number of fallback-values(%s) was not equal to the "
                    + "number of result-fields(%s)", fallbackValues.length,
                                                     resultFields.length));
        }
        maxRecords = conf.getLong(CONF_MAX_RECORDS, maxRecords);
        if (maxRecords <= 0) {
            log.warn(String.format(
                    "The property %s must be >0. It was %s. Resetting to "
                    + "default %s",
                    CONF_MAX_RECORDS, maxRecords,
                    DEFAULT_MAX_NUMBER_OF_RECORDS == Long.MAX_VALUE ?
                    "Long.MAX_VALUE" :
                    DEFAULT_MAX_NUMBER_OF_RECORDS));
            maxRecords = DEFAULT_MAX_NUMBER_OF_RECORDS;
        }
        warmupData = conf.getString(CONF_WARMUP_DATA, warmupData);
        warmupMaxTime = conf.getInt(CONF_WARMUP_MAXTIME, warmupMaxTime);
        log.debug("Warmup-data is '" + warmupData + "' with max warmup-time "
                  + (warmupMaxTime == Integer.MAX_VALUE ?
                     "Integer.MAX_VALUE" : warmupMaxTime) + " ms");

        log.debug(String.format(
                "Constructing with %s=%d, %s=%d, %s=%d, %s=%d, %s=%s, %s=%s, "
                + "%s=%d, %s=%s, %s=%d",
                CONF_SEARCH_QUEUE_MAX_SIZE, searchQueueMaxSize,
              CONF_NUMBER_OF_CONCURRENT_SEARCHES, maxConcurrentSearches,
                CONF_NUMBER_OF_SEARCHERS, searchers,
                CONF_SEARCHER_AVAILABILITY_TIMEOUT, searcherAvailabilityTimeout,
                CONF_RESULT_FIELDS, Arrays.toString(resultFields),
                CONF_DEFAULT_SORTKEY, sortKey,
                CONF_MAX_RECORDS, maxRecords,
                CONF_WARMUP_DATA, warmupData,
                CONF_WARMUP_MAXTIME, warmupMaxTime
                ));
        searchQueueSemaphore = new Semaphore(searchQueueMaxSize, true);
        searchActiveSemaphore = new Semaphore(maxConcurrentSearches,
                                              true);
        searchNodes = new ArrayList<SearchNodeWrapper>(searchers);
        log.trace("Constructing search nodes");
        for (int i = 0 ; i < searchers; i++) {
            try {
                searchNodes.add(constructSearchNode(conf));
            } catch (IOException e) {
                throw new ConfigurationException(String.format(
                        "Unable to construch searcher %d", i)
                );
            }
        }

        // Ready for open
        watcher = new IndexWatcher(conf);
        watcher.addIndexListener(this);
        watcher.startWatching(); // This fires an open to the indexes
    }

    /**
     * Construct a search node based on the given configuration. Note that nodes
     * does not open indexes before {@link SearchNode#open(String)} is called.
     * </p><p>
     * The search node must be wrapped in a SearchNodeWrapper and will be
     * search-engine-specific (e.g. a Lucene searcher).
     * @param conf the setup for the node.
     * @return a node ready to open an index.
     * @throws IOException if there was an error constructing the node.
     */
    public abstract SearchNodeWrapper constructSearchNode(Configuration conf)
                                                             throws IOException;

    public String fullSearch(String filter, String query, long startIndex,
                             long maxRecords, String sortKey,
                             boolean reverseSort, String[] resultFields,
                             String[] fallbacks) throws RemoteException {
        long fullStartTime = System.nanoTime();
        if (searchQueueSemaphore.availablePermits() == 0) {
            throw new RemoteException(
                    "Could not perform search as the queue of requests exceed "
                    + searchQueueMaxSize);
        }
        try {
            searchQueueSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new RemoteException(
                    "Interrupted while waiting for search queue access", e);
        }
        try {
            try {
                searchActiveSemaphore.acquire();
                int minLoad = Integer.MAX_VALUE;
                SearchNodeWrapper usable = null;
                for (SearchNodeWrapper wrapper : searchNodes) {
                    if (wrapper.isReady()) {
                        if (wrapper.getActive() < minLoad) {
                            minLoad = wrapper.getActive();
                            usable = wrapper;
                        }
                    }
                }
                if (usable == null) {
                    log.debug("None of the " + searchNodes.size()
                              + " search nodes were ready. Waiting for "
                              + "readyness.");
                    long startTime = System.currentTimeMillis();
                    long endTime = startTime + searcherAvailabilityTimeout;
                    out:
                    while (System.currentTimeMillis() < endTime) {
                        for (SearchNodeWrapper searchNode : searchNodes) {
                            if (searchNode.isReady()) {
                                usable = searchNode;
                                break out;
                            }
                        }
                        Thread.sleep(10);
                    }
                    if (usable == null) {
                        throw new RemoteException(
                                "Waited " + (endTime - startTime) + " ms for an"
                                + " available searcher out of "
                                + searchNodes.size() + ", but got none");
                    }
                }
                // TODO: Fix the race-condition on open vs. search
                String result =
                        usable.fullSearch(filter, query, startIndex, maxRecords,
                                          sortKey, reverseSort,
                                          resultFields, fallbacks);
                lastQuery = query;
                long responseTime = System.nanoTime() - fullStartTime;
                lastResponseTime = responseTime;
                log.trace("Query '" + query + "' performed in "
                          + responseTime + " nanoseconds");
                queryCount.incrementAndGet();
                totalResponseTime.addAndGet(responseTime);
                return result;
            } catch (InterruptedException e) {
                throw new RemoteException("Interrupted while waiting for active"
                                          + " search queue access", e);
            } finally {
                searchActiveSemaphore.release();
            }
        } finally {
            searchQueueSemaphore.release();
        }
    }

    public String simpleSearch(String query, long startIndex, long maxRecords) 
                                                        throws RemoteException {
        return fullSearch(null, query, startIndex, maxRecords,
                          null, false, null, null);
    }

    /**
     * Shut down the searcher and free all resources. The searcher cannot be
     * used after close() has been called.
     */
    public synchronized void close() {
        // TODO: Free as many resources as possible, even on exception
        watcher.stopWatching();
        for (SearchNodeWrapper wrapper: searchNodes) {
            wrapper.close();
        }
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
            for (SearchNodeWrapper wrapper: searchNodes) {
                wrapper.open(indexFolder == null
                             ? null
                             : indexFolder.getAbsolutePath());
            }
        } catch (IOException e) {
            // TODO: Consider to make this a fatal
            log.error("Exception received while opening '" + indexFolder + "'",
                      e);
        }
        log.debug("Finished indexChanged(" + indexFolder + ") in " +
                  (System.currentTimeMillis() - startTime) + " ms");
    }

    /* MBean implementations */

    public void reloadIndex() throws RemoteException {
        try {
            indexChanged(indexFolder);
        } catch (Exception e) {
            throw new RemoteException(
                    "Exception while forcing reload of index", e);
        }
    }

    public synchronized long performWarmup() throws RemoteException {
        log.trace("Performing explicitely requested warmup");
        long startTime = System.nanoTime();
        for (SearchNodeWrapper wrapper: searchNodes) {
            wrapper.warmup();
        }
        return System.nanoTime() - startTime;
    }

    public int getSearchers() {
        return searchers;
    }
    public void setSearchers(int numberOfSearchers) {
        // TODO: Implement change of searchers
        log.warn("setSearchers(" + numberOfSearchers + ") is not implemented"
                 + " properly yet");
        searchers = numberOfSearchers;
    }

    public int getMaxConcurrentSearches() {
        return maxConcurrentSearches;
    }
    public void setMaxConcurrentSearches(int maxConcurrentSearches) {
        log.debug(String.format("setMaxConcurrentSearches(%d) called",
                                maxConcurrentSearches));
        this.maxConcurrentSearches = maxConcurrentSearches;
    }

    public int getSearchQueueMaxSize() {
        return searchQueueMaxSize;
    }
    public void setSearchQueueMaxSize(int searchQueueMaxSize) {
        log.debug(String.format("setSearchQueueMaxSize(%s) called",
                                searchQueueMaxSize));
        this.searchQueueMaxSize = searchQueueMaxSize;
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

    public String getSortKey() {
        return sortKey;
    }
    public void setSortKey(String sortKey) {
        log.debug(String.format("setSortKey(%s) called", sortKey));
        this.sortKey = sortKey;
    }

    public String[] getResultFields() {
        return resultFields;
    }
    public void setResultFields(String[] resultFields) {
        log.debug(String.format("setResultFields(%s) called",
                                Arrays.toString(resultFields)));
        this.resultFields = resultFields;
    }

    public String[] getFallbackValues() {
        return fallbackValues;
    }
    public void setFallbackValues(String[] fallbackValues) {
        log.debug(String.format("setFallbackValues(%s) called",
                                Arrays.toString(fallbackValues)));
    }

    public long getMaxRecords() {
        return maxRecords;
    }
    public void setMaxRecords(long maxRecords) {
        log.debug(String.format("setMaxRecords(%d) called", maxRecords));
        this.maxRecords = maxRecords;
    }

    public String getWarmupData() {
        return warmupData;
    }
    public void setWarmupData(String warmupData) {
        log.debug(String.format("setWarmupData(%s) called", warmupData));
        this.warmupData = warmupData;
    }

    public int getWarmupMaxTime() {
        return warmupMaxTime;
    }
    public void setWarmupMaxTime(int warmupMaxTime) {
        log.debug(String.format("setWarmupMaxTime(%d ms) called",
                                warmupMaxTime));
        this.warmupMaxTime = warmupMaxTime;
    }

    public String getIndexLocation() throws RemoteException {
        return indexFolder.toString();
    }

    /* Statistics */

    public String getLastQuery() {
        return lastQuery;
    }
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
        return searchQueueSemaphore.getQueueLength();
    }
    public int getCurrentSearches() throws RemoteException {
        return searchActiveSemaphore.getQueueLength();
    }
    // Not really thread-safe, but statistically this should work ;-)
    public void clearStatistics() throws RemoteException {
        //noinspection AssignmentToNull
        lastQuery = null;
        lastResponseTime = -1;
        queryCount.set(0);
        totalResponseTime.set(0);
    }
}
