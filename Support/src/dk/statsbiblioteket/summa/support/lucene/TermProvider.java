/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.support.lucene;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Provides distributed term statistics.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface TermProvider {
    /**
     * @return the total number of documents is the distributes index.
     */
    int numDocs();

    /**
     * The sum of the document frequencies from all the index shards. 
     * @param term the term to look up. This will normally be
     *        {@code field:value}.
     * @return the document frequency for the term or -1 if it could not be
     *         resolved.
     */
    int docFreq(String term);
}
