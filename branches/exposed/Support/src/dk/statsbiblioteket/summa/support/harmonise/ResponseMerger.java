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
import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.summa.common.util.Triple;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Merges responses from different searchers based on specified strategies.
 * Intended for federated or fine tuning of distributed integrated search.
 * </p><p>
 * Properties are generally specified both at startup and runtime, where runtime
 * overrides startup. 
 * </p><p>
 * Currently only {@link DocumentResponse}s are explicitly processed.
 * Other results are merged with the default merger.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
// TODO: slope (measuring steep drops in scores)
public class ResponseMerger implements Configurable {
    private static Log log = LogFactory.getLog(ResponseMerger.class);

    /**
     * The overall way of merging DocumentResponses.
     * </p><p>
     * Optional. Default is 'score'. 
     * @see {@link MERGE_MODE} for details.
     */
    public static final String CONF_MODE = "responsemerger.mode";
    public static final MERGE_MODE DEFAULT_MODE = MERGE_MODE.score;
    public static final String SEARCH_MODE = CONF_MODE;
    
    /**
     * After performing a general merge, the resulting list can be 
     * post-processed.
     * </p><p>
     * Optional. Default is 'none'. 
     * @see {@link MERGE_POST} for details.
     */
    public static final String CONF_POST = "responsemerger.post";
    public static final MERGE_POST DEFAULT_POST = MERGE_POST.none;
    public static final String SEARCH_POST = CONF_POST;

    /**
     * Overall merging mode for documents. Note that some of these modes require
     * extra parameters.
     * score:       Direct sort by score.<br/>
     * concatenate: Directly concatenate the lists of responses.
     *              The parameters {@link #CONF_ORDER} or {@link #SEARCH_ORDER}
     *              must be specified to use this merger.<br/>
     * interleave:  The first documents from all results are inserted after each
     *              other, then the second and so forth.
     *              The parameters {@link #CONF_ORDER} or {@link #SEARCH_ORDER}
     *              must be specified to use this merger.<br/>
     */
    public static enum MERGE_MODE {
        /**
         * Direct sort by score. This is equivalent to the default merger for
         * DocumentResponses.
         */
        score,
        /**
         * Directly concatenate the lists of responses.
         * The parameters {@link #CONF_ORDER} or {@link #SEARCH_ORDER} should be
         * specified to use this merger.
         */
        concatenate,
        /**
         * The first documents from all results are inserted after each other,
         * then the second and so forth.
         * The parameters {@link #CONF_ORDER} or {@link #SEARCH_ORDER} should be
         * specified to use this merger.
         */
        interleave
    }

    /**
     * Used with {@link MERGE_MODE#concatenate} and
     * {@link MERGE_MODE#interleave} to specify the order of the sources.
     * </p><p>
     * Optional but highly recommended. The IDs for all possible sources
     * is stated separated by comma. Sources not specified here will be merged
     * after those stated and in order of appearance (i.e. semi-randomly).
     * Example: 'sb, summa, kb'.
     */
    public static final String CONF_ORDER = "responsemerger.order";
    public static final String SEARCH_ORDER = CONF_ORDER;

    /**
     * Post-merge adjustment of the order of the documents.<br/>
     * none:    No post-processing.<br/>
     * enforce: Force a certain number of documents from stated sources to
     *          appear as top-X.
     *          The parameters {@link #CONF_FORCE_TOPX} and
     *          {@link #CONF_FORCE_RULES} must be specified to use this
     *          post-processor.
     */
    public static enum MERGE_POST {none, enforce}

    /**
     * Documents from the sources specified in {@link #CONF_FORCE_RULES} will
     * be forced to appear among the first X documents returned. To make room.
     * documents from sources not specified in the rules will be pushed down
     * in the list, starting from document #X and up.
     * </p><p>
     * This option will only have effect if {@link #CONF_FORCE_RULES} is set.
     * </p><p>
     * Optional. Default is 20.
     */
    public static final String CONF_FORCE_TOPX = "responsemerger.force.topx";
    public static final String SEARCH_FORCE_TOPX = CONF_FORCE_TOPX;
    public static final int DEFAULT_FORCE_TOPX = 20;

    /**
     * The sources of documents to force into the top X documents, specified
     * as a comma-separated list of source IDs with the wanted number in
     * parenthesis. Example: 'sb(3), kb(2)'.
     * </p><p>
     * Optional. Default is none.
     */
    public static final String CONF_FORCE_RULES = "responsemerger.force.rules";
    public static final String SEARCH_FORCE_RULES = CONF_FORCE_RULES;

