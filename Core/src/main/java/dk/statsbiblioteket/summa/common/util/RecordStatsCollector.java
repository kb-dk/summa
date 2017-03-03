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

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Helper class for delivering statistics for Records.
 * This class is Thread-safe.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class RecordStatsCollector {
    private static Log log = LogFactory.getLog(RecordStatsCollector.class);

    /**
     * Log on IFO level for every x Records processed. 0 means disabled.
     * </p><p>
     * Optional. Default is 0.
     */
    public static final String CONF_INFO_LOG_EVERY = "stat.infolog.every";
    public static final int DEFAULT_INFO_LOG_EVERY = 0;

    /**
     * The name of this logger. Will be stated on logging.
     * </p><p>
     * Optional. Default is {@code this.getClass.getSimpleName()};
     */
    public static final String CONF_NAME = "stat.name";

    private final int logEvery;
    private final String name;

    private long recordCount = 0;
    private long sizeSum = 0;

    private String smallestID = null;
    private long smallestSize = Long.MAX_VALUE;

    private String largestID = null;
    private long largestSize = -1;

    private String lastID = null;
    private long lastSize = -1;

    public RecordStatsCollector(String name, int logEvery) {
        this.name = name;
        this.logEvery = logEvery;
        log.debug("Created " + this);
    }

    public RecordStatsCollector(String name, Configuration conf) {
        this(name, conf.getInt(CONF_INFO_LOG_EVERY, DEFAULT_INFO_LOG_EVERY));
    }

    public RecordStatsCollector(Configuration conf) {
        this.name = conf.getString(CONF_NAME, this.getClass().getSimpleName());
        this.logEvery = conf.getInt(CONF_INFO_LOG_EVERY, DEFAULT_INFO_LOG_EVERY);
        log.debug("Created " + this);
    }

    /**
     * Shortcut for {@code process(payload.getRecord())}.
     */
    public void process(Payload payload) {
        process(payload.getRecord());
    }

    /**
     * @param record the record to add to stats.
     * @return the number of all added Records.
     */
    public long process(Record record) {
        if (record == null) {
            return recordCount;
        }
        final String id = record.getId();
        final long size = record.getContent(false).length;
        return process(id, size);
    }

    public synchronized long process(String id, long size) {
        recordCount++;

        if (size < smallestSize) {
            smallestID = id;
            smallestSize = size;
        }
        if (size > largestSize) {
            largestID = id;
            largestSize = size;
        }
        lastID = id;
        lastSize = size;
        sizeSum += size;

        maybeLog();
        return recordCount;
    }

    private void maybeLog() {
        if (log.isDebugEnabled()) {
            log.debug(getLogMessage());
            return;
        }
        if (logEvery > 0 && recordCount % logEvery == 0) {
            log.info(getLogMessage());
        }
    }

    public void close() {
        log.info(getLogMessage());
    }

    private String getLogMessage() {
        return name + "(records=" + recordCount + ", average=" + (recordCount == 0 ? 0 : sizeSum/recordCount/1024)
               + "KB, smallest=" + pack(smallestID, smallestSize) + ", largest=" + pack(largestID, largestSize)
               + ", last=" + pack(lastID, lastSize) + ")";
    }

    private String pack(String recordID, long recordSize) {
        return "(size=" + recordSize/1024 + "KB, ID="  + recordID + ")";
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getLogMessage();
    }
}
