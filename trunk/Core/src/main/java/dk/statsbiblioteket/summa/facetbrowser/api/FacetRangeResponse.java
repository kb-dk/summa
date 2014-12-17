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
package dk.statsbiblioteket.summa.facetbrowser.api;


import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Represents buckets of data, modelled closely after https://wiki.apache.org/solr/SimpleFacetParameters#Facet_by_Range.
 */
public class FacetRangeResponse extends ResponseImpl {
    private static final transient Log log = LogFactory.getLog(FacetRange.class);
    private final transient XMLOutputFactory xmlOutFactory = XMLOutputFactory.newInstance();
    private static final long serialVersionUID = 7271119850L;

    protected ArrayList<FacetRange> ranges;

    public FacetRangeResponse() {
        this.ranges = new ArrayList<>();
    }

    public FacetRangeResponse(String prefix) {
        super(prefix);
        this.ranges = new ArrayList<>();
    }

    @Override
    public String getName() {
        return "FacetRangeResponse";
    }

    @Override
    public void merge(Response other) throws ClassCastException {
        if (!(other instanceof FacetRangeResponse)) {
            return;
        }
        FacetRangeResponse otherRange = (FacetRangeResponse)other;
        super.merge(otherRange);
        outer:
        for (FacetRange or: otherRange.ranges) {
            for (FacetRange tr: ranges) {
                if (tr.getField().equals(or.getField())) {
                    tr.merge(or);
                    continue outer;
                }
            }
            ranges.add(or);
        }
    }

    @Override
    public String toXML() {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            XMLStreamWriter out = xmlOutFactory.createXMLStreamWriter(os);
            String indent = "  ";

            out.writeCharacters(indent);
            out.writeStartElement("facet_ranges");
            out.writeAttribute("timing", getTiming());
            out.writeCharacters("\n");
            for (FacetRange range: ranges) {
                range.toXML(out, indent + "  ");
            }
            out.writeCharacters(indent);
            out.writeEndElement();
            out.flush();
            return os.toString("utf-8");
        } catch (XMLStreamException e) {
            throw new RuntimeException("Unable to create an XMLStreamWriter", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("utf-8 not supported", e);
        }
    }

    public class FacetRange extends AbstractList<Pair<String, Long>> implements Serializable {
        private static final long serialVersionUID = 127394189850L;
        private String field;
        private String start;
        private String end;
        private String gap;
        private ArrayList<Pair<String, Long>> counts;

        public FacetRange(String field, String start, String end, String gap) {
            this.field = field;
            this.start = start;
            this.end = end;
            this.gap = gap;
            counts = new ArrayList<>();
        }

        public void merge(FacetRange other) {
            if (!getField().equals(other.getField())) {
                throw new IllegalArgumentException(String.format(
                        "This field is '%s' but the other field is '%s'", getField(), other.getField()));
            }
            if (!(eq(getStart(), other.getStart()) && eq(getEnd(), other.getEnd()) && eq(getGap(), other.getGap()))) {
                log.warn(String.format(
                        "Accepting inequality in arguments start=%s %s, end=%s %s, gap=%s %s",
                        getStart(), other.getStart(), getEnd(), other.getEnd(), getGap(), other.getGap()));
            }
            other:
            for (Pair<String, Long> op: other) {
                for (Pair<String, Long> tp: this) {
                    if (tp.getKey().equals(op.getKey())) {
                        tp.setValue(tp.getValue() + op.getValue());
                        continue other;
                    }
                }
                add(op); // Bucket not present -> add it
            }
        }

        public void toXML(XMLStreamWriter xml, String indent) throws XMLStreamException {
            Collections.sort(this, inferComparator());

            xml.writeCharacters(indent);
            xml.writeStartElement("facet_range");
            xml.writeAttribute("field", field);
            xml.writeAttribute("start", start);
            xml.writeAttribute("end", end);
            xml.writeAttribute("gap", gap);
            xml.writeCharacters("\n");
            for (Pair<String, Long> count: this) {
                xml.writeCharacters(indent + "  ");
                xml.writeStartElement("int");
                xml.writeAttribute("name", count.getKey());
                xml.writeCharacters(Long.toString(count.getValue()));
                xml.writeEndElement();
                xml.writeCharacters("\n");
            }
            xml.writeCharacters(indent);
            xml.writeEndDocument();
            xml.writeCharacters("\n");
        }

        // Tries to guess whether the values are DateTime, longs or doubles
        private Comparator<Pair<String, Long>> inferComparator() {
            for (Pair<String, Long> p: this) {
                if (p.getKey().contains("T")) { // 2011-10-01T00:00:00Z
                    return new Comparator<Pair<String, Long>>() {
                        @Override
                        public int compare(Pair<String, Long> o1, Pair<String, Long> o2) {
                            return o1.getKey().compareTo(o2.getKey()); // Natural alphanumeric
                        }
                    };
                }
                if (p.getKey().contains(".")) { // 123.455
                    return new Comparator<Pair<String, Long>>() {
                        @Override
                        public int compare(Pair<String, Long> o1, Pair<String, Long> o2) {
                            return Double.valueOf(o1.getKey()).compareTo(Double.valueOf(o2.getKey())); // floating point
                        }
                    };
                }
            }
            return new Comparator<Pair<String, Long>>() {
                @Override
                public int compare(Pair<String, Long> o1, Pair<String, Long> o2) {
                    return Long.valueOf(o1.getKey()).compareTo(Long.valueOf(o2.getKey())); // int or long
                }
            };
        }

        private boolean eq(String s1, String s2) {
            return s1 == null ? s2 == null : s1.equals(s2);
        }
        /* Plain getters below */

        public String getField() {
            return field;
        }

        public String getStart() {
            return start;
        }

        public String getEnd() {
            return end;
        }

        public String getGap() {
            return gap;
        }

        /* Delegations to count below */

        @Override
        public int size() {
            return counts.size();
        }

        @Override
        public Pair<String, Long> get(int index) {
            return counts.get(index);
        }

        @Override
        public Pair<String, Long> set(int index, Pair<String, Long> element) {
            return counts.set(index, element);
        }

        @Override
        public boolean add(Pair<String, Long> stringLongPair) {
            return counts.add(stringLongPair);
        }

        @Override
        public void add(int index, Pair<String, Long> element) {
            counts.add(index, element);
        }

        @Override
        public Pair<String, Long> remove(int index) {
            return counts.remove(index);
        }

        @Override
        public void clear() {
            counts.clear();
        }
    }

    /*

     <lst name="facet_ranges">
    <lst name="crawl_date">
      <lst name="counts">
        <int name="2011-09-01T00:00:00Z">194</int>
        <int name="2011-10-01T00:00:00Z">572</int>
        <int name="2011-11-01T00:00:00Z">534</int>
        <int name="2011-12-01T00:00:00Z">122</int>
      </lst>
      <str name="gap">+1MONTH</str>
      <date name="start">2011-01-01T00:00:00Z</date>
      <date name="end">2013-01-01T00:00:00Z</date>
    </lst>
  </lst>

     */
}
