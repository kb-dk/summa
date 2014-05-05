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
package dk.statsbiblioteket.summa.web.services;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Log4JSetup;
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.util.Environment;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultExternal;
import dk.statsbiblioteket.summa.facetbrowser.api.IndexKeys;
import dk.statsbiblioteket.summa.facetbrowser.api.IndexResponse;
import dk.statsbiblioteket.summa.search.SummaSearcherFactory;
import dk.statsbiblioteket.summa.search.api.*;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.support.api.*;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.DOM;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class containing methods meant to be exposed as a web service.
 *
 * @author Mads Villadsen <mailto:mv@statsbiblioteket.dk>
 * @author Henrik Bitsch Kirk <mailto:hbk@statsbiblioteket.dk>
 */
@WebService
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te, mv, hbk")
public class SearchWS implements ServletContextListener {
    private Log log;

    static SummaSearcher searcher;
    static SummaSearcher suggester;
    static SummaSearcher didyoumean;
    private Configuration conf;

    /**
     * Local XML output factory.
     */
    private static XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

    /** Context or property key for the location of the configuration for this webservice. */
    public static final String CONFIGURATION_LOCATION = "SearchWS_config";
    /** Did-You-Mean XML namespace. */
    public static final String NAMESPACE = "http://statsbiblioteket.dk/summa/2009/SearchError";
    /** XML tag for search error response. */
    public static final String SEARCH_ERROR_RESPONSE = "searcherror";
    /** XML name tag. */
    public static final String NAME_TAG = "name";
    /** XML error tag. */
    public static final String ERROR_TAG = "error";
    /** XML message tag. */
    public static final String MESSAGE_TAG = "message";
    /** XML version attribute. */
    public static final String VERSION_TAG = "version";
    /** XML version attribute value. */
    public static final String VERSION = "1.0";

    private static final String DEFAULT_CONF_FILE = "configuration_search.xml";

    /**
     * Default constructor.
     */
    public SearchWS() {
        log = LogFactory.getLog(SearchWS.class);
        // Do not log anything here as contextInitialized will be called _after_ the constructor
    }

    /**
     * Get a single SearchClient based on the system configuration.
     *
     * @return A SearchClient.
     */
    private SummaSearcher getSearcher() {
        synchronized (SearchWS.class) {
            if (searcher == null) {
                searcher = createSearcher("search", "summa.web.search");
            }
            return searcher;
        }
    }

    private SummaSearcher createSearcher(String designation, String subKey) {
        try {
            Configuration conf = getConfiguration();
            if (conf.containsKey(subKey)) {
                log.debug("Using inner configuration " + subKey);
                conf = conf.getSubConfiguration(subKey);
            } else {
                log.debug("No inner configuration for " + designation + ". Using directly");
            }
            if (conf.containsKey(SummaSearcher.CONF_CLASS)) {
                log.info("Configuration " + SummaSearcher.CONF_CLASS + " present. " +
                         "Creating direct searcher for " + designation);
                return SummaSearcherFactory.createSearcher(conf);
            } else {
                log.info("Configuration " + SummaSearcher.CONF_CLASS + " not present. " +
                         "Creating SearchClient for " + designation);
                return new SearchClient(conf);
            }
        } catch (SubConfigurationsNotSupportedException e) {
            log.error("Storage doesn't support sub configurations");
        } catch (NullPointerException e) {
            log.error("Failed to load subConfiguration for " + designation, e);
        }
        return null;
    }

    private SummaSearcher getSuggester() {
        synchronized (SearchWS.class) {
            if (suggester == null) {
                suggester = createSearcher("suggest", "summa.web.suggest");
            }
            return suggester;
        }
    }

    /**
     * Get a single DidYouMeanClient for Did-you-Mean service, based on the
     * system configuration.
     *
     * @return A DidYouMeanClient which can be used for Did-You-Mean services.
     */
    private synchronized SummaSearcher getDidYouMeanClient() {
        synchronized (SearchWS.class) {
            if (didyoumean == null) {
                didyoumean = createSearcher("didyoumean", "summa.web.didyoumean");
            }
            return didyoumean;
        }
    }

    /**
     * Get the a Configuration object. First trying to load the configuration
     * from the location specified in the JNDI property
     * java:comp/env/confLocation, and if that fails, then the System
     * Configuration will be returned.
     *
     * @return The Configuration object.
     */
    private synchronized Configuration getConfiguration() {
        //final String SEARCH_CONTEXT = "java:comp/env/confLocation";
        if (conf == null) {
            conf = Configuration.resolve(CONFIGURATION_LOCATION, CONFIGURATION_LOCATION, DEFAULT_CONF_FILE, true);
        }
        return conf;
    }


