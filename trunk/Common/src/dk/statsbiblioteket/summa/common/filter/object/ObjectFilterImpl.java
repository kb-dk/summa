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
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Logging;
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
    private Log log = LogFactory.getLog(ObjectFilterImpl.class.getName() + "#"
                                        + this.getClass().getSimpleName());

    private ObjectFilter source;
    private long payloadCount = 0;
    private long totalTimeNS = 0;

    private String name;
    private Payload processedPayload = null;

    public ObjectFilterImpl(Configuration conf) {
        name = conf.getString(CONF_FILTER_NAME, this.getClass().getName());
    }

    // if hasNext is true, a processed Payload is ready for delivery
    public boolean hasNext() {
        if (processedPayload != null) {
            return true;
        }
        checkSource();
        while (processedPayload == null && source.hasNext()) {
            processedPayload = source.next();
            if (processedPayload == null) {
                log.debug("hasNext(): Got null from source. This is legal but"
                          + " unusual. Skipping to next payload");
                continue;
            }
            long startTime = System.nanoTime();
            try {
                log.trace("Processing Payload");
                processPayload(processedPayload);
            } catch (Exception e) {
                Logging.logProcess(name,
                                   "processPayload failed, Payload discarded",
                                   Logging.LogLevel.WARN, processedPayload, e);
                processedPayload.close();
                //noinspection UnusedAssignment
                processedPayload = null;
                continue;
            }
            long spendTime = System.nanoTime() - startTime;
            totalTimeNS += spendTime;
            payloadCount++;
            String ms = Double.toString((spendTime / 1000000.0));
            if (log.isTraceEnabled()) {
                //noinspection DuplicateStringLiteralInspection
                log.trace("Processed " + processedPayload + " (#" + payloadCount
                          + " in " + ms + " ms using " + this);
            }
            Logging.logProcess(name,
                               "processPayload #" + payloadCount
                               + " finished in " + ms + "ms",
                               Logging.LogLevel.TRACE, processedPayload);
            break;
        }

        return processedPayload != null;
    }

    public Payload next() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("next() called");
        if (!hasNext()) {
            throw new IllegalStateException("No more Payloads available");
        }
        Payload toDeliver = processedPayload;
        processedPayload = null;
        return toDeliver;
    }

    /**
     * Perform implementation-specific processing of the given Payload.
     * @param payload the Payload to process.
     */
    protected abstract void processPayload(Payload payload);

    public void remove() {
        // Do nothing as default
    }

    public void setSource(Filter filter) {
        if (filter == null) {
            //noinspection DuplicateStringLiteralInspection
            throw new IllegalArgumentException("Source filter was null");
        }
        if (!(filter instanceof ObjectFilter)) {
            throw new IllegalArgumentException(
                    "Only ObjectFilters accepted as source. The filter"
                    + " provided was of class " + filter.getClass());
        }
        source = (ObjectFilter)filter;
    }

    // TODO: Consider if close is a wise action - what about pooled ingests?
    public boolean pump() throws IOException {
        checkSource();
        if (!hasNext()) {
            log.trace("pump(): hasNext() returned false");
            return false;
        }
        Payload payload = next();
        if (payload != null) {
            payload.close();
        }
        return hasNext();
    }

    public void close(boolean success) {
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
        //noinspection DuplicateStringLiteralInspection
        return "Processed " + payloadCount + " Payloads at "
               + (payloadCount == 0 ? "NA" :
                  totalTimeNS / 1000000.0 / payloadCount)
               + " ms/Payload";
    }

    /**
     * @return the name of the filter, if specified. Else the class name of the
     *         object.
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Filter '" + getName() + "' " + getProcessStats();
    }
}
