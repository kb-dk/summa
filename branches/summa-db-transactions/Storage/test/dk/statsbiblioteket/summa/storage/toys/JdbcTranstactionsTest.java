package dk.statsbiblioteket.summa.storage.toys;

import dk.statsbiblioteket.summa.common.util.Environment;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.io.File;

/**
 * The point of this class is to play around with
 * Various test databases and see how and when unique key constraint
 * violations are raised
 */
public class JdbcTranstactionsTest {

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
}
