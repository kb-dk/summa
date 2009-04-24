/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * Copyright 2007 Statsbiblioteket, Denmark
 */
package dk.statsbiblioteket.summa.facetbrowser.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.facetbrowser.FacetStructure;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;

/**
 * Interface defining the search keys used by Summa's {@code BrowserImpl}.
 * </p><p>
 * The FacetBrowser piggy-backs on a DocumentSearcher, from which the query
 * and filter is used. See {@link DocumentKeys} for details.
 * </p><p>
 * Note: in order to enable the generation of docIDs for the FacetBrowser,
 * the DocumentSearcher needs to have the property
 * {@link DocumentKeys#SEARCH_COLLECT_DOCIDS} set to true.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface FacetKeys {

    /**
     * A comma-separeted list with the names of the wanted Facets.
     * Optionally, the maximum Tag-count for a given Facet can be specified in
     * parenthesis after the name.
     * Example: "Title, Author (5), City (10), Year".
     * If no maximum Tag-count is specified, the number is taken from the
     * defaults.
     * Optionally, the sort-type for a given Facet can be specified in the same
     * parenthesis. Valid values are {@link FacetStructure#SORT_POPULARITY} and
     * {@link FacetStructure#SORT_ALPHA}. If no sort-type is specified, the
     * number is taken from the defaults.
     * Example: "Title (ALPHA), Author (5 POPULARITY), City"
     * </p><p>
     * This is all optional. If no facets are specified, the default facets
     * are requested.
     */
    public static final String SEARCH_FACET_FACETS = "search.facet.facets";
}



