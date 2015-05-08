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
package dk.statsbiblioteket.summa.search.api.document;

import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseImpl;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLStepper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The result of a (normally Solr) highlight search, suitable for later merging.
 * </p><p>
 * The output is modelled closer after Solr, in preparation of a later switchover.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class HighlightResponse extends ResponseImpl {
    private static final long serialVersionUID = 26845664189L;
    private static Log log = LogFactory.getLog(HighlightResponse.class);

    // Map<id, Map<field, List<content>>>
    private Map<String, Map<String, List<String>>> highlights = new HashMap<>();

    /* Solr highlighting:
  <lst name="highlighting">
  <lst name="doms_newspaperCollection:uuid:368d1c46-7de0-4324-a6b7-8d914cb8a950-segment_6">
    <arr name="fulltext_org">
      <str>Som en <em>hest</em></str>
      <str>»Mountain« slår som en <em>hest</em> sparker. Den kanadiske gruppe med guitaristen Leslie West og</str>
    </arr>
  </lst>
  <lst name="doms_newspaperCollection:uuid:5279f701-2127-42e6-98ee-4077ebad5697-segment_4">
  ...
      */
    public boolean parseSolrFull(XMLStreamReader xml) throws XMLStreamException {
        final AtomicBoolean foundHL = new AtomicBoolean(false);
        XMLStepper.iterateTags(xml, new XMLStepper.Callback() {
            @Override
            public boolean elementStart(XMLStreamReader xml, List<String> tags, String current)
                    throws XMLStreamException {
                if ("lst".equals(current) && "highlighting".equals(XMLStepper.getAttribute(xml, "name", null))) {
                    foundHL.set(true);
                    xml.nextTag();
                    parseSolr(xml);
                    return true;
                }
                return false;
            }
        });
        return foundHL.get();
    }

    /*
  <lst name="doms_newspaperCollection:uuid:368d1c46-7de0-4324-a6b7-8d914cb8a950-segment_6">
    <arr name="fulltext_org">
      <str>Som en <em>hest</em></str>
      <str>»Mountain« slår som en <em>hest</em> sparker. Den kanadiske gruppe med guitaristen Leslie West og</str>
    </arr>
  </lst>
  ...
     */
    public void parseSolr(XMLStreamReader xml) throws XMLStreamException {
        XMLStepper.iterateTags(xml, new XMLStepper.Callback() {
            @Override
            public boolean elementStart(XMLStreamReader xml, List<String> tags, String current)
                    throws XMLStreamException {
                if ("lst".equals(current)) {
                    String id =  XMLStepper.getAttribute(xml, "name", null);
                    if (id == null) {
                        throw new XMLStreamException("Expected the element 'lst' to have an attribute 'name'");
                    }
                    xml.nextTag();
                    parseSolrFields(xml, id);
                    return true;
                }
                return false;
            }
        });
    }

    /*
    <arr name="fulltext_org">
      <str>Som en <em>hest</em></str>
      <str>»Mountain« slår som en <em>hest</em> sparker. Den kanadiske gruppe med guitaristen Leslie West og</str>
    </arr>
     */
    private void parseSolrFields(XMLStreamReader xml, final String id) throws XMLStreamException {
        XMLStepper.iterateTags(xml, new XMLStepper.Callback() {
            @Override
            public boolean elementStart(XMLStreamReader xml, List<String> tags, String current)
                    throws XMLStreamException {
                if ("arr".equals(current)) {
                    String field =  XMLStepper.getAttribute(xml, "name", null);
                    if (field == null) {
                        throw new XMLStreamException(
                                "Expected the element 'arr' to have an attribute 'name' for id='" + id + "'");
                    }
                    xml.nextTag();
                    parseSolrContents(xml, id, field);
                    return true;
                }
                return false;
            }
        });
    }

    /*
      <str>Som en <em>hest</em></str>
      <str>»Mountain« slår som en <em>hest</em> sparker. Den kanadiske gruppe med guitaristen Leslie West og</str>
      ...
    </arr>
     */
    private void parseSolrContents(XMLStreamReader xml, String id, String field) throws XMLStreamException {
        final List<String> contents = new ArrayList<>();
        XMLStepper.iterateTags(xml, new XMLStepper.Callback() {
            @Override
            public boolean elementStart(XMLStreamReader xml, List<String> tags, String current)
                    throws XMLStreamException {
                if ("str".equals(current)) {
                    contents.add(xml.getElementText());
                    return true;
                }
                return false;
            }
        });
        add(id, field, contents);
    }

    // Map<id, Map<field, List<content>>>
    private void add(String id, String field, List<String> contents) {
        Map<String, List<String>> fieldMap = new HashMap<>();
        fieldMap.put(field, contents);
        Map<String, Map<String, List<String>>> idMap = new HashMap<>();
        idMap.put(id, fieldMap);
        merge(idMap);
    }

    @Override
    public synchronized void merge(Response otherO) throws ClassCastException {
        if (!(otherO instanceof HighlightResponse)) {
            log.debug("Attempted merge of non-compatible object with class " + otherO.getClass());
            return;
        }
        HighlightResponse other = (HighlightResponse)otherO;
        merge(other.highlights);
    }

    private void merge(Map<String, Map<String, List<String>>> oHighlights) {
        // Map<id, Map<field, List<content>>>
        for (Map.Entry<String, Map<String, List<String>>> ohlEntry: oHighlights.entrySet()) {
            if (highlights.containsKey(ohlEntry.getKey())) {
                merge(ohlEntry);
            } else {
                highlights.put(ohlEntry.getKey(), deepCopy(ohlEntry.getValue()));
            }
        }
    }

    private void merge(Map.Entry<String, Map<String, List<String>>> ohlEntry) {
        Map<String, List<String>> thisFieldMap = highlights.get(ohlEntry.getKey()); // Contract: Never null
        for (Map.Entry<String, List<String>> oFieldEntry: ohlEntry.getValue().entrySet()) {
            List<String> thisContents = thisFieldMap.get(oFieldEntry.getKey());
            if (thisContents == null) {
                thisContents = new ArrayList<>();
                thisFieldMap.put(oFieldEntry.getKey(), thisContents);
            }
            thisContents.addAll(oFieldEntry.getValue());
        }
    }

    private Map<String, List<String>> deepCopy(Map<String, List<String>> in) {
        Map<String, List<String>> out = new HashMap<>(in.size());
        for (Map.Entry<String, List<String>> entry: in.entrySet()) {
            out.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return out;
    }

    @Override
    public void toXML(XMLStreamWriter xml) throws XMLStreamException {
        startln(xml, "lst", "name", "highlighting"); // highlighting
        // Map<id, Map<field, List<content>>>
        for (Map.Entry<String, Map<String, List<String>>> hlEntry: highlights.entrySet()) {
            startln(xml, "lst", "name", hlEntry.getKey()); // id
            // Map<field, List<content>>
            for (Map.Entry<String, List<String>> fieldEntry: hlEntry.getValue().entrySet()) {
                xml.writeCharacters("  ");
                startln(xml, "arr", "name", fieldEntry.getKey()); // field
                for (String content: fieldEntry.getValue()) {
                    xml.writeCharacters("    ");
                    start(xml, "str"); // entry
                    xml.writeCharacters(content);
                    endln(xml, ""); // entry
                }
                endln(xml, "  "); // field
            }
            endln(xml);// id
        }
        endln(xml); // highlighting
    }

    public HighlightResponse() {
        super("highlight.");
    }

    @Override
    public String getName() {
        return "HighlightResponse";
    }

    /**
     * @return extracted highlights as Map<docID, Map<field, List<content>>>.
     */
    public Map<String, Map<String, List<String>>> getHighlights() {
        return highlights;
    }

    public int size() {
        return highlights.size();
    }

    public boolean isEmpty() {
        return highlights.isEmpty();
    }

    @Override
    public String toString() {
        return "HighlightResponse(documents=" + size() + ")";
    }
}