    /**
     * Given search query and maximum number of result, this method returns a
     * XML block containing the suggestions given by the did-you-mean service.
     *
     * @param query User given search query.
     * @param maxSuggestions Maximum number of returned suggestions.
     * @return XML block containing the suggestions given the did-you-mean
     * services.
     */
    @WebMethod
    public String didYouMean(String query, int maxSuggestions) {
        log.trace("didYouMean('" + query + "', " + maxSuggestions + ")");
        long startTime = System.currentTimeMillis();
        String retXML;

        ResponseCollection res = null;

        Request req = new Request();
        req.put(DidYouMeanKeys.SEARCH_QUERY, query);
        req.put(DidYouMeanKeys.SEARCH_MAX_RESULTS, maxSuggestions);

        try {
            res = getDidYouMeanClient().search(req);
            if (res.isEmpty()) {
                log.debug("No response from DidYouMeanClient. It might be disabled");
                return getErrorXML(DidYouMeanResponse.DIDYOUMEAN_RESPONSE, "No response from DidYouMean Client", null);
            }
            Document dom = DOM.stringToDOM(res.toXML());
            Node subDom = DOM.selectNode(
                    dom, "/responsecollection/response[@name='DidYouMeanResponse']/DidYouMeanResponse");
            retXML = DOM.domToString(subDom);
        } catch (IOException e) {
            log.warn("Error executing didYouMean: '" + query + "', " + maxSuggestions + ". Error was: ", e);
            String mes = "Error performing didYouMean query";
            retXML = getErrorXML(DidYouMeanResponse.DIDYOUMEANRESPONSE, mes, e);
        } catch (TransformerException e) {
            log.warn("Error executing didYouMean: '" + query + "', " + maxSuggestions + ". Error was: ", e);
            String mes = "Error performing didYouMean query";
            retXML = getErrorXML(DidYouMeanResponse.DIDYOUMEANRESPONSE, mes, e);
        }

        if (log.isDebugEnabled()) {
            log.debug("didYouMean('" + query + "', " + maxSuggestions + ") finished in "
                      + (System.currentTimeMillis() - startTime) + "ms" + getTiming(res));
        }
        return retXML;
    }

    /**
     * Given a prefix this method returns other queries that start with the same
     * prefix.
     *
     * @param prefix The prefix that the returned queries must start with.
     * @param maxSuggestions The maximum number of queries to be returned.
     * @return An XML string containing the result or an error description.
     */
    @WebMethod
    public String getSuggestions(String prefix, int maxSuggestions) {
        log.trace("getSuggestion('" + prefix + "', " + maxSuggestions + ")");
        long startTime = System.currentTimeMillis();
        String retXML;

        ResponseCollection res = null;

        Request req = new Request();
        req.put(SuggestKeys.SEARCH_PREFIX, prefix);
        req.put(SuggestKeys.SEARCH_MAX_RESULTS, maxSuggestions);

        try {
            res = getSuggester().search(req);
            Document dom = DOM.stringToDOM(res.toXML());
            Node subDom = DOM.selectNode(dom,
                                         "/responsecollection/response[@name='SuggestResponse']/QueryResponse/suggestions");
            retXML = DOM.domToString(subDom);
        } catch (IOException e) {
            log.warn("Error executing getSuggestions: '" + prefix + "', " + maxSuggestions + ". Error was: ", e);
            String mes = "Error performing getSuggestions";
            retXML = getErrorXML(SuggestResponse.NAME, mes, e);
        } catch (TransformerException e) {
            log.warn("Error executing getSuggestions: '" + prefix + "', " + maxSuggestions + ". Error was: ", e);
            String mes = "Error performing getSuggestions";
            retXML = getErrorXML(SuggestResponse.NAME, mes, e);
        }

        if (log.isDebugEnabled()) {
            log.debug("getSuggestion('" + prefix + "', " + maxSuggestions + ") finished in "
                      + (System.currentTimeMillis() - startTime) + "ms" + getTiming(res));
        }
        return retXML;
    }

    /**
     * Web method for deleting a suggestion from storage.
     * @param suggestion The suggestion that should be deleted from storage.
     */
    @WebMethod
    public void deleteSuggestion(String suggestion) {
        log.trace("deleteSuggestion('" + suggestion + "')");

        Request req = new Request();
        req.put(SuggestKeys.DELETE_SUGGEST, suggestion);
        try {
            getSuggester().search(req);
        } catch (IOException e) {
            log.warn("Error deleting suggetion '" + suggestion + "'");
        }
    }

