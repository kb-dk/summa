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
package dk.statsbiblioteket.summa.storage;

import java.rmi.RemoteException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.WritableStorage;
import dk.statsbiblioteket.summa.storage.api.ReadableStorage;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * StorageBase is an abstract class to facilitate implementations of the
 * {@link Storage} interface.
 * <p/>
 * There is no choice of storage in StorageBase. This choice is made in the
 * subclasses.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te, mke")
public abstract class StorageBase implements Storage {
    private static Log log = LogFactory.getLog(StorageBase.class);

    /**
     * Meta flag used on {@link QueryOptions} allowing access to
     * "private" records in the storage service if set to {@code "true"}.
     */
    protected static final String ALLOW_PRIVATE = "ALLOW_PRIVATE";

    /**
     * Meta flag used on {@link QueryOptions} to indicate to {@code flush()}
     * or {@code flushAll()} that if the record already exists with the exact
     * same fields then nothing should be done (and consequently the record's
     * modification in storage will not be updated).
     */
    protected static final String TRY_UPDATE = "TRY_UPDATE";

    private long storageStartTime;
    private HashMap<String,Long> lastFlushTimes;
    private Matcher privateIdMatcher;

    public StorageBase(Configuration conf) {
        storageStartTime = System.currentTimeMillis();
        lastFlushTimes = new HashMap<String,Long>(10);
        privateIdMatcher = Pattern.compile("__.+__").matcher("");
    }

    /**
     * Create the storage base with an empty configuration. This means that
     * default values will be used throughout.
     */
    public StorageBase() {
        this(Configuration.newMemoryBased());
    }

    /**
     * Return the time stamp for the last time {@link #flush} or
     * {@link #flushAll} was called.
     * <p/>
     * In case there has been no changes to to the storage since
     * it was started the start time stamp of the storage will be returned.
     *
     * @param base the base to check for changes in. If {@code base} is
     *             {@code null} return the maximal time stamp from all bases
     * @return the time stamp
     * @throws IOException on communication errors
     */
    public long getModificationTime (String base) throws IOException {
        Long lastFlush = null;

        if (base != null) {
            lastFlush = lastFlushTimes.get(base);
            if (lastFlush == null) {
                lastFlushTimes.put (base, storageStartTime);
                lastFlush = storageStartTime; 
            }
        } else {
            lastFlush = storageStartTime;
            for (Long time : lastFlushTimes.values()) {
                lastFlush = Math.max(time, lastFlush);
            }
        }

        return lastFlush;
    }

    /**
     * Set the time stamp for the last time {@link #flush} or
     * {@link #flushAll} was called on something in {@code base}.
     *
     * @param base the base in which to register a change
     * @param timeStamp the new time stamp to set
     * @return the {@code timeStamp} argument
     */
    protected long setModificationTime (String base, long timeStamp) {
        lastFlushTimes.put (base, timeStamp);
        return timeStamp;
    }

    /**
     * Set the time stamp for the last time {@link #flush} or
     * {@link #flushAll} was called to {@code System.currentTimeMillis()}.
     *
     * @param base the base in which to register a change
     * @return the new time stamp
     */
    protected long updateModificationTime (String base) {
        return setModificationTime (base, System.currentTimeMillis());
    }

    /**
     * Default implementation that uses {@link #next(long)} to create the list
     * of records. As this happens server-side, this should be fast
     * enough.
     */
    public List<Record> next(long iteratorKey, int maxRecords) throws
                                                               IOException {
        List<Record> records = new ArrayList<Record>(maxRecords);
        int added = 0;
        while (added++ < maxRecords) {
            try {
                Record r = next(iteratorKey);
                records.add(r);
            } catch (NoSuchElementException e) {
                break;
            }

        }
        return records;
    }

