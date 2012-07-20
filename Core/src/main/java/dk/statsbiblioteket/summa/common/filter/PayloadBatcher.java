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
package dk.statsbiblioteket.summa.common.filter;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.LoggingExceptionHandler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Active PayloadQueue with callback flushing when it is full or when there is content and a timeout is reached.
 * Typically used to group requests to external resources.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class PayloadBatcher implements Configurable, Runnable {
    private static Log log = LogFactory.getLog(PayloadBatcher.class);

    /**
     * The maximum amount of Payloads before flushing.
     * </p><p>
     * Optional. Default is 1000.
     */
    public static final String CONF_MAX_COUNT = "batch.max.count";
    public static final int DEFAULT_MAX_COUNT = 2000;

    /**
     * The maximum sum of Payload byte sizes before flushing.
     * </p><p>
     * Optional. Default is 5000000 bytes (~5 MB).
     */
    public static final String CONF_MAX_BYTES = "batch.max.bytes";
    public static final long DEFAULT_MAX_BYTES = 5 * 1000 * 1000;

    /**
     * The maximum amount of time with inactivity before flushing.
     * </p><p>
     * Optional. Default is 1000 ms. If -1 is specified, there is no timeout.
     */
    public static final String CONF_MAX_MS = "batch.max.ms";
    public static final int DEFAULT_MAX_MS = 1000;

    private PayloadQueue queue;
    private final int maxCount;
    private final long maxBytes;
    private final int maxMS;

    private long lastAction = System.currentTimeMillis();
    private Thread watcher = null;
    private int received = 0;
    private boolean closed = false;
    private long flushTime = 0;

    public PayloadBatcher(Configuration conf) {
        maxCount = conf.getInt(CONF_MAX_COUNT, DEFAULT_MAX_COUNT);
        maxBytes = conf.getLong(CONF_MAX_BYTES, DEFAULT_MAX_BYTES);
        maxMS = conf.getInt(CONF_MAX_MS, DEFAULT_MAX_MS);
        queue = new PayloadQueue(maxCount, maxBytes);
        log.debug(String.format("Created PayloadBatcher with maxCount=%d, maxBytes=%d, maxMS=%d",
                                maxCount, maxBytes, maxMS));
        if (maxMS != -1) {
            watcher = new Thread(
                this, String.format("PayloadBatcher(count=%d, bytes=%d, MS=%d)", maxCount, maxBytes, maxMS));
            watcher.setDaemon(true); // Allow the JVM to exit
            watcher.setUncaughtExceptionHandler(new LoggingExceptionHandler());
            watcher.start();
        }
    }

    /**
     * Add a Payload to the batcher, potentially triggering a flush.
     * @param payload added to the internal queue.
     */
    public synchronized void add(Payload payload) {
        if (log.isTraceEnabled()) {
            log.trace("Adding " + payload);
        }
        if (closed) {
            throw new IllegalStateException("Unable to add " + payload + " after close() has been called");
        }
        received++;
        if (queue.offer(payload)) {
            notifyAll();
            return;
        }
        performFlush();
        if (!queue.offer(payload)) {
            throw new IllegalStateException("Unable to add " + payload + " to empty queue");
        }
        notifyAll();
    }

    @Override
    public void run () {
        log.debug("Starting batch watcher");
        synchronized (this) {
            while (!closed) {
                try {
                    if (log.isTraceEnabled()) {
                        log.trace("Waiting for records");
                    }
                    wait(maxMS);
                } catch (InterruptedException e) {
                    // We have been awoken!
                    log.trace("Interrupted while waiting for records");
                }
                checkFlush();
            }
        }
    }

    /**
     * Flush the content of the queue. It is the responsibility of the implementation to ensure that the queue is empty
     * when the method is finished.
     * @param queue the queue of Payloads to flush.
     */
    protected abstract void flush(PayloadQueue queue);

    /**
     * Request an explicit flush.
     */
    public synchronized void flush() {
        log.debug("Explicit flush called");
        performFlush();
    }

    private synchronized void performFlush() {
        if (queue.size() == 0) {
            log.trace("performFlush() called on empty queue");
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Flushing " + queue.size() + " records with a total size of " + queue.byteSize() / 1024
                      + " KB. Last flush was " + (System.currentTimeMillis() - lastAction) + " ms ago");
        }
        flushTime -= System.currentTimeMillis();
        flush(queue);
        flushTime += System.currentTimeMillis();
        lastAction = System.currentTimeMillis();
        if (queue.size() != 0) {
            throw new IllegalStateException(
                "Queue must be empty after flush, but contained " + queue.size() + " Payloads");
        }
    }

    private void checkFlush() {
        if (queue.isEmpty()) {
            return;
        }
        if (queue.size() == maxCount || System.currentTimeMillis() - lastAction >= maxMS) {
            performFlush();
        }
    }

    /**
     * Flushes remaining items in the queue and shuts down the flush timer. The PayloadBatcher cannot be used after
     * close() has been called.
     */
    public void close() {
        log.debug(String.format("Close called with %d received Payloads and %d still queued", received, queue.size()));
        closed = true;
        try {
            if (queue.size() > 0) {
                performFlush();
            }
        } finally {
            synchronized (this) {
                notifyAll();
            }
        }
        try {
            watcher.join();
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for Payload batching thread", e);
        }
        log.info(String.format("Closed batcher with %d processed Payloads, delivered in %d ms (%s Payloads/s)",
                               received, flushTime, flushTime == 0 ? "N/A" : (received * 1000 / flushTime)));
    }

    /**
     * @return the number of queues Payloads.
     */
    public int size() {
        return queue.size();
    }

    /**
     * @return the number of added Payloads.
     */
    public int getAdded() {
        return received;
    }

    public String toString() {
        return String.format("PayloadBatcher(count=%d, bytes=%d, MS=%d)", maxCount, maxBytes, maxMS);
    }
}
