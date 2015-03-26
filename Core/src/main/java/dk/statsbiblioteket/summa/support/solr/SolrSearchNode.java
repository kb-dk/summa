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
package dk.statsbiblioteket.summa.support.solr;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys;
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import dk.statsbiblioteket.summa.support.api.LuceneKeys;
import dk.statsbiblioteket.summa.support.summon.search.FacetQueryTransformer;
import dk.statsbiblioteket.summa.support.summon.search.SolrFacetRequest;
import dk.statsbiblioteket.summa.support.summon.search.SolrResponseBuilder;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryparser.classic.ParseException;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.util.*;

/**
 * A wrapper for Solr web calls, transforming requests and responses from and to Summa equivalents.
 * </p><p>
 * Besides the keys stated below, it is highly advisable to specify {@link SolrResponseBuilder#CONF_RECORDBASE}.
 * If 'recordBase' is requested as a facet and the response has no tags in that facet, a tag with the given ID as
 * content will be added with a count equal to the number of found documents.
 **/
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
// TODO: Always request the recordID field
public class SolrSearchNode extends SearchNodeImpl  { // TODO: implements DocumentSearcher
    private static Log log = LogFactory.getLog(SolrSearchNode.class);

    // TODO: Assign mandatory ID, use it for timing and result delivery
    /**
     * The entry point for calls to Solr.
     * </p><p>
     * Optional. Default is localhost:8983 (Solr default).
     */
    public static final String CONF_SOLR_HOST = "solr.host";
    public static final String DEFAULT_SOLR_HOST = "localhost:8983";
    /**
     * The rest call at {@link #CONF_SOLR_HOST}.
     * </p><p>
     * Optional. Default is '/solr' (Solr default).
     */
    public static final String CONF_SOLR_RESTCALL = "solr.restcall";
    public static final String DEFAULT_SOLR_RESTCALL = "/solr/select";
    /**
     * The timeout in milliseconds for establishing a connection to the remote Solr.
     * </p><p>
     * Optional. Default is 2000 milliseconds.
     */
    public static final String CONF_SOLR_CONNECTION_TIMEOUT = "solr.connection.timeout";
    public static final int DEFAULT_SOLR_CONNECTION_TIMEOUT = 2000;
    /**
     * The timeout in milliseconds for receiving data after a connection has been established to the remote Solr.
     * </p><p>
     * Optional. Default is 8000 milliseconds.
     */
    public static final String CONF_SOLR_READ_TIMEOUT = "solr.read.timeout";
    public static final int DEFAULT_SOLR_READ_TIMEOUT = 8000;
    /**
     * The prefix will be added to all IDs returned by Solr.
     * </p><p>
     * Optional. Default is empty.
     */
    public static final String CONF_SOLR_IDPREFIX = "solr.id.prefix";
    public static final String DEFAULT_SOLR_IDPREFIX = "";
    /**
     * The default number of documents results to return from a search.
     * </p><p>
     * This can be overridden with {@link dk.statsbiblioteket.summa.search.api.document.DocumentKeys#SEARCH_MAX_RECORDS}.
     * </p><p>
     * Optional. Default is 15.
     */
    public static final String CONF_SOLR_DEFAULTPAGESIZE = "solr.defaultpagesize";
    public static final int DEFAULT_SOLR_DEFAULTPAGESIZE = 15;
    /**
     * The minimum number of counts for a single tag to show up in the result list.
     * </p><p>
     * Optional. Default is 1.
     */
    public static final String CONF_SOLR_MINCOUNT = "solr.mincount";
    public static final int DEFAULT_SOLR_MINCOUNT = 1;
    /**
     * The default facets if none are specified. The syntax is a comma-separated
     * list of facet names, optionally with max tags in paranthesis.
     * This can be overridden with {@link dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys#SEARCH_FACET_FACETS}.
     * Specifying the empty string turns off faceting.
     * </p><p>
     * Optional. Default is {@link #DEFAULT_SOLR_FACETS}.
     */
    public static final String CONF_SOLR_FACETS = "solr.facets";
    public static final String DEFAULT_SOLR_FACETS = "";
    /**
     * The default number of tags tags to show in a facet if not specified
     * elsewhere.
     * </p><p>
     * Optional. Default is 15.
     */
    public static final String CONF_SOLR_FACETS_DEFAULTPAGESIZE = "solr.facets.defaultpagesize";
    public static final int DEFAULT_SOLR_FACETS_DEFAULTPAGESIZE = 15;
    /**
     * Whether facets should be searched with and or or.
     * Optional. Default is 'and'. Can only be 'and' or 'or'.
     */
    public static final String CONF_SOLR_FACETS_COMBINEMODE = "solr.facets.combinemode";
    public static final String DEFAULT_SOLR_FACETS_COMBINEMODE = "and";


