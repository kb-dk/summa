/* $Id: StorageBase.java,v 1.9 2007/12/04 09:08:19 te Exp $
 * $Revision: 1.9 $
 * $Date: 2007/12/04 09:08:19 $
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
package dk.statsbiblioteket.summa.storage;

import java.rmi.RemoteException;
import java.util.*;
import java.io.IOException;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.WritableStorage;
import dk.statsbiblioteket.summa.storage.api.ReadableStorage;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    private long storageStartTime;
    private HashMap<String,Long> lastFlushTimes;

    public StorageBase(Configuration conf) {
        storageStartTime = System.currentTimeMillis();
        lastFlushTimes = new HashMap<String,Long>(10);        
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
            List<Record> parents = getRecords(record.getParentIds(), 0);

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
            List<Record> children = getRecords(record.getChildIds(), 0);

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
     * <p>Convenience implementation of {@link WritableStorage#flushAll}
     * simply iterating through the list and calling
     * {@link WritableStorage#flush} on each record.</p>
     *
     * <p>Subclasses of {@code StorageBase} may choose to overwrite this method
     * for optimization purposes.</p>
     *
     * @param records the records to store or update
     * @throws RemoteException on comminication errors
     */
    public void flushAll (List<Record> records) throws IOException {
        for (Record rec : records) {
            flush(rec);
        }
    }

    /**
     * Simple implementation of {@link ReadableStorage#getRecords} fetching
     * each record one at a time and collecting them in a list.
     */
    public List<Record> getRecords (List<String> ids, int expansionDepth)
                                                        throws IOException {
        ArrayList<Record> result = new ArrayList<Record>(ids.size());
        for (String id : ids) {
            Record r = getRecord(id, expansionDepth);
            if (r != null) {
                result.add(r);
            }
        }

        return result;
    }
    
}



