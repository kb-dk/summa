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
package dk.statsbiblioteket.summa.common.lucene.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParserConstants;
import org.apache.lucene.queryParser.Token;
import org.apache.lucene.search.*;

import java.io.StringWriter;
import java.util.Iterator;
import java.util.Stack;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te, hal",
        comment = "Needs class JavaDoc")
public class SummaQueryParser {
    private static Log log = LogFactory.getLog(SummaQueryParser.class);

    /**
     * If true, query time boosts on fields are enabled. Field-boosts are
     * given in the query-string as defined by the class {@link LuceneBooster}.
     */
    public static final String CONF_QUERY_TIME_FIELD_BOOSTS =
            "summa.common.queryparser.querytimefieldboosts";
    public static final boolean DEFAULT_QUERY_TIME_FIELD_BOOSTS = true;
    private boolean supportQueryTimeBoosts = DEFAULT_QUERY_TIME_FIELD_BOOSTS;

//    private static final String START_GROUP = "(";
//    private static final String END_GROUP = ")";

    private DisjunctionQueryParser parser;
    private LuceneBooster booster;
    private LuceneIndexDescriptor descriptor;


    // needds to handle unbalanced queries bug # 2

    @QAInfo(level = QAInfo.Level.FINE,
            state = QAInfo.State.QA_NEEDED,
            author = "hal")
    static final class QueryBalancer {

        private Stack<Integer> balance;

