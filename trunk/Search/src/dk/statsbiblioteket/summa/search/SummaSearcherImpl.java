/**
 * Created: te 30-05-2008 16:30:28
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.util.concurrent.Semaphore;
import java.rmi.RemoteException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Convenience base class for SummaSearchers, taking care of basic setup.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class SummaSearcherImpl implements SummaSearcher, Configurable {
    private static Log log = LogFactory.getLog(SummaSearcherImpl.class);

    private int indexCheckInterval = DEFAULT_CHECK_INTERVAL;
    private int indexMinRetention = DEFAULT_MIN_RETENTION;
    private int numberOfSearchers = DEFAULT_NUMBER_OF_SEARCHERS;
    private int maxNumberOfConcurrentSearches =
            DEFAULT_NUMBER_OF_CONCURRENT_SEARCHES;
    private int searchQueueMaxSize = DEFAULT_SEARCH_QUEUE_MAX_SIZE;

    private Semaphore searchQueueSemaphore;
    private Semaphore searchActiveSemaphore;

    /**
     * Extracts basic settings from the configuration.
     * @param conf the configuration for the searcher.
     */
    public SummaSearcherImpl(Configuration conf) {
        searchQueueMaxSize =
                conf.getInt(CONF_SEARCH_QUEUE_MAX_SIZE, searchQueueMaxSize);
        searchQueueSemaphore = new Semaphore(searchQueueMaxSize, true);
        maxNumberOfConcurrentSearches =
                conf.getInt(CONF_NUMBER_OF_CONCURRENT_SEARCHES,
                            maxNumberOfConcurrentSearches);
        searchActiveSemaphore =
                new Semaphore(maxNumberOfConcurrentSearches, true);
    }

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
                // TODO: Find the one with the least load, which is ready
                return null;
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
}
