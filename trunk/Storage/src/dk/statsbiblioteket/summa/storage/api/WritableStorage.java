/* $Id: WritableStorage.java,v 1.6 2007/10/05 10:20:22 te Exp $
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
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;
import java.util.List;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public interface WritableStorage extends Configurable {

    /**
     * Flush a record to the storage. In other words write it.
     * <p/>
     * Any nested child records (added via {@link Record#setChildren}) will
     * be added recursively to the storage.
     *
     * @param record The record to store or update
     * @throws IOException on comminication errors
     */
    void flush(Record record) throws IOException;

    /**
     * A batch optimized version of {@link #flush}. Use this method
     * to optimize IPC overhead.
     * <p/>
     * Just like {@link #flush} any child records added with
     * {@link Record#setChildren} will be added recursively to the storage. 
     *
     * @param records a list of records to store or update
     * @throws IOException on communication errors
     */
    void flushAll(List<Record> records) throws IOException;

    /**
     * Close the storage for any further reads or writes.
     * When this method returns it should be safe to turn of the JVM in which
     * storage runs.
     *
     * @throws IOException on communication errors
     */
    void close() throws IOException;

    /**
     * Mark all records in a given base as deleted.
     *
     * @param base the name of the base to clear
     * @throws IOException on communication errors with the storage
     */
    void clearBase (String base) throws IOException;
}



