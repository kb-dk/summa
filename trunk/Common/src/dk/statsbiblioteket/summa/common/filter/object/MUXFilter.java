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
 * One straight forward use case is to mux several {@link XMLTransformer}s,
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

    private ObjectFilter source = null;

    private List<MUXFilterFeeder> feeders;
    private Payload lastPolled = null;
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
        log.trace("Constructed feeders, starting to fill the feeders");
        new Thread(this).start();
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
                break;
            }
            MUXFilterFeeder feeder;
            try {
                feeder = getFeeder(nextPayload);
            } catch (Exception e) {
                log.error("Unexpected exception while getting feeder for "
                          + nextPayload);
                continue;
            }
            if (feeder == null) {
                continue;
            }
            try {
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
            if (lastPolled != null) {
                return true;
            }
            lastPolled = getNextFilteredPayload();
        }
    }

    private int lastPolledPosition;
    /**
     * @return the next available Payload from the feeders by round-robin.
     */
    private Payload getNextFilteredPayload() {
        if (eofReached) {
            return null;
        }
        lastPolled = null;
        boolean allSaysEOF = true;
        int counter = 0;
        while (!allSaysEOF && lastPolled == null && counter < feeders.size()) {
            // TODO: Implement round-robin here
        }
        return null;
    }

    public Payload next() {
        log.trace("Next() called");
        if (!hasNext()) {
            throw new IllegalStateException("No more elements");
        }
        Payload returnPayload = lastPolled;
        lastPolled = null;
        return returnPayload;
    }

    public void remove() {
        throw new UnsupportedOperationException("Remove not supported");
    }
}
