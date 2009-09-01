/* $Id:$
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

    private List<Matcher> idMatchers;
    private List<Matcher> baseMatchers;
    private List<Matcher> contentMatchers;

    private static final String PAYLOAD_WITHOUT_RECORD =
            "Payload without record, can not check record %s. No match";

    public PayloadMatcher(Configuration conf) {
        log.trace("Constructing PayloadMatcher");
        List<String> idRegex = conf.getStrings(
                PayloadMatcher.CONF_ID_REGEX, (List<String>)null);
        List<String> baseRegex = conf.getStrings(
                PayloadMatcher.CONF_BASE_REGEX, (List<String>)null);
        List<String> contentRegex = conf.getStrings(
                PayloadMatcher.CONF_CONTENT_REGEX, (List<String>)null);

        if (idRegex != null) {
            idMatchers = new ArrayList<Matcher>(idRegex.size());
            for (String regex : idRegex) {
                log.debug("Compiling id filter regex: " + regex);
                idMatchers.add(Pattern.compile(regex).matcher(""));
            }
        }

        if (baseRegex != null) {
            baseMatchers = new ArrayList<Matcher>(baseRegex.size());
            for (String regex : baseRegex) {
                log.debug("Compiling base filter regex: " + regex);
                baseMatchers.add(Pattern.compile(regex).matcher(""));
            }
        }

        if (contentRegex != null) {
            contentMatchers = new ArrayList<Matcher>(contentRegex.size());
            for (String regex : contentRegex) {
                log.debug("Compiling content filter regex: " + regex);
                contentMatchers.add(Pattern.compile(regex).matcher(""));
            }
        }

        if (idMatchers == null && baseMatchers == null && contentMatchers == null){
            log.warn("No patterns configured. Set the properties "
                     + PayloadMatcher.CONF_ID_REGEX + ", "
                     + PayloadMatcher.CONF_BASE_REGEX +", and/or"
                     + PayloadMatcher.CONF_CONTENT_REGEX
                     + " to control the behaviour");
        }
    }

    /**
     * @param payload the Payload to match against.
     * @return true if the Payload matched, else false.
     */
    public boolean isMatch(Payload payload) {
        if (log.isTraceEnabled()) {
            log.trace("matching " + payload);
        }
        if (idMatchers != null) {
            for (Matcher m : idMatchers) {
                if (m.reset(payload.getId()).matches()) {
                    return true;
                }
            }
        }

        if (baseMatchers != null) {
            Record r = payload.getRecord();

            if (r == null) {
                Logging.logProcess(this.getClass().getSimpleName(),
                        String.format(PAYLOAD_WITHOUT_RECORD, "base"),
                        Logging.LogLevel.WARN, payload);
                return true;
            }

            for (Matcher m : baseMatchers) {
                if (m.reset(r.getBase()).matches()) {
                    return true;
                }
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

            for (Matcher m : contentMatchers) {
                if (m.reset(r.getContentAsUTF8()).matches()) {
                    return true;
                }
            }
        }

        log.trace("No match for payload");
        return false;
    }
}
