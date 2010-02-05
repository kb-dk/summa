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
package dk.statsbiblioteket.summa.storage.api;

import dk.statsbiblioteket.summa.common.util.StringMap;
import dk.statsbiblioteket.summa.common.Record;

import java.io.Serializable;
import java.util.HashMap;

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
 *
 * <h3>Meta flags</h3>
 * Query options can host an arbitrary map of key/value pairs known as meta
 * flags. These flags can be used to alter the behaviour of queries a to pass
 * sepcial information to batch jobs.
 * <p/>
 * Pre defined meta flags:
 * <ul>
 *   <li><tt>ALLOW_PRIVATE</tt> - Set this to {@code "true"} to allow
 *       {@code getRecord()} requests to access storage private records,
 *       like {@code __holdings__}</li>
 *   <li><tt>TRY_UPDATE</tt> - If this flag is set this to {@code "true"} any
 *       calls to {@code flush()} or {@code flushAll()} will not update
 *       the record if it already exists with the exact same fields. This also
 *       means that the record modication time will not be updated if it is
 *       already known</li>
 * </ul>
 */
public class QueryOptions implements Serializable {

    protected Boolean deletedFilter;
    protected Boolean indexableFilter;
    protected int childDepth;
    protected int parentHeight;
    protected StringMap meta;

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
        return meta != null && !meta.isEmpty();
    }

    /**
     * Return the underlying {@link StringMap} used to store the meta values
     * for these query options
     * @return the underlying StringMap or {@code null} if the options does
     *         not have any meta values associated with them
     */
    public StringMap meta() {
        return meta;
    }

    /**
     * Get a meta-value on these query options
     * @param key the meta key to look up
     * @return the meta value or {@code null} if no value corresponds to
     *         {@code key}
     */
    public String meta(String key) {
        return meta == null ? null : meta.get(key);
    }

    /**
     * Set a meta-value on these query options
     * @param key the key to store the value under
     * @param value the value to associate with {@code key}
     */
    public void meta(String key, String value) {
        if (meta == null) {
            meta = new StringMap();
        }
        meta.put(key, value);
    }

    /**
     * Check whether a given record has the required deletion and indexable
     * flags to match this set of query options
     * @param r the record to check
     * @return true iff the record has the required deletion and indexable flags
     */
    public boolean allowsRecord (Record r) {
        if (hasDeletedFilter()) {
            return r.isDeleted() == deletedFilter().booleanValue();
        }

        if (hasIndexableFilter()) {
            return r.isIndexable() == indexableFilter().booleanValue();
        }

        return true;
    }
}

