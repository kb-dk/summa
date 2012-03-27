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
import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.summa.facetbrowser.browse.IndexRequest;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseImpl;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.Serializable;
import java.io.StringWriter;
import com.ibm.icu.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * The result of an index-lookup, suitable for later merging and sorting.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexResponse extends ResponseImpl {
    private static final long serialVersionUID = 798434168190L;
    private static Log log = LogFactory.getLog(IndexResponse.class);

    public static final String INDEX_RESPONSE_NAMESPACE =
            "http://statsbiblioteket.dk/summa/2009/IndexResponse";
    public static final String NAME = "IndexResponse";

    private final IndexRequest request;

    // Sorting
    private transient Comparator<Pair<String, Integer>> sorter;

    private ArrayList<Pair<String, Integer>> index;

    public IndexResponse(IndexRequest request) {
        if (log.isDebugEnabled()) {
            log.debug("Creating index response for request " + request);
        }
        this.request = request;
        index = new ArrayList<Pair<String, Integer>>(request.getLength());
    }

    /**
     * Add a term to the index. No running cleanup of the structure is
     * performed, so it is the responsibility of the caller to avoid flooding
     * the IndexResponse with more terms than requested.
     * </p><p>
     * Note that a cleanup will be performed when {@link #toXML()} is called,
     * so the order and exact number of terms added is not significant.
     * @param term the term to add to the index (a pir of String an occurrence
     *        count.
     */
    public void addTerm(Pair<String, Integer> term) {
        if (log.isTraceEnabled()) {
            log.trace(String.format(
                    "Adding term '%s' to field '%s'", term, request.getField()));
        }
        if (term.getKey() == null) {
            log.warn("addTerm was called with null as term. Modifying to the "
                     + "String 'null'");
            term.setKey("null");
        }
        if (term.getValue() == null) {
            log.warn("addTerm was calles with null as count. Modifying to 0");
            term.setValue(0);
        }
        index.add(term);
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Used exclusively for merging.
     * @return the index of terms.
     */
    ArrayList<Pair<String, Integer>> getIndex() {
        return index;
    }

    /**
     * Merge the other SearchResult into this result.
     * @param other the index lookup result that should be merged into this.
     */
    @Override
    public void merge(Response other) {
        log.trace("Index-lookup merge called");
        if (!(other instanceof IndexResponse)) {
            throw new IllegalArgumentException(String.format(
                    "Expected index response of class '%s' but got '%s'",
                    getClass().toString(), other.getClass().toString()));
        }
        super.merge(other);
        IndexResponse indexResponse = (IndexResponse)other;
        outer:
        for (Pair<String, Integer> oPair: indexResponse.getIndex()) {
            for (Pair<String, Integer> tPair: getIndex()) {
                if (oPair.getKey().equals(tPair.getKey())) {
                    tPair.setValue(tPair.getValue() + oPair.getValue());
                    continue outer;
                }
            }
            getIndex().add(oPair);
        }
    }

    private static class SensitiveComparator implements
                               Comparator<Pair<String, Integer>>, Serializable {
        private static final long serialVersionUID = 798341696165L;
        private Collator collator = null;
        public SensitiveComparator(Collator collator) {
            this.collator = collator;
        }
        public SensitiveComparator() { }

        @Override
        public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
            return collator == null ?
                   o1.getKey().compareTo(o2.getKey()) :
                   collator.compare(o1.getKey(), o2.getKey());
        }
    }
    private static class InSensitiveComparator implements
                               Comparator<Pair<String, Integer>>, Serializable {
        private static final long serialVersionUID = 7984368165L;
        private Collator collator = null;
        public InSensitiveComparator(Collator collator) {
            this.collator = collator;
        }
        public InSensitiveComparator() { }

        @Override
        public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
            return collator == null ?
                   o1.getKey().compareToIgnoreCase(o2.getKey()) :
                   collator.compare(o1.getKey().toLowerCase(),
                                    o2.getKey().toLowerCase());
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
            if (request.getLocale() == null) {
                sorter = request.isCaseSensitive()
                         ? new SensitiveComparator()
                         : new InSensitiveComparator();
            } else {
                Collator collator = CollatorFactory.createCollator(
                    request.getLocale());
                sorter = request.isCaseSensitive()
                         ? new SensitiveComparator(collator)
                         : new InSensitiveComparator(collator);
            }
        }
        Collections.sort(index, sorter);
        int start = Math.max(0, getOrigo() + request.getDelta());
        index = new ArrayList<Pair<String, Integer>>(index.subList(
                start, Math.min(index.size(), start + request.getLength())));
    }

    private int getOrigo() {
        int origo = Collections.binarySearch(
            index, new Pair<String, Integer>(request.getTerm(), 0), sorter);
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
    @Override
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
            if (request.getQuery() != null) {
                xmlOut.writeAttribute("query", request.getQuery());
            }
            xmlOut.writeAttribute("field", request.getField());
            xmlOut.writeAttribute("term", request.getTerm());
            xmlOut.writeAttribute("caseSensitive",
                                  Boolean.toString(request.isCaseSensitive()));
            if (request.getLocale() != null) {
                xmlOut.writeAttribute(
                    "sortlocale", request.getLocale().toString());
            }
            xmlOut.writeAttribute(
                "delta", Integer.toString(request.getDelta()));
            xmlOut.writeAttribute(
                "length", Integer.toString(request.getLength()));
            xmlOut.writeAttribute("origo", Integer.toString(getOrigo()));
            xmlOut.writeAttribute(
                "mincount", Integer.toString(request.getMinCount()));
            xmlOut.writeAttribute(
                TIMING, getTiming());
            xmlOut.writeCharacters("\n");

            xmlOut.writeStartElement("index");
            xmlOut.writeCharacters("\n");
            for (Pair<String, Integer> term: index) {
                xmlOut.writeStartElement("term");
                xmlOut.writeAttribute(
                    "count", Integer.toString(term.getValue()));
                xmlOut.writeCharacters(term.getKey());
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
