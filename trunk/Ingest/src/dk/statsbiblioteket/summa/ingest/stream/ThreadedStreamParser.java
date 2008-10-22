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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.filter.Payload;

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
     * Optional. Default is 10.
     */
    public static final String CONF_QUEUE_SIZE =
            "summa.ingest.stream.threadedstreamparser.queue.size";
    public static final int DEFAULT_QUEUE_SIZE = 10;

    /**
     * The maximum number of milliseconds to wait for data when hasNext(),
     * next() or pump() is called.
     * </p><p>
     * Optional. Default is Integer.MAX_VALUE.
     */
    public static final String CONF_QUEUE_TIMEOUT =
            "summa.ingest.stream.threadedstreamparser.queue.timeout";
    public static final int DEFAULT_QUEUE_TIMEOUT = Integer.MAX_VALUE;

    private static final long HASNEXT_SLEEP = 100; // Sleep-ms between polls
    private static final Record interruptor =
            new Record("dummyID", "dummyBase", new byte[0]);

    private int queueTimeout = DEFAULT_QUEUE_TIMEOUT;
    protected ArrayBlockingQueue<Record> queue;
    protected Payload sourcePayload;
    protected boolean running = false;
    private boolean finished = false; // Totally finished

    public ThreadedStreamParser(Configuration conf) {
        queue = new ArrayBlockingQueue<Record>(
                conf.getInt(CONF_QUEUE_SIZE, DEFAULT_QUEUE_SIZE));
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
                    "Already parsing %s", sourcePayload));
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
        new Thread(this).start();
    }

    @SuppressWarnings({"ObjectEquality"})
    public synchronized boolean hasNext() {
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
            Record record = queue.peek();
            log.trace("hasNext(): Peek finished with Record " + record);
            if (record != null) {
                log.trace("hasNext(): queue.size() > 0, returning " 
                          + (record != interruptor));
                //noinspection ObjectEquality
                return record != interruptor;
            }

            try {
                Thread.sleep(HASNEXT_SLEEP); // Ugly
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for Record in hasNext(). "
                          + "Returning false", e);
                return false;
            }
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
                Record record = queue.poll(queueTimeout, TimeUnit.MILLISECONDS);
                if (record == null) {
                    throw new NoSuchElementException(String.format(
                            "Waited more than %d ms for Record and got none",
                            queueTimeout));
                }
                //noinspection ObjectEquality
                if (record == interruptor) { // Hack
                    throw new NoSuchElementException(
                            "Parsing interrupted, no more elements");
                }
                log.trace("Got record. Constructing and returning Payload");
                Payload newPayload = sourcePayload.clone();
                newPayload.setRecord(record);
                newPayload.setStream(null); // To avoid premature close
                return newPayload;
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for Record. Retrying");
            }
        }
        throw new NoSuchElementException("Expected more Records, but got none");
    }

    public void remove() {
        log.warn("Remove not supported by ThreadedStreamParser");
    }

    public void close() {
        //noinspection DuplicateStringLiteralInspection
        log.debug("close() called");
        finished = true;
        stop();
    }

    public void stop() {
        log.debug("stop() called");
        running = false;
        queue.clear();
        queue.add(interruptor);
    }

    public void run() {
        log.debug("run() entered");
        try {
            protectedRun();
        } catch (Exception e) {
            log.warn(String.format(
                    "Exception caught from protectedRun of %s with origin '%s'."
                    + " Stopping processing", 
                    sourcePayload, sourcePayload.getData(Payload.ORIGIN)), e);
        }
        if (log.isDebugEnabled()) {
            log.debug("run: Finished processing " + sourcePayload);
        }
        running = false;
        finished = true; // Too final?
        log.debug("run() finished");
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
     */
    protected abstract void protectedRun() throws Exception;
}
