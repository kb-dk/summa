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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.support.alto.Alto;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Highly Statsbiblioteket specific filter for splitting DOMS Records containing newspaper meta data and ALTO-XML.
 * The splitter is location and proximity oriented. The workflow is
 * 1) Locate location entities on the page
 * 2.1) For each entity create a new document
 * 2.2) Add extra entity metadata, if available. For places that could be global coordinates
 * 2.3.1) Add a field dist_0_r1 with all words within page spatial distance 0-r1
 * 2.3.2) Add a field dist_r1_r2 with all words within page spatial distance r1-r2
 * 2.3.3) Etc.
 * </p><p>
 * The format of the generated XML is dependent on setup, but for places it could be
 * ...doms-prefix-xml...
 * <altosegment>
 * <location_name>xxx</location_name>
 * <location_coordinates>Long, Lat</location_coordinates>
 * <content>A, b, c...</content>
 * </altosegment>
 * ...doms-postfix-xml...
 * </p>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DOMSLocationProximity extends DOMSNewspaperBase {
    private static Log log = LogFactory.getLog(DOMSLocationProximity.class);
    private final XMLOutputFactory xmlOutFactory = XMLOutputFactory.newFactory();

    /**
     * The radii to use when extracting text in proximity to the entity.
     * The starting radius 0 is implicit.
     * </p><p>
     * Optional (but highly recommended). Default is the list [200, 400].
     */
    public static final String CONF_CONTENT_RADII = "content.radii";
    public static final String DEFAULT_CONTENT_RADII = "200, 400";

    private final List<Integer> radii;
    private final LocationMatcher matcher;

    public DOMSLocationProximity(Configuration conf) {
        super(conf);

        List<String> radiiS = conf.getStrings(CONF_CONTENT_RADII, Arrays.asList(DEFAULT_CONTENT_RADII.split(",")));
        radii = new ArrayList<>(radiiS.size());
        for (String radiusS: radiiS) {
            radii.add(Integer.parseInt(radiusS));
        }
        matcher = new LocationMatcher(conf);

        log.info("Created " + this);
    }

    @Override
    protected void produceSegments(Payload payload, String content, int altoStart, int altoEnd) throws PayloadException {
        // Compensate for wring declaration in the ALTO XMLK
        Alto alto;
        try {
            alto = new Alto(content.substring(altoStart, altoEnd), payload.getId());
            alto.setHyphenMode(hyphenMode);
        } catch (XMLStreamException e) {
            throw new PayloadException("Unable to parse ALTO for substring " + altoStart + ", " + altoEnd, e, payload);
        }

        for (Alto.Page page : alto.getLayout()) {
            for (Alto.TextBlock block : page.getPrintSpace()) {
                matcher.reset();
                if (matcher.findMatches(block.getAllText()) == 0) {
                    continue;
                }
                Map<String, String> annuli = getContents(alto, block.getCenterX(), block.getCenterY());
                for (Map.Entry<String, Set<String>> location : matcher.getLocations().entrySet()) {
                    addRecords(
                            payload, alto, content, altoStart, altoEnd, location.getKey(), location.getValue(), annuli);
                }
            }
        }
    }

    private Map<String, String> getContents(Alto alto, int origoX, int origoY) {
        throw new UnsupportedOperationException("!");
    }

    private void addRecords(Payload payload, Alto alto, String content, int altoStart, int altoEnd, String designation,
                            Set<String> coordinates, Map<String, String> annuli) throws PayloadException {
        StringWriter sw = new StringWriter();
        try {
            XMLStreamWriter locationXML = xmlOutFactory.createXMLStreamWriter(sw);
            locationXML.writeStartElement("altosegment");
            locationXML.writeAttribute("altoid", payload.getId());
            addAltoBasics(locationXML, alto);
            matcher.addLocations(locationXML, designation, coordinates);

            for (Map.Entry<String, String> annulus: annuli.entrySet()) {
                locationXML.writeStartElement("content_" + annulus.getKey());
                locationXML.writeCharacters(annulus.getValue());
                locationXML.writeEndElement();
                locationXML.writeCharacters("\n");
            }
        } catch (XMLStreamException e) {
            throw new PayloadException("Unable to generate ALTO segment XML", e, payload);
        }

        String concatID = payload.getId() + "-loc-" + sanitize(designation);
        addToQueue(payload, content, concatID, altoStart, altoEnd, sw.toString());
    }

    private final Matcher SANITIZER = Pattern.compile("[^a-z0-9_-]").matcher("");
    private String sanitize(String designation) {
        return SANITIZER.reset(designation.toLowerCase().replace(" ", "_")).replaceAll("");
    }

}
