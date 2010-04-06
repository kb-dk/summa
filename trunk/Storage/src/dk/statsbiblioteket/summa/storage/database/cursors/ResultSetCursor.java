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

import dk.statsbiblioteket.summa.common.util.StringMap;
import dk.statsbiblioteket.summa.common.util.UniqueTimestampGenerator;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.common.Record;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * Wraps a ResultSet and the context in was created in as a {@link Cursor}
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke",
        reviewers = "hbk")
public class ResultSetCursor implements Cursor {
    private Log log = LogFactory.getLog(ResultSetCursor.class);

    private long firstAccess;
    private long lastAccess;
    private long totalRecords;

    private long key;    // key is 0 for anonymous cursors
    private long nextMtimeTimestamp;
    private long currentMtimeTimestamp;
    private PreparedStatement stmt;
    protected String base;
    private ResultSet resultSet;
    private Record nextRecord;
    private QueryOptions options;
    private boolean resultSetHasNext;
    private DatabaseStorage db;

    /**
     * Create a new non-anonymous cursor with {@code null} base and query
     * options.
     *
     * @param db The DatabaseStorage owning the cursor.
     * @param stmt The statement which produced {@code resultSet}.
     * @param resultSet The ResultSet to read records from.
     * @throws SQLException on any SQLException reading the result set.
     * @throws IOException on any IOExceptions reading records.
     */
    public ResultSetCursor(DatabaseStorage db,
                           PreparedStatement stmt,
                           ResultSet resultSet)
                                               throws SQLException, IOException{
        this(db, stmt, resultSet, null, null, false);
    }

    /**
     * Create a new possibly anonymous cursor with {@code null} base and query
     * options.
     *
     * @param db The DatabaseStorage owning the cursor.
     * @param stmt The statement which produced {@code resultSet}.
     * @param resultSet The ResultSet to read records from.
     * @param anonymous Anonymous cursors does less logging. They are suitable
     *                  for short lived, and intermediate, result sets.
     * @throws SQLException on any SQLException reading the result set.
     * @throws IOException on any IOExceptions reading records.
     */
    public ResultSetCursor(DatabaseStorage db,
                           PreparedStatement stmt,
                           ResultSet resultSet,
                           boolean anonymous)
                                           throws SQLException, IOException {
        this(db, stmt, resultSet, null, null, anonymous);
    }

    /**
     * Create a new non-anonymous cursor.
     *
     * @param db The DatabaseStorage owning the cursor.
     * @param stmt The statement which produced {@code resultSet}.
     * @param resultSet The ResultSet to read records from.
     * @param base The Record base the cursor is iterating over. Possibly
     *             {@code null} if the base is undefined.
     * @param options Any query options the records must match.
     * @throws SQLException on any SQLException reading the result set.
     * @throws IOException on any IOExceptions reading records.
     */
    public ResultSetCursor(DatabaseStorage db,
                           PreparedStatement stmt,
                           ResultSet resultSet,
                           String base,
                           QueryOptions options)
                                              throws SQLException, IOException {
        this(db, stmt, resultSet, base, options, false);
    }

    /**
     * Create a new possible anonymous cursor.
     *
     * @param db The DatabaseStorage owning the cursor.
     * @param stmt The statement which produced {@code resultSet}.
     * @param resultSet The ResultSet to read records from.
     * @param base The Record base the cursor is iterating over. Possibly
     *             {@code null} if the base is undefined.
     * @param options Any query options the records must match.
     * @param anonymous Anonymous cursors does less logging. They are suitable
     *                  for short lived, and intermediate, result sets.
     * @throws SQLException on any SQLException reading the result set.
     * @throws IOException on any IOExceptions reading records.
     */
    public ResultSetCursor(DatabaseStorage db,
                           PreparedStatement stmt,
                           ResultSet resultSet,
                           String base,
                           QueryOptions options,
                           boolean anonymous)
                                          throws SQLException, IOException {
        this.db = db;
        this.stmt = stmt;
        this.base = base;
        this.resultSet = resultSet;
        this.options = options;

        // The cursor start "outside" the result set, so step into it
        resultSetHasNext = resultSet.next();

        // This also updates resultSetHasNext
        nextRecord = nextValidRecord();

        if (anonymous) {
            key = 0;
        } else {
            // The generated timestamps a guaranteed to be unique, so no
            // cursor key clashes even within the same millisecond
            key = db.getTimestampGenerator().next();
        }

        // Extract the system time from when we generated the cursor key
        lastAccess = db.getTimestampGenerator().systemTime(key);
        firstAccess = 0;
        totalRecords = 0;

        if (!anonymous) {
            log.trace("Constructed with initial hasNext: "
                      + resultSetHasNext + ", on base " + base);
        }
        log.debug("Constructed with initial hasNext: "
                      + resultSetHasNext + ", nextValidRecord: '"
                      + nextRecord + "', on base " + base);
    }

    /**
     * Getter for last access time.
     *
     * @return last access time.
     */
    public long getLastAccess() {
        return lastAccess;
    }

    /**
     * Getter for query options.
     *
     * @return query options.
     */
    public QueryOptions getQueryOptions () {
        return options;
    }

    /**
     * Return the globally unique key for this cursor. If it was created
     * with {@code anonymous=true} then the key will not be unique, but always
     * be {@code 0}.
     *
     * @return {@code 0} if the cursor is anonymous. Otherwise the cursor's
     *         globally unique key will be returned.
     */
    public long getKey() {
        lastAccess = System.currentTimeMillis();
        return key;
    }

