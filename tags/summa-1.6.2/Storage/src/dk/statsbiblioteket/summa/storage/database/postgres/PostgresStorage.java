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
package dk.statsbiblioteket.summa.storage.database.postgres;

import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.MiniConnectionPoolManager;
import dk.statsbiblioteket.summa.storage.database.MiniConnectionPoolManager.StatementHandle;
import dk.statsbiblioteket.summa.storage.database.ManagedStatement;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.postgresql.ds.PGConnectionPoolDataSource;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

/**
 * Summa Storage backend that is implemented on top of a PostgresQL database.
 */
public class PostgresStorage extends DatabaseStorage implements Configurable {
    private static Log log = LogFactory.getLog(PostgresStorage.class);

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String driver = "org.postgresql.Driver";

    private String username;
    private String password;
    private String database;
    private String host;
    private int port;
    private int maxConnections;

    private PGConnectionPoolDataSource dataSource;
    private MiniConnectionPoolManager pool;

    /**
     * We need to create a custom connection pool because H2 doesn't support
     * StatementEventListeners
     */
    private static class PostgresConnectionPool extends MiniConnectionPoolManager {

        public PostgresConnectionPool(ConnectionPoolDataSource dataSource,
                                      int maxConnections) {
            super(dataSource, maxConnections);
        }

        public PostgresConnectionPool(ConnectionPoolDataSource dataSource,
                                      int maxConnections, int timeout) {
            super(dataSource, maxConnections, timeout);
        }

        @Override
        public PreparedStatement getManagedStatement(StatementHandle handle)
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

    public PostgresStorage (Configuration conf) throws IOException {
        super(conf);
        log.trace("Constructing PostgresStorage");

        username = conf.getString(CONF_USERNAME, DEFAULT_USERNAME);
        password = conf.getString(CONF_PASSWORD, null);
        database = conf.getString(CONF_DATABASE, DEFAULT_DATABASE);
        host = conf.getString(CONF_HOST, DEFAULT_HOST);
        port = conf.getInt(CONF_PORT, -1);
        maxConnections = conf.getInt(CONF_MAX_CONNECTIONS,
                                     DEFAULT_MAX_CONNECTIONS);

        init(conf);
        log.trace("Construction completed");
    }

    @Override
    protected void connectToDatabase(Configuration configuration) throws
                                                                  IOException {
        //noinspection DuplicateStringLiteralInspection
        log.info("Establishing connection to PostgresQL base '" + database + "'"
                 + " on host '" + host + "'"
                 + (port > 0 ? (" port " + port + ",") : "") + " with"
                 + " username '" + username
                 + "', password "
                 + (password == null ? "[undefined]" : "[defined]"));


        dataSource = new PGConnectionPoolDataSource();

        dataSource.setDatabaseName(database);

        if (port > 0) {
            dataSource.setPortNumber(port);
        }

        if (username != null) {
            dataSource.setUser(username);
        }

        if (password != null) {
            dataSource.setPassword(password);
        }

        if (host != null && !"".equals(host)) {
            dataSource.setServerName(host);
        }

        pool = new PostgresConnectionPool(dataSource, maxConnections);

        log.info("Connected to database");

        try {            
            createSchema();
            log.info ("Created new database schemas");
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.info("Schemas NOT created: " + e.getMessage(), e);
            } else {
                log.info("Schemas NOT created: " + e.getMessage());
            }
        }
    }

    /**
     * We override this method because Postgres only throws generic
     * SQLExceptions, and not the proper exceptions according to the
     * JDBC api
     * @param e sql exception to inspect
     * @return whether or not {@code e} represents an integrity constraint
     *         violation
     */
    @Override
    protected boolean isIntegrityConstraintViolation (SQLException e) {
        // The PostgresQL error codes for integrity constraint violations are
        // numbers between 23000 and 23999. Strangely PostgresQL doesn't use
        // SQLException.getErrorCode() to store the error code in, but uses
        // SQLState instead...
        try {
            int errorCode = Integer.parseInt(e.getSQLState());
            return (e instanceof SQLIntegrityConstraintViolationException ||
                (errorCode >= 23000 && errorCode < 24000));
        } catch (NumberFormatException ex) {
            // Looks like PostgresQL is inconsistent in the way it handles
            // errors...
            return false;
        }


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
    protected PreparedStatement getManagedStatement(StatementHandle handle)
                                                            throws SQLException{
        return pool.getManagedStatement(handle);
    }

    @Override
    protected String getMetaColumnDataDeclaration() {
        return "BYTEA";
    }

    protected String getDataColumnDataDeclaration() {
        return "BYTEA";
    }
}

