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
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.summa.search.api.*;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
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
// TODO: Rewrite requests to adjust number of records to return for paging
public class SummaSearcherAggregator implements SummaSearcher {
    private static Log log = LogFactory.getLog(SummaSearcherAggregator.class);

    /**
     * A list of configurations for connections to remote SummaSearchers.
     * Each configuration should contain all relevant {@link SearchClient}
     * properties and optionally {@link #CONF_SEARCHER_DESIGNATION} and
     * {@link #CONF_SEARCHER_THREADS}.
     * </p><p>
     * Mandatory.
     */
    public static final String CONF_SEARCHERS = "search.aggregator.searchers";

    /**
     * The designation for a remote SummaSearcher.
     * </p><p>
     * Optional. Default is
     * {@link dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer#CONF_RPC_TARGET}
     * for the given SummaSearcher.
     */
    public static final String CONF_SEARCHER_DESIGNATION = "search.aggregator.searcher.designation";

    /**
     * The maximum number of threads. A fixed list is created, so don't set
     * this too high.
     * </p><p>
     * Optional. Default is the number of remote searchers times 3.
     */
    public static final String CONF_SEARCHER_THREADS = "search.aggregator.threads";
    public static final int DEFAULT_SEARCHER_THREADS_FACTOR = 3;

    /**
     * A list of active searchers, referenced by their designation.
     * </p><p>
     * Optional. Default is all the searchers in {@link #CONF_SEARCHERS}.
     */
    public static final String CONF_ACTIVE = "search.aggregator.active";
    public static final String SEARCH_ACTIVE = CONF_ACTIVE;

    private List<Pair<String, SearchClient>> searchers;
    private ExecutorService executor;
    private final List<String> defaultSearchers;

    public SummaSearcherAggregator(Configuration conf) {
        preConstruction(conf);
        List<Configuration> searcherConfs;
        try {
             searcherConfs = conf.getSubConfigurations(CONF_SEARCHERS);
        } catch (SubConfigurationsNotSupportedException e) {
            throw new ConfigurationException("Storage doesn't support sub configurations");
        } catch (NullPointerException e) {
            throw new ConfigurationException("Unable to extract sub-configurations for " + CONF_SEARCHERS, e);
        }
        log.debug("Constructing SummaSearcherAggregator with " + searcherConfs.size() + " remote SummaSearchers");
        searchers = new ArrayList<Pair<String, SearchClient>>(searcherConfs.size());
        List<String> created = new ArrayList<String>(searcherConfs.size());
        for (Configuration searcherConf: searcherConfs) {
            SearchClient searcher = createClient(searcherConf);
            String searcherName = searcherConf.getString(CONF_SEARCHER_DESIGNATION, searcher.getVendorId());
            created.add(searcherName);
            searchers.add(new Pair<String, SearchClient>(searcherName, searcher));
            log.debug("Connected to " + searcherName + " at " + searcher.getVendorId());
        }
        this.defaultSearchers = conf.getStrings(CONF_ACTIVE, created);
        int threadCount = searchers.size() * DEFAULT_SEARCHER_THREADS_FACTOR;
        if (conf.valueExists(CONF_SEARCHER_THREADS)) {
            threadCount = conf.getInt(CONF_SEARCHER_THREADS);
        }
        //noinspection DuplicateStringLiteralInspection
        log.debug("Creating Executor with " + threadCount + " threads");
        executor = Executors.newFixedThreadPool(threadCount);

        log.debug("Finished connecting to " + searchers.size() + ". Ready for use");
    }

    protected void preConstruction(Configuration conf) {
        // Override if needed
    }

    /**
     * Creates a searchClient from the given configuration. Intended to be
     * overridden in subclasses that want special SearchClients.
     * @param searcherConf the configuration for the SearchClient.
     * @return a SearchClient build for the given configuration.
     */
    protected SearchClient createClient(Configuration searcherConf) {
        return new SearchClient(searcherConf);
    }

