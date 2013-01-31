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

import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.index.IndexGroup;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexField;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Version;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

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

    @SuppressWarnings({"FieldCanBeLocal"})
    private static float tieBreakerMultiplier = 0.0f;
    private LuceneIndexDescriptor descriptor;

    // TODO: Make this a property
    @SuppressWarnings({"FieldCanBeLocal"})
    private boolean disjunctGroups = true;
    @SuppressWarnings({"FieldCanBeLocal"})
    private boolean disjunctDefaults = false;

    public DisjunctionQueryParser(LuceneIndexDescriptor descriptor) {
        // TODO: Handle update of analyzer
        super(Version.LUCENE_30, null, descriptor.getQueryAnalyzer());
        setDefaultOperator(descriptor.getDefaultOperator() == IndexDescriptor.OPERATOR.and ?
                           Operator.AND : Operator.OR);
        this.descriptor = descriptor;
    }

    @Override
    protected Query getFieldQuery(String field, final String queryText, final int slop) throws ParseException {
        return getExpanded(field, new InnerQueryMaker() {
            @Override
            public Query getRecursiveQuery(String fieldOrGroup) throws ParseException {
                return getFieldQuery(fieldOrGroup, queryText, slop);
            }
            @Override
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
    private Query getExpanded(String field, InnerQueryMaker inner) throws ParseException {
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
            if (subQueries.isEmpty()) {  // happens for stopwords
                return null;
            }
            return makeMulti(subQueries, disjunctDefaults);
        }
        IndexGroup<LuceneIndexField> group = descriptor.getGroup(field);
        if (group != null) {
            if (log.isTraceEnabled()) {
                log.trace("Expanding group '" + group.getName() + "'");
            }
            List<Query> queries = new ArrayList<Query>(group.getFields().size());
            for (LuceneIndexField groupField: group.getFields()) {
                Query q = inner.getFinalQuery(groupField.getName());
                if (q != null) {
                    queries.add(q);
                }
            }
            return makeMulti(queries, disjunctGroups);
        }
        LuceneIndexField resolvedField = descriptor.getField(field);
        if (resolvedField == null) {
            // TODO: The field is unknown in the descriptor but might be indexed
            return inner.getFinalQuery(field);
        }

        // Note: This was introduced to support dynamic fields. It _should_ not collide with existing usage
        return inner.getFinalQuery(field);
        //return inner.getFinalQuery(resolvedField.getName());
    }

    private abstract interface InnerQueryMaker {
        /**
         * If the original field was a null, this method is called for every default field. Implementations should
         * perform a recursive expansion based on the received field as it is possible that it is a group.
         * @param fieldOrGroup designation for a field or group.
         * @return a Query expanded with the given field or group.
         * @throws ParseException if the query could not be processed.
         */
        public Query getRecursiveQuery(String fieldOrGroup) throws ParseException;

        /**
         * This method is called only with fully resolved field names and should produce a query based on that.
         * @param field a fully resolved field name, expected to be present in the index.
         * @return a query based on the field.
         * @throws ParseException if the query could not be processed.
         */
        public Query getFinalQuery(String field) throws ParseException;
    }

    // Calls super.getFieldQuery and ensures that slop is set if relevant
    private Query getFinalFieldQuery(String field, String queryText, int slop) throws ParseException {
        Query query;
        try {
            query = super.getFieldQuery(field, queryText, slop);
        } catch(NullPointerException e) {
            ParseException pe = new ParseException(
                "Got NullPointerException while calling getFieldQuery('" + field + "', '" + queryText + "')");
            pe.initCause(e);
            throw pe;
        }
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
        if (queries.isEmpty()) {
            return null; // Stopwords?
        }
        if (disjunct) {
            return new DisjunctionMaxQuery(queries, tieBreakerMultiplier);
        }
        //noinspection UseOfObsoleteCollectionType
        Vector<BooleanClause> clauses = new Vector<BooleanClause>(queries.size());
        for (Query query: queries) {
            //noinspection unchecked
            clauses.add(new BooleanClause(query, BooleanClause.Occur.SHOULD));
        }
        return getBooleanQuery(clauses, true);
    }

/*    @Override
    protected Query getFieldQuery(String field, String queryText) throws
                                                                ParseException {
        return getFieldQuery(field, queryText, 0);
    }
  */

    @Override
    protected Query getFuzzyQuery(String field, final String termStr, final float minSimilarity) throws ParseException {
        return getExpanded(field, new InnerQueryMaker() {
            @Override
            public Query getRecursiveQuery(String fieldOrGroup) throws ParseException {
                return getFuzzyQuery(fieldOrGroup, termStr, minSimilarity);
            }
            @Override
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
    private Query getSuperFuzzyQuery(
        String field, final String termStr, final float minSimilarity) throws ParseException {
        return super.getFuzzyQuery(field, termStr, minSimilarity);
    }

    @Override
    protected Query getFieldQuery(
        final String field, final String queryText, final boolean quoted) throws ParseException {
        return getExpanded(field, new InnerQueryMaker() {
            @Override
            public Query getRecursiveQuery(String fieldOrGroup) throws ParseException {
                return getFieldQuery(fieldOrGroup, queryText, quoted);
            }
            @Override
            public Query getFinalQuery(String field) throws ParseException {
                return getSuperFieldQuery(field, queryText, quoted);
            }
        });
    }

    private Query getSuperFieldQuery(String field, String queryText, boolean quoted) throws ParseException {
//        System.out.println(field + ":" + queryText + ", quoted=" + quoted);
        return super.getFieldQuery(field, queryText, quoted);
    }

    @Override
    protected Query getPrefixQuery(String field, final String termStr) throws ParseException {
        return getExpanded(field, new InnerQueryMaker() {
            @Override
            public Query getRecursiveQuery(String fieldOrGroup) throws ParseException {
                return getPrefixQuery(fieldOrGroup, termStr);
            }
            @Override
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
    protected Query getSuperPrefixQuery(String field, final String termStr) throws ParseException {
        return super.getPrefixQuery(field, termStr);
    }

    @Override
    protected Query getWildcardQuery(String field, final String termStr) throws ParseException {
        return getExpanded(field, new InnerQueryMaker() {
            @Override
            public Query getRecursiveQuery(String fieldOrGroup) throws ParseException {
                return getWildcardQuery(fieldOrGroup, termStr);
            }
            @Override
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
    protected Query getSuperWildcardQuery(String field, String termStr) throws ParseException {
        return super.getWildcardQuery(field, termStr);
    }


    @Override
    protected Query getRangeQuery(
        String field, final String part1, final String part2, final boolean inclusive, final boolean exclusive) {
        try {
            return getExpanded(field, new InnerQueryMaker() {
                @Override
                public Query getRecursiveQuery(String fieldOrGroup) {
                    return getRangeQuery(fieldOrGroup, part1, part2, inclusive, exclusive);
                }
                @Override
                public Query getFinalQuery(String field) throws ParseException {
                    return getSuperRangeQuery(field, part1, part2, inclusive, exclusive);
                }
            });
        } catch (ParseException e) {
            throw new RuntimeException("ParseException", e);
        }
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
    protected Query getSuperRangeQuery(String field, final String part1, final String part2,
                                       final boolean startInclusive, final boolean endInclusive) throws ParseException {
        return super.getRangeQuery(field, part1, part2, startInclusive, endInclusive);
    }

    @Override
    protected Query getRegexpQuery(final String field, final String termStr) throws ParseException {
        try {
            return getExpanded(field, new InnerQueryMaker() {
                @Override
                public Query getRecursiveQuery(String fieldOrGroup) throws ParseException {
                    return getRegexpQuery(fieldOrGroup, termStr);
                }
                @Override
                public Query getFinalQuery(String field) throws ParseException {
                    return getSuperRegexpQuery(field, termStr);
                }
            });
        } catch (ParseException e) {
            throw new RuntimeException("ParseException", e);
        }
    }
    protected Query getSuperRegexpQuery(String field, String termStr) throws ParseException {
        return super.getRegexpQuery(field, termStr);
    }
}
