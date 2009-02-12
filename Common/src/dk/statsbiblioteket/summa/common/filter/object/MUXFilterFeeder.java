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
import dk.statsbiblioteket.summa.common.filter.PayloadQueue;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
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
@QAInfo(level = QAInfo.Level.FINE,
        state = QAInfo.State.QA_NEEDED,
        author = "te",
        comment = "This is a central component which uses threading. "
                  + "Please pay special attention to potential deadlocks")
public class MUXFilterFeeder implements ObjectFilter, Runnable {
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
     * The maximum size in bytes of the Payloads in the input queue.
     * </p><p>
     * This property is optional. Default is 1 MB.
     */
    public static final String CONF_QUEUE_IN_MAXBYTES =
            "summa.muxfilter.feeder.queue.in.maxbytes";
    public static final int DEFAULT_QUEUE_IN_MAXBYTES = 1024 * 1024;

    /**
     * The length of the output queue.
     * </p><p>
     * This property is optional. Default is 10.
     */
    public static final String CONF_QUEUE_OUT_LENGTH =
            "summa.muxfilter.feeder.queue.out.length";
    public static final int DEFAULT_QUEUE_OUT_LENGTH = 10;

    /**
     * The maximum size in bytes of the Payloads in the output queue.
     * </p><p>
     * This property is optional. Default is 100 KB.
     */
    public static final String CONF_QUEUE_OUT_MAXBYTES =
            "summa.muxfilter.feeder.queue.out.maxbytes";
    public static final int DEFAULT_QUEUE_OUT_MAXBYTES = 100 * 1024;

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
    public static final String DEFAULT_FILTER_NAME = "Unnamed MUXFilterFeeder";
    /**
     * If a Filter is marked as fallback, it should only be used if no other
     * filters accepts the Payload, regardless of the queue-sizes of the
     * filters. If no fallback-filters are specified (or matching), a warning
     * will be issued and the non-usable Payload will be discarded.
     * </p><p>
     * For most setups, the default filter should accept all bases.
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
     * This property is optional. Default is "*".
     */
    public static final String CONF_FILTER_BASES =
            "summa.muxfilter.filter.bases";
    public static final String DEFAULT_FILTER_BASES = "*";

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private static final Payload STOP =
            new Payload(new Record("EOF", "Dummy", new byte[0]));

    private PayloadQueue inQueue;
    private PayloadQueue outQueue;
    private ObjectFilter filter;
    private String filterName = DEFAULT_FILTER_NAME;
    private boolean isFallback = DEFAULT_FILTER_ISFALLBACK;
    private Set<String> bases = null;

    private boolean eofReached = false;
    private Payload polledByHasNext = null;

    public MUXFilterFeeder(Configuration conf) {
        log.trace("Constructing MUXFilterFeeder");
        inQueue = new PayloadQueue(
                conf.getInt(CONF_QUEUE_IN_LENGTH, DEFAULT_QUEUE_IN_LENGTH),
                conf.getInt(CONF_QUEUE_IN_MAXBYTES, DEFAULT_QUEUE_IN_MAXBYTES));
        outQueue = new PayloadQueue(
              conf.getInt(CONF_QUEUE_OUT_LENGTH, DEFAULT_QUEUE_OUT_LENGTH),
              conf.getInt(CONF_QUEUE_OUT_MAXBYTES, DEFAULT_QUEUE_OUT_MAXBYTES));
        filter = createFilter(conf);
        filter.setSource(this);
        filterName = conf.getString(CONF_FILTER_NAME, filterName);
        isFallback = conf.getBoolean(CONF_FILTER_ISFALLBACK, isFallback);
        List<String> baseList = conf.getStrings(CONF_FILTER_BASES,
                                                (List<String>)null);
        if (baseList != null
            && !(baseList.size() == 1 && "*".equals(baseList.get(0)))) {
            bases = new HashSet<String>(baseList);
        }
        new Thread(this, filterName + "-" + this.hashCode()).start();
        log.debug("Constructed and activated " + this);
    }

    private ObjectFilter createFilter(Configuration configuration) {
        Class<? extends ObjectFilter> filter = configuration.getClass(
                CONF_FILTER_CLASS, ObjectFilter.class);
        log.debug(String.format("Got filter class %s. Commencing creation",
                                filter));
        return Configuration.create(filter, configuration);
    }

    /**
     * Add a Payload to the queue, blocking until the queue accepts it.
     * @param payload the Payload to add.
     */
    public void queuePayload(Payload payload) {
        if (log.isTraceEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("Queueing " + payload + " in " + this);
        }
        if (!accepts(payload)) {
            throw new IllegalArgumentException(String.format(
                    "%s does not accept %s", this, payload));
        }
        uninterruptableOffer(payload, inQueue);
    }

