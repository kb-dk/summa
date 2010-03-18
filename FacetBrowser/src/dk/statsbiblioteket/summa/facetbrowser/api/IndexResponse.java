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

import dk.statsbiblioteket.summa.common.util.CollatorFactory;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

/**
 * The result of an index-lookup, suitable for later merging and sorting.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexResponse implements Response {
    private static Log log = LogFactory.getLog(IndexResponse.class);

    public static final String INDEX_RESPONSE_NAMESPACE =
            "http://statsbiblioteket.dk/summa/2009/IndexResponse";
    public static final String NAME = "IndexResponse";

    // Taken directly from {@link IndexKeys}.
    private String field;
    private String term;
    private boolean caseSensitive;
    private int delta;
    private int length;

    // If the sortLocale is null, Unicode order is used.
    private Locale sortLocale;

    // Sorting
    private transient Comparator<String> sorter;

    private ArrayList<String> index;

    public IndexResponse(String field, String term, boolean caseSensitive,
                         int delta, int length, Locale sortLocale) {
        log.debug("Creating index response " + field + ":" + term);
        this.field = field;
        this.term = term;
        this.caseSensitive = caseSensitive;
        this.delta = delta;
        this.length = length;
        this.sortLocale = sortLocale;
        index = new ArrayList<String>(length);
    }

    /**
     * Add a term to the index. No running cleanup of the structure is
     * performed, so it is the responsibility of the caller to avoid flooding
     * the IndexResponse with more terms than requested.
     * </p><p>
     * Note that a cleanup will be performed when {@link #toXML()} is called,
     * so the order and exact number of terms added is not significant.
     * @param term the term to add to the index.
     */
    public void addTerm(String term) {
        if (log.isTraceEnabled()) {
            log.trace(String.format(
                    "Adding term '%s' to field '%s'", term, field));
        }
        index.add(term);
    }

    public String getName() {
        return NAME;
    }

    /**
     * Used exclusively for merging.
     * @return the index of terms.
     */
    ArrayList<String> getIndex() {
        return index;
    }

    /**
     * Merge the other SearchResult into this result.
     * @param other the index lookup result that should be merged into this.
     */
    public void merge(Response other) {
        log.trace("Index-lookup merge called");
        if (!(other instanceof IndexResponse)) {
            throw new IllegalArgumentException(String.format(
                    "Expected index response of class '%s' but got '%s'",
                    getClass().toString(), other.getClass().toString()));
        }
        IndexResponse indexResponse = (IndexResponse)other;
        index.addAll(indexResponse.getIndex());
    }

    private static class SensitiveComparator implements Comparator<String>,
                                                        Serializable {
        private Collator collator = null;
        public SensitiveComparator(Collator collator) {
            this.collator = collator;
        }
        public SensitiveComparator() { }

        public int compare(String o1, String o2) {
            return collator == null ?
                   o1.compareTo(o2) :
                   collator.compare(o1, o2);
        }
    }
    private static class InSensitiveComparator implements Comparator<String>,
                                                          Serializable {
        private Collator collator = null;
        public InSensitiveComparator(Collator collator) {
            this.collator = collator;
        }
        public InSensitiveComparator() { }

        public int compare(String o1, String o2) {
            return collator == null ?
                   o1.compareToIgnoreCase(o2) :
                   collator.compare(o1.toLowerCase(), o2.toLowerCase());
        }
    }

    /**
     * Ensures that the index is sorted and trimmed, according to the attributes
     * specified in this response. This is to be called before producing any
     * external representation of this response.
     */
    private void clean() {
        log.trace("clean() called");
        if (sorter == null) { // Create sorters
            if (sortLocale == null) {
                sorter = caseSensitive
                         ? new SensitiveComparator()
                         : new InSensitiveComparator();
            } else {
                Collator collator = CollatorFactory.createCollator(sortLocale);
                sorter = caseSensitive
                         ? new SensitiveComparator(collator)
                         : new InSensitiveComparator(collator);
            }
        }
        Collections.sort(index, sorter);
        int start = Math.max(0, getOrigo() + delta);
        index = new ArrayList<String>(index.subList(
                start, Math.min(index.size(), start + length)));
    }

    private int getOrigo() {
        int origo = Collections.binarySearch(index, term, sorter);
        return origo < 0 ? (origo + 1) * -1 : origo;
    }

    /**
     * {@code
    <indexresponse xmlns="http://statsbiblioteket.dk/summa/2009/IndexResponse"
                   field="author" term="hugo" caseSensitive="false"
                   delta="-2" length="5" origo="2">
        <index>
            <term>birger</term>
            <term>carsten</term>
            <term>hugo</term>
            <term>lars</term>
            <term>melanie</term>
        </index>
    </indexresponse>
       }
     * @return the search-result as XML, suitable for web-services et al.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public String toXML() {
        log.trace("toXML() called");
        clean();
        XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter(1000);
        try {
            XMLStreamWriter xmlOut = xmlFactory.createXMLStreamWriter(sw);
            xmlOut.setDefaultNamespace(INDEX_RESPONSE_NAMESPACE);

//            xmlOut.writeStartDocument();
//            xmlOut.writeCharacters("\n");

            xmlOut.writeStartElement("indexresponse");
            xmlOut.writeAttribute("field", field);
            xmlOut.writeAttribute("term", term);
            xmlOut.writeAttribute(
                    "caseSensitive", Boolean.toString(caseSensitive));
            xmlOut.writeAttribute("delta", Integer.toString(delta));
            xmlOut.writeAttribute("length", Integer.toString(length));
            xmlOut.writeAttribute("origo", Integer.toString(getOrigo()));
            xmlOut.writeCharacters("\n");

            xmlOut.writeStartElement("index");
            xmlOut.writeCharacters("\n");
            for (String term: index) {
                xmlOut.writeStartElement("term");
                xmlOut.writeCharacters(term);
                xmlOut.writeEndElement();
                xmlOut.writeCharacters("\n");
            }
            xmlOut.writeEndElement(); // index
            xmlOut.writeCharacters("\n");

            xmlOut.writeEndElement(); // indexresponse
            xmlOut.writeCharacters("\n");

//            xmlOut.writeEndDocument();
//            xmlOut.writeCharacters("\n");
        } catch (XMLStreamException e) {
            throw new RuntimeException(
                    "Unexpected XMLStreamException in toXML()", e);
        }

        log.trace("Returning XML from toXML()");
        return sw.toString();
    }
}