    /**
     * Returns any suggestions added or updated within the last
     * {@code ageSeconds} returning a maximum of {@code maxSuggestions}
     * results.
     *
     * @param ageSeconds Number of seconds to look back
     * @param maxSuggestions The maximum number of queries to be returned.
     * @return An XML string containing the result or an error description.
     */
    @WebMethod
    public String getRecentSuggestions(int ageSeconds, int maxSuggestions) {
        log.trace("getRecentSuggestions(" + ageSeconds + "s, " + maxSuggestions + ")");
        long startTime = System.currentTimeMillis();
        String retXML;
        ResponseCollection res;

        Request req = new Request();
        req.put(SuggestKeys.SEARCH_RECENT, ageSeconds);
        req.put(SuggestKeys.SEARCH_MAX_RESULTS, maxSuggestions);

        try {
            res = getSuggester().search(req);
            Document dom = DOM.stringToDOM(res.toXML());
            Node subDom = DOM.selectNode(dom,
                                         "/responsecollection/response[@name='SuggestResponse']/QueryResponse/suggestions");
            retXML = DOM.domToString(subDom);
        } catch (IOException e) {
            log.warn("Error executing getRecentSuggestions: " + ageSeconds + "s, " + maxSuggestions + ". Error was: ",
                     e);
            String mes = "Error performing getRecentSuggestions";
            retXML = getErrorXML(SuggestResponse.NAME, mes, e);
        } catch (TransformerException e) {
            log.warn("Error executing getRecentSuggestions: " + ageSeconds + "s, " + maxSuggestions + ". Error was: ",
                     e);
            String mes = "Error performing getRecentSuggestions";
            retXML = getErrorXML(SuggestResponse.NAME, mes, e);
        }

        log.debug("getRecentSuggestions(" + ageSeconds + "s, " + maxSuggestions
                  + ") finished in " + (System.currentTimeMillis() - startTime) + "ms");
        return retXML;
    }

    /**
     * Commits a query to the Suggestion database. This enables this query to be
     * returned in the result from getSuggestions. It is recommended that only
     * query that the user actually enters are committed - ie. it might not be
     * a good idea to commit queries that come from the user clicking facets,
     * etc.
     *
     * @param query The query to commit to the database.
     * @param hitCount The number of hits that resulted from the query. If this
     * is 0 then the query is removed from the Suggestion database.
     */
    @WebMethod
    public void commitQuery(String query, long hitCount) {
        log.debug("commitQuery('" + query + "', " + hitCount + ")");

        Request req = new Request();
        req.put(SuggestKeys.SEARCH_UPDATE_QUERY, cleanQuery(query));
        req.put(SuggestKeys.SEARCH_UPDATE_HITCOUNT, hitCount);

        try {
            getSuggester().search(req);
        } catch (IOException e) {
            log.warn("Error committing query '" + cleanQuery(query) + "' with hitCount '" + hitCount + "'");
        }
    }

/*    // TODO Should be implemented
    public String getFields(String[] ids, String fieldName) {
        log.trace("getFields([" + ids + "], '" + fieldName + "')");
        //long startTime = System.currentTimeMillis();
        String retXML = null;
        //ResponseCollection res;

        Request req = new Request();
        req.put(DocumentKeys.SEARCH_QUERY, ids);
        req.put(DocumentKeys.SEARCH_MAX_RECORDS, ids.length);
        req.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        return retXML;
    }*/

    /**
     * Returns a given field from the search index for a specific recordId. This
     * could for instance be used to get the shortformat for a specific record.
     *
     * @param id The recordId to look up.
     * @param fieldName The name of the field to return.
     * @return An XML string containing the result or an error description.
     */
    @WebMethod
    public String getField(String id, String fieldName) {
        log.trace("getField('" + id + "', '" + fieldName + "')");
        long startTime = System.currentTimeMillis();
        String retXML;

        ResponseCollection res = null;

        Request req = new Request();
        req.put(DocumentKeys.SEARCH_QUERY, "recordID:\"" + id + "\"");
        req.put(DocumentKeys.SEARCH_MAX_RECORDS, 1);
        req.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);

        try {
            res = getSearcher().search(req);
            Document dom = DOM.stringToDOM(res.toXML());
            Node subDom = DOM.selectNode(
                    dom, "/responsecollection/response/documentresult/record/field[@name='" + fieldName + "']");
            retXML = DOM.domToString(subDom);
            if (retXML == null || "".equals(retXML) && log.isDebugEnabled()) {
                log.debug("getField(" + id + ", " + fieldName + ") did not give any return-XML. " +
                          "The reply from the core search was\n" + res.toXML());
            }
        } catch (IOException e) {
            log.warn("Error querying for id: '" + id + "'." + "Error was: ", e);
            String mes = "Error performing query";
            retXML = getErrorXML(DocumentResponse.NAME, mes, e);
        } catch (TransformerException e) {
            log.warn("Error querying for id: '" + id + "'." + "Error was: ", e);
            String mes = "Error performing query";
            retXML = getErrorXML(DocumentResponse.NAME, mes, e);
        }

