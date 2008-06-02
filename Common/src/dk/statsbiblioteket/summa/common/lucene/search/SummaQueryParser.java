/* $Id: SummaQueryParser.java,v 1.3 2007/10/04 13:28:22 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/04 13:28:22 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
package dk.statsbiblioteket.summa.common.lucene.search;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import dk.statsbiblioteket.summa.common.lucene.index.OldIndexField;
import dk.statsbiblioteket.summa.common.lucene.index.SearchDescriptor;
import dk.statsbiblioteket.summa.common.lucene.AnalyzerFactory;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexField;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.index.IndexField;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.index.IndexGroup;
import dk.statsbiblioteket.util.qa.QAInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.FastCharStream;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.QueryParserConstants;
import org.apache.lucene.queryParser.QueryParserTokenManager;
import org.apache.lucene.queryParser.Token;
import org.apache.lucene.search.*;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te, hal",
        comment="Needs class JavaDoc")
public class SummaQueryParser {
    private static Log log = LogFactory.getLog(SummaQueryParser.class);

    private static final String START_GROUP = "(";
    private static final String END_GROUP = ")";

    private DisjunctionQueryParser _q;

    private IndexDescriptor descriptor;

    // needds to handle unbalanced queries bug # 2
    static final class QueryBalancer {

        private Stack<Integer> balance;

        QueryBalancer() {
            balance = new Stack<Integer>();
        }

        void addToken(Token t) throws ParseException {
            switch (t.kind) {
                case QueryParserConstants.LPAREN:       // (
                    balance.add(t.kind);
                    break;
                case QueryParserConstants.RANGEIN_START: // [
                    balance.add(t.kind);
                    break;
                case QueryParserConstants.RANGEEX_START: // {
                    balance.add(t.kind);
                    break;
                case QueryParserConstants.RANGEIN_END: // ]
                    if (balance.isEmpty() ||
                        balance.peek() != QueryParserConstants.RANGEIN_START) {
                        throw new ParseException("Unbalanced query near: "
                                                 + t.image + "<" + t
                                .beginColumn + "," + t.beginLine + ">");
                    }
                    balance.pop();
                    break;
                case QueryParserConstants.RANGEEX_END: // }
                    if (balance.isEmpty() || balance.peek()
                                             != QueryParserConstants
                            .RANGEEX_START) {
                        throw new ParseException("Unbalanced query near: "
                                                 + t.image + "<" + t
                                .beginColumn + "," + t.beginLine + ">");
                    }
                    balance.pop();
                    break;
                case QueryParserConstants.RPAREN: // )
                    if (balance.isEmpty()
                        || balance.peek() != QueryParserConstants.LPAREN) {
                        throw new ParseException("Unbalanced query near: "
                                                 + t.image + "<" + t
                                .beginColumn + "," + t.beginLine + ">");
                    }
                    balance.pop();
                    break;
                default:
            }


        }

        boolean isBalanced() {
            return balance.isEmpty();
        }
    }

    private boolean supportQueryTimeBoosts = false;

    public SummaQueryParser(IndexDescriptor descriptor) {
        // TODO: Add option for query time boosts
        log.debug("Creating query parser with given descriptor " + descriptor);
        this.descriptor = descriptor;
    }

    public SummaQueryParser(Configuration conf) {
        log.debug("Creating query parser with descriptor specified "
                  + "in configuration");
        descriptor = LuceneIndexUtils.getDescriptor(conf);
    }


    /**
     * Parse the given String and return a Lucene Query from it.
     * </p><p>
     * see http://lucene.apache.org/java/docs/queryparsersyntax.html for syntax.
     * @param queryString       a String with Lucene query syntax.
     * @return the expanded query.
     * @throws ParseException if the query could not be parsed.
     */
    public synchronized Query parse(String queryString) throws ParseException {
        String boosts = null;
        if (supportQueryTimeBoosts) {
            try {
                String[] tokens = splitQuery(queryString);
                queryString = tokens[0];
                boosts = tokens[1];
            } catch (Exception e) {
                log.error("Exception handling query-time boost", e);
            }
        }
        String qstr = expandQueryString(queryString);
        log.info("expanded query: " + qstr);
        Query a = _q.parse(queryString);
//        Query a = _q.parse(qstr);
        log.info("query: " + a.toString());
        if (supportQueryTimeBoosts) {
            try {
                applyBoost(a, boosts, descriptor);
            } catch (Exception e) {
                log.error("Exception applying query-time boost", e);
            }
        }
        if (log.isDebugEnabled()) {
            try {
                log.debug("Parsed query: " + queryToString(a));
            } catch (Exception e) {
                log.error("Could not dump parsed query to String", e);
            }
        }
        return a;
    }
    // TODO: Fix boosting of unqualified fields

    public synchronized Query parse(String queryString, QueryParser.Operator operator)
            throws ParseException {
        String qstr;
        try {
            String boosts = null;
            if (supportQueryTimeBoosts) {
                try {
                    String[] tokens = splitQuery(queryString);
                    queryString = tokens[0];
                    boosts = tokens[1];
                } catch (Exception e) {
                    log.error("Exception handling query-time boost", e);
                }
            }
            qstr = expandQueryString(queryString);
            _q.setDefaultOperator(operator);
            Query q = _q.parse(queryString);
//            Query q = _q.parse(qstr);
            log.info("query: " + q.toString());
            if (supportQueryTimeBoosts) {
                try {
                    applyBoost(q, boosts, descriptor);
                } catch (Exception e) {
                    log.error("Exception applying query-time boost", e);
                }
            }
            if (log.isDebugEnabled()) {
                try {
                    log.debug("Parsed query: " + queryToString(q));
                } catch (Exception e) {
                    log.error("Could not dump parsed query to String", e);
                }
            }
            return q;
        } catch (Throwable t) {
            throw (ParseException)
                    new ParseException("Exception during parse").initCause(t);
        }
    }


    protected String expandQueryString(String query) throws ParseException {

        QueryParserTokenManager TokenManager = new QueryParserTokenManager(
                new FastCharStream(new StringReader(query)));

        String returnval = "";

        Token lookahead;
        Token value = null;

        boolean infield = false;
        String[] currentFields = null;
        String currentFieldValue = "";

        int lastEnd = 0;

        QueryBalancer balance = new QueryBalancer();
        QueryBalancer fieldBalance = null;
        //this is equal to while(true);
        while ((lookahead = TokenManager.getNextToken()) != null) {
            balance.addToken(lookahead);
            //Start of fields
            if (lookahead.kind == QueryParserConstants.COLON) {
                //We have found a colon, and start a new balancer to check this field
                if (infield) {
                    throw new ParseException(
                            "Found : while already in field context <" + lookahead
                                    .beginColumn + "," + lookahead.beginLine + ">");
                }
                infield = true;
                fieldBalance = new QueryBalancer();
                returnval += START_GROUP;
                if (value != null) {
                       // TODO: Implement this
                    // currentFields = getFields(value.image);
                } else {
                    //  currentFields = new String[]{defaultField};
                    currentFields = new String[]{};
                }
            } else if (!infield) {
                if (value != null) {
                    returnval += " " + value.image;
//                    returnval += " " + value.image;
                }
            } else {
                //add to field value
                if (value != null) {
                    currentFieldValue += value.image;
                }
                //copy spaces between elements
                if (lookahead.beginColumn > lastEnd) {
                    currentFieldValue += " ";
                }
                if (fieldBalance.isBalanced() && value.kind != QueryParserConstants.COLON) {
                    //end of field
                    infield = false;
                    returnval += expgrp(currentFields, currentFieldValue)
                                 + END_GROUP;
                    currentFields = null;
                    currentFieldValue = "";
                } else {
                    fieldBalance.addToken(lookahead);
                }
            }

            value = lookahead;
            lastEnd = value.endColumn;

            //At end of file, add rest
            if (lookahead.kind == QueryParserConstants.EOF) {
                if (infield) {
                    returnval += expgrp(currentFields, currentFieldValue)
                                 + END_GROUP;
                }
                return returnval;
            }
        }
        //Will never reach this point
        log.error("expandQueryString: Unexpected while-loop exit");
        return null;
    }

    private static String expgrp(String[] fields, String val) {
        String r = "";
        val = val.substring(val.indexOf(":"));
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                r += " OR ";
            }
            r += fields[i] + val;
            if (i < fields.length - 1) {
                r += " ";
            }
        }
        return r;
    }


    /* Query time boosts */

    /**
     * @see {@link #splitQuery}
     * @return true if query-time field-level boosts is supported.
     */
    public boolean isSupportQueryTimeBoosts() {
        return supportQueryTimeBoosts;
    }

    /**
     * @see {@link #splitQuery}
     * @param supportQueryTimeBoosts set to true if query-time field-level
     *                               boosts should be supported.
     */
    public void setSupportQueryTimeBoosts(boolean supportQueryTimeBoosts) {
        this.supportQueryTimeBoosts = supportQueryTimeBoosts;
    }


    public static final Pattern boostPattern =
            Pattern.compile("^(.+)boost\\((.+)\\)$");
    /**
     * Splits the given query into the standard query and any field-boost
     * parameters.
     * </p><p>
     * The format for field-boost parameters is<br />
     * normalquery "boost("(fieldname"^"boostfactor)*")"
     * </p><p>
     * Example 1: "heste boost(title^3.5)" => " heste"<br />
     * Example 1: "heste boost(title^0.5 emne^4)" => " heste"<br />
     * Example 2: "galimafry foglio" => " galimafry foglio"<br />
     * @param query a query as provided by the end-user
     * @return the query and the field-boosts. The query is always something,
     *         the field-boosts is null if they are not defined in the input.
     */
    public static String[] splitQuery(String query) {
        if (log.isTraceEnabled()) {
            log.trace("splitQuery(" + query + ") called");
        }
        Matcher matcher = boostPattern.matcher(query);
        if (matcher.matches()) {
            return new String[]{matcher.group(1), matcher.group(2)};
        }
        return new String[]{query, null};
    }

    public static final Pattern singleBoost =
            Pattern.compile("^(.+)\\^([0-9]+(\\.[0-9]+)?)$");
    /**
     * Extract field-specific boosts from boost and apply them recursively to
     * Query, thereby turning field-boosts into term-boosts.
     * </p><p>
     * The format for boost is<br />
     * {@code (field"^"boost )*(field"^"boost)?}
     * </p><p>
     * Example 1: "title^2.9"<br />
     * Example 2: "title^0.5 emne^4"
     * @param query       a standard query.
     * @param boostString boost-specific parameters.
     * @param descriptor  the descriptor used for group-expansion.
     * @return true if at least one boost was applied.
     */
    public static boolean applyBoost(Query query, String boostString,
                                     IndexDescriptor descriptor) {
        log.trace("applyBoost(" + query + ", " + boostString + ") entered");
        if (boostString == null) {
            return false;
        }
        String[] boostTokens = boostString.split("\\s+");
        if (boostTokens.length == 0) {
            log.debug("No boosts defined in '" + boostString + "'. Returning");
            return false;
        }
        Map<String, Float> boosts =
                new HashMap<String, Float>(boostTokens.length);
        for (String boost: boostTokens) {
            Matcher matcher = singleBoost.matcher(boost);
            if (!matcher.matches()) {
                log.warn("Illegal boost '" + boost + "' in '" + query
                         + boostString + "'. Aborting boosting");
                return false;
            }
            try {
                boosts.put(matcher.group(1), Float.valueOf(matcher.group(2)));
            } catch (NumberFormatException e) {
                log.warn("Illegal float-value in '" + boost + "'. Aborting");
                return false;
            }
        }
        if (boosts.size() == 0) {
            log.debug("No boosts detected in " + boostString);
            return false;
        }
        return applyBoost(query, boosts, descriptor);
    }

    private static boolean applyBoost(Query query, Map<String, Float> boosts,
                                      IndexDescriptor descriptor) {
        log.trace("applyBoost(Query, Map) entered");
        expandBoosts(boosts, descriptor);
        boolean applied = false;
        if (query instanceof BooleanQuery) {
            log.trace("applyBoost: BooleanQuery found");
            for (BooleanClause clause: ((BooleanQuery)query).getClauses()) {
                applied = applied | applyBoost(clause.getQuery(), boosts,
                                               descriptor);
            }
        } else if (query instanceof TermQuery) {
            log.trace("applyBoost: termQuery found");
            TermQuery termQuery = (TermQuery)query;
            if (boosts.containsKey(termQuery.getTerm().field())) {
                Float boost = boosts.get(termQuery.getTerm().field());
                log.debug("applyBoost: Assigning boost " + boost
                          + " to TermQuery " + termQuery.getTerm());
                termQuery.setBoost(termQuery.getBoost() * boost);
                applied = true;
            }
        } else if (query instanceof DisjunctionMaxQuery) {
            log.trace("applyBoost: DisjunctionMaxQuery found");
            Iterator iterator = ((DisjunctionMaxQuery)query).iterator();
            while (iterator.hasNext()) {
                applied = applied | applyBoost((Query)iterator.next(), boosts,
                                               descriptor);
            }
        } else if (query instanceof ConstantScoreQuery) {
            log.trace("applyBoost: ConstantScoreQuery ignored");
        } else if (query instanceof ConstantScoreRangeQuery) {
            log.trace("applyBoost: ConstantScoreRangeQuery ignored");
        } else if (query instanceof RangeQuery) {
            log.trace("applyBoost: RangeQuery ignored");
        } else {
            log.warn("applyBoost: Unexpected Query '" + query.getClass()
                     + "' ignored");
        }
        log.trace("applyBoost(Query, Map) exited");
        return applied;
    }

    /**
     * Expand groups with boosts so that fields in the group gets the boost.
     * Note: Boosts on groups have lower priority that field-specific boosts.
     * @param boosts     a map with boosts for fields.
     * @param descriptor description of the index-view.
     */
    public static void expandBoosts(Map<String, Float> boosts,
                                    IndexDescriptor descriptor) {
        Map<String, Float> extras = new HashMap<String, Float>(boosts.size()*2);
        for (Map.Entry<String, Float> entry: boosts.entrySet()) {
            expandBoosts(entry.getKey(), entry.getValue(), extras, descriptor);
        }
        for (Map.Entry<String, Float> entry: extras.entrySet()) {
            if (!boosts.containsKey(entry.getKey())) {
                boosts.put(entry.getKey(), entry.getValue());
            }
        }

    }

    private static void expandBoosts(String fieldOrGroup, Float boost,
                                     Map<String, Float> extras,
                                     IndexDescriptor descriptor) {
        // TODO: Implement this
        if (descriptor.getGroups().containsKey(fieldOrGroup)) {
/*            IndexGroup group = descriptor.getGroups().get(fieldOrGroup);
            for (IndexField field: group.getFields()) {
                log.trace("expandBoost: added boost " + boost + " to "
                          + field.getName() + " in group " + fieldOrGroup);
                extras.put(field.getName(), boost);
            }*/
        }
    }

    /**
     * Parses a Query-tree and returns it as a human-readable String. This
     * dumper writes custom boosts. Not suitable to feed back into a parser!
     * @param query the query to dump as a String.
     * @return the query as a human-redable String.
     */
    // TODO: Make this dumper more solid - let it handle all known Queries
    public static String queryToString(Query query) {
        StringWriter sw = new StringWriter(100);
        if (query instanceof BooleanQuery) {
            sw.append("(");
            boolean first = true;
            for (BooleanClause clause: ((BooleanQuery)query).getClauses()) {
                if (!first) {
                    sw.append(" ");
                }
                sw.append(clause.getOccur().toString());
                sw.append(queryToString(clause.getQuery()));
                first = false;
            }
            sw.append(")");
        } else if (query instanceof TermQuery) {
            TermQuery termQuery = (TermQuery)query;
            sw.append(termQuery.toString()).append("[");
            sw.append(Float.toString(query.getBoost())).append("]");
        } else if (query instanceof RangeQuery) {
            sw.append(query.toString()).append("[");
            sw.append(Float.toString(query.getBoost())).append("]");
        } else if (query instanceof WildcardQuery) {
            sw.append(query.toString()).append("[");
            sw.append(Float.toString(query.getBoost())).append("]");
        } else if (query instanceof FuzzyQuery) {
            sw.append(query.toString()).append("[");
            sw.append(Float.toString(query.getBoost())).append("]");
        } else if (query instanceof PrefixQuery) {
            sw.append(query.toString()).append("[");
            sw.append(Float.toString(query.getBoost())).append("]");
        } else if (query instanceof PhraseQuery) {
            sw.append(query.toString()).append("[");
            sw.append(Float.toString(query.getBoost())).append("]");
        } else if (query instanceof DisjunctionMaxQuery) {
            Iterator iterator = ((DisjunctionMaxQuery)query).iterator();
            sw.append("<");
            boolean first = true;
            while (iterator.hasNext()) {
                if (!first) {
                    sw.append(" ");
                }
                sw.append(queryToString((Query)iterator.next()));
                first = false;
            }
            sw.append(">");
        } else {
            sw.append(query.getClass().toString());
            sw.append(query.toString()).append("[");
            sw.append(Float.toString(query.getBoost())).append("]");
        }
        return sw.toString();
    }


}