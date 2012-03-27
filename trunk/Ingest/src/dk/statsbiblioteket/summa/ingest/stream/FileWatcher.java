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
package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * The FileWatcher is a persistent wrapper for the FileReader.
 * When there are no more files to read, the next() and the hasNext() methods
 * blocks until new files are discovered or until close is called.
 * </p><p>
 * Note: This filter is not synchronized.
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

    private static final Payload END_PAYLOAD =
            new Payload(new Record("DummyPayload", "DummyBase", new byte[0]));

    private int pollInterval = DEFAULT_POLL_INTERVAL;
    private Thread fileWatcherThread = null;
    private boolean doRun = true;
    private Payload toDeliver = null;

    public FileWatcher(Configuration conf) {
        super(conf);
        log.debug("Creating FileWatcher");
        pollInterval = conf.getInt(CONF_POLL_INTERVAL, pollInterval);
        log.trace("FileWatcher created with pollInterval " + pollInterval);
        if (fileWatcherThread == null) {
            log.debug("Starting fileWatcherThread");
            fileWatcherThread = new Thread(
                    this, "FileWatcher(" + pollInterval + " seconds)");
            fileWatcherThread.start();
        }
    }

    @Override
    public boolean hasNext() {
        log.trace("hasNext() entered");
        if (toDeliver != null) {
            //noinspection ObjectEquality
            return toDeliver != END_PAYLOAD;
        }
        log.trace("hasNext(): polling todo");
        toDeliver = getNextBlocking();
        log.trace("hasNext(): polling finished. Got " + toDeliver);
        if (toDeliver == null) {
            throw new IllegalStateException(
                    "The FileWatcher should never get null for delivery");
        }
        return toDeliver != END_PAYLOAD;
    }

    /**
     * Waits until the FileWatcher is stopped or there is something in the to do
     * list.
     * @return {@link #END_PAYLOAD} to mark thet the FileWatcher has stopped or
     *         the next file.
     */
    private Payload getNextBlocking() {
        // TODO: Change this to a blocking call instead of busy wait
        log.trace("getNextBlocking() entered with doRun==" + doRun);
        while (doRun) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting in getNextBlocking",
                         e);
            }
            if (super.hasNext()) {
                return super.next();
            }

        }
        log.debug("getNextBlocking() exited with doRun==" + doRun);
        return END_PAYLOAD;
    }

    @Override
    public Payload next() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("next() called");
        if (!hasNext()) {
            throw new NoSuchElementException(
                    "No more streams in FileWatcher");
        }
        log.trace("next(): toDeliver set to '" + toDeliver + "'");
        Payload payload = toDeliver;
        toDeliver = null;
        return payload;
    }

    @Override
    public boolean pump() throws IOException {
        if (hasNext()) {
            Payload payload = next();
            //noinspection DuplicateStringLiteralInspection
            Logging.logProcess("FileWatcher",
                               "Calling close for Payload as part of pump()",
                               Logging.LogLevel.TRACE, payload);
            payload.close();
            return true;
        }
        return false;
    }

    @Override
    public void close(boolean success) {
        log.debug(String.format("close(%b) called", success));
        doRun = false;
        super.close(success);
        toDeliver = END_PAYLOAD;
    }

    public void run() {
        log.trace("Entering run() with doRun==" + doRun);
        try {
            while (doRun) {
                try {
                    Thread.sleep(pollInterval);
                    if (doRun) {
                        if (!super.hasNext()) {
                            if (doRun) {
                                log.trace("run(): Updating toDo");
                                updateToDo(root);
                                if (log.isTraceEnabled()) {
                                    log.trace("todo is "+  (isTodoEmpty() ? 
                                                            "empty" :
                                                            "not empty"));
                                }
                            }
                        } else {
                            log.trace("run(): Exited after super.hasNext() "
                                      + "with doRun==" + doRun);
                        }
                    } else {
                        log.trace("run(): Exited after sleep with doRun=="
                                  + doRun);
                    }
                } catch (InterruptedException e) {
                    log.error("Interrupted while sleeping for " + pollInterval
                              + " ms in FileWatcher. Retrying sleep", e);
                }
            }
        } catch (Exception e) {
            log.error("Unrecoverable exception in run(). Exiting", e);
        }
        log.debug("run() finished, FileWatcher is stopping with doRun=="
                  + doRun);
    }
}

