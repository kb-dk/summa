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
import dk.statsbiblioteket.util.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.h2.jdbcx.JdbcDataSource;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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
    private static final String SELECT_LOWERCASE_QUERY =
            "SELECT query FROM suggest WHERE querylower=?";
    private static final String INSERT_STATEMENT =
            "INSERT INTO suggest VALUES (?, ?, ?, ?)";

    /**
     * The maximum number of suggestions to return from {@link #listSuggestions}
     * regardless of the stated maximum in the method-call.
     */
    public static final int MAX_SUGGESTIONS = 100000;

    private File location = null;
    private Connection connection;
    private boolean closed = true;
    private boolean lowercaseQueries =
            SuggestSearchNode.DEFAULT_LOWERCASE_QUERIES;
    private Locale lowercaseLocale;

    @SuppressWarnings({"UnusedDeclaration"})
    public SuggestStorageH2(Configuration conf) {
        log.debug("Creating SuggestStorageH2");
        lowercaseQueries = conf.getBoolean(
                SuggestSearchNode.CONF_LOWERCASE_QUERIES, lowercaseQueries);
        lowercaseLocale = new Locale(conf.getString(
                SuggestSearchNode.CONF_LOWERCASE_LOCALE,
                SuggestSearchNode.DEFAULT_LOWERCASE_LOCALE));
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
                    + "querylower LIKE '" + prefix.toLowerCase(lowercaseLocale)
                    + "%' ORDER BY query_count DESC");
            ResultSet rs = psExists.executeQuery();
            List<BuildSuggestTripel> suggestions =
                    new ArrayList<BuildSuggestTripel>(maxResults); 
            int current = 0;
            try {
                while (rs.next() && suggestions.size() < maxResults) {
                    updateResponse(suggestions, rs.getString(1), rs.getLong(3),
                                   rs.getInt(2));
                }
                //noinspection DuplicateStringLiteralInspection
                log.debug("getSuggestion(" + prefix + ", " + maxResults
                          + ") -> " + current + " suggestions in "
                          + (System.nanoTime() - startTime) / 1000000D + "ms");
                SuggestResponse response = new SuggestResponse(
                        prefix, suggestions.size());
                for (BuildSuggestTripel bst: suggestions) {
                    // TODO: Update SuggestResponse to long for hits
                    response.addSuggestion(bst.getQuery(), (int)bst.getHits(),
                                           bst.getQueryCount());
                }
                return response;
            } finally {
                rs.close();
            }
        } catch (SQLException e) {
            throw new IOException(
                    "Unable to get suggestion for '" + prefix + "', "
                    + maxResults, e);
        }

    }

    private void updateResponse(List<BuildSuggestTripel> suggestions,
                                String query, long hits, int queryCount) {
        if (lowercaseQueries) {
            query = query.toLowerCase(lowercaseLocale);
            for (BuildSuggestTripel bst: suggestions) {
                if (bst.getQuery().equals(query)) {
                    bst.setHits(Math.max(bst.getHits(), hits));
                    bst.setQueryCount(bst.getQueryCount() + queryCount);
                    return;
                }
            }
        }
        suggestions.add(new BuildSuggestTripel(query, (int)hits, queryCount));
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
                // TODO: Consider updating hits for queries matching lowercase
                if (queryCount == -1) {
                    updateDb(query, hits, getQueryCount(query) + 1);
                } else {
                    updateDb(query, hits, queryCount);
                }
                return;
            }
            log.trace("New query '" + query + "' with hits " + hits);
            insertIntoDb(query, hits, queryCount == -1 ? 1 : queryCount);
        } catch (SQLException e) {
            throw new IOException(String.format(
                    "Unable to complete addSuggestion(%s, %d, %d)",
                    query, hits, queryCount), e);
        }
    }

    private void insertIntoDb(String query, int hits, int queryCount) throws
                                                                      SQLException {

        PreparedStatement psInsert = connection.prepareStatement(
                INSERT_STATEMENT);
        psInsert.setString(1, query);
        psInsert.setString(2, query.toLowerCase(lowercaseLocale));
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

    // Deletes all suggestions, no matter the case
    private void deleteSuggestion(String query) throws SQLException {
        log.debug("Removing suggestion '" + query + "'");
        PreparedStatement psExists =
                connection.prepareStatement(SELECT_LOWERCASE_QUERY);
        psExists.setString(1, query.toLowerCase(lowercaseLocale));
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

    public ArrayList<String> listSuggestions(int start, int max) throws
                                                                 IOException {
        log.debug(String.format("listsuggestions(%d, %d) called", start, max));
        ResultSet rs = null;
        try {
            PreparedStatement psExists = connection.prepareStatement(
                    "SELECT query, query_count, hit_count FROM suggest");
            rs = psExists.executeQuery();

            max = Math.min(max, MAX_SUGGESTIONS);
            ArrayList<String> suggestions = new ArrayList<String>(max);

            if (!rs.next()) {
                return suggestions;
            }

            int current = 0;
            while (current < start && current++ < start + max) {
                if (!rs.next()) {
                    log.debug("Out of suggestions before start was reached");
                    return suggestions;
                }
            }

            while (current++ < start + max) {
                suggestions.add(rs.getString(1) + "\t" + rs.getInt(3) + "\t"
                                + rs.getLong(2));
                if (!rs.next()) {
                    log.debug("No more suggestions. Got " + suggestions.size());
                    return suggestions;
                }
            }

            log.debug(String.format("Got all %d suggestions, as requested",
                                    suggestions.size()));
            return suggestions;
        } catch (SQLException e) {
            throw new IOException(String.format(
                    "SQLException while dumping a maximum of %d suggestions, "
                    + "starting at %d", max, start), e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    log.warn(String.format("Exception while closing Resultset "
                                           + "in listSuggestions(%d, %d)",
                                           start, max));
                }
            }
        }
    }

    public void addSuggestions(ArrayList<String> suggestions) throws
                                                                   IOException {
        log.debug(String.format("addsuggestion called with %d suggestions",
                                suggestions.size()));

        PreparedStatement psInsert;
        try {
            psInsert = connection.prepareStatement(INSERT_STATEMENT);
        } catch (SQLException e) {
            throw new IOException(String.format(
                    "SQLException while preparing statement '%s'",
                    INSERT_STATEMENT), e);
        }

        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new IOException(
                    "SQLException while setting autoCommit to false", e);
        }

        try {
            for (String suggestion: suggestions) {
                if (suggestion == null || "".equals(suggestion)) {
                    continue;
                }
                String[] tokens = suggestion.split("\t");
                if (tokens.length > 3) { // Compensate for tabs in the query
                    tokens[0] = Strings.join(
                            Arrays.asList(tokens).subList(0, tokens.length-2),
                            "\t");
                    tokens[1] = tokens[tokens.length - 2];
                    tokens[2] = tokens[tokens.length - 1];
                }
                String query = tokens[0];
                long hits = 1;
                int queryCount = 1;
                try {
                    if (tokens.length > 1) {
                        hits = Long.valueOf(tokens[1]);
                    }
                } catch (NumberFormatException e) {
                    log.warn(String.format(
                            "NumberFormatException for hits with '%s'",
                            tokens[1]));
                }
                try {
                    if (tokens.length > 2) {
                        queryCount = Integer.valueOf(tokens[2]);
                    }
                } catch (NumberFormatException e) {
                    log.warn(String.format(
                            "NumberFormatException for querycount with '%s'",
                            tokens[2]));
                }
                try {
                    psInsert.setString(1, query);
                    psInsert.setString(2, query.toLowerCase(lowercaseLocale));
                    psInsert.setInt(3, queryCount);
                    psInsert.setLong(4, hits);
                    psInsert.executeUpdate();
                } catch (SQLException e) {
                    log.warn(String.format(
                            "SQLException while inserting query '%s'"
                            + " with %d hits and %d queryCount",
                            query, hits, queryCount), e);
                }
            }
            log.trace(String.format("Finished adding %d suggestions. "
                                    + "Committing", suggestions.size()));
            connection.commit();
        } catch (SQLException e) {
            throw new IOException("SQLException adding suggestions", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                log.error("SQLException setting autoCommit to true. Suggest "
                          + "might not be able to get updates", e);
            }
        }
    }

    private static class BuildSuggestTripel {
        private String query;
        private long hits;
        private int queryCount;

        public BuildSuggestTripel(String query, int hits, int queryCount) {
            this.query = query;
            this.hits = hits;
            this.queryCount = queryCount;
        }

        public String getQuery() {
            return query;
        }

        public long getHits() {
            return hits;
        }

        public void setHits(long hits) {
            this.hits = hits;
        }

        public int getQueryCount() {
            return queryCount;
        }

        public void setQueryCount(int queryCount) {
            this.queryCount = queryCount;
        }
    }

}
