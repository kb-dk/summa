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
import java.sql.*;
import java.util.*;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.util.StringMap;
import dk.statsbiblioteket.summa.storage.StorageBase;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Zips;
import dk.statsbiblioteket.util.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An abstract implementation of a SQL database driven extension
 * of {@link StorageBase}.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke, te")
public abstract class DatabaseStorage extends StorageBase {
    private static Log log = LogFactory.getLog(DatabaseStorage.class);

    /**
     * The number of minutes iterators opened with
     * {@link #getRecordsModifiedAfter} are valid if they are unused. After the
     * specified amount of inactivity they may be cleaned up by the storage.
     * The default value is {@link #DEFAULT_ITERATOR_TIMEOUT}.
     */
    public static final String CONF_ITERATOR_TIMEOUT =
                                       "summa.storage.database.iteratortimeout";

    /**
     * Default value for the {@link #CONF_ITERATOR_TIMEOUT} property
     */
    public static final long DEFAULT_ITERATOR_TIMEOUT = 120;

    /**
     * The property-key for the username for the underlying database, if needed.
     * The default value is {@link #DEFAULT_USERNAME}.
     */
    public static String CONF_USERNAME = "summa.storage.database.username";
    /**
     * Default value for {@link #CONF_USERNAME}.
     */
    public static String DEFAULT_USERNAME = "summa";

    /**
     * The property-key for the the password for the underlying database, if
     * needed.
     */
    public static String CONF_PASSWORD = "summa.storage.database.password";

    /**
     * The name of the database to connect to. Default value is
     * {@link #DEFAULT_DATABASE}.
     */
    public static String CONF_DATABASE = "summa.storage.database.name";

    /**
     * Default value for {@link #CONF_DATABASE}.
     */
    public static String DEFAULT_DATABASE = "summa";

    /**
     * The port to connect to the datatbase on.
     */
    public static String CONF_PORT = "summa.storage.database.port";

    /**
     * The hostname to connect to the datatbase on. Default is
     * {@link #DEFAULT_HOST}.
     */
    public static String CONF_HOST = "summa.storage.database.host";

