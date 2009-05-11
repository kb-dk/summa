/* $Id: SearchWS.java,v 1.2 2007/10/04 13:28:21 mv Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:21 $
 * $Author: mv $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
package dk.statsbiblioteket.summa.web.services;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.support.api.LuceneKeys;
import dk.statsbiblioteket.summa.support.api.SuggestKeys;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.DOM;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * A class containing methods meant to be exposed as a web service
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mv")
public class SearchWS {
    private Log log;

    SearchClient searcher;
    Configuration conf;

    public SearchWS() {
        log = LogFactory.getLog(SearchWS.class);
    }

    /**
     * Get a single SearchClient based on the system configuration.
     * @return A SearchClient.
     */
    private synchronized SearchClient getSearchClient() {
        if (searcher == null) {
            try {
                searcher = new SearchClient(getConfiguration().getSubConfiguration("summa.web.search"));
            } catch (IOException e) {
                log.error("Failed to load subConfiguration for search.", e);
            }
        }
        return searcher;
    }

    /**
     * Get a single SearchClient for Suggest based on the system configuration.
     * @return A SearchClient to be used for Suggest.
     */
    private synchronized SearchClient getSuggestClient() {
        if (searcher == null) {
            try {
                searcher = new SearchClient(getConfiguration().getSubConfiguration("summa.web.suggest"));
            } catch (IOException e) {
                log.error("Failed to load subConfiguration for suggest.", e);
            }
        }
        return searcher;
    }

    /**
     * Get the a Configuration object. First trying to load the configuration from the location
     * specified in the JNDI property java:comp/env/confLocation, and if that fails, then the System
     * Configuration will be returned.
     * @return The Configuration object
     */
    private Configuration getConfiguration() {
        if (conf == null) {
            InitialContext context;
            try {
                context = new InitialContext();
                String paramValue = (String) context.lookup("java:comp/env/confLocation");
                log.debug("Trying to load configuration from: " + paramValue);
                conf = Configuration.load(paramValue);
            } catch (NamingException e) {
                log.error("Failed to lookup env-entry.", e);
                log.warn("Trying to load system configuration.");
                conf = Configuration.getSystemConfiguration(true);
            }
        }

        return conf;
    }

    /**
     * Given a prefix this method returns other queries that start with the same prefix.
     * @param prefix The prefix that the returned queries must start with.
     * @param maxSuggestions The maximum number of queries to be returned.
     * @return An XML string containing the result or an error description.
     */
    public String getSuggestions(String prefix, int maxSuggestions) {
        String retXML;

        ResponseCollection res;

        Request req = new Request();
        req.put(SuggestKeys.SEARCH_PREFIX, prefix);
        req.put(SuggestKeys.SEARCH_MAX_RESULTS, maxSuggestions);

        try {
            res = getSuggestClient().search(req);
            Document dom = DOM.stringToDOM(res.toXML());
            Node subDom = DOM.selectNode(dom,
                    "/responsecollection/response[@name='SuggestResponse']/QueryResponse/suggestions");
            retXML = DOM.domToString(subDom);
        } catch (IOException e) {
            log.warn("Error executing getSuggestions: '" + prefix + "', " +
                    maxSuggestions +
                    ". Error was: ", e);
            // TODO: return a nicer error xml block
            retXML = "<error>Error performing getSuggestions</error>";
        } catch (TransformerException e) {
            log.warn("Error executing getSuggestions: '" + prefix + "', " +
                    maxSuggestions +
                    ". Error was: ", e);
            // TODO: return a nicer error xml block
            retXML = "<error>Error performing getSuggestions</error>";
        }

        return retXML;
    }

    /**
     * Commits a query to the Suggestion database. This enables this query to be returned in the result from
     * getSuggestions. It is recommended that only query that the user actually enters are committed - ie. it might not
     * be a good idea to commit queries that come from the user clicking facets, etc.
     * @param query the query to commit to the database.
     * @param hitCount the number of hits that resulted from the query. If this is 0 then the query is removed from the
     * Suggestion database.
     */
    public void commitQuery(String query, long hitCount) {
        ResponseCollection res;

        Request req = new Request();
        req.put(SuggestKeys.SEARCH_UPDATE_QUERY, cleanQuery(query));
        req.put(SuggestKeys.SEARCH_UPDATE_HITCOUNT, hitCount);

        try {
            getSuggestClient().search(req);
        } catch (IOException e) {
            log.warn("Error committing query '" + cleanQuery(query)
                     + "' with hitCount '" + hitCount + "'");
        }
    }


    /**
     * Returns a given field from the search index for a specific recordId. This could for instance be used to get the
     * shortformat for a specific record. 
     * @param id The recordId to look up.
     * @param fieldName The name of the field to return.
     * @return An XML string containing the result or an error description.
     */
    public String getField(String id, String fieldName) {
        String retXML;

        ResponseCollection res;

        Request req = new Request();
        req.put(DocumentKeys.SEARCH_QUERY, "recordID:\"" + id + "\"");
        req.put(DocumentKeys.SEARCH_MAX_RECORDS, 1);
        req.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);

        try {
            res = getSearchClient().search(req);
            Document dom = DOM.stringToDOM(res.toXML());
            Node subDom = DOM.selectNode(dom,
                    "/responsecollection/response/documentresult/record/field[@name='" + fieldName + "']");
            retXML = DOM.domToString(subDom);
        } catch (IOException e) {
            log.warn("Error querying for id: '" + id + "'." +
                    "Error was: ", e);
            // TODO: return a nicer error xml block
            retXML = "<error>Error performing query</error>";
        } catch (TransformerException e) {
            log.warn("Error querying for id: '" + id + "'." +
                    "Error was: ", e);
            // TODO: return a nicer error xml block
            retXML = "<error>Error performing query</error>";
        }

        return retXML;
    }

    /**
     * Gives a search result of records that "are similar to" a given record. 
     * @param id The recordID of the record that should be used as base for the MoreLikeThis query.
     * @param numberOfRecords The maximum number of records to return.
     * @return An XML string containing the result or an error description.
     */
    public String getMoreLikeThis(String id, int numberOfRecords) {
        String retXML;

        ResponseCollection res;

        Request req = new Request();
        req.put(LuceneKeys.SEARCH_MORELIKETHIS_RECORDID, id);
        req.put(DocumentKeys.SEARCH_MAX_RECORDS, numberOfRecords);
        req.put(DocumentKeys.SEARCH_SORTKEY, DocumentKeys.SORT_ON_SCORE);
        req.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);

        try {
            res = getSearchClient().search(req);
            retXML = res.toXML();
        } catch (IOException e) {
            log.warn("Error executing morelikethis: '" + id + "', " +
                    numberOfRecords +
                    ". Error was: ", e);
            // TODO: return a nicer error xml block
            retXML = "<error>Error performing morelikethis</error>";
        }

        return retXML;
    }

    /**
     * A simple way to query the index returning results sorted by relevance. The same as calling
     * simpleSearchSorted while specifying a normal sort on relevancy.
     * @param query The query to perform.
     * @param numberOfRecords The maximum number of records to return.
     * @param startIndex Where to start returning records from (used to implement paging).
     * @return An XML string containing the result or an error description.
     */
    public String simpleSearch(String query, int numberOfRecords, int startIndex) {
        return simpleSearchSorted(query, numberOfRecords, startIndex, DocumentKeys.SORT_ON_SCORE, false);
    }

    public static final Pattern PROCESSING_OPTIONS =
            Pattern.compile("\\<\\:(.*)\\:\\>(.*)");
    /**
     * A simple way to query the index wile being able to specify which field to sort by and whether the sorting
     * should be reversed.
     * </p><p>
     * Processing-options can be specified at the start of the query prepended
     * by '<:' and appended by ':>', divided by spaces. As of now, the only
     * possible option is 'explain', which turns on explanation of the result.
     * This increases processing time, so do not turn it on as default.
     * example: "<:explain:>foo" will explain why the documents matching foo
     * was selected.
     * @param query The query to perform.
     * @param numberOfRecords The maximum number of records to return.
     * @param startIndex Where to start returning records from (used to implement paging).
     * @param sortKey The field to sort by.
     * @param reverse Whether or not the sort should be reversed.
     * @return An XML string containing the result or an error description.
     */
    public String simpleSearchSorted(String query, int numberOfRecords, int startIndex, String sortKey, boolean reverse) {
        log.debug(String.format(
                "simpleSearchSorted(query='%s', numberOfRecords=%d, "
                + "startIndex=%d, sortKey='%s', reverse=%b) entered",
                query, numberOfRecords, startIndex, sortKey, reverse));

        String retXML;
        ResponseCollection res;
        Request req = new Request();
        // Handle processing options
        String[] options = extractOptions(query);
        try {
            if (options != null) {
                log.debug("simpleSearchSorted received "
                          + options.length + " options");
                for (String option: options) {
                    if ("explain".equals(option)) {
                        log.debug("Turning on explain for query '"
                                  + query + "'");
                        req.put(DocumentKeys.SEARCH_EXPLAIN, true);
                        continue;
                    }
                    log.debug(String.format(
                            "Got unknown processing option '%s' in query '%s'",
                            option, query));
                }
            } else {
                log.trace("No processing options for query '%s'");
            }
        } catch (Exception e) {
            log.warn(String.format(
                    "Exception while extracting processing options from query "
                    + "'%s'. Options are skipped and the query left unchanged",
                    query));
        }
        query = cleanQuery(query);

        req.put(DocumentKeys.SEARCH_QUERY, query);
        req.put(DocumentKeys.SEARCH_MAX_RECORDS, numberOfRecords);
        req.put(DocumentKeys.SEARCH_START_INDEX, startIndex);
        req.put(DocumentKeys.SEARCH_SORTKEY, sortKey);
        req.put(DocumentKeys.SEARCH_REVERSE, reverse);
        req.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, false);        

        try {
            res = getSearchClient().search(req);
            retXML = res.toXML();
        } catch (IOException e) {
            log.warn("Error executing query: '" + query + "', " +
                    numberOfRecords + ", " +
                    startIndex + ", " +
                    sortKey + ", " +
                    reverse +
                    ". Error was: ", e);
            // TODO: return a nicer error xml block
            retXML = "<error>Error performing query</error>";
        }

        return retXML;
    }

    private String cleanQuery(String queryString) {
        Matcher optionsMatcher = PROCESSING_OPTIONS.matcher(queryString);
        if (optionsMatcher.matches()) {
            return optionsMatcher.group(2);
        }
        return queryString;
    }

    private String[] extractOptions(String query) {
        Matcher optionsMatcher = PROCESSING_OPTIONS.matcher(query);
        if (optionsMatcher.matches()) {
            return optionsMatcher.group(1).split(" +");
        }
        return null;
    }

    /**
     * A simple way to query the facet browser.
     * @param query The query to perform.
     * @return An XML string containing the facet result or an error description.
     */
    public String simpleFacet(String query) {
        query = cleanQuery(query);
        String retXML;

        ResponseCollection res;

        Request req = new Request();
        req.put(DocumentKeys.SEARCH_QUERY, query);
        req.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);

        try {
            res = getSearchClient().search(req);

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
            log.warn("Error faceting query: '" + query + "'" +
                    ". Error was: ", e);
            // TODO: return a nicer error xml block
            retXML = "<error>Error performing query</error>";
        } catch (TransformerException e) {
            log.warn("Error faceting query: '" + query + "'" +
                    ". Error was: ", e);
            // TODO: return a nicer error xml block
            retXML = "<error>Error performing query</error>";
        }

        return retXML;
    }
}



