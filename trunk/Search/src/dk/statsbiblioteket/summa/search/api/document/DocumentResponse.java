/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.search.api.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.text.Collator;
import java.io.StringWriter;
import java.io.Serializable;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.util.ParseUtil;
import dk.statsbiblioteket.summa.search.api.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The result of a search, suitable for later merging and sorting.
 */
// TODO: Handle sort-aggregator?
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DocumentResponse implements Response, DocumentKeys {
    private static Log log = LogFactory.getLog(DocumentResponse.class);

    /**
     * If true, records with missing sort-field will be put either at the
     * beginning of the results or at the end, no matter the collator and
     * the sort-direction.<br />
     * If false, the order is up to the collator.
     * @see {@link #NON_DEFINED_FIELDS_ARE_SORTED_LAST}.
     * @see {@link #merge(Response)}.
     */
    private static final boolean NON_DEFINED_FIELDS_ARE_SPECIAL_SORTED = true;
    /**
     * If true, sorting on field X will put all records without field X after
     * the records with field X, no matter if the search is reversed or not.
     * If false, the records without the sort-field will be put first.
     * @see {@link #NON_DEFINED_FIELDS_ARE_SPECIAL_SORTED}.
     * @see {@link #merge(Response)}.
     */
    private static final boolean NON_DEFINED_FIELDS_ARE_SORTED_LAST = true;

    private String filter;
    private String query;
    private long startIndex;
    private long maxRecords;
    private String sortKey;
    private boolean reverseSort;
    private String[] resultFields;
    private long searchTime;
    private long hitCount;

    private List<Record> records;

    public DocumentResponse(String filter, String query,
                        long startIndex, long maxRecords,
                        String sortKey, boolean reverseSort,
                        String[] resultFields, long searchTime,
                        long hitCount) {
        log.debug("Creating search result for query '" + query + "'");
        this.filter = filter;
        this.query = query;
        this.startIndex = startIndex;
        this.maxRecords = maxRecords;
        this.sortKey = sortKey;
        this.reverseSort = reverseSort;
        this.resultFields = resultFields;
        this.searchTime = searchTime;
        this.hitCount = hitCount;
        records = new ArrayList<Record>(50);
    }

    /**
     * Contains a representation of each hit from a search.
     */
    public static class Record implements Serializable {
        private float score;
        private String sortValue;
        private String id;
        private String source;
        private List<Field> fields = new ArrayList<Field>(50);

        /**
         * @param id        a source-specific id for the Record.
         *                  For Lucene searchers this would be an integer.
         * @param source    a designation for the searcher that provided the
         *                  Record. This is currently only used for debugging
         *                  and thus there are no formal requirements for the
         *                  structure of the value.
         * @param score     a ranking-score for the Record. Higher scores means
         *                  more fitting to the query.
         * @param sortValue the value used for sorting. It is legal to provide
         *                  null here, if score is to be used for sorting.
         */
        public Record(String id, String source, float score, String sortValue) {
            this.id = id;
            this.source = source;
            this.score = score;
            this.sortValue = sortValue;
        }

        public void addField(Field field) {
            fields.add(field);
        }

        public void toXML(StringWriter sw) {
            sw.append("  <record score=\"").append(Float.toString(score));
            sw.append("\"");
            appendIfDefined(sw, "sortValue", sortValue);
            appendIfDefined(sw, "id", id);
            appendIfDefined(sw, "source", source);
            sw.append(">\n");
            for (Field field: fields) {
                field.toXML(sw);
            }
            sw.append("  </record>\n");
        }

        public float getScore() {
            return score;
        }

        public String getSortValue() {
            return sortValue;
        }
    }

    /**
     * Containt content from a requested Field for a Record.
     */
    public static class Field implements Serializable {
        private String name;
        private String content;
        private boolean escapeContent;

        public Field(String name, String content, boolean escapeContent) {
            this.name = name;
            this.content = content;
            this.escapeContent = escapeContent;
        }

        public void toXML(StringWriter sw) {
            if (content == null || "".equals(content)) {
                return;
            }
            sw.append("    <field name=\"").append(name).append("\">");
            if (escapeContent) {
                sw.append(ParseUtil.encode(content)).append("</field>\n");
            } else {
                sw.append(content).append("</field>\n");
            }
        }
    }

    private static void appendIfDefined(StringWriter sw,
                                        String name, String value) {
        if (value == null) {
            return;
        }
        sw.append(" ").append(name).append("=\"");
        sw.append(ParseUtil.encode(value)).append("\"");
    }

    /**
     * Add a Record to the SearchResult. The order of Records is significant.
     * @param record a record that should belong to the search result.
     */
    public void addRecord(Record record) {
        if (record == null) {
            throw new IllegalArgumentException(
                    "Expected a Record, got null");
        }
        records.add(record);
    }

    public String getName() {
        return "DocumentResponse";
    }

    /**
     * Merge the other SearchResult into this result. Merging ensures that the
     * order of the merged Records is in accordance with the provided collator
     * on the property {@link Record#sortValue} and that the Record-count after
     * merging is at most {@link #maxRecords}.
     * </p><p>
     * In case of differences in the overall search result structures, such as
     * sortKey having different values, this result wins and a human-readable
     * warning is returned.
     * @param other the search result that should be merged into this.
     */
    public void merge(Response other) {
        log.trace("merge called");
        if (!(other instanceof DocumentResponse)) {
            throw new IllegalArgumentException(String.format(
                    "Expected response of class '%s' but got '%s'",
                    getClass().toString(), other.getClass().toString()));
        }
        DocumentResponse docResponse = (DocumentResponse)other;
        // TODO: Check for differences in basic attributes and warn if needed
        Collator collator = null;
/*     * @param collator determines the order of Records, based on their
     *                 sortValue. If no collator is given or sortKey equals
     *                 {@link #SORT_ON_SCORE} or sortKey equals
     *                 null, the values of {@link Record#score} are compared in
     *                 natural order.<br />
     *                 Note that the collator should ignore the property
     *                 {@link #reverseSort}, as the merge method will take care
     *                 of this if needed.
*/

        records.addAll(docResponse.getRecords());
        if (collator == null || sortKey == null
            || SORT_ON_SCORE.equals(sortKey)) {
            Collections.sort(records, scoreComparator);
        } else {
            Comparator<Record> collatorComparator = new Comparator<Record>() {
                public int compare(Record o1, Record o2) {
                    String s1 =
                            o1.getSortValue() == null ? "" : o1.getSortValue();
                    String s2 =
                            o2.getSortValue() == null ? "" : o2.getSortValue();
                    if (NON_DEFINED_FIELDS_ARE_SPECIAL_SORTED) {
                        // Handle empty cases
                        if ("".equals(s1)) {
                            return "".equals(s2) ?
                                   scoreComparator.compare(o1, o2) :
                                   NON_DEFINED_FIELDS_ARE_SORTED_LAST ? -1 : 1;
                        } else if ("".equals(s2)) {
                            return NON_DEFINED_FIELDS_ARE_SORTED_LAST ? 1 : -1;
                        }
                    }
                    // TODO: Port sorting from Stable
                    throw new IllegalStateException("Collator support not "
                                                    + "finished");
//                    return collator.compare(s1, s2);
                }
            };
            Collections.sort(records, collatorComparator);
        }
    }
    private Comparator<Record> scoreComparator = new ScoreComparator();

    private static class ScoreComparator implements Comparator<Record>,
                                                   Serializable {
        public int compare(Record o1, Record o2) {
            float diff = o1.getScore() - o2.getScore();
            return diff < 0 ? -1 : diff > 0 ? 1 : 0;
        }
    }

    /**
     * {@code
       <?xml version="1.0" encoding="UTF-8"?>
       <documentresult filter="..." query="..."
                     startIndex="..." maxRecords="..."
                     sortKey="..." reverseSort="..."
                     fields="..." searchTime="..." hitCount="...">
         <record score="..." sortValue="...">
           <field name="recordID">...</field>
           <field name="shortformat">...</field>
         </record>
         ...
       </documentresult>
       }
     * The content in the XML is entity-escaped.<br />
     * sortValue is the value that the sort was performed on. If the XML-result
     * from several searchers are to be merged, merge-ordering should be
     * dictated by this value.<br />
     * score is the score-value returned by the index implementation.<br />
     * searchTime is the number of milliseconds it took to perform the search.
     * @return the search-result as XML, suitable for web-services et al.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public String toXML() {
        log.trace("toXML() called");
        StringWriter sw = new StringWriter(2000);
        sw.append("<documentresult");
        appendIfDefined(sw, "filter", filter);
        appendIfDefined(sw, "query", query);
        sw.append(" startIndex=\"");
        sw.append(Long.toString(startIndex)).append("\"");
        sw.append(" maxRecords=\"");
        sw.append(Long.toString(maxRecords)).append("\"");
        appendIfDefined(sw, "sortKey", sortKey);
        sw.append(" reverseSort=\"");
        sw.append(Boolean.toString(reverseSort)).append("\"");
        if (resultFields != null) {
            sw.append(" fields=\"");
            for (int i = 0 ; i < resultFields.length ; i++) {
                sw.append(ParseUtil.encode(resultFields[i]));
                if (i < resultFields.length-1) {
                    sw.append(", ");
                }
            }
            sw.append("\"");
        }
        sw.append(" searchTime=\"");
        sw.append(Long.toString(searchTime)).append("\"");
        sw.append(" hitCount=\"");
        sw.append(Long.toString(hitCount)).append("\">\n");
        for (Record record: records) {
            record.toXML(sw);
        }
        sw.append("</documentresult>\n");
        log.trace("Returning XML from toXML()");
        return sw.toString();
    }

    /* Getters and setters */

    private List<Record> getRecords() {
        return records;
    }
    // Because building the DocumentResponse takes time
    public void setSearchTime(long searchTime) {
        this.searchTime = searchTime;
    }

    /**
     * @return the amount of added Records.
     */
    public int size() {
        return records.size();
    }
}



