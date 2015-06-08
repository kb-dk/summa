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
package dk.statsbiblioteket.summa.support.suggest;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.support.api.SuggestKeys;
import dk.statsbiblioteket.summa.support.api.SuggestResponse;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * A suggest-search is a low overhead Search meant for interactive use.
 * Suggest keeps track of triples with query, hitCount and queryCount.
 * When the user starts typing a query in an input field, the entered query
 * is continually send to the suggest which returns a list of queries where
 * the prefix matches what the user has entered, along with the hitCount.
 * The list is sorted descending by queryCount.
 * </p><p>
 * Example: Suggest for "Foo" is requested. The visual result is {code
 * foo fighters (3456)
 * foo bears (43567)
 * foo bar (23)
 * fooey kabloey (4563454)
 * }
 * </p><p>
 * When a search has been performed, suggest should be updated with the query
 * and the hitCount. The queryCount will be increased with 1 for each call that
 * matches an existing query.
 * </p><p>
 * It follows that suggest is independent of other searchers.
 * </p><p>
 * The suggest is updated through a pseudo-search-call. This is a bit of a hack.
 * </p><p>
 * The search-node is normally the only node in a SummaSearcher, as suggest
 * is normally used with a static location for persistence. In order to specify
 * a static location, set the following property in a SummaSearcherImpl:
 * {@link dk.statsbiblioteket.summa.search.SummaSearcherImpl#CONF_STATIC_ROOT}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class SuggestSearchNode extends SearchNodeImpl {
    private static Log log = LogFactory.getLog(SuggestSearchNode.class);
    private static Log qlog = LogFactory.getLog("suggestqueries");

    /**
     * The maximum number of results to return. This cannot be overruled in the
     * query.
     * </p><p>
     * Optional. Default is 1000.
     */
    public static final String CONF_MAX_RESULTS = "summa.support.suggest.maxresults";
    /**
     * Default value for {@link #CONF_MAX_RESULTS}.
     */
    public static final int DEFAULT_MAX_RESULTS = 1000;

    /**
     * The default number of results to return. This can be overruled in the
     * query, but cannot exceed {@link #CONF_MAX_RESULTS}.
     * </p><p>
     * Optional. Default is 10.
     *
     * @see SuggestKeys#SEARCH_MAX_RESULTS
     */
    public static final String CONF_DEFAULT_MAX_RESULTS = "summa.support.suggest.defaultmaxresults";
    /**
     * Default value for {@link #CONF_DEFAULT_MAX_RESULTS}.
     */
    public static final int DEFAULT_DEFAULT_MAX_RESULTS = 10;

    /**
     * If true, all results are lowercased. In case of equal results (e.g. "Foo"
     * and "fOO" both being lowercased to "foo"), the highest number of hits is
     * returned together with the sum of the queryCounts.
     * </p><p>
     * This setting does not affect the addition of queries, so it is possible
     * to switch back and forth between the two behaviours.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_NORMALIZE_QUERIES = "summa.support.suggest.normalizequeries";
    /**
     * Default value for {@link #CONF_NORMALIZE_QUERIES}.
     */
    public static final boolean DEFAULT_NORMALIZE_QUERIES = false;

    /**
     * The class for the back-end storage for suggest.
     * </p><p>
     * Optional. Default is
     * {@link dk.statsbiblioteket.summa.support.suggest.SuggestStorageH2}.
     */
    public static final String CONF_STORAGE_CLASS = "summa.support.suggest.storage";
    /**
     * Default value for {@link #CONF_STORAGE_CLASS}.
     */
    public static final Class<? extends SuggestStorage> DEFAULT_STORAGE = SuggestStorageH2.class;

    /**
     * The locale to use when lowercasing queries. This relevant both for
     * case-sensitive and case-insensitive operation as lowercasing is used
     * for lookup no matter the operation-mode.
     * </p><p>
     * Optional. Default is "da" (Danish).
     */
    public static final String CONF_LOWERCASE_LOCALE = "summa.support.suggest.lowercaselocale";
    /**
     * Default value for {@link #CONF_LOWERCASE_LOCALE}.
     */
    public static final String DEFAULT_LOWERCASE_LOCALE = "da";

    /**
     * The folder name for the sub-index created by Suggest.
     */
    public static final String SUGGEST_FOLDER = "suggest";

    /**
     * Maintenance search key. This should not be passed through from end-users.
     * </p><p>
     * If set to true, the suggest database is cleared.
     */
    static final String SEARCH_CLEAR = "summa.support.suggest.clean";

    /**
     * Maintenance search key. This should not be passed through from end-users.
     * </p><p>
     * If set to a URL the SuggestSearchNode will import suggest-data from the
     * given URL.
     * </p><p>
     * The data must be in tab-separated format with the columns
     * {@code hits count query}.
     */
    static final String SEARCH_IMPORT = "summa.support.suggest.import";

    /**
     * Maintenance search key. This should not be passed through from end-users.
     * </p><p>
     * If set to a file path, the SuggestSearchNode will export suggest-data
     * to the given file, creating any parent directories of necessary.
     * </p><p>
     * The data will be in tab-separated format with the columns
     * {@code hits count query}.
     */
    static final String SEARCH_EXPORT = "summa.support.suggest.export";

    /**
     * The maximum results returned.
     */
    private int maxResults;
    /**
     * Default default max result returned.
     */
    private int defaultMaxResults;
    /**
     * The suggest storage holding the suggestion.
     */
    private SuggestStorage storage;
    private final Profiler profiler = new Profiler(Integer.MAX_VALUE, 100);
    private final Profiler uProfiler = new Profiler(Integer.MAX_VALUE, 100);

    /**
     * Create a suggest search node.
     * @param conf The configuration for setting up the search node.
     */
    public SuggestSearchNode(Configuration conf) {
        super(conf);
        maxResults = conf.getInt(CONF_MAX_RESULTS, DEFAULT_MAX_RESULTS);
        defaultMaxResults = conf.getInt(CONF_DEFAULT_MAX_RESULTS, DEFAULT_DEFAULT_MAX_RESULTS);

        Class<? extends SuggestStorage> storageClass;
        storageClass = Configuration.getClass(CONF_STORAGE_CLASS, SuggestStorage.class, DEFAULT_STORAGE, conf);

        storage = Configuration.create(storageClass, conf);
        log.info(String.format(
                "Created SuggestSearchNode with maxResults=%d,  defaultMaxResults=%d and storage=%s",
                maxResults, defaultMaxResults, storageClass.getSimpleName()));
    }

    /**
     * Do a managed search. This method handles different search requests.
     * Clear ({@link SuggestStorage#clear()}),
     * import (@link {@link SuggestStorage#importSuggestions(URL)}),
     * export ({@link SuggestStorage#exportSuggestions(File)}),
     * prefix ({@link #suggestSearch(Request, ResponseCollection)}),
     * update ({@link #suggestUpdate(Request, ResponseCollection)}),
     * recent ({@link #suggestRecent(Request, ResponseCollection)}),
     * delete ({@link #deleteSuggestion(String)}).
     * See documentation for details about the individual search requests.
     *
     * @param request   The search request.
     * @param responses A collection of responses.
     * @throws RemoteException If an error occur searching or connection to the
     *                         remote searcher.
     */
    @Override
    protected void managedSearch(Request request, ResponseCollection responses) throws RemoteException {
        boolean maintenance = false;
        final int maxResults = 10;
        try {
            if (request.getBoolean(SEARCH_CLEAR, false)) {
                log.info("Clearing all suggestions");
                storage.clear();
                responses.add(new SuggestResponse("Suggestions cleared", maxResults));
                maintenance = true;
            }
            if (request.containsKey(SuggestKeys.DELETE_SUGGEST)) {
                String suggestion = request.getString(SuggestKeys.DELETE_SUGGEST);
                log.info("Deleting suggestion '" + suggestion + "' from storage");
                boolean isDeleted = storage.deleteSuggestion(suggestion);
                if (!isDeleted) {
                    log.warn("Suggestion still exists in storage");
                }
                maintenance = true;
            }
            if (request.containsKey(SEARCH_IMPORT)) {
                String sourceUrl = request.getString(SEARCH_IMPORT);
                log.info("Importing suggestions from " + sourceUrl);

                URL source = new URL(sourceUrl);
                storage.importSuggestions(source);
                responses.add(new SuggestResponse("Suggestions imported", maxResults));
                maintenance = true;
            }
            if (request.containsKey(SEARCH_EXPORT)) {
                String targetPath = request.getString(SEARCH_EXPORT);
                File target = new File(targetPath);
                log.info("Exporting suggestions to: " + target);

                storage.exportSuggestions(target);
                responses.add(new SuggestResponse("Suggestions exported", maxResults));
                maintenance = true;
            }
            if (request.containsKey(SuggestKeys.SEARCH_PREFIX)) {
                suggestSearch(request, responses);
                return;
            }
            if (request.containsKey(SuggestKeys.SEARCH_RECENT)) {
                suggestRecent(request, responses);
                return;
            }
            if (request.containsKey(SuggestKeys.SEARCH_UPDATE_QUERY)) {
                suggestUpdate(request, responses);
                return;
            }
        } catch (IOException e) {
            throw new RemoteException("Exception performing managed search with " + request, e);
        }
        if (maintenance) {
            return;
        }
        log.debug(String.format("None of the expected keys %s, %s, %s, %s or %s encountered, no suggest will be" +
                                " performed", SuggestKeys.SEARCH_PREFIX, SuggestKeys.SEARCH_UPDATE_QUERY, SEARCH_CLEAR,
                                SEARCH_IMPORT, SEARCH_EXPORT));
    }

    /**
     * Suggestion search.
     * @param request   The request. This should contain {@link SuggestKeys#SEARCH_PREFIX}.
     * @param responses The response collection.
     * @throws IOException If error occur while doing search.
     */
    private void suggestSearch(Request request, ResponseCollection responses) throws IOException {
        long startTime = System.nanoTime();
        String prefix = request.getString(SuggestKeys.SEARCH_PREFIX);
        int maxResults = request.getInt(SuggestKeys.SEARCH_MAX_RESULTS, this.defaultMaxResults);
        if (maxResults > this.maxResults) {
            log.warn(String.format("maxResults %d requested for '%s' with configuration-"
                                   + "defined maxResults %d. throttling to configuration-"
                                   + "defined max", maxResults, prefix, this.maxResults));
            maxResults = this.maxResults;
        }

        log.trace("Performing Suggest search on prefix '" + prefix + " with maxResults=" + maxResults);
        SuggestResponse response = storage.getSuggestion(prefix, maxResults);
        long time = (System.nanoTime() - startTime) / 1000000;
        profiler.beat();
        dualLog("Completed Suggest for prefix '" + prefix + "' with maxResults=" + maxResults + " in " + time + "ms. "
                + getRequestStats());
        response.addTiming("suggest.search", Math.round(time));
        responses.add(response);
    }

    /**
     * Perform a suggest recent on the suggest storage.
     * @param request   The request. This should contain {@link SuggestKeys#SEARCH_RECENT}
     * @param responses The response.
     * @throws IOException If error occur while querying.
     */
    private void suggestRecent(Request request, ResponseCollection responses) throws IOException {
        long startTime = System.nanoTime();
        int ageSeconds = request.getInt(SuggestKeys.SEARCH_RECENT);
        int maxResults = request.getInt(SuggestKeys.SEARCH_MAX_RESULTS, this.defaultMaxResults);
        if (maxResults > this.maxResults) {
            log.warn(String.format("maxResults %d requested for updates within last %ss with configuration-defined "
                                   + "maxResults %d. Throttling to configuration-defined max",
                                   maxResults, ageSeconds, this.maxResults));
            maxResults = this.maxResults;
        }

        log.trace("Performing suggestRecent search within " + ageSeconds + "s with maxResults=" + maxResults);
        responses.add(storage.getRecentSuggestions(ageSeconds, maxResults));
        dualLog("Completed suggestRecent for updates within " + ageSeconds + "s with maxResults=" + maxResults
                + " in " + (System.nanoTime() - startTime) / 1000000D + "ms");
    }

    /**
     * Perform a suggestion update.
     *
     * @param request   The request. This should contain {@link SuggestKeys#SEARCH_UPDATE_QUERY} and
     *                  {@link SuggestKeys#SEARCH_UPDATE_HITCOUNT}.
     * @param responses The response.
     * @throws IOException If error occur while querying.
     */
    private void suggestUpdate(Request request, ResponseCollection responses) throws IOException {
        long startTime = System.nanoTime();
        String query = request.getString(SuggestKeys.SEARCH_UPDATE_QUERY);
        if (!request.containsKey(SuggestKeys.SEARCH_UPDATE_HITCOUNT)) {
            String msg = String.format("Received an update with %s='%s' but no '%s' defined",
                                       SuggestKeys.SEARCH_UPDATE_QUERY, query, SuggestKeys.SEARCH_UPDATE_HITCOUNT);
            log.warn(msg);
            //noinspection DuplicateStringLiteralInspection
            responses.add(new SuggestResponse("Error: " + msg, 0));
            return;
        }
        int hits = request.getInt(SuggestKeys.SEARCH_UPDATE_HITCOUNT);
        uProfiler.beat();
        if (!request.containsKey(SuggestKeys.SEARCH_UPDATE_QUERYCOUNT)) {
            storage.addSuggestion(query, hits);
            dualLog("Completed addSuggestion(" + query + ", " + hits + ") in "
                    + (System.nanoTime() - startTime) / 1000000D + "ms. " + getUpdateStats());
        } else {
            int queryCount = request.getInt(SuggestKeys.SEARCH_UPDATE_QUERYCOUNT);
            storage.addSuggestion(query, hits, queryCount);
            dualLog("Completed extended addSuggestion(" + query + ", " + hits + ", " + queryCount + ") in "
                    + (System.nanoTime() - startTime) / 1000000D + "ms. " + getUpdateStats());
        }
        responses.add(new SuggestResponse("addSuggestion of query '" + query + "'", 10));
    }

    /**
     * Opens any existing suggest storage at the given location. If no storage
     * is present, a new storage will be created.
     * @param location where the storage is or should be. This must be parsable by {@code new File(...)}.
     * @throws RemoteException if the storage could not be opened or created.
     */
    @Override
    protected void managedOpen(String location) throws RemoteException {
        File fileLocation = new File(new File(location), SUGGEST_FOLDER);
        log.debug(String.format("manageOpen(%s) called. The specific folder was '%s'", location,
                                fileLocation.toString()));
        storage.close();
        try {
            storage.open(fileLocation);
        } catch (IOException e) {
            throw new RemoteException("Exception while opening '" + location + "'", e);
        }
    }

    /**
     * Closes storage.
     */
    @Override
    protected void managedClose() {
        log.debug("managedClose() called. Closing down storage");
        storage.close();
    }

    /**
     * No warmup is needed for suggestions.
     * @param request Not used.
     */
    @Override
    protected void managedWarmup(String request) {
        log.debug("Warmup is not necessary for Suggest");
    }

    /**
     * Wrapper for {@link SuggestStorage#listSuggestions}.
     * @param start the position from which to start extraction.
     * @param max   the maximum number of suggestions to extract.
     * @return a list of suggestions. Each suggest-entry if represented as
     *         {@code query\thits\tqueryCount} where {@code \t} is tab.
     * @throws IOException if the suggestions could not be extracted.
     */
    public ArrayList<String> listSuggestions(int start, int max) throws IOException {
        return storage.listSuggestions(start, max);
    }

    /**
     * Wrapper for {@link SuggestStorage#addSuggestions}.
     * @param suggestions a list of suggestions.
     * @throws IOException if the suggestions could not be added.
     */
    public void addSuggestions(ArrayList<String> suggestions) throws IOException {
        storage.addSuggestions(suggestions.iterator());
    }

    /**
     * Delete all suggestion in storage, not depended on case.
     * @param suggestion The suggestion string to remove from storage.
     * @return True if suggestion isn't present i storage anymore.
     */
    public boolean deleteSuggestion(String suggestion) {
        return storage.deleteSuggestion(suggestion);
    }

    private void dualLog(String message) {
        log.debug(message);
        qlog.info(message);
    }

    private String getRequestStats() {
        return "Stats(#getSuggests=" + profiler.getBeats()
               + ", q/s(last " + profiler.getBpsSpan() + ")=" + profiler.getBps(true);
    }
    private String getUpdateStats() {
        return "Stats(#updates=" + uProfiler.getBeats()
               + ", u/s(last " + uProfiler.getBpsSpan() + ")=" + uProfiler.getBps(true);
    }
}
