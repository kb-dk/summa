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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.rmi.RemoteException;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Handles the logic of opening new indexes without disturbing the running
 * searches.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SearchNodeWrapper implements SearchNode {
    private static Log log = LogFactory.getLog(SearchNodeWrapper.class);

    /**
     * The number of milliseconds to wait between each poll for the status
     * of active searches when opening or closing the index.
     */
    private static final int BUSY_WAIT_MS = 10;

    private boolean ready = false;
    private SearchNode node;
    private String location;
    private AtomicInteger activeSearches = new AtomicInteger();

    public SearchNodeWrapper(SearchNode node) {
        log.trace("Constructing SearchNodeWrapper for " + node);
        this.node = node;
        ready = true;
    }

    /**
     * Marks the search node as not ready, waits for pending searches to finish,
     * opens the index at the stated location and marks the node as ready.
     * As opening an index might involve warming up, this method might take
     * some time to finish.
     * @param location     where the index can be found.
     * @throws IOException if the index could not be opened.
     */
    public synchronized void open(String location) throws IOException {
        ready = false;
        log.trace("open called for location '" + location + "'");
        waitForSearches();
        try {
            node.open(location);
        } catch (IOException e) {
            throw new IOException("Could not open underlying search node for"
                                  + " location '" + location + "'", e);
        }
        this.location = location;
        ready = true;
        log.trace("open finished for location '" + location + "'");
    }

    private void waitForSearches() {
        while (activeSearches.get() > 0) {
            try {
                Thread.sleep(BUSY_WAIT_MS);
            } catch (InterruptedException e) {
                log.warn("open: Sleep was interrupted. Ignoring interruption",
                         e);
            }
        }
    }

    /**
     * Marks the search node as not ready, waits for pending searches to finish
     * and closes any open connection to index resources. When this method
     * returns, it should be safe to remove the search node from the collection.
     */
    public synchronized void close() {
        ready = false;
        log.trace("close() called for location '" + location + "'");
        waitForSearches();
        node.close();
        log.trace("close() finished for location '" + location + "'");
    }

    /**
     * @return true if an index is opened and ready for searching.
     *              Note that is is allowed for a search node to be not ready
     *              while searches are running.
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * @return the number of currently running searches.
     */
    public int getActive() {
        return activeSearches.get();
    }

    /*
     * Throws a RemoteException if the wrapper is not ready and keepe track of
     * concurrent searches. This method is expected to be called by different
     * threads.
     */
    public String fullSearch(String filter, String query, long startIndex,
                             long maxRecords, String sortKey,
                             boolean reverseSort, String[] fields,
                             String[] fallbacks) throws RemoteException {
        if (!ready) {
            throw new RemoteException("Not ready for searching");
        }
        activeSearches.incrementAndGet();
        try {
            return node.fullSearch(filter, query, startIndex, maxRecords,
                                   sortKey, reverseSort, fields, fallbacks);
        } finally {
            activeSearches.decrementAndGet();
        }
    }
}