    /**
     * If true, calls to Solr assumes that pure negative filters (e.g. "NOT foo NOT bar") are supported.
     * If false, pure negative filters are handled by rewriting the query to "(query) filter", so if query is "baz"
     * and the filter is "NOT foo NOT bar", the result is "(baz) NOT foo NOT bar".
     * Note that rewriting also requires the {@link DocumentKeys#SEARCH_FILTER_PURE_NEGATIVE} parameter to be true.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_SUPPORTS_PURE_NEGATIVE_FILTERS = "solr.purenegativefilters.support";
    public static final boolean DEFAULT_SUPPORTS_PURE_NEGATIVE_FILTERS = false;

    /**
     * If true, the SolrSearchNode does not attempt to extract facet-query from the query and passes the query and
     * filter through unmodified. Mainly used for debugging.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String SEARCH_PASSTHROUGH_QUERY = "solr.passthrough.query";
    public static final boolean DEFAULT_PASSTHROUGH_QUERY = false;

    /**
     * Properties with this prefix are added to the Solr query. Values are
     * lists of Strings. If one or more #CONF_SOLR_PARAM_PREFIX are
     * specified as part of a search query, the parameters are added to the
     * configuration defaults. Existing params with the same key are
     * overwritten.
     * </p><p>
     * Optional. Default is empty.
     */
    public static final String CONF_SOLR_PARAM_PREFIX = "solrparam.";
    /**
     * Search-time variation of {@link #CONF_SOLR_PARAM_PREFIX}.
     */

    /**
     * If true, {@link DocumentKeys#SEARCH_FILTER} must contain simple facet queries only. A simple facet query is
     * one or more {@code facet:term} pairs, optionally prefixed with {@code -} or {@code NOT}.
     * </p><p>
     * Valid sample query: {@code foo:bar -zoo:baz +ak:ve AND loo:poo NOT bum:bam}.
     * </p><p>
     * Note: This is basically an ugly hack until we switch to treating facet filtering as first class.
     */
    public static final String SEARCH_SOLR_FILTER_IS_FACET = "solr.filterisfacet";

    /**
     * The Solr field with the unique ID for a document.
     * </p><p>
     * Optional. Default is 'recordID'.
     */
    public static final String CONF_ID_FIELD = "solr.field.id";
    public static final String DEFAULT_ID_FIELD = IndexUtils.RECORD_FIELD;

    /**
     * Old hack that rewrote all queries so that there was a space after all parentheses in order to compensate
     * for the bug fixed by https://issues.apache.org/jira/browse/SOLR-3377
     */
    public static final String DEPRECATED_COMPENSATE_FOR_PARENTHESIS_BUG = "solr.solr3377.hack";

