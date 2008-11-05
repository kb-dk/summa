/* $Id$
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
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.io.IOException;

/**
 * Helper class for {@link MUXFilter} that handles feeding of the right Payloads
 * into Filters. The class for the Filter to wrap in the feeder must be
 * specified in {@link #CONF_FILTER_CLASS}.
 * </p><p>
 * The feeder works by continuously calling next() on the wrapped filter and
 * putting the resulting Payload in an out queue. The wrapped filter gets its
 * input Payloads from an inQueue.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MUXFilterFeeder implements ObjectFilter {
    private static Log log = LogFactory.getLog(MUXFilterFeeder.class);

    /**
     * The length of the input queue.
     * </p><p>
     * This property is optional. Default is 1.
     */
    public static final String CONF_QUEUE_IN_LENGTH =
            "summa.muxfilter.feeder.queue.in.length";
    public static final int DEFAULT_QUEUE_IN_LENGTH = 1;

    /**
     * The length of the output queue.
     * </p><p>
     * This property is optional. Default is 1.
     */
    public static final String CONF_QUEUE_OUT_LENGTH =
            "summa.muxfilter.feeder.queue.out.length";
    public static final int DEFAULT_QUEUE_OUT_LENGTH = 1;

    /**
     * The Class name for a filter specified in {@link MUXFilter#CONF_FILTERS}.
     * A new Filter will be created from this Class by introspection
     * and used by the muxer.
     * </p><p>
     * This property is mandatory.
     */
    public static final String CONF_FILTER_CLASS =
            "summa.muxfilter.filter.class";
    /**
     * The name of the Filter. Used for feedback and debugging.
     * </p><p>
     * This property is optional. Default is "Unnamed Filter".
     */
    public static final String CONF_FILTER_NAME =
            "summa.muxfilter.filter.name";
    public static final String DEFAULT_FILTER_NAME = "Unnamed Filter";
    /**
     * If a Filter is marked as fallback, it should only be used if no other
     * filters accepts the Payload, regardless of the queue-sizes of the
     * filters. If no fallback-filters are specified, a warning will be
     * issued and the non-usable Payload will be discarded.
     * </p><p>
     * This property is optional. Default is false.
     */
    public static final String CONF_FILTER_ISFALLBACK =
            "summa.muxfilter.filter.isfallback";
    public static final boolean DEFAULT_FILTER_ISFALLBACK = false;
    /**
     * A list of the bases that the Filter accepts. This is either a plain list
     * or "*", which designates all bases. Note that wildcards in general are
     * not supported, only "*".
     * </p><p>
     * This property is optinal. Default is "*".
     */
    public static final String CONF_FILTER_BASES =
            "summa.muxfilter.filter.bases";
    public static final String DEFAULT_FILTER_BASES = "*";

    private static final Payload STOP =
            new Payload(new Record("EOF", "Dummy", new byte[0]));

    private boolean eofReached = false;
    private ArrayBlockingQueue<Payload> inQueue;
    private ArrayBlockingQueue<Payload> outQueue;
    private Payload polledByHasNext = null;
    private ObjectFilter filter;

    public MUXFilterFeeder(Configuration conf) {
        log.trace("Constructing MUXFilterFeeder");
        inQueue = new ArrayBlockingQueue<Payload>(
                conf.getInt(CONF_QUEUE_IN_LENGTH, DEFAULT_QUEUE_IN_LENGTH),
                true);
        outQueue = new ArrayBlockingQueue<Payload>(
                conf.getInt(CONF_QUEUE_IN_LENGTH, DEFAULT_QUEUE_IN_LENGTH),
                true);
    }

    private Filter createFilter(Configuration configuration) {
        Class<? extends Filter> filter = configuration.getClass(
                CONF_FILTER_CLASS, Filter.class);
        log.debug("Got filter class " + filter + ". Commencing creation");
        return Configuration.create(filter, configuration);
    }

    /**
     * Add a Payload to the queue, blocking until the queue accepts it.
     * @param payload the Payload to add.
     * @throws InterruptedException if an interruption was received while
     *                              offering payload to the queue.
     */
    public void queuePayload(Payload payload) throws InterruptedException {
        if (log.isTraceEnabled()) {
            log.trace("Queueing " + payload);
        }
        inQueue.offer(payload, Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    /**
     * Signal that no more Payloads will be added.
     */
    public void signalEOF() {
        log.debug("signalEOF() entered");
        eofReached = true;
        while (true) {
            try {
                inQueue.offer(STOP, Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
                log.trace("signalEOF() completed");
                return;
            } catch (InterruptedException e) {
                log.warn("Interrupted while adding EOF-signal to inQueue. "
                         + "Retrying", e);
            }
        }
    }

    /**
     * @return the number of Payloads currently in the queue.
     */
    public int getQueueSize() {
        return inQueue.size();
    }

    public boolean hasNext() {
        while (true) {
            if (eofReached) {
                return false;
            }
            if (polledByHasNext != null && polledByHasNext != STOP) {
                return true;
            }
            try {
                polledByHasNext =
                        inQueue.poll(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
                if (polledByHasNext == STOP) {
                    eofReached = true;
                }
                return !eofReached;
            } catch (InterruptedException e) {
                log.warn("Interrupted while polling for next Payload.. "
                         + "Retrying", e);
            }
        }
    }

    public void setSource(Filter filter) {
        throw new UnsupportedOperationException(
                "MUXFilterFeeders are push-filters, so no explicit source "
                + "should be set");
    }

    public boolean pump() throws IOException {
        if (!hasNext()) {
            return false;
        }
        Payload next = next();
        next.close();
        return hasNext();
    }

    private boolean closed = false; // To avoid endless recursion
    public void close(boolean success) {
        if (closed) {
            return;
        }
        filter.close(success);
        closed = true;
    }

    public Payload next() {
        if (eofReached || !hasNext()) {
            throw new IllegalStateException("EOF reached. There is no next");
        }
        Payload result = polledByHasNext;
        polledByHasNext = null;
        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException(
                "Remove not supported by MUXFilterFeeder");
    }
}
