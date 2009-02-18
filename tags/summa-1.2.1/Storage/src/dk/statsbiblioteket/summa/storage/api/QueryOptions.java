package dk.statsbiblioteket.summa.storage.api;

import dk.statsbiblioteket.summa.common.util.StringMap;
import dk.statsbiblioteket.summa.common.Record;

import java.io.Serializable;

/**
 * {@code QueryFilter} is an immutable filter that is passed into the methods
 * in the {@link ReadableStorage} interface to add additional criteria the
 * records must match.
 * <p/>
 * There are four core options one can set on a query. It is possible to add
 * additional custom options to the storage by passing them in via the
 * {@code meta} argument.
 * <p/>
 * The main options are:
 * <ul>
 *   <li>{@code deletedFilter} : If {@code null} allow all records disregarding
 *                              their state. If {@code true} match only deleted
 *                              records, and if {@code false} match only records
 *                              that are not deleted</li>
 *   <li>{@code indexableFilter} : If {@code null} allow all records disregarding
 *                             their state. If {@code false} match only records
 *                             not indexable, and if {@code true} match only
 *                             records that are marked indexable</li>
 *   <li>{@code childDepth} : The number of child levels to expand children to.
 *                            If this value is {@code 0} then no expansion will
 *                            occur, if it is {@code -1} expansion will occur
 *                            recursively to any depth</li>
 *   <li>{@code parentHeight} : Number of levels to expand parent records
 *                              upwards. If this value is {@code 0} then no
 *                              parent expansion occurs, and if it is
 *                             {@code -1} do recursive expansion up to the root
 *                             record</li>
 * </ul>
 *
 * <h3>Null Options</h3>
 * If the whole {@code QueryOptions} object or any one of the options on it
 * is {@code null} it indicates a fully permisive option.
 * A {@code QueryOptions} set to {@code null} will allow all records and do no
 * expansion of children or parents.
 */
public class QueryOptions implements Serializable {

    private Boolean deletedFilter;
    private Boolean indexableFilter;
    private int childDepth;
    private int parentHeight;
    private StringMap meta;

    public QueryOptions(Boolean deletedFilter,
                        Boolean indexableFilter,
                        int childDepth,
                        int parentHeight,
                        StringMap meta) {
        this.deletedFilter = deletedFilter;
        this.indexableFilter = indexableFilter;
        this.childDepth = childDepth;
        this.parentHeight = parentHeight;
        this.meta = meta;
    }

    public QueryOptions() {
        this(null, null, 0, 0, null);
    }

    public QueryOptions(Boolean deletedFilter,
                        Boolean indexableFilter,
                        int childDepth,
                        int parentHeight) {
        this(deletedFilter, indexableFilter, childDepth, parentHeight, null);
    }

    /**
     * Create a clone of {@code original}.
     * @param original a non-{@code null} QueryOptions object
     */
    public QueryOptions(QueryOptions original) {
        this(original.deletedFilter(),
             original.indexableFilter(),
             original.childDepth(),
             original.parentHeight(),
             original.meta());
    }

    public Boolean deletedFilter() {
        return deletedFilter;
    }

    public boolean hasDeletedFilter() {
        return deletedFilter != null;
    }

    public Boolean indexableFilter () {
        return indexableFilter;
    }

    public boolean hasIndexableFilter () {
        return indexableFilter != null;
    }

    public int childDepth () {
        return childDepth;
    }

    public int parentHeight () {
        return parentHeight;
    }

    public boolean hasMeta () {
        return meta != null;
    }

    public StringMap meta() {
        return meta;
    }

    public String meta(String key) {
        return meta == null ? null : meta.get(key);
    }

    /**
     * Check whether a given record has the required deletion and indexable
     * flags to match this set of query options
     * @param r the record to check
     * @return true iff the record has the required deletion and indexable flags
     */
    public boolean allowsRecord (Record r) {
        if (hasDeletedFilter()) {
            if (r.isDeleted() != deletedFilter()) {
                return false;
            }
        }

        if (hasIndexableFilter()) {
            if (r.isIndexable() != indexableFilter()) {
                return false;
            }
        }

        return true;
    }
}
