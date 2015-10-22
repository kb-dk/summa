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
        state = QAInfo.State.QA_OK,
        author = "te")
public class DiscardRelativesFilter extends AbstractDiscardFilter {
    private static Log log = LogFactory.getLog(DiscardRelativesFilter.class);

    /**
     * If true, all Records with one or more parents are discarded.
     * </p><p>
     * Optional. Default is false;
     * </p><p>
     * @deprecated use {@link #CONF_PARENT} instead.
     */
    public static final String CONF_DISCARD_HASPARENT = "summa.relativesfilter.discard.hasparent";
    public static final boolean DEFAULT_DISCARD_HASPARENT = false;

    /**
     * If true, all Records with one or more children are discarded.
     * </p><p>
     * Optional. Default is false;
     * </p><p>
     * @deprecated use {@link #CONF_CHILDREN} instead.
     */
    public static final String CONF_DISCARD_HASCHILDREN = "summa.relativesfilter.discard.haschildren";
    public static final boolean DEFAULT_DISCARD_HASCHILDREN = false;

    /**
     * The {@link RELATION} from the current Record to its parent must be satisfied for the record to be accepted.
     * </p><p>
     * Sample: If {@code summa.relativesfilter.parent=require} and the current Record has no parent, the current
     * Record is discarded.
     * </p><p>
     * Optional. Default is ignore.
     * </p><p>
     * @see {@link #CONF_EXISTENCE_TYPE}.
     */
    public static final String CONF_PARENT = "summa.relativesfilter.parent";
    public static final RELATION DEFAULT_PARENT = RELATION.ignore;

    /**
     * The {@link RELATION} from the current Record to its children must be satisfied for the record to be accepted.
     * </p><p>
     * Sample: If {@code summa.relativesfilter.children=disallow} and the current Record has one or more children,
     * the current Record is discarded.
     * </p><p>
     * Optional. Default is ignore.
     * </p><p>
     * @see {@link #CONF_EXISTENCE_TYPE}.
     */
    public static final String CONF_CHILDREN = "summa.relativesfilter.children";
    public static final RELATION DEFAULT_CHILDREN = RELATION.ignore;

    /**
     * When checking relation for parent or child, this property controls what constitutes an existing relation.
     * </p><p>
     * id: There exists at least 1 ID in the corresponding ID-list.<br/>
     * object: There exists at least 1 Record object in the corresponding Record-list.<br/>
     * any_id_and_object: There exists at least 1 ID and 1 Record object in the corresponding ID and Record-lists.<br/>
     * all_id_and_object: There exists at least 1 ID and 1 Record object in the corresponding ID and Record-lists and
     * for each ID there is a corresponding Record and vice versa.<br/>
     * </p><p>
     * Optional. Default is object.
     */
    public static final String CONF_EXISTENCE_TYPE = "summa.relativesfilter.existence";
    public static final EXISTENCE DEFAULT_EXISTENCE_TYPE = EXISTENCE.object;

    public enum RELATION {ignore, require, disallow}
    public enum EXISTENCE {id, object, any_id_and_object, all_id_and_object}

    public final RELATION parentCheck;
    public final RELATION childrenCheck;
    public final EXISTENCE existence;

    @SuppressWarnings("deprecation")
    public DiscardRelativesFilter(Configuration conf) {
        super(conf);
        feedback = false;
        if (!conf.containsKey(CONF_PARENT)) {
            parentCheck = conf.getBoolean(CONF_DISCARD_HASPARENT, DEFAULT_DISCARD_HASPARENT) ?
                    RELATION.disallow : RELATION.ignore;
        } else {
            parentCheck = RELATION.valueOf(conf.getString(CONF_PARENT, DEFAULT_PARENT.toString()));
        }
        if (!conf.containsKey(CONF_CHILDREN)) {
            childrenCheck = conf.getBoolean(CONF_DISCARD_HASCHILDREN, DEFAULT_DISCARD_HASCHILDREN) ?
                    RELATION.disallow : RELATION.ignore;
        } else {
            childrenCheck =
                    RELATION.valueOf(conf.getString(CONF_CHILDREN, DEFAULT_CHILDREN.toString()));
        }
        existence = EXISTENCE.valueOf(conf.getString(CONF_EXISTENCE_TYPE, DEFAULT_EXISTENCE_TYPE.toString()));
        log.debug("Created " + this);
    }

