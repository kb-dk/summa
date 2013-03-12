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
package dk.statsbiblioteket.summa.support.harmonise.hub;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubAggregatorBase;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Merges responses with the possibility of prioritizing certain sources over others.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TermStatRewriter implements Configurable {
    private static Log log = LogFactory.getLog(TermStatRewriter.class);

    public TermStatRewriter(Configuration conf) {
        log.info("Created " + this);
    }

    public List<HubAggregatorBase.ComponentCallable> adjustRequests(
            List<HubAggregatorBase.ComponentCallable> components) {
        log.warn("No termstat rewriting yet. Passing the queries unchanged");
        return components;
    }

    @Override
    public String toString() {
        return "TermStatRewriter(not properly implemented)";
    }
}