    /**
     * If true, faceting is handled with the facet.filter parameter. This works well for the facet pars but also means
     * that the document results will be invalid.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_EXPLICIT_FACET_FILTERING = "solr.facet.explicit.filter";
    public static final boolean DEFAULT_EXPLICIT_FACET_FILTERING = false;

    /**
     * If true, querying for the empty string will result in no search being performed.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_EMPTY_QUERY_NO_SEARCH = "solr.query.emptymatchesnone";
    public static final boolean DEFAULT_EMPTY_QUERY_NO_SEARCH = true;

    /**
     * If true, filtering for the empty string will result in no search being performed.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_EMPTY_FILTER_NO_SEARCH = "solr.filter.emptymatchesnone";
    public static final boolean DEFAULT_EMPTY_FILTER_NO_SEARCH = true;

    /**
     * If true, MoreLikeThis functionality is enabled. If false, the searcher returns immediately.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_MLT_ENABLED = "solr.mlt.enabled";
    public static final boolean DEFAULT_MLT_ENABLED = true;

    //    private static final DateFormat formatter =
    //        new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);
    protected SolrResponseBuilder responseBuilder;
    protected final String host;
    protected final String restCall;
    protected final int connectionTimeout;
    protected final int readTimeout;
    protected final String idPrefix;
    protected final int defaultPageSize;
    protected final int minCount;
    protected int defaultFacetPageSize = DEFAULT_SOLR_FACETS_DEFAULTPAGESIZE;
    protected String defaultFacets = DEFAULT_SOLR_FACETS;
    protected final String combineMode;
    protected final Map<String, List<String>> solrDefaultParams;
    protected final boolean supportsPureNegative;
    protected final FacetQueryTransformer facetQueryTransformer;
    protected final Set<String> nonescapedFields = new HashSet<>(10);
    protected final String fieldID;
    protected final boolean explicitFacetFilter;
    protected final boolean emptyQueryNoSearch;
    protected final boolean emptyFilterNoSearch;
    protected final boolean mltEnabled;

    // Debug & feedback
    protected long lastConnectTime = -1;
    protected long lastDataTime = -1;

    public SolrSearchNode(Configuration conf) throws RemoteException {
        super(conf);
        setID(conf.getString(CONF_ID, "solr"));
        List<String> nonEscaped = conf.getStrings(DocumentSearcher.CONF_NONESCAPED_FIELDS, new ArrayList<String>(0));
        nonescapedFields.addAll(nonEscaped);
        responseBuilder = createResponseBuilder(conf);
        responseBuilder.setNonescapedFields(nonescapedFields);
        solrDefaultParams = new HashMap<>();

        host = conf.getString(CONF_SOLR_HOST, DEFAULT_SOLR_HOST);
        restCall = conf.getString(CONF_SOLR_RESTCALL, DEFAULT_SOLR_RESTCALL);
        connectionTimeout = conf.getInt(CONF_SOLR_CONNECTION_TIMEOUT, DEFAULT_SOLR_CONNECTION_TIMEOUT);
        readTimeout = conf.getInt(CONF_SOLR_READ_TIMEOUT, DEFAULT_SOLR_READ_TIMEOUT);
        idPrefix =   conf.getString(CONF_SOLR_IDPREFIX, DEFAULT_SOLR_IDPREFIX);
        defaultPageSize = conf.getInt(CONF_SOLR_DEFAULTPAGESIZE, DEFAULT_SOLR_DEFAULTPAGESIZE);
        minCount = conf.getInt(CONF_SOLR_MINCOUNT, DEFAULT_SOLR_MINCOUNT);
        resolveDefaultFacets(conf);
        combineMode = conf.getString(CONF_SOLR_FACETS_COMBINEMODE, DEFAULT_SOLR_FACETS_COMBINEMODE);
        fieldID = conf.getString(CONF_ID_FIELD, DEFAULT_ID_FIELD);
        supportsPureNegative = conf.getBoolean(
                CONF_SUPPORTS_PURE_NEGATIVE_FILTERS, DEFAULT_SUPPORTS_PURE_NEGATIVE_FILTERS);
        facetQueryTransformer = createFacetQueryTransformer(conf);
        if (conf.valueExists(DEPRECATED_COMPENSATE_FOR_PARENTHESIS_BUG)) {
            log.warn("The property " + DEPRECATED_COMPENSATE_FOR_PARENTHESIS_BUG
                     + " serves no purpose in Solr 4 BETA+ and should be removed");
        }
        explicitFacetFilter = conf.getBoolean(CONF_EXPLICIT_FACET_FILTERING, DEFAULT_EXPLICIT_FACET_FILTERING);
        emptyQueryNoSearch = conf.getBoolean(CONF_EMPTY_QUERY_NO_SEARCH, DEFAULT_EMPTY_QUERY_NO_SEARCH);
        emptyFilterNoSearch = conf.getBoolean(CONF_EMPTY_FILTER_NO_SEARCH, DEFAULT_EMPTY_FILTER_NO_SEARCH);
        mltEnabled = conf.getBoolean(CONF_MLT_ENABLED, DEFAULT_MLT_ENABLED);
        readyWithoutOpen();
        log.info("Created SolrSearchNode(" + getID() + ")");
        // TODO: Add proper toString;
    }

    /**
     * Attempts to resolve default facets from the Solr schema.
     * @param conf fallback configuration of facets.
     */
    private void resolveDefaultFacets(Configuration conf) {
        defaultFacetPageSize = conf.getInt(CONF_SOLR_FACETS_DEFAULTPAGESIZE, DEFAULT_SOLR_FACETS_DEFAULTPAGESIZE);
        defaultFacets = conf.getString(CONF_SOLR_FACETS, DEFAULT_SOLR_FACETS);

        // TODO: Actually implement this
/*        try {
            solrSchema = getData(restCall, "schema.xml&contentType=text/xml;charset=utf-8", new ResponseCollection());
        } catch (IOException e) {
            log.warn("IOException trying to resolve Solr schema.xml. Using fallback facet setup", e);
            defaultFacetPageSize = conf.getInt(CONF_SOLR_FACETS_DEFAULTPAGESIZE, DEFAULT_SOLR_FACETS_DEFAULTPAGESIZE);
            defaultFacets = conf.getString(CONF_SOLR_FACETS, DEFAULT_SOLR_FACETS);
            return;
        }
        System.out.println("Got Solr schema " + solrSchema);*/
    }

    /**
     * Create a response builder from Solr to Summa responses. Override this to get parsing of responses that differ
     * from standard Solr.
     * @param conf base configuration for the transformer.
     * @return a search backend specific transformer.
     */
    protected SolrResponseBuilder createResponseBuilder(Configuration conf) {
        return new SolrResponseBuilder(conf);
    }

    /**
     * Create a transformer from Lucene search syntax queries into Solr facet queries. Override this to get specific
     * facet queries for searchers that differ from standard Solr.
     * @param conf base configuration for the transformer.
     * @return a search backend specific transformer.
     */
    protected FacetQueryTransformer createFacetQueryTransformer(Configuration conf) {
        return new FacetQueryTransformer(conf);
    }

