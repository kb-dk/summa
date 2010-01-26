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
package dk.statsbiblioteket.summa.support.api;

import dk.statsbiblioteket.summa.support.suggest.SuggestSearchNode;

/**
 * Interface defining the search keys used to query a
 * {@link SuggestSearchNode}
 */
public interface SuggestKeys {
    /**
     * The prefix to use as the base for the suggestions-list. Use
     * {@link #SEARCH_MAX_RESULTS} to limit the size of the result set.
     * </p><p>
     * The presense of this property means that the request to suggest is a
     * standard search.
     * @see {@link #SEARCH_UPDATE_QUERY}.
     */
    String SEARCH_PREFIX =
            "summa.support.suggest.prefix";

    /**
     * Find the most recently updated suggestions ordered by query count.
     * Use {@link #SEARCH_MAX_RESULTS} to limit the size of the result set.
     * The value of this property should be set to the number of seconds
     * backwards in history to query.
     * </p><p>
     * The presense of this property means that the request to suggest is a
     * search for the most recently updated suggestions.
     * @see {@link #SEARCH_UPDATE_QUERY}.
     */
    String SEARCH_RECENT =
            "summa.support.suggest.recent";

    /**
     * If present, the request is seen as an update to the suggest-data.
     * If present, the property {@link #SEARCH_UPDATE_HITCOUNT} should
     * also be defined.
     */
    String SEARCH_UPDATE_QUERY =
            "summa.support.suggest.update.query";
    /**
     * If present, the request is seen as an update to the suggest-data.
     * If present, the property {@link #SEARCH_UPDATE_QUERY} should
     * also be defined.
     */
    String SEARCH_UPDATE_HITCOUNT =
            "summa.support.suggest.update.hitcount";
    /**
     * If present, the request is seen as an update to the suggest-data.
     * The query count is optional. If it is not present, any existing query
     * count will be preserved for the update.
     * If present, the properties {@link #SEARCH_UPDATE_QUERY} and
     * {@link #SEARCH_UPDATE_HITCOUNT} should also be defined.
     */
    String SEARCH_UPDATE_QUERYCOUNT =
            "summa.support.suggest.update.querycount";
    /**
     * The maximum number of results to return for the given request.
     * </p><p>
     * Optional. Default is {@link SuggestSearchNode#CONF_DEFAULT_MAX_RESULTS} (10).
     *           Maximum is {@link SuggestSearchNode##CONF_MAX_RESULTS} (1000).
     */
    String SEARCH_MAX_RESULTS = SuggestSearchNode.CONF_MAX_RESULTS;
}