    /**
     * If true, the response collections are treated as being generated
     * sequentially. If true, time measurements are added, if false, time
     * measurements are calculated by taking the maximum value.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_SEQUENTIAL = "responsemerger.sequential";
    public static final boolean DEFAULT_SEQUENTIAL = false;

    private MERGE_MODE defaultMode = DEFAULT_MODE;
    private MERGE_POST defaultPost = DEFAULT_POST;
    private List<String> defaultOrder = new ArrayList<String>();
    private int defaultForceTopX = DEFAULT_FORCE_TOPX;
    private List<Pair<String, Integer>> defaultForceRules = null;
    private boolean sequential = DEFAULT_SEQUENTIAL;
    private final long maxRecords;

    public ResponseMerger(Configuration conf) {
        defaultMode = MERGE_MODE.valueOf(
            conf.getString(CONF_MODE, defaultMode.toString()));
        defaultPost = MERGE_POST.valueOf(
            conf.getString(CONF_POST, defaultPost.toString()));
        defaultOrder = conf.getStrings(CONF_ORDER, defaultOrder);
        defaultForceTopX = conf.getInt(CONF_FORCE_TOPX, defaultForceTopX);
        if (conf.valueExists(CONF_FORCE_RULES)) {
            defaultForceRules =
                parseForceRules(conf.getString(CONF_FORCE_RULES));
        }
        sequential = conf.getBoolean(CONF_SEQUENTIAL, sequential);
        maxRecords = conf.getLong(
            DocumentSearcher.CONF_RECORDS, DocumentSearcher.DEFAULT_RECORDS);
        log.debug("Created response merger");
    }

    /**
     * Merges the given packages and returns the result based on basic setup and
     * parameters given in the request. 
     * @param request  the original request then resulted in the packages.
     * @param packages a collection og ResponseCollections to merge.
     * @return a merge of the given ResponseCollections.
     */
    public ResponseCollection merge(
        Request request,
        List<Triple<String, Request, ResponseCollection>> packages) {
        ResponseCollection merged = new ResponseCollection();
        merge(request, merged, packages);
        postProcess(request, merged, packages);
        trim(request, merged);
        return merged;
    }

    private void trim(Request request, ResponseCollection merged) {
        log.trace("trim called");
        long maxRecords = request.getLong(
            DocumentKeys.SEARCH_MAX_RECORDS, this.maxRecords);
        for (Response response: merged) {
            if (response instanceof DocumentResponse) {
                DocumentResponse docResponse = ((DocumentResponse)response);
                List<DocumentResponse.Record> rs = docResponse.getRecords();
                int newSize = Math.min((int)maxRecords, rs.size());
                log.trace("Trimming " + rs.size() + " to " + newSize);
                docResponse.setRecords(new ArrayList<DocumentResponse.Record>(
                    rs.subList(0, newSize)));
            }
        }
    }

    private void merge(
        Request request, ResponseCollection merged,
        List<Triple<String, Request, ResponseCollection>> packages) {
        log.trace("merge called");
        MERGE_MODE mode = MERGE_MODE.valueOf(
            request.getString(SEARCH_MODE, defaultMode.toString()));
        List<String> order = request.getStrings(SEARCH_ORDER, defaultOrder);
        switch (mode) {
            case score: {
                for (Triple<String, Request, ResponseCollection> mp: packages) {
                    merged.addAll(mp.getValue3());
                }
                break;
            }
            case concatenate:
            case interleave: {
                List<Pair<String, DocumentResponse>> documentResponses =
                    new ArrayList<Pair<String, DocumentResponse>>(packages.size());
                for (Triple<String, Request, ResponseCollection> mp: packages) {
                    for (Response response: mp.getValue3()) {
                        if (response instanceof DocumentResponse) {
                            documentResponses.add(
                                new Pair<String, DocumentResponse>(
                                    mp.getValue1(),(DocumentResponse)response));
                        } else {
                            merged.add(response);
                        }

                    }
                }
                documentMerge(merged, mode, order, documentResponses);
                break;
            }
            default: throw new UnsupportedOperationException(
                "Merge mode " + mode + " not supported yet");
        }
    }

    private void documentMerge(
        ResponseCollection merged, MERGE_MODE mode, List<String> order,
        List<Pair<String, DocumentResponse>> documentResponses) {
        log.trace("documentMerge called with mode " + mode + " and order "
                  + order + " for " + documentResponses.size() + " responses");
        documentResponses = sort(documentResponses, order);
        if (documentResponses.size() <= 1) {
            log.debug("Only " + documentResponses.size() + " responses, "
                      + "documentMerge skipped");
            return;
        }
        switch (mode) {
            case concatenate: {
                documentMergeConcatenate(merged, documentResponses);
                break;
            }
            case interleave: {
                documentMergeInterleave(merged, documentResponses);
                break;
            }
            default: throw new UnsupportedOperationException(
                "Document merge mode " + mode + " not supported yet");
        }
    }

