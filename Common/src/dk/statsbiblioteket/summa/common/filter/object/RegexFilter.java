package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.Strings;

import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An {@link ObjectFilter} that can black- or white list payloads based on
 * their ids, record base- and/or content.
 */
public class RegexFilter extends AbstractDiscardFilter {

    private static final Log log = LogFactory.getLog(RegexFilter.class);

    /**
     * Optional property defining the regular expression applied to
     * payload ids. 
     */
    public static final String CONF_ID_REGEX = "summa.regexfilter.idpattern";

    /**
     * Optional property defining the regular expression applied to
     * the {@link Record} base. Defining this property implies that
     * all filtered payloads must include a {@code Record}.
     */
    public static final String CONF_BASE_REGEX =
                                          "summa.regexfilter.basepattern";

    /**
     * Optional property defining the regular expression applied to
     * the {@link Record} content. Defining this property implies that
     * all filtered payloads must include a {@code Record}.
     */
    public static final String CONF_CONTENT_REGEX =
                                            "summa.regexfilter.contentpattern";

    /**
     * Optional property defining whether this filter {@code inclusive}
     * or {@code exclusive}. If the filter is {@code inclusive} only payloads
     * matching the given regular expressions pass the filter.
     * If it is {@code exclusive} records matching the regular expressions
     * are discarded.
     * <p/>
     * The values allowed for this property are the strings {@code inclusive}
     * and {@code exclusive}. The default value is {@code exclusive}.
     */
    public static final String CONF_MODE = "summa.regexfilter.mode";

    public static final String DEFAULT_MODE = "exclusive";

    private boolean isInclusive;
    private Pattern idPattern;
    private Pattern basePattern;
    private Pattern contentPattern;

    public RegexFilter(Configuration conf) {
        super(conf);

        isInclusive = parseIsInclusive(conf);

        String idRegex = conf.getString(CONF_ID_REGEX, null);
        String baseRegex = conf.getString(CONF_BASE_REGEX, null);
        String contentRegex = conf.getString(CONF_CONTENT_REGEX, null);

        if (idRegex != null) {
            log.debug("Compiling id filter regex: " + idRegex);
            idPattern = Pattern.compile(idRegex);
        }

        if (baseRegex != null) {
            log.debug("Compiling base filter regex: " + baseRegex);
            basePattern = Pattern.compile(baseRegex);
        }

        if (contentRegex != null) {
            log.debug("Compiling content filter regex: " + contentRegex);
            contentPattern = Pattern.compile(contentRegex);
        }

        if (idPattern == null && basePattern == null && contentPattern == null){
            log.warn("No patterns configured, everything will be "
                     + (isInclusive ? "discarded" : "accepted")
                     + ". Set the properties "
                     + CONF_ID_REGEX + ", " + CONF_BASE_REGEX +", and/or"
                     + CONF_CONTENT_REGEX + " to control the behaviour");
        }
    }

    private boolean parseIsInclusive(Configuration conf) {
        String mode = conf.getString(CONF_MODE, DEFAULT_MODE).toLowerCase();
        log.debug("Found mode: " + mode);

        if ("inclusive".equals(mode)) {
            return true;
        } else if ("exclusive".equals(mode)) {
            return false;
        }

        throw new ConfigurationException(
                                  "Illegal mode definition '" + mode
                                  + "'. Expected 'inclusive' or 'exclusive'");
    }

    protected boolean checkDiscard(Payload payload) {
        // If we are inclusive only payloads matching our conditions should
        // pass the filter. It seems that non conditions are configured,
        // so apply the default policy on all payloads
        boolean discard = isInclusive;

        if (idPattern != null) {
            discard =
                     idPattern.matcher(payload.getId()).matches() ^ isInclusive;
            if (discard) return true; 
        }

        if (basePattern != null) {
            Record r = payload.getRecord();

            if (r == null) {
                log.warn("Payload without record, can not check record base. "
                         + "Discarding payload");
                return true;
            }

            discard = basePattern.matcher(r.getBase()).matches() ^ isInclusive;
            if (discard) return true;
        }

        if (contentPattern != null) {
            Record r = payload.getRecord();

            if (r == null) {
                log.warn("Payload without record, can not check record base. "
                         + "Discarding payload");
                return true;
            }

            discard =
              contentPattern.matcher(r.getContentAsUTF8()).matches() ^ isInclusive;
            if (discard) return true;
        }


        if (log.isTraceEnabled()) {
            log.trace("No regular expressions configured");
        }
        return discard;
    }
}
