/* $Id: BrowserThread.java,v 1.6 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.6 $
 * $Date: 2007/10/05 10:20:22 $
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: BrowserThread.java,v 1.6 2007/10/05 10:20:22 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.browse;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

/**
 * Performs the primary work of mapping document IDs to tags.
 * Each BrowserThread creates a TagCounter, which requires a substantial amount
 * of memory (4 * #tags bytes). The counter is reused, so it is strongly adviced
 * to maintail a pool of Browserthreads and reuse them, to avoid heavy GCs.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class BrowserThread implements Runnable {
    private static Logger log = Logger.getLogger(BrowserThread.class);
    private CoreMap coreMap;
    private TagCounter tagCounter;

    private int[] docIDs;
    private int startPos;
    private int endPos;
    private Structure request;
    private FacetResult result = null;

    private ReentrantLock lock = new ReentrantLock();
    private Thread thread = null;

    public BrowserThread(TagHandler tagHandler, CoreMap coreMap) {
        this.coreMap = coreMap;
        tagCounter = new TagCounterArray(tagHandler, coreMap.getEmptyFacet());
    }

    /**
     * Starts a calculation thread and returns immediately. Use
     * {@link #hasFinished()} and {@link #getResult()} for further controls.
     * Note that this method isn't thread-safe.
     * @param docIDs   the document IDs from which to extract the structure.
     * @param startPos the starting position for the IDs to use (inclusive).
     * @param endPos   the end position for the IDs to use (inclusive).
     * @param request  details on the facets to return.
     */
    public synchronized void startRequest(int[] docIDs, int startPos,
                                          int endPos, Structure request) {
        lock.lock();
        try {
            this.docIDs = docIDs;
            this.startPos = startPos;
            this.endPos = endPos;
            this.request = request;
            thread = new Thread(this);
            thread.run();
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return true if the request has been processed and a result is ready.
     */
    public boolean hasFinished() {
        return !lock.isLocked();
    }

    /**
     * @return a FacetStructure with facets and tags, derived from the data
     *         given in {@link #startRequest(int[], int, int, Structure)}.
     *         null if an exception occured.
     */
    public synchronized FacetResult getResult() {
        if (!hasFinished()) {
            throw new IllegalStateException("It it not allowed to request the "
                                            + "result before the request has "
                                            + "been processed");
        }
        return result;
    }

    /**
     * Wait for the result.
     * @param timeout timeout in milliseconds.
     */
    public void waitForResult(long timeout) {
        if (thread == null) {
            return;
        }
        long endTime = System.currentTimeMillis() + timeout;
        while (!hasFinished() && endTime > System.currentTimeMillis()) {
            try {
                lock.tryLock(timeout, TimeUnit.MILLISECONDS);
                if (lock.isLocked()) {
                    lock.unlock();
                    return;
                }
//                thread.wait(timeout);
            } catch (InterruptedException e) {
                log.debug("InterruptedException while waiting for thread to"
                          + " finish");
            }
        }
    }

    /**
     * Fills the tagCounter, generates a result and starts a reset of the
     * tagCounter.
     */
    public void run() {
        lock.lock();
        try {
            try {
                coreMap.markCounterLists(tagCounter, docIDs, startPos, endPos);
            } catch (Exception e) {
                log.error("Exception calling markCounterLists with "
                          + (docIDs == null ? "null" : docIDs.length) + " docIDs, "
                          + "startPos " + startPos + ", " + endPos, e);
                //noinspection AssignmentToNull
                result = null; // There really is no result!
                return;
            }
            result = tagCounter.getFirst(request);
            try {
                tagCounter.reset(); // Clean-up
            } catch (Exception e) {
                log.error("Could not start clean up for the tagCounter: "
                          + e.getMessage(), e);
            }
        } finally {
            lock.unlock();
        }
    }
}
