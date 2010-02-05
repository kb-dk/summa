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
package dk.statsbiblioteket.summa.storage.database.cursors;

import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper class to clean up unused iterators. It should be started
 * via the runInThread() method
 */
public class CursorReaper implements Runnable {

    private static final Log log = LogFactory.getLog(CursorReaper.class);

    private Map<Long, Cursor> iterators;
    private Thread t;
    private boolean mayRun;
    private long graceTimeMinutes;

    public CursorReaper(Map<Long, Cursor> iterators,
                        long graceTimeMinutes) {
        this.iterators = iterators;
        this.graceTimeMinutes = graceTimeMinutes;
        mayRun = true;
    }

    public void runInThread () {
        t = new Thread(this, "CursorReaper");
        t.setDaemon(true); // Allow JVM to exit when the reaper is running
        t.start();
    }

    public void stop () {
        log.debug("Got stop request. Closing all iterators");

        for (Cursor iter : iterators.values()) {
            iter.close();
        }

        mayRun = false;
        t.interrupt();
    }

    public void run() {
        log.info("Starting");
        while (mayRun) {
            try {
                Thread.sleep(graceTimeMinutes*60*1000);
                fullSweep();
            } catch (InterruptedException e) {
                log.info("Interrupted");
            }
        }
        log.info("Stopped");
    }

    private void fullSweep() {
        log.debug("Scanning iterators for timeouts");

        long now = System.currentTimeMillis();
        List<Long> deadIters = new ArrayList<Long>();

        for (Cursor iter : iterators.values()) {
            if (iter.getLastAccess() + graceTimeMinutes*60*1000 <= now) {
                deadIters.add(iter.getKey());
            }
        }

        for (Long key : deadIters) {
            Cursor iter = iterators.remove(key);
            iter.close();
            log.info("Iterator " + iter.getKey() + " timed out");
        }

        log.debug("Scan complete");
    }
}

