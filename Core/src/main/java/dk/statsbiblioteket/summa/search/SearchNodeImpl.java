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
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.util.ChangingSemaphore;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Handles the logic of controlling concurrent searches, open, close and
 * warm-up. This is a convenience class for building SearchNodes.
 */
// TODO: Abort warmup if an open is issued while an old open is running
// TODO: Make it optional if searches are permitted before warmup is finished
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te, mke")
public abstract class SearchNodeImpl implements SearchNode {
    private static Log log = LogFactory.getLog(SearchNodeImpl.class);

    /**
     * The maximum number of concurrent searches for this node.
     * </p><p>
     * This is optional. Default is 2.
     */
    public static final String CONF_NUMBER_OF_CONCURRENT_SEARCHES = "summa.search.numberofconcurrentsearches";
    public static final int DEFAULT_NUMBER_OF_CONCURRENT_SEARCHES = 2;

    /**
     * A resource with queries (newline-delimited) that should be expanded
     * and searched every time an index is (re)opened. Not all SearchNodes
     * supports warm-up.
     * </p><p>
     * This is optional. If not specified, no warm-up is performed.
     * @see #CONF_WARMUP_MAXTIME
     */
    public static final String CONF_WARMUP_DATA = "summa.search.warmup.data";
    public static final String DEFAULT_WARMUP_DATA = null;

    /**
     * The maximum number of milliseconds to spend on warm-up. If all queries
     * specified in {@link #CONF_WARMUP_DATA} has been processed before this
     * time limit, the warmup-phase is exited.
     * </p><p>
     * This is optional. Default is 30 seconds (30,000 milliseconds).
     */
    public static final String CONF_WARMUP_MAXTIME = "summa.search.warmup.maxtime";
    public static final int DEFAULT_WARMUP_MAXTIME = 1000 * 30;

    /**
     * If true, the implementating SearchNode is capable of performing searches
     * during open. The obvious exemption is the first open.
     * </p><p>
     * This is optional. Default is false.
     */
    public static final String CONF_SEARCH_WHILE_OPENING = "summa.search.searchwhileopening";
    public static final boolean DEFAULT_SEARCH_WHILE_OPENING = false;

    /**
     * If no searchers are ready upon search, wait up to this number of
     * milliseconds for a searcher to become ready. If no searchers are ready
     * at that time, an exception will be thrown.
     */
    public static final String CONF_SEARCHER_AVAILABILITY_TIMEOUT =
        SummaSearcherImpl.CONF_SEARCHER_AVAILABILITY_TIMEOUT;
    public static final int DEFAULT_SEARCHER_AVAILABILITY_TIMEOUT =
        SummaSearcherImpl.DEFAULT_SEARCHER_AVAILABILITY_TIMEOUT;

    /**
     * The id of the SearchNode. If no ID is given, a default id will be used.
     */
    public static final String CONF_ID = "summa.search.id";

    /**
     * The maximum number of documents that can be requested with {@link DocumentKeys#SEARCH_IDS}.
     * See {@link #CONF_DOCUMENT_IDS_ACTION}.
     */
    public static final String CONF_DOCUMENT_IDS_MAX = "search.document.ids.max";
    public static final int DEFAULT_DOCUMENT_IDS_MAX = 1000000; // 1 million

    /**
     * The reaction to {@link DocumentKeys#SEARCH_IDS} exceeding {@link #CONF_DOCUMENT_IDS_MAX}.
     * </p><p>
     * Optional. Possible values are trim (trim down to max), fail (throw an exception) and
     * skip (ignore the request fully). Default is fail.
     */
    public static final String CONF_DOCUMENT_IDS_ACTION = "search.document.ids.max.action";
    public static final MAX_ACTION DEFAULT_DOCUMENT_IDS_ACTION = MAX_ACTION.fail;
    public enum MAX_ACTION {trim, fail, skip}

    /**
     * The size in bytes of the buffer used when retrieving warmup-data.
     */
    private static final int BUFFER_SIZE = 8192;
    private static final int WARMUP_TIMEOUT = 10;
    private static final int OPEN_TIMEOUT = 60 * 1000;

