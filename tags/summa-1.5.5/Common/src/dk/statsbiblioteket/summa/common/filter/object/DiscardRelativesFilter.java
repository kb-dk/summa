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
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Discards Records with parents or children, depending on setup.
 * The filter requires that received Records are expanded, so only realized
 * relatives count in determining family. ID-only references are ignored.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DiscardRelativesFilter extends AbstractDiscardFilter {
    private static Log log = LogFactory.getLog(DiscardRelativesFilter.class);

    /**
     * If true, all Records with one or more parents are discarded.
     * </p><p>
     * Optional. Default is false;
     */
    public static final String CONF_DISCARD_HASPARENT =
            "summa.relativesfilter.discard.hasparent";
    public static final boolean DEFAULT_DISCARD_HASPARENT = false;

    /**
     * If true, all Records with one or more children are discarded.
     * </p><p>
     * Optional. Default is false;
     */
    public static final String CONF_DISCARD_HASCHILDREN =
            "summa.relativesfilter.discard.haschildren";
    public static final boolean DEFAULT_DISCARD_HASCHILDREN = false;

    private boolean discardHasParent = DEFAULT_DISCARD_HASPARENT;
    private boolean discardHasChildren = DEFAULT_DISCARD_HASCHILDREN;

    public DiscardRelativesFilter(Configuration conf) {
        super(conf);
        feedback = false;
        discardHasParent =
                conf.getBoolean(CONF_DISCARD_HASPARENT, discardHasParent);
        discardHasChildren =
                conf.getBoolean(CONF_DISCARD_HASCHILDREN, discardHasChildren);
        log.debug("Created relativesFilter with discardHasParent="
                  + discardHasParent + " and discardHasChildren="
                  + discardHasChildren);
    }

    @Override
    protected boolean checkDiscard(Payload payload) {
        if (log.isTraceEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("checkDiscard(" + payload + ") called with "
                      + payload.getRecord());
        }
        if (payload.getRecord() == null) {
            return false;
        }
        log.trace("Checking for parents");
        List<Record> parents = payload.getRecord().getParents();
        if (discardHasParent && parents != null && parents.size() > 0) {
            Logging.logProcess(
                    getName() + "#" + this.getClass().getSimpleName(),
                    "Discarding due to parent existence",
                    Logging.LogLevel.TRACE, payload);
            //noinspection DuplicateStringLiteralInspection
            log.debug("Discarding " + payload + " due to parent existence");
            return true;
        }
        log.trace("Checking for children");
        List<Record> children = payload.getRecord().getChildren();
        if (discardHasChildren && children != null && children.size() > 0) {
            Logging.logProcess(
                    getName() + "#" + this.getClass().getSimpleName(),
                    "Discarding due to children existence",
                    Logging.LogLevel.TRACE, payload);
            //noinspection DuplicateStringLiteralInspection
            log.debug("Discarding " + payload + " due to children existence");
            return true;
        }
        return false;
    }
}

