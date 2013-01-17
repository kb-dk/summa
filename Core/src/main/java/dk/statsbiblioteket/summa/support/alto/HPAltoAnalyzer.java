/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.    
  */
package dk.statsbiblioteket.summa.support.alto;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hvide Programmer (a project at Statsbiblioteket) specific analyzer for Altos.
 */
public class HPAltoAnalyzer {
    private static Log log = LogFactory.getLog(HPAltoAnalyzer.class);

    // DESCRIPTION is a text block connected to an entry. The vdist and hdist specifies where it may be placed to
    // be considered a candidate

    public static final String CONF_DESCRIPTION_HDIST_MIN = "hp.description.hdist.min";
    public static final int DEFAULT_DESCRIPTION_HDIST_MIN = 10;

    public static final String CONF_DESCRIPTION_HDIST_MAX = "hp.description.hdist.max";
    public static final int DEFAULT_DESCRIPTION_HDIST_MAX = 50;

    public static final String CONF_DESCRIPTION_VDIST_MIN = "hp.description.vdist.min";
    public static final int DEFAULT_DESCRIPTION_VDIST_MIN = 1;
    
    public static final String CONF_DESCRIPTION_VDIST_MAX = "hp.description.vdist.max";
    public static final int DEFAULT_DESCRIPTION_VDIST_MAX = 20;

    private final int descriptionHDistMin;
    private final int descriptionHDistMax;
    private final int descriptionVDistMin;
    private final int descriptionVDistMax;

    public HPAltoAnalyzer(Configuration conf) {
        descriptionHDistMin = conf.getInt(CONF_DESCRIPTION_HDIST_MIN, DEFAULT_DESCRIPTION_HDIST_MIN);
        descriptionHDistMax = conf.getInt(CONF_DESCRIPTION_HDIST_MAX, DEFAULT_DESCRIPTION_HDIST_MAX);
        descriptionVDistMin = conf.getInt(CONF_DESCRIPTION_VDIST_MIN, DEFAULT_DESCRIPTION_VDIST_MIN);
        descriptionVDistMax = conf.getInt(CONF_DESCRIPTION_VDIST_MAX, DEFAULT_DESCRIPTION_VDIST_MAX);
    }

    // TODO: Improve these regexps
    private Pattern programPattern = Pattern.compile("(PROGRAM.+)", Pattern.CASE_INSENSITIVE);
    private Pattern fromTimePattern = Pattern.compile("([0-9]{1,2}).([0-9]{2})(.*)");
    private Pattern fullTimePattern = Pattern.compile("([0-9]{1,2}).([0-9]{2})[^0-9]{1,2}([0-9]{1,2}).([0-9]{2})(.*)");
    public List<Segment> getSegments(Alto alto) {
        List<Segment> segments = new ArrayList<Segment>();
        String lastProgram = null; // Last encountered program
        Alto.TextBlock textBlock = null;
        int hPos = 0;
        int vPos = 0;
        int counter = 0;
        while (true) {
            // TODO: Add support for multiple pages
            textBlock = getNextTextBlock(alto.getLayout().get(0), textBlock);
            if (textBlock == null) {
                break;
            }

            Matcher programMatcher = programPattern.matcher(textBlock.getAllText());
            if (programMatcher.matches()) {
                lastProgram = programMatcher.group(1);
                log.trace("Found program " + lastProgram);
                continue;
            }

            Segment segment = new Segment();
            segment.filename = alto.getFilename();
            segment.date = getDateFromFilename(segment.filename);
            segment.id = "alto_" + segment.date + "_" + counter;
            segment.program = lastProgram;
            segment.hpos = textBlock.getHpos();
            segment.vpos = textBlock.getVpos();

            List<Alto.TextLine> lines = textBlock.getLines();

            boolean gotTime = false;
            Matcher fullTimeMatcher = fullTimePattern.matcher(lines.get(0).getAllText()); // First line
            if (fullTimeMatcher.matches()) {
                segment.startTime = fullTimeMatcher.group(1) + ":" + fullTimeMatcher.group(2);
                segment.endTime = fullTimeMatcher.group(3) + ":" + fullTimeMatcher.group(4);
                segment.title = clean(fullTimeMatcher.group(5));
                log.trace("Found start time " + segment.startTime + " and end time " + segment.endTime + " with title "
                          + segment.title);
                gotTime = true;
            } else {
                Matcher fromTimeMatcher = fromTimePattern.matcher(lines.get(0).getAllText()); // First line
                if (fromTimeMatcher.matches()) {
                    segment.startTime = fromTimeMatcher.group(1) + ":" + fromTimeMatcher.group(2);
                    segment.title = clean(fromTimeMatcher.group(3));
                    log.trace("Found start time " + segment.startTime + " with title " + segment.title);
                    gotTime = true;
                }
            }
            if (gotTime) {
                if (lines.size() > 1) {
                    for (Alto.TextLine line: lines.subList(1, lines.size() - 1)) {
                        segment.paragraphs.add(clean(line.getAllText()));
                    }
                }
            } else {
                for (Alto.TextLine line: lines) {
                    segment.paragraphs.add(clean(line.getAllText()));
                }
            }
            segments.add(segment);
        }
        return segments;
    }

