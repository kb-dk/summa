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
import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubAggregatorBase;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

import java.util.*;

/**
 * Merges responses with the possibility of prioritizing certain sources over others.
 * </p><p>
 * This is a Solr port of the Summa ResponseMerger. In anticipation of possible Solr module, the code is fully
 * de-coupled from the ResponseMerger instead of sharing common methods (a common helper method might be a better
 * solution).
 * </p>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class HubResponseMerger implements Configurable {
    private static Log log = LogFactory.getLog(HubResponseMerger.class);

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

    private final MODE defaultMode;
    private final POST defaultPost;
    private final List<String> defaultOrder;
    private final int defaultForceTopX;
    private List<Pair<String, Integer>> defaultForceRules = null;
    private final boolean sequential;

    // responseHeaders
    public static final String STATUS = "status";
    public static final String QTIME = "QTime";

    public HubResponseMerger(Configuration conf) {
        defaultMode = MODE.valueOf(conf.getString(CONF_MODE, DEFAULT_MODE.toString()));
        defaultPost = POST.valueOf(conf.getString(CONF_POST, DEFAULT_POST.toString()));
        defaultOrder = conf.getStrings(CONF_ORDER, new ArrayList<String>());
        defaultForceTopX = conf.getInt(CONF_FORCE_TOPX, DEFAULT_FORCE_TOPX);
        if (conf.valueExists(CONF_FORCE_RULES)) {
            defaultForceRules = parseForceRules(conf.getString(CONF_FORCE_RULES));
        }
        sequential = conf.getBoolean(CONF_SEQUENTIAL, DEFAULT_SEQUENTIAL);
        log.info("Created " + this);
    }

    public QueryResponse merge(SolrParams params, List<HubAggregatorBase.NamedResponse> responses) {
        if (responses.size() == 1) {
            log.debug("Only a single response received (" + responses.get(0).getId() + "). No merging performed");
            return responses.get(0).getResponse();
        }
        Set<String> keys = new HashSet<String>();
        for (HubAggregatorBase.NamedResponse responsePair: responses) {
            String id = responsePair.getId();
            QueryResponse response = responsePair.getResponse();
            NamedList raw = response.getResponse();
            for (int i = 0 ; i < raw.size() ; i++) {
                keys.add(raw.getName(i));
            }
        }
        log.debug("Located " + keys.size() + " unique keys in " + responses.size() + " responses. Commencing merging");

        final NamedList<Object> merged = new NamedList<Object>();
        final List<HubAggregatorBase.NamedResponse> defined =
                new ArrayList<HubAggregatorBase.NamedResponse>(responses.size());
        for (String key: keys) {
            // Isolate the responses that contains the given key
            defined.clear();
            for (HubAggregatorBase.NamedResponse response: responses) {
                if (response.getResponse().getResponse().get(key) != null) {
                    defined.add(response);
                }
            }
            if (defined.isEmpty()) {
                log.error("Located 0/" + responses.size() + " responses for key '" + key
                          + "'. This is a program error since at least one response should contain the key");
            } else if (defined.size() == 1) {
                log.debug("Located 1/" + responses.size() + " responses for key '" + key + "'. Storing directly");
                merged.add(key, defined.get(0).getResponse().getResponse().getAll(key));
            } else {
                Object m = merge(params, key, defined);
                if (m != null) {
                    merged.add(key, m);
                }
            }
        }
        // TODO: Assign ID
        // TODO: Locate and extract hit count
        return new QueryResponse(merged, null); // TODO: Check if no Solr server is on
    }

    private Object merge(SolrParams params, String key, List<HubAggregatorBase.NamedResponse> responses) {
        if ("responseHeader".equals(key)) {
            return mergeResponseHeaders(getSimpleOrderedMaps(key, responses));
        }
        if ("response".equals(key)) { // Documents
            return mergeResponses(params, responses);
        }
        log.warn("No merger for key '" + key + "'. Values discarded for " + responses.size() + " responses");
        return null;
    }

    private Object mergeResponses(SolrParams params, List<HubAggregatorBase.NamedResponse> responses) {
        AdjustWrapper aw = deconstruct(responses);
        merge(params, aw);
        postProcess(params, aw);
        trim(params, aw);
        return aw.externalize();
    }

    private void trim(SolrParams request, AdjustWrapper aw) {
        // TODO: Implement this
        log.warn("trim not implemented yet");
    }

    private void postProcess(SolrParams request, AdjustWrapper aw) {
        long startTime = System.currentTimeMillis();
        POST post = POST.valueOf(request.get(SEARCH_POST, defaultPost.toString()));
        if (post == POST.none) {
            return;
        }
        int forceTopX = request.getInt(SEARCH_FORCE_TOPX, defaultForceTopX);
        List<Pair<String, Integer>> forceRules = defaultForceRules;
        if (request.get(SEARCH_FORCE_RULES) != null) {
            forceRules = parseForceRules(request.get(SEARCH_FORCE_RULES));
        }
        switch (post) {
            case enforce: {
                postProcessEnforce(request, aw, forceTopX, forceRules);
                break;
            }
            case ifnone:
                postProcessIfNone(request, aw, forceTopX, forceRules);
                break;
            case none:
                throw new IllegalStateException("POST.none should have been handled earlier");
            default:
                throw new UnsupportedOperationException("Post merge processing does not yet support '" + post + "'");
        }
//        aw.getMerged().addTiming("responsemerger.post", System.currentTimeMillis() - startTime);
    }

    private void postProcessIfNone(SolrParams request, AdjustWrapper aw, int topX, List<Pair<String, Integer>> rules) {
        log.trace("postProcessIfNone called");
        outer:
        for (Pair<String, Integer> rule: rules) {
            for (int i = 0 ; i < Math.min(aw.getDocs().size(), topX) ; i++) {
                if (aw.getDocs().get(i).getSearcherID().equals(
                    rule.getKey())) { // Got a hit within topX, so skip the rule
                    continue outer;
                }
            }
            // No hit. Enforce the rule!
            enforce(request, aw, topX, rule.getKey(), rule.getValue());
        }
        
    }

    private void postProcessEnforce(SolrParams request, AdjustWrapper aw, int topX, List<Pair<String, Integer>> rules) {
        log.trace("postProcessEnforce called");
        for (Pair<String, Integer> rule: rules) {
            enforce(request, aw, topX, rule.getKey(), rule.getValue());
        }
    }

    /* Ensure that there are the required number of records with the given
       search ID among the topX records.
      */
    private void enforce(SolrParams request, AdjustWrapper aw, int topX, String searchID, int required) {
        // Do we need to do this?
        List<AdjustWrapper.NamedDocument> records = aw.getDocs();
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
        List<AdjustWrapper.NamedDocument> promotees = new ArrayList<AdjustWrapper.NamedDocument>(required);
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
        // TODO: Verify that q contains the query
        Random random = new Random(request.get("q", "N/A").hashCode() << 12);
        List<Boolean> insertionPoints = new ArrayList<Boolean>(topX);
        for (int i = firstInsertPos ; i < topX ; i++) {
            insertionPoints.add(i-firstInsertPos < needed);
        }
        Collections.shuffle(insertionPoints, random);

        // Insert!
        List<AdjustWrapper.NamedDocument> result =
                new ArrayList<AdjustWrapper.NamedDocument>(records.size() + promotees.size());
        for (int i = 0 ; i < topX ; i++) {
            if (i >= firstInsertPos && insertionPoints.get(i-firstInsertPos) && !promotees.isEmpty()) {
                result.add(promotees.remove(0));
            } else if (!records.isEmpty()) {
                result.add(records.remove(0));
            }
        }
        result.addAll(records);
        result.addAll(promotees); // Just to be sure (it should be empty)
        aw.setDocs(result);
    }

    private void merge(SolrParams request, AdjustWrapper aw) {
        log.trace("merge called");
        long startTime = System.currentTimeMillis();
        MODE mode = MODE.valueOf(request.get(SEARCH_MODE, defaultMode.toString()));
        List<String> order = getStrings(request, SEARCH_ORDER, defaultOrder);
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
            default: throw new UnsupportedOperationException("Merge mode " + mode + " not supported yet");
        }
