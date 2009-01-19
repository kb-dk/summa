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

    private long key;
    private long lastAccess;
    private long nextMtimeTimestamp;
    private long currentMtimeTimestamp;
    private PreparedStatement stmt;
    protected String base;
    private ResultSet resultSet;
    private Record nextRecord;
    private QueryOptions options;
    private boolean resultSetHasNext;
    private DatabaseStorage db;

    public ResultSetCursor(DatabaseStorage db,
                           PreparedStatement stmt,
                           ResultSet resultSet)
                                           throws SQLException, IOException {
        this(db, stmt, resultSet, null, null);
    }

    public ResultSetCursor(DatabaseStorage db,
                           PreparedStatement stmt,
                           ResultSet resultSet,
                           String base,
                           QueryOptions options)
                                          throws SQLException, IOException {
        this.db = db;
        this.stmt = stmt;
        this.base = base;
        this.resultSet = resultSet;
        this.options = options;

        // The iterator start "outside" the result set, so step into it
        resultSetHasNext = resultSet.next();

        // This also updates resultSetHasNext
        nextRecord = nextValidRecord();

        log.trace("Constructed Record iterator with initial hasNext: "
                  + resultSetHasNext + ", on base " + base);

        // The generated timestamps a guranteed to be unique, so no
        // iterator key clashes even within the same millisecond
        key = db.getTimestampGenerator().next();

        // Extract the system time from when we generated the iterator key
        lastAccess = db.getTimestampGenerator().systemTime(key);
    }

    public long getLastAccess() {
        return lastAccess;
    }

    public QueryOptions getQueryOptions () {
        return options;
    }

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
        return null;
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

            log.trace("Closing " + this);
            resultSet.close();
            stmt.close();
        } catch (Exception e) {
            log.warn("Failed to close iterator statement " + stmt + ": "
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
