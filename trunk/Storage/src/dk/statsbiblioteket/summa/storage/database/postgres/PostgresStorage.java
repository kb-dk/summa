package dk.statsbiblioteket.summa.storage.database.postgres;

import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.rmi.RemoteException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    private Connection connection;

    public PostgresStorage (Configuration conf) throws IOException {
        super(conf);
        log.trace("Constructing PostgresStorage");
                username = conf.getString(CONF_USERNAME, DEFAULT_USERNAME);
                password = conf.getString(CONF_PASSWORD, null);
                database = conf.getString(CONF_DATABASE, DEFAULT_DATABASE);
                host = conf.getString(CONF_HOST, DEFAULT_HOST);
                port = conf.getInt(CONF_PORT, -1);

                log.debug("PostgresStorage extracted properties username: " + username
                         + ", password: "
                          + (password == null ? "[undefined]" : "[defined]"));
                init(conf);
                log.trace("Construction completed");
            }

    // TODO: Consider is authentication should be used or not
    protected void connectToDatabase(Configuration configuration) throws
                                                                  IOException {
        //noinspection DuplicateStringLiteralInspection
        log.info("Establishing connection to JavaDB with driver '"
                 + driver + "', username '" + username + "', password "
                 + (password == null || "".equals(password) ?
                    "[undefined]" : "[defined]"));


        // Build URL
        String connectionURL = "jdbc:postgresql:" + database;
        if (port > 0 && host != null) {
            connectionURL = "jdbc:postgresql://"+ host + ":" + port + "/"
                            + database;
        } else if (port <= 0 && host != null) {
            connectionURL = "jdbc:postgresql://"+ host + "/" + database;
        }

        // Add user/pass
        String sans = connectionURL;
        if (username != null && !"".equals(username)) {
            connectionURL += "?user=" + username;
            sans = connectionURL;
            connectionURL += password == null ? "" : "&password=" + password;
        }
        log.debug("Connection-URL (sans password): '" + sans + "'");

        // Initialize the JDBC driver
        try{
            Class.forName(driver);
        } catch(java.lang.ClassNotFoundException e) {
            throw new RemoteException("Could not connect to the Postgres JDBC "
                                      + "driver '" + driver + "'", e);
        }

        // Get connection
        try {
            connection = DriverManager.getConnection(connectionURL);
        } catch (SQLException e) {
            throw new RemoteException("Could not establish connection to '"
                                      + sans + "'"
                                      + (password == null
                                         || "".equals(password)
                                         ? ""
                                         : " [password defined]"), e);
        }

        log.info("Connected to database: " + connectionURL);
    }

    protected Connection getConnection() {
        return connection;
    }
}
