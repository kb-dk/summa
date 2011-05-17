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

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles the logistic of traversing a graph of Records with the input Record
 * as origin. Only Payloads with Records are permitted.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class GraphFilter<T> extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(GraphFilter.class);

    /**
     * If true, parent Records are visited.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_VISIT_PARENTS = "graphfilter.visit.parents";
    public static final boolean DEFAULT_VISIT_PARENTS = false;

    /**
     * If true, child Records are visited.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_VISIT_CHILDREN =
        "graphfilter.visit.children";
    public static final boolean DEFAULT_VISIT_CHILDREN = false;

    /**
     * The required positive returns from Record processing in order for the
     * Record to be passed on in the chain.
     * </p><p>
     * Valid values are origin, all, none and one.
     * </p><p>
     * Optional. Default is all.
     */
    public static final String
        CONF_SUCCESS_REQUIREMENT = "graphfilter.success.requirement";
    public static final String
        DEFAULT_SUCCESS_REQUIREMENT = "all";
    public static enum REQUIREMENT {origin, all, none, one}

    private final boolean visitParents;
    private final boolean visitChildren;
    private final REQUIREMENT requirement;

    public GraphFilter(Configuration conf) {
        super(conf);
        visitParents = conf.getBoolean(
            CONF_VISIT_PARENTS, DEFAULT_VISIT_PARENTS);
        visitChildren = conf.getBoolean(
            CONF_VISIT_CHILDREN, DEFAULT_VISIT_CHILDREN);
        String r = conf.getString(
            CONF_SUCCESS_REQUIREMENT, DEFAULT_SUCCESS_REQUIREMENT);
        requirement = REQUIREMENT.valueOf(r);
        if (requirement == null) {
            throw new ConfigurationException(
                "The requirement value '" + r + "' is not known. Valid values "
                + "are origin, all, none and one");
        }
        log.debug(String.format(
            "GraphFilter %s constructed with visitParents=%b, visitChildren=%b",
            getName(), visitParents, visitChildren));
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (payload.getRecord() == null) {
            throw new PayloadException("No Record defined", payload);
        }
        T state = createState(payload);
        boolean success = processRecord(payload.getRecord(), null, state);
        return finish(payload, state, success);
    }

    private boolean processRecord(Record record, Tracker tracker, T state)
                                                       throws PayloadException {
        if (tracker != null && tracker.isVisited(record)) {
            log.trace("Already visited " + record.getId());
            return true;
        }
        if (log.isDebugEnabled()) {
            log.debug("Processing "
                      + (tracker == null ? "sub " : "origin ") + record);
        }
        if (!visitChildren && !visitParents) { // No traversal
            return processRecord(record, true, state) ||
                   requirement == REQUIREMENT.none;
        }
        tracker = tracker == null ? new Tracker(requirement) : tracker;
        tracker.addVisited(record,
                           processRecord(record, tracker.isEmpty(), state));
        if (visitChildren && record.getChildren() != null) {
            for (Record child: record.getChildren()) {
                processRecord(child, tracker, state);
            }
        }
        if (visitParents && record.getParents() != null) {
            for (Record parent: record.getParents()) {
                processRecord(parent, tracker, state);
            }
        }

        return tracker.isSuccess();
    }

    /**
     * Process the content of the Record.
     * @param record the record to process.
     * @param origin if true, the Record is the origin in the Record graph.
     * @param state custom state object.
     * @return true if the Record processing is considered a success.
     * @throws PayloadException if an error that warrants overall discarding of
     *         the graph occurred during processing.
     */
    public abstract boolean processRecord(
        Record record, boolean origin, T state) throws PayloadException;

    /**
     * The state-object is created before any other processing is done and will
     * be send to all Records visited during traversal.
     * null is a valid value as all processing of the object is optional.
     * @param payload the Payload that is about to be processed.
     * @return a custom object for preserving state between process-calls.
     * @throws PayloadException if the state could not be created.
     */
    public abstract T createState(Payload payload) throws PayloadException;

    /**
     * Called with the originating payload when traversal has finished.
     * @param payload the Payload that entered this filter.
     * @param state the custom state object.
     * @param success if the overall processing was considered a success.
     * @throws PayloadException if an exception was appropriate instead of just
     *         returning false.
     * @return true if the Payload entering the GraphFilter should be preserved.
     */
    public abstract boolean finish(Payload payload, T state, boolean success)
                                                        throws PayloadException;

    private class Tracker {
        private final REQUIREMENT requirement;
        private int encountered = 0;
        private int successes = 0;
        private boolean originSuccess = false;
        private Set<String> visited = null;

        private Tracker(REQUIREMENT requirement) {
            this.requirement = requirement;
        }

        public void addVisited(Record record, boolean success) {
            if (visited == null) {
                visited = new HashSet<String>();
                originSuccess = success;
            }
            visited.add(record.getId());
            if (success) {
                successes++;
            }
            encountered++;
        }

        public boolean isVisited(Record record) {
            return visited != null && visited.contains(record.getId());
        }

        public boolean isEmpty() {
            return encountered == 0;
        }

        public boolean isSuccess() {
            switch (requirement) {
                case none:   return true;
                case all:    return encountered == successes;
                case one:    return successes != 0;
                case origin: return originSuccess;
                default: throw new UnsupportedOperationException(
                    "The requirement value '" + requirement
                    + "' is unknown and unhandled");
            }
        }
    }
}
