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
package dk.statsbiblioteket.summa.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.text.Collator;
import java.io.StringWriter;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.util.ParseUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The result of a search, suitable for later merging and sorting.
 */
// TODO: Handle sort-aggregator?
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SearchResult {
    private static Log log = LogFactory.getLog(SearchResult.class);

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

    public SearchResult(String filter, String query,
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
    public class Record {
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
    }

    /**
     * Containt content from a requested Field for a Record.
     */
    public class Field {
        private String name;
        private String content;

        public Field(String name, String content) {
            this.name = name;
            this.content = content;
        }

        public void toXML(StringWriter sw) {
            if (content == null || "".equals(content)) {
                return;
            }
            sw.append("    <field name=\"").append(name).append("\">");
            sw.append(ParseUtil.encode(content)).append("</field>\n");
        }
    }

    private void appendIfDefined(StringWriter sw, String name, String value) {
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
     * @param collator determines the order of Records, based on their
     *                 sortValue. If no collator is given or sortKey equals
     *                 {@link BasicSearcher#SORT_ON_SCORE} or sortKey equals
     *                 null, the values of {@link Record#score} are compared in
     *                 natural order.<br />
     *                 Note that the collator should ignore the property
     *                 {@link #reverseSort}, as the merge method will take care
     *                 of this if needed.
     * @return null if no conflicts, else a human-readable descriptions of the
     *              encountered conflicts.
     */
    public String merge(SearchResult other, final Collator collator) {
        log.trace("merge called");
        // TODO: Check for differences in basic attributes and warn if needed
        records.addAll(other.getRecords());
        if (collator == null || sortKey == null
            || BasicSearcher.SORT_ON_SCORE.equals(sortKey)) {
            Collections.sort(records, scoreComparator);
        } else {
            Comparator<Record> collatorComparator = new Comparator<Record>() {
                public int compare(Record o1, Record o2) {
//                    if (o1.get)
  //                  collator.compare(o1.get)
                    throw new UnsupportedOperationException("Not finished!");
//                    float diff = o1.getScore() - o2.getScore();
//                    return diff < 0 ? -1 : diff > 0 ? 1 : 0;
                }
            };

        }
        return null;
    }
    private Comparator<Record> scoreComparator = new Comparator<Record>() {
        public int compare(Record o1, Record o2) {
            float diff = o1.getScore() - o2.getScore();
            return diff < 0 ? -1 : diff > 0 ? 1 : 0;
        }
    };

    private boolean equals(String s1, String s2) {
        return s1 == null && s2 == null || s1 != null && s1.equals(s2);
    }

    /**
     * {@code
       <?xml version="1.0" encoding="UTF-8"?>
       <searchresult filter="..." query="..."
                     startIndex="..." maxRecords="..."
                     sortKey="..." reverseSort="..."
                     fields="..." searchTime="..." hitCount="...">
         <record score="..." sortValue="...">
           <field name="recordID">...</field>
           <field name="shortformat">...</field>
         </record>
         ...
       </searchresult>
       }
     * sortValue is the value that the sort was performed on. If the XML-result
     * from several searchers are to be merged, merge-ordering should be
     * dictated by this value.<br />
     * score is the score-value returned by the index implementation.<br />
     * searchTime is the number of milliseconds it took to perform the search.
     * @return the search-result as XML, suitable for web-services et al.
     */
    public String toXML() {
        log.trace("toXML() called");
        StringWriter sw = new StringWriter(2000);
        sw.append(ParseUtil.XML_HEADER).append("\n");
        sw.append("<searchresult");
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
        sw.append(" hitcount=\"");
        sw.append(Long.toString(hitCount)).append("\">\n");
        for (Record record: records) {
            record.toXML(sw);
        }
        sw.append("</searchresult>\n");
        log.trace("Returning XML from toXML()");
        return sw.toString();
    }

    /* Getters and setters */

    public List<Record> getRecords() {
        return records;
    }
}
