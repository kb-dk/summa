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
package dk.statsbiblioteket.summa.support.doms;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.AbstractDiscardFilter;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Highly Statsbiblioteket specific filter for discarding specific DOMS Records.
 * </p><p>
 * This class will be moved to the DOMS project at a later date and is only places in Summa as a quick fix.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DiscardDOMSFilter extends AbstractDiscardFilter {
    private static Log log = LogFactory.getLog(DiscardDOMSFilter.class);

    /**
     * The IDs to process.
     * </p><p>
     * Optional. Default is "^doms_radioTVCollection.*".
     */
    public static final String CONF_ID_PATTERN = "discarddomsfilter.id.regexp";
    public static final String DEFAULT_ID_PATTERN = "^doms_radioTVCollection.*";

    /**
     * The regexp for the dates. All matches are iterated and the last match is compared to
     * {@link #CONF_DATE_LASTVALID}.
     * </p><p>
     * Optional.
     */
    public static final String CONF_DATE_REGEXP = "discarddomsfilter.date.regexp";
    public static final String DEFAULT_DATE_REGEXP =
            "<pbcoreDateAvailable.*?<dateAvailableStart.*?>(.+?)</dateAvailableStart.*?</pbcoreDateAvailable.*?";
    // Not used as it only matches the first date
/*            "<foxml:xmlContent.*?<PBCoreDescriptionDocument.*?<pbcoreInstantiation.*<pbcoreDateAvailable.*?"
            + "<dateAvailableStart.*?>(.+?)</dateAvailableStart.*?"
            + "</pbcoreDateAvailable.*?</pbcoreInstantiation.*?</PBCoreDescriptionDocument.*?</foxml:xmlContent";
            */

    /**
     * The last valid date for a DOMS record that matches {@link #CONF_ID_PATTERN}, stated as {@code YYYY-MM-DD}..
     * </p><p>
     * Optional. Default is '2999-01-02'.
     */
    // TODO: Make true date matching instead of ISO-time matching
    public static final String CONF_DATE_LASTVALID = "discarddomsfilter.date.lastvalid";
    public static final String DEFAULT_DATE_LASTVALID = "2999-01-01";

    // Gaps are holes in the recordings. Gaps can be in the beginning (pre), middle (middle) or end (post)
    // of the recording.

    public static final String CONF_GAP_PRE_REGEXP = "discarddomsfilter.gap.pre.regexp";
    public static final String DEFAULT_GAT_PRE_REGEXP =
            "<missingStart>.*?<missingSeconds>([0-9]+?)</missingSeconds>.*?</missingStart>";

    public static final String CONF_GAP_PRE_MAX = "discarddomsfilter.gap.pre.max";
    public static final int DEFAULT_GAP_PRE_MAX = 60; // Seconds

    public static final String CONF_GAP_MIDDLE_REGEXP = "discarddomsfilter.gap.middle.regexp";
    public static final String DEFAULT_GAP_MIDDLE_REGEXP = "<holeLength>([0-9]+?)</holeLength>";

    public static final String CONF_GAP_MIDDLE_MAX = "discarddomsfilter.gap.middle.max";
    public static final int DEFAULT_GAP_MIDDLE_MAX = 60; // Seconds

    public static final String CONF_GAP_POST_REGEXP = "discarddomsfilter.gap.POST.regexp";
    public static final String DEFAULT_GAT_POST_REGEXP =
            "<missingEnd>.*?<missingSeconds>([0-9]+?)</missingSeconds>.*?</missingEnd>";

    public static final String CONF_GAP_POST_MAX = "discarddomsfilter.gap.POST.max";
    public static final int DEFAULT_GAP_POST_MAX = 60; // Seconds

    private final Pattern idPattern;

    private final Pattern datePattern;
    private final Pattern gapPrePattern;
    private final Pattern gapMiddlePattern;
    private final Pattern gapPostPattern;

    private final String dateLastValid;
    private final int gapPreMax;
    private final int gapMiddleMax;
    private final int gapPostMax;

    public DiscardDOMSFilter(Configuration conf) {
        super(conf);
        idPattern = getPattern(conf, CONF_ID_PATTERN, DEFAULT_ID_PATTERN);
        datePattern = getPattern(conf, CONF_DATE_REGEXP, DEFAULT_DATE_REGEXP);
        gapPrePattern = getPattern(conf, CONF_GAP_PRE_REGEXP, DEFAULT_GAT_PRE_REGEXP);
        gapMiddlePattern = getPattern(conf, CONF_GAP_MIDDLE_REGEXP, DEFAULT_GAP_MIDDLE_REGEXP);
        gapPostPattern = getPattern(conf, CONF_GAP_POST_REGEXP, DEFAULT_GAT_POST_REGEXP);

        dateLastValid = conf.getString(CONF_DATE_LASTVALID, DEFAULT_DATE_LASTVALID);
        gapPreMax = conf.getInt(CONF_GAP_PRE_MAX, DEFAULT_GAP_PRE_MAX);
        gapMiddleMax = conf.getInt(CONF_GAP_MIDDLE_MAX, DEFAULT_GAP_MIDDLE_MAX);
        gapPostMax = conf.getInt(CONF_GAP_POST_MAX, DEFAULT_GAP_POST_MAX);

        logDiscards = false; // We do more detailed logging in this class

        log.info(String.format(
                "Created DOMS discarder with date=['%s', '%s'], pre=['%s', %d], middle==['%s', %d], post==['%s', %d]",
                getPS(datePattern), dateLastValid, getPS(gapPrePattern), gapPreMax,
                getPS(gapMiddlePattern), gapMiddleMax, getPS(gapPostPattern), gapPostMax));
    }

    private Pattern getPattern(Configuration conf, String key, String defaultValue) {
        String val = conf.getString(key, defaultValue);
        return val == null || val.isEmpty() ? null : Pattern.compile(val, Pattern.DOTALL);
    }

    private String getPS(Pattern pattern) {
        return pattern == null ? "null" : pattern.pattern();
    }

    @Override
    protected boolean checkDiscard(Payload payload) {
        if (idPattern != null && !idPattern.matcher(payload.getId()).matches()) {
            Logging.logProcess(
                    "DiscardDOMSFilter", "Accepting Record as ID is not covered", Logging.LogLevel.TRACE, payload);
            return false;
        }
        final String content = payload.getRecord().getContentAsUTF8();
        return checkGap(payload, "prefix", content, gapPrePattern, gapPreMax)
               || checkGap(payload, "infix", content, gapMiddlePattern, gapMiddleMax)
               || checkGap(payload, "postfix", content, gapPostPattern, gapPostMax)
               || checkDate(payload, content);
    }

    private boolean checkDate(Payload payload, String content) {
        Matcher dm = datePattern.matcher(content);
        String last = null;
        int matches = 0;
        while (dm.find()) {
            last = dm.group(1);
            matches++;
        }
        if (last == null) {
            return false;
        }
        if (last.compareTo(dateLastValid) > 0) {
            Logging.logProcess("DiscardDOMSFilter",
                               String.format("Discarding record as date '%s' > '%s'. #dates=%d",
                                             last, dateLastValid, matches),
                               Logging.LogLevel.DEBUG, payload);
            return true;
        }
        return false;
    }

    // True if pattern matches and gap is exceeded
    private boolean checkGap(Payload payload, String fixType, String content, Pattern gapPattern, int gapMax) {
        if (gapPattern == null) {
            return false;
        }
        Matcher matcher = gapPattern.matcher(content);
        while (matcher.find()) {
            try {
                int gap = Integer.valueOf(matcher.group(1));
                if (gap > gapMax) {
                    Logging.logProcess("DiscardDOMSFilter",
                                       String.format("Discarding record due to %s gap %d > %d", fixType, gap, gapMax),
                                       Logging.LogLevel.DEBUG, payload);
                    return true;
                }
            } catch (NumberFormatException e) {
                log.warn("Expected number for gap pattern '" + gapPattern + "', but got '" + matcher.group()
                         + "' in " + payload);
            }
        }
        return false;
    }
}
