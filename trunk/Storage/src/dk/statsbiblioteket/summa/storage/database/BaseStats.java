package dk.statsbiblioteket.summa.storage.database;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLUtil;
import dk.statsbiblioteket.summa.common.util.StringMap;

import java.io.Serializable;
import java.io.Writer;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Encapsulation of storage statistics for a given base in the
 * storage service.
 *
 * @author mke
 * @since Dec 14, 2009
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class BaseStats implements Serializable {

    private String baseName;
    private long lastModified;
    private long deletedIndexables;
    private long nonDeletedIndexables;
    private long deletedNonIndexables;
    private long nonDeletedNonIndexables;
    private StringMap meta;

    public BaseStats(String baseName,
                     long lastModified,
                     long deletedIndexables,
                     long nonDeletedIndexables,
                     long deletedNonIndexables,
                     long nonDeletedNonIndexables) {
        this.baseName = baseName;
        this.lastModified = lastModified;
        this.deletedIndexables = deletedIndexables;
        this.nonDeletedIndexables = nonDeletedIndexables;
        this.deletedNonIndexables = deletedNonIndexables;
        this.nonDeletedNonIndexables = nonDeletedNonIndexables;
        this.meta = null;
    }

    /**
     * The total number of records in the <i>deleted</i> state,
     * disregarding whether or not they are <i>indexable</i>.
     * @return The total number of records that have the
     *         <i>deleted</i> flag set.
     */
    public long getDeletedCount() {
        return deletedIndexables + deletedNonIndexables;
    }

    /**
     * The total number of records in the <i>indexable</i> state,
     * disregarding whether or not they are <i>deleted</i>.
     * @return The total number of records that have the
     *         <i>indexable</i> flag set.
     */
    public long getIndexableCount() {
        return deletedIndexables + nonDeletedIndexables;
    }

    /**
     * Return the number of records that would be indexed in a normal
     * situation - which can be considered the "live set" of records
     * in the storage. This is calculated as the
     * number of records that has the <i>indexable</i> flag set, but
     * not the <i>deleted</i> flag.
     *
     * @return The number of records that has the <i>indexable</i>
     *         flag set, but not the <i>deleted</i> flag.
     */
    public long getLiveCount() {
        return nonDeletedIndexables;
    }

    /**
     * Return the total number of records in this base disregarding
     * the states of the records.
     *
     * @return The total number of records with their base set to
     *         the base represented by this object. Ie. the base with
     *         name equalling {@link #getBaseName()}.
     */
    public long getTotalCount() {
        return deletedIndexables + nonDeletedIndexables
               + nonDeletedNonIndexables + deletedNonIndexables;
    }

    /**
     * The name of the base these statistics were generated for
     * @return The name of the base these statistics were generated for
     */
    public String getBaseName() {
        return baseName;
    }

    /**
     * Get the timestamp for the last update on the base represented by these
     * statistics
     * @return the timestamp for when {@code flush()}, {@code flushAll()}, or
     * {@code clearBase()} was called on the base in question
     */
    public long getModificationTime() {
        return lastModified;
    }

    /**
     * Return whether or not these statistics has additional metadata
     * associated with them
     * @return {@code true}
     */
    public boolean hasMeta() {
        return meta != null && !meta.isEmpty();
    }

    /**
     * Return the additional metadata associated with these statistics.
     * Possibly {@code null} if no additional metadata has been recorded.
     * @return a key/value map of strings for the additional metadata or
     *         {@code null} if no metatdata is recorded
     */
    public StringMap meta() {
        return meta;
    }

    /**
     * Return the metadata associated with the given {@code key}, returning
     * {@code null} if no data exists for the key or no metadata is associated
     * with the statistics
     * @param key the name of the metadata field to look up
     * @return the value corresponding to {@code key} or {@code null}
     */
    public String meta(String key) {
        return meta == null ? null : meta.get(key);
    }

    /**
     * Set a key/value pair as additional metadata carried with these
     * statistics. If a field already exists under the given name it will
     * be replaced with the supplied value
     * @param key the unique name for the metadata field
     * @param value the value to set for the field
     * @return always returns {@code this}
     */
    public BaseStats meta(String key, String value) {
        if (meta == null) {
            meta = new StringMap();
        }
        meta.put(key,value);
        return this;
    }

    public static void toXML(List<BaseStats> stats, Writer out) {
        PrintWriter w = new PrintWriter(out);
        w.append("<holdings>\n");
        for (BaseStats b : stats) {
            w.append("  <base name=\"")
             .append(XMLUtil.encode(b.getBaseName()))
             .append("\"");
            w.append(" deleted=\"")
             .append(Long.toString(b.getDeletedCount()))
             .append("\"");
            w.append(" indexable=\"")
             .append(Long.toString(b.getIndexableCount()))
             .append("\"");
            w.append(" live=\"")
             .append(Long.toString(b.getLiveCount()))
             .append("\"");
            w.append(" total=\"")
             .append(Long.toString(b.getTotalCount()))
             .append("\"");
            w.append(" modificationTime=\"")
             .append(Long.toString(b.getModificationTime()))
             .append("\"");

            if (!b.hasMeta()) {
                w.append("/>\n");
            } else {
                w.append(">\n");
                for (Map.Entry<String,String> meta : b.meta().entrySet()) {
                    w.append("    <meta key=\"")
                     .append(XMLUtil.encode(meta.getKey()))
                     .append("\" value=\"")
                     .append(XMLUtil.encode(meta.getValue()))
                     .append("\"/>\n");
                }
                w.append("  </base>\n");
            }
        }
        w.append("</holdings>");
        w.flush();
    }
}
