package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
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
    private List<Matcher> idMatchers;
    private List<Matcher> baseMatchers;
    private List<Matcher> contentMatchers;

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
        if (idMatchers != null) {
            for (Matcher m : idMatchers) {
                if (m.reset(payload.getId()).matches()) {
                    return !isInclusive;
                }
            }
        }

        if (baseMatchers != null) {
            Record r = payload.getRecord();

            if (r == null) {
                Logging.logProcess(this.getClass().getSimpleName(),
                        "Payload without record, can not check "
                        +"record base. Discarding payload",
                        Logging.LogLevel.WARN, payload);
                return true;
            }

            for (Matcher m : baseMatchers) {
                if (m.reset(r.getBase()).matches()) {
                    return !isInclusive;
                }
            }
        }

        if (contentMatchers != null) {
            Record r = payload.getRecord();

            if (r == null) {
                Logging.logProcess(this.getClass().getSimpleName(),
                        "Payload without record, can not check "
                        +"record content. Discarding payload",
                        Logging.LogLevel.WARN, payload);
                return true;
            }

            for (Matcher m : contentMatchers) {
                if (m.reset(r.getContentAsUTF8()).matches()) {
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