    // TODO: Improve cleanup by collapsing multiple spaces and removing "unrealistic" chars
    private String clean(String text) {
        return text.trim();
    }

    // B-1977-10-02-P-0003.xml -> 19771002
    private final Pattern dateFromFile = Pattern.compile("..([0-9]{4})-([0-9]{2})-([0-9]{2}).*");
    private String getDateFromFilename(String filename) {
        Matcher matcher = dateFromFile.matcher(filename);
        return matcher.matches() ? matcher.group(1) + matcher.group(2) + matcher.group(3) : null;
    }

    // Seek from the given coordinates. Returned blocks are never to the left of the given block.
    // Blocks are scanned from left to right. The block with the lowest hPos that is below the given
    // coordinates is returned, except when the horizontal distance is greater than the with of the
    // previous block. In that case, vpos is set to 0 and hpos to previous.hpos + previous.width,
    // which results in the next column.
    private Alto.TextBlock getNextTextBlock(Alto.Page page, Alto.TextBlock previous) {
        int hPos = previous == null ? 0 : previous.getHpos();
        int vPos = previous == null ? 0 : previous.getVpos();
        int width = previous == null ? 99999999 : previous.getWidth(); // Not maxvalue as we add hpos later
        Alto.TextBlock closest = null;

        for (Alto.TextBlock candidate: page.getPrintSpace()) {
            if (closest == null) {
                if ((candidate.getHpos() >= hPos && candidate.getVpos() > vPos && candidate.getHpos() < hPos+width)
                    || candidate.getHpos() > hPos+width) { // Valid
                    closest = candidate;
                }
                continue;
            }
            // TODO: Introduce slop
            if ((candidate.getHpos() >= hPos && candidate.getVpos() > vPos && candidate.getHpos() < hPos+width)
                || candidate.getHpos() > hPos+width) { // Valid
                if (candidate.getHpos() < closest.getHpos()) { // Prioritize horizontal
                    closest = candidate;
                    continue;
                }
                if (candidate.getHpos() == closest.getHpos()) {
                    if (candidate.getVpos() < closest.getVpos()) {
                        closest = candidate;
                    }
                    continue;
                }
/*                if (candidate.getVpos() < closest.getVpos()) { // Vertical is secondary
                    System.out.println("Replacing " + closest + " with " + candidate);
                    closest = candidate;
                }*/
            }
        }
        return closest;
    }

    public static class Segment {
        private String filename = null;
        private String date = null;
        private String program = null;
        private String id = null;
        private int hpos = -1;
        private int vpos = -1;
        private int width = -1;
        private int height = -1;

        private String startTime = null; // HH:MM
        private String endTime = null;
        private boolean timeApproximate = false;
        private String title = null;
        private List<String> paragraphs = new ArrayList<String>();

        @Override
        public String toString() {
            return "Segment(time=" + startTime + '-' + endTime + ", title='" + title + "', program='" + program + '\''
                   + ", timeApproximate=" + timeApproximate + ", #paragraphs=" + paragraphs.size()
                   + (paragraphs.isEmpty() ? "" : ": " + paragraphs.get(0)) + ')';
        }
    }
}
