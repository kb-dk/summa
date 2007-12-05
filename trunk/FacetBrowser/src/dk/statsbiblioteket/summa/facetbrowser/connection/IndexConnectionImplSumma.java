/* $Id: IndexConnectionImplSumma.java,v 1.8 2007/12/04 09:28:20 te Exp $
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

import java.util.List;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

//import dk.statsbiblioteket.summa.search.SearchEngineImpl;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.TopDocs;
import org.apache.log4j.Logger;
import dk.statsbiblioteket.summa.common.lucene.index.IndexConnector;
import dk.statsbiblioteket.summa.common.lucene.search.SlimCollector;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * @deprecated in favor of {@link IndexConnector}.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "te")
public class IndexConnectionImplSumma  implements IndexConnection {
    private static Logger log =
            Logger.getLogger(IndexConnectionImplSumma.class);

//    SearchEngineImpl searchEngine = SearchEngineImpl.getInstance();

    List<SlimCollector> collectorPool = new LinkedList<SlimCollector>();
    private ReentrantLock poolLock = new ReentrantLock();
    private static final String NO_CONNECTION =
            "No connection to the Summa Searcher yet";

    public Hits getResults(String queryString) {
        throw new UnsupportedOperationException(NO_CONNECTION);
//        return searchEngine.getHits(queryString);
    }

    public TopDocs getTopResults(String queryString, int numberOfResults) {
        throw new UnsupportedOperationException(NO_CONNECTION);
//return searchEngine.getTopDocs(queryString, numberOfResults);
    }

    public Document getDoc(int id) {
        throw new UnsupportedOperationException(NO_CONNECTION);
//return searchEngine.getDoc(id);
    }

    public TermFreqVector getTermFreqVector(int docNumber, String field) {
        throw new UnsupportedOperationException(NO_CONNECTION);
//return searchEngine.getTermFreqVector(docNumber, field);
    }

    public IndexReader getIndexReader() {
        throw new UnsupportedOperationException(NO_CONNECTION);
//return searchEngine.getIndexReader();
    }

    public SlimCollector getSlimDocs(String query) {
        throw new UnsupportedOperationException(NO_CONNECTION);
/*        SlimCollector collector = getSlimCollector();
        if (collector == null) {
            return null;
        }
        if (searchEngine.searchWithCollector(query, collector)) {
            return collector;
        } else {
            releaseSlimCollector(collector);
            return null;
        } */
    }

    /**
     * SlimCollectors are made to be reused. When a SlimCollector is requested,
     * it is either taken from a pool or created if the pool is empty. When
     * the SlimCollector has been used, it can be returned by calling
     * {@link #releaseSlimCollector}. This resets the SlimCollector and puts
     * it back into the pool.<br />
     * Returning the collector and thereby reusing it later should be done in
     * order to avoid continuous allocations of new SlimCollectors. As these
     * collectors can take up a substantial amount of RAM (4*hitcount bytes),
     * this translates to significantly fewer garbage collections.
     * @return a SlimCollector, ready for use.
     */
    public SlimCollector getSlimCollector() {
        SlimCollector collector;
        poolLock.lock();
        try {
            try {
                if (collectorPool.size() == 0) {
                    log.info("Allocating new SlimCollector");
                    return new SlimCollector();
                } else {
                    log.trace("Reusing existing SlimCollector");
                    collector = collectorPool.remove(0);
                    return collector;
                }
            } catch (Exception e) {
                log.fatal("Could not allocate or remove a new SlimCollector",
                          e);
                return null;
            }
        } finally {
            poolLock.unlock();
        }
    }

    /**
     * Resets the given collector and put it back into the pool shared with
     * {@link #getSlimCollector}.
     * @param collector a SlimCollector that aren't used anymore.
     */
    public void releaseSlimCollector(SlimCollector collector) {
        poolLock.lock();
        try {
            collector.clean();
            log.trace("Putting SlimCollector back into pool");
            collectorPool.add(collector);
        } finally {
            poolLock.unlock();
        }
    }

}