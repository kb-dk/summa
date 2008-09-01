package dk.statsbiblioteket.summa.search.api.document;

/**
 * Interface defining the search keys used by Summa's {@code DocumentSearcher}.
 */
public interface DocumentKeys {
    public static final String SEARCH_FILTER = "search.document.filter";

    public static final String SEARCH_QUERY = "search.document.query";

    public static final String SEARCH_START_INDEX="search.document.start-index";

    public static final String SEARCH_MAX_RECORDS="search.document.max-records";

    public static final String SEARCH_SORTKEY = "search.document.sortkey";

    public static final String SEARCH_REVERSE = "search.document.reverse-sort";

    public static final String SEARCH_RESULT_FIELDS =
            "search.document.result-fields";

    public static final String SEARCH_FALLBACK_VALUES =
                    "search.document.fallback-values";
    /**
     * The special sortKey signifying that sorting should be done on score,
     * thus making the search return records in order of relevance.
     */
    public static final String SORT_ON_SCORE = "summa-score";
}
