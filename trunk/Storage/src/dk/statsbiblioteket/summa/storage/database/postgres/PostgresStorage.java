package dk.statsbiblioteket.summa.storage.database.postgres;

import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.MiniConnectionPoolManager;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.postgresql.ds.PGConnectionPoolDataSource;

/**
 * Created by IntelliJ IDEA. User: mke Date: Jan 9, 2009 Time: 2:05:57 PM To
 * change this template use File | Settings | File Templates.
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

        log.debug("PostgresStorage extracted properties username: " + username
                  + ", password: "
                  + (password == null ? "[undefined]" : "[defined]"));
        init(conf);
        log.trace("Construction completed");
    }

    @Override
    protected void connectToDatabase(Configuration configuration) throws
                                                                  IOException {
        //noinspection DuplicateStringLiteralInspection
        log.info("Establishing connection to PostgresQL on '" + host + "'"
                 + (port > 0 ? (" port " + port + ",") : "") + " with"
                 + " username '" + username
                 + "', password "
                 + (password == null || "".equals(password) ?
                    "[undefined]" : "[defined]"));


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

        if (host != null) {
            dataSource.setServerName(host);
        }

        pool = new MiniConnectionPoolManager(dataSource, maxConnections);

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
                                                            throws SQLException{
        return pool.getStatement(
                       (MiniConnectionPoolManager.PooledStatementHandle)handle);
    }

    @Override
    protected String getMetaColumnDataDeclaration() {
        return "BYTEA";
    }

    protected String getDataColumnDataDeclaration() {
        return "BYTEA";
    }
}
