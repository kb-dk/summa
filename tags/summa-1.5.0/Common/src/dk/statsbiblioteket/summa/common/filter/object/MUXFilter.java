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

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.PayloadQueue;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The MUXFilter divides incoming Payloads among one or more ObjectFilters
 * based on {@link dk.statsbiblioteket.summa.common.Record#getBase()}.
 * The Payloads must contain a Record.
 * </p><p>
 * One straight forward use case is to mux several XMLTransformers,
 * each responsible for handling a specific base.
 * </p><p>
 * The MUXFilter is - as all ObjectFilters - pull-based. There is a single
 * sink that pulls all contained Filters in parallel. Each filter gets Payloads
 * from a {@link MUXFilterFeeder} that in turn gets Payloads from the MUXFilter.
 * In order to provide hasNext()/next()-consistent behaviour (e.g. that there
 * always is a next(), is hasNext() is true), the MUXFilterFeeders might at any
 * time contain 1 or more cached Payload.
 * </p><p>
 * The resulting Payloads from the filters are extracted by round-robin. A more
 * flexible scheme, such as Payload order preservation, if left for later
 * improvements to the class.
 */
// TODO: Make a FilterProxy.
// TODO: Add optional consistent Payload ordering between feeders
// TODO: Add option to turn off feeding of filters, making the start points
@QAInfo(level = QAInfo.Level.FINE,
        state = QAInfo.State.QA_NEEDED,
        author = "te",
        comment = "This is a central component, which uses threading."
                  + " Please pay special attention to potential deadlocks")
public class MUXFilter implements ObjectFilter, Runnable {
    private static Log log = LogFactory.getLog(MUXFilter.class);

    /**
     * A list of sub-properties for the Filters to mux. Note that a
     * {@link MUXFilterFeeder} is created for each configuration, so
     * inspection of the properties for MUXFilterFeeder might be relevant.
     * </p><p>
     * Hint: Multiple instances of the same Filter can be used together with
     * ProxyFilters to provide Threaded execution.
     * </p><p>
     * This property is mandatory.
     */
    public static final String CONF_FILTERS = "summa.muxfilter.filters";

    /**
     * The number of instances to create from a given sub-configuration.
     * This provides an easy way to parallize filters.
     * <p/>
     * Please note that this property must be specified inside the
     * sub-configuration for the muxed filter and not in the configuration
     * for the MUXFilter itself.
     * </p><p>
     * Optional. Default is 1.
     */
    public static final String CONF_INSTANCES =
            "summa.muxfilter.filter.instances";
    public static final int DEFAULT_INSTANCES = 1;

    /**
     * The maximum number of Payloads in the output queue.
     * </p><p>
     * This property is optional. Default is 100.
     * @see {@link MUXFilterFeeder#CONF_QUEUE_MAXPAYLOADS}.
     */
    public static final String CONF_OUTQUEUE_MAXPAYLOADS =
            "summa.muxfilter.outqueue.maxpayloads";
    public static final int DEFAULT_OUTQUEUE_MAXPAYLOADS = 100;

    /**
     * The maximum size in bytes of the Payloads in the queue.
     * </p><p>
     * This property is optional. Default is 1 MB.
     */
    public static final String CONF_OUTQUEUE_MAXBYTES =
            "summa.muxfilter.outqueue.maxbytes";
    public static final int DEFAULT_OUTQUEUE_MAXBYTES = 1024 * 1024;

    /**
     * The number of ms to wait after trying to get a Payload from all feeders
     * before a retry is performed.
     */
    public static final int POLL_INTERVAL = 10;

    private ObjectFilter source = null;

    private List<MUXFilterFeeder> feeders;
    private Payload availablePayload = null;
    private boolean eofReached = false;
    private Profiler profiler;
    private PayloadQueue outqueue;

    public MUXFilter(Configuration conf) {
        log.debug("Constructing MUXFilter");
        outqueue = new PayloadQueue(
                conf.getInt(CONF_OUTQUEUE_MAXPAYLOADS,
                            DEFAULT_OUTQUEUE_MAXPAYLOADS),
                conf.getInt(CONF_OUTQUEUE_MAXBYTES,
                            DEFAULT_OUTQUEUE_MAXBYTES));
        if (!conf.valueExists(CONF_FILTERS)) {
            throw new Configurable.ConfigurationException(String.format(
                    "A value for the key %s must exist in the Configuration",
                    CONF_FILTERS));
        }
        List<Configuration> filterConfs;
        try {
            filterConfs = conf.getSubConfigurations(CONF_FILTERS);
        } catch (IOException e) {
            throw new ConfigurationException(String.format(
                    "Unable to extract Filter configurations from key %s",
                    CONF_FILTERS), e);
        }
        feeders = new ArrayList<MUXFilterFeeder>(filterConfs.size());
        for (Configuration filterConf: filterConfs) {
            for (int i = 0 ;
                 i < filterConf.getInt(CONF_INSTANCES, DEFAULT_INSTANCES) ;
                 i++) {
                feeders.add(new MUXFilterFeeder(filterConf, outqueue));
            }
        }
        if (feeders.size() > 0) {
            log.trace("Constructed feeders, starting to fill the feeders");
        } else {
            log.warn("No feeders defined for MUXFilter. There will never be any"
                     + " output although the source will be drained");
        }
        profiler = new Profiler();
        profiler.setBpsSpan(100);
        profiler.pause();
    }

    /**
     * The run-method extracts Payloads from source and feeds them into
     * the proper feeders until the source has no more Payloads.
     * </p><p>
     * The proper feeder is selected by priority:<br />
     * If non-fallback feeders which will accept the Payload exists, the one
     * with the fewest elements in the input-queue will be selected.
     * If no non-fallback feeders that will accept the Payload exists,
     * a repeat of the prioritization described above is done for default
     * feeders.
     * If no feeders at all will accept the Payload, a warning is issued.
     */
    public void run() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("Starting run");
        profiler.unpause();
        while (source.hasNext()) {
            Payload nextPayload = null;
            while (nextPayload == null && source.hasNext()) {
                try {
                    nextPayload = source.next();
                } catch (Exception e) {
                    log.warn("run(): Exception while getting next from source. "
                             + "Retrying in 500 ms", e);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        log.warn("run(): Interrupted while sleeping", e1);
                    }
                }
                if (nextPayload != null) {
                    profiler.beat();
                }
            }
            if (nextPayload == null) {
                log.warn("source.next() gave a null-pointer");
                break;
            }
            MUXFilterFeeder feeder;
            try {
                if (log.isTraceEnabled()) {
                    log.trace("Getting feeder for " + nextPayload);
                }
                feeder = getFeeder(nextPayload);
            } catch (Exception e) {
                log.error("Unexpected exception while getting feeder for "
                          + nextPayload);
                continue;
            }
            if (feeder == null) {
                // Warnings are issued by getFeeder
                continue;
            }
            if (log.isTraceEnabled()) {
                //noinspection DuplicateStringLiteralInspection
                log.trace("Adding " + nextPayload + " to " + feeder);
            }
            feeder.queuePayload(nextPayload);
        }
        sendEOFToFeeders();
    }

    private void sendEOFToFeeders() {
        log.debug("Sending EOF to all feeders");
        for (MUXFilterFeeder feeder: feeders) {
            feeder.signalEOF();
        }
    }

    private MUXFilterFeeder getFeeder(Payload payload) {
        // Find non-fallback candidates
        MUXFilterFeeder feederCandidate = null;
        int maxFreeSlots = 0;
        for (MUXFilterFeeder feeder: feeders) {
            if (!feeder.isFallback() && feeder.accepts(payload)
                && feeder.getFreeSlots() >= maxFreeSlots) {
                feederCandidate = feeder;
                maxFreeSlots = feeder.getFreeSlots();
            }
        }
        if (feederCandidate != null) {
            return feederCandidate;
        }

        // Find fallback candidates
        for (MUXFilterFeeder feeder: feeders) {
            if (feeder.isFallback() && feeder.accepts(payload)
                && feeder.getFreeSlots() >= maxFreeSlots) {
                feederCandidate = feeder;
                maxFreeSlots = feeder.getFreeSlots();
            }
        }
        if (feederCandidate == null) {
            log.warn("Unable to locate a MUXFilterFeeder for " + payload
                     + ". The Payload will be discarded");
        }
        return feederCandidate;
    }

    /* Objectfilter interface */

    public synchronized void setSource(Filter source) {
        if (!(source instanceof ObjectFilter)) {
            throw new IllegalArgumentException(String.format(
                    "The source must be an Objectfilter. Got '%s'",
                    source.getClass()));
        }
        if (this.source == source) {
            log.warn(String.format(
                    "The source %s is already assigned. No change is done and "
                    + "no new Threads are started", source));
            return;
        }
        if (this.source != null) {
            log.error(String.format(
                    "The source %s is already specified. A new thread will be"
                    + " started for source %s, but correctness is not "
                    + "guaranteed", this.source, source));
        }
        this.source = (ObjectFilter)source;
        log.debug("Source " + source + " specified. Starting mux-thread");
        Thread t = new Thread(this, "MUXFilter-" + this.hashCode());
//        t.setUncaughtExceptionHandler(this);
        t.start();
    }

    // TODO: Consider signalling to encloding FilterChain or similar
    // TODO: Consider shutting down the JVM
