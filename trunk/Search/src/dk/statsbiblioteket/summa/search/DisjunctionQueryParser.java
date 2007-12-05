/* $Id: DisjunctionQueryParser.java,v 1.4 2007/10/11 12:56:24 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/11 12:56:24 $
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
package dk.statsbiblioteket.summa.search;

import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.*;

import java.util.Vector;
import java.util.Collection;

import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class DisjunctionQueryParser extends QueryParser {


  private String[] fields;
  private static float tieBreakerMultiplier = 0.0f;

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
  public static Query parse(String[] queries, String[] fields,
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
  public static Query parse(String query, String[] fields,
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
  public static Query parse(String[] queries, String[] fields, BooleanClause.Occur[] flags,
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
