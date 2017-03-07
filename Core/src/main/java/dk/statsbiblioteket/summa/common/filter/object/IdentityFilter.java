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
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Passes all Payloads unchanged. Can be used with a muxer for processing some
 * Payloads while not touching others.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_OK,
        author = "te")
public class IdentityFilter extends ObjectFilterImpl  {
    private static Log log = LogFactory.getLog(IdentityFilter.class);

    /**
     * Not exactly identity... If true, the content of records is uncompressed (if it is not already uncompressed).
     * This can be used for optimization if subsequent filters access otherwise compressed content multiple times.
     * This has no effect if the Payload contains a Stream and not a Record.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_UNCOMPRESS = "identity.uncompress";
    public static final boolean DEFAULT_UNCOMPRESS = false;

    private final boolean uncompress;

    public IdentityFilter(Configuration conf) {
        super(conf);
        uncompress = conf.getBoolean(CONF_UNCOMPRESS, DEFAULT_UNCOMPRESS);
        log.debug("Created " + this);
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (log.isTraceEnabled() && payload.getRecord() != null) {
            Record record = payload.getRecord();
            log.trace(getName() + " processing " + record.getId() + " with compressed=" + record.isContentCompressed()
                      + " and raw content.length=" + record.getContent(false).length);
        }
        if (uncompress && payload.getRecord() != null && payload.getRecord().isContentCompressed()) {
            Logging.logProcess("IdentityFilter", "Uncompressing Payload content", Logging.LogLevel.TRACE, payload);
            final int compressedSize = payload.getRecord().getContent(false).length;
            final long startTime = System.nanoTime();
            RecordUtil.adjustCompression(payload.getRecord(), null, false);
            if (log.isDebugEnabled()) {
                Record record = payload.getRecord();
                log.debug(String.format("%s uncompressed %s content from %d to %d bytes in %.1f ms",
                                        getName(), record.getId(), compressedSize, record.getContent(false).length,
                                        (System.nanoTime()-startTime)/1000000.0));
            }
        } else if (log.isDebugEnabled()) {
            Logging.logProcess("IdentityFilter", "Passing Payload unmodified", Logging.LogLevel.TRACE, payload);
        }
        return true;
    }

    @Override
    public String toString() {
        return "IdentityFilter(uncompress=" + uncompress + ")";
    }
}
