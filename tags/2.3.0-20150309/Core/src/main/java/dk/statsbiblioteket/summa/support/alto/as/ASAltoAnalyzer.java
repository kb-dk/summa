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
package dk.statsbiblioteket.summa.support.alto.as;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.support.alto.Alto;
import dk.statsbiblioteket.summa.support.alto.AltoAnalyzerBase;
import dk.statsbiblioteket.summa.support.alto.AltoAnalyzerSetup;
import dk.statsbiblioteket.util.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Avissamling (a project at Statsbiblioteket) specific analyzer for Altos.
 */
public class ASAltoAnalyzer extends AltoAnalyzerBase<ASAltoAnalyzer.ASSegment> {
    private static Log log = LogFactory.getLog(ASAltoAnalyzer.class);

    /**
     * A list of sub configurations containing setups. When an Alto is precessed, the setup from the first matching
     * setup in the list is used.
     * </p><p>
     * Optional. Default is a list with 1 default {@link ASAltoAnalyzerSetup}.
     * </p>
     */
    public static final String CONF_SETUPS = "asaltoanalyzer.setups";

    /**
     *
     */
    public static final String CONF_ORIGIN2URL_REGEXP = "asaltoanalyzer.origin2url";
    // 1/Berlingske-2012-01-02-01-0002.alto
    public static final String DEFAULT_ORIGIN2URL_REGEXP = ".*/(\\d+/[^/]+).alto.xml";
    public static final String CONF_ORIGIN2URL_REPLACEMENT = "asaltoanalyzer.origin2url.replacement";
    public static final String DEFAULT_ORIGIN2URL_REPLACEMENT = "http://pc254.sb.statsbiblioteket.dk/alto/$1_files/";

    private final List<ASAltoAnalyzerSetup> setups = new ArrayList<>();
    private final Pattern origin2url;
    private final String origin2urlReplacement;

    public ASAltoAnalyzer(Configuration conf) throws SubConfigurationsNotSupportedException {
        super(conf);
        origin2url = Pattern.compile(conf.getString(CONF_ORIGIN2URL_REGEXP, DEFAULT_ORIGIN2URL_REGEXP));
        origin2urlReplacement = conf.getString(CONF_ORIGIN2URL_REPLACEMENT, DEFAULT_ORIGIN2URL_REPLACEMENT);
        if (conf.valueExists(CONF_SETUPS)) {
            List<Configuration> subs = conf.getSubConfigurations(CONF_SETUPS);
            for (Configuration sub: subs) {
                setups.add(new ASAltoAnalyzerSetup(sub));
            }
        } else {
            log.info("No setups defined under key " + CONF_SETUPS + ". Using a single default setup");
            setups.add(new ASAltoAnalyzerSetup(Configuration.newMemoryBased()));
        }
    }

    /**
     * The heart of the analyzer tries to extract Segments from the given alto. Special segments are "Program x" that
     * are used to mark subsequent segments with the right program.
     * @param alto an object representation of alto XML.
     * @return the Segments for the page in the alto (note: Currently only the first page is processed).
     */
    @Override
    public List<ASSegment> getSegments(Alto alto) {
        ASAltoAnalyzerSetup setup = getSetup(alto);
        // We'll do a lot of random access extraction so linked lists seems the obvious choice (ignoring caching)
        final List<Alto.TextBlock> blocks = new LinkedList<>(alto.getLayout().get(0).getPrintSpace());
        final List<ASSegment> segments = new ArrayList<>();
        int hPos = 0;
        int vPos = -1;
        int maxHPos = Integer.MAX_VALUE;

        while (!blocks.isEmpty()) {
            Alto.TextBlock best = null;

            // Find the first valid candidate with the given parameters
            for (Alto.TextBlock candidate: blocks) {
                if (candidate.getHpos() >= hPos && candidate.getVpos() > vPos && candidate.getHpos() <= maxHPos) {
                    best = candidate;
                    break;
                }
            }
            // Endless loop detection
            if (best == null && maxHPos == Integer.MAX_VALUE) {
                log.warn(String.format(
                        "getSegments found %d segments with %d remaining TextBlocks, where there should be 0 " +
                        "remaining. The content of the TextBlocks follows:\n%s",
                        segments.size(), blocks.size(), dumpFull(blocks)));
                return segments;
            }

            // If there are no candidate, adjust search parameters for next column
            if (best == null) {
                hPos = maxHPos;
                vPos = -1;
                maxHPos = Integer.MAX_VALUE;
                continue;
            }

            // See if there is a better candidate
            for (Alto.TextBlock candidate: blocks) {
                if (candidate.getHpos() >= hPos && candidate.getVpos() > vPos && candidate.getHpos() <= maxHPos) {
                    // Valid. Check is the distance is better
                    if (getDistance(setup, hPos, vPos, candidate) < getDistance(setup, hPos, vPos, best)) {
                        best = candidate;
                    }
                }
            }

            // We got the best block. Remove it from the pool
            blocks.remove(best);
            maxHPos = best.getHpos() + best.getWidth(); // Only skip to next column when the current one is exhausted
            vPos = best.getVpos();

            // Create the segment
            ASSegment segment = blockToSegment(alto, best);

            // See if there are blocks below that belongs to the current segment
            // TODO: Implement this
            segments.add(segment);
        }
        enrichWithNearBlocks(setup, segments, new LinkedList<>(alto.getLayout().get(0).getPrintSpace()));
        return segments;
    }

