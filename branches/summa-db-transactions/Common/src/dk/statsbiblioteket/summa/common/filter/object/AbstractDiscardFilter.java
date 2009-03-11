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
public abstract class AbstractDiscardFilter implements ObjectFilter {
    private static Log log = LogFactory.getLog(AbstractDiscardFilter.class);

    private ObjectFilter source;
    private Payload payload;

    @SuppressWarnings({"UnusedDeclaration"})
    public AbstractDiscardFilter(Configuration conf) {
        // No setup
    }

    /**
     * Checks whether the Payload should be discarded or not.
     * @param payload the payload to check.
     * @return true if the Payload should be discarded.
     */
    protected abstract boolean checkDiscard(Payload payload);

    public void setSource(Filter filter) {
        if (filter == null) {
            //noinspection DuplicateStringLiteralInspection
            throw new IllegalArgumentException("Source filter was null");
        }
        if (!(filter instanceof ObjectFilter)) {
            throw new IllegalArgumentException(
                    "Only ObjectFilters accepted as source. The filter "
                    + "provided was of class " + filter.getClass());
        }
        source = (ObjectFilter)filter;
    }

    public boolean pump() throws IOException {
        log.trace("Pump called");
        if (!hasNext()) {
            return false;
        }
        Payload payload = next();
        if (payload != null) {
            payload.close();
        }
        return hasNext();
    }

    public void close(boolean success) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("Close(" + success + ") called");
        source.close(success);
    }

    public boolean hasNext() {
        checkPayload();
        return payload != null;
    }

    public Payload next() {
        checkPayload();
        try {
            return payload;
        } finally {
            payload = null;
        }
    }

    private void checkPayload() {
        while (payload == null && source.hasNext()) {
            Payload potential = source.next();
            if (potential != null) {
                if (checkDiscard(potential)) {
                    if (log.isDebugEnabled()) {
                        //noinspection DuplicateStringLiteralInspection
                        log.debug("Discarding " + payload);
                    }
                    potential.close();
                } else {
                    payload = potential;
                }
            }
        }
    }

    public void remove() {
        log.warn("Removal not supported for DiscardFilter");
    }


}
