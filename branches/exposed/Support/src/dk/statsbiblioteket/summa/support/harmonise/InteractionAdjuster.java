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
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.Serializable;
import java.util.HashMap;
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
 * Note that there are CONF-equivalents to some SEARCH-arguments. Effects are
 * cumulative.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
// TODO: disable facets? empty/null? force specific facets?
// TODO: Rewrite facet-names (many-to-many?)
// TODO: Map tag names (many-to-many)
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
     * Add a constant to returned scores for documents.
     * Additions are performed after multiplications.
     */
    public static final String SEARCH_ADJUST_SCORE_ADD = "adjust.score.add";
    public static final String CONF_ADJUST_SCORE_ADD = SEARCH_ADJUST_SCORE_ADD;

    /**
     * Multiply the returned scores for documents with a constant.
     * Multiplications are performed before additions.
     */
    public static final String SEARCH_ADJUST_SCORE_MULTIPLY =
        "adjust.score.multiply";
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
        "adjust.document.fields";
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
        "adjust.facet.fields";
    public static final String 
        SEARCH_ADJUST_FACET_FIELDS = CONF_ADJUST_FACET_FIELDS;

    private final String id;
    private final String prefix;
    private double baseFactor = 1.0;
    private double baseAddition = 0.0;
    private Map<String, String> defaultDocumentFields = null;
    private Map<String, String> defaultFacetFields = null;

    public InteractionAdjuster(Configuration conf) {
        id = conf.getString(CONF_IDENTIFIER);
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
        log.debug("Constructed search adjuster for '" + id + "'");
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
        Request adjusted = clone(request);
        rewriteDocumentQueryFields(request);
        rewriteFacetQueryFields(request);
        return adjusted;
    }

    private void rewriteDocumentQueryFields(Request request) {
        Map<String, String> documentFields = resolveMap(
            request, defaultDocumentFields, SEARCH_ADJUST_DOCUMENT_FIELDS);
        if (documentFields == null) {
            return;
        }
        log.trace("Adjusting fields in document filter and query");
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
        Map<String, String> facetFields = resolveMap(
            request, defaultFacetFields, SEARCH_ADJUST_FACET_FIELDS);
        if (facetFields == null) {
            return;
        }
        log.trace("Adjusting fields in facet request");
        if (request.containsKey(FacetKeys.DocumentKeys.SEARCH_FILTER)) {
            request.put(DocumentKeys.SEARCH_FILTER, replaceFields(
                request.getString(DocumentKeys.SEARCH_FILTER),
                facetFields));
        }
        if (request.containsKey(DocumentKeys.SEARCH_QUERY)) {
            request.put(DocumentKeys.SEARCH_QUERY, replaceFields(
                request.getString(DocumentKeys.SEARCH_QUERY),
                facetFields));
        }
    }

    private Map<String, String> resolveMap(
        Request request, Map<String, String> defaultMap, String key) {
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
            query = query.replaceAll(entry.getKey() + ":", entry.getValue());
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

    public void adjust(Request request, ResponseCollection responses) {
        adjustDocuments(request, responses);
    }

    private void adjustDocuments(
        Request request, ResponseCollection responses) {
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

    private void replaceDocumentFields(
        Request request, DocumentResponse documentResponse) {
        Map<String, String> documentFields = resolveMap(
            request, defaultDocumentFields, SEARCH_ADJUST_DOCUMENT_FIELDS);
        if (documentFields == null) {
            return;
        }
        Map<String, String> reverse = new HashMap<String, String>(documentFields.size());
        for (Map.Entry<String, String> entry: documentFields.entrySet()) {
            reverse.put(entry.getValue(), entry.getKey());
        }

        log.trace("Replacing document fields (" + documentFields.size()
                  + " replacements)");
        for (DocumentResponse.Record record: documentResponse.getRecords()) {
            for (DocumentResponse.Field field: record.getFields()) {
                if (reverse.containsValue(field.getName())) {
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

    /**
     * If the responses contain a {@link DocumentResponse}, the scores for the
     * documents are adjusted with the given factor and addition.
     * @param request   potential tweaks to factor and addition.
     * @param documentResponse the response to adjust.
     */
    private void adjustDocumentScores(
        Request request, DocumentResponse documentResponse) {
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
        if (baseAddition == 0 && baseFactor == 1.0) {
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