    /**
     * Blocks until a Payload is available in the out queue or EOF is reached.
     * @return a filtered Payload or null if EOF is reached.
     */
    public Payload getNextFilteredPayload() {
        if (eofReached) {
            return null;
        }
        Payload next = null;
        while (!eofReached && next == null) {
            try {
                log.trace("Polling for next filtered payload");
                next = outQueue.poll(500, TimeUnit.MILLISECONDS);
                if (log.isTraceEnabled()) {
                    log.trace("getNextFilteredPayload() got " + next);
                }
            } catch (InterruptedException e) {
                //noinspection DuplicateStringLiteralInspection
                log.warn("Interrupted while waiting for Payload in out queue. "
                         + "Retrying");
            }
        }
        if (next == STOP) {
            log.debug("Got STOP-Record from outQueue in " + this 
                      + ". Setting eofReached to true");
            eofReached = true;
        }
        return eofReached ? null : next;
    }

    /**
     * @param payload the Payload to check.
     * @return true if this filter will accept the payload in
     * {@link #queuePayload}.
     */
    public boolean accepts(Payload payload) {
        if (bases == null) {
            return true;
        }
        if (payload.getRecord() == null) {
            log.warn("A Payload without base was received in accepts("
                     + payload + ") in " + this);
            return false;
        }
        return bases.contains(payload.getRecord().getBase());
    }

    /**
     * Signal that no more Payloads will be added. Note that the signal will
     * propagate through the queues and thus will only be visible when all
     * previous queued Payloads are extracted.
     */
    public void signalEOF() {
        log.debug("signalEOF() entered for " + this);
        uninterruptableOffer(STOP, inQueue);
        log.trace("signalEOF() completed for " + this);
    }

    private void uninterruptableOffer(Payload payload, PayloadQueue queue) {
        while (true) {
            try {
                queue.offer(payload, Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
                return;
            } catch (InterruptedException e) {
                log.warn(String.format(
                        "Interrupted while adding %s to queue. Retrying",
                        payload), e);
            }
        }

    }

    /**
     * @return the number of Payloads currently in the input queue.
     */
    public int getInQueueSize() {
        return inQueue.size();
    }
    /**
     * @return the number of Payloads currently in the output queue.
     */
    public int getOutQueueSize() {
        return outQueue.size();
    }

    public boolean isFallback() {
        return isFallback;
    }

    public boolean isEofReached() {
        if (eofReached || outQueue.peek() == STOP) {
            log.debug("isEofReached(): " + this + " has eofReached="
                      + eofReached + " and (outQueue.peek() == STOP)="
                      + (outQueue.peek() == STOP));
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "MUXFilterFeeder(" + filterName + ", " + filter + ")";
    }

    public void run() {
        try {
            while (filter.hasNext()) {
                try {
                    log.trace("Polling filter for next processes Payload");
                    Payload next = filter.next();
                    if (log.isTraceEnabled()) {
                        log.trace("run() got " + next);
                    }
                    if (next != null) {
                        while (!eofReached) {
                            try {
                                log.trace("Offering payload to outQueue in"
                                          + this);
                                outQueue.offer(next, Integer.MAX_VALUE,
                                               TimeUnit.MILLISECONDS);
                                log.trace("outQueue accepted Payload");
                                break;
                            } catch (InterruptedException e) {
                                log.warn("Interrupted while trying to add "
                                         + next + " to outQueue in " + this
                                         + ". Retrying");
                            }
                        }
                        if (next == STOP) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.warn(String.format(
                            "Exception while calling next on filter '%s' in"
                            + " %s. Sleeping a bit, then retrying",
                            filter, this), e);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        log.warn("Interrupted while sleeping before next "
                                 + "poll in " + this,
                                 ex);
                    }
                }
            }
        } catch (Exception e) {
            log.error(String.format(
                    "Got unexpected exception in run-method for '%s'",
                    this), e);
        }
    }

/* ObjectFilter implementation */

    public boolean hasNext() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("hasNext() called");
        while (true) {
            if (eofReached) {
                return false;
            }
            if (polledByHasNext != null && polledByHasNext != STOP) {
                return true;
            }
            try {
                polledByHasNext = inQueue.poll(Integer.MAX_VALUE,
                                               TimeUnit.MILLISECONDS);
                if (polledByHasNext == STOP) {
                    log.debug("STOP received in inQueue, adding to outQueue"
                              + " for " + this);
                    uninterruptableOffer(STOP, outQueue);
                    return false;
                }
                return true;
            } catch (InterruptedException e) {
                log.warn("Interrupted while polling for next Payload. Retrying",
                         e);
            }
        }
    }

    public void setSource(Filter filter) {
        throw new UnsupportedOperationException(
                "MUXFilterFeeders are push-filters, so no explicit source "
                + "should be set");
    }

    public boolean pump() throws IOException {
        if (isEofReached()) {
            return false;
        }
        Payload next = getNextFilteredPayload();
        next.close();
        return isEofReached();
    }

    private boolean closed = false; // To avoid endless recursion
    public void close(boolean success) {
        if (closed) {
            return;
        }
        closed = true;
        filter.close(success);
    }

    /**
     * This should only be called internally from MUXFilterFeeder. To retrieve
     * the next processed Payload, use {@link #getNextFilteredPayload()}.
     * @return the next payload to filter through {@link #filter}.
     */
    public Payload next() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("next() called");
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
