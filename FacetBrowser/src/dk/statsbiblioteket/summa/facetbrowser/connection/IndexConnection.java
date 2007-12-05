/* $Id: IndexConnection.java,v 1.8 2007/12/04 09:28:20 te Exp $
 * $Revision: 1.8 $
 * $Date: 2007/12/04 09:28:20 $
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
package dk.statsbiblioteket.summa.facetbrowser.connection;

import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.Hits;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.IndexReader;
import dk.statsbiblioteket.summa.common.lucene.index.IndexConnector;
import dk.statsbiblioteket.summa.common.lucene.search.SlimCollector;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * @deprecated in favor of {@link IndexConnector}.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "te")
public interface IndexConnection {
    /**
     * Constructs a query based on queryString, performs a search and returns
     * all the Hits.
     * @param queryString converted to a Query
     * @return all Hits, containing Document-IDs
     */
    public Hits getResults(String queryString);

    /**
     * Constructs a query based on queryString, performs a search and returns
     * the highest ranking numberOfResults Document-IDs.
     * @param queryString converted to a Query
     * @param numberOfResults the maximum number of Documents-IDs to return
     * @return a TopDoc, containing Document-IDs
     */
    public TopDocs getTopResults(String queryString, int numberOfResults);

    /**
     * Requests a Document from the Lucene index.
     * @param id the identifier for the Document
     * @return the document, correspinding to the id, if it exists
     */
    public Document getDoc(int id);

    /**
     * Telegram a vector of terms for a specific Lucene Document Field.
     * @param docNumber the ID for the document
     * @param field     the Field-name
     * @return          A space-separated string of terms
     */
    public TermFreqVector getTermFreqVector(int docNumber, String field);

    /**
     * Constructs a Query based on query, performs a search and returns a
     * SlimCollector, containing the document IDs. A SlimCollector is a thin
     * wrapper around document IDs, which discards scores. This is the fastest
     * known way of getting the document IDs for searches with many hits.
     * @param query converted to a Query
     * @return a collection of the document IDs from the search.
     *         Important: It is the responsibility of the caller to release
     *         the returned SlimCollector, using releaseSlimCollector.
     *         Failure to do so will result in memory leaking.
     */
    public SlimCollector getSlimDocs(String query);

    /**
     * Get an IndexReader for this index.
     * @return an IndexReader for this index.
     */
    public IndexReader getIndexReader();

    /**
     * Release the SlimCollector returned by getSlimDocs.
     * @param collector a collector that isn't used anymore
     */
    public void releaseSlimCollector(SlimCollector collector);

}
