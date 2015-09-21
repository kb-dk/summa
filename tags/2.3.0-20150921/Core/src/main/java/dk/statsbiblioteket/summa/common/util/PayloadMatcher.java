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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class that performs regexp-matches on Payloads.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class PayloadMatcher {
    private static Log log = LogFactory.getLog(PayloadMatcher.class);

    /**
     * Optional property defining a list regular expressions applied to
     * payload ids.
     */
    public static final String CONF_ID_REGEX = "summa.record.idpatterns";
    /**
     * Optional property defining a list of regular expressions applied to
     * the {@link dk.statsbiblioteket.summa.common.Record} base.
     * Defining this property implies that all filtered payloads must include
     * a {@code Record}. Payloads without Record will return false.
     */
    public static final String CONF_BASE_REGEX = "summa.record.basepatterns";
    /**
     * Optional property defining a list of regular expressions applied to
     * the {@link dk.statsbiblioteket.summa.common.Record} content.
     * Defining this property implies that all filtered payloads must include a
     * {@code Record}. Payloads without Record will return false.
     */
    public static final String CONF_CONTENT_REGEX = "summa.record.contentpatterns";

    /**
     * Optional property defining a list of keys. A list of the same length must
     * be defined for {@link #CONF_META_VALUE_REGEXP}. For each entry in Payload
     * or Record that has one of the keys in this list, the corresponding regexp
     * is checked.
     */
    public static final String CONF_META_KEY = "summa.record.metakey";

    /**
     * The list of regexps corresponfing to {@link #CONF_META_KEY}. Should be
     * defined if CONF_META_KEY, but if null, all values will match.
     */
    public static final String CONF_META_VALUE_REGEXP = "summa.record.metavaluepattern";

    /**
     * The default condition for matching. Possible values:<br/>
     * full:    The regexp must match the full input.<br/>
     * partial: The regexp must match part of the input.
     * </p><p>
     * Optional. Default is full.
     */
    public static final String CONF_MATCH_METHOD = "payloadmatcher.method";
    public static final String DEFAULT_MATCH_METHOD = MATCH_METHOD.full.toString();
    public static enum MATCH_METHOD {full, partial}

    /**
     * Paired with {@link #CONF_MATCH_AMOUNT} to define the amount of matches needed.
     * </p><p>
     * Optional. Default it 'more', with CONT_MATCH_AMOUNT=0, thereby requiring 1 or more matches.
     * </p>
     */
    public static final String CONF_MATCH_AMOUNT_EQUALITY = "payloadmatcher.amount.equality";
    public static final String DEFAULT_MATCH_AMOUNT_EQUALITY = MATCH_AMOUNT_EQUALITY.more.toString();
    public static enum MATCH_AMOUNT_EQUALITY {
        less {
            @Override
            public String sign() {
                return "<";
            }
        }, equal {
            @Override
            public String sign() {
                return "=";
            }
        }, more {
            @Override
            public String sign() {
                return ">";
            }
        };
        public abstract String sign();
    }

    /**
     * The amount of matches required, measured with the {@link #CONF_MATCH_AMOUNT_EQUALITY} modifier.
     * </p><p>
     * Optional. Default is 0.
     */
    public static final String CONF_MATCH_AMOUNT = "payloadmatcher.amount";
    public static final int DEFAULT_MATCH_AMOUNT = 0;


    /**
     * Whether or not all regexp patterns should be treated as DOT_ALL (multi line matching).
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_DOT_ALL = "payloadmatcher.dotall";
    public static final boolean DEFAULT_DOT_ALL = false;

    private List<Matcher> idMatchers;
    private List<Matcher> baseMatchers;
    private List<Matcher> contentMatchers;
    private List<String> metaKeys;
    private List<Matcher> metaValueMatchers;
    private final MATCH_METHOD matchMethod;
    private final boolean dotAll;
    private final int matchAmount;
    private final MATCH_AMOUNT_EQUALITY matchEquality;


    public PayloadMatcher(Configuration conf) {
        this(conf, true);
    }

    public PayloadMatcher(Configuration conf, boolean warnOnNoMatchers) {
        log.debug("Constructing PayloadMatcher");
        idMatchers = getMatchers(conf, CONF_ID_REGEX, "id");
        baseMatchers = getMatchers(conf, CONF_BASE_REGEX, "base");
        //noinspection DuplicateStringLiteralInspection
        contentMatchers = getMatchers(conf, CONF_CONTENT_REGEX, "content");

        metaKeys = conf.getStrings(CONF_META_KEY, (List<String>)null);
        metaValueMatchers = getMatchers(
                conf, CONF_META_VALUE_REGEXP, "meta value");
        if (metaKeys != null && metaValueMatchers != null
            && metaKeys.size() != metaValueMatchers.size()) {
            throw new IllegalArgumentException(String.format(
                    "The number of %s was %d while number of %s was %s. As the"
                    + " lists are used in parallel, the numbers must match",
                    CONF_META_KEY, metaKeys.size(),
                    CONF_META_VALUE_REGEXP, metaValueMatchers.size()));
        }

        if (warnOnNoMatchers && !isMatcherActive()) {
            log.warn("No patterns configured. Set the properties "
                     + PayloadMatcher.CONF_META_KEY + ", "
                     + PayloadMatcher.CONF_ID_REGEX + ", "
                     + PayloadMatcher.CONF_BASE_REGEX +", and/or"
                     + PayloadMatcher.CONF_CONTENT_REGEX
                     + " to control the behaviour");
        }
        matchMethod = MATCH_METHOD.valueOf(conf.getString(CONF_MATCH_METHOD, DEFAULT_MATCH_METHOD));
        dotAll = conf.getBoolean(CONF_DOT_ALL, DEFAULT_DOT_ALL);
        matchAmount = conf.getInt(CONF_MATCH_AMOUNT, DEFAULT_MATCH_AMOUNT);
        matchEquality = MATCH_AMOUNT_EQUALITY.valueOf(
                conf.getString(CONF_MATCH_AMOUNT_EQUALITY, DEFAULT_MATCH_AMOUNT_EQUALITY));
        log.info("Constructed " + this);
    }

    /**
     * @return true is any match property is set.
     */
    public boolean isMatcherActive() {
        return idMatchers != null || baseMatchers != null || contentMatchers != null || metaKeys != null;
    }

    private List<Matcher> getMatchers(Configuration conf, String confKey, String type) {
        List<String> regexps = conf.getStrings(confKey, (List<String>)null);
        if (regexps == null) {
            return null;
        }
        List<Matcher> matchers = new ArrayList<>(regexps.size());
        for (String regex : regexps) {
            log.debug("Compiling " + type + " filter regex: " + regex);
            matchers.add(dotAll ? Pattern.compile(regex, Pattern.DOTALL).matcher("") :
                                 Pattern.compile(regex).matcher(""));
        }
        return matchers;
    }

    /**
     * @param payload the Payload to match against.
     * @return true if the Payload matched, else false.
     */
    public boolean isMatch(Payload payload) {
        if (log.isTraceEnabled()) {
            log.trace("matching " + payload);
        }
        MatchState matchState = new MatchState();

        updateMatch(idMatchers, payload.getId(), matchState);
        if (matchState.canTerminateEarly()) {
            return matchState.earlyTerminationResult();
        }

        if (baseMatchers != null || contentMatchers != null || metaKeys != null) {
            if (payload.getRecord() == null) {
                Logging.logProcess(this.getClass().getSimpleName(),
                        "Payload without Record. Cannot perform extended matching",
                        Logging.LogLevel.WARN, payload);
                return false;
            }
        }
        if (metaKeys != null) {
            for (int i = 0 ; i < metaKeys.size() ; i++) {
                String metaKey = metaKeys.get(i);
                Object value = payload.getData(metaKey);
                if (value != null &&
                    (metaValueMatchers == null
                     || metaValueMatchers.get(i).reset(value.toString()).matches())) {
                    matchState.inc();
                    if (matchState.canTerminateEarly()) {
                        return matchState.earlyTerminationResult();
                    }
                }
            }
        }

        return isMatch(payload.getRecord(), matchState);
    }

    /**
     * @param record the Record to match against.
     * @return true if the Record matched, else false.
     */
    public boolean isMatch(Record record) {
        return isMatch(record, new MatchState());
    }
    private boolean isMatch(Record record, MatchState matchState) {
        if (updateMatch(idMatchers, record.getId(), matchState)) {
            return matchState.earlyTerminationResult();
        }

        if (updateMatch(baseMatchers, record.getBase(), matchState)) {
            return matchState.earlyTerminationResult();
        }
        if (contentMatchers != null && updateMatch(contentMatchers, record.getContentAsUTF8(), matchState)) {
            return matchState.earlyTerminationResult();
        }
        if (metaKeys != null) {
            for (int i = 0 ; i < metaKeys.size() ; i++) {
                String metaKey = metaKeys.get(i);
                Object value;
                if ((value = record.getMeta(metaKey)) != null) {
                    if (metaValueMatchers == null || metaValueMatchers.get(i).reset(value.toString()).matches()) {
                        matchState.inc();
                        if (matchState.canTerminateEarly()) {
                            return matchState.earlyTerminationResult();
                        }
                    }
                }
            }
        }

        // Final evaluation
        if (matchState.hasSucceeded(true)) {
            return true;
        }
        if (matchState.hasFailed(true)) {
            return false;
        }
        Logging.logProcess("PayloadMatcher", "Unable to calculate final match state, returning false",
                           Logging.LogLevel.ERROR, record.getId());
        return false;
    }

    private class MatchState {
        int count = 0;
        public boolean hasFailed(boolean finished) {
            if (finished) {
                switch (matchEquality) {
                    case equal: return count != matchAmount;
                    case less: return count >= matchAmount;
                    case more: return count <= matchAmount;
                    default: throw new IllegalArgumentException("Unknown equality '" + matchEquality + "'");
                }
            }
            // Not finished
            switch (matchEquality) {
                case equal: return count > matchAmount; // Only if exceeded
                case less: return count >= matchAmount; // If equal or exceeded
                case more: return false; // Cannot be sure yet
                default: throw new IllegalArgumentException("Unknown equality '" + matchEquality + "'");
            }
        }

        public boolean hasSucceeded(boolean finished) {
            if (finished) {
                switch (matchEquality) {
                    case equal: return count == matchAmount;
                    case less: return count < matchAmount;
                    case more: return count > matchAmount;
                    default: throw new IllegalArgumentException("Unknown equality '" + matchEquality + "'");
                }
            }
            // Not finished
            switch (matchEquality) {
                case equal: return false; // Cannot be sure yet
                case less: return false; // Cannot be sure yet
                case more: return count > matchAmount;
                default: throw new IllegalArgumentException("Unknown equality '" + matchEquality + "'");
            }
        }

        public boolean canTerminateEarly() {
            return hasSucceeded(false) || hasFailed(false);
        }

        public boolean earlyTerminationResult() {
            if (hasSucceeded(false)) {
                return true;
            } else if (hasFailed(false)) {
                return false;
            }
            throw new IllegalStateException(
                    "Unable to calculate early termination result with canTerminateEarly()==" + canTerminateEarly());
        }

        // Add 1 match point
        public void inc() {
            count++;
        }
    }

    private boolean updateMatch(List<Matcher> matchers, String value, MatchState matchState) {
        if (matchers == null) {
            return matchState.canTerminateEarly();
        }
        for (Matcher m : matchers) {
            switch (matchMethod) {
                case full: {
                    if (m.reset(value).matches()) {
                        matchState.inc();
                    }
                    break;
                }
                case partial: {
                    m.reset(value);
                    while (m.find()) {
                        matchState.inc();
                        if (matchState.canTerminateEarly()) {
                            return true;
                        }
                    }
                    break;
                }
                default: throw new IllegalArgumentException("The match method '" + matchMethod + "' is not supported");
            }
            if (matchState.canTerminateEarly()) {
                return true;
            }
        }
        return matchState.canTerminateEarly();
    }

    @Override
    public String toString() {
        return "PayloadMatcher(dotAll=" + dotAll + ", matchMethod=" + matchMethod + ", idPatterns=["
                + toString(idMatchers) + "], basePatterns=[" + toString(baseMatchers) + "], contentPatterns=["
                + toString(contentMatchers) + "], metaKeys=[" + (metaKeys == null ? "" : Strings.join(metaKeys))
                + "], metaPatterns=[" + toString(metaValueMatchers)
                + "], matchEquality/amount: " + matchEquality.sign() + " " + matchAmount + ")";
    }

    private String toString(List<Matcher> matchers) {
        if (matchers == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Matcher matcher: matchers) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(matcher.pattern());
        }
        return sb.toString();
    }
}
