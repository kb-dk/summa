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
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.util.RecordStatsCollector;
import dk.statsbiblioteket.util.Timing;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * Stats-oriented base for building ObjectFilters.
 * </p><p>
 * Sub-classes are responsible for updating timing & stats as well as calling {@link #logStatusIfNeeded()} on every
 * processed Payload.
 * </p><p>
 * Note: Default instrumentation logging is process time and process size only.
 */
public abstract class ObjectFilterBase implements ObjectFilter {
    protected Log log = LogFactory.getLog(ObjectFilterBase.class.getName() + "#" + this.getClass().getSimpleName());

    /**
     * The feedback level used to log statistics to the process log.
     * Valid values are FATAL, ERROR, WARN, INFO, DEBUG and TRACE.
     * </p><p>
     * Optional. Default is TRACE.
     */
    public static final String CONF_PROCESS_LOGLEVEL = "process.loglevel";
    public static final Logging.LogLevel DEFAULT_FEEDBACK = Logging.LogLevel.TRACE;

    /**
     * Log overall status in the class log on INFO for every x Payloads processed.
     * </p><p>
     * Optional. Default is 0 (disabled).
     */
    public static final String CONF_STATUS_EVERY = "process.status.every";
    public static final int DEFAULT_STATUS_EVERY = 0;

    /**
     * Show pull timing stats as part of feedback.
     * </p><p>
     * Optional. Default is false;
     */
    public static final String CONF_SHOWSTATS_PULL_TIMING = "process.showstats.pull.timing";
    public static final boolean DEFAULT_SHOWSTATS_PULL_TIMING = false;

    /**
     * Show pull size stats as part of feedback.
     * </p><p>
     * Optional. Default is false;
     */
    public static final String CONF_SHOWSTATS_PULL_SIZE = "process.showstats.pull.size";
    public static final boolean DEFAULT_SHOWSTATS_PULL_SIZE = false;

    /**
     * Show process timing stats as part of feedback.
     * </p><p>
     * Optional. Default is true;
     */
    public static final String CONF_SHOWSTATS_PROCESS_TIMING = "process.showstats.process.timing";
    public static final boolean DEFAULT_SHOWSTATS_PROCESS_TIMING = true;

    /**
     * Show process size stats as part of feedback.
     * </p><p>
     * Optional. Default is true;
     */
    public static final String CONF_SHOWSTATS_PROCESS_SIZE = "process.showstats.process.size";
    public static final boolean DEFAULT_SHOWSTATS_PROCESS_SIZE = true;

    // Sub-class-overridable tweaks to output
    protected boolean feedback = true; // Log running feedback, on DEBUG
    protected boolean showPullTimingStats;
    protected boolean showProcessTimingStats;
    protected boolean showPullSizeStats;
    protected boolean showProcessSizeStats;

    // The name of the filter
    private String name;

    private final int everyStatus;
    // If true, process-time statistics are logged after processPayload-calls
    private Logging.LogLevel processLogLevel;

    protected final Timing timingPull;
    protected final Timing timingProcess;
    protected final RecordStatsCollector sizePull;
    protected final RecordStatsCollector sizeProcess;

    public ObjectFilterBase(Configuration conf) {
        name = conf.getString(CONF_FILTER_NAME, this.getClass().getSimpleName());
        processLogLevel = Logging.LogLevel.valueOf(conf.getString(CONF_PROCESS_LOGLEVEL, DEFAULT_FEEDBACK.toString()));
        everyStatus = conf.getInt(CONF_STATUS_EVERY, DEFAULT_STATUS_EVERY);
        //timing = new Timing(name, null, "Payload", new Timing.STATS[]{Timing.STATS.ms}); // FIXME: Also affects children
        timingPull = createTimingPull();
        timingProcess = createTimingProcess();
        sizePull = createSizePull(conf);
        sizeProcess = createSizeProcess(conf);
        setStatsDefaults(conf, DEFAULT_SHOWSTATS_PULL_TIMING, DEFAULT_SHOWSTATS_PULL_SIZE,
                         DEFAULT_SHOWSTATS_PROCESS_TIMING, DEFAULT_SHOWSTATS_PROCESS_SIZE);
        // Don't log in the constructor as this class is always subclassed: Initialization probably hasn't finished
   //     log.info("Created " + this);
    }

    protected void setStatsDefaults(
            Configuration conf, boolean pullTiming, boolean pullSize, boolean processTiming, boolean processSize) {
        showPullTimingStats = conf.getBoolean(CONF_SHOWSTATS_PULL_TIMING, pullTiming);
        showPullSizeStats = conf.getBoolean(CONF_SHOWSTATS_PULL_SIZE, pullSize);

        showProcessTimingStats = conf.getBoolean(CONF_SHOWSTATS_PROCESS_TIMING, processTiming);
        showProcessSizeStats = conf.getBoolean(CONF_SHOWSTATS_PROCESS_SIZE, processSize);
    }

    protected void logStatusIfNeeded() {
        if (everyStatus > 0) {
            final long updates = getUpdates();
            if (updates > 0 && updates % everyStatus == 0) {
                log.info(getName() + ": " + getProcessStats());
            }
        }
    }

    protected void logProcess(Payload payload, long ns) {
        final double ms = ns / 1000000.0;

        if (log.isTraceEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.trace(String.format("%s processed %s, #%d, in %.1f ms using %s",
                                    getName(), payload, getUpdates(), ms, this));
        } else if (log.isDebugEnabled() && feedback) {
            if (showPullSizeStats || showProcessSizeStats) {
                log.debug(String.format("%s processed Payload #%d, in %.1f ms, %s",
                                        getName(), getUpdates(), ms, getProcessStats()));
            } else {
                log.debug(String.format("%s processed %s, #%d, in %.1f ms, %s",
                                        getName(), payload, getUpdates(), ms, getProcessStats()));
            }
        }

        Logging.logProcess(name, "processPayload #" + getUpdates()
                                 + " finished in " + ns / 1000000 + "ms for " + name,
                           processLogLevel, payload);
    }

    private long getUpdates() {
        return showPullTimingStats ? timingPull.getUpdates() : showProcessTimingStats ? timingProcess.getUpdates() :
                showPullSizeStats ? sizePull.getRecordCount() : showProcessSizeStats ? sizeProcess.getRecordCount() :
                        0;
    }

    @Override
    public void remove() {
        // Do nothing as default
    }

    // TODO: Consider if close is a wise action - what about pooled ingests?
    @Override
    public boolean pump() throws IOException {
        if (!hasNext()) {
            log.trace("pump(): hasNext() returned false");
            return false;
        }
        Payload payload = next();
        if (payload != null) {
            //noinspection DuplicateStringLiteralInspection
            Logging.logProcess(getName(),
                               "Calling close for Payload as part of pump()",
                               Logging.LogLevel.TRACE, payload);
            payload.close();
        }
        return hasNext();
    }

    @Override
    public void close(boolean success) {
        log.info(String.format("Closing down %s with success=%b and final stats %s",
                               getName(), success, getProcessStats()));
    }

    /**
     * @return statistics on processed Payloads.
     */
    @SuppressWarnings("ConstantConditions")
    public String getProcessStats() {
        final StringBuilder sb = new StringBuilder(500);

        sb.append("timing=(");
        if (showPullTimingStats && showProcessTimingStats) {
            sb.append(String.format("%s, %s)", timingPull, timingProcess));
        }
        if (showPullTimingStats && !showProcessTimingStats) {
            sb.append(String.format("%s)", timingPull));
        }
        if (!showPullTimingStats && showProcessTimingStats) {
            sb.append(String.format("%s)", timingProcess));
        }
        if (!showPullTimingStats && !showProcessTimingStats) {
            sb.append("N/A)");
        }

        sb.append(", size=(");
        if (showPullSizeStats && showProcessSizeStats) {
            sb.append(String.format("%s, %s)", sizePull, sizeProcess));
        }
        if (showPullSizeStats && !showProcessSizeStats) {
            sb.append(String.format("%s)", sizePull));
        }
        if (!showPullSizeStats && showProcessSizeStats) {
            sb.append(String.format("%s)", sizeProcess));
        }
        if (!showPullSizeStats && !showProcessSizeStats) {
            sb.append("N/A)");
        }

        return sb.toString();
    }

    /* Override the stats methods to provide custom feedback */
    protected RecordStatsCollector createSizePull(Configuration conf) {
        return new RecordStatsCollector("in", conf, false);
    }
    protected Timing createTimingPull() {
        return new Timing("pull", null, "Payload");
    }

    protected RecordStatsCollector createSizeProcess(Configuration conf) {
        return new RecordStatsCollector("out", conf, false);
    }
    protected Timing createTimingProcess() {
        return new Timing("process", null, "Payload");
    }

    /**
     * @return the name of the filter, if specified. Else the class name of the object.
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName() + "(feedback=" + feedback + ", processLogLevel=" + processLogLevel
               + ", " + getProcessStats() + ")";
    }
}
