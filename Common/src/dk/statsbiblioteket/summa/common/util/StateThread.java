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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple extension of Runnable that allows for status and resuming after stop.
 * </p><p>
 * Implementers should query {@link #getStatus} periodically and stop executing
 * if the result is not STATUS.running. Note that the runMethod might be
 * called again after stop. If this is undesired, the implementation must take
 * steps to skip execution for subsequent calls.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class StateThread implements Runnable {
    private final Log log = LogFactory.getLog(StateThread.class.getName()
                                       + "#" + this.getClass().getSimpleName());

    /**
     * ready:    Ready to run for the first time since creation.<br />
     * running:  As the name says.<br />
     * stopping: Attempting to stop running.<br />
     * stopped:  The run has stopped.<br />
     * error:    An exception occured and the StateThread is in an unstable
     *           state. Further calls will probably fail.
     */
    public static enum STATUS {ready, running, stopping, stopped, error}

    // TODO: Consider reusing the Status from the Control framework
    private STATUS status = STATUS.ready;

    private String errorMessage;

    private Throwable errorThrowable;
    private Thread thread;

    public STATUS getStatus() {
        if (status == STATUS.running || status == STATUS.stopping) {
            if (!thread.isAlive()) {
                status = STATUS.stopped;
            }
        }
        return status;
    }

    /**
     * Set the status to error. Start cannot be called on StateThreads with
     * status error.
     * @param message a description of the error.
     * @param cause   the cause of the error or null.
     */
    protected void setError(String message, Throwable cause) {
        //noinspection DuplicateStringLiteralInspection
        log.debug("setError(" + message + ", ...) called", cause);
        status = STATUS.error;
        errorMessage = message;
        errorThrowable = cause;
    }

    /**
     * Implement this method as a standard run from Thread. Remember to check
     * for getStatus() == STATUS.running continously as described in the
     * class documentation.
     */
    protected abstract void runMethod();

    public final void run() {
        dk.statsbiblioteket.util.Profiler profiler =
                new dk.statsbiblioteket.util.Profiler();
        try {
            log.debug("Starting run");
            runMethod();
            log.info("Run complete after " + profiler.getSpendTime()
                     + ". Cleaning up");
            if (!status.equals(STATUS.error)) {
                status = STATUS.stopped;
            }
            if (STATUS.error.equals(getStatus())) {
                //noinspection DuplicateStringLiteralInspection
                log.debug("run stopped with error. Status " + status);
            } else {
                //noinspection DuplicateStringLiteralInspection
                log.warn("run stopped. Status " + status
                         + " (" + getErrorMessage() + ")", getErrorCause());
            }
        } catch (Throwable t) {
            log.error("Exception during run" , t);
            status = STATUS.error;
        }
        try {
            finishedCallback();
        } catch (Throwable t) {
            log.error("Throwable during finishedCallback", t);
        }
    }

    /**
     * Start or continue execution.
     */
    public synchronized void start() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("start called");
        switch (getStatus()) {
            case running: {
                log.debug("start: Already running");
                break;
            }
            case stopping: {
                log.debug("start: Attempting to abort previous stop");
                // Inform the thread that it should continue
                status = STATUS.running;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    log.warn("start: Interrupted during sleep");
                }
                if (status != STATUS.running) {
                    // continuing didn't work, try starting a new thread
                    log.debug("start: Aborting previous stop failed, starting "
                              + "anew instead");
                    start();
                } else {
                    log.debug("start: Aborting previous stop estimated to be "
                              + "successful");
                }
                return;
            }
            case error: throw new IllegalStateException("start: Cannot start "
                                                        + "when the status is "
                                                        + STATUS.error);
            case ready:
            case stopped: {
                log.debug("start: Creating and starting Thread");
                String threadName = "".equals(getClass().getSimpleName()) ?
                                    "Anonymous" : getClass().getSimpleName();
                thread = new Thread(this,
                                    threadName+"-"+hashCode());
                status = STATUS.running;
                thread.start();
                break;
            }
            default: {
                //noinspection DuplicateStringLiteralInspection
                throw new UnsupportedOperationException("Unknown status: "
                                                        + status);
            }
        }

    }

    /**
     * Signal the implementation to stop.
     */
    public synchronized void stop() {
        log.debug("Instructed to stop with status: " + getStatus());
        switch (getStatus()) {
            case error: {
                log.warn("stop called with status " + STATUS.error);
                break;
            }
            case stopping: {
                log.debug("Calling stop on already stopping");
                break;
            }
            case stopped: {
                log.debug("Calling stop on already stopped");
                break;
            }
            case ready: {
                log.debug("Calling stop on non-started");
                break;
            }
            case running: {
                //noinspection DuplicateStringLiteralInspection
                log.debug("Signalling stop");
                status = STATUS.stopping;
                break;
            }
            default: {
                //noinspection DuplicateStringLiteralInspection
                throw new UnsupportedOperationException("Unknown status: "
                                                        + status);
            }
        }
    }

    /**
     * @return true if the thread is running or stopping, else false.
     */
    public boolean isRunning() {
        return status.equals(STATUS.running) || status.equals(STATUS.stopping);
    }

    /**
    * Wait for the implementation to finish.
    * @throws InterruptedException if any Thread has interrupted the underlying
    *                              thread.
     * */
    public void waitForFinish() throws InterruptedException {
        waitForFinish(0);
    }

    /**
     * Wait for the implementation to finish.
     * @param timeout wait at most timeout milliseconds for finish.
     *                0 means wait forever.
     * @throws InterruptedException if any Thread has interrupted the underlying
     *                              thread.
     */
    public void waitForFinish(long timeout) throws InterruptedException {
        log.trace("Waiting for finish");
        while (true) {
            if (status == STATUS.ready
                || status == STATUS.stopped
                || status == STATUS.error) {
                return;
            }
            thread.join(timeout);
        }
    }

    /**
     * This method will be called when the runMethod exits. Override it if some
     * processing should be done then.
     */
    protected void finishedCallback() { }

    /**
     * @return a description of the last error encountered.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @return the last cause of an error, if any cause was given.
     */
    public Throwable getErrorCause() {
        return errorThrowable;
    }

}

