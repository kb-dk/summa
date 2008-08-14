/* $Id: Builder.java,v 1.5 2007/10/05 10:20:24 te Exp $
 * $Revision: 1.5 $
 * $Date: 2007/10/05 10:20:24 $
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
package dk.statsbiblioteket.summa.facetbrowser.build;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.File;
import java.io.IOException;

/**
 * Builds or updates the structures needed for facet browsing.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface Builder {
    /**
     * Updates the facet-structure based on the given Record.
     * The record must contain either meta-value
     * {@link LuceneIndexUtils#META_ADD_DOCID},
     * {@link LuceneIndexUtils#META_DELETE_DOCID} or both, depending on whether
     * it is an addition, a deletion or an update.
     * @param record used as basis for the update.
     */
    public void updateRecord(Record record);

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
     * @param shift if true, all subsequent documents are shifted down. This
     *              can be considered a removal of the docID, instead of just
     *              a clear. If false, no shifting is done.
     *              Note: Lucene deletes documents with delayed shifting:
     *              A deleted document is marked as deleted. Upon merging,
     *              optimization and purgeDeletes, subsequent documents are
     *              shifted. The order after sifting is not guaranteed.
     * @throws IOException if the document could not be cleared.
     */
    public void clear(int docID, boolean shift) throws IOException;

    /**
     * Clear the underlying structure.
     * @param keepTags if true, the tag structures are kept.
     * @throws IOException if the structure could not be cleared.
     */
    public void clear(boolean keepTags) throws IOException;

    /**
     * Store the internal representation to disk.
     * @param directory the location of the data.
     * @throws IOException if the internal representation could not be stored.
     */
    public void save(File directory) throws IOException;
}
