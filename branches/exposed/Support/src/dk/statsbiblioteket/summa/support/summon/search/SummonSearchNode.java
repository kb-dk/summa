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
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.support.harmonise.QueryRewriter;
import dk.statsbiblioteket.summa.support.solr.SolrSearchNode;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
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
 */
// Map contenttype, language. Convert date (til år lige nu), potentially library
    // TODO: Check if startpos is 0 or 1, adjuct accordingly
    // TODO: Implement configurable rangefacets
    // TODO Implement getShortRecord
    // TODO: Implement getRecord in Storage
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te, mv")
public class SummonSearchNode extends SolrSearchNode {
    private static Log log = LogFactory.getLog(SummonSearchNode.class);

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
        + "Genre, " // Direct
        + "IsScholarly, " // Direct
        + "Language, " // Map
        + "IsFullText, "
        //"Library,and,1,5",
        //"PackageID,and,1,5",
        //"SourceID,and,1,5",
        + "SubjectTerms, " // SB:subject
        + "TemporalSubjectTerms"; // Direct

    private final String accessID;
    private final String accessKey;
    private final Configuration conf; // Used when constructing QueryRewriter

    public SummonSearchNode(Configuration conf) throws RemoteException {
        super(legacyConvert(conf));
        setID("summon");
        this.conf = conf;
        accessID =   conf.getString(CONF_SUMMON_ACCESSID);
        accessKey =  conf.getString(CONF_SUMMON_ACCESSKEY);
        for (Map.Entry<String, Serializable> entry : conf) {
            convertSolrParam(solrDefaultParams, entry);
        }
        readyWithoutOpen();
        log.info("Serial Solutions Summon search node ready for host " + host);
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
        return conf;
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
        final String RF = "s.rf"; // RangeField
        try {
            return new QueryRewriter(conf, null, new QueryRewriter.Event() {
                @Override
                public Query onQuery(TermRangeQuery query) {
                    List<String> sq = summonSearchParams.get(RF);
                    if (sq == null) {
                        sq = new ArrayList<String>();
                        summonSearchParams.put(RF, sq);
                    }
                    sq.add(query.getField() + "," + query.getLowerTerm().utf8ToString() + ":"
                           + query.getUpperTerm().utf8ToString());
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
                    }
                    return query;
                }
            }).rewrite(query);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Error parsing '" + query + "'", e);
        }
    }

    /**
     * Perform a search in Summon.
     *
     * @param request    the basic request.
     * @param filter     a Solr-style filter (same syntax as query).
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
        Request request, String filter, String query, Map<String, List<String>> solrParams, SolrFacetRequest facets,
        int startIndex, int maxRecords, String sortKey, boolean reverseSort,
        ResponseCollection responses) throws RemoteException {
        long buildQuery = -System.currentTimeMillis();
        int startpage = maxRecords == 0 ? 0 : ((startIndex-1) / maxRecords) + 1;
        @SuppressWarnings({"UnnecessaryLocalVariable"})
        int perpage = maxRecords;
        log.trace("Calling simpleSearch(" + query + ", " + facets + ", " + startIndex + ", " + maxRecords + ")");
        Map<String, List<String>> querymap;
        try {
            querymap = buildSummonQuery(
                request, filter, query, facets, startpage, perpage, sortKey, reverseSort);
        } catch (ParseException e) {
            throw new RemoteException("Unable to build Solr query", e);
        }
        if (solrParams != null) {
            querymap.putAll(solrParams);
        }

        Date date = new Date();
        String idstring = computeIdString("application/xml", summonDateFormat.format(date), host, restCall, querymap);
        String queryString = computeSortedQueryString(querymap, true);
        buildQuery += System.currentTimeMillis();
        log.trace("Parameter preparation done in " + buildQuery + "ms");
        String result;

        try {
            result = getData("http://" + host, restCall + "?" + queryString, date, idstring, null,responses);
        } catch (Exception e) {
            throw new RemoteException(
                "Unable to perform remote call to "  + host + restCall + " with argument '" + queryString, e);
        }
        long prefixIDs = -System.currentTimeMillis();
        String retval = prefixIDs(result, idPrefix);
        prefixIDs += System.currentTimeMillis();
        log.trace("simpleSearch done in " + (System.currentTimeMillis() - buildQuery) + "ms");
        return new Pair<String, String>(retval, "summon.buildquery:" + buildQuery +  "|summon.prefixIDs:" + prefixIDs);
    }

    private Map<String, List<String>> buildSummonQuery(
        Request request, String filter, String query, SolrFacetRequest facets,
        int startpage, int perpage, String sortKey, boolean reverseSort) throws ParseException {
        Map<String, List<String>> querymap = new HashMap<String, List<String>>();

        querymap.put("s.dym", Arrays.asList("true"));
        querymap.put("s.ho",  Arrays.asList("true"));
        // Note: summon supports pure negative filters so we do not use
        // DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE for anything
        if (filter != null) { // We allow missing filter
            if (request.getBoolean(SEARCH_SOLR_FILTER_IS_FACET, false)) {
                convertFilterToFacet(filter, querymap);
            } else if (supportsPureNegative || !request.getBoolean(DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE, false)) {
                querymap.put("s.fq",   Arrays.asList(filter)); // FilterQuery
            } else {
                if (query == null) {
                    throw new IllegalArgumentException(
                        "No query and filter marked with '" + DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE
                        + "' is not possible in summon. Filter is '" + filter + "'");
                }
                query = "(" + query + ") " + filter;
                log.debug("Munging filter after query as the filter '" + filter + "' is marked '"
                          + DocumentKeys.SEARCH_FILTER_PURE_NEGATIVE + "' and summon is set up to not support pure "
                          + "negative filters natively. resulting query is '" + query + "'");
            }
        }
        if (query != null) { // We allow missing query
            querymap.put("s.q",   Arrays.asList(query));
        }

        querymap.put("s.ps",  Arrays.asList(Integer.toString(perpage)));
        querymap.put("s.pn",  Arrays.asList(Integer.toString(startpage)));

        // TODO: Add support for sorting on multiple fields
        if (sortKey != null) {
            querymap.put("s.sort", Arrays.asList(sortKey + ":" + (reverseSort ? "desc" : "asc")));
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
        if (sessionId != null && !sessionId.equals("")) {
            conn.setRequestProperty("x-summon-session-id", sessionId);
        }
        conn.setConnectTimeout(1000);
        Long readStart = System.currentTimeMillis();
    	long summonConnect = -System.currentTimeMillis();
        conn.connect();
        summonConnect += System.currentTimeMillis();
        
        BufferedReader in;
        try {
        	long rawCall = -System.currentTimeMillis();             
        	in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String str;
        
            while ((str = in.readLine()) != null) {
                retval.append(str);
            }
            log.trace("Reading from Summon done in " + (System.currentTimeMillis() - readStart) + "ms");
            in.close();
            rawCall += System.currentTimeMillis();
            responses.addTiming("summon.connect", summonConnect);
            responses.addTiming("summon.rawcall", rawCall); 
            
        } catch (IOException e) {
            log.warn(String.format(
                "getData(target=%s, content=%s, date=%s, idstring=%s, sessionID=%s failed with error stream\n%s",
                target, content, date, idstring, sessionId,
                Strings.flush(new InputStreamReader(conn.getErrorStream(), "UTF-8"))), e);
        }

        return retval.toString();
    }

    private static final QueryParser qp = QueryRewriter.createDefaultQueryParser();
    /*
     * The filter is marked as being pure facet:term pairs with optional NOT or -. If this is not the case, a
     * ParseException will be thrown. The filter will be converted to facet filters in the query map.
     * // http://api.summon.serialssolutions.com/help/api/search/parameters/facet-value-filter
     */
    static void convertFilterToFacet(String filter, Map<String, List<String>> querymap) throws ParseException {
        log.debug("The filter '" + filter + "' is marked as a facet filter. Attempting conversion...");
        Query q = qp.parse(filter);
        if (q instanceof TermQuery) {
            convertTermQuery(querymap, (TermQuery)q, false);
        } else if (q instanceof PhraseQuery) {
                convertPhraseQuery(querymap, (PhraseQuery) q, false);
        } else if (q instanceof BooleanQuery) {
            for (BooleanClause clause: ((BooleanQuery)q).getClauses()) {
                if (clause.getOccur() == BooleanClause.Occur.SHOULD) {
                    throw new ParseException("Encountered SHOULD in BooleanClause '" + clause + "' where only MUST or "
                                             + "MUST_NOT are allowed in filter '" + filter + "'");
                }
                if (clause.getQuery() instanceof TermQuery) {
                    convertTermQuery(
                        querymap, (TermQuery)clause.getQuery(), clause.getOccur() == BooleanClause.Occur.MUST_NOT);
                } else if (clause.getQuery() instanceof PhraseQuery) {
                    convertPhraseQuery(
                        querymap, (PhraseQuery)clause.getQuery(), clause.getOccur() == BooleanClause.Occur.MUST_NOT);
                } else {
                    throw new ParseException("Expected TermQuery or PhraseQuery as inner Query in BooleanQuery but got "
                                             + clause.getQuery().getClass().getSimpleName() + " in '" + filter + "'");
                }
            }
        } else {
            throw new ParseException("Only TermQuery and BooleanQuery is supported. Got '"
                                     + q.getClass().getSimpleName() + " from filter '" + filter + "'");
        }
    }

    private static void convertPhraseQuery(
        Map<String, List<String>> querymap, PhraseQuery pq, boolean negated) throws ParseException {
        if (pq.getTerms()[0].field() == null || "".equals(pq.getTerms()[0].field())) {
            throw new ParseException("Encountered PhraseQuery without field '" + pq + "'");
        }
        StringWriter terms = new StringWriter(100);
        boolean first = true;
        for (Term term: pq.getTerms()) {
            if (term.text() == null || "".equals(term.text())) {
                throw new ParseException("Encountered Term without text '" + pq + "' in PhraseQuery '" + pq + "'");
            }
            if (first) {
                first = false;
            } else {
                terms.append(" "); // This should work as we use a plain WhiteSpaceAnalyzer for tokenization
            }
            terms.append(term.text());
        }
        appendPut(querymap, "s.fvf", pq.getTerms()[0].field() + "," + terms + "," + negated);
    }

    static void convertTermQuery(
        Map<String, List<String>> querymap, TermQuery tq, boolean negated) throws ParseException {
        if (tq.getTerm().field() == null || "".equals(tq.getTerm().field())) {
            throw new ParseException("Encountered TermQuery without field '" + tq + "'");
        }
        if (tq.getTerm().text() == null || "".equals(tq.getTerm().text())) {
            throw new ParseException("Encountered TermQuery without text '" + tq + "'");
        }
        appendPut(querymap, "s.fvf", tq.getTerm().field() + "," + tq.getTerm().text() + "," + negated);
    }

    private static void appendPut(Map<String, List<String>> querymap, String key, String... values) {
        List<String> existing = querymap.get(key);
        if (existing == null) {
            querymap.put(key, Arrays.asList(values));
            return;
        }
        if (!(existing instanceof ArrayList)) {
            existing = new ArrayList<String>(existing);
            querymap.put(key, existing);
        }
        existing.addAll(Arrays.asList(values));
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
