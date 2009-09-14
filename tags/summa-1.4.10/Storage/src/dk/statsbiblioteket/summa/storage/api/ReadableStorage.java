/* $Id: ReadableStorage.java,v 1.7 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.7 $
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

import java.util.List;
import java.util.Iterator;
import java.io.IOException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configurable;

/**
 * One of the two interfaces comprising the core {@link Storage} interface in
 * Summa. The other interface is the {@link WritableStorage} interface.
 * <p/>
 * Iteration over the result sets returned by this interface is done via
 * keys (in the form of {@code long}s which are passed to the {@link #next(long)}
 * or {@link #next(long,int)} methods until a
 * {@link java.util.NoSuchElementException} is thrown. Alternatively one can
 * use the {@link StorageIterator} class to handle all this behind an
 * {@link Iterator} facade.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke")
public interface ReadableStorage extends Configurable {

    /**
     * Get an iterator over all records from the given base modified after the
     * given time. The iterator is sorted by timestamps and records with
     * identical timestamps are sorted by record id.
     * <p/>
     * If the base is null, all records for all bases are selected.
     * <p/>
     * The implementation of this method should be very light in the case there
     * are no updates as change notification services might poll the storage
     * using this service.
     * <p/>
     * For convenient iteration over the result set one can use a
     * {@link StorageIterator} on the returned iterator key.
     *
     * @param time a timestamp in milliseconds
     * @param base the name of the original record base or null if all bases are
     *             to be used.
     * @param options a possible {@code null} set of options to apply to the
     *                query. Please see the documentation for
     *                {@link QueryOptions} on how it is interpreted
     * @return an iterator key that can be used to iterate over all records
     *         modified after given time (sorted by record id)
     * @throws IOException on communication errors with the storage
     */
    long getRecordsModifiedAfter(long time, String base, QueryOptions options)
                                                             throws IOException;

    /**
     * Returns the timestamp for the most recent modification in {@code base}.
     * <p/>
     * Change notification services polling the storage for changes should use
     * this method.
     * <p/>
     * <i>Warning</i>: Callers of this method should never compare the
     * local system time with the returned timestamp.
     *
     * @param base the base in which to check for changes. If {@code base} is
     *             {@code null} the most recent change across all bases is
     *             returned
     * @return the timestamp for the last change in {@code base}. If
     *         {@code base} is unknown or has never received updates {@code 0}
     *         is returned.
     * @throws IOException on communication errors with the storage service
     */
    long getModificationTime (String base) throws IOException;

    /**
     * Get the records with the given ids. Child records will be expanded
     * recursively to a depth of {@code expansionDepth}
     * @param ids list of ids to fetch
     * @param options a possible {@code null} set of options to apply to the
     *                query. Please see the documentation for
     *                {@link QueryOptions} on how it is interpreted
     * @return a list of the requested Records. The list will be ordered
     *         arcording to the {@code ids} parameter. If one or more records
     *         can not be found they will be omitted from the list.
     * @throws IOException on communication errors with the storage
     */
    List<Record> getRecords(List<String> ids, QueryOptions options)
                                                             throws IOException;

    /**
     * Retrieve the record given by {@code id} expanding child records to a
     * depth of {@code expansionDepth}.
     * <p/>
     * If {@code expansionDepth == 0} no child expansion will occur, and a
     * depth of {@code -1} will do recursive expansion of all children.
     * @param id the id of the record to retrieve
     * @param options a possible {@code null} set of options to apply to the
     *                query. Please see the documentation for
     *                {@link QueryOptions} on how it is interpreted
     * @return the requested record or {@code null} if it wasn't found. Expanded
     *         child records can be retrieved with {@link Record#getChildren}
     * @throws IOException on communication errors
     */
    Record getRecord (String id, QueryOptions options) throws IOException;

    /**
     * Return the next record in the record iteration identified by the given
     * iterator key. This method should not be accessed by third parties.
     * <p/>
     * There is no {@code hasNext} method on the {{{ReadableStorage}}} interface
     * instead iterate until you receive a {{{NoSuchElementException}}}. This is
     * to enforce that bad clients don't do two remote calls per record.
     * 
     * @param iteratorKey iterator key
     * @return the next record in the iteration
     * @throws IOException if there was a problem requesting the record
     * @throws java.util.NoSuchElementException if there are no more records
     *                                          available for the iterator.
     *                                          In case this exception is thrown
     *                                          the iterator should not be
     *                                          accessed again
     * @throws IllegalArgumentException if {@code iteratorKey} is not known
     *                                  by the storage
     */
    Record next(long iteratorKey) throws IOException;

    /**
     * Return a maximum of maxRecords Records in the record iteration identified
     * by the given iterator key.
     * <p/>
     * There is no {@code hasNext} method on the {{{ReadableStorage}}} interface
     * instead iterate until you receive a {{{NoSuchElementException}}}. This is
     * to enforce that bad clients don't do two remote calls per record.
     * 
     * @param iteratorKey iterator key.
     * @param maxRecords  the maximum number of records to return. If there are
     *                    less records returned than {@code maxRecords} then the
     *                    iterator has been depleted
     * @return a list of {@link Record} objects
     * @throws java.util.NoSuchElementException if there are no more records
     *                                          available for the iterator.
     *                                          In case this exception is thrown
     *                                          the iterator should not be
     *                                          accessed again
     * @throws IOException if there was a problem requesting
     *                                  Records.
     * @throws IllegalArgumentException if {@code iteratorKey} is not known
     *                                  by the storage
     */
    List<Record> next(long iteratorKey, int maxRecords) throws IOException;

}