    /**
     * Sort and optionally urlencodes a query string
     * @param queryParameters A Map<String, List<String>> containing the query parameters
     * @param urlencode Whether or not to urlencode the query parameters
     * @return A sorted and urlencoded query string
     */
    protected static String computeSortedQueryString(Map<String, List<String>> queryParameters, boolean urlencode) {
        if (queryParameters == null) {
            return "";
        }
        List<String> parameterStrings = new ArrayList<>();

        // for each parameter, get its key and values
        for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
            // for each value, create a string in the format key=value
            for (String value : entry.getValue()) {
                if (urlencode) {
                    try {
                        parameterStrings.add(entry.getKey() + "=" + URLEncoder.encode(value, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException("Unable to encode '" + value + "' to UTF-8. UTF-8 support is "
                                                   + "required for Summa to function", e);
                    }
                } else {
                    parameterStrings.add(entry.getKey() + "=" + value);
                }
            }
        }

        // sort the individual parameters
        Collections.sort(parameterStrings);
        return Strings.join(parameterStrings, "&");
    }

    @Override
    protected void managedSearch(Request request, ResponseCollection responses) throws RemoteException {
        try {
            barrierSearch(request, responses);
        } catch (StackOverflowError e) {
            String message = String.format(
                "Caught StackOverflow at outer level during handling of Solr request %s:\n%s",
                request.toString(true), reduceStackTrace(request, e));
            log.error(message, e);
            throw new RemoteException("SolrSearchNode.managedSearch: " + message);
        }
    }

    private static final String MLT_KEY = CONF_SOLR_PARAM_PREFIX + "mlt";
    protected void barrierSearch(Request oRequest, ResponseCollection responses) throws RemoteException {
        long startTime = System.currentTimeMillis();
        final Request request = oRequest.getPrefixAdjustedView(this.getID() + ".");
        if (!handleMLT(request, responses)) {
            return;
        }
        if (!handleDocIDs(request, responses)) {
            return;
        }
//        String rawQuery = getEmptyIsNull(request, DocumentKeys.SEARCH_QUERY);
//        String filter =  getEmptyIsNull(request, DocumentKeys.SEARCH_FILTER);

        // Due to reasons unknown, an empty value is returned as null.
        // We perform this ugly hack to get the empty string back if the value is the empty string.
        String rawQuery = request.getString(
                DocumentKeys.SEARCH_QUERY, request.containsKey(DocumentKeys.SEARCH_QUERY) ? "" : null);
        List<String> filters =  request.getStrings(
                DocumentKeys.SEARCH_FILTER, request.containsKey(DocumentKeys.SEARCH_FILTER) ?
                Arrays.asList("") : new ArrayList<String>());
        String sortKey = getEmptyIsNull(request, DocumentKeys.SEARCH_SORTKEY);
        if (DocumentKeys.SORT_ON_SCORE.equals(sortKey)) {
            sortKey = null; // null equals relevance ranking
        }
        boolean reverseSort = request.getBoolean(DocumentKeys.SEARCH_REVERSE, false);
        int startIndex = request.getInt(DocumentKeys.SEARCH_START_INDEX, 0);
        int maxRecords = request.getInt(DocumentKeys.SEARCH_MAX_RECORDS, (int)DocumentSearcher.DEFAULT_RECORDS);
        boolean collectdocIDs = request.getBoolean(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        boolean passThroughQuery = request.getBoolean(SEARCH_PASSTHROUGH_QUERY, DEFAULT_PASSTHROUGH_QUERY);

        if (rawQuery == null && filters.isEmpty()) {
            log.debug("No filter or query, proceeding anyway as other params might be specified");
        }

        String query;
        query = "*".equals(rawQuery) ? "*:*" : rawQuery;

        String facetsDef =  request.getString(FacetKeys.SEARCH_FACET_FACETS, defaultFacets);
        if (facetsDef != null && facetsDef.isEmpty()) {
            facetsDef = null;
        }
        SolrFacetRequest facets = null == facetsDef || facetsDef.isEmpty() ? null :
                                  createFacetRequest(facetsDef, minCount, defaultFacetPageSize, combineMode);

        Map<String, List<String>> solrSearchParams = new HashMap<>(solrDefaultParams);
        for (Map.Entry<String, Serializable> entry : request.entrySet()) {
            convertSolrParam(solrSearchParams, entry);
            convertFacetRangeRequest(solrSearchParams, entry);
        }
        if (query != null && !passThroughQuery) {
            query = convertQuery(query, solrSearchParams);
        }
        if ("".equals(query)) {
            if (emptyQueryNoSearch) {
                log.debug("Query was empty string and emptyQueryNoSearch==true");
                return;
            }
            query = null;
        }
        if (!filters.isEmpty() && !passThroughQuery && !request.getBoolean(SEARCH_SOLR_FILTER_IS_FACET, false)) {
            List<String> convertedFilters = new ArrayList<>(filters.size());
            for (String filter: filters) {
                String convertedFilter = convertQuery(filter, solrSearchParams);
                if (convertedFilter != null) {
                    convertedFilters.add(convertedFilter);
                }
            }
            filters = convertedFilters;
        }
        if (filters.size() == 1 && filters.get(0).isEmpty()) {
            if (emptyFilterNoSearch) {
                log.debug("Filter was empty string and emptyFilterNoSearch==true for query='" + query + "'. " +
                          "Returning without search");
                return;
            }
            filters.clear();
        }

        long searchTime = -System.currentTimeMillis();
/*        if (request.containsKey(LuceneKeys.SEARCH_MORELIKETHIS_RECORDID)) {
            if (fieldID != null && !fieldID.isEmpty()) {
                query = fieldID + ":\"" + request.getString(LuceneKeys.SEARCH_MORELIKETHIS_RECORDID) + "\"";
            } else {
                query = request.getString(LuceneKeys.SEARCH_MORELIKETHIS_RECORDID);
            }
            solrSearchParams.put("mlt", Arrays.asList("true"));
            log.debug("Performing MoreLikeThis search for '" + query);
        }*/
        log.trace("Performing search for '" + query + "' with facets '" + facets + "'");
        String solrResponse;
        String solrTiming;
        try {
            Pair<String, String> sums = solrSearch(
                request, filters, query, solrSearchParams, collectdocIDs ? facets : null,
                startIndex, maxRecords, sortKey, reverseSort, responses);
            solrResponse = sums.getKey();
            solrTiming = sums.getValue();
        } catch (StackOverflowError e) {
            String message = String.format("Caught StackOverflow while performing Solr request %s:\n%s",
                                           request.toString(true), reduceStackTrace(request, e));
            log.error(message, e);
            throw new RemoteException("SolrSearchNode.barrierSearch: " + message);
        }
        if (solrResponse == null || solrResponse.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("fullSearch(" + request.toString(true) + ", [" + Strings.join(filters) + "], " + rawQuery
                          + ", " + startIndex + ", " + maxRecords + ", " + sortKey + ", " + reverseSort
                          + ") did not return a result. This might be due to missing query and filter");
            }
            responses.addTiming(getID() + ".search.total", System.currentTimeMillis() - startTime);
            responses.addTiming(solrTiming);
            return;
        }
        searchTime += System.currentTimeMillis();

        long buildResponseTime = -System.currentTimeMillis();
        long hitCount;
        try {
            //System.out.println(solrResponse.replace(">", ">\n"));
            hitCount = responseBuilder.buildResponses(request, facets, responses, solrResponse, solrTiming);
        } catch (XMLStreamException e) {
            String message = "Unable to transform Solr XML response to Summa response for '" + request + "'";
            if (log.isDebugEnabled()) {
                log.debug(message + ". Full XML follows:\n" + solrResponse);
            }
            throw new RemoteException(message, e);
        } catch (StackOverflowError e) {
            String message = String.format(
                "Caught StackOverflow while building response for Solr request %s\nReduced stack trace:\n%s\n"
                + "Reduced raw Solr response:\n%s",
                request.toString(true), reduceStackTrace(request, e),
                solrResponse.length() > 2000 ? solrResponse.substring(0, 2000) : solrResponse);
            log.error(message, e);
            throw new RemoteException("SolrSearchNode.barrierSearch: " + message);

        }
        buildResponseTime += System.currentTimeMillis();

        if (log.isDebugEnabled()) {
            log.debug("fullSearch(" + request.toString(true) + ", [" + Strings.join(filters) + "], " + rawQuery + ", "
                      + startIndex + ", " + maxRecords + ", " + sortKey + ", " + reverseSort + ") with " + hitCount
                      + " hits finished in " + searchTime + " ms (" + searchTime + " ms for remote search call, "
                      + buildResponseTime + " ms for converting to Summa response)");
        }
        responses.addTiming(getID() + ".search.buildresponses", buildResponseTime);
        responses.addTiming(getID() + ".search.total", System.currentTimeMillis() - startTime);
    }

    /**
     * Rewrite or explicitly process requests for docIDs.
     * @param request   a Solr request.
     * @param responses responses so far.
     * @return true if standard processing should commence, false if the searcher should return immediately.
     */
    protected boolean handleDocIDs(Request request, ResponseCollection responses) throws RemoteException {
        return true; // Handled by {@link DocumentSearcherImpl#adjustRequest}.
    }

    /**
     * Rewrite the MoreLikeThis requests or discard the request altogether if MLT is requested but not supported.
     * @param request   a Solr request.
     * @param responses responses so far.
     * @return true if standard processing should commence, false if the searcher should return immediately.
     */
    protected boolean handleMLT(Request request, ResponseCollection responses) {
        if (!mltEnabled && (request.containsKey(LuceneKeys.SEARCH_MORELIKETHIS_RECORDID) ||
                            request.getBoolean(MLT_KEY, false))) {
            log.debug("Received MLT for '" + request.getString(LuceneKeys.SEARCH_MORELIKETHIS_RECORDID, "N/A") + "'. "
                      + "Skipping MoreLikeThis as this is not enabled for this search node");
            return false;
        }

        // MoreLikeThis
        if (request.containsKey(LuceneKeys.SEARCH_MORELIKETHIS_RECORDID)) {
            String id = request.getString(LuceneKeys.SEARCH_MORELIKETHIS_RECORDID);
            if (!request.containsKey(MLT_KEY)) {
                log.debug("Setting " + MLT_KEY + "=true as " + LuceneKeys.SEARCH_MORELIKETHIS_RECORDID
                          + " is set with '" + id);
                request.put(MLT_KEY, true);
            }
            String q = IndexUtils.RECORD_FIELD + ":\"" + id + "\"";
            log.debug("Setting " + DocumentKeys.SEARCH_QUERY + "=" + q + " and removing filter as "
                      + LuceneKeys.SEARCH_MORELIKETHIS_RECORDID + " is defined");
            request.put(DocumentKeys.SEARCH_QUERY, q);
            request.remove(DocumentKeys.SEARCH_FILTER);
        }
        return true;
    }

    // Override this to get search backend specific facet request syntax
    protected SolrFacetRequest createFacetRequest(
        String facetsDef, int minCount, int defaultFacetPageSize, String combineMode) {
        return new SolrFacetRequest(facetsDef, minCount, defaultFacetPageSize, combineMode);
    }

    /**
     * Extracts parameters with key prefix "solrparam." and stores the truncated keys with their value(s) as a list of
     * Strings.
     * </p><p>
     * If the key is not prefixed by "solrparam." or id+".solrparam., it is ignored.
     * </p>
     * @param solrParam the map where the converted key/value will be stored.
     * @param entry the source for the key/value pair.
     * @return true if the entry was added to solrParam, else false.
     */
    protected boolean convertSolrParam(Map<String, List<String>> solrParam, Map.Entry<String, Serializable> entry) {
        final String key = toSolrKey(entry.getKey());
        if (key == null) {
            log.trace("convertSolrParam got unsupported key " + entry.getKey() + ". Ignoring entry");
            return false;
        }
        Serializable value = entry.getValue();

        putSolrParam(solrParam, key, value);
        return true;
    }

    private void putSolrParam(Map<String, List<String>> solrParam, String key, Serializable value) {
        if (value instanceof List) {
            ArrayList<String> values = new ArrayList<>();
            for (Object v: (List)value) {
                if (v instanceof String) {
                    values.add((String)v);
                } else {
                    log.warn("Expected List entry of type String in Solr parameter " + key + ", but got Object of "
                             + "class " + v.getClass());
                }
            }
            if (values.isEmpty()) {
                log.warn("Got empty list for Solr param " + key + ". Ignoring");
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("convertSolrParam assigning list " + key + ":" + Strings.join(values, ", "));
                }
                solrParam.put(key, values);
            }
        } else if (value instanceof String[]) {
            if (log.isTraceEnabled()) {
                log.trace("convertSolrParam assigning array " + key + ":" + Strings.join((String[]) value, ", "));
            }
            solrParam.put(key, Arrays.asList((String[]) value));
        } else { // Simple type (String, Integer...)
            solrParam.put(key, Arrays.asList(value.toString()));
            if (log.isTraceEnabled()) {
                log.trace("convertSolrParam assigning " + key + ":" + value);
            }
        }
    }

