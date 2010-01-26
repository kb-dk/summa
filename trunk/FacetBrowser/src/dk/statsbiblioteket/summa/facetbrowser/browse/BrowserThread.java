/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
/*
 * The State and University Library of Denmark
 * CVS:  $Id: BrowserThread.java,v 1.6 2007/10/05 10:20:22 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.browse;

import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResult;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Performs the primary work of mapping document IDs to tags.
 * Each BrowserThread creates a TagCounter, which requires a substantial amount
 * of memory (4 * #tags bytes). The counter is reused, so it is strongly adviced
 * to maintail a pool of Browserthreads and reuse them, to avoid heavy GCs.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
// TODO: Make this a Future
public class BrowserThread implements Runnable {
    private static Logger log = Logger.getLogger(BrowserThread.class);
    private CoreMap coreMap;
    private TagCounter tagCounter;

    private DocIDCollector docIDs;
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
    public synchronized void startRequest(DocIDCollector docIDs, int startPos,
                                          int endPos, Structure request) {
        lock.lock();
        try {
            this.docIDs = docIDs;
            this.startPos = startPos;
            this.endPos = endPos;
            this.request = request;
            thread = new Thread(
                    this, "BrowserThread_" + System.currentTimeMillis());
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
     *         given in {@link #startRequest}.
     *         null if an exception occured.
     */
    public synchronized FacetResult getResult() {
        if (!hasFinished()) {
            throw new IllegalStateException(
                    "It it not allowed to request the result before the request"
                    + " has been processed");
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
            log.trace("Verifying tagCounter structure");
            tagCounter.verify();
            try {
                coreMap.markCounterLists(tagCounter, docIDs, startPos, endPos);
//                System.out.println("After mark: " + tagCounter);
            } catch (Exception e) {
                log.error("Exception calling markCounterLists with "
                          + (docIDs == null ? "null" : docIDs.getDocCount())
                          + " docIDs, startPos " + startPos + ", " + endPos, e);
                //noinspection AssignmentToNull
                result = null; // There really is no result!
                return;
            }
            log.trace("run: Extracting result by calling tagCounter.getFirst");
            result = tagCounter.getFirst(request);
            log.trace("Getfirst completed successfully");
//            log.info("***" + result.toXML());
            try {
                tagCounter.reset(); // Clean-up
            } catch (Exception e) {
                log.error("Could not start clean up for the tagCounter: "
                          + e.getMessage(), e);
            }
        } catch (Exception e) {
            log.warn("Unhandled exception while extracting first tags", e);
        } finally {
            lock.unlock();
        }
    }
}

