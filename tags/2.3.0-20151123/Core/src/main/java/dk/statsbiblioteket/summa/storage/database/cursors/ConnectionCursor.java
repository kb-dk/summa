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
import dk.statsbiblioteket.util.qa.QAInfo;

import java.sql.Connection;

/**
 * Simple Cursor-wrapper than also keeps track of a connection (normally the one used by the Cursor).
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")

public class ConnectionCursor implements Cursor {
    private final Cursor inner;
    private final Connection connection;

    public ConnectionCursor(Cursor inner, Connection connection) {
        this.inner = inner;
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }

    public Cursor getInnerCursor() {
        return inner;
    }

    /* Delegates below */

    @Override
    public void close() {
        inner.close();
    }

    @Override
    public long getKey() {
        return inner.getKey();
    }

    @Override
    public long getLastAccess() {
        return inner.getLastAccess();
    }

    @Override
    public QueryOptions getQueryOptions() {
        return inner.getQueryOptions();
    }

    @Override
    public String getBase() {
        return inner.getBase();
    }

    @Override
    public boolean hasNext() {
        return inner.hasNext();
    }

    @Override
    public Record next() {
        return inner.next();
    }

    @Override
    public void remove() {
        inner.remove();
    }
}
