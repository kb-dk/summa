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
package dk.statsbiblioteket.summa.storage.toys;

import dk.statsbiblioteket.summa.common.util.Environment;
import junit.framework.*;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.io.File;

/**
 * The point of this class is to play around with
 * Various test databases and see how and when unique key constraint
 * violations are raised
 */
public class JdbcTranstactionsTest extends TestCase {

    public static final String DB = "${user.home}/tmp/h2test";

    public static void main (String[] args) throws Exception {
        Class.forName("org.h2.Driver");

        File dbDir = new File(Environment.escapeSystemProperties(DB));
        dbDir.mkdirs();

        System.out.println("Setting database up in " + dbDir);
        Connection conn = DriverManager.getConnection(
                                         "jdbc:h2:"+dbDir+File.separator+"db");

        createTables(conn);
        insertDummiesAutoCommit(conn);
        insertTransaction(conn);
    }

    private static void createTables (Connection conn) throws Exception {
        System.out.println("TL: " + conn.getTransactionIsolation());
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE records (id VARCHAR(255) PRIMARY KEY,\n" +
                                            "base VARCHAR(31))");
        stmt.close();
    }

    private static void insertDummiesAutoCommit (Connection conn)
                                                              throws Exception {
        Statement stmt = conn.createStatement();

        stmt.executeUpdate("INSERT INTO records (id,base) VALUES ('id1','base1')");
        stmt.executeUpdate("INSERT INTO records (id,base) VALUES ('id2','base1')");

        stmt.close();
    }

    private static void insertTransaction (Connection conn) throws Exception {
        Statement stmt = conn.createStatement();
        conn.setAutoCommit(false);

        // This SHOULD throw a unique key constraint violation immediately... (and it does in H2 at least)
        stmt.executeUpdate("INSERT INTO records (id,base) VALUES ('id1','base1')");
        stmt.executeUpdate("INSERT INTO records (id,base) VALUES ('id2','base1')");

        conn.commit();

        stmt.close();
        conn.setAutoCommit(true);
    }

    public void testDummy() {
        assertTrue(true);
    }

}

