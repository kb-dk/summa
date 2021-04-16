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
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.PayloadQueue;
import dk.statsbiblioteket.summa.common.util.RecordStatsCollector;
import dk.statsbiblioteket.summa.common.util.StatUtil;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Timing;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
public class MUXFilter extends ObjectFilterBase implements Runnable {
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
    public static final String CONF_INSTANCES = "summa.muxfilter.filter.instances";
    public static final int DEFAULT_INSTANCES = 1;

    /**
     * The maximum number of Payloads in the output queue.
     * </p><p>
     * This property is optional. Default is 100.
     * @see MUXFilterFeeder#CONF_QUEUE_MAXPAYLOADS
     */
    public static final String CONF_OUTQUEUE_MAXPAYLOADS = "summa.muxfilter.outqueue.maxpayloads";
    public static final int DEFAULT_OUTQUEUE_MAXPAYLOADS = 100;

    /**
     * The maximum size in bytes of the Payloads in the queue.
     * </p><p>
     * This property is optional. Default is 1 MB.
     */
    public static final String CONF_OUTQUEUE_MAXBYTES = "summa.muxfilter.outqueue.maxbytes";
    public static final int DEFAULT_OUTQUEUE_MAXBYTES = 1024 * 1024;

    /**
     * If true, Payloads that does not match any feeders will be passed on
     * directly without logging any errors. If false, non-matched Payloads will
     * be discarded and an error will be logged.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_ALLOW_UNMATCHED = "summa.muxfilter.allow.unmatched";
    public static final boolean DEFAULT_ALLOW_UNMATCHED = false;

    private ObjectFilter source = null;

    private final List<MUXFilterFeeder> feeders;
    private Payload availablePayload = null;
    private boolean eofReached = false;
    private final Profiler profiler;
    private final PayloadQueue outqueue;
    private final boolean allowUnmatched;
    private final String name;

    public MUXFilter(Configuration conf) {
        super(conf);
        log.debug("Constructing MUXFilter");
        outqueue = new PayloadQueue(
                conf.getInt(CONF_OUTQUEUE_MAXPAYLOADS, DEFAULT_OUTQUEUE_MAXPAYLOADS),
                conf.getInt(CONF_OUTQUEUE_MAXBYTES, DEFAULT_OUTQUEUE_MAXBYTES));
        if (!conf.valueExists(CONF_FILTERS)) {
            throw new Configurable.ConfigurationException(String.format(Locale.ROOT,
                    "A value for the key %s must exist in the Configuration",
                    CONF_FILTERS));
        }
        List<Configuration> filterConfs;
        try {
            filterConfs = conf.getSubConfigurations(CONF_FILTERS);
        } catch (SubConfigurationsNotSupportedException e) {
            throw new ConfigurationException("Storage doesn't support sub configurations", e);
        } catch (NullPointerException e) {
            throw new ConfigurationException(String.format(Locale.ROOT,
                    "Unable to extract Filter configurations from key %s",
                    CONF_FILTERS), e);
        }
        feeders = new ArrayList<>(filterConfs.size());
        for (Configuration filterConf: filterConfs) {
            for (int i = 0 ; i < filterConf.getInt(CONF_INSTANCES, DEFAULT_INSTANCES) ; i++) {
                feeders.add(new MUXFilterFeeder(filterConf, outqueue));
            }
        }
        if (!feeders.isEmpty()) {
            log.trace("Constructed feeders, starting to fill the feeders");
        } else {
            log.warn("No feeders defined for MUXFilter. There will never be any"
                     + " output although the source will be drained");
        }
        allowUnmatched = conf.getBoolean(CONF_ALLOW_UNMATCHED, DEFAULT_ALLOW_UNMATCHED);
        name = conf.getString(ObjectFilterImpl.CONF_FILTER_NAME, "Unnamed (" + feeders + " feeders)");
        profiler = new Profiler();
        profiler.setBpsSpan(100);
        profiler.pause();
        setStatsDefaults(conf, true, true, true, false);

        log.info(String.format(Locale.ROOT, "Constructed MUXFilter '%s' with %d feeders, allow unmatched: %b",
                               name, feeders.size(), allowUnmatched));
    }

    @Override
    protected RecordStatsCollector createSizeProcess(Configuration conf) {
        return new RecordStatsCollector("queue", conf, false);
    }
    @Override
    protected Timing createTimingProcess(Configuration conf) {
        return StatUtil.createTiming(conf, "process", "queued", null, "Payload", null);
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
    @Override
    public void run() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("Starting run");
        profiler.unpause();
        while (sourceHasNext()) {
            Payload nextPayload = null;
            while (nextPayload == null && sourceHasNext()) {
                try {
                    nextPayload = sourceNext();
                } catch (Exception e) {
                    log.warn(
                        "run() Exception while getting next from source in '" + name
                        + "'. Retrying in 500 ms", e);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        log.warn("run() in muxer " + name + ": Interrupted while sleeping", e1);
                    }
                }
                if (nextPayload != null) {
                    profiler.beat();
                }
            }
            if (nextPayload == null) {
                log.warn("source.next() in muxer " + name + " gave a null-pointer");
                break;
            }

            sizePull.process(nextPayload);
            timingProcess.start();
            MUXFilterFeeder feeder;
            try {
                if (log.isTraceEnabled()) {
                    log.trace("Getting feeder for " + nextPayload + " in muxer " + name);
                }
                feeder = getFeeder(nextPayload);
            } catch (Exception e) {
                log.error("Unexpected exception while getting feeder for " + nextPayload + " in '" + name + "'");
                timingProcess.stop();
                continue;
            }
            if (feeder == null) {
                // Logging is handled by getFeeder when feeder == null
                if (allowUnmatched) {
                    outqueue.uninterruptablePut(nextPayload);
                }
            } else {
                if (log.isTraceEnabled()) {
                    //noinspection DuplicateStringLiteralInspection
                    log.trace("Muxer " + name + ": Adding " + nextPayload + " to " + feeder);
                }
                feeder.queuePayload(nextPayload);
                sizeProcess.process(nextPayload);
            }
            logProcess(nextPayload, timingProcess.stop());
            logStatusIfNeeded();
        }
        sendEOFToFeeders();
    }

    private Payload sourceNext() {
        try {
            timingPull.start();
            return source.next();
        } finally {
            timingPull.stop();
        }
    }

    private boolean sourceHasNext() {
        try {
            timingPull.start();
            return source.hasNext();
        } finally {
            timingPull.stop(timingPull.getUpdates()); // hasNext does not count as a full update
        }
    }

    private void sendEOFToFeeders() {
        log.debug("Muxer " + name + ": Sending EOF to all feeders");
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
            if (allowUnmatched) {
                log.debug("Unable to locate a MUXFilterFeeder for " + payload + " in '" + name
                          + "'. The Payload will be passed on directly");
                Logging.logProcess(
                    "MUXFilter " + name, "Unable to locate feeder. Passing unmodified",
                    Logging.LogLevel.TRACE, payload);
            } else {
                log.warn("Unable to locate a MUXFilterFeeder for " + payload + " in '" + name
                         + "'. The Payload will be discarded");
                Logging.logProcess(
                    "MUXFilter " + name, "Unable to locate feeder. Discarding Payload",
                    Logging.LogLevel.WARN, payload);
            }
        }
        return feederCandidate;
    }

    /* Objectfilter interface */

