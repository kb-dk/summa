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
package dk.statsbiblioteket.summa.support.summon.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import dk.statsbiblioteket.summa.search.document.DocumentSearcherImpl;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
 * {@link DocumentResponse} and {@link FacetResult}.
 * See http://api.summon.serialssolutions.com/help/api/ for Summon API.
 */
// Map contenttype, language. Convert date (til år lige nu), potentially library 
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummonSearchNode extends DocumentSearcherImpl {
    private static Log log = LogFactory.getLog(SummonSearchNode.class);

    /**
     * The entry point for calls to Summon. This is unlikely to change.
     * </p><p>
     * Optional. Default is api.summon.serialssolutions.com.
     */
    public static final String CONF_SUMMON_HOST =
        "summon.host";
    public static final String DEFAULT_SUMMON_HOST =
        "api.summon.serialssolutions.com";

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
     * The default facets if none are specified. The syntax is a comma-separated
     * list of facet names, optionally with max tags in paranthesis.
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

    private static final DateFormat formatter =
        new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);


    private String summonHost = DEFAULT_SUMMON_HOST;
    private String accessID;
    private String accessKey;
    private String idPrefix = DEFAULT_SUMMON_IDPREFIX;
    private int defaultPageSize = DEFAULT_SUMMON_FACETS_DEFAULTPAGESIZE;
    private String defaultFacets = DEFAULT_SUMMON_FACETS;
    private String combineMode = DEFAULT_SUMMON_FACETS_COMBINEMODE;

    public SummonSearchNode(Configuration conf) throws RemoteException {
        super(conf);
        summonHost = conf.getString(CONF_SUMMON_HOST, summonHost);
        accessID =   conf.getString(CONF_SUMMON_ACCESSID);
        accessKey =  conf.getString(CONF_SUMMON_ACCESSKEY);
        idPrefix =   conf.getString(CONF_SUMMON_IDPREFIX, idPrefix);
        defaultPageSize = conf.getInt(
            CONF_SUMMON_FACETS_DEFAULTPAGESIZE, defaultPageSize);
        defaultFacets = conf.getString(CONF_SUMMON_FACETS, defaultFacets);
        combineMode = conf.getString(
            CONF_SUMMON_FACETS_COMBINEMODE, combineMode);
    }

    @Override
    protected void managedOpen(String location) throws RemoteException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void managedClose() throws RemoteException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public DocumentResponse fullSearch(
        Request request, String filter, String query, long startIndex,
        long maxRecords, String sortKey, boolean reverseSort,
        String[] resultFields, String[] fallbacks) throws RemoteException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected long getHitCount(Request request, String query, String filter)
                                                            throws IOException {
        return fullSearch(request, filter, query, 0, 1, null, false,
                          new String[0], new String[0]).getHitCount();
    }

    private boolean collectCalled = false;
    @Override
    protected DocIDCollector collectDocIDs(
        Request request, String query, String filter) throws IOException {
        if (!collectCalled) {
            log.debug("No collectdocIDs for '" + query + "' as it is not "
                      + "supported by the Summon API. Further requests "
                      + "for colelctDocIDs will be silently ignored");
            collectCalled = true;
        }
        return null;
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







    public static String SUMMON_ID_PREFIX = "summon_";
    private static final String ACCESSID = "statsbiblioteket";

    /**
     * Perform a search in Summon
     * @param query The query to look for
     * @param startpage the page to start on
     * @param perpage number of items per page
     * @param resolveLinks whether or not to call the link resolver to resolve openurls to actual links
     * @return A String containing XML describing the search result
     */
    public static String simpleSearch(String query, String startpage, String perpage, boolean resolveLinks) {
        long methodStart = System.currentTimeMillis();
        log.trace("Calling simpleSearch(" + query + ", " + startpage + ", " + perpage + ")");
        //Building
        Date date = new Date();
        Map<String, List<String>> querymap = new HashMap<String, List<String>>();

        List<String> dymstring = new ArrayList<String>();
        dymstring.add("true");
        querymap.put("s.dym",dymstring);

        List<String> hostring = new ArrayList<String>();
        hostring.add("true");
        querymap.put("s.ho", hostring);

        List<String> perpagestring = new ArrayList<String>();
        perpagestring.add(perpage);
        querymap.put("s.ps",perpagestring);

        List<String> startpagestring = new ArrayList<String>();
        startpagestring.add(startpage);
        querymap.put("s.pn",startpagestring);

        List<String> facetstring = new ArrayList<String>();
        //facetstring.add("Audience,and,1,5");
        facetstring.add("Author,and,1,15");
        facetstring.add("ContentType,and,1,15");
        facetstring.add("Genre,and,1,15");
        //facetstring.add("GeographicLocation,and,1,5"); // TODO: Gives an error
        facetstring.add("IsScholarly,and,1,15");
        facetstring.add("Language,and,1,15");
        //facetstring.add("Library,and,1,5");
        //facetstring.add("PackageID,and,1,5");
        //facetstring.add("SourceID,and,1,5");
        facetstring.add("SubjectTerms,and,1,15");
        facetstring.add("TemporalSubjectTerms,and,1,15");
        querymap.put("s.ff",facetstring);

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

        List<String> querystring = new ArrayList<String>();
        querystring.add(query);
        querymap.put("s.q",querystring);

        String idstring = computeIdString("application/xml", formatter.format(date),
                "api.summon.serialssolutions.com", "/search", querymap);
        String queryString = computeSortedQueryString(querymap, true);
        log.trace("Parameter preparation to Summon done in " + (System.currentTimeMillis() - methodStart) + "ms");
        String result = "";
        try {
            long serviceStart = System.currentTimeMillis();
            result = getData("http://api.summon.serialssolutions.com", "/search?" +
                    queryString, date, idstring, null);
                 log.trace("Call to Summon done in " + (System.currentTimeMillis() - serviceStart) + "ms");
            //log.trace("Result of call to Summon: " + result);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        String retval = prefixIDs(result, SUMMON_ID_PREFIX);
        if (resolveLinks) {
            retval = linkResolve(retval);
        }
        log.trace("simpleSearch done in " + (System.currentTimeMillis() - methodStart) + "ms");
        return retval;
    }

    /**
     * Add to an existing Summon search using commands
     * @param queryString The query string identifiying an existing Summon search
     * @param resolveLinks whether or not to call the link resolver to resolve openurls to actual links
     * @param commands The commands to apply to the search
     * @return A String containing XML describing the search result
     */
    public static String continueSearch(String queryString, boolean resolveLinks, String... commands) {
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

        String retval = prefixIDs(result, SUMMON_ID_PREFIX);
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
    private static String getData(String target, String content, Date date,
                                  String idstring, String sessionId) throws Exception {
        StringBuilder retval = new StringBuilder();

        System.setProperty("java.protocol.handler.pkgs","com.sun.net.ssl.internal.www.protocol");
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());

        URL url = new URL(target + content);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Host", "api.summon.serialssolutions.com");
        conn.setRequestProperty("Accept", "application/xml");
        conn.setRequestProperty("Accept-Charset", "utf-8");
        DateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);
        conn.setRequestProperty("x-summon-date", formatter.format(date));
        conn.setRequestProperty("Authorization", "Summon " + ACCESSID + ";" + buildDigest(SECRETKEY, idstring));
        if (sessionId != null && !sessionId.equals("")) {
            conn.setRequestProperty("x-summon-session-id", sessionId);
        }
        conn.setConnectTimeout(1000);
        conn.connect();

        Long readStart = System.currentTimeMillis();
        BufferedReader in;
        try {
            in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String str;
            while ((str = in.readLine()) != null) {
                retval.append(str);
            }
            log.trace("Reading from Summon done in " + (System.currentTimeMillis() - readStart) + "ms");
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
            String str;
            while ((str = err.readLine()) != null) {
                log.error(str);
            }
        }

        return retval.toString();
    }

    /**
     * Given an id returns the corresponding Summon record in a format similar to Summa's shortformat
     * @param id Summon id
     * @return A String containing XML in shortformat
     */
    public static String getShortRecord(String id) {
        // TODO: include id in shortrecord tag to help out sendListEmail.jsp and ExportToExternalFormat.java
        StringBuilder retval = new StringBuilder();

        if (id.startsWith(SUMMON_ID_PREFIX)) {
            id = id.substring(SUMMON_ID_PREFIX.length());
        }

        String temp = simpleSearch("ID:" + id, "1", "1", false);
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
    public static String getRecord(String id) {
        return getRecord(id, false);
    }

    /**
     * Gets a record from Summon while optionally resolving links
     * @param id Summon id
     * @param resolveLinks Whether or not to resolve links through the link resolver
     * @return A String containing a Summon record in XML
     */
    public static String getRecord(String id, boolean resolveLinks) {
        String retval = "";

        if (id.startsWith(SUMMON_ID_PREFIX)) {
            id = id.substring(SUMMON_ID_PREFIX.length());
        }

        String temp = simpleSearch("ID:" + id, "1", "1", false);
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
//            WebServices services = WebServices.getInstance();
//            String links = (String) services.execute("getfrom360linkmulti", arg);
            long serviceTime = System.currentTimeMillis() - serviceStart;
            log.trace("Called getfrom360linkmulti in : " + serviceTime + "ms");

/*            Document linksDom = DOM.stringToDOM(links);
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

*/
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
    }

    public static void main(String[] args) {
        String temp;


        temp = simpleSearch("foo", "1", "3", false);
        System.out.println(temp);

        temp = simpleSearch("CatchAll:foo", "1", "3", false);
        System.out.println(temp);
    }



}
