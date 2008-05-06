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
import java.util.*;

import dk.statsbiblioteket.summa.common.lucene.index.OldIndexField;
import dk.statsbiblioteket.summa.common.lucene.index.SearchDescriptor;
import dk.statsbiblioteket.summa.common.lucene.AnalyzerFactory;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
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

/**
 * The SummaQueryParser is an autoexpanding query parser where an array of
 * defaultfields can be given. This QueryParser is also aware of the
 * SearchDescriptor indirect SummaConfiguration mechanism, making it aware of
 * the fieldGroup principle in Summa.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class SummaQueryParser {

    private static final String START_GROUP = "(";
    private static final String END_GROUP = ")";
    private String[] defaultFields;

    private static final QueryParser.Operator defaultOperator
            = QueryParser.AND_OPERATOR;


    private DisjunctionQueryParser _q;
    private QueryParser _p;

    private Analyzer analyzer;

    private static Log log = LogFactory.getLog(SummaQueryParser.class);

    private Map<String, String[]> expandTag;
    /** Property name for the default fields to query, the value is a comma
     * separated list of field names. */
    public static final String DEFAULT_FIELDS =
            "common.lucene.search.defaultFields";
//    public static final String DEFAULT_FIELDS = "search.fields.default";

    private static final class DisjunctionQueryParser extends QueryParser {
        private String[] fields;
        private  float tieBreakerMultiplier = 0.0f;

        /**
         * Creates a MultiFieldQueryParser.
         *
         * <p>It will, when parse(String query)
         * is called, construct a query like this (assuming the query consists of
         * two terms and you specify the two fields <code>title</code> and <code>body</code>):</p>
         *
         * <code>
         * (title:term1 body:term1) (title:term2 body:term2)
         * </code>
         *
         * <p>When setDefaultOperator(AND_OPERATOR) is set, the result will be:</p>
         *
         * <code>
         * +(title:term1 body:term1) +(title:term2 body:term2)
         * </code>
         *
         * <p>In other words, all the query's terms must appear, but it doesn't matter in
         * what fields they appear.</p>
         */


        public DisjunctionQueryParser(String[] fields, Analyzer analyzer) {
            super(null, analyzer);
            this.fields = fields;
        }

        protected Query getFieldQuery(String field, String queryText, int slop) throws ParseException {
            if (field == null) {
                Collection<Query> subQueries = new Vector<Query>();
                for (int i = 0; i < fields.length; i++) {
                    Query q = super.getFieldQuery(fields[i], queryText);
                    if (q != null) {
                        if (q instanceof PhraseQuery) {
                            ((PhraseQuery) q).setSlop(slop);
                        }
                        if (q instanceof MultiPhraseQuery) {
                            ((MultiPhraseQuery) q).setSlop(slop);
                        }
                        subQueries.add(q);
                    }
                }
                if (subQueries.size() == 0)  // happens for stopwords
                    return null;
                return   new DisjunctionMaxQuery(subQueries,tieBreakerMultiplier);
            }
            return super.getFieldQuery(field, queryText);
        }

        protected Query getFieldQuery(String field, String queryText) throws ParseException {
            return getFieldQuery(field, queryText, 0);
        }


        protected Query getFuzzyQuery(String field, String termStr, float minSimilarity) throws ParseException
        {
            if (field == null) {
                Vector clauses = new Vector();
                for (int i = 0; i < fields.length; i++) {
                    clauses.add(new BooleanClause(super.getFuzzyQuery(fields[i], termStr, minSimilarity),
                                                  BooleanClause.Occur.SHOULD));
                }
                return getBooleanQuery(clauses, true);
            }
            return super.getFuzzyQuery(field, termStr, minSimilarity);
        }

        protected Query getPrefixQuery(String field, String termStr) throws ParseException
        {
            if (field == null) {
                Vector clauses = new Vector();
                for (int i = 0; i < fields.length; i++) {
                    clauses.add(new BooleanClause(super.getPrefixQuery(fields[i], termStr),
                                                  BooleanClause.Occur.SHOULD));
                }
                return getBooleanQuery(clauses, true);
            }
            return super.getPrefixQuery(field, termStr);
        }

        protected Query getWildcardQuery(String field, String termStr) throws ParseException {
            if (field == null) {
                Vector clauses = new Vector();
                for (int i = 0; i < fields.length; i++) {
                    clauses.add(new BooleanClause(super.getWildcardQuery(fields[i], termStr),
                                                  BooleanClause.Occur.SHOULD));
                }
                return getBooleanQuery(clauses, true);
            }
            return super.getWildcardQuery(field, termStr);
        }


        protected Query getRangeQuery(String field, String part1, String part2, boolean inclusive) throws ParseException {
            if (field == null) {
                Vector clauses = new Vector();
                for (int i = 0; i < fields.length; i++) {
                    clauses.add(new BooleanClause(super.getRangeQuery(fields[i], part1, part2, inclusive),
                                                  BooleanClause.Occur.SHOULD));
                }
                return getBooleanQuery(clauses, true);
            }
            return super.getRangeQuery(field, part1, part2, inclusive);
        }




        /**
         * Parses a query which searches on the fields specified.
         * <p>
         * If x fields are specified, this effectively constructs:
         * <pre>
         * <code>
         * (field1:query1) (field2:query2) (field3:query3)...(fieldx:queryx)
         * </code>
         * </pre>
         * @param queries Queries strings to parse
         * @param fields Fields to search on
         * @param analyzer Analyzer to use
         * @throws ParseException if query parsing fails
         * @throws IllegalArgumentException if the length of the queries array differs
         *  from the length of the fields array
         */
        public Query parse(String[] queries, String[] fields,
                           Analyzer analyzer) throws ParseException
        {
            if (queries.length != fields.length)
                throw new IllegalArgumentException("queries.length != fields.length");
            DisjunctionMaxQuery dQuery = new DisjunctionMaxQuery(tieBreakerMultiplier);
            for (int i = 0; i < fields.length; i++)
            {
                QueryParser qp = new QueryParser(fields[i], analyzer);
                Query q = qp.parse(queries[i]);
                dQuery.add(q);
            }
            return dQuery;
        }


        /**
         * Parses a query, searching on the fields specified.
         * Use this if you need to specify certain fields as required,
         * and others as prohibited.
         * <p><pre>
         * Usage:
         * <code>
         * String[] fields = {"filename", "contents", "description"};
         * BooleanClause.Occur[] flags = {BooleanClause.Occur.SHOULD,
         *                BooleanClause.Occur.MUST,
         *                BooleanClause.Occur.MUST_NOT};
         * MultiFieldQueryParser.parse("query", fields, flags, analyzer);
         * </code>
         * </pre>
         *<p>
         * The code above would construct a query:
         * <pre>
         * <code>
         * (filename:query) +(contents:query) -(description:query)
         * </code>
         * </pre>
         *
         * @param query Query string to parse
         * @param fields Fields to search on
         * @param flags Flags describing the fields
         * @param analyzer Analyzer to use
         * @throws ParseException if query parsing fails
         * @throws IllegalArgumentException if the length of the fields array differs
         *  from the length of the flags array
         */
        public  Query parse(String query, String[] fields,
                            BooleanClause.Occur[] flags, Analyzer analyzer) throws ParseException {
            if (fields.length != flags.length)
                throw new IllegalArgumentException("fields.length != flags.length");
            DisjunctionMaxQuery dQuery = new DisjunctionMaxQuery(tieBreakerMultiplier);
            //BooleanQuery bQuery = new BooleanQuery();
            for (String field1 : fields) {
                QueryParser qp = new QueryParser(field1, analyzer);
                Query q = qp.parse(query);
                dQuery.add(q);
            }
            return dQuery;
        }


        /**
         * Parses a query, searching on the fields specified.
         * Use this if you need to specify certain fields as required,
         * and others as prohibited.
         * <p><pre>
         * Usage:
         * <code>
         * String[] query = {"query1", "query2", "query3"};
         * String[] fields = {"filename", "contents", "description"};
         * BooleanClause.Occur[] flags = {BooleanClause.Occur.SHOULD,
         *                BooleanClause.Occur.MUST,
         *                BooleanClause.Occur.MUST_NOT};
         * MultiFieldQueryParser.parse(query, fields, flags, analyzer);
         * </code>
         * </pre>
         *<p>
         * The code above would construct a query:
         * <pre>
         * <code>
         * (filename:query1) +(contents:query2) -(description:query3)
         * </code>
         * </pre>
         *
         * @param queries Queries string to parse
         * @param fields Fields to search on
         * @param flags Flags describing the fields
         * @param analyzer Analyzer to use
         * @throws ParseException if query parsing fails
         * @throws IllegalArgumentException if the length of the queries, fields,
         *  and flags array differ
         */
        public Query parse(String[] queries, String[] fields, BooleanClause.Occur[] flags,
                           Analyzer analyzer) throws ParseException
        {
            if (!(queries.length == fields.length && queries.length == flags.length))
                throw new IllegalArgumentException("queries, fields, and flags array have have different length");
            BooleanQuery bQuery = new BooleanQuery();
            for (int i = 0; i < fields.length; i++)
            {
                QueryParser qp = new QueryParser(fields[i], analyzer);
                Query q = qp.parse(queries[i]);
                bQuery.add(q, flags[i]);
            }
            return bQuery;
        }



    }

    public SummaQueryParser(Configuration configuration) {
        //String[] defaultFields,
        //                    Analyzer analyzer, SearchDescriptor descriptor) {
        SearchDescriptor descriptor = new SearchDescriptor(configuration);
        analyzer = AnalyzerFactory.buildAnalyzer(configuration);
        
        List<String> confFields = configuration.getStrings(DEFAULT_FIELDS);
        ArrayList<String> defF = new ArrayList<String>(confFields.size()*2);
        for (String s : confFields){
            if (descriptor.getGroups().containsKey(s)){
                for (OldIndexField idf: descriptor.getGroups().get(s).getFields()){
                    defF.add(idf.getName());
                }
            } else {
                defF.add(s);
            }
        }
        defaultFields = defF.toArray(new String[]{});
        setDefaultFields(defaultFields);

        expandTag = new HashMap<String, String[]>();
        HashMap<String, Set<String>> help = new HashMap<String, Set<String>>();

        for (SearchDescriptor.Group g : descriptor.getGroups().values()) {
            String name = g.getName();
            if (!help.containsKey(name)) {
                help.put(name, new HashSet<String>());
            }
            for (OldIndexField f : g.getFields()) {
                help.get(name).add(f.getName());
            }

        }

        for (Map.Entry<String, Set<String>> me : help.entrySet()) {
            expandTag.put(me.getKey(), me.getValue().toArray(new String[]{}));
        }
    }



    // needds to handle unbalanced queries bug # 2
    private static final class QueryBalancer {

        private Stack<Integer> balance;

        QueryBalancer() {
            balance = new Stack<Integer>();
        }

        void addToken(Token t) throws ParseException {

            switch (t.kind) {
                case QueryParserConstants.LPAREN:
                    balance.add(t.kind);
                    break;
                case QueryParserConstants.RANGEIN_START:
                    balance.add(t.kind);
                    break;
                case QueryParserConstants.RANGEEX_START:
                    balance.add(t.kind);
                    break;
                case QueryParserConstants.RANGEIN_END:
                    if (balance.isEmpty() || balance.peek()
                                             != QueryParserConstants
                            .RANGEIN_START) {
                        throw new ParseException("Unbalanced query near: "
                                                 + t.image + "<" + t
                                .beginColumn + "," + t.beginLine + ">");
                    }
                    balance.pop();
                    break;
                case QueryParserConstants.RANGEEX_END:
                    if (balance.isEmpty() || balance.peek()
                                             != QueryParserConstants
                            .RANGEEX_START) {
                        throw new ParseException("Unbalanced query near: "
                                                 + t.image + "<" + t
                                .beginColumn + "," + t.beginLine + ">");
                    }
                    balance.pop();
                    break;
                case QueryParserConstants.RPAREN:
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


    public SummaQueryParser(String[] defaultFields,
                            Analyzer analyzer, SearchDescriptor descriptor) {

        this.analyzer = analyzer;
        ArrayList<String> defF = new ArrayList<String>();

        for (String s : defaultFields){
            if (descriptor.getGroups().containsKey(s)){
                for (OldIndexField idf :  descriptor .getGroups().get(s).getFields()){
                    defF.add(idf.getName());
                }
            } else {
                defF.add(s);
            }
        }

        this.defaultFields = defF.toArray(new String[]{});

        expandTag = new HashMap<String, String[]>();
        HashMap<String, Set<String>> help = new HashMap<String, Set<String>>();

        for (SearchDescriptor.Group g : descriptor.getGroups().values()) {
            String name = g.getName();
            if (!help.containsKey(name)) {
                help.put(name, new HashSet<String>());
            }
            for (OldIndexField f : g.getFields()) {
                help.get(name).add(f.getName());
            }

        }

        for (Map.Entry<String, Set<String>> me : help.entrySet()) {
            expandTag.put(me.getKey(), me.getValue().toArray(new String[]{}));
        }
        setDefaultFields(this.defaultFields);
    }


    /**
     * @param queryString
     * @return the query Object
     * @throws ParseException
     */
    public synchronized Query parse(String queryString) throws ParseException {
        try{
            String qstr = expandQueryString(queryString);
            log.info("expanded query: " + qstr);

            Query a = _q.parse(qstr);


            log.info("query: " + a.toString());
            return a;
        } catch (Exception e){
            log.warn("Exception in SummaQueryParser.parse:" + e);
            throw new ParseException();
        }
    }


    public synchronized Query parse(String queryString,
                                    QueryParser.Operator operator)
            throws ParseException {
        String qstr;
        try {
            qstr = expandQueryString(queryString);
            _q.setDefaultOperator(operator);
            Query q = _q.parse(qstr);
            log.info("query: " + q.toString());
            _q.setDefaultOperator(defaultOperator);
            return q;
        } catch (Exception e) {
            log.warn("Exception in SummaQueryParser.parse:" + e);
            throw new ParseException();
        } finally {
            _q.setDefaultOperator(defaultOperator);
        }
    }


    private String expandQueryString(String query) throws ParseException {

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
                    currentFields = getFields(value.image);
                } else {
                    //  currentFields = new String[]{defaultField};
                    currentFields = new String[]{};
                }
            } else if (!infield) {
                if (value != null) {
                    returnval += " " + value.image;
                }
            } else {
                //add to field value
                currentFieldValue += value.image;
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


    private String[] getFields(String field) {
        String [] a;
        return (a = expandTag.get(field)) != null ? a : new String[]{field};
    }

    public void setDefaultFields(String[] defaultFields) {
        this.defaultFields = defaultFields;
        _q = new DisjunctionQueryParser(this.defaultFields, analyzer);
        _q.setDefaultOperator(defaultOperator);
    }



}