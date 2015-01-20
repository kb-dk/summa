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
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles logistics of retrieving a Payload and outputting 0-n Payloads.
 * Not suitable for streaming processing as the resulting Payloads should
 * be generated in one take.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class MultiFilterBase implements ObjectFilter {
    private Log log = LogFactory.getLog(MultiFilterBase.class.getName() + "#" + this.getClass().getSimpleName());

    /**
     * The feedback level used to log statistics to the process log.
     * Valid values are FATAL, ERROR, WARN, INFO, DEBUG and TRACE.
     * </p><p>
     * Optional. Default is TRACE.
     */
    public static final String CONF_PROCESS_LOGLEVEL = "process.loglevel";
    public static final Logging.LogLevel DEFAULT_FEEDBACK = Logging.LogLevel.TRACE;

    private ObjectFilter source;
    private long payloadCount = 0;
    private long totalTimeNS = 0;

    private String name;
    private final List<Payload> queue = new ArrayList<>();
    protected boolean feedback = true;
    // If true, process-time statistics are logged after processPayload-calls
    private Logging.LogLevel processLogLevel;

    public MultiFilterBase(Configuration conf) {
        name = conf.getString(CONF_FILTER_NAME, this.getClass().getSimpleName());
        processLogLevel = Logging.LogLevel.valueOf(conf.getString(CONF_PROCESS_LOGLEVEL, DEFAULT_FEEDBACK.toString()));
//        log.info("Created " + this);
    }

    // if hasNext is true, a processed Payload is ready for delivery
    @Override
    public boolean hasNext() {
        // Ensure that there is something in the queue
        while (queue.isEmpty() && source.hasNext()) {
            Payload inPayload = source.next();
            if (inPayload == null) {
                log.debug("hasNext(): Got null from source. This is legal but unusual. Skipping to next payload");
                continue;
            }
            wrappedProcess(inPayload);
        }
        if (queue.isEmpty()) {
            log.info("No more Payloads available from " + this);
            return false;
        }
            return true;
    }

    private void wrappedProcess(Payload inPayload) {
        try {
            log.trace("Processing Payload");
            payloadCount++; // A bit prematurely but we also want to count exceptions
            long startTime = System.nanoTime();
            process(inPayload);
            long endTime = System.nanoTime();
            long ms = (endTime - startTime) / 1000000;
            totalTimeNS += endTime - startTime;


            Logging.logProcess(name, "process #" + payloadCount + " finished in " + ms + "ms for " + name + " with "
                                     + queue.size() + " Payload as result", processLogLevel, inPayload.getId());
        } catch (PayloadException e) {
            Logging.logProcess(name, "process failed with explicit PayloadException, Payload discarded",
                               Logging.LogLevel.WARN, inPayload, e);
            inPayload.close();
        } catch (Exception e) {
            Logging.logProcess(name, "processPayload failed, Payload discarded",
                               Logging.LogLevel.WARN, inPayload, e);
            inPayload.close();
        } catch (Throwable t) {
            /* Woops, this means major trouble, we dump everything we have and prepare to die */
            String msg = "Unexpected error on payload " + inPayload.toString();
            String content = "";
            Record rec = inPayload.getRecord();
            if (rec != null) {
                msg += ", enclosed record : " + rec.toString(true);
                content = "\nRecord content:\n" + rec.getContentAsUTF8();
            } else {
                msg += ", no enclosed record";
            }
            Logging.fatal(log, "ObjectFilterImpl.hasNext", msg + content, t);
            throw new Error(msg, t);
        }
    }

    @Override
    public Payload next() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("next() called");
        if (!hasNext()) {
            throw new IllegalStateException("No more Payloads available");
        }
        return queue.remove(0);
    }

    /**
     * Perform implementation-specific processing of the given Payload. Manipulated or newly created Payloads
     * should be delivered by calling {@link #deliver}. It is allowed to call this method multiple times, but
     * it is up to the implementation to assign new IDs, recordBases and/or content.
     * @param payload the Payload to process.
     * @throws dk.statsbiblioteket.summa.common.filter.object.PayloadException if it was not possible to process the
     *         Payload and if this means that further processing of the Payload does
     *         not make sense. Throwing this means that the Payload will be
     *         discarded by ObjectFilterImpl.
     */
    protected abstract void process(Payload payload) throws PayloadException;


    protected void deliver(Payload payload) {
        Logging.logProcess(name, "adding new/modified Payload  to out queue", processLogLevel, payload.getId());
        queue.add(payload);
    }

    @Override
    public void remove() {
        // Do nothing as default
    }

    @Override
    public void setSource(Filter filter) {
        if (filter == null) {
            //noinspection DuplicateStringLiteralInspection
            throw new IllegalArgumentException("Source filter was null");
        }
        if (!(filter instanceof ObjectFilter)) {
            throw new IllegalArgumentException(
                    "Only ObjectFilters accepted as source. The filter provided was of class " + filter.getClass());
        }
        source = (ObjectFilter)filter;
    }

    // TODO: Consider if close is a wise action - what about pooled ingests?
    @Override
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

    @Override
    public void close(boolean success) {
        log.debug(String.format("Closing down '%s'. %s", this, getProcessStats()));
        checkSource();
        source.close(success);
    }

    private void checkSource() {
        if (source == null) {
            throw new IllegalStateException("No source defined for " + getClass().getSimpleName() + " filter");
        }
    }

    /**
     * @return simple statistics on processed Payloads.
     *         Sample output: "Processed 1234 Payloads at 1.43232 ms/Payload" 
     */
    public String getProcessStats() {
        //noinspection DuplicateStringLiteralInspection
        return "Processed " + payloadCount + " Payloads at "
               + (payloadCount == 0 ? "NA" : totalTimeNS / 1000000.0 / payloadCount) + " ms/Payload";
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
        return getName() + "(feedback=" + feedback + ", processLogLevel=" + processLogLevel
               + ", stats=" + getProcessStats() + ")";
    }
}
