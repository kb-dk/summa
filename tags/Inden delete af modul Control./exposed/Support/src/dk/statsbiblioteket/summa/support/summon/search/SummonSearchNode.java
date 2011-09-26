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
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.support.api.LuceneKeys;
import dk.statsbiblioteket.summa.support.harmonise.QueryRewriter;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
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
 * Note that {@link #SEARCH_SUMMON_RESOLVE_LINKS} is unique to this searcher.
 */
// Map contenttype, language. Convert date (til år lige nu), potentially library
    // TODO: Check if startpos is 0 or 1, adjuct accordingly
    // TODO: Implement configurable rangefacets
    // TODO Implement getShortRecord
    // TODO: Implement getRecord in Storage
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummonSearchNode extends SearchNodeImpl {
    private static Log log = LogFactory.getLog(SummonSearchNode.class);
    private static Log summonlog = LogFactory.getLog("summon_performance");

    /**
     * The entry point for calls to Summon. This is unlikely to change.
     * </p><p>
     * Optional. Default is api.summon.serialssolutions.com.
     */
    public static final String CONF_SUMMON_HOST = "summon.host";
    public static final String DEFAULT_SUMMON_HOST =
        "api.summon.serialssolutions.com";

    /**
     * The rest call at {@link #CONF_SUMMON_HOST}. This is unlikely to change.
     * </p><p>
     * Optional. Default is '/search'.
     */
    public static final String CONF_SUMMON_RESTCALL = "summon.restcall";
    public static final String DEFAULT_SUMMON_RESTCALL = "/search";

    /**
     * The access ID for Summon. Serial Solutions controls this and in order to
     * use this filter, a valid access ID must be provided.
     * </p><p>
     * Mandatory.
     * @see {@link #CONF_SUMMON_ACCESSKEY}.
     */
    public static final String CONF_SUMMON_ACCESSID = "summon.accessid";

    /**
     * The access key for Summon. Serial Solutions controls this and in order to
     * use this filter, a valid access key must be provided.
     * </p><p>
     * Mandatory.
     * @see {@link #CONF_SUMMON_ACCESSID}.
     */
    public static final String CONF_SUMMON_ACCESSKEY = "summon.accesskey";

    /**
     * The prefix will be added to all IDs returned by Summon.
     * </p><p>
     * Optional. Default is summon_.
     */
    public static final String CONF_SUMMON_IDPREFIX = "summon.id.prefix";
    public static final String DEFAULT_SUMMON_IDPREFIX = "summon_";

    /**
     * The default number of documents results to return from a search.
     * </p><p>
     * This can be overridden with {@link DocumentKeys#SEARCH_MAX_RECORDS}.
     * </p><p>
     * Optional. Default is 15.
     */
    public static final String CONF_SUMMON_DEFAULTPAGESIZE =
        "summon.defaultpagesize";
    public static final int DEFAULT_SUMMON_DEFAULTPAGESIZE = 15;

    /**
     * The default facets if none are specified. The syntax is a comma-separated
     * list of facet names, optionally with max tags in paranthesis.
     * This can be overridden with {@link FacetKeys#SEARCH_FACET_FACETS}.
     * Specifying the empty string turns off faceting.
     * </p><p>
     * Optional. Default is {@link #DEFAULT_SUMMON_FACETS}.
     */
    public static final String CONF_SUMMON_FACETS = "summon.facets";
    public static final String DEFAULT_SUMMON_FACETS =
        "Author, " // Map
        + "ContentType, " // Map 1:n
        + "Genre, " // Direct
        + "IsScholarly, " // Direct
        + "Language, " // Map
        + "IsFullText, "
        //"Library,and,1,5",
        //"PackageID,and,1,5",
        //"SourceID,and,1,5",
        + "SubjectTerms, " // SB:subject
        + "TemporalSubjectTerms"; // Direct

    /**
     * The default number of tags tags to show in a facet if not specified
     * elsewhere.
     * </p><p>
     * Optional. Default is 15.
     * See http://api.summon.serialssolutions.com/help/api/search/commands/add-facet-field
     */
    public static final String CONF_SUMMON_FACETS_DEFAULTPAGESIZE =
        "summon.facets.defaultpagesize";
    public static final int DEFAULT_SUMMON_FACETS_DEFAULTPAGESIZE = 15;

    /**
     * Whether facets should be searched with and or or.
     * Optional. Default is 'and'. Can only be 'and' or 'or'.
     * See http://api.summon.serialssolutions.com/help/api/search/commands/add-facet-field
     */
    public static final String CONF_SUMMON_FACETS_COMBINEMODE =
        "summon.facets.combinemode";
    public static final String DEFAULT_SUMMON_FACETS_COMBINEMODE = "and";

    /**
     * Whether or not links in the Summon response should be resolved using the
     * 360 link resolver.
     * This can be overridden with {@link #SEARCH_SUMMON_RESOLVE_LINKS}.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_SUMMON_RESOLVE_LINKS =
        "summon.resolvelinks";
    public static final boolean DEFAULT_SUMMON_RESOLVE_LINKS = false;

    public static final String SEARCH_SUMMON_RESOLVE_LINKS =
        "search.summon.resolvelinks";

    /**
     * Properties with this prefix are added to the summon query. Values are
     * lists of Strings. If one or more {@link #SEARCH_SUMMON_PARAM_PREFIX} are
     * specified as part of a search query, the parameters are added to the
     * configuration defaults. Existing params with the same key are
     * overwritten.
     * </p><p>
     * Optional. Default is empty.
     */
    public static final String CONF_SUMMON_PARAM_PREFIX = "summonparam.";
    /**
     * Search-time variation of {@link #CONF_SUMMON_PARAM_PREFIX}.
     */
    public static final String SEARCH_SUMMON_PARAM_PREFIX =
        CONF_SUMMON_PARAM_PREFIX;

    private static final DateFormat formatter =
        new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);
    private SummonResponseBuilder responseBuilder;

    private String host = DEFAULT_SUMMON_HOST;
    private String restCall = DEFAULT_SUMMON_RESTCALL;
    private String accessID;
    private String accessKey;
    private String idPrefix = DEFAULT_SUMMON_IDPREFIX;
    private int defaultPageSize = DEFAULT_SUMMON_DEFAULTPAGESIZE;
    private int defaultFacetPageSize = DEFAULT_SUMMON_FACETS_DEFAULTPAGESIZE;
    private String defaultFacets = DEFAULT_SUMMON_FACETS;
    private String combineMode = DEFAULT_SUMMON_FACETS_COMBINEMODE;
    private boolean defaultResolveLinks = DEFAULT_SUMMON_RESOLVE_LINKS;
    private final Map<String, List<String>> summonDefaultParams;

    public SummonSearchNode(Configuration conf) throws RemoteException {
        super(conf);
        host = conf.getString(CONF_SUMMON_HOST, host);
        restCall = conf.getString(CONF_SUMMON_RESTCALL, restCall);
        accessID =   conf.getString(CONF_SUMMON_ACCESSID);
        accessKey =  conf.getString(CONF_SUMMON_ACCESSKEY);
        idPrefix =   conf.getString(CONF_SUMMON_IDPREFIX, idPrefix);
        defaultPageSize = conf.getInt(
            CONF_SUMMON_DEFAULTPAGESIZE, defaultPageSize);
        defaultFacetPageSize = conf.getInt(
            CONF_SUMMON_FACETS_DEFAULTPAGESIZE, defaultFacetPageSize);
        defaultFacets = conf.getString(CONF_SUMMON_FACETS, defaultFacets);
        combineMode = conf.getString(
            CONF_SUMMON_FACETS_COMBINEMODE, combineMode);
        defaultResolveLinks = conf.getBoolean(
            CONF_SUMMON_RESOLVE_LINKS, defaultResolveLinks);
        responseBuilder = new SummonResponseBuilder(conf);
        summonDefaultParams = new HashMap<String, List<String>>();
        for (Map.Entry<String, Serializable> entry : conf) {
            convertSummonParam(summonDefaultParams, entry);
        }
        readyWithoutOpen();
        log.info("Serial Solutions Summon search node ready for host " + host);
    }

    /**
     * Extracts parameters with key prefix "summonparam." and stores the
     * truncated keys with their value(s) as a list of Strings.
     * </p><p>
     * If the key is not prefixed by "summonparam.", it is ignored.
     * </p>
     * @param summonParam the map where the key/value is stored.
     * @param entry the source for the key/value pair.
     */
    private void convertSummonParam(Map<String, List<String>> summonParam,
                                    Map.Entry<String, Serializable> entry) {
        if (!entry.getKey().startsWith(CONF_SUMMON_PARAM_PREFIX)) {
            log.trace("convertSummonParam got unsupported key "
                      + entry.getKey() + ". Ignoring entry");
            return;
        }
        String key = entry.getKey().substring(
                CONF_SUMMON_PARAM_PREFIX.length(), entry.getKey().length());
        if (entry.getValue() instanceof String) {
            summonParam.put(key, Arrays.asList((String)entry.getValue()));
            if (log.isTraceEnabled()) {
                log.trace(
                    "convertSummonParam assigning " + key + ":"
                    + entry.getValue());
            }
        } else if (entry.getValue() instanceof List) {
            ArrayList<String> values = new ArrayList<String>();
            for (Object v: (List)entry.getValue()) {
                if (v instanceof String) {
                    values.add((String)v);
                } else {
                    log.warn(
                        "Expected List entry of type String in Summon "
                        + "parameter " + key + ", but got Object of class "
                        + v.getClass());
                }
            }
            if (values.size() == 0) {
                log.warn(
                    "Got empty list for Summon param " + key+ ". Ignoring");
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("convertSummonParam assigning list " + key + ":"
                              + Strings.join(values, ", "));
                }
                summonParam.put(key, values);
            }
        } else if (entry.getValue() instanceof String[]) {
            if (log.isTraceEnabled()) {
                log.trace("convertSummonParam assigning array " + key + ":"
                          + Strings.join((String[])entry.getValue(), ", "));
            }
            summonParam.put(key, Arrays.asList((String[])entry.getValue()));
        } else {
            log.warn("convertSummonParam expected String, List<String> og "
                     + "String[] for key " + key + " but got value of class "
                     + entry.getValue().getClass());
        }
    }

    @Override
    protected void managedSearch(
        Request request, ResponseCollection responses) throws RemoteException {
        try {
            barrierSearch(request, responses);
        } catch (StackOverflowError e) {
            String message = String.format(
                "Caught StackOverflow at outer level during handling of"
                + "Summon request %s:\n%s",
                request.toString(true), reduceStackTrace(request, e));
            log.error(message, e);
            throw new RemoteException(
                "SummonSearchNode.managedSearch: " + message);
        }
    }

    private void barrierSearch(
        Request request, ResponseCollection responses) throws RemoteException {
        long startTime = System.currentTimeMillis();
        if (request.containsKey(LuceneKeys.SEARCH_MORELIKETHIS_RECORDID)) {
            log.trace("MoreLikeThis search is not supported by Summon, "
                      + "returning immediately");
            return;
        }

        String rawQuery = getEmptyIsNull(request, DocumentKeys.SEARCH_QUERY);
        String filter =  getEmptyIsNull(request, DocumentKeys.SEARCH_FILTER);
        String sortKey = getEmptyIsNull(request, DocumentKeys.SEARCH_SORTKEY);
        if (DocumentKeys.SORT_ON_SCORE.equals(sortKey)) {
            sortKey = null; // null equals relevance ranking
        }
        boolean reverseSort = request.getBoolean(
            DocumentKeys.SEARCH_REVERSE, false);
        int startIndex = request.getInt(DocumentKeys.SEARCH_START_INDEX, 0) + 1;
        int maxRecords = request.getInt(
            DocumentKeys.SEARCH_MAX_RECORDS, defaultFacetPageSize);
        boolean resolveLinks = request.getBoolean(
            SEARCH_SUMMON_RESOLVE_LINKS, defaultResolveLinks);
        boolean collectdocIDs = request.getBoolean(
            DocumentKeys.SEARCH_COLLECT_DOCIDS, false);

        if (rawQuery == null && filter == null) {
            log.debug("No filter or query, proceeding anyway as other params "
                      + "might be specified");
        }

        // s.fq

        String query;
        query = "*".equals(rawQuery) ? "*:*" : rawQuery;

/*        if (filter == null) {
            query = rawQuery;
        } else if (rawQuery == null) {
            query = filter;
        } else {
            if ("*".equals(rawQuery)) {
                query = filter;
            } else {
                query = "(" + filter + ") AND (" + rawQuery + ")";
            }
        }*/

        String facetsDef =  request.getString(
            FacetKeys.SEARCH_FACET_FACETS, defaultFacets);
        if ("".equals(facetsDef)) {
            facetsDef = null;
        }
        SummonFacetRequest facets = null == facetsDef || "".equals(facetsDef) ?
                                    null : new SummonFacetRequest(
            facetsDef, defaultFacetPageSize, combineMode);

        Map<String, List<String>> summonSearchParams =
            new HashMap<String, List<String>>(summonDefaultParams);
        for (Map.Entry<String, Serializable> entry : request.entrySet()) {
            convertSummonParam(summonSearchParams, entry);
        }

        if (query != null) {
            query = convertQuery(query, summonSearchParams);
        }
        if ("".equals(query)) {
            query = null;
        }
        if (filter != null) {
            filter = convertQuery(filter, summonSearchParams);
        }
        if ("".equals(filter)) {
            filter = null;
        }

        long searchTime = -System.currentTimeMillis();
        log.trace("Performing search for '" + query + "' with facets '"
                  + facets + "'");
        String summonResponse;
        String summonTiming;
        try {
            Pair<String, String> sums = summonSearch(
                filter, query, summonSearchParams,
                collectdocIDs ? facets : null,
                startIndex, maxRecords, resolveLinks, sortKey, reverseSort);
            summonResponse = sums.getKey();
            summonTiming = sums.getValue();
        } catch (StackOverflowError e) {
            String message = String.format(
                "Caught StackOverflow while performing summon request %s:\n%s",
                request.toString(true), reduceStackTrace(request, e));
            log.error(message, e);
            throw new RemoteException(
                "SummonSearchNode.barrierSearch: " + message);

        }
        if (summonResponse == null || "".equals(summonResponse)) {
            throw new RemoteException(
                "Summon search for '" + query + " yielded empty result");
        }
        searchTime += System.currentTimeMillis();

        long buildResponseTime = -System.currentTimeMillis();
        long hitCount;
        try {
            hitCount = responseBuilder.buildResponses(
                request, facets, responses, summonResponse, summonTiming);
        } catch (XMLStreamException e) {
            String message = "Unable to transform Summon XML response to Summa "
                             + "response for '" + request + "'";
            if (log.isDebugEnabled()) {
                log.debug(message + ". Full XML follows:\n" + summonResponse);
            }
            throw new RemoteException(message, e);
        } catch (StackOverflowError e) {
            String message = String.format(
                "Caught StackOverflow while building response for summon "
                + "request %s\nReduced stack trace:\n%s\nReduced raw summon "
                + "response:\n%s",
                request.toString(true), reduceStackTrace(request, e),
                summonResponse.length() > 2000 ?
                summonResponse.substring(0, 2000) : summonResponse);
            log.error(message, e);
            throw new RemoteException(
                "SummonSearchNode.barrierSearch: " + message);

        }
        buildResponseTime += System.currentTimeMillis();

        log.debug("fullSearch(..., " + filter + ", " + rawQuery + ", "
                  + startIndex + ", " + maxRecords + ", " + sortKey + ", "
                  + reverseSort + ") with " + hitCount + " hits finished in "
                  + searchTime + " ms (" + searchTime + " ms for remote search "
                  + "call, " + buildResponseTime + " ms for converting to "
                  + "Summa response)");
        responses.addTiming("summon.search.buildresponses", buildResponseTime);
        responses.addTiming("summon.search.total",
                            (System.currentTimeMillis() - startTime));
    }

    /**
     * It seems that the Summon query parser does not support ranges. Instead
     * it expects ranges to be stated in 's.rf' as 'field,minvalue:maxvalue'.
     * This method parses the query, extracts & removes the range query parts
     * and adds them to the Summon search parameters.
     * </p><p>
     * Similarly, term queries with the field 'ID' are special as the text-
     * section must be modified by stripping leading {@link #idPrefix}.
     * @param query as entered by the user.
     * @param summonSearchParams range-queries are added to this.
     * @return the query minus range queries.
     */
    // TODO: Fix ID
    public String convertQuery(
        final String query,
        final Map<String, List<String>> summonSearchParams) {
        final String RF = "s.rf";
        try {
            return new QueryRewriter(new QueryRewriter.Event() {
                @Override
                public Query onQuery(TermRangeQuery query) {
                    List<String> sq = summonSearchParams.get(RF);
                    if (sq == null) {
                        sq = new ArrayList<String>();
                        summonSearchParams.put(RF, sq);
                    }
                    sq.add(query.getField() + ","
                           + query.getLowerTerm().utf8ToString() + ":"
                           + query.getUpperTerm().utf8ToString());
                    return null;
                }

                @Override
                public Query onQuery(TermQuery query) {
                    // ID is first class so no configuration here
                    if ("ID".equals(query.getTerm().field())
                        || IndexUtils.RECORD_FIELD.equals(
                        query.getTerm().field())) {
                        String text = query.getTerm().text();
                        if (idPrefix != null && text != null
                            && text.startsWith(idPrefix)) {
                            text = text.substring(idPrefix.length());
                        }
                        TermQuery tq = new TermQuery(new Term("ID", text));
                        tq.setBoost(query.getBoost());
                        return tq;
                    }
                    return query;
                }
            }).rewrite(query);
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                "Error parsing '" + query + "'", e);
        }
    }

    private String getEmptyIsNull(Request request, String key) {
        String response = request.getString(key, null);
        return "".equals(response) ? null : response;
    }

    private boolean warmupCalled = false;
    @Override
    protected void managedWarmup(String request) {
        if (!warmupCalled) {
            log.debug("No warmup for '" + request
                      + "' as warmup is handled externally. Further requests "
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
        log.debug("managedClose() called. No effect as this search node is "
                  + "stateless");
    }

    /* Copied and slightly modified from Yellowfoot (internal project at
       Statsbiblioteket)
     */

    /**
     * Perform a search in Summon.
     *
     * @param filter    a Solr-style filter (same syntax as query).
     * @param query     a Solr-style query.
     * @param summonParams optional extended params for Summon. If not null,
     *                  these will be added to the Summon request.
     * @param facets    which facets to request or null if no facets are wanted.
     * @param startIndex the index for the first Record to return, counting
     *                   from 0. This is translated to startpage for Solr.
     *
     * @param maxRecords number of items per page.
     * @param resolveLinks whether or not to call the link resolver to resolve
     *                  openurls to actual links.
     * @param sortKey the field to sort on. If null, default ranking sort is
     *                used.
     * @param reverseSort if true, sort order is reversed.
     * @return XML with the search result as per Summon API followed by timing
     *         information.
     * @throws java.rmi.RemoteException if there were an error performing the
     * remote search call.
     */
    private Pair<String, String> summonSearch(
        String filter, String query, Map<String, List<String>> summonParams,
        SummonFacetRequest facets, int startIndex,
        int maxRecords, boolean resolveLinks, String sortKey,
        boolean reverseSort) throws RemoteException {
        long buildQuery = -System.currentTimeMillis();
        int startpage = maxRecords == 0 ? 0 : (startIndex / maxRecords) + 1;
        @SuppressWarnings({"UnnecessaryLocalVariable"})
        int perpage = maxRecords;
        log.trace("Calling simpleSearch(" + query + ", " + facets + ", "
                  + startIndex + ", " + maxRecords + ")");
        Map<String, List<String>> querymap = buildSummonQuery(
               filter, query, facets, startpage, perpage, sortKey, reverseSort);
        if (summonParams != null) {
            querymap.putAll(summonParams);
        }

        Date date = new Date();
        String idstring = computeIdString(
            "application/xml", summonDateFormat.format(date), host, restCall,
            querymap);
        String queryString = computeSortedQueryString(querymap, true);
        buildQuery += System.currentTimeMillis();
        log.trace("Parameter preparation done in " + buildQuery + "ms");
        String result;
        long rawCall = -1;
        try {
            rawCall = -System.currentTimeMillis();
            result = getData("http://" + host, restCall + "?" +
                    queryString, date, idstring, null);
            rawCall += System.currentTimeMillis();
            summonlog.debug(
                "Call to Summon done in " + rawCall + "ms: " + queryString);
        } catch (Exception e) {
            throw new RemoteException(
                "Unable to perform remote call to "  + host + restCall
                + " with argument '" + queryString, e);
        }
        long prefixIDs = -System.currentTimeMillis();
        String retval = prefixIDs(result, idPrefix);
        prefixIDs += System.currentTimeMillis();
        long linkResolve = -System.currentTimeMillis();
        if (resolveLinks) {
            retval = linkResolve(retval);
        }
        linkResolve += System.currentTimeMillis();
        log.trace("simpleSearch done in "
                  + (System.currentTimeMillis() - buildQuery) + "ms");
        return new Pair<String, String>(
            retval, "summon.buildquery:" + buildQuery + "|summon.rawcall:"
                    + rawCall + "|summon.prefixIDs:" + prefixIDs
                    + "|summon.linkresolve:" + linkResolve);
    }

    private Map<String, List<String>> buildSummonQuery(
        String filter, String query, SummonFacetRequest facets,
        int startpage, int perpage, String sortKey, boolean reverseSort) {
        Map<String, List<String>> querymap = new HashMap<String, List<String>>();

        querymap.put("s.dym", Arrays.asList("true"));
        querymap.put("s.ho",  Arrays.asList("true"));
        if (query != null) { // We allow missing query
            querymap.put("s.q",   Arrays.asList(query));
        }
        // Note: summon supports pure negative filters so we do not use
        // DocumentKeys.SEARCH_FILTER_PURENEGATIVE for anything
        if (filter != null) { // We allow missing filter
            querymap.put("s.fq",   Arrays.asList(filter));
        }
        querymap.put("s.ps",  Arrays.asList(Integer.toString(perpage)));
        querymap.put("s.pn",  Arrays.asList(Integer.toString(startpage)));

        // TODO: Add support for sorting on multiple fields
        if (sortKey != null) {
            querymap.put("s.sort", Arrays.asList(
                         sortKey + ":" + (reverseSort ? "desc" : "asc")));
        }

        if (facets != null) {
            querymap.put("s.ff", facets.getFacetQueries());
        } else {
            log.debug("No facets specified, skipping faceting");
        }

        List<String> rangefacet = new ArrayList<String>();
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
        querymap.put("s.rff", rangefacet);

        return querymap;
    }

    /**
     * Add to an existing Summon search using commands
     * @param queryString The query string identifiying an existing Summon search
     * @param resolveLinks whether or not to call the link resolver to resolve openurls to actual links
     * @param commands The commands to apply to the search
     * @return A String containing XML describing the search result
     */
    public String continueSearch(String queryString, boolean resolveLinks, String... commands) {
        long methodStart = System.currentTimeMillis();
        log.trace("Calling continueSearch(" + queryString + ", " + commands + ")");
        String result = "";

        String newQueryString = queryString;
        for (String s : commands) {
            try {
                newQueryString += "&s.cmd=" + URLEncoder.encode(s, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                newQueryString += "&s.cmd=" + s;
            }
        }
        // TODO: use http://hc.apache.org/ instead?
        Map<String, List<String>> tempParams = new HashMap<String, List<String>>();
        try {
            for (String param : newQueryString.split("&")) {
                String[] pair = param.split("=");
                String key = null;
                key = URLDecoder.decode(pair[0], "UTF-8");
                String value = URLDecoder.decode(pair[1], "UTF-8");
                List<String> values = tempParams.get(key);
                if (values == null) {
                    values = new ArrayList<String>();
                    tempParams.put(key, values);
                }
                values.add(value);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        Date date = new Date();
        DateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);

        String idstring = computeIdString("application/xml", formatter.format(date),
                "api.summon.serialssolutions.com", "/search", tempParams);
        String queries = computeSortedQueryString(tempParams, true);
        log.trace("Parameter preparation to Summon done in " + (System.currentTimeMillis() - methodStart) + "ms");
        try {
            long serviceStart = System.currentTimeMillis();
            result = getData("http://api.summon.serialssolutions.com", "/search?" +
                    queries, date, idstring, null);
            log.trace("Call to Summon done in " + (System.currentTimeMillis() - serviceStart) + "ms");
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        String retval = prefixIDs(result, idPrefix);
        if (resolveLinks) {
            retval = linkResolve(retval);
        }
        log.trace("continueSearch done in " + (System.currentTimeMillis() - methodStart) + "ms");
        return retval;
    }

    private static String computeIdString(String acceptType, String  date, String  host, String  path, Map<String, List<String>> queryParameters) {
        return appendStrings(acceptType, date, host, path, computeSortedQueryString(queryParameters, false));
    }

    /**
     * Sort and optionally urlencodes a query string
     * @param queryParameters A Map<String, List<String>> containing the query parameters
     * @param urlencode Whether or not to urlencode the query parameters
     * @return A sorted and urlencoded query string
     */
    private static String computeSortedQueryString(Map<String, List<String>> queryParameters, boolean urlencode) {
        List<String> parameterStrings = new ArrayList<String>();

        // for each parameter, get its key and values
        for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {

            // for each value, create a string in the format key=value
            for (String value : entry.getValue()) {
                if (urlencode) {
                    try {
                        parameterStrings.add(entry.getKey() + "=" + URLEncoder.encode(value,"UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        parameterStrings.add(entry.getKey() + "=" + value);
                    }
                } else {
                    parameterStrings.add(entry.getKey() + "=" + value);
                }
            }
        }

        // sort the individual parameters
        Collections.sort(parameterStrings);
        StringBuilder queryString = new StringBuilder();

        // append strings together with the '&' character as a delimiter
        for (String parameterString : parameterStrings) {
            queryString.append(parameterString).append("&");
        }

        // remove any final trailing '&'
        if (queryString.length() > 0) {
            queryString.setLength(queryString.length() - 1);
        }
        return queryString.toString();
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
     * @throws java.security.SignatureException in case of Signature problems.
     */
    private static String buildDigest(String key, String idString) throws SignatureException {
        try {
            String algorithm = "HmacSHA1";
            Charset charset = Charset.forName("utf-8");
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), algorithm);
            Mac mac = Mac.getInstance(algorithm);
            mac.init(signingKey);
            return new String(Base64.encodeBase64(mac.doFinal(idString.getBytes(charset))), charset);
        } catch (Exception e) {
            throw new SignatureException("Failed to generate HMAC : " + e.getMessage());
        }
    }


    static {
        System.setProperty("java.protocol.handler.pkgs",
                           "com.sun.net.ssl.internal.www.protocol");
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
    }
    DateFormat summonDateFormat =
        new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);
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
    private String getData(
        String target, String content, Date date, String idstring,
        String sessionId) throws Exception {
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
        conn.setRequestProperty(
            "Authorization",
            "Summon " + accessID + ";" + buildDigest(accessKey, idstring));
        if (sessionId != null && !sessionId.equals("")) {
            conn.setRequestProperty("x-summon-session-id", sessionId);
        }
        conn.setConnectTimeout(1000);
        conn.connect();

        Long readStart = System.currentTimeMillis();
        BufferedReader in;
        try {
            in = new BufferedReader(new InputStreamReader(
                conn.getInputStream(), "UTF-8"));
            String str;
            while ((str = in.readLine()) != null) {
                retval.append(str);
            }
            log.trace("Reading from Summon done in "
                      + (System.currentTimeMillis() - readStart) + "ms");
            in.close();
        } catch (IOException e) {
            log.warn(String.format(
                "getData(target=%s, content=%s, date=%s, idstring=%s, "
                + "sessionID=%s failed with error stream\n%s",
                target, content, date, idstring, sessionId,
                Strings.flush(
                    new InputStreamReader(conn.getErrorStream(), "UTF-8"))), e);
        }

        return retval.toString();
    }

    /**
     * Given an id returns the corresponding Summon record in a format similar to Summa's shortformat
     * @param id Summon id
     * @return A String containing XML in shortformat
     * @throws java.rmi.RemoteException if Summon could not be contacted-
     */
    public String getShortRecord(String id) throws RemoteException {
        // TODO: include id in shortrecord tag to help out sendListEmail.jsp and ExportToExternalFormat.java
        StringBuilder retval = new StringBuilder();

        if (id.startsWith(idPrefix)) {
            id = id.substring(idPrefix.length());
        }

        String temp = summonSearch(
            null, "ID:" + id, null, null, 1, 1, false, null, false).getKey();
        Document dom = DOM.stringToDOM(temp);


        retval.append("<field name=\"shortformat\">\n");
        retval.append("  <shortrecord>\n");
        retval.append("    <rdf:RDF xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n");
        retval.append("      <rdf:Description>\n");
        retval.append("        <dc:title>").
                append(XMLUtil.encode(DOM.selectString(dom, "//documents/document/field[@name=\"Title\"]/value/text()", ""))).
                append(" : ").
                append(XMLUtil.encode(DOM.selectString(dom, "//documents/document/field[@name=\"Subtitle\"]/value/text()", ""))).
                append("</dc:title>\n");
        retval.append("        <dc:creator>").
                append(XMLUtil.encode(DOM.selectString(dom, "//documents/document/field[@name=\"Author\"]/value/text()", ""))).
                append("</dc:creator>\n");
        retval.append("        <dc:type xml:lang=\"da\">").
                append(XMLUtil.encode(DOM.selectString(dom, "//documents/document/field[@name=\"ContentType\"]/value/text()", ""))).
                append("</dc:type>\n");
        retval.append("        <dc:type xml:lang=\"en\">").
                append(XMLUtil.encode(DOM.selectString(dom, "//documents/document/field[@name=\"ContentType\"]/value/text()", ""))).
                append("</dc:type>\n");
        retval.append("        <dc:date>").
                append(XMLUtil.encode(DOM.selectString(dom, "//documents/document/field[@name=\"PublicationDate_xml\"]/datetime/@year", ""))).
                append("</dc:date>\n");
        retval.append("        <dc:format></dc:format>\n");
        retval.append("      </rdf:Description>\n");
        retval.append("    </rdf:RDF>\n");
        retval.append("  </shortrecord>\n");
        retval.append("</field>\n");


        return retval.toString();
    }


    /**
     * Gets a record from Summon
     * @param id Summon id
     * @return A String containing a Summon record in XML
     * @throws java.rmi.RemoteException
     */
    public String getRecord(String id) throws RemoteException {
        return getRecord(id, false);
    }

    /**
     * Gets a record from Summon while optionally resolving links
     * @param id Summon id
     * @param resolveLinks Whether or not to resolve links through the link resolver
     * @return A String containing a Summon record in XML
     * @throws java.rmi.RemoteException
     */
    public String getRecord(String id, boolean resolveLinks)
                                                        throws RemoteException {
        String retval = "";

        if (id.startsWith(idPrefix)) {
            id = id.substring(idPrefix.length());
        }

        String temp = summonSearch(
            null, "ID:" + id, null, null, 1, 1, false, null, false).getKey();
        if (resolveLinks) {
            temp = linkResolve(temp);
        }

        if (!temp.trim().equals("")){
        Document dom = DOM.stringToDOM(temp);
        Node firstDoc = DOM.selectNode(dom, "//documents/document");

        try {
            retval = DOM.domToString(firstDoc, true);
        } catch (TransformerException e) {
            log.error("Unable to convert dom node to string", e);
        }

        }

        return retval;
    }

    /**
     * Takes the xml result from a Summon query and prefixes all IDs with prefix. It looks for the value-tag in <field name="ID">
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

    /**
     * Adds links elements to a Summon search result based on the contained openurls.
     * @param searchResult A String containing XML with the result from a Summon query
     * @return The searchResult where the documents have been enriched with links
     */
    private static String linkResolve(String searchResult) {
        throw new UnsupportedOperationException(
            "Link resolving is not yet a part of this implementation");
        /*
        long methodStart = System.currentTimeMillis();
        log.trace("Calling linkResolve");

        String retval = searchResult;

        List<String> openurls = new ArrayList<String>();


        Document dom = DOM.stringToDOM(searchResult);

        NodeList documents = DOM.selectNodeList(dom, "//document");


        if (documents.getLength() > 0) {
            for (int i = 0; i < documents.getLength(); i++) {
                Node n = documents.item(i);
                String openurl = DOM.selectString(n, "@openUrl");
                openurls.add(openurl);
            }

            String arg = Strings.join(openurls, ",");

            long serviceStart = System.currentTimeMillis();
            // TODO: Consider how to handle this
            WebServices services = WebServices.getInstance();
            String links = (String) services.execute("getfrom360linkmulti", arg);
            long serviceTime = System.currentTimeMillis() - serviceStart;
            log.trace("Called getfrom360linkmulti in : " + serviceTime + "ms");

            Document linksDom = DOM.stringToDOM(links);
            NodeList linksNL = DOM.selectNodeList(linksDom, "//links");
            Node imp;
            for (int i = 0; i < linksNL.getLength(); i++) {
                Node linksNode = linksNL.item(i);
                String ou = DOM.selectString(linksNode, "@openurl");
                Node importTo = DOM.selectNode(dom, "//document[@openUrl=\"" + ou + "\"]");
                if (importTo != null) {
                    imp = dom.importNode(linksNode, true);
                    importTo.appendChild(imp);
                } else {
                    log.warn("Unable to find document with openurl: " + ou);
                }
            }


            try {
                retval = DOM.domToString(dom);
            } catch (TransformerException e) {
                log.error("Failed to transform searchresult", e);
                retval = "";
            }
            log.trace("Time spent on DOM manipulation: " + (System.currentTimeMillis() - methodStart - serviceTime) + "ms");
            log.trace("Called linkResolve in : " + (System.currentTimeMillis() - methodStart) + "ms");
            return retval;

        }  else {
            return retval;
        }
        */
    }

/*    public static void main(String[] args) {

        String temp;


        temp = simpleSearch("foo", "1", "3", false);
        System.out.println(temp);

        temp = simpleSearch("CatchAll:foo", "1", "3", false);
        System.out.println(temp);
    }
  */


}
