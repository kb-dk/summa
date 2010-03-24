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
import dk.statsbiblioteket.util.qa.QAInfo;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * The response from {@link dk.statsbiblioteket.summa.support.suggest.SuggestSearchNode}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SuggestResponse implements Response {
    public static final String NAMESPACE =
            "http://statsbiblioteket.dk/summa/2009/QueryResponse";
    public static final String VERSION = "1.0";

    public static final String QUERY_RESPONSE = "QueryResponse";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String VERSION_TAG = "version";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String QUERY_TAG = "query";
    public static final String MAXRESULTS_TAG = "maxResults";

    public static final String SUGGESTIONS = "suggestions";

    public static final String SUGGESTION = "suggestion";
    public static final String HITS_TAG = "hits";
    public static final String QUERYCOUNT_TAG = "queryCount";
    public static final String NAME = "SuggestResponse";

    private static XMLOutputFactory xmlOutputFactory;
    static {
        xmlOutputFactory = XMLOutputFactory.newInstance();
    }

    private String query;
    private int maxResults;
    private ArrayList<SuggestTripel> suggestions;

    public SuggestResponse(String query, int maxResults) {
        this.query = query;
        this.maxResults = maxResults;
        suggestions = new ArrayList<SuggestTripel>(maxResults);
    }

    public void addSuggestion(String query, int hits, int queryCount) {
        suggestions.add(new SuggestTripel(query, hits, queryCount));
    }

    public ArrayList<SuggestTripel> getSuggestions() {
        return suggestions;
    }

    /**
     * As suggest is normally distributed by replication, merging is normally
     * not used. If this merger is to be used with sharded suggest-noded, it
     * should be tested vigorously.
     * @param otherResponse another SuggestResponse to merge into this.
     * @throws ClassCastException if otherResponse was not a SuggestResponse.
     */
    public void merge(Response otherResponse) throws ClassCastException {
        SuggestResponse other = (SuggestResponse)otherResponse;
        otherLoop:
        for (SuggestTripel otherTripel: other.getSuggestions()) {
            for (SuggestTripel thisTripel: getSuggestions()) {
                if (thisTripel.getQuery().equals(otherTripel.getQuery())) {
                    thisTripel.setHits(
                            thisTripel.getHits() + otherTripel.getHits());
                    thisTripel.setQueryCount(Math.max(
                            thisTripel.getQueryCount(),
                            otherTripel.getQueryCount()));
                    continue otherLoop;
                }
            }
            // No match, so we add a copy
            addSuggestion(otherTripel.getQuery(), otherTripel.getHits(),
                          otherTripel.getQueryCount());
        }
        sortAndReduce();
    }

    /**
     * Sort the suggestions descending by queryCount and reduce the number to
     * maxResults.
     */
    public void sortAndReduce() {
        Collections.sort(suggestions, new Comparator<SuggestTripel>() {
            public int compare(SuggestTripel o1, SuggestTripel o2) {
                return Integer.valueOf(
                        o2.getQueryCount()).compareTo(o1.getQueryCount());
            }
        });
        if (suggestions.size() > maxResults) {
            ArrayList<SuggestTripel> newSuggestions =
                    new ArrayList<SuggestTripel>(maxResults);
            newSuggestions.addAll(suggestions.subList(0, maxResults));
            suggestions = newSuggestions;
        }
    }

    public String toXML() {
        sortAndReduce();
        StringWriter sw = new StringWriter(2000);
        XMLStreamWriter writer;
        try {
            writer = xmlOutputFactory.createXMLStreamWriter(sw);
        } catch (XMLStreamException e) {
            throw new RuntimeException(
                    "Unable to create XMLStreamWriter from factory", e);
        }
        try {
            writer.setDefaultNamespace(NAMESPACE);
            //writer.writeStartDocument();
            writer.writeStartElement(QUERY_RESPONSE);
            writer.writeDefaultNamespace(NAMESPACE);
            writer.writeAttribute(VERSION_TAG, VERSION);
            writer.writeAttribute(QUERY_TAG, query);
            writer.writeAttribute(MAXRESULTS_TAG, Integer.toString(maxResults));
            writer.writeCharacters("\n");

            writer.writeCharacters("    ");
            writer.writeStartElement(SUGGESTIONS);
            writer.writeCharacters("\n");
            for (SuggestTripel suggestion: suggestions) {
                suggestion.toXML(writer);
            }
            writer.writeCharacters("    ");
            writer.writeEndDocument();

            writer.writeEndDocument();
            writer.flush(); // Just to make sure
        } catch (XMLStreamException e) {
            throw new RuntimeException(
                    "Got XMLStreamException while constructing XML from "
                    + "SuggestionResponse", e);
        }
        return sw.toString();
    }

    public String getName() {
        return NAME;
    }

    private static class SuggestTripel implements Serializable {
        private String query;
        private int hits;
        private int queryCount;

        public SuggestTripel(String query, int hits, int queryCount) {
            this.query = query;
            this.hits = hits;
            this.queryCount = queryCount;
        }

        public void toXML(XMLStreamWriter writer) throws XMLStreamException {
            writer.writeCharacters("        ");
            writer.writeStartElement(SUGGESTION);
            writer.writeAttribute(HITS_TAG, Integer.toString(hits));
            writer.writeAttribute(QUERYCOUNT_TAG, Integer.toString(queryCount));
            writer.writeCharacters(query);
            writer.writeEndElement();
            writer.writeCharacters("\n");
        }

        public String getQuery() {
            return query;
        }

        public int getHits() {
            return hits;
        }

        public void setHits(int hits) {
            this.hits = hits;
        }

        public int getQueryCount() {
            return queryCount;
        }

        public void setQueryCount(int queryCount) {
            this.queryCount = queryCount;
        }
    }
}

