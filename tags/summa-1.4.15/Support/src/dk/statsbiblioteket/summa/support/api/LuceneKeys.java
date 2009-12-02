package dk.statsbiblioteket.summa.support.api;

import dk.statsbiblioteket.summa.support.lucene.search.LuceneSearchNode;

/**
 * Interface collecting the search keys used for interacting with a
 * {@link LuceneSearchNode}.
 */
public interface LuceneKeys {


    /**
     * If present, normal search will be skipped an a MoreLikeThis-search will
     * be performed. The recordid is verbatim for the record (document) that
     * should be used as base for the MoreLikethis-functionality.
     * </p><p>
     * Optional. If no value is present, MoreLikeThis will not be active.
     * @see {@link dk.statsbiblioteket.summa.search.api.document.DocumentKeys#SEARCH_START_INDEX}.
     * @see {@link dk.statsbiblioteket.summa.search.api.document.DocumentKeys#SEARCH_MAX_RECORDS}.
     */
    String SEARCH_MORELIKETHIS_RECORDID =
            "search.document.lucene.morelikethis.recordid";
}
