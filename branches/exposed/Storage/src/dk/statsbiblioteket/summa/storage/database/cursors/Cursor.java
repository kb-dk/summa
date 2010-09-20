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
package dk.statsbiblioteket.summa.storage.database.cursors;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;

import java.util.Iterator;

/**
 * Convenience interface to access and manage SQL ResultSets
*/
public interface Cursor extends Iterator<Record> {

    /**
     * Release and close all resources held by this cursor
     */
    public void close();

    /**
     * Get the unique key identifying this cursor. Keys are used to track
     * results sets from processes outside the runing
     * {@link dk.statsbiblioteket.summa.storage.database.DatabaseStorage}
     * @return a {@code long} unique identifying this cursor
     */
    public long getKey();

    /**
     * Get the system time for the last access to this cursor. Ie. not the
     * actual timestamps of the underlying data, but the timestamp for when
     * a client interacted with this cursor.
     * <p/>
     * The typical use case for calling this is to time out abandoned cursors.
     * @return the system timestamp for the last access to this instance
     */
    public long getLastAccess();

    /**
     * The {@link QueryOptions} used for the query generating this cursor
     * @return the query options set when creating the cursor.
     *         Possibly {@code null}
     */
    public QueryOptions getQueryOptions();

    /**
     * The base this cursor iterates over.
     * @return The name of the base the generating query is on. Possibly
     *         {@code null} in case the query runs over all bases
     */
    public String getBase();
}

