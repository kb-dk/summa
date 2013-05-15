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
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Wrapper that transforms requests with large {@link DocumentKeys#SEARCH_MAX_RECORDS} into multiple paged requests.
 * </p></p>
 * The underlying Search node is specified by the property
 * {@link dk.statsbiblioteket.summa.search.SearchNodeFactory#CONF_NODE} and are constructed using
 * {@link dk.statsbiblioteket.summa.search.SearchNodeFactory}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class PagingSearchNode extends ArrayList<SearchNode> implements SearchNode {
    private static final long serialVersionUID = 8974568541L;
    private static Log log = LogFactory.getLog(PagingSearchNode.class);

    /**
     * If true, all requests are performed in sequence for the underlying
     * searcher. If false, all requests are performed in parallel.
     * </p><p>
     * Optional. default is true.
     */
    public static final String CONF_SEQUENTIAL = "pager.sequential";
    public static final boolean DEFAULT_SEQUENTIAL = true;

    /**
     * The maximum page size that the underlying searcher supports.
     * </p><p>
     * Mandatory as a (low) maximum size is the sole reason for using the PagingSearchNode..
     */
    public static final String CONF_MAXPAGESIZE = "pager.maxpagesize";

    /**
     * The pagesize wil be a multipla of guipagesize when splitting a single request into multiple.
     * </p><p>
     * If the underlying SearchNode is caching, this should be set to the pagesize used by the GUI in order to
     * create multiple queries that has a chance of giving a cache hit. If the GUI pagesize is 20, the max pagesize is
     * 50 and 60 results are requested, it is assumed that a search has previously been issued for the first 40 hits
     * (to get page 2 of the search result). The resulting requests will be for hit 0-39 and hit 40-79.
     * A subsequent request for 80 hits will give the exact same requests. The request after that will be for
     * hit 0-39, 40-79 and 80-119.
     * </p><p>
     * Optional. If not specified, this will be {@link #CONF_MAXPAGESIZE}.
     */
    public static final String CONF_GUIPAGESIZE = "pager.guipagesize";

    /**
     * The timeout for searches in the sub SearchNode.
     * </p><p>
     * Milliseconds, optional. Default is 3600000 (1 hour)
     * </p>
     */
    public static final String CONF_TIMEOUT = "pager.timeoutms";
    public static final int DEFAULT_TIMEOUT = 60*60*1000; // 1 hour


    private static final int CONCURRENT = 20;
    private ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT);

    private final SearchNode subNode;
    private int timeout;
    private boolean sequential;
    private int maxPagesize;
    private int guiPagesize;
    private int requestPagesize;

    public PagingSearchNode(Configuration conf) throws RemoteException {
        sequential = conf.getBoolean(CONF_SEQUENTIAL, DEFAULT_SEQUENTIAL);
        subNode = SearchNodeFactory.createSearchNode(conf);
        if (!conf.containsKey(CONF_MAXPAGESIZE)) {
            throw new ConfigurationException("The property " + CONF_MAXPAGESIZE + " is mandatory");
        }
        maxPagesize = conf.getInt(CONF_MAXPAGESIZE);
        guiPagesize = conf.getInt(CONF_GUIPAGESIZE, maxPagesize);
        requestPagesize = maxPagesize / guiPagesize * guiPagesize;
        timeout = conf.getInt(CONF_TIMEOUT, DEFAULT_TIMEOUT);
        log.info("Constructed " + this);
    }

    @Override
    public void search(final Request request, final ResponseCollection responses) throws RemoteException {
        log.trace("Starting search");
        long processingTime = -System.currentTimeMillis();
        int requestedRecords;
        if ((requestedRecords = request.getInt(DocumentKeys.SEARCH_MAX_RECORDS, -1)) <= maxPagesize) {
            if (log.isDebugEnabled()) {
                log.debug("Passing request directly: Requested records " + requestedRecords
                          + " <= max page size " + maxPagesize);
                subNode.search(request, responses);
                return;
            }
        }

        final int requestCount = (int) Math.ceil(1.0 * requestedRecords / requestPagesize);
        final int origiStart = request.getInt(DocumentKeys.SEARCH_START_INDEX, 0);
        List<Request> requests = new ArrayList<Request>(requestCount);

        for (int r = 0 ; r < requestCount ; r++) {
            Request subRequest = request.getCopy();
            subRequest.put(DocumentKeys.SEARCH_MAX_RECORDS, requestPagesize);
            subRequest.put(DocumentKeys.SEARCH_START_INDEX, origiStart + r * requestPagesize);
            if (r > 0) {
                optimizeSubsequent(subRequest);
            }
            requests.add(subRequest);
        }

        log.debug("Requested records " + requestedRecords + " > max page size " + maxPagesize
                  + ". Issuing " + requestCount + " separate requests with page size " + requestPagesize);
        List<ResponseCollection> unmergedResponses = search(requests);
        processingTime += System.currentTimeMillis();
        mergeResponses(request, responses, unmergedResponses, processingTime);
        log.debug("Performed " + requestCount + " " + (sequential ? "sequential" : "parallel") + " paged searches in "
                  + processingTime + "ms");
    }

    // Merge is a bit special as only the documents are added and nothing else changed
    private void mergeResponses(Request request, ResponseCollection merged, List<ResponseCollection> responses,
                                long processingTime) {
        log.trace("Merging " + responses.size() + " responses");
        if (responses.size() == 1) {
            log.debug("mergeResponses: Only 1 response. Skipping merging");
            return;
        }
        if (responses.isEmpty()) {
            log.debug("mergeResponses: Zero responses. Skipping merging");
            return;
        }
        ResponseCollection response = responses.get(0);
        DocumentResponse mergedDocs = null;
        for (Response r: response) {
            if (r instanceof DocumentResponse) {
                mergedDocs = (DocumentResponse)r;
            }
        }
        if (mergedDocs == null) {
            log.warn("No DocumentResponse found in the first response so no result is returned. First response: "
                     + response);
            return;
        }
        mergedDocs.setMaxRecords(request.getLong(DocumentKeys.SEARCH_MAX_RECORDS, mergedDocs.getMaxRecords()));
        mergedDocs.setStartIndex(request.getInt(DocumentKeys.SEARCH_START_INDEX, 0));
        mergedDocs.setSearchTime(processingTime);

        for (int i = 1 ; i < responses.size() ; i++) {
            DocumentResponse docs = null;
            for (Response subsequent: responses.get(i)) {
                if (subsequent instanceof DocumentResponse) {
                    docs = (DocumentResponse)subsequent;
                }
            }
            if (docs == null) {
                log.warn("No DocumentResponse found in response " + (i+1) + "/" + responses.size()
                         + ". Merging skipped for that response");
                continue;
            }
            mergedDocs.getRecords().addAll(docs.getRecords());
        }
        // TODO: Fix timing info
        merged.add(mergedDocs);
        log.debug("Merged " + responses.size() + " responses");
    }

    private void optimizeSubsequent(Request request) {
        log.trace("OptimizeSubsequent called");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false); // Disable faceting for subSequent
    }

    /**
     * Performs multiple searches, returning a ResponseCollection for each in the same order as the issued requests.
     * @param requests to be issued to the sub SearchNode.
     * @return the responses from the requests.
     */
    private List<ResponseCollection> search(List<Request> requests) throws RemoteException {
        if (sequential) {
            return searchSequential(requests);
        }
        return searchParallel(requests);
    }

    private List<ResponseCollection> searchSequential(List<Request> requests) throws RemoteException {
        List<ResponseCollection> responses = new ArrayList<ResponseCollection>(requests.size());
        int count = 0;
        for (Request request: requests) {
            count++;
            ResponseCollection response = new ResponseCollection();
            try {
                subNode.search(request, response);
            } catch (RemoteException e) {
                throw new RemoteException("Unable to process request #" + count + "/" + requests.size()
                                          + " with query " + request, e);
            }
            responses.add(response);
        }
        return responses;
    }

    private List<ResponseCollection> searchParallel(List<Request> requests) throws RemoteException {
        List<ResponseCollection> responses = new ArrayList<ResponseCollection>(requests.size());
        log.trace("Creating and starting " + requests.size() + " future tasks");
        List<FutureTask<ResponseCollection>> futures = new ArrayList<FutureTask<ResponseCollection>>(requests.size());
        for (final Request request : requests) {
            FutureTask<ResponseCollection> future = new FutureTask<ResponseCollection>(
                    new Callable<ResponseCollection>() {
                        @Override
                        public ResponseCollection call() throws Exception {
                            ResponseCollection response = new ResponseCollection();
                            subNode.search(request, response);
                            return response;
                        }
                    });
            futures.add(future);
            executor.submit(future);
        }
        for (int i = 0; i < futures.size(); i++) {
            FutureTask<ResponseCollection> future = futures.get(i);
            try {
                ResponseCollection response = future.get(timeout, TimeUnit.MILLISECONDS);
                responses.add(response);
            } catch (InterruptedException e) {
                throw new RemoteException(
                        "Interrupted while requesting result from search task " + (i+1) + "/" + futures.size()
                        + ": " + requests.get(i), e);
            } catch (ExecutionException e) {
                throw new RemoteException(
                        "Exception executing request " + (i+1) + "/" + futures.size() + ": " + requests.get(i), e);
            } catch (TimeoutException e) {
                throw new RemoteException(
                        "Timeout (" + timeout + "ms) executing request " + (i+1) + "/" + futures.size()
                        + ": " + requests.get(i), e);
            }
        }
        return responses;
    }

    @Override
    public String toString() {
        return "PagingSearchNode(subNode=" + subNode + ", calls=" + (sequential ? "sequential" : "parallel")
               + ", maxPagesize=" + maxPagesize + ", guiPagesize=" + guiPagesize
               + ", requestPagesize=" + requestPagesize + ", timeout=" + timeout + "ms)";
    }

    // Note: Warmup is not paged
    @Override
    public void warmup(final String request) {
        if (log.isTraceEnabled()) {
            log.trace("Performing warmup with " + request);
        }
        subNode.warmup(request);
    }

    @Override
    public void open(final String location) throws RemoteException {
        log.debug(String.format("open(%s) called", location));
        subNode.open(location);
    }
    @Override
    public void close() {
        log.trace("close() called");
        try {
            subNode.close();
        } catch (RemoteException e) {
            log.error("Got a RemoteException during close. This should not happen", e);
        }
    }

    /**
     * @return the minimum free slots for the underlying searchers.
     */
    @Override
    public int getFreeSlots() {
        log.trace("getFreeSlots called");
        return subNode.getFreeSlots();
    }
}
