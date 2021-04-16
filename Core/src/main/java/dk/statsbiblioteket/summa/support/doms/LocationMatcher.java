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

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.object.EnrichWithLocationsFilter;
import dk.statsbiblioteket.util.reader.VerbatimMatcher;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Collects matched locations. Workflow is {@code {@link #findMatches} -> {@link #getLocations()} -> {@link #reset()}}.
 */
public class LocationMatcher extends VerbatimMatcher<String> {
    // Fixed Solr XML: <field name="somename">content</field>
    public static final String FIELD = "field";
    public static final String NAME = "name";

    /**
     * The source of locations. Must be a uncompresses UTF-8 file, one entry/line.
     * The format must be [designation][delimiter][decimal_lattitude][delimiter][decimal_longitude].
     * The source can be an URL, a class path location or a file path.
     * </p><p>
     * Mandatory.
     */
    public static final String CONF_LOCATIONS_SOURCE = "enrich.locations.source";
    private static final Log log = LogFactory.getLog(LocationMatcher.class);

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
    public static final MATCH_MODE DEFAULT_MATCH_MODE = MATCH_MODE.all;

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

    private final String source;
    private final String sourceDelimiter;

    // "designation:coordinates[:coordinates]*"
    private final Set<String> locations = new HashSet<>();
    private final boolean addDanishGenitive;
    private final MATCH_MODE matchMode;
    private final boolean matcherSpacePrefix;
    private final boolean matcherSpacePostfix;

    private final String designationField;
    private final String coordinatesField;
    private final String combinedField;

    private long assignedLocations = 0;
    private long assignedLocationSets = 0;

    public LocationMatcher(Configuration conf) {
        super();
        if (!conf.valueExists(CONF_LOCATIONS_SOURCE)) {
            throw new Configurable.ConfigurationException(
                    "Could not locate '" + CONF_LOCATIONS_SOURCE + "'");
        }

        source = conf.getString(CONF_LOCATIONS_SOURCE);
        sourceDelimiter = conf.getString(EnrichWithLocationsFilter.CONF_LOCATIONS_DELIMITER, EnrichWithLocationsFilter.DEFAULT_LOCATIONS_DELIMITER);
        addDanishGenitive = conf.getBoolean(CONF_INPUT_DANISH_GENITIVE, DEFAULT_INPUT_DANISH_GENITIVE);
        setMatchMode(matchMode = MATCH_MODE.valueOf(
                conf.getString(CONF_MATCH_MODE, DEFAULT_MATCH_MODE.toString())));
        if (matchMode != MATCH_MODE.all) {
            setSkipMatching(true);
        }
        if (matcherSpacePrefix = conf.getBoolean(EnrichWithLocationsFilter.CONF_MATCH_SPACE_PREFIX, EnrichWithLocationsFilter.DEFAULT_MATCH_SPACE_PREFIX)) {
            setLeading(' ');
        }
        if (matcherSpacePostfix = conf.getBoolean(EnrichWithLocationsFilter.CONF_MATCH_SPACE_POSTFIX, EnrichWithLocationsFilter.DEFAULT_MATCH_SPACE_POSTFIX)) {
            setFollowing(' ');
        }
        loadMatcherRules();

        designationField = conf.getString(CONF_DESIGNATION_FIELD, DEFAULT_DESIGNATION_FIELD);
        coordinatesField = conf.getString(CONF_COORDINATES_FIELD, DEFAULT_COORDINATES_FIELD);
        combinedField = conf.getString(CONF_COMBINED_FIELD, DEFAULT_COMBINED_FIELD);
    }

    @Override
    public void callback(String match, String payload) {
        locations.add(payload);
    }

    /**
     * @return Map of designations -> coordinates
     */
    public Map<String, Set<String>> getLocations() {
        Map<String, Set<String>> expanded = new HashMap<>(locations.size());
        for (String location : locations) {
            // "designation:coordinates[:coordinates]*"
            String[] base = location.split(":", 2);
            expanded.put(base[0], new HashSet<>(Arrays.asList(base[1].split(":"))));
        }
        return expanded;
    }

    public void reset() {
        locations.clear();
    }

    public void addLocations(XMLStreamWriter xml) throws XMLStreamException {
        final Map<String, Set<String>> locations = getLocations();
        assignedLocationSets++;
        log.trace("Adding " + locations.size() + " location designations");

        for (Map.Entry<String, Set<String>> location: locations.entrySet()) {
            addLocations(xml, location.getKey(), location.getValue());
        }
    }

    public void addLocations(
            XMLStreamWriter xml, String designation, Set<String> coordinates) throws XMLStreamException {
        assignedLocations++;
        write(xml, designationField, designation);
        for (String coordinate: coordinates) {
            write(xml, combinedField, designation + COMBINED_DELIMITER + coordinate);
        }
        for (String coordinate: coordinates) {
            write(xml, coordinatesField, coordinate);
        }
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

    public void loadMatcherRules() {
        URL url = Resolver.getURL(source);
        if (url == null) {
            throw new Configurable.ConfigurationException("Unable to locate coordinate source '" + source + "'");
        }
        try {
            Pattern delimiter = Pattern.compile(sourceDelimiter);
            log.info("Reading coordinates from " + url);
            InputStream in = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = delimiter.split(line);
                if (tokens.length < 3) {
                    log.warn("Skipping line '" + line + "' in " + url + " as it does not have 3 tokens, delimited by '"
                             + sourceDelimiter + "'");
                }
                log.trace("Adding " + tokens[0] + ": " + tokens[1] + "," + tokens[2]);
                try {
                    extend(tokens[0], tokens[1] + "," + tokens[2]);
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Exception adding '" + tokens[0] + " with coordinates [" +
                            tokens[1] + "," + tokens[2] + "]", e);
                }
            }

        } catch (IOException e) {
            throw new Configurable.ConfigurationException("Unable to read coordinates from " + url, e);
        }
    }

    private void extend(String designation, String coordinates) {
        if (designation.contains(":")) {
            throw new IllegalArgumentException(
                    "This implementations does not accept the character ':' in the designation \"" +
                    designation + "\"");
        }
        log.trace("Adding " + designation + " ; " + coordinates);

        Node entry = getNode(designation);
        if (entry == null) {
            addRule(designation, designation + ":" + coordinates);
        } else {
            entry.setPayload(entry.getPayload() + ":" + coordinates);
        }

        if (addDanishGenitive && !designation.endsWith("s")) {
            entry = getNode(designation + "s");
            if (entry == null) {
                addRule(designation + "s", designation + ":" + coordinates);
            } else {
                entry.setPayload(entry.getPayload() + ":" + coordinates);
            }
        }
    }

    public long getAssignedLocations() {
        return assignedLocations;
    }

    public long getAssignedLocationSets() {
        return assignedLocationSets;
    }
}