    /**
     * Default value for {@link #CONF_HOST}.
     */
    public static String DEFAULT_HOST = "localhost";

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
     * The name of the main table in the database in which record metadata
     * is stored. Parent/child relations are stored in {@link #RELATIONS}
     */
    public static final String RECORDS = "summa_records";

    /**
     * The name of the table holding the parent/child relations of the records
     * from {@link #RECORDS}
     */
    public static final String RELATIONS = "summa_relations";

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
    public static final String PARENT_ID_COLUMN = "parentId";
    /**
     * children optionally contains a list of children. The children does not
     * need to be present in the database.
     */
    public static final String CHILD_ID_COLUMN = "childId";
    /**
     * meta contains meta-data for the Record in the form of key-value pairs
     * of Strings. See {@link StringMap#toFormal()} for format.
     */
    public static final String META_COLUMN  = "meta";

    /* Constants for database-setup */
    public static final int ID_LIMIT =       255;
    public static final int BASE_LIMIT =     31;
    public static final int DATA_LIMIT =     50*1024*1024;


    private static final long EMPTY_ITERATOR_KEY = -1;

    private PreparedStatement stmtGetModifiedAfter;
    private PreparedStatement stmtGetModifiedAfterAll;
    private PreparedStatement stmtGetRecord;
    private PreparedStatement stmtClearBase;
    private PreparedStatement stmtDeleteRecord;
    private PreparedStatement stmtCreateRecord;
    private PreparedStatement stmtUpdateRecord;
    private PreparedStatement stmtTouchParents;
    private PreparedStatement stmtGetChildren;
    private PreparedStatement stmtGetParents;
    private PreparedStatement stmtCreateRelation;    

    private static final int FETCH_SIZE = 10000;

    private Map<Long, ResultIterator> iterators =
                                         new HashMap<Long, ResultIterator>(10);

    private ResultIteratorReaper iteratorReaper;

    /**
     * A variation of {@link QueryOptions} used to keep track
     * of recursion depths for expanding children and parents
     */
    private static class RecursionQueryOptions extends QueryOptions {

        private int childRecursionDepth;
        private int parentRecursionHeight;

        public RecursionQueryOptions (QueryOptions original) {
            super(original);
            resetRecursionLevels();
        }

        public int childRecursionDepth() {
            return childRecursionDepth;
        }

        public int parentRecursionHeight() {
            return parentRecursionHeight;
        }

        public RecursionQueryOptions decChildRecursionDepth() {
            childRecursionDepth--;
            return this;
        }

        public RecursionQueryOptions decParentRecursionHeight() {
            parentRecursionHeight--;
            return this;
        }

        public RecursionQueryOptions resetRecursionLevels(){
            childRecursionDepth = childDepth();
            parentRecursionHeight = parentHeight();
            return this;
        }

        /**
         * Ensure that {@code options} is a RecursionQueryOptions. If it already
         * is, just reset it and return it.
         * @param options the QueryOptions to convert to a RecursionQueryOptions
         * @return the input wrapped as a RecursionQueryOptions, or just the
         *         input if it is already recursive
         */
        public static RecursionQueryOptions wrap (QueryOptions options) {
            if (options == null) {
                return null;
            }

            if (options instanceof RecursionQueryOptions) {
                return ((RecursionQueryOptions)options).resetRecursionLevels();
            } else {
                return new RecursionQueryOptions(options);
            }
        }
    }

    public DatabaseStorage() throws IOException {
        this(0);
    }

    public DatabaseStorage(int port) throws IOException {

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
            log.trace("locationFile: " + locationFile
                      + ", persistent location file: " + newLocationFile);
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
     * You <i>must</i> call this in all constructors of sub classes of
     * DatabaseStorage.
     * @param conf    the setup for the database.
     * @throws ConfigurationException if the initialization could not finish.
     * @throws IOException on failing to connect to the database
     */
    protected void init(Configuration conf) throws IOException {
        log.trace("init called");
        connectToDatabase(conf);
        try {
            prepareStatements();
        } catch (SQLException e) {
            throw new ConfigurationException("SQLException in init", e);
        }

        iteratorReaper =
               new ResultIteratorReaper(iterators,
                                        conf.getLong(CONF_ITERATOR_TIMEOUT,
                                                     DEFAULT_ITERATOR_TIMEOUT));
        iteratorReaper.runInThread();

        log.debug("Initialization finished");
    }

    /**
     * Connect to the relevant database and establish a connection, so that
     * calls to {@link #getConnection()} can be performed. Depending on the
     * configuration, this might involve creating a table in the database and
     * initializing that to Summa-use.
     * @param configuration setup for the database.
     * @throws IOException if a connection could not be established to the
     *                         database.
     */
    protected abstract void connectToDatabase(Configuration configuration)
                                                        throws IOException;

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
        String allCells = RECORDS + "." + ID_COLUMN + ","
                          + RECORDS + "." + BASE_COLUMN + ","
                          + RECORDS + "." + DELETED_COLUMN + ","
                          + RECORDS + "." + INDEXABLE_COLUMN + ","
                          + RECORDS + "." + DATA_COLUMN + ","
                          + RECORDS + "." + CTIME_COLUMN + ","
                          + RECORDS + "." + MTIME_COLUMN + ","
                          + RELATIONS + "." + PARENT_ID_COLUMN + ","
                          + RELATIONS + "." + CHILD_ID_COLUMN + ","
                          + RECORDS + "." + META_COLUMN;

        String relationsClause = RECORDS + "." + ID_COLUMN + "="
                                + RELATIONS + "." + PARENT_ID_COLUMN
                                + " OR " + RECORDS + "." + ID_COLUMN + "="
                                + RELATIONS + "." + CHILD_ID_COLUMN;

        String modifiedAfterQuery = "SELECT " + allCells
                                    + " FROM " + RECORDS
                                    + " LEFT JOIN " + RELATIONS
                                    + " ON " + relationsClause
                                    + " WHERE " + BASE_COLUMN + "=?"
                                    + " AND " + MTIME_COLUMN + ">?"
                                    + " ORDER BY "
                                    + MTIME_COLUMN + ", " + ID_COLUMN;
        log.debug("Preparing query getModifiedAfter with '"
                  + modifiedAfterQuery + "'");
        stmtGetModifiedAfter = getConnection().prepareStatement(modifiedAfterQuery);

        String modifiedAfterAllQuery = "SELECT " + allCells
                                       + " FROM " + RECORDS
                                       + " LEFT JOIN " + RELATIONS
                                       + " ON " + relationsClause
                                       + " WHERE " + MTIME_COLUMN + ">?"
                                       + " ORDER BY "
                                       + MTIME_COLUMN + ", " + ID_COLUMN;
        log.debug("Preparing query getModifiedAfterAll with '"
                  + modifiedAfterAllQuery + "'");
        stmtGetModifiedAfterAll = getConnection().prepareStatement(
                                                         modifiedAfterAllQuery);

        String getRecordQuery = "SELECT " + allCells
                                + " FROM " + RECORDS
                                + " LEFT JOIN " + RELATIONS
                                + " ON " + relationsClause
                                + " WHERE " + RECORDS + "." + ID_COLUMN + "=?";
        log.debug("Preparing query getRecord with '" + getRecordQuery + "'");
        stmtGetRecord = getConnection().prepareStatement(getRecordQuery);

        String clearBaseQuery = "UPDATE " + RECORDS
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
                                + " FROM " + RECORDS
                                + " WHERE " + ID_COLUMN + " IN ?";
        log.debug("Preparing query recordsQuery with '" + getRecordsQuery + "'");
        stmtGetRecords = getConnection().prepareStatement(getRecordsQuery);
         */

        String deleteRecordQuery = "UPDATE " + RECORDS
                                   + " SET " + MTIME_COLUMN + "=?, "
                                   + DELETED_COLUMN + "=?"
                                   + " WHERE " + ID_COLUMN + "=?";
        log.debug("Preparing query deleteRecord with '"
                  + deleteRecordQuery + "'");
        stmtDeleteRecord = getConnection().prepareStatement(deleteRecordQuery);

        /* createRecord */
        String createRecordQuery = "INSERT INTO " + RECORDS
                                + " (" + ID_COLUMN + ", "
                                       + BASE_COLUMN + ", "
                                       + DELETED_COLUMN + ", "
                                       + INDEXABLE_COLUMN + ", "
                                       + CTIME_COLUMN + ", "
                                       + MTIME_COLUMN + ", "
                                       + DATA_COLUMN + ", "
                                       + META_COLUMN
                                       + ") VALUES (?,?,?,?,?,?,?,?)";
        log.debug("Preparing query createRecord with '" + createRecordQuery
                  + "'");
        stmtCreateRecord = getConnection().prepareStatement(createRecordQuery);

        /* updateRecord */
        String updateRecordQuery = "UPDATE " + RECORDS + " SET "
                                   + BASE_COLUMN + "=?, "
                                   + DELETED_COLUMN + "=?, "
                                   + INDEXABLE_COLUMN + "=?, "
                                   + MTIME_COLUMN + "=?, "
                                   + DATA_COLUMN + "=?, "
                                   + META_COLUMN + "=? "
                                   + "WHERE " + ID_COLUMN +"=?";
        log.debug("Preparing query updateRecord with '" + updateRecordQuery
                  + "'");
        stmtUpdateRecord = getConnection().prepareStatement(updateRecordQuery);

        /* touchRecord */
        /*String touchRecordQuery = "UPDATE " + RECORDS + " SET "
                                   + MTIME_COLUMN + "=? "
                                   + "WHERE " + ID_COLUMN +"=?";
        log.debug("Preparing query touchRecord with '" + touchRecordQuery
                  + "'");
        stmtTouchRecord = prepareStatement(touchRecordQuery);*/

        /* touchParents */
        String touchParentsQuery = "UPDATE " + RECORDS
                                + " SET " + MTIME_COLUMN + "=? "
                                + " WHERE " + ID_COLUMN + " IN ("
                                + " SELECT " + PARENT_ID_COLUMN
                                + " FROM " + RELATIONS
                                + " WHERE " + CHILD_ID_COLUMN + "=? )";


        log.debug("Preparing query touchParents with '" + touchParentsQuery
                  + "'");
        stmtTouchParents = prepareStatement(touchParentsQuery);

        /* getChildren */
        String getChildrenQuery = "SELECT " + allCells
                                + " FROM " + RELATIONS
                                + " JOIN " + RECORDS
                                + " ON " + RECORDS + "." + ID_COLUMN + "="
                                          + RELATIONS + "." + CHILD_ID_COLUMN
                                + " WHERE " + RELATIONS + "."
                                            + PARENT_ID_COLUMN + "=?"
                                + " ORDER BY " + RECORDS + "." + ID_COLUMN;
        log.debug("Preparing query getChildren with '" + getChildrenQuery
                  + "'");
        stmtGetChildren = prepareStatement(getChildrenQuery);

        /* getParents */
        String getParentsQuery = "SELECT " + allCells
                                + " FROM " + RELATIONS
                                + " JOIN " + RECORDS
                                + " ON " + RECORDS + "." + ID_COLUMN + "="
                                          + RELATIONS + "." + PARENT_ID_COLUMN
                                + " WHERE " + RELATIONS + "."
                                            + CHILD_ID_COLUMN + "=?" 
                                + " ORDER BY " + RECORDS + "." + ID_COLUMN;
        log.debug("Preparing query getParents with '" + getParentsQuery
                  + "'");
        stmtGetParents = prepareStatement(getParentsQuery);

        /* createRelation */
        String createRelation = "INSERT INTO " + RELATIONS
                                + " (" + PARENT_ID_COLUMN + ","
                                       + CHILD_ID_COLUMN
                                       + ") VALUES (?,?)";
        log.debug("Preparing query createRelation with '" +
                                              createRelation + "'");
        stmtCreateRelation = getConnection().prepareStatement(
                                                     createRelation);

        /* deleteRelation */
        /*String deleteRelation = "DELETE FROM " + RELATIONS
                                + " WHERE " + PARENT_ID_COLUMN + "=? "
                                + " OR " + CHILD_ID_COLUMN + "=? ";
        log.debug("Preparing query deleteRelation with '" +
                                              deleteRelation + "'");
        stmtDeleteRelation = getConnection().prepareStatement(
                                                     deleteRelation);*/

        /* countParents */
        /*String countParents = "SELECT count(*) FROM " + RELATIONS
                                + " WHERE " + CHILD_ID_COLUMN + "=?";
        log.debug("Preparing query countParents with '" +
                                              countParents + "'");
        stmtCountParents = getConnection().prepareStatement(
                                                     countParents);*/

        /* countChildren */
        /*String countChildren = "SELECT count(*) FROM " + RELATIONS
                                + " WHERE " + PARENT_ID_COLUMN + "=?";
        log.debug("Preparing query countChildren with '" +
                                              countParents + "'");
        stmtCountChildren = getConnection().prepareStatement(
                                                     countChildren);*/

        log.trace("Finished preparing SQL statements");
    }

    private PreparedStatement prepareStatement(String statement) throws
                                                                 SQLException {
        PreparedStatement preparedStatement =
                getConnection().prepareStatement(statement);
        preparedStatement.setFetchSize(FETCH_SIZE);
        return preparedStatement;
    }

    @Override
    public synchronized long getRecordsModifiedAfter(long time,
                                                     String base,
                                                     QueryOptions options)
                                                        throws IOException {
        log.debug("getRecordsModifiedAfter('" + time + "', " + base
                  + ") entered");

        if (time > getModificationTime(base)) {
            log.debug ("Storage not flushed after " + time + ". Returning"
                       + " empty iterator");
            return EMPTY_ITERATOR_KEY;
        }

        if (base == null) { // All bases
            try {
                stmtGetModifiedAfterAll.setTimestamp(1, new Timestamp(time));
            } catch (SQLException e) {
                throw new IOException(String.format(
                        "Could not prepare stmtGetModifiedAfterAll with time"
                        + " %d", time), e);
            }
            return prepareIterator(stmtGetModifiedAfterAll, options);
        }

        //time += 1000;
        try {
            stmtGetModifiedAfter.setString(1, base);
            stmtGetModifiedAfter.setTimestamp(2, new Timestamp(time));
        } catch (SQLException e) {
            throw new IOException("Could not prepare stmtGetModifiedAfter "
                                      + "with base '" + base + "' and time "
                                      + time, e);
        }
        return prepareIterator(stmtGetModifiedAfter, options);
    }    

    @Override
    public Record getRecord(String id, QueryOptions options)
                                                        throws IOException {
        log.trace("getRecord('" + id + "', " + options + ")");

        try {
            stmtGetRecord.setString(1, id);
        } catch (SQLException e) {
            throw new IOException("Could not prepare stmtGetRecord "
                                      + "with id '" + id + "''", e);
        }

        Record record = null;
        try {
            ResultSet resultSet = stmtGetRecord.executeQuery();

            if (!resultSet.next()) {
                log.debug("No such record '" + id + "'");
                return null;
            }

            try {
                record = scanRecord(resultSet);

                /* Sanity checks if we log on debug */
                if (log.isDebugEnabled()) {
                    if (!id.equals(record.getId())) {
                        log.error ("Record id '" + record.getId()
                                   + "' does not match requested id: " + id);
                    }

                    while (resultSet.next()) {                        
                        Record tmpRec = scanRecord(resultSet);
                        log.warn ("Bogus record in result set: " + tmpRec);
                    }
                }

                if (options != null && !options.allowsRecord(record)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Record '" + id
                                  + "' not allowed by query options. "
                                  + "Returning null");
                    }
                    return null;
                }

                expandRelations(record, options);

            } finally {
                resultSet.close();
            }

        } catch (SQLException e) {
            throw new IOException("SQLException", e);
        }
        return record;
    }

    /* Expand child records if we need to and there indeed
     * are any children to expand */
    private void expandChildRecords (Record record,
                                     RecursionQueryOptions options)
                                                        throws IOException {
        if (options.childRecursionDepth() == 0) {
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
                                               options.decChildRecursionDepth());
            if (children.isEmpty()) {
                record.setChildren(null);
            } else {
                record.setChildren(children);
            }
        }
    }

    /* Expand parent records if we need to and there indeed
     * are any parents to expand */
    private void expandParentRecords (Record record,
                                      RecursionQueryOptions options)
                                                        throws IOException {
        if (options.parentRecursionHeight() == 0) {
            return;
        }

        List<String> parentIds = record.getParentIds();

        if (parentIds != null && parentIds.size() != 0) {

            if (log.isTraceEnabled()) {
                log.trace ("Expanding parents of record '"
                           + record.getId() + "': "
                           + Strings.join(parentIds, ", "));
            }

            List<Record> parents = getRecords(parentIds,
                                              options.decParentRecursionHeight());
            if (parents.isEmpty()) {
                record.setParents(null);
            } else {
                record.setParents(parents);
            }
        }
    }

    @Override
    public Record next(long iteratorKey) throws IOException {
        boolean error = false;
        ResultIterator iterator = iterators.get(iteratorKey);

        if (iterator == null) {
            throw new IllegalArgumentException("No result iterator with key "
                                      + iteratorKey);
        }

        try {
            if (!iterator.hasNext()) {
                iterator.close();
                iterators.remove(iterator.getKey());
                throw new NoSuchElementException ("Iterator " + iteratorKey
                                                  + " depleted");
            }

            Record r = iterator.getRecord();
            return expandRelations(r, iterator.getQueryOptions());
        } catch (SQLException e) {
            error = true;
            throw new IOException("SQLException: " + e.getMessage(), e);
        } finally {
            if (error) {
                log.debug ("Error detected on iterator " + iteratorKey + "."
                           + " Removing");
                iterator.close();
                iterators.remove(iterator.getKey());
            }
        }
    }

    private Record expandRelations(Record r, QueryOptions options)
                                                             throws IOException{
        if (options == null) {
            return r;
        }

        // This also makes sure that the recursion levels are reset
        RecursionQueryOptions opts;

        if (options.childDepth() != 0) {
            opts = RecursionQueryOptions.wrap(options);
            expandChildRecords(r, opts);
        }

        if (options.parentHeight() != 0) {
            opts = RecursionQueryOptions.wrap(options);
            expandParentRecords(r, opts);
        }

        return r;
    }

    @Override
    public void flush(Record record) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("Flushing " + record.toString(true));
        } else if (log.isDebugEnabled()) {
            log.trace("Flushing " + record.toString(false));
        }

        /* Update the timestamp we check agaist in getRecordsModifiedAfter */
        updateModificationTime (record.getBase());

        if (record.isDeleted()){
            log.debug("Deleting record '" + record.getId() + "' from base '"
                      + record.getBase() + "'");
            deleteRecord(record.getId());
        } else {
            createNewRecord(record);
        }

        /* Recursively add child records */
        if (record.getChildren() != null) {
            log.debug ("Flushing " + record.getChildren().size()
                       + " nested child records of '" + record.getId() + "'");
            for (Record child : record.getChildren()) {
                flush(child);
            }
        }

        /* Touch parents recursively upwards
         * FIXME: The call to touchParents() might be pretty expensive... */
        try {
            touchParents(record.getId(), null);
        } catch (IOException e) {
            // Consider this non-fatal
            log.error("Failed to touch parents of '" + record.getId() + "': "
                      + e.getMessage(), e);
        }

        /* Again - update the timestamp we check agaist in
         * getRecordsModifiedAfter. This is also done in the end of the flush()
         * because the operation is non-instantaneous  */
        updateModificationTime (record.getBase());

    }

    /* Recursively touch parents upwards */
    private void touchParents(String id, QueryOptions options)
                                                            throws IOException {
        boolean doTrace = log.isTraceEnabled();

        if (doTrace) {
            log.trace ("Touching parents of '" + id + "'");
        }

        List<Record> parents = getParents(id, options);

        if (parents == null || parents.isEmpty()) {
            if (doTrace) {
                log.trace("No parents to update for record " + id);
            }
            return;
        }

        try {
            long now = System.currentTimeMillis();
            Timestamp nowStamp = new Timestamp(now);
            stmtTouchParents.setTimestamp (1, nowStamp);
            stmtTouchParents.setString(2, id);
            stmtTouchParents.execute();
        } catch (SQLException e) {
            log.error ("Failed to touch parents of '" + id + "': "
                       + e.getMessage(), e);
            // Consider this non-fatal
            return;
        }

        // Recurse upwards
        for (Record parent : parents) {
            touchParents(parent.getId(), options);
        }
    }

    /**
     * Get all immediate parent records of the record with the given id.
     * @param id the id of the record to look up parents for
     * @param options query options to match. Only records returning true
     *                on {@link QueryOptions#allowsRecord(Record)} are returned.
     *                If this argument is {@code null} all records are allowed
     * @return A list of parent records. This list will be empty if there are no
     *         parents
     * @throws IOException on communication errors with the db
     */
    protected List<Record> getParents (String id, QueryOptions options)
                                                            throws IOException {
        List<Record> parents = new ArrayList<Record>(1);

        try {
            stmtGetParents.setString(1, id);
            stmtGetParents.execute();

            ResultSet results = stmtGetParents.getResultSet();
            ResultIterator iter = new ResultIterator(results, null);

            while (iter.hasNext()) {
                try {
                    Record r = iter.getRecord();
                    if (options != null && options.allowsRecord(r)) {
                        parents.add(r);
                    } else if (options == null) {
                        parents.add(r);
                    } else if (log.isTraceEnabled()) {
                        log.trace("Parent record '" + r.getId()
                                  + "' not allowed by query options");
                    }
                } catch (IOException e) {
                    throw new IOException("Error scanning parent records "
                                              + "for '" + id + "': "
                                              + e.getMessage(), e);
                }
            }

            if (log.isTraceEnabled()) {
                log.trace ("Looked up parents for '" + id +"': "
                           + Strings.join (parents, ";"));
            }

            return parents;

        } catch (SQLException e) {
            throw new IOException("Failed to get parents for record '"
                                      + id + "': " + e.getMessage(), e);
        }
    }

    /**
     * Get all immediate child records of the record with the given id.
     * @param id the id of the record to look up children for
     * @param options query options to match. Only records returning true
     *                on {@link QueryOptions#allowsRecord(Record)} are returned.
     *                If this argument is {@code null} all records are allowed
     * @return A list of child records. This list will be empty if there are no
     *         parents
     * @throws IOException on communication errors with the db
     */
    protected List<Record> getChildren (String id, QueryOptions options)
                                                            throws IOException {
        List<Record> children = new ArrayList<Record>(3);

        try {
            stmtGetChildren.setString(1, id);
            stmtGetChildren.execute();

            ResultSet results = stmtGetChildren.getResultSet();
            ResultIterator iter = new ResultIterator(results, null);

            while (iter.hasNext()) {
                Record r = iter.getRecord();
                if (options != null && options.allowsRecord(r)) {
                    children.add(r);
                } else if (options == null) {
                    children.add(r);
                } else if (log.isTraceEnabled()) {
                    log.trace("Parent record '" + r.getId()
                              + "' not allowed by query options");
                }
            }

            if (log.isTraceEnabled()) {
                log.trace ("Looked up children for '" + id +"': "
                           + Strings.join (children, ";"));
            }

            return children;

        } catch (SQLException e) {
            throw new IOException("Failed to get children for record '"
                                      + id + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void clearBase (String base) throws IOException {
        log.info ("Clearing base '" + base + "'");

        try {
            stmtClearBase.setString(1, base);
            stmtClearBase.execute();
            updateModificationTime(base);
        } catch (SQLException e) {
            throw new IOException("SQLException clearing base '"
                                      + base + "'", e);
        }
    }

    /* Create parent/child and child/parent relations for the given record */
    private void createRelations (Record rec) throws SQLException {
        // FIXME: Some transactional safety here would be nice
        if (rec.getChildIds() != null) {
            for (String childId : rec.getChildIds()) {
                if (log.isDebugEnabled()) {
                    log.debug ("Creating relation: " + rec.getId()
                               + " -> " + childId);
                }
                stmtCreateRelation.setString(1, rec.getId());
                stmtCreateRelation.setString(2, childId);

                try {
                    stmtCreateRelation.execute();
                } catch (SQLIntegrityConstraintViolationException e) {
                    if (log.isDebugEnabled()) {
                        log.debug ("Relation "+ rec.getId() + " -> "
                                   + childId + ", already known");
                    }
                }
            }
        }

        if (rec.getParentIds() != null) {
            for (String parentId : rec.getParentIds()) {
                if (log.isDebugEnabled()) {
                    log.debug ("Creating relation: " + parentId
                               + " -> " + rec.getId());
                }
                stmtCreateRelation.setString(1, parentId);
                stmtCreateRelation.setString(2, rec.getId());

                try {
                    stmtCreateRelation.execute();
                } catch (SQLIntegrityConstraintViolationException e) {
                    if (log.isDebugEnabled()) {
                        log.debug ("Relation "+ parentId + " -> "
                                   + rec.getId() + ", already known");
                    }
                }
            }
        }
    }

    private void createNewRecord(Record record) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("Preparing to create or update record: "
                      + record.toString(true));
        }

        long now = System.currentTimeMillis();
        Timestamp nowStamp = new Timestamp(now);

        try {
            stmtCreateRecord.setString(1, record.getId());
            stmtCreateRecord.setString(2, record.getBase());
            stmtCreateRecord.setInt(3, boolToInt(record.isDeleted()));
            stmtCreateRecord.setInt(4, boolToInt(record.isIndexable()));
            stmtCreateRecord.setTimestamp(5, nowStamp);
            stmtCreateRecord.setTimestamp(6, nowStamp);
            stmtCreateRecord.setBytes(7, Zips.gzipBuffer(record.getContent()));
            stmtCreateRecord.setBytes(8,record.hasMeta() ?
                                         record.getMeta().toFormalBytes() :
                                         new byte[0]);
            stmtCreateRecord.execute();
            if (log.isDebugEnabled()) {
                log.debug("Created new record: " + record);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            if (log.isTraceEnabled()) {
                log.trace ("Record '" + record.getId() + "' already stored. "
                           + "Updating instead");
            }
            updateRecord(record);
            if (log.isDebugEnabled()) {
                log.debug("Updated record: " + record);
            }

            return;

        } catch (SQLException e) {
            throw new IOException("SQLException creating new record " + record
                                  + ": " + e.getMessage(), e);
        }

        /* We must create the relations before calling updateRelations() or else
         * said method will recurse infinitely */
        try {
            createRelations(record);
        } catch (SQLException e) {
            throw new IOException("Error creating relations for "
                                      + record + ": " + e.getMessage(),
                                      e);
        }
        // FIXME: Is this call really not just an expensive no-op?
        updateRelations(record);
    }

    /* Note that creationTime isn't touched */
    private void updateRecord(Record record) throws IOException {
        // FIXME: Add child records recursively (parents?)
        long now = System.currentTimeMillis();
        Timestamp nowStamp = new Timestamp(now);

        try {
            stmtUpdateRecord.setString(1, record.getBase());
            stmtUpdateRecord.setInt(2, boolToInt(record.isDeleted()));
            stmtUpdateRecord.setInt(3, boolToInt(record.isIndexable()));
            stmtUpdateRecord.setTimestamp(4, nowStamp);
            stmtUpdateRecord.setBytes(5, Zips.gzipBuffer(record.getContent()));
            stmtUpdateRecord.setBytes(6, record.hasMeta() ?
                                         record.getMeta().toFormalBytes() :
                                         new byte[0]);
            stmtUpdateRecord.setString(7, record.getId());
            stmtUpdateRecord.execute();

            if (stmtUpdateRecord.getUpdateCount() == 0) {
                log.warn("The record with id '" + record.getId()
                         + "' was marked as modified, but did not exist in the"
                         + " database. The record will be added as new");
                createNewRecord(record);
                return;
            }
        } catch (SQLException e) {
            throw new IOException("SQLException updating record '"
                                      + record.getId() + "'", e);
        }

        /* We must create the relations before calling updateRelations() or else
         * said method will recurse infinitely */
        try {
            createRelations(record);
        } catch (SQLException e) {
            throw new IOException("Error creating relations for '"
                                      + record.getId() + "': " + e.getMessage(),
                                      e);
        }

        // FIXME: Is this call really not just an expensive no-op?
        updateRelations(record);
     }

    private int deleteRecord(String id) throws IOException {
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
            throw new IOException("SQLException deleting record '"
                                      + id + "'", e);
        }

        // FIXME: Should we updateRelations()?
    }

    /*public void touchRecord(String id, long lastModified) throws
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
    }*/

    /**
     * Creates the tables {@link #RECORDS} and {@link #RELATIONS} and relevant
     * indexes on the database.
     *
     * @throws IOException if the database could not be created.
     */
    protected void createSchema() throws IOException {
        log.debug("Creating database schema");
        try {
            // Do this in a separate call to avoid massively nested code block
            doCreateSchema();
        } catch (SQLException e) {
            throw new IOException("Could not create schema: "
                                      + e.getMessage(), e);
        }
    }

    private void doCreateSchema() throws SQLException {

        /* RECORDS */
        String createRecordsQuery =
                "CREATE TABLE " + RECORDS + " ("
                + ID_COLUMN        + " VARCHAR(" + ID_LIMIT + ") PRIMARY KEY, "
                + BASE_COLUMN      + " VARCHAR(" + BASE_LIMIT + "), "
                + DELETED_COLUMN   + " INTEGER, "
                + INDEXABLE_COLUMN + " INTEGER, "
                + DATA_COLUMN      + " " + getDataColumnDataDeclaration()
                + CTIME_COLUMN     + " TIMESTAMP, "
                + MTIME_COLUMN     + " TIMESTAMP, "
                + META_COLUMN      + " " + getMetaColumnDataDeclaration()
                + ")";
        log.debug("Creating table "+RECORDS+" with query: '"
                  + createRecordsQuery + "'");

        Statement stmt = getConnection().createStatement();
        stmt.execute(createRecordsQuery);
        stmt.close();

        /* RECORDS INDEXES */
        String createRecordsIdIndexQuery =
                "CREATE UNIQUE INDEX i ON " + RECORDS + "("+ID_COLUMN+")";
        log.debug("Creating index 'i' on table "+RECORDS+" with query: '"
                  + createRecordsIdIndexQuery + "'");
        stmt = getConnection().createStatement();
        stmt.execute(createRecordsIdIndexQuery);
        stmt.close();

        String createRecordsMTimeIndexQuery =
                "CREATE INDEX m ON " + RECORDS + "("+MTIME_COLUMN+")";
        log.debug("Creating index 'm' on table "+RECORDS+" with query: '"
                  + createRecordsMTimeIndexQuery + "'");
        stmt = getConnection().createStatement();
        stmt.execute(createRecordsMTimeIndexQuery);
        stmt.close();

        /* RELATIONS */
        String createRelationsQuery =
                "CREATE TABLE " + RELATIONS + " ("
                + PARENT_ID_COLUMN     + " VARCHAR(" + ID_LIMIT + "), "
                + CHILD_ID_COLUMN      + " VARCHAR(" + ID_LIMIT + ") )";
        log.debug("Creating table "+RELATIONS+" with query: '"
                  + createRelationsQuery + "'");
        stmt = getConnection().createStatement();
        stmt.execute(createRelationsQuery);
        stmt.close();

        /* RELATIONS INDEXES */
        String createRelationsPCIndexQuery =
                "CREATE UNIQUE INDEX pc ON "
                + RELATIONS + "("+PARENT_ID_COLUMN+","+CHILD_ID_COLUMN+")";
        log.debug("Creating index 'pc' on table "+RELATIONS+" with query: '"
                  + createRelationsPCIndexQuery + "'");
        stmt = getConnection().createStatement();
        stmt.execute(createRelationsPCIndexQuery);
        stmt.close();

        String createRelationsCIndexQuery =
                "CREATE INDEX c ON "
                + RELATIONS + "("+CHILD_ID_COLUMN+ ")";
        log.debug("Creating index 'c' on table "+RELATIONS+" with query: '"
                  + createRelationsCIndexQuery + "'");
        stmt = getConnection().createStatement();
        stmt.execute(createRelationsCIndexQuery);
        stmt.close();
    }

    protected abstract String getMetaColumnDataDeclaration();

    protected abstract String getDataColumnDataDeclaration();

    /**
     * Extract elements from a SQL result set and create a Record from these
     * elements.
     * <p/>
     * This method will position the result set at the beginning of the next
     * record
     *
     * @param resultSet     a SQL result set. The result set will be stepped
     *                      to the beginning of the following record
     * @param iter          a result iterator on which to update record
     *                      availability via {@link ResultIterator#setResultSetHasNext}
     * @return              a Record based on the result set.
     * @throws SQLException if there was a problem extracting values from the
     *                      SQL result set.
     * @throws IOException  If the data (content) could not be uncompressed
     *                      with gunzip.
     */
    protected static Record scanRecord(ResultSet resultSet, ResultIterator iter)
                                              throws SQLException, IOException {

        boolean hasNext;

        String id = resultSet.getString(ID_COLUMN);
        String base = resultSet.getString(BASE_COLUMN);
        boolean deleted = intToBool(resultSet.getInt(DELETED_COLUMN));
        boolean indexable = intToBool(resultSet.getInt(INDEXABLE_COLUMN));
        byte[] gzippedContent = resultSet.getBytes(DATA_COLUMN);
        long ctime = resultSet.getTimestamp(CTIME_COLUMN).getTime();
        long mtime = resultSet.getTimestamp(MTIME_COLUMN).getTime();
        String parentIds = resultSet.getString(PARENT_ID_COLUMN);
        String childIds = resultSet.getString(CHILD_ID_COLUMN);
        byte[] meta = resultSet.getBytes(META_COLUMN);

        if (log.isTraceEnabled()) {
            log.trace ("Scanning record: " + id);
        }

        /* If the record is listed as parent or child of something this will
         * appear in the parent/child columns, so ignore these cases */
        if (id.equals(parentIds)) {
            parentIds = null;
        }
        if (id.equals(childIds)) {
            childIds = null;
        }

        /* The way the result is returned from the DB is several identical rows
         * with different parent and child column values. We need to iterate
         * through all rows with the same id and collect the different parents
         * and children listed */
        while ((hasNext = resultSet.next()) &&
               id.equals(resultSet.getString(ID_COLUMN))) {

            /* If we log on debug we do sanity checking of the result set.
            * Of course the parent and child columns should not be checked,
            * since they are the ones changing */
            if (log.isDebugEnabled()) {
                log.trace("Sanity checking record block for: " + id);
                if (!base.equals(resultSet.getString(BASE_COLUMN))) {
                    log.warn("Base mismatch for record: " + id);
                    return null;
                } else if (deleted != intToBool(resultSet.getInt(DELETED_COLUMN))) {
                    log.warn("Deleted state mismatch for record: " + id);
                    return null;
                } else if (indexable != intToBool(resultSet.getInt(INDEXABLE_COLUMN))) {
                    log.warn("Indexable state mismatch for record: " + id);
                    return null;
                } else if (!Arrays.equals(gzippedContent, resultSet.getBytes(DATA_COLUMN))) {
                    log.warn("Content mismatch for record: " + id);
                    return null;
                }  else if (ctime != resultSet.getTimestamp(CTIME_COLUMN).getTime()) {
                    log.warn("CTime state mismatch for record: " + id);
                    return null;
                } else if (mtime != resultSet.getTimestamp(MTIME_COLUMN).getTime()) {
                    log.warn("MTime state mismatch for record: " + id);
                    return null;
                }  else if (!Arrays.equals(meta,resultSet.getBytes(META_COLUMN))) {
                    log.warn("Meta tags mismatch for record: " + id);
                    return null;
                }
            }

            /* Pick up parent and child ids */
            String newParent = resultSet.getString (PARENT_ID_COLUMN);
            String newChild = resultSet.getString (CHILD_ID_COLUMN);

            /* If the record is listed as parent or child of something this
             * will appear in the parent/child columns, so ignore these cases */
            if (id.equals(newParent)) {
                newParent = null;
            }
            if (id.equals(newChild)) {
                newChild = null;
            }

            if (newParent != null) {
                parentIds = parentIds != null ?
                                (parentIds + ";" + newParent) : newParent;
            }

            if (newChild != null) {
                childIds = childIds != null ?
                                (childIds + ";" + newChild) : newChild;
            }

            if (log.isTraceEnabled()) {
                log.trace ("For record '" + id + "', collected children: "
                           + childIds + ", collected parents: " + parentIds);
            }
        }

        if (iter != null) {
            iter.setResultSetHasNext(hasNext);
        }

        /* The result set cursor will now be on the start of the next record */

        /* Create a record with gzipped content. The content will be unzipped
         * lazily by the Record class upon access */
        return new Record(id,
                          base,
                          deleted,
                          indexable,
                          gzippedContent,
                          ctime,
                          mtime,
                          Record.idStringToList(parentIds),
                          Record.idStringToList(childIds),
                          StringMap.fromFormal(meta),
                          true);
    }

    /**
     * As {@link #scanRecord(ResultSet,ResultIterator)} with
     * {@code resultSet = null}.
     */
    public Record scanRecord (ResultSet resultSet)
                                              throws SQLException, IOException {
        return scanRecord(resultSet, null);
    }

    /**
     * Given a query, execute this query and transform the resultset to a
     * RecordIterator.
     * @param statement the statement to execute.
     * @return a RecordIterator of the result.
     * @throws IOException - also on no getConnection() and SQLExceptions.
     */
    private long prepareIterator(PreparedStatement statement,
                                 QueryOptions options) throws
                                                               IOException {
        log.debug("Getting results for '" + statement + "'");
        ResultSet resultSet;
         try {
//             longConn.setAutoCommit(false);
             resultSet = statement.executeQuery();
             /*
               FIXME: assert that resultSet.getType() != ResultSet.TYPE_FORWARD_ONLY,
               or else resultSet.previous() will fail.
               If this fails we might need some class derived from ResultSet that
               can do manual caching for us
               */
             log.debug("Got resultSet from '" + statement.toString() + "'");
         } catch (SQLException e) {
             log.error("SQLException in prepareIterator", e);
             throw new IOException("SQLException", e);
         }
         return createRecordIterator(resultSet, options);
    }
    
    
    /**
     * Creates new RecordIterator from the given resultset and getConnection(),
     * i.e. creates new key and saves the given resultset and getConnection() and
     * "forwards" the resultset by one row to keep a step ahead.
     * @param resultSet the results to wrap.
     * @return an iterator over the resultset
     * @throws IOException on SQLException
     */
    private long createRecordIterator(ResultSet resultSet,
                                      QueryOptions options)
                                                       throws IOException {
        log.trace("createRecordIterator entered with result set " + resultSet);
        ResultIterator iterator;
        try {
            iterator = new ResultIterator(resultSet, options);
            log.trace("createRecordIterator: Got iterator " + iterator.getKey()
                      + " adding to iterator-list");
            iterators.put(iterator.getKey(), iterator);
        } catch (SQLException e) {
            log.error("SQLException creating record iterator", e);
            throw new IOException("SQLException creating record iterator",
                                      e);
        }
        log.trace("returning new StorageIterator");
        return iterator.getKey();
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
     * @throws IOException if the info could not be retireved.
     */
    public String getDatabaseInfo() throws IOException {
        try {
            return getConnection().getMetaData().getDatabaseProductName();
        } catch (SQLException e) {
            throw new IOException("Could not get catalog info", e);
        }
    }

    @Override
    public void close() throws IOException {
        log.info("Closing");

        iteratorReaper.stop();

        try {
            getConnection().close();
            if (!getConnection().isClosed()) {
                throw new IOException("close was called on the connection, "
                                          + "but the connection state is not "
                                          + "closed");
            }
        } catch (SQLException e) {
            throw new IOException("SQLException when closing connection",
                                      e);
        }

        log.info("Closed");
    }

    protected class ResultIterator {
        private Log log = LogFactory.getLog(ResultIterator.class);

        private long key;        
        private long lastAccess;
        private ResultSet resultSet;
        private boolean resultSetHasNext;
        private Record nextRecord;
        private RecursionQueryOptions options;

        public ResultIterator(ResultSet resultSet,
                              QueryOptions options)
                                              throws SQLException, IOException {
            this.resultSet = resultSet;
            this.options = options == null ?
                                      null : new RecursionQueryOptions(options);

            // The iterator start "outside" the result set, so step into it
            resultSetHasNext = resultSet.next();

            // This also updates resultSetHasNext
            nextRecord = nextValidRecord();

            log.trace("Constructed Record iterator with initial hasNext: "
                      + resultSetHasNext);
            
            key = System.currentTimeMillis();
            lastAccess = key;
        }

        public long getLastAccess() {
            return lastAccess;
        }
        
        public RecursionQueryOptions getQueryOptions () {
            return options;
        }

        public long getKey() {
            lastAccess = System.currentTimeMillis();
            return key;
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
         * @throws IOException if the Record data could not be gunzipped.
         */
        public Record getRecord() throws SQLException, IOException {
            lastAccess = System.currentTimeMillis();

            if (nextRecord == null) {
                throw new NoSuchElementException("Iterator " + key
                                                 + " depleted");
            }


            Record record = nextRecord;
            nextRecord = nextValidRecord();

            if (log.isTraceEnabled()) {
                log.trace("getRecord returning '" + record.getId() + "'");
            }

            /* The naive solution to updating resultSetHasNext is to just do:
             *   resultSetHasNext = !resultSet.isAfterLast();
             * here, but the ResultSet type does not allow this unless it is
             * of a scrollable type, which we do not use for resource limitation
             * purposes. Instead the resultSetHasNext state is updated ny scanRecord()
             */            

            return record;
//            rs.getStatement().close();//note: rs is closed when stmt is closed
        }

        private Record nextValidRecord () throws SQLException, IOException {
            // This check should _not_ use the method resultSetHasNext() because
            // it is only the state of the resultSet that is important here
            if (!resultSetHasNext) {
                return null;
            }

            /* scanRecord() steps the resultSet to the next record.
             * It will update the state of the iterator appropriately */
            Record r = DatabaseStorage.scanRecord(resultSet, this);

            // Allow all mode
            if (options == null) {
                return r;
            }

            while (resultSetHasNext && !options.allowsRecord(r)) {
                r = DatabaseStorage.scanRecord(resultSet, this);
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
            log.warn("Close in ResultIterator not implemented yet");
//            rs.getStatement().close();//note: rs is closed when stmt is closed
//            resultSet.close();
            // TODO: Implement this
        }

        /**
         * Called from
         * {@link DatabaseStorage#scanRecord(ResultSet,ResultIterator)}.
         * @param resultSetHasNext
         */
        public void setResultSetHasNext(boolean resultSetHasNext) {
            this.resultSetHasNext = resultSetHasNext;
        }
    }

    /**
     * Helper class to clean up unused iterators. It should be started
     * via the runInThread() method
     */
    private static class ResultIteratorReaper implements Runnable {

        private Map<Long,ResultIterator> iterators;
        private Thread t;
        private boolean mayRun;
        private long graceTimeMinutes;
        
        public ResultIteratorReaper (Map<Long,ResultIterator> iterators,
                                     long graceTimeMinutes) {
            this.iterators = iterators;
            this.graceTimeMinutes = graceTimeMinutes;
            mayRun = true;
        }
        
        public void runInThread () {
            t = new Thread(this);
            t.setDaemon(true); // Allow JVM to exit we the reaper is running
            t.start();
        }

        public void stop () {
            log.debug("Got stop request");
            mayRun = false;
            t.interrupt();
        }

        public void run() {
            log.info("Starting");
            while (mayRun) {
                try {
                    Thread.sleep(graceTimeMinutes*60*60);
                    fullSweep();
                } catch (InterruptedException e) {
                    log.info("Interrupted");
                }
            }
            log.info("Stopped");
        }

        private void fullSweep() {
            log.debug("Scanning iterators for timeouts");

            long now = System.currentTimeMillis();
            List<Long> deadIters = new ArrayList<Long>();

            for (ResultIterator iter : iterators.values()) {
                if (iter.getLastAccess() + graceTimeMinutes*60*60 <= now) {
                    deadIters.add(iter.getKey());
                }
            }

            for (Long key : deadIters) {
                ResultIterator iter = iterators.remove(key);

                if (iter != null) {
                    log.info("Iterator " + iter.getKey() + " timed out");
                } else {
                    log.warn("Iterator " + iter.getKey() + " timed out, "
                             + "but is not around anymore");
                }
            }

            log.debug("Scan complete");
        }
    }
    
    /*private int countParents(String id) {
        ResultSet results = null;

        try {
            stmtCountParents.setString(0, id);
            stmtCountParents.execute();
            results = stmtCountParents.getResultSet();

            if (!results.next()) {
                log.warn("No parent count returned for '" + id + "'. "
                         + "Returning -1");
                return -1;
            }

            int count = results.getInt(0);

            if (results.next()) {
                log.warn("countParents query returned more than one row. " +
                         "This should never happen");
            }

            return count;
        } catch (SQLException e) {
            log.warn("Failed to count parents of '" + id + "': "
                     + e.getMessage() + ". Returning -1", e);
            return -1;
        } finally {
            if (results != null) {
                try {
                    results.close();
                } catch (SQLException e) {
                    log.error("Failed to close result set for countParents " +
                              "query", e);
                }
            }
        }
    }*/

    /*private int countChildren(String id) {
        ResultSet results = null;

        try {
            stmtCountChildren.setString(0, id);
            stmtCountChildren.execute();
            results = stmtCountChildren.getResultSet();

            if (!results.next()) {
                log.warn("No child count returned for '" + id + "'. "
                         + "Returning -1");
                return -1;
            }

            int count = results.getInt(0);

            if (results.next()) {
                log.warn("countChildren query returned more than one row. " +
                         "This should never happen");
            }

            return count;
        } catch (SQLException e) {
            log.warn("Failed to count children of '" + id + "': "
                     + e.getMessage() + ". Returning -1", e);
            return -1;
        } finally {
            if (results != null) {
                try {
                    results.close();
                } catch (SQLException e) {
                    log.error("Failed to close result set for countChildren " +
                              "query", e);
                }
            }
        }
    }*/
}