    /**
     * Update all {@link Record}s related {@code record}, fixing parent/child
     * relationships and adding missing relations. This method does not
     * write the actual relations of {@code record} this is still the job of
     * {@link #flush}.
     * <p/>
     * This method is only a generic implementation and can likely be optimized
     * a great deal for a concrete storage implementation.
     *
     * @param record the record to update related records for
     * @throws java.io.IOException on communication errors with the db
     */
    protected void updateRelations(Record record) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("updateRelations("+record.getId()+")");
        }

        /* We collect a list of changes and submit them in one transaction */
        List<Record> flushQueue = new ArrayList<Record>(5);

        /* Make sure parent records are correct */
        if (record.getParentIds() != null) {
            List<Record> parents = getRecords(record.getParentIds(), null);

            /* If a record has any *existing* parents it is not indexable */
            if (parents != null && !parents.isEmpty()) {
                record.setIndexable(false);
            }

            /* Assert that the record is set as child on all existing parents */
            for (Record parent : parents) {
                List<String> parentChildren = parent.getChildIds();

                if (parentChildren == null) {
                    parent.setChildIds(Arrays.asList(record.getId()));
                    log.trace ("Creating child list '" + record.getId()
                               + "' on parent " + parent.getId());
                    flushQueue.add (parent);
                } else if (!parentChildren.contains(record.getId())) {
                    parentChildren.add (record.getId());
                    parent.setChildIds(parentChildren);
                    log.trace ("Adding child '" + record.getId()
                               + "' to parent " + parent.getId());
                    flushQueue.add (parent);
                }
            }
        }

        /* Make sure child records are correct */
        if (record.getChildIds() != null) {
            List<Record> children = getRecords(record.getChildIds(), null);

            /* Assert that the existing child records have this record set
             * as parent and that they are marked not indexable  */
            for (Record child : children) {
                List<String> childParents = child.getParentIds();

                if (childParents == null) {
                    child.setParentIds(Arrays.asList(record.getId()));
                    child.setIndexable(false);
                    log.trace ("Creating parent list '" + record.getId()
                               + " on child " + child.getId());
                    flushQueue.add(child);
                } else if (!childParents.contains(record.getId())) {
                    child.getParentIds().add(record.getId());
                    child.setIndexable(false);
                    log.trace ("Adding parent '" + record.getId()
                               + "' to child " + child.getId());
                    flushQueue.add(child);

                } else {
                    if (child.isIndexable()) {
                        log.debug ("Child '" + child.getId() + "' of '"
                                   + record.getId() + "' was marked as "
                                   + "indexable. Marking as not indexable");
                        child.setIndexable(false);
                    }
                }

            }
        }

        flushAll(flushQueue);

        /* Pseudo-code for new or modified (self = new or modified record):
        if parent exists
          mark self as not indexable
          add self to parent-children
          touch parent upwards (to trigger update)
        foreach child
          mark child as not indexable
          set self as child parent
        foreach child in each record
          if child equals self
            touch record

        Idea: Keep a cache of ghost-childs (child-id's without corresponding
              record) and their Record-id, for faster lookup.

        Pseudo-code for deleted:
          if parent exists
            touch parent upwards
          foreach child
            mark child as indexable
         */
    }

    /**
     * Convenience implementation of calling
     * {@link #flush(Record, QueryOptions)} with query options set to
     * {@code null}
     * @param record the record to flush
     * @throws IOException
     */
    public void flush(Record record) throws IOException {
        flush(record, null);
    }

    /**
     * <p>Convenience implementation of {@link WritableStorage#flushAll}
     * simply iterating through the list and calling
     * {@link WritableStorage#flush} on each record.</p>
     *
     * <p>Subclasses of {@code StorageBase} may choose to overwrite this method
     * for optimization purposes.</p>
     *
     * @param records the records to store or update
     * @param options the options to pass to {@link #flush}
     * @throws IOException on comminication errors
     */
    public void flushAll (List<Record> records, QueryOptions options)
                                                            throws IOException {
        for (Record rec : records) {
            flush(rec, options);
        }
    }

    /**
     * Convenience implementation of calling
     * {@link #flushAll(List<Record>, QueryOptions)} with query options set to
     * {@code null}
     * @param records the records to flush
     * @throws IOException
     */
    public void flushAll(List<Record> records) throws IOException {
        flushAll(records, null);
    }

    /**
     * Simple implementation of {@link ReadableStorage#getRecords} fetching
     * each record one at a time and collecting them in a list.
     */
    public List<Record> getRecords (List<String> ids, QueryOptions options)
                                                        throws IOException {
        long startTime = System.currentTimeMillis();
        ArrayList<Record> result = new ArrayList<Record>(ids.size());
        for (String id : ids) {
            Record r = getRecord(id, options);
            if (r != null) {
                result.add(r);
            }
        }
        if (log.isDebugEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.debug("Finished getRecords(" + ids.size()
                      + " ids, ...) in "
                      + (System.currentTimeMillis() - startTime) + "ms");
        }

        return result;
    }

    /**
     * Returns {@code true} if {@code id} matches the regular expression
     * {@code __.+__}, indicating that the id belongs to a private
     * record.
     * <p/>
     * Private records may only be retrieved if the {@code ALLOW_PRIVATE}
     * meta field is set on the query options passed to the storage when
     * calling {@link #getRecord}(s).
     * @param id
     * @return
     */
    protected boolean isPrivateId(String id) {
        return privateIdMatcher.reset(id).matches();
    }

    /**
     * Returns {@code true} if and only if the {@code ALLOW_PRIVATE} meta
     * field is set to the string {@code "true"} in {@code opts}.
     * @param opts the query options to check
     * @return {@code true} if {@code ALLOW_PRIVATE} is set to {@code "true"}
     *         in {@code opts}
     */
    protected boolean allowsPrivate(QueryOptions opts) {
        if (opts != null) {
            return "true".equals(opts.meta(ALLOW_PRIVATE));
        }
        return false;
    }
}



