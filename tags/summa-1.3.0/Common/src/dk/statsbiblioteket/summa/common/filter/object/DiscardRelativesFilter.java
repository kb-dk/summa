/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Record;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

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
            log.trace("checkDiscard(" + payload + ") called");
        }
        if (payload.getRecord() == null) {
            return false;
        }
        log.trace("Checking for parents");
        List<Record> parents = payload.getRecord().getParents();
        if (discardHasParent && parents != null && parents.size() > 0) {
            //noinspection DuplicateStringLiteralInspection
            log.debug("Discarding " + payload + " due to parent existence");
            return true;
        }
        log.trace("Checking for children");
        List<Record> children = payload.getRecord().getChildren();
        if (discardHasChildren && children != null && children.size() > 0) {
            //noinspection DuplicateStringLiteralInspection
            log.debug("Discarding " + payload + " due to children existence");
            return true;
        }
        return false;
    }
}
