/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.    
  */
package dk.statsbiblioteket.summa.support.alto;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.ingest.split.ThreadedStreamParser;
import dk.statsbiblioteket.util.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 *
 */
public class AltoParser extends ThreadedStreamParser {
    private static Log log = LogFactory.getLog(AltoParser.class);

    /**
     * The output format for the parser.
     * </p><p>
     * Optional. Valid values are 'summadocument'. Default value is 'summadocument'.
     */
    public static final String CONF_OUTPUT = "altoparser.output";
    public static final String DEFAULT_OUTPUT = OUTPUT.summadocument.toString();

    /**
     * The maximum number of characters to use for dc:description in shortformat.
     * </p><p>
     * Optional. Default is 50.
     */
    public static final String CONF_SNIPPET_CHARACTERS = "altoparser.snippet.characters";
    public static final int DEFAULT_SNIPPET_CHARACTERS = 100;

    public enum OUTPUT {summadocument}

    private static final XMLOutputFactory factory = XMLOutputFactory.newInstance();
    private final HPAltoAnalyzer analyzer;
    private final OUTPUT output;
    private final int snippetCharacters;

    public AltoParser(Configuration conf) throws SubConfigurationsNotSupportedException {
        super(conf);
        analyzer = new HPAltoAnalyzer(conf);
        output = OUTPUT.valueOf(conf.getString(CONF_OUTPUT, DEFAULT_OUTPUT));
        snippetCharacters = conf.getInt(CONF_SNIPPET_CHARACTERS, DEFAULT_SNIPPET_CHARACTERS);
        log.info("Created HPAltoAnalyzer");
    }

    @Override
    protected void protectedRun(Payload source) throws Exception {
        List<HPAltoAnalyzer.Segment> segments = analyzer.getSegments(
                new Alto(new InputStreamReader(source.getStream(), "utf-8"), (String)source.getData(Payload.ORIGIN)));
        for (HPAltoAnalyzer.Segment segment: segments) {
            if (segment.getId() == null) {
                Logging.logProcess("AltoParser", "Received segment without ID: " + segment.toString() + ". Skipping",
                                   Logging.LogLevel.WARN, source);
                continue;
            }
            pushSegment(segment);
        }
    }

    // TODO: Make proper Dublin Core output for later XSLT-processing.
    private void pushSegment(HPAltoAnalyzer.Segment segment) throws XMLStreamException {
        switch (output) {
            case summadocument: {
                pushSegmentAsSummaDocument(segment);
                break;
            }
            default: throw new UnsupportedOperationException("Output format " + output + " not supported yet");
        }
    }

    private void pushSegmentAsSummaDocument(HPAltoAnalyzer.Segment segment) throws XMLStreamException {
        byte[] content = getDirectXML(segment);
        Record record = new Record("sb_hp_" + segment.getId(), "sb_hp", content);
        addToQueue(record);
    }

    private byte[] getDirectXML(HPAltoAnalyzer.Segment segment) throws XMLStreamException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLStreamWriter xml = factory.createXMLStreamWriter(out);
        xml.writeStartDocument();
        xml.writeStartElement("SummaDocument");
        xml.writeNamespace(null, "http://statsbiblioteket.dk/summa/2008/Document");
        xml.writeAttribute("version", "1.0");
        xml.writeAttribute("id", segment.getId());
        xml.writeCharacters("\n  ");

        xml.writeStartElement("fields");
        xml.writeCharacters("\n");

        writeField(xml, "title", segment.getTitle());
        writeField(xml, "sort_title", segment.getTitle());
        writeField(xml, "sort_time",
                   segment.getDate() + (segment.getStartTime() == null ? "" : "t" + segment.getStartTime()));
        writeField(xml, "lma", "hp");
        writeField(xml, "lma_long", "hvideprogrammer");
        writeField(xml, "starttime", segment.getStartTime());
        writeField(xml, "endtime", segment.getEndTime());
        writeField(xml, "filename", segment.getFilename());
        writeField(xml, "url", segment.getURL());
//        writeField(xml, "freetext", segment.getAllText());
        writeField(xml, "date", segment.getDate());
        writeField(xml, "year", segment.getYear());
        writeField(xml, "timeapproximate", Boolean.toString(segment.isTimeApproximate()));
        for (String paragraph: segment.getParagraphs()) {
            writeField(xml, "content", paragraph);
        }
        xml.writeCharacters("    ");
        xml.writeStartElement("field");
        xml.writeAttribute("name", "shortformat");
        xml.writeCData(getShortFormat(segment));
        xml.writeEndElement();
        xml.writeCharacters("\n");

        xml.writeEndElement(); // fields
        xml.writeCharacters("\n");
        xml.writeEndElement(); // SummaDocument
        xml.writeCharacters("\n");
        xml.writeEndDocument();
        xml.flush();
        return out.toByteArray();
    }

    private String getShortFormat(HPAltoAnalyzer.Segment segment) throws XMLStreamException {
        final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLStreamWriter xml = factory.createXMLStreamWriter(out);
        xml.writeCharacters("      ");
        xml.writeStartElement("shortrecord");
        xml.writeCharacters("\n        ");
        xml.writeStartElement("rdf", "RDF", RDF);
        xml.writeNamespace("RDF", RDF);
        xml.writeCharacters("\n          ");
        xml.writeStartElement("rdf", "Description", RDF);
        xml.writeCharacters("\n");
        if (segment.getStartTime() != null) {
            writeDC(xml, "title", segment.getTitle() + " : " + segment.getReadableTime());
        } else {
            writeDC(xml, "title", segment.getTitle());
        }
        if (!segment.getParagraphs().isEmpty()) {
            String snippet = Strings.join(segment.getParagraphs(), " ");
            if (snippet.length() > snippetCharacters) {
                snippet = snippet.substring(0, snippetCharacters);
            }
            writeDC(xml, "description", snippet);
        }
        // TODO: Add year + airing-iso (for sorting)
        writeDC(xml, "date", segment.getYear());
        writeDC(xml, "identifier", segment.getURL());
        writeDC(xml, "type", "hvideprogrammer");
        writeDC(xml, "lang", "da");
        // http://bja-linux2.sb/index.php?vScale=0.4&hScale=0.4&image=Arkiv_A.6/1929_07-09/PNG/A-1929-07-05-P-0015
        // writeDC(xml, "identifier", "");

        xml.writeCharacters("          ");
        xml.writeEndElement(); // Description

        xml.writeCharacters("\n        ");
        xml.writeEndElement(); // rdf

        xml.writeCharacters("\n      ");
        xml.writeEndElement(); // shortrecord

        xml.writeCharacters("\n    ");
        xml.flush();
        return out.toString();
    }

    private void writeDC(XMLStreamWriter xml, String tag, String content) throws XMLStreamException {
        if (content == null|| content.isEmpty()) {
            return;
        }
        final String DC = "http://purl.org/dc/elements/1.1/";
        xml.writeCharacters("            ");
        xml.writeStartElement("dc", tag, DC);
        xml.writeCharacters(content);
        xml.writeEndElement();
        xml.writeCharacters("\n");
    }

    private void writeField(XMLStreamWriter xml, String field, String content) throws XMLStreamException {
        if (content == null || content.isEmpty()) {
            return;
        }
        xml.writeCharacters("    ");
        xml.writeStartElement("field");
        xml.writeAttribute("name", field);
        xml.writeCharacters(content);
        xml.writeEndElement();
        xml.writeCharacters("\n");
    }
}
