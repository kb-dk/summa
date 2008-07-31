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

import java.util.List;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.facetbrowser.Structure;


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
     * @return a list with the names of all mapped Facets.
     */
    public List<String> getFacetNames();

    /**
     * Generate a facet browser structure and return it in a form suitable for
     * further handling, such as merging, reduction or conversion to XML.
     * </p><p>
     * The parameters of this call are fairly low-level. It is recommended that
     * implementations of Browser also supply a query-based method for
     * requesting facet/tag information, as it eases experiments.
     * @param docIDs    the ids for the documents to calculate Tags for.
     * @param startPos  the start-position within the docIDs.
     * @param length    the number of docIDs to use.
     * @param facets    a comma-separated list with the names of the wanted
     *                  Facets. See {@link Request} for details. If null is
     *                  specified, the default Facets are returned.
     * @return a machine-oriented representation of the facet browser structure
     *         corresponding to the given query. Note that the result need not
     *         be directly Serializable, but that calling 
     *         {@link Result#externalize()} on the result should produce
     *         a Serializable version.
     */
    public Result getFacetMap(int[] docIDs, int startPos, int length,
                              String facets);

    // TODO: Add index-lookup
}
