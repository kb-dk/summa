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
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Delays the processing of Records. Useful for avoiding spikes in machine-load
 * and for testing. The delays are all coupled to the {@link #next}-method,
 * so even though {@link #hasNext} replies true, that does not mean that the
 * next Payload will be delivered immediately by next().
 * </p><p>
 * Note: All delays are in nano-seconds (1 milli-second = 1000000 nano-seconds).
 *       The filter uses {@link Thread#sleep(long, int)} for delays, so the
 *       exactness of the delays depends on the underlying runtime system.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DelayFilter extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(DelayFilter.class);

    /**
     * A fixed delay in ns before requesting a Payload from the source as part
     * of the {@link #next}-operation.
     * </p><p>
     * Optional. Default is 0 ns.
     */
    public static final String CONF_FIXED_DELAY_PREREQUEST =
            "summa.filter.delay.fixed.prerequest";
    public static final long DEFAULT_FIXED_DELAY_PREREQUEST = 0;

    /**
     * A fixed delay in ns after requesting a Payload from the source as part
     * of the {@link #next}-operation, but before passing the Payload on to
     * the caller.
     * </p><p>
     * Optional. Default is 0 ns.
     */
    public static final String CONF_FIXED_DELAY_POSTREQUEST =
            "summa.filter.delay.fixed.postrequest";
    public static final long DEFAULT_FIXED_DELAY_POSTREQUEST = 0;

    /**
     * The minimum amount of ns between Payloads delivered by {@link #next}.
     * </p><p>
     * Example: Setting this to 5000000 ensures that a maximum of 1000/5 = 200
     * Payloads will be passed along each second.
     * </p><p>
     * Optional. Default is 0 ns.
     */
    public static final String CONF_MIN_DELAY_BETWEEN_PAYLOADS =
            "summa.filter.delay.mindelay.betweenpayloads";
    public static final long DEFAULT_MIN_DELAY_BETWEEN_PAYLOADS = 0;

    private long preRequestDelay = DEFAULT_FIXED_DELAY_PREREQUEST;
    private long postRequestDelay = DEFAULT_FIXED_DELAY_POSTREQUEST;
    private long minDelay = DEFAULT_MIN_DELAY_BETWEEN_PAYLOADS;
    private long lastPayload = 0;

    public DelayFilter(Configuration conf) {
        super(conf);
        preRequestDelay = conf.getLong(
                CONF_FIXED_DELAY_PREREQUEST, preRequestDelay);
        postRequestDelay = conf.getLong(
                CONF_FIXED_DELAY_POSTREQUEST, postRequestDelay);
        minDelay = conf.getLong(
                CONF_MIN_DELAY_BETWEEN_PAYLOADS, minDelay);
        log.debug("Constructed DelayFilter with "
                  + CONF_FIXED_DELAY_PREREQUEST + "=" + preRequestDelay + "ns, "
                  + CONF_FIXED_DELAY_POSTREQUEST + "=" + postRequestDelay
                  + "ns, " + CONF_MIN_DELAY_BETWEEN_PAYLOADS + "=" + minDelay
                  + "ns");
    }

    @Override
    public Payload next() {
        sleep(preRequestDelay);
        Payload next = super.next();
        sleep(postRequestDelay);
        sleep(minDelay - (System.nanoTime() -lastPayload));
        lastPayload = System.nanoTime();
        return next;
    }

    @Override
    protected boolean processPayload(Payload payload) {
        // We don't do processing on the payload itself
        return true;
    }

    private void sleep(long ns) {
        if (ns <= 0) {
            return;
        }
        try {
            long startTime = System.nanoTime();
            if (log.isTraceEnabled()) {
                //noinspection DuplicateStringLiteralInspection
                log.trace("Sleeping " + ns + "ns = "
                          + (ns / 1000000) + "ms + "
                          + (ns % 1000000) + "ns");
            }
            long remaining;
            while ((remaining = System.nanoTime() - startTime) < ns) {
                // We loop to ensure that we wait at least ns
                Thread.sleep(remaining / 1000000, (int)(remaining % 1000000));
            }
            log.trace("Finished sleeping " + ns + "ns");
        } catch (InterruptedException e) {
            //noinspection DuplicateStringLiteralInspection
            log.warn("Interrupted while sleeping " + ns + "ns. Skipping sleep");
        }
    }
}
