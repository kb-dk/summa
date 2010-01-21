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
import dk.statsbiblioteket.summa.common.util.UniqueTimestampGenerator;
import dk.statsbiblioteket.summa.common.lucene.analysis.SummaKeywordAnalyzer;
import dk.statsbiblioteket.summa.support.api.SuggestResponse;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.reader.CharSequenceReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.h2.jdbcx.JdbcDataSource;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.net.URL;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SuggestStorageH2 extends SuggestStorageImpl {
    private static Log log = LogFactory.getLog(SuggestStorageH2.class);

    public static final String DB_FILE = "suggest_h2storage";

    public static final int MAX_QUERY_LENGTH = 250;

    /**
     * The maximum number of suggestions to return from {@link #listSuggestions}
     * and {@link #getSuggestion(String, int)}  regardless of the stated
     * maximum in the method-call.
     */
    public static final int MAX_SUGGESTIONS = 1000;

    private File location = null;
    private Connection connection;
    private boolean closed = true;
    private Analyzer normalizer, sanitizer;
    private boolean normalizeQueries;
    private int updateCount = 0;
    private boolean useL2cache;
    private UniqueTimestampGenerator timestamps;
    public static final int ANALYZE_INTERVAL = 50000;

    /**
     * Whether or not to enable the H2 level 2 page cache. This will be a
     * performance gain on most databases, but might decrease performance
     * on small databases. The default value is {@code false}.
     * <p/>
     * <i>Warning:</i>Memory leaks has been observed when running with the
     * level 2 cache enabled.
     */
    public static final String CONF_L2CACHE = "summa.support.suggest.l2cache";
    public static final boolean DEFAULT_L2CACHE = false;

    /**
     * The {@link Analyzer} implementation to use for generating normalized
     * query collation keys. When suggestion lookup is done the query-string
     * is normalized and compared against the normalized suggestions in the
     * database. The normalizer only affects how suggestions are looked up
     * in the database, to affect how they are presented to the user see
     * {@link #CONF_SANITIZER}.
     * <p/>
     * Default is
     * {@link dk.statsbiblioteket.summa.common.lucene.analysis.SummaKeywordAnalyzer}.
     * <p/>
     * The specified class <i>must</i> have a no-arguments constructor.
     */
    public static final String CONF_NORMALIZER = "summa.support.suggest.normalizer";
    public static final Class<? extends Analyzer> DEFAULT_NORMALIZER =
                                                     SummaKeywordAnalyzer.class;

    /**
     * The {@link Analyzer} implementation to use for for sanitizing queries
     * before storing them in the database. The typical use case for sanitizing
     * suggestions is to not differentiate between queries with leading- or
     * trailing whitespace at the user level. The sanitizer only affects how
     * suggestions are presented to the user, not how they are matched in the
     * database. To affect the actual matching see {@link #CONF_NORMALIZER}.
     * <p/>
     * Default is
     * {@link WhitespaceAnalyzer}.
     * <p/>
     * The specified class <i>must</i> have a no-arguments constructor.
     */
    public static final String CONF_SANITIZER = "summa.support.suggest.sanitizer";
    public static final Class<? extends Analyzer> DEFAULT_SANITIZER =
                                                     WhitespaceAnalyzer.class;

    private ThreadLocal<StringBuilder> threadLocalBuilder =
                                              new ThreadLocal<StringBuilder>() {
        @Override
        protected StringBuilder initialValue() {
            return new StringBuilder();
        }

        @Override
        public StringBuilder get() {
            StringBuilder b = super.get();
            b.setLength(0); // clear/reset the buffer
            return b;
        }

    };

    @SuppressWarnings({"UnusedDeclaration"})
    public SuggestStorageH2(Configuration conf) {
        log.debug("Creating SuggestStorageH2");

        normalizeQueries = conf.getBoolean(
                SuggestSearchNode.CONF_NORMALIZE_QUERIES,
                SuggestSearchNode.DEFAULT_NORMALIZE_QUERIES);

        /* Initialize query normalizer */
        Class<? extends Analyzer> analyzerClass = Configuration.getClass(
                CONF_NORMALIZER,Analyzer.class, DEFAULT_NORMALIZER, conf);
        try {
            normalizer = analyzerClass.newInstance();
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Unable to instantiate query normalizer", e);
        }

        /* Initialize query sanitizer */
        analyzerClass = Configuration.getClass(
                CONF_SANITIZER, Analyzer.class, DEFAULT_SANITIZER, conf);
        try {
            sanitizer = analyzerClass.newInstance();
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Unable to instantiate query sanitizer", e);
        }

        useL2cache = conf.getBoolean(CONF_L2CACHE, DEFAULT_L2CACHE);
        timestamps = new UniqueTimestampGenerator();
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
            if (new File(location, DB_FILE + ".h2.db").isFile() ||
                new File(location, DB_FILE + ".data.db").isFile()) {
                /* Database location exists*/
                log.debug(String.format(
                        "Reusing old database found at '%s'", location));
                createNew = false;
            } else {
                log.debug(String.format("No database at '%s'", location));
                createNew = true;
            }
        }

        connection = resetConnection();
        if (createNew) {
            log.info(String.format("Creating new table for '%s'", location));
            try {
                createSchema();
            } catch (SQLException e) {
                throw new IOException("Unable to create schema", e);
            }
        } else {
            log.debug("Calling normalize, just to make sure");
            try {
                optimizeTables();
            } catch (Exception e) {
                log.warn("Exception while optimizing tables upon startup", e);
            }
            try {
                createIndexes();
            } catch (SQLException e) {
                throw new IOException("SQLException while creating indexes "
                                      + "during startup", e);
            }
        }

        closed = false;
    }

    private Connection resetConnection() throws IOException {
        if (connection != null) {
            log.warn("Resetting database connection");
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("Failed to close database connection: "
                          + e.getMessage(), e);
            }
        }

        String l2cache = "";
        if (useL2cache) {
            log.debug("Enabling H2 L2 cache");
            l2cache = ";CACHE_TYPE=SOFT_LRU";
        }

        String sourceURL = "jdbc:h2:" + location.getAbsolutePath()
                           + File.separator + DB_FILE + l2cache;

        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(sourceURL);
        try {
            connection = dataSource.getConnection();
            connection.setTransactionIsolation(
                    Connection.TRANSACTION_READ_UNCOMMITTED);
        } catch (SQLException e) {
            throw new IOException(String.format(
                    "Unable to get a connection from '%s'", sourceURL), e);
        }

        setMaxMemoryRows(MAX_SUGGESTIONS*3);

        return connection;
    }

    private void createSchema() throws SQLException {
        Statement s = connection.createStatement();
        s.execute(String.format("CREATE TABLE IF NOT EXISTS suggest_index("
                               + "query_id INTEGER AUTO_INCREMENT PRIMARY KEY, "
                               + "normalized_query VARCHAR(%1$d) NOT NULL)",
                                MAX_QUERY_LENGTH));

        s.execute(String.format("CREATE TABLE IF NOT EXISTS suggest_data("
                                      + "query_id INTEGER NOT NULL, "
                                      + "user_query VARCHAR(%1$d) UNIQUE, "
                                      + "query_count INTEGER, "
                                      + "hit_count INTEGER, "
                                      + "mtime BIGINT, "
                                      + "PRIMARY KEY (query_id,user_query))",
                                MAX_QUERY_LENGTH));
        s.close();
        createIndexes();
    }

    private void createIndexes() throws SQLException {
        long startTime = System.currentTimeMillis();
        log.info("Preparing table indices");
        Statement s = connection.createStatement();

        //
        s.execute("CREATE INDEX IF NOT EXISTS suggest_normalized_query "
                + "ON suggest_index(normalized_query)");

        // This index is used for prefix searches sorted on query_count,
        // as well as fast lookups of query_id given a user_query
        s.execute("CREATE INDEX IF NOT EXISTS suggest_query_count "
                + "ON suggest_data(user_query, query_count desc)");

        // This index is used for most recent queries sorted by query_count
        s.execute("CREATE UNIQUE INDEX IF NOT EXISTS suggest_mtime "
                + "ON suggest_data(mtime desc, query_count desc)");

        s.close();
        log.info("Table indices prepared in "
                 + (System.currentTimeMillis() - startTime) + "ms");
    }

    private void dropIndexes() throws SQLException {
        Statement s = connection.createStatement();
        s.execute("DROP INDEX suggest_normalized_query");
        s.execute("DROP INDEX suggest_query_count");
        s.execute("DROP INDEX suggest_mtime");
        s.close();
    }

    public synchronized void close() {
        log.info(String.format("Closing '%s'" , location));
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
        if (log.isTraceEnabled()) {
            log.trace("getSuggestion(" + prefix + ", " + maxResults + ")");
        }

        try {
            long startTime = System.nanoTime();
            maxResults = Math.min(maxResults, MAX_SUGGESTIONS);

            connection.setReadOnly(true);
            connection.setAutoCommit(false);

            PreparedStatement psExists = connection.prepareStatement(
                 // FIXME: Sorting on query_count is slow
                 "SELECT user_query AS query, query_count, hit_count " +
                 "FROM suggest_data " +
                 "INNER JOIN suggest_index " +
                 "ON suggest_index.query_id = suggest_data.query_id " +
                 "WHERE suggest_index.normalized_query LIKE ? " +
                 "ORDER BY query_count DESC " +
                 "LIMIT ?");
            psExists.setString(1, normalize(prefix) + "%");
            psExists.setInt(2, maxResults);
            psExists.setFetchDirection(ResultSet.FETCH_FORWARD);
            psExists.setFetchSize(maxResults);

            ResultSet rs = psExists.executeQuery();
            int count = 0;
            SuggestResponse response = new SuggestResponse(prefix, maxResults);
            try {
                while (rs.next() && count < maxResults) {
                    count++;
                    response.addSuggestion(
                            rs.getString(1), rs.getInt(3), rs.getInt(2));
                }

                log.debug("getSuggestion(" + prefix + ", " + maxResults
                          + ") -> " + count + " suggestions in "
                          + (System.nanoTime() - startTime) / 1000000D + "ms");

                return response;
            } finally {
                rs.close();
                connection.setReadOnly(false);
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IOException(
                    "Unable to get suggestion for '" + prefix + "', "
                    + maxResults, e);
        }
    }

    @Override
    public synchronized SuggestResponse getRecentSuggestions(
            int ageSeconds, int maxResults) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("getRecentSuggestions("
                      + ageSeconds + "s, " + maxResults + ")");
        }

        try {
            long startTime = System.nanoTime();
            long mtime = System.currentTimeMillis() - ageSeconds*1000;
            mtime = timestamps.baseTimestamp(mtime);
            maxResults = Math.min(maxResults, MAX_SUGGESTIONS);

            connection.setReadOnly(true);
            connection.setAutoCommit(false);

            PreparedStatement psExists = connection.prepareStatement(
                 // FIXME: ORDER BY query_count is slow
                 "SELECT user_query AS query, query_count, hit_count " +
                 "FROM suggest_data " +
                 "INNER JOIN suggest_index " +
                 "ON suggest_index.query_id = suggest_data.query_id " +
                 "WHERE suggest_data.mtime > ? " +
                 "ORDER BY mtime DESC " +
                 "LIMIT ?");
            psExists.setLong(1, mtime);
            psExists.setInt(2, maxResults);
            psExists.setFetchDirection(ResultSet.FETCH_FORWARD);
            psExists.setFetchSize(maxResults);

            ResultSet rs = psExists.executeQuery();
            int count = 0;
            SuggestResponse response = new SuggestResponse("", maxResults);
            try {
                while (rs.next() && count < maxResults) {
                    count++;
                    response.addSuggestion(
                            rs.getString(1), rs.getInt(3), rs.getInt(2));
                }

                log.debug(String.format(
                   "getRecentSuggestions(%ss, + %s) -> %s  suggestions in %sms",
                   ageSeconds, maxResults, count,
                   (System.nanoTime() - startTime) / 1000000D));

                return response;
            } finally {
                rs.close();
                connection.setReadOnly(false);
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IOException(
                    "Unable to get " + maxResults + " recent suggestions for"
                    + " the last " + ageSeconds + "s", e);
        }
    }

    public void addSuggestion(String query, int hits) throws IOException {
        addSuggestion(query, hits, -1);
    }

    // -1 means add 1 to suggest_index.query_count
    public synchronized void addSuggestion(
                    String query, int hits, int queryCount) throws IOException {

        // Doing checkString() here makes sure we don't allocate huge amounts
        // of memory in our thread local StringBuilders used in join()
        if (!checkString(query)) return;
        query = sanitize(query);

        if (hits == 0) {
            log.trace("No hits for '" + query + "'. Deleting query...");
            try {
                deleteSuggestion(query);
            } catch (SQLException e) {
                log.warn(String.format(
                        "Unable to delete suggestion '%s': %s",
                        query, e.getMessage()), e);
            }
            return;
        }

        try {
            insertSuggestion(query, hits, queryCount == -1 ? 1 : queryCount);
            log.debug("Created new suggestion '"
                      + query + "' with " + hits + " hits");
        } catch (SQLException e) {
            if (isIntegrityConstraintViolation(e)) {
                updateSuggestion(query, hits, queryCount);
                log.debug("Updated suggestion '"
                      + query + "' with " + hits + " hits");
            } else {
                throw new IOException(String.format(
                        "Unable to complete addSuggestion(%s, %d, %d)",
                        query, hits, queryCount), e);
            }
        }
    }

    private void insertSuggestion(String query, int hits, int queryCount)
            throws SQLException {

        String normalizedQuery = normalize(query);

        if (!checkString(normalizedQuery)) return;

        if (normalizeQueries) {
            query = normalizedQuery;
        }

        // We need to control the transaction in order to roll back the
        // suggest_index insertion if 'query' is already in
        // suggest_data.user_query
        connection.setAutoCommit(false);

        PreparedStatement psInsert;
        try {
            // Can not raise an integrity constriant violation because PK
            // is AUTO_INCREMENT
            psInsert = connection.prepareStatement(
                    "INSERT INTO suggest_index (normalized_query)" +
                    "VALUES (?)");
            psInsert.setString(1, normalizedQuery);
            psInsert.executeUpdate();
            psInsert.close();

            // Raises integrity constraint violation if 'query' is already
            // listed in suggest_data.user_query
            psInsert = connection.prepareStatement(
                    "INSERT INTO suggest_data " +
                    "VALUES (identity(), ?, ?, ?, ?)");
            psInsert.setString(1, query);
            psInsert.setInt(2, queryCount);
            psInsert.setInt(3, hits);
            psInsert.setLong(4, timestamps.next());
            psInsert.executeUpdate();
            psInsert.close();

        } catch (SQLException e) {
            // Undo addition to suggest_index
            connection.rollback();

            // Integrity constraint violations are OK,
            // we must do an update instead - see comments above
            if (isIntegrityConstraintViolation(e)) {
                throw e; // Ok to ditch stack trace
            } else {
                throw new SQLException(
                        "Unexpected error inserting normalized query '"
                        + normalizedQuery + "' in index", e);
            }
        } finally {
            connection.setAutoCommit(true);
        }

        updateCount++;
        analyzeIfNeeded();
    }

    private boolean checkString (String normalizedQuery) {
        if (normalizedQuery.length() > MAX_QUERY_LENGTH) {
            log.info("addSuggestion: The analyzed query must be "
                     + MAX_QUERY_LENGTH + " chars or less. Got "
                     + normalizedQuery.length()
                      + " chars from '" + normalizedQuery + "'");
            return false;
        }
        return true;
    }

    // Deletes all suggestions, no matter the case
    private void deleteSuggestion(String query) throws SQLException {
        log.debug("Removing suggestion '" + query + "'");

        String normalizedQuery = normalize(query);
        int queryId = getQueryId(normalizedQuery);

        // Delete from suggest_index table
        PreparedStatement psUpdate = connection.prepareStatement(
                "DELETE FROM suggest_index " +
                "WHERE normalized_query=?"
        );
        psUpdate.setString(1, normalizedQuery);
        psUpdate.executeUpdate();

        // Delete from suggest_data table
        if (queryId != -1) {
            psUpdate = connection.prepareStatement(
                    "DELETE FROM suggest_data " +
                    "WHERE query_id=?"
            );
            psUpdate.setInt(1, queryId);
            psUpdate.executeUpdate();
        } else {
            log.warn("No suggest data for normalized query '"
                     + normalizedQuery + "'");
        }

        updateCount++;
        analyzeIfNeeded();
    }

    private int getQueryId (String normalizedQuery) throws SQLException {
        PreparedStatement psGetId = connection.prepareStatement(
                "SELECT query_id FROM suggest_index WHERE normalized_query=?"
        );
        psGetId.setString(1, normalizedQuery);
        ResultSet results = psGetId.executeQuery();
        try {
            if (results.next()) {
                return results.getInt(1);
            } else {
                return -1;
            }
        } finally {
            results.close();
        }
    }

    /**
     * If queryCount is -1 we must add one to the previous value, otherwise
     * we should set suggest_index.query_count to queryCount
     * @param query
     * @param hits
     * @param queryCount
     */
    private void updateSuggestion(String query, int hits, int queryCount) {
        try {
            String normalizedQuery = normalize(query);

            if (!checkString(normalizedQuery)) return;

            if (normalizeQueries) {
                query = normalizedQuery;
            }

            PreparedStatement psUpdate;
            if (queryCount == -1){
                queryCount = 1;
                psUpdate = connection.prepareStatement(
                        "UPDATE suggest_data " +
                        "SET query_count=query_count+?, " +
                        "    hit_count=?, " +
                        "    mtime=? " +
                        "WHERE user_query=?");
            } else {
                psUpdate = connection.prepareStatement(
                        "UPDATE suggest_data " +
                        "SET query_count=?, " +
                        "    hit_count=?, " +
                        "    mtime=? " +
                        "WHERE user_query=?");
            }

            psUpdate.setInt(1, queryCount);
            psUpdate.setLong(2, hits);
            psUpdate.setLong(3, timestamps.next());
            psUpdate.setString(4, query);
            psUpdate.executeUpdate();

            try {
                psUpdate = connection.prepareStatement(
                        "INSERT INTO suggest_index (normalized_query)" +
                        "VALUES (?)");
                psUpdate.setString(1, normalizedQuery);
                psUpdate.executeUpdate();
            } catch (SQLException e) {
                if (!isIntegrityConstraintViolation(e)) {
                    log.error(String.format(
                            "Failed to update suggestion map with '%s': %s",
                            query, e.getMessage()), e);
                }
            }
        } catch (SQLException e) {
            log.error(String.format(
                    "Failed to update database with query %s, "
                    + "hits=%s, queryCount=%s: %s",
                    query, hits, queryCount, e.getMessage()), e);
        }

        updateCount++;
        analyzeIfNeeded();
    }

    public ArrayList<String> listSuggestions(int start, int max) throws
                                                                 IOException {
        log.debug(String.format(
                "Listing suggestions from %d to %d", start, max));

        long startTime = System.currentTimeMillis();
        ResultSet rs = null;
        max = Math.min(max, MAX_SUGGESTIONS);
        try {
            PreparedStatement psAll = connection.prepareStatement(
                    "SELECT user_query AS query, " +
                    "       query_count," +
                    "       hit_count," +
                    "       mtime " +
                    "FROM suggest_data " +
                    "LIMIT ? " +
                    "OFFSET ?");
            psAll.setInt(1, max);
            psAll.setInt(2, start);
            rs = psAll.executeQuery();

            ArrayList<String> suggestions = new ArrayList<String>(max);

            while (rs.next()) {
                suggestions.add(rs.getString(1) + "\t" + rs.getInt(3) + "\t"
                                + rs.getInt(2));
            }

            log.debug(String.format(
                   "Listed %d suggestions in %sms",
                   suggestions.size(), System.currentTimeMillis() - startTime));
            return suggestions;
        } catch (SQLException e) {
            log.error(String.format(
                    "SQLException while dumping a maximum of %d suggestions, "
                    + "starting at %d", max, start), e);
            return new ArrayList<String>();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    log.warn(String.format("Exception while closing Resultset "
                                           + "in listSuggestions(%d, %d)",
                                           start, max), e);
                }
            }
        }
    }

    @Override
    public void addSuggestions(Iterator<String> suggestions) throws
                                                                   IOException {
        log.debug("addSuggestions called");
        long start = System.nanoTime();

        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new IOException(
                    "SQLException while setting autoCommit to false", e);
        }

        try {
            int count = 0;
            while (suggestions.hasNext()) {
                count++;
                String suggestion = suggestions.next();
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
                addSuggestion(query, (int)hits, queryCount);
            }
            log.debug(String.format(
                    "Finished adding %d suggestions in %sms ",
                    count, (System.nanoTime() - start)/1000000D));
            connection.commit();
            updateCount += count;
            analyzeIfNeeded();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                log.error("Failed to roll back transaction: " + e.getMessage(),
                          e);
            }
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

    public synchronized void clear() throws IOException {
        log.info("Clearing suggest data");
        try {
            Statement s = connection.createStatement();
            s.execute("DROP TABLE suggest_index;");
            s.execute("DROP TABLE suggest_data;");
            createSchema();
        } catch (SQLException e) {
            throw new IOException(
                    "Exception while dropping and re-creating table", e);
        }
    }

    public File getLocation() {
        return location;
    }

    private void analyzeIfNeeded() {
        if (updateCount < ANALYZE_INTERVAL) {
            return;
        }
        updateCount = 0;
        optimizeTables();
    }

    private synchronized void optimizeTables() {
        long startTime = System.currentTimeMillis();
        try {
            // Rebuild the table selectivity indexes used by the query optimizer
            log.debug("Optimizing suggest table selectivity");
            Statement stmt = connection.createStatement();
            //noinspection DuplicateStringLiteralInspection
            stmt.execute("ANALYZE");
        } catch (SQLException e) {
            log.warn("Failed to optimize suggest table selectivity", e);
        }
        log.debug("Optimize finished in "
                  + (System.currentTimeMillis() - startTime) + "ms");
    }

    private void setMaxMemoryRows(int maxMemoryRows) {
        try {
            log.debug("Setting MAX_MEMORY_ROWS for suggest to "
                      + maxMemoryRows);
            Statement stmt = connection.createStatement();
            //noinspection DuplicateStringLiteralInspection
            stmt.execute("SET MAX_MEMORY_ROWS " + maxMemoryRows);
        } catch (SQLException e) {
            log.warn("Failed to set MAX_MEMORY_ROWS for suggest", e);
        }
    }

    /* Drop and re-create the indexes */
    @Override
    public void importSuggestions(URL in) throws IOException {
        log.debug("importsuggestions: Dropping indexes");
        try {
            dropIndexes();
        } catch (SQLException e) {
            throw new IOException("SQLException while dropping indexes", e);
        }
        try {
            log.debug("Calling super.import");
            super.importSuggestions(in);
        } finally {

            log.debug("importsuggestions: Creating indexes");
            try {
                createIndexes();
                log.debug("importsuggestion: Finished creating indexes");
            } catch (SQLException e) {
                //noinspection ThrowFromFinallyBlock
                throw new IOException("SQLException while creating indexes", e);
            }
        }
    }

    /**
     * Check if an SQLException is an integrity constraint violation.
     * H2 only uses its custom JdbcSQLExceptions, and not the proper
     * exceptions according to the JDBC spec, so we have to apply
     * this heuristic to detect violations.
     *
     * @param e the sql exception to check
     * @return true if {@code e} has been raised due to a unique key violation
     */
    private boolean isIntegrityConstraintViolation (SQLException e) {
        // The H2 error codes for integrity constraint violations are
        // numbers between 23000 and 23999
        return (e instanceof SQLIntegrityConstraintViolationException ||
                (e.getErrorCode() >= 23000 && e.getErrorCode() < 24000));
    }

    private String normalize(String s) {
        try {
            TokenStream tokens = normalizer.reusableTokenStream(
                    "query", new CharSequenceReader(s));
            return join(tokens, " ");
        } catch (IOException e) {
            log.error(String.format(
                    "Error analyzing query '%s': %s", s, e.getMessage()), e);
            return "ERROR";
        }
    }

    private String sanitize(String s) {
        try {
            TokenStream tokens = sanitizer.reusableTokenStream(
                    "query", new CharSequenceReader(s));
            return join(tokens, " ");
        } catch (IOException e) {
            log.error(String.format(
                    "Error analyzing query '%s': %s", s, e.toString()), e);
            return "ERROR";
        }
    }

    private String join(TokenStream toks, String delimiter) {
        StringBuilder buf = threadLocalBuilder.get();
        try {
            Token tok = new Token();
            while (toks.next(tok) != null) {
                if (buf.length() != 0) {
                    buf.append(" ");
                }
                buf.append(tok.termBuffer(), 0, tok.termLength());
            }
            return buf.toString();
        } catch (IOException e) {
            // This should *never* happen because we read from a local String,
            // not really doing IO
            log.error(String.format(
                    "Error analyzing query: %s", e.toString()), e);
            return "ERROR";
        }
    }

}
