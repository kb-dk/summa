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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.rmi.RemoteException;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;

import dk.statsbiblioteket.summa.storage.database.DatabaseControl;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ControlDerby extends DatabaseControl implements ControlDerbyMBean {
    private static Log log = LogFactory.getLog(ControlDerby.class);

    public static final String driver = "org.apache.derby.jdbc.EmbeddedDriver";

    private String username;
    private String password;
    private String location;
    private boolean createNew = false;

    private Connection connection;

    public ControlDerby(Configuration configuration) throws RemoteException {
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
        init(configuration);
    }

    protected void connectToDatabase(Configuration configuration) throws
                                                               RemoteException {
        log.info("Attempting to establish connection to JavaDB with driver '"
                 + driver + "', username '" + username + "', "
                 + (password == null || "".equals(password) ?
                    "no password" : "a password")
                 + ", location '" + location + "' and createNew " + createNew);

        if (!createNew && !new File(location).exists()) {
            log.info("No new database was requested, but no current database "
                     + "exists. Turning createNew on");
            createNew = true;
        }
        if (createNew && new File(location).exists()) {
            log.debug("New database requested while '" + location
                      + "' exists. Deleting '" + location + "'");
            try {
                Files.delete(location);
            } catch (FileNotFoundException e) {
                throw new RemoteException("Could not delete old database at '"
                                          + location + "'", e);
            }
        }
        if (!new File(location).exists()) {
            log.debug("Creating folder '" + location + "' for database");
            new File(location).mkdirs();
        }

        //noinspection NonConstantStringShouldBeStringBuffer
        String connectionURL = "jdbc:derby:" + location;
        if (createNew) {
            connectionURL += ";create=true";
        }
        String sans = connectionURL;
        if (username != null && !"".equals(username)) {
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
                                      + sans + "'" + (password == null
                                                      || "".equals(password)
                                                      ? ""
                                                      : " (password removed)"));
        }
        if (createNew) {
            createTable();
        }
    }
    
    protected Connection getConnection() {
        return null;
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
