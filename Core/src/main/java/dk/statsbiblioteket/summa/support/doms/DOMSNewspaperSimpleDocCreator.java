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
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.summa.support.alto.Alto;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLStepper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.*;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Highly Statsbiblioteket specific filter for producing a simple Solr document from DOMS Streams containing newspaper
 * meta data and ALTO-XML. Intended for Proof-Of-Concept and testing.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DOMSNewspaperSimpleDocCreator extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(DOMSNewspaperSimpleDocCreator.class);
    private XMLInputFactory xmlFactory = XMLInputFactory.newFactory();
    private XMLOutputFactory xmlOutFactory = XMLOutputFactory.newFactory();

    public DOMSNewspaperSimpleDocCreator(Configuration conf) {
        super(conf);
        log.info("Created " + this);
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        XMLStreamReader xml;
        try {
            xml = xmlFactory.createXMLStreamReader(RecordUtil.getReader(payload));
        } catch (XMLStreamException e) {
            throw new PayloadException("Unable to create XMLStreamReader for input", e, payload);
        }
        try {
            XMLStepper.findTagStart(xml, "alto");
            Alto alto = new Alto(xml);
            xml.close();
            payload.setRecord(produceRecord(payload, alto));
        } catch (XMLStreamException e) {
            throw new PayloadException("Unable to process the DOMS XML", e, payload);
        }
        return true;
    }

    int dummyIDCounter = 0;
    private Record produceRecord(Payload payload, Alto alto) throws XMLStreamException {
        final String ID = "DummyID_" + dummyIDCounter++;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLStreamWriter xmlOut = xmlOutFactory.createXMLStreamWriter(out);
        xmlOut.writeStartElement("doc");

        writeField(xmlOut, "recordID", ID);
        writeField(xmlOut, "recordBase", "aviser");
        addShortFormat(xmlOut, ID, "aviser");

        xmlOut.writeEndElement();
        xmlOut.flush();
        return new Record(ID, "aviser", out.toByteArray());
    }

    private void addShortFormat(XMLStreamWriter xmlOut, String id, String base) throws XMLStreamException {
        xmlOut.setNamespaceContext(createShortformatContext());
        xmlOut.writeStartElement("field");
        xmlOut.writeAttribute("name", "shortformat");

        xmlOut.writeStartElement("shortrecord");
//        xmlOut.writeNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
//        xmlOut.writeNamespace("dc", "http://purl.org/dc/elements/1.1/");

        xmlOut.writeStartElement("rdf", "RDF");
        xmlOut.writeStartElement("rdf", "description");

        xmlOut.writeStartElement("dc", "title");
        xmlOut.writeCharacters("Alto " + id);
        xmlOut.writeEndElement();

        xmlOut.writeStartElement("dc", "date");
        xmlOut.writeCharacters("????");
        xmlOut.writeEndElement();

        xmlOut.writeStartElement("dc", "identifier");
        xmlOut.writeCharacters("URL-to-imageserver");
        xmlOut.writeEndElement();

        xmlOut.writeEndElement();
        xmlOut.writeEndElement();
        xmlOut.writeEndElement();

        xmlOut.writeEndElement();
    }

    private NamespaceContext createShortformatContext() {
        return new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                switch (prefix) {
                    case "rdf" : return "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
                    case "dc"  : return "http://purl.org/dc/elements/1.1/";
                    default: throw new IllegalArgumentException("Unable to resolve prefix '" + prefix + "'");
                }
            }

            @Override
            public String getPrefix(String namespaceURI) {
                switch (namespaceURI) {
                    case "http://www.w3.org/1999/02/22-rdf-syntax-ns#" : return "rdf";
                    case "http://purl.org/dc/elements/1.1/" : return "dc";
                    default: throw new IllegalArgumentException("Unable to resolve uri '" + namespaceURI + "'");
                }
            }

            @SuppressWarnings("rawtypes")
            @Override
            public Iterator getPrefixes(String namespaceURI) {
                return Arrays.asList(getPrefix(namespaceURI)).iterator(); // Always only 1
            }
        };
    }

    /*
            shortformat.append("    <rdf:RDF xmlns:dc=\"http://purl.org/dc/elements/1.1/\" "
                               + "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n");
            shortformat.append("      <rdf:Description>\n");
            shortformat.append("        <dc:title>").append(XMLUtil.encode(extracted.getString("Title", "")));
            String subTitle = extracted.getString("Subtitle", "");
            if (!"".equals(subTitle)) {
                shortformat.append(" : ").append(XMLUtil.encode(subTitle));
            }
            shortformat.append("</dc:title>\n");
            addMultiple(extracted, shortformat, "        ", "dc:creator", "Author");
            shortformat.append("        <dc:type xml:lang=\"da\">").
                    append(XMLUtil.encode(extracted.getString("ContentType", ""))).
                    append("</dc:type>\n");
            shortformat.append("        <dc:type xml:lang=\"en\">").
                append(XMLUtil.encode(extracted.getString("ContentType", ""))).
                append("</dc:type>\n");
            shortformat.append("        <dc:date>").
                    append(XMLUtil.encode(extracted.getString("Date", "???"))).
                    append("</dc:date>\n");
            shortformat.append("        <dc:format></dc:format>\n");
            if (extracted.containsKey("openUrl")) {
                shortformat.append("        <dc:identifier>").
                        append(XMLUtil.encode(extracted.getString("openUrl"))).
                        append("</dc:identifier>\n");
            }
            shortformat.append("      </rdf:Description>\n");
            shortformat.append("    </rdf:RDF>\n");
            shortformat.append("  </shortrecord>\n");

        }
      */
    private void writeField(XMLStreamWriter xmlOut, String field, String content) throws XMLStreamException {
        xmlOut.writeStartElement("field");
        xmlOut.writeAttribute("name", field);
        xmlOut.writeCharacters(content);
        xmlOut.writeEndElement();
    }

    public String toString() {
        return "DOMSNewspaperSplitter()";
    }
}
