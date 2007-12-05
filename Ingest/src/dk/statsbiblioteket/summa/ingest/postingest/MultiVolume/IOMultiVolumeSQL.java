/* $Id: IOMultiVolumeSQL.java,v 1.6 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.6 $
 * $Date: 2007/10/05 10:20:23 $
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
package dk.statsbiblioteket.summa.ingest.postingest.MultiVolume;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// Postgres dep is obsolete
//import org.postgresql.jdbc3.Jdbc3PoolingDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.io.IOException;
import java.util.Properties;
import java.util.ArrayList;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Provides a specialized persistent layer for the multi volume merger.
 *
 * This implementation depends on a Jdbc3PoolingDataSource.
 *
 * @author Hans Lund, State and University Library, Aarhus Denmark
 * @version $Id: IOMultiVolumeSQL.java,v 1.6 2007/10/05 10:20:23 te Exp $
 *
 *                   Table "public.flerbind"
 * Column  |          Type          |   Modifiers
 * ----------+------------------------+---------------
 *  id       | character varying(255) | not null
 *  data     | bytea                  |
 *  base     | character varying(15)  |
 *  type     | character varying(15)  |
 *  isfound  | boolean                | default false
 *  parent   | character varying(255) |
 *  position | integer                |
 * Indexes:
 *     "flerbind_pkey" PRIMARY KEY, btree (id)
 *
 * main => ((section|bind)*  =>)* bind+
 * @deprecated Multi volume is now part of the MetadataStorage.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class IOMultiVolumeSQL implements IOMultiVolume {


    public static final String DB_TABLE_NAME = "flerbind";
    public static final String DB_TYPE_COLUMN = "type";
    //public static final String DB_TIMESTAMP_COLUMN = "time";
    public static final String DB_PARENT_CLOUMN = "parent";
    public static final String DB_ID_COLUMN = "id";
    public static final String DB_BASE_COLUMN = "base";
    public static final String DB_DATA_COLUMN = "data";
    public static final String DB_POSITION_COLUMN = "position";
    public static final String DB_FOUND_COLUMN = "isfound";

    //Postgres dep obsolete
    //private static Jdbc3PoolingDataSource source;

    private static final String propertyName = "flerbind.properties.xml";

    private static final Log log = LogFactory.getLog(IOMultiVolumeSQL.class);

    public static final String CreateNewQuery = "INSERT INTO " + DB_TABLE_NAME + " " +
            "(" + DB_ID_COLUMN + "," + DB_TYPE_COLUMN + "," + DB_BASE_COLUMN +") VALUES (?,?,?)";

    public static final String UpdateRecordQuery = "UPDATE " + DB_TABLE_NAME +
            " SET " + DB_TYPE_COLUMN + "=?," + DB_DATA_COLUMN + "=?," + DB_FOUND_COLUMN + "=?" +
            " WHERE " + DB_ID_COLUMN + "=?";

    public static final String AddChildQuery = "UPDATE " + DB_TABLE_NAME +
            " SET " + DB_PARENT_CLOUMN + "=? WHERE " + DB_ID_COLUMN + "=?";

    public static final String AddChildWithPosition = "UPDATE" + DB_TABLE_NAME +
            " SET " + DB_PARENT_CLOUMN + "=?," + DB_POSITION_COLUMN + "=? WHERE " + DB_ID_COLUMN + "=?";

    public static final String DeleteRecordQuery = "DELETE FROM " + DB_TABLE_NAME + " WHERE " + DB_ID_COLUMN + "=?";

    public static final String ALLMainRecords = "SELECT " + DB_ID_COLUMN + "," + DB_DATA_COLUMN + "," + DB_TYPE_COLUMN + " FROM " +
            DB_TABLE_NAME + " WHERE " + DB_TYPE_COLUMN + "='MAIN' AND " + DB_BASE_COLUMN +"=?";

    public static final String ALLChildRecords = "SELECT " + DB_ID_COLUMN + "," + DB_DATA_COLUMN + "," + DB_TYPE_COLUMN + " FROM " +
            DB_TABLE_NAME + " WHERE " + DB_PARENT_CLOUMN + "=? ORDER BY " + DB_POSITION_COLUMN;

    private static IOMultiVolumeSQL _instance;

    private IOMultiVolumeSQL() {

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Properties prop = new Properties();

        try {
            prop.loadFromXML(loader.getResourceAsStream(propertyName));
        } catch (IOException e) {
            log.error("Unable to find properties\nOr unable to parse properties file:\n" + e);
        }

        String datasourcename = prop.getProperty("datasourcename", "datasourcename");
        String database_server = prop.getProperty("database_server", "");
        String database_name = prop.getProperty("database_name", "");
        String database_user = prop.getProperty("database_user", "");
        String database_password = prop.getProperty("database_password", "");
        int database_pool_size = Integer.parseInt(prop.getProperty("database_pool_size", "10"));


        log.info("CreateNewQuery " + CreateNewQuery);
        log.info("UpdateRecordQuery " + UpdateRecordQuery);
        log.info("AddChildQuery " + AddChildQuery);
        log.info("AddChildWithPosition " + AddChildWithPosition);
        log.info("DeleteRecordQuery " + DeleteRecordQuery);
        log.info("ALLMainRecords " + ALLMainRecords);
        log.info("ALLChildRecords " + ALLChildRecords);

        //Postgres dep obsolete
        /*source = new Jdbc3PoolingDataSource();
        log.info("datasource name: " + datasourcename);
        source.setDataSourceName(datasourcename);

        log.info("database_server:" + database_server);
        source.setServerName(database_server);

        log.info("database_name: " +  database_name);
        source.setDatabaseName(database_name);

        log.info("database_user: "+  database_user);
        source.setUser(database_user);

        log.info("database_passwd: " + database_password);
        source.setPassword(database_password);

        log.info("database_pool_size: " + database_pool_size);
        source.setMaxConnections(database_pool_size);*/
        throw new UnsupportedOperationException("The Summa Ingester is not implemented yet!");

    }

    /**
     * Get a connection from the data base connection manager.
     *
     * @return connection
     */
    private Connection getConnection() {
        /**try {
            log.info("getting connection");
            Connection c = source.getConnection();
            log.info("got connection");
            return c;
            return null;
        } catch (SQLException e) {
            log.error(e);
        }

        return null;*/
        throw new UnsupportedOperationException("The Summa Ingester is not implemented yet!");
    }

    /**
     * Release the given connection to the data base connection manager.
     *
     * @param conn
     */
    private void releaseConnection(Connection conn) {

        try {
            conn.close();
        } catch (SQLException e) {
            log.error(e);
        }
    }


    public synchronized static IOMultiVolumeSQL getInstance() {
        if (_instance == null) {
            _instance = new IOMultiVolumeSQL();
        }
        return _instance;
    }

    /**
     * Creates a new merge candidate record.
     * @param id  record id for the candidate
     * @param type  the type
     * @param base
     */
    private void create(String id, MultiVolumeRecord.RecordType type, String base) {
        log.info("create record: " + id + " type: " + type);
        getInstance();
        Connection conn = _instance.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(CreateNewQuery);
            stmt.setString(1, id);
            stmt.setString(2, type.toString());
            stmt.setString(3,base);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            log.error(e);
        }
        _instance.releaseConnection(conn);
        log.info("done create record");
    }


    private void update(String id, MultiVolumeRecord.RecordType type,
                        byte[] content, boolean isDeleted) {
        log.info("update record:" + id + " type:" + type + " isDeleted:"
                 + isDeleted);
        getInstance();
        Connection conn = _instance.getConnection();
        if (isDeleted) {
            try {
                PreparedStatement stmt = conn.prepareStatement(DeleteRecordQuery);
                stmt.setString(1, id);
                stmt.executeUpdate();
                stmt.close();
            } catch (SQLException e) {
                log.error(e);
            }
        } else {
            try {
                PreparedStatement stmt = conn.prepareStatement(UpdateRecordQuery);
                stmt.setString(1, type.toString());
                stmt.setBytes(2, content);
                stmt.setBoolean(3, true);
                stmt.setString(4, id);
                stmt.executeUpdate();
                stmt.close();
            } catch (SQLException e) {
                log.error(e);
            }
        }
        _instance.releaseConnection(conn);
        log.info("done update record");
    }

    private void addChild(String parentID, String childID) {
        log.info("addChild parent:" + parentID + " child:" + childID);
        getInstance();
        Connection conn = _instance.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(AddChildQuery);
            stmt.setString(1, parentID);
            stmt.setString(2, childID);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            log.error(e);
        }
        _instance.releaseConnection(conn);
        log.info("done addChild parent");
    }

    private void addChild(String parentID, String childID, int position) {
        log.info("addChild parent:" + parentID + " child:" + childID + " at position:" + position);
        getInstance();
        Connection conn = _instance.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(AddChildWithPosition);
            stmt.setString(1, parentID);
            stmt.setInt(2, position);
            stmt.setString(3, childID);
            stmt.close();
        } catch (SQLException e) {
            log.error(e);
        }
        _instance.releaseConnection(conn);
        log.info("done addChild parent");
    }



    public MultiVolumeRecord.Record[] getAllMainRecords(String base) {
        getInstance();
        Connection conn = _instance.getConnection();
        try {
            ArrayList<MultiVolumeRecord.Record> res = new ArrayList<MultiVolumeRecord.Record>();
            PreparedStatement stmt = conn.prepareStatement(ALLMainRecords);
            stmt.setString(1,base);
            ResultSet set = stmt.executeQuery();
            while (set.next()) {
                res.add(new MultiVolumeRecord.Record(set.getString(DB_ID_COLUMN), MultiVolumeRecord.RecordType.MAIN, set.getBytes(DB_DATA_COLUMN)));
            }
            stmt.close();
            _instance.releaseConnection(conn);
            return res.toArray(new MultiVolumeRecord.Record[1]);
        } catch (SQLException e) {
            log.error(e);
            _instance.releaseConnection(conn);
            return null;
        }

    }




    public void addChild(String parentID, String childID, MultiVolumeRecord.RecordType child, String base){
        create(childID,child, base);
        addChild(parentID,childID);
    }

    public void addChild(String parentID, String childID,
                         MultiVolumeRecord.RecordType child, int childPosition,
                         String base){
        create(childID, child, base);
        addChild(parentID,childID,childPosition);
    }

     public void updateRecord(String id, MultiVolumeRecord.RecordType type,
                              byte[] data, boolean isDeleted){
        update(id,type,data, isDeleted);
    }

    public void addRecord(String id, MultiVolumeRecord.RecordType type, String base){
        create(id,type, base);

    }

    public MultiVolumeRecord.Record[] getChilds(String id) {
        getInstance();
        Connection conn = _instance.getConnection();
        try {
            ArrayList<MultiVolumeRecord.Record> res = new ArrayList<MultiVolumeRecord.Record>();
            PreparedStatement stmt = conn.prepareStatement(ALLChildRecords);
            stmt.setString(1, id);
            log.info("stmt:" + stmt.toString());
            ResultSet set = stmt.executeQuery();

            while (set.next()) {
                if ("BIND".equals(set.getString(DB_TYPE_COLUMN))) {
                    res.add(new MultiVolumeRecord.Record(set.getString(DB_ID_COLUMN), MultiVolumeRecord.RecordType.BIND, set.getBytes(DB_DATA_COLUMN)));
                } else if ("SECTION".equals(set.getString(DB_TYPE_COLUMN))) {
                    res.add(new MultiVolumeRecord.Record(set.getString(DB_ID_COLUMN), MultiVolumeRecord.RecordType.SECTION, set.getBytes(DB_DATA_COLUMN)));
                }
            }
            stmt.close();
            _instance.releaseConnection(conn);
            return res.toArray(new MultiVolumeRecord.Record[0]);
        } catch (SQLException e) {
            log.error(e);
            _instance.releaseConnection(conn);
            return null;
        }
    }
}
