/* $Id: DatabaseControl.java,v 1.8 2007/12/04 09:08:19 te Exp $
 * $Revision: 1.8 $
 * $Date: 2007/12/04 09:08:19 $
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
/**
 * Created: te 2007-09-10 10:54:43
 * CVS:     $Id: DatabaseControl.java,v 1.8 2007/12/04 09:08:19 te Exp $
 */
package dk.statsbiblioteket.summa.storage.database;

import dk.statsbiblioteket.summa.storage.io.Control;
import dk.statsbiblioteket.summa.storage.io.RecordIterator;
import dk.statsbiblioteket.summa.storage.io.RecordAndNext;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.GZIP.GZIPUtils;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.RemoteException;
import java.sql.*;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An abstract implementation of a SQL-oriented database driven implementation
 * of Control.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class DatabaseControl extends Control {
    private static Log log = LogFactory.getLog(DatabaseControl.class);

    public static String PROP_USERNAME  = "summa.storage.database.username";
    public static String PROP_PASSWORD  = "summa.storage.database.password";
    public static String PROP_CREATENEW = "summa.storage.database.createnew";
    public static String PROP_LOCATION  = "summa.storage.database.location";

    public static final String TABLE            = "summa_io";
    public static final String ID_COLUMN        = "id";
    public static final String BASE_COLUMN      = "base";
    public static final String DELETED_COLUMN   = "deleted";
    public static final String INDEXABLE_COLUMN = "indexable";
    public static final String DATA_COLUMN      = "data";
    public static final String CTIME_COLUMN     = "ctime";
    public static final String MTIME_COLUMN     = "mtime";
    public static final String PARENT_COLUMN    = "parent";
    public static final String CHILDREN_COLUMN  = "children";
    public static final String VALID_COLUMN     = "valid";

    public static final int ID_LIMIT =       255;
    public static final int BASE_LIMIT =     31;
    public static final int DATA_LIMIT =     50*1024*1024;
    public static final int PARENT_LIMIT =   10*ID_LIMIT; // Room for the future
    public static final int CHILDREN_LIMIT = 1000*ID_LIMIT;
    public static final int VALID_LIMIT =    20;
    private static final int BLOB_MAX_SIZE = 50*1024*1024; // MAX_VALUE instead?

    private PreparedStatement stmtGetAll;
    private PreparedStatement stmtGetFromBase;
    private PreparedStatement stmtGetModifiedAfter;
    private PreparedStatement stmtGetFrom;
    private PreparedStatement stmtGetRecord;
    private PreparedStatement stmtDeleteRecord;
    private PreparedStatement stmtCreateRecord;
    private PreparedStatement stmtUpdateRecord;
    private PreparedStatement stmtCreateTable;

    private static final int FETCH_SIZE = 10000;

    private Map<Long, ResultIterator> iterators =
            new HashMap<Long, ResultIterator>(10);

    public DatabaseControl() throws RemoteException {
        // We need to define this to declare RemoteException
    }

    /**
     * The initializer connects to the database and prepares SQL statements.
     * It is recommended that this is called by all constructors.
     * @param configuration    the setup for the database.
     * @throws RemoteException if the initialization could not finish.
     */
    protected void init(Configuration configuration) throws RemoteException {
        log.info("Initializing ControlDerby");
        connectToDatabase(configuration);
        try {
            prepareStatements();
        } catch (SQLException e) {
            throw new RemoteException("SQLException in init", e);
        }
        log.debug("Initialization finished");
    }

    /**
     * Connect to the relevant database and establish a connection, so that
     * calls to {@link #getConnection()} can be performed. Depending on the
     * configuration, this might involve creating a table in the database and
     * initializing that to Summa-use.
     * @param configuration setup for the database.
     */
    protected abstract void connectToDatabase(Configuration configuration)
                                                        throws RemoteException;

    /**
     * @return a connection to a SQL-compatible database.
     */
    protected abstract Connection getConnection();

    /**
     * Prepare relevant SQL statements for later use.
     * @throws SQLException if the syntax of a statement was wrong or the
     *                      connection to the database was faulty.
     */
    private void prepareStatements() throws SQLException {
        log.debug("Preparing SQL statements");
        String allCells = TABLE + "." + ID_COLUMN + ","
                          + TABLE + "." + BASE_COLUMN + ","
                          + TABLE + "." + DELETED_COLUMN + ","
                          + TABLE + "." + INDEXABLE_COLUMN + ","
                          + TABLE + "." + DATA_COLUMN + ","
                          + TABLE + "." + CTIME_COLUMN + ","
                          + TABLE + "." + MTIME_COLUMN + ","
                          + TABLE + "." + PARENT_COLUMN + ","
                          + TABLE + "." + CHILDREN_COLUMN + ","
                          + TABLE + "." + VALID_COLUMN;

        String allQuery = "SELECT " + allCells
                          + " FROM " + TABLE
                          + " ORDER BY " + ID_COLUMN;
        stmtGetAll = getConnection().prepareStatement(allQuery);

        String fromBaseQuery = "SELECT " + allCells
                               + " FROM " + TABLE
                               + " WHERE " + BASE_COLUMN + "=?"
                               + " ORDER BY " + ID_COLUMN;
        stmtGetFromBase = getConnection().prepareStatement(fromBaseQuery);

        String modifiedAfterQuery = "SELECT " + allCells
                                    + " FROM " + TABLE
                                    + " WHERE " + BASE_COLUMN + "=?"
                                    + " AND " + MTIME_COLUMN + ">?"
                                    + " ORDER BY " + ID_COLUMN;
        stmtGetModifiedAfter = getConnection().prepareStatement(modifiedAfterQuery);

        String fromQuery = "SELECT " + allCells
                           + " FROM " + TABLE
                           + " WHERE " + BASE_COLUMN + "=?"
                           + " AND " + ID_COLUMN + ">=?"
                           + " ORDER BY " + ID_COLUMN;
        stmtGetFrom = getConnection().prepareStatement(fromQuery);

        String getRecordQuery = "SELECT " + allCells
                                + " FROM " + TABLE
                                + " WHERE " + ID_COLUMN + "=?";
        stmtGetRecord = getConnection().prepareStatement(getRecordQuery);

        String deleteRecordQuery = "UPDATE " + TABLE
                                   + " SET " + MTIME_COLUMN + "=?, "
                                   + DELETED_COLUMN + "=?"
                                   + " WHERE " + ID_COLUMN + "=?";
        stmtDeleteRecord = getConnection().prepareStatement(deleteRecordQuery);

        String createRecordQuery = "INSERT INTO " + TABLE
                                + " (" + ID_COLUMN + ","
                                       + BASE_COLUMN + ","
                                       + DELETED_COLUMN + ","
                                       + INDEXABLE_COLUMN + ","
                                       + DATA_COLUMN + ","
                                       + CTIME_COLUMN + ","
                                       + MTIME_COLUMN + ","
                                       + PARENT_COLUMN + ","
                                       + CHILDREN_COLUMN + ","
                                       + VALID_COLUMN
                                       + ") VALUES (?,?,?,?,?,?,?,?,?, ?)";
        stmtCreateRecord = getConnection().prepareStatement(createRecordQuery);

        String updateRecordQuery = "UPDATE " + TABLE + " SET "
                                   + BASE_COLUMN + "=?, "
                                   + DELETED_COLUMN + "=?, "
                                   + INDEXABLE_COLUMN + "=?, "
                                   + DATA_COLUMN + "=?, "
                                   + MTIME_COLUMN + "=?, "
                                   + PARENT_COLUMN + "=?, "
                                   + CHILDREN_COLUMN + "=? "
                                   + VALID_COLUMN + "=? "
                                   + "WHERE " + ID_COLUMN +"=?";
        stmtUpdateRecord = getConnection().prepareStatement(updateRecordQuery);

        String touchRecordQuery = "UPDATE " + TABLE + " SET "
                                   + MTIME_COLUMN + "=?, "
                                   + "WHERE " + ID_COLUMN +"=?";
        stmtUpdateRecord = prepareStatement(touchRecordQuery);

        String createTableQuery =
                "CREATE table " + TABLE + " ("
                + ID_COLUMN        + " VARCHAR(" + ID_LIMIT + ") PRIMARY KEY, "
                + BASE_COLUMN      + " VARCHAR(" + BASE_LIMIT + "), "
                + DELETED_COLUMN   + " BOOLEAN NOT NULL, "
                + INDEXABLE_COLUMN + " BOOLEAN NOT NULL, "
                + DATA_COLUMN      + " BLOB(" + BLOB_MAX_SIZE + "), "
                + CTIME_COLUMN     + " TIMESTAMP, "
                + MTIME_COLUMN     + " TIMESTAMP, "
                + PARENT_COLUMN    + " VARCHAR(" + PARENT_LIMIT + "), "
                + CHILDREN_COLUMN  + " VARCHAR(" + CHILDREN_LIMIT + "), "
                + VALID_COLUMN     + " VARCHAR(" + VALID_LIMIT + ") );";
        stmtCreateTable = prepareStatement(createTableQuery);

        log.trace("Finished preparing SQL statements");
    }

    private PreparedStatement prepareStatement(String statement) throws
                                                                 SQLException {
        PreparedStatement preparedStatement =
                getConnection().prepareStatement(statement);
        preparedStatement.setFetchSize(FETCH_SIZE);
        return preparedStatement;
    }

    public synchronized RecordIterator getAllRecords() throws RemoteException {
        log.debug("getAllRecords entered");
        return getResult(stmtGetAll);
    }

    public synchronized RecordIterator getRecords(String base) throws
                                                               RemoteException {
        log.debug("getRecords('" + base + "') entered");
        try {
            stmtGetFromBase.setString(1, base);
        } catch (SQLException e) {
            throw new RemoteException("Could not prepare stmtGetFromBase with "
                                      + "base '" + base + "'", e);
        }
        return getResult(stmtGetFromBase);
    }

    public synchronized RecordIterator getRecordsModifiedAfter(long time,
                                                               String base)
                                                        throws RemoteException {
        log.debug("getRecordsModifiedAfter('" + base + "', " + time
                  + ") entered");
        try {
            stmtGetModifiedAfter.setString(1, base);
            stmtGetModifiedAfter.setTimestamp(2, new Timestamp(time));
        } catch (SQLException e) {
            throw new RemoteException("Could not prepare stmtGetModifiedAfter "
                                      + "with base '" + base + "' and time "
                                      + time, e);
        }
        return getResult(stmtGetModifiedAfter);
    }

    public synchronized RecordIterator getRecordsFrom(String id, String base)
                                                        throws RemoteException {
        log.debug("getRecordsFrom('" + base + "', '" + id + "') entered");
        try {
            stmtGetFrom.setString(1, base);
            stmtGetFrom.setString(2, id);
        } catch (SQLException e) {
            throw new RemoteException("Could not prepare stmtGetFrom "
                                      + "with base '" + base + "' and id '"
                                      + id + "'", e);
        }
        return getResult(stmtGetFrom);
    }

    public Record getRecord(String id) throws RemoteException {
        log.debug("getRecord('" + id + "') entered");
        try {
            stmtGetRecord.setString(1, id);
        } catch (SQLException e) {
            throw new RemoteException("Could not prepare stmtGetRecord "
                                      + "with id '" + id + "''", e);
        }
        Record record = null;
        try {
            ResultSet resultSet = stmtGetRecord.executeQuery();
            if (resultSet.next()) {
                try {
                    record = resultToRecord(resultSet);
                } catch (IOException e) {
                    throw new RemoteException("Exception transforming SQL "
                                              + "result set to record", e);
                }
            }
// TODO: Close?       stmtGetRecord.close();
        } catch (SQLException e) {
            throw new RemoteException("SQLException", e);
        }
        return record;
    }

    public RecordAndNext next(Long iteratorKey) throws RemoteException {
        ResultIterator iterator = iterators.get(iteratorKey);
        if (iterator == null) {
            throw new RemoteException("No result iterator with key "
                                      + iteratorKey);
        }
        RecordAndNext ran;
        try {
            try {
                ran = new RecordAndNext(iterator.getRecord(),
                                                      iterator.hasNext());
            } catch (IOException e) {
                throw new RemoteException("Exception getting next record", e);
            }
        } catch (SQLException e) {
            throw new RemoteException("SQLException", e);
        }
        if (!iterator.hasNext()) {
            iterator.close();
            iterators.remove(iterator.getKey());
        }
        return ran;
    }

    public void flush(Record record) throws RemoteException {
        log.debug("Flushing record '" + record.getId() + "' from base '"
                  + record.getBase() + "'");
        if (log.isTraceEnabled()) {
            log.trace("Flushing " + record);
        }
        if (record.isDeleted()){
            deleteRecord(record.getId());
        } else if (record.isNew()){
            createNewRecord(record);
        } else if (record.isModified()){
            updateRecord(record);
        } else {
            throw new RemoteException("Illegal State",
                                      new IllegalStateException(record
                                                                + " not i a "
                                                                + "flushable "
                                                                + "state"));
        }
    }

    private void createNewRecord(Record record) throws RemoteException {
        // TODO: Check for existence before creating
        try {
            stmtCreateRecord.setString(1, record.getId());
            stmtCreateRecord.setString(2, record.getBase());
            stmtCreateRecord.setBoolean(3, record.isDeleted());
            stmtCreateRecord.setBoolean(4, record.isIndexable());
            stmtCreateRecord.setBytes(5, GZIPUtils.gzip(record.getContent()));
            stmtCreateRecord.setTimestamp(6,
                                 new Timestamp(record.getCreationTime()));
            stmtCreateRecord.setTimestamp(7,
                                 new Timestamp(record.getModificationTime()));
            stmtCreateRecord.setString(8, record.getParent());
            stmtCreateRecord.setString(9,
                             Record.childrenListToString(record.getChildren()));
            stmtCreateRecord.setString(10,
                                 record.getValidationState().toString());
            stmtCreateRecord.execute();
        } catch (IOException e) {
            throw new RemoteException("IOException GZIPping data from record '"
                                      + record.getId() + "'", e);
        } catch (SQLException e) {
            throw new RemoteException("SQLException creating new record '"
                                      + record.getId() + "'", e);
        }
        updateMultiVolume(record);
    }

    /* Note that creationTime aren't touched */
    private void updateRecord(Record record) throws RemoteException {
        // TODO: Check for existence before creating
        try {
            stmtUpdateRecord.setString(1, record.getBase());
            stmtUpdateRecord.setBoolean(2, record.isDeleted());
            stmtUpdateRecord.setBoolean(3, record.isIndexable());
            stmtUpdateRecord.setBytes(4, GZIPUtils.gzip(record.getContent()));
            stmtUpdateRecord.setTimestamp(5,
                                 new Timestamp(record.getModificationTime()));
            stmtUpdateRecord.setString(6, record.getParent());
            stmtUpdateRecord.setString(7,
                             Record.childrenListToString(record.getChildren()));
            stmtUpdateRecord.setString(8,
                                 record.getValidationState().toString());
            stmtUpdateRecord.setString(9, record.getId());
            stmtUpdateRecord.execute();
        } catch (IOException e) {
            throw new RemoteException("IOException GZIPping data from record '"
                                      + record.getId() + "'", e);
        } catch (SQLException e) {
            throw new RemoteException("SQLException updating record '"
                                      + record.getId() + "'", e);
        }
        updateMultiVolume(record);
     }

    private int deleteRecord(String id) throws RemoteException {
        long now = System.currentTimeMillis();
        Timestamp nowStamp = new Timestamp(now);
        try {
            stmtDeleteRecord.setTimestamp(1, nowStamp);
            stmtDeleteRecord.setBoolean(2, true);
            stmtDeleteRecord.setString(3, id);
            // TODO: Consider stmt.close();
            return stmtDeleteRecord.executeUpdate();
        } catch (SQLException e) {
            log.error("SQLException deleting record '" + id + "'", e);
            throw new RemoteException("SQLException deleting record '"
                                      + id + "'", e);
        }
    }

    public void touchRecord(String id, long lastModified) throws
                                                           RemoteException {
        // TODO: Check for existence before touching
        try {
            stmtUpdateRecord.setTimestamp(1, new Timestamp(lastModified));
            stmtUpdateRecord.setString(2, id);
            stmtUpdateRecord.execute();
        } catch (SQLException e) {
            throw new RemoteException("SQLException touching record '"
                                      + id + "'", e);
        }
    }

    /**
     * Creates the primary table for a database based metadata storage. This
     * used SQL with BLOBs.
     * @throws RemoteException if the database could not be created.
     */
    protected void createTable() throws RemoteException {
        log.info("Attempting to create table");
        try {
            stmtCreateTable.execute();
        } catch (SQLException e) {
            throw new RemoteException("Could not create table", e);
        }
    }

    public void perform() {
        // TODO: Implement this
    }

    /**
     * Extract elements from a SQL result set and create a Record from these
     * elements.
     * @param resultSet     a SQL result set.
     * @return              a Record based on the result set.
     * @throws SQLException if there was a problem extracting values from the
     *                      SQL result set.
     * @throws IOException  If the data (content) could not be uncompressed
     *                      with gunzip.
     */
    public static Record resultToRecord(ResultSet resultSet) throws SQLException,
                                                             IOException {
        return new Record(resultSet.getString(ID_COLUMN),
                          resultSet.getString(BASE_COLUMN),
                          resultSet.getBoolean(DELETED_COLUMN),
                          resultSet.getBoolean(INDEXABLE_COLUMN),
                          GZIPUtils.gunzip(resultSet.getBytes(DATA_COLUMN)),
                          resultSet.getTimestamp(CTIME_COLUMN).getTime(),
                          resultSet.getTimestamp(MTIME_COLUMN).getTime(),
                          resultSet.getString(PARENT_COLUMN),
                          Record.childrenStringToList(resultSet.
                                  getString(CHILDREN_COLUMN)),
                          Record.ValidationState.fromString(
                                  resultSet.getString(VALID_COLUMN)));
    }

    /**
     * Given a query, execute this query and transform the resultset to a
     * RecordIterator.
     * @param statement the statement to execute.
     * @return a RecordIterator of the result.
     * @throws RemoteException - also on no getConnection() and SQLExceptions.
     */
    private RecordIterator getResult(PreparedStatement statement) throws
                                                               RemoteException {
        log.debug("Getting results for '" + statement + "'");
        ResultSet resultSet;
         try {
//             longConn.setAutoCommit(false);
             resultSet = statement.executeQuery();
             log.info("Got resultSet from '" + statement.toString() + "'");
         } catch (SQLException e) {
             log.error("SQLException in getResult", e);
             throw new RemoteException("SQLException", e);
         }
         return createRecordIterator(resultSet);
    }

    protected class ResultIterator {
        private Log log = LogFactory.getLog(ResultIterator.class);

        private long key = System.currentTimeMillis();

        public long getLastAccess() {
            return lastAccess;
        }

        private long lastAccess = key;
        private ResultSet resultSet;
        private boolean hasNext;

        public ResultIterator(ResultSet resultSet) throws SQLException {
            this.resultSet = resultSet;
            hasNext = resultSet.next();
            lastAccess = System.currentTimeMillis();
        }

        public long getKey() {
            lastAccess = System.currentTimeMillis();
            return key;
        }

        public boolean hasNext() {
            lastAccess = System.currentTimeMillis();
            return hasNext;
        }

        /**
         * Constructs a Record based on the row in the result set, then advances
         * to the next record.
         * @return a Record based on the current row in the result set.
         * @throws java.sql.SQLException if the record could not be requested.
         * @throws IOException if the Record data could not be gunzipped.
         */
        public Record getRecord() throws SQLException, IOException {
            lastAccess = System.currentTimeMillis();
            Record record = DatabaseControl.resultToRecord(resultSet);
            if (log.isTraceEnabled()) {
                log.trace("getRecord returning '" + record.getId() + "'");
            }
            hasNext = resultSet.next();
            return record;
//            rs.getStatement().close();//note: rs is closed when stmt is closed
        }

        public void close() {
            log.warn("Close in ResultIterator not implemented yet");
//            rs.getStatement().close();//note: rs is closed when stmt is closed
//            resultSet.close();
            // TODO: Implement this
        }
    }

    /**
     * Creates new RecordIterator from the given resultset and getConnection(),
     * i.e. creates new key and saves the given resultset and getConnection() and
     * "forwards" the resultset by one row to keep a step ahead.
     * @param resultSet the results to wrap.
     * @return RecordIterator an iterator over the resultset.
     * @throws RemoteException on SQLException
     */
    private RecordIterator createRecordIterator(ResultSet resultSet) throws
                                                               RemoteException {
        ResultIterator iterator;
        try {
            iterator = new ResultIterator(resultSet);
            iterators.put(iterator.getKey(), iterator);
        } catch (SQLException e) {
            log.error("SQLException creating record iterator", e);
            throw new RemoteException("SQLException creating record iterator",
                                      e);
        }
        log.trace("returning new RecordIterator");
        return new RecordIterator(this, iterator.getKey(), iterator.hasNext());
    }

}
