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

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.PayloadQueue;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/**
 * Helper implementation of StreamParser that handles the bookkeeping of
 * threaded parsing. With threaded parsing, a thread is responsible for reading
 * from the Stream, producing Records and adding the Records to a queue.
 * Reading from the queue is done from outside the Thread.
 * </p><p>
 * Implementators of this abstract class only needs to override the
 * {@link #protectedRun()} method. They should check {@link #running}
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
    public static final String CONF_QUEUE_SIZE =
            "summa.ingest.stream.threadedstreamparser.queue.size";
    public static final int DEFAULT_QUEUE_SIZE = 100;

    /**
     * The maximum queue size, counted in bytes.
     * </p><p>
     * Optional. Default is 5 MB.
     */
    public static final String CONF_QUEUE_BYTESIZE =
            "summa.ingest.stream.threadedstreamparser.queue.bytesize";
    public static final int DEFAULT_QUEUE_BYTESIZE = 5*1000*1000; // 5 MB

    /**
     * The maximum number of milliseconds to wait for data when hasNext(),
     * next() or pump() is called.
     * </p><p>
     * Optional. Default is Integer.MAX_VALUE.
     */
    public static final String CONF_QUEUE_TIMEOUT =
            "summa.ingest.stream.threadedstreamparser.queue.timeout";
    public static final int DEFAULT_QUEUE_TIMEOUT = Integer.MAX_VALUE;
    private int queueTimeout = DEFAULT_QUEUE_TIMEOUT;

    //private static final long HASNEXT_SLEEP = 50; // Sleep-ms between polls
    private static final Payload INTERRUPTOR =
            new Payload(new Record("dummyID", "dummyStreamBase", new byte[0]));

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
    protected Payload sourcePayload;

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

    public ThreadedStreamParser(Configuration conf) {
        queue = new PayloadQueue(
                conf.getInt(CONF_QUEUE_SIZE, DEFAULT_QUEUE_SIZE),
                conf.getInt(CONF_QUEUE_BYTESIZE, DEFAULT_QUEUE_BYTESIZE));
        queueTimeout = conf.getInt(CONF_QUEUE_TIMEOUT, queueTimeout);
        log.debug("Constructed ThreadedStreamParser with queue-size "
                  + queue.remainingCapacity() + " and queue timeout "
                  + queueTimeout + " ms");
    }

    public void open(Payload streamPayload) {
        //noinspection DuplicateStringLiteralInspection
        log.debug("open(" + streamPayload + ") called");
        if (!empty) {
            throw new IllegalStateException(String.format(
                    "Already parsing %s when open(%s) was called",
                    sourcePayload, streamPayload));
        }
        queue.clear(); // Clean-up from previous runs
        if (streamPayload.getStream() == null) {
            log.warn("No stream in received " + streamPayload
                     + ". No Records will be generated");
            empty = true;
            return;
        }
        sourcePayload = streamPayload;
        startThread();
    }

    private void startThread() {
        log.trace("Starting Thread for " + sourcePayload);
        running = true;
        empty = false;
        /* Set up a thread reporting errors back to us */
        //noinspection DuplicateStringLiteralInspection
        runningThread = new Thread(new Runnable() {
            public void run() {
                log.trace("run() entered");
                try {
                    protectedRun();
                    sourcePayload.close();
                } catch (Exception e) {
                    log.warn(String.format(
                            "Exception caught from protectedRun of %s with "
                          + "origin '%s' in '%s'. Stopping processing",
                            sourcePayload,
                            sourcePayload.getData(Payload.ORIGIN), this),
                            e);

                    // We don't close in a 'finally' clause because we shouldn't
                    // clean up if the JVM raises an Error type throwable
                    sourcePayload.close();
                } finally {
                    running = false;
                    addToQueue(INTERRUPTOR);
                }
                if (log.isDebugEnabled()) {
                    log.debug("run() finished with " + queue.size()
                              + " remaining queued Payloads (the last queued "
                              + "Payload is the interruptor-token-Payload) for "
                              + this + " with source " + sourcePayload);
                }
            }
        }, "ThreadedStreamParser(" + this.getClass().getSimpleName() + ")");

        final ThreadedStreamParser dummyThis = this;
        runningThread.setUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    ThreadedStreamParser owner = dummyThis;
                    public void uncaughtException(Thread t, Throwable e) {
                        owner.setError(e);
                        running = false;
                    }
                });
        runningThread.start();
        log.trace("Thread started");
    }

    private void setError(Throwable e) {
        log.error(String.format(
                "Encountered error during processing of %s", sourcePayload), e);
        lastError = e;
    }

    public Throwable getLastError() {
        return lastError;
    }

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
                log.warn(String.format(
                        "Timed out while waiting for Payload. This is bad as "
                        + "a thread is probably still processing %s. The queue "
                        + "is marked as empty in order to accept new Payloads, "
                        + "but this might lead to missed Payloads",
                        sourcePayload));
                empty = true;
                return false;
            }

            if (toDeliver == INTERRUPTOR) {
                if (log.isTraceEnabled()) {
                    log.trace(
                            "Encountered INTERRUPTOR. This signals that proces"
                            + "sing has been finished for " + sourcePayload);
                }
                toDeliver = null;
                empty = true;
                return false;
            }
            // got something
            try {
                postProcess(toDeliver);
            } catch (Exception e) {
                log.warn("Got exception in postProcess, skipping " + toDeliver
                         + " from " + sourcePayload);
                continue;
            }
            return true;
        }
    }

    public synchronized Payload next() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("next() called");
        if (!hasNext()) {
            throw new NoSuchElementException(
                    "No more Records for the current stream from "
                    + sourcePayload);
        }
        if (toDeliver == null) {
            throw new NoSuchElementException(
                    "Failed sanity-check: toDeliver was null, but hasNext() == "
                    + "true means that it should be something. The offending "
                    + "source was" + sourcePayload);
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
            log.trace(String.format(
                    "Wrapping Record %s as Payload and adding it to the queue",
                    record));
        }
        Payload newPayload = sourcePayload.clone();
        newPayload.setRecord(record);
        newPayload.setStream(null); // To avoid premature close
        if (record.getId() != null) {
            newPayload.setID(record.getId());
        }

        uninterruptiblePut(newPayload);
    }

    /**
     * Add the generated InputStream to the queue by creating a Payload around
     * it, blocking until there is room in the queue.
     * @param stream the newly generated strean to add to the out queue.
     */
    protected void addToQueue(InputStream stream) {
        uninterruptiblePut(new Payload(stream));
    }

    /**
     * Add the generated Payload to the queue, blocking until there is room
     * in the queue.
     * @param payload the payload to add to the queue.
     */
    protected void addToQueue(Payload payload) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Adding %s to queue", payload));
        }
        uninterruptiblePut(payload);
    }

    private void uninterruptiblePut(Payload p) {
        while (true) {
            try {
                queue.put(p);
                return;
            } catch (InterruptedException e) {
                log.warn("Interrupted while queueing payload. Retrying");
            }
        }
    }

    /**
     * Override this to perform processing on the Payload immediately before it
     * is returned by {@link #next}.
     * @param payload the Payload to post-process.
     */
    protected void postProcess(Payload payload) {
        // Override if any post-processing is to be done
    }

    public void remove() {
        log.warn("Remove not supported by ThreadedStreamParser");
    }

    public void close() {
        //noinspection DuplicateStringLiteralInspection
        log.debug("close() called");
        // TODO: Check whether this discards any currently processed Payloads
        empty = true;
        stop();
    }

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
     * When protectedRun is entered, it is guaranteed that 
     * {@link #sourcePayload} contains a Payload with a Stream. It is the
     * responsibility of the implementation to generate Records from the Stream
     * and add them to {@link #queue}. The implementation must check
     * {@link #running} before adding to the queue. If running is false, no
     * Records must be added and the processing must be terminated.
     * </p><p>
     * It is perfectly valid for implementations to throw an Exception. This
     * is handled gracefully by logging an appropriate error and skipping
     * to the next available Stream.
     * @throws Exception if the sourcePayload could not be parsed properly.
     * @see #addToQueue(dk.statsbiblioteket.summa.common.Record)
     * @see #addToQueue(dk.statsbiblioteket.summa.common.filter.Payload)
     */
    protected abstract void protectedRun() throws Exception;

    @Override
    public String toString() {
        //noinspection DuplicateStringLiteralInspection
        return "ThreadedStreamParser(" + queue.size() + " payloads queued)";
    }
}

