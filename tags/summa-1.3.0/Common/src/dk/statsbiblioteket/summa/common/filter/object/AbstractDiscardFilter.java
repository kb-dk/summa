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
                    "Discarding due to unwanted relatives (pun intended)",
                    Logging.LogLevel.TRACE, payload);
        } else {
            Logging.logProcess(
                    getName() + this.getClass().getSimpleName(),
                    "No offending relatives: Payload not discarded",
                    Logging.LogLevel.TRACE, payload);
        }
        return discard;
    }
}
