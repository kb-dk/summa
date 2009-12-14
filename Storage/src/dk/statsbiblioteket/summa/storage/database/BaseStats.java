package dk.statsbiblioteket.summa.storage.database;

import dk.statsbiblioteket.util.qa.QAInfo;

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
public class BaseStats {

    private String baseName;
    private long deletedIndexables;
    private long nonDeletedIndexables;
    private long deletedNonIndexables;
    private long nonDeletedNonIndexables;

    public BaseStats(String baseName,
                     long deletedIndexables,
                     long nonDeletedIndexables,
                     long deletedNonIndexables,
                     long nonDeletedNonIndexables) {
        this.baseName = baseName;
        this.deletedIndexables = deletedIndexables;
        this.nonDeletedIndexables = nonDeletedIndexables;
        this.deletedNonIndexables = deletedNonIndexables;
        this.nonDeletedNonIndexables = nonDeletedNonIndexables;
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

    public String getBaseName() {
        return baseName;
    }
}