        QueryBalancer() {
            balance = new Stack<Integer>();
        }
        String UNBALANCED = "Unbalanced query near: ";
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
                        throw new ParseException(UNBALANCED
                                                 + t.image + "<" + t
                                .beginColumn + "," + t.beginLine + ">");
                    }
                    balance.pop();
                    break;
                case QueryParserConstants.RANGEEX_END: // }
                    if (balance.isEmpty() || balance.peek()
                                             != QueryParserConstants
                            .RANGEEX_START) {
                        throw new ParseException(UNBALANCED
                                                 + t.image + "<" + t
                                .beginColumn + "," + t.beginLine + ">");
                    }
                    balance.pop();
                    break;
                case QueryParserConstants.RPAREN: // )
                    if (balance.isEmpty()
                        || balance.peek() != QueryParserConstants.LPAREN) {
                        throw new ParseException(UNBALANCED
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

    /**
     * Create a parser based on the given descriptor. It is recommended to use
     * the constructor {@link SummaQueryParser(Configuration, IndexDescriptor)}
     * instead as it allows for customization of the parser.
     * @param descriptor the index descriptor.
     */
    public SummaQueryParser(LuceneIndexDescriptor descriptor) {
        log.debug("Creating query parser with given descriptor " + descriptor);
        init(descriptor);
    }

    /**
     * Create a query parser from the given configuration. This includes the
     * construction of an {@link IndexDescriptor}. For performance and
     * stability reasons, it is recommended to share index descriptors, so it
     * is recommended to use the constructor
     * {@link SummaQueryParser(Configuration, IndexDescriptor)} instead.
     * @param conf the configuration for the parser and corresponding index
     *             descriptor.
     */
    public SummaQueryParser(Configuration conf) {
        log.debug("Creating query parser with descriptor specified "
                  + "in configuration");
        init(LuceneIndexUtils.getDescriptor(conf));
        extractSetup(conf);
    }

    /**
     * Create a query parser with the given configuration and descriptor.
     * This is the recommended way of constructing the parser as is allows for
     * reuse and customizability.
     * @param conf       the configuration for the parser.
     * @param descriptor the index descriptor.
     */
    public SummaQueryParser(Configuration conf,
                            LuceneIndexDescriptor descriptor) {
        log.debug("Creating query parser with configuration and descriptor");
        init(descriptor);
        log.trace("Determining query time boosts");
        extractSetup(conf);
        log.trace("Finished setup");
    }

    private void extractSetup(Configuration conf) {
        supportQueryTimeBoosts = conf.getBoolean(CONF_QUERY_TIME_FIELD_BOOSTS,
                                                 supportQueryTimeBoosts);
        //noinspection DuplicateStringLiteralInspection
        log.debug("Query time boosts are "
                  + (supportQueryTimeBoosts ? "enabled" : "disabled"));
    }

    public void init(LuceneIndexDescriptor descriptor) {
        this.descriptor = descriptor;
        booster = new LuceneBooster(descriptor);
        parser = new DisjunctionQueryParser(descriptor);
    }

    /**
     * Parse the given String and return a Lucene Query from it.
     * </p><p>
     * see http://lucene.apache.org/java/docs/queryparsersyntax.html for syntax.
     * @param queryString a String with Lucene query syntax.
     * @return the expanded query.
     * @throws ParseException if the query could not be parsed.
     */
    public synchronized Query parse(String queryString) throws ParseException {
        long startTime = System.nanoTime();
        String boosts = null;
        if (supportQueryTimeBoosts) {
            try {
                String[] tokens = booster.splitQuery(queryString);
                queryString = tokens[0];
                boosts = tokens[1];
            } catch (Exception e) {
                log.error("Exception handling query-time boost", e);
            }
        }
        //String qstr = expandQueryString(queryString);
        //log.debug("expanded query: " + qstr);
        Query query = parser.parse(queryString);
//        Query query = parser.parse(qstr);


        if (log.isTraceEnabled()) {
            log.trace("Parsed query (" + query + ") in "
                      + (System.nanoTime() - startTime) / 1000000D + "ms: "
                      + query.toString());
        }

        booster.applyDescriptorBoosts(query);

        if (supportQueryTimeBoosts) {
            long boostStartTime = System.nanoTime();
            try {
                booster.applyBoost(query, boosts);
            } catch (Exception e) {
                log.error("Exception applying query-time boost", e);
            }
            if (log.isTraceEnabled()) {
                log.trace("Applied boost in "
                          + (System.nanoTime() - boostStartTime)  / 1000000D
                          + "ms");
            }
        }

        if (log.isDebugEnabled()) {
            try {
                log.debug("Fully parsed and boosted query in "
                          + (System.nanoTime() - startTime) / 1000000D
                          + "ms: " + LuceneIndexUtils.queryToString(query));
            } catch (Exception e) {
                log.error("Could not dump fully parsed and boosted query to" 
                          + " String", e);
            }
        }
        return query;
    }

    /*protected String expandQueryString(String query) throws ParseException {

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
                if (fieldBalance.isBalanced() &&
                    value.kind != QueryParserConstants.COLON) {
                    //end of field
                    infield = false;
                    if (currentFields != null) {
                        returnval += expgrp(currentFields, currentFieldValue)
                                     + END_GROUP;
                    } else {
                        log.debug("expandQueryString: currentFields == null at"
                                  + " EOField");
                    }
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
                    if (currentFields != null) {
                        returnval += expgrp(currentFields, currentFieldValue)
                                     + END_GROUP;
                    } else {
                        log.debug("expandQueryString: currentFields == null at"
                                  + " EOFile");
                    }
                }
                return returnval;
            }
        }
        //Will never reach this point
        log.error("expandQueryString: Unexpected while-loop exit");
        return null;
    }*/

/*    private static String expgrp(String[] fields, String val) {
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
  */
    /**
     * @see {@link LuceneBooster#splitQuery(String)}.
     * @return true if query-time field-level boosts is supported.
     */
    public boolean isSupportQueryTimeBoosts() {
        return supportQueryTimeBoosts;
    }

    /**
     * @see {@link LuceneBooster#splitQuery(String)}.
     * @param supportQueryTimeBoosts set to true if query-time field-level
     *                               boosts should be supported.
     */
    public void setSupportQueryTimeBoosts(boolean supportQueryTimeBoosts) {
        this.supportQueryTimeBoosts = supportQueryTimeBoosts;
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
        } else if (query instanceof ConstantScoreRangeQuery) {
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

    public LuceneIndexDescriptor getDescriptor() {
        return descriptor;
    }
}



