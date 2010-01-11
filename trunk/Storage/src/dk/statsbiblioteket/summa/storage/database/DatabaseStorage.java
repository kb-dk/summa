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

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.util.StringMap;
import dk.statsbiblioteket.summa.common.util.UniqueTimestampGenerator;
import dk.statsbiblioteket.summa.storage.BaseStats;
import dk.statsbiblioteket.summa.storage.BatchJob;
import dk.statsbiblioteket.summa.storage.StorageBase;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.database.MiniConnectionPoolManager.StatementHandle;
import dk.statsbiblioteket.summa.storage.database.cursors.Cursor;
import dk.statsbiblioteket.summa.storage.database.cursors.CursorReaper;
import dk.statsbiblioteket.summa.storage.database.cursors.PagingCursor;
import dk.statsbiblioteket.summa.storage.database.cursors.ResultSetCursor;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.Zips;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.sql.*;
import java.util.*;

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
getco     */
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
     * A list of strings with base names that explicitly should have relations
     * tracking disabled. That is, records from a base listed here will not have
     * their parent/child relations tracked.
     * <p/>
     * Excepting big bases which are known a-priori to not use relations from
     * relations tracking may be a big optimization on certain Storage
     * implementations.
     * <p/>
     * Even though relation ship tracking is disabled records may still have
     * relationships, the storage just wont make sure they are consistent.
     * <p/>
     * The default value for this property is the empty list, meaning that
     * all bases will have relationship tracking enabled.
     */
    public static final String CONF_DISABLE_REALTIONS_TRACKING =
                              "summa.storage.database.disablerelationstracking";

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
     * The {@code hasRelations} column contains a flag that indicates whether
     * the record is a part of any parent/child relationsship. This flag
     * is used as an optimization in the lazy relation lookups strategy
     */
    public static final String HAS_RELATIONS_COLUMN = "hasRelations";

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
    private StatementHandle stmtDeleteRecord;
    private StatementHandle stmtCreateRecord;
    private StatementHandle stmtUpdateRecord;
    private StatementHandle stmtTouchRecord;
    private StatementHandle stmtTouchParents;
    private StatementHandle stmtGetChildren;
    private StatementHandle stmtGetParents;
    private StatementHandle stmtGetRelatedIds;
    private StatementHandle stmtMarkHasRelations;
    private StatementHandle stmtCreateRelation;
    private String allColumns;

    private Map<Long, Cursor> iterators =
                                         new HashMap<Long, Cursor>(10);

    private CursorReaper iteratorReaper;
    private UniqueTimestampGenerator timestampGenerator;

    // List of base namesfor which we don't track relations
    private Set<String> disabledRelationsTracking;

    private boolean useLazyRelations;
    private boolean usePagingModel;
    private int pageSize;

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

        /**
         * Wrap {@code options} in a RecursionQueryOptions which will be
         * set to not extract parent records.
         *
         * @param options the query options to wrap
         * @return a new query options that will not extract parent records
         */
        public static RecursionQueryOptions asChildOnlyOptions(
                                                         QueryOptions options) {
            if (options.parentHeight() == 0
                && options instanceof RecursionQueryOptions) {
                return (RecursionQueryOptions)options;
            }

            RecursionQueryOptions o = new RecursionQueryOptions(options);
            o.parentHeight = 0;
            o.parentRecursionHeight = 0;
            return o;
        }

        /**
         * Wrap {@code options} in a RecursionQueryOptions which will be
         * set to not extract child records.
         *
         * @param options the query options to wrap
         * @return a new query options that will not extract child records
         */
        public static RecursionQueryOptions asParentsOnlyOptions(
                                                         QueryOptions options) {
            if (options.childDepth() == 0
                && options instanceof RecursionQueryOptions) {
                return (RecursionQueryOptions)options;
            }

            RecursionQueryOptions o = new RecursionQueryOptions(options);
            o.childDepth = 0;
            o.childRecursionDepth = 0;
            return o;
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

        disabledRelationsTracking = new TreeSet<String>();
        disabledRelationsTracking.addAll(
                                conf.getStrings(CONF_DISABLE_REALTIONS_TRACKING,
                                                new ArrayList<String>()));
        if (disabledRelationsTracking.size() == 0) {
            log.debug("Tracking relations on all bases");
        } else {
            log.info("Disabling relationships tracking on: "
                     + Strings.join(disabledRelationsTracking, ", "));
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
            throw new IOException("init(): Failed to prepare SQL statements: "
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
     * locking up the entire storage.
     *
     * @return a connection to a SQL-compatible database. The returned
     *         connection <i>must</i> be closed by the caller to avoid
     *         connection leaking
     */
    protected abstract Connection getConnection();

    /**                                  getrec
     * Get an auto committing, write enabled connection
     * @return a pooled connection
     * @throws SQLException if unable to set write access or auto commit
     */
    private Connection getDefaultConnection() throws SQLException {
        Connection conn = getConnection();

        conn.setReadOnly(false);
        conn.setAutoCommit(true);

        return conn;
    }

    /**
     * Get a new pooled connection via {@link #getConnection()} and optimize it
     * for transactional mode, meaning that auto commit is off and the
     * transaction isolation level is set to the fastet one making sense.
     * @return
     */
    private Connection getTransactionalConnection() {
        Connection conn = getConnection();
        try {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        } catch (SQLException e) {
            // This is non fatal so simply log a warning
            log.warn("Failed to optimize new connection for transaction mode: "
                     + e.getMessage(), e);
        }
        return conn;
    }

    /**
     * Set up a prepared statement and return an opaque handle to it.
     * <p/>
     * A copy of the {@link PreparedStatement} can be retrieved by calling
     * {@link #getManagedStatement}. PreparedStatements <i>must</i> be
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
    protected abstract PreparedStatement getManagedStatement(
                                 StatementHandle handle) throws SQLException;

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
     *   sql + " LIMIT " + getPageSize()
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
        throw new UnsupportedOperationException("Non-paging data model. You "
                                                + "must override "
                                                + "DatabaseStorage.getPagingStatement "
                                                + "when using paging result "
                                                + "sets");
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


        allColumns = RECORDS + "." + ID_COLUMN + ","
                     + RECORDS + "." + BASE_COLUMN + ","
                     + RECORDS + "." + DELETED_COLUMN + ","
                     + RECORDS + "." + INDEXABLE_COLUMN + ","
                     + RECORDS + "." + HAS_RELATIONS_COLUMN + ", "
                     + RECORDS + "." + DATA_COLUMN + ","
                     + RECORDS + "." + CTIME_COLUMN + ","
                     + RECORDS + "." + MTIME_COLUMN + ","
                     + RECORDS + "." + META_COLUMN + ","
                     + allCellsRelationsCols;

        String relationsClause = RECORDS + "." + ID_COLUMN + "="
                                + RELATIONS + "." + PARENT_ID_COLUMN
                                + " OR " + RECORDS + "." + ID_COLUMN + "="
                                + RELATIONS + "." + CHILD_ID_COLUMN;

        /* modifiedAfter */
        // We can order by mtime only because the generated mtimes are unique
        String modifiedAfterQuery;
        if (useLazyRelations) {
            modifiedAfterQuery = "SELECT " + allColumns
                                 + " FROM " + RECORDS
                                 + " WHERE " + BASE_COLUMN + "=?"
                                 + " AND " + MTIME_COLUMN + ">?"
                                 + " ORDER BY " + MTIME_COLUMN;
        } else {
            modifiedAfterQuery = "SELECT " + allColumns
                                 + " FROM " + RECORDS
                                 + " LEFT JOIN " + RELATIONS
                                 + " ON " + relationsClause
                                 + " WHERE " + BASE_COLUMN + "=?"
                                 + " AND " + MTIME_COLUMN + ">?"
                                 + " ORDER BY " + MTIME_COLUMN;
        }
        if (usePagingModel) {
            modifiedAfterQuery = getPagingStatement(modifiedAfterQuery);
        }
        log.debug("Preparing query getModifiedAfter with '"
                  + modifiedAfterQuery + "'");
        stmtGetModifiedAfter = prepareStatement(modifiedAfterQuery);
        log.debug("getModifiedAfter handle: " + stmtGetModifiedAfter);

        /* modifiedAfterAll */
        // We can order by mtime only because the generated mtimes are unique
        String modifiedAfterAllQuery;
        if (useLazyRelations){
            modifiedAfterAllQuery = "SELECT " + allColumns
                                    + " FROM " + RECORDS
                                    + " WHERE " + MTIME_COLUMN + ">?"
                                    + " ORDER BY " + MTIME_COLUMN;
        } else {
            modifiedAfterAllQuery = "SELECT " + allColumns
                                    + " FROM " + RECORDS
                                    + " LEFT JOIN " + RELATIONS
                                    + " ON " + relationsClause
                                    + " WHERE " + MTIME_COLUMN + ">?"
                                    + " ORDER BY " + MTIME_COLUMN;
        }
        if (usePagingModel) {
            modifiedAfterAllQuery = getPagingStatement(modifiedAfterAllQuery);
        }
        log.debug("Preparing query getModifiedAfterAll with '"
                  + modifiedAfterAllQuery + "'");
        stmtGetModifiedAfterAll = prepareStatement(modifiedAfterAllQuery);
        log.debug("getModifiedAfterAll handle: " + stmtGetModifiedAfterAll);

        /* getRecord */
        // getRecordsQuery uses JOINs no matter if useLazyRelations is set.
        // Fetching single records using a LEFT JOIN is generally not a problem 
        String getRecordQuery = "SELECT " + allColumns
                                + " FROM " + RECORDS
                                + " LEFT JOIN " + RELATIONS
                                + " ON " + relationsClause
                                + " WHERE " + RECORDS + "." + ID_COLUMN + "=?";
        log.debug("Preparing query getRecord with '" + getRecordQuery + "'");
        stmtGetRecord = prepareStatement(getRecordQuery);
        log.debug("getRecord handle: " + stmtGetRecord);

        /*
         FIXME: We might want a prepared statement to fetch multiple records in
                one go. However it seems to be inefficient to use prepared
                statements with IN clauses in them . See fx:
                http://forum.springframework.org/archive/index.php/t-16001.html

        String getRecordsQuery = "SELECT " + allColumns
                                + " FROM " + RECORDS
                                + " WHERE " + ID_COLUMN + " IN ?";
        log.debug("Preparing query recordsQuery with '" + getRecordsQuery + "'");
        stmtGetRecords = prepareStatement(getRecordsQuery);
         */

        /* deleteRecord */
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
                                       + HAS_RELATIONS_COLUMN + ", "
                                       + CTIME_COLUMN + ", "
                                       + MTIME_COLUMN + ", "
                                       + DATA_COLUMN + ", "
                                       + META_COLUMN
                                       + ") VALUES (?,?,?,?,?,?,?,?,?)";
        log.debug("Preparing query createRecord with '" + createRecordQuery
                  + "'");
        stmtCreateRecord = prepareStatement(createRecordQuery);
        log.debug("createRecord handle: " + stmtCreateRecord);

        /* updateRecord */
        String updateRecordQuery = "UPDATE " + RECORDS + " SET "
                                   + BASE_COLUMN + "=?, "
                                   + DELETED_COLUMN + "=?, "
                                   + INDEXABLE_COLUMN + "=?, "
                                   + HAS_RELATIONS_COLUMN + "=?, "
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
        String getChildrenQuery = "SELECT " + allColumns
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
        String getParentsQuery = "SELECT " + allColumns
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

        /* getRelatedIds */
        // The obvious thing to do here was to use an OR instead of the UNION,
        // however some query optimizers have porblems using the right indexes
        // when ORing (H2 for instance). Using a UNION is easier for the optimizer
        String getRelatedIdsQuery = "SELECT " + PARENT_ID_COLUMN
                                    + ", " + CHILD_ID_COLUMN
                                    + " FROM " + RELATIONS
                                    + " WHERE " + PARENT_ID_COLUMN + "=?"
                                    + " UNION "
                                    + "SELECT " + PARENT_ID_COLUMN
                                    + ", " + CHILD_ID_COLUMN
                                    + " FROM " + RELATIONS
                                    + " WHERE " + CHILD_ID_COLUMN + "=?";

        log.debug("Preparing getRelatedIds with '" + getRelatedIdsQuery +"'");
        stmtGetRelatedIds = prepareStatement(getRelatedIdsQuery);
        log.debug("getRelatedIds handle: " + stmtGetRelatedIds);

        /* markHasRelations */
        String markHasRelationsQuery = "UPDATE " + RECORDS
                                    + " SET " + HAS_RELATIONS_COLUMN + "=1 "
                                    + " WHERE " + ID_COLUMN + "=?";
        log.debug("Preparing markHasRelations with '"
                  + markHasRelationsQuery + "'");
        stmtMarkHasRelations = prepareStatement(markHasRelationsQuery);
        log.debug("markHasRelations handle: " + stmtMarkHasRelations);

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

        log.debug("Finished preparing SQL statements");
    }

    /**
     * Close a statement and log any errors in the process. If this method
     * is passed {@code null} it will return silently.
     *
     * @param stmt the statement to close
     */
    private void closeStatement (Statement stmt) {
        if (stmt == null) {
            return;
        }

        try {
            stmt.close();
        } catch (SQLException e) {
            log.warn("Failed to close statement " + stmt + ": "
                     + e.getMessage(), e);
        }
    }

    /**
     * Close a connection and log any errors in the process. If this method
     * is passed {@code null} it will return silently.
     *
     * @param conn the connection to close
     */
    private void closeConnection (Connection conn) {
        if (conn == null) {
            return;
        }

        try {
            conn.close();
        } catch (SQLException e) {
            log.warn("Failed to close connection " + conn + ": "
                     + e.getMessage(), e);
        }
    }

    @Override
    public synchronized long getRecordsModifiedAfter(
            long time, String base, QueryOptions options) throws IOException {
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
     * {@link dk.statsbiblioteket.summa.common.util.UniqueTimestampGenerator}, ie. it is <i>not</i> a normal system
     * time in milliseconds.
     * <p/>
     * The returned ResultSetCursor <i>must</i> be closed by the caller to
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
    public ResultSetCursor getRecordsModifiedAfterCursor (
            long mtimeTimestamp, String base, QueryOptions options)
                                                            throws IOException {
        PreparedStatement stmt;

        try {
            if (base == null) {
                stmt = getManagedStatement(stmtGetModifiedAfterAll);
            } else {
                stmt = getManagedStatement(stmtGetModifiedAfter);
            }
        } catch (SQLException e) {
            throw new IOException("Failed to get prepared statement "
                                  + stmtGetModifiedAfterAll +": "
                                  + e.getMessage(), e);
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
    private synchronized ResultSetCursor doGetRecordsModifiedAfterCursor(
            long mtimeTimestamp, String base, QueryOptions options,
            PreparedStatement stmt) throws IOException {
        log.debug("getRecordsModifiedAfter('" + mtimeTimestamp + "', " + base
                  + ") entered");

        // Set the statement up for fetching of large result sets, see fx.
        // http://jdbc.postgresql.org/documentation/83/query.html#query-with-cursor
        // This prevents an OOM for backends like Postgres
        try {
            //stmt.getConnection().setAutoCommit(false);
            stmt.getConnection().setTransactionIsolation(
                                       Connection.TRANSACTION_READ_UNCOMMITTED);
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
        } catch (SQLException e) {
            throw new IOException("Could not prepare stmtGetModifiedAfter "
                                      + "with base '" + base + "' and time "
                                      + mtimeTimestamp, e);
        }

        return startIterator(stmt, base, options);
    }    

    @Override
    public List<Record> getRecords (List<String> ids, QueryOptions options)
                                                        throws IOException {
        long startTime = System.currentTimeMillis();
        Connection conn = getTransactionalConnection();
        try {
            conn.setReadOnly(true);
        } catch (SQLException e) {
            // This is not fatal for the operation so try and proceed
            // past the exception
            log.warn("Failed to optimize connection for batch retrieval: "
                     + e.getMessage(), e);
        }

        try {
            List<Record> result = getRecordsWithConnection(ids, options, conn);
            if (log.isDebugEnabled()) {
                log.debug("Finished getRecords(" + ids.size()
                          + " ids, ...) in "
                          + (System.currentTimeMillis() - startTime) + "ms");
            }
            return result;
        } finally {
            closeConnection(conn);
        }
    }

    public List<Record> getRecordsWithConnection(
            List<String> ids, QueryOptions options, Connection conn)
                                                            throws IOException {
        ArrayList<Record> result = new ArrayList<Record>(ids.size());

        for (String id : ids) {
            try {
                Record r = getRecordWithConnection(id, options, conn);
                if (r != null) {
                    result.add(r);
                }
            } catch (SQLException e) {
                log.error("Failed to get record '" + id
                          + "' for batch request: " + e.getMessage(), e);
                result.add(null);
            }

        }

        return result;
    }

    @Override
    public Record getRecord(String id, QueryOptions options)
                                                            throws IOException {
        long startTime = System.currentTimeMillis();
        Connection conn = getTransactionalConnection();

        try {
            conn.setReadOnly(true);
        } catch (SQLException e) {
            throw new IOException("Failed to get prepared connection for "
                                  + stmtGetRecord + ": " + e.getMessage(), e);
        }

        try {
            Record record =  getRecordWithConnection(id, options, conn);
            log.debug("Finished getRecord(" + id + ", ...) in "
                      + (System.currentTimeMillis()-startTime) + "ms");
            return record;
        } catch (SQLException e){
            log.error(String.format("Failed to get record '%s': %s",
                                    id, e.getMessage()), e);
            return null;
        } finally {
            closeConnection(conn);
        }

    }

    private Record getRecordWithConnection(String id, QueryOptions options,
                                           Connection conn)
                                              throws IOException, SQLException {
        log.trace("getRecord('" + id + "', " + options + ")");

        if (isPrivateId(id)) {
            if (!allowsPrivate(options)) {
                log.debug(String.format(
                        "Request for private record '%s' denied", id));
                throw new IllegalArgumentException(
                        "Private record requested, but ALLOW_PRIVATE flag "
                        + "not set in query options");
            }
            return getPrivateRecord(id);
        }

        PreparedStatement stmt = conn.prepareStatement(stmtGetRecord.getSql());

        Record record = null;
        try {
            stmt.setString(1, id);
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

                expandRelationsWithConnection(record, options, conn);

            } finally {
                resultSet.close();
            }

        } catch (SQLException e) {
            throw new IOException("Error getting record " + id +": "
                                  + e.getMessage(), e);
        } finally {
            closeStatement(stmt);
        }

        return record;
    }

    /**
     * Return a private record, such as __holdings__ or __statistics__
     * @param id the id of the private record to retrieve
     * @return the matching record or {@code null} in case of an unknown id
     */
    private Record getPrivateRecord(String id) throws IOException {
        log.debug(String.format("Fetching private record '%s'", id));

        if ("__holdings__".equals(id)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(bytes);
            BaseStats.toXML(getStats(), writer);
            return new Record(
                    "__holdings__", "__private__", bytes.toByteArray());
        } else {
            log.debug(String.format("No such private record '%s'", id));
            return null;
        }
    }

    /* Expand child records if we need to and there indeed
     * are any children to expand */
    private void expandChildRecords(
            Record record, RecursionQueryOptions options, Connection conn)
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

            // Make sure we don't go into an infinite parent/child
            // expansion ping-pong
            options = RecursionQueryOptions.asChildOnlyOptions(options);

            List<Record> children = getRecordsWithConnection(
                              childIds, options.decChildRecursionDepth(), conn);

            if (children.isEmpty()) {
                record.setChildren(null);
            } else {
                record.setChildren(children);
            }
        }
    }

    /* Expand parent records if we need to and there indeed
     * are any parents to expand */
    private void expandParentRecords(
            Record record, RecursionQueryOptions options, Connection conn)
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

            // Make sure we don't go into an infinite parent/child
            // expansion ping-pong
            options = RecursionQueryOptions.asParentsOnlyOptions(options);

            List<Record> parents = getRecordsWithConnection(
                           parentIds, options.decParentRecursionHeight(), conn);

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
            throw new NoSuchElementException("Empty cursor");
        }

        Cursor cursor = iterators.get(iteratorKey);

        if (cursor == null) {
            throw new IllegalArgumentException("No result cursor with key "
                                      + iteratorKey);
        }

        if (!cursor.hasNext()) {
            cursor.close();
            iterators.remove(cursor.getKey());
            throw new NoSuchElementException ("Iterator " + iteratorKey
                                              + " depleted");
        }

        Record r = cursor.next();

        try {
            return expandRelations(r, cursor.getQueryOptions());
        } catch (SQLException e) {
            log.warn("Failed to expand relations for '" + r.getId() +"': "
                     + e.getMessage(), e);
            return r;
        }                               
    }

    private Record expandRelations(Record r, QueryOptions options)
                                              throws IOException, SQLException {

        Connection conn = getTransactionalConnection();
        try {
            conn.setReadOnly(true);
        } catch (SQLException e) {
            // This is not fatal to the operation so try an proceed
            // past the exception
            log.warn("Failed to optimize connection for read only access: "
                     + e.getMessage(), e);
        }

        try {
            return expandRelationsWithConnection(r, options, conn);
        } finally {
            closeConnection(conn);
        }

    }

    private Record expandRelationsWithConnection(
            Record r, QueryOptions options, Connection conn) throws IOException{
        if (options == null) {
            return r;
        }

        // This also makes sure that the recursion levels are reset
        RecursionQueryOptions opts;

        if (options.childDepth() != 0) {
            opts = RecursionQueryOptions.wrap(options);
            expandChildRecords(r, opts, conn);
        }

        if (options.parentHeight() != 0) {
            opts = RecursionQueryOptions.wrap(options);
            expandParentRecords(r, opts, conn);
        }

        return r;
    }

    /*
     * Note: the 'synchronized' part of this method decl is paramount to
     * allowing us to set our transaction level to
     * Connection.TRANSACTION_READ_UNCOMMITTED
     */
    @Override
    public synchronized void flush(Record record, QueryOptions options)
                                                            throws IOException {
        long startTime = System.currentTimeMillis();
        Connection conn = getTransactionalConnection();

        // Brace yourself for the try-catch-finally hell, but we really don't
        // want to leak them pooled connections!
        String error = null;
        try {
            conn.setReadOnly(false);
            flushWithConnection(record, options, conn);
        } catch (SQLException e) {
            // This error is logged in the finally clause below
            error = e.getMessage() + '\n' + Strings.getStackTrace(e);

            // We can not throw the SQLException over RPC as the receiver
            // probably does not have the relevant exception class
            throw new IOException(String.format(
                    "flush(...): Failed to flush %s: %s",
                    record, e.getMessage()));
        } finally {
            try {
                if(error == null) {
                    // All is OK, write to the DB
                    conn.commit();
                    log.debug("Committed " + record.getId() + " in " 
                              + (System.currentTimeMillis()-startTime) + "ms");
                } else {
                    log.warn(String.format(
                            "Not committing %s because of error: %s",
                            record.getId(), error));
                }
            } catch (SQLException e) {
                error = "flush: Failed to commit " + record.getId() + ": "
                        + e.getMessage();
                log.warn(error, e);
                throw new IOException(error, e);
            } finally {
                try {
                    conn.close();
                } catch(SQLException e) {
                    log.warn("Error closing connection after committing "
                             + record.getId() + ": " + e.getMessage(), e);
                }
            }
        }
    }

    /*
     * Note: the 'synchronized' part of this method decl is paramount to
     * allowing us to set our transaction level to
     * Connection.TRANSACTION_READ_UNCOMMITTED
     */
    @Override
    public synchronized void flushAll(List<Record> recs, QueryOptions options)
                                                            throws IOException {
        Connection conn = getTransactionalConnection();
        try {
            conn.setReadOnly(false);
        } catch (SQLException e) {
            log.error("Failed to set connection in write mode: "
                      + e.getMessage(), e);
            closeConnection(conn);
            // We can not throw the SQLException over RPC as the receiver
            // probably does not have the relevant exception class
            throw new IOException("Can not prepare database for write mode: "
                                  + e.getMessage());
        }

        // Brace yourself for the try-catch-finally hell, but we really don't
        // want to leak them pooled connections!
        String error = null;
        Record lastRecord = null;
        long start = System.nanoTime();
        boolean isDebug = log.isDebugEnabled();
        try {
            for (Record r : recs) {
                if (isDebug) {
                    log.debug("Flushing: " + r.getId());
                }
                lastRecord = r;
                flushWithConnection(r, options, conn);
            }
            // TODO: Introduce time-based logging on info
            log.debug("Flushed " + recs.size() + " in "
                      + ((System.nanoTime() - start)/1000000D) + "ms");
        } catch (SQLException e) {
            error = e.getMessage();
            throw new IOException(String.format(
                    "flushAll(%d records): Failed to flush %s: %s",
                    recs.size(), lastRecord, e.getMessage()), e);
        } finally {
            try {
                if(error == null) {
                    // All is OK, write to the DB
                    log.debug("Commiting transaction of "
                              + recs.size() + " records");
                    start = System.nanoTime();
                    conn.commit();
                    log.debug("Transaction of " + recs.size()
                              + " records completed in " +
                              + ((System.nanoTime() - start)/1000000D) + "ms");
                    if (isDebug) {
                        for (Record r : recs) {
                            // It may seem dull to iterate over all records *again*,
                            // but time has taught us that this info is really nice
                            // to have in the log...
                            log.debug("Committed: " + r.getId());
                        }
                    }
                } else {
                    log.warn(String.format(
                            "Not committing the last %d records because of "
                            + "error '%s'. The records was %s",
                            recs.size(), error, Logs.expand(recs, 10)));
                }
            } catch (SQLException e) {
                error = "Failed to commit the last " + recs.size()
                        + " records: " + e.getMessage();
                log.warn(error, e);
                throw new IOException(error, e);
            } finally {
                try {
                    if (error != null) {
                        conn.rollback();
                        log.info("Transaction rolled back succesfully");
                    }
                } catch (SQLException e) {
                    log.error("Transaction rollback failed: " + e.getMessage(),
                              e);
                }
                closeConnection(conn);
            }
        }
    }

    protected void flushWithConnection(
                          Record r, QueryOptions options, Connection conn)
                                              throws IOException, SQLException {
        if (log.isTraceEnabled()) {
            log.trace("Flushing: " + r.toString(true));
        } else if (log.isDebugEnabled()) {
            log.debug("Flushing: " + r.toString(false));
        }

        /* Update the timestamp we check agaist in getRecordsModifiedAfter */
        updateModificationTime (r.getBase());

        try{
            createNewRecordWithConnection(r, options, conn);
        } catch (SQLException e) {
            if (isIntegrityConstraintViolation(e)) {
                // We already had the record stored, so fire an update instead
                // Note that this will also handle deleted records
                updateRecordWithConnection(r, options, conn);
            } else {
                throw new IOException(String.format(
                        "flushWithConnection: Internal error in "
                        + "DatabaseStorage, failed to flush %s: %s",
                        r.getId(), e.getMessage()), e);
            }
        }

        /* Recursively add child records */
        List<Record> children = r.getChildren();
        if (children != null) {
            log.debug ("Flushing " + children.size()
                       + " nested child records of '" + r.getId() + "'");
            for (Record child : children) {
                flushWithConnection(child, options, conn);
            }
        }

        /* Touch parents recursively upwards
         * FIXME: The call to touchParents() might be pretty expensive... */
        try {
            touchParents(r.getId(), null, conn);
        } catch (IOException e) {
            // Consider this non-fatal
            log.error("Failed to touch parents of '" + r.getId() + "': "
                      + e.getMessage(), e);
        }

        /* Again - update the timestamp we check agaist in
         * getRecordsModifiedAfter. This is also done in the end of the flush()
         * because the operation is non-instantaneous  */
        updateModificationTime (r.getBase());
    }

    /**
     * Touch (that is, set the 'mtime' to now) the parents
     * of <code>id</code> recursively upwards
     * @param id the id of the records which parents to touch
     * @param options any query options that may affect how the touching is
     *                handled
     */
    protected void touchParents (String id, QueryOptions options,
                                 Connection conn)
                                              throws IOException, SQLException {
        List<Record> parents = getParents(id, options, conn);

        if (parents == null || parents.isEmpty()) {
            if (log.isTraceEnabled()) {
                log.trace("No parents to update for record " + id);
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug ("Touching " + parents.size() + " parents of " + id);
        }

        PreparedStatement stmt =
                               conn.prepareStatement(stmtTouchParents.getSql());
        try {
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
            closeStatement(stmt);
        }

        // Recurse upwards
        for (Record parent : parents) {
            touchParents(parent.getId(), options, conn);
        }
    }

    protected List<Record> getParents(
            String id, QueryOptions options, Connection conn)
                                              throws IOException, SQLException {
        PreparedStatement stmt = conn.prepareStatement(stmtGetParents.getSql());

        List<Record> parents = new ArrayList<Record>(1);
        Cursor iter = null;

        try {
            stmt.setString(1, id);
            stmt.executeQuery();

            ResultSet results = stmt.getResultSet();
            iter = new ResultSetCursor(this, stmt, results, true); // Anon cursor

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
            } else {
                closeStatement(stmt);
            }
        }
    }

    private List<Record> getChildren (String id,
                                      QueryOptions options,
                                      Connection conn)
                                              throws IOException, SQLException {
        PreparedStatement stmt =
                                conn.prepareStatement(stmtGetChildren.getSql());

        List<Record> children = new ArrayList<Record>(3);
        ResultSetCursor iter = null;

        try {
            stmt.setString(1, id);
            stmt.executeQuery();

            ResultSet results = stmt.getResultSet();
            iter = new ResultSetCursor(this, stmt, results, true);

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
            throw new IOException(String.format(
                    "Failed to get children for record '%s': %s",
                    id, e.getMessage()), e);
        } finally {
            if (iter != null) {
                iter.close();
            } else {
                stmt.close();
            }
        }
    }

    /**
     * Find all records related to {@code rec} and add them to {@code rec}
     * as parents and children accordingly.
     *
     * @param rec the record to have its parent and child ids expanded
     *            to real nested records
     * @param conn the sql connection to use for the lookups
     * @throws SQLException if stuff is bad
     */
    protected void resolveRelatedIds (Record rec, Connection conn)
                                                           throws SQLException {
        if (log.isTraceEnabled()) {
            log.trace("Preparing to resolve relations for " + rec.getId());
        }

        PreparedStatement stmt =
                              conn.prepareStatement(stmtGetRelatedIds.getSql());
        List<String> parentIds = new LinkedList<String>();
        List<String> childIds = new LinkedList<String>();

        if (log.isTraceEnabled()) {
            log.trace("Querying relations for " + rec.getId());
        }

        try {
            stmt.setString(1, rec.getId());
            stmt.setString(2, rec.getId());
            stmt.executeQuery();
            ResultSet results = stmt.getResultSet();

            // Collect all relations from the result set. Note that each row
            // will mention rec.getId() in one of the columns and we don't want
            // to list rec as a parent or child of itself
            while (results.next()) {
                String parentId = results.getString(1);
                String childId = results.getString(2);

                if (parentId != null && !"".equals(parentId) &&
                    !rec.getId().equals(parentId)) {
                    parentIds.add(parentId);
                }

                if (childId != null && !"".equals(childId) &&
                    !rec.getId().equals(childId)) {
                    childIds.add(childId);
                }
            }
        } catch (SQLException e) {
            log.warn("Failed to resolve related ids for " + rec.getId() + ": "
                     + e.getMessage(), e);
        } finally {
            closeStatement(stmt);
        }

        if (log.isTraceEnabled()) {
            if (parentIds.isEmpty() && childIds.isEmpty()) {
                log.trace("No relations for " + rec.getId());
            } else {
                log.trace("Found relations for " + rec.getId()
                          + ": Parents[" + Strings.join(parentIds, ",") + "] "
                          + " Children[" + Strings.join(childIds, ",") + "]");
            }
        }

        // Update the record
        if (parentIds.isEmpty()) {
            rec.setParentIds(null);
        } else {
            rec.setParentIds(parentIds);
        }

        if (childIds.isEmpty()) {
            rec.setChildIds(null);
        } else {
            rec.setChildIds(childIds);
        }
    }

    // The synchronized modifier on clearBase() is paramount to have the
    // TRANSACTION_READ_UNCOMMITTED transaction isolation level work properly,
    // in a nutshell this is the only isolation level that gives us the
    // throughput we want
    @Override
    public synchronized void clearBase (String base) throws IOException {
        log.debug(String.format("clearBase(%s) called", base));
        Connection conn = null;

        if (base == null) {
            throw new NullPointerException("Can not clear base 'null'");
        }

        try {
            conn = getDefaultConnection();
            clearBaseWithConnection(base, conn);
        } catch (SQLException e) {
            String msg = "Error clearing base '" + base + "': "
                         + e.getMessage();
            log.error(msg, e);
            throw new IOException(msg, e);
        } finally {
            closeConnection(conn);
        }
    }

    private void clearBaseWithConnection (String base, Connection conn)
                                              throws IOException, SQLException {
        long start = System.currentTimeMillis();
        log.info ("Clearing base '" + base + "'");

        int _ID = 1, _MTIME = 2, _DELETED = 3;
        String sql = "SELECT id, mtime, deleted "
                   + " FROM " + RECORDS
                   + " WHERE " + BASE_COLUMN + "=?"
                   + " AND " + MTIME_COLUMN + ">?"
                   + " AND " + MTIME_COLUMN + "<?";

        if (usePagingModel) {
            sql = getPagingStatement(sql);
        }

        PreparedStatement stmt = conn.prepareStatement(
                                                 sql,
                                                 ResultSet.TYPE_FORWARD_ONLY,
                                                 ResultSet.CONCUR_UPDATABLE);

        // Set the statement up for fetching of large result sets, see fx.
        // http://jdbc.postgresql.org/documentation/83/query.html#query-with-cursor
        // This prevents an OOM for backends like Postgres
        try {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(
                                       Connection.TRANSACTION_READ_UNCOMMITTED);
            conn.setReadOnly(false);
            stmt.setFetchDirection(ResultSet.FETCH_FORWARD);

            if (usePagingModel) {
                stmt.setFetchSize(pageSize);
            } else {
                stmt.setFetchSize(FETCH_SIZE);
            }
        } catch (SQLException e) {
            closeStatement(stmt);
            throw new IOException("Error preparing connection for "
                                  + "clearing base '" + base + "': "
                                  + e.getMessage(), e);
        }

        // Convert time to the internal binary format used by DatabaseStorage
        long lastMtimeTimestamp = timestampGenerator.baseTimestamp(0);
        long startTimestamp = timestampGenerator.next();
        String id = null;
        long totalCount = 0;
        long pageCount = pageSize;
        try {
            // Page through all records in base and mark them as deleted
            // in one transaction
            while (pageCount >= pageSize) {
                log.debug(String.format(
                        "Preparing page for deletion on base '%s' for records "
                        + "in the range %s to %s",
                        base,
                        timestampGenerator.formatTimestamp(lastMtimeTimestamp),
                        timestampGenerator.formatTimestamp(startTimestamp)));
                pageCount = 0;
                stmt.setString(1, base);
                stmt.setLong(2, lastMtimeTimestamp);
                stmt.setLong(3, startTimestamp);
                stmt.execute();
                ResultSet cursor = stmt.getResultSet();
                while (cursor.next()) {
                    // We read the data before we start updating the row, not all
                    // JDBC backends like if we update the row before we read it
                    id = cursor.getString(_ID);
                    lastMtimeTimestamp = cursor.getLong(_MTIME);

                    cursor.updateInt(_DELETED, 1);
                    cursor.updateLong(_MTIME, timestampGenerator.next());
                    log.debug("Deleted " + id);
                    cursor.updateRow();
                    totalCount++;
                    pageCount++;
                }
                cursor.close();
            }

            // Commit the full transaction
            // FIXME: It would probably save memory to do incremental commits
            stmt.getConnection().commit();

            updateModificationTime(base);
            log.info("Cleared base '" + base + "' in "
                    + (System.currentTimeMillis() - start)
                    + "ms. Marked " + totalCount + " records as deleted");
        } catch (SQLException e) {
            String msg ="Error clearing base '" + base + "' after " + totalCount
                    + " records, last record id was '" + id + "': "
                    + e.getMessage();
            log.error(msg, e);
            stmt.getConnection().rollback();
            throw new IOException(msg, e);
        } finally {
            closeStatement(stmt);
        }
    }

    public synchronized String batchJob (String jobName,
            String base, long minMtime, long maxMtime, QueryOptions options)
                                                            throws IOException {
        log.info(String.format(
                "\n  Batch job: %s\n  Base: %s\n  Min mtime: %s\n  "
                + "Max mtime: %s\n  Query options: %s",
                jobName, base, minMtime, maxMtime, options));
        Connection conn = null;

        long start = System.currentTimeMillis();
        try {
            conn = getDefaultConnection();
            String result = batchJobWithConnection(
                    jobName, base, minMtime, maxMtime, options, conn);
            log.info("Batch job completed in " +
                     (System.currentTimeMillis() - start)/1000 + "s");
            return result;
        } catch (SQLException e) {
            String msg = "Error running batch job: " + e.getMessage();
            log.error(msg, e);
            throw new IOException(msg, e);
        } finally {
            closeConnection(conn);
        }
    }

    private String batchJobWithConnection (String jobName,
                                      String base, long minMtime, long maxMtime,
                                      QueryOptions options, Connection conn)
                                              throws IOException, SQLException {
        // Make sure options is always != null to ease logic later
        options = options != null ? options : new QueryOptions();

        String sql = "SELECT " + allColumns
                + " FROM " + RECORDS
                + " WHERE ( mtime<? AND mtime>? )";

        if (base != null) {
            sql += " AND base=?";
        }

        sql += " ORDER BY " + MTIME_COLUMN;

        if (usePagingModel) {
            sql = getPagingStatement(sql);
        }

        PreparedStatement stmt = conn.prepareStatement(
                                                 sql,
                                                 ResultSet.TYPE_FORWARD_ONLY,
                                                 ResultSet.CONCUR_READ_ONLY);

        // Set the statement up for fetching of large result sets, see fx.
        // http://jdbc.postgresql.org/documentation/83/query.html#query-with-cursor
        // This prevents an OOM for backends like Postgres
        try {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(
                                       Connection.TRANSACTION_READ_UNCOMMITTED);
            conn.setReadOnly(false);
            stmt.setFetchDirection(ResultSet.FETCH_FORWARD);

            if (usePagingModel) {
                stmt.setFetchSize(pageSize);
            } else {
                stmt.setFetchSize(FETCH_SIZE);
            }
        } catch (SQLException e) {
            closeStatement(stmt);
            throw new IOException("Error preparing connection for "
                                  + "batch job : " + e.getMessage(), e);
        }

        BatchJob job;
        try {
            job = new BatchJob(jobName, log, base, minMtime, maxMtime, options);
        } catch (ScriptException e) {
            throw new IOException("Error creating batch job '"
                                  + jobName + "' : " + e.getMessage(), e);
        }

        // Convert time to the internal binary format used by DatabaseStorage
        long maxTimestamp = timestampGenerator.baseTimestamp(
                maxMtime > UniqueTimestampGenerator.MAX_TIME ?
                                UniqueTimestampGenerator.MAX_TIME : maxMtime);
        long minTimestamp = timestampGenerator.baseTimestamp(
                minMtime > UniqueTimestampGenerator.MAX_TIME ?
                                UniqueTimestampGenerator.MAX_TIME : minMtime);
        long totalCount = 0;
        long pageCount = pageSize;
        try {
            // Page through all records in the result set in one transaction
            while (pageCount >= pageSize) {
                pageCount = 0;
                stmt.setLong(1, maxTimestamp);
                stmt.setLong(2, minTimestamp);
                if (base != null) stmt.setString(3, base);
                stmt.execute();
                ResultSet cursor = stmt.getResultSet();

                // Step into the result set if there are any results
                if (!cursor.next()) {
                    return "";
                }

                // Read the current page. Note that we must track
                // the record id since it's in effect a new record
                // if it's changed (we must purge the old one)
                while (!cursor.isAfterLast()) {                    
                    minTimestamp = cursor.getLong(MTIME_COLUMN);
                    Record record = scanRecord(cursor);
                    String oldId = record.getId();
                    if (!options.allowsRecord(record)) {
                        continue;
                    }

                    // Set up the batch job context and run it
                    log.debug(String.format(
                            "Running batch job '%s' on '%s' (%s)",
                            job, record.getId(), totalCount));
                    job.setContext(
                            record, totalCount == 0, cursor.isAfterLast());
                    try {
                        job.eval();
                    } catch (ScriptException e) {
                        throw new IOException(String.format(
                                "Error running batch job '%s': %s",
                                 job, e.getMessage()), e);
                    }
                    if (job.shouldCommit()) {
                        // If the record id has changed we must flush() the
                        // new record (in order to insert/update it) and then
                        // delete the old one from the db
                        if (oldId.equals(record.getId())) {
                            updateRecordWithConnection(record, options, conn);
                        } else {
                            log.debug(String.format(
                                    "Record renamed '%s' -> '%s'",
                                    oldId, record.getId()));
                            flushWithConnection(record, options, conn);
                            PreparedStatement delete = conn.prepareStatement(
                                                   "DELETE FROM " + RECORDS
                                                + " WHERE " + ID_COLUMN + "=?");
                            delete.setString(1, oldId);
                            delete.executeUpdate();
                        }
                    }
                    totalCount++;
                    pageCount++;
                }
                cursor.close();
            }

            // Commit the full transaction
            // FIXME: It would probably save memory to do incremental commits
            stmt.getConnection().commit();
        } catch (SQLException e) {
            String msg = String.format(
                    "Error running batch job '%s': %s", job, e.getMessage());
            log.error(msg, e);
            stmt.getConnection().rollback();
            throw new IOException(msg, e);
        } finally {
            closeStatement(stmt);
        }

        // We must update the base here because the updated records might have
        // changed their bases!
        if (totalCount > 0 && base != null) {
            updateModificationTime(base);
        }

        return job.getOutput();
    }

    private void updateRecordForRow(ResultSet cursor, Record record) throws SQLException {
        log.debug("Updating '" + record.getId() +"'");
        boolean hasRelations =
                record.hasParents() || record.hasChildren();

        // Make sure we store compressed record content
        record.compressContent();
        byte[] compressedContent = record.getContent(false);

        cursor.updateString(ID_COLUMN, record.getId());
        cursor.updateString(BASE_COLUMN, record.getBase());
        cursor.updateInt(
                DELETED_COLUMN, boolToInt(record.isDeleted()));
        cursor.updateInt(
                INDEXABLE_COLUMN, boolToInt(record.isIndexable()));
        cursor.updateInt(
                HAS_RELATIONS_COLUMN, boolToInt(hasRelations));
        cursor.updateLong(MTIME_COLUMN, timestampGenerator.next());
        cursor.updateBytes(DATA_COLUMN, compressedContent);
        cursor.updateBytes(META_COLUMN, record.hasMeta() ?
                                        record.getMeta().toFormalBytes() :
                                        new byte[0]);
        cursor.updateRow();

        // Set last update time for the record's base () note that the
        // base of the record might have changed!
        updateModificationTime(record.getBase());

        // FIXME: Update relations - but which semantics to use?
    }

    /* Create parent/child and child/parent relations for the given record */
    private void createRelations (Record rec, Connection conn)
                                                           throws SQLException {
        PreparedStatement stmt =
                             conn.prepareStatement(stmtCreateRelation.getSql());

        if (rec.hasChildren()) {
            List<String> childIds = rec.getChildIds();

            for (String childId : childIds) {
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
                            log.debug("Relation "+ rec.getId() + " -> "
                                      + childId + ", already known");
                        }
                    } else {
                        closeStatement(stmt);
                        throw new SQLException(String.format(
                                "Error creating child relations for %s",
                                rec.getId()), e);
                    }
                }
            }

            // Make sure that all children are tagged as having relations,
            // unless the record is excepted from relations tracking
            if (shouldTrackRelations(rec)) {
                markHasRelations(childIds, conn);
            }
        }

        if (rec.hasParents()) {
            List<String> parentIds = rec.getParentIds();

            for (String parentId : parentIds) {
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
                        closeStatement(stmt);
                        throw new SQLException("Error creating parent relations"
                                               + " for " + rec.getId(), e);
                    }
                }
            }

            // Make sure that all parents are tagged as having relations
            // unless the record is excepted from relations tracking
            if (shouldTrackRelations(rec)) {
                markHasRelations(parentIds, conn);
            }
        }

        closeStatement(stmt);
    }

    // Return false if the record should be excepted from relations tracking
    private boolean shouldTrackRelations (Record rec) {
        return !disabledRelationsTracking.contains(rec.getBase());
    }

    /**
     * Set the {@code hasRelations} column to {@code true} on the listed
     * childIds
     * @param childIds
     */
    private void markHasRelations(List<String> childIds,
                                  Connection conn) {
        // We can't use a PreparedStatement here because the parameter list
        // to the IN clause is of varying length
        String sql = "UPDATE " + RECORDS
                     + " SET " + HAS_RELATIONS_COLUMN + "=" + boolToInt(true)
                     + " WHERE id IN ('"
                     + Strings.join(childIds, "','")
                     + "')";

        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            log.warn("Failed to mark " + Strings.join(childIds, ", ")
                     + " as having relations: " + e.getMessage(), e);
        } finally {
            closeStatement(stmt);
        }
    }

    private void checkHasRelations(String id, Connection conn)
                                                           throws SQLException {;
        PreparedStatement stmt =
                              conn.prepareStatement(stmtGetRelatedIds.getSql());

        boolean hasRelations = false;

        log.debug("Checking relations for: " + id);

        /* Detect if we have any relations for 'id' */
        try {
            stmt.setString(1, id);
            stmt.setString(2, id);
            stmt.executeQuery();
            ResultSet results = stmt.getResultSet();

            if (results.next()) {
                hasRelations = true;
            }

            results.close();
        } catch (SQLException e) {
            log.warn("Failed to check relations for " + id + ": "
                     + e.getMessage(), e);
        } finally {
            closeStatement(stmt);
        }

        /* Return if we don't have any relations */
        if (!hasRelations) {
            if (log.isTraceEnabled()) {
                log.trace("No relations for record " + id);
            }
            return;
        }

        /* Set a check mark in the hasRelations column */
        log.debug("Marking " + id + " as having relations");
        try {
            stmt = conn.prepareStatement(stmtMarkHasRelations.getSql());
            stmt.setString(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.warn("Failed to mark " + id + " as having relations: "
                     + e.getMessage(), e);
        } finally {
            closeStatement(stmt);
        }
    }

    private void createNewRecordWithConnection(
                     Record record, QueryOptions options, Connection conn)
                                              throws IOException, SQLException {
        if (log.isTraceEnabled()) {
            log.debug("Creating: " + record.getId());
        }

        PreparedStatement stmt =
                               conn.prepareStatement(stmtCreateRecord.getSql());

        long nowStamp = timestampGenerator.next();
        boolean hasRelations = record.hasParents() || record.hasChildren();

        try{
            stmt.setString(1, record.getId());
            stmt.setString(2, record.getBase());
            stmt.setInt(3, boolToInt(record.isDeleted()));
            stmt.setInt(4, boolToInt(record.isIndexable()));
            stmt.setInt(5, boolToInt(hasRelations));
            stmt.setLong(6, nowStamp);
            stmt.setLong(7, nowStamp);
            stmt.setBytes(8, Zips.gzipBuffer(record.getContent()));
            stmt.setBytes(9,record.hasMeta() ?
                                         record.getMeta().toFormalBytes() :
                                         new byte[0]);
            stmt.executeUpdate();
        } finally {
            closeStatement(stmt);
        }

        if (hasRelations) {
            createRelations(record, conn);
        } else {
            // If the record does not have explicit relations we have to check
            // if we know any relations for it already, unless the record
            // is excepted from relations tracking of course
            if (shouldTrackRelations(record)) {
                checkHasRelations(record.getId(), conn);
            }
        }
        
    }

    /* Note that creationTime isn't touched */
    private void updateRecordWithConnection(
                  Record record, QueryOptions options, Connection conn)
                                              throws IOException, SQLException {
        log.debug("Updating: " + record.getId());

        // Respect the TRY_UPDATE meta flag. See docs for QueryOptions
        if (options != null &&
            "true".equals(options.meta(TRY_UPDATE))) {
            Record old = getRecordWithConnection(record.getId(), options, conn);
            if (record.equals(old)) {
                log.debug("Record '%s' already up to date, skipping update");
                return;
            }
        }

        long nowStamp = timestampGenerator.next();
        boolean hasRelations = record.hasParents() || record.hasChildren();

        PreparedStatement stmt =
                               conn.prepareStatement(stmtUpdateRecord.getSql());

        try {
            stmt.setString(1, record.getBase());
            stmt.setInt(2, boolToInt(record.isDeleted()));
            stmt.setInt(3, boolToInt(record.isIndexable()));
            stmt.setInt(4, boolToInt(hasRelations));
            stmt.setLong(5, nowStamp);
            stmt.setBytes(6, Zips.gzipBuffer(record.getContent()));
            stmt.setBytes(7, record.hasMeta() ?
                                         record.getMeta().toFormalBytes() :
                                         new byte[0]);
            stmt.setString(8, record.getId());
            stmt.executeUpdate();

            if (stmt.getUpdateCount() == 0) {
                String msg = "The record with id '" + record.getId()
                             + "' was marked as modified, but did not exist in"
                             + " the database";
                log.warn(msg);
                throw new IOException(msg);
            }
        } finally {
            closeStatement(stmt);
        }

        if (hasRelations) {
            createRelations(record, conn);
        } else {
            // If the record does not have explicit relations we have to check
            // if we know any relations for it already, unless the record
            // is excepted from relations tracking of course
            if (shouldTrackRelations(record)) {
                checkHasRelations(record.getId(), conn);
            }
        }
     }

    protected void touchRecord (String id, Connection conn)
                                              throws IOException, SQLException {
        PreparedStatement stmt =
                                conn.prepareStatement(stmtTouchRecord.getSql());
        log.debug("Touching: " + id);

        try {
            stmt.setLong(1, timestampGenerator.next());
            stmt.setString(2, id);
            stmt.executeUpdate();
        } finally {
            closeStatement(stmt);
        }

        if (log.isTraceEnabled()) {
            log.trace("Touched: " + id);
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
        try {
            doCreateSchema();
        } catch (Exception e) {
            log.fatal("Error creating or checking database tables: "
                      + e.getMessage(), e);
            throw new IOException("Error creating or checking database tables: "
                                  + e.getMessage());
        }
    }

    private void doCreateSchema() throws SQLException {

        Connection conn = null;

        try {
            conn = getDefaultConnection();

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
            closeConnection(conn);
        }
    }

    private void doCreateSummaRelationsTable(Connection conn)
                                                           throws SQLException {
        Statement stmt;

        String createRelationsQuery =
                "CREATE TABLE IF NOT EXISTS " + RELATIONS + " ("
                + PARENT_ID_COLUMN     + " VARCHAR(" + ID_LIMIT + "), "
                + CHILD_ID_COLUMN      + " VARCHAR(" + ID_LIMIT + ") )";
        log.debug("Creating table "+RELATIONS+" with query: '"
                  + createRelationsQuery + "'");
        stmt = conn.createStatement();
        stmt.execute(createRelationsQuery);
        stmt.close();

        /* RELATIONS INDEXES */
        String createRelationsPCIndexQuery =
                "CREATE UNIQUE INDEX IF NOT EXISTS pc ON "
                + RELATIONS + "("+PARENT_ID_COLUMN+","+CHILD_ID_COLUMN+")";
        log.debug("Creating index 'pc' on table "+RELATIONS+" with query: '"
                  + createRelationsPCIndexQuery + "'");
        stmt = conn.createStatement();
        stmt.execute(createRelationsPCIndexQuery);
        stmt.close();

        String createRelationsCIndexQuery =
                "CREATE INDEX IF NOT EXISTS c ON "
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
                "CREATE TABLE IF NOT EXISTS " + RECORDS + " ("
                + ID_COLUMN        + " VARCHAR(" + ID_LIMIT + ") PRIMARY KEY, "
                + BASE_COLUMN      + " VARCHAR(" + BASE_LIMIT + "), "
                + DELETED_COLUMN   + " INTEGER, "
                + INDEXABLE_COLUMN + " INTEGER, "
                + HAS_RELATIONS_COLUMN + " INTEGER, "
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
                "CREATE UNIQUE INDEX IF NOT EXISTS i ON " + RECORDS + "("+ID_COLUMN+")";
        log.debug("Creating index 'i' on table "+RECORDS+" with query: '"
                  + createRecordsIdIndexQuery + "'");
        stmt = conn.createStatement();
        stmt.execute(createRecordsIdIndexQuery);
        stmt.close();

        // Because we use a UniqueTimestampGenerator we can apply the UNIQUE
        // to the 'mtime' column. This is paramount to getting paginated result
        // sets for getRecordsModifiedAfter. To make selects by mtime and base
        // faster we use a covering index on (mtime,base)
        String createRecordsMTimeIndexQuery =
                "CREATE UNIQUE INDEX IF NOT EXISTS mb ON "
                               + RECORDS + "("+MTIME_COLUMN+","+BASE_COLUMN+")";
        log.debug("Creating index 'mb' on table "+RECORDS+" with query: '"
                  + createRecordsMTimeIndexQuery + "'");
        stmt = conn.createStatement();
        stmt.execute(createRecordsMTimeIndexQuery);
        stmt.close();

        // This index is used to speed up record counts segregated by base,
        // deleted- and indexable flags.
        String createRecordsBaseIndexQuery =
                "CREATE INDEX IF NOT EXISTS bdi ON "
                + RECORDS +"("
                + BASE_COLUMN + ","+DELETED_COLUMN+","+INDEXABLE_COLUMN+")";
        log.debug("Creating index 'bdi' on table "+RECORDS+" with query: '"
                  + createRecordsBaseIndexQuery + "'");
        stmt = conn.createStatement();
        stmt.execute(createRecordsBaseIndexQuery);
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

        Connection conn = getDefaultConnection();

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
            closeConnection(conn);
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
     *                      availability via
     *                      {@link ResultSetCursor#setResultSetHasNext}
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
        boolean hasRelations = intToBool(resultSet.getInt(5));
        byte[] gzippedContent = resultSet.getBytes(6);
        long ctime = resultSet.getLong(7);
        long mtime = resultSet.getLong(8);
        byte[] meta = resultSet.getBytes(9);
        String parentIds = resultSet.getString(10);
        String childIds = resultSet.getString(11);

        if (log.isTraceEnabled()) {
            log.trace ("Scanning record: " + id);
        }

        /* If the record is listed as parent or child of something this will
         * appear in the parent/child columns, so ignore these cases.
         * Also if the parent/child ids are empty strings they are injected
         * because we do lazy relation lookups */
        if (id.equals(parentIds) || "".equals(parentIds)) {
            parentIds = null;
        }
        if (id.equals(childIds) || "".equals(childIds)) {
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
                } else if (hasRelations != intToBool(resultSet.getInt(5))) {
                    log.warn("hasRelations state mismatch for record: " + id);
                    return null;
                }else if (!Arrays.equals(gzippedContent,
                                          resultSet.getBytes(6))) {
                    log.warn("Content mismatch for record: " + id);
                    return null;
                }  else if (ctime != resultSet.getLong(7)) {
                    log.warn("CTime state mismatch for record: " + id);
                    return null;
                } else if (mtime != resultSet.getLong(8)) {
                    log.warn("MTime state mismatch for record: " + id);
                    return null;
                }  else if (!Arrays.equals(meta,resultSet.getBytes(9))) {
                    log.warn("Meta tags mismatch for record: " + id);
                    return null;
                }
            }

            /* Pick up parent and child ids */
            String newParent = resultSet.getString (10);
            String newChild = resultSet.getString (11);

            /* If the record is listed as parent or child of something this
             * will appear in the parent/child columns, so ignore these cases */
            if (id.equals(newParent)) {
                newParent = null;
            }
            if (id.equals(newChild)) {
                newChild = null;
            }

            /* Treat empty strings as nulls because we inject empty strings in
             * the result set when we are doing lazy relation lookups */
            if (newParent != null && !"".equals(newParent)) {
                parentIds = parentIds != null ?
                                (parentIds + ";" + newParent) : newParent;
            }

            if (newChild != null && !"".equals(newChild)) {
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
        Record rec = new Record(id,
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

        /* Only resolve relations if we have to, that is, if
         * "useRelations && hasRelations". Moreover some codepaths will have
         * queried the relations even though useLazyRelations==true so don't
         * resolve the relations in that case
         */
        if (useLazyRelations && hasRelations
            && parentIds == null && childIds == null) {
            resolveRelatedIds(rec,resultSet.getStatement().getConnection());
        }

        return rec;
    }

    /**
     * As {@link #scanRecord(ResultSet, ResultSetCursor)} with
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
        ResultSet resultSet;

        log.trace("Getting results for '" + stmt + "'");
        long startQuery = System.currentTimeMillis();

         try {
             resultSet = stmt.executeQuery();
             log.debug("Got results from '" + stmt + "' in "
                       + (System.currentTimeMillis() - startQuery) + "ms");
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

    @Override
    public void close() throws IOException {
        log.info("Closing");

        iteratorReaper.stop();

        log.info("Closed");
    }

    // FIXME: In Summa 2.0 we might want to make this public API
    public List<BaseStats> getStats() throws IOException {
        log.trace("getStats()");

        Connection conn = null;
        try {
            conn = getConnection();
            return getStatsWithConnection(conn);
        } catch (SQLException e) {
            throw new IOException("Could not get database stats", e);
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

    private List<BaseStats> getStatsWithConnection(Connection conn)
                                              throws SQLException, IOException {
        long startTime = System.currentTimeMillis();
        List<BaseStats> stats = new LinkedList<BaseStats>();
        String query =
                "SELECT base, deleted, indexable, count(base) "
              + "FROM summa_records "
              + "GROUP BY base,deleted,indexable";

        Statement stmt = conn.createStatement();
        ResultSet result = stmt.executeQuery(query);

        try {
            result.next();
            while (!result.isAfterLast()) {
                String base = result.getString(1);
                String lastBase = base;
                long deletedIndexables = 0;
                long nonDeletedIndexables = 0;
                long deletedNonIndexables = 0;
                long nonDeletedNonIndexables = 0;

                // Collect all stats for the current base and append it
                // to the stats list
                while (lastBase.equals(base)) {
                    boolean deleted = intToBool(result.getInt(2));
                    boolean indexable = intToBool(result.getInt(3));
                    long count = result.getLong(4);

                    // These boolean cases could be simplified, but we list
                    // them all explicitely to help the (human) reader
                    if (deleted && indexable) {
                        deletedIndexables = count;
                    } else if (deleted && !indexable) {
                        deletedNonIndexables = count;
                    } else  if (!deleted && indexable) {
                        nonDeletedIndexables = count;
                    } else if (!deleted && !indexable) {
                        nonDeletedNonIndexables = count;
                    }

                    result.next();
                    if (!result.isAfterLast()) {
                        base = result.getString(1);
                    } else {
                        break;
                    }
                }
                stats.add(new BaseStats(
                        lastBase, getModificationTime(lastBase), startTime,
                        deletedIndexables, nonDeletedIndexables,
                        deletedNonIndexables, nonDeletedNonIndexables));
            }
        } finally {
            result.close();
        }

        log.debug(String.format("Extracted storage stats in %sms",
                                (System.currentTimeMillis() - startTime)));

        return stats;
    }

}



