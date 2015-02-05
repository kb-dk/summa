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
package dk.statsbiblioteket.summa.support.alto;

import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.xml.XMLStepper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An object representation of an Alto-file.
 * </p><p>
 * Note: The representation does not support the ComposedBlock grouping of elements.
 * TODO: Add ComposedBlock support.
 */
public class Alto {
    private static Log log = LogFactory.getLog(Alto.class);

    /**
     * When requesting groups with {@link #getTextBlockGroups(int, int)}, blocks not satisfying the minWord constraint
     * will be put in a group with this name.
     */
    public static final String NOGROUP = "_nogroup_";

    /**
     * http://www.loc.gov/standards/alto/techcenter/elementSet/index.php#ALTO_DescriptionElements
     * percent is percent of image width and height respectively.
     */
    public enum MEASUREMENT_UNIT {pixel, mm10, inch1200, percent}
    public static final MEASUREMENT_UNIT DEFAULT_MEASUREMENT_UNIT = MEASUREMENT_UNIT.mm10;

    public enum SEGMENT_LEVEL {page, textblock, textline, string}

    private static final XMLInputFactory factory = XMLInputFactory.newInstance();
    static {
        factory.setProperty(XMLInputFactory.IS_COALESCING, true);
    }

    private String filename = null;
    private String origin = null;
    private String processingStepSettings = null;
    private MEASUREMENT_UNIT measurementUnit = DEFAULT_MEASUREMENT_UNIT;
    private Map<String, TextStyle> styles = new HashMap<>();
    private List<Page> layout = new ArrayList<>();
    private HYPHEN_MODE hyphenMode = HYPHEN_MODE.join;

    public enum HYPHEN_MODE {split, join}