    /**
     * Extracts facet range parameters and converts them to Solr params.
     * @param solrParams the map where the converted key/value will potentially be stored.
     * @param entry the source for the key/value pair.
     */
    protected void convertFacetRangeRequest(
            Map<String, List<String>> solrParams, Map.Entry<String, Serializable> entry) {
        String key = entry.getKey();
        if (FacetKeys.FACET_RANGE.equals(key)) {
            if (entry.getValue() instanceof String) {
                putSolrParam(solrParams, key, ((String)entry.getValue()).split(", *"));
            } else {
                putSolrParam(solrParams, key, entry.getValue());
            }
        } else if (key.contains(FacetKeys.FACET_RANGE_START) ||
                   key.contains(FacetKeys.FACET_RANGE_END) ||
                   key.contains(FacetKeys.FACET_RANGE_GAP)) {
            putSolrParam(solrParams, key, entry.getValue());
        }
    }

    /* If the key is prefixed the right way, the prefix is removed and the key returned, else null is returned */
    protected String toSolrKey(String key) {
        if (key.startsWith(getID() + ".")) {
            key = key.substring((getID() + ".").length());
        }
        return !key.startsWith(CONF_SOLR_PARAM_PREFIX) ? null: key.substring(CONF_SOLR_PARAM_PREFIX.length());
    }

