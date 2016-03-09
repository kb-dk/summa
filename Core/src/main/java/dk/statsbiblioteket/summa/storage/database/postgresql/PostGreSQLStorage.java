package dk.statsbiblioteket.summa.storage.database.postgresql;


import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.MiniConnectionPoolManager;
import dk.statsbiblioteket.summa.storage.database.MiniConnectionPoolManager.StatementHandle;
import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class PostGreSQLStorage  extends DatabaseStorage implements Configurable {

    private static Log log = LogFactory.getLog(PostGreSQLStorage.class);

    /**
     * The driver for the storage. This is rarely changed.
     * </p><p>
     * Optional. Default is org.postgresql.Driver.
     */
    public static final String CONF_DRIVER = "database.driver";
    public static final String DEFAULT_DRIVER = "org.postgresql.Driver";

    /**
     * URL to the database.
     * </p><p>
     * Mandatory. Sample: jdbc:postgresql://devel06:5432/elba-devel
     */
    public static final String CONF_DRIVER_URL = "database.driver.url";

    /**
     * The DB username.
     */
    private String username;
    /**
     * The DB password.
     */
    private String password;
    
    
    /**
     * The max allowed connection to the DB.
     */
    private int maxConnections = 20;


    private String driverUrl;
    
    private String driver;
    
    /**
     * The connection pool.
     */
    private MiniConnectionPoolManager pool;
   
    
    /**
     * Creates a  database storage, given the configuration.
     *
     * @param conf
     *            The configuration.
     * @throws IOException
     *             If error occur while doing IO work for creation of database.
     */
    public PostGreSQLStorage(Configuration conf) throws IOException {
        super(conf);
        log.trace("Constructing PostGreSQLStorage");

        username = conf.getString(CONF_USERNAME, "");        
        password = conf.getString(CONF_PASSWORD, "");

        maxConnections = conf.getInt(CONF_MAX_CONNECTIONS, maxConnections);

        driverUrl = conf.getString(CONF_DRIVER_URL); //"jdbc:postgresql://devel06:5432/elba-devel";
        driver = conf.getString(CONF_DRIVER, DEFAULT_DRIVER);;
        
        log.debug("PostGreSqlStorage extracted properties username: " + username + ", password: " + (password == null ? "[undefined]" : "[defined]") + ",0 driver: '" + driver + "', createNew: ");
        init(conf);

        log.info("Started " + this);
    }


  

    /**
     * The method {@link DatabaseStorage#touchParents} does the touching in one single SQL call. This call involves a nested <code>SELECT</code> which triggers
     * a performance issue on large databases.
     * <p/>
     * Because of this we override the generic method with one that does manual looping over all parents. It should still perform fairly good.
     *
     * @param id
     *            The record id of the record which parents to update.
     * @param options
     *            Any query options that may affect how the touching should be carried out.
     * @param conn
     *            The database connection.
     * @throws IOException
     *             In case of communication errors with the database.
     * @throws SQLException
     *             If error occur while executing the SQL.
     */
    @Override
    protected void touchParents(String id, QueryOptions options, Connection conn) throws IOException, SQLException {
        touchParents(id, null, options, conn);
    }

    /**
     * @param id
     *            The record id of the record which parents to update.
     * @param options
     *            Any query options that may affect how the touching should be carried out.
     * @param conn
     *            The database connection.
     * @param touched
     *            The set of already touched IDs.
     * @throws IOException
     *             In case of communication errors with the database.
     * @throws SQLException
     *             If error occur while executing the SQL.
     * @see #touchParents(String, QueryOptions, Connection);
     */
    private void touchParents(String id, Set<String> touched, QueryOptions options, Connection conn) throws IOException, SQLException {
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

        touched = touched != null ? touched : new HashSet<String>(10);

        // Touch each parent and recurse upwards
        for (Record parent : parents) {
            if (touched.contains(parent.getId())) {
                log.trace("Parent '" + parent.getId() + "' already touched");
                continue;
            }
            touched.add(parent.getId());
            touchRecord(parent.getId(), conn);
            touchParents(parent.getId(), touched, options, conn);
        }
    }

    
    @Override
    protected void connectToDatabase(Configuration configuration) throws IOException {
        log.info("Establishing connection to H2 with  username '" + username + "', password " + (password == null || "".equals(password) ? "[undefined]" : "[defined]") + ", driver '" + driver);
       

        /*
         * The database source.
         */
        //JdbcDataSource dataSource = new JdbcDataSource();

     //BasicManagedDataSource dataSource = new BasicManagedDataSource(); 
     DriverAdapterCPDS dataSource = new DriverAdapterCPDS();

        if (username != null) {
            dataSource.setUser(username);
        }

        if (password != null) {
            dataSource.setPassword(password);
         }
      try{
     
        dataSource.setDriver(driver);
      }
      catch(Exception e){
          throw new IOException(e);
      }
 
 
     dataSource.setUrl(driverUrl);
     
        
        pool = new MiniConnectionPoolManager(dataSource,  maxConnections);

        
        log.info("Connected to database at '" + driver + "'");

        //TODO set these for postgres ?
      //  setMaxMemoryRows();

    }


    @Override
    protected Connection getConnection() {
        Connection connection = pool.getConnection();
        try{
        connection.setAutoCommit(false);
        connection.setReadOnly(false);
        connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        }
        catch(Exception e){
            log.warn("Still a problem changing connection properties on existing connection.");
        }
        return connection;
    }
   
    /**
     * Return a prepared statement, from the statement handler.
     *
     * @param handle
     *            The statement handler.
     * @return The prepared statement.
     * @throws SQLException
     *             If error occurs while manage statements.
     */
    @Override
    protected PreparedStatement getManagedStatement(StatementHandle handle) throws SQLException {
        return pool.getManagedStatement(handle);
    }

    
    /**
     * Prepare a SQL statement.
     *
     * @param sql
     *            The SQL statement.
     * @return The statement handle created from the SQL statement.
     */
    @Override
    protected StatementHandle prepareStatement(String sql) {
        return pool.prepareStatement(sql);
    }


    /**
     * Return the meta column data declaration.
     *
     * @return The meta column " BYTEA".
     */
    @Override
    protected String getMetaColumnDataDeclaration() {
        return " BYTEA";
    }

    /**
     * Return the data column data declaration.
     *
     * @return The data column " BYTEA".
     */
    @Override
    protected String getDataColumnDataDeclaration() {
        return " BYTEA";
    }

    
    
}
