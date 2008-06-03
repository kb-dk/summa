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
package dk.statsbiblioteket.summa.common.lucene.search;

import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Vector;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.index.IndexField;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.index.IndexGroup;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexField;

/**
 * This query parser handles expansion of non-qualified query terms and group
 * expansion. Members of groups are OR'ed with the highest boost being used,
 * instead of a combination of the boosts.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te, hal")
public class DisjunctionQueryParser extends QueryParser {
    private static Log log = LogFactory.getLog(DisjunctionQueryParser.class);

    private static float tieBreakerMultiplier = 0.0f;
    private LuceneIndexDescriptor descriptor;

    // TODO: Make this a property
    private boolean disjunctGroups = true;
    private boolean disjunctDefaults = false;

    public DisjunctionQueryParser(LuceneIndexDescriptor descriptor) {
        // TODO: Handle update of analyzer
        super(null, descriptor.getQueryAnalyzer());
        setDefaultOperator(
                descriptor.getDefaultOperator() == IndexDescriptor.OPERATOR.and
                ? Operator.AND : Operator.OR);
        this.descriptor = descriptor;
    }

    protected Query getFieldQuery(String field, final String queryText,
                                  final int slop)
                                                         throws ParseException {
        return getExpanded(field, new InnerQueryMaker() {
            public Query getRecursiveQuery(String fieldOrGroup) throws
                                                                ParseException {
                return getFieldQuery(fieldOrGroup, queryText, slop);
            }
            public Query getFinalQuery(String field) throws ParseException {
                return getFinalFieldQuery(field, queryText, slop);
            }
        });
    }

    /**
     * Handles expansion of groups independend of query-type.<br />
     * If the field is empty, the default fields are used recursively.<br />
     * If the field is a group, the fields in the group are used.<br />
     * If the field is defined and not a group, it is used directly.
     * @param field the field to expand.
     * @param inner how to create the inner Queries.
     * @return a group- ande fefault-field expanded Query.
     * @throws ParseException if the query could not be parsed.
     */
    private Query getExpanded(String field, InnerQueryMaker inner) throws
                                                                ParseException {
        if (log.isTraceEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("getExpanded(" + field + ", " + inner + ") called");
        }
        if (field == null) {
            Collection<Query> subQueries = new ArrayList<Query>(10);
            for (String defaultField : descriptor.getDefaultFields()) {
                Query q = inner.getRecursiveQuery(defaultField);
                if (q != null) {
                    subQueries.add(q);
                }
            }
            if (subQueries.size() == 0) {  // happens for stopwords
                return null;
            }
            return makeMulti(subQueries, disjunctDefaults);
        }
        IndexGroup<LuceneIndexField> group = descriptor.getGroup(field);
        if (group != null) {
            if (log.isTraceEnabled()) {
                log.trace("Expanding group '" + group.getName() + "'");
            }
            List<Query> queries =
                    new ArrayList<Query>(group.getFields().size());
            for (IndexField groupField: group.getFields()) {
                Query q = inner.getFinalQuery(groupField.getName());
                if (q != null) {
                    queries.add(q);
                }
            }
            return makeMulti(queries, disjunctGroups);
        }
        try {
            return inner.getFinalQuery(descriptor.getField(field).getName());
        } catch (IllegalArgumentException e) {
            // TODO: The field is unknown in the descriptor but might be indexed
            return inner.getFinalQuery(field);
        }
    }

    private abstract interface InnerQueryMaker {
        public Query getRecursiveQuery(String fieldOrGroup) throws
                                                                 ParseException;
        public Query getFinalQuery(String field) throws ParseException;
    }

    // Calls super.getFieldQuery and ensures that slop is set if relevant
    private Query getFinalFieldQuery(String field, String queryText, int slop)
                                                         throws ParseException {
        Query query = super.getFieldQuery(field, queryText);
        if (query != null) {
            if (query instanceof PhraseQuery) {
                ((PhraseQuery) query).setSlop(slop);
            }
            if (query instanceof MultiPhraseQuery) {
                ((MultiPhraseQuery) query).setSlop(slop);
            }
        }
        return query;
    }

    /**
     * Create a Query based on the given queries. Depending on the value for
     * disjunct, this will either be a BooleanQuery or a DisjunctionMaxQuery.
     * @param queries  the queries to wrap.
     * @param disjunct if true, a DisjunctMaxQuery is generated.
     *                 If false, a BooleanQuery is generated.
     * @return a Query wrapping queries.
     * @throws ParseException in case of underlying parse errors.
     */
    private Query makeMulti(Collection<Query> queries, boolean disjunct) throws
                                                                ParseException {
        if (queries.size() == 0) {
            return null; // Stopwords?
        }
        if (disjunct) {
            return new DisjunctionMaxQuery(queries, tieBreakerMultiplier);
        }
        //noinspection UseOfObsoleteCollectionType
        Vector clauses = new Vector(queries.size());
        for (Query query: queries) {
            //noinspection unchecked
            clauses.add(new BooleanClause(query, BooleanClause.Occur.SHOULD));
        }
        return getBooleanQuery(clauses, true);
    }

    protected Query getFieldQuery(String field, String queryText) throws
                                                                ParseException {
        return getFieldQuery(field, queryText, 0);
    }


    protected Query getFuzzyQuery(String field, final String termStr,
                                  final float minSimilarity) throws
                                                                ParseException {
        return getExpanded(field, new InnerQueryMaker() {
            public Query getRecursiveQuery(String fieldOrGroup) throws
                                                                ParseException {
                return getFuzzyQuery(fieldOrGroup, termStr, minSimilarity);
            }
            public Query getFinalQuery(String field) throws ParseException {
                return getSuperFuzzyQuery(field, termStr, minSimilarity);
            }
        });
/*        if (field == null) {
            Vector clauses = new Vector();
            for (int i = 0; i < fields.length; i++) {
                clauses.add(new BooleanClause(super.getFuzzyQuery(fields[i], termStr, minSimilarity),
                                              BooleanClause.Occur.SHOULD));
            }
            return getBooleanQuery(clauses, true);
        }
        return super.getFuzzyQuery(field, termStr, minSimilarity);*/
    }
    private Query getSuperFuzzyQuery(String field, final String termStr,
                                     final float minSimilarity) throws
                                                                ParseException {
        return super.getFuzzyQuery(field, termStr, minSimilarity);
    }

    protected Query getPrefixQuery(String field, final String termStr) throws
                                                                ParseException {
        return getExpanded(field, new InnerQueryMaker() {
            public Query getRecursiveQuery(String fieldOrGroup) throws
                                                                ParseException {
                return getPrefixQuery(fieldOrGroup, termStr);
            }
            public Query getFinalQuery(String field) throws ParseException {
                return getSuperPrefixQuery(field, termStr);
            }
        });

/*        if (field == null) {
            Vector clauses = new Vector();
            for (int i = 0; i < fields.length; i++) {
                clauses.add(new BooleanClause(super.getPrefixQuery(fields[i], termStr),
                                              BooleanClause.Occur.SHOULD));
            }
            return getBooleanQuery(clauses, true);
        }
        return super.getPrefixQuery(field, termStr);
        */
    }
    protected Query getSuperPrefixQuery(String field, final String termStr)
            throws ParseException {
        return super.getPrefixQuery(field, termStr);
    }

    protected Query getWildcardQuery(String field, final String termStr) throws
                                                                ParseException {
        return getExpanded(field, new InnerQueryMaker() {
            public Query getRecursiveQuery(String fieldOrGroup) throws
                                                                ParseException {
                return getWildcardQuery(fieldOrGroup, termStr);
            }
            public Query getFinalQuery(String field) throws ParseException {
                return getSuperWildcardQuery(field, termStr);
            }
        });
/*        if (field == null) {
            Vector clauses = new Vector();
            for (int i = 0; i < fields.length; i++) {
                clauses.add(new BooleanClause(super.getWildcardQuery(fields[i], termStr),
                                              BooleanClause.Occur.SHOULD));
            }
            return getBooleanQuery(clauses, true);
        }
        return super.getWildcardQuery(field, termStr);*/
    }
    protected Query getSuperWildcardQuery(String field, String termStr) throws
                                                                ParseException {
        return super.getWildcardQuery(field, termStr);
    }


    protected Query getRangeQuery(String field, final String part1,
                                  final String part2, final boolean inclusive)
                                                         throws ParseException {
        return getExpanded(field, new InnerQueryMaker() {
            public Query getRecursiveQuery(String fieldOrGroup) throws
                                                                ParseException {
                return getRangeQuery(fieldOrGroup, part1, part2, inclusive);
            }
            public Query getFinalQuery(String field) throws ParseException {
                return getSuperRangeQuery(field, part1, part2, inclusive);
            }
        });
/*        if (field == null) {
            Vector clauses = new Vector();
            for (int i = 0; i < fields.length; i++) {
                clauses.add(new BooleanClause(super.getRangeQuery(fields[i], part1, part2, inclusive),
                                              BooleanClause.Occur.SHOULD));
            }
            return getBooleanQuery(clauses, true);
        }
        return super.getRangeQuery(field, part1, part2, inclusive);*/
    }
    protected Query getSuperRangeQuery(String field, final String part1,
                                  final String part2, final boolean inclusive)
                                                         throws ParseException {
        return super.getRangeQuery(field, part1, part2, inclusive);
    }

}
