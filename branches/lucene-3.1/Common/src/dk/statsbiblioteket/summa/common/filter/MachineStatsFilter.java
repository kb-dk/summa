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
package dk.statsbiblioteket.summa.common.filter;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.util.MachineStats;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Debug oriented filter which extracts stats like free heap and loaded classed.
 * Optionally it performs explicit garbage collections.
 * </p><p>
 * Statistics are logged at DEBUG level.
 * </p><p>
 * See {@link MachineStats} for setup parameters.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MachineStatsFilter extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(MachineStatsFilter.class);

    private MachineStats stats;

    public MachineStatsFilter(Configuration conf) {
        super(conf);
        stats = new MachineStats(conf);
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        stats.ping();
        return true;
    }

    @Override
    public void close(boolean success) {
        log.debug("Closing down");
        stats.close();
        super.close(success);
    }
}

