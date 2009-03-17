package dk.statsbiblioteket.summa.storage.database.h2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.h2.jdbcx.JdbcDataSource;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.*;
import java.util.List;

import dk.statsbiblioteket.summa.storage.database.MiniConnectionPoolManager;
import dk.statsbiblioteket.summa.storage.database.MiniConnectionPoolManager.StatementHandle;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.ManagedStatement;
import dk.statsbiblioteket.summa.storage.StorageUtils;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.Files;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

/**
 * Storage implementation on top of the H" database engine.
 */
public class H2Storage extends DatabaseStorage implements Configurable {

    private static Log log = LogFactory.getLog(H2Storage.class);

    private static final int BLOB_MAX_SIZE = 50*1024*1024; // MAX_VALUE instead?
    public static final int META_LIMIT =     50*1024*1024; // MAX_VALUE instead?

    /**
     * We optimize the index statistics after this many flushes
     */
    public static final int OPTIMIZE_INDEX_THRESHOLD = 100000;

    private String username;
    private String password;
    private File location;
    private int maxConnections;
    private boolean createNew = true;
    private boolean forceNew = false;

    private JdbcDataSource dataSource;
    private H2ConnectionPool pool;
    private long numFlushes;

    /**
     * We need to create a custom connection pool because H2 doesn't support
     * StatementEventListeners
     */
    private static class H2ConnectionPool extends MiniConnectionPoolManager {

        public H2ConnectionPool(ConnectionPoolDataSource dataSource,
                                int maxConnections) {
            super(dataSource, maxConnections);
        }

        public H2ConnectionPool(ConnectionPoolDataSource dataSource,
                                int maxConnections, int timeout) {
            super(dataSource, maxConnections, timeout);
        }

        @Override
        public PreparedStatement getStatement(StatementHandle handle)
                throws SQLException {

            PooledConnection pconn = getPooledConnection();
            Connection conn = pconn.getConnection();

            if (log.isTraceEnabled()) {
                log.trace("Getting statement for handle " + handle
                          + " on connection " + pconn.hashCode());
            }

            // We prepare a new statement on each invocation.
            // This might look insane but the JDBC _should_ be caching the
            // statements for us
            PreparedStatement stmt = conn.prepareStatement(handle.getSql());

            // We wrap the statement in a special class that closes the
            // underlying connection when the statement is closed
            return new ManagedStatement(stmt);
        }
    }

    public H2Storage(Configuration conf) throws IOException {
        super(conf);
        log.trace("Constructing H2Storage");
        numFlushes = 0;
        username = conf.getString(CONF_USERNAME, "");
        password = conf.getString(CONF_PASSWORD, "");
        maxConnections = conf.getInt(CONF_MAX_CONNECTIONS,
                                     DEFAULT_MAX_CONNECTIONS);

        // Database file location
        if (conf.valueExists(CONF_LOCATION)) {
            log.debug("Property '" + CONF_LOCATION + "' exists, using value '"
                      + conf.getString(CONF_LOCATION) + "' as location");
            location = new File(conf.getString(CONF_LOCATION));
        } else {
            location = new File (StorageUtils.getGlobalPersistentDir(conf),
                                 "storage" + File.separator + "derby");
            log.debug("Using default location '" + location + "'");
        }

        if (!location.equals(location.getAbsoluteFile())) {
            log.debug(String.format("Transforming relative location '%s' to "
                                    + "absolute location'%s'",
                                    location, location.getAbsoluteFile()));
            location = location.getAbsoluteFile();
        }

        if (!location.getParentFile().exists()) {
            location.getParentFile().mkdirs ();
        }

        // Create new DB?
        if (conf.valueExists(CONF_CREATENEW)) {
                createNew = conf.getBoolean(CONF_CREATENEW);
        }

        // Force new DB?
        if (conf.valueExists(CONF_FORCENEW)) {
                forceNew = conf.getBoolean(CONF_FORCENEW);
        }

        log.debug("H2Storage extracted properties username: " + username
                 + ", password: "
                  + (password == null ? "[undefined]" : "[defined]")
                  + ", location: '" + location + "', createNew: " + createNew
                  + ", forceNew: " + forceNew);
        init(conf);
        log.trace("Construction completed");
    }

