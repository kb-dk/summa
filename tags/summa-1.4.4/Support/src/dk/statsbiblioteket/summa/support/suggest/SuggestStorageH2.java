/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.support.suggest;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.support.api.SuggestResponse;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.h2.jdbcx.JdbcDataSource;

import java.io.File;
import java.io.IOException;
import java.sql.*;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SuggestStorageH2 implements SuggestStorage {
    private static Log log = LogFactory.getLog(SuggestStorageH2.class);

    public static final String DB_FILE = "suggest_h2storage";
    private static final String SELECT_QUERY =
            "SELECT query FROM suggest WHERE query=?";

    private File location = null;
    private Connection connection;
    private boolean closed = true;

    @SuppressWarnings({"UnusedDeclaration"})
    public SuggestStorageH2(Configuration conf) {
        log.debug("Creating SuggestStorageH2");
    }

    public synchronized void open(File location) throws IOException {
        log.debug(String.format(
                "open(%s) called for SuggestStorageH2", location));
        this.location = location;
        boolean createNew;
        if (!location.exists()) {
            log.info(String.format(
                    "Creating new SuggestStorageH2 at location '%s'",
                    location));
            if (!location.mkdirs()) {
                throw new ConfigurationException(String.format(
                        "Unable to create folder '%s'", location));
            }
            createNew = true;
        } else {
            if (new File(location, DB_FILE + ".data.db").isFile()) {
                /* Database location exists*/
                log.debug(String.format(
                        "Reusing old database found at '%s'", location));
                createNew = false;
            } else {
                log.debug(String.format("No database at '%s'", location));
                createNew = true;
            }
        }

        String sourceURL = "jdbc:h2:" + location.getAbsolutePath()
                           + File.separator + DB_FILE;
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(sourceURL);
        try {
            connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new IOException(String.format(
                    "Unable to get a connection from '%s'", sourceURL), e);
        }
        if (createNew) {
            log.info(String.format("Creating new table for '%s'", location));
            try {
                createSchema();
            } catch (SQLException e) {
                throw new IOException("Unable to create schema", e);
            }
        }
        closed = false;
    }

    private void createSchema() throws SQLException {
        Statement s = connection.createStatement();
        s.execute("create table suggest("
                  + "query varchar(250), "
                  + "querylower varchar(250), "
                  + "query_count int, "
                  + "hit_count int)");
        s.execute("create index suggest_query ON suggest(querylower)");
        s.execute("create index suggest_query_count ON suggest(query_count)");
        s.execute("create index suggest_query_count_desc ON "
                  + "suggest(query_count desc)");

    }

    public synchronized void close() {
        log.debug(String.format("close() called for location '%s'" , location));
        if (closed) {
            return;
        }
        try {
            connection.close();
            closed = true;
        } catch (SQLException e) {
            log.error("Exception while closing data source", e);
        }
    }

    public synchronized SuggestResponse getSuggestion(
            String prefix, int maxResults) throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("getSuggestion(" + prefix + ", " + maxResults + ")");
        try {
            long startTime = System.nanoTime();
            PreparedStatement psExists = connection.prepareStatement(
                "SELECT query, query_count, hit_count FROM suggest WHERE "
                + "querylower LIKE '" + prefix.toLowerCase()
                + "%' ORDER BY query_count DESC");
            ResultSet rs = psExists.executeQuery();
            SuggestResponse response = new SuggestResponse(prefix, maxResults);
            int current = 0;
            try {
                while (rs.next() && current++ < maxResults) {
                    response.addSuggestion(
                            rs.getString(1), rs.getInt(3), rs.getInt(2));
                }
                //noinspection DuplicateStringLiteralInspection
                log.debug("getSuggestion(" + prefix + ", " + maxResults
                          + ") -> " + current + " suggestions in "
                          + (System.nanoTime() - startTime) / 1000000D + "ms");
                return response;
            } finally {
                rs.close();
            }
        } catch (SQLException e) {
            throw new IOException(
                    "Unable to get suggestion for '" + prefix + "', "
                    + maxResults);
        }

    }

    public void addSuggestion(String query, int hits) throws IOException {
        addSuggestion(query, hits, -1);
    }

    // -1 means no update of queryCount
    public synchronized void addSuggestion(String query, int hits,
                                           int queryCount) throws IOException {
        if (hits == 0) {
            log.trace("No hits for '" + query + "'. Deleting query...");
            try {
                deleteSuggestion(query);
            } catch (SQLException e) {
                throw new IOException("Unable to delete suggestion '"
                                      + query + "'");
            }
            return;
        }
        try {
            if (existsInDb(query)) {
                if (queryCount == -1) {
                    updateDb(query, hits, getQueryCount(query) + 1);
                } else {
                    updateDb(query, hits, queryCount);
                }
                return;
            }
            log.trace("New query '" + query + "' with hits " + hits);
            insertIntoDb(query, hits, 1);
        } catch (SQLException e) {
            throw new IOException(String.format(
                    "Unable to complete addSuggestion(%s, %d, %d)",
                    query, hits, queryCount), e);
        }
    }

    private void insertIntoDb(String query, int hits, int queryCount) throws
                                                                  SQLException {

        PreparedStatement psInsert = connection.prepareStatement(
                "INSERT INTO suggest VALUES (?, ?, ?, ?)");
        psInsert.setString(1, query);
        psInsert.setString(2, query.toLowerCase());
        psInsert.setInt(3, queryCount);
        psInsert.setLong(4, hits);
        psInsert.executeUpdate();
    }

    private int getQueryCount(String query) throws SQLException {
        int retval = 0;

        PreparedStatement psQuery = connection.prepareStatement(
                "SELECT query_count FROM suggest WHERE query=?");
        psQuery.setString(1, query);
        ResultSet rs = psQuery.executeQuery();
        try {
            while (rs.next()) {
                retval = rs.getInt(1);
            }
        } finally {
            rs.close();
        }

        log.trace("getQueryCount(" + query + ") -> " + retval);
        return retval;
    }

    private void deleteSuggestion(String query) throws SQLException {
        log.debug("Removing suggestion '" + query + "'");
        PreparedStatement psExists =
                connection.prepareStatement(SELECT_QUERY);
        psExists.setString(1, query);
        ResultSet rs = psExists.executeQuery();
        try {
            while (rs.next()) {
                rs.deleteRow();
            }
        } finally {
            rs.close();
        }
    }

    private boolean existsInDb(String query) throws SQLException {
        log.trace("existsInDb called with '" + query + "'");
        PreparedStatement psExists = connection.prepareStatement(SELECT_QUERY);
        psExists.setString(1, query);
        ResultSet rs = psExists.executeQuery();
        try {
            return rs.next();
        } finally {
            rs.close();
        }
    }

    private void updateDb(String query, int hits, int queryCount)
            throws SQLException {
        PreparedStatement psUpdate = connection.prepareStatement(
                "UPDATE suggest SET query_count=?, hit_count=? WHERE query=?");
        psUpdate.setInt(1, queryCount);
        psUpdate.setLong(2, hits);
        psUpdate.setString(3, query);
        psUpdate.executeUpdate();
    }
}
