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
package dk.statsbiblioteket.summa.common.util;

import java.net.URL;
import java.io.IOException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Periodically fetches the content of a given URL and calls
 * {@link #resourceChanged} when the content changes.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class ResourceListener implements Runnable {
    private static Log log = LogFactory.getLog(ResourceListener.class);

    private URL location;
    private long interval = -1;
    private boolean active = true;
    private String oldContent;

    /**
     * Create a listener for the given location.
     * @param location where to check for new content.
     * @param interval how many milliseconds to sleep before checking again.
     * @param active   if true, active checking is performed. If false, the user
     *                 of the class has to call {@link #performCheck()}.
     */
    public ResourceListener(URL location, long interval, boolean active) {
        this.location = location;
        setInterval(interval);
        setActive(active);
    }

    public void run() {
        log.trace("Run entered");
        while (active && interval >= 0) {
            performCheck();
            if (interval > 0) {
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    log.error("Interrupted while sleeping in run. Aborting "
                              + "auto-check");
                    setActive(false);
                }
            }
        }
    }

    private Exception lastException;
    /**
     * Performs a check for new content and calls {@link #resourceChanged} if
     * the content has changed. This method will never throw any exceptions.
     * If problems occur during check, the last exception can be accessed by
     * {@link #getLastException} and false is returned.
     * @return true if the check resulted in new content and resourceChanged was
     * called without problems.
     */
    public boolean performCheck() {
        log.trace("performCheck() called");
        String newContent;
        try {
            newContent = Resolver.getUTF8Content(location);
        } catch (IOException e) {
            lastException = new IOException("Exception fetching '" + location
                                            + "'", e);
            return false;
        }
        try {
            if (oldContent == null || !oldContent.equals(newContent)) {
                log.debug("performCheck got new content");
                oldContent = newContent;
                resourceChanged(newContent);
                log.trace("resourceChanged called successfully");
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            lastException = new Exception("Exception calling resourceChanged",
                                          e);
        }
        return false;
    }

    /**
     * Handle changes to content at the location specified in the constructor.
     * @param newContent the current content at the location.
     * @throws Exception if there was a problem handling the content.
     */
    public abstract void resourceChanged(String newContent) throws Exception;

    /* Mutators */

    /**
     * @return how frequently (in miliseconds) checks for changes are performed.
     */
    public long getInterval() {
        return interval;
    }

    /**
     * How often the listener should check for updates. Settings this to -1
     * disables checking and makes {@link #isActive()} return false.
     * @param interval the wait between checks in milliseconds.
     */
    public void setInterval(long interval) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("setInterval(" + interval + ") called");
        this.interval = interval;
        if (interval == -1) {
            setActive(false);
        }
    }

    /**
     * @return true if the listener performs active checks.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Whether or not the listener should perform auto-checking for changes.
     * @param active true is autp-checking should be performed.
     */
    public synchronized void setActive(boolean active) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("setActive(" + active + ") called");
        boolean oldActive = this.active;
        this.active = active;
        if (active && oldActive) {
            log.debug("setActive: already active");
            return;
        }
        if (!active && !oldActive) {
            log.debug("Already inactive");
            return;
        }
        if (active && !oldActive) {
            log.debug("Starting new listener thread");
            new Thread(this).start();
            return;
        }
        //noinspection DuplicateStringLiteralInspection
        log.debug("Signalling stop");
    }

    public Exception getLastException() {
        return lastException;
    }
}