//        aw.getMerged().addTiming("responsemerger.merge", System.currentTimeMillis() - startTime);
    }

    private void sortByStandard(AdjustWrapper aw) {
        // TODO: Implement this
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private void interleave(AdjustWrapper aw, List<String> order) {
        log.trace("Sorting by interleaving");
        // (searchID, records*)*
        Map<String, List<AdjustWrapper.NamedDocument>> providers =
            new LinkedHashMap<String, List<AdjustWrapper.NamedDocument>>();
        for (String o: order) { // Ordered first
            providers.put(o, new ArrayList<AdjustWrapper.NamedDocument>());
        }
        for (AdjustWrapper.NamedDocument ar: aw.getDocs()) {
            if (!providers.containsKey(ar.getSearcherID())) {
                providers.put(ar.getSearcherID(), new ArrayList<AdjustWrapper.NamedDocument>());
            }
            providers.get(ar.getSearcherID()).add(ar);
        }
        // providers now contains ordered lists of records split by search ID
        List<AdjustWrapper.NamedDocument> interleaved = new ArrayList<AdjustWrapper.NamedDocument>();
        boolean any = true;
        while (any) {
            any = false;
            for (Map.Entry<String, List<AdjustWrapper.NamedDocument>> entry:
                providers.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    any = true;
                    interleaved.add(entry.getValue().remove(0));
                }
            }
        }
        aw.setDocs(interleaved);
        
    }

    private void sortByID(AdjustWrapper aw, final List<String> order) {
        log.trace("Sorting records by searcher ID, secondarily by score");
        Collections.sort(
            aw.getDocs(),
            new Comparator<AdjustWrapper.NamedDocument>() {
                @Override
                public int compare(AdjustWrapper.NamedDocument o1, AdjustWrapper.NamedDocument o2) {
                    int order1 = order.indexOf(o1.getSearcherID());
                    order1 = order1 == -1 ? Integer.MAX_VALUE : order1;
                    int order2 = order.indexOf(o2.getSearcherID());
                    order2 = order2 == -1 ? Integer.MAX_VALUE : order2;
                    return order1 != order2 ? order1 - order2 : -Float.compare(o1.getScore(), o2.getScore());
                }
            });
    }

    private void sortByScore(AdjustWrapper aw) {
        log.trace("Sorting records by score");
        Collections.sort(
            aw.getDocs(),
            new Comparator<AdjustWrapper.NamedDocument>() {
                @Override
                public int compare(AdjustWrapper.NamedDocument o1, AdjustWrapper.NamedDocument o2) {
                    return -Float.compare(o1.getScore(), o2.getScore());
                }
            });
        
    }

    private List<String> getStrings(SolrParams params, String key, List<String> defaults) {
        String base = params.get(key);
        if (base == null) {
            return defaults;
        }
        return Arrays.asList(base.split((" *, *")));
    }

    private AdjustWrapper deconstruct(List<HubAggregatorBase.NamedResponse> responses) {
        AdjustWrapper aw = new AdjustWrapper();
        for (HubAggregatorBase.NamedResponse response: responses) {
            SolrDocumentList sdl = (SolrDocumentList) response.getResponse().getResponse().get("response");
            if (sdl == null) {
                log.warn("deconstruct: No SolrDocumentList for key 'response' in '" + response.getId() + "'");
                continue;
            }
            // TODO: Consider maxScore & start
            aw.addHits(sdl.getNumFound());
            for (SolrDocument doc: sdl) {
                aw.addDoc(response.getId(), doc);
            }
        }
        return aw;
    }

    private List<SolrDocumentList> getSolrDocumentList(List<HubAggregatorBase.NamedResponse> responses) {
        List<SolrDocumentList> sdls = new ArrayList<SolrDocumentList>(responses.size());
        for (HubAggregatorBase.NamedResponse response: responses) {
            SolrDocumentList sdl = (SolrDocumentList) response.getResponse().getResponse().get("response");
            if (sdl != null) {
                sdls.add(sdl);
            }
        }
        return sdls;
    }

    private SimpleOrderedMap mergeResponseHeaders(List<SimpleOrderedMap> headers) {
        final Set<String> keys = getKeys(headers);
        final SimpleOrderedMap<Object> merged = new SimpleOrderedMap<Object>();

        for (String key: keys) {
            // status: Anything else than 0 is considered an anomaly and has preference
            if (STATUS.equals(key)) {
                Set<Integer> statuses = new HashSet<Integer>(headers.size());
                for (SimpleOrderedMap som: headers) {
                    Integer status = (Integer) som.get(STATUS);
                    if (status != null) {
                        statuses.add(status);
                    }
                }
                if (statuses.size() == 1) {
                    merged.add(STATUS, statuses.iterator().next());
                } else {
                    log.debug("Got " + statuses.size() + " different responseHeader.status. Returning first != 0");
                    for (Integer status: statuses) {
                        if (0 != status) {
                            merged.add(STATUS, status);
                            break;
                        }
                    }
                }
                continue;
            }

            // QTime: Depending on CONF_PARALLEL_SEARCHES, this is either summed or maxed
            if (QTIME.equals(key)) {
                int endQTime = 0;
                for (SimpleOrderedMap som: headers) {
                    Integer qtime = (Integer) som.get(QTIME);
                    if (qtime != null) {
                        endQTime = sequential ? endQTime + qtime : Math.max(endQTime, qtime);
                    }
                }
                merged.add(QTIME, endQTime);
                continue;
            }

            // default: Just return the first occurrence TODO: Consider adding all as list
            for (SimpleOrderedMap som: headers) {
                List value = som.getAll(key);
                if (value != null) {
                    merged.add(key, value);
                    break;
                }
            }
        }
        return merged;
    }

    private Set<String> getKeys(List<SimpleOrderedMap> maps) {
        Set<String> keys = new HashSet<String>();
        for (SimpleOrderedMap map: maps) {
            for (int i = 0 ; i < map.size() ; i++) {
                keys.add(map.getName(i));
            }
        }
        return keys;
    }

    private List<SimpleOrderedMap> getSimpleOrderedMaps(String key, List<HubAggregatorBase.NamedResponse> responses) {
        List<SimpleOrderedMap> soms = new ArrayList<SimpleOrderedMap>(responses.size());
        for (HubAggregatorBase.NamedResponse response: responses) {
            SimpleOrderedMap som = (SimpleOrderedMap) response.getResponse().getResponse().get(key);
            if (som != null) {
                soms.add((SimpleOrderedMap) response.getResponse().getResponse().get(key));
            }
        }
        return soms;
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


    /* Contains everything besides DocumentResponse until externalize is called. */
    private static class AdjustWrapper {

        /* All records from the DocumentResponses, held until externalise */
        private List<NamedDocument> docs = new ArrayList<NamedDocument>();
        private long hitCount = 0;
        /* Merge everything into the ResponseCollection. This class should not
         be used further after this call.
          */
        public SolrDocumentList externalize() {
            log.trace("Externalizing AdjustWrapper");
            SolrDocumentList  docR = new SolrDocumentList();
            float maxScore = 0.0f;
            for (NamedDocument adjR: docs) {
                docR.add(adjR.getDoc());
                maxScore = Math.max(maxScore, adjR.getScore());
            }
            docR.setNumFound(hitCount);
            docR.setMaxScore(maxScore);

            // TODO: Add searcher as field in document
            return docR;
        }

        public void addHits(long hitCount) {
            this.hitCount += hitCount;
        }

        public List<NamedDocument> getDocs() {
            return docs;
        }

        public long getHitCount() {
            return hitCount;
        }

        public void setDocs(List<NamedDocument> docs) {
            this.docs = docs;
        }

        public void addDoc(String searcherID, SolrDocument doc) {
            docs.add(new NamedDocument(searcherID, doc));
        }

        private static class NamedDocument {
            private final String searcherID;
            private final SolrDocument doc;

            private NamedDocument(String searcherID, SolrDocument doc) {
                this.searcherID = searcherID;
                this.doc = doc;
            }

            public String getSearcherID() {
                return searcherID;
            }
            public float getScore() {
                Float score = (Float) doc.get("_score");
                return score == null ? 1.0f : score;
            }
            public SolrDocument getDoc() {
                return doc;
            }
        }
    }

    @Override
    public String toString() {
        return "HubResponseMerger(not properly implemented)";
    }
}
