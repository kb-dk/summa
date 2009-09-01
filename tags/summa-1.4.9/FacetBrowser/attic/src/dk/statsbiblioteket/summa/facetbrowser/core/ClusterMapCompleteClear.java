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
/*
 * The State and University Library of Denmark
 * CVS:  $Id$
 */
package dk.statsbiblioteket.summa.facetbrowser.core;

import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import dk.statsbiblioteket.summa.facetbrowser.core.ClusterMapCompleteThread;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Threaded background clean-up and preparation for future cluster calls.
 * @deprecated a suitable replacement is under development.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "te")
public class ClusterMapCompleteClear implements Runnable {
    private static Logger log =
            Logger.getLogger(ClusterMapCompleteClear.class);

    private ArrayList<ClusterMapCompleteThread> runnables;
    private ReentrantLock lock = new ReentrantLock();

    public ClusterMapCompleteClear(ArrayList<ClusterMapCompleteThread>
            runnables) {
        this.runnables = runnables;
    }

    /**
     * The method responsible for cleaning the counter lists.
     */
    public void run() {
        lock.lock();
        try {
//        Profiler pf = new Profiler();
//        log.info("Clearing");
            // This can be further threaded, but will the overhead be too high?
            // Arrays.fill is all about memory access, so for multithreading
            // to work, it requires efficient parallel memory access.
            for (ClusterMapCompleteThread clusterThread: runnables) {
                clusterThread.clearCounterLists();
            }
//            log.info("Finished clearing in " + pf.getSpendTime());
        } catch (Exception e) {
            log.error("Exception clearing counter lists. Cluster mapping " +
                      "will probably give imprecise results: " +
                      e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Ensure that the last optimise has finished.
     */
    public void waitForCleanup() {
        if (lock.isLocked()) {
            lock.lock();
            lock.unlock();
        }
    }

    /**
     * Start a new optimise thread and returns immediately.
     */
    public void cleanup() {
        new Thread(this).start();
    }
}


