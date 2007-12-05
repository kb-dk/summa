/* $Id: TagCounter.java,v 1.11 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.11 $
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: TagCounter.java,v 1.11 2007/10/05 10:20:22 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.browse;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Interface for counting occurences of tags in facets. This is normally used
 * together with a search-result.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface TagCounter {

    /**
     * Increment the count for a Tag in a Facet. The count is incremented by 1.
     * Note: due to performance reasons, this isn't possible to make
     *       thread-safe.
     * @param facetID the ID for the Facet.
     * @param tagID   the ID for the Tag.
     */
    public void increment(int facetID, int tagID);

    /**
     * Get a map with Facets containing Tags, sorted by sortOrder. See
     * {@link FacetStructure} for obvious use of this.
     * @param sortOrder how to sort the result. It is assumed that the
     *                  ID's for tags resolves to Strings, that are in
     *                  alpha-order.
     * @return a representation of the first elements, as specified by the
     *         structureDescription and the sortOrder.
     */
    public FacetStructure getFirst(FacetStructure.TagSortOrder sortOrder);

    /**
     * Reset this counter, so that is is ready for new increments.
     * If possible, this should be done in a threaded manner.
     * Implementations must ensure that reset and {@link #getFirst} are
     * thread-safe.
     */
    public void reset();

}