    private String getEmptyIsNull(Request request, String key) {
        String response = request.getString(key, null);
        return response == null || response.isEmpty() ? null : response;
    }

    private boolean warmupCalled = false;
    @Override
    protected void managedWarmup(String request) {
        if (!warmupCalled) {
            log.debug("No warmup for '" + request + "' as warmup is handled externally. Further requests "
                      + "for warmup will be silently ignored");
            warmupCalled = true;
        }
    }

    @Override
    protected void managedOpen(String location) throws RemoteException {
        log.info("Open called with location '" + location + "' which is "
                 + "ignored by this search node as it is stateless");
    }

    @Override
    protected void managedClose() throws RemoteException {
        log.debug("managedClose() called. No effect as this search node is stateless");
    }


    /**
     * Optionally converts the query to conform to searcher specific syntax.
     * @param query            the input query.
     * @param solrSearchParams parameters that will be passed to Solr.
     * @return the converted query.
     */
    protected String convertQuery(String query, Map<String, List<String>> solrSearchParams) {
        log.trace("Default convertQuery does not change the query");
        return query;
    }

    protected List<String> convertQueries(List<String> queries, Map<String, List<String>> solrSearchParams) {
        log.trace("Default convertQueries does not change the input");
        List<String> convertedQueries = new ArrayList<>(queries.size());
        for (String query: queries) {
            String convertedQuery = convertQuery(query, solrSearchParams);
            if (convertedQuery != null) {
                convertedQueries.add(convertedQuery);
            }
        }
        return convertedQueries;
    }

