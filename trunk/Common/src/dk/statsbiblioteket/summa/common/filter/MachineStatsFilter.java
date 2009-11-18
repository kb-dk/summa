/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2009  The State and University Library
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
