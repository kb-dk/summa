/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2009  The State and University Library
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
package dk.statsbiblioteket.summa.common.filter;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.management.ManagementFactory;
import java.util.Locale;

/**
 * Debug oriented filter which extracts stats like free heap and loaded classed.
 * Optionally it performs explicit garbage collections.
 * </p><p>
 * Statistics are logged at DEBUG level.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MachineStatsFilter extends ObjectFilterImpl implements Runnable {
    private static Log log = LogFactory.getLog(MachineStatsFilter.class);

    /**
     * If defined, a log-action is performed at intervals defined by the
     * given number of Payloads.
     * </p><p>
     * 0 means that this parameter is not used as trigger.
     * </p><p>
     * Optional. Default is 1000;
     */
    public static final String CONF_LOG_INTERVAL_PAYLOADS =
            "summa.machinestats.log.interval.payloads";
    public static final int DEFAULT_LOG_INTERVAL_PAYLOADS = 1000;

    /**
     * If defined, a log-action is performed at intervals defined by the
     * given number of milliseconds.
     * </p><p>
     * 0 means that this parameter is not used as trigger.
     * </p><p>
     * Optional. Default is 10000; // 10 seconds
     */
    public static final String CONF_LOG_INTERVAL_MS =
            "summa.machinestats.log.interval.ms";
    public static final int DEFAULT_LOG_INTERVAL_MS = 10 * 1000;

    /**
     * If true, {@link System#gc()} is called before each logging. Note that
     * this requests a full garbage collection from the JVM. If the JVM honors
     * the request, it will have influence on performance.
     * </p><p>
     * Optional. Default is true;
     */
    public static final String CONF_GC_BEFORE_LOG =
            "summa.machinestats.gc.beforelog";
    public static final boolean DEFAULT_GC_BEFORE_LOG = true;

    /**
     * If defined, the statistics thread sleeps the given number of milliseconds
     * after af call to {@link System#gc()}, before it logs. As the Thread is
     * independent of the Payload flow, this does not hamper performance.
     * </p><p>
     * Optional. Default is 5 ms.
     */
    public static final String CONF_GC_SLEEP_MS =
            "summa.machinestats.gc.sleepms";
    public static final int DEFAULT_GC_SLEEP_MS = 5;

    private int logIntervalPayloads = DEFAULT_LOG_INTERVAL_PAYLOADS;
    private int logIntervalMS =       DEFAULT_LOG_INTERVAL_MS;
    private boolean gcBeforeLog =     DEFAULT_GC_BEFORE_LOG;
    private int gcSleepMS =           DEFAULT_GC_SLEEP_MS;

    private final Thread watcher;
    private long receivedPayloads = 0;
    private long lastLogCount = Long.MIN_VALUE;
    private long lastLogTime = 0;
    private boolean running = true;
    private Profiler profiler;

    public MachineStatsFilter(Configuration conf) {
        super(conf);
        logIntervalPayloads = conf.getInt(
                CONF_LOG_INTERVAL_PAYLOADS, logIntervalPayloads);
        logIntervalMS = conf.getInt(CONF_LOG_INTERVAL_MS, logIntervalMS);
        gcBeforeLog = conf.getBoolean(CONF_GC_BEFORE_LOG, gcBeforeLog);
        gcSleepMS = conf.getInt(CONF_GC_SLEEP_MS, gcSleepMS);
        log.debug(String.format(
                "Constructed MachineStatsFilter(logIntervalPayloads=%d, "
                + "logIntervalMS=%d, gcBeforeLog=%b, gcSleepMS=%d)",
                logIntervalPayloads, logIntervalMS, gcBeforeLog, gcSleepMS));
        if (logIntervalPayloads == 0 && logIntervalMS == 0) {
            log.info("Both logIntervalPayloads and logIntervalMS are 0. No "
                     + "logging will be performed");
        }
        profiler = new Profiler(500);
        watcher = new Thread(this);
        watcher.start();
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        profiler.beat();
        receivedPayloads++;
        synchronized (watcher) {
            watcher.notifyAll();
        }
        return true;
    }

    public void run() {
        while (running) {
            if ((logIntervalPayloads > 0
                 && (receivedPayloads - lastLogCount >= logIntervalPayloads))
                || (logIntervalMS > 0
                    && (System.currentTimeMillis() - lastLogTime
                        > logIntervalMS))) {
                doStat();
            } else {
                try {
                    long waitTime = lastLogTime + logIntervalMS
                                    - System.currentTimeMillis();
                    if (waitTime > 0) {
                        synchronized (watcher) {
                            watcher.wait(waitTime);
                        }
                    }
                } catch (InterruptedException e) {
                    log.trace("interrupted while waiting. No problem, we just"
                              + "go on");
                }
            }
        }
        doStat();
        log.debug("run() finished");
    }

    private static final Locale locale = new Locale("en");
    private void doStat() {
        if (gcBeforeLog) {
            log.trace("Requesting gc");
            System.gc();
            if (gcSleepMS > 0) {
                try {
                    Thread.sleep(gcSleepMS);
                } catch (InterruptedException e) {
                    log.debug("Interrupted while sleeping " + gcSleepMS + "ms");
                }
            }
        }
        Runtime r = Runtime.getRuntime();
        log.debug(String.format(locale,
            "Processed Payloads: %d, Runtime: %s, Average Payloads/second: %.2f"
            + ", Free memory: %s, Max memory: %s, Total memory: %s, "
            + "Heap memory used: %s, Threads: %d, Load average: %s",
            receivedPayloads, profiler.getSpendTime(), profiler.getBps(true), 
            reduce(r.freeMemory()), reduce(r.maxMemory()),
            reduce(r.totalMemory()),
            reduce(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().
                    getUsed()),
            ManagementFactory.getThreadMXBean().getThreadCount(),
            ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage()
            ));
        lastLogCount = receivedPayloads;
        lastLogTime = System.currentTimeMillis();
    }

    private String reduce(long bytes) {
        return bytes / 1048576 + "MB";
/*        if (bytes > 1048576) {
            return bytes / 1048576 + "MB";
        } else if (bytes > 1024) {
            return bytes / 1024 + "KB";
        }
        return bytes + "bytes";*/
    }

    @Override
    public void close(boolean success) {
        doStat();
        log.debug("Closing down");
        running = false;
        super.close(success);
    }
}
