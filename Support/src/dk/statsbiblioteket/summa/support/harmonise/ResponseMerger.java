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
import dk.statsbiblioteket.summa.search.SummaSearcherAggregator;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.*;

/**
 * Merges responses from different searchers based on specified strategies.
 * Intended for federated or fine tuning of distributed integrated search.
 * </p><p>
 * Seen from the sky, this class deconstructs received responses into
 * DocumentResponses and everything else. Responses that are not
 * DocumentResponses are passed on as usual. DocumentResponses are deconstructed
 * into records paired with the ID from the searcher that produced them. Those
 * records are merged into a list, adjusted according to preference and
 * extracted to a single DocumentResponse that is returned with the unmodified
 * responses.
 * </p><p>
 * Properties are generally specified both at startup and runtime, where runtime
 * overrides startup. 
 * </p><p>
 * Currently only {@link DocumentResponse}s are explicitly processed.
 * Other results are merged with the default merger.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
// TODO: slope (measuring steep drops in scores)
public class ResponseMerger implements Configurable {
    private static Log log = LogFactory.getLog(ResponseMerger.class);

    /**
     * The overall way of merging DocumentResponses.
     * </p><p>
     * Optional. Default is 'standard'.
     * @see {@link MODE} for details.
     */
    public static final String CONF_MODE = "responsemerger.mode";
    public static final MODE DEFAULT_MODE = MODE.standard;
    public static final String SEARCH_MODE = CONF_MODE;
    
    /**
     * After performing a general merge, the resulting list can be 
     * post-processed.
     * </p><p>
     * Optional. Default is 'none'. 
     * @see {@link POST} for details.
     */
    public static final String CONF_POST = "responsemerger.post";
    public static final POST DEFAULT_POST = POST.none;
    public static final String SEARCH_POST = CONF_POST;

    /**
     * Overall merging mode for documents. Note that some of these modes require
     * extra parameters.
     * standard:    Order provided by the DocumentResponses.<br/>
     * score:       Direct sort by score.<br/>
     * concatenate: Directly concatenate the lists of responses.
     *              The parameters {@link #CONF_ORDER} or {@link #SEARCH_ORDER}
     *              must be specified to use this merger.<br/>
     * interleave:  The first documents from all results are inserted after each
     *              other, then the second and so forth.
     *              The parameters {@link #CONF_ORDER} or {@link #SEARCH_ORDER}
     *              must be specified to use this merger.<br/>
     */
    public static enum MODE {
        /**
         * The standard order for the provided DocumentResponses is used.
         */
        standard,
        /**
         * Direct sort by score. This is equivalent to the default merger for
         * DocumentResponses.
         * </p><p>
         * Note: This will normally be the same as standard. Only use score is
         *       you are absolutely sure that score should be forced.
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
     * Used with {@link MODE#concatenate} and
     * {@link MODE#interleave} to specify the order of the sources.
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
     * ifnodoc: If no documents from a source given in {@link #CONF_FORCE_RULES}
     *          are present in the {@link #CONF_FORCE_TOPX} hits, this works as
     *          the enforced option. If one or more documents are present, no
     *          change is done.
     */
    public static enum POST {none, enforce, ifnone}

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

    private MODE defaultMode = DEFAULT_MODE;
    private POST defaultPost = DEFAULT_POST;
    private List<String> defaultOrder = new ArrayList<String>();
    private int defaultForceTopX = DEFAULT_FORCE_TOPX;
    private List<Pair<String, Integer>> defaultForceRules = null;
    private boolean sequential = DEFAULT_SEQUENTIAL;
    private final long maxRecords;

    public ResponseMerger(Configuration conf) {
        defaultMode = MODE.valueOf(
            conf.getString(CONF_MODE, defaultMode.toString()));
        defaultPost = POST.valueOf(
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

    private static class AdjustWrapper {
        /* Contains everything besides DocumentResponse until externalize is
           called.
         */
        private final ResponseCollection merged = new ResponseCollection();
        /* Merged hitcount and time. The only DocumentResponse in the wrapper */
        private DocumentResponse base = null;
        /* All records from the DocumentResponses, held until externalise */
        private List<AdjustRecord> records = new ArrayList<AdjustRecord>();
        private final Request request;

        public AdjustWrapper(Request request) {
            this.request = request;
        }

        /* Merge everything into the ResponseCollection. This class should not
         be used further after this call.
          */
        public ResponseCollection externalize() {
            log.trace("Externalizing AdjustWrapper");
            if (base == null) {
                log.trace("No DocumentResponse to externalize");
                if (records != null && records.size() > 0) {
                    log.warn("Internal inconsistency: No DocumentResponse "
                             + "present, but " + records.size()
                             + " Records exist");
                }
                return merged;
            }
            ArrayList<DocumentResponse.Record> docR =
                new ArrayList<DocumentResponse.Record>(records.size());
            for (AdjustRecord adjR: records) {
                docR.add(adjR.getRecord());
            }
            base.setRecords(docR);
            merged.add(base);
            return merged;
        }

        public ResponseCollection getMerged() {
            return merged;
        }

        public List<AdjustRecord> getRecords() {
            return records;
        }

        public void setRecords(List<AdjustRecord> records) {
            this.records = records;
        }

        public DocumentResponse getBase() {
            return base;
        }

        public void setBase(DocumentResponse base) {
            this.base = base;
        }

        private static class AdjustRecord {
            private final String searcherID;
            private final DocumentResponse.Record record;

            private AdjustRecord(String searcherID,
                                DocumentResponse.Record record) {
                this.searcherID = searcherID;
                this.record = record;
            }

            public String getSearcherID() {
                return searcherID;
            }
            public float getScore() {
                return record.getScore();
            }
            public DocumentResponse.Record getRecord() {
                return record;
            }
        }
    }

    /**
     * Merges the given packages and returns the result based on basic setup and
     * parameters given in the request. 
     * @param request  the original request then resulted in the packages.
     * @param responses a collection of ResponseCollections to merge.
     * @return a merge of the given ResponseCollections.
     */
    public ResponseCollection merge(
        Request request,
        List<SummaSearcherAggregator.ResponseHolder> responses) {
        long startTime = System.currentTimeMillis();
        AdjustWrapper aw = deconstruct(request, responses);
        if (aw.getBase() == null) {
            log.debug(
                "No DocumentResponses present in responses, skipping merge");
            return aw.externalize();
        }
        merge(request, aw);
        postProcess(request, aw);
        trim(request, aw);
        ResponseCollection result = aw.externalize();
        result.addTiming("responsemerger.total",
                         System.currentTimeMillis() - startTime);
        return result;
    }

    private void trim(Request request, AdjustWrapper aw) {
        long maxRecords = request.getLong(
            DocumentKeys.SEARCH_MAX_RECORDS, this.maxRecords);
        log.trace("trim called with maxRecords " + maxRecords);
        if (aw.getRecords().size() > maxRecords) {
            aw.setRecords(aw.getRecords().subList(0, (int)maxRecords));
        }
    }

    private void merge(Request request, AdjustWrapper aw) {
        log.trace("merge called");
        long startTime = System.currentTimeMillis();
        MODE mode = MODE.valueOf(
            request.getString(SEARCH_MODE, defaultMode.toString()));
        List<String> order = request.getStrings(SEARCH_ORDER, defaultOrder);
        log.debug("Merging DocumentResponses with mode " + mode);
        switch (mode) {
            case standard: {
                sortByStandard(aw);
                break;
            }
            case score: {
                sortByScore(aw);
                break;
            }
            case concatenate: {
                sortByID(aw, order);
                break;
            }
            case interleave: {
                interleave(aw, order);
                break;
            }
            default: throw new UnsupportedOperationException(
                "Merge mode " + mode + " not supported yet");
        }
        aw.getMerged().addTiming("responsemerger.merge",
                                 System.currentTimeMillis() - startTime);
    }

    private void interleave(AdjustWrapper aw, List<String> order) {
        log.trace("Sorting by interleaving");
        // (searchID, records*)*
        Map<String, List<AdjustWrapper.AdjustRecord>> providers =
            new LinkedHashMap<String, List<AdjustWrapper.AdjustRecord>>();
        for (String o: order) { // Ordered first
            providers.put(o, new ArrayList<AdjustWrapper.AdjustRecord>());
        }
        for (AdjustWrapper.AdjustRecord ar: aw.getRecords()) {
            if (!providers.containsKey(ar.getSearcherID())) {
                providers.put(ar.getSearcherID(),
                              new ArrayList<AdjustWrapper.AdjustRecord>());
            }
            providers.get(ar.getSearcherID()).add(ar);
        }
        // providers now contains ordered lists of records split by search ID
        List<AdjustWrapper.AdjustRecord> interleaved =
            new ArrayList<AdjustWrapper.AdjustRecord>();
        boolean any = true;
        while (any) {
            any = false;
            for (Map.Entry<String, List<AdjustWrapper.AdjustRecord>> entry:
                providers.entrySet()) {
                if (entry.getValue().size() > 0) {
                    any = true;
                    interleaved.add(entry.getValue().remove(0));
                }
            }
        }
        aw.setRecords(interleaved);
    }

    private void sortByStandard(AdjustWrapper aw) {
        log.trace("Sorting by provided order");
        if (aw.getBase() == null) {
            log.debug("sortByStandard(...): No base in AdjustWrapper. Exiting");
            return;
        }
        final Comparator<DocumentResponse.Record> comparator =
            aw.getBase().getComparator();
        Collections.sort(
            aw.getRecords(),
            new Comparator<AdjustWrapper.AdjustRecord>() {
                @Override
                public int compare(AdjustWrapper.AdjustRecord o1,
                                   AdjustWrapper.AdjustRecord o2) {
                    return comparator.compare(o1.getRecord(), o2.getRecord());
                }
            });
    }

    private void sortByScore(AdjustWrapper aw) {
        log.trace("Sorting records by score");
        Collections.sort(
            aw.getRecords(),
            new Comparator<AdjustWrapper.AdjustRecord>() {
                @Override
                public int compare(AdjustWrapper.AdjustRecord o1,
                                   AdjustWrapper.AdjustRecord o2) {
                    return -Float.compare(o1.getScore(), o2.getScore());
                }
            });
    }

    private void sortByID(AdjustWrapper aw, final List<String> order) {
        log.trace("Sorting records by searcher ID, secondarily by score");
        Collections.sort(
            aw.getRecords(),
            new Comparator<AdjustWrapper.AdjustRecord>() {
                @Override
                public int compare(AdjustWrapper.AdjustRecord o1,
                                   AdjustWrapper.AdjustRecord o2) {
                    int order1 = order.indexOf(o1.getSearcherID());
                    order1 = order1 == -1 ? Integer.MAX_VALUE : order1;
                    int order2 = order.indexOf(o2.getSearcherID());
                    order2 = order2 == -1 ? Integer.MAX_VALUE : order2;
                    return order1 != order2 ? order1 - order2 :
                           -Float.compare(o1.getScore(), o2.getScore());
                }
            });
    }

    private AdjustWrapper deconstruct(
        Request request,
        List<SummaSearcherAggregator.ResponseHolder> responses) {
        AdjustWrapper aw = new AdjustWrapper(request);
        List<AdjustWrapper.AdjustRecord> adjustRecords =
            new ArrayList<AdjustWrapper.AdjustRecord>();
        for (SummaSearcherAggregator.ResponseHolder response: responses) {
            if (!"".equals(response.getResponses().getTopLevelTiming())) {
                aw.getMerged().addTiming(response.getResponses().
                    getTopLevelTiming());
            }
            for (Response r: response.getResponses()) {
                if (!(r instanceof DocumentResponse)) {
                    aw.getMerged().add(r);
                    continue;
                }
                DocumentResponse dr = (DocumentResponse)r;
                for (DocumentResponse.Record record: dr.getRecords()) {
                    adjustRecords.add(new AdjustWrapper.AdjustRecord(
                        response.getSearcherID(), record));
                }
                if (aw.getBase() == null) {
                    aw.setBase(dr);
                } else { // Merge hit and time
                    aw.getBase().setHitCount(
                        aw.getBase().getHitCount() + dr.getHitCount());
                    aw.getBase().setSearchTime(
                        sequential ?
                        aw.getBase().getSearchTime() + dr.getSearchTime() :
                        Math.max(aw.getBase().getSearchTime(),
                                 dr.getSearchTime()));
                    aw.getMerged().addTiming(dr.getTiming());
                }
            }
        }
        aw.setRecords(adjustRecords);
        return aw;
    }

    private void postProcess(Request request, AdjustWrapper aw) {
        long startTime = System.currentTimeMillis();
        POST post = POST.valueOf(
            request.getString(SEARCH_POST, defaultPost.toString()));
        if (post == POST.none) {
            return;
        }
        int forceTopX = request.getInt(SEARCH_FORCE_TOPX, defaultForceTopX);
        List<Pair<String, Integer>> forceRules = defaultForceRules;
        if (request.containsKey(SEARCH_FORCE_RULES)) {
            forceRules = parseForceRules(request.getString(SEARCH_FORCE_RULES));
        }
        switch (post) {
            case enforce: {
                postProcessEnforce(request, aw, forceTopX, forceRules);
                break;
            }
            case ifnone:
                postProcessIfNone(request, aw, forceTopX, forceRules);
                break;
            default: throw new UnsupportedOperationException(
                "Post merge processing does not yet support '" + post + "'");
        }
        aw.getMerged().addTiming("responsemerger.post",
                                 System.currentTimeMillis() - startTime);
    }

    private void postProcessIfNone(
        Request request, AdjustWrapper aw, int topX,
        List<Pair<String, Integer>> rules) {
        log.trace("postProcessIfNone called");
        outer:
        for (Pair<String, Integer> rule: rules) {
            for (int i = 0 ; i < Math.min(aw.getRecords().size(), topX) ; i++) {
                if (aw.getRecords().get(i).getSearcherID().equals(
                    rule.getKey())) { // Got a hit within topX, so skip the rule
                    continue outer;
                }
            }
            // No hit. Enforce the rule!
            enforce(request, aw, topX, rule.getKey(), rule.getValue());
        }
    }

    private void postProcessEnforce(Request request, AdjustWrapper aw, int topX,
                                    List<Pair<String, Integer>> rules) {
        log.trace("postProcessEnforce called");
        for (Pair<String, Integer> rule: rules) {
            enforce(request, aw, topX, rule.getKey(), rule.getValue());
        }
    }

    /* Ensure that there are the required number of records with the given
       search ID among the topX records.
      */
    private void enforce(Request request, AdjustWrapper aw, int topX, String searchID, int required) {
        // Do we need to do this?
        List<AdjustWrapper.AdjustRecord> records = aw.getRecords();
        if (records.size() < topX) {
            return; // Not enough Records
        }
        int matches = 0;
        int firstInsertPos = 0; // We only insert after last real entry
        for (int i = 0 ; i < topX && i < records.size() ; i++) {
            if (searchID.equals(records.get(i).getSearcherID())) {
                matches++;
                firstInsertPos = i+1;
            }
        }
        if (matches >= required) {
            return; // Enough matches already
        }
        int needed = required - matches;
        boolean moveUp = firstInsertPos + needed > topX; // Not enough room for extra records so we pull some up
        if (moveUp) {
            firstInsertPos = topX - needed -1;
            needed = required;
        }

        // Extract the needed records from the non-showing entries
        List<AdjustWrapper.AdjustRecord> promotees = new ArrayList<AdjustWrapper.AdjustRecord>(required);
        int position = firstInsertPos;
        while (position < records.size() && promotees.size() < needed) {
            if (searchID.equals(records.get(position).getSearcherID())) {
                promotees.add(records.remove(position));
                continue;
            }
            position++;
        }

        // Generate semi-random insertion-points from firstInsertPos to topX
        // We want the order to be the same between searches so we seed
        // by hashing query
        Random random = new Random((request.getString(DocumentKeys.SEARCH_QUERY, "N/A")).hashCode() << 12);
        List<Boolean> insertionPoints = new ArrayList<Boolean>(topX);
        for (int i = firstInsertPos ; i < topX ; i++) {
            insertionPoints.add(i-firstInsertPos < needed);
        }
        Collections.shuffle(insertionPoints, random);

        // Insert!
        List<AdjustWrapper.AdjustRecord> result = new ArrayList<AdjustWrapper.AdjustRecord>(records.size() + promotees.size());
        for (int i = 0 ; i < topX ; i++) {
            if (i >= firstInsertPos && insertionPoints.get(i-firstInsertPos) && promotees.size() > 0) {
                result.add(promotees.remove(0));
            } else if (records.size() > 0) {
                result.add(records.remove(0));
            }
        }
        result.addAll(records);
        result.addAll(promotees); // Just to be sure (it should be empty)
        aw.setRecords(result);
    }

    private List<Pair<String, Integer>> parseForceRules(String s) {
        String[] ruleTokens = s.split(" *, *");
        if (ruleTokens.length == 0) {
            log.trace("parseForceRules found no rules in '" + s + "'");
            return null;
        }
        List<Pair<String, Integer>> rules = new ArrayList<Pair<String, Integer>>(ruleTokens.length);
        for (String ruleToken: ruleTokens) {
            // zoo(12)
            String[] subTokens = ruleToken.split(" *\\(", 2);
            // zoo
            if (subTokens.length < 2) {
                throw new IllegalArgumentException(
                    "The syntax for a rule is 'id(minCount)' but the input was '" + ruleToken + "'");
            }
            // "  5)  "
            String noParen = subTokens[1].split("\\)", 2)[0].trim();
            Integer count = Integer.parseInt(noParen);
            log.debug("parseForceRules adding rule " + subTokens[0] + ", " + count);
            rules.add(new Pair<String, Integer>(subTokens[0], count));
        }
        return rules;
    }
}
