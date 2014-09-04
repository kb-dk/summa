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

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.summa.ingest.split.ThreadedStreamParser;
import dk.statsbiblioteket.summa.support.alto.Alto;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

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
     * The base for te generated Reords.
     * </p><p>
     * Optional. Default is 'aviser'.
     */

    public static final String CONF_BASE = "domsnewspaperparser.base";
    public static final String DEFAULT_BASE = "aviser";

    final private int minBlocks;
    final private int minWords;
    final private String base;

    public DOMSNewspaperParser(Configuration conf) {
        super(conf);
        minBlocks = conf.getInt(CONF_SEGMENT_MIN_BLOCKS, DEFAULT_SEGMENT_MIN_BLOCKS);
        minWords = conf.getInt(CONF_SEGMENT_MIN_WORDS, DEFAULT_SEGMENT_MIN_WORDS);
        base = conf.getString(CONF_BASE, DEFAULT_BASE);
        log.info("Created " + this);
    }

    @Override
    protected void protectedRun(Payload source) throws PayloadException {
        // Hackity hack. We should stream-process the XML, which seems to require quite a lot of custom coding

        // Unbound schemaLocation makes Java 1.7 XML streaming throw an exception, so we must remove it
        final String PROBLEM = "xsi:schemaLocation=\"http://www.loc.gov/standards/alto alto-v2.0.xsd\"";
        final String SOLUTION = "";

        // Split the DOMS XML into start, ALTO and end
        String content = RecordUtil.getString(source).replace(PROBLEM, SOLUTION);
        int altoStart = content.indexOf("<alto ");
        int altoEnd = content.indexOf(">", content.indexOf("</alto"))+1;
        if (altoStart < 0 || altoEnd < 1) {
            throw new PayloadException(
                    "Unable to locate start (" + altoStart + ") or end (" + altoEnd + ") tags for element 'alto'",
                    source);
        }
        produceSegments(source, content, altoStart, altoEnd);
    }

    private void produceSegments(Payload payload, String content, int altoStart, int altoEnd) throws PayloadException {
        // Compensate for wring declaration in the ALTO XMLK
        Alto alto;
        try {
            alto = new Alto(content.substring(altoStart, altoEnd), payload.getId());
        } catch (XMLStreamException e) {
            throw new PayloadException("Unable to parse ALTO for substring " + altoStart + ", " + altoEnd, e, payload);
        }
        Map<String, List<Alto.TextBlock>> groups = alto.getTextBlockGroups(minBlocks, minWords);
        if (groups.isEmpty()) {
            log.warn("Unable to extract any groups at all from " + payload.getId());
            return;
        }

        final String pre = content.substring(0, altoStart);
        final String post = content.substring(altoEnd);

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

                segmentXML.writeStartElement("content");
                segmentXML.writeCharacters("\n");
                for (Alto.TextBlock block: group.getValue()) {
                    segmentXML.writeStartElement("textblock");
                    segmentXML.writeCharacters(block.getAllText());
                    segmentXML.writeEndElement();
                    segmentXML.writeCharacters("\n");
                }
                segmentXML.writeEndElement(); // content
                segmentXML.writeCharacters("\n");

                segmentXML.writeEndElement(); // altosegment
                segmentXML.writeCharacters("\n");

                segmentXML.flush();
            } catch (XMLStreamException e) {
                throw new PayloadException("Unable to generate ALTO segment XML", e, payload);
            }

            String concatID = payload.getId() + "-" + group.getKey();
            try {
                addToQueue(new Record(concatID, base, (pre + sw + post).getBytes("utf-8")));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("utf-8 not supported", e);
            }
        }

    }

    // TODO: Consider creating a more intelligent headline extractor that looks at font size and has a max length
    private String getHeadline(List<Alto.TextBlock> segment) {
        return segment.isEmpty() ? "N/A" : segment.get(0).getAllText();
    }

    @Override
    protected boolean acceptStreamlessPayloads() {
        return true;
    }

    @Override
    public String toString() {
        return String.format(
                "DOMSNewspaperSplitter(minBlocks=%d, minWords=%d)",
                minBlocks, minWords);
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
