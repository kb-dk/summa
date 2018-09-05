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
import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keeps track of the ids of processed Records. Records with new ids are passed
 * along while Records with already encountered ids are discarded.
 * </p><p>
 * The design scenario is batch ingesting of a huge amount of records with a
 * lot of updates. By using a FileReader in reverse order and this filter,
 * only the latest versions of the Records are processed.
 * </p><p>
 * It is expected that this filter will be extended in the future to contact a
 * Storage upon startup to determine the ids of already ingested Records.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DiscardUpdatesFilter extends AbstractDiscardFilter {
//    private static Log log = LogFactory.getLog(DiscardUpdatesFilter.class);

    // recordID
    private Set<String> encountered = new HashSet<>(10000);
    // recordID, time
    private Map<String, String> encounteredWithTime = new HashMap<>(10000);

    /**
     * The update detection algorithm.
     *
     * Optional. Possible values are:
     * ignore_time_keep_first: First Payload with a given ID is let through, subsequent Payloads with same ID are
     * discarded
     * ignore_time_override_previous: No filtering. This effectively disables processing in the DiscardUpdatesFilter.
     * respect_time_prefer_oldest: If a Payload the same ID as a previously encountered Payload, their timestamps are
     * compared. If the current Payload is older it is passed through. If not, it is discarded.
     * respect_time_prefer_newest: If a Payload the same ID as a previously encountered Payload, their timestamps are
     * compared. If the current Payload is newer it is passed through. If not, it is discarded.
     *
     * Note 1: Using respect_time_prefer_oldest or respect_time_prefer_newest requires that {@link #CONF_TIME_SOURCE}
     * is specified.
     * Note 2: Although this is intended as time-oriented, the ordering of the timestamps are simply their alphanumeric
     * order. Any String can be used as long as it sorts as intended.
     */
    public static final String CONF_POLICY = "discard.policy";
    public static final POLICY DEFAULT_POLICY = POLICY.ignore_time_keep_first;
    public enum POLICY {
        ignore_time_keep_first,
        ignore_time_override_previous,
        respect_time_prefer_oldest,
        respect_time_prefer_newest}

    /**
     * Where to locate the timestamp when using a time oriented priority.
     * This is used with {@link RecordUtil#getString(Record, String)}.
     *
     * Mandatory if {@link POLICY#respect_time_prefer_oldest} or {@link POLICY#respect_time_prefer_newest}
     * is specified for {@link #CONF_POLICY}
     */
    public static final String CONF_TIME_SOURCE = "discard.time.source";

    /**
     * Used with {@link #CONF_TIME_TEMPLATE} to extract the timestamp from the content of {@link #CONF_TIME_SOURCE}.
     *
     * Optional. Default is to use the full content of {@link #CONF_TIME_SOURCE}.
     */
    public static final String CONF_TIME_REGEXP = "discard.time.regexp";

    /**
     * Used with {@link #CONF_TIME_REGEXP} to extract the timestamp from the content of {@link #CONF_TIME_SOURCE}.
     *
     * Optional. Default is $0 (the full match).
     */
    public static final String CONF_TIME_TEMPLATE = "discard.time.template";
    public static final String DEFAULT_TIME_TEMPLATE = "$0";

    private final POLICY policy;
    private final String source;
    private final Pattern regexp;
    private final String template;

    @SuppressWarnings({"UnusedDeclaration"})
    public DiscardUpdatesFilter(Configuration conf) {
        super(conf);
        feedback = false;
        policy = POLICY.valueOf(conf.getString(CONF_POLICY, DEFAULT_POLICY.toString()));
        source = policy == POLICY.respect_time_prefer_oldest || policy == POLICY.respect_time_prefer_newest ?
                conf.getString(CONF_TIME_SOURCE) : null;
        regexp = conf.valueExists(CONF_TIME_REGEXP) ? Pattern.compile(conf.getString(CONF_TIME_REGEXP)) : null;
        template = conf.getString(CONF_TIME_TEMPLATE, DEFAULT_TIME_TEMPLATE);
    }

    @Override
    protected boolean checkDiscard(Payload payload) {
        switch (policy) {
            case ignore_time_override_previous: return false;
            case ignore_time_keep_first: {
                if (!encountered.contains(payload.getId())) {
                    encountered.add(payload.getId());
                    return false;
                }
                return true;
            }
        }
        // Time oriented

        String time = getTime(payload);
        if (time == null) {
            return true; // Error logging is already handled
        }
        String existingTime = encounteredWithTime.get(payload.getId());
        if (existingTime == null) {
            encounteredWithTime.put(payload.getId(), time);
            return false;
        }

        if ((policy == POLICY.respect_time_prefer_oldest && time.compareTo(existingTime) <= 0) ||
            policy == POLICY.respect_time_prefer_newest && time.compareTo(existingTime) > 0)  {
            // Override existing
            log.debug("Replaced content for " + payload.getId() + " with previous version timestamp=" + existingTime +
                      " and new version timestamp=" + time);
            encounteredWithTime.put(payload.getId(), time);
            return false;
        }
        return true;
    }

    private String getTime(Payload payload) {
        try {
            String rawContent = RecordUtil.getString(payload.getRecord(), source);
            if (rawContent == null || rawContent.isEmpty()) {
                return logFatal("Content extracted with key '" + source + "' was empty, " +
                                "which is not acceptable. Payload will be discarded", payload, null);
            }

            if (regexp == null) {
                return rawContent;
            }

            Matcher matcher = regexp.matcher(rawContent);
            if (!matcher.find()) {
                return logFatal("Unable to match content from '" + source + "' with regexp '" + regexp.pattern() +
                                "', which is not acceptable. Patload will be discarded", payload);
            }

            StringBuffer sb = new StringBuffer();
            matcher.appendReplacement(sb, template);
            return sb.toString();
        } catch (Exception e) {
            return logFatal("Unexpected Exception during update detection. Payload will be discarded", payload, e);
        }
    }

    private String logFatal(String message, Payload payload) {
        logFatal(message, payload, null);
        return null;
    }
    private String logFatal(String message, Payload payload, Exception exception) {
        String localLogMessage = message + " Payload ID=" + payload.getId();
        if (exception == null) {
            Logging.logProcess(
                    "DiscardUpdatesFilter " + getName(), message,
                    Logging.LogLevel.FATAL, payload);
            log.warn(localLogMessage);
        } else {
            Logging.logProcess(
                    "DiscardUpdatesFilter " + getName(), message,
                    Logging.LogLevel.FATAL, payload, exception);
            log.warn(localLogMessage, exception);
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("DiscardUpdatesFilter(policy=%s, source=%s, regexp=\"%s\", template=\"%s\", super=%s)",
                             policy, source, regexp, template, super.toString());
    }
}

