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

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.summa.ingest.split.ThreadedStreamParser;
import dk.statsbiblioteket.summa.support.alto.Alto;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.util.*;

/**
 * Highly Statsbiblioteket specific filter for splitting DOMS Records containing newspaper meta data and ALTO-XML
 * into heuristically determined newspaper articles.
 * </p><p>
 * The format of the generated XML is
 * ...doms-prefix-xml...
 * <altosegment altoid="original-payload-id" segmentid="generatedgroupid" connected="true/false">
 *     <headline>
 *         Generated headline (empty if connected=false)
 *     </headline>
 *     <content>
 *         <textblock>First block</textblock>
 *         <textblock>Second block</textblock>
 *     </content>
 * </altosegment>
 * ...doms-postfix-xml...
 * </p>
 */
// TODO: Better handling of compound-words (write both compoundwords and compund-words)
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DOMSNewspaperParser extends ThreadedStreamParser {
    private static Log log = LogFactory.getLog(DOMSNewspaperParser.class);
    private final XMLOutputFactory xmlOutFactory = XMLOutputFactory.newFactory();

    /**
     * The minimum amount of blocks that must be present to constitute a segment.
     * </p><p>
     * Optional. Default is 1.
     */
    public static final String CONF_SEGMENT_MIN_BLOCKS = "segment.minblocks";
    public static final int DEFAULT_SEGMENT_MIN_BLOCKS = 1;

    /**
     * The minimum number of words for a group of blocks to be considered a segment.
     * </p><p>
     * Optional. Default is 20.
     */
    public static final String CONF_SEGMENT_MIN_WORDS = "segment.minwords";
    public static final int DEFAULT_SEGMENT_MIN_WORDS = 20;

    /**
     * The maximum number of terms in extracted headlines.
     * </p><p>
     * Optional. Default is 10.
     */
    public static final String CONF_HEADLINE_MAX_WORDS = "headline.maxwords";
    public static final int DEFAULT_HEADLINE_MAX_WORDS = 10;

    /**
     * The maximum number of characters in extracted headlines.
     * </p><p>
     * Optional. Default is 40.
     */
    public static final String CONF_HEADLINE_MAX_CHARS = "headline.maxchars";
    public static final int DEFAULT_HEADLINE_MAX_CHARS = DEFAULT_HEADLINE_MAX_WORDS*4;

    /**
     * The base for te generated Reords.
     * </p><p>
     * Optional. Default is 'aviser'.
     */

    public static final String CONF_BASE = "domsnewspaperparser.base";
    public static final String DEFAULT_BASE = "aviser";

    /**
     * Whether or not to pass ALTO-less records onwards.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_ACCEPT_ALTOLESS = "domsnewspaperparser.acceptaltoless";
    public static final boolean DEFAULT_ACCEPT_ALTOLESS = true;

    /**
     * How to handle hyphenated multi-line terms. Valid values are
     * {@code split}; hyphenated words are reported as distinct words,
     * {@code join}, hyphenated words are joined without hyphenation sign.
     * </p><p>
     * Optional. Default is {@code join}.
     */
    public static final String CONF_HYPHENATION = "altoparser.hyphenation";
    public static final String DEFAULT_HYPHENATION = Alto.HYPHEN_MODE.join.toString();

    /**
     * If true, relatives (parent Records and child Records) are preserved when producing segment Records.
     * </p><p>
     * Optional boolean. Default is true.
     */
    public static final String CONF_KEEPRELATIVES = "altoparser.keeprelatives";
    public static final boolean DEFAULT_KEEPRELATIVES = true;

    public static final String NOALTO = "_noalto_";

    final private int minBlocks;
    final private int minWords;
    private final int maxHeadlineWords;
    private final int maxHeadlineChars;
    final private boolean acceptALTOLess;
    final private String base;
    private final Alto.HYPHEN_MODE hyphenMode;
    private final boolean keepRelatives;
    private final NumberFormat spatial = NumberFormat.getInstance(Locale.ENGLISH);
    {
        spatial.setGroupingUsed(false);
    }

    public DOMSNewspaperParser(Configuration conf) {
        super(conf);
        minBlocks = conf.getInt(CONF_SEGMENT_MIN_BLOCKS, DEFAULT_SEGMENT_MIN_BLOCKS);
        minWords = conf.getInt(CONF_SEGMENT_MIN_WORDS, DEFAULT_SEGMENT_MIN_WORDS);
        maxHeadlineWords = conf.getInt(CONF_HEADLINE_MAX_WORDS, DEFAULT_HEADLINE_MAX_WORDS);
        maxHeadlineChars = conf.getInt(CONF_HEADLINE_MAX_CHARS, DEFAULT_HEADLINE_MAX_CHARS);
        base = conf.getString(CONF_BASE, DEFAULT_BASE);
        acceptALTOLess = conf.getBoolean(CONF_ACCEPT_ALTOLESS, DEFAULT_ACCEPT_ALTOLESS);
        hyphenMode = Alto.HYPHEN_MODE.valueOf(conf.getString(CONF_HYPHENATION, DEFAULT_HYPHENATION));
        keepRelatives = conf.getBoolean(CONF_KEEPRELATIVES, DEFAULT_KEEPRELATIVES);
        log.info("Created " + this);
    }

    @Override
    protected void protectedRun(Payload source) throws PayloadException {
        // Hackity hack. We should stream-process the XML, which seems to require quite a lot of custom coding

        // Unbound schemaLocation makes Java 1.7 XML streaming throw an exception, so we must remove it
        String content = RecordUtil.getString(source).replaceAll("xsi:schemaLocation=\"[^\"]+\"", "");
        int altoStart = content.indexOf("<alto ");
        int altoEnd = content.indexOf(">", content.indexOf("</alto"))+1;
        if (altoStart < 0 || altoEnd < 1) {
            if (!acceptALTOLess) {
                throw new PayloadException(
                        "Unable to locate start (" + altoStart + ") or end (" + altoEnd + ") tags for element 'alto'",
                        source);
            }
            Logging.logProcess("DOMSNewspaperParser", "Unable to locate ALTO. Passing content unmodified",
                               Logging.LogLevel.INFO, source);
            try {
                addToQueue(source, new Record(source.getId() + NOALTO, base, content.getBytes("utf-8")));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("utf-8 not supported", e);
            }
        } else {
            produceSegments(source, content, altoStart, altoEnd);
        }
    }

    private void produceSegments(Payload payload, String content, int altoStart, int altoEnd) throws PayloadException {
        // Compensate for wring declaration in the ALTO XMLK
        Alto alto;
        try {
            alto = new Alto(content.substring(altoStart, altoEnd), payload.getId());
            alto.setHyphenMode(hyphenMode);
        } catch (XMLStreamException e) {
            throw new PayloadException("Unable to parse ALTO for substring " + altoStart + ", " + altoEnd, e, payload);
        }
        List<Alto.Illustration> illustrations = alto.getIllustrations(); // Not coupled to textBlockGroups
        Map<String, List<Alto.TextBlock>> groups = alto.getTextBlockGroups(minBlocks, minWords);
        if (groups.isEmpty()) {
            Logging.logProcess("DOMSNewspaperParser",
                               "Unable to extract any groups at all. Creating empty NOGROUP segment",
                               Logging.LogLevel.INFO, payload);
            groups.put(Alto.NOGROUP, Collections.<Alto.TextBlock>emptyList());
        }

        final String pre = content.substring(0, altoStart);
        final String post = content.substring(altoEnd);

        int segmentCount = 1;
        for (Map.Entry<String, List<Alto.TextBlock>> group: groups.entrySet()) {
            StringWriter sw = new StringWriter();
            try {
                XMLStreamWriter segmentXML = xmlOutFactory.createXMLStreamWriter(sw);
                /*
 * <altosegment altoid="original-payload-id" segmentid="generatedgroupid" connected="true/false">
 *     <headline>
 *         Generated headline
 *     </headline>
 *     <content>
 *         <textblock>First block</textblock>
 *         <textblock>Second block</textblock>
 *     </content>
 * </altosegment>
                 */
                segmentXML.writeStartElement("altosegment");
                segmentXML.writeAttribute("altoid", payload.getId());
                segmentXML.writeAttribute("segmentid", group.getKey());
                segmentXML.writeAttribute("segmentCount", Integer.toString(segmentCount++));
                if (Alto.NOGROUP.equals(group.getKey())) {
                    segmentXML.writeAttribute("connected", "false");
                } else {
                    segmentXML.writeAttribute("connected", "true");
                    segmentXML.writeCharacters("\n");
                    segmentXML.writeStartElement("headline"); // Duplicated as first textblock in content
                    segmentXML.writeCharacters(getHeadline(group.getValue()));
                    segmentXML.writeEndElement();
                }
                segmentXML.writeCharacters("\n");
                writeIfDefined(segmentXML, "filename", alto.getFilename());
                writeIfDefined(segmentXML, "origin", alto.getOrigin());
                writeIfDefined(segmentXML, "processingStepSettings", alto.getProcessingStepSettings());
                writeIfDefined(segmentXML, "measurementUnit", alto.getMeasurementUnit());
                writeIfDefined(segmentXML, "pageWidth", alto.getWidth());
                writeIfDefined(segmentXML, "pageHeight", alto.getHeight());
                writeIfDefined(segmentXML, "pagePixels", alto.getPixels());
                writeIfDefined(segmentXML, "predictedWordAccuracy", alto.getPredictedWordAccuracy());
                writeIfDefined(segmentXML, "predictedWordAccuracy_sort", padDouble(alto.getPredictedWordAccuracy()));
                writeIfDefined(segmentXML, "characterErrorRatio", alto.getCharacterErrorRatio());
                writeIfDefined(segmentXML, "characterErrorRatio_sort", padDouble(alto.getCharacterErrorRatio()));
                segmentXML.writeStartElement("content");
                segmentXML.writeCharacters("\n");
                long blockCount = 0;
                long wordCount = 0;
                long charCount = 0;
                long lowerCount = 0;
                long upperCount = 0;
                long letterCount = 0;
                long digitCount = 0;
                long stopCharCount = 0; // .!?
                long commaCharCount = 0;
                for (Alto.TextBlock block: group.getValue()) {
                    blockCount++;
                    final String allText = block.getAllText();
                    charCount += allText.length();
                    char prevC = ' '; // Whitespace
                    if (!allText.isEmpty()) {
                        wordCount++; // Technically this is wrong as it could all be whitespace
                        for (int i = 0; i < allText.length(); i++) {
                            final char c = allText.charAt(i);
                            if (Character.isWhitespace(c)) {
                                prevC = c;
                                continue;
                            }
                            if (Character.isWhitespace(prevC)) { // Beginning of word
                                wordCount++;
                            }
                            if (Character.isLetter(c)) {
                                letterCount++;
                                if (Character.isLowerCase(c)) {
                                    lowerCount++;
                                }
                                if (Character.isUpperCase(c)) {
                                    upperCount++;
                                }
                            } else if (Character.isDigit(c)) {
                                digitCount++;
                            }
                            switch (c) {
                                case ',':
                                    commaCharCount++;
                                    break;
                                case '.':
                                case '!':
                                case '?':
                                    stopCharCount++;
                                    break;
                            }
                            prevC = c;
                        }
                    }

                    segmentXML.writeStartElement("textblock");
                    segmentXML.writeAttribute("x", spatial.format(block.getHposFraction()));
                    segmentXML.writeAttribute("y", spatial.format(block.getVposFraction()));
                    segmentXML.writeAttribute("width", spatial.format(block.getWidthFraction()));
                    segmentXML.writeAttribute("height", spatial.format(block.getHeightFraction()));
                    segmentXML.writeCharacters(allText);
                    segmentXML.writeEndElement();
                    segmentXML.writeCharacters("\n");
                }
                segmentXML.writeStartElement("statistics");
                segmentXML.writeAttribute("blocks", Long.toString(blockCount));
                segmentXML.writeAttribute("words", Long.toString(wordCount));
                segmentXML.writeAttribute("chars", Long.toString(charCount));
                segmentXML.writeAttribute("lowercase", Long.toString(lowerCount));
                segmentXML.writeAttribute("uppercase", Long.toString(upperCount));
                segmentXML.writeAttribute("letters", Long.toString(letterCount));
                segmentXML.writeAttribute("digits", Long.toString(digitCount));
                segmentXML.writeAttribute("stopchars", Long.toString(stopCharCount));
                segmentXML.writeAttribute("commas", Long.toString(commaCharCount));
                segmentXML.writeEndElement(); // statistics
                segmentXML.writeCharacters("\n");

                segmentXML.writeEndElement(); // content
                segmentXML.writeCharacters("\n");

                segmentXML.writeStartElement("illustrations");
                segmentXML.writeComment("Shared among all segments on the same page");
                segmentXML.writeCharacters("\n");
                for (Alto.Illustration illustration: alto.getIllustrations()) {
                    segmentXML.writeStartElement("illustration");
                    segmentXML.writeAttribute("id", illustration.getID());
                    segmentXML.writeAttribute("x", spatial.format(illustration.getHposFraction()));
                    segmentXML.writeAttribute("y", spatial.format(illustration.getVposFraction()));
                    segmentXML.writeAttribute("width", spatial.format(illustration.getWidthFraction()));
                    segmentXML.writeAttribute("height", spatial.format(illustration.getHeightFraction()));
                    segmentXML.writeEndElement();
                    segmentXML.writeCharacters("\n");
                }
                segmentXML.writeEndElement(); // illustrations
                segmentXML.writeCharacters("\n");


                segmentXML.writeEndElement(); // altosegment
                segmentXML.writeCharacters("\n");

                segmentXML.flush();
            } catch (XMLStreamException e) {
                throw new PayloadException("Unable to generate ALTO segment XML", e, payload);
            }

            String concatID = payload.getId() + "-" + group.getKey();
            try {
                addToQueue(payload, new Record(concatID, base, (pre + sw + post).getBytes("utf-8")));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("utf-8 not supported", e);
            }
        }
    }

    // Special processing: If the origin has parents or children, add those to the articleRecord before queuing
    private void addToQueue(Payload origin, Record articleRecord) {
        if (keepRelatives && origin.getRecord() != null) {
            articleRecord.setParents(origin.getRecord().getParents());
            articleRecord.setChildren(origin.getRecord().getChildren());
        }
        addToQueue(articleRecord);
    }

    private String padDouble(Double val) {
        return val == null ? null : Double.toString(val + 100000).substring(1);
    }

    private void writeIfDefined(XMLStreamWriter xml, String element, Object content) throws XMLStreamException {
        if (content != null && !content.toString().isEmpty()) {
            xml.writeStartElement(element);
            xml.writeCharacters(content.toString());
            xml.writeEndElement();
            xml.writeCharacters("\n");
        }
        // TODO: Implement this
    }

    // TODO: Consider creating a more intelligent headline extractor that looks at font size
    private String getHeadline(List<Alto.TextBlock> segment) {
        final String NO_STYLE = "notDefinedYet";

        if (segment.isEmpty()) {
            return "N/A";
        }
        int length = 0;
        String previousStyle = NO_STYLE;
        List<String> headline = new ArrayList<>();
        out:
        for (Alto.TextLine line: segment.get(0).getLines()) {
            String style = line.getStyle() == null ? "" : line.getStyle();
            if (NO_STYLE.equals(previousStyle)) {
                previousStyle = style;
            }
            if (length > maxHeadlineChars || headline.size() > maxHeadlineWords ||
                !previousStyle.equals(style)) {
                break;
            }
            for (String s: line.getAllTexts()) {
                if (length + s.length() > maxHeadlineChars || headline.size() == maxHeadlineWords) {
                    break out;
                }
                length += s.length();
                headline.add(s);
            }
        }
        if (headline.isEmpty()) {
            return "N/A";
        }
        return Strings.join(headline, " ");
    }

    @Override
    protected boolean acceptStreamlessPayloads() {
        return true;
    }

    @Override
    public String toString() {
        return String.format(
                "DOMSNewspaperSplitter(minBlocks=%d, minWords=%d, hyphenMode=%s, keepRelatives=%b)",
                minBlocks, minWords, hyphenMode, keepRelatives);
    }

    // This did not work due to buffering of the ByteArrayInputStream, making the position unreliable
/*    protected boolean disabledprocessPayload(Payload payload) throws PayloadException {
        // Split the DOMS XML into start, ALTO and end
        byte[] content;
        try {
            content = RecordUtil.getBytes(payload);
        } catch (IOException e) {
            throw new PayloadException("Unable to get content", e, payload);
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(content);

        int altoStart;
        int altoEnd;
        try {
            XMLStreamReader xml = xmlFactory.createXMLStreamReader(bis);
            if (!XMLStepper.findTagStart(xml, "alto")) {
                throw new PayloadException("Unable to find start tag for element 'alto'", payload);
            }
            altoStart = content.length - bis.available() - "<alto".length();
            if (!XMLStepper.findTagEnd(xml, "alto")) {
                throw new PayloadException("Unable to find end tag for element 'alto'", payload);
            }
            altoEnd = content.length - bis.available();
        } catch (XMLStreamException e) {
            throw new PayloadException("Unable to parse input as XML", e, payload);
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("Located inner ALTO from %d to %d (%d bytes) in %s",
                                    altoStart, altoEnd, altoEnd - altoStart, payload.getId()));
        }
        //produceSegments(payload, content, altoStart, altoEnd);
        return true;
    }
  */
}
