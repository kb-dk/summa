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
     * How multiple matches from the same starting point in the input text should be treated.
     * </p><p>
     * Optional. Default is "all". Possible values are:
     * all: "Visit London Greenwich" produces "London", "London Greenwich" and "Greenwich".
     * shortest: "Visit London Greenwich" produces "London" and "Greenwich".
     * longest: "Visit London Greenwich" produces "London Greenwich".
     */
    public static final String CONF_MATCH_MODE = "enrich.matcher.mode";
    public static final VerbatimMatcher.MATCH_MODE DEFAULT_MATCH_MODE = VerbatimMatcher.MATCH_MODE.all;

    private final String designationField;
    private final String coordinatesField;
    private final String combinedField;

    private final String inputField;

    private final String source;
    private final String sourceDelimiter;

    private final LocationMatcher matcher;
    private final VerbatimMatcher.MATCH_MODE matchMode;

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
        matcher = new LocationMatcher();
        matchMode = VerbatimMatcher.MATCH_MODE.valueOf(conf.getString(CONF_MATCH_MODE, DEFAULT_MATCH_MODE.toString()));
        matcher.setMatchMode(matchMode);
        if (matchMode != VerbatimMatcher.MATCH_MODE.all) {
            matcher.setSkipMatching(true);
        }
        loadMatcherRules();
        log.info("Created " + this);
    }

    @Override
    public boolean elementStart(XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
        if (current.equals(FIELD) && XMLStepper.getAttribute(xml, NAME, null) != inputField) {
            matcher.findMatches(xml.getElementText());
            return true;
        }
        return false;
    }

    @Override
    public void beforeLastEndTag(XMLStreamWriter xml) throws XMLStreamException {
        log.debug("Flushing " + matcher.getLocations().size() + " location designations");

        for (Map.Entry<String, Set<String>> location: matcher.getLocations().entrySet()) {
            final String designation = location.getKey();
            write(xml, designationField, designation);
        }

        for (Map.Entry<String, Set<String>> location: matcher.getLocations().entrySet()) {
            final String designation = location.getKey();
            for (String coordinates: location.getValue()) {
                write(xml, combinedField, designation + COMBINED_DELIMITER + coordinates);
            }
        }

        for (Map.Entry<String, Set<String>> location: matcher.getLocations().entrySet()) {
            final String designation = location.getKey();
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
                matcher.extend(tokens[0], tokens[1] + "," + tokens[2]);
            }


        } catch (IOException e) {
            throw new ConfigurationException("Unable to read coordinates from " + url, e);
        }
    }

    // Collects matched locations. Should be reset between each source Payload.
    private class LocationMatcher extends VerbatimMatcher<Set<String>> {
        // designation, coordinates
        private final Map<String, Set<String>> locations = new HashMap<>();

        @Override
        public void callback(String match, Set<String> payload) {
            locations.put(match, payload);
        }

        public Map<String, Set<String>> getLocations() {
            return locations;
        }
        public void reset() {
            locations.clear();
        }

        public void extend(String designation, String coordinates) {
            log.trace("Adding " + designation + " ; " + coordinates);
            LocationMatcher.Node entry = matcher.getNode(designation);
            if (entry == null) {
                matcher.addRule(designation, new HashSet<>(Collections.singleton(coordinates)));
            } else {
                // TODO: Handle too many instances for a destination (very common names)
                entry.getPayload().add(coordinates);
            }

        }
    }
}