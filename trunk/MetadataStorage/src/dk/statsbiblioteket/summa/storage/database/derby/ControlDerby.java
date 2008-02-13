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
import dk.statsbiblioteket.summa.storage.database.DatabaseControl;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ControlDerby extends DatabaseControl implements ControlDerbyMBean,
                                                             Configurable {
    private static Log log = LogFactory.getLog(ControlDerby.class);

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String driver = "org.apache.derby.jdbc.EmbeddedDriver";

    private String username;
    private String password;
    private String location;
    private boolean createNew = true;
    private boolean forceNew = false;

    private Connection connection;

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public ControlDerby(Configuration configuration) throws RemoteException {
        log.trace("Constructing ControlDerby");
        username = configuration.getString(PROP_USERNAME, "");
        password = configuration.getString(PROP_PASSWORD, "");
        boolean locationExists;
        try {
            locationExists = configuration.valueExists(PROP_LOCATION);
        } catch (IOException e) {
            throw new RemoteException("Exception requesting property "
                                      + PROP_LOCATION, e);
        }
        if (!locationExists) {
            throw new RemoteException("Could not get the property "
                                      + PROP_LOCATION
                                      + ". Aborting database setup");
        }
        location = configuration.getString(PROP_LOCATION);
        try {
            if (configuration.valueExists(PROP_CREATENEW)) {
                createNew = configuration.getBoolean(PROP_CREATENEW);
            }
        } catch (IOException e) {
            throw new RemoteException("Exception requesting property "
                                      + PROP_CREATENEW, e);
        }
        try {
            if (configuration.valueExists(PROP_FORCENEW)) {
                forceNew = configuration.getBoolean(PROP_FORCENEW);
            }
        } catch (IOException e) {
            //noinspection DuplicateStringLiteralInspection
            throw new RemoteException("Exception requesting property "
                                      + PROP_FORCENEW, e);
        }
        log.debug("ControlDerby extracted properties username: " + username
                 + ", password: "
                  + (password == null ? "[undefined]" : "[defined]")
                  + ", location: '" + location + "', createNew: " + createNew 
                  + ", forceNew: " + forceNew);
        init(configuration);
        log.trace("Construction completed");
    }

    // TODO: Consider is authentication should be used or not
    protected void connectToDatabase(Configuration configuration) throws
                                                               RemoteException {
        //noinspection DuplicateStringLiteralInspection
        log.info("Establishing connection to JavaDB with driver '"
                 + driver + "', username '" + username + "', password "
                 + (password == null || "".equals(password) ?
                    "[defined]" : "[undefined]")
                 + ", location '" + location + "', createNew " + createNew
                 + " and forceNew " + forceNew);

        if (new File(location).exists()) { /* Database location exists*/
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
            createTable();
        }
    }

    protected Connection getConnection() {
        return connection;
    }

    public void close() throws RemoteException {
        try {
            getConnection().close();
        } catch (SQLException e) {
            throw new RemoteException("SQLException when closing connection",
                                      e);
        }
    }
}
