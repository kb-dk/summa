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
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Building block for making a filter that discards Payloads based on some
 * implementation-specific conditions.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class AbstractDiscardFilter extends ObjectFilterImpl {

    /**
     * If true, Payloads determined to be discarded are marked as deleted instead of actually being discarded.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_MARK = "discard.markpayload";
    public static final boolean DEFAULT_MARK = false;

    /**
     * If true, discarded records are logged in the process log.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_LOG_DISCARDS = "discard.logdiscards";
    public static final boolean DEFAULT_LOG_DISCARDS = true;

    protected boolean logDiscards;
    protected boolean markDiscards;
    protected long totalCount = 0;
    protected long discardCount = 0;

    @SuppressWarnings({"UnusedDeclaration"})
    public AbstractDiscardFilter(Configuration conf) {
        super(conf);
        markDiscards = conf.getBoolean(CONF_MARK, DEFAULT_MARK);
        logDiscards = conf.getBoolean(CONF_LOG_DISCARDS, DEFAULT_LOG_DISCARDS);
    }

    /**
     * Checks whether the Payload should be discarded or not.
     *
     * @param payload the payload to check.
     * @return true if the Payload should be discarded.
     */
    protected abstract boolean checkDiscard(Payload payload);

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        totalCount++;
        boolean discard = checkDiscard(payload);
        if (discard) {
            discardCount++;
        }
        if (!discard) {
            if (logDiscards) {
                Logging.logProcess(getName(), "Payload not discarded", Logging.LogLevel.TRACE, payload);
            }
            return true;
        }
        // Discard signaled
        if (markDiscards) {
            if (payload.getRecord() == null) {
                Logging.logProcess(getName(),
                                   "Payload.record should be marked as deleted but did not contain a Record. " +
                                   "Discarding Payload", Logging.LogLevel.WARN, payload);
                return false;
            } else {
                if (logDiscards) {
                    Logging.logProcess(getName(), "Marking as deleted", Logging.LogLevel.DEBUG, payload);
                }
                payload.getRecord().setDeleted(true);
                return true;
            }
        }
        if (logDiscards) {
            Logging.logProcess(getName(), "Discarding payload #" + discardCount + "/" + totalCount,
                               Logging.LogLevel.DEBUG, payload);
        }
        return !discard;
    }

    @Override
    public String toString() {
        return "AbstractDiscardFilter(logDiscards=" + logDiscards + ", markDiscards=" + markDiscards
               + ", discarded=" + discardCount + "/" + totalCount + ")";
    }
}
