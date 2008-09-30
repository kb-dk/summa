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

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public interface ReadableStorage {
    /**
     * Get an iterator over all records in the database from the given base sorted by name.
     * @param base the name of the original record base
     * @return a iterator over all records (sorted by name)
     * @throws IOException
     */
    Iterator<Record> getRecordsFromBase(String base) throws IOException;

    /**
     * Get an iterator over all records from the given base modified after the given time.
     * The iterator is sorted by name.
     * @param time a timestamp in milliseconds
     * @param base the name of the original record base
     * @return an iterator over all records modified after given time (sorted by name)
     * @throws IOException
     */
    Iterator<Record> getRecordsModifiedAfter(long time, String base) throws IOException;

    /**
     * Get an iterator over all records from the given base "from" the given id.
     * I.e. get all records with id "larger than" the given id.
     * Would we prefer "larger than or equal to"?
     * The iterator is sorted by id.
     * @param id record id (bib#/id)
     * @param base the name of the original record base
     * @return an iterator over records "from" the given id (sorted by id)
     * @throws IOException
     */
    Iterator<Record> getRecordsFrom(String id, String base) throws IOException;

    /**
     * Get the records with the given ids. Child records will be expanded
     * recursively to a depth of {@code expansionDepth}
     * @param ids list of ids to fetch
     * @param expansionDepth the level of recursion any child records should
     *        be expanded to. 0 indicates that only the flat records should be
     *        returned, -1 that all child records should be expanded
     * @return a list of the requested Records. The list will be ordered
     *         arcording to the {@code ids} parameter. If one or more records
     *         can not be found they will be omitted from the list.
     * @throws IOException on communication errors with the storage
     */
    List<Record> getRecords(List<String> ids, int expansionDepth)
                                                             throws IOException;

    /**
     * Return the next record in the record iteration identified by the given
     * iterator key. This method should not be accessed by third parties.
     * <p/>
     * There is no {@code hasNext} method on the {{{ReadableStorage}}} interface
     * instead iterate until you receive a {{{NoSuchElementException}}}. This is
     * to enforce that bad clients don't do two remote calls per record.
     * @param iteratorKey iterator key
     * @return the next record in the iteration
     * @throws IOException if there was a problem requesting the record
     * @throws java.util.NoSuchElementException if there are no more records
     *                                          available for the iterator.
     *                                          In case this exception is thrown
     *                                          the iterator should not be
     *                                          accessed again
     */
    Record next(Long iteratorKey) throws IOException;

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
     */
    List<Record> next(Long iteratorKey, int maxRecords) throws IOException;

}



