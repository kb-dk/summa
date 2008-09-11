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
package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * The FileWatcher is a persistent wrapper for the FileReader.
 * When there are no more files to read, the next() and the hasNext() methods
 * blocks until new files are discovered or until close is called.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FileWatcher extends FileReader implements Runnable {
    private static Log log = LogFactory.getLog(FileWatcher.class);

    /**
     * The interval in milliseconds between checks for new files. Note that
     * a check potentially involves a large recursive listing of files,
     * so setting this value very low might cost a lot of resources.
     * </p><p>
     * Optional. Default is 60,000 milliseconds (1 minute).
     */
    public static final String CONF_POLL_INTERVAL =
            "summa.ingest.filewatcher.pollinterval";
    public static final int DEFAULT_POLL_INTERVAL = 60 * 1000;

    private static final File END_FILE = new File("DummyFileMarkingEnd");

    private int pollInterval = DEFAULT_POLL_INTERVAL;
    private Thread fileWatcherThread = null;
    private boolean doRun = true;
    private File toDeliver = null;

    public FileWatcher(Configuration conf) {
        super(conf);
        log.debug("Creating FileWatcher");
        pollInterval = conf.getInt(CONF_POLL_INTERVAL, pollInterval);
        log.trace("FileWatcher created with pollInterval " + pollInterval);
    }

    /* One-time setup */
    protected void checkInit() {
        if (started) {
            return;
        }
        super.checkInit();
        if (fileWatcherThread == null) {
            log.debug("Starting fileWatcherThread");
            fileWatcherThread = new Thread(this);
            fileWatcherThread.start();
        }
    }

    public synchronized boolean hasNext() {
        log.trace("hasNext() entered");
        checkInit();
        if (toDeliver != null) {
            //noinspection ObjectEquality
            return toDeliver != END_FILE;
        }
        log.trace("hasNext(): polling todo");
        toDeliver = getNextFromTodo();
        log.trace("hasNext(): polling finished. Got " + toDeliver);
        if (toDeliver == null) {
            throw new IllegalStateException("The FileWatcher should never get "
                                            + "null for delivery");
        }
        return toDeliver != END_FILE;
    }


    public synchronized Payload next() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("next() called");
        checkInit();
        if (!hasNext()) {
            throw new NoSuchElementException("No more streams in FileWatcher");
        }
        log.trace("next(): toDeliver set to '" + toDeliver + "'");
        File file = toDeliver;
        toDeliver = null;
        return deliverFile(file);
    }

    public synchronized boolean pump() throws IOException {
        if (hasNext()) {
            next().close();
            return true;
        }
        return false;
    }

    public void close(boolean success) {
        doRun = false;
        super.close(success);
        toDeliver = END_FILE;
    }

    public void run() {
        try {
            while (doRun) {
                try {
                    Thread.sleep(pollInterval);
                    if (doRun) {
                        log.trace("run(): Updating toDo");
                        updateToDo(root);
                    }
                } catch (InterruptedException e) {
                    log.error("Interrupted while sleeping for " + pollInterval
                              + " ms in FileWatcher. Retrying sleep", e);
                }
            }
        } catch (Exception e) {
            log.error("Unrecoverable exception in run(). Exiting", e);
        }
    }
}
