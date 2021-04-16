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
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.PayloadQueue;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.Locale;

/**
 * This filter reverses the normal pull-flow for filters to push. It does not
 * perform any processing on the Payloads besides queueing.
 * </p><p>
 * This filter uses a blocking queue to hold incoming Payloads. Any subsequent
 * filters will fetch Payloads from that queue. In order to push Payloads
 * through the PushFilter, {@link #add(Payload)} must be called.
 * Note that this call is blocking if the queue has reached its limit.
 * </p><p>
 * In order to stop processing, call {@link #signalEOF}. As this filter does not
 * have a known parent and thus cannot propagate close, it will normally be used
 * inside more complex filters as a helper.
 * </p><p>
 * This filter is not thread-safe.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class PushFilter implements ObjectFilter {
    private static Log log = LogFactory.getLog(PushFilter.class);

    /**
     * The maximum number of Payloads in the queue.
     * </p><p>
     * This property is optional. Default is 100.
     */
    public static final String CONF_QUEUE_MAXPAYLOADS = "summa.pushfilter.queue.maxpayloads";
    public static final int DEFAULT_QUEUE_MAXPAYLOADS = 100;

    /**
     * The maximum size in bytes of the Payloads in the queue.
     * </p><p>
     * This property is optional. Default is 1 MB.
     */
    public static final String CONF_QUEUE_MAXBYTES = "summa.pushfilter.queue.maxbytes";
    public static final int DEFAULT_QUEUE_MAXBYTES = 1024 * 1024;

    // Signals EOF
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private static final Payload STOP = new Payload(new Record("EOF", "Dummy", new byte[0]));

    private PayloadQueue queue;
    private Payload next = null;
    private long inPayloads = 0;
    private long outPayloads = 0;


    /**
     * @param conf the configuration for this filter.
     * @see #CONF_QUEUE_MAXPAYLOADS
     * @see #CONF_QUEUE_MAXBYTES
     */
    public PushFilter(Configuration conf) {
        this(conf.getInt(CONF_QUEUE_MAXPAYLOADS, DEFAULT_QUEUE_MAXPAYLOADS),
             conf.getInt(CONF_QUEUE_MAXBYTES, DEFAULT_QUEUE_MAXBYTES));
    }

    /**
     * Construct a PushFilter with the given constraints.
     *
     * @param maxPayloads the maximum number of Payloads in the queue before it
     *                    blocks inserts.
     * @param maxBytes    the maximum number of bytes in the queue before it
     *                    blocks inserts.
     */
    public PushFilter(int maxPayloads, int maxBytes) {
        queue = new PayloadQueue(maxPayloads, maxBytes);
        log.debug(String.format(Locale.ROOT, "Constructed PushFilter with queue max payloads %d and max payload bytes %d",
                                maxPayloads, maxBytes));
    }

    public void add(Payload payload) {
        if (log.isTraceEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("add(" + payload + ") called");
        }
        queue.uninterruptablePut(payload);
        inPayloads++;
        if (log.isTraceEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("add(" + payload + ") finished");
        }
    }

    /**
     * Signal that processing should stop. Note that this will only take effect
     * when all previously queued Payloads has been extracted.
     */
    public void signalEOF() {
        log.debug("signalEOF() called");
        queue.uninterruptablePut(STOP);
    }

    /**
     * @return the appromimate number of free slots for Payloads before
     *         blocking for inputs.
     */
    public int getFreeSlots() {
        return queue.remainingCapacity();
    }


    /* ObjectFilter interface */

    @Override
    public boolean hasNext() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("hasNext() called");
        if (next == STOP) {
            log.trace("hasNext(): next == STOP");
            return false;
        } else if (next != null) {
            log.trace("Returning true in hasNext()");
            return true;
        }
        log.trace("Waiting for input in hasNext()");
        next = queue.uninterruptibleTake();
        log.trace("Returning " + (next != STOP) + " after waiting in hasNext()");
        return next != STOP;
    }

    @Override
    public void setSource(Filter filter) {
        throw new UnsupportedOperationException("PushFilter cannot have explicit source");
    }

    @Override
    public boolean pump() throws IOException {
        return hasNext() && next() != null;
    }

    @Override
    public void close(boolean success) {
        log.trace("Close has no effect on PushFilter. Use signalEOF to shut down the filter");
    }

    @Override
    public Payload next() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("next() called");
        if (!hasNext()) {
            throw new IllegalStateException("EOF has been signalled. No more Payloads available");
        }
        Payload deliver = next;
        next = null;
        outPayloads++;
        if (log.isTraceEnabled()) {
            log.trace("next() delivering " + deliver + ". Total in=" + inPayloads + ", out=" + outPayloads);
        }
        return deliver;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove is not supported in PushFilter");
    }
}
