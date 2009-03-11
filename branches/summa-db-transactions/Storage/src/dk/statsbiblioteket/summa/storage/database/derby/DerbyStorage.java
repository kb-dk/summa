/* $Id: ControlDerby.java,v 1.7 2007/12/04 09:08:19 te Exp $
 * $Revision: 1.7 $
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
    protected PreparedStatement getStatement(StatementHandle handle)
                                                            throws SQLException{
        return pool.getStatement(
                       (MiniConnectionPoolManager.PooledStatementHandle)handle);
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
    protected void touchParents(String id, QueryOptions options)
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

        // Touch each parent and recurse upwards
        for (Record parent : parents) {
            touchRecord(parent.getId());
            touchParents(parent.getId(), options);
        }
    }
}



