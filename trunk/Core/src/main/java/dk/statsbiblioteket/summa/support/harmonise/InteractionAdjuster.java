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

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.common.util.ManyToManyMapper;
import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.summa.facetbrowser.FacetStructure;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultExternal;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.search.tools.QueryRewriter;
import dk.statsbiblioteket.summa.support.summon.search.SummonSearchNode;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.*;

/**
 * Acts as a transformer of requests and responses. Queries can be rewritten with weight-adjustment of terms, scores for
 * returned documents can be tweaked.
 * </p><p>
 * IMPORTANT: Search-arguments for this adjuster are special as they should be prepended by an identifier that matches
 * the adjuster. If no identifier is given, the argument will be applied to all adjusters.
 * </p><p>
 * IMPORTANT: Specific for score and multiply is the possibility for specifying special behaviour when the query
 * is simple by prepending with {@code adjusterid.simple.}. If the query is simple and the simple-prefix is
 * specified, it overrides the default score and multiply for the adjuster with the same id. A simple query is
 * specified as a non-qualified term-query e.g. {@code 'foo'} or {@code '"foo"^1.2'} or a BooleanQuery of TermQuerys
 * that are all specified as neutral or required e.g. {@code 'foo bar'} or {@code '+"foo"^1.2 +"bar"^0.9'}, but not
 * {@code '+"foo"^1.2 -"bar"^0.9'}.
 * </p><p>
 * Limitations: Document field and facet field replacement is 1:1.
 *              Tag replacement is 1:n.
 * </p><p>
 * Note that there are CONF-equivalents to some SEARCH-arguments. Effects are cumulative.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
// TODO: disable facets? empty/null? force specific facets?
// TODO: Term weight rewrite from lookup-table (share tables if possible)
public class InteractionAdjuster implements Configurable {
    private static Log log = LogFactory.getLog(InteractionAdjuster.class);

    /**
     * The id for this search adjuster. All search-time arguments must be prepended with this id and a dot.
     * Example: The id is 'remote_a' and the returned scores should be multiplied by 1.5. The search-argument must be
     * {@code remote_a.adjust.score.multiply=1.5}.
     */
    public static final String CONF_IDENTIFIER = "adjuster.id";

    /**
     * If false, no adjustments are performed.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String SEARCH_ADJUST_ENABLED = "adjuster.enabled";
    public static final String CONF_ADJUST_ENABLED = SEARCH_ADJUST_ENABLED;
    public static final boolean DEFAULT_ADJUST_ENABLED = true;

    public static final String SEARCH_ADJUST_RESPONSE_FIELDS_ENABLED = "adjuster.response.fields.enabled";
    public static final String CONF_ADJUST_RESPONSE_FIELDS_ENABLED = SEARCH_ADJUST_RESPONSE_FIELDS_ENABLED;
    public static final boolean DEFAULT__ADJUST_RESPONSE_FIELDS_ENABLED = true;

    public static final String SEARCH_ADJUST_RESPONSE_FACETS_ENABLED = "adjuster.response.facets.enabled";
    public static final String CONF_ADJUST_RESPONSE_FACETS_ENABLED = SEARCH_ADJUST_RESPONSE_FACETS_ENABLED;
    public static final boolean DEFAULT__ADJUST_RESPONSE_FACETS_ENABLED = true;

    /**
     * Add a constant to returned scores for documents.
     * Additions are performed after multiplications.
     */
    public static final String SEARCH_ADJUST_SCORE_ADD = "adjuster.score.add";
    public static final String CONF_ADJUST_SCORE_ADD = SEARCH_ADJUST_SCORE_ADD;

    /**
     * Add a constant to returned scores for documents from simple queries (see the JavaDoc for the class for details).
     * Additions are performed after multiplications.
     */
    public static final String SEARCH_SIMPLE_ADJUST_SCORE_ADD = "simple.adjuster.score.add";
    public static final String CONF_SIMPLE_ADJUST_SCORE_ADD = SEARCH_SIMPLE_ADJUST_SCORE_ADD;

    /**
     * Multiply the returned scores for documents with a constant.
     * Multiplications are performed before additions.
     */
    public static final String SEARCH_ADJUST_SCORE_MULTIPLY = "adjuster.score.multiply";
    public static final String CONF_ADJUST_SCORE_MULTIPLY = SEARCH_ADJUST_SCORE_MULTIPLY;

    /**
     * Multiply the returned scores for documents with a constant if they resulted from a simple query (see the JavaDoc
     * for the class for details).
     * Multiplications are performed before additions.
     */
    public static final String SEARCH_SIMPLE_ADJUST_SCORE_MULTIPLY = "simple.adjuster.score.multiply";
    public static final String CONF_SIMPLE_ADJUST_SCORE_MULTIPLY = SEARCH_SIMPLE_ADJUST_SCORE_MULTIPLY;

    /**
     * Maps from field names to field names, one way when rewriting queries,
     * the other way when adjusting the returned result. This involves only
     * document-related elements.
     * </p><p>
     * The format is a comma-separated list of rewrite-rules. The format of
     * a single rule is 'fieldname - fieldname'.
     * Example: {@code author - AuthorField, title - main_title}.
     * </p><p>
     * This option is not cumulative. Search-time overrides base configuration.
     * </p><p>
     * Optional. Default is no rewriting.
     */
    // TODO: Handle many-to-many re-writing
    public static final String CONF_ADJUST_DOCUMENT_FIELDS = "adjuster.document.fields";
    public static final String SEARCH_ADJUST_DOCUMENT_FIELDS = CONF_ADJUST_DOCUMENT_FIELDS;

    /**
     * If true, requests with filters marked with pure negative are never treated as simple queries.
     * </p><p>
     * This is relevant for triggering adjustments due to (non)conformance to DisMax queries. If there is a pure
     * negative filter that must be merged into the query, the resulting query will not be simple.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_PURE_NEGATIVE_FILTER_TRIGGERS_NOT_SIMPLE = "adjuster.purenegative.isnotsimple";
    public static final boolean DEFAULT_PURE_NEGATIVE_FILTER_TRIGGERS_NOT_SIMPLE = true;

    /**
     * Fields that are rewritten to a term that match nothing in the searcher.
     */
    public static final String CONF_ADJUST_UNSUPPORTED_FIELDS = "adjuster.document.unsupported.fields";

    /**
     * Query that match nothing in the searcher (ie. year:991234)
     */
    public static final String CONF_ADJUST_UNSUPPORTED_QUERY= "adjuster.document.unsupported.query";


    /**
     * Maps from field names to field names, one way when rewriting queries,
     * the other way when adjusting the returned result. This involves only
     * facet-related elements.
     * </p><p>
     * The format is a comma-separated list of rewrite-rules. The format of
     * a single rule is 'fieldname - fieldname'.
     * Example: {@code author - AuthorField, title - main_title}.
     * </p><p>
     * This option is not cumulative. Search-time overrides base configuration.
     * </p><p>
     * Optional. Default is no rewriting.
     */
    // TODO: Handle many-to-many re-writing
    public static final String CONF_ADJUST_FACET_FIELDS = "adjuster.facet.fields";
    public static final String SEARCH_ADJUST_FACET_FIELDS = CONF_ADJUST_FACET_FIELDS;

    /**
     * Maps, extends and contracts tag names for returned facet results.
     * </p><p>
     * Note: This is configuration-level only.
     * </p><p>
     * The format is a list of configurations conforming to {@link TagAdjuster}.
     * There is one configuration for each facet for which to adjust tags.
     * </p><p>
     * Optional. Default is no adjustments.
     */
    public static final String CONF_ADJUST_FACET_TAGS = "adjuster.facet.tags";

    private final String id;
    private final String prefix;
    private double baseFactor;
    private double baseAddition;
    private double simpleBaseFactor;
    private double simpleBaseAddition;
    private ManyToManyMapper defaultDocumentFields = null;
    private ManyToManyMapper defaultFacetFields = null;
    private List<TagAdjuster> tagAdjusters = null;
    private Set<String> unsupportedFields = new HashSet<String>();
    private Query unsupportedQuery = null;
    private final boolean enabled;
    private boolean adjustResponseFieldsEnabled;
    private boolean adjustResponseFacetsEnabled;
    private final boolean pureNegativeNotSimple;

    public InteractionAdjuster(Configuration conf) throws ConfigurationException {
        id = conf.getString(CONF_IDENTIFIER);
        enabled = conf.getBoolean(CONF_ADJUST_ENABLED, DEFAULT_ADJUST_ENABLED);
        prefix = id + ".";

        baseFactor = conf.getDouble(CONF_ADJUST_SCORE_MULTIPLY, 1.0);
        baseFactor *= conf.getDouble(prefix + CONF_ADJUST_SCORE_MULTIPLY, 1.0);
        simpleBaseFactor = conf.getDouble(CONF_SIMPLE_ADJUST_SCORE_MULTIPLY, baseFactor);
        simpleBaseFactor *= conf.getDouble(prefix + CONF_SIMPLE_ADJUST_SCORE_MULTIPLY, 1.0);

        baseAddition = conf.getDouble(CONF_ADJUST_SCORE_ADD, 0.0);
        baseAddition += conf.getDouble(prefix + CONF_ADJUST_SCORE_ADD, 0.0);
        simpleBaseAddition = conf.getDouble(CONF_SIMPLE_ADJUST_SCORE_ADD, baseAddition);
        simpleBaseAddition += conf.getDouble(prefix + CONF_SIMPLE_ADJUST_SCORE_ADD, 0.0);

        pureNegativeNotSimple = conf.getBoolean(
                CONF_PURE_NEGATIVE_FILTER_TRIGGERS_NOT_SIMPLE, DEFAULT_PURE_NEGATIVE_FILTER_TRIGGERS_NOT_SIMPLE);

        adjustResponseFieldsEnabled = conf.getBoolean(
                CONF_ADJUST_RESPONSE_FIELDS_ENABLED, DEFAULT__ADJUST_RESPONSE_FIELDS_ENABLED);
        adjustResponseFacetsEnabled = conf.getBoolean(
                CONF_ADJUST_RESPONSE_FACETS_ENABLED, DEFAULT__ADJUST_RESPONSE_FACETS_ENABLED);
        if (conf.valueExists(CONF_ADJUST_DOCUMENT_FIELDS)) {
            defaultDocumentFields = new ManyToManyMapper(conf.getStrings(CONF_ADJUST_DOCUMENT_FIELDS));
        }
        if (conf.valueExists(CONF_ADJUST_FACET_FIELDS)) {
            defaultFacetFields = new ManyToManyMapper(conf.getStrings(CONF_ADJUST_FACET_FIELDS));
        }
        if (conf.valueExists(CONF_ADJUST_FACET_TAGS)) {
            List<Configuration> taConfs;
            try {
                taConfs = conf.getSubConfigurations(CONF_ADJUST_FACET_TAGS);
            } catch (SubConfigurationsNotSupportedException e) {
                throw new ConfigurationException(
                        "Expected a list of sub configurations for key " + CONF_ADJUST_FACET_TAGS + " but the current "
                        + "Configuration does not support them", e);
            }
            tagAdjusters = new ArrayList<TagAdjuster>(taConfs.size());
            for (Configuration tagConf: taConfs) {
                TagAdjuster tagAdjuster = new TagAdjuster(tagConf);
                tagAdjuster.setID(id);
                tagAdjusters.add(tagAdjuster);
            }
            log.debug("Created " + tagAdjusters.size() + " tag adjusters");
        }

        if (conf.valueExists(CONF_ADJUST_UNSUPPORTED_FIELDS)){
            unsupportedFields = new HashSet<String>(conf.getStrings(CONF_ADJUST_UNSUPPORTED_FIELDS));
            String unsupportedQueryString = conf.getString(CONF_ADJUST_UNSUPPORTED_QUERY);

            String[] split = unsupportedQueryString.split(":");
            unsupportedQuery = new TermQuery(new Term(split[0],split[1]));
        }

        log.debug(String.format(
                "Constructed search adjuster with id='%s', enabled=%b, baseFactor=%f, baseAddition=%f, "
                + "adjustingDocumentFields='%s', adjustingFacetFields='%s', tagAdjusters=%d, "
                + "adjustResponseFieldsEnabled=%b, adjustResponseFacetsEnabled=%b",
                id, enabled, baseFactor, baseAddition,
                conf.getStrings(CONF_ADJUST_DOCUMENT_FIELDS, new ArrayList<String>(0)),
                conf.getStrings(CONF_ADJUST_FACET_FIELDS, new ArrayList<String>(0)),
                tagAdjusters == null ? 0 : tagAdjusters.size(),
                adjustResponseFieldsEnabled, adjustResponseFacetsEnabled));
    }

    /**
     * Creates a copy of the provided request and rewrites arguments according
     * to settings and request-time arguments, then returns the adjusted
     * request.
     * </p><p>
     * Note: The rewriter logs exceptions and returns the unmodified request in
     *       case of errors.
     * @param request the unadjusted request.
     * @return an adjusted request.
     */
    public Request rewrite(Request request) {
        log.trace("rewrite called");
        String incoming=null;

        if (log.isDebugEnabled()) {
            incoming=request.toString();
        }

        Request adjusted = clone(request);
        if (!adjusted.getBoolean(SEARCH_ADJUST_ENABLED, enabled)) {
            log.trace("The adjuster is disabled. Exiting rewrite");
            return adjusted;
        }
        rewriteFacetFields(adjusted);
        try {
            rewriteQuery(adjusted);
        } catch (ParseException e) {
            log.info("ParseException while rewriting request", e);
        }

        if (log.isDebugEnabled()) {
            log.debug("Query Request:" + incoming + " Query rewritten:"+adjusted);

        }
        return adjusted;

    }

    /**
     * Rewrites search filter and query according to the given rules for
     * document field, facet field and facet tag re-writing.
     * @param request a search request with a filter and/or a query.
     * @throws org.apache.lucene.queryparser.classic.ParseException if the filter or
     *         the query could not be parsed.
     */
    @SuppressWarnings({"unchecked"})
    private void rewriteQuery(Request request) throws ParseException {
        log.trace("rewriteQuery called");
        final ManyToManyMapper documentFieldMap = resolveMap(
                request, defaultDocumentFields, SEARCH_ADJUST_DOCUMENT_FIELDS);
        final ManyToManyMapper facetFieldMap = resolveMap(
                request, defaultFacetFields, SEARCH_ADJUST_FACET_FIELDS);

        if (documentFieldMap == null && facetFieldMap == null &&
            tagAdjusters == null) {
            log.trace("No document fields, facet fields or tag adjusters for rewriteQuery");
            return;
        }

        log.trace("Rewriting fields and content in document filter, query and sort");
        final String filter = request.getString(DocumentKeys.SEARCH_FILTER, "");
        if (!"".equals(filter)) {
            request.put(DocumentKeys.SEARCH_FILTER, rewriteQuery(filter, documentFieldMap, facetFieldMap));
        }
        final String query = request.getString(DocumentKeys.SEARCH_QUERY, "");
        if (!"".equals(query)) {
            request.put(DocumentKeys.SEARCH_QUERY, rewriteQuery(query, documentFieldMap, facetFieldMap));
        }
        if (documentFieldMap != null
            && request.containsKey(DocumentKeys.SEARCH_SORTKEY)) {
            String key = request.getString(DocumentKeys.SEARCH_SORTKEY);
            Set<String> replaced = documentFieldMap.getForward().get(key);
            if (replaced != null) {
                if (replaced.size()> 1) {
                    // TODO: Can't we do this with Lucene?
                    log.warn("Warning: The sort key '" + key + "' will be replaced with multiple values "
                             + Strings.join(replaced, ", "));
                    request.put(DocumentKeys.SEARCH_SORTKEY, Strings.join(replaced, ", "));
                } else {
                    request.put(DocumentKeys.SEARCH_SORTKEY, replaced.iterator().next());
                }
            }
        }
    }

    private String rewriteQuery(final String query, final ManyToManyMapper... maps) throws ParseException {
        return new QueryRewriter(
                getRewriterConfig(), null, // TODO: Consider supplying the SummaAnalyzer
                new QueryRewriter.Event() {

                    // For phrases we only replace the field (if any)
                    @Override
                    public Query onQuery(PhraseQuery query) {
                        String baseField = query.getTerms()[0].field();
                        if (unsupportedFields.contains(baseField)) {
                            return unsupportedQuery;
                        }
                        // We cannot skip processing as we need the escaping
//                    if ("".equals(baseField)) {
//                        return query;
//                    }
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
                        List<Pair<String, String>> terms = makeTerms(field, sw.toString(), maps);
                        Query result = makeQuery(terms, query.getBoost(), true);
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
//                    if ("".equals(query.getTerm().field())) {
//                        return query;
//                    }
                        String baseField = query.getTerm().field();
                        if (unsupportedFields.contains(baseField)) {
                            return unsupportedQuery;
                        }

                        List<Pair<String, String>> terms = makeTerms(
                                query.getTerm().field(), query.getTerm().text(), maps);
                        Query result = makeQuery(terms, query.getBoost(), false);
                        if (log.isTraceEnabled()) {
                            log.trace("rewriteQuery(query) changed " + query + " to " + result);
                        }
                        return result;
                    }

                    @Override
                    public Query onQuery(final TermRangeQuery query) {
                        String baseField = query.getField();
                        if (unsupportedFields.contains(baseField)) {
                            return unsupportedQuery;
                        }


                        return handleFieldExpansionQuery(query, query.getField(), new FieldExpansionCallback() {
                            @Override
                            public Query createQuery(String field) {
                                // TODO: Escape
                                return new TermRangeQuery(field, query.getLowerTerm(), query.getUpperTerm(), query.includesLower(), query.includesUpper());
                            }
                        }, maps);
                    }

                    @Override
                    public Query onQuery(final PrefixQuery query) {
                        String baseField = query.getPrefix().field();
                        if (unsupportedFields.contains(baseField)) {
                            return unsupportedQuery;
                        }

                        return handleFieldExpansionQuery(query, query.getPrefix().field(), new FieldExpansionCallback() {
                            @Override
                            public Query createQuery(String field) {
                                return new PrefixQuery(new Term(field, query.getPrefix().text()));
                            }
                        }, maps);
                    }

                    @Override
                    public Query onQuery(final FuzzyQuery query) {
                        String baseField = query.getTerm().field();
                        if (unsupportedFields.contains(baseField)) {
                            return unsupportedQuery;
                        }

                        return handleFieldExpansionQuery(query, query.getTerm().field(), new FieldExpansionCallback() {
                            @Override
                            public Query createQuery(String field) {
                                return new FuzzyQuery(new Term(field, query.getTerm().text()), query.getMaxEdits(), query.getPrefixLength());
                            }
                        }, maps);
                    }

                    @Override
                    public Query onQuery(Query query) {
                        log.trace("Ignoring query of type " + query.getClass().getSimpleName());
                        return query;
                    }
                }).rewrite(query);
    }

    public Configuration getRewriterConfig() {
        return Configuration.newMemoryBased(
                QueryRewriter.CONF_QUOTE_TERMS, false
        );
    }

    private interface FieldExpansionCallback {
        Query createQuery(String field);
    }
    // Returns original Query (if no expansion), created Query (if 1 expansion)
    // or BooleanQuery (is 2+ expansions)
    // Also sets boost
    private Query handleFieldExpansionQuery(
            final Query originalQuery, final String field,
            FieldExpansionCallback callback, ManyToManyMapper... maps) {
        Set<String> newFields = getAlternativeFields(field, maps);
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

    // Returns null on empty field or no alternatives
    private Set<String> getAlternativeFields(
            String field, ManyToManyMapper... maps) {
        if ("".equals(field)) {
            return null;
        }
        Set<String> newFields = null;
        for (ManyToManyMapper map: maps) {
            if (map == null) {
                continue;
            }
            if (map.getForward().containsKey(field)) {
                if (newFields == null) {
                    newFields = new HashSet<String>();
                }
                Set<String> s = map.getForward().get(field);
                newFields.addAll(s);
            }
        }
        if (newFields == null) {
            return null;
        }
        return newFields;
    }

    private Query makeQuery(List<Pair<String, String>> terms, float boost) {
        if (terms.size() == 1) {
            Query q = createPhraseOrTermQuery(terms.get(0).getKey(), terms.get(0).getValue());
            q.setBoost(boost);
            return q;
        }
        BooleanQuery bq = new BooleanQuery();
        for (Pair<String, String> term: terms) {
            Query q = createPhraseOrTermQuery(term.getKey(), term.getValue());
            bq.add(new BooleanClause(q, BooleanClause.Occur.SHOULD));
        }
        bq.setBoost(boost); // TODO: Verify this should not be on clause
        return bq;
    }

    private Query makeQuery(List<Pair<String, String>> terms, float boost, boolean phrase) {
        if (terms.size() == 1) {
            return makeQuery(terms.get(0).getKey(), terms.get(0).getValue(), boost, phrase);
        }
        BooleanQuery bq = new BooleanQuery();
        for (Pair<String, String> term: terms) {
            Query q = makeQuery(term.getKey(), term.getValue(), 1.0f, phrase);
            bq.add(new BooleanClause(q, BooleanClause.Occur.SHOULD));
        }
        bq.setBoost(boost); // TODO: Verify this should not be on clause
        return bq;
    }

    private Query makeQuery(String field, String text, float boost, boolean phrase) {
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

    private List<Pair<String, String>> makeTerms(
            final String field, final String text,
            final ManyToManyMapper... maps) {
        Set<String> newFields = null;
        for (ManyToManyMapper map: maps) {
            if (map != null && map.getForward().containsKey(field)) {
                newFields = map.getForward().get(field);
                break;
            }
        }
        if (newFields == null) {
            newFields = new HashSet<String>(1);
            newFields.add(field);
        }

        // We base this on old field, as it is the normalised field
        Set<String> newTexts = null;
        if (tagAdjusters != null) {
            for (TagAdjuster tagAdjuster: tagAdjusters) {
                if (tagAdjuster.getFacetNames().contains(field)) {
                    newTexts = tagAdjuster.getReverse(text);
                }
            }
        }
        if (newTexts == null) {
            // No transformation
            newTexts = new HashSet<String>(Arrays.asList(text));
        }

        List<Pair<String, String>> result =
                new ArrayList<Pair<String, String>>();
        for (String newField: newFields) {
            for (String newText: newTexts) {
                result.add(new Pair<String, String>(newField, newText));
            }
        }

        return result;
    }

    private Query createPhraseOrTermQuery(String field, String text) {
        if (text.contains(" ")) {
            PhraseQuery phraseQuery = new PhraseQuery();
            String[] tokens = text.split(" +");
            for (String term: tokens) {
                phraseQuery.add(new Term(field, term));
            }
            return phraseQuery;
        }
        return new TermQuery(new Term(field, text));
    }

    private void rewriteFacetFields(Request request) {
        log.trace("rewriteFacetFields called");
        ManyToManyMapper facetFieldMap = resolveMap(request, defaultFacetFields, SEARCH_ADJUST_FACET_FIELDS);
        if (facetFieldMap == null) {
            return;
        }
        log.trace("Adjusting fields in facet request");
        if (request.containsKey(FacetKeys.SEARCH_FACET_FACETS)) {
            List<String> facets = request.getStrings(FacetKeys.SEARCH_FACET_FACETS);
            List<String> adjusted = new ArrayList<String>(facets.size() * 2);
            for (String facetString: facets) {
                FacetStructure facet = new FacetStructure(facetString, -1, -1);
                if (facet.getFields().length == 1 && facetFieldMap.getForward().containsKey(facet.getFields()[0])) {
                    Set<String> alts = facetFieldMap.getForward().get(facet.getFields()[0]);
                    for (String alt: alts) {
                        if (FacetStructure.DEFAULT_FACET_SORT_TYPE.equals(facet.getSortType())) {
                            if (facet.getWantedTags() == -1) { // Plain
                                adjusted.add(alt);
                            } else { // Wanted Tags
                                adjusted.add(alt + "(" + facet.getWantedTags() + ")");
                            }
                        } else {
                            if (facet.getWantedTags() == -1) { // Only order change
                                adjusted.add(alt + "(" + facet.getSortType() + ")");
                            } else { // Wanted Tags and order change
                                adjusted.add(alt + "(" + facet.getWantedTags() + " " + facet.getSortType() + ")");
                            }
                        }
                    }
                } else { // No change at all
                    adjusted.add(facetString);
                }
            }
            request.put(FacetKeys.SEARCH_FACET_FACETS, Strings.join(adjusted, ", "));
        }
    }

    private ManyToManyMapper resolveMap(Request request, ManyToManyMapper defaultMap, String key) {
        log.trace("resolveMap called");
        ManyToManyMapper map = defaultMap;
        if (request.containsKey(key)) {
            map = new ManyToManyMapper(request.getStrings(key));
        }
        if (request.containsKey(prefix + key)) {
            map = new ManyToManyMapper(request.getStrings(prefix + key));
        }
        return map;
    }

    private Request clone(Request request) {
        Request cloned = new Request();
        for (Map.Entry<String, Serializable> entry: request.entrySet()) {
            cloned.put(entry.getKey(), entry.getValue());
        }
        return cloned;
    }

    /* ********************************************************************* */

    /**
     * Modifies the responses according to the given settings.
     * @param request   the rewritten request that resulted in the responses.
     * @param responses non-modified responses.
     */
    public void adjust(Request request, ResponseCollection responses) {
        log.trace("adjust called");
        long startTime = System.currentTimeMillis();
        if (!request.getBoolean(SEARCH_ADJUST_ENABLED, enabled)) {
            log.trace("The adjuster is disabled. Exiting adjust");
            return;
        }
        adjustDocuments(request, responses);
        responses.addTiming("interactionadjuster.adjust.documents", System.currentTimeMillis() - startTime);
        if (request.getBoolean(SEARCH_ADJUST_RESPONSE_FACETS_ENABLED, adjustResponseFacetsEnabled)) {
            adjustFacets(request, responses);
            responses.addTiming("interactionadjuster.adjust.facets", System.currentTimeMillis() - startTime);
        }
        responses.addTiming("interactionadjuster.adjust.total", System.currentTimeMillis() - startTime);
    }

    private void adjustDocuments(
            Request request, ResponseCollection responses) {
        log.trace("adjustDocuments called");
        DocumentResponse documentResponse = null;
        for (Response response: responses) {
            if (!DocumentResponse.NAME.equals(response.getName())) {
                continue;
            }
            if (!(response instanceof DocumentResponse)) {
                log.error("adjustDocuments found response wil name " + DocumentResponse.NAME + " and expected Class "
                          + DocumentResponse.class + " but got " + response.getClass());
                continue;
            }
            documentResponse = (DocumentResponse)response;
        }
        if (documentResponse == null) {
            log.debug("No DocumentResponse found in adjustDocuments. Exiting");
            return;
        }
        adjustDocumentScores(request, documentResponse);
        replaceDocumentFields(request, documentResponse);
    }

    private void adjustFacets(Request request, ResponseCollection responses) {
        log.trace("adjustFacets called");
        FacetResultExternal facetResponse = null;
        for (Response response: responses) {
            if (!FacetResultExternal.NAME.equals(response.getName())) {
                continue;
            }
            // TODO: Requiring FacetResultExternal is too harsh
            if (!(response instanceof FacetResultExternal)) {
                log.error("adjustDocuments found response wil name " + DocumentResponse.NAME + " and expected Class "
                          + DocumentResponse.class + " but got " + response.getClass());
                continue;
            }
            facetResponse = (FacetResultExternal)response;
        }
        if (facetResponse == null) {
            log.debug("No FacetResponseExternal found in adjustDocuments. Exiting");
            return;
        }
        replaceFacetFields(request, facetResponse);
        long startAdjust = System.currentTimeMillis();
        if (tagAdjusters != null) {
            for (TagAdjuster tagAdjuster: tagAdjusters) {
                tagAdjuster.adjust(facetResponse);
            }
        }
        facetResponse.addTiming("tagadjuster.adjusts.total", System.currentTimeMillis() - startAdjust);
    }

    private boolean warnedOnIncompleteFacetMap = false;
    private void replaceFacetFields(Request request, FacetResultExternal facetResponse) {
        log.trace("adjustFacetFields called");
        ManyToManyMapper facetMap = resolveMap(request, defaultFacetFields, SEARCH_ADJUST_FACET_FIELDS);
        if (facetMap == null) {
            return;
        }
        // Check and warn for multiple destinations for same source
        for (Map.Entry<String, Set<String>> entry:
                facetMap.getForward().entrySet()) {
            if (entry.getValue().size() > 1 && !warnedOnIncompleteFacetMap) {
                warnedOnIncompleteFacetMap = true;
                log.warn(String.format(
                        "Encountered mapping from source '%s' to destinations '%s'. Multiple sources is not yet supported",
                        entry.getValue(), entry.getKey()));
            }
        }

        Map<String, String> reversedSimplified = new HashMap<String, String>(facetMap.getForward().size());
        for (Map.Entry<String, Set<String>> entry:
                facetMap.getReverse().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                reversedSimplified.put(entry.getKey(), entry.getValue().iterator().next());
            }
        }
        facetResponse.renameFacetsAndFields(reversedSimplified);
    }

    private void replaceDocumentFields(
            Request request, DocumentResponse documentResponse) {
        log.trace("replaceDocumentFields called");
        long startTime = System.currentTimeMillis();
        ManyToManyMapper docFieldMap = resolveMap(request, defaultDocumentFields, SEARCH_ADJUST_DOCUMENT_FIELDS);
        if (docFieldMap == null) {
            return;
        }

        if (documentResponse.getSortKey() != null &&
            docFieldMap.getReverse().containsKey(documentResponse.getSortKey())) {
            documentResponse.setSortKey(Strings.join(docFieldMap.getReverse().get(documentResponse.getSortKey()),", "));
        }
        if (!request.getBoolean(SEARCH_ADJUST_RESPONSE_FIELDS_ENABLED, adjustResponseFieldsEnabled)) {
            return;
        }
        log.trace("Replacing document fields (" + docFieldMap.getForward().size() + " replacements)");
        for (DocumentResponse.Record record: documentResponse.getRecords()) {
            List<DocumentResponse.Field> newFields = new ArrayList<DocumentResponse.Field>(record.getFields().size()*2);
            for (DocumentResponse.Field field: record.getFields()) {
                if (docFieldMap.getReverse().containsKey(field.getName())) {
                    if (log.isTraceEnabled()) {
                        log.trace("Changing field name '" + field.getName() + "' to '" + Strings.join(
                                docFieldMap.getReverse().get(field.getName()), ", ") + " for " + record.getId());
                    }
                    Set<String> alts = docFieldMap.getReverse().get(field.getName());
                    if (alts.size() == 1) { // 1:1
                        field.setName(alts.iterator().next());
                        newFields.add(field);
                    } else { // 1:n
                        for (String alt: alts) {
                            newFields.add(new DocumentResponse.Field(alt, field.getContent(), field.isEscapeContent()));
                        }
                    }
                } else { // No mapping
                    newFields.add(field);
                }
            }
            record.getFields().clear();
            record.getFields().addAll(newFields);
        }
        documentResponse.addTiming("interactionadjuster.replacedocumentfields", System.currentTimeMillis() - startTime);
    }

    private final QueryRewriter qrw = new QueryRewriter(null, null, null); // Danger! No event. Use only for isSimple!

    /**
     * If the responses contain a {@link DocumentResponse}, the scores for the
     * documents are adjusted with the given factor and addition.
     * @param request   potential tweaks to factor and addition.
     * @param documentResponse the response to adjustAll.
     */
    private void adjustDocumentScores(Request request, DocumentResponse documentResponse) {
        log.trace("adjustDocumentScores called");
        boolean filtersContaminateQuery =
                request.containsKey(DocumentKeys.SEARCH_FILTER)
                && !request.getBoolean(SummonSearchNode.SEARCH_SOLR_FILTER_IS_FACET, false)
                && pureNegativeNotSimple && request.containsKey(DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE);

        boolean isSimple = (!filtersContaminateQuery
                            && request.containsKey(DocumentKeys.SEARCH_QUERY)
                            && qrw.isSimple(request.getString(DocumentKeys.SEARCH_QUERY)));

        double factor = isSimple ? simpleBaseFactor : baseFactor;
        double addition = isSimple ? simpleBaseAddition : baseAddition;
        if (request.containsKey(SEARCH_ADJUST_SCORE_MULTIPLY)) {
            factor *= request.getDouble(SEARCH_ADJUST_SCORE_MULTIPLY);
        }
        if (request.containsKey(prefix + SEARCH_ADJUST_SCORE_MULTIPLY)) {
            factor *= request.getDouble(prefix + SEARCH_ADJUST_SCORE_MULTIPLY);
        }
        if (isSimple && request.containsKey(SEARCH_SIMPLE_ADJUST_SCORE_MULTIPLY)
            || request.containsKey(prefix + SEARCH_SIMPLE_ADJUST_SCORE_MULTIPLY)) {
            // Note: Complete override
            factor = simpleBaseFactor;
            if (request.containsKey(SEARCH_SIMPLE_ADJUST_SCORE_MULTIPLY)) {
                factor *= simpleBaseFactor * request.getDouble(SEARCH_SIMPLE_ADJUST_SCORE_MULTIPLY);
            }
            if (request.containsKey(prefix + SEARCH_SIMPLE_ADJUST_SCORE_MULTIPLY)) {
                factor *= simpleBaseFactor * request.getDouble(prefix + SEARCH_SIMPLE_ADJUST_SCORE_MULTIPLY);
            }
        }

        if (request.containsKey(SEARCH_ADJUST_SCORE_ADD)) {
            addition += request.getDouble(SEARCH_ADJUST_SCORE_ADD);
        }
        if (request.containsKey(prefix + SEARCH_ADJUST_SCORE_ADD)) {
            addition += request.getDouble(prefix + SEARCH_ADJUST_SCORE_ADD);
        }
        if (isSimple && request.containsKey(SEARCH_SIMPLE_ADJUST_SCORE_ADD)
            || request.containsKey(prefix + SEARCH_SIMPLE_ADJUST_SCORE_ADD)) {
            // Note: Complete override
            factor = simpleBaseAddition;
            if (request.containsKey(SEARCH_SIMPLE_ADJUST_SCORE_ADD)) {
                addition *= simpleBaseFactor * request.getDouble(SEARCH_SIMPLE_ADJUST_SCORE_ADD);
            }
            if (request.containsKey(prefix + SEARCH_SIMPLE_ADJUST_SCORE_ADD)) {
                addition *= simpleBaseFactor * request.getDouble(prefix + SEARCH_SIMPLE_ADJUST_SCORE_ADD);
            }
        }

        // It is okay to compare as worst case is an unnecessary adjustment
        //noinspection FloatingPointEquality
        if (addition == 0 && factor == 1.0) {
            log.trace("No adjustment to make to scores (factor == 1.0, addition == 0.0");
            return;
        }

        log.trace("adjustDocuments called with factor " + factor + ", addition " + addition);
        for (DocumentResponse.Record record: documentResponse.getRecords()){
            record.setScore((float)(record.getScore() * factor + addition));
        }
    }

    public String getId() {
        return id;
    }
}
