/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.util.ChangingSemaphore;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;

/**
 * Handles the logic of controlling concurrent searches, open, close and
 * warm-up. This is a convenience class for building SearchNodes.
 */
// TODO: Abort warmup if an open is issued while an old open is running
// TODO: Make it optional if searches are permitted before warmup is finished
public abstract class SearchNodeImpl implements SearchNode {
    private static Log log = LogFactory.getLog(SearchNodeImpl.class);

    /**
     * The maximum number of concurrent searches for this node.
     * </p><p>
     * This is optional. Default is 2.
     */
    public static final String CONF_NUMBER_OF_CONCURRENT_SEARCHES =
            "summa.search.numberofconcurrentsearches";
    public static final int DEFAULT_NUMBER_OF_CONCURRENT_SEARCHES = 2;

    /**
     * A resource with queries (newline-delimited) that should be expanded
     * and searched every time an index is (re)opened. Not all SearchNodes
     * supports warm-up.
     * </p><p>
     * This is optional. If not specified, no warm-up is performed.
     * @see {@link #CONF_WARMUP_MAXTIME}.
     */
    public static final String CONF_WARMUP_DATA =
            "summa.search.warmup.data";
    public static final String DEFAULT_WARMUP_DATA = null;

    /**
     * The maximum number of milliseconds to spend on warm-up. If all queries
     * specified in {@link #CONF_WARMUP_DATA} has been processed before this
     * time limit, the warmup-phase is exited.
     * </p><p>
     * This is optional. Default is 30 seconds (30,000 milliseconds).
     */
    public static final String CONF_WARMUP_MAXTIME =
            "summa.search.warmup.maxtime";
    public static final int DEFAULT_WARMUP_MAXTIME = 1000 * 30;

    /**
     * If true, the implementating SearchNode is capable of performing searches
     * during open. The obvious exemption is the first open.
     * </p><p>
     * This is optional. Default is false.
     */
    public static final String CONF_SEARCH_WHILE_OPENING =
            "summa.search.searchwhileopening";
    public static final boolean DEFAULT_SEARCH_WHILE_OPENING = false;

    /**
     * If no searchers are ready upon search, wait up to this number of
     * milliseconds for a searcher to become ready. If no searchers are ready
     * at that time, an exception will be thrown.
     */
    public static final String CONF_SEARCHER_AVAILABILITY_TIMEOUT =
            SummaSearcherImpl.CONF_SEARCHER_AVAILABILITY_TIMEOUT;
    public static final int DEFAULT_SEARCHER_AVAILABILITY_TIMEOUT =
            SummaSearcherImpl.DEFAULT_SEARCHER_AVAILABILITY_TIMEOUT;

    /**
     * The size in bytes of the buffer used when retrieving warmup-data.
     */
    private static final int BUFFER_SIZE = 8192;
    private static final int WARMUP_TIMEOUT = 10;
    private static final int OPEN_TIMEOUT = 60 * 1000;
    private static final int CLOSE_TIMEOUT = 60 * 1000;
    private int searcherAvailabilityTimeout =
            DEFAULT_SEARCHER_AVAILABILITY_TIMEOUT;

    private int concurrentSearches = DEFAULT_NUMBER_OF_CONCURRENT_SEARCHES;
    private String warmupData = DEFAULT_WARMUP_DATA;
    private int warmupMaxTime = DEFAULT_WARMUP_MAXTIME;
    private boolean searchWhileOpening = DEFAULT_SEARCH_WHILE_OPENING;
    private ChangingSemaphore slots = new ChangingSemaphore(0);

    public SearchNodeImpl(Configuration conf) {
        log.trace("Constructing SearchNodeImpl");
        concurrentSearches = conf.getInt(CONF_NUMBER_OF_CONCURRENT_SEARCHES,
                                         concurrentSearches);
        warmupData = conf.getString(CONF_WARMUP_DATA, warmupData);
        warmupMaxTime = conf.getInt(CONF_WARMUP_MAXTIME, warmupMaxTime);
        searchWhileOpening = conf.getBoolean(CONF_SEARCH_WHILE_OPENING,
                                             searchWhileOpening);
        searcherAvailabilityTimeout =
                conf.getInt(CONF_SEARCHER_AVAILABILITY_TIMEOUT,
                            searcherAvailabilityTimeout);
        log.debug(String.format(
                "Constructed SearchNodeImpl with concurrentSearches %d, "
                + "warmupData '%s', warmupMaxTime %d, searchWhileOpening %s",
                concurrentSearches, warmupData, warmupMaxTime,
                searchWhileOpening));
    }

