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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.Serializable;
import java.util.*;

/**
 * Helper class for {@link dk.statsbiblioteket.summa.support.alto.AltoBoxSearcher}, encapsulating the external response.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AltoBoxResponse extends ResponseImpl {
    private Set<String> requestTerms;
    private Set<String> resolvedTerms = new HashSet<>();
    private Set<String> requestRecordIDs;
    private Set<String> resolvedRecordIDs = new HashSet<>();
    private Map<String, List<Box>> boxes = new HashMap<>();
    private Boolean relativeCoordinates = true;
    // If relative coordinates are used, calculate y and height relative to width, not height
    private boolean yisx = true;

    public AltoBoxResponse(Collection<String> requestTerms, Collection<String> requestRecordIDs) {
        super("altobox.");
        this.requestTerms = new HashSet<>(requestTerms);
        this.requestRecordIDs = new HashSet<>(requestRecordIDs);
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
    public void addAllResolvedRecordIDs(Collection<String> recordIDs) {
        resolvedRecordIDs.addAll(recordIDs);
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
    public Map<String, List<Box>> getBoxes() {
        return boxes;
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
    public void toXML(XMLStreamWriter xml) throws XMLStreamException {
        startln(xml, "boxresponse",
                "requestTerms", Strings.join(requestTerms),
                "resolvedTerms", Strings.join(resolvedTerms),
                "requestRecords", Strings.join(requestRecordIDs),
                "resolvedRecords", Strings.join(resolvedRecordIDs),
                "relative", Boolean.toString(relativeCoordinates),
                "relative_yisx", Boolean.toString(yisx),
                TIMING, getTiming());

        for (Map.Entry<String, List<Box>> entry: boxes.entrySet()) {
            xml.writeCharacters("  ");
            startln(xml, "document", "id", entry.getKey());
            for (Box box: entry.getValue()) {
                box.toXML(xml);
            }
            endln(xml, "  "); // document
        }
        endln(xml); // boxResponse
    }

    public void setRelativeCoordinates(Boolean relativeCoordinates) {
        this.relativeCoordinates = relativeCoordinates;
    }

    public Boolean isRelativeCoordinates() {
        return relativeCoordinates;
    }

    public boolean isYisx() {
        return yisx;
    }

    public void setYisx(boolean relativeCoordinatesYisx) {
        this.yisx = relativeCoordinatesYisx;
    }

    public static class Box implements Serializable {
        protected boolean relative;
        protected  double hpos;
        protected  double vpos;
        protected  double width;
        protected  double height;
        protected String content;
        protected String wc;
        protected String cc;

        public Box(double hpos, double vpos, double width, double height, String content, boolean relative) {
            this(hpos, vpos, width, height, content, "N/A", "N/A", relative);
        }
        public Box(double hpos, double vpos, double width, double height, String content, String wc, String cc,
                   boolean relative) {
            this.hpos = hpos;
            this.vpos = vpos;
            this.width = width;
            this.height = height;
            this.content = content;
            this.wc = wc;
            this.cc = cc;
            this.relative = relative;
        }

        public void toXML(XMLStreamWriter xml) throws XMLStreamException {
            xml.writeCharacters("    ");
            start(xml, "textblock",
                  "x", rel(hpos),
                  "y", rel(vpos),
                  "width", rel(width),
                  "height", rel(height),
                  "wc", wc,
                  "cc", cc,
                  "rel", Boolean.toString(relative));
            xml.writeCharacters(content);
            endln(xml);
        }

        private String rel(double val) {
            return relative ? Double.toString(val) : Integer.toString((int) val);
        }
    }

    @Override
    public String toString() {
        return "AltoBoxResponse(boxes=" + boxes.size() + ")";
    }
}