        log.trace("getField('" + id + "', '" + fieldName + "') finished in " + (System.currentTimeMillis() - startTime)
                  + "ms" + getTiming(res));
        return retXML;
    }


    /**
     * Performs a lookup in the given field for the given term and returns a
     * list starting at the term position + delta with the given length.
     * </p><p>
     * Example: {@code indexLookup(myField, d, -2, 5)} on a field that has
     *          the values a, b, c, d, e, f, g, h will give the result
     *          b, c, d, e, f.
     * </p><p>
     * If the term cannot be located, the nearest matching position will be
     * used instead.
     *
     * @param field  The field to perform a lookup on. Currently this must be
     *               the name of a facet.
     * @param term   The term to search for. This can be multiple words.
     * @param delta  The offset relative to the term position.
     * @param length The maximum number of terms to return.
     * @return An XML string containing the result or an error description.
     */
    @WebMethod
    public String indexLookup(String field, String term, int delta, int length) {
        //noinspection DuplicateStringLiteralInspection
        String call = "indexLookup(" + field + ":" + term + ", " + delta + ", " + length + ")";
        log.trace(call);
        long startTime = System.currentTimeMillis();
        String retXML;

        ResponseCollection res = null;

        Request req = new Request();
        req.put(IndexKeys.SEARCH_INDEX_FIELD, field);
        req.put(IndexKeys.SEARCH_INDEX_TERM, term);
        req.put(IndexKeys.SEARCH_INDEX_DELTA, delta);
        req.put(IndexKeys.SEARCH_INDEX_LENGTH, length);

        try {
            res = getSearcher().search(req);
            retXML = res.toXML();
        } catch (IOException e) {
            log.warn("Error executing " + call + ": ", e);
            String mes = "Error performing " + call + ": " + e.getMessage();
            retXML = getErrorXML(IndexResponse.NAME, mes, e);
        }
        //noinspection DuplicateStringLiteralInspection
        if (log.isTraceEnabled()) {
            log.trace(call + " finished in " + (System.currentTimeMillis() - startTime) + "ms" + getTiming(res));
        }
        return retXML;
    }

    /**
     * Performs a lookup in the given field for the given term and returns a
     * list starting at the term position + delta with the given length.
     * </p><p>
     * The method works as {@link #indexLookup(String, String, int, int)} but
     * allows for query-based restriction on the documents used to form the
     * index.
     * @param query  If not null, only the terms from the documents matching the
     *               query will be used for the index lookup. If null, all
     *               documents will be matched.
     * @param field  The field to perform a lookup on. Currently this must be
     *               the name of a facet.
     * @param term   The term to search for. This can be multiple words.
     * @param delta  The offset relative to the term position.
     * @param length The maximum number of terms to return.
     * @param minCount The minimum number of documents that must contain the
     *               term for the term to be returned. Normally 0 or 1.
     * @return An XML string containing the result or an error description.
     */
    @WebMethod
    public String extendedIndexLookup(String query, String field, String term, int delta, int length, int minCount) {
        //noinspection DuplicateStringLiteralInspection
        String call = "indexLookup(" + field + ":" + term + ", " + delta + ", " + length + ")";
        log.trace(call);
        long startTime = System.currentTimeMillis();
        String retXML;

        ResponseCollection res = null;

        Request req = new Request();
        if (query != null) {
            req.put(IndexKeys.SEARCH_INDEX_QUERY, query);
        }
        req.put(IndexKeys.SEARCH_INDEX_FIELD, field);
        req.put(IndexKeys.SEARCH_INDEX_TERM, term);
        req.put(IndexKeys.SEARCH_INDEX_DELTA, delta);
        req.put(IndexKeys.SEARCH_INDEX_LENGTH, length);
        req.put(IndexKeys.SEARCH_INDEX_MINCOUNT, minCount);

        try {
            res = getSearcher().search(req);
            retXML = res.toXML();
        } catch (IOException e) {
            log.warn("Error executing " + call + ": ", e);
            String mes = "Error performing " + call + ": " + e.getMessage();
            retXML = getErrorXML(IndexResponse.NAME, mes, e);
        }
        //noinspection DuplicateStringLiteralInspection
        if (log.isTraceEnabled()) {
            log.trace(call + " finished in " + (System.currentTimeMillis() - startTime) + "ms" + getTiming(res));
        }
        return retXML;
    }

    /**
     * Gives a search result of records that "are similar to" a given record.
     *
     * @param id The recordID of the record that should be used as base for the
     * MoreLikeThis query.
     * @param numberOfRecords The maximum number of records to return.
     * @return An XML string containing the result or an error description.
     */
    @WebMethod
    public String getMoreLikeThis(String id, int numberOfRecords) {
        log.trace("getMoreLikeThis('" + id + "', " + numberOfRecords + ")");
        long startTime = System.currentTimeMillis();
        String retXML;

        ResponseCollection res = null;

        Request req = new Request();
        req.put(LuceneKeys.SEARCH_MORELIKETHIS_RECORDID, id);
        req.put(DocumentKeys.SEARCH_MAX_RECORDS, numberOfRecords);
        req.put(DocumentKeys.SEARCH_SORTKEY, DocumentKeys.SORT_ON_SCORE);
        req.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        req.put(DocumentKeys.SEARCH_RESULT_FIELDS, IndexUtils.RECORD_FIELD + ", shortformat, score");

        try {
            res = getSearcher().search(req);
            retXML = res.toXML();
        } catch (IOException e) {
            log.warn("Error executing morelikethis: '" + id + "', " + numberOfRecords + ". Error was: ", e);
            String mes = "Error performing morelikethis";
            retXML = getErrorXML(DocumentResponse.NAME, mes, e);
        }
        if (log.isDebugEnabled()) {
            log.debug("getMoreLikeThis('" + id + "', " + numberOfRecords + ") finished in "
                      + (System.currentTimeMillis() - startTime) + "ms" + getTiming(res));
        }
        return retXML;
    }

    /**
     * A simple way to query the index returning results sorted by relevance.
     * The same as calling simpleSearchSorted while specifying a normal sort on
     * relevancy.
     *
     * @param query The query to perform.
     * @param numberOfRecords The maximum number of records to return.
     * @param startIndex Where to start returning records from (used to
     * implement paging).
     * @return An XML string containing the result or an error description.
     */
    @WebMethod
    public String simpleSearch(String query, int numberOfRecords, int startIndex) {
        return simpleSearchSorted(query, numberOfRecords, startIndex, DocumentKeys.SORT_ON_SCORE, false);
    }

    /**
     * A do-all method that takes a JSON list of key-value pairs as input and
     * returns an XML structure representing document responses. The caller is
     * responsible for defining all necessary keys.
     * </p><p>
     * Sample: {"search.document.query":"foo bar",
     *          "search.document.sortkey":"sort_title",
     *          "search.document.startindex":100}
     * </p><p>
     * Warning: Here be dragons. It is not recommended to expose this method
     *          directly to uncontrolled parties, as there is no contract
     *          in place that limit Searcher behaviour to be non-destructive.
     *          E.g. one Searcher could expose a method for performing rollback
     *          to an earlier index. Another could expose a delete document
     *          feature.
     * </p><p>
     * See http://json.org for details on JSON.
     * @param json a key-value list.
     * @return XML representing DocumentResponses.
     */
    public String directJSON(String json) {
        long startTime = System.currentTimeMillis();
        String retXML;
        ResponseCollection res = null;
        Request req = new Request();
        req.addJSON(json);
        try {
            res = getSearcher().search(req);
            log.trace("Got result, converting to XML");
            retXML = res.toXML();
        } catch (IOException e) {
            String mes = String.format("Error performing JSON query '%s': %s", json, e.getMessage());
            log.warn(mes, e);
            retXML = getErrorXML(DocumentResponse.NAME, mes, e);
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("directJSON(%s) finished in %s ms%s",
                                    json, System.currentTimeMillis() - startTime, getTiming(res)));
        }
        return retXML;
    }

    public static final Pattern PROCESSING_OPTIONS = Pattern.compile("\\<\\:(.*)\\:\\>(.*)");
    /**
     * A simple way to query the index wile being able to specify which field to
     * sort by and whether the sorting should be reversed.
     * </p><p>
     * Processing-options can be specified at the start of the query prepended
     * by '<:' and appended by ':>', divided by spaces. As of now, the only
     * possible option is 'explain', which turns on explanation of the result.
     * This increases processing time, so do not turn it on as default.
     * example: "<:explain:>foo" will explain why the documents matching foo
     * was selected.
     *
     * @param query The query to perform.
     * @param numberOfRecords The maximum number of records to return.
     * @param startIndex Where to start returning records from (used to
     * implement paging).
     * @param sortKey The field to sort by.
     * @param reverse Whether or not the sort should be reversed.
     * @return An XML string containing the result or an error description.
     */
    @WebMethod
    public String simpleSearchSorted(
            String query, int numberOfRecords, int startIndex, String sortKey, boolean reverse) {
        if (log.isTraceEnabled()) {
            log.trace(String.format(
                    "simpleSearchSorted(query='%s', numberOfRecords=%d, " +
                    "startIndex=%d, sortKey='%s', reverse=%b) entered",
                    query, numberOfRecords, startIndex, sortKey, reverse));
        }
        return filterSearchSorted(null, query, numberOfRecords, startIndex, sortKey, reverse);
    }

    /**
     * A simple way to query the index wile being able to specify which field to
     * sort by and whether the sorting should be reversed.
     * </p><p>
     * Processing-options can be specified at the start of the query prepended
     * by '<:' and appended by ':>', divided by spaces. As of now, the only
     * possible option is 'explain', which turns on explanation of the result.
     * This increases processing time, so do not turn it on as default.
     * example: "<:explain:>foo" will explain why the documents matching foo
     * was selected.
     *
     * @param filter The filter to use before querying.
     * @param query The query to perform.
     * @param numberOfRecords The maximum number of records to return.
     * @param startIndex Where to start returning records from (used to
     * implement paging).
     * @param sortKey The field to sort by.
     * @param reverse Whether or not the sort should be reversed.
     * @return An XML string containing the result or an error description.
     */
    @WebMethod
    public String filterSearchSorted(
            String filter, String query, int numberOfRecords, int startIndex, String sortKey, boolean reverse) {
        final String PARAMS = "filter='%s', query='%s', numberOfRecords=%d, startIndex=%d, sortKey='%s', reverse=%b";
        if (log.isDebugEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.debug(String.format("filterSearchSorted(" + PARAMS + ") called",
                                    filter, query, numberOfRecords, startIndex, sortKey, reverse));
        }
        long startTime = System.currentTimeMillis();

        String retXML;
        ResponseCollection res = null;
        Request req = new Request();
        // Handle processing options
        log.trace("Extracting options (aka explain)");
        try {
            String[] options = extractOptions(query);
            if (options != null) {
                log.debug("simpleSearchSorted received " + options.length + " options");
                for (String option: options) {
                    if ("explain".equals(option)) {
                        log.debug("Turning on explain for query '" + query + "'");
                        req.put(DocumentKeys.SEARCH_EXPLAIN, true);
                        continue;
                    }
                    log.debug(String.format("Got unknown processing option '%s' in query '%s'", option, query));
                }
            } else {
                log.trace(String.format("No processing options for query '%s'", query));
            }
        } catch (Exception e) {
            log.warn(String.format(
                    "Exception while extracting processing options from query "
                    + "'%s'. Options are skipped and the query left unchanged",
                    query));
        }
        log.trace("Cleaning query");
        query = cleanQuery(query);

        if (filter != null) {
            log.trace("Assigning filter '" + filter + "'");
            req.put(DocumentKeys.SEARCH_FILTER , filter);
        }
        if (query != null) {
            log.trace("Assigning query '" + query + "'");
            req.put(DocumentKeys.SEARCH_QUERY, query);
        }
        req.put(DocumentKeys.SEARCH_MAX_RECORDS, numberOfRecords);
        req.put(DocumentKeys.SEARCH_START_INDEX, startIndex);
        if (sortKey != null) {
            req.put(DocumentKeys.SEARCH_SORTKEY, sortKey);
        }
        req.put(DocumentKeys.SEARCH_REVERSE, reverse);
        req.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);
        log.trace("Produced search request, calling search");
        try {
            long callStart = System.currentTimeMillis();
            res = getSearcher().search(req);
            res.addTiming("searchws.outercall", System.currentTimeMillis() - callStart);
            log.trace("Got result, converting to XML");
            retXML = res.toXML();
        } catch (IOException e) {
            log.warn(String.format(
                    "Error executing query '" + PARAMS + "'. Error was: %s",
                    filter, query, numberOfRecords, startIndex, sortKey, reverse, e.getMessage()), e);
            String mes = String.format("Error performing query: %s", e.getMessage());
            retXML = getErrorXML(DocumentResponse.NAME, mes, e);
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "simpleSearchSorted(" + PARAMS + ") finished in %s ms%s",
                    filter, query, numberOfRecords, startIndex, sortKey, reverse,
                    System.currentTimeMillis() - startTime,  getTiming(res)));
        }
        return retXML;
    }

    /**
     * Performs a pseudo-search without any keys and returns the time it took
     * to call the searcher. In order to measure properly, the caller should
     * measure the total call time including webservice overhead.
     * @param message the return message.
     * @return the given message and the time it took to perform a null search.
     */
    @WebMethod
    public String ping(String message) {
        long pingTime = -System.currentTimeMillis();
        try {
            ResponseCollection res = getSearcher().search(new Request());
            String returnMessage = "ping".equals(message) ? "pong" : message;
            returnMessage = returnMessage.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            pingTime += System.currentTimeMillis();
            final String pingResponse = String.format(
                    "<pingresponse >\n"
                    + "<message>%s</message>\n"
                    + "<ms>%d</ms>\n"
                    + "</pingresponse>\n",
                    returnMessage, pingTime);
            res.add(new ResponseImpl() {
                @Override
                public String getName() {
                    return "PingResponse";
                }
                // TODO: Implement ping merge
                @Override
                public void merge(Response other) throws ClassCastException {
                    super.merge(other);
                    log.warn("No ping merge right now");
                }
                @Override
                public String toXML() {
                    return pingResponse;
                }
            });
            log.debug("Performed ping for '" + message + "' in " + pingTime + "ms");
            return res.toXML();
        } catch (IOException e) {
            String mes = String.format("Error pinging with message '%s': %s", message, e.getMessage());
            log.warn(mes, e);
            return  getErrorXML("PingResponse", mes, e);
        }

    }

    /**
     * Cleanup a query string.
     *
     * @param queryString The query string, which should be cleaned.
     * @return A cleaned query string.
     */
    private String cleanQuery(String queryString) {
        if (queryString == null) {
            return null;
        }
        Matcher optionsMatcher = PROCESSING_OPTIONS.matcher(queryString);
        if (optionsMatcher.matches()) {
            return optionsMatcher.group(2);
        }
        return queryString;
    }

    /**
     * Extract options from 'query' message.
     *
     * @param query The message from which we should extract options.
     * @return A String array containing all options.
     */
    private String[] extractOptions(String query) {
        if (query == null) {
            return null;
        }
        Matcher optionsMatcher = PROCESSING_OPTIONS.matcher(query);
        if (optionsMatcher.matches()) {
            return optionsMatcher.group(1).split(" +");
        }
        return null;
    }

    /**
     * A simple way to query the facet browser.
     *
     * @param query The query to perform.
     * @return An XML string containing the facet result or an error
     * description.
     */
    @WebMethod
    public String simpleFacet(String query) {
        log.trace("simpleFacet('" + query + "')");
        long startTime = System.currentTimeMillis();

        String retXML = advancedFacet(query, null);

        log.debug("simpleFacet('" + query + "') finished in " + (System.currentTimeMillis() - startTime) + "ms");
        return retXML;
    }


    public static final String EXPOSED_FORMAT_XML = "xml";

    /**
     * Performs a direct call to exposed facets. This exposes all facet-related
     * functionality of LUCENE-2369.
     * @param request a format specific request.
     * @param format the format of the request. Currently only 'xml' is allowed.
     * xml: See {@link FacetKeys#SEARCH_FACET_XMLREQUEST}.
     * @return a facet structure conforming to FacetResponse.xsd.
     */
    public String exposedFacet(String request, String format) {
        if (log.isTraceEnabled()) {
            log.trace("exposedFacet called with format '" + format + "' and request\n" + request);
        }
        long facetTime = -System.currentTimeMillis();
        if (!EXPOSED_FORMAT_XML.equals(format)) {
            String message ="exposedFacet called with unknown format '" + format + "'";
            log.warn(message);
            return getErrorXML("exposedFacet", message, null);
        }
        ResponseCollection res = null;

        String retXML;
        Request req = new Request();
        req.put(FacetKeys.SEARCH_FACET_XMLREQUEST, request);
        try {
            res = getSearcher().search(req);
            Document dom = DOM.stringToDOM(res.toXML());

            // remove any response not related to FacetResult
            NodeList nl =
                    DOM.selectNodeList(dom, "/responsecollection/response");
            for (int i = 0; i < nl.getLength(); i++) {
                Node n = nl.item(i);
                NamedNodeMap attr = n.getAttributes();
                Node attr_name = attr.getNamedItem("name");
                if (attr_name != null) {
                    if (!"ExposedFacetResult".equals(
                            attr_name.getNodeValue())) {
                        // this is not FacetResult so we remove it
                        n.getParentNode().removeChild(n);
                    }
                }
            }

            // transform dom back into a string
            retXML = DOM.domToString(dom);
        } catch (IOException e) {
            log.warn("Error faceting exposed request: '" + request + "'" + ". Error was: ", e);
            String mes = "Error performing request";
            retXML = getErrorXML(FacetResultExternal.NAME, mes, e);
        } catch (TransformerException e) {
            log.warn("Error faceting request: '" + request + "'" + ". Error was: ", e);
            String mes = "Error performing request";
            retXML = getErrorXML(FacetResultExternal.NAME, mes, e);
        }
        facetTime += System.currentTimeMillis();
        log.debug("exposedFacet(..., " + format + ") finished in " + facetTime + "ms" + getTiming(res));
        return retXML;

    }

    /**
     * A more advanced way to query the facet browser giving the caller control
     * over the individual facets and tags.
     * @param query The query to perform.
     * @param facetKeys A comma-separeted list with the names of the wanted
     * Facets.
     * Optionally, the maximum Tag-count for a given Facet can be specified in
     * parenthesis after the name.
     *
     * Example: "Title, Author (5), City (10), Year".
     *
     * If no maximum Tag-count is specified, the number is taken from the
     * defaults.
     * Optionally, the sort-type for a given Facet can be specified in the same
     * parenthesis. Valid values are POPULARITY and ALPHA. If no sort-type is
     * specified, the number is taken from the defaults.
     *
     * Example: "Title (ALPHA), Author (5 POPULARITY), City"
     *
     * This is all optional. If no facets are specified, the default facets are
     * requested.
     *
     * @return An XML string containing the facet result or an error
     * description.
     */
    @WebMethod
    public String advancedFacet(String query, String facetKeys) {
        log.trace("advancedFacet('" + query + "', '" + facetKeys + "')");
        long startTime = System.currentTimeMillis();

        query = cleanQuery(query);
        String retXML;

        ResponseCollection res = null;

        Request req = new Request();
        req.put(DocumentKeys.SEARCH_QUERY, query);
        req.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        if (facetKeys != null && !"".equals(facetKeys)) {
            req.put(FacetKeys.SEARCH_FACET_FACETS, facetKeys);
        }

        try {
            res = getSearcher().search(req);

            // parse string into dom
            Document dom = DOM.stringToDOM(res.toXML());

            // remove any response not related to FacetResult
            NodeList nl = DOM.selectNodeList(dom, "/responsecollection/response");
            for (int i = 0; i < nl.getLength(); i++) {
                Node n = nl.item(i);
                NamedNodeMap attr = n.getAttributes();
                Node attr_name = attr.getNamedItem("name");
                if (attr_name != null) {
                    if (!"FacetResult".equals(attr_name.getNodeValue())) {
                        // this is not FacetResult so we remove it
                        n.getParentNode().removeChild(n);
                    }
                }
            }

            // transform dom back into a string
            retXML = DOM.domToString(dom);
        } catch (IOException e) {
            log.warn("Error faceting query: '" + query + "'" + ". Error was: ", e);
            String mes = "Error performing query";
            retXML = getErrorXML(FacetResultExternal.NAME, mes, e);
        } catch (TransformerException e) {
            log.warn("Error faceting query: '" + query + "'" + ". Error was: ", e);
            String mes = "Error performing query";
            retXML = getErrorXML(FacetResultExternal.NAME, mes, e);
        }

        log.debug("advancedFacet('" + query + "', '" + facetKeys + "') finished in "
                  + (System.currentTimeMillis() - startTime) + "ms" + getTiming(res));
        return retXML;
    }

    /**
     * Local helper method for creating an XML output response. This response
     * can be used directly as it comes.
     *
     * @param responseName The response name.
     * @param message The response error message.
     * @param exception The exception catched while creating the original error
     * message.
     * @return An XML string which can be returned from the server in case of
     * errors.
     */
    private String getErrorXML(String responseName, String message, Exception exception) {
        StringWriter sw = new StringWriter(2000);
        XMLStreamWriter writer;
        try {
            writer = xmlOutputFactory.createXMLStreamWriter(sw);
        } catch (XMLStreamException e) {
            throw new RuntimeException("Unable to create XMLStreamWriter from factory", e);
        }
        // Write XML document.
        try {
            writer.setDefaultNamespace(NAMESPACE);
            writer.writeStartElement(SEARCH_ERROR_RESPONSE);
            writer.writeDefaultNamespace(NAMESPACE);
            writer.writeAttribute(NAME_TAG, responseName);
            writer.writeAttribute(VERSION_TAG, VERSION);
            // message
            writer.writeStartElement(MESSAGE_TAG);
            writer.writeCharacters(message);
            writer.writeEndElement();
            writer.writeCharacters("\n");
            // stack trace
            writer.writeStartElement(ERROR_TAG);
            writer.writeCharacters(exception == null ? "N/A" : exception.toString());
            writer.writeEndElement();
            writer.writeCharacters("\n");

            writer.writeEndElement();
            writer.writeCharacters("\n");
            writer.writeEndDocument();

            writer.flush(); // Just to make sure
        } catch (XMLStreamException e) {
            throw new RuntimeException("Got XMLStreamException while constructing XML from search response", e);
        }
        return sw.toString();
    }

    private String getTiming(ResponseCollection res) {
        return " with timing " + (res == null ? "N/A" : res.getTiming());
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Log4JSetup.ensureInitialized(sce);
        Environment.checkJavaVersion();
        getSearcher();  // We need to start it here to get RMI activated
        getSuggester();
        log.info("SearchWS context initialized");
    }
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("Shutting down " + searcher);
        try {
            searcher.close();
        } catch (IOException e) {
            log.warn("Exception shutting down searcher in contextDestroyed for " + searcher, e);
        }
    }
}
