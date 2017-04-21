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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.support.doms.LocationMatcher;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLStepper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Perform look ups for a list of entities
 * Important: Works only on Solr XML documents (for now).
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class EnrichWithLocationsFilter extends EnrichXMLFilter {
    private static final Log log = LogFactory.getLog(EnrichWithLocationsFilter.class);

    /**
     * The field to get the source text from.
     * </p><p>
     * Mandatory.
     */
    public static final String CONF_INPUT_FIELD = "enrich.input.field";

    /**
     * The source of locations. Must be a uncompresses UTF-8 file, one entry/line.
     * The format must be [designation][delimiter][decimal_lattitude][delimiter][decimal_longitude].
     * The source can be an URL, a class path location or a file path.
     * </p><p>
     * Mandatory.
     */
    public static final String CONF_LOCATIONS_SOURCE = "enrich.locations.source";

    /**
     * The delimiter for the locations source data. This is a regexp Pattern.
     * </p><p>
     * Optional. Default is ",".
     */
    public static final String CONF_LOCATIONS_DELIMITER = "enrich.locations.delimiter";
    public static final String DEFAULT_LOCATIONS_DELIMITER = ",";



    /**
     * If true, the effect will be the same as prefixing all verbatims with a space (0x20).
     * It is recommended to also specify {@link #CONF_INPUT_CLEAN_REGEXP} is this property is true.
     * If true, the overall input will be prefixed with a space.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_MATCH_SPACE_PREFIX = "enrich.matcher.space.prefix";
    public static final boolean DEFAULT_MATCH_SPACE_PREFIX = false;

    /**
     * If true, the effect will be the same as postfixing all verbatims with a space (0x20).
     * It is recommended to also specify {@link #CONF_INPUT_CLEAN_REGEXP} is this property is true.
     * If true, the overall input will be postfixed with a space.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_MATCH_SPACE_POSTFIX = "enrich.matcher.space.postfix";
    public static final boolean DEFAULT_MATCH_SPACE_POSTFIX = false;

    /**
     * If specified, a replaceAll with this regexp and {@link #CONF_INPUT_CLEAN_REPLACEMENT} as replacement will be
     * performed on all input.
     * </p><p>
     * </p><p>
     * Optional. Default is "" (no cleanup).
     */
    public static final String CONF_INPUT_CLEAN_REGEXP = "enrich.input.clean.regexp";
    public static final String DEFAULT_INPUT_CLEAN_REGEXP = "";

    /**
     * If {@link #CONF_INPUT_CLEAN_REGEXP} is defined, this property holds the replacement.
     * </p><p>
     * Optional. Default is " " (space).
     */
    public static final String CONF_INPUT_CLEAN_REPLACEMENT = "enrich.input.clean.replacement";
    public static final String DEFAULT_INPUT_CLEAN_REPLACEMENT = " ";

    private final String inputField;
    private final Pattern inputRegexp;
    private final String inputReplacement;

    private final LocationMatcher matcher;

    public EnrichWithLocationsFilter(Configuration conf) {
        super(conf);
        if (!conf.valueExists(CONF_INPUT_FIELD)) {
            throw new ConfigurationException("Could not locate '" + CONF_INPUT_FIELD + "'");
        }
        inputField = conf.getString(CONF_INPUT_FIELD);
        if (!conf.valueExists(CONF_LOCATIONS_SOURCE)) {
            throw new ConfigurationException("Could not locate '" + CONF_LOCATIONS_SOURCE + "'");
        }

        matcher = new LocationMatcher(conf);
        String inputRegexpStr = conf.getString(CONF_INPUT_CLEAN_REGEXP, DEFAULT_INPUT_CLEAN_REGEXP);
        inputRegexp = inputRegexpStr == null || inputRegexpStr.isEmpty() ? null : Pattern.compile(inputRegexpStr);
        inputReplacement = conf.getString(CONF_INPUT_CLEAN_REPLACEMENT, DEFAULT_INPUT_CLEAN_REPLACEMENT);
        setStatsDefaults(conf, false, true, true, true);
        // TODO: Proper toString
        log.info("Created " + this);
    }

    @Override
    public boolean elementStart(XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
        if (current.equals(LocationMatcher.FIELD) &&
            inputField.equals(XMLStepper.getAttribute(xml, LocationMatcher.NAME, null))) {
            String input = inputRegexp == null ?
                    xml.getElementText() :
                    inputRegexp.matcher(xml.getElementText()).replaceAll(inputReplacement);
            matcher.findMatches(input);
            return true;
        }
        return false;
    }

    @Override
    public void beforeLastEndTag(XMLStreamWriter xml) throws XMLStreamException {
        matcher.addLocations(xml);
        matcher.reset();
    }

    @Override
    public String toString() {
        return "EnrichWithLocationsFilter(enrichedPayloads=" + matcher.getAssignedLocationSets()
               + ", assignedLocations=" + matcher.getAssignedLocations() + ", " + super.toString() + ")";
    }
}