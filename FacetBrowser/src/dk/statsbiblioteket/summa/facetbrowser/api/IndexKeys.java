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
package dk.statsbiblioteket.summa.facetbrowser.api;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Keys for specifying Index-lookup. The lookups currently uses the Facet-system
 * to provide answers to queries, but is not dependent on other Searchers.
 * </p><p>
 * An index-lookup returns a list of strings from a given field. This translates
 * to tags from a given facet. As facets are capable of representing multiple
 * fields, the mapping is not 1:1. Users should consult the IndexDescriptor
 * before requesting an index.
 * </p><p>
 * The Locale used for sorting is inferred from the provider of the index data.
 * </p><p>
 * Example: field=author, term=hugo, delta=-2, length=5
 * Result: birger, carsten, hugo, lars, melanie
 * </p><p>
 * Example: field=author, term=hugo, delta=0, length=5
 * Result: hugo, lars, melanie, åse
 * </p><p>
 * Example: field=author, term=, delta=-2, length=5
 * Result: allan, birger, carsten, hugo, lars
 * </p><p>
 * Example: field=author, term=erik, delta=-2, length=5
 * Result: birger, carsten, hugo, lars, melanie
 * </p><p>
 * Example: field=author, term=åse, delta=-2, length=5
 * Result: lars, melanie, åse
 * @see IndexResponse for the returned result.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface IndexKeys {

    /**
     * The conceptual field to perform a lookup on. As this is mapped directly
     * to a facet, the facet must be specified in the IndexDescriptor.
     * </p><p>
     * Mandatory: If no field is specified, no index-lookup is performed.
     */
    public static final String SEARCH_INDEX_FIELD = "search.index.field";

    /**
     * The term to use for the lookup. The terms in the stated field are
     * searched for this term and the nearest matching term is used as origo
     * for the returned index.
     * </p><p>
     * Optional: If no term is specified, index-lookup is performed from the
     *           first available term.
     */
    public static final String SEARCH_INDEX_TERM = "search.index.term";

    /**
     * Specifies whether the search is case-sensitive or not.
     * </p><p>
     * Optional. If no value is specified, the default setup is used.
     */
    public static final String SEARCH_INDEX_CASE_SENSITIVE =
            "search.index.casesensitive";

    /**
     * The delta, relative to the origo derived from the given term, to the
     * start-position for the index. This is normally 0 or negative.
     * </p><p>
     * Optional. If no value is specified, the default setup is used.
     */
    public static final String SEARCH_INDEX_DELTA = "search.index.delta";

    /**
     * The maximum length of the index to return.
     * </p><p>
     * Optional. If no value is specified, the default setup is used.
     */
    public static final String SEARCH_INDEX_LENGTH = "search.index.length";
}