    /**
     * Merges response collections after the searches has been performed.
     * Intended to be overridden in subclasses than wants custom merging.
     * @param request the original request that resulted in the responses.
     * @param responses a collection of responses, one from each searcher.
     * @return a merge of the responses.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected ResponseCollection merge(Request request, List<ResponseHolder> responses) {
        ResponseCollection merged = new ResponseCollection();
        for (ResponseHolder holder: responses) {
            merged.addAll(holder.getResponses());
            if (!"".equals(holder.getResponses().getTopLevelTiming())) {
                merged.addTiming(holder.getResponses().getTopLevelTiming());
            }
        }
        return merged;
    }

    protected void preProcess(Request request) {
        log.trace("No preprocessing for this node");
    }

    // TODO: Add explicit handling of query rewriting for paging
    @Override
    public ResponseCollection search(Request request) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("Starting search for " + request);
        }
        long startTime = System.currentTimeMillis();
        preProcess(request);
        ResponseCollection merged = null;
        boolean success = false;
        try {
            if (searchers.isEmpty()) {
                throw new IOException("No remote summaSearchers connected");
            }

            long startIndex = request.getLong(DocumentKeys.SEARCH_START_INDEX, 0L);
            long maxRecords = request.getLong(DocumentKeys.SEARCH_MAX_RECORDS, 0L);
            if (startIndex > 0) {
                if (log.isTraceEnabled()) {
                    log.trace(
                            "Start index is " + startIndex + " and maxRecords is " + maxRecords
                            + ". Setting startIndex=0 and maxRecords=" + (startIndex + maxRecords) + " for " + request);
                }
                request.put(DocumentKeys.SEARCH_START_INDEX, 0);
                request.put(DocumentKeys.SEARCH_MAX_RECORDS, startIndex + maxRecords);
            }

            List<String> selected = request.getStrings(SEARCH_ACTIVE, defaultSearchers);
            List<Pair<String, Future<ResponseCollection>>> searchFutures = new ArrayList<Pair<String, Future<ResponseCollection>>>(selected.size());
            for (Pair<String, SearchClient> searcher: searchers) {
                if (selected.contains(searcher.getKey())) {
                    searchFutures.add(new Pair<String, Future<ResponseCollection>>(
                            searcher.getKey(),
                            executor.submit(new SearcherCallable(searcher.getKey(), searcher.getValue(), request))));
                } else {
                    log.trace("search(...) skipping searcher " + searcher.getKey() + " as it is not asked for");
                }
            }
            log.trace("All searchers started, collecting and waiting");


            List<ResponseHolder> responses = new ArrayList<ResponseHolder>(searchFutures.size());
            for (Pair<String, Future<ResponseCollection>> searchFuture: searchFutures) {
                try {
                    responses.add(new ResponseHolder(searchFuture.getKey(), request, searchFuture.getValue().get()));
                } catch (InterruptedException e) {
                    throw new IOException(
                            "Interrupted while waiting for searcher result from " + searchFuture.getKey(), e);
                } catch (ExecutionException e) {
                    throw new IOException(
                            "ExecutionException while requesting search result from " + searchFuture.getKey(), e);
                } catch (Exception e) {
                    throw new IOException("Exception while requesting search result from " + searchFuture.getKey(), e);
                }
            }
            merged = merge(request, responses);
            postProcessPaging(merged, startIndex, maxRecords);
            log.debug("Finished search in " + (System.currentTimeMillis() - startTime) + " ms");
            merged.addTiming("aggregator.searchandmergeall", (System.nanoTime() - startTime) / 1000000);
            success = true;
            return merged;
        } finally {

            if (merged.getTransient() != null && merged.getTransient().containsKey(DocumentSearcher.DOCIDS)) {
                Object o = merged.getTransient().get(DocumentSearcher.DOCIDS);
                if (o instanceof DocIDCollector) {
                    ((DocIDCollector)o).close();
                }
            }
            if (queries.isInfoEnabled()) {
                String hits = "N/A";
                for (Response response: merged) {
                    if (response instanceof DocumentResponse) {  // If it's there, we might as well get some stats
                        hits = Long.toString(((DocumentResponse)response).getHitCount());
                    }
                }
                queries.info("Search finished " + (success ? "successfully" : "unsuccessfully (see logs for errors)")
                              + " in " + (System.currentTimeMillis()-startTime) / 1000000 + "ms with "
                              + hits + " hits. " + "Request was " + request.toString(true)
                              + " with Timing(" + merged.getTiming() + ")");
            }
        }
    }

    public static class ResponseHolder {
        private final String searcherID;
        private final Request request;
        private final ResponseCollection responses;

        public ResponseHolder(String searcherID, Request request, ResponseCollection responses) {
            this.searcherID = searcherID;
            this.request = request;
            this.responses = responses;
        }

        public String getSearcherID() {
            return searcherID;
        }
        public Request getRequest() {
            return request;
        }
        public ResponseCollection getResponses() {
            return responses;
        }
    }

    private void postProcessPaging(ResponseCollection merged, long startIndex, long maxRecords) {
        if (startIndex == 0) {
            log.trace("No paging fix needed");
            return;
        }
        log.trace("Fixing paging with startIndex=" + startIndex + " and maxRecords=" + maxRecords);
        for (Response response: merged) {
            if (!(response instanceof DocumentResponse)) {
                continue;
            }
            DocumentResponse docResponse = (DocumentResponse)response;
            docResponse.setStartIndex(startIndex);
            docResponse.setMaxRecords(maxRecords);
            List<DocumentResponse.Record> records = docResponse.getRecords();
            if (records.size() < startIndex) {
                records.clear();
            } else {
                records = new ArrayList<DocumentResponse.Record>(
                    records.subList((int)startIndex, (int)Math.min(records.size(), startIndex+maxRecords)));
            }
            docResponse.setRecords(records);
        }
    }

    @Override
    public void close() throws IOException {
        log.info("Close called for aggregator. Remote SummaSearchers will be disconnected but not closed");
        searchers.clear();
    }

    private static class SearcherCallable implements Callable<ResponseCollection> {
        private String designation;
        private SearchClient client;
        private Request request;

        private SearcherCallable(String designation, SearchClient client, Request request) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("Creating " + designation + " Future");
            this.designation = designation;
            this.client = client;
            this.request = request;
        }

        @Override
        public ResponseCollection call() throws Exception {
            long searchStart = System.currentTimeMillis();
            ResponseCollection result = client.search(request);
            result.addTiming("aggregator.searchcall." + designation, System.currentTimeMillis() - searchStart);
            return result;
        }

        public String getDesignation() {
            return designation;
        }
    }
}
