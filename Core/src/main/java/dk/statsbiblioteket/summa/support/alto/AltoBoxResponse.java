package dk.statsbiblioteket.summa.support.alto;/*
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

import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseImpl;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * Helper class for {@link dk.statsbiblioteket.summa.support.alto.AltoBoxSearcher}, encapsulating the external response.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AltoBoxResponse extends ResponseImpl {
    private static volatile XMLOutputFactory xmlOutFactory;
    private volatile ByteArrayOutputStream os;

    private Set<String> requestTerms;
    private Set<String> resolvedTerms = new HashSet<>();
    private Set<String> requestRecordIDs;
    private Set<String> resolvedRecordIDs = new HashSet<>();
    private Map<String, List<Box>> boxes = new HashMap<>();

    public AltoBoxResponse(Collection<String> requestTerms, Collection<String> requestRecordIDs) {
        super("altobox");
        this.requestTerms = new HashSet<>(requestTerms);
        this.requestRecordIDs = new HashSet<>(requestRecordIDs);
        synchronized (AltoBoxResponse.class) {
            if (xmlOutFactory == null) {
                xmlOutFactory = XMLOutputFactory.newInstance();
            }
        }
        os = new ByteArrayOutputStream();
    }

    public void add(String recordID, Box box) {
        getSpecificBoxes(recordID).add(box);
    }
    public void addAll(String recordID, Collection<Box> boxes) {
        getSpecificBoxes(recordID).addAll(boxes);
    }
    private List<Box> getSpecificBoxes(String recordID) {
        List<Box> boxes = this.boxes.get(recordID);
        if (boxes == null) {
            boxes = new ArrayList<>();
            this.boxes.put(recordID, boxes);
        }
        return boxes;
    }

    public Set<String> getRequestTerms() {
        return requestTerms;
    }

    public void addAllResolvedTerms(Collection<String> terms) {
        resolvedTerms.addAll(terms);
    }

    public Set<String> getRequestRecordIDs() {
        return requestRecordIDs;
    }

    public void addResolvedRecordID(String recordID) {
        resolvedRecordIDs.add(recordID);
    }
    public Set<String> getResolvedRecordIDs() {
        return resolvedRecordIDs;
    }


    public Set<String> getLookupTerms() {
        return resolvedTerms.isEmpty() ? requestTerms : resolvedTerms;
    }

    public Set<String> getLookupRecordIDs() {
        return resolvedRecordIDs.isEmpty() ? requestRecordIDs : resolvedRecordIDs;
    }

    @Override
    public synchronized void merge(Response otherResponse) throws ClassCastException {
        if (!(otherResponse instanceof AltoBoxResponse)) {
            return;
        }
        super.merge(otherResponse);
        AltoBoxResponse other = (AltoBoxResponse)otherResponse;

        requestTerms.addAll(other.requestTerms);
        resolvedTerms.addAll(other.resolvedTerms);
        requestRecordIDs.addAll(other.requestRecordIDs);
        resolvedRecordIDs.addAll(other.resolvedRecordIDs);
        for (Map.Entry<String, List<Box>> entry: other.boxes.entrySet()) {
            addAll(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public String getName() {
        return "AltoBoxResponse";
    }

    @Override
    public String toXML() {
        os.reset();
        XMLStreamWriter xml;
        try {
            xml = xmlOutFactory.createXMLStreamWriter(os);
        } catch (XMLStreamException e) {
            throw new RuntimeException("Unable to create XMLStreamWriter", e);
        }

        try {
            xml.writeStartElement("boxresponse");
            xml.writeAttribute("requestTerms", Strings.join(requestTerms));
            xml.writeAttribute("resolvedTerms", Strings.join(resolvedTerms));
            xml.writeAttribute("requestRecords", Strings.join(requestRecordIDs));
            xml.writeAttribute("resolvedRecords", Strings.join(resolvedRecordIDs));
            xml.writeAttribute(TIMING, getTiming());
            xml.writeCharacters("\n");

            for (Map.Entry<String, List<Box>> entry: boxes.entrySet()) {
                xml.writeCharacters("  ");
                xml.writeStartElement("document");
                xml.writeAttribute("id", entry.getKey());
                xml.writeCharacters("\n");
                for (Box box: entry.getValue()) {
                    box.toXML(xml);
                }
                xml.writeEndElement(); // document
                xml.writeCharacters("\n");
            }
            xml.writeEndElement(); // boxResponse
            xml.writeCharacters("\n");

        // TODO: Implement this

            xml.flush();
        } catch (XMLStreamException e) {
            throw new RuntimeException("XMLStreamException", e);
        }
        try {
            os.flush();
        } catch (IOException e) {
            throw new RuntimeException("Unable to flush output stream", e);
        }
        return os.toString();
    }

    public static class Box implements Serializable {
        protected  int hpos;
        protected  int vpos;
        protected  int width;
        protected  int height;
        protected String content;
        protected String wc;
        protected String cc;

        public Box(int hpos, int vpos, int width, int height, String content) {
            this(hpos, vpos, width, height, content, "N/A", "N/A");
        }
        public Box(int hpos, int vpos, int width, int height, String content, String wc, String cc) {
            this.hpos = hpos;
            this.vpos = vpos;
            this.width = width;
            this.height = height;
            this.content = content;
            this.wc = wc;
            this.cc = cc;
        }

        public void toXML(XMLStreamWriter xml) throws XMLStreamException {
            xml.writeCharacters("    ");
            xml.writeStartElement("textblock");
            xml.writeAttribute("x", Integer.toString(hpos));
            xml.writeAttribute("y", Integer.toString(vpos));
            xml.writeAttribute("width", Integer.toString(width));
            xml.writeAttribute("height", Integer.toString(height));
            xml.writeAttribute("wc", wc);
            xml.writeAttribute("cc", cc);
            xml.writeCharacters(content);
            xml.writeEndElement(); // textblock
            xml.writeCharacters("\n");
        }
    }
}
