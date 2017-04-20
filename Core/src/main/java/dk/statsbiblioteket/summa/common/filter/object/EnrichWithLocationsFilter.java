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
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.reader.VerbatimMatcher;
import dk.statsbiblioteket.util.xml.XMLStepper;
import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.net.URL;
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

    // Solr XML: <field name="somename">content</field>
    private static final String FIELD = "field";
    private static final String NAME = "name";

    /**
     * The name of the field which will hold the location designation sans coordinates.
     * </p><p>
     * Optional. Default is "location_name". If the empty string is specified, the field will not be populated.
     */
    public static final String CONF_DESIGNATION_FIELD = "enrich.location.designation.field";
    public static final String DEFAULT_DESIGNATION_FIELD = "location_name";

    /**
     * The name of the field which will hold the location coordinates.
     * </p><p>
     * Optional. Default is "location_coordinates". If the empty string is specified, the field will not be populated.
     */
    public static final String CONF_COORDINATES_FIELD = "enrich.location.coordinates.field";
    public static final String DEFAULT_COORDINATES_FIELD = "location_coordinates";

    /**
     * The name of the field which will hold concatenations of the location name and coordinates, delimited
     * by ";".
     * </p><p>
     * Optional. Default is "location_combined". If the empty string is specified, the field will not be populated.
     */
    public static final String CONF_COMBINED_FIELD = "enrich.location.combined.field";
    public static final String DEFAULT_COMBINED_FIELD = "location_combined";
    public static final String COMBINED_DELIMITER = ";";

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
     * If true, Danish genitive is added to the locations:
     * Bogense -> [Bogense, Bogenses], Aarhus -> [Aarhus']
     * The location names extracted from the input will be without genitives.
     */
    public static final String CONF_INPUT_DANISH_GENITIVE = "enrich.locations.adddanishgenitive";
    public static final boolean DEFAULT_INPUT_DANISH_GENITIVE = false;


    /**
     * How multiple matches from the same starting point in the input text should be treated.
     * </p><p>
     * Optional. Default is "all". Possible values are:
     * all: "Visit London Greenwich" produces "London", "London Greenwich" and "Greenwich".
     * shortest: "Visit London Greenwich" produces "London" and "Greenwich".
     * longest: "Visit London Greenwich" produces "London Greenwich".
     */
    public static final String CONF_MATCH_MODE = "enrich.matcher.mode";
    public static final VerbatimMatcher.MATCH_MODE DEFAULT_MATCH_MODE = VerbatimMatcher.MATCH_MODE.all;

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

    private final String designationField;
    private final String coordinatesField;
    private final String combinedField;

    private final String inputField;
    private final Pattern inputRegexp;
    private final String inputReplacement;

    private final String source;
    private final String sourceDelimiter;
    private final boolean addDanishGenitive;

    private final LocationMatcher matcher;
    private final VerbatimMatcher.MATCH_MODE matchMode;
    private final boolean matcherSpacePrefix;
    private final boolean matcherSpacePostfix;

    private long assignedLocations = 0;
    private long assignedLocationSets = 0;

    public EnrichWithLocationsFilter(Configuration conf) {
        super(conf);
        designationField = conf.getString(CONF_DESIGNATION_FIELD, DEFAULT_DESIGNATION_FIELD);
        coordinatesField = conf.getString(CONF_COORDINATES_FIELD, DEFAULT_COORDINATES_FIELD);
        combinedField = conf.getString(CONF_COMBINED_FIELD, DEFAULT_COMBINED_FIELD);
        if (!conf.valueExists(CONF_INPUT_FIELD)) {
            throw new ConfigurationException("Could not locate '" + CONF_INPUT_FIELD + "'");
        }
        inputField = conf.getString(CONF_INPUT_FIELD);
        if (!conf.valueExists(CONF_LOCATIONS_SOURCE)) {
            throw new ConfigurationException("Could not locate '" + CONF_LOCATIONS_SOURCE + "'");
        }
        source = conf.getString(CONF_LOCATIONS_SOURCE);
        sourceDelimiter = conf.getString(CONF_LOCATIONS_DELIMITER, DEFAULT_LOCATIONS_DELIMITER);
        addDanishGenitive = conf.getBoolean(CONF_INPUT_DANISH_GENITIVE, DEFAULT_INPUT_DANISH_GENITIVE);

        matcher = new LocationMatcher();
        if (matcherSpacePrefix = conf.getBoolean(CONF_MATCH_SPACE_PREFIX, DEFAULT_MATCH_SPACE_PREFIX)) {
            matcher.setLeading(' ');
        }
        if (matcherSpacePostfix = conf.getBoolean(CONF_MATCH_SPACE_POSTFIX, DEFAULT_MATCH_SPACE_POSTFIX)) {
            matcher.setFollowing(' ');
        }
        ;
        matcher.setMatchMode(matchMode = VerbatimMatcher.MATCH_MODE.valueOf(
                conf.getString(CONF_MATCH_MODE, DEFAULT_MATCH_MODE.toString())));
        if (matchMode != VerbatimMatcher.MATCH_MODE.all) {
            matcher.setSkipMatching(true);
        }
        String inputRegexpStr = conf.getString(CONF_INPUT_CLEAN_REGEXP, DEFAULT_INPUT_CLEAN_REGEXP);
        inputRegexp = inputRegexpStr == null || inputRegexpStr.isEmpty() ? null : Pattern.compile(inputRegexpStr);
        inputReplacement = conf.getString(CONF_INPUT_CLEAN_REPLACEMENT, DEFAULT_INPUT_CLEAN_REPLACEMENT);
        loadMatcherRules();
        setStatsDefaults(conf, false, true, true, true);
        // TODO: Proper toString
        log.info("Created " + this);
    }

    @Override
    public boolean elementStart(XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
        if (current.equals(FIELD) && inputField.equals(XMLStepper.getAttribute(xml, NAME, null))) {
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
        final Map<String, Set<String>> locations = matcher.getLocations();
        assignedLocationSets++;
        assignedLocations += locations.size();
        log.trace("Flushing " + locations.size() + " location designations");

        for (Map.Entry<String, Set<String>> location: locations.entrySet()) {
            final String designation = location.getKey();
            write(xml, designationField, designation);
        }

        for (Map.Entry<String, Set<String>> location: locations.entrySet()) {
            final String designation = location.getKey();
            for (String coordinates: location.getValue()) {
                write(xml, combinedField, designation + COMBINED_DELIMITER + coordinates);
            }
        }

        for (Map.Entry<String, Set<String>> location: locations.entrySet()) {
            for (String coordinates: location.getValue()) {
                write(xml, coordinatesField, coordinates);
            }
        }
        matcher.reset();
    }

    private void write(XMLStreamWriter xml, String fieldName, String content) throws XMLStreamException {
        if (fieldName.isEmpty()) {
            return;
        }
        xml.writeStartElement(FIELD);
        xml.writeAttribute(NAME, fieldName);
        xml.writeCharacters(content);
        xml.writeEndElement();
        xml.writeCharacters("\n");
    }

    private void loadMatcherRules() {
        URL url = Resolver.getURL(source);
        if (url == null) {
            throw new ConfigurationException("Unable to locate coordinate source '" + source + "'");
        }
        try {
            Pattern delimiter = Pattern.compile(sourceDelimiter);
            log.info("Reading coordinates from " + url);
            InputStream in = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "utf-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = delimiter.split(line);
                if (tokens.length < 3) {
                    log.warn("Skipping line '" + line + "' in " + url + " as it does not have 3 tokens, delimited by '"
                             + sourceDelimiter + "'");
                }
                log.trace("Adding " + tokens[0] + ": " + tokens[1] + "," + tokens[2]);
                try {
                    matcher.extend(tokens[0], tokens[1] + "," + tokens[2]);
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Exception adding '" + tokens[0] + " with coordinates [" +
                            tokens[1] + "," + tokens[2] + "]", e);
                }
            }

        } catch (IOException e) {
            throw new ConfigurationException("Unable to read coordinates from " + url, e);
        }
    }

    // Collects matched locations. Should be reset between each source Payload.
    private class LocationMatcher extends VerbatimMatcher<String> {
        // "designation:coordinates[:coordinates]*"
        private final Set<String> locations = new HashSet<>();

        @Override
        public void callback(String match, String payload) {
            locations.add(payload);
        }

        public Map<String, Set<String>> getLocations() {
            Map<String, Set<String>> expanded = new HashMap<>(locations.size());
            for (String location: locations) {
                // "designation:coordinates[:coordinates]*"
                String[] base = location.split(":", 2);
                expanded.put(base[0], new HashSet<>(Arrays.asList(base[1].split(":"))));
            }
            return expanded;
        }
        public void reset() {
            locations.clear();
        }

        public void extend(String designation, String coordinates) {
            if (designation.contains(":")) {
                throw new IllegalArgumentException(
                        "This implementations does not accept the character ':' in the designation \"" +
                        designation + "\"");
            }
            log.trace("Adding " + designation + " ; " + coordinates);

            LocationMatcher.Node entry = matcher.getNode(designation);
            if (entry == null) {
                matcher.addRule(designation, designation + ":" + coordinates);
            } else {
                entry.setPayload(entry.getPayload() + ":" + coordinates);
            }

            if (addDanishGenitive && !designation.endsWith("s")) {
                entry = matcher.getNode(designation + "s");
                if (entry == null) {
                    matcher.addRule(designation + "s", designation + ":" + coordinates);
                } else {
                    entry.setPayload(entry.getPayload() + ":" + coordinates);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "EnrichWithLocationsFilter(enrichedPayloads=" + assignedLocationSets
               + ", assignedLocations=" + assignedLocations + ", " + super.toString() + ")";
    }
}