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
package dk.statsbiblioteket.summa.support.suggest;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.lucene.analysis.SummaKeywordAnalyzer;
import dk.statsbiblioteket.summa.common.util.UniqueTimestampGenerator;
import dk.statsbiblioteket.summa.support.api.SuggestResponse;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.reader.CharSequenceReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.h2.jdbcx.JdbcDataSource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;


/**
 * H2 database implementation of the {@link SuggestStorage}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SuggestStorageH2 extends SuggestStorageImpl {
    /**
     * Logger for this class.
     */
    private static Log log = LogFactory.getLog(SuggestStorageH2.class);
    /**
     * Database file for this storage.
     */
    public static final String DB_FILE = "suggest_h2storage";

    public static final int MAX_QUERY_LENGTH = 250;

    /**
     * The maximum number of suggestions to return from {@link #listSuggestions}
     * and {@link #getSuggestion(String, int)}  regardless of the stated
     * maximum in the method-call.
     */
    public static final int MAX_SUGGESTIONS = 1000;

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
    /**
     * Default value for {@link #CONF_L2CACHE}.
     */
    public static final boolean DEFAULT_L2CACHE = false;

    /**
     * The {@link Analyzer} implementation to use for generating normalized
     * query collation keys. When suggestion lookup is done the query-string
     * is normalized and compared against the normalized suggestions in the
     * database. The normalizer only affects how suggestions are looked up
     * in the database, to affect how they are presented to the user see
     * {@link #CONF_SANITIZER}.
     * <p/>
     * Default is {@link SummaKeywordAnalyzer}.
     * <p/>
     * The specified class <i>must</i> have a no-arguments constructor.
     */
    public static final String CONF_NORMALIZER = "summa.support.suggest.normalizer";
    /**
     * Default value for {@link #CONF_NORMALIZER}.
     */
    public static final Class<? extends Analyzer> DEFAULT_NORMALIZER = SummaKeywordAnalyzer.class;

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
    /**
     * Default value for {@link #CONF_SANITIZER}.
     */
    public static final Class<? extends Analyzer> DEFAULT_SANITIZER = WhitespaceAnalyzer.class;

    /**
     * Suggestion requests with prefixes shorter than this are not processed.
     * </p><p>
     * Optional. Default is 1.
     */
    public static final String CONF_MIN_SUGGEST_LENGTH = "summa.support.suggest.minlength";
    public static final int DEFAULT_MIN_SUGGEST_LENGTH = 1;

    private File location = null;
    private Connection connection;
    private boolean closed = true;
    private Analyzer normalizer, sanitizer;
    private boolean normalizeQueries;
    private int updateCount = 0;
    private boolean useL2cache;
    private UniqueTimestampGenerator timestamps;
    private final int minLength;

    @SuppressWarnings("unused")
    public SuggestStorageH2(Configuration conf) {
        log.debug("Creating SuggestStorageH2");

        normalizeQueries = conf.getBoolean(
                SuggestSearchNode.CONF_NORMALIZE_QUERIES, SuggestSearchNode.DEFAULT_NORMALIZE_QUERIES);

        /* Initialize query normalizer */
        Class<? extends Analyzer> analyzerClass =
                Configuration.getClass(CONF_NORMALIZER, Analyzer.class, DEFAULT_NORMALIZER, conf);
        try {
            normalizer = analyzerClass.newInstance();
        } catch (Exception e) {
            throw new ConfigurationException("Unable to instantiate query normalizer", e);
        }

        /* Initialize query sanitizer */
        analyzerClass = Configuration.getClass(CONF_SANITIZER, Analyzer.class, DEFAULT_SANITIZER, conf);
        try {
            // All analyzers now takes version
            sanitizer = analyzerClass.getConstructor(Version.class).newInstance(Version.LUCENE_46);
        } catch (Exception e) {
            throw new ConfigurationException("Unable to instantiate query sanitizer", e);
        }

        minLength = conf.getInt(CONF_MIN_SUGGEST_LENGTH, DEFAULT_MIN_SUGGEST_LENGTH);

        useL2cache = conf.getBoolean(CONF_L2CACHE, DEFAULT_L2CACHE);
        timestamps = new UniqueTimestampGenerator();
    }

    @Override
    public synchronized void open(File location) throws IOException {
        log.debug(String.format(Locale.ROOT, "open(%s) called for SuggestStorageH2", location));
        this.location = location;
        boolean createNew;
        if (!location.exists()) {
            System.out.println("new");
            log.info(String.format(Locale.ROOT, "Creating new SuggestStorageH2 at location '%s'", location));
            if (!location.mkdirs()) {
                throw new ConfigurationException(String.format(Locale.ROOT, "Unable to create folder '%s'", location));
            }
            createNew = true;
        } else {
            System.out.println("exist");
            if (new File(location, DB_FILE + ".h2.db").isFile() || new File(location, DB_FILE + ".data.db").isFile()) {
                /* Database location exists*/
                log.debug(String.format(Locale.ROOT, "Reusing old database found at '%s'", location));
                createNew = false;
            } else {
                log.debug(String.format(Locale.ROOT, "No database at '%s'", location));
                createNew = true;
            }
        }

        connection = resetConnection();
        if (createNew) {
            log.info(String.format(Locale.ROOT, "Creating new table for '%s'", location));
            try {
                createSchema();
            } catch (SQLException e) {
                throw new IOException("Unable to create schema", e);
            }
        } else {
            log.debug("Calling normalize, just to make sure");           
            try {
                createIndexes();
            } catch (SQLException e) {
                throw new IOException("SQLException while creating indexes during startup", e);
            }
        }

        closed = false;
    }

    /**
     * Resets the connection.
     *
     * @return The reset connection.
     * @throws IOException If connection could not be reset.
     */
    private Connection resetConnection() throws IOException {
        if (connection != null) {
            log.warn("Resetting database connection");
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("Failed to close database connection: " + e.getMessage(), e);
            }
        }

        if (useL2cache) {
            log.info("H2 L2 cache option is not supported anymore");           
        }

        String sourceURL = "jdbc:h2:" + location.getAbsolutePath() + File.separator + DB_FILE;

        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(sourceURL);
        try {
            connection = dataSource.getConnection();
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        } catch (SQLException e) {
            throw new IOException(String.format(Locale.ROOT, "Unable to get a connection from '%s'", sourceURL), e);
        }
        

        return connection;
    }

    /**
     * Creates the Schema for the suggest database implementation.
     *
     * @throws SQLException If error occurs while executing SQL.
     */
    private void createSchema() throws SQLException {
        System.out.println("createSchema");
        Statement s = null;
        try {
            s = connection.createStatement();

            s.execute(String.format(Locale.ROOT, "CREATE TABLE IF NOT EXISTS suggest_data("
                                    + "user_query VARCHAR(%1$d) PRIMARY KEY, normalized_query VARCHAR(%1$d) NOT NULL, query_count INTEGER, "
                                    + "hit_count INTEGER, mtime BIGINT)", MAX_QUERY_LENGTH));
            System.out.println(1);
            createIndexes();
            System.out.println(2);
        } finally {
            if (s != null) {
                s.close();
            }
        }
    }

    /**
     * Create indexes on the tables.
     *
     * @throws SQLException If error occur while creating indexes.
     */
    private void createIndexes() throws SQLException {
        Statement s = null;
        try {
            long startTime = System.currentTimeMillis();
            log.info("Preparing table indices");
            s = connection.createStatement();

            //

            // This index is used for prefix searches sorted on query_count,
            // as well as fast lookups of query_id given a user_query
            s.execute("CREATE INDEX IF NOT EXISTS normalized_query_in "
                      + "ON suggest_data(normalized_query)");

            // This index is used for prefix searches sorted on query_count,
            // as well as fast lookups of query_id given a user_query
            s.execute("CREATE INDEX IF NOT EXISTS normalized_query_count_in "
                      + "ON suggest_data(normalized_query,query_count)");

            
            // For updates         
            s.execute("CREATE INDEX IF NOT EXISTS user_query_in "
                      + "ON suggest_data(user_query)");
                       
              
            log.info("Table indices prepared in " + (System.currentTimeMillis() - startTime) + "ms");
        } finally {
            if (s != null) {
                s.close();
            }
        }
    }

    /**
     * Drop all indexes on suggest tables.
     *
     * @throws SQLException If error occurs while dropping indexes.
     */
    private void dropIndexes() throws SQLException {
        Statement s = null;
        try {
            s = connection.createStatement();           
            s.execute("DROP INDEX normalized_query_in");
            s.execute("DROP INDEX normalized_query_count_in");
            s.execute("DROP INDEX user_query_in");
        
            
            
        } finally {
            if (s != null) {
                s.close();
            }
        }
    }

    /**
     * Closing the suggest database and the database connection.
     */
    @Override
    public synchronized void close() {
        log.info(String.format(Locale.ROOT, "Closing '%s'", location));
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

    @Override
    public synchronized SuggestResponse getSuggestion(String prefix, int maxResults) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("getSuggestion(" + prefix + ", " + maxResults + ")");
        }
        if (prefix.length() < minLength) {
            return new SuggestResponse(prefix, maxResults);
        }


        try {
            long startTime = System.nanoTime();
            maxResults = Math.min(maxResults, MAX_SUGGESTIONS);

            connection.setReadOnly(true);
            connection.setAutoCommit(false);

            PreparedStatement psExists = connection.prepareStatement(
                    // FIXME: Sorting on query_count is slow
                    "SELECT user_query AS query, query_count, hit_count FROM suggest_data "                    
                    + "WHERE normalized_query LIKE ? ORDER BY query_count DESC LIMIT ?");
            psExists.setString(1, normalize(prefix) + "%");
            psExists.setInt(2, maxResults);
            psExists.setFetchDirection(ResultSet.FETCH_FORWARD);
            psExists.setFetchSize(maxResults);

            int count = 0;
            SuggestResponse response = new SuggestResponse(prefix, maxResults);
            try (ResultSet rs = psExists.executeQuery()) {
                while (rs.next() && count < maxResults) {
                    count++;
                    response.addSuggestion(rs.getString(1), rs.getInt(3), rs.getInt(2));
                }

                log.debug("getSuggestion(" + prefix + ", " + maxResults + ") -> " + count + " suggestions in "
                          + (System.nanoTime() - startTime) / 1000000D + "ms");

                return response;
            } finally {
                connection.setReadOnly(false);
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IOException("Unable to get suggestion for '" + prefix + "', " + maxResults, e);
        }
    }

    @Override
    public synchronized SuggestResponse getRecentSuggestions(int ageSeconds, int maxResults) throws IOException {        
          throw new UnsupportedOperationException("Has been removed");
    }

    @Override
    public void addSuggestion(String query, int hits) throws IOException {
        addSuggestion(query, hits, -1);
    }

    // -1 means add 1 to suggest_index.query_count
    @Override
    public synchronized void addSuggestion(String query, int hits, int queryCount) throws IOException {

        // Doing checkString() here makes sure we don't allocate huge amounts
        // of memory in our thread local StringBuilders used in join()
        if (!checkString(query)) {
            return;
        }
        query = sanitize(query);

        if (hits == 0) {
            log.trace("No hits for '" + query + "'. Deleting query...");
            try {
                delete(query);
            } catch (SQLException e) {
                log.warn(String.format(Locale.ROOT, "Unable to delete suggestion '%s': %s", query, e.getMessage()), e);
            }
            return;
        }

        try {
            insertSuggestion(query, hits, queryCount == -1 ? 1 : queryCount);
            log.debug("Created new suggestion '" + query + "' with " + hits + " hits");
        } catch (SQLException e) {
            if (isIntegrityConstraintViolation(e)) {
                updateSuggestion(query, hits, queryCount);
                log.debug("Updated suggestion '" + query + "' with " + hits + " hits");
            } else {
                throw new IOException(String.format(Locale.ROOT, "Unable to complete addSuggestion(%s, %d, %d)", query, hits,
                                                    queryCount), e);
            }
        }
    }

    private void insertSuggestion(String query, int hits, int queryCount) throws SQLException {

        String normalizedQuery = normalize(query);

        if (!checkString(normalizedQuery)) {
            return;
        }

        if (normalizeQueries) {
            query = normalizedQuery;
        }

        // We need to control the transaction in order to roll back the
        // suggest_index insertion if 'query' is already in
        // suggest_data.user_query
        connection.setAutoCommit(false);

        PreparedStatement psInsert;
        try {

            // Raises integrity constraint violation if 'query' is already
            // listed in suggest_data.user_query
            psInsert = connection.prepareStatement("INSERT INTO suggest_data VALUES (?, ?, ?, ?, ?)");
            psInsert.setString(1, query);
            psInsert.setString(2, normalizedQuery);
            psInsert.setInt(3, queryCount);
            psInsert.setInt(4, hits);
            psInsert.setLong(5, timestamps.next());
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
                        "Unexpected error inserting normalized query '" + normalizedQuery + "' in index", e);
            }
        } finally {
            connection.setAutoCommit(true);
        }

        updateCount++;        
    }

    private boolean checkString(String normalizedQuery) {
        if (normalizedQuery.length() > MAX_QUERY_LENGTH) {
            log.info("addSuggestion: The analyzed query must be " + MAX_QUERY_LENGTH + " chars or less. Got "
                     + normalizedQuery.length() + " chars from '" + normalizedQuery + "'");
            return false;
        }
        return true;
    }

    @Override
    public boolean deleteSuggestion(String suggestion) {
        try {
            delete(suggestion);
            return true;
        } catch (SQLException e) {
            log.warn("Deletion of suggstion '" + suggestion + "' was not succesfull");
        }
        return false;
    }

    /**
     * Deletes all suggestions, no matter the case.
     *
     * @param query The query.
     * @throws SQLException SQL error if suggestion wasn't deleted.
     */
    private void delete(String query) throws SQLException {
        log.debug("Removing suggestion '" + query + "'");

        String normalizedQuery = normalize(query);

        // Delete from suggest_data table

            PreparedStatement  psUpdate = connection.prepareStatement("DELETE FROM suggest_data WHERE normalized_query=?");
            psUpdate.setString(1, normalizedQuery);
            psUpdate.executeUpdate();
        updateCount++;     
    }


    /**
     * If queryCount is -1 we must add one to the previous value, otherwise
     * we should set suggest_index.query_count to queryCount.
     *
     * @param query      The search query.
     * @param hits       The hit count, gonna override 'suggest_data.hit_count'.
     * @param queryCount if -1 we add one to old query_count otherwise override.
     */
    private void updateSuggestion(String query, int hits, int queryCount) {
        try {
            String normalizedQuery = normalize(query);

            if (!checkString(normalizedQuery)) {
                return;
            }

            if (normalizeQueries) {
                query = normalizedQuery;
            }

            PreparedStatement psUpdate;
            if (queryCount == -1) {
                queryCount = 1;
                psUpdate = connection.prepareStatement("UPDATE suggest_data " +
                                                       "SET query_count=query_count+?, " +
                                                       "    hit_count=?, " +
                                                       "    mtime=? " +
                                                       "WHERE user_query=?");
            } else {
                psUpdate = connection.prepareStatement("UPDATE suggest_data " +
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

           
        } catch (SQLException e) {
            log.error(String.format(Locale.ROOT, "Failed to update database with query %s, hits=%s, queryCount=%s: %s", query,
                                    hits, queryCount, e.getMessage()), e);
        }

        updateCount++;    
    }

    @Override
    public ArrayList<String> listSuggestions(int start, int max) throws IOException {
        log.debug(String.format(Locale.ROOT, "Listing suggestions from %d to %d", start, max));

        long startTime = System.currentTimeMillis();
        ResultSet rs = null;
        max = Math.min(max, MAX_SUGGESTIONS);
        try {
            PreparedStatement psAll = connection.prepareStatement("SELECT user_query AS query, " +
                                                                  "       query_count," +
                                                                  "       hit_count," +
                                                                  "       mtime " +
                                                                  "FROM suggest_data " +
                                                                  "LIMIT ? " +
                                                                  "OFFSET ?");
            psAll.setInt(1, max);
            psAll.setInt(2, start);
            rs = psAll.executeQuery();

            ArrayList<String> suggestions = new ArrayList<>(max);

            while (rs.next()) {
                suggestions.add(rs.getString(1) + "\t" + rs.getInt(3) + "\t" + rs.getInt(2));
            }

            log.debug(String.format(Locale.ROOT, "Listed %d suggestions in %sms", suggestions.size(),
                                    System.currentTimeMillis() - startTime));
            return suggestions;
        } catch (SQLException e) {
            log.error(String.format(Locale.ROOT, "SQLException while dumping a maximum of %d suggestions, starting at %d", max,
                                    start), e);
            return new ArrayList<>();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    log.warn(String.format(Locale.ROOT, "Exception while closing Resultset in listSuggestions(%d, %d)", start,
                                           max), e);
                }
            }
        }
    }

    @Override
    public void addSuggestions(Iterator<String> suggestions) throws IOException {
        log.debug("addSuggestions called");
        long start = System.nanoTime();

        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new IOException("SQLException while setting autoCommit to false", e);
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
                    tokens[0] = Strings.join(Arrays.asList(tokens).subList(0, tokens.length - 2), "\t");
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
                    log.warn(String.format(Locale.ROOT, "NumberFormatException for hits with '%s'", tokens[1]));
                }
                try {
                    if (tokens.length > 2) {
                        queryCount = Integer.valueOf(tokens[2]);
                    }
                } catch (NumberFormatException e) {
                    log.warn(String.format(Locale.ROOT, "NumberFormatException for querycount with '%s'", tokens[2]));
                }
                addSuggestion(query, (int) hits, queryCount);
            }
            log.debug(String.format(Locale.ROOT, "Finished adding %d suggestions in %sms ", count,
                                    (System.nanoTime() - start) / 1000000D));
            connection.commit();
            updateCount += count;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                log.error("Failed to roll back transaction: " + e.getMessage(), e);
            }
            throw new IOException("SQLException adding suggestions", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                log.error("SQLException setting autoCommit to true. Suggest might not be able to get updates", e);
            }
        }
    }

    @Override
    public synchronized void clear() throws IOException {
        log.info("Clearing suggest data");
        Statement s = null;
        try {
            s = connection.createStatement();
            s.execute("DROP TABLE suggest_data;");
            createSchema();
        } catch (SQLException e) {
            throw new IOException("Exception while dropping and re-creating table", e);
        } finally {
            try {
                s.close();
            } catch (SQLException e) {
                throw new IOException("Error while closing statement", e);
            }
        }
    }

    @Override
    public File getLocation() {
        return location;
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
    private boolean isIntegrityConstraintViolation(SQLException e) {
        // The H2 error codes for integrity constraint violations are
        // numbers between 23000 and 23999
        return (e instanceof SQLIntegrityConstraintViolationException
                || (e.getErrorCode() >= 23000 && e.getErrorCode() < 24000));
    }

    private String normalize(String s) {
        try {
            TokenStream tokens = normalizer.tokenStream("query", new CharSequenceReader(s));
            return join(tokens, " ");
        } catch (IOException e) {
            log.error(String.format(Locale.ROOT, "Error analyzing query '%s': %s", s, e.getMessage()), e);
            return "ERROR";
        }
    }

    private String sanitize(String s) {
        try {
            TokenStream tokens = sanitizer.tokenStream("query", new CharSequenceReader(s));
            return join(tokens, " ");
        } catch (IOException e) {
            log.error(String.format(Locale.ROOT, "Error analyzing query '%s': %s", s, e.toString()), e);
            return "ERROR";
        }
    }

    /**
     * Join a stream of tokens, between each token, is added the delimiter.
     *
     * @param toks      Token stream.
     * @param delimiter The delimiter.
     * @return The string build by added delimiter between each token.
     */
    private String join(TokenStream toks, String delimiter) {
        StringBuilder buf = threadLocalBuilder.get();
        try {
            CharTermAttribute term = toks.getAttribute(CharTermAttribute.class);
            toks.reset();
            //Token tok = new Token();
            while (toks.incrementToken()) {
                if (buf.length() != 0) {
                    buf.append(delimiter);
                }
                buf.append(term.toString());
            }
            toks.end();
            toks.close();
            return buf.toString();
        } catch (IOException e) {
            // This should *never* happen because we read from a local String,
            // not really doing IO
            log.error(String.format(Locale.ROOT, "Error analyzing query: %s", e.toString()), e);
            return "ERROR";
        }
    }

    private ThreadLocal<StringBuilder> threadLocalBuilder = new ThreadLocal<StringBuilder>() {
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

}