    /**
     * Getter for base.
     *
     * @return the base name.
     */
    public String getBase() {
        return base;
    }

    /**
     * Return true if this iterator has a next item.
     * Note: Side-effect updated lastAccess time.
     *
     * @return true if it has next item.
     */
    public boolean hasNext() {
        lastAccess = System.currentTimeMillis();
        return nextRecord != null;
    }

    /**
     * Constructs a Record based on the row in the result set, then advances
     * to the next record.
     *
     * @return a Record based on the current row in the result set.
     */
    public Record next () {
        lastAccess = System.currentTimeMillis();

        if (nextRecord == null) {
            throw new NoSuchElementException("Iterator " + key + " depleted");
        }

        if (totalRecords == 0) {
            firstAccess = lastAccess; // Set firstAccess to 'now'
        }
        totalRecords++;


        Record record = nextRecord;
        currentMtimeTimestamp = nextMtimeTimestamp;

        try {
            nextRecord = nextValidRecord();
        } catch (SQLException e) {
            log.warn("Error reading next record: " + e.getMessage(), e);
            nextRecord = null;
        } catch (IOException e) {
            log.warn("Error reading next record: " + e.getMessage(), e);
            nextRecord = null;
        }

        if (log.isTraceEnabled()) {
            log.trace("next returning '" + record.getId() + "'");
        }

        /* The naive solution to updating resultSetHasNext is to just do:
         *   resultSetHasNext = !resultSet.isAfterLast();
         * here, but the ResultSet type does not allow this unless it is
         * of a scrollable type, which we do not use for resource limitation
         * purposes. Instead the resultSetHasNext state is updated in
         * scanRecord()
         */

        return record;
    }

    /**
     * Remove first element, by calling next() and throwing the returned record
     * away.
     */
    public void remove () {
        if(hasNext()) {
            next();
        }
    }

    /**
     * Return next valid record from the iterator.
     *
     * @return next valid record.
     * @throws SQLException if database error occurred while fetching record.
     * @throws IOException if error occurred while fetching record.
     */
    private Record nextValidRecord () throws SQLException, IOException {
        /*This check should _not_ use the method resultSetHasNext() because
          it is only the state of the resultSet that is important here. */
        if (!resultSetHasNext) {
            logDepletedStats();
            return null;
        }

        /* scanRecord() steps the resultSet to the next record.
         * It will update the state of the iterator appropriately. */
        Record r = db.scanRecord(resultSet, this);

        // Allow all mode
        if (options == null) {
            return r;
        }

        while (resultSetHasNext && !options.allowsRecord(r)) {
            r = db.scanRecord(resultSet, this);
        }

        // We don't need all information from a record.
        if(options.newRecordNeeded()) {
            r = options.getNewRecord(r);
        }

        if (options.allowsRecord(r)) {
            return r;
        }

        // If we end here there are no more records and the one we have
        // is not OK by the options
        resultSetHasNext = false;
        logDepletedStats();
        return null;
    }

    /**
     * Log depleted stats to debug.
     */
    private void logDepletedStats () {
        // Only log stats if this is a non-anonymous cursor
        if (key != 0) {
            log.debug(this + " depleted. After " + totalRecords
                    + " records and " + (lastAccess - firstAccess) + "ms");
        }
    }

    /**
     * Closes result set cursor.
     */
    public void close() {
        try {
            if (stmt.isClosed()) {
                if (log.isTraceEnabled()) {
                    log.trace("Ignoring close request on iterator "
                              + this + ". Already closed");
                }
                return;
            }

            if (key != 0) {
                log.trace("Closing " + this);
            }

            resultSet.close();
            stmt.close();
        } catch (Exception e) {
            log.warn("Failed to close cursor statement " + stmt + ": "
                     + e.getMessage(), e);
        }
    }

    /**
     * Called from
     * {@link DatabaseStorage#scanRecord(java.sql.ResultSet, ResultSetCursor)}.
     *
     * @param resultSetHasNext the new value.
     */
    public void setResultSetHasNext(boolean resultSetHasNext) {
        this.resultSetHasNext = resultSetHasNext;
    }

    /**
     * Called from
     * {@link DatabaseStorage#scanRecord(java.sql.ResultSet, ResultSetCursor)}
     * with the raw mtime timestamp for the current Record as generated by
     * the {@link UniqueTimestampGenerator} of the {@link DatabaseStorage}.
     * <p/>
     * To make it clear; the timestamp set here is not the same as the Record's
     * modification time, but a unique timestamp that can be used by paging
     * models as offset for subsequent queries.
     *
     * @param mtimeTimestamp a timestamp in the binary format of a
     *                                         {@link UniqueTimestampGenerator}.
     */
    public void setRecordMtimeTimestamp(long mtimeTimestamp) {
        nextMtimeTimestamp = mtimeTimestamp;
    }

    /**
     * Return the timestamp for the modification time for the last
     * {@link Record} retrieved by calling {@link #next()}.
     * <p/>
     * The timestamp is <i>not</i> a standard system time value, but in the
     * binary format as generated by a {@link UniqueTimestampGenerator}. The
     * primary intent is to use this unique timestamp for {@link PagingCursor}.
     * 
     * @return the raw, unique, binary timestamp of the last record returned by
     *         this cursor.
     */
    public long currentMtimeTimestamp() {
        return currentMtimeTimestamp;
    }

    /**
     * Returns a result set cursor string, which are unique, and in the format
     * 'ResultSetCursor[unique key]'.
     *
     * @return unique string defining this object.
     */
    public String toString() {
        return "ResultSetCursor[" + key + "]";
    }
}

