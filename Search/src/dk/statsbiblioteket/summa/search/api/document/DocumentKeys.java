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
    public static final String SEARCH_FILTER = "search.document.filter";

    public static final String SEARCH_QUERY = "search.document.query";

    public static final String SEARCH_START_INDEX="search.document.start-index";

    public static final String SEARCH_MAX_RECORDS="search.document.max-records";

    /**
     * String ("myfield" or "summa-score"). Optional.
     * </p><p>
     * The sort option. This can either be the name of a field or
     * the string {@link #SORT_ON_SCORE} ("summa-score") which signifies that
     * sorting should be done according to ranking.
     */
    public static final String SEARCH_SORTKEY = "search.document.sortkey";

    public static final String SEARCH_REVERSE = "search.document.reverse-sort";

    /**
     * Comma-separated Strings ("foo, bar, zoo"). Optional.
     * </p><p>
     * When a search has been performed by the underlying index searcher,
     * the content of these fields should be returned.
     */
    public static final String SEARCH_RESULT_FIELDS =
            "search.document.result-fields";

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
                    "search.document.fallback-values";

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
            "search.document.collect-docids";

    /**
     * The special sortKey signifying that sorting should be done on score,
     * thus making the search return records in order of relevance.
     */
    public static final String SORT_ON_SCORE = "summa-score";
}