    private List<Pair<String, DocumentResponse>> sort(
        List<Pair<String, DocumentResponse>> documentResponses,
        List<String> order) {
        log.trace("sort called with " + documentResponses.size()
                  + " document responses");
        if (documentResponses.size() <= 0) {
            return documentResponses;
        }
        List<Pair<String, DocumentResponse>> drs =
            new ArrayList<Pair<String, DocumentResponse>>(
                documentResponses.size());
        for (String id : order) {
            for (int dr = 0; dr < documentResponses.size(); dr++) {
                if (documentResponses.get(dr).getKey().equals(id)) {
                    drs.add(documentResponses.remove(dr));
                    break;
                }
            }
        }
        for (Pair<String, DocumentResponse> dr: documentResponses) {
            drs.add(dr);
        }
        return drs;
    }

    // It is assumed that the document responses are ordered and that there are
    // at least 2 documentResponses
    private void documentMergeInterleave(
        ResponseCollection merged,
        List<Pair<String, DocumentResponse>> documentResponses) {
        log.trace("documentMergeInterleave called");
        List<Pair<String, DocumentResponse>> working =
            new ArrayList<Pair<String, DocumentResponse>>(
                documentResponses.size());
        for (Pair<String, DocumentResponse> response: documentResponses) {
            working.add(response);
        }
        List<DocumentResponse.Record> records =
            new ArrayList<DocumentResponse.Record>(
                working.size() *
                working.get(0).getValue().getRecords().size() * 2);
        while (working.size() > 0) {
            Iterator<Pair<String, DocumentResponse>> iterator =
                working.iterator();
            while (iterator.hasNext()) {
                Pair<String, DocumentResponse> response = iterator.next();
                if (response.getValue().getRecords().size() == 0) {
                    iterator.remove();
                    continue;
                }
                // TODO: A bit inefficient with arraylists. Could be optimized
                records.add(response.getValue().getRecords().remove(0));
            }
        }
        Pair<String, DocumentResponse> base = documentResponses.remove(0);
        mergeHitcountAndTime(base, documentResponses);
        base.getValue().setRecords(records);
        merged.add(base.getValue());
    }

    // It is assumed that the document responses are ordered and that there are
    // at least 2 documentResponses
    private void documentMergeConcatenate(
        ResponseCollection merged,
        List<Pair<String, DocumentResponse>> documentResponses) {
        log.trace("documentMergeConcatenate called");
        // Responses are now in the given order
        Pair<String, DocumentResponse> base = documentResponses.remove(0);
        for (Pair<String, DocumentResponse> dr: documentResponses) {
            base.getValue().getRecords().addAll(dr.getValue().getRecords());
        }
        mergeHitcountAndTime(base, documentResponses);
        merged.add(base.getValue());
    }

    private void mergeHitcountAndTime(
        Pair<String, DocumentResponse> base,
        List<Pair<String, DocumentResponse>> documentResponses) {
        for (Pair<String, DocumentResponse> dr: documentResponses) {
            base.getValue().setHitCount(
                base.getValue().getHitCount() + dr.getValue().getHitCount());
            base.getValue().setSearchTime(
                sequential ?
                base.getValue().getSearchTime() + dr.getValue().getSearchTime():
                Math.max(base.getValue().getSearchTime(),
                         dr.getValue().getSearchTime())
            );
        }
    }

    private void postProcess(
        Request request, ResponseCollection merged,
        List<Triple<String, Request, ResponseCollection>> packages) {
        MERGE_POST post = MERGE_POST.valueOf(
            request.getString(SEARCH_POST, defaultPost.toString()));
        if (post == MERGE_POST.none) {
            return;
        }
        List<String> order = request.getStrings(SEARCH_ORDER, defaultOrder);
        int forceTopX = request.getInt(SEARCH_FORCE_TOPX, defaultForceTopX);
        List<Pair<String, Integer>> forceRules = defaultForceRules;
        if (request.containsKey(SEARCH_FORCE_RULES)) {
            forceRules = parseForceRules(request.getString(CONF_FORCE_RULES));
        }
        switch (post) {
            default: throw new UnsupportedOperationException(
                "Post merge processing does not yet support '" + post + "'");
        }
    }

    private List<Pair<String, Integer>> parseForceRules(String s) {
        String[] ruleTokens = s.split(" *, *");
        if (ruleTokens.length == 0) {
            log.trace("parceForceRules found no rules in '" + s + "'");
            return null;
        }
        List<Pair<String, Integer>> rules =
            new ArrayList<Pair<String, Integer>>(ruleTokens.length);
        for (String ruleToken: ruleTokens) {
            // zoo(12)
            String[] subTokens = ruleToken.split(" *\\(", 2);
            // zoo
            if (subTokens.length < 2) {
                throw new IllegalArgumentException(
                    "The syntax for a rule is 'id(minCount)' but the input "
                    + "was '" + ruleToken + "'");
            }
            // "  5)  "
            String noParen = subTokens[1].split("\\)", 2)[0].trim();
            Integer count = Integer.parseInt(noParen);
            log.debug("parseForceRules adding rule " + subTokens[0] + ", "
                      + count);
            rules.add(new Pair<String, Integer>(subTokens[0], count));
        }
        return rules;
    }

}
