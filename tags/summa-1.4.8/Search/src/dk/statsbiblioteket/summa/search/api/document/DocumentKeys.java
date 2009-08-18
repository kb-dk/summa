package dk.statsbiblioteket.summa.search.api.document;

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
     * @see {@link #SEARCH_FILTER}.
     * @see {@url http://lucene.apache.org/java/docs/queryparsersyntax.html}.
     */
    public static final String SEARCH_QUERY = "search.document.query";

    /**
     * String ("foo"). Optional.
     * </p><p>
     * The filter limits the amount of possible documents from a search.
     * It has the exact same syntax as a Query.
     * @see {@link #SEARCH_QUERY}.
     */
    public static final String SEARCH_FILTER = "search.document.filter";

    /**
     * Integer ("10"). Optional.
     * </p><p>
     * Used for paging. States the first hit to return from the whole set of
     * documents matching the filter and the query.
     */
    public static final String SEARCH_START_INDEX="search.document.startindex";

    /**
     * Integer ("20"). Optional.
     * </p><p>
     * Used to limit the amount of hits that is returned for a given search.
     */
    public static final String SEARCH_MAX_RECORDS="search.document.maxrecords";

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
    public static final String SEARCH_RESULT_FIELDS =
            "search.document.resultfields";

    /**
     * Comma-separated Strings ("foo, bar, zoo"). Optional.
     * </p><p>
     * If a result-field is not present in a given hit, the fallback-value
     * at the same array-position is returned. If no fallback-values are
     * defined, null is returned.
     * </p><p>
     * Note: The number of fallback-values must either be null or the same
     *       as the number of result-fields.
     */
    public static final String SEARCH_FALLBACK_VALUES =
                    "search.document.fallbackvalues";

    /**
     * Boolean ("true" or "false"). Optional.
     * </p><p>
     * If true, docIDs for all hits in the search are collected and send on
     * through the chain of search-nodes. This enables later nodes to piggy-back
     * on the search.
     * </p><p>
     * This must be true for the FacetBrowser search node to work.
     */
    public static final String SEARCH_COLLECT_DOCIDS =
            "search.document.collectdocids";

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
    public static final String RECORD_ID = "recordID";

    /**
     * Every Document is guaranteed to have the field "recordBase", which will
     * be stored, indexable and identical to
     * {@link dk.statsbiblioteket.summa.common.Record#getBase()}}.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String RECORD_BASE = "recordBase";
}
