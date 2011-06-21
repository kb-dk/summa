/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
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
package dk.statsbiblioteket.summa.support.harmonise;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;

import java.io.Reader;

/**
 * Lucene query rewriter with callback on TermQuery.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te, hsm")
public class QueryRewriter {

    /**
     * A simple callback interface that fires when the rewriter encounters
     * a TermQuery.
     */
    public interface Event {
        /**
         * Optionally change the given TermQuery or construct a new Query in
         * its place.
         * @param query the query to be processed.
         * @return the processed query. This can be the given query (optionally
         * modified) or a new Query, which will be inserted into the Query tree
         * at the originating position.
         */
        Query onTermQuery(TermQuery query);
    }

    private Event event;
    private QueryParser queryParser;

    /**
     * Constructs a new QueryRewriter.
     * @param event the event to be fired on all Term queries
     */
    public QueryRewriter(Event event) {
        this.event = event;

        queryParser = new QueryParser(Version.LUCENE_31, "", new Analyzer() {
            @Override
            public TokenStream tokenStream(String s, Reader reader) {
                return new WhitespaceTokenizer(Version.LUCENE_31, reader);
            }
        });

        queryParser.setDefaultOperator(QueryParser.AND_OPERATOR);
    }

    /**
     *
     * Rewrites the query from one textual representation to another textual
     * representation. Any difference in the semantics of the given and
     * resulting query is due to the modification of the specific term queries.
     * In other words, if the term queries are not modified, the resulting query
     * tring is semantically equivalent to the given query string with  respect
     * to the standard Lucene query parser.
     * </p>
     * Note that the Lucene query parser handles boolean operators in a rather
     * exotic way. For details, see
     * http://wiki.apache.org/lucene-java/BooleanQuerySyntax
     * https://issues.apache.org/jira/browse/LUCENE-1823
     * https://issues.apache.org/jira/browse/LUCENE-167
     * @param query the unmodified query.
     * @return the rewritten query.
     * @throws org.apache.lucene.queryParser.ParseException if the query could
     * not be parsed by the Lucene query parser.
     */
    public String rewrite(String query) throws ParseException {
        Query q = queryParser.parse(query);
        return convertQueryToString(walkQuery(q));
    }

    private Query walkQuery(Query query) {
        if (query instanceof BooleanQuery) {
            BooleanQuery booleanQuery = (BooleanQuery) query;
            BooleanQuery result = new BooleanQuery();
            for (BooleanClause clause : booleanQuery.getClauses()) {
                BooleanClause clauseResult = new BooleanClause(
                    walkQuery(clause.getQuery()), clause.getOccur());
                result.add(clauseResult);
            }
            return result;
        }
        if (query instanceof TermQuery) {
            TermQuery termQuery = (TermQuery) query;
            return event.onTermQuery(termQuery);
        }
        return query;
    }

    private String convertQueryToString(Query query) {
        String result = "";

        if (!(query instanceof BooleanQuery)) {
            result += query.toString();
        } else {
            BooleanQuery booleanQuery = (BooleanQuery) query;
            String inner = "";
            for (int i = 0; i < booleanQuery.getClauses().length; i++) {
                BooleanClause currentClause = booleanQuery.getClauses()[i];
                switch (currentClause.getOccur()) {
                    case SHOULD:
                        inner += convertQueryToString(currentClause.getQuery());
                        if (i != booleanQuery.getClauses().length - 1) {
                            inner += " OR ";
                        }
                        break;
                    case MUST:
                        inner += "+" + convertQueryToString(
                            currentClause.getQuery()) + " ";
                        break;
                    case MUST_NOT:
                        inner += "-" + convertQueryToString(
                            currentClause.getQuery()) + " ";
                        break;
                    default:
                        throw new RuntimeException(
                            "Unknown occur: " + currentClause.getOccur());
                }
            }
            result += "(" + inner.trim() + ")";
        }

        return result.trim();
    }
}