    /**
     * Perform a search in Solr. Override this to get different behaviour for search backends other than standard Solr.
     *
     * @param request    a standard Summa Search request, primarily filled with values from {@link DocumentKeys}.
     * @param filters    Solr-style filters (same syntax as query). This is based on {@link DocumentKeys#SEARCH_FILTER}
     *                   but might have been rewritten.
     * @param query      a Solr-style query. This is based on {@link DocumentKeys#SEARCH_QUERY} but might have been
     *                   rewritten.
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
    protected Pair<String, String> solrSearch(
        Request request, List<String> filters, String query, Map<String, List<String>> solrParams,
        SolrFacetRequest facets, int startIndex, int maxRecords, String sortKey, boolean reverseSort,
        ResponseCollection responses) throws RemoteException {
        long buildQuery = -System.currentTimeMillis();
        log.trace("Calling simpleSearch(" + query + ", " + facets + ", " + startIndex + ", " + maxRecords + ")");
        Map<String, List<String>> queryMap;
        try {
            queryMap = buildSolrQuery(
                request, filters, query, solrParams, facets, startIndex, maxRecords, sortKey, reverseSort);
        } catch (ParseException e) {
            throw new RemoteException("Unable to build Solr query", e);
        }
        String queryString = computeSortedQueryString(queryMap, true);
        buildQuery += System.currentTimeMillis();
        log.trace("Parameter preparation done in " + buildQuery + "ms");
        String result = null;
        if (validRequest(queryMap)) {
            try {
                result = getData(restCall, queryString, responses);
//                System.out.println("*** " + result.replace("<", "\n<"));
            } catch (Exception e) {
                throw new RemoteException("SolrSearchNode: Unable to perform remote call to "  + host + restCall
                                          + " with argument '" + queryString + " and message " + e.getMessage());
            }
        }
        log.trace("simpleSearch done in " + (System.currentTimeMillis() - buildQuery) + "ms");
        return new Pair<>(result, "solr.buildquery:" + buildQuery);

    }

    // True if either a query or a filter is present
    private boolean validRequest(Map<String, List<String>> queryMap) {
        return queryMap.containsKey("q") || queryMap.containsKey("fq");
    }
       // {start=[0], q=[gense], spellcheck.dictionary=[summa_spell], qt=[/didyoumean], rows=[15]}
    //  {spellcheck=[true], start=[0], q=[gense], spellcheck.dictionary=[summa_spell], spellcheck.count=[5], qt=[/didyoumean], rows=[15]}

    private String getData(String path, String params, ResponseCollection responses) throws IOException {
        String command = path + "?" + params;
        StringBuilder retval = new StringBuilder();

        if (log.isDebugEnabled()) {
            log.debug("Performing Solr request for '" + command + "'");
        }

        //URL url = new URL("http://" + host + command);
        URL url = new URL("http://" + host + path);
        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) url.openConnection();
        } catch (Exception e) {
            String message = "Exception while calling HttpURLConnection(" + url.toExternalForm() + ").openConnection()";
            log.error(message, e);
            throw (ConnectException)new ConnectException(message).initCause(e);
        }
        conn.setRequestProperty("Host", host);
        conn.setRequestProperty("Accept", "application/xml");
        conn.setRequestProperty("Accept-Charset", "utf-8");
        // http://www.xyzws.com/Javafaq/how-to-use-httpurlconnection-post-data-to-web-server/139
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", "" + Integer.toString(params.getBytes().length));
        conn.setUseCaches (false);
        conn.setDoInput(true);
        conn.setDoOutput(true);

        conn.setConnectTimeout(connectionTimeout);
        conn.setReadTimeout(readTimeout);
        Long readStart = System.currentTimeMillis();
    	long summonConnect = -System.currentTimeMillis();
        try {
            //Send request
            DataOutputStream wr = new DataOutputStream (conn.getOutputStream ());
            wr.writeBytes(params);
            wr.flush ();
            wr.close ();

            //conn.connect();
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

        BufferedReader in;
        long rawCall = -System.currentTimeMillis();
        try {
        	in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String str;

            while ((str = in.readLine()) != null) {
                retval.append(str);
            }
            log.trace("Reading from Solr done in " + (System.currentTimeMillis() - readStart) + "ms");
            in.close();
            rawCall += System.currentTimeMillis();
            lastDataTime = rawCall;
            responses.addTiming(getID() + ".connect", summonConnect);
            responses.addTiming(getID() + ".rawcall", rawCall);

        } catch (Exception e) {
            rawCall += System.currentTimeMillis();
            lastDataTime = rawCall;
            String error = String.format(
                "getData(host='%s', command='%s') for %s failed with error stream\n%s",
                "http://" + host, command, getID(),
                Strings.flush(new InputStreamReader(conn.getErrorStream(), "UTF-8")));
            log.warn(error, e);
            throw new IOException(error, e);
        }
        // TODO: Should we disconnect?
        //System.out.println(retval.toString());
        return retval.toString();
    }

    /**
     * Generate a map of search backend specific request parameters.
     * @param request    a standard Summa Search request, primarily filled with values from {@link DocumentKeys}.
     * @param filters    Solr-style filters (same syntax as query). This is based on {@link DocumentKeys#SEARCH_FILTER}
     *                   but might have been rewritten.
     * @param query      a Solr-style query. This is based on {@link DocumentKeys#SEARCH_QUERY} but might have been
     *                   rewritten.
     * @param solrParams optional extended params for Solr. If not null, these will be added to the Solr request.
     * @param facets     which facets to request or null if no facets are wanted.
     * @param startIndex the index for the first Record to return, counting from 0.
     * @param maxRecords number of items per page.
     * @param sortKey    the field to sort on. If null, default ranking sort is used.
     * @param reverseSort if true, sort order is reversed.
     * @return key-value map with multiple values/key.
     * @throws ParseException if the facets could not be parsed.
     */
    protected Map<String, List<String>> buildSolrQuery(
        Request request, List<String> filters, String query, Map<String, List<String>> solrParams,
        SolrFacetRequest facets, int startIndex, int maxRecords, String sortKey, boolean reverseSort)
            throws ParseException {
        int startPage = Math.max(0, startIndex); // Solr pages exactly as Lucene
        //int startPage = Math.max(0, maxRecords == 0 ? 0 : (startIndex-1) / maxRecords);
        Map<String, List<String>> queryMap = new HashMap<>();
        if (request.containsKey(DocumentKeys.SEARCH_RESULT_FIELDS)) {
            Set<String> fl = new HashSet<>(request.getStrings(DocumentKeys.SEARCH_RESULT_FIELDS));
            if (sortKey != null) {
                fl.add(sortKey); // In order to return the value of the sortKey as part of the response
            }
            queryMap.put("fl", Arrays.asList(Strings.join(fl, ",")));
        } else if (sortKey != null) {
            queryMap.put("fl", Arrays.asList("*," + sortKey));
        }
        if (!filters.isEmpty()) { // We allow missing filter
            boolean facetsHandled = false;
            if (explicitFacetFilter && request.getBoolean(SEARCH_SOLR_FILTER_IS_FACET, false)) {
                Map<String, List<String>> facetRequest = facetQueryTransformer.convertQueriesToFacet(filters);
                if (facetRequest == null) {
                    log.debug("Unable to convert facet filters [" + Strings.join(filters) + "' to Solr facet request." +
                              " Switching to filter/query based handling");
                } else {
                    log.debug("Successfully converted filter [" + Strings.join(filters) + "] to Solr facet query");
                    queryMap.putAll(facetRequest);
                    facetsHandled = true;
                }
            }
            if (!facetsHandled) {
                if (supportsPureNegative || !request.getBoolean(DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE, false)) {
                    queryMap.put("fq", filters); // FilterQuery
                } else {
                    if (query == null) {
                        throw new IllegalArgumentException(
                            "No query and filter marked with '" + DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE
                            + "' is not possible in Solr. Filters are [" + Strings.join(filters) + "]");
                    }
                    query = "(" + query + ")";
                    for (String filter: filters) {
                        query += "AND (" + filter + ")";
                    }
                    log.debug("Munging filter after query as the filters '" + Strings.join(filters) + "' are marked '"
                              + DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE + "' and Solr is set up to not support pure "
                              + "negative filters natively. resulting query is '" + query + "'");
                }
            }
        }
        if (query != null) { // We allow missing query
            queryMap.put("q", Arrays.asList(query));
        }

        queryMap.put("start", Arrays.asList(Integer.toString(startPage)));
        queryMap.put("rows", Arrays.asList(Integer.toString(maxRecords)));
        // TODO: Add support for explicit row parameter
        if (request.containsKey(DocumentKeys.GROUP)) {
            queryMap.put(DocumentKeys.GROUP, Arrays.asList(
                    request.getBoolean(DocumentKeys.GROUP, false).toString()));
        }
        if (request.containsKey(DocumentKeys.GROUP_FIELD)) {
            queryMap.put(DocumentKeys.GROUP_FIELD, Arrays.asList(
                    request.getString(DocumentKeys.GROUP_FIELD)));
        }
        if (request.containsKey(DocumentKeys.GROUP_LIMIT)) {
            queryMap.put(DocumentKeys.GROUP_LIMIT, Arrays.asList(
                    request.getInt(DocumentKeys.GROUP_LIMIT, DocumentKeys.DEFAULT_GROUP_LIMIT).toString()));
        }
        if (request.containsKey(DocumentKeys.ROWS)) {
            queryMap.put(DocumentKeys.ROWS, Arrays.asList(
                    request.getInt(DocumentKeys.ROWS, maxRecords).toString()));
        }

        // TODO: Add support for sorting on multiple fields
        if (reverseSort && sortKey == null) {
            sortKey = "score"; // Relevance sorting
        }
        if (sortKey != null) {
            queryMap.put("sort", Arrays.asList(sortKey + " " + (reverseSort ? "desc" : "asc")));
        }

        if (facets != null) { // The facets to display
            queryMap.put("facet", Arrays.asList(Boolean.TRUE.toString()));
            facets.addFacetQueries(queryMap);
        }

        append(solrParams, queryMap);
        return queryMap;
    }