    /**
     * Fetches warm-up data from the location specified by
     * {@link #CONF_WARMUP_DATA} and calls {@link #warmup(String)} with each
     * line in the data.
     * </p><p>
     * The warmup-method never throws an exception, as warming is considered
     * a non-critical event.
     */
    public void warmup() {
        log.trace("warmup() called");
        if (warmupData == null || "".equals(warmupData)) {
            log.trace("No warmup-data defined. Skipping warmup");
            return;
        }
        if (slots.getOverallPermits() == 0) {
            log.warn("No warmup as no permits are available");
            return;
        }
        log.trace("Warming up with data from '" + warmupData + "'");
        long startTime = System.currentTimeMillis();
        long endTime = startTime + warmupMaxTime;
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
                warmup(query);
                searchCount++;
            }
            log.debug("Warmup finished with warm-up data from '" + warmupData
                      + "' in " + (System.currentTimeMillis() - startTime)
                      + " ms and " + searchCount + " searches");
        } catch (RemoteException e) {
            log.error(String.format(
                    "RemoteException performing warmup with "
                    + "data from '%s'", warmupData), e);
        } catch (IOException e) {
            log.warn("Exception reading the content from '" + warmupData
                     + "' for warmup", e);
        } catch (Exception e) {
            log.error(String.format(
                    "Exception performing warmup with data from '%s'",
                    warmupData), e);
        }
    }

    /**
     * Performs a warm-up with the given request. This could be a single query
     * for a DocumentSearcher or a word for a did-you-mean searcher.
     * @param request implementation-specific warmup-data.
     */
    public void warmup(String request) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("warmup(" + request + ") called");
        try {
            if (slots.tryAcquire(1, WARMUP_TIMEOUT, TimeUnit.MILLISECONDS)) {
                managedWarmup(request);
            } else {
                log.debug("Skipped warmup(" + request + ") due to timeout");
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for free slot in warmup", e);
        } finally {
            slots.release();
        }
    }

    /**
     * A managed version of {@link SearchNode#warmup(String)}. Implementations
     * are free to ignore threading and locking-issues.
     * @param request as specified in {@link SearchNode#warmup(String)}.
     */
    protected abstract void managedWarmup(String request);

    public synchronized void open(String location) throws RemoteException {
        log.trace("open called for location '" + location + "'");
        syncOpen(location);
        warmup();
        log.trace("open finished for location '" + location + "'");
    }
    private synchronized void syncOpen(String location) throws RemoteException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("syncOpen(" + location + ") called");
        if (slots.getOverallPermits() != 0 && !searchWhileOpening) {
            try {
                log.trace("open: acquiring " + slots.getOverallPermits()
                          + " slots");
                if (!slots.tryAcquire(slots.getOverallPermits(), OPEN_TIMEOUT,
                                      TimeUnit.MILLISECONDS)) {
                    //noinspection DuplicateStringLiteralInspection
                    log.warn(String.format(
                            "open(%s): Unable to acquire all slots within %d "
                            + "milliseconds. Re-creating slot-semaphore",
                            location, OPEN_TIMEOUT));
                    slots = new ChangingSemaphore(0);
                }
            } catch (InterruptedException e) {
                throw new RemoteException("Interrupted while acquiring all "
                                          + "slots for open", e);
            }
        }
        if (location == null) {
            log.info("Location was null in open. Closing searcher");
            managedClose();
            slots.setOverallPermits(0);
            return;
        }
        log.trace("open: calling managedOpen(" + location + ")");
        managedOpen(location);
        if (slots.getOverallPermits() == 0) {
            slots.setOverallPermits(concurrentSearches);
        } else if (!searchWhileOpening) {
            slots.release(slots.getOverallPermits());
        }
        log.trace("syncOpen finished");
    }

    /**
     * A managed version of {@link SearchNode#open(String)}. Implementations
     * are free to ignore threading and locking-issues.
     * @param location as specified in {@link SearchNode#open(String)}.
     * @throws RemoteException if the index could not be opened.
     */
    protected abstract void managedOpen(String location) throws RemoteException;

    public int getFreeSlots() {
        return slots.availablePermits();
    }

    public synchronized void close() throws RemoteException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("close() called");
        try {
            if (log.isTraceEnabled()) {
                log.trace(String.format(
                        "close: acquiring %d slots",
                        slots.getOverallPermits()));
            }
            if (!slots.tryAcquire(slots.getOverallPermits(), CLOSE_TIMEOUT,
                                  TimeUnit.MILLISECONDS)) {
                //noinspection DuplicateStringLiteralInspection
                log.warn(String.format(
                        "close: Unable to acquire all slots within %d "
                        + "milliseconds. Re-creating slot-semaphore",
                        CLOSE_TIMEOUT));
                slots = new ChangingSemaphore(0);
            } else {
                slots.setOverallPermits(0);
            }
        } catch (InterruptedException e) {
            slots.setOverallPermits(0);
            throw new RemoteException(
                    "Interrupted while acquiring all slots for close", e);
        }

        try {
            managedClose();
        } catch(RemoteException e) {
            throw new RemoteException("close: Exception calling managedClose",
                                      e);
        }
    }

    /**
     * A managed version of {@link SearchNode#close()}. Implementations are free
     * to ignore threading and locking-issues.
     * @throws RemoteException if there was an exception closing.
     */
    protected abstract void managedClose() throws RemoteException;

    public void search(Request request, ResponseCollection responses) throws
                                                               RemoteException {
        log.trace("search called");
        try {
            // TODO: Consider timeout for slot acquirement
            if (!slots.tryAcquire(1, searcherAvailabilityTimeout,
                                  TimeUnit.MILLISECONDS)) {
                throw new RemoteException(String.format(
                        "Time-limit of %d milliseconds exceeded",
                        searcherAvailabilityTimeout));
            }
        } catch (InterruptedException e) {
            throw new RemoteException("Interruped while waiting for free slot "
                                      + "for search");
        }
        try {
            managedSearch(request, responses);
        } finally {
            slots.release();
        }
    }

    /**
     * A managed version of
     * {@link SearchNode#search(Request, dk.statsbiblioteket.summa.search.api.ResponseCollection)} open(String)}.
     * Implementations are free to ignore threading and locking-issues.
     * @param request   as specified in
     *                  {@link SearchNode#search(Request, dk.statsbiblioteket.summa.search.api.ResponseCollection)}
     * @param responses as specified in
     *                  {@link SearchNode#search(Request, ResponseCollection)}
     * @throws RemoteException as specified in
     *                  {@link SearchNode#search(dk.statsbiblioteket.summa.search.api.Request , ResponseCollection)}
     */
    protected abstract void managedSearch(Request request,
                                          ResponseCollection responses) throws
                                                                RemoteException;

    /* Mutators */

    public String getWarmupData() {
        return warmupData;
    }
    public void setWarmupData(String warmupData) {
        log.debug(String.format("setWarmupData(%s) called", warmupData));
        this.warmupData = warmupData;
    }

    public int getWarmupMaxTime() {
        return warmupMaxTime;
    }
    public void setWarmupMaxTime(int warmupMaxTime) {
        log.debug(String.format("setWarmupMaxTime(%d ms) called",
                                warmupMaxTime));
        this.warmupMaxTime = warmupMaxTime;
    }

    public int getMaxConcurrentSearches() {
        return concurrentSearches;
    }
    public synchronized void setMaxConcurrentSearches(
                                                    int maxConcurrentSearches) {
        log.debug(String.format("setMaxConcurrentSearches(%d) called",
                                maxConcurrentSearches));
        concurrentSearches = maxConcurrentSearches;
        if (slots.getOverallPermits() != 0) { // Race-condition here?
            slots.setOverallPermits(concurrentSearches);
        }
    }
}



