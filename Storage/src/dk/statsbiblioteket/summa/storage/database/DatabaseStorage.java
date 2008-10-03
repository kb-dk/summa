/* $Id: DatabaseStorage.java,v 1.8 2007/12/04 09:08:19 te Exp $
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
 * CVS:     $Id: DatabaseStorage.java,v 1.8 2007/12/04 09:08:19 te Exp $
 */
package dk.statsbiblioteket.summa.storage.database;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.util.StringMap;
import dk.statsbiblioteket.summa.storage.StorageBase;
import dk.statsbiblioteket.summa.storage.api.RecordIterator;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Zips;
import dk.statsbiblioteket.util.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An abstract implementation of a SQL-oriented database driven extension
 * of StorageBase.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class DatabaseStorage extends StorageBase {
    private static Log log = LogFactory.getLog(DatabaseStorage.class);

    /**
     * The property-key for the username for the underlying database, if needed.
     */
    public static String CONF_USERNAME = "summa.storage.database.username";
    /**
     * The property-key for the the password for the underlying database, if
     * needed.
     */
    public static String CONF_PASSWORD = "summa.storage.database.password";
    /**
     * The property-key for the boolean value determining if a new database
     * should be created is there is no existing database. If createnew is
     * true and a database exists and forcenew is true, the existing database
     * is deleted and a new one created. If createnew is true and a database
     * exists and forcenew is false, the existing database is reused.
     */
    public static String CONF_CREATENEW = "summa.storage.database.createnew";
    /**
     * The property-key for the boolean determining if a new database should
     * be created, no matter is a database already exists.
     */
    public static String CONF_FORCENEW = "summa.storage.database.forcenew";
    /**
     * The location of the database to use/create. If the location is not an
     * absolute path, it will be appended to the System property "
     * summa.control.client.persistent.dir". If that system property does not
     * exist, the location will be relative to the current dir.
     */
    public static String CONF_LOCATION = "summa.storage.database.location";

    /**
     * The name of the table in the database.
     */
    public static final String TABLE            = "summa_io";
    /**
     * id is the unique identifier for a given record in the database.
     */
    public static final String ID_COLUMN        = "id";
    /**
     * The base dictates choise of xslt's et al for the record.
     */
    public static final String BASE_COLUMN      = "base";
    /**
     * deleted signifies that the record should be treated as non-existing.
     * Implementations are free to clean up deleted records at will, but not
     * required to.
     */
    public static final String DELETED_COLUMN   = "deleted";
    /**
     * indexable signifies that the record should be delivered to the indexer
     * upon request.
     */
    public static final String INDEXABLE_COLUMN = "indexable";
    /**
     * data contains the raw record-data as ingested.
     */
    public static final String DATA_COLUMN      = "data";
    /**
     * ctime signifies the time of record creation in the database.
     */
    public static final String CTIME_COLUMN     = "ctime";
    /**
     * mtime signifies the time of record modification in the database.
     * This timestamp is used when {@link #getRecordsModifiedAfter} is called.
     */
    public static final String MTIME_COLUMN     = "mtime";
    /**
     * parent optionally contains the id of a parent record. The parent does
     * not need to be present in the database, but if it is, the field indexable
     * should be false for the child.
     */
    public static final String PARENT_COLUMN    = "parent";
    /**
     * children optionally contains a list of children. The children does not
     * need to be present in the database.
     */
    public static final String CHILDREN_COLUMN  = "children";
    /**
     * meta contains meta-data for the Record in the form of key-value pairs
     * of Strings. See {@link StringMap#toFormal()} for format.
     */
    public static final String META_COLUMN  = "meta";

    /* Constants for database-setup */
    public static final int ID_LIMIT =       255;
    public static final int BASE_LIMIT =     31;
    public static final int DATA_LIMIT =     50*1024*1024;
    public static final int PARENT_LIMIT =   10*ID_LIMIT; // Room for the future
    public static final int CHILDREN_LIMIT = 100*ID_LIMIT; // Change to CLOB?
    public static final int META_LIMIT =     50*1024*1024; // MAX_VALUE instead?
    private static final int BLOB_MAX_SIZE = 50*1024*1024; // MAX_VALUE instead?

    private PreparedStatement stmtGetAll;
    private PreparedStatement stmtGetFromBase;
    private PreparedStatement stmtGetModifiedAfter;
    private PreparedStatement stmtGetFrom;
    private PreparedStatement stmtGetRecord;
    private PreparedStatement stmtGetRecords;
    private PreparedStatement stmtClearBase;
    private PreparedStatement stmtDeleteRecord;
    private PreparedStatement stmtCreateRecord;
    private PreparedStatement stmtUpdateRecord;
    private PreparedStatement stmtTouchRecord;

    private static final int FETCH_SIZE = 10000;

    private Map<Long, ResultIterator> iterators =
            new HashMap<Long, ResultIterator>(10);

    public DatabaseStorage() throws IOException {
        // We need to define this to declare RemoteException
    }

    public DatabaseStorage(int port) throws IOException {
        // We need to define this to declare RemoteException
    }

    public DatabaseStorage(Configuration configuration) throws IOException {
        super(updateConfiguration(configuration));
    }

    private static Configuration updateConfiguration(Configuration
                                                     configuration) {
        String location;
        try {
            location = configuration.getString(CONF_LOCATION);
        } catch (NullPointerException e) {
            log.debug("Could not locate key " + CONF_LOCATION
                      + ". Skipping updating of location");
            return configuration;
        }
        try {
            File locationFile = new File(location);
            File newLocationFile = Resolver.getPersistentFile(locationFile);

            if (!locationFile.equals(newLocationFile)) {
                    log.debug("Storing new location '" + newLocationFile
                              + "' to property key " + CONF_LOCATION);
                    configuration.set(CONF_LOCATION, newLocationFile.getPath());
            } else {
                log.debug(CONF_LOCATION + " is an absolute path ("
                          + locationFile + "). No changes will be done");
            }
        } catch (Exception e) {
            log.debug("Could not transmute key '" + CONF_LOCATION
                      + "' in configuration", e);
        }
        return configuration;
    }

    /**
     * The initializer connects to the database and prepares SQL statements.
     * It is recommended that this is called by all constructors.
     * @param configuration    the setup for the database.
     * @throws RemoteException if the initialization could not finish.
     */
    protected void init(Configuration configuration) throws RemoteException {
        log.trace("init called");
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
     * @throws RemoteException if a connection could not be established to the
     *                         database.
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
                          + TABLE + "." + META_COLUMN;

        String allQuery = "SELECT " + allCells
                          + " FROM " + TABLE
                          + " ORDER BY " + ID_COLUMN;
        log.debug("Preparing query getAll with '" + allQuery + "'");
        stmtGetAll = getConnection().prepareStatement(allQuery);

        String fromBaseQuery = "SELECT " + allCells
                               + " FROM " + TABLE
                               + " WHERE " + BASE_COLUMN + "=?"
                               + " ORDER BY " + ID_COLUMN;
        log.debug("Preparing query getFromBase with '" + fromBaseQuery + "'");
        stmtGetFromBase = getConnection().prepareStatement(fromBaseQuery);

        String modifiedAfterQuery = "SELECT " + allCells
                                    + " FROM " + TABLE
                                    + " WHERE " + BASE_COLUMN + "=?"
                                    + " AND " + MTIME_COLUMN + ">?"
                                    + " ORDER BY " + ID_COLUMN;
        log.debug("Preparing query getModifiedAfter with '"
                  + modifiedAfterQuery + "'");
        stmtGetModifiedAfter = getConnection().prepareStatement(modifiedAfterQuery);
// TODO: Handle deletions and indexables
        String fromQuery = "SELECT " + allCells
                           + " FROM " + TABLE
                           + " WHERE " + BASE_COLUMN + "=?"
                           + " AND " + ID_COLUMN + ">?"
                           + " ORDER BY " + ID_COLUMN;
        log.debug("Preparing query getFrom with '" + fromQuery + "'");
        stmtGetFrom = getConnection().prepareStatement(fromQuery);

        String getRecordQuery = "SELECT " + allCells
                                + " FROM " + TABLE
                                + " WHERE " + ID_COLUMN + "=?";
        log.debug("Preparing query recordQuery with '" + getRecordQuery + "'");
        stmtGetRecord = getConnection().prepareStatement(getRecordQuery);

        String clearBaseQuery = "UPDATE " + TABLE
                                + " SET " + DELETED_COLUMN + "=1"
                                + " WHERE " + BASE_COLUMN + "=?";
        log.debug("Preparing query clearBase with '"
                  + clearBaseQuery + "'");
        stmtClearBase = getConnection().prepareStatement(clearBaseQuery);

        /*
         FIXME: We might want a prepared statement to fetch multiple records in
                one go. However it seems to be inefficient to use prepared
                statements with IN clauses in them . See fx:
                http://forum.springframework.org/archive/index.php/t-16001.html

        String getRecordsQuery = "SELECT " + allCells
                                + " FROM " + TABLE
                                + " WHERE " + ID_COLUMN + " IN ?";
        log.debug("Preparing query recordsQuery with '" + getRecordsQuery + "'");
        stmtGetRecords = getConnection().prepareStatement(getRecordsQuery);
         */

        String deleteRecordQuery = "UPDATE " + TABLE
                                   + " SET " + MTIME_COLUMN + "=?, "
                                   + DELETED_COLUMN + "=?"
                                   + " WHERE " + ID_COLUMN + "=?";
        log.debug("Preparing query deleteRecord with '"
                  + deleteRecordQuery + "'");
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
                                       + META_COLUMN
                                       + ") VALUES (?,?,?,?,?,?,?,?,?,?)";
        log.debug("Preparing query createRecord with '" + createRecordQuery
                  + "'");
        stmtCreateRecord = getConnection().prepareStatement(createRecordQuery);

        String updateRecordQuery = "UPDATE " + TABLE + " SET "
                                   + BASE_COLUMN + "=?, "
                                   + DELETED_COLUMN + "=?, "
                                   + INDEXABLE_COLUMN + "=?, "
                                   + DATA_COLUMN + "=?, "
                                   + MTIME_COLUMN + "=?, "
                                   + PARENT_COLUMN + "=?, "
                                   + CHILDREN_COLUMN + "=?, "
                                   + META_COLUMN + "=? "
                                   + "WHERE " + ID_COLUMN +"=?";
        log.debug("Preparing query updateRecord with '" + updateRecordQuery
                  + "'");
        stmtUpdateRecord = getConnection().prepareStatement(updateRecordQuery);

        String touchRecordQuery = "UPDATE " + TABLE + " SET "
                                   + MTIME_COLUMN + "=? "
                                   + "WHERE " + ID_COLUMN +"=?";
        log.debug("Preparing query touchRecord with '" + touchRecordQuery
                  + "'");
        stmtTouchRecord = prepareStatement(touchRecordQuery);

        log.trace("Finished preparing SQL statements");
    }

    private PreparedStatement prepareStatement(String statement) throws
                                                                 SQLException {
        PreparedStatement preparedStatement =
                getConnection().prepareStatement(statement);
        preparedStatement.setFetchSize(FETCH_SIZE);
        return preparedStatement;
    }

    public synchronized Iterator<Record> getAllRecords() throws RemoteException {
        log.debug("getAllRecords entered");
        return getResult(stmtGetAll);
    }

    public synchronized Iterator<Record> getRecordsFromBase(String base) throws
                                                               RemoteException {
        log.debug("getRecordsFromBase('" + base + "') entered");
        try {
            stmtGetFromBase.setString(1, base);
        } catch (SQLException e) {
            throw new RemoteException("Could not prepare stmtGetFromBase with "
                                      + "base '" + base + "'", e);
        }
        return getResult(stmtGetFromBase);
    }

    public synchronized Iterator<Record> getRecordsModifiedAfter(long time,
                                                               String base)
                                                        throws RemoteException {
        log.debug("getRecordsModifiedAfter('" + time + "', " + base
                  + ") entered");
        // TODO: Fix the SQL with a WHERE != time instead of hacking this way
        time += 1000;
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

    public synchronized Iterator<Record> getRecordsFrom(String id, String base)
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

    /**
     * Convenience method to fetch a single record
     * @param id id of record to fetch
     * @return the record or {@code null} if the record is not found
     * @throws RemoteException on communication errors with the database
     */
    public Record getRecord(String id, int expansionDepth)
                                                        throws RemoteException {
        log.trace("getRecord('" + id + "', " + expansionDepth + ")");

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
                    expandChildRecords(record, expansionDepth);

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

    /**
     * Simple implementation fetching each record one at a time and collecting
     * them in a list.
     * <p/>
     * FIXME: See FIXME inside {@link #prepareStatements()} code
     */
    public List<Record> getRecords (List<String> ids, int expansionDepth)
                                                        throws RemoteException {
        ArrayList<Record> result = new ArrayList<Record>(ids.size());
        for (String id : ids) {
            Record r = getRecord(id, expansionDepth);
            if (r != null) {
                result.add(r);
            }
        }

        return result;
    }

    /* Expand child records if we need to and there indeed
     * are any children to expand */
    private void expandChildRecords (Record record, int expansionDepth)
                                                        throws RemoteException {
        if (expansionDepth == 0) {
            return;
        }

        List<String> childIds = record.getChildIds();

        if (childIds != null && childIds.size() != 0) {

            if (log.isTraceEnabled()) {
                log.trace ("Expanding children of record '"
                           + record.getId() + "': "
                           + Strings.join(childIds, ", "));
            }

            List<Record> children = getRecords(childIds,
                                               expansionDepth - 1);
            if (children.isEmpty()) {
                record.setChildren(null);
            } else {
                record.setChildren(children);
            }
        }
    }

    public Record next(Long iteratorKey) throws RemoteException {
        boolean error = false;
        ResultIterator iterator = iterators.get(iteratorKey);
        if (iterator == null) {
            throw new RemoteException("No result iterator with key "
                                      + iteratorKey);
        }

        try {

            if (!iterator.hasNext()) {
                iterator.close();
                iterators.remove(iterator.getKey());
                throw new NoSuchElementException ("Iterator " + iteratorKey
                                                  + " depleted");
            }

            return iterator.getRecord();
        } catch (IOException e) {
            error = true;
                throw new RemoteException("Exception getting next record: "
                                          + e.getMessage(), e);
        } catch (SQLException e) {
            error = true;
            throw new RemoteException("SQLException: " + e.getMessage(), e);
        } finally {
            if (error) {
                log.debug ("Error detected on iterator " + iteratorKey + "."
                           + " Removing");
                iterator.close();
                iterators.remove(iterator.getKey());
            }
        }
    }

    public void flush(Record record) throws RemoteException {
        if (log.isTraceEnabled()) {
            log.trace("Flushing " + record);
        }
        if (record.isDeleted()){
            log.debug("Deleting record '" + record.getId() + "' from base '"
                      + record.getBase() + "'");
            deleteRecord(record.getId());
        } else if (record.isNew()){
            log.debug("Creating new record '" + record.getId() + "' from base '"
                      + record.getBase() + "'");
            createNewRecord(record);
        } else if (record.isModified()){
            log.debug("Updating record '" + record.getId() + "' from base '"
                      + record.getBase() + "'");
            updateRecord(record);
        } else {
            throw new RemoteException("Illegal State",
                                      new IllegalStateException(
                                              record + " not in a flushable "
                                              + "state"));
        }
    }

    public void clearBase (String base) throws RemoteException {
        log.info ("Clearing base '" + base + "'");

        try {
            stmtClearBase.setString(1, base);
            stmtClearBase.execute();
        } catch (SQLException e) {
            throw new RemoteException("SQLException clearing base '"
                                      + base + "'", e);
        }
    }

    private void createNewRecord(Record record) throws RemoteException {
        // TODO: Consider calling modify if the record already exists
        try {
            stmtCreateRecord.setString(1, record.getId());
            stmtCreateRecord.setString(2, record.getBase());
            stmtCreateRecord.setInt(3, boolToInt(record.isDeleted()));
            stmtCreateRecord.setInt(4, boolToInt(record.isIndexable()));
            stmtCreateRecord.setBytes(5, Zips.gzipBuffer(record.getContent()));
            stmtCreateRecord.setTimestamp(6,
                                 new Timestamp(record.getCreationTime()));
            stmtCreateRecord.setTimestamp(7,
                                 new Timestamp(record.getModificationTime()));
            stmtCreateRecord.setString(8, record.getParentId());
            stmtCreateRecord.setString(9,
                             Record.childrenListToString(record.getChildIds()));
            stmtCreateRecord.setBytes(10,record.hasMeta() ?
                                         record.getMeta().toFormalBytes() :
                                         new byte[0]);
            stmtCreateRecord.execute();
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
            stmtUpdateRecord.setInt(2, boolToInt(record.isDeleted()));
            stmtUpdateRecord.setInt(3, boolToInt(record.isIndexable()));
            stmtUpdateRecord.setBytes(4, Zips.gzipBuffer(record.getContent()));
            stmtUpdateRecord.setTimestamp(5,
                                 new Timestamp(record.getModificationTime()));
            stmtUpdateRecord.setString(6, record.getParentId());
            stmtUpdateRecord.setString(7,
                             Record.childrenListToString(record.getChildIds()));
            stmtUpdateRecord.setBytes(8, record.hasMeta() ?
                                         record.getMeta().toFormalBytes() :
                                         new byte[0]);
            stmtUpdateRecord.setString(9, record.getId());
            stmtUpdateRecord.execute();
            if (stmtUpdateRecord.getUpdateCount() == 0) {
                log.warn("The record with id '" + record.getId()
                         + "' was marked as modified, but did not exist in the"
                         + " database. The record will be added as new");
                createNewRecord(record);
                return;
            }
        } catch (RemoteException e) {
            throw new RemoteException("RemoteException calling update for "
                                      + "record '" + record.getId() + "'", e);
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
            stmtTouchRecord.setTimestamp(1, new Timestamp(lastModified));
            stmtTouchRecord.setString(2, id);
            stmtTouchRecord.execute();
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
        log.debug("Attempting to create table");
        try {
            constructCreateTableQuery().execute();
        } catch (SQLException e) {
            throw new RemoteException("Could not create table", e);
        }
    }

    private PreparedStatement constructCreateTableQuery() throws
                                                            SQLException {
        String createTableQuery =
                "CREATE table " + TABLE + " ("
                + ID_COLUMN        + " VARCHAR(" + ID_LIMIT + ") PRIMARY KEY, "
                + BASE_COLUMN      + " VARCHAR(" + BASE_LIMIT + "), "
                + DELETED_COLUMN   + " INTEGER, "
                + INDEXABLE_COLUMN + " INTEGER, "
                + DATA_COLUMN      + " BLOB(" + BLOB_MAX_SIZE + "), "
                + CTIME_COLUMN     + " TIMESTAMP, "
                + MTIME_COLUMN     + " TIMESTAMP, "
                + PARENT_COLUMN    + " VARCHAR(" + PARENT_LIMIT + "), "
                + CHILDREN_COLUMN  + " VARCHAR(" + CHILDREN_LIMIT + "), "
                + META_COLUMN      + " BLOB(" + META_LIMIT + ") )";
        log.debug("Creating table with query '" + createTableQuery + "'");
        return prepareStatement(createTableQuery);
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
                          intToBool(resultSet.getInt(DELETED_COLUMN)),
                          intToBool(resultSet.getInt(INDEXABLE_COLUMN)),
                          Zips.gunzipBuffer(resultSet.getBytes(DATA_COLUMN)),
                          resultSet.getTimestamp(CTIME_COLUMN).getTime(),
                          resultSet.getTimestamp(MTIME_COLUMN).getTime(),
                          resultSet.getString(PARENT_COLUMN),
                          Record.childrenStringToList(resultSet.
                                  getString(CHILDREN_COLUMN)),
                          StringMap.fromFormal(resultSet.
                                  getBytes(META_COLUMN)));
    }

    /**
     * Given a query, execute this query and transform the resultset to a
     * RecordIterator.
     * @param statement the statement to execute.
     * @return a RecordIterator of the result.
     * @throws RemoteException - also on no getConnection() and SQLExceptions.
     */
    private Iterator<Record> getResult(PreparedStatement statement) throws
                                                               RemoteException {
        log.debug("Getting results for '" + statement + "'");
        ResultSet resultSet;
         try {
//             longConn.setAutoCommit(false);
             resultSet = statement.executeQuery();
             log.debug("Got resultSet from '" + statement.toString() + "'");
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
            log.trace("Constructed Record iterator with initial hasNext: "
                      + hasNext);
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
            Record record = DatabaseStorage.resultToRecord(resultSet);
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
     * @return an iterator over the resultset
     * @throws RemoteException on SQLException
     */
    private Iterator<Record> createRecordIterator(ResultSet resultSet) throws
                                                               RemoteException {
        log.trace("createRecordIterator entered with result set " + resultSet);
        ResultIterator iterator;
        try {
            iterator = new ResultIterator(resultSet);
            log.trace("createRecordIterator: Got iterator " + iterator.getKey()
                      + " adding to iterator-list");
            iterators.put(iterator.getKey(), iterator);
        } catch (SQLException e) {
            log.error("SQLException creating record iterator", e);
            throw new RemoteException("SQLException creating record iterator",
                                      e);
        }
        log.trace("returning new RecordIterator");
        return new RecordIterator(this, iterator.getKey(), iterator.hasNext());
    }

    /* Our version of a boolean packed as integer is that 0 = false, everything
       else = true. This should match common practice.
     */
    private int boolToInt(boolean isTrue) {
        return isTrue ? 1 : 0;
    }
    private static boolean intToBool(int anInt) {
        return anInt != 0;
    }

    /**
     * Queries the connection for information on the currently connected
     * database. This can be used as a ping, as it will throw an exception if
     * the info could not be retrieved.
     * @return some info on the underlying database. The only guarantee is that
     *         this will not be an empty string, if a connection to a database
     *         is established.
     * @throws RemoteException if the info could not be retireved.
     */
    public String getDatabaseInfo() throws RemoteException {
        try {
            return getConnection().getMetaData().getDatabaseProductName();
        } catch (SQLException e) {
            throw new RemoteException("Could not get catalog info", e);
        }
    }

}



