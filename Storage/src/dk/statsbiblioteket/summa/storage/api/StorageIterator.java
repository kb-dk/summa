/* $Id: StorageIterator.java,v 1.6 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.6 $
 * $Date: 2007/10/05 10:20:22 $
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
package dk.statsbiblioteket.summa.storage.api;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.Serializable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Convenience iterator implementation masking the manual storage interactions
 * of the key-based iteration of the {@link ReadableStorage#next} methods.
 * <p/>
 * This class takes care to optimize the network overhead by only requesting
 * batches of records via {@link ReadableStorage#next(long, int)} instead of
 * fetching them one by one.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
       state = QAInfo.State.IN_DEVELOPMENT,
       author = "mke")
public class StorageIterator implements Iterator<Record>, Serializable {
    // TODO: Consider making this adjustable
    public static int MAX_QUEUE_SIZE = 100;
    private final ReadableStorage iteratorHolder;
    private final long key;
    private final Queue<Record> records;
    private boolean next;

    private Log log;

    /**
     * Create an iterator on a given storage and iteration key (as returned
     * by one of the getters on the {@link ReadableStorage} interface)
     *
     * @param iteratorHolder the storage holding the iterator key {@code key}
     * @param key the iteration key as returned from the {@link ReadableStorage}
     */
    public StorageIterator(ReadableStorage iteratorHolder, long key) {
        log = LogFactory.getLog (this.getClass().getName());
        this.iteratorHolder = iteratorHolder;
        this.key = key;
        this.next = true;
        records = new LinkedBlockingQueue<Record>(MAX_QUEUE_SIZE);
    }

    public boolean hasNext() {
        try {
            checkRecords();
        } catch (IOException e) {
            log.warn ("Failed to retrieve records: " + e.getMessage(), e);
            next = false;
        }

        return next || records.size() > 0;
    }

    public Record next() {
        if (!hasNext()) {
            throw new NoSuchElementException ("Depleted");
        }

        return records.poll();

    }

    /**
     * Not supported
     * @throws UnsupportedOperationException the remove operation is not
     *                                       supported by this Iterator
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Download next batch of records if applicable and set the 'next' state
     * appropriately
     */
    private void checkRecords () throws IOException {
        if (records.size() == 0 && next) {
            try {
                List<Record> recs = iteratorHolder.next(key, MAX_QUEUE_SIZE);

                if (recs.size() < MAX_QUEUE_SIZE) {
                    next = false;
                }
                records.addAll(recs);

            } catch (NoSuchElementException e) {
                next = false;
            }
        }
    }
}



