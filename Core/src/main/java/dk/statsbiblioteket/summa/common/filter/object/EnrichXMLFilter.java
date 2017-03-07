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
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLStepper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.*;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.List;

/**
 * Building block for other filters. Steps through the XML of a record with callback on elements and and performs a
 * final callback right before the end, allowing for insertion of extra XML.
 * </p><p>
 * @see {@link XMLStepper#limitXML} for details on setup.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public abstract class EnrichXMLFilter extends ObjectFilterImpl {
    private static final Log log = LogFactory.getLog(EnrichXMLFilter.class);

    private final XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    {
        xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
        // No resolving of external DTDs
        xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    }
    private final XMLOutputFactory xmlOutFactory = XMLOutputFactory.newInstance();

    /**
     * Constructs a new XML reducing filter, with the specified configuration.
     * @param conf The configuration to construct this filter.
     */
    public EnrichXMLFilter(Configuration conf) {
        super(conf);
    }

    private ByteArrayOutputStream os = new ByteArrayOutputStream();
    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        String content = RecordUtil.getString(payload); // This is two-pass, so we need the copy
        XMLStreamReader in;
        try {
            in = xmlFactory.createXMLStreamReader(new StringReader(content));
        } catch (XMLStreamException e) {
            throw new PayloadException("Unable to create an XMLStreamReader", e, payload);
        }

        // Handle data extraction

        TrackingCallback tracker = new TrackingCallback();
        try {
            XMLStepper.iterateTags(in, tracker);
        } catch (XMLStreamException e) {
            throw new PayloadException("Exception iterating xml", e, payload);
        }

        // TODO: Maybe it would be safer to require a content?
        if (payload.getRecord() == null) {
            payload.setRecord(new Record(
                    payload.getId() == null ? "dummy_id_" + idc++ : payload.getId(), "dummy_base", null));
        }
        payload.getRecord().setContent(enrich(payload, content), payload.getRecord().isContentCompressed());
        return true;
    }
    private int idc = 0;

    private byte[] enrich(Payload payload, String content) throws PayloadException {
        XMLStreamReader in;
        try {
            in = xmlFactory.createXMLStreamReader(new StringReader(content));
        } catch (XMLStreamException e) {
            throw new PayloadException("Unable to create an XMLStreamReader", e, payload);
        }

        // Create new XML content
        os.reset();
        XMLStreamWriter out;
        try {
            out = xmlOutFactory.createXMLStreamWriter(os);
        } catch (XMLStreamException e) {
            throw new PayloadException("Unable to create an XMLStreamWriter", e, payload);
        }

        try {
            // No prologue for Solr XML documents
/*            if (in.getEncoding() == null) {
                out.writeStartDocument(in.getVersion());
            } else {
                out.writeStartDocument(in.getEncoding(), in.getVersion());
            }*/
            in.nextTag(); // First tag
            final String outerElement = in.getLocalName();
            pipeElementStart(in, out);
            in.next();

            while (!(in.isEndElement() && in.getLocalName().equals(outerElement))) {
                XMLStepper.pipeXML(in, out, true);
            }
        } catch (XMLStreamException e) {
            throw new PayloadException("Unable to pipe XML for enrichment data extraction", e, payload);
        }

        try {
            // All piped, except the closing tag
            beforeLastEndTag(out);
            out.writeEndElement();
            out.writeEndDocument();
            out.flush();
            return os.toByteArray();
        } catch (XMLStreamException e) {
            throw new PayloadException("Unable to construct enriched XML", e, payload);
        }
    }

    // Taken from SBUtil XMLStepper
    private void pipeElementStart(XMLStreamReader in, XMLStreamWriter out) throws XMLStreamException {
        if (in.getPrefix() == null || in.getPrefix().isEmpty()) {
            if (in.getNamespaceURI() == null || in.getNamespaceURI().isEmpty()) {
                out.writeStartElement(in.getLocalName());
            } else {
                out.writeStartElement(in.getPrefix(), in.getLocalName(), in.getNamespaceURI());
            }
        } else {
            if (in.getNamespaceURI() == null || in.getNamespaceURI().isEmpty()) {
                throw new XMLStreamException(
                        "Encountered element '" + in.getLocalName() + "' with prefix '" + in.getPrefix()
                        + "' but no namespace URI");
            } else {
                out.writeStartElement(in.getPrefix(), in.getLocalName(), in.getNamespaceURI());
            }
        }
        for (int i = 0 ; i < in.getNamespaceCount() ; i++) {
            out.writeNamespace(in.getNamespacePrefix(i), in.getNamespaceURI(i));
        }
        for (int i = 0 ; i < in.getAttributeCount() ; i++) {
            if (in.getAttributeNamespace(i) == null || in.getAttributeNamespace(i).isEmpty()) {
                out.writeAttribute(in.getAttributeLocalName(i), in.getAttributeValue(i));
            } else {
                out.writeAttribute(in.getAttributeNamespace(i), in.getAttributeLocalName(i),
                                   in.getAttributeValue(i));
            }
        }
    }

    public class TrackingCallback extends XMLStepper.Callback {
        private String firstElement = null;
        @Override
        public boolean elementStart(XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
            if (firstElement == null) {
                firstElement = current;
            }
            return EnrichXMLFilter.this.elementStart(xml, tags, current);
        }

        public String getFirstElement() {
            return firstElement;
        }
    }

    /**
     * Called for each encountered START_ELEMENT in the part of the xml that is within scope. If the implementation
     * calls {@code xml.next()} or otherwise advances the position in the stream, it must ensure that the list of
     * tags is consistent with the position in the DOM.
     * @param xml        the Stream.
     * @param tags       the start tags encountered in the current sub tree.
     * @param current    the local name of the current tag.
     * @return true if the implementation called {@code xml.next()} one or more times, else false.
     */
    public abstract boolean elementStart(XMLStreamReader xml, List<String> tags, String current)
            throws XMLStreamException;

    /**
     * Everything written to the given stream will be inserted just before the end-tag for the outer element.
     * @param xml a stream where all previous XML from the record has been added, except for the closing tag for the
     *            outer element. The closing tag will be added automatically after this method returns.
     */
    public abstract void beforeLastEndTag(XMLStreamWriter xml) throws XMLStreamException;

}