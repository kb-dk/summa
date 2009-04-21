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
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;

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
     * @see {@link #SEARCH_MAX_RESULTS}.
     */
    public static final String CONF_DEFAULT_MAX_RESULTS =
            "summa.support.suggest.defaultmaxresults";
    public static final int DEFAULT_DEFAULT_MAX_RESULTS = 10;

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
     * The prefix to use as the base for the suggestions-list.
     * </p><p>
     * The presense of this property means that the request to suggest is a
     * standard search.
     * @see {@link #SEARCH_UPDATE_QUERY}.
     */
    public static final String SEARCH_PREFIX =
            "summa.support.suggest.prefix";

    /**
     * If present, the request is seen as an update to the suggest-data.
     * If present, the property {@link #SEARCH_UPDATE_HITCOUNT} should
     * also be defined.
     */
    public static final String SEARCH_UPDATE_QUERY =
            "summa.support.suggest.update.query";

    /**
     * If present, the request is seen as an update to the suggest-data.
     * If present, the property {@link #SEARCH_UPDATE_QUERY} should
     * also be defined.
     */
    public static final String SEARCH_UPDATE_HITCOUNT =
            "summa.support.suggest.update.hitcount";

    /**
     * If present, the request is seen as an update to the suggest-data.
     * The query count is optional. If it is not present, any existing query
     * count will be preserved for the update.
     * If present, the properties {@link #SEARCH_UPDATE_QUERY} and
     * {@link #SEARCH_UPDATE_HITCOUNT} should also be defined.
     */
    public static final String SEARCH_UPDATE_QUERYCOUNT =
            "summa.support.suggest.update.querycount";

    /**
     * The maximum number of results to return for the given request.
     * </p><p>
     * Optional. Default is {@link #CONF_DEFAULT_MAX_RESULTS} (10).
     *           Maximum is {@link #CONF_MAX_RESULTS} (1000).
     */
    public static final String SEARCH_MAX_RESULTS = CONF_MAX_RESULTS;

    public static final String SUGGEST_FOLDER = "suggest";

    private int maxResults = DEFAULT_MAX_RESULTS;
    private int defaultMaxResults = DEFAULT_DEFAULT_MAX_RESULTS;
    private SuggestStorage storage;

    public SuggestSearchNode(Configuration conf) {
        super(conf);
        maxResults = conf.getInt(CONF_MAX_RESULTS, maxResults);
        defaultMaxResults = conf.getInt(
                CONF_DEFAULT_MAX_RESULTS, defaultMaxResults);

        Class<? extends SuggestStorage> storageClass;
        storageClass = Configuration.getClass(CONF_STORAGE_CLASS,
                                              SuggestStorage.class,
                                              DEFAULT_STORAGE,
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
        try {
            if (request.containsKey(SEARCH_PREFIX)) {
                suggestSearch(request, responses);
                return;
            }
            if (request.containsKey(SEARCH_UPDATE_QUERY)) {
                suggestUpdate(request, responses);
                return;
            }
        } catch (IOException e) {
            throw new RemoteException(
                    "Exception performing managed search with " + request);
        }
        log.debug("None of the expected keys " + SEARCH_PREFIX + " or "
                  + SEARCH_UPDATE_QUERY + " encountered, no suggest performed");
    }

    private void suggestSearch(Request request, ResponseCollection responses)
                                                            throws IOException {
        long startTime = System.nanoTime();
        String prefix = request.getString(SEARCH_PREFIX);
        int maxResults = request.getInt(
                SEARCH_MAX_RESULTS, this.defaultMaxResults);
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

    private void suggestUpdate(Request request, ResponseCollection responses)
                                                            throws IOException {
        long startTime = System.nanoTime();
        String query = request.getString(SEARCH_UPDATE_QUERY);
        if (!request.containsKey(SEARCH_UPDATE_HITCOUNT)) {
            log.warn(String.format(
                    "Received an update with %s='%s' but no '%s' defined",
                    SEARCH_UPDATE_QUERY, query, SEARCH_UPDATE_HITCOUNT));
        }
        int hits = request.getInt(SEARCH_UPDATE_HITCOUNT);
        if (!request.containsKey(SEARCH_UPDATE_QUERYCOUNT)) {
            storage.addSuggestion(query, hits);
            log.debug("Completed addSuggestion(" + query + ", " + hits
                      + ") in "
                      + (System.nanoTime() - startTime) / 1000000D + "ms");
        } else {
            int queryCount = request.getInt(SEARCH_UPDATE_QUERYCOUNT);
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
}
