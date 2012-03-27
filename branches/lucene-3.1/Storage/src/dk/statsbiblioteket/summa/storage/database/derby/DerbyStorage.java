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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: ControlDerby.java,v 1.7 2007/12/04 09:08:19 te Exp $
 */
package dk.statsbiblioteket.summa.storage.database.derby;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.List;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.MiniConnectionPoolManager;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.summa.storage.database.MiniConnectionPoolManager.StatementHandle;
import dk.statsbiblioteket.summa.storage.StorageUtils;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DerbyStorage extends DatabaseStorage implements Configurable {
    private static Log log = LogFactory.getLog(DerbyStorage.class);

    private static final int BLOB_MAX_SIZE = 50*1024*1024; // MAX_VALUE instead?
    public static final int META_LIMIT =     50*1024*1024; // MAX_VALUE instead?

    private String username;
    private String password;
    private File location;
    private int maxConnections;
    private boolean createNew = true;
    private boolean forceNew = false;

    private EmbeddedConnectionPoolDataSource dataSource;
    private MiniConnectionPoolManager pool;

    public DerbyStorage(Configuration conf) throws IOException {
        super(conf);
        log.trace("Constructing DerbyStorage");
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

        // Create new DB?
        if (conf.valueExists(CONF_CREATENEW)) {
                createNew = conf.getBoolean(CONF_CREATENEW);
        }

        // Force new DB?
        if (conf.valueExists(CONF_FORCENEW)) {
                forceNew = conf.getBoolean(CONF_FORCENEW);
        }

        log.debug("DerbyStorage extracted properties username: " + username
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

        dataSource = new EmbeddedConnectionPoolDataSource();

        dataSource.setDatabaseName(location.getAbsolutePath());

        if (createNew) {
            dataSource.setCreateDatabase("create");
        }

        if (username != null) {
            dataSource.setUser(username);
        }

        if (password != null) {
            dataSource.setPassword(password);
        }

        //pool = new H2Storage.H2ConnectionPool(dataSource, maxConnections);
        pool = new MiniConnectionPoolManager(dataSource, maxConnections);


        log.info("Connected to database at '" + location + "'");
        
        if (createNew) {
            log.info("Creating new table for '" + location + "'");
            createSchema();
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
        return " BLOB(" + META_LIMIT + ")";
    }

    @Override
    protected String getDataColumnDataDeclaration() {
        return " BLOB(" + BLOB_MAX_SIZE + ")";
    }

    /**
     * The method {@link DatabaseStorage#touchParents} does the touching
     * in one single SQL call. This call involves a nested <code>SELECT</code>
     * which triggers a known performance bug in Derby,
     * <a href="https://issues.apache.org/jira/browse/DERBY-4007">DERBY-4007</a>.
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
        boolean doTrace = log.isTraceEnabled();

        if (doTrace) {
            log.trace ("Touching parents of '" + id + "'");
        }

        List<Record> parents = getParents(id, options, conn);

        if (parents == null || parents.isEmpty()) {
            if (doTrace) {
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

    // Derby is very bad at large result sets
    @Override
    public boolean usePagingResultSets() {
        return true;
    }

    // Genrally Java embedded DBs are bad at JOINs over tables with many rows,
    // however Derby's internal connection handling is too random, so we need
    // to do the JOINs
    @Override
    public boolean useLazyRelationLookups() {
        return false;
    }

    @Override
    public String getPagingStatement(String sql) {
        // You gotta love this minimalistic syntax :-S
        return sql + " FETCH NEXT " + getPageSize() + " ROWS ONLY";
    }
}




