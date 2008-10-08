/* $Id: TestBasicDerby.java,v 1.9 2007/12/04 09:08:19 te Exp $
 * $Revision: 1.9 $
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
/**
 * Created: te 2007-09-04 10:44:53
 * CVS:     $Id: TestBasicDerby.java,v 1.9 2007/12/04 09:08:19 te Exp $
 */
package dk.statsbiblioteket.summa.storage.database.derby;

import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.sql.*;
import java.util.Random;

/**
 * Some experimentations with Derby.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TestBasicDerby extends TestCase {
    public static final int BLOB_MAX_SIZE = 50*1024*1024;
    public static final int BLOB_SIZE = 10000;

    String driver = "org.apache.derby.jdbc.EmbeddedDriver";
    String dbRoot = System.getProperty("java.io.tmpdir") + "/DerbyDB";
//    String dbRoot = "Storage/test/DerbyDB";
    String dbName = "test14";
    Connection conn = null;
    /**
     * @deprecated in favor of DELETED and INDEXABLE
     */
    private static final String STATE_COLUMN = "state";
    /**
     * @deprecated in favor of MTIME and CTIME.
     */
    private static final String TIMESTAMP_COLUMN = "timestamp";

    public void setUp() throws Exception {
        super.setUp();
        connectAndInit();
        prepareStatements();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        if (conn != null) {
            conn.close();
        }
    }

    public void connectAndInit() throws Exception {
        String connectionURL = "jdbc:derby:" + dbRoot + "/" + dbName;
        if (!new File(dbRoot).exists()) {
            new File(dbRoot).mkdirs();
        }
        boolean createNewDatabase = !new File(dbRoot + "/" + dbName).exists();
        if (createNewDatabase) {
            // http://technology.amis.nl/blog/?p=2044
            connectionURL += ";create=true";
        }

        try{
            Class.forName(driver);
        } catch(java.lang.ClassNotFoundException e) {
            e.printStackTrace();
            fail("Could not connect to the Derby JDBC embedded driver");
        }

        try {
            System.out.println("Connecting to URL '" + connectionURL + "'");
            conn = DriverManager.getConnection(connectionURL);

            try {
                if (createNewDatabase) {
                    System.out.println("Creating database");
                    Statement createDatabase = conn.createStatement();
                    // http://java.sun.com/developer/technicalArticles/J2SE/Desktop/javadb/
                    createDatabase.execute(
              "CREATE table " + DatabaseStorage.RECORDS + " ("
              + " " + DatabaseStorage.ID_COLUMN    + " VARCHAR(255), "
              + " " + STATE_COLUMN + " VARCHAR(15), "
              + " " + DatabaseStorage.BASE_COLUMN  + " VARCHAR(15), "
              + " " + TIMESTAMP_COLUMN  + " TIMESTAMP,"
              + " " + DatabaseStorage.DATA_COLUMN
                                  + " BLOB(" + BLOB_MAX_SIZE + ") )");

                 }
            } catch (Exception e) {
                e.printStackTrace();
                fail("Could not create database summa_io with '"
                     + connectionURL + "'");
            }
        }  catch (Throwable e)  {
            e.printStackTrace();
            fail("Could not connect to '" + connectionURL + "'");
        }
    }

    PreparedStatement stmtSaveNewRecord;
    PreparedStatement getAll;

    public void prepareStatements() throws Exception {

            // http://java.sun.com/developer/technicalArticles/J2SE/Desktop/javadb/
            stmtSaveNewRecord = conn.prepareStatement(
        "INSERT INTO " + DatabaseStorage.RECORDS + " " +
        "   (" + DatabaseStorage.ID_COLUMN + ", "
               + STATE_COLUMN + ", "
               + DatabaseStorage.BASE_COLUMN + ", "
               + TIMESTAMP_COLUMN + ", "
               + DatabaseStorage.DATA_COLUMN + ") "
                + "VALUES (?, ?, ?, ?, ?)",
        Statement.RETURN_GENERATED_KEYS);

            String RecordsQuery = "SELECT " + DatabaseStorage.RECORDS + "." + DatabaseStorage
                    .ID_COLUMN + ","  +
                    DatabaseStorage.RECORDS + "." + STATE_COLUMN +
                    " FROM " + DatabaseStorage.RECORDS; // + " WHERE " + DatabaseStorage.RECORDS + "." + DatabaseStorage.BASE_COLUMN  + " =? ORDER BY " + ID_COLUMN;
            getAll = conn.prepareStatement(RecordsQuery);
    }

    Random random = new Random();
    public int addRandomRecord() throws Exception {
        return addRecord("Flammo " + System.currentTimeMillis());
    }
    public int addRecord(String id) throws Exception {
        byte[] blobbo = new byte[BLOB_SIZE];
        random.nextBytes(blobbo);
        InputStream blobIn = new ByteArrayInputStream(blobbo);
        stmtSaveNewRecord.clearParameters();
        stmtSaveNewRecord.setString(1, id);
        stmtSaveNewRecord.setString(2, "State" + random.nextInt(100));
        stmtSaveNewRecord.setString(3, "TestBase" + random.nextInt(100));
        stmtSaveNewRecord.setTimestamp(4,
                                 new Timestamp(System.currentTimeMillis()));
        stmtSaveNewRecord.setBlob(5, blobIn);
        return stmtSaveNewRecord.executeUpdate();
/*        System.out.println("Inserted rows: " + rowCount);
        ResultSet results = stmtSaveNewRecord.getGeneratedKeys();
        if (results.next()) {
            System.out.println("Result:  " + results.getString(1));
        }  */
    }

    public void dumpFirst(int max) throws Exception {
        getAll.execute();
        ResultSet results = getAll.getResultSet();
        while (results.next() && max-- > 0) {
            System.out.println("ID:  " + results.getString(1)
                               + ", State: " + results.getString(2));
        }
    }

    public void testInsert() throws Exception {
        assertEquals("The number of newly inserted records should be 1",
                     1, addRandomRecord());
    }

    public void dumpFirst10() throws Exception {
        dumpFirst(10);
    }

    public void dumpSpeed() throws Exception {
        dumpAddSpeed(2000000);
        dumpRetrievalSpeed();
    }

    public void dumpAddSpeed(int count) throws Exception {
        addRandomRecord();
        int feedback = Math.max(10, count / 100);
        Profiler profiler = new Profiler();
        profiler.setBpsSpan(5000);
        profiler.setExpectedTotal(count);
        for (int i = 0 ; i < count ; i++) {
            addRandomRecord();
            profiler.beat();
            if (i % feedback == 0) {
                System.out.println("Added " + (i+1) + "/" + count + " at "
                                   + profiler.getBps(true) + " adds/second. "
                                   + "ETA: " + profiler.getETAAsString(true));
            }
        }
        System.out.println("Added " + count + " records in "
                           + profiler.getSpendTime() + ". Average speed: "
                           + profiler.getBps() + " adds/second");
    }

    public void dumpRetrievalSpeed() throws Exception {
        getAll.execute();
        ResultSet results = getAll.getResultSet();
        Profiler profiler = new Profiler();
        int counter = 0;
        while (results.next()) {
            String dummy = "ID:  " + results.getString(1)
                         + ", State: " + results.getString(2);
            if ("kghfdsk".equals(dummy)) {
                // Just here to actually perform the request
            }
            profiler.beat();
            counter++;
        }
        System.out.println("Requested " + counter + " records in "
                           + profiler.getSpendTime() + ". Average speed: "
                           + profiler.getBps() + " requests/second");
    }
}



