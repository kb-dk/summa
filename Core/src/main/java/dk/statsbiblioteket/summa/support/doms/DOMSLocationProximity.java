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
// TODO: This is way too coarse. Need to do spatial calculations based on TextString instead of TextBlock
public class DOMSLocationProximity extends DOMSNewspaperBase {
    private static Log log = LogFactory.getLog(DOMSLocationProximity.class);
    private final XMLOutputFactory xmlOutFactory = XMLOutputFactory.newFactory();

    /**
     * The radii to use when extracting text in proximity to the entity.
     * The starting radius 0 is implicit.
     * </p><p>
     * Optional (but highly recommended). Default is the list [200, 400].
     */
    public static final String CONF_CONTENT_RADII = "proximity.radii";
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
        // Compensate for wring declaration in the ALTO XML
        Alto alto;
        try {
            alto = new Alto(content.substring(altoStart, altoEnd), payload.getId());
            alto.setHyphenMode(hyphenMode);
        } catch (XMLStreamException e) {
            throw new PayloadException("Unable to parse ALTO for substring " + altoStart + ", " + altoEnd, e, payload);
        }

        final Map<String, Integer> locationCounters = new HashMap<>();
        for (Alto.Page page : alto.getLayout()) {
            for (Alto.TextBlock block : page.getPrintSpace()) {
                matcher.reset();
                if (matcher.findMatches(block.getAllText()) == 0) {
                    continue;
                }
                for (Map.Entry<String, Set<String>> location : matcher.getLocations().entrySet()) {
                    int locationCounter = locationCounters.containsKey(location.getKey()) ?
                            locationCounters.get(location.getKey()) : 0;
                    List<Point> centers = getCenters(block, location.getKey());
                    if (centers.isEmpty()) {
                        log.debug("Unable to locate word-based center(s) for '" + location.getKey() +
                                  "', switching to block-based center with " + payload.getId());
                        centers.add(new Point(block.getCenterX(), block.getCenterY()));
                    }
                    for (Point center : centers) {
                        locationCounter++;
                        Map<String, String> annuli = getAnnuli(payload, alto, center);
                        if (annuli.isEmpty()) {
                            continue;
                        }
                        addRecord(payload, alto, content, altoStart, altoEnd,
                                  location.getKey(), locationCounter, location.getValue(), annuli);
                    }
                    locationCounters.put(location.getKey(), locationCounter);
                }
            }
        }
    }

    // Not very efficient as it creates a temporary list of TextStrings

    private final Pattern SPLITTER = Pattern.compile(" ");
    private List<Point> getCenters(Alto.TextBlock block, String locationKey) {
        final List<Alto.TextString> texts = new ArrayList<>();
        for (Alto.TextLine line: block.getLines()) {
            texts.addAll(line.getTextStrings());
        }
        final String[] tokens = SPLITTER.split(locationKey);

        final Point centerSum = new Point(0, 0);
        final List<Point> centers = new ArrayList<>();
        for (int textPos = 0 ; textPos <= texts.size()-tokens.length ; textPos++) {
            addCenterIfValid(texts, textPos, tokens, 0, centerSum, centers);
        }
        return centers;
    }

    private void addCenterIfValid(List<Alto.TextString> texts, int textPos, String[] tokens, int relIndex,
                                  Point centerSum, List<Point> centers) {
        final Alto.TextString text = texts.get(textPos + relIndex);
        if (!text.getContent().equals(tokens[relIndex])) {
            return;
        }
        centerSum = centerSum.add(text.getCenterX(), text.getCenterY());
        if (relIndex == tokens.length-1) {
            centers.add(centerSum.avg(tokens.length));
            return;
        }
        addCenterIfValid(texts, textPos, tokens, relIndex+1, centerSum, centers);
    }

    /**
     * Using {@link #radii}, extract the texts from blocks where the center is inside of the annuli defined by
     * subsequent radii.
     */
    private Map<String, String> getAnnuli(Payload payload, Alto alto, Point origo) {
        Map<String, String> annuli = new LinkedHashMap<>(radii.size());
        int innerRadius = 0;
        int radiiCount = 0;
        for (int outerRadius: radii) {
            if (outerRadius <= 0) { // 0 is implicit
                continue;
            }
            radiiCount++;
            final String content = getAnnulusContent(alto, origo, innerRadius, outerRadius);
            if (log.isTraceEnabled()) {
                log.trace(String.format(Locale.ROOT,
                        "getAnnuli(%s, ..., %s) with annulus #%d(%d→%d) got content of length %d",
                        payload.getId(), origo, radiiCount, innerRadius, outerRadius, content.length()));
            }
            if (content.isEmpty()) {
                innerRadius = outerRadius;
                continue;
            }
            //annuli.put(innerRadius + "_" + outerRadius, content);
            annuli.put(Integer.toString(radiiCount), content);
            innerRadius = outerRadius;
        }
        return annuli;
    }

    /**
     * Iterates all TextBlocks in the Alto and returns the text content of those blocks which center is within the
     * annulus defined by innerRadius (inclusive) and outerRadius (exclusive).
     */
    private String getAnnulusContent(Alto alto, Point origo, int innerRadius, int outerRadius) {
        final StringBuilder sb = new StringBuilder();
        final int innerSquared = innerRadius*innerRadius;
        final int outerSquared = outerRadius*outerRadius;
        int tokenCount = 0;

        for (Alto.Page page : alto.getLayout()) {
            for (Alto.TextBlock block : page.getPrintSpace()) {
                for (Alto.TextLine line: block.getLines()) {
                    for (Alto.TextString text: line.getTextStrings()) {
                        final int distX = text.getCenterX() - origo.x;
                        final int distY = text.getCenterY() - origo.y;
                        final int distSquared = distX * distX + distY * distY;
//                        System.out.println("Center=(" + block.getCenterX() + ", " + block.getCenterY() + "), " +
//                                           "Origo=(" + origo + "), text=" + text.getContent());
                        //System.out.println("Dist: " + distSquared + ", inner=" + innerSquared + ", outer=" + outerSquared);
                        if (innerSquared <= distSquared && distSquared < outerSquared) {
                            if (sb.length() != 0) {
                                sb.append(" ");
                            }
                            sb.append(text.getContent());
                            tokenCount++;
                        }
                    }
                }
            }
        }
        //System.out.println("Origo=(" + origo + "), raadii=" + innerRadius + "/" + outerRadius + ", tokens=" + tokenCount);
        return sb.toString();
    }

    private void addRecord(
            Payload payload, Alto alto, String content, int altoStart, int altoEnd, String designation,
            int locationCounter, Set<String> coordinates, Map<String, String> annuli) throws PayloadException {
        StringWriter sw = new StringWriter();
        try {
            XMLStreamWriter locationXML = xmlOutFactory.createXMLStreamWriter(sw);
            locationXML.writeStartElement("altosegment");
            locationXML.writeAttribute("altoid", payload.getId());
            addAltoBasics(locationXML, alto);
            matcher.addLocations(locationXML, designation, coordinates);

            for (Map.Entry<String, String> annulus: annuli.entrySet()) {
                locationXML.writeStartElement("annulus_" + annulus.getKey());
//                locationXML.writeAttribute("index", annulus.getKey());
                locationXML.writeCharacters(annulus.getValue());
                locationXML.writeEndElement(); // annulus_*
                locationXML.writeCharacters("\n");
            }
            locationXML.writeEndElement(); // altosegment
            locationXML.flush();
        } catch (XMLStreamException e) {
            throw new PayloadException("Unable to generate ALTO segment XML", e, payload);
        }

        String concatID = payload.getId() + "-loc-" + sanitize(designation) + "-" + locationCounter;
        addToQueue(payload, content, concatID, altoStart, altoEnd, sw.toString());
    }

    private final Matcher SANITIZER = Pattern.compile("[^a-z0-9_-]").matcher("");
    private String sanitize(String designation) {
        return SANITIZER.reset(designation.toLowerCase().replace(" ", "_")).replaceAll("");
    }

    // You basic immutable point
    private static class Point {
        public final int x;
        public final int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Point add(int x, int y) {
            return new Point(this.x + x, this.y + y);
        }

        public Point avg(int count) {
            return new Point(this.x / count, this.y / count);
        }

        @Override
        public String toString() {
            return x + ", " + y;
        }
    }
}