    @Override
    protected void connectToDatabase(Configuration configuration) throws
                                                               IOException {
        log.info("Establishing connection to Derby with  username '" + username
                 + "', password " + (password == null || "".equals(password) ?
                                            "[undefined]" : "[defined]")
                 + ", location '" + location + "', createNew " + createNew
                 + " and forceNew " + forceNew);

        if (location.exists()) { /* Database location exists*/
            log.debug("Old database found at '" + location + "'");
            if (forceNew) {
                log.info("Deleting existing database at '" + location + "'");
                try {
                    Files.delete(location);
                } catch (IOException e) {
                    throw new RemoteException("Could not delete old database "
                                              + "at '" + location + "'", e);
                }
            } else {
                log.info("Reusing old database at '" + location + "'");
                createNew = false;
            }
        } else {
            log.debug("No database at '" + location + "'");
            if (createNew) {
                log.debug("A new database will be created at '" + location
                          + "'");
            } else {
                throw new IOException("No database exists at '" + location
                                          + " and createNew is false. The "
                                          + "metadata storage cannot run "
                                          + "without a backend. Exiting");
            }
        }

        dataSource = new JdbcDataSource();

        dataSource.setURL("jdbc:h2:"+location.getAbsolutePath());

        if (username != null) {
            dataSource.setUser(username);
        }

        if (password != null) {
            dataSource.setPassword(password);
        }

        pool = new H2ConnectionPool(dataSource, maxConnections);

        log.info("Connected to database at '" + location + "'");

        if (createNew) {
            log.info("Creating new table for '" + location + "'");
            createSchema();
        }

        setMaxMemoryRows();
        optimizeTables();
    }

    private void optimizeTables() {
        Connection conn = getConnection();
        try {
            // Rebuild the table selectivity indexes used by the query optimizer
            log.debug("Optimizing table selectivity");
            Statement stmt = conn.createStatement();
            stmt.execute("ANALYZE");
        } catch (SQLException e) {
            log.warn("Failed to optimize table selectivity");
            return;
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                log.warn("Failed to close connection: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void flush(Record rec) throws IOException {
        numFlushes++;
        numFlushes = numFlushes % OPTIMIZE_INDEX_THRESHOLD;

        if (numFlushes == 0) {
            optimizeTables();
        }

        super.flush(rec);
    }

    /**
     * The purpose of this method is to make sure that H2's limit on the
     * maximum number of memory buffered rows is bigger than the pageSize
     * of the result sets
     */
    private void setMaxMemoryRows() {
        Connection conn = getConnection();
        try {
            // There might be several rows per record if the records has
            // relations. There will be one extra row per relation
            int maxMemoryRows = getPageSize()*3;
            log.debug("Setting MAX_MEMORY_ROWS to " + maxMemoryRows);
            Statement stmt = conn.createStatement();
            stmt.execute("SET MAX_MEMORY_ROWS " + maxMemoryRows);
        } catch (SQLException e) {
            log.warn("Failed to set MAX_MEMORY_ROWS this may affect performance"
                      + " on large result sets");
            return;
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                log.warn("Failed to close connection: " + e.getMessage(), e);
            }
        }
    }

    /**
     * We override this method because H2 only uses its custom
     * JdbcSQLExceptions, and not the proper exceptions according to the
     * JDBC api
     * @param e sql exception to inspect
     * @return whether or not {@code e} represents an integrity constraint
     *         violation
     */
    @Override
    protected boolean isIntegrityConstraintViolation (SQLException e) {
        // The H2 error codes for integrity constraint violations are
        // numbers between 23000 and 23999
        return (e instanceof SQLIntegrityConstraintViolationException ||
                (e.getErrorCode() >= 23000 && e.getErrorCode() < 24000));
    }

    @Override
    protected Connection getConnection() {
        return pool.getConnection();
    }

    @Override
    protected StatementHandle prepareStatement(String sql) {
        return pool.prepareStatement(sql);
    }

    @Override
    protected PreparedStatement getStatement(StatementHandle handle)
                                                           throws SQLException {
        return pool.getStatement(handle);
    }

    @Override
    protected String getMetaColumnDataDeclaration() {
        return " BYTEA";
    }

    @Override
    protected String getDataColumnDataDeclaration() {
        return " BYTEA";
    }

    /**
     * The method {@link DatabaseStorage#touchParents} does the touching
     * in one single SQL call. This call involves a nested <code>SELECT</code>
     * which triggers a performance issue on large databases.
     * <p/>
     * Because of this we override the generic method with one that does manual
     * looping over all parents. It should still perform fairly good.
     *
     * @param id the record id of the record which parents to update
     * @param options any query options that may affect how the touching should
     *                be carried out
     * @throws IOException in case of communication errors with the database
     */
    @Override
    protected void touchParents(String id,
                                QueryOptions options, Connection conn)
                                              throws IOException, SQLException {
        if (log.isTraceEnabled()) {
            log.trace("Preparing to touch parents of " + id);
        }

        List<Record> parents = getParents(id, options, conn);

        if (parents == null || parents.isEmpty()) {
            if (log.isTraceEnabled()) {
                log.trace("No parents to update for record " + id);
            }
            return;
        }

        // Touch each parent and recurse upwards
        for (Record parent : parents) {
            touchRecord(parent.getId(), conn);
            touchParents(parent.getId(), options, conn);
        }
    }

    // H2 is very bad at large result sets (_very_)
    @Override
    public boolean usePagingResultSets() {
        return true;
    }

    // Genrally Java embedded DBs are bad at JOINs over tables with many rows
    @Override
    public boolean useLazyRelationLookups() {
        return true;
    }

    @Override
    public String getPagingStatement(String sql) {
        return sql + " LIMIT ?";
    }



}
