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
import dk.statsbiblioteket.summa.common.util.ManyToManyMap;
import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultExternal;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.*;

/**
 * Acts as a transformer of requests and responses. Queries can be rewritten
 * with weight-adjustment of terms, scores for returned documents can be
 * tweaked.
 * </p><p>
 * IMPORTANT: Search-arguments for this adjuster are special as they should be
 * prepended by an identifier that matches the adjuster. If no identifier is
 * given, the argument will be applied to all adjusters.
 * </p><p>
 * Limitations: Document field and facet field replacement is 1:1.
 *              Tag replacement is 1:n.
 * </p><p>
 * Note that there are CONF-equivalents to some SEARCH-arguments. Effects are
 * cumulative.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
// TODO: disable facets? empty/null? force specific facets?
// TODO: Term weight rewrite from lookup-table (share tables if possible)
public class InteractionAdjuster implements Configurable {
    private static Log log = LogFactory.getLog(InteractionAdjuster.class);

    /**
     * The id for this search adjuster. All search-time arguments must be
     * prepended with this id and a dot.
     * Example: The id is 'remote_a' and the returned scores should be
     * multiplied by 1.5. The search-argument must be
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

    public static final String SEARCH_ADJUST_RESPONSE_FIELDS_ENABLED =
        "adjuster.response.fields.enabled";
    public static final String CONF_ADJUST_RESPONSE_FIELDS_ENABLED =
        SEARCH_ADJUST_RESPONSE_FIELDS_ENABLED;
    public static final boolean DEFAULT__ADJUST_RESPONSE_FIELDS_ENABLED = true;

    public static final String SEARCH_ADJUST_RESPONSE_FACETS_ENABLED =
        "adjuster.response.facets.enabled";
    public static final String CONF_ADJUST_RESPONSE_FACETS_ENABLED =
        SEARCH_ADJUST_RESPONSE_FACETS_ENABLED;
    public static final boolean DEFAULT__ADJUST_RESPONSE_FACETS_ENABLED = true;

    /**
     * Add a constant to returned scores for documents.
     * Additions are performed after multiplications.
     */
    public static final String SEARCH_ADJUST_SCORE_ADD = "adjuster.score.add";
    public static final String CONF_ADJUST_SCORE_ADD = SEARCH_ADJUST_SCORE_ADD;

    /**
     * Multiply the returned scores for documents with a constant.
     * Multiplications are performed before additions.
     */
    public static final String SEARCH_ADJUST_SCORE_MULTIPLY =
        "adjuster.score.multiply";
    public static final String CONF_ADJUST_SCORE_MULTIPLY =
        SEARCH_ADJUST_SCORE_MULTIPLY;

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
    public static final String CONF_ADJUST_DOCUMENT_FIELDS =
        "adjuster.document.fields";
    public static final String
        SEARCH_ADJUST_DOCUMENT_FIELDS = CONF_ADJUST_DOCUMENT_FIELDS;

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
    public static final String CONF_ADJUST_FACET_FIELDS =
        "adjuster.facet.fields";
    public static final String 
        SEARCH_ADJUST_FACET_FIELDS = CONF_ADJUST_FACET_FIELDS;

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
    private double baseFactor = 1.0;
    private double baseAddition = 0.0;
    private ManyToManyMap defaultDocumentFields = null;
    private ManyToManyMap defaultFacetFields = null;
    private List<TagAdjuster> tagAdjusters = null;
    private final boolean enabled;
    private boolean adjustResponseFieldsEnabled;
    private boolean adjustResponseFacetsEnabled;

    public InteractionAdjuster(Configuration conf)
                                                 throws ConfigurationException {
        id = conf.getString(CONF_IDENTIFIER);
        enabled = conf.getBoolean(CONF_ADJUST_ENABLED, DEFAULT_ADJUST_ENABLED);
        prefix = id + ".";
        baseFactor = conf.getDouble(CONF_ADJUST_SCORE_MULTIPLY, baseFactor);
        baseAddition = conf.getDouble(CONF_ADJUST_SCORE_ADD, baseAddition);
        adjustResponseFieldsEnabled = conf.getBoolean(
            CONF_ADJUST_RESPONSE_FIELDS_ENABLED,
            DEFAULT__ADJUST_RESPONSE_FIELDS_ENABLED);
        adjustResponseFacetsEnabled = conf.getBoolean(
            CONF_ADJUST_RESPONSE_FACETS_ENABLED,
            DEFAULT__ADJUST_RESPONSE_FACETS_ENABLED);
        if (conf.valueExists(CONF_ADJUST_DOCUMENT_FIELDS)) {
            defaultDocumentFields = new ManyToManyMap(
                conf.getStrings(CONF_ADJUST_DOCUMENT_FIELDS));
        }
        if (conf.valueExists(CONF_ADJUST_FACET_FIELDS)) {
            defaultFacetFields = new ManyToManyMap(
                conf.getStrings(CONF_ADJUST_FACET_FIELDS));
        }
        if (conf.valueExists(CONF_ADJUST_FACET_TAGS)) {
            List<Configuration> taConfs;
            try {
                taConfs = conf.getSubConfigurations(CONF_ADJUST_FACET_TAGS);
            } catch (SubConfigurationsNotSupportedException e) {
                throw new ConfigurationException(
                    "Expected a list of sub configurations for key "
                    + CONF_ADJUST_FACET_TAGS + " but the current Configuration "
                    + "does not support them", e);
            }
            tagAdjusters = new ArrayList<TagAdjuster>(taConfs.size());
            for (Configuration tagConf: taConfs) {
                TagAdjuster tagAdjuster = new TagAdjuster(tagConf);
                tagAdjuster.setID(id);
                tagAdjusters.add(tagAdjuster);
            }
            log.debug("Created " + tagAdjusters.size() + " tag adjusters");
        }
        log.debug(String.format(
            "Constructed search adjuster with id='%s', enabled=%b, "
            + "baseFactor=%f, baseAddition=%f, "
            + "adjustingDocumentFields='%s', "
            + "adjustingFacetFields='%s', tagAdjusters=%d, "
            + "adjustResponseFieldsEnabled=%b, adjustResponseFacetsEnabled=%b",
            id, enabled, baseFactor, baseAddition,
            conf.getStrings(CONF_ADJUST_DOCUMENT_FIELDS,
                            new ArrayList<String>(0)),
            conf.getStrings(CONF_ADJUST_FACET_FIELDS,
                            new ArrayList<String>(0)),
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
        final ManyToManyMap documentFieldMap = resolveMap(
            request, defaultDocumentFields, SEARCH_ADJUST_DOCUMENT_FIELDS);
        final ManyToManyMap facetFieldMap = resolveMap(
            request, defaultFacetFields, SEARCH_ADJUST_FACET_FIELDS);

        if (documentFieldMap == null && facetFieldMap == null &&
            tagAdjusters == null) {
            log.trace("No document fields, facet fields or tag adjusters for "
                      + "rewriteQuery");
            return;
        }

        log.trace("Rewriting fields and content in document filter, query and"
                  + " sort");
        if (request.containsKey(DocumentKeys.SEARCH_FILTER)) {
            request.put(DocumentKeys.SEARCH_FILTER, rewriteQuery(
                request.getString(DocumentKeys.SEARCH_FILTER),
                documentFieldMap, facetFieldMap));
        }
        if (request.containsKey(DocumentKeys.SEARCH_QUERY)) {
            request.put(DocumentKeys.SEARCH_QUERY, rewriteQuery(
                request.getString(DocumentKeys.SEARCH_QUERY),
                documentFieldMap, facetFieldMap));
        }
        if (documentFieldMap != null
            && request.containsKey(DocumentKeys.SEARCH_SORTKEY)) {
            String key = request.getString(DocumentKeys.SEARCH_SORTKEY);
            String replaced[] = documentFieldMap.get(key);
            if (replaced != null) {
                if (replaced.length > 1) {
                    log.warn("Warning: The sort key '" + key + "' will be "
                             + "replaced with multiple values "
                             + Strings.join(replaced, ", "));
                    request.put(DocumentKeys.SEARCH_SORTKEY,
                                Strings.join(replaced, ", "));
                } else {
                    request.put(DocumentKeys.SEARCH_SORTKEY, replaced[0]);
                }
            }
        }
    }

    private String rewriteQuery(
        final String query, final ManyToManyMap... maps)
                                                         throws ParseException {
        return new QueryRewriter(
            new QueryRewriter.Event() {

                // For phrases we only replace the field (if any)
                @Override
                public Query onQuery(PhraseQuery query) {
                    if ("".equals(query.getTerms()[0].field())) {
                        return query;
                    }
                    boolean first = true;
                    String field = "";
                    StringWriter sw = new StringWriter();
                    for (Term term: query.getTerms()) {
                        if (first) {
                            first = false;
                            field = term.field();
                        } else {
                            sw.append(" ");
                        }
                        sw.append(term.text());
                    }
                    List<Pair<String, String>> terms = makeTerms(
                        field, sw.toString(), maps);
                    Query result = makeQuery(terms, query.getBoost());
                    if (result instanceof PhraseQuery) {
                        ((PhraseQuery)result).setSlop(query.getSlop());
                    }
                    return result;
                }

                @Override
                public Query onQuery(TermQuery query) {
                    if ("".equals(query.getTerm().field())) {
                        return query;
                    }
                    List<Pair<String, String>> terms = makeTerms(
                        query.getTerm().field(), query.getTerm().text(), maps);
                    Query result = makeQuery(terms, query.getBoost());
                    if (log.isTraceEnabled()) {
                        log.trace("rewriteQuery(query) changed " + query
                                  + " to " + result);
                    }
                    return result;
                }

                @Override
                public Query onQuery(final TermRangeQuery query) {
                    return handleFieldExpansionQuery(
                        query, query.getField(), new FieldExpansionCallback() {
                            @Override
                            public Query createQuery(String field) {
                                return new TermRangeQuery(
                                    field, query.getLowerTerm(),
                                    query.getUpperTerm(), query.includesLower(),
                                    query.includesUpper());
                            }
                        }, maps);
                }

                @Override
                public Query onQuery(final PrefixQuery query) {
                    return handleFieldExpansionQuery(
                        query, query.getField(), new FieldExpansionCallback() {
                            @Override
                            public Query createQuery(String field) {
                                return new PrefixQuery(
                                    new Term(field,new BytesRef(
                                        query.getPrefix().bytes())));
                            }
                        }, maps);
                }

                @Override
                public Query onQuery(final FuzzyQuery query) {
                    return handleFieldExpansionQuery(
                        query, query.getField(), new FieldExpansionCallback() {
                            @Override
                            public Query createQuery(String field) {
                                return new FuzzyQuery(
                                    new Term(field,new BytesRef(
                                        query.getTerm().bytes())),
                                    query.getMinSimilarity(),
                                    query.getPrefixLength());
                            }
                        }, maps);
                }

                @Override
                public Query onQuery(Query query) {
                    log.trace("Ignoring query of type "
                              + query.getClass().getSimpleName());
                    return query;
                }
            }).rewrite(query);
    }

    private interface FieldExpansionCallback {
        Query createQuery(String field);
    }
    // Returns original Query (if no expansion), created Query (if 1 expansion)
    // or BooleanQuery (is 2+ expansions)
    // Also sets boost
    private Query handleFieldExpansionQuery(
        final Query originalQuery, final String field,
        FieldExpansionCallback callback, ManyToManyMap... maps) {
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
        String field, ManyToManyMap... maps) {
        if ("".equals(field)) {
            return null;
        }
        Set<String> newFields = null;
        for (ManyToManyMap map: maps) {
            if (map == null) {
                continue;
            }
            if (map.containsKey(field)) {
                if (newFields == null) {
                    newFields = new HashSet<String>();
                }
                Collections.addAll(newFields, map.get(field));
            }
        }
        if (newFields == null) {
            return null;
        }
        return newFields;
    }

    private Query makeQuery(List<Pair<String, String>> terms, float boost) {
        if (terms.size() == 1) {
            Query q = createPhraseOrTermQuery(
                terms.get(0).getKey(), terms.get(0).getValue());
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

    private List<Pair<String, String>> makeTerms(
        final String field, final String text,
        final ManyToManyMap... maps) {
        String[] newFields = null;
        for (ManyToManyMap map: maps) {
            if (map != null && map.containsKey(field)) {
                newFields = map.get(field);
                break;
            }
        }
        if (newFields == null) {
            newFields = new String[]{field};
        }

        // We base this on old field, as it is the normalised field
        String[] newTexts = null;
        if (tagAdjusters != null) {
            for (TagAdjuster tagAdjuster: tagAdjusters) {
                if (tagAdjuster.getFacetNames().contains(field)) {
                    newTexts = tagAdjuster.getReverse(text);
                }
            }
        }
        if (newTexts == null) {
            newTexts = new String[]{text}; // No transformation
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
        Query newQuery;
        if (text.contains(" ")) {
            PhraseQuery phraseQuery = new PhraseQuery();
            String[] tokens = text.split(" +");
            for (String term: tokens) {
                phraseQuery.add(new Term(field, term));
            }
            newQuery = phraseQuery;
        } else {
            newQuery = new TermQuery(new Term(field, text));
        }
        return newQuery;
    }

    private void rewriteFacetFields(Request request) {
        log.trace("rewriteFacetFields called");
        ManyToManyMap facetFieldMap = resolveMap(
            request, defaultFacetFields, SEARCH_ADJUST_FACET_FIELDS);
        if (facetFieldMap == null) {
            return;
        }
        log.trace("Adjusting fields in facet request");
        if (request.containsKey(FacetKeys.SEARCH_FACET_FACETS)) {
            List<String> facets = request.getStrings(
                FacetKeys.SEARCH_FACET_FACETS);
            List<String> adjusted = new ArrayList<String>(facets.size() * 2);
            for (String facet: facets) {
                if (facetFieldMap.containsKey(facet)) {
                    String[] alts = facetFieldMap.get(facet);
                    Collections.addAll(adjusted, alts);
                } else {
                    adjusted.add(facet);
                }
            }
            request.put(FacetKeys.SEARCH_FACET_FACETS,
                        Strings.join(adjusted, ", "));
        }
    }

    private ManyToManyMap resolveMap(
        Request request, ManyToManyMap defaultMap, String key) {
        log.trace("resolveMap called");
        ManyToManyMap map = defaultMap;
        if (request.containsKey(key)) {
            map = new ManyToManyMap(request.getStrings(key));
        }
        if (request.containsKey(prefix + key)) {
            map = new ManyToManyMap(request.getStrings(prefix + key));
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
        responses.addTiming("interactionadjuster.adjust.documents",
                            System.currentTimeMillis() - startTime);
        if (request.getBoolean(SEARCH_ADJUST_RESPONSE_FACETS_ENABLED,
                               adjustResponseFacetsEnabled)) {
            adjustFacets(request, responses);
            responses.addTiming("interactionadjuster.adjust.facets",
                                System.currentTimeMillis() - startTime);
        }
        responses.addTiming("interactionadjuster.adjust.total",
                            System.currentTimeMillis() - startTime);
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
                log.error("adjustDocuments found response wil name "
                          + DocumentResponse.NAME + " and expected Class "
                          + DocumentResponse.class + " but got "
                          + response.getClass());
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

    private void adjustFacets(
        Request request, ResponseCollection responses) {
        log.trace("adjustFacets called");
        FacetResultExternal facetResponse = null;
        for (Response response: responses) {
            if (!FacetResultExternal.NAME.equals(response.getName())) {
                continue;
            }
            // TODO: Requiring FacetResultExternal is too harsh
            if (!(response instanceof FacetResultExternal)) {
                log.error("adjustDocuments found response wil name "
                          + DocumentResponse.NAME + " and expected Class "
                          + DocumentResponse.class + " but got "
                          + response.getClass());
                continue;
            }
            facetResponse = (FacetResultExternal)response;
        }
        if (facetResponse == null) {
            log.debug(
                "No FacetResponseExternal found in adjustDocuments. Exiting");
            return;
        }
        replaceFacetFields(request, facetResponse);
        long startAdjust = System.currentTimeMillis();
        if (tagAdjusters != null) {
            for (TagAdjuster tagAdjuster: tagAdjusters) {
                tagAdjuster.adjust(facetResponse);
            }
        }
        facetResponse.addTiming("tagadjuster.adjusts.total",
                                System.currentTimeMillis() - startAdjust);
    }

    private boolean warnedOnIncompleteFacetMap = false;
    private void replaceFacetFields(
        Request request, FacetResultExternal facetResponse) {
        log.trace("adjustFacetFields called");
        ManyToManyMap facetMap = resolveMap(
            request, defaultFacetFields, SEARCH_ADJUST_FACET_FIELDS);
        if (facetMap == null) {
            return;
        }
        Map<String, String> reversedSimplified =
            new HashMap<String, String>(facetMap.size());
        for (Map.Entry<String, String[]> entry: facetMap.entrySet()) {
            for (String dest: entry.getValue()) {
                if (reversedSimplified.containsKey(entry.getValue())
                    && !warnedOnIncompleteFacetMap) {
                    warnedOnIncompleteFacetMap = true;
                    log.warn(String.format(
                        "Encountered reverse mapping from destination '%s' to "
                        + "source '%s' with an existing source '%s'. Multiple "
                        + "sources is not yet supported",
                        dest, entry.getKey(),
                        reversedSimplified.containsKey(entry.getValue())));
                }
                reversedSimplified.put(dest, entry.getKey());
            }
        }
        facetResponse.renameFacetsAndFields(reversedSimplified);
    }

    private void replaceDocumentFields(
        Request request, DocumentResponse documentResponse) {
        log.trace("replaceDocumentFields called");
        long startTime = System.currentTimeMillis();
        ManyToManyMap docFieldMap = resolveMap(
            request, defaultDocumentFields, SEARCH_ADJUST_DOCUMENT_FIELDS);
        if (docFieldMap == null) {
            return;
        }

        if (documentResponse.getSortKey() != null
            && docFieldMap.reverseContainsKey(documentResponse.getSortKey())) {
            documentResponse.setSortKey(Strings.join(
                docFieldMap.reverseGet(documentResponse.getSortKey()), ", "));
        }
        if (!request.getBoolean(SEARCH_ADJUST_RESPONSE_FIELDS_ENABLED,
                               adjustResponseFieldsEnabled)) {
            return;
        }
        log.trace("Replacing document fields (" + docFieldMap.size()
                  + " replacements)");
        for (DocumentResponse.Record record: documentResponse.getRecords()) {
            List<DocumentResponse.Field> newFields =
                new ArrayList<DocumentResponse.Field>(
                    record.getFields().size()*2);
            for (DocumentResponse.Field field: record.getFields()) {
                if (docFieldMap.reverseContainsKey(field.getName())) {
                    if (log.isTraceEnabled()) {
                        log.trace("Changing field name '" + field.getName()
                                  + "' to '"+ Strings.join(
                            docFieldMap.reverseGet(field.getName()), ", ")
                                  + " for " + record.getId());
                    }
                    String[] alts = docFieldMap.reverseGet(field.getName());
                    if (alts.length == 1) { // 1:1
                        field.setName(alts[0]);
                        newFields.add(field);
                    } else { // 1:n
                        for (String alt: alts) {
                            newFields.add(new DocumentResponse.Field(
                                alt, field.getContent(),
                                field.isEscapeContent()));
                        }
                    }
                } else { // No mapping
                    newFields.add(field);
                }
            }
            record.getFields().clear();
            record.getFields().addAll(newFields);
        }
        documentResponse.addTiming("interactionadjuster.replacedocumentfields",
                                   System.currentTimeMillis() - startTime);
    }

    /**
     * If the responses contain a {@link DocumentResponse}, the scores for the
     * documents are adjusted with the given factor and addition.
     * @param request   potential tweaks to factor and addition.
     * @param documentResponse the response to adjustAll.
     */
    private void adjustDocumentScores(
        Request request, DocumentResponse documentResponse) {
        log.trace("adjustDocumentScores called");
        double factor = baseFactor;
        double addition = baseAddition;
        if (request.containsKey(SEARCH_ADJUST_SCORE_MULTIPLY)) {
            factor *= request.getDouble(SEARCH_ADJUST_SCORE_MULTIPLY);
        }
        if (request.containsKey(prefix + SEARCH_ADJUST_SCORE_MULTIPLY)) {
            factor *= request.getDouble(prefix + SEARCH_ADJUST_SCORE_MULTIPLY);
        }
        if (request.containsKey(SEARCH_ADJUST_SCORE_ADD)) {
            addition += request.getDouble(SEARCH_ADJUST_SCORE_ADD);
        }
        if (request.containsKey(prefix + SEARCH_ADJUST_SCORE_ADD)) {
            addition += request.getDouble(prefix + SEARCH_ADJUST_SCORE_ADD);
        }
        // It is okay to compare as worst case is an unnecessary adjustment
        //noinspection FloatingPointEquality
        if (addition == 0 && factor == 1.0) {
            log.trace("No adjustment to make to scores "
                      + "(factor == 1.0, addition == 0.0");
            return;
        }

        log.trace("adjustDocuments called with factor " + factor + ", addition "
                  + addition);
        for (DocumentResponse.Record record: documentResponse.getRecords()){
            record.setScore((float)(record.getScore() * factor + addition));
        }
    }

    public String getId() {
        return id;
    }
}