    private int searcherAvailabilityTimeout = DEFAULT_SEARCHER_AVAILABILITY_TIMEOUT;

    private int concurrentSearches = DEFAULT_NUMBER_OF_CONCURRENT_SEARCHES;
    private String warmupData = DEFAULT_WARMUP_DATA;
    private int warmupMaxTime = DEFAULT_WARMUP_MAXTIME;
    private boolean searchWhileOpening = DEFAULT_SEARCH_WHILE_OPENING;
    private ChangingSemaphore slots = new ChangingSemaphore(0);
    private String id;
    private boolean explicitID = false;
    private int idsMax;
    private MAX_ACTION idsAction;

    public SearchNodeImpl(Configuration conf) {
        log.trace("Constructing SearchNodeImpl");
        concurrentSearches = conf.getInt(CONF_NUMBER_OF_CONCURRENT_SEARCHES, concurrentSearches);
        warmupData = conf.getString(CONF_WARMUP_DATA, warmupData);
        warmupMaxTime = conf.getInt(CONF_WARMUP_MAXTIME, warmupMaxTime);
        searchWhileOpening = conf.getBoolean(CONF_SEARCH_WHILE_OPENING, searchWhileOpening);
        searcherAvailabilityTimeout = conf.getInt(CONF_SEARCHER_AVAILABILITY_TIMEOUT, searcherAvailabilityTimeout);
        explicitID = conf.valueExists(CONF_ID);
        id = conf.getString(CONF_ID, this.getClass().getSimpleName());
        idsMax = conf.getInt(CONF_DOCUMENT_IDS_MAX, DEFAULT_DOCUMENT_IDS_MAX);
        idsAction = MAX_ACTION.valueOf(
                conf.getString(CONF_DOCUMENT_IDS_ACTION, DEFAULT_DOCUMENT_IDS_ACTION.toString()));
        log.info(String.format(
                Locale.ROOT,
                "Constructed SearchNodeImpl(" + this.getClass().getSimpleName() + ") with concurrentSearches %d, "
                + "warmupData length '%s', warmupMaxTime %d, searchWhileOpening %s, idsMax=%d, idsAction=%s)",
                concurrentSearches, warmupData == null ? 0 : warmupData.length(), warmupMaxTime, searchWhileOpening,
                idsMax, idsAction));
    }

    /**
     * @return the ID for this searcher.
     */
    public String getID() {
        return id;
    }
    protected void setID(String id) {
        this.id = id; // Only config triggers explicitID
    }

    /**
     * @return true if the ID was specified in the Configuration.
     */
    protected boolean isIDExplicit() {
        return explicitID;
    }

    /**
     * Helper method for sub classes that does not use open to be ready.
     * This method sets the configuration-defined number of permits.
     */
    protected void readyWithoutOpen() {
        slots.setOverallPermits(concurrentSearches);

    }

    /**
     * Fetches warm-up data from the location specified by
     * {@link #CONF_WARMUP_DATA} and calls {@link #warmup(String)} with each
     * line in the data.
     * </p><p>
     * The warmup-method never throws an exception, as warming is considered
     * a non-critical event.
     */
    public void warmup() {
        log.trace("warmup() called");
        if (warmupData == null || "".equals(warmupData)) {
            log.trace("No warmup-data defined. Skipping warmup");
            return;
        }
        if (slots.getOverallPermits() == 0) {
            log.warn("No warmup as no permits are available");
            return;
        }
        log.trace("Warming up with data from '" + warmupData + "'");
        long startTime = System.currentTimeMillis();
        long endTime = startTime + warmupMaxTime;
        try {
            long searchCount = 0;
            URL warmupDataURL = Resolver.getURL(warmupData);
            if (warmupDataURL == null) {
                log.warn("Could not resolve '" + warmupData + "' to an URL. Skipping warmup");
                return;
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(warmupDataURL.openStream(), StandardCharsets.UTF_8), BUFFER_SIZE);
            String query;
            while ((query = in.readLine()) != null &&
                   System.currentTimeMillis() < endTime) {
                // TODO: Add sorting-calls to warmup
                warmup(query);
                searchCount++;
            }
            log.debug("Warmup finished with warm-up data from '" + warmupData + "' in "
                      + (System.currentTimeMillis() - startTime) + " ms and " + searchCount + " searches");
        } catch (RemoteException e) {
            log.error(String.format(Locale.ROOT, "RemoteException performing warmup with data from '%s'", warmupData), e);
        } catch (IOException e) {
            log.warn("Exception reading the content from '" + warmupData + "' for warmup", e);
        } catch (Exception e) {
            log.error(String.format(Locale.ROOT, "Exception performing warmup with data from '%s'", warmupData), e);
        }
    }

