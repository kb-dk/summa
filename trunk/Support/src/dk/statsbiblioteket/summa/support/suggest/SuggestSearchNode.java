/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.support.suggest;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.support.api.SuggestResponse;
import dk.statsbiblioteket.summa.support.api.SuggestKeys;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.net.URL;

/**
 * A suggest-search is a low overhead Search meant for interactive use.
 * Suggest keeps track of triples with query, hitCount and queryCount.
 * When the user starts typing a query in an input field, the entered query
 * is continually send to the suggest which returns a list of queries where
 * the prefix matches what the user has entered, along with the hitCount.
 * The list is sorted descending by queryCount.
 * </p><p>
 * Example: Suggest for "Foo" is requested. The visual result is {code
 foo fighters (3456)
 foo bears (43567)
 foo bar (23)
 fooey kabloey (4563454)
 }
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
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SuggestSearchNode extends SearchNodeImpl {
    private static Log log = LogFactory.getLog(SuggestSearchNode.class);

    /**
     * The maximum number of results to return. This cannit be overruled in the
     * query.
     * </p><p>
     * Optional. Default is 1000.
     */
    public static final String CONF_MAX_RESULTS =
            "summa.support.suggest.maxresults";
    public static final int DEFAULT_MAX_RESULTS = 1000;

    /**
     * The default number of results to return. This can be overruled in the
     * query, but cannot exceep {@link #CONF_MAX_RESULTS}.
     * </p><p>
     * Optional. Default is 10.
     * @see {@link SuggestKeys#SEARCH_MAX_RESULTS}.
     */
    public static final String CONF_DEFAULT_MAX_RESULTS =
            "summa.support.suggest.defaultmaxresults";
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
    public static final String CONF_NORMALIZE_QUERIES =
            "summa.support.suggest.normalizequeries";
    public static final boolean DEFAULT_NORMALIZE_QUERIES = false;

    /**
     * The class for the back-end storage for suggest.
     * </p><p>
     * Optional. Default is
     * {@link dk.statsbiblioteket.summa.support.suggest.SuggestStorageH2}.
     */
    public static final String CONF_STORAGE_CLASS =
                                                "summa.support.suggest.storage";
    public static final Class<? extends SuggestStorage> DEFAULT_STORAGE =
                                                         SuggestStorageH2.class;

    /**
     * The locale to use when lowercasing queries. This relevant both for
     * case-sensitive and case-insensitive operation as lowercasing is used
     * for lookup no matter the operation-mode.
     * </p><p>
     * Optional. Default is "da" (Danish).
     */
    public static final String CONF_LOWERCASE_LOCALE =
            "summa.support.suggest.lowercaselocale";
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

    private int maxResults = DEFAULT_MAX_RESULTS;
    private int defaultMaxResults = DEFAULT_DEFAULT_MAX_RESULTS;
    private SuggestStorage storage;

    public SuggestSearchNode(Configuration conf) {
        super(conf);
        maxResults = conf.getInt(CONF_MAX_RESULTS, maxResults);
        defaultMaxResults = conf.getInt(
                CONF_DEFAULT_MAX_RESULTS, defaultMaxResults);

        Class<? extends SuggestStorage> storageClass;
        storageClass = Configuration.getClass(
                CONF_STORAGE_CLASS, SuggestStorage.class, DEFAULT_STORAGE,
                conf);

        storage = Configuration.create(storageClass, conf);
        log.info(String.format("Created SuggestSearchNode with maxResults=%d, "
                               + " defaultMaxResults=%d and storage=%s",
                               maxResults, defaultMaxResults,
                               storageClass.getSimpleName()));
    }

    @Override
    protected void managedSearch(Request request, ResponseCollection responses)
            throws RemoteException {
        boolean maintenance = false;
        try {
            if (request.getBoolean(SEARCH_CLEAR, false)) {
                log.info("Clearing all suggestions");
                storage.clear();
                responses.add(new SuggestResponse("Suggestions cleared", 10));
                maintenance = true;
            }
            if (request.containsKey(SEARCH_IMPORT)) {
                String sourceUrl = request.getString(SEARCH_IMPORT);
                log.info("Importing suggestions from " + sourceUrl);

                URL source = new URL(sourceUrl);
                storage.importSuggestions(source);
                responses.add(new SuggestResponse("Suggestions imported", 10));
                maintenance = true;
            }
            if (request.containsKey(SEARCH_EXPORT)) {
                String targetPath = request.getString(SEARCH_EXPORT);
                File target = new File(targetPath);
                log.info("Exporting suggestions to: " + target);

                storage.exportSuggestions(target);
                responses.add(new SuggestResponse("Suggestions exported", 10));
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
            throw new RemoteException(
                    "Exception performing managed search with " + request, e);
        }
        if (maintenance) {
            return;
        }
        log.debug(String.format(
                "None of the expected keys %s, %s, %s, %s or %s encountered,"
                + " no suggest will be performed", 
                SuggestKeys.SEARCH_PREFIX, SuggestKeys.SEARCH_UPDATE_QUERY,
                SEARCH_CLEAR, SEARCH_IMPORT, SEARCH_EXPORT));
    }

    private void suggestSearch(Request request, ResponseCollection responses)
                                                            throws IOException {
        long startTime = System.nanoTime();
        String prefix = request.getString(SuggestKeys.SEARCH_PREFIX);
        int maxResults = request.getInt(
                SuggestKeys.SEARCH_MAX_RESULTS, this.defaultMaxResults);
        if (maxResults > this.maxResults) {
            log.warn(String.format(
                    "maxResults %d requested for '%s' with configuration-"
                    + "defined maxResults %d. throttling to configuration-"
                    + "defined max", maxResults, prefix, this.maxResults));
            maxResults = this.maxResults;
        }

        log.trace("Performing Suggest search on prefix '" + prefix
                  + " with maxResults=" + maxResults);
        responses.add(storage.getSuggestion(prefix, maxResults));
        log.debug("Completed Suggest for prefix '" + prefix
                  + "' with maxResults=" + maxResults + " in "
                  + (System.nanoTime() - startTime) / 1000000D + "ms");
    }

    private void suggestRecent(Request request, ResponseCollection responses)
                                                            throws IOException {
        long startTime = System.nanoTime();
        int ageSeconds = request.getInt(SuggestKeys.SEARCH_RECENT);
        int maxResults = request.getInt(
                SuggestKeys.SEARCH_MAX_RESULTS, this.defaultMaxResults);
        if (maxResults > this.maxResults) {
            log.warn(String.format(
                    "maxResults %d requested for updates within last %ss" +
                    " with configuration-defined maxResults %d. Throttling " +
                    "to configuration-defined max",
                    maxResults, ageSeconds, this.maxResults));
            maxResults = this.maxResults;
        }

        log.trace("Performing suggestRecent search within " + ageSeconds
                  + "s with maxResults=" + maxResults);
        responses.add(storage.getRecentSuggestions(ageSeconds, maxResults));
        log.debug("Completed suggestRecent for updates within " + ageSeconds
                  + "s with maxResults=" + maxResults + " in "
                  + (System.nanoTime() - startTime) / 1000000D + "ms");
    }

    private void suggestUpdate(Request request, ResponseCollection responses)
                                                            throws IOException {
        long startTime = System.nanoTime();
        String query = request.getString(SuggestKeys.SEARCH_UPDATE_QUERY);
        if (!request.containsKey(SuggestKeys.SEARCH_UPDATE_HITCOUNT)) {
            String msg = String.format(
                    "Received an update with %s='%s' but no '%s' defined",
                    SuggestKeys.SEARCH_UPDATE_QUERY, query,
                    SuggestKeys.SEARCH_UPDATE_HITCOUNT);
            log.warn(msg);
            //noinspection DuplicateStringLiteralInspection
            responses.add(new SuggestResponse(
                "Error: " + msg, 0));
            return;
        }
        int hits = request.getInt(SuggestKeys.SEARCH_UPDATE_HITCOUNT);
        if (!request.containsKey(SuggestKeys.SEARCH_UPDATE_QUERYCOUNT)) {
            storage.addSuggestion(query, hits);
            log.debug("Completed addSuggestion(" + query + ", " + hits
                      + ") in "
                      + (System.nanoTime() - startTime) / 1000000D + "ms");
        } else {
            int queryCount = request.getInt(SuggestKeys.SEARCH_UPDATE_QUERYCOUNT);
            storage.addSuggestion(query, hits, queryCount);
            log.debug("Completed extended addSuggestion(" + query + ", "
                      + hits + ", " + queryCount + ") in "
                      + (System.nanoTime() - startTime) / 1000000D + "ms");
        }
        responses.add(new SuggestResponse(
                "addSuggestion of query '" + query + "'", 10));
    }

    /**
     * Opens any existing suggest storage at the given location. If no storage
     * is present, a new storage will be created.
     * @param location where the storage is or should be. This must be parsable
     *        by {@code new File(...)}.
     * @throws RemoteException if the storage could not be opened or created.
     */
    @Override
    protected void managedOpen(String location) throws RemoteException {
        File fileLocation = new File(new File(location), SUGGEST_FOLDER);
        log.debug(String.format(
                "manageOpen(%s) called. The specific folder was '%s'",
                location, fileLocation.toString()));
        storage.close();
        try {
            storage.open(fileLocation);
        } catch (IOException e) {
            throw new RemoteException(
                    "Exception while opening '" + location + "'", e);
        }
    }

    @Override
    protected void managedClose() throws RemoteException {
        log.debug("managedClose() called. Closing down storage");
        storage.close();
    }

    @Override
    protected void managedWarmup(String request) {
        log.debug("Warmup is not necessary for Suggest");
    }

    /**
     * Wrapper for {@link SuggestStorage#listSuggestions}.
     * @param start the position from which to start extraction.
     * @param max   the maximum number of suggestions to extract.
     * @return a list of suggestions. Each suggest-entry if represented as
     *        {@code query\thits\tqueryCount} where {@code \t} is tab.
     * @throws IOException if the suggestions could not be extracted.
     */
    public ArrayList<String> listSuggestions(int start, int max) throws
                                                                 IOException {
        return storage.listSuggestions(start, max);
    }

    /**
     * Wrapper for {@link SuggestStorage#addSuggestions}.
     * @param suggestions a list of suggestions.
     * @throws IOException if the suggestions could not be added.
     */
    public void addSuggestions(ArrayList<String> suggestions) throws
                                                              IOException {
        storage.addSuggestions(suggestions.iterator());
    }
}
