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

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Map<String, String> defaultDocumentFields = null;
    private Map<String, String> defaultFacetFields = null;
    private List<TagAdjuster> tagAdjusters = null;
    private boolean enabled = DEFAULT_ADJUST_ENABLED;

    public InteractionAdjuster(Configuration conf)
                                                 throws ConfigurationException {
        id = conf.getString(CONF_IDENTIFIER);
        enabled = conf.getBoolean(CONF_ADJUST_ENABLED, enabled);
        prefix = id + ".";
        baseFactor = conf.getDouble(CONF_ADJUST_SCORE_MULTIPLY, baseFactor);
        baseAddition = conf.getDouble(CONF_ADJUST_SCORE_ADD, baseAddition);
        if (conf.valueExists(CONF_ADJUST_DOCUMENT_FIELDS)) {
            defaultDocumentFields = parseSingleMapRules(
                conf.getString(CONF_ADJUST_DOCUMENT_FIELDS));
        }
        if (conf.valueExists(CONF_ADJUST_FACET_FIELDS)) {
            defaultFacetFields = parseSingleMapRules(
                conf.getString(CONF_ADJUST_FACET_FIELDS));
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
                tagAdjusters.add(new TagAdjuster(tagConf));
            }
            log.debug("Created " + tagAdjusters.size() + " tag adjusters");
        }
        log.debug(String.format(
            "Constructed search adjuster with id='%s', enabled=%b, "
            + "baseFactor=%f, baseAddition=%f, "
            + "adjustingDocumentFields='%s', "
            + "adjustingFacetFields='%s', tagAdjusters=%d",
            id, enabled, baseFactor, baseAddition,
            conf.getString(CONF_ADJUST_DOCUMENT_FIELDS, ""),
            conf.getString(CONF_ADJUST_FACET_FIELDS, ""),
            tagAdjusters == null ? 0 : tagAdjusters.size()));
    }

    // a - b, c - d, e - f
    private Map<String, String> parseSingleMapRules(String str) {
        String[] rules = str.split(" *, *");
        if (rules.length == 0 || (rules.length == 1 && "".equals(rules[0]))) {
            return null;
        }
        Map<String, String> map = new HashMap<String, String>(rules.length);
        for (String rule: rules) {
            String[] parts = rule.split(" *- *");
            if (parts.length != 2) {
                throw new IllegalArgumentException(
                    "Unable to split '" + rule + "' from '" + str
                    + "' in exactly 2 parts using the delimiter '-'");
            }
            map.put(parts[0].trim(), parts[1].trim());
        }
        return map;
    }

    /**
     * Creates a copy of the provided request and rewrites arguments according
     * to settings and request-time arguments, then returns the adjusted
     * request.
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
        rewriteDocumentQueryFields(adjusted);
        rewriteFacetQueryFields(adjusted);
        return adjusted;
    }

    private void rewriteDocumentQueryFields(Request request) {
        log.trace("rewriteDocumentQueryFields called");
        Map<String, String> documentFields = resolveMap(
            request, defaultDocumentFields, SEARCH_ADJUST_DOCUMENT_FIELDS);
        if (documentFields == null) {
            log.trace("No document fields for rewriteDocumentQueryFields");
            return;
        }
        log.trace("rewriting fields in document filter and query");
        if (request.containsKey(DocumentKeys.SEARCH_FILTER)) {
            request.put(DocumentKeys.SEARCH_FILTER, replaceFields(
                request.getString(DocumentKeys.SEARCH_FILTER),
                documentFields));
        }
        if (request.containsKey(DocumentKeys.SEARCH_QUERY)) {
            request.put(DocumentKeys.SEARCH_QUERY, replaceFields(
                request.getString(DocumentKeys.SEARCH_QUERY),
                documentFields));
        }
    }

    private void rewriteFacetQueryFields(Request request) {
        log.trace("rewriteFacetQueryFields called");
        Map<String, String> facetMap = resolveMap(
            request, defaultFacetFields, SEARCH_ADJUST_FACET_FIELDS);
        if (facetMap == null) {
            return;
        }
        log.trace("Adjusting fields in facet request");
        if (request.containsKey(FacetKeys.SEARCH_FACET_FACETS)) {
            String[] facets =
                request.getString(FacetKeys.SEARCH_FACET_FACETS).split(" *, *");
            for (int i = 0 ; i < facets.length ; i++) {
                if (facetMap.containsKey(facets[i])) {
                    facets[i] = facetMap.get(facets[i]);
                }
            }

            request.put(FacetKeys.SEARCH_FACET_FACETS,
                        Strings.join(facets, ", "));
        }
    }

    private Map<String, String> resolveMap(
        Request request, Map<String, String> defaultMap, String key) {
        log.trace("resolveMap called");
        Map<String, String> map = defaultMap;
        if (request.containsKey(key)) {
            map = parseSingleMapRules(request.getString(key));
        }
        if (request.containsKey(prefix + key)) {
            map = parseSingleMapRules(request.getString(prefix + key));
        }
        return map;
    }
    
    // TODO: Make a proper parser that handles quotes
    private Serializable replaceFields(
        String query, Map<String, String> documentFields) {
        for (Map.Entry<String, String> entry: documentFields.entrySet()) {
            // TODO: Optimize by using the replace-framework
            query = query.replace(entry.getKey() + ":", entry.getValue() + ":");
        }
        return query;
    }

    private Request clone(Request request) {
        Request cloned = new Request();
        for (Map.Entry<String, Serializable> entry: request.entrySet()) {
            cloned.put(entry.getKey(), entry.getValue());
        }
        return cloned;
    }

    /**
     * Modifies the responses according to the given settings.
     * @param request   the rewritten request that resulted in the responses.
     * @param responses non-modified responses.
     */
    public void adjust(Request request, ResponseCollection responses) {
        log.trace("adjust called");
        if (!request.getBoolean(SEARCH_ADJUST_ENABLED, enabled)) {
            log.trace("The adjuster is disabled. Exiting adjust");
            return;
        }
        adjustDocuments(request, responses);
        adjustFacets(request, responses);
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
        if (tagAdjusters != null) {
            for (TagAdjuster tagAdjuster: tagAdjusters) {
                tagAdjuster.adjust(facetResponse);
            }
        }
    }

    private void replaceFacetFields(
        Request request, FacetResultExternal facetResponse) {
        log.trace("adjustFacetFields called");
        Map<String, String> reverse = resolveReversedMap(
            request, defaultFacetFields, SEARCH_ADJUST_FACET_FIELDS);
        if (reverse == null) {
            return;
        }
        facetResponse.renameFacetsAndFields(reverse);
    }

    private void replaceDocumentFields(
        Request request, DocumentResponse documentResponse) {
        log.trace("replaceDocumentFields called");
        Map<String, String> reverse = resolveReversedMap(
            request, defaultDocumentFields, SEARCH_ADJUST_DOCUMENT_FIELDS);
        if (reverse == null) {
            return;
        }

        log.trace("Replacing document fields (" + reverse.size()
                  + " replacements)");
        for (DocumentResponse.Record record: documentResponse.getRecords()) {
            for (DocumentResponse.Field field: record.getFields()) {
                if (reverse.containsKey(field.getName())) {
                    if (log.isTraceEnabled()) {
                        log.trace("Changing field name '" + field.getName()
                                  + "' to '" + reverse.get(field.getName())
                                  + " for " + record.getId());
                    }
                    field.setName(reverse.get(field.getName()));
                }
            }
        }
    }

    private Map<String, String> resolveReversedMap(
        Request request, Map<String, String> defaultFields, String key) {
        Map<String, String> fields = resolveMap(
            request, defaultFields, key);
        if (fields == null) {
            return null;
        }
        Map<String, String> reverse = new HashMap<String, String>(fields.size());
        for (Map.Entry<String, String> entry: fields.entrySet()) {
            reverse.put(entry.getValue(), entry.getKey());
        }
        return reverse;
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
