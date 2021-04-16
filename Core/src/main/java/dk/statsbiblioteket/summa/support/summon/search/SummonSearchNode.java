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
package dk.statsbiblioteket.summa.support.summon.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.search.tools.QueryRewriter;
import dk.statsbiblioteket.summa.support.solr.SolrSearchNode;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.security.Security;
import java.security.SignatureException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transforms requests based on {@link DocumentKeys} and {@link FacetKeys} to
 * calls to Serial Solutions Summon API and transform the result to
 * {@link DocumentResponse} and
 * {@link dk.statsbiblioteket.summa.facetbrowser.api.FacetResult}.
 * See http://api.summon.serialssolutions.com/help/api/ for Summon API.
 * </p><p>
 * Supported DocumentKeys: {@link DocumentKeys#SEARCH_QUERY},
 * {@link DocumentKeys#SEARCH_START_INDEX} and
 * {@link DocumentKeys#SEARCH_MAX_RECORDS}. To adjust for differences between
 * Summa and Summon, 1 is added to the given start index.
 * Supported FacetKeys: {@link FacetKeys#SEARCH_FACET_FACETS} without the sort
 * option.
 */
// Map contenttype, language. Convert date (til år lige nu), potentially library
    // TODO: Check if startpos is 0 or 1, adjuct accordingly
    // TODO: Implement configurable rangefacets
    // TODO Implement getShortRecord
    // TODO: Implement getRecord in Storage
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te, mv")
public class SummonSearchNode extends SolrSearchNode {
    private static Log log = LogFactory.getLog(SummonSearchNode.class);

    /**
     * All summon record IDs starts with this. Used for ID-lookup.
     */
    private static final String SUMMON_ID_PREFIX = "FETCH-";

    /**
     * If the ID of the returned record from a first class document ID lookup does nbot match the provided ID,
     * the provided ID is stored in this field.
     */
    public static final String ORIGINAL_LOOKUP_ID = "OriginalLookupID";

    /**
     * The maximum number of documents to request in one call when performing lookups on IDs.
     * http://api.summon.serialssolutions.com/help/api/search/parameters/fetch-ids
     */
    public static final int SUMMON_MAX_IDS = 50; // Theoretically 100, but max page size is 50

    /**
     * When performing a paged ID-search, wait this number of milliseconds between request.
     */
    private static final long SUMMON_ID_SEARCH_WAIT = 200;

    /**
     * If true, all configuration parameters, except the explicit ones in this class, that starts with "summon."
     * are converted to start with "solr." in order to conform with the superclass.
     * </p><p>
     * Optional. Default is true;
     */
    public static final String CONF_LEGACY_PREFIX_CONVERT = "summon.legacyprefixconvert";
    public static final boolean DEFAULT_LEGACY_PREFIX_CONVERT = true;

    public static final String DEFAULT_SUMMON_HOST = "api.summon.serialssolutions.com";
//    public static final String DEFAULT_SUMMON_RESTCALL = "/search";
    public static final String DEFAULT_SUMMON_RESTCALL = "/2.0.0/search";

    /**
     * Summon requires filtering on facets to be in summon-format and not as standard filters.
     */
    private static final boolean DEFAULT_SUMMON_EXPLICIT_FACET_FILTERING = true;
    /**
     * The access ID for Summon. Serial Solutions controls this and in order to use this filter, a valid access ID must
     * be provided.
     * </p><p>
     * Mandatory.
     * @see {@link #CONF_SUMMON_ACCESSKEY}.
     */
    public static final String CONF_SUMMON_ACCESSID = "summon.accessid";

    /**
     * The access key for Summon. Serial Solutions controls this and in order to use this filter, a valid access key
     * must be provided.
     * </p><p>
     * Mandatory.
     * @see {@link #CONF_SUMMON_ACCESSID}.
     */
    public static final String CONF_SUMMON_ACCESSKEY = "summon.accesskey";

    public static final String DEFAULT_SUMMON_IDPREFIX = "summon_";

//    See http://api.summon.serialssolutions.com/help/api/search/commands/add-facet-field
    public static final String DEFAULT_SUMMON_FACETS =
        "Author, " // Map
        + "ContentType, " // Map 1:n
//        + "Genre, " // Direct // Does not exist anymore as of 20120402
        + "IsScholarly, " // Direct
        + "Language, " // Map
        + "IsFullText, "
        //"Library,and,1,5",
        //"PackageID,and,1,5",
        //"SourceID,and,1,5",
        + "SubjectTerms, "; // SB:subject
        //+ "TemporalSubjectTerms"; // Direct // Does not exist anymore as of 20120402


    /**
     * If true, queries will be actively re-written to ensure that they do not conform to any format handled by the
     * DisMax-like parser for summon.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_DISMAX_SABOTAGE = "summon.dismax.sabotage";
    public static final String SEARCH_DISMAX_SABOTAGE = CONF_DISMAX_SABOTAGE;
    public static final boolean DEFAULT_DISMAX_SABOTAGE = false;

    public static final String DEFAULT_SUMMON_ID_FIELD = "id";

    /**
     * Used for emulating recordBase:notsummon in facet requests.
     * The facet must be specified as "existingfacet:nonexistingvalue".
     * </p><p>
     * Optional. Default is 'Author:nonexisting34538'.
     */
    public static final String CONF_NONMATCHING_FACET = "summon.facet.nonmatching";
    public static final String DEFAULT_NONMATCHING_FACET = "Author:nonexisting34538";

    /**
     * Used for emulating recordBase:notsummon in query and filter requests.
     * The query must be specified as "existingfield:nonexistingvalue".
     * </p><p>
     * Optional. Default is 'Author:nonexisting34538'.
     */
    public static final String CONF_NONMATCHING_QUERY = "summon.query.nonmatching";
    public static final String DEFAULT_NONMATCHING_QUERY = "Author:nonexisting34538";

    /**
     * If true, range queries of the type {@code PublicationYear:[YYYY TO yyyy]} are treated as
     * {@code PublicationYear:[YYYY TO yyyy-12-31]}.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_FIX_RANGE_PUBLICATION = "summon.range.publication.fix";
    public static final boolean DEFAULT_FIX_RANGE_PUBLICATION = true;

    // http://api.summon.serialssolutions.com/help/api/search/healthcheck
    public static final String CONF_SUMMON_PING_REST = "summon.ping.restcall";
    public static final String DEFAULT_SUMMON_PING_REST = "/2.0.0/search/ping";

//    public static final String PING_URL = "http://api.summon.serialssolutions.com/2.0.0/search/ping";

    /**
     * summon seems to be migrating a lot of documents where the new ID is {@code <oldID>1}.
     * When performing a lookup and this is true, it is assumed that {@code <id>1} refers to the same
     * document as {@code <id>}.
     */
    public static final String CONF_ACCEPT_ONEPREFIX_IDS = "summon.idlookup.acceptoneprefix";
    public static final boolean DEFAULT_ACCEPT_ONEPREFIX_IDS = true;



    private final String accessID;
    private final String accessKey;
    private final Configuration conf; // Used when constructing QueryRewriter
    private final boolean sabotageDismax;
    private final String nonMatchingFacet;
    private final TermQuery nonMatchingQuery;
    private final String pingRest;
    private final boolean lookupAcceptOnePrefix;

    public SummonSearchNode(Configuration conf) throws RemoteException {
        super(legacyConvert(conf));
        setID("summon");
        this.conf = conf;
        accessID =   conf.getString(CONF_SUMMON_ACCESSID);
        accessKey =  conf.getString(CONF_SUMMON_ACCESSKEY);
        for (Map.Entry<String, Serializable> entry : conf) {
            convertSolrParam(solrDefaultParams, entry);
        }
        sabotageDismax = conf.getBoolean(CONF_DISMAX_SABOTAGE, DEFAULT_DISMAX_SABOTAGE);
        nonMatchingFacet = conf.getString(CONF_NONMATCHING_FACET, DEFAULT_NONMATCHING_FACET);
        String[] qt = conf.getString(CONF_NONMATCHING_QUERY, DEFAULT_NONMATCHING_QUERY).split(":");
        nonMatchingQuery = new TermQuery(new Term(qt[0], qt[1]));
        pingRest = conf.getString(CONF_SUMMON_PING_REST, DEFAULT_SUMMON_PING_REST);
//        boolean fixPublication = conf.getBoolean(CONF_FIX_RANGE_PUBLICATION, DEFAULT_FIX_RANGE_PUBLICATION);
        //readyWithoutOpen();  // Already handled in parent class
        lookupAcceptOnePrefix = conf.getBoolean(CONF_ACCEPT_ONEPREFIX_IDS, DEFAULT_ACCEPT_ONEPREFIX_IDS);
        log.info(String.format(Locale.ROOT, "Created Summon wrapper (host=%s, sabotageDismax=%b)", host, sabotageDismax));
    }

    @Override
    protected SolrResponseBuilder createResponseBuilder(Configuration conf) {
        return new SummonResponseBuilder(conf);
    }

    @Override
    protected FacetQueryTransformer createFacetQueryTransformer(final Configuration conf) {
        return new FacetQueryTransformer(conf) {
            String NONMATCHING = null;
            @Override
            protected void addFacetQuery(
                Map<String, List<String>> queryMap, String field, String value, boolean negated) {
                if (NONMATCHING == null) { // Ugly, but initialization order dictates it
                    NONMATCHING = nonMatchingFacet.replace(":", ",") + ",false";
                }
                if ("recordBase".equals(field)) {
                    if ((negated && responseBuilder.getRecordBase().equals(value))
                    || (!negated && !responseBuilder.getRecordBase().equals(value))) {
                        append(queryMap, "s.fvf", NONMATCHING);
                    }
                } else {
                    append(queryMap, "s.fvf", field + "," + value + "," + negated);
                }
            }
        };
    }

    // Converts the Configuration so that legacy-versions of the properties (those starting with "summon.") are
    // copied to new versions (prefix ".solr").
    private static Configuration legacyConvert(Configuration conf) {
        if (conf.getBoolean(CONF_LEGACY_PREFIX_CONVERT, DEFAULT_LEGACY_PREFIX_CONVERT)) {
            final String[] SKIP = new String[]{CONF_SUMMON_ACCESSID, CONF_SUMMON_ACCESSKEY};
            Configuration adds = Configuration.newMemoryBased();
            for (Map.Entry<String, Serializable> entry: conf) {
                if (entry.getKey().startsWith("summon.") && Arrays.binarySearch(SKIP, entry.getKey()) < 0) {
                    adds.set("solr." + entry.getKey().substring(0, 7), entry.getValue());
                }
                if (entry.getKey().startsWith("summonparam.") && Arrays.binarySearch(SKIP, entry.getKey()) < 0) {
                    adds.set("solr.param." + entry.getKey().substring(0, 12), entry.getValue());
                }
            }
            for (Map.Entry<String, Serializable> entry: adds) {
                conf.set(entry.getKey(), entry.getValue());
            }
        }

        if (!conf.valueExists(CONF_SOLR_HOST)) {
            conf.set(CONF_SOLR_HOST, DEFAULT_SUMMON_HOST);
        }
        if (!conf.valueExists(CONF_SOLR_RESTCALL)) {
            conf.set(CONF_SOLR_RESTCALL, DEFAULT_SUMMON_RESTCALL);
        }
        if (!conf.valueExists(CONF_SOLR_IDPREFIX)) {
            conf.set(CONF_SOLR_IDPREFIX, DEFAULT_SUMMON_IDPREFIX);
        }
        if (!conf.valueExists(CONF_SOLR_FACETS)) {
            conf.set(CONF_SOLR_FACETS, DEFAULT_SUMMON_FACETS);
        }
        if (!conf.valueExists(CONF_ID_FIELD)) {
            conf.set(CONF_ID_FIELD, DEFAULT_SUMMON_ID_FIELD);
        }
        if (!conf.valueExists(CONF_EXPLICIT_FACET_FILTERING)) {
            conf.set(CONF_EXPLICIT_FACET_FILTERING, DEFAULT_SUMMON_EXPLICIT_FACET_FILTERING);
        }

        // summon does not support MoreLikeThis (TODO: Confirm this) so default is off
        if (!conf.valueExists(CONF_MLT_ENABLED)) {
            conf.set(CONF_MLT_ENABLED, false);
        }
        return conf;
    }

    @Override
    protected String toSolrKey(String key) {
        final String SPREFIX = "summonparam.";
        String sKey = super.toSolrKey(key);
        return sKey != null? sKey :
                !key.startsWith(SPREFIX) ? null: key.substring(SPREFIX.length());
    }

    /**
     * It seems that the Summon query parser does not support ranges. Instead it expects ranges to be stated in 's.rf'
     * as 'field,minvalue:maxvalue'. This method parses the query, extracts & removes the range query parts and adds
     * them to the Summon search parameters.
     * </p><p>
     * Similarly, term queries with the field 'ID' are special as the text section must be modified by stripping leading
     * {@link #idPrefix}.
     * @param query as entered by the user.
     * @param summonSearchParams range-queries are added to this.
     * @return the query minus range queries.
     */
    @Override
    public String convertQuery(final String query, final Map<String, List<String>> summonSearchParams) {
        // TODO: Perform legacy conversion "summonparam.*" -> "solrparam.*"
        final String RF = "s.rf"; // RangeField
        final String BASE = "recordBase";
        try {
            return new QueryRewriter(conf, null, new QueryRewriter.Event() {
                @Override
                public Query onQuery(BooleanQuery query) {
                    // Rule: At least 1 clause must match
                    boolean noMatches = true;
                    boolean match = false;
                    List<BooleanClause> reduced = new ArrayList<>(query.clauses().size());
                    for (BooleanClause clause: query.clauses()) {
                        if (clause.getQuery() instanceof TermQuery
                            && BASE.equals(((TermQuery)clause.getQuery()).getTerm().field())) {
                            TermQuery tq = (TermQuery)clause.getQuery();
                            boolean baseMatch = responseBuilder.getRecordBase().equals(tq.getTerm().text());

                            if (BooleanClause.Occur.MUST == clause.getOccur()) {
                                if (baseMatch) {
                                    noMatches = false; // Match encountered
                                    match = true;
                                } else {
                                    return nonMatchingQuery;
                                }
                            } else if (BooleanClause.Occur.MUST_NOT == clause.getOccur()) {
                                if (baseMatch) {
                                    return nonMatchingQuery;
                                }
                                // MUST_NOT unfulfilled queries does not affect the evaluation as Lucene does not
                                // support all negative clauses
                                noMatches = false;
                            } else { // SHOULD
                                if (baseMatch) {
                                    noMatches = false; // Match encountered
                                    match = true;
                                }
                                // SHOULD unfulfilled queries does not affect the evaluation
                            }
                        } else {
                            noMatches = false;
                            reduced.add(clause);
                        }
                    }
                    if (noMatches) {
                        // There are no matches
                        return nonMatchingQuery;
                    }
                    if (reduced.isEmpty() && match) {
                        // No queries left, but we encountered at least 1 match
                        return null; // As we do not have match-all
                    }
                    // There may be matches
                    query.clauses().clear();
                    query.clauses().addAll(reduced);
                    return query;
                }

                @Override
                public Query onQuery(TermRangeQuery query) {
                    if (summonSearchParams == null ) {
                        return query;
                    }
                    List<String> sq = summonSearchParams.get(RF);
                    if (sq == null) {
                        sq = new ArrayList<>();
                        summonSearchParams.put(RF, sq);
                    }
                    if ("PublicationYear".equals(query.getField())
                        && query.getLowerTerm().utf8ToString().length() == 4
                        && query.getUpperTerm().utf8ToString().length() == 4) {
                        sq.add("PublicationDate," + query.getLowerTerm().utf8ToString() + ":"
                               + query.getUpperTerm().utf8ToString() + "-12-31");
                    } else {
                        sq.add(query.getField() + "," + query.getLowerTerm().utf8ToString() + ":"
                               + query.getUpperTerm().utf8ToString());
                    }
                    return null;
                }

                @Override
                public Query onQuery(TermQuery query) {
                    // ID is first class so no configuration here
                    if ("ID".equals(query.getTerm().field()) ||
                        IndexUtils.RECORD_FIELD.equals(query.getTerm().field())) {
                        String text = query.getTerm().text();
                        if (idPrefix != null && text != null && text.startsWith(idPrefix)) {
                            text = text.substring(idPrefix.length());
                        }
                        TermQuery tq = new TermQuery(new Term("ID", text));
                        tq.setBoost(query.getBoost());
                        return tq;
                    } else if ("recordBase".equals(query.getTerm().field())) {
                        if (responseBuilder.getRecordBase().equals(query.getTerm().text())) {
                            return null; // The same as matching (in a klugdy hacky way)
                        } else {
                            return nonMatchingQuery;
                        }
                    }
                    return query;
                }

/*                @Override
                public BooleanClause createBooleanClause(Query query, BooleanClause.Occur occur) {
                    if (query instanceof TermQuery) {
                        TermQuery tq = (TermQuery)query;
                        if ("recordBase".equals(tq.getTerm().field())) {
                            if (occur == BooleanClause.Occur.MUST &&
                                responseBuilder.getRecordBase().equals(tq.getTerm().text())) {
                                return null; // The same as matching (in a klugdy hacky way)
                            }
                            if (occur == BooleanClause.Occur.MUST_NOT &&
                                !responseBuilder.getRecordBase().equals(tq.getTerm().text())) {
                                // Guaranteed non matching
                                return super.createBooleanClause(nonMatchingQuery, BooleanClause.Occur.MUST);
                            }
                        }
                    }
                    return super.createBooleanClause(query, occur);
                }*/
            }).rewrite(query);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Error parsing '" + query + "'", e);
        }
    }

    // This is handled by {@link #handleDocIDs}.
    @Override
    protected boolean rewriteIDRequestToLuceneQuery(Request request) {
        return true;
    }

    private static final String LEAF_ID_REQUEST = "SUMMON_HANDLE_DOC_ID_LEAF"; // Terminates ID-lookup
    @Override
    protected boolean handleDocIDs(Request request, ResponseCollection responses) throws RemoteException {
        if (!request.containsKey(DocumentKeys.SEARCH_IDS)) {
            return true;
        }
        final List<String> lookupIDs = request.getStrings(DocumentKeys.SEARCH_IDS, new ArrayList<String>());
        if (request.getBoolean(LEAF_ID_REQUEST, false)) { // Everything is readied: Search & return
            log.debug("handleDocIDs: Adding " + CONF_SOLR_PARAM_PREFIX + "s.fids to the Summon request with "
                      + lookupIDs.size() + " document IDs");
            request.put(CONF_SOLR_PARAM_PREFIX + "s.fids", Strings.join(normalizeIDs(lookupIDs)));
            request.put(DocumentKeys.SEARCH_MAX_RECORDS, SUMMON_MAX_IDS);
            return true;
        }
        final long oMatches = cacheMatches;
        final long oMisses = cacheMisses;
        // Fresh request for documents based on ID
        List<String> summonIDs = extractSummonIDs(lookupIDs);
        if (summonIDs.isEmpty()) {
            log.debug("handleDocIDs: No summon IDs in request. Exiting");
            return false;
        }
        if (log.isDebugEnabled()) {
            log.debug("handleDocIDs called with " + lookupIDs.size() + " IDs, pruned to summon-IDs: "
                      + Strings.join(summonIDs, 10));
        }
        //if (summonIDs.size() > SUMMON_MAX_IDS) {
        handleDocIDsPaged(summonIDs, request, responses);
        responses.addTiming("summon.idlookup.cached=" + (cacheMatches-oMatches));
        responses.addTiming("summon.idlookup.noncached=" + (cacheMisses-oMisses));
        return false;
    }

    private void handleDocIDsPaged(List<String> summonIDs, Request request, ResponseCollection responses)
            throws RemoteException {
        log.debug("handleDocIDsPaged(" + summonIDs.size() +", ..., ...) called");
        List<ResponseCollection> rawResponses = new ArrayList<>(summonIDs.size()/SUMMON_MAX_IDS+1);
        ArrayList<String> notProcessed = new ArrayList<>(summonIDs);
        Set<String> nonResolved = new HashSet<>(summonIDs.size()); // Failed first class resolve

        int cacheHits = resolveRecordsFromCache(request, notProcessed, responses);

        // We do this iteratively so we do not overwhelm summon
        while (!notProcessed.isEmpty()) {
            ArrayList<String> sub;
            if (notProcessed.size() > SUMMON_MAX_IDS) {
                sub = new ArrayList<>(notProcessed.subList(0, SUMMON_MAX_IDS));
                notProcessed = new ArrayList<>(notProcessed.subList(SUMMON_MAX_IDS, notProcessed.size()));
            } else {
                sub = notProcessed;
            }
            ResponseCollection subResponses = new ResponseCollection();
            Request subRequest = new Request();
            subRequest.put(LEAF_ID_REQUEST, true);
            subRequest.putAll(request);
            subRequest.put(DocumentKeys.SEARCH_IDS, sub);
            subRequest.put(DocumentKeys.SEARCH_MAX_RECORDS, SUMMON_MAX_IDS);
            subRequest.put(DocumentKeys.ROWS, SUMMON_MAX_IDS);
            barrierSearch(subRequest, subResponses);
            cleanupDocIDResponse(sub, subResponses, nonResolved);
            rawResponses.add(subResponses);
            if (sub != notProcessed) { // More to come
                synchronized (this) {
                    try {
                        this.wait(SUMMON_ID_SEARCH_WAIT);
                    } catch (InterruptedException e) {
                        log.warn("handleDocIDsPaged: Interrupted while waiting. Not a problem as it was just a delay");
                    }
                }
            } else {
                notProcessed.clear();
            }
        }

        final int singleLookups = nonResolved.size();
        List<String> missing;
        try {
            missing = singleStepIDLookup(nonResolved, rawResponses);
        } catch (Exception e) {
            log.warn("handleDocIDsPaged: Exception while attempting to single step resolve IDs "
                     + Strings.join(nonResolved, 10), e);
            missing = new ArrayList<>(nonResolved);
        }

        raws:
        for (ResponseCollection rc: rawResponses) {
            for (Response r: rc) {
                if (r instanceof DocumentResponse) {
                    DocumentResponse docs = (DocumentResponse)r;
                    docs.setMaxRecords(Integer.MAX_VALUE); // No trim on merge!
                    docs.setMaxRows(Integer.MAX_VALUE); // No trim on merge!
                    responses.add(docs);
                    continue raws;
                }
            }
            log.debug("handleDocIDsPaged: Encountered ResponseCollection without a DocumentResponse. " +
                      "Request was: " + request);
        }
        for (Response r: responses) {
            if (r instanceof DocumentResponse) {
                ((DocumentResponse)r).setMaxRecords(Integer.MAX_VALUE); // To guard against capping from later merge
            }
        }
        if (!missing.isEmpty()) {
            log.debug("handleDocIDsPaged: Unable to resolve IDs " + Strings.join(missing, 50));
        }
        log.info(summonIDs.size() + " documents requested. chunks_size=" + SUMMON_MAX_IDS + ", single_lookups="
                  + singleLookups + ", unresolved=" + missing.size() +
                  (missing.isEmpty() ? "" : "(" + Strings.join(missing, 3) + ")") + ", cached=" + cacheHits);
    }

    private int resolveRecordsFromCache(Request request, ArrayList<String> recordIDs, ResponseCollection responses) {
        if (cacheType != CACHE_TYPE.field && cacheType != CACHE_TYPE.wildcard) {
            return 0;
        }
        int localMatches = 0;
        String cacheKeyPostfix = cacheKeyPostfix(request);
        DocumentResponse cachedResponse = responseBuilder.createBasicDocumentResponse(request);
        for (int i = recordIDs.size()-1 ; i >= 0 ; i--) {
            DocumentResponse.Record record = recordCache.get(recordIDs.get(i) + cacheKeyPostfix);
            if (record != null) {
                localMatches++;
                cacheMatches++;
                cachedResponse.addRecord(record);
                recordIDs.remove(i);
            } else {
                cacheMisses++;
            }
        }
        if (cachedResponse.size() != 0) {
            responses.add(cachedResponse);
        }
        return localMatches;
    }

    /**
     * Performs a series of single requests for the given IDs.
     * @param recordIDs a list of IDs to look up.
     * @param responses one ResponseCollection/ID.
     * @return the IDs that could not be resolved.
     */
    private List<String> singleStepIDLookup(
            Set<String> recordIDs, List<ResponseCollection> responses) throws RemoteException {
        long startTime = System.currentTimeMillis();
        if (recordIDs.isEmpty()) {
            log.trace("singleStepIDLookup: No IDs to resolve");
            return Collections.emptyList();
        }
        List<String> missing = new ArrayList<>(recordIDs.size());
        for (final String id: recordIDs) { // FETCH_<id>
            synchronized (this) {
                try {
                    this.wait(SUMMON_ID_SEARCH_WAIT);
                } catch (InterruptedException e) {
                    log.debug("singleStepIDLookup: Interrupted while waiting. Not a problem as it was just a delay to "
                              + "guard against the summon request throughput limiter");
                }
            }
            if (singleIDSearch(responses, id, incl(id)) || singleIDSearch(responses, id, sans(id))) { // <id>
                continue;
            }
            missing.add(id);
            synchronized (this) {
                try {
                    this.wait(SUMMON_ID_SEARCH_WAIT);
                } catch (InterruptedException e) {
                    log.debug("singleStepIDLookup: Interrupted while waiting 2. Not a problem as it was just a delay");
                }
            }
        }
        log.debug("singleStepIDLookup(" + Strings.join(recordIDs, 10) + ", ...) got "
                  + (recordIDs.size() - missing.size()) + " records in " + (System.currentTimeMillis() - startTime)
                  + "ms." + (missing.isEmpty() ? "All resolved" : ("Unresolvable: " + Strings.join(missing))));
        return missing;
    }

    // Returns true if the ID-lookup was processes
    private boolean singleIDSearch(List<ResponseCollection> responses, final String originalID, String id)
            throws RemoteException {
        Request singleRequest = new Request();
        ResponseCollection singleResponses = new ResponseCollection();
        singleRequest.put(LEAF_ID_REQUEST, true);
        singleRequest.put(DocumentKeys.SEARCH_QUERY, fieldID + ":\"" + id + "\"");
        singleRequest.put(DocumentKeys.SEARCH_MAX_RECORDS, SUMMON_MAX_IDS);
        barrierSearch(singleRequest, singleResponses);
        if (countRecords(singleResponses) == 0) {
            return false;
        }
        fixID(singleResponses, originalID, id);
        if (countRecords(singleResponses) > 0) {
            responses.add(singleResponses);
        }
        return true;
    }

    private void fixID(ResponseCollection responses, String originalID, String id) {
        for (Response r: responses) {
            if (r instanceof DocumentResponse) {
                DocumentResponse docs = (DocumentResponse)r;
                for (DocumentResponse.Record record: docs.getRecords()) {
                    if (sans(id).equals(sans(record.getId()))) {
                        return; // Full match
                    }
                    log.debug("fixID: Explicit search for recordID:\"" + id + "\" resulted in ID '" + record.getId()
                              + "'");
                    record.add(new DocumentResponse.Field(ORIGINAL_LOOKUP_ID, originalID, true));
                }
            }
        }
    }

    // Remove documents with an ID not requested and update nonResolved with unmatched IDs from the request

    /**
     * Iterates the requestedIDs and locates the documents matching. The {@link #ORIGINAL_LOOKUP_ID} is set for those
     * documents and all non-matching documents are removed. All non-resolved IDs are copied to nonResolved.
     * @param requestedIDs the IDs to match to documents.
     * @param responses    summon response with documents.
     * @param nonResolved  all IDs that could not be matched to documents are added to this.
     */
    private void cleanupDocIDResponse(
            ArrayList<String> requestedIDs, ResponseCollection responses, Set<String> nonResolved) {
        DocumentResponse docs = null;
        List<DocumentResponse.Record> nonMatchedRecords = null;
        for (Response r: responses) {
            if (r instanceof DocumentResponse) {
                docs = (DocumentResponse)r;
                nonMatchedRecords = new ArrayList<>(docs.getRecords());
                break;
            }
        }
        if (docs == null) {
            log.debug("cleanupDocIDResponse: No document response with records so no ID-cleaning");
            return;
        }
        int nonResolvedCount = 0;

        // Prepare quick check for existence of ID
        final List<String> returnedVariants = new ArrayList<>();
        for (DocumentResponse.Record record: nonMatchedRecords) {
            addVariants(returnedVariants, record.getId());
        }
        final Set<String> quickCheck = new HashSet<>(returnedVariants);
        final List<String> requestIDVariants = new ArrayList<>();

        // Iterate requested IDs
        requestedLoop:
        for (String requestID: requestedIDs) {
            String sansPrefix = removePrefix(requestID);
            if (!quickCheck.contains(sansPrefix)) { // No matching record
                nonResolved.add(requestID);
                nonResolvedCount++;
                continue;
            }
            // Matching record present. Find it and process it
            requestIDVariants.clear();
            addVariants(requestIDVariants, requestID);
            for (int foundIndex = nonMatchedRecords.size()-1 ; foundIndex >= 0 ; foundIndex--) {
                final String returnedID = removePrefix(nonMatchedRecords.get(foundIndex).getId());
                if (requestIDVariants.contains(returnedID)) { // Found the record
                    nonMatchedRecords.get(foundIndex).add(
                            new DocumentResponse.Field(ORIGINAL_LOOKUP_ID, requestID, true));
                    nonMatchedRecords.remove(foundIndex);
                    continue requestedLoop;
                }
            }
            log.warn(String.format(Locale.ROOT,
                    "cleanupDocIDResponse: Logic error: Matched requestID '%s' with sans-representation '%s' with "
                    + "quick lookups (%s) but was unable to locate the ID in %d records",
                    requestID, sansPrefix, Strings.join(quickCheck), nonMatchedRecords.size()));
        }

        // Remove non-matched Records from the search result
        for (DocumentResponse.Record nonMatchedRecord: nonMatchedRecords) {
            if (docs.getRecords().remove(nonMatchedRecord)) {
                log.debug("cleanupDocIDResponse: Removed non-matching record " + nonMatchedRecord + " from result");
                docs.setHitCount(docs.getHitCount()-1);
            } else {
                log.warn("cleanupDocIDResponse: Logic error: Attempted to remove non-matching record "
                         + nonMatchedRecord + " from result but is was not present among the "
                         + docs.getRecords().size() + " records");
            }
        }
        if (nonResolvedCount == 0) {
            log.debug("cleanupDocIDResponse: Resolved all " + requestedIDs.size() + " IDs with "
                      + nonMatchedRecords.size() + " extra records left in the search result");
        }
    }

    private String removePrefix(String requestID) {
        return requestID.startsWith(idPrefix) ? requestID.substring(idPrefix.length()) : requestID;
    }

    // Adds all permutations of the docID that is seen as designating the same summon document, in order of priority
    private void addVariants(List<String> variants, String docID) {
        final String reduced = removePrefix(docID);
        variants.add(reduced);
        {
            final String adjusted = sans(docID);
            if (!reduced.equals(adjusted)) {
                variants.add(adjusted);
            }
        }
        {
            final String adjusted = incl(docID);
            if (!reduced.equals(adjusted)) {
                variants.add(adjusted);
            }
        }
        if (lookupAcceptOnePrefix) {
            variants.add(sans(docID) + "1");
            variants.add(incl(docID) + "1");
        }
    }

    private String sans(String id) {
        id = removePrefix(id);
        return id.replace(SUMMON_ID_PREFIX, "");
    }
    private String incl(String id) {
        id = removePrefix(id);
        return id.startsWith(SUMMON_ID_PREFIX) ? id : SUMMON_ID_PREFIX + id;
    }

    private int countRecords(List<ResponseCollection> rawResponses) {
        int count = 0;
        for (ResponseCollection rc: rawResponses) {
            count += countRecords(rc);
        }
        return count;
    }

    private int countRecords(ResponseCollection rc) {
        int count = 0;
        for (Response r: rc) {
            if (r instanceof DocumentResponse) {
                count += ((DocumentResponse)r).getRecords().size();
            }
        }
        return count;
    }

    private List<String> extractSummonIDs(final List<String> ids) {
        List<String> directIDs = new ArrayList<>(ids.size());
        for (final String id: ids) {
            if (id.startsWith(idPrefix) || id.startsWith(SUMMON_ID_PREFIX)) {
                directIDs.add(id);
            }
        }
        return directIDs;
    }

    private List<String> normalizeIDs(final List<String> ids) {
        List<String> summonIDs = new ArrayList<>(ids.size());
        for (String id: ids) {
            id = removePrefix(id);
            summonIDs.add(id.startsWith(SUMMON_ID_PREFIX) ? id : SUMMON_ID_PREFIX + id);
        }
        return summonIDs;
    }

    /**
     * Perform a search in Summon.
     *
     * @param request    the basic request.
     * @param filters    Solr-style filters (same syntax as query).
     * @param query      a Solr-style query.
     * @param solrParams optional extended params for Solr. If not null, these will be added to the Solr request.
     * @param facets     which facets to request or null if no facets are wanted.
     * @param startIndex the index for the first Record to return, counting from 0.
     * @param maxRecords number of items per page.
     * @param sortKey    the field to sort on. If null, default ranking sort is used.
     * @param reverseSort if true, sort order is reversed.
     * @param responses  results are stored here.
     * @return XML with the search result as per Solr API followed by timing information.
     * @throws java.rmi.RemoteException if there were an error performing the remote search call.
     */
    @Override
    protected Pair<String, String> solrSearch(
        Request request, List<String> filters, String query, Map<String, List<String>> solrParams,
        SolrFacetRequest facets, int startIndex, int maxRecords, String sortKey, boolean reverseSort,
        ResponseCollection responses) throws RemoteException {
        long buildQuery = -System.currentTimeMillis();

        // Summon treats startIndex as pages and counts from 1
        // This is handled in buildSolrQuery now
//        startIndex = startIndex == 0 ? 1 : maxRecords / startIndex + 1;

        log.trace("Calling simpleSearch(" + query + ", " + facets + ", " + startIndex + ", " + maxRecords + ")");
        Map<String, List<String>> queryMap;
        try {
            queryMap = buildSolrQuery(
                request, filters, query, solrParams, facets, startIndex, maxRecords, sortKey, reverseSort);
        } catch (ParseException e) {
            throw new RemoteException("Unable to build Solr query", e);
        }

        if (isPingRequest(request)) {
            log.trace("Ping requested");
            Date date = new Date();
            String sumID = computeIdString("application/xml", summonDateFormat.format(date), host, pingRest, null);
            buildQuery += System.currentTimeMillis();
            log.trace("Ping preparation done in " + buildQuery + "ms");

            long pingTime = -System.currentTimeMillis();
            String result;
            try {
                result = getData("http://" + host, pingRest, date, sumID, null,responses);
                pingTime += System.currentTimeMillis();
                if (log.isDebugEnabled()) {
                    log.debug("Ping returned in " + pingTime + "ms with result " + result);
                }
            } catch (Exception e) {
                throw new RemoteException("SummonSearchNode: Unable to ping "  + host + restCall, e);
            }
            return new Pair<>(null, "summon.pingtime:" + pingTime);
        }
        String retVal = null;
        if (validRequest(queryMap)) {
            Date date = new Date();
            String sumID = computeIdString("application/xml", summonDateFormat.format(date), host, restCall, queryMap);
            String queryString = computeSortedQueryString(queryMap, true);
            buildQuery += System.currentTimeMillis();
            log.trace("Parameter preparation done in " + buildQuery + "ms");

            String result;
            try {
                result = getData("http://" + host, restCall + "?" + queryString, date, sumID, null,responses);
///                System.out.println("********");
///                System.out.println(result);
///                System.out.println("********");
            } catch (Exception e) {
                throw new RemoteException("SummonSearchNode: Unable to perform remote call to "  + host + restCall
                                          + " with argument '" + queryString, e);
            }
            retVal = prefixIDs(result, idPrefix);
        }
        long prefixIDs = -System.currentTimeMillis();
        prefixIDs += System.currentTimeMillis();
        return new Pair<>(retVal, "summon.buildquery:" + buildQuery +  "|summon.prefixIDs:" + prefixIDs);
    }

    private boolean isPingRequest(Request request) {
        return request.isEmpty();
    }

    // True if either a query or a filter is present
    private boolean validRequest(Map<String, List<String>> queryMap) {
        if (queryMap.containsKey("s.fids") || queryMap.containsKey(DocumentKeys.SEARCH_IDS)) {
            return true;
        }
        if (emptyQueryNoSearch && containsEmpty(queryMap, "s.q")) {
            log.debug("Empty query. Skipping search");
            return false;
        }
        if (emptyFilterNoSearch && containsEmpty(queryMap, "s.fq")) {
            log.debug("Empty filter. Skipping search");
            return false;
        }
        return queryMap.containsKey("s.q") || queryMap.containsKey("s.fq");
    }

    private boolean containsEmpty(Map<String, List<String>> queryMap, String key) {
        if (!queryMap.containsKey(key)) {
            return false;
        }
        for (String value: queryMap.get(key)) {
            if (value.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected SolrFacetRequest createFacetRequest(
        String facetsDef, int minCount, int defaultFacetPageSize, String combineMode) {
        return new SolrFacetRequest(facetsDef, minCount, defaultFacetPageSize, combineMode) {
            @Override
            protected void addFacetQuery(
                Map<String, List<String>> queryMap, String field, String combineMode, int startPage, int pageSize) {
                append(queryMap, "s.ff", field + "," + combineMode + ",1," + pageSize);
            }
        };
    }

    @Override
    protected Map<String, List<String>> buildSolrQuery(
        Request request, List<String> filters, String query, Map<String, List<String>> solrParams,
        SolrFacetRequest facets, int startIndex, int maxRecords, String sortKey, boolean reverseSort)
            throws ParseException {

        if (maxRecords <= 0) {
            maxRecords = DocumentKeys.DEFAULT_MAX_RECORDS;
        }
        @SuppressWarnings("UnnecessaryLocalVariable")
        int perPage = maxRecords;

        // Solr starts at page 1
        int startPage = startIndex == 0 ? 1 : startIndex / perPage + 1;
        Map<String, List<String>> queryMap = new HashMap<>();

        queryMap.put("s.dym", Arrays.asList("true"));
        queryMap.put("s.ho", Arrays.asList("true"));
        if (!filters.isEmpty()) { // We allow missing filter
            boolean facetsHandled = false;
            if (request.getBoolean(SEARCH_SOLR_FILTER_IS_FACET, false)) {
                Map<String, List<String>> facetRequest = facetQueryTransformer.convertQueriesToFacet(filters);
                if (facetRequest == null) {
                    log.debug("Unable to convert facet filter [" + Strings.join(filters) + "] to Solr facet request. " +
                              "Switching to filter/query based handling");
                } else {
                    log.debug("Successfully converted filter [" + Strings.join(filters) + "] to Solr facet query");
                    queryMap.putAll(facetRequest);
                    facetsHandled = true;
                }
            }
            if (!facetsHandled) {
                if (supportsPureNegative || !request.getBoolean(DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE, false)) {
                    List<String> reducedFilters = convertQueries(filters, null);
                    if (!reducedFilters.isEmpty()) {
                        queryMap.put("s.fq", reducedFilters); // FilterQuery
                    }
                } else {
                    if (query == null) {
                        throw new IllegalArgumentException(
                            "No query and filter marked with '" + DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE
                            + "' is not possible in summon. Filters are [" + Strings.join(filters) + "]");
                    }
                    query = "(" + query + ")";
                    for (String filter: filters) {
                        query += "AND (" + filter + ")";
                    }
                    log.debug("Munging filter after query as the filters [" + Strings.join(filters) + "] are marked '"
                              + DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE + "' and summon is set up to not support pure "
                              + "negative filters natively. resulting query is '" + query + "'");
                }
            }
        }
        if (query != null) { // We allow missing query
            if (request.getBoolean(SEARCH_DISMAX_SABOTAGE, sabotageDismax)) {
                query = "(" + query + ")";
                log.debug("Sabotaging summon DisMax by putting the query in parentheses: '" + query + "'");
            }
            queryMap.put("s.q", Arrays.asList(query));
        }

        queryMap.put("s.ps", Arrays.asList(Integer.toString(perPage)));
        queryMap.put("s.pn", Arrays.asList(Integer.toString(startPage)));

        // TODO: Add support for sorting on multiple fields
        if (sortKey != null) {
            queryMap.put("s.sort", Arrays.asList(sortKey + ":" + (reverseSort ? "desc" : "asc")));
        }

        if (facets != null) {
            facets.addFacetQueries(queryMap);
        } else {
            log.debug("No facets specified, skipping faceting");
        }

        List<String> rangefacet = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        StringBuilder ranges = new StringBuilder();
        // generate 15 entries in the publication date facet starting from current year and working our way back
        ranges.append(currentYear).append(":");
        for (int i=1; i<15; i++) {
            ranges.append(",");
            ranges.append(currentYear - i).append(":").append(currentYear - i);
        }
        ranges.append(",").append(":").append(currentYear - 15);
        rangefacet.add("PublicationDate," + ranges.toString());
        queryMap.put("s.rff", rangefacet);

        if (solrParams != null) {
            queryMap.putAll(solrParams);
        }
        return queryMap;
    }

    private static String computeIdString(String acceptType, String  date, String  host, String  path,
                                          Map<String, List<String>> queryParameters) {
        return appendStrings(acceptType, date, host, path, computeSortedQueryString(queryParameters, false));
    }


    /**
     * Append the strings together with '\n' as a delimiter
     * @param strings The Strings to append together
     * @return A new String containing all the input Strings separated by '\n'
     */
    private static String appendStrings(String... strings) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String string : strings) {
            stringBuilder.append(string).append("\n");
        }
        return stringBuilder.toString();
    }

    /**
     * Calculate a base64 sha-1 digest for the input.
     * @param key The key to use while calculating the digest.
     * @param idString The String to digest.
     * @return A String containing a base64 encoded sha-1 digest.
     * @throws SignatureException in case of Signature problems.
     */
    private static String buildDigest(String key, String idString) throws SignatureException {
        try {
            String algorithm = "HmacSHA1";
            Charset charset = StandardCharsets.UTF_8;
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm);
            Mac mac = Mac.getInstance(algorithm);
            mac.init(signingKey);
            return new String(Base64.encodeBase64(mac.doFinal(idString.getBytes(charset))), charset);
        } catch (Exception e) {
            throw new SignatureException("Failed to generate HMAC : " + e.getMessage());
        }
    }


    static {
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
    }
    DateFormat summonDateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);
    /*
     * Gets the data from the remote Summon server
     * @param target
     * @param content
     * @param date
     * @param idstring
     * @param sessionId
     * @return A String containing XML describing the result of the call
     * @throws Exception
     */
    private String getData(String target, String content, Date date, String idstring,
                           String sessionId, ResponseCollection responses) throws Exception {
        StringBuilder retval = new StringBuilder();

        if (log.isDebugEnabled()) {
            log.debug("Performing Summon request for '" + content + "'");
        }

        URL url = new URL(target + content);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Host", host);
        conn.setRequestProperty("Accept", "application/xml");
        conn.setRequestProperty("Accept-Charset", "utf-8");
        conn.setRequestProperty("x-summon-date", summonDateFormat.format(date));
        conn.setRequestProperty("Authorization", "Summon " + accessID + ";" + buildDigest(accessKey, idstring));
        if (sessionId != null && !sessionId.isEmpty()) {
            conn.setRequestProperty("x-summon-session-id", sessionId);
        }
        conn.setConnectTimeout(connectionTimeout);
        conn.setReadTimeout(readTimeout);
        Long readStart = System.currentTimeMillis();
    	long summonConnect = -System.currentTimeMillis();
        try {
            conn.connect();
        } catch (Exception e) {
            String message = "Unable to connect to remote Solr with URL '" + url.toExternalForm()
                             + "' and connection timeout " + connectionTimeout;
            log.error(message, e);
            summonConnect += System.currentTimeMillis();
            lastConnectTime = summonConnect;
            throw (ConnectException)new ConnectException(message).initCause(e);
        }
        summonConnect += System.currentTimeMillis();
        lastConnectTime = summonConnect;

        long rawCall = -System.currentTimeMillis();
        BufferedReader in;
        try {
        	in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String str;
        
            while ((str = in.readLine()) != null) {
                retval.append(str);
            }
            log.trace("Reading from Summon done in " + (System.currentTimeMillis() - readStart) + "ms");
            in.close();
            rawCall += System.currentTimeMillis();
            lastDataTime = rawCall;
            responses.addTiming(getID() + ".connect", summonConnect);
            responses.addTiming(getID() + ".rawcall", rawCall);
            
        } catch (SocketTimeoutException e) {
            rawCall += System.currentTimeMillis();
            lastDataTime = rawCall;
            String error = String.format(Locale.ROOT,
                "getData(target='%s', content='%s', date=%s, idstring='%s', sessionID=%s) timed out",
                target, content, date, idstring, sessionId);
            log.warn(error, e);
            throw new IOException(error, e);
        } catch (Exception e) {
            rawCall += System.currentTimeMillis();
            lastDataTime = rawCall;
            String error = String.format(Locale.ROOT,
                "getData(target='%s', content='%s', date=%s, idstring='%s', sessionID=%s) failed with error stream\n%s",
                target, content, date, idstring, sessionId,
                conn.getErrorStream() == null ? "N/A" :
                Strings.flush(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8)));
            log.warn(error, e);
            throw new IOException(error, e);
        }
        // TODO: Should we disconnect?

        return retval.toString();
    }

    /**
     * Takes the xml result from a Summon query and prefixes all IDs with prefix. It looks for the value-tag in
     * <field name="ID">.
     * @param content xml result from a Summon query as a String
     * @param prefix the String the use as prefix
     * @return content with all IDs prefixed with prefix
     */
    private static String prefixIDs(String content, String prefix) {
        // <field name="ID">
        // <value>

        StringBuilder retval = new StringBuilder();

        Pattern pattern = Pattern.compile("<field name=\"ID\">\\s*<value>([^>]+)</value>");
        Matcher matcher = pattern.matcher(content);

        int index = 0;
        while (matcher.find()) {
            retval.append(content.substring(index, matcher.start(1)));
            retval.append(prefix).append(matcher.group(1));
            index = matcher.end(1);
        }
        retval.append(content.substring(index));

        return retval.toString();
    }
}
