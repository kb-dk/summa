/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
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

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Discards Payloads based on the size of their content.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class DiscardSizeFilter extends AbstractDiscardFilter {
    private static Log log = LogFactory.getLog(DiscardSizeFilter.class);

    /**
     * The size of content must be at least this many bytes for the Record to be passed.
     * </p><p>
     * Optional. Default is 0.
     */
    public static final String CONF_MIN = "discard.size.min";
    public static final int DEFAULT_MIN = 0;

    /**
     * The size of content must be at most this many bytes for the Record to be passed.
     * </p><p>
     * Optional. -1 means no limit. Default is -1.
     * </p>
     */
    public static final String CONF_MAX = "discard.count.max";
    public static final int DEFAULT_MAX = -1;

    /**
     * If true and the record content is compressed, it is uncompressed to check the true size.
     * If false, the content size is taken as-is.
     * </p><p>
     * Warning: Setting this to true results in a significant processing overhead for large records.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_UNCOMPRESS = "discard.count.uncompress";
    public static final boolean DEFAULT_UNCOMPRESS = false;

    /**
     * The log level to use when flagging a record as discardable.
     * See {@link dk.statsbiblioteket.summa.common.Logging.LogLevel}
     * </p><p>
     * Optional. Default is 'info'.
     */
    public static final String CONF_SIZE_LOGLEVEL = "discard.count.loglevel";
    public static final Logging.LogLevel DEFAULT_SIZE_LOGLEVEL = Logging.LogLevel.INFO;


    private final int min;
    private final int max;
    private final boolean uncompress;
    private final Logging.LogLevel logLevel;
    private int maxEncountered = -1;
    private String maxID = "N/A";

    public DiscardSizeFilter(Configuration conf) {
        super(conf);
        min = conf.getInt(CONF_MIN, DEFAULT_MIN);
        max = conf.getInt(CONF_MAX, DEFAULT_MAX);
        uncompress = conf.getBoolean(CONF_UNCOMPRESS, DEFAULT_UNCOMPRESS);
        logLevel = Logging.LogLevel.valueOf(conf.getString(CONF_SIZE_LOGLEVEL, DEFAULT_SIZE_LOGLEVEL.toString()));
        this.feedback = false;
        log.info("Created " + this);
    }

    @Override
    protected boolean checkDiscard(Payload payload) {
        if (min == 0 && max == -1) {
            return false;
        }
        if (payload.getRecord() == null) { // Fail early and hard
            throw new IllegalArgumentException("Unable to determine size of Streams");
        }

        int size = payload.getRecord().getContent(payload.getRecord().isContentCompressed() && uncompress).length;
        if (size > maxEncountered) {
            maxEncountered = size;
            maxID = payload.getId();
        }
        if (size < min) {
            Logging.logProcess("DiscardSizeFilter", "Discarding as content size " + size + " < min size " + min,
                               logLevel, payload);
            return true;
        }
        if (max != -1 && size > max) {
            Logging.logProcess("DiscardSizeFilter", "Discarding as content size " + size + " > max size " + max,
                               logLevel, payload);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "DiscardSizeFilter(min=" + min + ", max=" + max + ", logLevel=" + logLevel
               +  ", (" + super.toString() + "))";
    }

    @Override
    public void close(boolean success) {
        log.info("Shutting down with max size encountered " + maxEncountered + " for " + maxID);
        super.close(success);
    }
}
