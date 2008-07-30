/* $Id: Browser.java,v 1.7 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.7 $
 * $Date: 2007/10/05 10:20:22 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
package dk.statsbiblioteket.summa.facetbrowser.browse;

import dk.statsbiblioteket.util.qa.QAInfo;


/**
 * This is tightly connected to
 * {@link dk.statsbiblioteket.summa.common.search.Search}
 * as the browser should present the same query possibilities as the search.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface Browser {
    /**
     * Generate an XML representation for facet browsing of the result from
     * searching with the given query.
     * @param queryString Query string.
     * @param filterQuery The filter is used before the query.
     * @param queryLang the language, as specified in ISO 639-1. This selects
     *                  the add of aliases used for query-parsing. A value of
     *                  null selects the default language.
     * @param sortOrder the sort order for the tags. The order of the facets
     *                  is specified in the properties.
     * @return an XML representation for facet browsing.
     */
    public String getFacetXML(String queryString,
                              String filterQuery,
                              String queryLang,
                              FacetStructure.TagSortOrder sortOrder);

    // TODO: Consider a request-object with wanted facets et al

    /**
     * Generate a facet browser structure and return it in a form suitable for
     * further handling, such as merging, reduction or conversion to XML.
     * @param queryString Query string.
     * @param filterQuery The filter is used before the query.
     * @param queryLang the language, as specified in ISO 639-1. This selects
     *                  the add of aliases used for query-parsing. A value of
     *                  null selects the default language.
     * @param sortOrder the sort order for the tags. The order of the facets
     *                  is specified in the properties.
     * @return a machine-oriented representation of the facet browser structure
     *         corresponding to the given query. Note that the result need not
     *         be directly Serializable, but that calling 
     *         {@link FacetStructure#externalize()} on the result should produce
     *         a Serializable version.
     */
    public FacetStructure getFacetMap(String queryString,
                              String filterQuery,
                              String queryLang,
                              FacetStructure.TagSortOrder sortOrder);
}
