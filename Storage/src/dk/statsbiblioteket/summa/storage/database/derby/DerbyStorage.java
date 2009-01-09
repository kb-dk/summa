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
import java.sql.DriverManager;
import java.sql.SQLException;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.StorageUtils;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DerbyStorage extends DatabaseStorage implements Configurable {
    private static Log log = LogFactory.getLog(DerbyStorage.class);

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String driver = "org.apache.derby.jdbc.EmbeddedDriver";

    private static final int BLOB_MAX_SIZE = 50*1024*1024; // MAX_VALUE instead?
    public static final int META_LIMIT =     50*1024*1024; // MAX_VALUE instead?

    private String username;
    private String password;
    private File location;
    private boolean createNew = true;
    private boolean forceNew = false;

    private Connection connection;

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public DerbyStorage(Configuration conf) throws IOException {
        super(conf);
        log.trace("Constructing ControlDerby");
        username = conf.getString(CONF_USERNAME, "");
        password = conf.getString(CONF_PASSWORD, "");

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
        if (conf.valueExists(CONF_CREATENEW)) {
                createNew = conf.getBoolean(CONF_CREATENEW);
        }

        if (conf.valueExists(CONF_FORCENEW)) {
                forceNew = conf.getBoolean(CONF_FORCENEW);
        }

        log.debug("ControlDerby extracted properties username: " + username
                 + ", password: "
                  + (password == null ? "[undefined]" : "[defined]")
                  + ", location: '" + location + "', createNew: " + createNew
                  + ", forceNew: " + forceNew);
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
                throw new RemoteException("No database exists at '" + location
                                          + " and createNew is false. The "
                                          + "metadata storage cannot run "
                                          + "without a backend. Exiting");
            }
        }

        //noinspection NonConstantStringShouldBeStringBuffer,DuplicateStringLiteralInspection
        String connectionURL = "jdbc:derby:" + location;
        if (createNew) {
            //noinspection DuplicateStringLiteralInspection
            connectionURL += ";create=true";
        }
        String sans = connectionURL;
        if (username != null && !"".equals(username)) {
//            System.setProperty("derby.connection.requireAuthentication",
//                               "true");
            log.warn("Authentication is not currently supported");
            connectionURL += ";user=" + username;
            sans = connectionURL;
            connectionURL += password == null ? "" : ";password=" + password;
        }
        log.debug("Connection-URL (sans password): '" + sans + "'");

        try{
            Class.forName(driver);
        } catch(java.lang.ClassNotFoundException e) {
            throw new RemoteException("Could not connect to the Derby JDBC "
                                      + "embedded driver '" + driver + "'", e);
        }

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

        log.info("Connected to database at '" + location + "'");
        if (createNew) {
            log.info("Creating new table for '" + location + "'");
            createSchema();
        }
    }

    protected Connection getConnection() {
        return connection;
    }

    protected String getMetaColumnDataDeclaration() {
        return " BLOB(" + META_LIMIT + ")";
    }

    protected String getDataColumnDataDeclaration() {
        return " BLOB(" + BLOB_MAX_SIZE + "), ";
    }
}



