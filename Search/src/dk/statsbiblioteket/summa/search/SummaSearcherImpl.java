/**
 * Created: te 30-05-2008 16:30:28
 * CVS:     $Id:$
 */
package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.util.List;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Convenience base class for SummaSearchers, taking care of basic setup.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class SummaSearcherImpl implements SummaSearcher, Configurable {
    private static Log log = LogFactory.getLog(SummaSearcherImpl.class);

    private String[] defaultResultFields = DEFAULT_RESULT_FIELDS;
    private String[] defaultFallbackValues = DEFAULT_FALLBACK_VALUES;
    private long maxRecords = DEFAULT_MAX_NUMBER_OF_RECORDS;

    /**
     * Extracts basic settings from the configuration.
     * @param conf the configuration for the searcher.
     */
    public SummaSearcherImpl(Configuration conf) {
        defaultResultFields = getStrings(conf, CONF_RESULT_FIELDS,
                                         defaultResultFields, "result-fields");
        defaultFallbackValues = getStrings(conf, CONF_FALLBACK_VALUES,
                                           defaultFallbackValues,
                                           "fallback-values");
        if (defaultFallbackValues != null
            && defaultResultFields.length != defaultFallbackValues.length) {
            throw new IllegalArgumentException(String.format(
                    "The number of fallback-values(%s) was not equal to the "
                    + "number of fields(%s)", defaultFallbackValues.length,
                                              defaultResultFields.length));
        }
        maxRecords = conf.getLong(CONF_MAX_RECORDS, maxRecords);
        if (maxRecords <= 0) {
            log.warn(String.format(
                    "The property %s must be >0. It was %s. Resetting to "
                    + "default %s",
                    CONF_MAX_RECORDS, maxRecords,
                    DEFAULT_MAX_NUMBER_OF_RECORDS == Long.MAX_VALUE ?
                    "Long.MAX_VALUE" :
                    DEFAULT_MAX_NUMBER_OF_RECORDS));
            maxRecords = DEFAULT_MAX_NUMBER_OF_RECORDS;
        }
    }

    private String[] getStrings(Configuration conf, String key,
                                String[] defaultValues, String type) {
        String[] result;
        try {
            List<String> fields = conf.getStrings(key);
            result = fields.toArray(new String[fields.size()]);
            log.debug("Assigning " + type + " " + Arrays.toString(result));
            return result;
        } catch (NullPointerException e) {
            log.debug("Result-fields not specified in configuration. "
                      + "Using default " + type + " "
                      + Arrays.toString(defaultValues));
        } catch (IllegalArgumentException e) {
            log.warn(String.format(
                    "The property %s was expected to be a list of Strings, but "
                    + "it was not. Using default %s %s instead",
                    key, type, Arrays.toString(defaultValues)));
        }
        return defaultValues;
    }
}
