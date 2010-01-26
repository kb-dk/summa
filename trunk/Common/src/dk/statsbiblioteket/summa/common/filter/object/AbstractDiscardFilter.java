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

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Logging;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.IOException;

/**
 * Building block for making a filter that discards Payloads based on some
 * implementation-specific conditions.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class AbstractDiscardFilter extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(AbstractDiscardFilter.class);

    @SuppressWarnings({"UnusedDeclaration"})
    public AbstractDiscardFilter(Configuration conf) {
        super(conf);
    }

    /**
     * Checks whether the Payload should be discarded or not.
     * @param payload the payload to check.
     * @return true if the Payload should be discarded.
     */
    protected abstract boolean checkDiscard(Payload payload);

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        boolean discard = checkDiscard(payload);
        if (discard) {
            Logging.logProcess(
                    getName() + "#" + this.getClass().getSimpleName(),
                    "Discarding", Logging.LogLevel.DEBUG, payload);
        } else {
            Logging.logProcess(
                    getName() + this.getClass().getSimpleName(),
                    "Payload not discarded", Logging.LogLevel.TRACE, payload);
        }
        return !discard;
    }
}

