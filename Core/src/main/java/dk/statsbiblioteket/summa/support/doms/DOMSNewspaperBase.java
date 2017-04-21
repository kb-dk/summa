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
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Highly Statsbiblioteket specific filter. Isolates the ALTO-part of a DOMS newspaper-export Record and feeds
 * the ALTO-XML to a subclass for production on new Records.
 */
public abstract class DOMSNewspaperBase extends ThreadedStreamParser {
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
    private static Log log = LogFactory.getLog(DOMSNewspaperBase.class);

    public static final String NOALTO = "_noalto_";

    protected final boolean acceptALTOLess;
    protected final String base;
    protected final Alto.HYPHEN_MODE hyphenMode;
    protected final boolean keepRelatives;
    protected final NumberFormat spatial = NumberFormat.getInstance(Locale.ENGLISH);
    {
        spatial.setGroupingUsed(false);
    }

    public DOMSNewspaperBase(Configuration conf) {
        super(conf);
        base = conf.getString(CONF_BASE, DEFAULT_BASE);
        acceptALTOLess = conf.getBoolean(CONF_ACCEPT_ALTOLESS, DEFAULT_ACCEPT_ALTOLESS);
        hyphenMode = Alto.HYPHEN_MODE.valueOf(conf.getString(CONF_HYPHENATION, DEFAULT_HYPHENATION));
        keepRelatives = conf.getBoolean(CONF_KEEPRELATIVES, DEFAULT_KEEPRELATIVES);
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
            Logging.logProcess(this.getClass().getSimpleName(), "Unable to locate ALTO. Passing content unmodified",
                               Logging.LogLevel.INFO, source);
            try {
                addToQueue(source, createRecord(
                        source, source.getId() + NOALTO, base, content.getBytes("utf-8")));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("utf-8 not supported", e);
            }
        } else {
            produceSegments(source, content, altoStart, altoEnd);
        }
    }

    /**
     * Parse the relevant parts of the content, generate Records and deliver them to {@link #addToQueue}.
     */
    protected abstract void produceSegments(
            Payload payload, String content, int altoStart, int altoEnd) throws PayloadException;

    /**
     * Produces a new Record with the given id, base and content. If a record is embedded in source, its core
     * attributes (isIndexable, isDeleted, creationTime) will be assigned to the newly created Record.
     */
    protected Record createRecord(Payload source, String id, String base, byte[] content) {
        Record record = new Record(id, base, content);
        if (source.getRecord() != null) {
            Record s = source.getRecord();
            record.setDeleted(s.isDeleted());
            record.setIndexable(s.isIndexable());
            record.setCreationTime(s.getCreationTime());
        }
        return record;
    }

    protected void addAltoBasics(XMLStreamWriter segmentXML, Alto alto) throws XMLStreamException {
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
    }
    private String padDouble(Double val) {
        return val == null ? null : Double.toString(val + 100000).substring(1);
    }

    // Special processing: If the origin has parents or children, add those to the articleRecord before queuing
    protected void addToQueue(Payload origin, Record articleRecord) {
        if (keepRelatives && origin.getRecord() != null) {
            articleRecord.setParents(origin.getRecord().getParents());
            articleRecord.setChildren(origin.getRecord().getChildren());
        }
        addToQueue(articleRecord);
    }

    protected void addToQueue(
            Payload origin, String originContent, String newID, int altoStart, int altoEnd, String innerXML) {
        final String pre = originContent.substring(0, altoStart);
        final String post = originContent.substring(altoEnd);
        try {
            addToQueue(origin, createRecord(origin, newID, base, (pre + innerXML + post).getBytes("utf-8")));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("utf-8 not supported", e);
        }
    }

    protected void writeIfDefined(XMLStreamWriter xml, String element, Object content) throws XMLStreamException {
        if (content != null && !content.toString().isEmpty()) {
            xml.writeStartElement(element);
            xml.writeCharacters(content.toString());
            xml.writeEndElement();
            xml.writeCharacters("\n");
        }
    }

    @Override
    protected boolean acceptStreamlessPayloads() {
        return true;
    }

}
