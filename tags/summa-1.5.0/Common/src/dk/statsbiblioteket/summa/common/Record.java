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
package dk.statsbiblioteket.summa.common;

import dk.statsbiblioteket.summa.common.util.StringMap;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.Zips;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * A Record is the atom data unit in Summa. Is is used for ingesting to the
 * Storage as well as extracting from it.
 * <p/>
 * The records may optionally have their content payload GZip compressed, in
 * which case it will be unzipped lazily on the first call to
 * {@link #getContent} or {@link #getContentAsUTF8()}
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal, te, mke")
public class Record implements Serializable, Comparable{
    private static Log log = LogFactory.getLog(Record.class);

    /**
     * The validation-state can only be stored indirectly using the map
     * provided by getMeta().
     */
    public enum ValidationState {notValidated, valid, invalid;
        public static ValidationState fromString(String in) {
            if ("notValidated".equals(in)) {
                return notValidated;
            } else if ("valid".equals(in)) {
                return valid;
            } else if ("invalid".equals(in)) {
                return invalid;
            } else {
                log.warn("Unknown state: '" + in
                         + "' in ValidationState.fromString. Returning "
                         + notValidated);
                return notValidated;
            }
        }
    }
    public static final String META_VALIDATION_STATE = "ValidationState";

    public static final String ID_DELIMITER = ";";

    /**
     * The id is a persistent unique identifier for the Record. This is normally
     * provided by the target. All Records must have an id.
     */
    private String id;
    /**
     * The base designates the target that this Record belongs to. Examples are
     * horizon, fagref and nat. All Records must have a base.
     */
    private String base;
    /**
     * If true, the Record is/is to be marked as deleted in the Storage.
     * Deleted records should not be indexed, but they can be viewed.
     */
    private boolean deleted = false;
    /**
     * If true, the Record should be indexed by the indexer. This is normally
     * controlled by the Storage when it scans for multi-volumes.
     */
    private boolean indexable = true;
    /**
     * The target-specific content of the Record. All Records must have data.
     */
    private byte[] data;
    /**
     * Creation-time for the content that the Record encapsulates.
     * The format is that of System.currentTimeMillis.
     */
    private long creationTime = System.currentTimeMillis();
    /**
     * Last modification-time for the content that the Record encapsulates.
     * The format is that of System.currentTimeMillis.
     */
    private long modificationTime = creationTime;
    /**
     * The id of the parent-record for this record, if any. If there is no
     * parent-record, this must be null. If a parent-record is present,
     * {@link #indexable} will normally be false.
     */
    private LinkedHashSet<String> parentIds;
    /**
     * A list of Record instances representing the parents of this record
     */
    private LinkedHashSet<Record> parents;
    /**
     * The ids of the children-records for this record, if any. If there are
     * no children-records, this must be null.
     */
    private LinkedHashSet<String> childIds;
    /**
     * A list of Record instances representing the children of this record
     */
    private LinkedHashSet<Record> children;

    /**
     * Meta-data for the Record, such as validation-state. Used for filter-
     * specific data. The map can be accessed by {@link#getMeta}. It does not
     * permit null - neither as key, nor value.
     */
    private StringMap meta;

    /**
     * Indicates whether the bytes stored in {@link #data} is GZip compressed.
     * If it is a call to {@link #getContent} should uncompress it before
     * returning the data and set this value to {@code false}.
     */
    // TODO: Add a getter and update MarcMultiVolumeMerger to use it for set
    private boolean contentCompressed;

    /**
     * Create a Record without content. The state of the Record is not
     * guaranteed to be consistent.
     */
    protected Record() {
        log.trace("Creating empty record");
    }

    /**
     * Create a Record with minimal values and mark is as not modified (new).
     * @param id   {@link #id}.
     * @param base {@link #base}.
     * @param data {@link #data}.
     */
    public Record(String id, String base, byte[] data) {
        long now = System.currentTimeMillis();
        init(id, base, false, true, data, now, now, null, null, null, false);
    }

    /**
     * Create a Record with minimal values and mark it as modified with the
     * supplied lastModified time. The {@link #creationTime} will be 0.
     * @param id           {@link #id}.
     * @param base         {@link #base}.
     * @param data         {@link #data}.
     * @param lastModified {@link #modificationTime}.
     */
     public Record(String id, String base, byte[] data, long lastModified){
         init(id, base, false, true, data, 0, lastModified,
              null, null, null, false);
    }

    /**
     * Create a Record with all parts explicitly specified.
     * @param id           the ID of the Record. {@link #id}.
     * @param base         the base that the Record comes from. {@link #base}.
     * @param deleted      true if the Record is deleted. {@link #deleted}.
     * @param indexable    true if the Record should be indexed.
     *                     {@link #indexable}.
     * @param data         the data for the Record. {@link #data}.
     * @param creationTime the time that the Record elements were created.
     *                     {@link #creationTime}.
     * @param lastModified the last time that the Record elements were modified.
     *                     {@link #modificationTime}.
     * @param parentIds       the ID for the parent record. {@link #parentIds}.
     * @param childIds     the ID's for the children records. {@link #childIds}.
     * @param meta         metadata for the Record.
     * @param contentCompressed whether the contents of the {@code data}
     *                          argument id GZip compressed. If this argument is
     *                          {@code true} the first call to
     *                          {@link #getContent} or
     *                          {@link #getContentAsUTF8()} will unzip the data
     */
    public Record(String id, String base, boolean deleted, boolean indexable,
                  byte[] data, long creationTime, long lastModified,
                  List<String> parentIds, List<String> childIds, StringMap meta,
                  boolean contentCompressed){
        init(id, base, deleted, indexable, data, creationTime, lastModified,
             parentIds, childIds, meta, contentCompressed);
    }

    /**
     * Initialize a Record with all parts explicitly specified.
     * @param id           the ID of the Record. {@link #id}.
     * @param base         the base that the Record comes from. {@link #base}.
     * @param deleted      true if the Record is deleted. {@link #deleted}.
     * @param indexable    true if the Record should be indexed.
     *                     {@link #indexable}.
     * @param data         the data for the Record. {@link #data}.
     * @param creationTime the time that the Record elements were created.
     *                     {@link #creationTime}.
     * @param lastModified the last time that the Record elements were modified.
     *                     {@link #modificationTime}.
     * @param parents       the ID for the parent record. {@link #parentIds}.
     * @param children     the ID's for the children records. {@link #childIds}.
     * @param meta         metadata for the Record.
     * @param contentCompressed if true, content is GZipped.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public void init(String id, String base, boolean deleted, boolean indexable,
                     byte[] data, long creationTime, long lastModified,
                     List<String> parents, List<String> children, StringMap meta,
                     boolean contentCompressed){
        setId(id);
        setBase(base);
        setDeleted(deleted);
        setIndexable(indexable);
        setRawContent(data);
        setCreationTime(creationTime);
        setModificationTime(lastModified);
        setParentIds(parents);
        setChildIds(children);
        setChildren(null);
        this.contentCompressed = contentCompressed;
        this.meta = meta;

        if (log.isTraceEnabled()) {
            log.trace("Created " + toString(true));
        } else if (log.isDebugEnabled()) {
            log.trace("Created " + toString());
        }
    }


    /* Mutators */

    public String getId(){
        return id;
    }
    public void setId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must be specified");
        }
        if ("".equals(id)) {
            //noinspection DuplicateStringLiteralInspection
            throw new IllegalArgumentException("ID must not be the empty "
                                               + "string");
        }
        this.id = id;
    }

    public String getBase() {
        return base;
    }
    public void setBase(String base) {
        if (base == null) {
            //noinspection DuplicateStringLiteralInspection
            throw new IllegalArgumentException("base must be specified for "
                                               + "record '" + getId() + "'");
        }
        if ("".equals(base)) {
            throw new IllegalArgumentException("base must not be the mpty "
                                               + "string for record '"
                                               + getId() + "'");
        }
        this.base = base;
    }

    public boolean isDeleted() {
        return deleted;
    }
    public void setDeleted(boolean isDeleted) {
        deleted = isDeleted;
    }

    public boolean isIndexable() {
        return indexable;
    }
    public void setIndexable(boolean isIndexable) {
        indexable = isIndexable;
    }

    /**
     * Get the content in uncompressed form, no matter if it is internally
     * compressed or not.
     * @return uncompressed content.
     */
    public byte[] getContent() {
        return getContent(true);
    }

    /**
     * Get the content in internal form or uncompressed form, depending on
     * the autoUncompress value.
     * @param autoUncompress if true, the content is always returned in
     *                       uncompressed form. If false, the content is
     *                       returned directly: If it is compressed, it will
     *                       be returned compressed.
     * @return the content in compressed or uncompressed form, depending on
     *         internal structure and the autoUncompress value.
     */
    public byte[] getContent(boolean autoUncompress) {
        if (contentCompressed && autoUncompress) {
            return Zips.gunzipBuffer(data);
        }
        return data;
    }

    /**
     * If the content is compressed, uncompress it.
     */
    public void unCompressContent() {
        if (contentCompressed) {
            // this call also sets contentCompressed = false
            setRawContent(Zips.gunzipBuffer(data));
        }
    }

    /**
     * If the content is uncompressed, compress it.
     */
    public void compressContent() {
        if (!contentCompressed) {
            // this call also sets contentCompressed = false
            setRawContent(Zips.gzipBuffer(data), true);
        }
    }

    /**
     * Store {@code content} as the data payload of this record. Be aware that
     * the content should be uncompressed, to avoid unclear semantics and
     * potential double-compression.
     * @param content    raw payload to use as record data.
     * @param doCompress if {@code true}, the  {@code content} will be
     *                   compressed upon storing.
     */
    public void setContent(byte[] content, boolean doCompress) {
        setRawContent(content, false);
        if (doCompress) {
            compressContent();
        }
    }

    /**
     * Set the content payload of this record. use this method to store
     * raw uncompressed data on the record. If you want to store compressed
     * data in the record (and have it transparently uncompressed on access)
     * use {@link #setRawContent(byte[], boolean)} instead.
     *
     * @param content raw payload to use as record data.
     */
    public void setRawContent(byte[] content) {
        setRawContent(content,  false);
    }


    /**
     * Store {@code content} as the data payload of this record. If
     * {@code contentCompressed == true} the content will be uncompressed
     * lazily (with GZip) on the first request to {@link #getContent()} or
     * {@link #getContentAsUTF8()}.
     *
     * @param content raw payload to use as record data.
     * @param contentCompressed if {@code true} {@code content} will be
     *                          uncompressed using GZipwhen read.
     */
    public void setRawContent(byte[] content, boolean contentCompressed) {
        if (content == null) {
            //noinspection DuplicateStringLiteralInspection
            throw new IllegalArgumentException("data must be specified for "
                                               + "record '" + getId()
                                               + "' from base '" + getBase()
                                               + "'");
        }
        data = content;
        this.contentCompressed = contentCompressed;
    }

    public String getContentAsUTF8() {
        try {
            return new String(getContent(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Could not convert uning utf-8");
        }
    }

    public long getCreationTime() {
        return creationTime;
    }
    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public long getModificationTime() {
        return modificationTime;
    }
    public void setModificationTime(long modificationTime) {
        this.modificationTime = modificationTime;
    }

    public List<String> getParentIds() {
        return parentIds == null ? null : new ArrayList<String>(parentIds);
    }

    /**
     * Sets the parent-IDs for this Record. This copies the content of the
     * given parentIDs to the Record, so callers are free to clear the given
     * list after this method is called.
     * </p><p>
     * Note: Duplicates are removed as part of the copy.
     * </p><p>
     * Note 2: Object-references to parents are cleared as part of this to
     *         ensure consistence.
     * @param parentIds the IDs to assign to the Record.
     */
    public void setParentIds(List<String> parentIds) {
        if (parentIds == null) {
            this.parentIds = null;
        } else if (parentIds.isEmpty()) {
            //noinspection DuplicateStringLiteralInspection
            log.warn("The non-existence of a parentId should be stated by null, "
                     + "not the empty list. Problematic Record with id '"
                     + getId() + "' from base '" + getBase()
                     + "'. Continuing creation");
            //noinspection AssignmentToNull
            this.parentIds = null;
        } else {
            this.parentIds = new LinkedHashSet<String>(parentIds);
        }

        parents = null;
    }

    public List<String> getChildIds() {
        return childIds == null ? null : new ArrayList<String>(childIds);
    }

    /**
     * Set the child ids listed as children of this record. This will reset
     * any children registered with {@link #setChildren(List)}. Note that this
     * method copies the content of the given list, so callers are free to
     * clear the list after calling.
     * Note 2: Object-references to children are cleared as part of this to
     *         ensure consistence.
     * @param childIds list of record ids for the record's children
     */
    public void setChildIds(List<String> childIds) {
        if (childIds == null) {
            this.childIds = null;
            return;
        } else if (childIds.isEmpty()) {
            //noinspection DuplicateStringLiteralInspection
            log.warn("No childIds should be stated by null, not the empty "
                     + "list. Problematic Record with id '" + getId()
                     + "' from base '" + getBase() + "'. Continuing creation");
            //noinspection AssignmentToNull
            this.childIds = null;
        } else {
            this.childIds = new LinkedHashSet<String>(childIds);
        }

        children = null;
    }

    /**
     * Set child {@code Record} instances of this record. This will also call
     * {@link #setChildIds} with the ids of the child records
     *
     * @param children list of record instances to register as children
     */
    public void setChildren(List<Record> children) {
        if (children == null) {
            this.children = null;
            return;
        }

        List<String> newChildIds = new ArrayList<String>(children.size());
        for (Record child : children) {
            newChildIds.add (child.getId());
        }

        setChildIds(newChildIds);
        this.children = new LinkedHashSet<Record>(children);
    }

    /**
     * Get the list of resolved child records. If the record was constructed
     * without child id resolution or {@link #setChildren} has never been
     * called this method will return {@code null}
     *
     * @return a list of {@code Record} instances representing the children
     *         of this record, or {@code null} if the child ids has never been
     *         resolved
     */
    public List<Record> getChildren () {
        return children == null ? null : new ArrayList<Record>(children);
    }

    /**
     * Set parent {@code Record} instances of this record. This will also call
     * {@link #setParentIds} with the ids of the parent records
     *
     * @param parents list of record instances to register as parents
     */
    public void setParents(List<Record> parents) {
        if (parents == null) {
            this.parents = null;
            return;
        }

        List<String> newParentIds = new ArrayList<String>(parents.size());
        for (Record parent : parents) {
            newParentIds.add (parent.getId());
        }

        setParentIds(newParentIds);
        this.parents = new LinkedHashSet<Record>(parents);
        
    }

    /**
     * Get the list of resolved parent records. If the record was constructed
     * without parent id resolution or {@link #setParents} has never been
     * called this method will return {@code null}
     *
     * @return a list of {@code Record} instances representing the parents
     *         of this record, or {@code null} if the parent ids has never been
     *         resolved
     */
    public List<Record> getParents () {
        return parents == null ? null : new ArrayList<Record>(parents);
    }

    /**
     * Return whether or not the record has any listed parents
     * @return whether or not the record has any listed parents
     */
    public boolean hasParents() {
        return parentIds != null;
    }

    /**
     * Return whether or not the record has any listed children
     * @return whether or not the record has any listed children
     */
    public boolean hasChildren() {
        return childIds != null;
    }

    public long getLastModified() {
        return getModificationTime();
    }

    /**
     * Get length of content ({@link #data}) of the Record.
     * @return length of content/data
     */
    public long getLength() {
        return data == null ? 0 : data.length;
    }

    /**
     * @return true if the Record is new.
     */
    public boolean isNew() {
        return getModificationTime() == getCreationTime();
    }

    /**
     * @return true if the Record is modified.
     */
    public boolean isModified() {
        return !isNew();
    }

    /**
     * Update {@link #modificationTime} to the current time, thereby marking the
     * record as modified.
     * </p><p>
     * Note: The granularity of this is in milliseconds, so if youch is called imm
     */
    public void touch() {
        setModificationTime(System.currentTimeMillis());
    }

    /**
     * There is always a meta-map for each Record, but it is created lazily if
     * it has no values. Use {@link #getMeta(String)} for fast look-up of values
     * where it is expected that the map is empty, as it will never create a
     * new map.
     * @return the meta-map for this Record.
     */
    public StringMap getMeta() {
        if (meta == null) {
            meta = new StringMap(10);
        }
        return meta;
    }

    /**
     * Request a meta-value for the given key. This method is more efficient
     * than requesting the full map with {@link#getMeta()}, as it never creates
     * a new map.
     * @param key the key for the value.
     * @return the value for the key, or null if the key is not in the map.
     */
    public String getMeta(String key) {
        return meta == null ? null :meta.get(key);
    }

    public void setMeta (StringMap meta) {
        this.meta = meta;
    }
    
    public void addMeta (String key, String value) {
        getMeta().put(key, value);
    }

    /**
     * @return true if a meta-map has been created. Used for time/space
     *         optimization. 
     */
    public boolean hasMeta() {
        return meta != null;
    }

    /**
     * Returns a hash code value for the record.
     * @return a hash code value
     */
    @Override
    @QAInfo(level = QAInfo.Level.FINE,
        state = QAInfo.State.QA_NEEDED,
        comment="Hans made the hash-function, so no present persons know how it "
                + "works (or if)")
    public int hashCode() {
        int result;
        result = (int) (modificationTime ^ modificationTime >>> 32);
        result = 29 * result + (int) (getLength() ^ getLength() >>> 32);
        result = 29 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    /**
     * Compares this Record with the specified Record for order.
     * The natural ordering on records is defined to be lexicographical by id.
     * @param o parameter object to be compared to this record
     * @return a negative integer, zero, or a positive integer as the name of this record is
     *         lexicographically less than, equal to, or greater than the name of the record argument.
     * @throws ClassCastException if o is not a Record
     */
    public int compareTo(Object o) {
        return getId().compareTo(((Record) o).getId());
    }

    @Override
    @SuppressWarnings({"UnnecessaryParentheses"})
    @QAInfo(level = QAInfo.Level.PEDANTIC,
            state = QAInfo.State.IN_DEVELOPMENT,
            author = "te",
            comment="Tripple-check childIds and meta")
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Record)) {
            return false;
        }
        Record other = (Record)o;

        try {
            return id.equals(other.getId())
                   && base.equals(other.getBase())
                   // Semantically we compare only the record content, storage context, such as not timestamps:
                   //&& timeEquals(creationTime, other.getCreationTime())
                   //&& timeEquals(modificationTime, other.getModificationTime())
                   && deleted == other.isDeleted()
                   && indexable == other.isIndexable()
                   && ((parentIds == null && other.getParentIds() == null)
                       || (parentIds != null && deepEquals(parentIds, other.getParentIds())))
                   && Arrays.equals(getContent(), other.getContent())
                   && ((childIds == null && other.getChildIds() == null) ||
                       (childIds != null) && deepEquals(childIds, other.getChildIds()))
                    && ((!hasMeta() == !other.hasMeta()) 
                        || (hasMeta() && getMeta().equals(other.getMeta())));
        } catch (Exception e) {
            log.error("Error calling equals for " + this + " and " + other
                      + ". Returning false", e);
            return false;
        }
    }

    /**
     * @return a human-readable single line version of Record.
     */
    @Override
    public String toString() {
        return toString(false);
    }

    /**
     * @param verbose if true, a verbose description is returned.
     * @return a human-readable single line version of Record.
     * Note: The verbose option uses Calendar to output timestamps and is thus
     *       expensive.
     */
    public String toString(boolean verbose) {
        return "Record [id(" + getId() + "), base(" + getBase()
               + "), deleted(" + isDeleted() + "), indexable(" + isIndexable()
               + "), data-length(" + getLength()
               + "), num-childrenIDs(" + (childIds == null ? 0 : childIds.size())
               + "), num-parentsIDs(" + (parentIds == null ? 0 : parentIds.size())
               + "), num-childRecords("
               + (children == null ? 0 : children.size())
               + "), num-parentRecords(" 
               + (parents == null ? 0 : parents.size())
               + ")" + (verbose ?
                        ", creationTime(" + timeToString(getCreationTime())
                        + "), modificationTime("
                        + timeToString(getModificationTime())
                        + "), parentIds("
                        + (parentIds == null ? "" :
                           Logs.expand(new ArrayList<String>(parentIds), 5))
                        + "), childIds("
                        + (childIds == null ? "" :
                           Logs.expand(new ArrayList<String>(childIds), 5))
                        + "), meta("
                        + (meta == null ? "" : Logs.expand(
                                Arrays.asList(meta.keySet().toArray()), 5))
                        : "")
               + ")]";
    }

    /**
     * Simple conversion of milliseconds to ISO-standard date-time display.
     * @param timestamp milliseconds since 1972-01-01.
     * @return a human readable timestamp.
     */
    private String timeToString(long timestamp) {
        if (timestamp == 0) {
            return "NA";
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        return String.format("%1$tF %1$tT", calendar);
    }

    /**
     * Converts a String-encoded list of record-ID's to a proper list.
     * The String-encoded list is the ID's delimited by ';'.
     * @param ids a ';'-delimited string with children-ID's.
     * @return a List with the children-ID's. If the input-string is null or
     *         of length 0, null is returned.
     */
    public static List<String> idStringToList(String ids) {
        if (ids == null || "".equals(ids)) {
            return null;
        }
        String[] stringChildren = ids.split(ID_DELIMITER);
        return Arrays.asList(stringChildren);
    }

    /**
     * Converts a proper list of record-ID's to a String-encoded list.
     * The String-encoded list is the ID's delimited by ';'.
     * @param ids as a proper list of children-ID's.
     * @return a ';'-delimited string with children-ID's. If the input-list is
     *         null or of length 0, null is returned.
     */
    public static String idListToString(List<String> ids) {
        if (ids == null || ids.size() == 0) {
            return null;
        }
        StringWriter sw = new StringWriter(ids.size()*256);
        for (int i = 0 ; i < ids.size() ; i++) {
            sw.append(ids.get(i));
            if (i < ids.size()-1) {
                sw.append(ID_DELIMITER);
            }
        }
        return sw.toString();
    }

    /**
     * Helper-method for creating a pseudo-Record which represents a deleted
     * Record.
     * @param id   {@link #id}
     * @param base {@link #base}
     * @return a Record marked as deleted.
     */
    public static Record createDeletedRecord(String id, String base) {
        Record record = new Record();
        record.setId(id);
        record.setBase(base);
        record.setDeleted(true);
        record.setIndexable(false);
        return record;
    }

    /**
     * Deep equality check of iterables. Used to check equality of parentIds
     * and childrenIds lists
     * @param a first iterable to compare
     * @param b second iterable to compare
     * @return true iff a and b has the same number of elements and all elements
     *         respond true to and equals()
     */
    private static <E extends Comparable> boolean deepEquals
                                                (Iterable<E> a, Iterable<E> b) {
        Iterator<? extends Comparable> ia = a.iterator();
        Iterator<? extends Comparable> ib = b.iterator();

        while (ia.hasNext()) {
            if (!ib.hasNext()) {
                return false;
            }
            if (!ia.next().equals(ib.next())) {
                return false;
            }
        }

        return !ib.hasNext();
    }
}

