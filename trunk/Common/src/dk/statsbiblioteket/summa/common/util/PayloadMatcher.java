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

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.Logging;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

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
    public static final String CONF_CONTENT_REGEX =
                                            "summa.record.contentpatterns";

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
    public static final String CONF_META_VALUE_REGEXP =
            "summa.record.metavaluepattern";

    private List<Matcher> idMatchers;
    private List<Matcher> baseMatchers;
    private List<Matcher> contentMatchers;
    private List<String> metaKeys;
    private List<Matcher> metaValueMatchers;

    private static final String PAYLOAD_WITHOUT_RECORD =
            "Payload without record, can not check record %s. No match";

    public PayloadMatcher(Configuration conf) {
        this(conf, true);
    }

    public PayloadMatcher(Configuration conf, boolean warnOnNoMatchers) {
        log.trace("Constructing PayloadMatcher");
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
                    + " lists are used in parallen, the numbers must match",
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
    }

    /**
     * @return true is any match property is set.
     */
    public boolean isMatcherActive() {
        return idMatchers != null || baseMatchers != null
               || contentMatchers != null || metaKeys != null;
    }

    private List<Matcher> getMatchers(Configuration conf,
                                      String confKey, String type) {
        List<String> regexps = conf.getStrings(confKey, (List<String>)null);
        if (regexps == null) {
            return null;
        }
        List<Matcher> matchers = new ArrayList<Matcher>(regexps.size());
        for (String regex : regexps) {
            log.debug("Compiling " + type + " filter regex: " + regex);
            matchers.add(Pattern.compile(regex).matcher(""));
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
        if (isMatch(idMatchers, payload.getId())) {
            return true;
        }

        if (baseMatchers != null) {
            Record r = payload.getRecord();

            if (r == null) {
                Logging.logProcess(this.getClass().getSimpleName(),
                        String.format(PAYLOAD_WITHOUT_RECORD, "base"),
                        Logging.LogLevel.WARN, payload);
                return true;
            }

            if (isMatch(baseMatchers, r.getBase())) {
                return true;
            }
        }

        if (contentMatchers != null) {
            Record r = payload.getRecord();

            if (r == null) {
                //noinspection DuplicateStringLiteralInspection
                Logging.logProcess(
                        this.getClass().getSimpleName(),
                        String.format(PAYLOAD_WITHOUT_RECORD, "content"),
                        Logging.LogLevel.WARN, payload);
                return false;
            }

            if (isMatch(contentMatchers, r.getContentAsUTF8())) {
                return true;
            }
        }

        if (metaKeys != null) {
            for (int i = 0 ; i < metaKeys.size() ; i++) {
                String metaKey = metaKeys.get(i);
                Object value = payload.getData(metaKey);
                if (value != null &&
                    (metaValueMatchers == null
                     || metaValueMatchers.get(i).reset(
                            value.toString()).matches())) {
                    return true;
                }
                if (payload.getRecord() != null 
                    && (value = payload.getRecord().getMeta(metaKey)) != null) {
                    if (metaValueMatchers == null ||
                        metaValueMatchers.get(i).reset(
                                value.toString()).matches()) {
                        return true;
                    }
                }
            }
        }
        log.trace("No match for payload");
        return false;
    }

    private boolean isMatch(List<Matcher> matchers, String value) {
        if (matchers == null) {
            return false;
        }
        for (Matcher m : matchers) {
            if (m.reset(value).matches()) {
                return true;
            }
        }
        return false;
    }
}

