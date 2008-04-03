/* $Id: Record.java,v 1.12 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.12 $
 * $Date: 2007/10/04 13:28:21 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
package dk.statsbiblioteket.summa.common;

import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import dk.statsbiblioteket.summa.common.util.StringMap;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A Record is the atom data unit in summa. Is is used for ingesting to the
 * MetadataStorage as well as extracting from it.<br>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal, te")
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
     * If true, the Record is/is to be marked as deleted in the MetadataStorage.
     * Deleted records should not be indexed, but they can be viewed.
     */
    private boolean deleted = false;
    /**
     * If true, the Record should be indexed by the indexer. This is normally
     * controlled by the MetadataStorage when it scans for multi-volumes.
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
    private String parent;
    /**
     * The ids of the children-records for this record, if any. If there are
     * no children-records, this must be null.
     */
    private List<String> children;

    /**
     * Meta-data for the Record, such as validation-state. Used for filter-
     * specific data. The map can be accessed by {@link#getMeta}. It does not
     * permit null - neither as key, nor value.
     */
    private StringMap meta;

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
        init(id, base, false, true, data, now, now, null, null, null);
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
         init(id, base, false, true, data, 0, lastModified, null, null, null);
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
     * @param parent       the ID for the parent record. {@link #parent}.
     * @param children     the ID's for the children records. {@link #children}.
     * @param meta         metadata for the Record.
     */
    public Record(String id, String base, boolean deleted, boolean indexable,
                  byte[] data, long creationTime, long lastModified,
                  String parent, List<String> children, StringMap meta){
        init(id, base, deleted, indexable, data, creationTime, lastModified,
             parent, children, meta);
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
     * @param parent       the ID for the parent record. {@link #parent}.
     * @param children     the ID's for the children records. {@link #children}.
     * @param meta         metadata for the Record.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public void init(String id, String base, boolean deleted, boolean indexable,
                     byte[] data, long creationTime, long lastModified,
                     String parent, List<String> children, StringMap meta){
        log.trace("Creating Record with id '" + id + "' from base '" + base
                  + "'");
        setId(id);
        setBase(base);
        setDeleted(deleted);
        setIndexable(indexable);
        setContent(data);
        setCreationTime(creationTime);
        setModificationTime(lastModified);
        setParent(parent);
        setChildren(children);
        this.meta = meta;
        if (log.isDebugEnabled()) {
            log.debug("Created " + toString());
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

    public byte[] getContent() {
        return data;
    }
    public void setContent(byte[] content) {
        if (content == null) {
            //noinspection DuplicateStringLiteralInspection
            throw new IllegalArgumentException("data must be specified for "
                                               + "record '" + getId()
                                               + "' from base '" + getBase()
                                               + "'");
        }
        data = content;
    }
    public String getContentAsUTF8() {
        try {
            return new String(data, "utf-8");
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

    public String getParent() {
        return parent;
    }
    public void setParent(String parent) {
        if ("".equals(parent)) {
            //noinspection DuplicateStringLiteralInspection
            log.warn("The non-existence of a parent should be stated by null, "
                     + "not the empty string. Problematic Record with id '"
                     + getId() + "' from base '" + getBase()
                     + "'. Continuing creation");
            //noinspection AssignmentToNull
            this.parent = null;
        } else {
            this.parent = parent;
        }
    }

    public List<String> getChildren() {
        return children;
    }
    public void setChildren(List<String> children) {
        if (children != null && children.size() == 0) {
            //noinspection DuplicateStringLiteralInspection
            log.warn("No children should be stated by null, not the empty "
                     + "list. Problematic Record with id '" + getId()
                     + "' from base '" + getBase() + "'. Continuing creation");
            //noinspection AssignmentToNull
            this.children = null;
        } else {
            this.children = children;
        }
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
     * The natural ordering on records is defined to be lexicographical by name (bib#/id).
     * @param o parameter object to be compared to this record
     * @return a negative integer, zero, or a positive integer as the name of this record is
     *         lexicographically less than, equal to, or greater than the name of the record argument.
     * @throws ClassCastException if o is not a Record
     */
    public int compareTo(Object o) {
        return getId().compareTo(((Record) o).getId());
    }

    @SuppressWarnings({"UnnecessaryParentheses"})
    @QAInfo(level = QAInfo.Level.PEDANTIC,
            state = QAInfo.State.IN_DEVELOPMENT,
            author = "te",
            comment="Tripple-check children and meta")
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Record)) {
            return false;
        }
        Record other = (Record)o;

        try {
            return id.equals(other.getId())
                   && base.equals(other.getBase())
                   && timeEquals(creationTime, other.getCreationTime())
                   && timeEquals(modificationTime, other.getModificationTime())
                   && deleted == other.isDeleted()
                   && indexable == other.isIndexable()
                   && ((parent == null && other.getParent() == null)
                       || (parent != null && parent.equals(other.getParent())))
                   && Arrays.equals(data, other.getContent())
                   && ((children == null && other.getChildren() == null) ||
                       (children != null) && Strings.join(children, ",").equals(
                    Strings.join(other.getChildren(), ",")))
                    && ((!hasMeta() == !other.hasMeta()) 
                        || (hasMeta() && getMeta().equals(other.getMeta())));
        } catch (Exception e) {
            log.error("Error calling equals for " + this + " and " + other
                      + ". Returning false", e);
            return false;
        }
    }

    /**
     * As we cannot rely on the underlying storage for fine granularity of
     * time, we use seconds when comparing timestamps.
     * @param time1 a timestamp in milliseconds, as used by
     *              System.currentTimeMsillis.
     * @param time2 a timestamp in milliseconds, as used by
     *              System.currentTimeMillis.
     * @return true if the timestamps are equal down to second resolution.
     */
    private boolean timeEquals(long time1, long time2) {
        return time1 / 1000 == time2 / 1000;
    }

    /**
     * @return a human-readable single line version of Record.
     */
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
               + ")" + (verbose ?
                        ", creationTime(" + timeToString(getCreationTime())
                        + "), modificationTime("
                        + timeToString(getModificationTime())
                        + "), parent(" + getParent()
                        + "), children("
                        + (children == null ? "" : Logs.expand(children, 5))
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
     * Converts a String-encoded list of children-ID's to a proper list.
     * The String-encoded list is the ID's delimited by ';'.
     * @param children a ';'-delimited string with children-ID's.
     * @return a List with the children-ID's. If the input-string is null or
     *         of length 0, null is returned.
     */
    public static List<String> childrenStringToList(String children) {
        if (children == null || "".equals(children)) {
            return null;
        }
        String[] stringChildren = children.split(ID_DELIMITER);
        return Arrays.asList(stringChildren);
    }

    /**
     * Converts a proper list of children-ID's to a String-encoded list.
     * The String-encoded list is the ID's delimited by ';'.
     * @param children as a proper list of children-ID's.
     * @return a ';'-delimited string with children-ID's. If the input-list is
     *         null or of length 0, null is returned.
     */
    public static String childrenListToString(List<String> children) {
        if (children == null || children.size() == 0) {
            return null;
        }
        StringWriter sw = new StringWriter(children.size()*256);
        for (int i = 0 ; i < children.size() ; i++) {
            sw.append(children.get(i));
            if (i < children.size()-1) {
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
}
