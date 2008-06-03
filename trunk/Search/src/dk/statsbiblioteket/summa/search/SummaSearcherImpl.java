/**
 * Created: te 30-05-2008 16:30:28
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.util.List;
import java.util.Arrays;

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

    /**
     * Extracts basic settings from the configuration.
     * @param conf the configuration for the searcher.
     */
    public SummaSearcherImpl(Configuration conf) {
    }

    // TODO: Pool of requests (futures)
}
