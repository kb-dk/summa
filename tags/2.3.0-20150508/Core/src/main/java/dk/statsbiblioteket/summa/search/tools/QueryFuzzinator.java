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
package dk.statsbiblioteket.summa.search.tools;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * Rewrites simple TermQueries into fuzzy queries.
 * </p><p>
 * Warning: This makes it impossible to perform specific searches and requires more processing power than regular
 * queries. Use only for searches in corpora that has very dirty data (e.g. low quality OCR).
 * </p><p>
 * Note: As the query is converted back into a String, only the {@link #CONF_MAX_EDITS} option has any effect.
 * </p>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class QueryFuzzinator implements Configurable {
    private static Log log = LogFactory.getLog(QueryFuzzinator.class);

    /**
     * FuzzyQuery parameter. See {@link FuzzyQuery#defaultPrefixLength}.
     * </p><p>
     * Optional. Default is 0.
     */
    private static final String CONF_PREFIX_LENGTH = "fuzzinator.prefixlength";
    private static final int DEFAULT_PREFIX_LENGTH = FuzzyQuery.defaultPrefixLength;
    private static final String SEARCH_PREFIX_LENGTH = CONF_PREFIX_LENGTH;

    /**
     * FuzzyQuery parameter. See {@link FuzzyQuery#maxEdits}.
     * </p><p>
     * Optional. Valid distances are 0, 1 and 2. Default is 2.
     */
    public static final String CONF_MAX_EDITS = "fuzzinator.maxedits";
    public static final int DEFAULT_MAX_EDITS = FuzzyQuery.defaultMaxEdits;
    public static final String SEARCH_MAX_EDITS = CONF_MAX_EDITS;

    /**
     * FuzzyQuery parameter. See {@link FuzzyQuery#maxExpansions}.
     * </p><p>
     * Optional. Default is 50.
     */
    private static final String CONF_MAX_EXPANSIONS = "fuzzinator.maxexpansions";
    private static final int DEFAULT_MAX_EXPANSIONS = FuzzyQuery.defaultMaxExpansions;
    private static final String SEARCH_MAX_EXPANSIONS = CONF_MAX_EXPANSIONS;

    /**
     * FuzzyQuery parameter. See {@link FuzzyQuery#defaultTranspositions}.
     * </p><p>
     * Optional. Default is true.
     */
    private static final String CONF_TRANSPOSITIONS = "fuzzinator.transpositions";
    private static final boolean DEFAULT_TRANSPOSITIONS = FuzzyQuery.defaultTranspositions;
    private static final String SEARCH_TRANSPOSITIONS = CONF_TRANSPOSITIONS;

    /**
     * Whether or not the fuzzinator should process the query or pass it through.
     */
    public static final String CONF_ENABLED = "fuzzinator.enabled";
    public static final boolean DEFAULT_ENABLED = true;
    public static final String SEARCH_ENABLED = CONF_ENABLED;

    public int prefixLength;
    public int maxEdits;
    public int maxExpansions;
    public boolean transpositions;

    public boolean enabled;

    public QueryFuzzinator(Configuration conf) {
        prefixLength = conf.getInt(CONF_PREFIX_LENGTH, DEFAULT_PREFIX_LENGTH);
        maxEdits = conf.getInt(CONF_MAX_EDITS, DEFAULT_MAX_EDITS);
        maxExpansions = conf.getInt(CONF_MAX_EXPANSIONS, DEFAULT_MAX_EXPANSIONS);
        transpositions = conf.getBoolean(CONF_TRANSPOSITIONS, DEFAULT_TRANSPOSITIONS);
        enabled = conf.getBoolean(CONF_ENABLED, DEFAULT_ENABLED);
        log.info("Created " + this);
    }

    public String rewrite(Request request, String query) throws ParseException {
        if (!request.getBoolean(SEARCH_ENABLED, this.enabled)) {
            log.trace("Skipping fuzzyfying as it is disabled");
            return query;
        }
        final int prefixLength = request.getInt(SEARCH_PREFIX_LENGTH, this.prefixLength);
        final int maxEdits = request.getInt(SEARCH_MAX_EDITS, this.maxEdits);
        final int maxExpansions = request.getInt(SEARCH_MAX_EXPANSIONS, this.maxExpansions);
        final boolean transpositions = request.getBoolean(SEARCH_TRANSPOSITIONS, DEFAULT_TRANSPOSITIONS);

        QueryRewriter queryRewriter = new QueryRewriter(null, null, new QueryRewriter.Event() {
            @Override
            public Query onQuery(TermQuery query) {
                return new FuzzyQuery(query.getTerm(), maxEdits, prefixLength, maxExpansions, transpositions);
            }
        });
        String rewritten = queryRewriter.rewrite(query);
        log.debug("Fuzzified '" + query + "' to '" + rewritten + "''");
        return rewritten;
    }

    @Override
    public String toString() {
        return "QueryFuzzinator(prefixLength=" + prefixLength + ", maxEdits=" + maxEdits +
               ", maxExpansions=" + maxExpansions + ", transpositions=" + transpositions +
               ", enabled=" + enabled + ')';
    }
}
