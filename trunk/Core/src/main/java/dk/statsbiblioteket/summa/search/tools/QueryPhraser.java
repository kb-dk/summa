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
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.io.StringWriter;

/**
 * Analyzes a given query and expands multiple non-qualified term queries to phrase queries. The current conversion is
 * extremely simple and skips conversion if it encounters anything unexpected.
 * </p><p>
 * Example: {@code 'zoo moo'} -> {@code '(zoo moo) OR "zoo moo"'}<br/>
 * Example: {@code 'zoo -moo'} -> {@code 'zoo -moo'}<br/>
 * Example: {@code 'zoo moo bar:baz'} -> {@code 'zoo moo bar:baz'}<br/>
 * </p><p>
 * Potential future enhancement is to rewrite all sequences:
 * {@code 'foo:bar zoo moo -baz:poo grr olk:pom red rum'} ->
 * {@code '(foo:bar zoo moo -baz:poo grr olk:pom red rum) OR (foo:bar "zoo moo" -baz:poo grr olk:pom "red rum")'}
 * But this has the unfortunate implication that both "zoo moo" and "red rum" must be satisfied for the boost to
 * work. A better solution would be to make all possible permutations, but this can easily lead to query explosion
 * as field-based expansion is added later in the chain.
 * </p><p>
 * It is possible to set slop on configuration- as well as search time. Note that it is not possible to set boost
 * as this is not used by Lucene on phrase queries.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
// TODO: Consider moving this down to field-level or to expand to a given list of fields
// TODO: Remove synchronized and use a pool of parsers
public class QueryPhraser implements Configurable {
    private static Log log = LogFactory.getLog(QueryPhraser.class);

    /**
     * The slop used for proximity matching.
     * https://wiki.apache.org/lucene-java/LuceneFAQ#Is_there_a_way_to_use_a_proximity_operator_.28like_near_or_within.29_with_Lucene.3F
     * </p><p>
     * Optional. Default is 2 (two terms can be switched).
     * </p>
     */
    public static final String CONF_SLOP = "queryphraser.slop";
    public static final int DEFAULT_SLOP = 2;
    public static final String SEARCH_SLOP = CONF_SLOP;

    private final int defaultSlop;
    private final QueryParser qp = QueryRewriter.createDefaultQueryParser();

    public QueryPhraser(Configuration conf) {
        defaultSlop = conf.getInt(CONF_SLOP, DEFAULT_SLOP);
        log.info("Created QueryPhraser with default slop " + defaultSlop);
    }

    /**
     * If possible, extends the query to '(query) OR "terms"', as described in the JavaDoc for the class.
     * @param request the detailed request, potentially stating {@link #SEARCH_SLOP}.
     * @param query original query.
     * @return the query rewritten to include a phrase search or the unmodified query, depending on input.
     * @throws org.apache.lucene.queryparser.classic.ParseException if the Query could not be parsed at all.
     */
    public String rewrite(Request request, String query) throws ParseException {
        int slop = request.getInt(SEARCH_SLOP, defaultSlop);
        String terms;
        if ((terms = getTerms(query)) == null) {
            log.debug("Unable to add phrase boost to '" + query + "'");
            return query;
        }
        String phrased = "(" + query + ") OR \"" + terms + "\"~" + slop;
        log.debug("Phrase boosted '" + query + "' to '" + phrased + "'");
        return phrased;
    }

    private synchronized String getTerms(String query) throws ParseException {
        Query q;
        q = qp.parse(query);
/*        if (q instanceof TermQuery) {
            return ((TermQuery)q).getTerm().text();
        }*/
        if (!(q instanceof BooleanQuery)) {
            return null;
        }
        BooleanQuery bq = (BooleanQuery)q;
        StringWriter sw = new StringWriter(50);
        int counter = 0;
        for (BooleanClause bc: bq.getClauses()) {
            if (!bc.isRequired() || !(bc.getQuery() instanceof TermQuery)) {
                return null;
            }
            if (counter > 0) {
                sw.append(" ");
            }
            sw.append(((TermQuery)bc.getQuery()).getTerm().text());
            counter++;
        }
        return counter > 1 ? sw.toString() : null; // Single term is never a phrase
    }

}