    public Alto(String xml, String origin) throws XMLStreamException {
        this(new StringReader(xml), origin);
    }
    public Alto(File xml) throws XMLStreamException, FileNotFoundException {
        this(new FileReader(xml), xml.getAbsolutePath());
    }
    public Alto(Reader xml) throws XMLStreamException {
        this(factory.createXMLStreamReader(xml));
    }
    public Alto(Reader xml, String origin) throws XMLStreamException {
        this(factory.createXMLStreamReader(xml), origin);
    }
    public Alto(XMLStreamReader xml) throws XMLStreamException {
        this(xml, null);
    }
    // coalescing expected
    public Alto(XMLStreamReader xml, String origin) throws XMLStreamException {
        log.trace("Starting alto parsing of XML with origin " + origin);
        long startTime = System.currentTimeMillis();
        this.origin = origin;
        this.filename = origin;
        XMLStepper.iterateTags(xml, new XMLStepper.Callback() {
            @Override
            public boolean elementStart(
                    XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                if ("MeasurementUnit".equals(current)) {
                    String unit = xml.getElementText();
                    if (!unit.isEmpty()) {
                        try {
                            measurementUnit = MEASUREMENT_UNIT.valueOf(unit);
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException(String.format(
                                    "The MeasurementUnit '%s' is unknown. Only the following units are accepted: %s",
                                    unit, Strings.join(MEASUREMENT_UNIT.values())), e);
                        }
                    }
                    return true;
                }
//              <sourceImageInformation>
//                  <fileName>400026952148-1\2002-01-31-01\morgenavisenjyllandsposten-2002-01-31-01-0570A.jp2</fileName>
//              </sourceImageInformation>
                if ("fileName".equals(current)) {
                    filename = xml.getElementText();
                    return true;
                }

//                <OCRProcessing ID="OCR1">
//                    <ocrProcessingStep>
//                        <processingStepSettings>
                if ("ocrProcessingStep".equals(current)) {
                    XMLStepper.jumpToNextTagStart(xml);
                    if ("processingStepSettings".equals(xml.getLocalName())) {
                        processingStepSettings = xml.getElementText();
                    }
                    XMLStepper.findTagEnd(xml, xml.getLocalName());
                    xml.next();
                    return true;
                }
                if ("Styles".equals(current)) {
                    xml.next();
                    XMLStepper.iterateElements(xml, "Styles", "TextStyle", new XMLStepper.XMLCallback() {
                        @Override
                        public void execute(XMLStreamReader xml) throws XMLStreamException {
                            TextStyle style = new TextStyle(xml);
                            styles.put(style.getId(), style);
                        }
                    });
                    return true;
                }
                if ("Layout".equals(current)) {
                    xml.next();
                    XMLStepper.iterateElements(xml, "Layout", "Page", new XMLStepper.XMLCallback() {
                        @Override
                        public void execute(XMLStreamReader xml) throws XMLStreamException {
                            Page page = new Page(xml);
                            layout.add(page);
                        }
                    });
                    return true;
                }
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        if (log.isDebugEnabled()) {
            log.debug(String.format("Successfully parsed alto XML in %dms, fileName='%s', #pages=%d",
                                    System.currentTimeMillis() - startTime, filename, layout.size()));
        }
    }

    public String getFilename() {
        return filename;
    }
    public MEASUREMENT_UNIT getMeasurementUnit() {
        return measurementUnit;
    }
    public String getProcessingStepSettings() {
        return processingStepSettings;
    }
    private final static Pattern PWA = Pattern.compile("Predicted Word Accuracy:([0-9]+.[0-9]+)", Pattern.DOTALL);
    private final static Pattern CER = Pattern.compile("Character Error Ratio:([0-9]+.[0-9]+)", Pattern.DOTALL);
    public Double getPredictedWordAccuracy() {
        return getDouble(PWA);
    }
    public Double getCharacterErrorRatio() {
        return getDouble(CER);
    }
    private Double getDouble(Pattern pattern) {
        Matcher matcher;
        if (processingStepSettings == null || !(matcher = pattern.matcher(processingStepSettings)).find()) {
            return null;
        }
        try {
            return Double.parseDouble(matcher.group(1));
        } catch (NumberFormatException e) {
            log.warn("Unable to parse '" + matcher.group(1) + "' as a double from pattern " + pattern.pattern());
        }
        return null;
    }
    public Map<String, TextStyle> getStyles() {
        return styles;
    }
    public List<Page> getLayout() {
        return layout;
    }
    public String getOrigin() {
        return origin;
    }

    private Map<String, List<TextBlock>> textBlockGroups = null;
    /**
     * Iterates all TextBlocks on the page and returns them grouped by {@link PositionedElement#idNext}.
     * If an element is not connected to any other elements, a single-entry group will be created for it.
     * </p><p>
     * The generation of groups is lazy but stored for subsequent requests.
     * </p><p>
     * The IDs of the groups are auto-generated and only unique within the current Alto object. The IDs are not
     * guaranteed to follow a consistent numbering pattern. Ordering is attempted but not guaranteed.
     * @return a map of grouped TextBlocks.
     */
    public Map<String, List<TextBlock>> getTextBlockGroups() {
        if (textBlockGroups != null) {
            return textBlockGroups;
        }
        int counter = 1;
        int blockCount = 0;
        Map<String, List<TextBlock>> groups = new LinkedHashMap<>();

        // This is O(n^2), but normally there are few TextBlocks (10-30) so optimization has low priority
        for (Page page: getLayout()) {
            currents:
            for (TextBlock currentBlock: page.getPrintSpace()) {
                // Check for links to and from this block
                for (Map.Entry<String, List<TextBlock>> entry: groups.entrySet()) {
                    List<TextBlock> groupedBlocks = entry.getValue();
                    for (TextBlock groupedBlock: groupedBlocks) {
                        blockCount++;
                        if (equals(currentBlock.getIDNext(), groupedBlock.getID())
                            || equals(groupedBlock.getIDNext(), currentBlock.getID())) {
                            log.trace("Adding block " + currentBlock.getID() + " to group " + entry.getKey());
                            entry.getValue().add(currentBlock);
                            continue currents;
                        }
                    }
                }
                // No links, create a new group
                // TODO: Smarter name generation by sorting blocks according to linked ID and using the first ID
                String groupID = "segment_" + counter++;
                log.trace("Creating group " + groupID + " for block " + currentBlock.getID());
                groups.put(groupID, new ArrayList<>(Arrays.asList(currentBlock)));
            }
        }
        log.debug("Created " + groups.size() + " groups with a total of " + blockCount + " TextBlocks");
        textBlockGroups = groups;
        return groups;
    }

    public List<String> getAllTexts() {
        List<String> texts = new ArrayList<>();
        for (Page page: getLayout()) {
            texts.addAll(page.getAllTexts());
        }
        return texts;
    }

    private Map<String, Map<String, List<TextBlock>>> minGroups = new HashMap<>();
    /**
     * Works like {@link #getTextBlockGroups()} with the differences that groups containing less than minWords are
     * collapsed into the single group {@link #NOGROUP}.
     * @param minBlocks the minimum amount of blocks needed to constitute a group.
     * @param minWords  the minimum amount of words needed to constitute a group.
     * @return the groups as described above.
     */
    public Map<String, List<TextBlock>> getTextBlockGroups(int minBlocks, int minWords) {
        final String key = "minBlocks=" + minBlocks + ", minWords=" + minWords;
        if (minGroups.containsKey(key)) {
            return minGroups.get(key);
        }
        Map<String, List<TextBlock>> all = getTextBlockGroups();
        List<TextBlock> rubble = new ArrayList<>();
        Map<String, List<TextBlock>> pruned = new HashMap<>(all.size());

        for (Map.Entry<String, List<TextBlock>> entry: all.entrySet()) {
            if (entry.getValue().size() < minBlocks || countWords(entry.getValue()) < minWords) {
                rubble.addAll(entry.getValue());
            } else {
                pruned.put(entry.getKey(), entry.getValue());
            }
        }
        if (!rubble.isEmpty()) {
            pruned.put(NOGROUP, rubble);
        }
        minGroups.put(key, pruned);
        return pruned;
    }

    private int countWords(List<? extends PositionedElement> elements) {
        int count = 0;
        for (PositionedElement element: elements) {
            for (String text: element.getAllTexts()) {
                count += text.split(" ").length;
            }
        }
        return count;
    }


    /**
     * Calculate the minimum box containing all elements.
     * @param elements the elements that must be inside the box.
     * @return a bounding box for the elements.
     */
    public static Box getBoundingBox(List<? extends PositionedElement> elements) {
        int left = Integer.MAX_VALUE;
        int top = Integer.MAX_VALUE;
        int right = -1;
        int bottom = -1;
        for (PositionedElement element: elements) {
            left = Math.min(left, element.getHpos());
            top = Math.min(top, element.getVpos());
            right = Math.max(right, element.getHpos() + element.getWidth());
            bottom = Math.max(right, element.getVpos() + element.getHeight());
        }
        left = left == Integer.MAX_VALUE ? -1 : left; 
        top = top == Integer.MAX_VALUE ? -1 : top; 
        return new Box(left, top, right-left, bottom-top);
    }

    // null != null
    private boolean equals(String s1, String s2) {
        return s1 != null && s2 != null && s1.equals(s2);
    }

    // <Page ID="P1" PHYSICAL_IMG_NR="0003" HEIGHT="3605" WIDTH="2557">
    //   <TopMargin ID="TM1" HPOS="0" VPOS="0" WIDTH="2557" HEIGHT="153" />
    //   <LeftMargin ID="LM1" HPOS="0" VPOS="153" WIDTH="192" HEIGHT="3413" />
    //   <RightMargin ID="RM1" HPOS="2436" VPOS="153" WIDTH="121" HEIGHT="3413" />
    //   <BottomMargin ID="BM1" HPOS="0" VPOS="3566" WIDTH="2557" HEIGHT="39" />
    //   <PrintSpace ID="BS1" HPOS="192" VPOS="153" WIDTH="2244" HEIGHT="3413">
    //     <TextBlock ID="TB_0001" HPOS="192" VPOS="158" WIDTH="480" HEIGHT="128">
    public final class Page extends PositionedElement {
        private List<TextBlock> printSpace = new ArrayList<>();

        public Page(XMLStreamReader xml) throws XMLStreamException {
            super(xml);
            xml.next();
            XMLStepper.iterateElements(xml, "Page", "TextBlock", new XMLStepper.XMLCallback() {
                @Override
                public void execute(XMLStreamReader xml) throws XMLStreamException {
                    TextBlock textBlock = new TextBlock(xml);
                    printSpace.add(textBlock);
                }
            });
            log.trace("Parsed Page " + getID() + " with " + printSpace.size() + " TextBlocks");
        }

        public List<TextBlock> getPrintSpace() {
            return printSpace;
        }

        @Override
        public List<String> getAllTexts() {
            List<String> result = new ArrayList<>();
            for (PositionedElement pe: printSpace) {
                result.addAll(pe.getAllTexts());
            }
            return result;
        }
    }

    // <TextBlock ID="TB_0001" HPOS="192" VPOS="158" WIDTH="480" HEIGHT="128">
    //   <TextLine ID="Tl_0001" HPOS="192" VPOS="158" WIDTH="480" HEIGHT="87">
    public final class TextBlock extends PositionedElement {
        private List<TextLine> lines = new ArrayList<>();

        public TextBlock(XMLStreamReader xml) throws XMLStreamException {
            super(xml);
            xml.next();
            XMLStepper.iterateElements(xml, "TextBlock", "TextLine", new XMLStepper.XMLCallback() {
                @Override
                public void execute(XMLStreamReader xml) throws XMLStreamException {
                    TextLine textLine = new TextLine(xml);
                    lines.add(textLine);
                }
            });
            log.trace("Parsed TextBlock " + getID() + " with " + lines.size() + " TextLines");
        }

        public List<TextLine> getLines() {
            return lines;
        }

        public String getAllText() {
            StringBuilder sb = new StringBuilder(100);
            for (TextLine l: lines) {
                if (sb.length() != 0) {
                    sb.append(' ');
                }
                sb.append(l.getAllText());
            }
            return sb.toString();
        }

        @Override
        public List<String> getAllTexts() {
            List<String> result = new ArrayList<>();
            for (PositionedElement pe: lines) {
                result.addAll(pe.getAllTexts());
            }
            return result;
        }

        public String toString() {
            return "TextBlock(" + getHpos() + ", " + getVpos() + ". #lines=" + lines.size() + ")";
        }
    }

    // <TextLine ID="Tl_0001" HPOS="192" VPOS="158" WIDTH="480" HEIGHT="87">
    //   <String ID="TS_0001" STYLEREFS="TXT_1" HPOS="192" VPOS="158" WIDTH="480" HEIGHT="87" CONTENT="SONDAG" />
    public final class TextLine extends PositionedElement {
        private final String style;
        private List<TextString> strings = new ArrayList<>();

        public TextLine(XMLStreamReader xml) throws XMLStreamException {
            super(xml);
            style = XMLStepper.getAttribute(xml, "STYLEREFS", null);

            xml.next();
            XMLStepper.iterateElements(xml, "TextLine", "String", new XMLStepper.XMLCallback() {
                @Override
                public void execute(XMLStreamReader xml) throws XMLStreamException {
                    TextString textString = new TextString(xml);
                    strings.add(textString);
                }
            });
            xml.next();
            log.trace("Parsed TextLine " + getID() + " with " + strings.size() + " Strings");
        }

        public List<TextString> getTextStrings() {
            return strings;
        }

        public String getStyle() {
            return style;
        }

        public String getAllText() {
            StringBuilder sb = new StringBuilder(100);
            for (TextString l: strings) {
                if (sb.length() != 0) {
                    sb.append(' ');
                }
                sb.append(l.getContent());
            }
            return sb.toString();
        }

        @Override
        public List<String> getAllTexts() {
            List<String> result = new ArrayList<>();
            for (PositionedElement ts: strings) {
                result.addAll(ts.getAllTexts());
            }
            return result;
        }
    }

    // <String ID="TS_0001" STYLEREFS="TXT_1" HPOS="192" VPOS="158" WIDTH="480" HEIGHT="87" CONTENT="SONDAG" />

    public final class TextString extends PositionedElement {
        private final String content;
        private final String styleRefs;
        private final String subsContent; // The whole term from multi-line hyphenated terms. Only 2 lines supported!

        public TextString(XMLStreamReader xml) throws XMLStreamException {
            super(xml);
            content = XMLStepper.getAttribute(xml, "CONTENT", null);
            styleRefs = XMLStepper.getAttribute(xml, "STYLEREFS", null);
    // <String ID="S369" CONTENT="her" WC="0.852" CC="7 8 8" HEIGHT="148" WIDTH="176" HPOS="3700" VPOS="8052" SUBS_TYPE="HypPart1" SUBS_CONTENT="herfra"/>
            subsContent = XMLStepper.getAttribute(xml, "SUBS_CONTENT", null);
            xml.next();
        }

        public String getContent() {
            return content;
        }
        public String getStyleRefs() {
            return styleRefs;
        }

        @Override
        public List<String> getAllTexts() {
            if (subsContent != null && hyphenMode == HYPHEN_MODE.join) {
                if (content.equals(subsContent.substring(0, content.length()))) { // Content is prefix
                    return Arrays.asList(subsContent);
                }
                return Collections.emptyList();

            }
            return Arrays.asList(content);
        }
    }

    public static final class TextStyle {
        private final String id;
        private final String fontFamily;
        private final Double fontSize;
        private final String fontStyle;

        // Positioned at a TextStyle element
        // <TextStyle ID="TXT_4" FONTFAMILY="TimesNewRoman" FONTSIZE="7.5" FONTSTYLE="bold" />
        public TextStyle(XMLStreamReader xml) throws XMLStreamException {
            id = XMLStepper.getAttribute(xml, "ID", null);
            fontFamily = XMLStepper.getAttribute(xml, "FONTFAMILY", null);
            String fontSizeStr = XMLStepper.getAttribute(xml, "FONTSIZE", null);
            fontSize = fontSizeStr == null ? null : Double.parseDouble(fontSizeStr);
            fontStyle = XMLStepper.getAttribute(xml, "DONTSTYLE", null);
            xml.next();
        }

        public String getId() {
            return id;
        }
        public String getFontFamily() {
            return fontFamily;
        }
        public Double getFontSize() {
            return fontSize;
        }
        public String getFontStyle() {
            return fontStyle;
        }
    }

    public static abstract class PositionedElement extends Box {
        private final String id;
        private final String idNext;

        public PositionedElement(XMLStreamReader xml) throws XMLStreamException {
            super(Integer.parseInt(XMLStepper.getAttribute(xml, "HPOS", "-1")),
                  Integer.parseInt(XMLStepper.getAttribute(xml, "VPOS", "-1")),
                  Integer.parseInt(XMLStepper.getAttribute(xml, "WIDTH", "-1")),
                  Integer.parseInt(XMLStepper.getAttribute(xml, "HEIGHT", "-1")), 1, 1); // TODO: Implement this (1, 1) is wrong
            id = XMLStepper.getAttribute(xml, "ID", null);
            idNext = XMLStepper.getAttribute(xml, "IDNEXT", null);
        }

        /**
         * @return all the text in the element, broken down to the smallest elements (TextString).
         */
        public abstract List<String> getAllTexts();

        public String getID() {
            return id;
        }
        public String getIDNext() {
            return idNext;
        }
    }

    /**
     * When extracting text from lines or above, multi-line hyphenated words can be exported in multiple ways.
     * @param hyphenMode if {@code split}, hyphenated words are reported as distinct words,
     *                   if {@code join}, hyphenated words are joined without hyphenation sign.
     */
    public void setHyphenMode(HYPHEN_MODE hyphenMode) {
        this.hyphenMode = hyphenMode;
    }

    public static class Box {
        protected final int hpos;
        protected final int vpos;
        protected final int width;
        protected final int height;
        protected final double scaleH;
        protected final double scaleV;
        
        public Box(int hpos, int vpos, int width, int height, double scaleH, double scaleV) {
            this.hpos = hpos;
            this.vpos = vpos;
            this.width = width;
            this.height = height;
            this.scaleH = scaleH;
            this.scaleV = scaleV;
        }
        public Box(int hpos, int vpos, int width, int height) {
            this.hpos = hpos;
            this.vpos = vpos;
            this.width = width;
            this.height = height;
            this.scaleH = 1.0;
            this.scaleV = 1.0;
        }
        public int getHpos() {
            return hpos;
        }
        public int getVpos() {
            return vpos;
        }
        public int getWidth() {
            return width;
        }
        public int getHeight() {
            return height;
        }

        /**
         * @return horizontal position (x) as a fraction of the page width.
         */
        public double getHposFraction() {
            return hpos*scaleH;
        }

        /**
         * @return vertical position (y) as a fraction of the page height.
         */
        public double getVposFraction() {
            return vpos*scaleV;
        }
        /**
         * @return width as a fraction of the page width.
         */
        public double getWidthFraction() {
            return width*scaleH;
        }

        /**
         * @return height as a fraction of the page height.
         */
        public double getHeightFraction() {
            return height*scaleV;
        }

        
        @Override
        public String toString() {
            return "Box(" + hpos + "," + vpos + " " + width + "x" + height + ")";
        }
    }
}
