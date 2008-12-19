/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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

import java.io.IOException;

import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Convenience implementation of ObjectFilter, suitable as a super-class for
 * filters. The implementation can only be chained after other ObjectFilters.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class ObjectFilterImpl implements ObjectFilter {
    private static Log log = LogFactory.getLog(ObjectFilterImpl.class);

    private ObjectFilter source;
    private long payloadCount = 0;
    private long totalTimeNS = 0;
    
    public boolean hasNext() {
        checkSource();
        return source.hasNext();
    }

    public Payload next() {
        checkSource();
        Payload payload = source.next();
        long startTime = System.nanoTime();
        processPayload(payload);
        long spendTime = System.nanoTime() - startTime;
        totalTimeNS += spendTime;
        payloadCount++;
        if (log.isTraceEnabled()) {
            log.trace("Processed " + payload + " (#" + payloadCount + " in "
                      + (spendTime / 1000000.0) + " ms using " + this);
        }
        return payload;
    }

    protected abstract void processPayload(Payload payload);

    public void remove() {
        // Do nothing as default
    }

    public synchronized void setSource(Filter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("Source filter was null");
        }
        if (!(filter instanceof ObjectFilter)) {
            throw new IllegalArgumentException("Only ObjectFilters accepted as "
                                               + "source. The filter provided "
                                               + "was of class "
                                               + filter.getClass());
        }
        source = (ObjectFilter)filter;
    }

    // TODO: Consider if close is a wise action - what about pooled ingests?
    public synchronized boolean pump() throws IOException {
        checkSource();
        if (!hasNext()) {
            return false;
        }
        Payload payload = next();
        if (payload != null) {
            payload.close();
        }
        return hasNext();
    }

    public synchronized void close(boolean success) {
        log.debug(String.format(
                "Closing down '%s'. %s", this, getProcessStats()));
        checkSource();
        source.close(success);
    }

    private void checkSource() {
        if (source == null) {
            throw new IllegalStateException("No source defined for "
                                            + "CreateDocument filter");
        }
    }

    /**
     * @return simple statistics on processed Payloads.
     *         Sample output: "Processed 1234 Payloads at 1.43232 ms/Payload" 
     */
    public String getProcessStats() {
        return String.format(
                "Processed %d Payloads at %s ms/Payload",
                payloadCount, payloadCount == 0 ? "NA" :
                totalTimeNS / 1000000.0 / payloadCount);
    }
 }
