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
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;

import java.util.Set;
import java.util.HashSet;

/**
 * Keeps track of the ids of processed Records. Records with new ids are passed
 * along while Records with already encountered ids are discarded.
 * </p><p>
 * The design scenario is batch ingesting of a huge amount of records with a
 * lot of updates. By using a FileReader in reverse order and this filter,
 * only the latest versions of the Records are processed.
 * </p><p>
 * It is expected that this filter will be extended in the future to contact a
 * Storage upon startup to determine the ids of already ingested Records.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DiscardUpdatesFilter extends AbstractDiscardFilter {
//    private static Log log = LogFactory.getLog(DiscardUpdatesFilter.class);

    private Set<String> encountered = new HashSet<String>(10000);

    @SuppressWarnings({"UnusedDeclaration"})
    public DiscardUpdatesFilter(Configuration conf) {
        super(conf);
        feedback = false;
        // No configuration for this filter
    }

    @Override
    protected boolean checkDiscard(Payload payload) {
        if (!encountered.contains(payload.getId())) {
            encountered.add(payload.getId());
            return false;
        }
        return true;
    }
}

