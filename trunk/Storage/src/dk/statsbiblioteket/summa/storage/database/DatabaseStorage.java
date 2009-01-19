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
import dk.statsbiblioteket.summa.storage.database.cursors.Cursor;
import dk.statsbiblioteket.summa.storage.database.cursors.PagingCursor;
import dk.statsbiblioteket.summa.storage.database.cursors.ResultSetCursor;
import dk.statsbiblioteket.summa.storage.database.cursors.CursorReaper;
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
    public static final String CONF_USERNAME = "summa.storage.database.username";
    /**
     * Default value for {@link #CONF_USERNAME}.
     */
    public static final String DEFAULT_USERNAME = "summa";

    /**
     * The property-key for the the password for the underlying database, if
     * needed.
     */
    public static final String CONF_PASSWORD = "summa.storage.database.password";

    /**
     * The name of the database to connect to. Default value is
     * {@link #DEFAULT_DATABASE}.
     */
    public static final String CONF_DATABASE = "summa.storage.database.name";

    /**
     * Default value for {@link #CONF_DATABASE}.
     */
    public static final String DEFAULT_DATABASE = "summa";

    /**
     * Maximum number of concurrent connections to keep to the database.
     * Default value is {@link #DEFAULT_MAX_CONNECTIONS}.
     */
    public static final String CONF_MAX_CONNECTIONS =
                                        "summa.storage.database.maxconnections";

    /**
     * Default value for {@link #CONF_MAX_CONNECTIONS}.
     */
    public static final int DEFAULT_MAX_CONNECTIONS = 10;

    /**
     * The port to connect to the datatbase on.
     */
    public static final String CONF_PORT = "summa.storage.database.port";

    /**
     * The hostname to connect to the datatbase on. Default is
     * {@link #DEFAULT_HOST}.
     */
    public static final String CONF_HOST = "summa.storage.database.host";

    /**
     * Default value for {@link #CONF_HOST}.
     */
    public static final String DEFAULT_HOST = "localhost";

    /**
     * The property-key for the boolean value determining if a new database
     * should be created is there is no existing database. If createnew is
     * true and a database exists and forcenew is true, the existing database
     * is deleted and a new one created. If createnew is true and a database
     * exists and forcenew is false, the existing database is reused.
     */
    public static final String CONF_CREATENEW = "summa.storage.database.createnew";

    /**
     * The property-key for the boolean determining if a new database should
     * be created, no matter is a database already exists.
     */
    public static final String CONF_FORCENEW = "summa.storage.database.forcenew";

    /**
     * The location of the database to use/create. If the location is not an
     * absolute path, it will be appended to the System property "
     * summa.control.client.persistent.dir". If that system property does not
     * exist, the location will be relative to the current dir.
     */
    public static final String CONF_LOCATION = "summa.storage.database.location";

    /**
     * Number of hits to return per-page. This configuration option is only
     * used if the actual storage implementation request that a paging model
     * be used to scan large result sets.
     * <p/>
     * The default value for this property is {@link #DEFAULT_PAGE_SIZE}
     */
    public static final String CONF_PAGE_SIZE =
                                             "summa.storage.database.pagesize";

    /**
     * Default value for the {@link #CONF_PAGE_SIZE} property
     */
    public static final int DEFAULT_PAGE_SIZE = 500;

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
     * The {@code id} column contains the unique identifier for a given record
     * in the database. The value that is mapped directly to the {@code id}
     * value of the constructed {@link Record} objects.
     */
    public static final String ID_COLUMN        = "id";

    /**
     * The {@code base} column contains the value that is mapped directly to
     * the {@code base} of the {@link Record} objects retrieved from this
     * storage.
     * <p/>
     * Generally the record base is used to track provenance and decide which
     * filters/XSLTs to apply to the contents and so forth.
     */
    public static final String BASE_COLUMN      = "base";

    /**
     * The {@code deleted} column signifies whether the record should be treated
     * as non-existing. If deleted records should be retrieved or not can
     *  be specified by setting the relevant flags in the {@link QueryOptions}
     * passed to the storage
     *
     */
    public static final String DELETED_COLUMN   = "deleted";

    /**
     * The {@code indexable} column signifies whether the indexer should index
     * the given record. Whether or not to retrieve non-indexable records
     * can be handled by setting the appropriate {@link QueryOptions} in
     * {#getRecordsModifiedAfter}
     */
    public static final String INDEXABLE_COLUMN = "indexable";


    /**
     * The {@code data} column contains the raw record-data as ingested.
     */
    public static final String DATA_COLUMN      = "data";

    /**
     * The {@code ctime} column signifies the time of record creation in the
     * database.
     * <p/>
     * The value stored here is a unique timestamp generated with a
     * {@link UniqueTimestampGenerator} which means it does not map directly
     * to the system time. On record construction time the timestamp is
     * mapped back to a system time value and can be inspected via
     * {@link Record#getCreationTime()} as usual.
     */
    public static final String CTIME_COLUMN     = "ctime";

    /**
     * The {@code mtime} column signifies the time of record modification in the
     * database. This timestamp is used when {@link #getRecordsModifiedAfter} is
     * called.
     * <p/>
     * The value stored here is a unique timestamp generated with a
     * {@link UniqueTimestampGenerator} which means it does not map directly
     * to the system time. On record construction time the timestamp is
     * mapped back to a system time value and can be inspected via
     * {@link Record#getModificationTime()} as usual.
     */
    public static final String MTIME_COLUMN     = "mtime";

    /**
     * The {@code meta} column contains meta-data for the Record in the form of
     * key-value pairs of Strings. See {@link StringMap#toFormal()} for format.
     * These values are mapped to the {@link Record} meta contents which can
     * be handled with {@link Record#getMeta()} and friends.
     */
    public static final String META_COLUMN  = "meta";

    /**
     * First column in the {@link #RECORDS} table. Contains the record id of the
     * parent in a parent/child relationship. The refered parent does
     * not need to be present in the database.
     */
    public static final String PARENT_ID_COLUMN = "parentId";

    /**
     * Second row in the {@link #RELATIONS} table. Contains the record
     * id of the child record. The refered child record does not need to
     * exist in the database.
     */
    public static final String CHILD_ID_COLUMN = "childId";

    /* Constants for database-setup */
    public static final int ID_LIMIT =       255;
    public static final int BASE_LIMIT =     31;
    public static final int DATA_LIMIT =     50*1024*1024;
    private static final int FETCH_SIZE = 100;

    private static final long EMPTY_ITERATOR_KEY = -1;

    private StatementHandle stmtGetModifiedAfter;
    private StatementHandle stmtGetModifiedAfterAll;
    private StatementHandle stmtGetRecord;
    private StatementHandle stmtClearBase;
    private StatementHandle stmtDeleteRecord;
    private StatementHandle stmtCreateRecord;
    private StatementHandle stmtUpdateRecord;
    private StatementHandle stmtTouchRecord;
    private StatementHandle stmtTouchParents;
    private StatementHandle stmtGetChildren;
    private StatementHandle stmtGetParents;
    private StatementHandle stmtCreateRelation;

    private Map<Long, Cursor> iterators =
                                         new HashMap<Long, Cursor>(10);

    private CursorReaper iteratorReaper;
    private UniqueTimestampGenerator timestampGenerator;

    private boolean useLazyRelations;
    private boolean usePagingModel;
    private int pageSize;

    /**
     * Opaque datatype used to represent preparedStatements. Implementors
     * of the {@link DatabaseStorage} class should provide custom
     * implementations of this datatype and return it from
     * {@code #prepareStatement()}
     */
    public interface StatementHandle {}

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

    public DatabaseStorage(Configuration conf) throws IOException {
        super(updateConfiguration(conf));

        timestampGenerator = new UniqueTimestampGenerator();
        iteratorReaper =
               new CursorReaper(iterators,
                                        conf.getLong(CONF_ITERATOR_TIMEOUT,
                                                     DEFAULT_ITERATOR_TIMEOUT));

        usePagingModel = usePagingResultSets();
        useLazyRelations = useLazyRelationLookups();

        if (usePagingModel) {
            pageSize = conf.getInt(CONF_PAGE_SIZE, DEFAULT_PAGE_SIZE);
            log.debug("Using paging model for large result sets. Page size: "
                      + pageSize);
        } else {
            log.debug("Using default model for large result sets");
            pageSize = -1;
        }

        if (useLazyRelations) {
            log.debug("Using lazy relation resolution");
        } else {
            log.debug("Using direct relation resolution");
        }
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
            throw new IOException("Failed to prepare SQL statements: "
                                  + e.getMessage(), e);
        }

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
     * Look up a connection from a connection pool. The returned connection
     * <i>must</i> be closed by the caller. This should typically be done in a
     * <code>finally</code> clause.
     * <p/>
     * Unclosed connections will lead to connection leaking which can end up
     * locking up the entire storage
     *
     * @return a connection to a SQL-compatible database. The returned
     *         connection <i>must</i> be closed by the caller to avoid
     *         connection leaking
     */
    protected abstract Connection getConnection();

    /**
     * Set up a prepared statement and return an opaque handle to it.
     * <p/>
     * A copy of the {@link PreparedStatement} can be retrieved by calling
     * {@link #getStatement}. PreparedStatements <i>must</i> be
     * closed by invoking the {@link PreparedStatement#close} method when the
     * client is done using them. This should typically be done in a
     * <code>finally</code> clause to make sure statements
     * (and thus connections) are not leaked.
     * <p/>
     * Connection leaking can end up locking up the entire storage process
     * @param sql SQL statement for the prepared statement
     * @return a handle that can be used to retrieve a PreparedStatement
     */
    protected abstract StatementHandle prepareStatement(String sql)
                                                            throws SQLException;

    /**
     * Look up a prepared statement given a handle as constructed by
     * {@link #prepareStatement(String)}.
     * <p/>
     * Note that PreparedStatements <i>must</i> be
     * closed by invoking the {@link PreparedStatement#close} method when the
     * client is done using them. This should typically be done in a
     * <code>finally</code> clause to make sure statements
     * (and thus connections) are not leaked.
     * <p/>
     * Connection leaking can end up locking up the entire storage process
     *
     * @param handle statement handle as obtained when calling
     *               {@link #prepareStatement(String)}
     * @return a prepared statement that <i>must</i> be closed by the caller
     */
    protected abstract PreparedStatement getStatement(StatementHandle handle)
                                                            throws SQLException;

    /**
     * Not all database backends return real
     * {@link SQLIntegrityConstraintViolationException}s when they should, but
     * use custom vendor-specific error codes that can be retrieved by calling
     * {@link java.sql.SQLException#getErrorCode()}.
     * <p/>
     * The default implementation of this method simply checks if {@code e}
     * is {@code instanceof SQLIntegrityConstraintViolationExceptio}.
     *
     * @param e the sql exception to inspect the real cause for
     * @return whether or not {@code e} was due to an integrity constraint
     *         violation
     */
    protected boolean isIntegrityConstraintViolation (SQLException e) {
        return e instanceof SQLIntegrityConstraintViolationException;
    }

    /**
     * Return whether the database must do manual paging over the result sets
     * or the cursoring supplied by the database is sufficient. DatabaseStorage
     * will invoke this method one once during its initialization and never
     * again.
     * <p/>
     * If this method returns {@code true} the
     * {@link #getPagingStatement(String)} will be called with the SQL
     * statements the DatabaseStorage wants paging versions of.
     * <p/>
     * The default implementation of this method returns {@code false}
     * @return whether or not to use a manual client side paging model for
     *         retrieval of large result sets
     */
    protected boolean usePagingResultSets() {
        return false;
    }

    /**
     * Return whether or not Record relations should be resolved lazily
     * (in other words "on-the-fly") or if they should be resolved "directly"
     * using an SQL JOIN.
     * <p/>
     * Some databases perform badly when doing JOINs on tables with a lot of
     * rows. This appear especially to be the case for Java embedded databases
     * like Derby and H2.
     * <p/>
     * The default implementation of this method returns {@code false}, meaning
     * that JOINs should be used.
     * @return {@code false} if JOINs should be used to fetch records and their
     *         relation in one go, or {@code true} if relations should be looked
     *         up in a separate SQL call
     */
    protected boolean useLazyRelationLookups() {
        return false;
    }

    /**
     * Return an altered version of the input {@code sql} which adds one extra
     * '?' parameter, <i>to the end of the SQL statement</i>, which can be used
     * to limit the number of rows returned.
     * <p/>
     * For many databases a legal implementation of this method would simply
     * return:<br/>
     * <pre>
     *   sql + " LIMIT ?"
     * </pre>
     * <p/>
     * The default implementation of this method throws an
     * {@link UnsupportedOperationException}.
     * @param sql Input SQL statement on which to append another parameter that
     *            limits the number of rows returned
     * @return A modified version of {@code sql} that adds a new '?' parameter
     *         to the statement that will limit the number of rows returned
     */
    protected String getPagingStatement(String sql) {
        throw new UnsupportedOperationException("Non-paging data model");
    }

    public UniqueTimestampGenerator getTimestampGenerator() {
        return timestampGenerator;
    }

    public int getPageSize() {
        return pageSize;
    }

    /**
     * Prepare relevant SQL statements for later use.
     * @throws SQLException if the syntax of a statement was wrong or the
     *                      connection to the database was faulty.
     */
    private void prepareStatements() throws SQLException {
        log.debug("Preparing SQL statements");

        String allCellsRelationsCols;
        if (useLazyRelations) {
            // Always select empty cells for relations and look them up later
            allCellsRelationsCols = "'', ''";
        } else {
            allCellsRelationsCols = RELATIONS + "." + PARENT_ID_COLUMN + ","
                                  + RELATIONS + "." + CHILD_ID_COLUMN;
        }


        String allCells = RECORDS + "." + ID_COLUMN + ","
                          + RECORDS + "." + BASE_COLUMN + ","
                          + RECORDS + "." + DELETED_COLUMN + ","
                          + RECORDS + "." + INDEXABLE_COLUMN + ","
                          + RECORDS + "." + DATA_COLUMN + ","
                          + RECORDS + "." + CTIME_COLUMN + ","
                          + RECORDS + "." + MTIME_COLUMN + ","
                          + RECORDS + "." + META_COLUMN + ","
                          + allCellsRelationsCols;

        String relationsClause = RECORDS + "." + ID_COLUMN + "="
                                + RELATIONS + "." + PARENT_ID_COLUMN
                                + " OR " + RECORDS + "." + ID_COLUMN + "="
                                + RELATIONS + "." + CHILD_ID_COLUMN;

        // We can order by mtime only because the generated mtimes are unique
        String modifiedAfterQuery = "SELECT " + allCells
                                    + " FROM " + RECORDS
                                    + " LEFT JOIN " + RELATIONS
                                    + " ON " + relationsClause
                                    + " WHERE " + BASE_COLUMN + "=?"
                                    + " AND " + MTIME_COLUMN + ">?"
                                    + " ORDER BY " + MTIME_COLUMN;
        if (usePagingModel) {
            modifiedAfterQuery = getPagingStatement(modifiedAfterQuery);
        }
        log.debug("Preparing query getModifiedAfter with '"
                  + modifiedAfterQuery + "'");
        stmtGetModifiedAfter = prepareStatement(modifiedAfterQuery);
        log.debug("getModifiedAfter handle: " + stmtGetModifiedAfter);

        // We can order by mtime only because the generated mtimes are unique
        String modifiedAfterAllQuery = "SELECT " + allCells
                                       + " FROM " + RECORDS
                                       + " LEFT JOIN " + RELATIONS
                                       + " ON " + relationsClause
                                       + " WHERE " + MTIME_COLUMN + ">?"
                                       + " ORDER BY " + MTIME_COLUMN;
        if (usePagingModel) {
            modifiedAfterAllQuery = getPagingStatement(modifiedAfterAllQuery);
        }
        log.debug("Preparing query getModifiedAfterAll with '"
                  + modifiedAfterAllQuery + "'");
        stmtGetModifiedAfterAll = prepareStatement(modifiedAfterAllQuery);
        log.debug("getModifiedAfterAll handle: " + stmtGetModifiedAfterAll);

        String getRecordQuery = "SELECT " + allCells
                                + " FROM " + RECORDS
                                + " LEFT JOIN " + RELATIONS
                                + " ON " + relationsClause
                                + " WHERE " + RECORDS + "." + ID_COLUMN + "=?";
        log.debug("Preparing query getRecord with '" + getRecordQuery + "'");
        stmtGetRecord = prepareStatement(getRecordQuery);
        log.debug("getRecord handle: " + stmtGetRecord);

        String clearBaseQuery = "UPDATE " + RECORDS
                                + " SET " + DELETED_COLUMN + "=1"
                                + " WHERE " + BASE_COLUMN + "=?";
        log.debug("Preparing query clearBase with '"
                  + clearBaseQuery + "'");
        stmtClearBase = prepareStatement(clearBaseQuery);
        log.debug("clearBase handle: " + stmtClearBase);

        /*
         FIXME: We might want a prepared statement to fetch multiple records in
                one go. However it seems to be inefficient to use prepared
                statements with IN clauses in them . See fx:
                http://forum.springframework.org/archive/index.php/t-16001.html

        String getRecordsQuery = "SELECT " + allCells
                                + " FROM " + RECORDS
                                + " WHERE " + ID_COLUMN + " IN ?";
        log.debug("Preparing query recordsQuery with '" + getRecordsQuery + "'");
        stmtGetRecords = prepareStatement(getRecordsQuery);
         */

        String deleteRecordQuery = "UPDATE " + RECORDS
                                   + " SET " + MTIME_COLUMN + "=?, "
                                   + DELETED_COLUMN + "=?"
                                   + " WHERE " + ID_COLUMN + "=?";
        log.debug("Preparing query deleteRecord with '"
                  + deleteRecordQuery + "'");
        stmtDeleteRecord = prepareStatement(deleteRecordQuery);
        log.debug("deleteRecord handle: " + stmtDeleteRecord);

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
        stmtCreateRecord = prepareStatement(createRecordQuery);
        log.debug("createRecord handle: " + stmtCreateRecord);

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
        stmtUpdateRecord = prepareStatement(updateRecordQuery);
        log.debug("updateRecord handle: " + stmtUpdateRecord);

        /* touchRecord */
        String touchRecordQuery = "UPDATE " + RECORDS + " SET "
                                   + MTIME_COLUMN + "=? "
                                   + "WHERE " + ID_COLUMN +"=?";
        log.debug("Preparing query touchRecord with '" + touchRecordQuery
                  + "'");
        stmtTouchRecord = prepareStatement(touchRecordQuery);

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
        log.debug("touchParents handle: " + stmtTouchParents);

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
        log.debug("getChildren handle: " + stmtGetChildren);

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
        log.debug("getParents handle: " + stmtGetParents);

        /* createRelation */
        String createRelation = "INSERT INTO " + RELATIONS
                                + " (" + PARENT_ID_COLUMN + ","
                                       + CHILD_ID_COLUMN
                                       + ") VALUES (?,?)";
        log.debug("Preparing query createRelation with '" +
                                              createRelation + "'");
        stmtCreateRelation = prepareStatement(createRelation);
        log.debug("createRelation handle: " + stmtCreateRelation);

        /* deleteRelation */
        /*String deleteRelation = "DELETE FROM " + RELATIONS
                                + " WHERE " + PARENT_ID_COLUMN + "=? "
                                + " OR " + CHILD_ID_COLUMN + "=? ";
        log.debug("Preparing query deleteRelation with '" +
                                              deleteRelation + "'");
        stmtDeleteRelation = prepareStatement(
                                                     deleteRelation);*/

        log.trace("Finished preparing SQL statements");
    }

    @Override
    public synchronized long getRecordsModifiedAfter(long time,
                                                     String base,
                                                     QueryOptions options)
                                                        throws IOException {
        // Convert time to the internal binary format used by DatabaseStorage
        long mtimeTimestamp = timestampGenerator.baseTimestamp(time);

        Cursor iter = getRecordsModifiedAfterCursor(mtimeTimestamp,
                                                    base, options);

        if (iter == null) {
            return EMPTY_ITERATOR_KEY;
        }

        if (usePagingModel) {
            iter = new PagingCursor(this, (ResultSetCursor)iter);
        }

        return registerCursor(iter);
    }

    /**
     * Get a {@link ResultSetCursor} over all records with an {@code mtime}
     * timestamp strictly bigger than {@code mtimeTimestamp}. The provided
     * timestamp must be in the format as returned by a
     * {@link UniqueTimestampGenerator}, ie. it is <i>not</i> a normal system
     * time in milliseconds.
     * <p/>
     * The returned ResultSetCursor <i>must</i> be closed by the called to
     * avoid leaking connections and locking up the storage
     *
     * @param mtimeTimestamp a timestamp as returned by a
     *                       {@link UniqueTimestampGenerator}
     * @param base the base which the retrieved records must belong to
     * @param options any {@link QueryOptions} the query should match
     * @return a {@link ResultSetCursor} that <i>must</i> be closed by the
     *         caller to avoid leaking connections and locking up the storage
     * @throws IOException
     */
    public ResultSetCursor getRecordsModifiedAfterCursor (long mtimeTimestamp,
                                                          String base,
                                                          QueryOptions options)
                                                            throws IOException {
        PreparedStatement stmt;

        try {
            if (base == null) {
                stmt = getStatement(stmtGetModifiedAfterAll);
            } else {
                stmt = getStatement(stmtGetModifiedAfter);
            }
        } catch (SQLException e) {
            throw new IOException("Failed to get prepared statement "
                                  + stmtGetModifiedAfterAll);
        }

        // doGetRecordsModifiedAfter creates and iterator and 'stmt' will
        // be closed together with that iterator
        ResultSetCursor iter = doGetRecordsModifiedAfterCursor (mtimeTimestamp,
                                                                base,
                                                                options,
                                                                stmt);

        return iter;
    }

    /**
     * Helper method dispatched by {@link #getRecordsModifiedAfterCursor}
     * <p/>
     * This method is responsible for closing 'stmt'. This is handled implicitly
     * since 'stmt' is added to a ResultIterator and it will be closed when
     * the iterator is closed.
     * @return {@code null} if there are no records updated in {@code base}
     *         after {@code time}. Otherwise a ResultIterator ready for fetching
     *         records
     */
    private synchronized ResultSetCursor
                         doGetRecordsModifiedAfterCursor(long mtimeTimestamp,
                                                         String base,
                                                         QueryOptions options,
                                                         PreparedStatement stmt)
                                                            throws IOException {
        log.debug("getRecordsModifiedAfter('" + mtimeTimestamp + "', " + base
                  + ") entered");

        // Set the statement up for fetching of large result sets, see fx.
        // http://jdbc.postgresql.org/documentation/83/query.html#query-with-cursor
        // This prevents an OOM for backends like Postgres
        try {
            stmt.getConnection().setAutoCommit(false);
            stmt.getConnection().setReadOnly(true);
            stmt.setFetchDirection(ResultSet.FETCH_FORWARD);
            
            if (usePagingModel) {
                stmt.setFetchSize(pageSize);
            } else {
                stmt.setFetchSize(FETCH_SIZE);
            }
        } catch (SQLException e) {
            throw new IOException("Error preparing connection for cursoring: "
                                  + e.getMessage(), e);
        }

        if (timestampGenerator.systemTime(mtimeTimestamp) >
                                                    getModificationTime(base)) {
            log.debug ("Storage not flushed after " + mtimeTimestamp
                       + ". Returning empty iterator");
            try {
                stmt.close();
            } catch (SQLException e) {
                log.warn("Failed to close statement: " + e.getMessage(), e);
            }
            return null;
        }

        // Prepared stmt for all bases
        if (base == null) {
            try {
                stmt.setLong(1, mtimeTimestamp);

                if (usePagingModel) {
                    stmt.setInt(2, pageSize);
                }

            } catch (SQLException e) {
                throw new IOException(String.format(
                        "Could not prepare stmtGetModifiedAfterAll with time"
                        + " %d", mtimeTimestamp), e);
            }

            // stmt will be closed when the iterator is closed
            return startIterator(stmt, base, options);
        }

        // Prepared stmt for a specfic base
        try {
            stmt.setString(1, base);
            stmt.setLong(2, mtimeTimestamp);

            if (usePagingModel) {
                stmt.setInt(3, pageSize);
            }

        } catch (SQLException e) {
            throw new IOException("Could not prepare stmtGetModifiedAfter "
                                      + "with base '" + base + "' and time "
                                      + mtimeTimestamp, e);
        }

        return startIterator(stmt, base, options);
    }    

    @Override
    public Record getRecord(String id, QueryOptions options)
                                                            throws IOException {
        PreparedStatement stmt;

        try {
            stmt = getStatement(stmtGetRecord);
        } catch (SQLException e) {
            throw new IOException("Failed to get prepared statement "
                                  + stmtGetModifiedAfterAll);
        }

        try {
            return doGetRecord(id, options, stmt);
        } finally {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.warn("Failed to close statement: " + e.getMessage(), e);
            }
        }

    }

    private Record doGetRecord(String id, QueryOptions options,
                               PreparedStatement stmt)
                                                            throws IOException {
        log.trace("getRecord('" + id + "', " + options + ")");

        try {
            stmt.setString(1, id);
        } catch (SQLException e) {
            throw new IOException("Could not prepare stmtGetRecord "
                                      + "with id '" + id + "''", e);
        }

        Record record = null;
        try {
            ResultSet resultSet = stmt.executeQuery();

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
        if (iteratorKey == EMPTY_ITERATOR_KEY) {
            throw new NoSuchElementException("Empty iterator");
        }

        Cursor iterator = iterators.get(iteratorKey);

        if (iterator == null) {
            throw new IllegalArgumentException("No result iterator with key "
                                      + iteratorKey);
        }

        if (!iterator.hasNext()) {
            iterator.close();
            iterators.remove(iterator.getKey());
            throw new NoSuchElementException ("Iterator " + iteratorKey
                                              + " depleted");
        }

        Record r = iterator.next();
        return expandRelations(r, iterator.getQueryOptions());
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

    /**
     *  Touch (that is, set the 'mtime' to now) the parents
     * of <code>id</code> recursively upwards
     * @param id the id of the records which parents to touch
     * @param options any query options that may affect how the touching is
     *                handled
     */
    protected void touchParents(String id, QueryOptions options)
                                                            throws IOException {
        List<Record> parents = getParents(id, options);

        if (parents == null || parents.isEmpty()) {
            if (log.isTraceEnabled()) {
                log.trace("No parents to update for record " + id);
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug ("Touching " + parents.size() + " parents of " + id);
        }

        PreparedStatement stmt = null;
        try {
            stmt = getStatement(stmtTouchParents);

            long nowStamp = timestampGenerator.next();
            stmt.setLong (1, nowStamp);
            stmt.setString(2, id);
            stmt.executeUpdate();

            // It would be a tempting optimization to drop the getParents() call
            // at the top and simply return here if stmt.getUpdateCount() == 0.
            // This would avoid the creation of a ResultSet in getParents().
            // We can't do this because there might be a ref to a non-existing
            // parent which in turn might have a parent that actually exist.
            // If we returned on zero updates we wouldn't touch the topmost
            // parent

        } catch (SQLException e) {
            log.error ("Failed to touch parents of '" + id + "': "
                       + e.getMessage(), e);
            // Consider this non-fatal
            return;
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    log.warn("Failed to close statement: " + e.getMessage(), e);
                }
            }
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
        PreparedStatement stmt;

        try {
            stmt = getStatement(stmtGetParents);
        } catch (SQLException e) {
            throw new IOException("Failed to look up statement "
                                  + stmtGetParents + ": " + e.getMessage(), e);
        }

        // doGetParents() will close stmt
        return doGetParents(id, options, stmt);

    }

    /*
     * This method is responsible for closing stmt itself
     */
    private List<Record> doGetParents (String id, QueryOptions options,
                                       PreparedStatement stmt)
                                                            throws IOException {
        List<Record> parents = new ArrayList<Record>(1);
        Cursor iter = null;

        try {
            stmt.setString(1, id);
            stmt.executeQuery();

            ResultSet results = stmt.getResultSet();
            iter = new ResultSetCursor(this, stmt, results);

            while (iter.hasNext()) {
                Record r = iter.next();
                if (options != null && options.allowsRecord(r)) {
                    parents.add(r);
                } else if (options == null) {
                    parents.add(r);
                } else if (log.isTraceEnabled()) {
                    log.trace("Parent record '" + r.getId()
                              + "' not allowed by query options");
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
        } finally {
            if (iter != null) {
                iter.close();
            }
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
        PreparedStatement stmt;

        try {
            stmt = getStatement(stmtGetChildren);
        } catch (SQLException e) {
            throw new IOException("Failed to look up statement "
                                  + stmtGetChildren + ": " + e.getMessage(), e);
        }

        // doGetChildren() will close stmt
        return doGetChildren(id, options, stmt);
    }

    /*
     * This method is responsible for closing stmt itself
     */
    private List<Record> doGetChildren (String id, QueryOptions options,
                                        PreparedStatement stmt)
                                                            throws IOException {
        List<Record> children = new ArrayList<Record>(3);
        ResultSetCursor iter = null;

        try {
            stmt.setString(1, id);
            stmt.executeQuery();

            ResultSet results = stmt.getResultSet();
            iter = new ResultSetCursor(this, stmt, results);

            while (iter.hasNext()) {
                Record r = iter.next();
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
        } finally {
            if (iter != null) {
                iter.close();
            }
        }
    }

    @Override
    public void clearBase (String base) throws IOException {
        PreparedStatement stmt;

        try {
            stmt = getStatement(stmtClearBase);
        } catch (SQLException e) {
            throw new IOException("Failed to look up statement "
                                  + stmtClearBase + ": " + e.getMessage(), e);
        }

        try {
            doClearBase(base, stmt);
        } finally {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.warn("Failed to close statement: " + e.getMessage(), e);
            }
        }
    }

    private void doClearBase (String base, PreparedStatement stmt)
                                                            throws IOException {
        log.info ("Clearing base '" + base + "'");

        try {
            stmt.setString(1, base);
            stmt.executeUpdate();
            updateModificationTime(base);
        } catch (SQLException e) {
            throw new IOException("SQLException clearing base '"
                                      + base + "': " + e.getMessage(), e);
        }
    }

    private void createRelations (Record rec) throws SQLException {
        PreparedStatement stmt;

        stmt = getStatement(stmtCreateRelation);

        try {
            doCreateRelations(rec, stmt);
        } finally {
            stmt.close();
        }
    }

    /* Create parent/child and child/parent relations for the given record */
    private void doCreateRelations (Record rec, PreparedStatement stmt)
                                                           throws SQLException {
        // FIXME: Some transactional safety here would be nice
        if (rec.getChildIds() != null) {
            for (String childId : rec.getChildIds()) {
                if (log.isDebugEnabled()) {
                    log.debug ("Creating relation: " + rec.getId()
                               + " -> " + childId);
                }
                stmt.setString(1, rec.getId());
                stmt.setString(2, childId);

                try {
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    if (isIntegrityConstraintViolation(e)) {
                        if (log.isDebugEnabled()) {
                            log.debug ("Relation "+ rec.getId() + " -> "
                                       + childId + ", already known");
                        }
                    } else {
                        throw new SQLException("Error creating child relations"
                                               + " for " + rec.getId (), e);
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
                stmt.setString(1, parentId);
                stmt.setString(2, rec.getId());

                try {
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    if (isIntegrityConstraintViolation(e)) {
                        if (log.isDebugEnabled()) {
                            log.debug ("Relation "+ parentId + " -> "
                                       + rec.getId() + ", already known");
                        }
                    } else {
                        throw new SQLException("Error creating parent relations"
                                               + " for " + rec.getId(), e);
                    }
                }
            }
        }
    }

    private void createNewRecord(Record record) throws IOException {
        PreparedStatement stmt;

        try {
            stmt = getStatement(stmtCreateRecord);
        } catch (SQLException e) {
            throw new IOException("Failed to look up prepared statement "
                                  + stmtCreateRecord + ": " + e.getMessage(),
                                  e);
        }

        try {
            doCreateNewRecord(record, stmt);
        } finally {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.warn("Failed to close statement: " + e.getMessage(), e);
            }
        }
    }

    private void doCreateNewRecord(Record record, PreparedStatement stmt)
                                                            throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("Preparing to create or update record: "
                      + record.toString(true));
        }

        long nowStamp = timestampGenerator.next();

        try {
            stmt.setString(1, record.getId());
            stmt.setString(2, record.getBase());
            stmt.setInt(3, boolToInt(record.isDeleted()));
            stmt.setInt(4, boolToInt(record.isIndexable()));
            stmt.setLong(5, nowStamp);
            stmt.setLong(6, nowStamp);
            stmt.setBytes(7, Zips.gzipBuffer(record.getContent()));
            stmt.setBytes(8,record.hasMeta() ?
                                         record.getMeta().toFormalBytes() :
                                         new byte[0]);
            stmt.executeUpdate();
            if (log.isDebugEnabled()) {
                log.debug("Created new record: " + record);
            }
        } catch (SQLException e) {
            if (isIntegrityConstraintViolation(e)) {
                // The constraint violation is because we already have the
                // record, so update the record instead...

                if (log.isTraceEnabled()) {
                    log.trace ("Record '" + record.getId() + "' already stored. "
                               + "Updating instead");
                }
                updateRecord(record);
                if (log.isDebugEnabled()) {
                    log.debug("Updated record: " + record);
                }

                return;
            } else {
                throw new IOException("Error creating new record " + record
                                      + ": " + e.getMessage(), e);
            }

        }

        try {
            createRelations(record);
        } catch (SQLException e) {
            throw new IOException("Error creating relations for "
                                      + record + ": " + e.getMessage(),
                                      e);
        }
        
    }

    private void updateRecord(Record record) throws IOException {
        PreparedStatement stmt;

        try {
            stmt = getStatement(stmtUpdateRecord);
        } catch (SQLException e) {
            throw new IOException("Failed to look up prepared statement "
                                  + stmtUpdateRecord + ": " + e.getMessage(),
                                  e);
        }

        try {
            doUpdateRecord(record, stmt);
        } finally {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.warn("Failed to close statement: " + e.getMessage(), e);
            }
        }
    }

    /* Note that creationTime isn't touched */
    private void doUpdateRecord(Record record, PreparedStatement stmt)
                                                            throws IOException {
        // FIXME: Add child records recursively (parents?)
        long nowStamp = timestampGenerator.next();

        try {
            stmt.setString(1, record.getBase());
            stmt.setInt(2, boolToInt(record.isDeleted()));
            stmt.setInt(3, boolToInt(record.isIndexable()));
            stmt.setLong(4, nowStamp);
            stmt.setBytes(5, Zips.gzipBuffer(record.getContent()));
            stmt.setBytes(6, record.hasMeta() ?
                                         record.getMeta().toFormalBytes() :
                                         new byte[0]);
            stmt.setString(7, record.getId());
            stmt.executeUpdate();

            if (stmt.getUpdateCount() == 0) {
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

        try {
            createRelations(record);
        } catch (SQLException e) {
            throw new IOException("Error creating relations for '"
                                      + record.getId() + "': " + e.getMessage(),
                                      e);
        }
     }

    private int deleteRecord(String id) throws IOException {
        PreparedStatement stmt;

        try {
            stmt = getStatement(stmtDeleteRecord);
        } catch (SQLException e) {
            throw new IOException("Failed to look up prepared statement "
                                  + stmtDeleteRecord + ": " + e.getMessage(),
                                  e);
        }

        try {
            return doDeleteRecord(id, stmt);
        } finally {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.warn("Failed to close statement: " + e.getMessage(), e);
            }
        }
    }

    private int doDeleteRecord(String id, PreparedStatement stmt)
                                                            throws IOException {
        long nowStamp = timestampGenerator.next();

        try {
            stmt.setLong(1, nowStamp);
            stmt.setBoolean(2, true);
            stmt.setString(3, id);
            // TODO: Consider stmt.close();
            return stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("SQLException deleting record '" + id + "'", e);
            throw new IOException("SQLException deleting record '"
                                      + id + "'", e);
        }

        // FIXME: Should we updateRelations()?
    }

    protected void touchRecord (String id)
                                                            throws IOException {
        PreparedStatement stmt;

        try {
            stmt = getStatement(stmtTouchRecord);
        } catch (SQLException e) {
            throw new IOException("Failed to look up prepared statement "
                                  + stmtTouchRecord + ": " + e.getMessage(),
                                  e);
        }

        try {
            doTouchRecord(id, stmt);
        } finally {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.warn("Failed to close statement: " + e.getMessage(), e);
            }
        }
    }

    private void doTouchRecord(String id, PreparedStatement stmt)
                                                            throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("Touching " + id);
        }

        try {
            stmt.setLong(1, timestampGenerator.next());
            stmt.setString(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("SQLException touching record '"
                                  + id + "'", e);
        }

        if (log.isDebugEnabled()) {
            log.debug("Touched " + id);
        }
    }

    /**
     * Creates the tables {@link #RECORDS} and {@link #RELATIONS} and relevant
     * indexes on the database.
     *
     * @throws IOException if the database could not be created.
     */
    protected void createSchema() throws IOException {
        log.debug("Creating database schema");
        doCreateSchema();
    }

    private void doCreateSchema() {

        Connection conn = getConnection();

        try {
            /* RECORDS */
            try {
                doCreateSummaRecordsTable(conn);
            } catch (SQLException e) {
                if (log.isDebugEnabled()) {
                    log.info("Failed to create table for record data: "
                             + e.getMessage(), e);
                } else {
                    log.info("Failed to create table for record data: "
                             + e.getMessage());
                }
            }

            /* RELATIONS */
            try {
                doCreateSummaRelationsTable(conn);
            } catch (SQLException e) {
                if (log.isDebugEnabled()) {
                    log.info("Failed to create table for record relations: "
                             + e.getMessage(), e);
                } else {
                    log.info("Failed to create table for record relations: "
                             + e.getMessage());
                }
            }
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                log.warn("Failed to close connection to database: "
                         + e.getMessage(), e);
            }
        }
    }

    private void doCreateSummaRelationsTable(Connection conn)
                                                           throws SQLException {
        Statement stmt;

        String createRelationsQuery =
                "CREATE TABLE " + RELATIONS + " ("
                + PARENT_ID_COLUMN     + " VARCHAR(" + ID_LIMIT + "), "
                + CHILD_ID_COLUMN      + " VARCHAR(" + ID_LIMIT + ") )";
        log.debug("Creating table "+RELATIONS+" with query: '"
                  + createRelationsQuery + "'");
        stmt = conn.createStatement();
        stmt.execute(createRelationsQuery);
        stmt.close();

        /* RELATIONS INDEXES */
        String createRelationsPCIndexQuery =
                "CREATE UNIQUE INDEX pc ON "
                + RELATIONS + "("+PARENT_ID_COLUMN+","+CHILD_ID_COLUMN+")";
        log.debug("Creating index 'pc' on table "+RELATIONS+" with query: '"
                  + createRelationsPCIndexQuery + "'");
        stmt = conn.createStatement();
        stmt.execute(createRelationsPCIndexQuery);
        stmt.close();

        String createRelationsCIndexQuery =
                "CREATE INDEX c ON "
                + RELATIONS + "("+CHILD_ID_COLUMN+ ")";
        log.debug("Creating index 'c' on table "+RELATIONS+" with query: '"
                  + createRelationsCIndexQuery + "'");
        stmt = conn.createStatement();
        stmt.execute(createRelationsCIndexQuery);
        stmt.close();
    }

    private void doCreateSummaRecordsTable(Connection conn)
                                                           throws SQLException {
        String createRecordsQuery =
                "CREATE TABLE " + RECORDS + " ("
                + ID_COLUMN        + " VARCHAR(" + ID_LIMIT + ") PRIMARY KEY, "
                + BASE_COLUMN      + " VARCHAR(" + BASE_LIMIT + "), "
                + DELETED_COLUMN   + " INTEGER, "
                + INDEXABLE_COLUMN + " INTEGER, "
                + DATA_COLUMN      + " " + getDataColumnDataDeclaration() + ", "
                + CTIME_COLUMN     + " BIGINT, " // BIGINT is 64 bit
                + MTIME_COLUMN     + " BIGINT, "
                + META_COLUMN      + " " + getMetaColumnDataDeclaration()
                + ")";
        log.debug("Creating table "+RECORDS+" with query: '"
                  + createRecordsQuery + "'");

        Statement stmt = conn.createStatement();
        stmt.execute(createRecordsQuery);
        stmt.close();

        /* RECORDS INDEXES */
        String createRecordsIdIndexQuery =
                "CREATE UNIQUE INDEX i ON " + RECORDS + "("+ID_COLUMN+")";
        log.debug("Creating index 'i' on table "+RECORDS+" with query: '"
                  + createRecordsIdIndexQuery + "'");
        stmt = conn.createStatement();
        stmt.execute(createRecordsIdIndexQuery);
        stmt.close();

        // because we use a UniqueTimestampGenerator we can apply the UNIQUE
        // to the 'm' index. This is paramount to getting paginated result sets
        // for getRecordsModifiedAfter
        String createRecordsMTimeIndexQuery =
                "CREATE UNIQUE INDEX m ON " + RECORDS + "("+MTIME_COLUMN+")";
        log.debug("Creating index 'm' on table "+RECORDS+" with query: '"
                  + createRecordsMTimeIndexQuery + "'");
        stmt = conn.createStatement();
        stmt.execute(createRecordsMTimeIndexQuery);
        stmt.close();
    }

    /**
     * WARNING: <i>This will remove all data from the storage!</i>.
     * Destroys and removes all table definitions from the underlying database.
     * Caveat emptor.
     * @throws SQLException if there are problems executing the required SQL
     *                      statements
     */
    public void destroyDatabase() throws SQLException {
        log.warn("Preparing to destroy database. All data will be lost");

        Connection conn = getConnection();

        try {
            log.warn("Destroying all record data");
            Statement stmt = conn.createStatement();
            stmt.execute("DROP TABLE " + RECORDS);
            stmt.close();

            log.warn("Destroying all relations");
            stmt = conn.createStatement();
            stmt.execute("DROP TABLE " + RELATIONS);
            stmt.close();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                log.warn("Failed to close connection to database: "
                         + e.getMessage(), e);
            }
        }

        log.info("All Summa data wiped from database");
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
     *                      availability via {@link dk.statsbiblioteket.summa.storage.database.cursors.ResultSetCursor#setResultSetHasNext}
     * @return              a Record based on the result set.
     * @throws SQLException if there was a problem extracting values from the
     *                      SQL result set.
     * @throws IOException  If the data (content) could not be uncompressed
     *                      with gunzip.
     */
    public Record scanRecord(ResultSet resultSet, ResultSetCursor iter)
                                              throws SQLException, IOException {

        boolean hasNext;

        String id = resultSet.getString(1);
        String base = resultSet.getString(2);
        boolean deleted = intToBool(resultSet.getInt(3));
        boolean indexable = intToBool(resultSet.getInt(4));
        byte[] gzippedContent = resultSet.getBytes(5);
        long ctime = resultSet.getLong(6);
        long mtime = resultSet.getLong(7);
        byte[] meta = resultSet.getBytes(8);
        String parentIds = resultSet.getString(9);
        String childIds = resultSet.getString(10);

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
               id.equals(resultSet.getString(1))) {

            /* If we log on debug we do sanity checking of the result set.
            * Of course the parent and child columns should not be checked,
            * since they are the ones changing */
            if (log.isDebugEnabled()) {
                log.trace("Sanity checking record block for: " + id);
                if (!base.equals(resultSet.getString(2))) {
                    log.warn("Base mismatch for record: " + id);
                    return null;
                } else if (deleted != intToBool(resultSet.getInt(3))) {
                    log.warn("Deleted state mismatch for record: " + id);
                    return null;
                } else if (indexable != intToBool(resultSet.getInt(4))) {
                    log.warn("Indexable state mismatch for record: " + id);
                    return null;
                } else if (!Arrays.equals(gzippedContent,
                                          resultSet.getBytes(5))) {
                    log.warn("Content mismatch for record: " + id);
                    return null;
                }  else if (ctime != resultSet.getLong(6)) {
                    log.warn("CTime state mismatch for record: " + id);
                    return null;
                } else if (mtime != resultSet.getLong(7)) {
                    log.warn("MTime state mismatch for record: " + id);
                    return null;
                }  else if (!Arrays.equals(meta,resultSet.getBytes(8))) {
                    log.warn("Meta tags mismatch for record: " + id);
                    return null;
                }
            }

            /* Pick up parent and child ids */
            String newParent = resultSet.getString (9);
            String newChild = resultSet.getString (10);

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
            iter.setRecordMtimeTimestamp(mtime);
        }

        // We use salted unique timestamps generated by a
        // UniqueTimestampGenerator so we have to extract the system time from
        // the timestamp
        ctime = timestampGenerator.systemTime(ctime);
        mtime = timestampGenerator.systemTime(mtime);

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
     * As {@link #scanRecord(ResultSet, dk.statsbiblioteket.summa.storage.database.cursors.ResultSetCursor)} with
     * {@code resultSet = null}.
     */
    public Record scanRecord (ResultSet resultSet)
                                              throws SQLException, IOException {
        return scanRecord(resultSet, null);
    }

    /**
     * Given a query, execute this query and transform the {@link ResultSet}
     * to a {@link ResultSetCursor}.
     * @param stmt the statement to execute.
     * @param base the base we are iterating over
     * @return a RecordIterator of the result.
     * @throws IOException - also on no getConnection() and SQLExceptions.
     */
    private ResultSetCursor startIterator (PreparedStatement stmt,
                                          String base,
                                          QueryOptions options)
                                                            throws IOException {
        log.debug("Getting results for '" + stmt + "'");
        ResultSet resultSet;
         try {
             resultSet = stmt.executeQuery();
             log.debug("Got resultSet from '" + stmt.toString() + "'");
         } catch (SQLException e) {
             log.error("SQLException in startIterator", e);
             throw new IOException("SQLException", e);
         }

        ResultSetCursor iter;
        try {
            iter = new ResultSetCursor(this, stmt, resultSet, base, options);
        } catch (SQLException e) {
            log.error("SQLException creating record iter", e);
            throw new IOException("SQLException creating record iter",
                                      e);
        }

        return iter;
    }
    
    
    /**
     * Register a {@link Cursor} and return the iterator key for it.
     * Iterators registered with this call will automatically be reaped
     * after periods of inactivity.
     * @param iter the Cursor to register with the DatabaseStorage
     * @return a key to access the iterator remotely via the {@link #next}
     *         methods
     */
    private long registerCursor(Cursor iter) {
        log.trace("registerCursor: Got iter " + iter.getKey()
                  + " adding to iterator list");
        iterators.put(iter.getKey(), iter);

        return iter.getKey();
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
        Connection conn = null;

        try {
            conn = getConnection();
            return conn.getMetaData().getDatabaseProductName();
        } catch (SQLException e) {
            throw new IOException("Could not get catalog info", e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.warn("Failed to close connection database: "
                         + e.getMessage(), e);
            }
        }
    }

    @Override
    public void close() throws IOException {
        log.info("Closing");

        iteratorReaper.stop();

        log.info("Closed");
    }

}



