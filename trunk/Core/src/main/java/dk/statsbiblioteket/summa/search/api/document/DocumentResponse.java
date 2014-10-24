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

import com.ibm.icu.text.Collator;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseImpl;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.*;

/**
 * The result of a search, suitable for later merging and sorting.
 * </p><p>
 * All insertion and merging operations treats documents as grouped. The property {@link #grouped} whether XML output
 * is grouped or not. If a merge occurs, the grouped-property is set to {@code this.grouped() | other.grouped()}.
 */
// TODO: Handle sort-aggregator?
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DocumentResponse extends ResponseImpl implements DocumentKeys {
    private static final long serialVersionUID = 268189L;
    private static Log log = LogFactory.getLog(DocumentResponse.class);

    public static final String NAME = "DocumentResponse";

    /**
     * If true, records with missing sort-field will be put either at the
     * beginning of the results or at the end, no matter the collator and
     * the sort-direction.<br />
     * If false, the order is up to the collator.
     *
     * @see #NON_DEFINED_FIELDS_ARE_SORTED_LAST
     * @see #merge(Response)
     */
    private static final boolean NON_DEFINED_FIELDS_ARE_SPECIAL_SORTED = true;
    /**
     * If true, sorting on field X will put all records without field X after
     * the records with field X, no matter if the search is reversed or not.
     * If false, the records without the sort-field will be put first.
     *
     * @see #NON_DEFINED_FIELDS_ARE_SPECIAL_SORTED
     * @see #merge(Response)
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
    private boolean grouped;

    // Subset of https://cwiki.apache.org/confluence/display/solr/Result+Grouping
    private String groupField = null; // Must be defined if group == true
    private int groupRows = DocumentKeys.DEFAULT_ROWS;
    private int groupLimit = DocumentKeys.DEFAULT_GROUP_LIMIT;

    private List<Group> groups = new ArrayList<>();

//    private final Collator collator;

    // Born-grouped response
    public DocumentResponse(String filter, String query, long startIndex, long maxRecords, String sortKey,
                            boolean reverseSort, String[] resultFields, long searchTime, long hitCount,
                            String groupField, int groupRows, int groupLimit) {
        this(filter, query, startIndex, maxRecords, sortKey, reverseSort, resultFields, searchTime, hitCount);
        this.grouped = true;
        this.groupField = groupField;
        this.groupRows = groupRows;
        this.groupLimit = groupLimit;
        groups = new ArrayList<>(groupRows*5);
    }

    public DocumentResponse(String filter, String query, long startIndex, long maxRecords, String sortKey,
                            boolean reverseSort, String[] resultFields, long searchTime, long hitCount) {
        log.trace("Creating search result for query '" + query + "'");
        this.filter = filter;
        this.query = query;
        this.startIndex = startIndex;
        this.maxRecords = maxRecords;
        this.sortKey = sortKey;
        this.reverseSort = reverseSort;
        this.resultFields = resultFields;
        this.searchTime = searchTime;
        this.hitCount = hitCount;
        this.grouped = false;
        // TODO: Port proper collator creation from stable
//        collator = Collator.getInstance(new Locale("da"));
    }

    /**
     * A mirror of Solr's group representation
     * https://cwiki.apache.org/confluence/display/solr/Result+Grouping
     */
    public static class Group extends AbstractList<Record> implements Serializable {
        private static final long serialVersionUID = 48759222L;
        private String groupValue;
        private long numFound;
        private List<Record> docs = new ArrayList<>();

        public Group(String groupValue, long numFound) {
            this.groupValue = groupValue;
            this.numFound = numFound;
        }

        // Creates a single-member Group from the given Record
        public Group(Record record, String groupField) {
            this.numFound = 1;
            docs.add(record);
            for (Field field: record) {
                if (groupField != null && groupField.equals(field.getName())) {
                    this.groupValue = field.getContent();
                    return;
                }
            }
            log.debug("Unable to locate field '" + groupField + "' for groupValue in " + record + ". Using empty");
            groupValue = "";
        }

        /**
         * Constructor that performs a deep copy of the provided group.
         * @param otherGroup the group to deep-copy for the new Group.
         */
        public Group(Group otherGroup) {
            groupValue = otherGroup.groupValue;
            numFound = otherGroup.numFound;
            for (Record record: otherGroup.docs) {
                docs.add(new Record(record));
            }
        }

        public void addRecords(List<Record> records) {
            docs.addAll(records);
        }

        public void sort() {
            Collections.sort(docs, new Comparator<Record>() {
                @SuppressWarnings("FloatingPointEquality")
                @Override
                public int compare(Record o1, Record o2) {
                    return o1.score == o2.score ?
                            o1.id == null ? 0 : o1.id.compareTo(o2.id) : o1.score < o2.score ? -1 : 1;
                }
            });
        }

        public void reduce(int maxRecords) {
            sort();
            if (docs.size() > maxRecords) {
                docs = docs.subList(0, maxRecords);
            }
        }

        public void merge(Group other) {
            if (!groupValue.equals(other.groupValue)) {
                throw new IllegalArgumentException(
                        "Only groups with the same group value can be merged. " +
                        "Had '" + groupValue + "', encountered '" + groupValue + "'");
            }
            numFound += other.numFound;
            addRecords(other.docs);
        }

        public void toXML(StringWriter sw, String indent, boolean grouped) {
            sw.append(indent);
            if (grouped) {
                sw.append("<group groupValue=\"").append(groupValue);
                sw.append("\" numFound=\"").append(Long.toString(numFound));
                sw.append("\" score=\"").append(docs.isEmpty() ? "" : Float.toString(docs.get(0).getScore()));
                sw.append("\">\n");
                for (Record record: docs) {
                    record.toXML(sw, indent + "  ");
                }
                sw.append(indent);
                sw.append("</group>\n");
            } else {
                for (Record record: docs) {
                    record.toXML(sw, indent);
                }
            }
        }

        public void setGroupField(String groupField) {
            String gv = null;
            for (Record record: docs) {
                String cgv = record.getFieldValue(groupField, null);
                if (cgv == null) {
                    log.trace("Unable to locate field '" + groupField + "' in " + record);
                } else if (gv == null) {
                    gv = cgv;

                } else if(!gv.equals(cgv)) {
                    log.warn(String.format("setGroupField(%s): Different groupValues encountered: '%s' and '%s'",
                                           groupField, gv, cgv));
                }
            }
            if (gv == null) {
                log.debug("Unable to locate any values for groupField '" + groupField + "'");
            }
        }

        @Override
        public Record get(int index) {
            return docs.get(index);
        }

        @Override
        public int size() {
            return docs.size();
        }

        @Override
        public boolean add(Record record) {
            return docs.add(record);
        }

        @Override
        public Record set(int index, Record element) {
            return docs.set(index, element);
        }

        @Override
        public Record remove(int index) {
            return docs.remove(index);
        }
    }

    /**
     * Contains a representation of each hit from a search.
     */
    public static class Record extends AbstractList<Field> implements Serializable {
        private static final long serialVersionUID = 48785613L;
        private float score;

        private final float originalScore;
        private String sortValue;
        private String id;
        private String source;
        private List<Field> fields = new ArrayList<>(50);

        /**
         * @param id        A source-specific id for the Record.
         *                  For Lucene searchers this would be an integer.
         * @param source    A designation for the searcher that provided the
         *                  Record. This is currently only used for debugging
         *                  and thus there are no formal requirements for the
         *                  structure of the value.
         * @param score     A ranking-score for the Record. Higher scores means
         *                  more fitting to the query.
         * @param sortValue The value used for sorting. It is legal to provide
         *                  null here, if score is to be used for sorting.
         */
        public Record(String id, String source, float score, String sortValue) {
            this.id = id;
            this.source = source;
            this.score = score;
            this.originalScore = score;
            this.sortValue = sortValue;
        }

        /**
         * Deep copy constructor.
         * @param other all content of this will be deep copied.
         */
        public Record(Record other) {
            score = other.score;
            originalScore = other.originalScore;
            sortValue = other.sortValue;
            id = other.id;
            source = other.source;
            for (Field field: other.fields) {
                fields.add(new Field(field));
            }
        }

        @Override
        public boolean add(Field field) {
            return fields.add(field);
        }

        @Override
        public Field get(int index) {
            return fields.get(index);
        }

        @Override
        public boolean remove(Object o) {
            return fields.remove(o);
        }

        @Override
        public int size() {
            return fields.size();
        }

        @Override
        public Field set(int index, Field element) {
            return fields.set(index, element);
        }

        @Override
        public void add(int index, Field element) {
            fields.add(index, element);
        }

        @Override
        public Field remove(int index) {
            return fields.remove(index);
        }

        public void toXML(StringWriter sw, String indent) {
            sw.append(indent);
            sw.append("<record score=\"").append(Float.toString(score));
            sw.append("\"");
            // Only not equal if the score has been changed
            //noinspection FloatingPointEquality
            if (score != originalScore) {
                sw.append(" unadjustedscore").append("=\"");
                sw.append(Float.toString(originalScore)).append("\"");
            }
            appendIfDefined(sw, "sortValue", sortValue);
            appendIfDefined(sw, "id", id);
            appendIfDefined(sw, "source", source);
            sw.append(">\n");
            for (Field field : fields) {
                field.toXML(sw, indent + "  ");
            }
            sw.append(indent);
            sw.append("</record>\n");
        }

        public String getId() {
            return id;
        }

        public String getSource() {
            return source;
        }

        public float getScore() {
            return score;
        }

        public void setScore(float score) {
            this.score = score;
        }

        public String getSortValue() {
            return sortValue;
        }

        public void setSortValue(String sortValue) {
            this.sortValue = sortValue;
        }

        /**
         * @deprecated use the Record-class directly instead.
         * @return the fields in this Record.
         */
        @Deprecated
        public List<Field> getFields() {
            return fields;
        }

        public String getFieldValue(String fieldName, String defaultValue) {
            for (Field field: fields) {
                if (fieldName.equals(field.getName())) {
                    return field.getContent();
                }
            }
            return defaultValue;
        }

        @Override
        public String toString() {
            return "Record(id='" + id + "', sortValue='" + sortValue + "', source='" + source + "', score=" + score
                   + ", #fields=" + fields.size() + ')';
        }

    }

    /**
     * Contain content from a requested Field for a Record.
     */
    public static class Field implements Serializable {
        private static final long serialVersionUID = 38486524L;
        private String name;
        private String content;
        private boolean escapeContent;

        public Field(String name, String content, boolean escapeContent) {
            this.name = name;
            this.content = content;
            this.escapeContent = escapeContent;
        }

        /**
         * Deep copy contructor.
         * @param other the attributes from this field will be assigned to the new one.
         */
        public Field(Field other) {
            name = other.name;
            content = other.content;
            escapeContent = other.escapeContent;
        }

        public void toXML(StringWriter sw, String indent) {
            if (content == null || "".equals(content)) {
                return;
            }
            sw.append(indent);
            sw.append("<field name=\"").append(name).append("\">");
            sw.append(escapeContent ? XMLUtil.encode(content) : content);
            sw.append("</field>\n");
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getContent() {
            return content;
        }

        public boolean isEscapeContent() {
            return escapeContent;
        }

        @Override
        public String toString() {
            return "Field(" + name + ":" + content + ")";
        }
    }

    private static void appendIfDefined(StringWriter sw, String name, String value) {
        if (value == null) {
            return;
        }
        sw.append(" ").append(name).append("=\"");
        sw.append(XMLUtil.encode(value)).append("\"");
    }

    /**
     * Add a Record to the SearchResult. The order of Records is significant.
     *
     * @param record A record that should belong to the search result.
     */
    public void addRecord(Record record) {
        if (record == null) {
            throw new IllegalArgumentException("Expected a Record, got null");
        }
        log.debug("Adding Record " + record + " packed as single-member group");
        groups.add(new Group(record, groupField));
    }

    /**
     * Add a Group to the SearchResult.
     * @param group A group that belongs to the search result.
     */
    public void addGroup(Group group) {
        if (group == null) {
            throw new IllegalArgumentException("Expected a Group, got null");
        }
        log.debug("Adding Group " + group);
        groups.add(group);
    }

    @Override
    public String getName() {
        return NAME;
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
     *
     * @param otherO The search result that should be merged into this.
     */
    @Override
    public void merge(Response otherO) {
        log.trace("merge called");
        if (!(otherO instanceof DocumentResponse)) {
            throw new IllegalArgumentException(String.format("Expected response of class '%s' but got '%s'",
                                                             getClass().toString(), otherO.getClass().toString()));
        }
        super.merge(otherO);
        DocumentResponse other = (DocumentResponse) otherO;
        log.debug("Merging " + other.getRecords().size() + " records into this "
                  + getRecords().size() + " records with max records " + getMaxRecords() + " and "
                  + other.getMaxRecords());
        // TODO: Check for differences in basic attributes and warn if needed
        maxRecords = Math.max(getMaxRecords(), other.getMaxRecords());
        hitCount += other.hitCount;
        // This could also be additive, but we assume parallel calls
        searchTime = Math.max(searchTime, other.searchTime);

        if (!this.grouped && other.grouped) { // Copy group-attributes from other
            grouped = true;
            groupLimit = other.groupLimit;
            groupRows= other.groupRows;
            setGroupField(other.groupField); // Updates existing Records
        }
        boolean setOtherGroup = this.grouped && !other.grouped; // Update the valueField of other before assigning

        outer:
        for (Group otherGroup: other.groups) {
            for (Group thisGroup: groups) {
                if (thisGroup.groupValue != null && thisGroup.groupValue.equals(otherGroup.groupValue)) {
                    if (setOtherGroup) {
                        Group otherClone = new Group(otherGroup);
                        otherClone.setGroupField(groupField);
                        thisGroup.merge(otherClone);
                    } else {
                        thisGroup.merge(otherGroup);
                    }
                    continue outer;
                }
            }
            groups.add(otherGroup);
        }
        sort();
        reduce();
    }

    private void reduce() {
        while (groups.size() > maxRecords) { // TODO: Should we use rows here?
            groups.remove(groups.size()-1);
        }
        for (Group group: groups) {
            group.reduce(groupLimit);
        }
    }

    private void sort() {
        Collections.sort(groups, getGroupComparator());
    }


    /**
     * Assigns a new group field, stepping through all groups and updating the relevant attributes.
     * @param groupField new field for existing response content.
     */
    private void setGroupField(String groupField) {
        this.groupField = groupField;
        for (Group group: groups) {
            group.setGroupField(groupField);
        }
    }

    /**
     * @return the comparator used for ordering Records belonging to this
     *         response.
     */
    public Comparator<Record> getComparator() {
        if (sortKey == null || SORT_ON_SCORE.equals(sortKey)) {
            return scoreComparator;
        }
        final Collator collator = Collator.getInstance(new Locale("da"));

        return new Comparator<Record>() {
            @SuppressWarnings("ConstantConditions")
            @Override
            public int compare(Record o1, Record o2) {
                String s1 = o1.getSortValue() == null ? "" : o1.getSortValue();
                String s2 = o2.getSortValue() == null ? "" : o2.getSortValue();
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
//                    throw new IllegalStateException("Collator support not "
//                                                    + "finished");
                return collator.compare(s1, s2) * (reverseSort ? -1 : 1);
            }
        };
    }

    public Comparator<Group> getGroupComparator() {
        final Comparator<Record> recordComparator = getComparator();
        return new Comparator<Group>() {
            @Override
            public int compare(Group g1, Group g2) {
                if (g1.isEmpty()) {
                    return g2.isEmpty() ? 0 : -1;
                }
                if (g2.isEmpty()) {
                    return 1;
                }
                return recordComparator.compare(g1.get(0), g2.get(0));
            }
        };
    }

    private Comparator<Record> scoreComparator = new ScoreComparator();

    private static class ScoreComparator implements Comparator<Record>, Serializable {
        private static final long serialVersionUID = 168413841L;

        @Override
        public int compare(Record o1, Record o2) {
            float diff = o2.getScore() - o1.getScore();
            return diff < 0 ? -1 : diff > 0 ? 1 : 0;
        }
    }

    /**
     * {@code
     * <?xml version="1.0" encoding="UTF-8"?>
     * <documentresult filter="..." query="..."
     * startIndex="..." maxRecords="..."
     * sortKey="..." reverseSort="..."
     * fields="..." searchTime="..." hitCount="...">
     * <record score="..." sortValue="...">
     * <field name="recordID">...</field>
     * <field name="shortformat">...</field>
     * </record>
     * ...
     * </documentresult>
     * }
     * The content in the XML is entity-escaped.<br />
     * sortValue is the value that the sort was performed on. If the XML-result
     * from several searchers are to be merged, merge-ordering should be
     * dictated by this value.<br />
     * score is the score-value returned by the index implementation.<br />
     * searchTime is the number of milliseconds it took to perform the search.
     *
     * @return The search-result as XML, suitable for web-services et al.
     */
    @Override
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public String toXML() {
        log.trace("toXML() called");
        if (grouped) {
            sort();
            reduce();
        }
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
            for (int i = 0; i < resultFields.length; i++) {
                sw.append(XMLUtil.encode(resultFields[i]));
                if (i < resultFields.length - 1) {
                    sw.append(", ");
                }
            }
            sw.append("\"");
        }
        sw.append(" searchTime=\"");
        sw.append(Long.toString(searchTime)).append("\"");
        sw.append(" " + TIMING + "=\"");
        sw.append(XMLUtil.encode(getTiming())).append("\"");
        sw.append(" hitCount=\"");
        sw.append(Long.toString(hitCount)).append("\">\n");
        for (Group group: groups) {
            group.toXML(sw, "  ", grouped);
        }
        sw.append("</documentresult>\n");
        log.trace("Returning XML from toXML()");
        return sw.toString();
    }

    /* Getters and setters */

    /**
     * @return all records in all groups as a one-dimensional list.
     */
    public List<Record> getRecords() {
        List<Record> records = new ArrayList<>(groups.size());
        for (Group group: groups) {
            records.addAll(group);
        }
        return records;
    }

    public String getFilter() {
        return filter;
    }

    public String getQuery() {
        return query;
    }

    public long getStartIndex() {
        return startIndex;
    }

    public long getMaxRecords() {
        return maxRecords;
    }

    public String getSortKey() {
        return sortKey;
    }

    public boolean isReverseSort() {
        return reverseSort;
    }

    public void setRecords(List<Record> records) {
        if (!(records instanceof Serializable)) {
            throw new IllegalArgumentException(
                    "The records list was of class " + records.getClass() + " which is not serializable");
        }
        groups.clear();
        for (Record record: records) {
            addRecord(record);
        }
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

    // Because building the DocumentResponse takes time
    public void setSearchTime(long searchTime) {
        this.searchTime = searchTime;
    }

    public void setStartIndex(long startIndex) {
        this.startIndex = startIndex;
    }

    public void setMaxRecords(long maxRecords) {
        this.maxRecords = maxRecords;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setSortKey(String sortKey) {
        this.sortKey = sortKey;
    }

    public void setReverseSort(boolean reverseSort) {
        this.reverseSort = reverseSort;
    }

    /**
     * @return The amount of groups.
     */
    public int size() {
        return groups.size();
    }

    public long getSearchTime() {
        return searchTime;
    }

    public long getHitCount() {
        return hitCount;
    }

    @Override
    public String toString() {
        return "DocumentResponse(hitCount=" + hitCount + ", #groups=" + groups.size() + ")";
    }
}
