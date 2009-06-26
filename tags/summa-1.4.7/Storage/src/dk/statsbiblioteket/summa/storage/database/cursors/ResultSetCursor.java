package dk.statsbiblioteket.summa.storage.database.cursors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.UniqueTimestampGenerator;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.common.Record;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.IOException;
import java.util.NoSuchElementException;

/**
 *
 */
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
     * Create a new non-anonymous cursor with {@code null} base and query options
     */
    public ResultSetCursor(DatabaseStorage db,
                           PreparedStatement stmt,
                           ResultSet resultSet)
                                               throws SQLException, IOException{
        this(db, stmt, resultSet, null, null, false);
    }

    /**
     * Create a new cursor with {@code null} base and query options
     */
    public ResultSetCursor(DatabaseStorage db,
                           PreparedStatement stmt,
                           ResultSet resultSet,
                           boolean anonymous)
                                           throws SQLException, IOException {
        this(db, stmt, resultSet, null, null, anonymous);
    }

    /**
     * Create a new non-anonymous cursor
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
     * Create a new cursor.
     * @param db the DatabaseStorage owning the cursor
     * @param stmt the statement which produced {@code resultSet}
     * @param resultSet the ResultSet to read records from
     * @param base the Record base the cursor is iterating over. Possibly
     *             {@code null} if the base is undefined
     * @param options any query options the records must match
     * @param anonymous anonymous cursors does less logging
     * @throws SQLException on any SQLException reading the result set
     * @throws IOException on any IOExceptions reading records
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
            // The generated timestamps a guranteed to be unique, so no
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
    }

    public long getLastAccess() {
        return lastAccess;
    }

    public QueryOptions getQueryOptions () {
        return options;
    }

    /**
     * Return the globally unique key for this cursor. If it was created
     * with {@code anonymous=true} then the key will not be unique, but always
     * be {@code 0}.
     * @return {@code 0} if the cursor is anonymous. Otherwise the cursor's
     *         globally unique key will be returned
     */
    public long getKey() {
        lastAccess = System.currentTimeMillis();
        return key;
    }

    public String getBase() {
        return base;
    }

    public boolean hasNext() {
        lastAccess = System.currentTimeMillis();
        return nextRecord != null;
    }

    /**
     * Constructs a Record based on the row in the result set, then advances
     * to the next record.
     * @return a Record based on the current row in the result set.
     * @throws java.sql.SQLException if the record could not be requested.
     * @throws java.io.IOException if the Record data could not be gunzipped.
     */
    public Record next () {
        lastAccess = System.currentTimeMillis();

        if (nextRecord == null) {
            throw new NoSuchElementException("Iterator " + key
                                             + " depleted");
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

    public void remove () {
        throw new UnsupportedOperationException();
    }

    private Record nextValidRecord () throws SQLException, IOException {
        // This check should _not_ use the method resultSetHasNext() because
        // it is only the state of the resultSet that is important here
        if (!resultSetHasNext) {
            logDepletedStats();
            return null;
        }

        /* scanRecord() steps the resultSet to the next record.
         * It will update the state of the iterator appropriately */
        Record r = db.scanRecord(resultSet, this);

        // Allow all mode
        if (options == null) {
            return r;
        }

        while (resultSetHasNext && !options.allowsRecord(r)) {
            r = db.scanRecord(resultSet, this);
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

    private void logDepletedStats () {
        // Only log stats if this is a non-anonymous cursor
        if (key != 0) {
            log.debug(this + " depleted. After " + totalRecords + " records and "
                      + (lastAccess - firstAccess) + "ms");
        }
    }

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
     * @param resultSetHasNext
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
     * @param mtimeTimestamp a timestamp in the binary format of a
     *                       {@link UniqueTimestampGenerator}
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
     * primary intent is to use this unique timestamp for {@link PagingCursor}s.
      * @return the raw, unique, binary timestamp of the last record returned by
     *          this cursor
     */
    public long currentMtimeTimestamp() {
        return currentMtimeTimestamp;
    }

    public String toString() {
        return "ResultSetCursor[" + key + "]";
    }
}
