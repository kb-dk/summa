package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An {@link ObjectFilter} that can black- or white list payloads based on
 * their ids, record base- and/or content. It can be configured to check lists
 * of regular expressions against any of these three properties.
 */
public class RegexFilter extends AbstractDiscardFilter {

    private static final Log log = LogFactory.getLog(RegexFilter.class);

    /**
     * Optional property defining a list regular expressions applied to
     * payload ids. Whether or not the payload is discarded is decided
     * based upon the {@link #CONF_MODE} property.
     */
    public static final String CONF_ID_REGEX = "summa.regexfilter.idpatterns";

    /**
     * Optional property defining a list of regular expressions applied to
     * the {@link Record} base. Defining this property implies that
     * all filtered payloads must include a {@code Record}. Whether or not the
     * payload is discarded is decided based upon the {@link #CONF_MODE}
     * property.
     */
    public static final String CONF_BASE_REGEX =
                                          "summa.regexfilter.basepatterns";

    /**
     * Optional property defining a list of regular expressions applied to
     * the {@link Record} content. Defining this property implies that
     * all filtered payloads must include a {@code Record}. Whether or not the
     * payload is discarded is decided based upon the {@link #CONF_MODE}
     * property.
     */
    public static final String CONF_CONTENT_REGEX =
                                            "summa.regexfilter.contentpatterns";

    /**
     * Optional property defining whether this filter {@code inclusive}
     * or {@code exclusive}.
     * <p/>
     * If the filter is {@code inclusive} payloads
     * matching any of the the given regular expressions pass the filter.
     * If it is {@code exclusive} records matching any of the regular
     * expressions are discarded.
     * <p/>
     * The values allowed for this property are the strings {@code inclusive}
     * and {@code exclusive}. The default value is {@code exclusive}.
     */
    public static final String CONF_MODE = "summa.regexfilter.mode";

    public static final String DEFAULT_MODE = "exclusive";

    private boolean isInclusive;
    private List<Pattern> idPatterns;
    private List<Pattern> basePatterns;
    private List<Pattern> contentPatterns;

    public RegexFilter(Configuration conf) {
        super(conf);

        isInclusive = parseIsInclusive(conf);

        List<String> idRegex = conf.getStrings(CONF_ID_REGEX,
                                               (List<String>)null);
        List<String> baseRegex = conf.getStrings(CONF_BASE_REGEX,
                                                 (List<String>)null);
        List<String> contentRegex = conf.getStrings(CONF_CONTENT_REGEX,
                                                    (List<String>)null);

        if (idRegex != null) {
            idPatterns = new ArrayList<Pattern>(idRegex.size());
            for (String regex : idRegex) {
                log.debug("Compiling id filter regex: " + regex);
                idPatterns.add(Pattern.compile(regex));
            }
        }

        if (baseRegex != null) {
            basePatterns = new ArrayList<Pattern>(baseRegex.size());
            for (String regex : baseRegex) {
                log.debug("Compiling base filter regex: " + regex);
                basePatterns.add(Pattern.compile(regex));
            }
        }

        if (contentRegex != null) {
            contentPatterns = new ArrayList<Pattern>(contentRegex.size());
            for (String regex : contentRegex) {
                log.debug("Compiling content filter regex: " + regex);
                contentPatterns.add(Pattern.compile(regex));
            }
        }

        if (idPatterns == null && basePatterns == null && contentPatterns == null){
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
        if (idPatterns != null) {
            for (Pattern p : idPatterns) {
                if (p.matcher(payload.getId()).matches()) {
                    return !isInclusive;
                }
            }
        }

        if (basePatterns != null) {
            Record r = payload.getRecord();

            if (r == null) {
                Logging.logProcess(this.getClass().getSimpleName(),
                        "Payload without record, can not check "
                        +"record base. Discarding payload",
                        Logging.LogLevel.WARN, payload);
                return true;
            }

            for (Pattern p : basePatterns) {
                if (p.matcher(r.getBase()).matches()) {
                    return !isInclusive;
                }
            }
        }

        if (contentPatterns != null) {
            Record r = payload.getRecord();

            if (r == null) {
                Logging.logProcess(this.getClass().getSimpleName(),
                        "Payload without record, can not check "
                        +"record content. Discarding payload",
                        Logging.LogLevel.WARN, payload);
                return true;
            }

            for (Pattern p : contentPatterns) {
                if (p.matcher(r.getContentAsUTF8()).matches()) {
                    return !isInclusive;
                }
            }
        }


        if (log.isTraceEnabled()) {
            log.trace("No regular expressions configured");
        }

        // If we are inclusive only payloads matching our conditions should
        // pass the filter. It seems that non conditions are configured,
        // so apply the default policy on all payloads
        return isInclusive;
    }
}