    @Override
    protected boolean checkDiscard(Payload payload) {
        if (log.isTraceEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("checkDiscard(" + payload + ") called with " + payload.getRecord());
        }
        if (payload.getRecord() == null) {
            return false;
        }
        if (parentCheck != RELATION.ignore) {
            log.trace("Checking for parents");
            if (checkDiscard("parents", payload.getRecord().getParentIds(), payload.getRecord().getParents(),
                             parentCheck, payload)) {
                return true;
            }
        }
        if (childrenCheck != RELATION.ignore) {
            log.trace("Checking for children");
            if (checkDiscard("children", payload.getRecord().getChildIds(), payload.getRecord().getChildren(),
                             childrenCheck, payload)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkDiscard(
            String designation, List<String> ids, List<Record> records, RELATION relation, Payload payload) {
        switch (relation) {
            case ignore:
                return false;
            case disallow:
                switch (existence) {
                    case id: return checkDiscardLog(
                            ids != null && !ids.isEmpty(),
                            "Discarding due to " + designation + " ID existence when none allowed", payload);
                    case any_id_and_object:
                    case all_id_and_object: return checkDiscardLog(
                            (ids != null && !ids.isEmpty()) || (records != null && !records.isEmpty()),
                            "Discarding due to " + designation + " ID or Record existence when none allowed", payload);
                    case object: return checkDiscardLog(
                            records != null && !records.isEmpty(),
                            "Discarding due to " + designation + " Record existence when none allowed", payload);
                    default: throw new UnsupportedOperationException("Unknown existence: " + existence);
                }
            case require:
                switch (existence) {
                    case id: return checkDiscardLog(
                            ids == null || ids.isEmpty(),
                            "Discarding due to missing " + designation + " IDs", payload);
                    case any_id_and_object:
                        return checkDiscardLog(
                                (ids == null || ids.isEmpty()) && (records == null || records.isEmpty()),
                                "Discarding due to missing " + designation + " IDs or Records", payload);
                    case all_id_and_object:
                        if (checkDiscardLog(
                                ids == null || ids.isEmpty() || records == null || records.isEmpty(),
                                "Discarding due to missing " + designation + " IDs or Records", payload)) {
                            return true;
                        }
                        if (ids == null || records == null) {
                            return true; // Should not be needed, but the compiler complains below
                        }
                        id:
                        for (String id: ids) {
                            for (Record record: records) {
                                if (id.equals(record.getId())) {
                                    continue id;
                                }
                            }
                            return checkDiscardLog(
                                    true,
                                    "Discarding due to non-matching " + designation + " IDs and Records", payload);
                        }
                        record:
                        for (Record record: records) {
                            for (String id: ids) {
                                if (id.equals(record.getId())) {
                                    continue record;
                                }
                            }
                            return checkDiscardLog(
                                    true,
                                    "Discarding due to non-matching " + designation + " IDs and Records", payload);
                        }
                        return false;
                    case object: return checkDiscardLog(
                            records == null || records.isEmpty(),
                            "Discarding due to missing " + designation + " Records", payload);
                    default: throw new UnsupportedOperationException("Unknown existence: " + existence);
                }
            default: throw new UnsupportedOperationException("Unknown relation: " + relation);
        }
    }

    private boolean checkDiscardLog(boolean discard, String message, Payload payload) {
        if (discard) {
            Logging.logProcess(getName() + "#" + this.getClass().getSimpleName(), message,
                               Logging.LogLevel.DEBUG, payload);
        }
        return discard;
    }

    @Override
    public String toString() {
        return "DiscardRelativesFilter(parentCheck=" + parentCheck + ", childrenCheck=" + childrenCheck
               + ", existence=" + existence + ')';
    }
}