    @Override
    public synchronized void setSource(Filter source) {
        if (!(source instanceof ObjectFilter)) {
            throw new IllegalArgumentException(String.format(Locale.ROOT,
                    "The source must be an Objectfilter. Got '%s'",
                    source.getClass()));
        }
        if (this.source == source) {
            log.warn(String.format(Locale.ROOT,
                    "The source %s is already assigned. No change is done and no new Threads are started",
                    source));
            return;
        }
        if (this.source != null) {
            log.error(String.format(Locale.ROOT,
                    "The source %s is already specified. A new thread will be started for source %s, but correctness "
                    + "is not guaranteed",
                    this.source, source));
        }
        this.source = (ObjectFilter)source;
        log.debug("Source " + source + " specified. Starting mux-thread for " + name);
        Thread t = new Thread(this, "MUXFilter-" + this.hashCode() + " daemon");
        t.setDaemon(true);
//        t.setUncaughtExceptionHandler(this);
        t.start();
    }

    // TODO: Consider signalling to encloding FilterChain or similar
    // TODO: Consider shutting down the JVM
/*    public void uncaughtException(Thread t, Throwable e) {
        log.fatal(String.format(Locale.ROOT,
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

    @Override
    public void close(boolean success) {
        super.close(success);
        // Feeders are push-oriented, so in theory they should be when an EOF is received from the source.
        // However, this does not seem to work so...
        for (MUXFilterFeeder feeder : feeders) {
            try {
                feeder.close(success);
            } catch (Exception e) {
                log.warn("Exception calling close on feeder " + feeder);
            }
        }

        source.close(success);
    }

    @Override
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
        while (!outqueue.isEmpty()) {
            next = outqueue.uninterruptibleTake();
            if (next != MUXFilterFeeder.STOP) {
                break;
            }
        }
        return next == MUXFilterFeeder.STOP ? null : next;
    }

    @Override
    public Payload next() {
        log.trace("Next() called");
        if (!hasNext()) {
            throw new IllegalStateException("No more elements");
        }
        Payload returnPayload = availablePayload;
        availablePayload = null;
        return returnPayload;
    }

    @Override
    public void remove() {
        //noinspection DuplicateStringLiteralInspection
        throw new UnsupportedOperationException("Remove not supported");
    }
}
