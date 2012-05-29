package org.apache.solr.exposed;

/**
 * Exposed Facet Parameters, taken from
 * {@link org.apache.solr.common.params.FacetParams}.
 */
public interface ExposedFacetParams {
  /**
   * Should facet counts be calculated?
   */
  public static final String EFACET = "efacet";

  /**
   * Any lucene formated queries the user would like to use for
   * Facet Constraint Counts (multi-value)
   */
//  public static final String EFACET_QUERY = EFACET + ".query";
  /**
   * Any field whose terms the user wants to enumerate over for
   * Facet Constraint Counts (multi-value)
   */
  public static final String EFACET_FIELD = EFACET + ".field";

  /**
   * The offset into the list of facets.
   * Can be overridden on a per field basis.
   */
  public static final String EFACET_OFFSET = EFACET + ".offset";

  /**
   * Numeric option indicating the maximum number of facet field counts
   * be included in the response for each field - in descending order of count.
   * Can be overridden on a per field basis.
   */
  public static final String EFACET_LIMIT = EFACET + ".limit";

  /**
   * Numeric option indicating the minimum number of hits before a facet should
   * be included in the response.  Can be overridden on a per field basis.
   */
  public static final String EFACET_MINCOUNT = EFACET + ".mincount";

  /**
   * Comma separated list of fields to pivot
   *
   * example: author,type  (for types by author / types within author)
   */
//  public static final String EFACET_PIVOT = EFACET + ".pivot";

  /**
   * Minimum number of docs that need to match to be included in the sublist
   *
   * default value is 1
   */
//  public static final String EFACET_PIVOT_MINCOUNT = EFACET_PIVOT + ".mincount";


  /**
   * String option: "count" causes facets to be sorted
   * by the count, "index" results in index order, "locale" requires
   * efacet.sort.locale to be set to a valid locale.
   *
   * This can be overridden on a per field basis..
   */
  public static final String EFACET_SORT = EFACET + ".sort";

  public static final String EFACET_SORT_COUNT = "count";
  public static final String EFACET_SORT_INDEX = "index";
  public static final String EFACET_SORT_LOCALE = "locale";

  /**
   * If true, the order of the tags is reversed.
   */
  public static final String EFACET_REVERSE = EFACET + ".reverse";


  /**
   * Used when efacet.sort == locale.
   */
  public static final String EFACET_SORT_LOCALE_VALUE = EFACET + ".sort.locale";


  /**
   * If true, the facets are treated as hierarchical.
   *
   * This can be overridden on a per field basis..
   */
  public static final String EFACET_HIERARCHICAL = EFACET + ".hierarchical";

  /**
   * The delimiter when using hierarchical faceting. Default is '/'.
   *
   * This can be overridden on a per field basis.
   */
  public static final String EFACET_HIERARCHICAL_DELIMITER =
      EFACET + ".hierarchical.delimiter";
  public static final String EFACET_HIERARCHICAL_DELIMITER_DEFAULT = "/";

  /**
   * The maximum depth to expand hierarchical faceting to. 1 is equivalent to
   * simple faceting. Default is unlimited.
   *
   * This can be overridden on a per field basis.
   */
  public static final String EFACET_HIERARCHICAL_LEVELS =
      EFACET + ".hierarchical.levels";

  // TODO: Mimic params from SOLR-64

  /**
   * Only return constraints of a facet field with the given prefix.
   */
//  public static final String EFACET_PREFIX = EFACET + ".prefix";
}
