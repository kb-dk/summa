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
import dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys;
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.transform.TransformerException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
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
    // TODO: Support for filter and sort (+ reverse)
    // TODO: Check if startpos is 0 or 1, adjuct accordingly
    // TODO: Implement configurable rangefacets
    // TODO; Fix defaultPageSize
    // TODO Implement getShortRecord
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummonSearchNode extends SearchNodeImpl {
    private static Log log = LogFactory.getLog(SummonSearchNode.class);

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
    }

    @Override
    protected void managedSearch(
        Request request, ResponseCollection responses) throws RemoteException {
        String facets =  request.getString(
            FacetKeys.SEARCH_FACET_FACETS, defaultFacets);
        if ("".equals(facets)) {
            facets = null;
        }
        String query =   request.getString(DocumentKeys.SEARCH_QUERY, null);
        String filter =  request.getString(DocumentKeys.SEARCH_FILTER, null);
        String sortKey = request.getString(DocumentKeys.SEARCH_SORTKEY, null);
        boolean reverseSort = request.getBoolean(
            DocumentKeys.SEARCH_REVERSE, false);
        int startIndex = request.getInt(DocumentKeys.SEARCH_START_INDEX, 0) + 1;
        int maxRecords = request.getInt(
            DocumentKeys.SEARCH_MAX_RECORDS, defaultFacetPageSize);
        boolean resolveLinks = request.getBoolean(
            SEARCH_SUMMON_RESOLVE_LINKS, defaultResolveLinks);

        if (query == null || "".equals(query)) {
            log.debug("No query, returning immediately");
            return;
        }
        if (filter != null) {
            log.warn("fullSearch: Filter '" + filter + "' is ignored");
        }
        if (!DocumentKeys.SORT_ON_SCORE.equals(sortKey)) {
            log.warn("fullSearch: Sort key '" + sortKey + "' is ignored");
        }

        long searchTime = -System.currentTimeMillis();
        log.trace("Performing search for '" + query + "' with facets '"
                  + facets + "'");
        String sResponse = summonSearch(
            query, facets, startIndex, maxRecords, resolveLinks);
        searchTime += System.currentTimeMillis();

        long buildResponseTime = -System.currentTimeMillis();
        long hitCount = responseBuilder.buildResponses(
            request, responses, sResponse);
        buildResponseTime += System.currentTimeMillis();

        log.debug("fullSearch(..., " + filter + ", " + query + ", " + startIndex
                  + ", " + maxRecords + ", " + sortKey + ", " + reverseSort
                  + ") with " + hitCount + " hits finished in "
                  + searchTime + " ms (" + searchTime + " ms for remote search "
                  + "call, " + buildResponseTime + " ms for converting to "
                  + "Summa response)");
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
     * @param query     a Solr-style query.
     * @param facets    a comma-separated list of facets, optionally with
     *                  pageSize in paranthesis after fact name.
     * @param startpage the page to start on, starting at 1.
     * @param perpage   number of items per page.
     * @param resolveLinks whether or not to call the link resolver to resolve
     *                  openurls to actual links.
     * @return XML with the search result as per Summon API.
     * @throws java.rmi.RemoteException if there were an error performing the
     * remote search call.
     */
    public String summonSearch(
        String query, String facets, int startpage,
        int perpage, boolean resolveLinks) throws RemoteException {
        long methodStart = System.currentTimeMillis();
        log.trace("Calling simpleSearch(" + query + ", " + facets + ", "
                  + startpage + ", " + perpage + ")");
        Map<String, List<String>> querymap =
            buildSummonQuery(query, facets, startpage, perpage);

        Date date = new Date();
        String idstring = computeIdString(
            "application/xml", summonDateFormat.format(date), host, restCall,
            querymap);
        String queryString = computeSortedQueryString(querymap, true);
        log.trace("Parameter preparation to Summon done in "
                  + (System.currentTimeMillis() - methodStart) + "ms");
        String result = "";
        try {
            long serviceStart = System.currentTimeMillis();
            result = getData("http://" + host, restCall + "?" +
                    queryString, date, idstring, null);
            log.trace("Call to Summon done in "
                      + (System.currentTimeMillis() - serviceStart) + "ms");
        } catch (Exception e) {
            throw new RemoteException(
                "Unable to perform remote call to "  + host + restCall
                + " with argument '" + queryString, e);
        }

        String retval = prefixIDs(result, idPrefix);
        if (resolveLinks) {
            retval = linkResolve(retval);
        }
        log.trace("simpleSearch done in "
                  + (System.currentTimeMillis() - methodStart) + "ms");
        return retval;
    }

    private Map<String, List<String>> buildSummonQuery(
        String query, String facets, int startpage, int perpage) {
        Map<String, List<String>> querymap = new HashMap<String, List<String>>();

        querymap.put("s.dym", Arrays.asList("true"));
        querymap.put("s.ho",  Arrays.asList("true"));
        querymap.put("s.q",   Arrays.asList(query));
        querymap.put("s.ps",  Arrays.asList(Integer.toString(perpage)));
        querymap.put("s.pn",  Arrays.asList(Integer.toString(startpage)));

        if (facets != null) {
            List<String> facetQueries = new ArrayList<String>();
            String[] facetTokens = facets.split(" *, *");
            for (String facetToken: facetTokens) {
                // zoo(12 ALPHA)
                String[] subTokens = facetToken.split(" *\\(", 2);
                String facetName = subTokens[0];
                int pageSize = defaultFacetPageSize;
                if (subTokens.length > 1) {
                    // "  5  ALPHA)  " | "5)" | " ALPHA) | "vgfsd"
                    String noParen = subTokens[1].split("\\)", 2)[0].trim();
                    // "5  ALPHA" | "5" | "ALPHA" | "vgfsd"
                    String[] facetArgs = noParen.split(" +", 2);
                    // "5", "ALPHA" | "5" | "ALPHA" | "vgfsd"
                    if (facetArgs.length > 0) {
                        pageSize = Integer.parseInt(facetArgs[0]);
                    }
                    if (facetArgs.length > 1
                        && !"POPULARITY".equals(facetArgs[1])) {
                        log.warn("The facet request '" + facetToken
                                 + "' specifies sort order '" + facetArgs[1]
                                 + " which is not supported by this node. "
                                 + "Defaulting to populatiry sort");
                    }
                }
//                facetstring.add("Author,and,1,15");
                facetQueries.add(
                    facetName + "," + combineMode + ",1," + pageSize);
            }
            querymap.put("s.ff", facetQueries);
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
     * Calculate a base64 sha-1 digest for the input
     * @param key The key to use while calculating the digest
     * @param idString The String to digest
     * @return A String containing a base64 encoded sha-1 digest
     * @throws java.security.SignatureException
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
    /**
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
     */
    public String getShortRecord(String id) throws RemoteException {
        // TODO: include id in shortrecord tag to help out sendListEmail.jsp and ExportToExternalFormat.java
        StringBuilder retval = new StringBuilder();

        if (id.startsWith(idPrefix)) {
            id = id.substring(idPrefix.length());
        }

        String temp = summonSearch("ID:" + id, null, 1, 1, false);
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
     */
    public String getRecord(String id) throws RemoteException {
        return getRecord(id, false);
    }

    /**
     * Gets a record from Summon while optionally resolving links
     * @param id Summon id
     * @param resolveLinks Whether or not to resolve links through the link resolver
     * @return A String containing a Summon record in XML
     */
    public String getRecord(String id, boolean resolveLinks)
                                                        throws RemoteException {
        String retval = "";

        if (id.startsWith(idPrefix)) {
            id = id.substring(idPrefix.length());
        }

        String temp = summonSearch("ID:" + id, null, 1, 1, false);
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

        StringBuffer retval = new StringBuffer();

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
