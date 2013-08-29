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
package dk.statsbiblioteket.summa.support.harmonise.hub;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.ManyToManyMapper;
import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.summa.search.tools.QueryRewriter;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;

import java.io.StringWriter;
import java.util.*;

/**
 * Rewrites queries and filters by parsing and replacing fields and terms. The resulting query might change complexity
 * as one field can be mapped to multiple fields and one term to multiple terms.
 * See also {@link dk.statsbiblioteket.summa.support.harmonise.hub.QueryReducer}.
 * // TODO: Consider collapsing the functionality of this class and the QueryReducer
 * </p><p>
 * The provided configuration if passed directly to the {@link QueryRewriter}s constructor as well as being used for
 * QueryAdjuster-specific configuration.
 * </p><p>
 * The order of adjustment for query re-writing is fields first, then terms.
 * </p>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class QueryAdjuster {
    private static Log log = LogFactory.getLog(QueryAdjuster.class);

    private final ManyToManyMapper defaultFieldMap;
    private final Map<String, ManyToManyMapper> defaultTermMaps;
    private final QueryRewriter defaultRewriter;
    private static Configuration defaultConf;

    public QueryAdjuster(
            Configuration conf, ManyToManyMapper defaultFieldMap, Map<String, ManyToManyMapper> defaultTermMaps) {
        defaultConf = conf;
        this.defaultFieldMap = defaultFieldMap;
        this.defaultTermMaps = defaultTermMaps;
        defaultRewriter = new QueryRewriter(
                defaultConf, null, new AdjustmentEvent(defaultFieldMap, defaultTermMaps));
    }

    /**
     * Rewrite fields and terms in the given query using the rules provided in the constructor.
     * @param query a Lucene-style query.
     * @return the query with fields and terms potentially rewritten.
     * @throws ParseException if the query could not be parsed.
     */
    public String rewrite(String query) throws ParseException {
        return rewrite(query, null, null);
    }

    /**
     * Rewrite fields and terms in the given query using specified rules.
     * @param query a Lucene-style query.
     * @param overridingFieldMap rules for mapping of field names. If null, the default rules will be used.
     * @param overridingTermMap  rules for mapping terms, mapped as fieldName->termRules.
     *                           If null, the default rules will be used.
     * @return the query with fields and terms potentially rewritten.
     * @throws ParseException if the query could not be parsed.
     */
    public String rewrite(String query, ManyToManyMapper overridingFieldMap,
                          Map<String, ManyToManyMapper> overridingTermMap) throws ParseException {
        return overridingFieldMap == null && overridingTermMap == null ?
                defaultRewriter.rewrite(query) :
                new QueryRewriter(defaultConf, null, new AdjustmentEvent(overridingFieldMap, overridingTermMap)).
                        rewrite(query);
    }

    // First transform fields, then transform tags
    private class AdjustmentEvent extends QueryRewriter.Event implements Configurable {
        private final ManyToManyMapper fieldMap;
        private final Map<String, ManyToManyMapper> termMaps;

        public AdjustmentEvent(ManyToManyMapper fieldMap, Map<String, ManyToManyMapper> termMaps) {
            this.fieldMap = fieldMap;
            this.termMaps = termMaps;
        }

        // A phrase is treated as a single term
        @Override
        public Query onQuery(PhraseQuery query) {
            String baseField = query.getTerms()[0].field();

            if ("".equals(baseField) ||
                ((fieldMap == null || fieldMap.getForwardSet(baseField) == null) && !termMaps.containsKey(baseField))) {
                return query;
            }

            boolean first = true;
            String field = "";
            StringWriter sw = new StringWriter();
            for (Term term : query.getTerms()) {
                if (first) {
                    first = false;
                    field = term.field();
                } else {
                    sw.append(" ");
                }
                sw.append(term.text());
            }
            List<Pair<String, String>> terms = adjustFieldTerms(field, sw.toString());
            Query result = buildCompoundQuery(terms, query.getBoost(), true);
            if (result instanceof PhraseQuery) {
                ((PhraseQuery) result).setSlop(query.getSlop());
            } else if (result instanceof BooleanQuery) {
                for (BooleanClause clause: ((BooleanQuery)result).clauses()) {
                    if (clause.getQuery() instanceof PhraseQuery) {
                        ((PhraseQuery) clause.getQuery()).setSlop(query.getSlop());
                    }
                }
            }
            return result;
        }

        @Override
        public Query onQuery(TermQuery query) {
            List<Pair<String, String>> terms = adjustFieldTerms(query.getTerm().field(), query.getTerm().text());
            Query result = buildCompoundQuery(terms, query.getBoost(), false);
            if (log.isTraceEnabled()) {
                log.trace("rewriteQuery(query) changed " + query + " to " + result);
            }
            return result;
        }

        @Override
        public Query onQuery(final TermRangeQuery query) {
            return handleFieldExpansionQuery(query, query.getField(), new FieldExpansionCallback() {
                @Override
                public Query createQuery(String field) {
                    // TODO: Escape
                    return new TermRangeQuery(field, query.getLowerTerm(), query.getUpperTerm(),
                                              query.includesLower(), query.includesUpper());
                }
            });
        }

        @Override
        public Query onQuery(final PrefixQuery query) {
            return handleFieldExpansionQuery(query, query.getPrefix().field(), new FieldExpansionCallback() {
                @Override
                public Query createQuery(String field) {
                    return new PrefixQuery(new Term(field, query.getPrefix().text()));
                }
            });
        }

        @Override
        public Query onQuery(final FuzzyQuery query) {
            return handleFieldExpansionQuery(query, query.getTerm().field(), new FieldExpansionCallback() {
                @Override
                public Query createQuery(String field) {
                    return new FuzzyQuery(new Term(field, query.getTerm().text()), query.getMaxEdits(),
                                          query.getPrefixLength());
                }
            });
        }

        @Override
        public Query onQuery(Query query) {
            log.trace("Ignoring query of type " + query.getClass().getSimpleName());
            return query;
        }

        private List<Pair<String, String>> adjustFieldTerms(final String field, final String text) {
            // Get the new field(s)
            Set<String> newFields;
            if (fieldMap != null && fieldMap.getForward().containsKey(field)) {
                newFields = fieldMap.getForward().get(field);
            } else {
                newFields = new HashSet<String>(1);
                newFields.add(field);
            }

            // Generate <field, term>*
            List<Pair<String, String>> adjusted = new ArrayList<Pair<String, String>>();
            for (String newField: newFields) {
                if (termMaps != null && termMaps.containsKey(newField)) {
                    Set<String> replacements = termMaps.get(newField).getForwardSet(text);
                    if (replacements != null) {
                        for (String replacement: replacements) {
                            adjusted.add(new Pair<String, String>(newField, replacement));
                        }
                    } else {
                        adjusted.add(new Pair<String, String>(newField, text));
                    }
                } else { // No term conversion
                    adjusted.add(new Pair<String, String>(newField, text));
                }
            }

            return adjusted;
        }

        private Query buildCompoundQuery(List<Pair<String, String>> terms, float boost, boolean phrase) {
            if (terms.size() == 1) {
                return buildSingleQuery(terms.get(0).getKey(), terms.get(0).getValue(), boost, phrase);
            }
            BooleanQuery bq = new BooleanQuery();
            for (Pair<String, String> term: terms) {
                Query q = buildSingleQuery(term.getKey(), term.getValue(), 1.0f, phrase);
                bq.add(new BooleanClause(q, BooleanClause.Occur.SHOULD));
            }
            bq.setBoost(boost); // TODO: Verify this should not be on clause
            return bq;
        }

        private Query buildSingleQuery(String field, String text, float boost, boolean phrase) {
            //final Term t = new Term(field, qrw.escape(text, phrase));
            final Term t = new Term(field, text); // Escaping is done later
            Query query;
            if (phrase) {
                PhraseQuery phraseQuery = new PhraseQuery();
                phraseQuery.add(t);
                query = phraseQuery;
            } else {
                query =new TermQuery(t);
            }
            query.setBoost(boost);
            return query;
        }

        // Returns original Query (if no expansion), created Query (if 1 expansion)
        // or BooleanQuery (is 2+ expansions)
        // Also sets boost
        private Query handleFieldExpansionQuery(
                final Query originalQuery, final String field, FieldExpansionCallback callback) {
            Set<String> newFields = fieldMap.getForwardSet(field);
            if (newFields == null) {
                return originalQuery;
            }
            if (newFields.size() == 1) {
                Query result = callback.createQuery(newFields.iterator().next());
                result.setBoost(originalQuery.getBoost());
                return result;
            }
            BooleanQuery bq = new BooleanQuery();
            for (String newField: newFields) {
                Query q = callback.createQuery(newField);
                bq.add(new BooleanClause(q, BooleanClause.Occur.SHOULD));
            }
            bq.setBoost(originalQuery.getBoost());
            return bq;
        }
    }

    private static interface FieldExpansionCallback {
        Query createQuery(String field);
    }

    @Override
    public String toString() {
        return "QueryAdjuster(defaultFieldMap=" + defaultFieldMap
               + ", #defaultTermMaps=" + defaultTermMaps.size() + ")";
    }
}
