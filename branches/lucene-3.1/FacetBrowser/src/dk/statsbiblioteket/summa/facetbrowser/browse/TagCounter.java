/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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




