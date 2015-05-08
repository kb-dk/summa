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

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
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
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public abstract class AltoParser extends ThreadedStreamParser {
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

    public static final String CONF_BASE = "altoparser.base";
    public static final String DEFAULT_BASE = "alto";

    public enum OUTPUT {summadocument}

    private static final XMLOutputFactory factory = XMLOutputFactory.newInstance();
    private final AltoAnalyzerBase analyzer;
    private final OUTPUT output;
    private final int snippetCharacters;
    private final String base;

    public AltoParser(Configuration conf) {
        super(conf);
        analyzer = createAnalyzer(conf);
        output = OUTPUT.valueOf(conf.getString(CONF_OUTPUT, DEFAULT_OUTPUT));
        snippetCharacters = conf.getInt(CONF_SNIPPET_CHARACTERS, DEFAULT_SNIPPET_CHARACTERS);
        base = conf.getString(CONF_BASE, DEFAULT_BASE);
        log.info("Created HPAltoAnalyzer");
    }

    protected abstract AltoAnalyzerBase createAnalyzer(Configuration conf);

    @Override
    protected void protectedRun(Payload source) throws Exception {
        List<? extends AltoAnalyzerBase.Segment> segments = analyzer.getSegments(
                new Alto(new InputStreamReader(source.getStream(), "utf-8"), (String)source.getData(Payload.ORIGIN)));
        for (AltoAnalyzerBase.Segment segment: segments) {
            if (segment.getId() == null) {
                Logging.logProcess("AltoParser", "Received segment without ID: " + segment.toString() + ". Skipping",
                                   Logging.LogLevel.WARN, source);
                continue;
            }
            pushSegment(segment);
        }
    }

    // TODO: Make proper Dublin Core output for later XSLT-processing.
    private void pushSegment(AltoAnalyzerBase.Segment segment) throws XMLStreamException {
        switch (output) {
            case summadocument: {
                pushSegmentAsSummaDocument(segment);
                break;
            }
            default: throw new UnsupportedOperationException("Output format " + output + " not supported yet");
        }
    }

    private void pushSegmentAsSummaDocument(AltoAnalyzerBase.Segment segment) throws XMLStreamException {
        byte[] content = getDirectXML(segment);
        Record record = new Record(base + "_" + segment.getId(), base, content);
        addToQueue(record);
    }

    private byte[] getDirectXML(AltoAnalyzerBase.Segment segment) throws XMLStreamException {
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


        List<AltoAnalyzerBase.Segment.Term> terms = new ArrayList<>();

        terms.add(new AltoAnalyzerBase.Segment.Term("title", segment.getTitle()));
        terms.add(new AltoAnalyzerBase.Segment.Term("date", segment.getDate()));
        terms.add(new AltoAnalyzerBase.Segment.Term("year", segment.getYear()));
        terms.add(new AltoAnalyzerBase.Segment.Term("sort_title", segment.getTitle()));
        terms.add(new AltoAnalyzerBase.Segment.Term("url", segment.getURL()));
        terms.add(new AltoAnalyzerBase.Segment.Term("filename", segment.getFilename()));
        terms.add(new AltoAnalyzerBase.Segment.Term("boundingbox", String.format(
                "%d,%d %dx%d %s",
                segment.getHpos(), segment.getVpos(), segment.getWidth(), segment.getHeight(), segment.getTitle())));
        terms.add(new AltoAnalyzerBase.Segment.Term("boundingboxfraction", String.format(
                "%f,%f %fx%f %s",
                segment.getHpos(true), segment.getVpos(true), segment.getWidth(true), segment.getHeight(true),
                segment.getTitle())));
        for (String paragraph: segment.getParagraphs()) {
            terms.add(new AltoAnalyzerBase.Segment.Term("content", paragraph));
        }

        segment.addIndexTerms(terms);
        if (logTermGeneration() && log.isDebugEnabled()) {
            log.debug("getDirectXML: Writing " + terms.size() + " field:text pairs for '" + segment.getId() + "'");
        }
        for (AltoAnalyzerBase.Segment.Term term: terms) {
            writeField(xml, term.field, term.text);
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

    /**
     * @return true if the amount of terms should be logged for {@link #getDirectXML}.
     */
    protected boolean logTermGeneration() {
        return true;
    }

    private String getShortFormat(AltoAnalyzerBase.Segment segment) throws XMLStreamException {
        final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
        final String DC = "http://purl.org/dc/elements/1.1/";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLStreamWriter xml = factory.createXMLStreamWriter(out);
        xml.writeCharacters("      ");
        xml.writeStartElement("shortrecord");
        xml.writeCharacters("\n        ");
        xml.writeStartElement("rdf", "RDF", RDF);
        xml.writeNamespace("rdf", RDF);
        xml.writeNamespace("dc", DC);
        xml.writeCharacters("\n          ");
        xml.writeStartElement("rdf", "Description", RDF);
        xml.writeCharacters("\n");
        writeDC(xml, "title", segment.getShortFormatTitle());
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
        writeDC(xml, "type", segment.getType());
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
