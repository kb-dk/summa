/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.    
  */
package dk.statsbiblioteket.summa.support.alto;

import dk.statsbiblioteket.summa.common.xml.XMLStepper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.util.*;

/**
 * An object representation of an Alto-file.
 */
public class Alto {
    private static Log log = LogFactory.getLog(Alto.class);

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
    private MEASUREMENT_UNIT measurementUnit = DEFAULT_MEASUREMENT_UNIT;
    private Map<String, TextStyle> styles = new HashMap<String, TextStyle>();
    private List<Page> layout = new ArrayList<Alto.Page>();

    public Alto(String xml, String origin) throws XMLStreamException, FileNotFoundException {
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
                        measurementUnit = MEASUREMENT_UNIT.valueOf(unit);
                    }
                    return true;
                }
                if ("fileName".equals(current)) {
                    filename = xml.getElementText();
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
    public Map<String, TextStyle> getStyles() {
        return styles;
    }
    public List<Page> getLayout() {
        return layout;
    }
    public String getOrigin() {
        return origin;
    }

    // <Page ID="P1" PHYSICAL_IMG_NR="0003" HEIGHT="3605" WIDTH="2557">
    //   <TopMargin ID="TM1" HPOS="0" VPOS="0" WIDTH="2557" HEIGHT="153" />
    //   <LeftMargin ID="LM1" HPOS="0" VPOS="153" WIDTH="192" HEIGHT="3413" />
    //   <RightMargin ID="RM1" HPOS="2436" VPOS="153" WIDTH="121" HEIGHT="3413" />
    //   <BottomMargin ID="BM1" HPOS="0" VPOS="3566" WIDTH="2557" HEIGHT="39" />
    //   <PrintSpace ID="BS1" HPOS="192" VPOS="153" WIDTH="2244" HEIGHT="3413">
    //     <TextBlock ID="TB_0001" HPOS="192" VPOS="158" WIDTH="480" HEIGHT="128">
    public static final class Page extends PositionedElement {
        private List<TextBlock> printSpace = new ArrayList<TextBlock>();

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
            List<String> result = new ArrayList<String>();
            for (PositionedElement pe: printSpace) {
                result.addAll(pe.getAllTexts());
            }
            return result;
        }

    }

    // <TextBlock ID="TB_0001" HPOS="192" VPOS="158" WIDTH="480" HEIGHT="128">
    //   <TextLine ID="Tl_0001" HPOS="192" VPOS="158" WIDTH="480" HEIGHT="87">
    public static final class TextBlock extends PositionedElement {
        private List<TextLine> lines = new ArrayList<TextLine>();

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
            List<String> result = new ArrayList<String>();
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
    public static final class TextLine extends PositionedElement {
        private List<TextString> strings = new ArrayList<TextString>();

        public TextLine(XMLStreamReader xml) throws XMLStreamException {
            super(xml);
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
            List<String> result = new ArrayList<String>();
            for (PositionedElement ts: strings) {
                result.addAll(ts.getAllTexts());
            }
            return result;
        }
    }

    // <String ID="TS_0001" STYLEREFS="TXT_1" HPOS="192" VPOS="158" WIDTH="480" HEIGHT="87" CONTENT="SONDAG" />

    public static final class TextString extends PositionedElement {
        private final String content;
        private final String styleRefs;

        public TextString(XMLStreamReader xml) throws XMLStreamException {
            super(xml);
            content = XMLStepper.getAttribute(xml, "CONTENT", null);
            styleRefs = XMLStepper.getAttribute(xml, "STYLEREFS", null);
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

    public static abstract class PositionedElement {
        private final String id;
        private final int hpos;
        private final int vpos;
        private final int width;
        private final int height;

        public PositionedElement(XMLStreamReader xml) throws XMLStreamException {
            id = XMLStepper.getAttribute(xml, "ID", null);
            hpos = Integer.parseInt(XMLStepper.getAttribute(xml, "HPOS", "-1"));
            vpos = Integer.parseInt(XMLStepper.getAttribute(xml, "VPOS", "-1"));
            width = Integer.parseInt(XMLStepper.getAttribute(xml, "WIDTH", "-1"));
            height = Integer.parseInt(XMLStepper.getAttribute(xml, "HEIGHT", "-1"));
        }

        /**
         * @return all the text in the element, broken down to the smallest elements (TextString).
         */
        public abstract List<String> getAllTexts();

        public String getID() {
            return id;
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
    }
}
