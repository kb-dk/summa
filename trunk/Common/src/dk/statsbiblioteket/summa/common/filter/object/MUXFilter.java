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
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

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
// TODO: Make a FilterProxy and a FilterSequencer.
// TODO: Add optional consistent Payload ordering between feeders
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MUXFilter implements ObjectFilter, Runnable {
    private static Log log = LogFactory.getLog(MUXFilter.class);

    /**
     * A list of Strings specifying the subProperties for the Filters to mux.
     * Note that Filters may appear more than once in the list, in which case
     * more instances are created. Multiple instances of the same Filter might
     * be used together with ProxyFilters to provide Threaded execution.
     * </p><p>
     * For each unique Filter specified in this property, a subConfiguration
     * must exist in the Configuration. Note that {@link MUXFilterFeeder}
     *
     * </p><p>
     * This property is mandatory.
     */
    public static final String CONF_FILTERS = "summa.muxfilter.filters";

    /**
     * The number of ms to wait after trying to get a Payload from all feeders
     * before a retry is performed.
     */
    public static final int POLL_INTERVAL = 200;

    private ObjectFilter source = null;

    private List<MUXFilterFeeder> feeders;
    private Payload availablePayload = null;
    private boolean eofReached = false;

    public MUXFilter(Configuration conf) {
        log.debug("Constructing MUXFilter");
        if (!conf.valueExists(CONF_FILTERS)) {
            throw new Configurable.ConfigurationException(String.format(
                    "A value for the key %s must exist in the Configuration",
                    CONF_FILTERS));
        }
        List<String> filterConfKeys = conf.getStrings(CONF_FILTERS);
        feeders = new ArrayList<MUXFilterFeeder>(filterConfKeys.size());
        for (String filterConfKey: filterConfKeys) {
            try {
                feeders.add(new MUXFilterFeeder(
                        conf.getSubConfiguration(filterConfKey)));
            } catch (IOException e) {
                throw new Configurable.ConfigurationException(String.format(
                        "Unable to create MUXFilterFeeder with key '%s'",
                        filterConfKey), e);
            }
        }
        if (feeders.size() > 0) {
            log.trace("Constructed feeders, starting to fill the feeders");
        } else {
            log.warn("No feeders defined for MUXFilter. There will never be any"
                     + " output but the source will be drained");
        }
    }

    /**
     * The run-method extractes Payloads from source and feeds them into
     * the proper feeders until the source has no more Payloads.
     * </p><p>
     * The proper feeder is selected by priority:<br />
     * If non-fallback feeders which will accept the Payload exists, the onw
     * with the fewest elements in the input-queue will be selected.
     * If no non-fallback feeders that will accept the Payload exists,
     * a repeat of the prioritization described above is done for default
     * feeders.
     * If no feeders at all will accept the Payload, a warning is issued.
     */
    public void run() {
        log.trace("Starting run");
        Profiler profiler = new Profiler();
        profiler.setBpsSpan(100);
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
            try {
                if (log.isTraceEnabled()) {
                    log.trace("Adding " + nextPayload + " to " + feeder);
                }
                feeder.queuePayload(nextPayload);
            } catch (InterruptedException e) {
                log.warn(String.format(
                        "Interrupted while offering %s to %s. Skipping to next "
                        + "Payload", nextPayload, feeder), e);
            }
        }
    }

    private MUXFilterFeeder getFeeder(Payload payload) {
        // Find non-fallback candidates
        MUXFilterFeeder feederCandidate = null;
        int queueSize = Integer.MAX_VALUE;
        for (MUXFilterFeeder feeder: feeders) {
            if (!feeder.isFallback() && feeder.accepts(payload)
                && feeder.getInQueueSize() < queueSize) {
                feederCandidate = feeder;
                queueSize = feeder.getInQueueSize();
            }
        }
        if (feederCandidate != null) {
            return feederCandidate;
        }

        // Find fallback candidates
        for (MUXFilterFeeder feeder: feeders) {
            if (feeder.isFallback() && feeder.accepts(payload)
                && feeder.getInQueueSize() < queueSize) {
                feederCandidate = feeder;
                queueSize = feeder.getInQueueSize();
            }
        }
        if (feederCandidate == null) {
            log.warn("Unable to locate a MUXFilterFeeder for " + payload
                     + ". The Payload will be discarded");
        }
        return feederCandidate;
    }

    /* Objectfilter interface */

    public void setSource(Filter source) {
        if (!(source instanceof ObjectFilter)) {
            throw new IllegalArgumentException(String.format(
                    "The source must be an Objectfilter. Got '%s'",
                    source.getClass()));
        }
        this.source = (ObjectFilter)source;
        log.debug("Source specified. Starting mux-thread");
        new Thread(this).start();
    }

    public boolean pump() throws IOException {
        if (!hasNext()) {
            return false;
        }
        Payload next = next();
        next.close();
        return hasNext();
    }

    public void close(boolean success) {
        for (MUXFilterFeeder feeder: feeders) {
            feeder.close(success);
        }
        source.close(success);
    }

    public boolean hasNext() {
        while (true) {
            if (eofReached) {
                return false;
            }
            if (availablePayload != null) {
                return true;
            }
            availablePayload = getNextFilteredPayload();
        }
    }

    private int lastPolledPosition = -1;
    /**
     * @return the next available Payload from the feeders by round-robin.
     *         null is a valid value and does not indicate EOF (check for
     *         EOF with {@link #eofReached}).
     */
    private Payload getNextFilteredPayload() {
        if (feeders.size() == 0 || eofReached) {
            return null;
        }
        int feederPos = ++lastPolledPosition;
        while (true) {
            boolean allSaysEOF = true;
            /* Iterate through all feeders, starting from feederPos and wrapping
               to 0 when the last feeder is reached. If an available Payload is
               encountered, it is returned.
            */
            for (int counter = 0 ; counter < feeders.size() ; counter++) {
                if (feederPos++ > feeders.size()) {
                    feederPos = 0;
                }
                MUXFilterFeeder feeder = feeders.get(feederPos);
                if (!feeder.isEofReached()) {
                    allSaysEOF = false;
                    if (feeder.getOutQueueSize() > 0) {
                        return feeder.getNextFilteredPayload();
                    }
                }
            }
            if (allSaysEOF) {
                log.debug("All feeders says that EOF is reached");
                eofReached = true;
                return null;
            }
            log.trace("No Payloads was ready in any of the feeders. "
                      + "Sleeping a bit and retrying");
            // It's hard to do proper wait on multiple sources. Consider a flag
            try {
                Thread.sleep(POLL_INTERVAL);
            } catch (InterruptedException e) {
                log.warn("Interrupted while sleeping before re-querying "
                         + "feeders", e);
            }
        }
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
        throw new UnsupportedOperationException("Remove not supported");
    }
}