    private void append(Map<String, List<String>> src, Map<String, List<String>> dest) {
        for (Map.Entry<String, List<String>> entry: src.entrySet()) {
            if (dest.containsKey(entry.getKey())) {
                // TODO: Find fuller list of single value
                if ("q".equals(entry.getKey())) {
                    log.warn(String.format(
                            "The solr params contained q='%s' while the explicit params contained q='%s'. "
                            + "The solr param will overwrite the explicit param",
                            Strings.join(entry.getValue()), Strings.join(dest.get(entry.getKey()))));
                    dest.put(entry.getKey(), entry.getValue());
                    continue;
                }
                List<String> combined = new ArrayList<>(entry.getValue());
                combined.addAll(dest.get(entry.getKey()));
                dest.put(entry.getKey(), combined);
            } else {
                dest.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Clear all content in the index.
     */
    public void clear() throws IOException {
        log.info("Clearing all data in the Solr index");
        long startTime = System.currentTimeMillis();
        ResponseCollection responses = new ResponseCollection();
        getData(restCall, "<delete><query>*:*</query></delete>?commit=true", responses);
        log.info("Cleared all data in the Solr index in " + (System.currentTimeMillis() - startTime) + "ms");
    }

    /**
     * @return the number of milliseconds used for establishing the last connection.
     */
    public long getLastConnectTime() {
        return lastConnectTime;
    }

    /**
     * @return the number of milliseconds used for receiving data for the last request.
     */
    public long getLastDataTime() {
        return lastDataTime;
    }
}