    /**
     * Adds the text content from TextLines spatially near to the segments and lower priority search text.
     * This introduces false positives for searching, but should be ranked lower than the correct results.
     * @param setup the setup for the given segments.
     * @param segments extracted segments.
     * @param blocks all blocks.
     */
    private void enrichWithNearBlocks(
            ASAltoAnalyzerSetup setup, List<ASSegment> segments, LinkedList<Alto.TextBlock> blocks) {
        for (ASSegment segment: segments) {
            for (Alto.PositionedElement element: getNearbyTexts(setup, segment, blocks)) {
                segment.addNearText(Strings.join(element.getAllTexts(), " "));
            }
        }
    }

    private List<Alto.PositionedElement> getNearbyTexts(
            ASAltoAnalyzerSetup setup, ASSegment segment, LinkedList<Alto.TextBlock> blocks) {
        int origoX = segment.getHpos() + segment.getWidth()/2;
        int origoY = segment.getVpos() + segment.getHeight()/2;
        double maxDist = segment.getWidth() * setup.getNearbyFactor();
        List<Alto.PositionedElement> lines = new ArrayList<>();
        for (Alto.TextBlock block: blocks) {
            for (Alto.TextLine line: block.getLines()) {
                for (Alto.TextString ts: line.getTextStrings()) {
                    if (isNear(origoX, origoY, maxDist, ts)) {
                        lines.add(line);
                    }
                }
            }
        }
        return lines;
    }

    private boolean isNear(int origoX, int origoY, double maxDist, Alto.PositionedElement element) {
        int lineX = element.getHpos() + element.getWidth()/2;
        int lineY = element.getVpos() + element.getHeight()/2;
        return Math.sqrt(Math.pow(origoX-lineX, 2) + Math.pow(origoY-lineY, 2)) < maxDist;
    }

    private ASAltoAnalyzerSetup getSetup(Alto alto) {
        for (ASAltoAnalyzerSetup setup: setups) {
            if (setup.fitsDate(getDateFromFilename(alto.getFilename()))) {
                return setup;
            }
        }
        throw new IllegalStateException(
                "Unable to find a ASAltoAnalyzerSetup that matches the date " + getDateFromFilename(alto.getFilename())
                + ". Consider adding a catch-all setup at the end of the setup chain");
    }

    private double getDistance(AltoAnalyzerSetup setup, int hPos, int vPos, Alto.TextBlock candidate) {
        return Math.sqrt(Math.pow((hPos-candidate.getHpos())*setup.getHdistFactor(), 2)
                         + Math.pow(vPos-candidate.getVpos(), 2));
    }

    @Override
    protected ASSegment blockToSegment(Alto alto, Alto.TextBlock textBlock) {
        ASSegment segment = super.blockToSegment(alto, textBlock);

        List<Alto.TextLine> lines = new LinkedList<>(textBlock.getLines());
        extractTitle(segment, lines);

        // Just add the rest of the lines
        for (Alto.TextLine line: lines) {
            segment.addParagraph(cleanTitle(line.getAllText()));
        }
        return segment;
    }

    private void extractTitle(ASSegment segment, List<Alto.TextLine> lines) {

        // TODO: Match textStyles for headline
        while ((segment.getTitle() == null || segment.getTitle().isEmpty()) && !lines.isEmpty()) { // First real text is the title
            //String textStyle = lines.get(0).getTextStrings().get(0).getStyleRefs();
            segment.setTitle(cleanTitle(lines.remove(0).getAllText()));
        }
    }

    // TODO: Improve cleanup by collapsing multiple spaces and removing "unrealistic" chars
    protected String cleanTitle(String text) {
        text = text.trim();
        if (".".equals(text)) {
            return "";
        }
        return text;
    }

    @Override
    public ASSegment createSegment() {
        return new ASSegment();
    }

    public class ASSegment extends AltoAnalyzerBase.Segment  {
        private String nearText = null;

        public void addNearText(String nearText) {
            if (this.nearText == null) {
                this.nearText = nearText;
            } else {
                this.nearText += " " + nearText;
            }
        }

        @Override
        public String toString() {
            return "ASSegment(title='" + getTitle() + "', #paragraphs=" + getParagraphs().size()
                   + (getParagraphs().isEmpty() ? "" : ": " + Strings.join(getParagraphs(), 10)) + ')';
        }

        @Override
        public String getType() {
            return "avisscanning";
        }

        @Override
        public void addIndexTerms(List<Term> terms) {
            terms.add(new Term("lma", "as"));
            terms.add(new Term("lma_long", "avisscanning"));
            if (nearText != null) {
                // TODO: Consider special field
                terms.add(new Term("freetext", nearText));
            }
        }

        @Override
        public String getURL() {
            if (getOrigin() == null) {
                return null;
            }

            Matcher o2u = origin2url.matcher(getOrigin());
            if (!o2u.matches()) {
                log.warn("Unable to match origin2url pattern '" + origin2url.pattern() + "' to '" + getOrigin() + "'");
                return null;
            }
            return o2u.replaceAll(origin2urlReplacement);
        }
    }
}
