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

import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResult;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Interface for counting occurences of tags in facets. This is normally used
 * together with a search-result. The order of usage is normally as follows:
 * </p><p>
 * 1. Call {@link #verify} to make sure the counter is ready.<br />
 * 2. Fill the counter with a series of calls to {@link #increment}.<br />
 * 3. Get the result from {@link #getFirst}.<br />
 * 4. Start a clearing with {@link #reset} so that it can run in the background.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface TagCounter {
    /**
     * Ensures that previous {@link #reset}s are finished and that the internal
     * structures are in sync with Tag-pools. It is *strongly* adviced to call
     * this before starting to fill the counter with {@link #increment}.
     */
    public void verify();

    /**
     * Increment the count for a Tag in a Facet. The count is incremented by 1.
     * Note: due to performance reasons, this is not thread-safe.
     * @param facetID the ID for the Facet.
     * @param tagID   the ID for the Tag.
     */
    public void increment(int facetID, int tagID);

    /**
     * Get a map with Facets containing Tags, sorted by sortOrder. See
     * {@link FacetResult} for obvious use of this.
     * @param request the facets and max tags to return together with sort order
     *                and similar options. If this is null, the default
     *                structure will be used.
     * @return a representation of the first elements, as specified by the
     *         structureDescription and the request.
     */
    public FacetResult getFirst(Structure request);

    /**
     * Reset this counter, so that is is ready for new increments.
     * This must be done before a new round of updates can take place.
     * If possible, this should be done in a threaded manner.
     */
    public void reset();

}
