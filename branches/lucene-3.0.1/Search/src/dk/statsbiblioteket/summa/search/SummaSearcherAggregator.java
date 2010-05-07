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

import dk.statsbiblioteket.summa.common.configuration.*;
import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Straight forward aggregator for remote SummaSearchers that splits a request
 * to all searchers and merges the results. No load-balancing.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummaSearcherAggregator implements SummaSearcher {
    private static Log log = LogFactory.getLog(SummaSearcherAggregator.class);

    /**
     * A list of configurations for connections to remote SummaSearchers.
     * Each configuration should contain all relevant {@link SearchClient}
     * properties and optionally {@link #CONF_SEARCHER_DESIGNATION} and
     * {@link }.
     * </p><p>
     * Mandatory.
     */
    public static final String CONF_SEARCHERS =
            "search.aggregator.searchers";

    /**
     * The designation for a remote SummaSearcher.
     * </p><p>
     * Optional. Default is
     * {@link dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer#CONF_RPC_TARGET}
     * for the given SummaSearcher.
     */
    public static final String CONF_SEARCHER_DESIGNATION =
            "search.aggregator.searcher.designation";

    /**
     * The maximum number of threads. A fixed list is created, so don't set
     * this too high.
     * </p><p>
     * Optional. Default is the number of remote searchers times 3.
     */
    public static final String CONF_SEARCHER_THREADS =
            "search.aggregator.threads";
    public static final int DEFAULT_SEARCHER_THREADS_FACTOR = 3;

    private List<Pair<String, SearchClient>> searchers;
    private ExecutorService executor;

    public SummaSearcherAggregator(Configuration conf) {
        List<Configuration> searcherConfs;
        try {
             searcherConfs = conf.getSubConfigurations(CONF_SEARCHERS);
        } catch (SubConfigurationsNotSupportedException e) {
            throw new ConfigurationException(
                    "Storage doesn't support sub configurations");
        } catch (NullPointerException e) {
            throw new ConfigurationException(
                    "Unable to extract sub-configurations for "
                    + CONF_SEARCHERS, e);
        }
        log.debug("Constructing SummaSearcherAggregator with "
                  + searcherConfs.size() + " remote SummaSearchers");
        searchers = new ArrayList<Pair<String, SearchClient>>(
                searcherConfs.size());
        for (Configuration searcherConf: searcherConfs) {
            SearchClient searcher = new SearchClient(searcherConf);
            String searcherName = searcherConf.getString(
                    CONF_SEARCHER_DESIGNATION, searcher.getVendorId());
            searchers.add(new Pair<String, SearchClient>(
                    searcherName, searcher));
            log.debug("Connected to " + searcherName + " at "
                      + searcher.getVendorId());
        }
        int threadCount = searchers.size() * DEFAULT_SEARCHER_THREADS_FACTOR;
        if (conf.valueExists(CONF_SEARCHER_THREADS)) {
            threadCount = conf.getInt(CONF_SEARCHER_THREADS);
        }
        //noinspection DuplicateStringLiteralInspection
        log.debug("Creating Executor with " + threadCount + " threads");
        executor = Executors.newFixedThreadPool(threadCount);

        log.debug("Finished connecting to " + searchers.size()
                  + ". Ready for use");
    }

    public ResponseCollection search(Request request) throws IOException {
        long startTime = System.nanoTime();
        if (log.isTraceEnabled()) {
            log.trace("Starting search for " + request);
        }
        if (searchers.size() == 0) {
            throw new IOException("No remote summaSearchers connected");
        }
        List<Pair<String, Future<ResponseCollection>>> searchFutures =
                new ArrayList<Pair<String, Future<ResponseCollection>>>(
                        searchers.size());
        for (Pair<String, SearchClient> searcher: searchers) {
            searchFutures.add(new Pair<String, Future<ResponseCollection>>(
                    searcher.getKey(),
                    executor.submit(new SearcherCallable(
                    searcher.getKey(), searcher.getValue(), request))));
        }
        log.trace("All searchers started, collecting and waiting");

        ResponseCollection response = null;
        for (Pair<String, Future<ResponseCollection>> searchFuture:
                searchFutures) {
            try {
                if (response == null) {
                    // TODO: Consider explicit timeout
                    response = searchFuture.getValue().get();
                } else {
                    response.addAll(searchFuture.getValue().get());
                }
            } catch (InterruptedException e) {
                throw new IOException(
                        "Interrupted while waiting for searcher result from "
                        + searchFuture.getKey(), e);
            } catch (ExecutionException e) {
                throw new IOException(
                        "ExecutionException while requesting search result "
                        + "from " + searchFuture.getKey(), e);
            } catch (Exception e) {
                throw new IOException(
                        "Exception while requesting search result from "
                        + searchFuture.getKey(), e);
            }
        }
        log.debug("Finished search in " + (System.nanoTime() - startTime)
                  + " ns");
        return response;
    }

    public void close() throws IOException {
        log.info("Close called for aggregator. Remote SummaSearchers will be "
                 + "disconnected but not closed");
        searchers.clear();
    }

    private static class SearcherCallable implements
                                          Callable<ResponseCollection> {
        private String designation;
        private SearchClient client;
        private Request request;

        private SearcherCallable(String designation, SearchClient client,
                                 Request request) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("Creating " + designation + " Future");
            this.designation = designation;
            this.client = client;
            this.request = request;
        }

        public ResponseCollection call() throws Exception {
            return client.search(request);
        }

        public String getDesignation() {
            return designation;
        }
    }
}

