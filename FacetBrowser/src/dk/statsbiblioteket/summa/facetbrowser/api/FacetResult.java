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
 * CVS:  $Id: FacetResult.java,v 1.5 2007/10/05 10:20:22 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.api;

import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * The FacetStructure represents the result of facet and tag extraction from
 * a search result.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface FacetResult<T> extends Response {
    /**
     * Sort order for tags.<br/>
     * tag:        Natural order for the tags (most probably alpha-numeric).
     * popularity: Order by number of occurences.
     */
    public enum TagSortOrder {tag, popularity}

    /**
     * Reduce the representation according to the limitations defined in
     * the given description. The reduction is also responsible for sorting.
     * @param tagSortOrder the order in which the tags should be sorted.
     */
    public void reduce(TagSortOrder tagSortOrder);

    /**
     * Resolve any JVM-specific dependencies and produce a FacetStructure
     * suitable for network transfer.
     * @return a version of the FacetStructure suitable for external use.
     */
    public FacetResult externalize();
}




