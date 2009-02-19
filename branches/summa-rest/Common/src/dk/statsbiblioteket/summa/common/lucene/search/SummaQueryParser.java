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

import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.util.qa.QAInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.lucene.queryParser.FastCharStream;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParserConstants;
import org.apache.lucene.queryParser.QueryParserTokenManager;
import org.apache.lucene.queryParser.Token;
import org.apache.lucene.search.*;

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
    public static final boolean DEFAULT_QUERY_TIME_FIELD_BOOSTS = false;
    private boolean supportQueryTimeBoosts = DEFAULT_QUERY_TIME_FIELD_BOOSTS;

    private static final String START_GROUP = "(";
    private static final String END_GROUP = ")";

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
        extractSetup(conf);
    }

    private void extractSetup(Configuration conf) {
        supportQueryTimeBoosts = conf.getBoolean(CONF_QUERY_TIME_FIELD_BOOSTS,
                                                 supportQueryTimeBoosts);
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
        Query a = parser.parse(queryString);
//        Query a = parser.parse(qstr);


        log.debug("Parsed query (" + a.getClass() + "): " + a.toString());

        if (supportQueryTimeBoosts) {
            try {
                booster.applyBoost(a, boosts);
            } catch (Exception e) {
                log.error("Exception applying query-time boost", e);
            }
        }

        if (log.isDebugEnabled()) {
            try {
                log.debug("Boosted query: "
                          + LuceneIndexUtils.queryToString(a));
            } catch (Exception e) {
                log.error("Could not dump boosted query to String", e);
            }
        }
        return a;
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
}


