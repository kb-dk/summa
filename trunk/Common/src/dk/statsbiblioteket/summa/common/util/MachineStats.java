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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.management.ManagementFactory;
import java.util.Locale;

/**
 * continuously provides stats for memory consumption, running Threads etc.
 * Optionally requests a full garbage collection before dumping stats.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MachineStats implements Runnable {
    private static Log log = LogFactory.getLog(MachineStats.class);

    /**
     * If defined, a log-action is performed at intervals defined by the
     * given number of pings.
     * </p><p>
     * 0 means that this parameter is not used as trigger.
     * </p><p>
     * Optional. Default is 0 (disabled);
     */
    public static final String CONF_LOG_INTERVAL_PINGS =
            "summa.machinestats.log.interval.pings";
    public static final int DEFAULT_LOG_INTERVAL_PINGS = 0;

    /**
     * If defined, a log-action is performed at intervals defined by the
     * given number of milliseconds.
     * </p><p>
     * 0 means that this parameter is not used as trigger.
     * </p><p>
     * Optional. Default is 60000; // 1 minute
     */
    public static final String CONF_LOG_INTERVAL_MS =
            "summa.machinestats.log.interval.ms";
    public static final int DEFAULT_LOG_INTERVAL_MS = 60 * 1000;

    /**
     * If true, {@link System#gc()} is called before each logging. Note that
     * this requests a full garbage collection from the JVM. If the JVM honors
     * the request, it will have influence on performance.
     * </p><p>
     * Optional. Default is false;
     */
    public static final String CONF_GC_BEFORE_LOG =
            "summa.machinestats.gc.before-log";
    public static final boolean DEFAULT_GC_BEFORE_LOG = false;

    /**
     * If defined, the statistics thread sleeps the given number of milliseconds
     * after af call to {@link System#gc()}, before it logs. As the Thread is
     * independent of the Ping flow, this does not hamper performance.
     * </p><p>
     * Optional. Default is 5 ms.
     */
    public static final String CONF_GC_SLEEP_MS =
            "summa.machinestats.gc.sleep-ms";
    public static final int DEFAULT_GC_SLEEP_MS = 5;

    /**
     * A name describing the runtime, such as "Searcher", "Storage" or
     * "CSA ingest". This will be part of the logs.
     * </p><p>
     * Optional. Default is "Anonymous".
     */
    public static final String CONF_DESIGNATION =
            "summa.machinestats.designation";
    public static final String DEFAULT_DESIGNATION = "Anonymous";

    private int logIntervalPings = DEFAULT_LOG_INTERVAL_PINGS;
    private int logIntervalMS =    DEFAULT_LOG_INTERVAL_MS;
    private boolean gcBeforeLog =  DEFAULT_GC_BEFORE_LOG;
    private int gcSleepMS =        DEFAULT_GC_SLEEP_MS;
    private String designation;

    private final Thread watcher;
    private long receivedPings = 0;
    private long lastLogCount = Long.MIN_VALUE;
    private long lastLogTime = 0;
    private boolean running = true;
    private dk.statsbiblioteket.util.Profiler profiler;

    public MachineStats(Configuration conf) {
        this(conf, conf.getString(CONF_DESIGNATION, DEFAULT_DESIGNATION));
    }

    public MachineStats(Configuration conf, String designation) {
        logIntervalPings = conf.getInt(
                CONF_LOG_INTERVAL_PINGS, logIntervalPings);
        logIntervalMS = conf.getInt(CONF_LOG_INTERVAL_MS, logIntervalMS);
        gcBeforeLog = conf.getBoolean(CONF_GC_BEFORE_LOG, gcBeforeLog);
        gcSleepMS = conf.getInt(CONF_GC_SLEEP_MS, gcSleepMS);
        this.designation = designation;
        log.debug(String.format(
                "Constructed MachineStatsFilter(logIntervalPings=%d, "
                + "logIntervalMS=%d, gcBeforeLog=%b, gcSleepMS=%d, "
                + "designation='%s')",
                logIntervalPings, logIntervalMS, gcBeforeLog, gcSleepMS,
                designation));
        if (logIntervalPings == 0 && logIntervalMS == 0) {
            log.info("Both logIntervalPings and logIntervalMS are 0. "
                     + "No logging will be performed");
            watcher = null;
            return;
        }
        profiler = new Profiler(500);
        watcher = new Thread(this, "MachineStats");
        watcher.setDaemon(true);
        watcher.start();
    }

    /**
     * Pings are counted and used to trigger stats if
     * {@link #CONF_LOG_INTERVAL_PINGS} is defined.
     */
    public void ping() {
        profiler.beat();
        receivedPings++;
        try {
            synchronized (watcher) {
                watcher.notifyAll();
            }
        } catch (Exception e) {
            log.warn("Exception while calling notifyAll on watcher. Stats will"
                     + " probably not be logged");
        }
    }

    @Override
    public void run() {
        while (running) {
            if ((logIntervalPings > 0
                 && (receivedPings - lastLogCount >= logIntervalPings))
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
                    log.trace("Interrupted while waiting. Processing will "
                              + "continue as before");
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
            "%s: Pings: %d, Runtime: %s, Average Pings/second: %.2f, "
            + "Allocated memory: %s, Allocated unused memory: %s, "
            + "Heap memory used: %s, Max memory: %s, "
            + "Threads: %d, Load average: %s",
            designation,
            receivedPings, profiler.getSpendTime(), profiler.getBps(true),
            reduce(r.totalMemory()),
            reduce(r.freeMemory()),
            reduce(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().
                    getUsed()),
            reduce(r.maxMemory()),
            ManagementFactory.getThreadMXBean().getThreadCount(),
            ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage()
            ));
        lastLogCount = receivedPings;
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

    public void close() {
        log.debug("Closing down");
        running = false;
    }
}

