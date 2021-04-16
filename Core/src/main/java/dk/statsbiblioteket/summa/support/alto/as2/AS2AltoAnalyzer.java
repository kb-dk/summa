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
package dk.statsbiblioteket.summa.support.alto.as2;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.support.alto.Alto;
import dk.statsbiblioteket.summa.support.alto.AltoAnalyzerBase;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AS2AltoAnalyzer extends AltoAnalyzerBase<AS2AltoAnalyzer.AS2Segment> {
    private static Log log = LogFactory.getLog(AS2AltoAnalyzer.class);

    // AdresseContoirsEfterretninger-1795-06-13-01-0006.alto.xml -> 17950613
    private static final Serializable DEFAULT_AS2_DATE_PATTERN = ".+-(\\d{4})-(\\d{2})-(\\d{2})-\\d{2}-.*";

    /**
     * The authoritative source for the image. This is used to derive URL and ID.
     * </p><p>
     * Optional. Valid values are 'alto' (the image source as stated in the alto file) and
     * 'origin' (the path for the alto XML). Default is 'alto'.
     * </p>
     */
    public static final String CONF_DESIGNATION_SOURCE = "altoanalyzer.designationsource";
    public static final String DEFAULT_DESIGNATION_SOURCE = DESIGNATION_SOURCE.alto.toString();
    public enum DESIGNATION_SOURCE {alto, origin}

    // foo/bar/AdresseContoirsEfterretninger-1795-06-13-01-0006.alto.xml ->
    // AdresseContoirsEfterretninger-1795-06-13-01-0006
    public static final String CONF_ID_PATTERN = "altoanalyzer.papername.pattern";
    public static final String DEFAULT_ID_PATTERN =
            ".*[/\\\\]([^/\\\\]+-\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{4}[^\\.]*).*";

    public static final String CONF_ID_REPLACEMENT = "altoanalyzer.papername.replacement";
    public static final String DEFAULT_ID_REPLACEMENT = "$1";

    // B400022028241-RT2/400022028241-14/1795-06-13-01/AdresseContoirsEfterretninger-1795-06-13-01-0006.alto.xml ->
    // http://foo/B400022028241-RT2/400022028241-14/1795-06-13-01/AdresseContoirsEfterretninger-1795-06-13-01-0006.html
    /**
     * Points to a full view for the image. All backwards slashes will be replaced with forward slashes.
     */
    public static final String CONF_URL_REGEXP = "asaltoanalyzer.url";
    public static final String DEFAULT_URL_REGEXP =
            "^(.*/[^/]+-\\d{4}-\\d{2}-\\d{2}-\\d{2}-[^/.]+).*$";
            //".*([^/]+/[^/]+/[^/]+/[^/]+-\\d{4}-\\d{2}-\\d{2}-\\d{2}-[^/.]+).*$";

    public static final String CONF_URL_REPLACEMENT = "asaltoanalyzer.url.replacement";
    public static final String DEFAULT_URL_REPLACEMENT =
            "http://pc254.sb.statsbiblioteket.dk/quack/tilbud2/$1.html";

    private static final String CONF_TITLE_MAXLENGTH = "asaltoanalyzer.title.maxlength";
    private static final int DEFAULT_TITLE_MAXLENGTH = 40;

    /**
     * The minimum amount of blocks that must be present to constitute a segment.
     * </p><p>
     * Optional. Default is 1.
     */
    public static final String CONF_SEGMENT_MIN_BLOCKS = "asaltoanalyzer.segment.minblocks";
    public static final int DEFAULT_SEGMENT_MIN_BLOCKS = 1;

    /**
     * The minimum number of characters be present for a group og blocks to be considered a segment.
     * </p><p>
     * Optional. Default is 100.
     */
    public static final String CONF_SEGMENT_MIN_TEXT_LENGTH = "asaltoanalyzer.segment.mintextlength";
    public static final int DEFAULT_SEGMENT_MIN_TEXT_LENGTH = 100;

    private final Pattern paperIDPattern;
    private final String paperIDReplacement;
    private final Pattern urlPattern;
    private final String urlReplacement;
    private final int maxTitleLength;
    private final DESIGNATION_SOURCE designationSource;
    final private int minBlocks;
    final private int minTextLength;

    public AS2AltoAnalyzer(Configuration conf) {
        super(addDefaultDatePattern(conf));
        paperIDPattern = Pattern.compile(conf.getString(CONF_ID_PATTERN, DEFAULT_ID_PATTERN));
        paperIDReplacement = conf.getString(CONF_ID_REPLACEMENT, DEFAULT_ID_REPLACEMENT);
        urlPattern = Pattern.compile(conf.getString(CONF_URL_REGEXP, DEFAULT_URL_REGEXP));
        urlReplacement = conf.getString(CONF_URL_REPLACEMENT, DEFAULT_URL_REPLACEMENT);
        maxTitleLength = conf.getInt(CONF_TITLE_MAXLENGTH, DEFAULT_TITLE_MAXLENGTH);
        minBlocks = conf.getInt(CONF_SEGMENT_MIN_BLOCKS, DEFAULT_SEGMENT_MIN_BLOCKS);
        minTextLength = conf.getInt(CONF_SEGMENT_MIN_TEXT_LENGTH, DEFAULT_SEGMENT_MIN_TEXT_LENGTH);
        designationSource = DESIGNATION_SOURCE.valueOf(
                conf.getString(CONF_DESIGNATION_SOURCE, DEFAULT_DESIGNATION_SOURCE));
        log.info("Created " + this);
    }

    private static Configuration addDefaultDatePattern(Configuration conf) {
        if (!conf.containsKey(AltoAnalyzerBase.CONF_DATE_PATTERN)) {
            conf.set(AltoAnalyzerBase.CONF_DATE_PATTERN, DEFAULT_AS2_DATE_PATTERN);
        }
        return conf;
    }

    // TODO: Make this configurable
    private final static String ID_PREFIX = "sb_avis_";
    @Override
    public List<AS2Segment> getSegments(Alto alto) {
        String rawDesignation = getRawDesignation(alto);

        Matcher paperNameMatcher = paperIDPattern.matcher(rawDesignation);
        if (!paperNameMatcher.matches()) {
            throw new IllegalArgumentException("Unable to get paper name from '" + rawDesignation
                                               + "' with pattern '" + paperIDPattern.pattern() + "'");
        }
        String paperName = paperNameMatcher.replaceAll(paperIDReplacement);

        List<AS2Segment> segments = new ArrayList<>(alto.getTextBlockGroups().size());
        for (Map.Entry<String, List<Alto.TextBlock>> entry: alto.getTextBlockGroups(minBlocks, minTextLength).entrySet()) {
            AS2Segment segment = new AS2Segment(alto);
            // TODO: Use alto filename as prefix for ID to make it unique
            // ***
            segment.setId(ID_PREFIX + paperName + "_" + entry.getKey());

            segment.setOrigin(alto.getOrigin());
            segment.setFilename(alto.getFilename());
            segment.setDate(getDateFromFilename(alto.getFilename()));
            segment.setBoundingBox(Alto.getBoundingBox(entry.getValue()));
            segment.setPageWidth(alto.getLayout().get(0).getWidth()); // TODO: Remove reliance on page 0
            segment.setPageHeight(alto.getLayout().get(0).getHeight()); // TODO: Remove reliance on page 0

            boolean first = true;
            for (Alto.TextBlock textBlock: entry.getValue()) {
                if (first) {
                    // TODO: Use font size to guess headings instead
                    segment.setTitle(textBlock.getAllText(), maxTitleLength);
                    first = false;
                }
                segment.addParagraph(textBlock.getAllText());
            }
            segments.add(segment);
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format(Locale.ROOT, "Created %d segments with base paperName '%s' (constructed from '%s')",
                                    segments.size(), ID_PREFIX + paperName, rawDesignation));
        }
        return segments;
    }

    private String getRawDesignation(Alto alto) {
        String rawDesignation;
        switch (designationSource) {
            case alto:
                rawDesignation = alto.getFilename();
                break;
            case origin:
                rawDesignation = alto.getOrigin();
                break;
            default: throw new UnsupportedOperationException(
                    "The designation source " + designationSource + " is unknown");
        }
        return rawDesignation.replace("\\", "/");
    }

    @Override
    public AS2Segment createSegment() {
        throw new UnsupportedOperationException("Segments must have an Alto assigned for AS2AltoAnalyzer");
    }

    public class AS2Segment extends AltoAnalyzerBase.Segment {
        private final Alto alto;

        public AS2Segment(Alto alto) {
            this.alto = alto;
        }

        @Override
        public String getURL() {
            Matcher originMatcher = urlPattern.matcher(getRawDesignation(alto));
            if (!originMatcher.matches()) {
                throw new IllegalArgumentException("Unable to match '" + getRawDesignation(alto)
                                                   + "' using origin pattern '" + urlPattern.pattern() + "'");
            }
            return originMatcher.replaceAll(urlReplacement);
        }

        @Override
        public void addIndexTerms(List<Term> terms) {
            terms.add(new Term("lma", "as"));
            terms.add(new Term("lma_long", "avisscanning"));
        }

        @Override
        public String getType() {
            return "sb_avis";
        }
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT,
                "AS2AltoAnalyzer(paperIDPattern='%s', paperIDReplacement='%s', urlPattern='%s', urlReplacement='%s', "
                + "minBlocks=%d, minTextLength=%d)",
                paperIDPattern.pattern(), paperIDReplacement, urlPattern.pattern(), urlReplacement,
                minBlocks, minTextLength);
    }
}
