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

import java.io.IOException;

import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.Record;
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
    // If true, process-time statistics are logged after processPayload-calls
    protected boolean feedback = true;

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
                if (!processPayload(processedPayload)) {
                    processedPayload.close();
                    processedPayload = null;
                    continue;
                }
            } catch (PayloadException e) {
                Logging.logProcess(
                        name,
                        "processPayload failed with explicit PayloadException, "
                        + "Payload discarded",
                        Logging.LogLevel.WARN, processedPayload, e);
                processedPayload.close();
                processedPayload = null;
                continue;
            } catch (Exception e) {
                Logging.logProcess(name,
                                   "processPayload failed, Payload discarded",
                                   Logging.LogLevel.WARN, processedPayload, e);
                processedPayload.close();
                //noinspection UnusedAssignment
                processedPayload = null;
                continue;
            } catch (Throwable t) {
                /* Woops, this means major trouble, we dump everything we have */
                String msg = "Unexpected error on payload "
                             + processedPayload.toString();
                String content = "";
                Record rec = processedPayload.getRecord();
                if (rec != null) {
                    msg += ", enclosed record : "
                           + rec.toString(true);
                    content = "\n" + "Record content:\n"
                              + rec.getContentAsUTF8();
                } else {
                    msg += ", no enclosed record";
                }
                log.fatal(msg + content, t);
                throw new Error(msg, t);
            }

            long spendTime = System.nanoTime() - startTime;
            totalTimeNS += spendTime;
            payloadCount++;
            String ms = Double.toString((spendTime / 1000000.0));
            if (log.isTraceEnabled()) {
                //noinspection DuplicateStringLiteralInspection
                log.trace("Processed " + processedPayload + ", #" + payloadCount
                          + ", in " + ms + " ms using " + this);
            } else if (log.isDebugEnabled() && feedback) {
                log.debug("Processed " + processedPayload + ", #" + payloadCount
                          + ", in " + ms + " ms");
            }
            Logging.logProcess(name,
                               "processPayload #" + payloadCount
                               + " finished in " + ms + "ms for " + name,
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
     * @return true if the processing resulted in a payload that should be
     *         preserved, false if the Payload should be discarded. Note that
     *         returning false will not raise any warnings and should be used
     *         where the discarding of the Payload is an non-exceptional event.
     *         An example of such use is the AbstractDiscardFilter.
     *         If the Payload is to be discarded because of an error, throw
     *         a PayloadException instead.
     * @throws PayloadException if it was not possible to process the Payload
     *         and if this means that further processing of the Payload does
     *         not make sense. Throwing this means that the Payload will be
     *         discarded by ObjectFilterImpl.
     */
    protected abstract boolean processPayload(Payload payload) throws
                                                               PayloadException;

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
            //noinspection DuplicateStringLiteralInspection
            Logging.logProcess("ObjectFilterImpl",
                               "Calling close for Payload as part of pump()",
                               Logging.LogLevel.TRACE, payload);
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
                                            + getClass().getSimpleName()
                                            + " filter");
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

