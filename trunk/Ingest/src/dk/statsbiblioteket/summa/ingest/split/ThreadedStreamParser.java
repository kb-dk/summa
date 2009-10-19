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
 * {@link #protectedRun()} method.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class ThreadedStreamParser implements StreamParser, Runnable {
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

    //private static final long HASNEXT_SLEEP = 50; // Sleep-ms between polls
    private static final Payload interruptor =
            new Payload(new Record("dummyID", "dummyStreamBase", new byte[0]));

    private int queueTimeout = DEFAULT_QUEUE_TIMEOUT;
    private PayloadQueue queue;
    protected Payload sourcePayload;
    protected boolean running = false;
    private boolean finished = false; // Totally finished
    private Throwable lastError;

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
        if (running) {
            throw new IllegalStateException(String.format(
                    "Already parsing %s when open(%s) was called",
                    sourcePayload, streamPayload));
        }
        queue.clear(); // Clean-up from previous runs
        if (streamPayload.getStream() == null) {
            log.warn("No stream in received " + streamPayload
                     + ". No Records will be generated");
            finished = true;
            return;
        }
        sourcePayload = streamPayload;
        log.trace("Starting Thread for " + streamPayload);
        running = true;
        finished = false;

        /* Set up a thread reporting errors back to us */
        Thread t = new Thread(this, "ThreadedStreamParser("
                         + this.getClass().getSimpleName() + ")");
        final ThreadedStreamParser dummyThis = this;
        t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            ThreadedStreamParser owner = dummyThis;
            public void uncaughtException(Thread t, Throwable e) {
                owner.setError(e);
            }
        });
        t.start();
    }

    private void setError(Throwable e) {
        lastError = e;
    }

    public Throwable getLastError() {
        return lastError;
    }

    @SuppressWarnings({"ObjectEquality"})
    public synchronized boolean hasNext() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("hasNext() called");
        // TODO: If this check solid enough?
        if (queue == null || sourcePayload == null) {
            return false;
        }
        long endTime = System.currentTimeMillis() + queueTimeout;
        while (System.currentTimeMillis() < endTime) {
            if (finished) {
                if (log.isTraceEnabled()) {
                    log.trace("hasNext reached state finished=true, returning "
                    + (queue.size() > 0 && queue.peek() != interruptor));
                }
                return queue.size() > 0 && queue.peek() != interruptor;
            }

            if (queue.size() > 0) {
                //noinspection ObjectEquality
                log.trace("hasNext queue > 0, returning "
                          + (queue.peek() != interruptor));
                //noinspection ObjectEquality
                return queue.peek() != interruptor;
            }

            log.trace("hasNext(): Calling peek on queue of size 0 and running " 
                      + running);
            Payload payload = queue.peek();
            log.trace("hasNext(): Peek finished with  " + payload);
            if (payload != null) {
                log.trace("hasNext(): queue.size() > 0, returning " 
                          + (payload != interruptor));
                //noinspection ObjectEquality
                return payload != interruptor;
            }

//            try {
                // Sleeping in a high throughput filter chain is very bad;
                // we log this as a warning.
                log.trace("hasNext(): Waiting for further Payloads");
                queue.uninterruptibleWaitForEntry();
                log.trace("hasNext(): Finished waiting");
//                Thread.sleep(HASNEXT_SLEEP); // Ugly
//            } catch (InterruptedException e) {
//                log.warn("Interrupted while waiting for Record in hasNext(). "
//                          + "Returning false", e);
//                return false;
//            }
        }
        log.warn(String.format("hasNext waited more than %d ms for status and"
                               + " got none. Returning false", queueTimeout));
        return false;
    }

    public synchronized Payload next() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("next() called");
        if (!hasNext()) {
            throw new NoSuchElementException(
                    "No more Records for the current stream from "
                    + sourcePayload);
        }
        while (!(finished && queue.size() == 0)) {
            try {
                log.trace("next: Polling for Record with timeout of "
                          + queueTimeout + " ms");
                Payload payload = queue.poll(
                        queueTimeout, TimeUnit.MILLISECONDS);
                if (payload == null) {
                    throw new NoSuchElementException(String.format(
                            "Waited more than %d ms for Record and got none",
                            queueTimeout));
                }
                //noinspection ObjectEquality
                if (payload == interruptor) { // Hack
                    throw new NoSuchElementException(
                            "Parsing interrupted, no more elements");
                }
                log.trace("Got record. Constructing and returning Payload");
                postProcess(payload);
                return payload;
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for Record. Retrying");
            }
        }
        throw new NoSuchElementException("Expected more Records, but got none");
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
        finished = true;
        stop();
    }

    public void stop() {
        log.debug("stop() called on " + this);
        running = false;
        queue.clear();
        queue.uninterruptablePut(interruptor);
    }

    public void run() {
        log.debug("run() entered");
        try {
            protectedRun();
            sourcePayload.close();
        } catch (Exception e) {
            log.warn(String.format(
                    "Exception caught from protectedRun of %s with origin '%s'"
                    + " in '%s'. Stopping processing", 
                    sourcePayload, sourcePayload.getData(Payload.ORIGIN), this),
                     e);

            // We don't close in a 'finally' clause because we shouldn't
            // clean up if the JVM raises an Error type throwable
            sourcePayload.close();
        }
        if (log.isDebugEnabled()) {
            log.debug("run: Finished processing " + sourcePayload);
        }
        running = false;
        finished = true; // Too final?
        addToQueue(interruptor);
        log.debug("run() finished for " + this);
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
     * @see {@link #addToQueue(dk.statsbiblioteket.summa.common.Record)}.
     * @see {@link #addToQueue(dk.statsbiblioteket.summa.common.filter.Payload)}.
     */
    protected abstract void protectedRun() throws Exception;

    @Override
    public String toString() {
        //noinspection DuplicateStringLiteralInspection
        return "ThreadedStreamParser(" + queue.size() + " payloads queued)";
    }
}
