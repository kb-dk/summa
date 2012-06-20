package org.apache.solr.exposed;

/**
 * Exposed Index Lookup Parameters.
 */
public interface ExposedIndexLookupParams {
  /**
   * Set this to true to enable index lookup.
   */
  public static final String ELOOKUP = "elookup";

  /**
   * The query used for limiting the index lookup result. If no query is
   * stated, all terms in the field will be used.
   * </p><p>
   * Optional. Default is not defined (all terms).
   */
//  public static final String ELOOKUP_QUERY = "elookup.query";

  /**
   * The field to perform a lookup on.
   * </p><p>
   * Mandatory: If no field is specified, no index-lookup is performed.
   */
  public static final String ELOOKUP_FIELD = "elookup.field";

  /**
   * How to sort the terms. Note that specifying locale will result in
   * the terms being sorted by a Collator with the locale provided by
   * {@link #ELOOKUP_SORT_LOCALE_VALUE}. Depending on term count for the given
   * field, sorting the terms by locale might result is a significant delay for
   * the first request. A very rough time estimate is 1 minute / 1 million terms
   * for current hardware anno 2012.
   * </p><p>
   * Optional. Default is field index order.
   * Possible values are 'index' or 'locale'. If locale is specified, the
   * property {@link #ELOOKUP_SORT_LOCALE_VALUE} must be specified.
   * Note that 'count' is not valid.
   */
  public static final String ELOOKUP_SORT = "elookup.sort";
  // Mirror of ComparatorFactory.ORDER
  public static final String ELOOKUP_SORT_BYINDEX = "index";
  // By COUNT makes no sense as index lookup is inherently ordered by terms
  public static final String ELOOKUP_SORT_BYLOCALE = "locale";
  public static final String ELOOKUP_DEFAULT_SORT = ELOOKUP_SORT_BYINDEX;

  /**
   * Used when elookup.sort == locale.
   */
  public static final String ELOOKUP_SORT_LOCALE_VALUE = "elookup.sort.locale";


  /**
   * The term to use for the lookup. The terms in the stated field are
   * searched for this term and the nearest matching term is used as origo
   * for the returned index.
   * </p><p>
   * Optional: If no term is specified, index-lookup is performed from the
   *           first available term.
   */
  public static final String ELOOKUP_TERM = "elookup.term";

  /**
   * Specifies whether the search is case-sensitive or not.
   * </p><p>
   * Optional. Default is false.
   */
  public static final String ELOOKUP_CASE_SENSITIVE = "elookup.casesensitive";
  public static final boolean ELOOKUP_DEFAULT_CASE_SENSITIVE = false;

  /**
   * The delta, relative to the origo derived from the given term, to the
   * start-position for the index. This is normally 0 or negative.
   * </p><p>
   * Optional. Default value is 5.
   */
  public static final String ELOOKUP_DELTA = "elookup.delta";
  public static final int ELOOKUP_DEFAULT_DELTA = -5;

  /**
   * The maximum length of the index to return (i.e. the number of terms).
   * </p><p>
   * Optional. If no value is specified, the default setup is used.
   */
  public static final String ELOOKUP_LENGTH = "elookup.length";
  public static final int ELOOKUP_DEFAULT_LENGTH = 20;

  /**
   * The minimum number of documents that must contain the term for the term
   * to be returned.
   * </p><p>
   * Optional. Default is 0.
   */
  public static final String ELOOKUP_MINCOUNT = "elookup.mincount";
  public static final int ELOOKUP_DEFAULT_MINCOUNT = 0;
}