/*    public void uncaughtException(Thread t, Throwable e) {
        log.fatal(String.format(
                "Uncaught Exception in %s. Terminating processing by "
                + "sending close(false) to source, sending EOF to "
                + "feeders and emptying the source. Some Payloads will "
                + "probably be discarded as part of this process",
                t.getName()), e);
        close(false);
        sendEOFToFeeders();
        try {
            //noinspection StatementWithEmptyBody
            while (source.pump());
        } catch (Exception e2) {
            log.error("Exception while emptying source with pump(). pump() will" 
                      + " no longer be called", e);
        }
    }*/

    public boolean pump() throws IOException {
        if (!hasNext()) {
            return false;
        }
        Payload next = next();
        Logging.logProcess("MUXFilter",
                           "Calling close for object as part of pump()",
                           Logging.LogLevel.TRACE, next);
        next.close();
        return hasNext();
    }

    public void close(boolean success) {
        // No closing for feeders as they are push-oriented and EOF is signalled
        // when an EOF is received from the source.
        source.close(success);
    }

    public synchronized boolean hasNext() {
        while (true) {
            if (availablePayload != null) {
                return true;
            }
            if (eofReached) {
                log.trace("hasNext() EOF reached");
                return false;
            }
            // Check for EOF
            // We need to do this before we drain to avoid race-conditions where
            // EOF is raised and STOP is put in the queue
            boolean allSaysEOF = true;
            for (MUXFilterFeeder feeder: feeders) {
                if (!feeder.isEOFReached()) {
                    allSaysEOF = false;
                    break;
                }
            }
            availablePayload = drain();
            if (availablePayload != null) {
                // We got something, so we don't care about the EOF-signals
                return true;
            }

            if (allSaysEOF) {
                log.trace("hasNext() allSaysEOF and availablePayload == null");
                // When all says EOF, we know that no new valid Payloads will
                // appear in the outqueue. Since we just drained it and got
                // null, we're finished.
                eofReached = true;
                return false;
            }

            // No payloads in the queue but not EOF either:
            // We'll wait for something to appear.
            Payload next = outqueue.uninterruptibleTake();
            if (next != MUXFilterFeeder.STOP) {
                availablePayload = next;
            }
        }
    }

    /**
     * @return the next available payload, if any.
     */
    private Payload drain() {
        Payload next = null;
        while (outqueue.size() > 0) {
            next = outqueue.uninterruptibleTake();
            if (next != MUXFilterFeeder.STOP) {
                break;
            }
        }
        return next == MUXFilterFeeder.STOP ? null : next;
    }

    public Payload next() {
        log.trace("Next() called");
        if (!hasNext()) {
            throw new IllegalStateException("No more elements");
        }
        Payload returnPayload = availablePayload;
        availablePayload = null;
        return returnPayload;
    }

    public void remove() {
        //noinspection DuplicateStringLiteralInspection
        throw new UnsupportedOperationException("Remove not supported");
    }

}

