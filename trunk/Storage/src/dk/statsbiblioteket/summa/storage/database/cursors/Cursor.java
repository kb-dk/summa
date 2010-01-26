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
    public void close();

    public long getKey();

    public long getLastAccess();

    public QueryOptions getQueryOptions();

    public String getBase();
}

