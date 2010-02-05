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

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.util.PayloadMatcher;
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
 * </p><p>
 * The filter uses {@link PayloadMatcher}. See that class for relevant
 * properties for matching.
 */
public class RegexFilter extends AbstractDiscardFilter {
   private static final Log log = LogFactory.getLog(RegexFilter.class);

    /**
     * Optional property defining whether this filter {@code inclusive}
     * or {@code exclusive}.
     * <p/>
     * If the filter is {@code inclusive} payloads matching any of the the given
     * regular expressions pass the filter.
     * If it is {@code exclusive} records matching any of the regular
     * expressions are discarded.
     * <p/>
     * The values allowed for this property are the strings {@code inclusive}
     * and {@code exclusive}. The default value is {@code exclusive}.
     */
    public static final String CONF_MODE = "summa.record.patternmode";

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String DEFAULT_MODE = "exclusive";

    private boolean isInclusive;
    private PayloadMatcher payloadMatcher;

    public RegexFilter(Configuration conf) {
        super(conf);

        isInclusive = parseIsInclusive(conf);
        payloadMatcher = new PayloadMatcher(conf);

    }

    private boolean parseIsInclusive(Configuration conf) {
        String mode = conf.getString(CONF_MODE, DEFAULT_MODE).toLowerCase();
        log.debug("Found mode: " + mode);

        //noinspection DuplicateStringLiteralInspection
        if ("inclusive".equals(mode)) {
            return true;
        } else //noinspection DuplicateStringLiteralInspection
            if ("exclusive".equals(mode)) {
            return false;
        }

        throw new ConfigurationException(
                                  "Illegal mode definition '" + mode
                                  + "'. Expected 'inclusive' or 'exclusive'");
    }

    @Override
    protected boolean checkDiscard(Payload payload) {
        if (payloadMatcher.isMatch(payload)) {
            return !isInclusive;
        }
        return isInclusive;
    }
}

