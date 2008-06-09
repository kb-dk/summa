/**
 * Created: te 30-05-2008 16:30:28
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.util.concurrent.Semaphore;
import java.util.List;
import java.util.ArrayList;
import java.rmi.RemoteException;
import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Convenience base class for SummaSearchers, taking care of basic setup.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class SummaSearcherImpl implements SummaSearcher, Configurable,
                                                   IndexListener {
    private static Log log = LogFactory.getLog(SummaSearcherImpl.class);

    private int numberOfSearchers = DEFAULT_NUMBER_OF_SEARCHERS;
    private int maxNumberOfConcurrentSearches =
            DEFAULT_NUMBER_OF_CONCURRENT_SEARCHES;
    private int searchQueueMaxSize = DEFAULT_SEARCH_QUEUE_MAX_SIZE;
    private int searcherAvailabilityTimeout =
            DEFAULT_SEARCHER_AVAILABILITY_TIMEOUT;

    private Semaphore searchQueueSemaphore;
    private Semaphore searchActiveSemaphore;
    protected List<SearchNodeWrapper> searchNodes;
    private IndexWatcher watcher;

    /**
     * Extracts basic settings from the configuration.
     * @param conf the configuration for the searcher.
     */
    public SummaSearcherImpl(Configuration conf) {
        log.trace("Constructor(Configuration) called");
        searchQueueMaxSize =
                conf.getInt(CONF_SEARCH_QUEUE_MAX_SIZE, searchQueueMaxSize);
        maxNumberOfConcurrentSearches =
                conf.getInt(CONF_NUMBER_OF_CONCURRENT_SEARCHES,
                            maxNumberOfConcurrentSearches);
        numberOfSearchers = conf.getInt(CONF_NUMBER_OF_SEARCHERS,
                                        numberOfSearchers);
        if (numberOfSearchers < 1) {
            throw new ConfigurationException(String.format(
                    "The number of searchers must be 1 or more. It was %s",
                    numberOfSearchers));
        }
        searcherAvailabilityTimeout =
                conf.getInt(CONF_SEARCHER_AVAILABILITY_TIMEOUT,
                            searcherAvailabilityTimeout);
        log.debug(String.format(
                "Constructing with %s=%d, %s=%d, %s=%d, %s=%d",
                CONF_SEARCH_QUEUE_MAX_SIZE, searchQueueMaxSize,
              CONF_NUMBER_OF_CONCURRENT_SEARCHES, maxNumberOfConcurrentSearches,
                CONF_NUMBER_OF_SEARCHERS, numberOfSearchers,
                CONF_SEARCHER_AVAILABILITY_TIMEOUT, searcherAvailabilityTimeout
                ));
        searchQueueSemaphore = new Semaphore(searchQueueMaxSize, true);
        searchActiveSemaphore = new Semaphore(maxNumberOfConcurrentSearches, 
                                              true);
        searchNodes = new ArrayList<SearchNodeWrapper>(numberOfSearchers);
        log.trace("Constructing search nodes");
        for (int i = 0 ; i < numberOfSearchers ; i++) {
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
                return usable.fullSearch(filter, query, startIndex, maxRecords,
                                         sortKey, reverseSort,
                                         resultFields, fallbacks);
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

    /**
     * Shut down the searcher and free all resources. The searcher cannot be
     * used after close() has been called.
     */
    public void close() {
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
    public void indexChanged(File indexFolder) {
        //noinspection DuplicateStringLiteralInspection
        log.debug("indexChanged(" + indexFolder + ") called");
        try {
            for (SearchNodeWrapper wrapper: searchNodes) {
                wrapper.open(indexFolder.getAbsolutePath());
            }
        } catch (IOException e) {
            // TODO: Consider to make this a fatal
            log.error("Exception received while opening '" + indexFolder + "'");
        }
    }
}
