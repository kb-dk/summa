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
package dk.statsbiblioteket.summa.search.api.document;

import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Interface defining the search keys used by Summa's {@code DocumentSearcher}.
 * For all optional values, the defaults from DocumentSearcher is used.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public interface DocumentKeys {
    /**
     * String ("foo"). Mandatory.
     * </p><p>
     * The query is the basic request for a DocumentSearcher. It follows the
     * syntax of Lucene.
     *
     * @see #SEARCH_FILTER and
     * @see <a href="http://lucene.apache.org/java/docs/queryparsersyntax.html">queryparsersyntax</a>
     */
    public static final String SEARCH_QUERY = "search.document.query";

    /**
     * Strings ("sb_aleph:1234", "summon:egsklj2"...). Optional.
     * </p><p>
     * Explicit ID-query which allows the backend to optimize lookup.
     * Using this disables {@link #SEARCH_QUERY} and {@link #SEARCH_FILTER}.
     */
    public static final String SEARCH_IDS = "search.document.ids";

    /**
     * One or more Strings ("foo"). Optional.
     * </p><p>
     * The filters limits the amount of possible documents from a search.
     * They have the exact same syntax as a Query.
     *
     * @see #SEARCH_QUERY
     */
    public static final String SEARCH_FILTER = "search.document.filter";

    /**
     * If true, the {@link #SEARCH_FILTER} contains only negative clauses.
     * Lucene does not support pure negative queries so this would normally
     * result in an error. By setting this argument to true, the searcher is
     * free to do tricks to make it work anyway. Tricks might involve inserting
     * an all-matching clause in front of the filter or append the filter to
     * the query. Note that rewriting the query alters the order of the returned
     * result, which might still be preferable to not returning any results at
     * all.
     * </p><p>
     * Ideally the searcher should detect pure negative queries automatically.
     * However this is not trivial due to nested query clauses so implementation
     * has been deferred.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String SEARCH_FILTER_PURE_NEGATIVE = "search.document.filter.purenegative";


    /**
     * Integer ("10"). Optional.
     * </p><p>
     * Used for paging. States the first hit to return from the whole set of
     * documents matching the filter and the query.
     */
    public static final String SEARCH_START_INDEX = "search.document.startindex";

    /**
     * Integer ("20"). Optional.
     * </p><p>
     * Used to limit the amount of hits that is returned for a given search.
     */
    public static final String SEARCH_MAX_RECORDS = "search.document.maxrecords";
    public static final int DEFAULT_MAX_RECORDS = 20;

    /**
     * String ("myfield" or "summa-score"). Optional.
     * </p><p>
     * The sort option. This can either be the name of a field or
     * the string {@link #SORT_ON_SCORE} ("summa-score") which signifies that
     * sorting should be done according to ranking.
     */
    public static final String SEARCH_SORTKEY = "search.document.sortkey";

    /**
     * Boolean ("true" or "false"). Optional.
     * </p><p>
     * If true, sorting is performed in reverse.
     */
    public static final String SEARCH_REVERSE = "search.document.reversesort";

    /**
     * Comma-separated Strings ("foo, bar, zoo"). Optional.
     * </p><p>
     * When a search has been performed by the underlying index searcher,
     * the content of these fields should be returned.
     */
    public static final String SEARCH_RESULT_FIELDS = "search.document.resultfields";

    /**
     * Comma-separated Strings ("foo, bar, zoo"). Optional.
     * </p><p>
     * If a result-field is not present in a given hit, the fallback-value
     * at the same array-position is returned. If no fallback-values are
     * defined, null is returned.
     * </p><p>
     * Note: The number of fallback-values must either be null or the same
     * as the number of result-fields.
     */
    public static final String SEARCH_FALLBACK_VALUES = "search.document.fallbackvalues";

    /**
     * Boolean ("true" or "false"). Optional.
     * </p><p>
     * If true, docIDs for all hits in the search are collected and send on
     * through the chain of search-nodes. This enables later nodes to piggy-back
     * on the search.
     * </p><p>
     * This must be true for the FacetBrowser search node to work.
     */
    public static final String SEARCH_COLLECT_DOCIDS = "search.document.collectdocids";

    /**
     * Equivalent to
     * <a href="https://cwiki.apache.org/confluence/display/solr/Result+Grouping">Solr Result Grouping</a>.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String GROUP = "group";
    /**
     * Equivalent to
     * <a href="https://cwiki.apache.org/confluence/display/solr/Result+Grouping">Solr Result Grouping</a>.
     * </p><p>
     * The field to group on.
     * </p><p>
     * Mandatory if group==true.
     */
    public static final String GROUP_FIELD = "group.field";
    /**
     *  Equivalent to
     * <a href="https://cwiki.apache.org/confluence/display/solr/Result+Grouping">Solr Result Grouping</a>.
     * </p><p>
     * Optional. Default is 10.
     */
    public static final String ROWS = "rows";
    public static final int DEFAULT_ROWS = 10;

    /**
     *  Equivalent to
     * <a href="https://cwiki.apache.org/confluence/display/solr/Result+Grouping">Solr Result Grouping</a>.
     * </p><p>
     * Optional. Default is 1.
     */
    public static final String GROUP_LIMIT = "group.limit";
    public static final int DEFAULT_GROUP_LIMIT = 1;

    /**
     *  Equivalent to
     * <a href="https://cwiki.apache.org/confluence/display/solr/Result+Grouping">Solr Result Grouping</a>.
     * </p><p>
     * Optional. Default is not specified, which means it is created from {@link #SEARCH_SORTKEY} and
     * {@link #SEARCH_REVERSE} if specified, else "score desc".
     */
    public static final String GROUP_SORT = "group.sort";

    /**
     * Boolean ("true" or "false"). Optional. Default is false.
     * </p><p>
     * If true, explain how the searcher arrived at the result. This is
     * potentially a computationally intensive operation and should only
     * be used for testing and development.
     * </p><p>
     * Explanations are returned as the field "summa:explain" as part of the
     * DocumentResponse.Record.
     */
    public static final String SEARCH_EXPLAIN = "search.document.explain";

    public static final String EXPLAIN_RESPONSE_FIELD = "summa:explain";

    /**
     * The special sortKey signifying that sorting should be done on score,
     * thus making the search return records in order of relevance.
     */
    public static final String SORT_ON_SCORE = "summa-score";

    /**
     * Every Document is guaranteed to have the field "recordID", which will
     * be stored, indexable and identical to
     * {@link dk.statsbiblioteket.summa.common.Record#getId()}.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String RECORD_ID = IndexUtils.RECORD_FIELD;

    /**
     * Every Document is guaranteed to have the field "recordBase", which will
     * be stored, indexable and identical to
     * {@link dk.statsbiblioteket.summa.common.Record#getBase()}}.
     */
    public static final String RECORD_BASE = "recordBase";

    /**
     * If present and with the value 'true', the searcher is free to return immediately with empty result set.
     */
    public static final String PING = "ping";

    /**
     * Is present and > 0, a sleep is called with the given value in milliseconds before processing the request.
     * Primarily used for testing timeouts.
     */
    public static final String SLEEP = "sleep";

    /**
     * If true, implementations should add custom {@link dk.statsbiblioteket.summa.support.api.DebugResponse}
     * entries to the response collection.
     */
    public static final String DEBUG = "debug";
}