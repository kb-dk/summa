/* $Id: RecordIterator.java,v 1.6 2007/10/05 10:20:22 te Exp $
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
package dk.statsbiblioteket.summa.storage.io;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A RecordIterator is a facade for an iterator over a collection of records.
 * The collection of records is a ResultSet (of rows in the database) saved in
 * an Access object and is accessible by the RecordIterator via its private key.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
       state = QAInfo.State.IN_DEVELOPMENT,
       author = "bam, hal, te")
public class RecordIterator implements Iterator<Record>, Serializable {
    // TODO: Consider making this adjustable
    public static int MAX_QUEUE_SIZE = 100;
    private final Access iteratorHolder;
    private final Long key;
    private final Queue<RecordAndNext> records;
    private boolean next;

    /**
     * RecordIterator Constructor.
     * The constructor for a RecordIterator object saves a private reference to
     * the Access object, which holds the ResultSet accessible by this
     * RecordIterator, and the key by which the ResultSet can be accessed, and
     * next is initialised for hasNext.
     * @param iteratorHolder the object which holds the iterator
     * @param key the key for the remote iterator
     * @param next the value for the first call to hasNext()
     */
    public RecordIterator(Access iteratorHolder, Long key, boolean next) {
        this.iteratorHolder = iteratorHolder;
        this.key = key;
        this.next = next;
        records = new LinkedBlockingQueue<RecordAndNext>(MAX_QUEUE_SIZE);
    }

    /**
     * Returns true if the iteration can be accessed and has more Records.
     * @return true if the iterator has more (accessible) Records
     */
    public boolean hasNext() {
        return next || records.size() > 0;
    }

    /**
     * Returns the next Record in the iteration.
     * Calling this method repeatedly until the hasNext() method returns false
     * will return each Record in the underlying collection (ResultSet) exactly
     * once and in the underlying order.
     * @return the next Record in the iteration
     * @throws NoSuchElementException - iteration has no more elements;
     *                                  or cannot be accessed
     */
    public Record next() {
        try {
            if (records.size() == 0) {
                records.addAll(iteratorHolder.next(key, MAX_QUEUE_SIZE));
            }
            RecordAndNext ran = records.poll();
            Record rec = ran.getRecord();
            next = ran.getNext();
            return rec;
        } catch (RemoteException e) {
            throw new NoSuchElementException("Could not get next Record for "
                                             + "key '" + key + "': "
                                             + e.getMessage());
            //note: next() cannot throw a RemoteException (by the Iterator
            // interface definition);
            //it can throw a NoSuchElementException or return null...
        }
    }

    /**
     * @throws UnsupportedOperationException the remove operation is not
     *                                       supported by this Iterator
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