    /**
     * Performs a warm-up with the given request. This could be a single query
     * for a DocumentSearcher or a word for a did-you-mean searcher.
     * @param request implementation-specific warmup-data.
     */
    @Override
    public void warmup(String request) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("warmup(" + request + ") called");
        try {
            if (slots.tryAcquire(1, WARMUP_TIMEOUT, TimeUnit.MILLISECONDS)) {
                managedWarmup(request);
            } else {
                log.debug("Skipped warmup(" + request + ") due to timeout");
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for free slot in warmup", e);
        } finally {
            slots.release();
        }
    }

    /**
     * A managed version of {@link SearchNode#warmup(String)}. Implementations
     * are free to ignore threading and locking-issues.
     * @param request as specified in {@link SearchNode#warmup(String)}.
     */
    protected abstract void managedWarmup(String request);

    @Override
    public synchronized void open(String location) throws RemoteException {
        log.trace("open called for location '" + location + "'");
        syncOpen(location);
        warmup();
        log.trace("open finished for location '" + location + "'");
    }
    private synchronized void syncOpen(String location) throws RemoteException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("syncOpen(" + location + ") called");
        if (slots.getOverallPermits() != 0 && !searchWhileOpening) {
            try {
                log.trace("open: acquiring " + slots.getOverallPermits() + " slots");
                if (!slots.tryAcquire(slots.getOverallPermits(), OPEN_TIMEOUT, TimeUnit.MILLISECONDS)) {
                    //noinspection DuplicateStringLiteralInspection
                    log.warn(String.format(Locale.ROOT,
                            "open(%s): Unable to acquire all slots within %d milliseconds. Re-creating slot-semaphore",
                            location, OPEN_TIMEOUT));
                    slots = new ChangingSemaphore(0);
                }
            } catch (InterruptedException e) {
                throw new RemoteException("Interrupted while acquiring all slots for open", e);
            }
        }
        if (location == null) {
            log.info("Location was null in open. Closing searcher");
            managedClose();
            slots.setOverallPermits(0);
            return;
        }
        log.trace("open: calling managedOpen(" + location + ")");
        managedOpen(location);
        if (slots.getOverallPermits() == 0) {
            slots.setOverallPermits(concurrentSearches);
        } else if (!searchWhileOpening) {
            slots.release(slots.getOverallPermits());
        }
        log.trace("syncOpen finished");
    }

    /**
     * A managed version of {@link SearchNode#open(String)}. Implementations
     * are free to ignore threading and locking-issues.
     * @param location as specified in {@link SearchNode#open(String)}.
     * @throws RemoteException if the index could not be opened.
     */
    protected abstract void managedOpen(String location) throws RemoteException;

    @Override
    public int getFreeSlots() {
        return slots.availablePermits();
    }

    @Override
    public synchronized void close() throws RemoteException {
        //noinspection DuplicateStringLiteralInspection
        log.debug("close() called");
        // The whole slot acquiring is performed by the SearchNodeImpl, except for final shutdown and in that case
        // it does not matter that the shutdown is unclean
/*        try {
            if (log.isTraceEnabled()) {
                log.trace(String.format(Locale.ROOT, "close: acquiring %d slots", slots.getOverallPermits()));
            }
            if (!slots.tryAcquire(slots.getOverallPermits(), CLOSE_TIMEOUT, TimeUnit.MILLISECONDS)) {
                //noinspection DuplicateStringLiteralInspection
                log.warn(String.format(Locale.ROOT,
                        "close: Unable to acquire all slots within %d milliseconds. Re-creating slot-semaphore",
                        CLOSE_TIMEOUT));
                slots = new ChangingSemaphore(0);
            } else {
                slots.setOverallPermits(0);
            }
        } catch (InterruptedException e) {
            slots.setOverallPermits(0);
            throw new RemoteException("Interrupted while acquiring all slots for close", e);
        }
  */
        try {
            managedClose();
        } catch(RemoteException e) {
            throw new RemoteException("close: Exception calling managedClose", e);
        }
        log.debug("Close finished");
    }

    /**
     * A managed version of {@link SearchNode#close()}. Implementations are free
     * to ignore threading and locking-issues.
     * @throws RemoteException if there was an exception closing.
     */
    protected abstract void managedClose() throws RemoteException;

    @Override
    public void search(Request request, ResponseCollection responses) throws RemoteException {
        log.trace("search called");
        if (request.getBoolean(DocumentKeys.PING, false)) {
            log.debug("Ping requested, returning immediately");
            return;
        }
        if (request.containsKey(DocumentKeys.SLEEP)) {
            long sleepMS = request.getLong(DocumentKeys.SLEEP, -1L);
            if (sleepMS > 0) {
                log.info("Sleeping for " + sleepMS + "ms before processing request, because "
                         + DocumentKeys.SLEEP + " was specified");
                responses.addTiming(getID() + ".sleep=" + sleepMS);
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepMS);
                } catch (InterruptedException e) {
                    log.warn("Interrupted while attempting to sleep for " + sleepMS
                             + "ms. Continuing standard processing");
                }
            }
        }
        try {
            // TODO: Consider timeout for slot acquirement
            if (!slots.tryAcquire(1, searcherAvailabilityTimeout, TimeUnit.MILLISECONDS)) {
                throw new RemoteException(String.format(Locale.ROOT,
                        "Time-limit of %d milliseconds exceeded", searcherAvailabilityTimeout));
            }
        } catch (InterruptedException e) {
            throw new RemoteException("Interrupted while waiting for free slot for search");
        }
        try {
            if (!checkIDRequestLimit(request)) {
                log.debug("checkIDRequestLimit returned false. Skipping search");
                return;
            }
            if (!adjustRequest(request)) {
                log.debug("adjustRequest returned false. Skipping search");
                return;
            }
            managedSearch(request, responses);
        } finally {
            slots.release();
        }
        if (request.containsKey(DocumentKeys.SEARCH_IDS)) {
            for (Response response: responses) {
                if (response instanceof DocumentResponse) {
                    ((DocumentResponse)response).setMaxRecords(Integer.MAX_VALUE);
                }
            }
        }
    }

    /**
     * If the request contains {@link DocumentKeys#SEARCH_IDS}, the request is checked against
     * {@link #CONF_DOCUMENT_IDS_MAX} and {@link #CONF_DOCUMENT_IDS_ACTION}.
     * @param request the original and unmodified request.
     */
    protected boolean checkIDRequestLimit(Request request) {
        if (!request.containsKey(DocumentKeys.SEARCH_IDS)) {
            return true;
        }
        List<String> ids = request.getStrings(DocumentKeys.SEARCH_IDS);
        if (ids.size() <= idsMax) {
            return true;
        }
        switch (idsAction) {
            case fail: throw new UnsupportedOperationException(
                    "Requested document lookup for " + ids.size() + " with max=" + idsMax);
            case trim:
                log.debug("Reducing document lookup for " + ids.size() + " with max=" + idsMax + " down to max");
                ids = ids.subList(0, idsMax);
                request.put(DocumentKeys.SEARCH_IDS,
                            ids instanceof ArrayList? (ArrayList<String>) ids : new ArrayList<>(ids));
                return true;
            case skip:
                log.debug("Requested document lookup for " + ids.size() + " exceeds max=" + idsMax
                          + ". Skipping request fully");
                return false;
            default: throw new UnsupportedOperationException("No code path for unknown action '" + idsAction + "'");
        }
    }

    /**
     * Optional adjustment of the request before search is called. Default behaviour is to rewrite
     * {@link DocumentKeys#SEARCH_IDS} by callink {@link #rewriteIDRequestToLuceneQuery}.
     * @param request the user-issued request.
     * @return true if search should commence. Else false.
     */
    protected boolean adjustRequest(Request request) {
        return rewriteIDRequestToLuceneQuery(request);
    }

    protected boolean rewriteIDRequestToLuceneQuery(Request request) {
        if (request.containsKey(DocumentKeys.SEARCH_IDS)) {
            StringBuilder sb = new StringBuilder(200);
            List<String> ids = request.getStrings(DocumentKeys.SEARCH_IDS);
            for (String id: ids) {
                if (sb.length() != 0) {
                    sb.append(" OR ");
                }
                sb.append(IndexUtils.RECORD_FIELD).append(":\"").append(id.replace("\"", "\\\"")).append("\"");
            }
            if (log.isDebugEnabled()) {
                log.debug("Expanded " + DocumentKeys.SEARCH_IDS + " query to '" + sb.toString() + "'");
            }
            request.put(DocumentKeys.SEARCH_QUERY, sb.toString());
            if (!request.containsKey(DocumentKeys.SEARCH_MAX_RECORDS)) {
                request.put(DocumentKeys.SEARCH_MAX_RECORDS, ids.size());
            }
            request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        }
        return true;
    }

    /**
     * A managed version of
     * {@link SearchNode#search(Request,
     *  dk.statsbiblioteket.summa.search.api.ResponseCollection)} open(String)}.
     * Implementations are free to ignore threading and locking-issues.
     *
     * @param request   As specified in
     *                  {@link SearchNode#search(Request,
     *                 dk.statsbiblioteket.summa.search.api.ResponseCollection)}
     * @param responses As specified in
     *                  {@link SearchNode#search(Request, ResponseCollection)}
     * @throws RemoteException as specified in
     *                  {@link SearchNode#search(
     *       dk.statsbiblioteket.summa.search.api.Request , ResponseCollection)}
     */
    protected abstract void managedSearch(Request request, ResponseCollection responses) throws RemoteException;

    /* Mutators */

    public String getWarmupData() {
        return warmupData;
    }
    public void setWarmupData(String warmupData) {
        log.debug(String.format(Locale.ROOT, "setWarmupData(%s) called", warmupData));
        this.warmupData = warmupData;
    }

    public int getWarmupMaxTime() {
        return warmupMaxTime;
    }
    public void setWarmupMaxTime(int warmupMaxTime) {
        log.debug(String.format(Locale.ROOT, "setWarmupMaxTime(%d ms) called", warmupMaxTime));
        this.warmupMaxTime = warmupMaxTime;
    }

    public int getMaxConcurrentSearches() {
        return concurrentSearches;
    }
    public synchronized void setMaxConcurrentSearches(int maxConcurrentSearches) {
        log.debug(String.format(Locale.ROOT, "setMaxConcurrentSearches(%d) called", maxConcurrentSearches));
        concurrentSearches = maxConcurrentSearches;
        if (slots.getOverallPermits() != 0) { // Race-condition here?
            slots.setOverallPermits(concurrentSearches);
        }
    }

    /*
    Helper method for logging
     */
    protected String reduceStackTrace(Request request, Throwable t) throws RemoteException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(100);
        PrintStream printer = new PrintStream(out);
        t.printStackTrace(printer);
        printer.flush();
        String message;
        try {
            message = out.toString("utf-8");
        } catch (UnsupportedEncodingException e1) {
            throw new RemoteException(
                "Unable to convert stacktrace to utf-8 from " + t.getMessage()
                + " for failed request: " + request.toString(true));
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
