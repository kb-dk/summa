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
package dk.statsbiblioteket.summa.facetbrowser.build;

import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.facetbrowser.core.FacetCore;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;

/**
 * Builds or updates the structures needed for facet browsing.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface Builder extends FacetCore {
    /**
     * Updates the facet-structure based on the given Payload.
     * The record must contain either meta-value
     * {@link LuceneIndexUtils#META_ADD_DOCID},
     * {@link LuceneIndexUtils#META_DELETE_DOCID} or both, depending on whether
     * it is an addition, a deletion or an update.
     * @param payload used as basis for the update.
     * @return if the underlying structure was updated as expected.
     */
    public boolean update(Payload payload);

    /**
     * Discard any previous mapping and optionally tags and perform a complete
     * build of the internal structure.
     * @param keepTags if true, any existing tags are re-used. If the build is
     *                 triggered because of suspicion of inconsistencies, it is
     *                 recommended to let keepTags be true, as it speeds up
     *                 building.
     * @throws IOException in case of I/O problems.
     */
    public void build(boolean keepTags) throws IOException;

    /**
     * Add the tag under the facet to the docID. If any value doesn't exist,
     * the underlying structures are responsible for creating them.
     * @param docID the document-index specific document ID.
     * @param facet the facet for the tag. Depending on implementation, the
     *              facet must be specified in the setup, before calling this
     *              method. The default implementation requires it.
     * @param tag   the tag to add.
     * @throws IOException if an I/O error occured.
     */
    public void add(int docID, String facet, String tag) throws IOException;

    /**
     * Clear all tags for the given docID.
     * @param docID the document-index specific document ID.
     * @throws IOException if the document could not be cleared.
     */
    public void remove(int docID) throws IOException;

    /**
     * Clear the underlying structure.
     * @param keepTags if true, the tag structures are kept.
     * @throws IOException if the structure could not be cleared.
     */
    public void clear(boolean keepTags) throws IOException;

    /**
     * Store the internal representation. The location will normally be
     * specified upon creation of the Builder.
     * @throws IOException if the internal representation could not be stored.
     */
    public void store() throws IOException;
}




