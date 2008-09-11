/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.search;

import org.apache.lucene.search.Hits;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.TermFreqVector;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * LowLevelSearchEngine.
 * User: bam. Date: Jun 9, 2006.
 * @deprecated superseded by {@link SummaSearcher}. 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam, te")
public interface LowLevelSearchEngine {
    /**
     * Constructs a query based on queryString, performs a search and returns
     * all the Hits.
     * @param queryString converted to a Query
     * @return all Hits, containing Document-IDs
     */
    public Hits getHits(String queryString);

    /**
     * Constructs a query based on queryString, performs a search and returns
     * the highest ranking numberOfResults Document-IDs.
     * @param queryString converted to a Query
     * @param numberOfDocs the maximum number of Documents-IDs to return
     * @return a TopDoc, containing Document-IDs
     */
    public TopDocs getTopDocs(String queryString, int numberOfDocs);

    /**
     * Requests a Document from the Lucene index.
     * @param docNumber the identifier for the Document
     * @return the document, corresponding to the docNumber, if it exists
     */
    public Document getDoc(int docNumber);

    /**
     * Telegram a vector of terms for a specific Lucene Document Field.
     * @param docNumber the ID for the document
     * @param field     the Field-name
     * @return          A space-separated string of terms
     */
    public TermFreqVector getTermFreqVector(int docNumber, String field);
}


