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

import org.apache.lucene.document.Document;

import java.io.IOException;
import java.util.List;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;

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
     */
    public void build(boolean keepTags);

    /**
     * Store the internal representation to disk.
     * @param directory the location of the data.
     * @throws IOException if the internal representation could not be stored.
     */
    public void save(String directory) throws IOException;
}
