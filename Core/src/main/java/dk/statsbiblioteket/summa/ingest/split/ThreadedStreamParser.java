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
package dk.statsbiblioteket.summa.ingest.split;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.PayloadQueue;
import dk.statsbiblioteket.summa.common.util.DeferredSystemExit;
import dk.statsbiblioteket.util.Timing;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/**
 * Helper implementation of StreamParser that handles the bookkeeping of
 * threaded parsing. With threaded parsing, a thread is responsible for reading
 * from the Stream, producing Records and adding the Records to a queue.
 * Reading from the queue is done from outside the Thread.
 * </p><p>
 * Implementators of this abstract class only needs to override the
 * {@link #protectedRun(Payload)} method. They should check {@link #running}
 * frequently and stop processing if it is false.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class ThreadedStreamParser implements StreamParser {
    private static Log log = LogFactory.getLog(ThreadedStreamParser.class);

    /**
     * The maximum queue size, counted in number of Records.
     * </p><p>
     * Optional. Default is 100.
     */
    public static final String CONF_QUEUE_SIZE = "summa.ingest.stream.threadedstreamparser.queue.size";
    public static final int DEFAULT_QUEUE_SIZE = 100;

    /**
     * The maximum queue size, counted in bytes.
     * </p><p>
     * Optional. Default is 5 MB.
     */
    public static final String CONF_QUEUE_BYTESIZE = "summa.ingest.stream.threadedstreamparser.queue.bytesize";
    public static final int DEFAULT_QUEUE_BYTESIZE = 5*1000*1000; // 5 MB

    /**
     * If true, any exception thrown in {@link #protectedRun(dk.statsbiblioteket.summa.common.filter.Payload)} will
     * result in fatal logging and a forced shutdown of the JVM.
     * </p><p>
     * If the workflow is oriented around singular large data dumps, it is recommended to set this to true to get
     * transactional-like behaviour and to signal that potentially nearly all data is unprocessed.
     * </p><p>
     * Optional. Default is false;
     */
    public static final String CONF_SHUTDOWN_ON_EXCEPTION =
            "summa.ingest.stream.threadedstreamparser.shutdown_on_exception";
    public static final boolean DEFAULT_SHUTDOWN_ON_EXCEPTION = false;
    private static final int SHUTDOWN_DELAY = 100; // ms

    /**
     * The maximum number of milliseconds to wait for data when hasNext(),
     * next() or pump() is called.
     * </p><p>
     * Optional. Default is Integer.MAX_VALUE.
     */
    public static final String CONF_QUEUE_TIMEOUT = "summa.ingest.stream.threadedstreamparser.queue.timeout";
    public static final int DEFAULT_QUEUE_TIMEOUT = Integer.MAX_VALUE;
    private int queueTimeout = DEFAULT_QUEUE_TIMEOUT;

    //private static final long HASNEXT_SLEEP = 50; // Sleep-ms between polls
    private static final Payload INTERRUPTOR =
            new Payload(new Record("InterruptorPayload", "dummyStreamBase", new byte[0]));

    /*
     * Holds the produced Payloads. Generated Payloads will always be followed
     * by {@link #INTERRUPTOR}.
     */
    private PayloadQueue queue;
    /*
     * The next Payload to deliver. This is assigned by {@link #hasNext} and
     * used by {@link #next}. toDeliver will never be {@link INTERRUPTOR}.
     */
    private Payload toDeliver = null;

    /**
     * The source Payload that is to be processed.
     */
    private Payload sourcePayload;

    /**
     * If true, processing should proceed normally. If false, processing should
     * be terminated when it can be done without affecting stability.
     */
    protected boolean running = false;

    /**
     * empty == true iff no Payload has been assigned yet or
     * (the processing thread is not running and the
     * interruptor-Payload has been encountered in the queue).
     * </p><p>
     * {@link #open} must only be called when empty == true.
     */
    private boolean empty = true; // Starting condition

    private Throwable lastError = null;
    private Thread runningThread = null;
    private long queueCount = 0;
    private final boolean shutdownOnException;

    private final Timing timing;
    private final Timing timingProcess;
    private final Timing timingPut;

    public ThreadedStreamParser(Configuration conf) {
        queue = new PayloadQueue(conf.getInt(CONF_QUEUE_SIZE, DEFAULT_QUEUE_SIZE),
                                 conf.getInt(CONF_QUEUE_BYTESIZE, DEFAULT_QUEUE_BYTESIZE));
        queueTimeout = conf.getInt(CONF_QUEUE_TIMEOUT, queueTimeout);
        shutdownOnException = conf.getBoolean(CONF_SHUTDOWN_ON_EXCEPTION, DEFAULT_SHUTDOWN_ON_EXCEPTION);
        timing =  new Timing(this.getClass().getSimpleName());
        timingProcess = timing.getChild("process", null, "Payload");
        timingPut = timing.getChild("put");
        log.debug("Constructed ThreadedStreamParser with queue-size " + queue.remainingCapacity()
                  + " shutdown on exception " + shutdownOnException + " and queue timeout " + queueTimeout + " ms");
    }

    @Override
    public void open(Payload streamPayload) {
        //noinspection DuplicateStringLiteralInspection
        log.debug("open(" + streamPayload + ") called");
        if (!empty) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "Already parsing %s when open(%s) was called",
                    sourcePayload, streamPayload));
        }
        if (!queue.isEmpty()) {
            log.debug("open(" + streamPayload.getId() + ") clearing old queue of size " + queue.size());
            queue.clear(); // Clean-up from previous runs
        }
        if (streamPayload.getStream() == null && !acceptStreamlessPayloads()) {
            log.warn("No stream in received " + streamPayload + ". No Records will be generated");
            empty = true;
            return;
        }
        sourcePayload = streamPayload;
        startThread();
    }

    protected boolean acceptStreamlessPayloads() {
        return false;
    }

    // TODO: This should use a thread-pool instead
    private void startThread() {
        log.trace("Starting Thread for " + sourcePayload);
        running = true;
        empty = false;
        /* Set up a thread reporting errors back to us */
        //noinspection DuplicateStringLiteralInspection
        runningThread = new Thread(new Runnable() {
            @SuppressWarnings("ObjectToString")
            @Override
            public void run() {
                log.trace("run() entered");
                final long startTime = System.nanoTime();
                try {
                    protectedRun(sourcePayload);
                    if (autoClose()) {
                        sourcePayload.close();
                    }
                } catch (Exception e) {
                    setError(e);
                    if (shutdownOnException) {
                        String message = String.format(
                                Locale.ROOT,
                                "Exception in protectedRun of %s with origin '%s'. Shutting down the JVM in %dms",
                                sourcePayload, sourcePayload.getData(Payload.ORIGIN), SHUTDOWN_DELAY);
                        Logging.fatal(log, "ThreadedStreamParser", message, e);
                        Logging.logProcess("ThreadedStreamParser", message, Logging.LogLevel.WARN, sourcePayload, e);
                        new DeferredSystemExit(1, SHUTDOWN_DELAY);
                    } else {
                        String message = String.format(
                                Locale.ROOT, "Exception caught from protectedRun of %s with origin '%s'",
                                sourcePayload, sourcePayload.getData(Payload.ORIGIN));
                        log.warn(String.format(Locale.ROOT, "%s in '%s'. Stopping processing", message, this), e);
                        Logging.logProcess("ThreadedStreamParser", message, Logging.LogLevel.WARN, sourcePayload, e);
                        // We don't close in a 'finally' clause because we shouldn't
                        // clean up if the JVM raises an Error type throwable
                        sourcePayload.close();
                    }
                    running = false;
                    addToQueue(INTERRUPTOR);
                } finally {
                    synchronized (timingProcess) {
                        timingProcess.addNS(System.nanoTime()-startTime);
                    }
                    running = false;
                    addToQueue(INTERRUPTOR);
                }
                if (log.isDebugEnabled()) {
                    log.debug(this.getClass().getSimpleName() + ": run() finished with "
                              + queue.size() + " remaining queued Payloads (the last queued Payload is the "
                              + "interruptor-token-Payload) for " + this + " with source " + sourcePayload + " in "
                              + (System.nanoTime()-startTime)/1000000 + "ms");
                }
            }
        }, "ThreadedStreamParser(" + this.getClass().getSimpleName() + ") daemon");
        runningThread.setDaemon(true);
        final ThreadedStreamParser dummyThis = this;
        runningThread.setUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    ThreadedStreamParser owner = dummyThis;
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        owner.setError(e);
                        running = false;
                    }
                });
        runningThread.start();
        log.trace("Thread started");
    }

    private void setError(Throwable e) {
        log.error(String.format(Locale.ROOT, "Encountered error during processing of %s", sourcePayload), e);
        lastError = e;
    }

    public Throwable getLastError() {
        return lastError;
    }

    @Override
    @SuppressWarnings({"ObjectEquality"})
    public synchronized boolean hasNext() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("hasNext() called");

        while (true) {
            if (empty) { // We're finished
                return false;
            }
            if (toDeliver != null) { // Something's waiting
                return true;
            }
            long endTime = System.currentTimeMillis() + queueTimeout;
            while (System.currentTimeMillis() < endTime && toDeliver == null) {
                try {
                    toDeliver = queue.poll(queueTimeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    log.warn("Interrupted while waiting for Payload. Retrying");
                }
            }
            if (toDeliver == null) {
                log.warn(String.format(Locale.ROOT,
                        "Timed out while waiting for Payload. This is bad as a thread is probably still processing %s. "
                        + "The queue is marked as empty in order to accept new Payloads, but this might lead to missed "
                        + "Payloads",
                        sourcePayload));
                empty = true;
                return false;
            }

            if (toDeliver == INTERRUPTOR) {
                if (log.isTraceEnabled()) {
                    log.trace("Encountered INTERRUPTOR. This signals that processing has been finished for "
                              + sourcePayload);
                }
                toDeliver = null;
                empty = true;
                return false;
            }
            // got something
            try {
                postProcess(toDeliver);
            } catch (Exception e) {
                log.warn("Got exception in postProcess, skipping " + toDeliver + " from " + sourcePayload);
                continue;
            }
            return true;
        }
    }

    @Override
    public synchronized Payload next() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("next() called");
        if (!hasNext()) {
            throw new NoSuchElementException("No more Records for the current stream from "  + sourcePayload);
        }
        if (toDeliver == null) {
            throw new NoSuchElementException(
                    "Failed sanity-check: toDeliver was null, but hasNext() == true means that it should be something. "
                    + "The offending source was" + sourcePayload);
        }
        Payload result = toDeliver;
        toDeliver = null;
        return result;
    }

    /**
     * Add the generated Record to the queue by creating a Payload around it,
     * blocking until there is room in the queue.
     * @param record the newly generated Record to add to the out queue.
     */
    protected void addToQueue(Record record) {
        if (log.isTraceEnabled()) {
            log.trace(String.format(Locale.ROOT,
                    "Wrapping Record %s as Payload and adding it to the queue",
                    record));
        }
        Payload newPayload = sourcePayload.clone();
        newPayload.setRecord(record);
        newPayload.setStream(null); // To avoid premature close
        if (record.getId() != null) {
            newPayload.setID(record.getId());
        }
        queueCount++;
        uninterruptiblePut(newPayload);
    }

    /**
     * Add the generated Payload to the queue, blocking until there is room
     * in the queue.
     * @param payload the payload to add to the queue.
     */
    protected void addToQueue(Payload payload) {
        if (log.isTraceEnabled()) {
            log.trace(String.format(Locale.ROOT, "Adding %s to queue", payload));
        }
        queueCount++;
        uninterruptiblePut(payload);
    }

    private void uninterruptiblePut(Payload p) {
        final long startTime = System.nanoTime();
        while (true) {
            try {
                queue.put(p);
                synchronized (timingPut) {
                    timingPut.addNS(System.nanoTime()-startTime);
                }
                return;
            } catch (InterruptedException e) {
                log.warn("Interrupted while queueing payload. Retrying");
            }
        }
    }

    /**
     * @return the current size of the out-queue in bytes.
     */
    protected long getQueueByteSize() {
        return queue.byteSize();
    }

    /**
     * @return the current size of the out-queue in Payloads.
     */
    protected long getQueueSize() {
        return queue.size();
    }

    /**
     * Override this to perform processing on the Payload immediately before it
     * is returned by {@link #next}.
     * @param payload the Payload to post-process.
     */
    protected void postProcess(Payload payload) {
        // Override if any post-processing is to be done
    }

    @Override
    public void remove() {
        log.warn("Remove not supported by ThreadedStreamParser");
    }

    @Override
    public void close() {
        //noinspection DuplicateStringLiteralInspection
        log.info("close() called after " + queueCount + " queued Payloads with timing " + timing);
        // TODO: Check whether this discards any currently processed Payloads
        empty = true;
        stop();
    }

    @Override
    public void stop() {
        log.debug("stop() called on " + this);
        running = false;

        /* The runningThread is only set if we have opened a payload */
        if (runningThread != null) {
            log.debug("stop() sending interrupt to Thread");
            runningThread.interrupt();
        } else {
            log.debug("stop() never received any payloads");
        }
    }

    /**
     * When protectedRun is entered, it is guaranteed that source contains a
     * Payload with a Stream. It is the responsibility of the implementation to
     * generate Records from the Stream and add them to {@link #queue}.
     * The implementation must check {@link #running} before adding to the
     * queue. If running is false, no Records must be added and the processing
     * must be terminated.
     * </p><p>
     * If {@link #autoClose()} is overridden to return false, it is the
     * responsibility of the implementation to close source when all
     * processing has finished. If autoClose() returns true, the source is
     * closed automatically.
     * </p><p>
     * It is perfectly valid for implementations to throw an Exception. This
     * is handled gracefully by logging an appropriate error and skipping
     * to the next available Stream. If an exception is thrown, the stream
     * will be closed automatically.
     * @param source should be used to generate new Payloads from.
     * @throws Exception if the sourcePayload could not be parsed properly.
     * @see #addToQueue(dk.statsbiblioteket.summa.common.Record)
     * @see #addToQueue(dk.statsbiblioteket.summa.common.filter.Payload)
     */
    protected abstract void protectedRun(Payload source) throws Exception;

    protected boolean autoClose() {
        return true;
    }

    @Override
    public String toString() {
        return "ThreadedStreamParser#" + getClass().getSimpleName() + "(" + queue.size() + " payloads queued)";
    }
}
