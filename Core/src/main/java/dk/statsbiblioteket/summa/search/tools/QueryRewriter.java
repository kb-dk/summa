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
package dk.statsbiblioteket.summa.search.tools;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.reader.ReplaceFactory;
import dk.statsbiblioteket.util.reader.ReplaceReader;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Version;

import java.util.HashMap;
import java.util.Map;

// TODO: Make a pool of parsers instead of using synchronized
/**
 * Lucene query rewriter with callback on various types of queries.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te, hsm")
public class QueryRewriter {

    /**
     * If true, the QueryRewriter attempts to make a safe terse output. This affects parentheses on BooleanQueries
     * and explicit must ('+) on clauses. Summon DisMax does not like parentheses and explicit '+' and falls back to
     * standard query parsing.
     * </p><p>
     * Sample: terse=true:  "(+foo +(+bar +zoo))" -> "foo (bar zoo)".
     *         terse=false: "(+foo (bar zoo))" -> "(+foo (bar zoo))".
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_TERSE = "queryrewriter.output.terse";
    public static final boolean DEFAULT_TERSE = true;

    /**
     * If true, the text part of a TermQuery is quoted. If false, an escape of problematic characters is attempted.
     * </p><p>
     * Optional. Default is true (safer, but might clash with custom QueryParsers).
     */
    public static final String CONF_QUOTE_TERMS = "queryrewriter.terms.quote";
    public static final boolean DEFAULT_QUOTE_TERMS = true;

    /**
     * A simple callback that fires when the rewriter encounters Queries.
     * Returning null is allowed.
     */
    public static class Event {
        /**
         * Optionally change the given Query or construct a new Query in
         * its place.
         *
         * @param query the query to be processed.
         * @return the processed query. This can be the given query (optionally modified) or a new Query, which will be
         *         inserted into the Query tree at the originating position.
         */
        public Query onQuery(TermQuery query) {
            return query;
        }

        /**
         * Optionally change the given Query or construct a new Query in
         * its place.
         *
         * @param query the query to be processed.
         * @return the processed query. This can be the given query (optionally modified) or a new Query, which will be
         *         inserted into the Query tree at the originating position.
         */
        public Query onQuery(PhraseQuery query) {
            return query;
        }

        /**
         * Optionally change the given Query or construct a new Query in
         * its place.
         *
         * @param query the query to be processed.
         * @return the processed query. This can be the given query (optionally modified) or a new Query, which will be
         *         inserted into the Query tree at the originating position.
         */
        public Query onQuery(TermRangeQuery query) {
            return query;
        }

        /**
         * Optionally change the given Query or construct a new Query in
         * its place.
         *
         * @param query the query to be processed.
         * @return the processed query. This can be the given query (optionally modified) or a new Query, which will be
         *         inserted into the Query tree at the originating position.
         */
        public Query onQuery(PrefixQuery query) {
            return query;
        }

        /**
         * Optionally change the given Query or construct a new Query in
         * its place.
         *
         * @param query the query to be processed.
         * @return the processed query. This can be the given query (optionally modified) or a new Query, which will be
         *         inserted into the Query tree at the originating position.
         */
        public Query onQuery(FuzzyQuery query) {
            return query;
        }

        /**
         * Optionally change the given Query or construct a new Query in its place. This is the fallback method.
         *
         * @param query the query to be processed.
         * @return the processed query. This can be the given query (optionally modified) or a new Query, which will be
         *         inserted into the Query tree at the originating position.
         */
        public Query onQuery(Query query) {
            return query;
        }
    }

    private Event event;
    private QueryParser queryParser;
    private final boolean terse;
    private final boolean quoteTerms;

    /**
     * Constructs a new QueryRewriter.
     *
     * @param event the event to be fired on all sub-queries
     * @deprecated use the full constructor {@link QueryRewriter#QueryRewriter(Configuration, QueryParser, Event)} as
     *             a QueryParser matching the underlying QueryParser should ensure the best result.
     */
    public QueryRewriter(Event event) {
        this(null, null, event);
    }

    /**
     * @param conf        specific setup for the QueryRewriter. null is allowed.
     * @param queryParser should be comparable to the parser used by the intended searcher. If null, a standard Lucene
     *                    WhitespaceTokenizer will be used inside of a plain analyzer.
     * @param event fired on all sub-queries.
     */
    public QueryRewriter(Configuration conf, QueryParser queryParser, Event event) {
        this.event = event;
        this.queryParser = queryParser == null ? createDefaultQueryParser() : queryParser;
        terse = conf == null ? DEFAULT_TERSE : conf.getBoolean(CONF_TERSE, DEFAULT_TERSE);
        quoteTerms = conf == null ? DEFAULT_QUOTE_TERMS : conf.getBoolean(CONF_QUOTE_TERMS, DEFAULT_QUOTE_TERMS);
    }

    public static QueryParser createDefaultQueryParser() {
        QueryParser queryParser = new QueryParser(Version.LUCENE_40, "", new WhitespaceAnalyzer(Version.LUCENE_40));

        queryParser.setDefaultOperator(QueryParser.AND_OPERATOR);
        return queryParser;
    }

    /**
     * Parses the Query and returns true if it contains only non-qualified terms that are either neutral or required.
     * Sampled: {@code 'foo bar'} or {@code '+"foo"^1.2 +"bar"^0.9'}, but not {@code '+"foo"^1.2 -"bar"^0.9'} or
     * {@code foo [2000 TO 2009]}.
     * </p><p>
     * In case of parse errors, false is returned.
     * </p><p>
     * Simple queries are assumed to be usable by the simple DisMax in Solr. Note that eDisMax supports more than
     * simple queries.
     * @param query a textual query.
     * @return true if the query is simple.
     */
    public synchronized boolean isSimple(String query) {
        Query q;
        try {
            q = queryParser.parse(query);
        } catch (ParseException e) {
            return false;
        }
        if (q instanceof TermQuery) {
            return true;
        }
        if (!(q instanceof BooleanQuery)) {
            return false;
        }
        BooleanQuery bq = (BooleanQuery)q;
        for (BooleanClause bc: bq.getClauses()) {
            if (bc.isProhibited() || !(bc.getQuery() instanceof TermQuery)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Rewrites the query from one textual representation to another textual representation. Any difference in the
     * semantics of the given and resulting query is due to the modification of the specific term queries.
     * In other words, if the term queries are not modified, the resulting query String is semantically equivalent to
     * the given query string with respect to the standard Lucene query parser.
     * </p>
     * Note that the Lucene query parser handles boolean operators in a rather exotic way. For details, see
     * http://wiki.apache.org/lucene-java/BooleanQuerySyntax
     * https://issues.apache.org/jira/browse/LUCENE-1823
     * https://issues.apache.org/jira/browse/LUCENE-167
     *
     * @param query the unmodified query.
     * @return the rewritten query. Note that this might be null if the Query is collapsed to nothing.
     * @throws org.apache.lucene.queryparser.classic.ParseException if the query could not be parsed by the Lucene query
     *         parser.
     */
    public synchronized String rewrite(String query) throws ParseException {
        Query q = queryParser.parse(query);
        Query walked = walkQuery(q, true);
        return walked == null ? null : convertQueryToString(walked, true);
    }

    private Query walkQuery(Query query, boolean top) {
        if (query instanceof BooleanQuery) {
            BooleanQuery booleanQuery = (BooleanQuery) query;
            BooleanQuery result = new BooleanQuery();
            boolean foundSome = false;
            for (BooleanClause clause : booleanQuery.getClauses()) {
                Query walked = walkQuery(clause.getQuery(), false);
                if (walked != null) {
                    BooleanClause clauseResult = new BooleanClause(walked, clause.getOccur());
                    result.add(clauseResult);
                    foundSome = true;
                }
            }
            return foundSome ? result : null;
        } else if (query instanceof TermQuery) {
            return event.onQuery((TermQuery) query);
        } else if (query instanceof PhraseQuery) {
            return event.onQuery((PhraseQuery) query);
        } else if (query instanceof TermRangeQuery) {
            return event.onQuery((TermRangeQuery) query);
        } else if (query instanceof PrefixQuery) {
            return event.onQuery((PrefixQuery) query);
        } else if (query instanceof FuzzyQuery) {
            return event.onQuery((FuzzyQuery) query);
        }
        return event.onQuery(query);
    }

    /**
     * @param query a standard Lucene query.
     * @return a String representation fo the Query, parsable by the Lucene standard QueryParser.
     */
    public String toString(Query query) {
        return convertQueryToString(query, true);
    }
    private String convertQueryToString(Query query, boolean top) {
        String result = "";

        if (query instanceof TermQuery) {
            TermQuery tq = (TermQuery) query;
            // We need to quote a term query because even though certain reserved characters get parsed correctly,
            // boosting this term is incorrect syntax. Example: "- " is OK. "-^1" is not OK. It must be converted to
            // "\"- \"^1".
            result = convertSubqueryToString(tq.getTerm().field(), tq.getTerm().text(), quoteTerms);
            // It does not hurt if we mistakenly send on 1.0 by bad comparison
            //noinspection FloatingPointEquality
            if (tq.getBoost() != 1.0f) {
                result += "^" + tq.getBoost();
            }
        } else if (query instanceof PrefixQuery) {
            PrefixQuery prefixQuery = (PrefixQuery) query;
            return convertSubqueryToString(prefixQuery.getField(), prefixQuery.getPrefix().text(), false) + "*";
        } else if (query instanceof BooleanQuery) {
            String inner = booleanQueryToString((BooleanQuery) query);
            result += top && terse ? inner.trim() : "(" + inner.trim() + ")";
        } else {
            result += query.toString();
        }

        return result.trim();
    }

    /*
    This method implicitly expects the searcher to use AND as default. The omission of + when all clauses are MUST is
    necessary as the Solr DisMax query parser apparently does not accept "+foo +bar".
     */
    private String booleanQueryToString(BooleanQuery booleanQuery) {
        // convert the boolean query to a string recursively
        String inner = "";
        boolean onlyMust = containsOnlyMust(booleanQuery) && terse;
        for (int i = 0; i < booleanQuery.getClauses().length; i++) {
            BooleanClause currentClause = booleanQuery.getClauses()[i];
            switch (currentClause.getOccur()) {
                case SHOULD:
                    inner += convertQueryToString(currentClause.getQuery(), false);
                    if (i != booleanQuery.getClauses().length - 1) {
                        inner += " ";
                        if (booleanQuery.getClauses()[i+1].getOccur() == BooleanClause.Occur.SHOULD) {
                            inner += "OR ";
                        }
                    }
                    break;
                case MUST:
                    inner += (onlyMust && terse ? "" : "+") + convertQueryToString(currentClause.getQuery(), false)
                             + " ";
                    break;
                case MUST_NOT:
                    inner += "-" + convertQueryToString(currentClause.getQuery(), false) + " ";
                    break;
                default:
                    throw new RuntimeException("Unknown occur: " + currentClause.getOccur());
            }
        }
        return inner;
    }

    private boolean containsOnlyMust(BooleanQuery booleanQuery) {
        for (int i = 0; i < booleanQuery.getClauses().length; i++) {
            if (booleanQuery.getClauses()[i].getOccur() != BooleanClause.Occur.MUST) {
                return false;
            }
        }
        return true;
    }

    private ReplaceReader escaper;
    {
        String PROBLEMS = "!*\\'\"";
        Map<String, String> rules = new HashMap<String, String>(PROBLEMS.length());
        for (int i = 0 ; i < PROBLEMS.length() ; i++) {
            char problem = PROBLEMS.charAt(i);
            rules.put("" + problem, "\\" + problem);
        }
        escaper = ReplaceFactory.getReplacer(rules);
    }
    private String convertSubqueryToString(String field, String text, boolean quote) {
        // Lucene removes back slashes
        String escapedText = text.replaceAll("([^\\\\]) ", "$1\\\\ ");
        if (quote) {
            escapedText = "\"" + escapedText + "\"";
        } else {
            escapedText = escaper.transform(escapedText);
        }
        return (field == null || field.isEmpty()) ? escapedText : field + ":" + escapedText;
    }
}
