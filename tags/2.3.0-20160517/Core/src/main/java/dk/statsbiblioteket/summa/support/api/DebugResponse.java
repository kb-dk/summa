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
package dk.statsbiblioteket.summa.support.api;

import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseImpl;
import dk.statsbiblioteket.util.qa.QAInfo;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Intended for debugging.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DebugResponse extends ResponseImpl {
    private static final long serialVersionUID = 543897549123L;
    public static final String NAMESPACE = "http://statsbiblioteket.dk/summa/2009/DebugResponse";
    public static final String VERSION = "1.0";

    public static final String DEBUG_RESPONSE = "DebugResponse";

    private static XMLOutputFactory xmlOutputFactory;
    static {
        xmlOutputFactory = XMLOutputFactory.newInstance();
    }

    public final List<DebugGroup> groups = new ArrayList<>();

    public DebugResponse(String implementation, Map<String, String> elements) {
        groups.add(new DebugGroup(implementation, elements));
    }

    /**
     * Shortcut constructor that constructs a map from designation and output.
     */
    public DebugResponse(String implementation, String designation, String output) {
        Map<String, String> elements = new HashMap<>();
        elements.put(designation, output);
        groups.add(new DebugGroup(implementation, elements));
    }

    @Override
    public void merge(Response otherResponse) throws ClassCastException {
        DebugResponse other = (DebugResponse) otherResponse;
        super.merge(other);
        for (DebugGroup otherGroup: other.groups) {
            groups.add(otherGroup);
        }
    }

    @Override
    public String toXML() {
        StringWriter sw = new StringWriter(2000);
        XMLStreamWriter writer;
        try {
            writer = xmlOutputFactory.createXMLStreamWriter(sw);
        } catch (XMLStreamException e) {
            throw new RuntimeException("Unable to create XMLStreamWriter from factory", e);
        }
        try {
            writer.setDefaultNamespace(NAMESPACE);
            //writer.writeStartDocument();
            writer.writeStartElement(DEBUG_RESPONSE);
            writer.writeDefaultNamespace(NAMESPACE);
            writer.writeAttribute(TIMING, getTiming());
            writer.writeCharacters("\n");

            writer.writeCharacters("  ");
            writer.writeStartElement("debugs");
            writer.writeCharacters("\n");
            for (DebugGroup group: groups) {
                group.toXML(writer);
            }
            writer.writeCharacters("  ");
            writer.writeEndElement(); // Debugs
            writer.writeCharacters("\n");

            writer.writeEndElement(); // DEBUG_RESPONSE
            writer.writeCharacters("\n");

            writer.writeEndDocument();
            writer.flush(); // Just to make sure
        } catch (XMLStreamException e) {
            throw new RuntimeException("Got XMLStreamException while constructing XML from SuggestionResponse", e);
        }
        return sw.toString();
    }

    @Override
    public String getName() {
        return DEBUG_RESPONSE;
    }

    private class DebugGroup extends ResponseImpl {
        private final String implementation;
        private final HashMap<String, String> elements;

        public DebugGroup(String implementation, Map<String, String> elements) {
            super(implementation);
            this.implementation = implementation;
            this.elements = elements instanceof HashMap ? (HashMap<String, String>)elements : new HashMap<>(elements);
        }

        @Override
        public String getName() {
            return "DebugGroup_" + implementation;
        }

        @Override
        public void toXML(XMLStreamWriter writer) throws XMLStreamException {
            writer.writeCharacters("    ");
            writer.writeStartElement("group");
            writer.writeAttribute("implementation", implementation);
            writer.writeCharacters("\n");
            for (Map.Entry<String, String> entry: elements.entrySet()) {
                writer.writeCharacters("      ");
                writer.writeStartElement("element");
                writer.writeAttribute("implementation", entry.getKey());
                writer.writeCData(entry.getValue());
                writer.writeCharacters("\n");
            }
            writer.writeCharacters("    ");
            writer.writeEndElement(); // group
            writer.writeCharacters("\n");
        }
    }

}
