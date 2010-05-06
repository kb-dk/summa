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

import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.PayloadQueue;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.Set;
import java.util.List;
import java.util.HashSet;

/**
 * Helper class for {@link MUXFilter} that handles feeding of the right Payloads
 * into Filters. The class for the Filter to wrap in the feeder must be
 * specified in {@link #CONF_FILTER_CLASS}.
 * </p><p>
 * The feeder works by continuously calling next() on the wrapped filter and
 * putting the resulting Payload in an out queue. When the feeder reaches EOF,
 * the special Payload {@link #STOP} is added to the out queue.
 */
@QAInfo(level = QAInfo.Level.FINE,
        state = QAInfo.State.QA_NEEDED,
        author = "te",
        comment = "This is a central component which uses threading. "
                  + "Please pay special attention to potential deadlocks")
public class MUXFilterFeeder implements Runnable {
    private static Log log = LogFactory.getLog(MUXFilterFeeder.class);

    /**
     * The length of the input queue.
     * </p><p>
     * This property is optional. Default is 1.
     */
    public static final String CONF_QUEUE_MAXPAYLOADS =
            "summa.muxfilter.feeder.queue.in.length";
    public static final int DEFAULT_QUEUE_MAXPAYLOADS = 100;

    /**
     * The maximum size in bytes of the Payloads in the input queue.
     * </p><p>
     * This property is optional. Default is 1 MB.
     */
    public static final String CONF_QUEUE_MAXBYTES =
            "summa.muxfilter.feeder.queue.in.maxbytes";
    public static final int DEFAULT_QUEUE_MAXBYTES = 1024 * 1024;

    /*
     * @deprecated in favor of a shared outQueue.
     * @see {@link MUXFilter#CONF_OUTQUEUE_MAXPAYLOADS}.
     */
    private static final String CONF_QUEUE_OUT_LENGTH =
            "summa.muxfilter.feeder.queue.out.length";

    /*
     * @deprecated in favor of a shared outQueue.
     * @see {@link MUXFilter#CONF_OUTQUEUE_MAXBYTES}.
     */
    private static final String CONF_QUEUE_OUT_MAXBYTES =
            "summa.muxfilter.feeder.queue.out.maxbytes";

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
    public static final Payload STOP =
            new Payload(new Record("EOF", "Dummy", new byte[0]));


    PayloadQueue out;
    private PushFilter pusher;
    private ObjectFilter filter;
    private String filterName = DEFAULT_FILTER_NAME;
    private boolean isFallback = DEFAULT_FILTER_ISFALLBACK;
    private Set<String> bases = null;
    private long payloadCount = 0;

    private boolean eofReached = false;

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public MUXFilterFeeder(Configuration conf, PayloadQueue out) {
        log.trace("Constructing MUXFilterFeeder");
        if (out == null) {
            throw new IllegalArgumentException("out queue must not be null");
        }
        this.out = out;

        pusher = new PushFilter(
                conf.getInt(CONF_QUEUE_MAXPAYLOADS, DEFAULT_QUEUE_MAXPAYLOADS),
                conf.getInt(CONF_QUEUE_MAXBYTES, DEFAULT_QUEUE_MAXBYTES));
        if (conf.valueExists(CONF_QUEUE_OUT_LENGTH)) {
            log.warn(String.format(
                    "The configuration contained the deprecated key %s. "
                    + "Use %s instead", CONF_QUEUE_OUT_LENGTH,
                                        MUXFilter.CONF_OUTQUEUE_MAXPAYLOADS));
        }
        if (conf.valueExists(CONF_QUEUE_OUT_MAXBYTES)) {
            log.warn(String.format(
                    "The configuration contained the deprecated key %s. "
                    + "Use %s instead", CONF_QUEUE_OUT_MAXBYTES,
                                        MUXFilter.CONF_OUTQUEUE_MAXBYTES));
        }
        filter = createFilter(conf);
        filter.setSource(pusher);
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
        pusher.add(payload);
    }

    /**
     * @return the appromimate number of free slots for Payloads before
     *         blocking for inputs.
     */
    public int getFreeSlots() {
        return pusher.getFreeSlots();
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
        pusher.signalEOF();
        log.trace("signalEOF() completed for " + this);
    }

    public boolean isFallback() {
        return isFallback;
    }

    public boolean isEOFReached() {
        return eofReached;
    }

    @Override
    public String toString() {
        return "MUXFilterFeeder(" + filterName + ", " + filter + ", bases: " 
               + Strings.join(bases, ", ") + ")";
    }

    /**
     * Calls next on the filter until EOF is reached, puth the processed payload
     * in the out queue given in the constructor.
     */
    public void run() {
        try {
            while (filter.hasNext()) {
                payloadCount++;
                try {
                    log.trace("Polling filter for next processes Payload");
                    Payload next = filter.next();
                    if (log.isTraceEnabled()) {
                        log.trace("run() got " + next);
                    }
                    if (next != null) {
                        try {
                            log.trace("Offering payload to out queue in "
                                      + this);
                            out.put(next);
                            log.trace("outQueue accepted Payload");
                        } catch (InterruptedException e) {
                            log.warn("Interrupted while trying to add "
                                     + next + " to outQueue in " + this
                                     + ". Retrying");
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
            //noinspection DuplicateStringLiteralInspection
            log.debug("Emptied " + filter + " after " + payloadCount
                      + " Payloads");
            eofReached = true;
            out.uninterruptablePut(STOP);
        } catch (Exception e) {
            log.error(String.format(
                    "Got unexpected exception in run-method for '%s'",
                    this), e);
        }
    }

}

