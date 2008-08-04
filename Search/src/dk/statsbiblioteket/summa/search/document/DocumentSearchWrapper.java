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
package dk.statsbiblioteket.summa.search.document;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;
import java.rmi.RemoteException;
import java.net.URL;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.search.SearchNode;
import dk.statsbiblioteket.summa.search.SummaSearcherMBean;
import dk.statsbiblioteket.summa.search.SummaSearcher;
import dk.statsbiblioteket.summa.search.SearchResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Handles the logic of opening new document searchers without disturbing the
 * running searches.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DocumentSearchWrapper implements SearchNode {
    private static Log log = LogFactory.getLog(DocumentSearchWrapper.class);

    /**
     * The number of milliseconds to wait between each poll for the status
     * of active searches when opening or closing the index.
     */
    private static final int BUSY_WAIT_MS = 10;

    /**
     * The size in bytes of the buffer used when retrieving warmup-data.
     */
    private static final int BUFFER_SIZE = 8192;

    private SummaSearcherMBean master;
    private boolean ready = false;
    private SearchNode node;
    private String location;
    private AtomicInteger activeSearches = new AtomicInteger();

    @SuppressWarnings({"UnusedDeclaration"})
    public DocumentSearchWrapper(SummaSearcherMBean master, Configuration conf,
                             SearchNode node) {
        log.trace("Constructing DocumentSearchWrapper for " + node);
        this.master = master;
        this.node = node;
        ready = true;
    }

    /**
     * Marks the search node as not ready, waits for pending searches to finish,
     * opens the index at the stated location and marks the node as ready.
     * As opening an index might involve warming up, this method might take
     * some time to finish.
     * </p><p>
     * If is not necessary to call close() before open().
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
        warmup();
        ready = true;
        log.trace("open finished for location '" + location + "'");
    }

    public synchronized void warmup() throws RemoteException {
        String warmupData = master.getWarmupData();
        if (warmupData == null || "".equals(warmupData)) {
            log.trace("No warmup-data defined. Skipping warmup");
            return;
        }
        log.trace("Warming up '" + location + "' with data from '"
                  + warmupData + "'");
        long startTime = System.currentTimeMillis();
        long endTime = startTime + master.getWarmupMaxTime();
        try {
            long searchCount = 0;
            URL warmupDataURL = Resolver.getURL(warmupData);
            if (warmupDataURL == null) {
                log.warn("Could not resolve '" + warmupDataURL
                         + "' to an URL. Skipping warmup");
                return;
            }
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(warmupDataURL.openStream()),
                    BUFFER_SIZE);
            String query;
            while ((query = in.readLine()) != null &&
                   System.currentTimeMillis() < endTime) {
                // TODO: Add sorting-calls to warmup
                node.warmup(query, null, null);
                searchCount++;
            }
            log.debug("Warmup finished for location '" + location
                      + "' with warm-up data from '" + warmupData
                      + "' in " + (System.currentTimeMillis() - startTime)
                      + " ms and " + searchCount + " searches");
        } catch (RemoteException e) {
            log.error(String.format(
                    "RemoteException performing warmup for index at '%s' with "
                    + "data from '%s'", location, warmupData));
        } catch (IOException e) {
            log.warn("Exception reading the content from '" + warmupData
                     + "' for warmup for location '" + location + "'");
        } catch (Exception e) {
            log.error(String.format(
                    "Exception performing warmup for index at '%s' with data "
                    + "from '%s'", location, warmupData));
        }
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
    public synchronized void close() throws RemoteException {
        ready = false;
        log.trace("close() called for location '" + location + "'");
        waitForSearches();
        node.close();
        log.trace("close() finished for location '" + location + "'");
    }

    public void warmup(String query, String sortKey, String[] fields) {
        log.warn("The warmup(String, String, String[]) method should not be "
                 + "called directly on the wrapper");
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

    /**
     * Throws a RemoteException if the wrapper is not ready and keeps track of
     * concurrent searches. This method is expected to be called by different
     * threads.
     * @param filter      a query that narrows the search.
     * @param query       a query as entered by a user.
     * @param startIndex  the starting index for the result, counting from 0.
     * @param maxRecords  the maximum number of records to return.
     * @param sortKey     specifies how to sort.
     * @param reverseSort if true, the sort is performed in reverse order.
     * @param fields      the fields to extract content from.
     * @param fallbacks   if the value of a given field cannot be extracted,
     *                    the corresponding value from fallbacks is returned.
     * @return the result of a search in XML.
     * @throws RemoteException if there was an exception during search.
     * @see {@link SummaSearcher#fullSearch(String, String, long, long, String,
     * boolean, String[], String[])} for full syntax.
     */
    public SearchResult fullSearch(String filter, String query, long startIndex,
                                   long maxRecords, String sortKey,
                                   boolean reverseSort, String[] fields,
                                   String[] fallbacks) throws RemoteException {
        if (!ready) {
            // TODO: Not good! Block instead until ready
            throw new RemoteException("Not ready for searching");
        }
        activeSearches.incrementAndGet();
        try {
            if (maxRecords > master.getMaxRecords()) {
                log.warn("fullSearch requested " + maxRecords
                         + " max records, with only " + master.getMaxRecords()
                         + " allowed. Delivering a max of "
                         + master.getMaxRecords());
                maxRecords = master.getMaxRecords();
            }
            return node.fullSearch(filter, query, startIndex, maxRecords,
                                   sortKey, reverseSort, fields, fallbacks);
        } finally {
            activeSearches.decrementAndGet();
        }
    }

    public String simpleSearch(String query, long startIndex,
                               long maxRecords) throws RemoteException {
        return fullSearch(null, query, startIndex, maxRecords,
                          null, false, null, null).toXML();
    }
}
