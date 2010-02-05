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
import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.summa.common.util.StringMap;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.storage.api.StorageReaderClient;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.ReadableStorage;
import dk.statsbiblioteket.summa.support.api.LuceneKeys;
import dk.statsbiblioteket.summa.support.api.SuggestKeys;
import dk.statsbiblioteket.summa.facetbrowser.browse.IndexRequest;
import dk.statsbiblioteket.summa.facetbrowser.api.IndexKeys;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XMLUtil;
import dk.statsbiblioteket.util.xml.XSLT;
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
import java.sql.Date;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * A class containing methods meant to be exposed as a web service
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class StatusWS {
    private Log log;
    private DateFormat dateFormat;
    private NumberFormat numberFormat;

    static StorageReaderClient storage;
    static SearchClient searcher;
    static SearchClient suggester;
    Configuration conf;

    public StatusWS() {
        log = LogFactory.getLog(StatusWS.class);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        numberFormat = NumberFormat.getIntegerInstance();
        numberFormat.setGroupingUsed(false);
        numberFormat.setMinimumIntegerDigits(12); // Padding to ensure correct sort
    }

    /**
     * Get a single StorageReaderClient based on the system configuration.
     * @return A SearchClient.
     */
    private synchronized StorageReaderClient getStorageClient() {
        if (storage == null) {
            try {
                storage = new StorageReaderClient(getConfiguration().getSubConfiguration("summa.web.storage"));
            } catch (IOException e) {
                log.error("Failed to load subConfiguration for storage.", e);
            }
        }
        return storage;
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
        if (suggester == null) {
            try {
                suggester = new SearchClient(getConfiguration().getSubConfiguration("summa.web.suggest"));
            } catch (IOException e) {
                log.error("Failed to load subConfiguration for suggest.", e);
            }
        }
        return suggester;
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
                log.warn("Failed to lookup env-entry. Trying to load system "
                         + "configuration.", e);
                conf = Configuration.getSystemConfiguration(true);
            }
        }

        return conf;
    }

    /**
     * Collect status for all known services
     * @return An XML string containing the status
     */
    public String fullStatus() {
        log.trace("fullStatus()");
        try {
        long startTime = System.currentTimeMillis();
        Status status = new Status();

        collectStorageStats(status);
        collectSearcherStats(status);
        collectSuggestStats(status);

        log.debug("fullStatus() finished in "
                  + (System.currentTimeMillis() - startTime) + "ms");
        return status.toXML();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "ERROR";
        }
    }

    private void collectStorageStats(Status status) {
        try {
            ReadableStorage storage = getStorageClient();
            StringMap meta = new StringMap();
            meta.put("ALLOW_PRIVATE", "true");
            QueryOptions opts = new QueryOptions(null, null, 0, 0, meta);

            long responseTime = System.currentTimeMillis();
            long timestamp = storage.getModificationTime(null);
            Record holdings = storage.getRecord("__holdings__", opts);
            responseTime = System.currentTimeMillis() - responseTime;
            String datetime = dateFormat.format(new Date(timestamp));
            status.put("storage", "lastUpdate", datetime);
            status.put("storage", "status", "OK");
            status.put("storage", "responseTime", Long.toString(responseTime));
            status.put("storage", "holdings", holdings.getContentAsUTF8());
        } catch (IOException e) {
            status.put("storage", "status", "ERROR: " + e.getMessage());
        }
    }

    private void collectSearcherStats(Status status) {
        try {
            Request req = new Request();
            req.put(DocumentKeys.SEARCH_QUERY, "*");

            long responseTime = System.currentTimeMillis();
            ResponseCollection resp = getSearchClient().search(req);
            responseTime = System.currentTimeMillis() - responseTime;
            status.put("searcher", "status", "OK");
            status.put("searcher", "responseTime", Long.toString(responseTime));

            /* Parse interesting data about search engine */
            Document dom = DOM.stringToDOM(resp.toXML());
            NamedNodeMap header =
                    DOM.selectNode(dom, "//documentresult").getAttributes();
            status.put("searcher", "searchTime",
                       header.getNamedItem("searchTime").getNodeValue());
            status.put("searcher", "numDocs",
                       header.getNamedItem("hitCount").getNodeValue());

            /* Parse interesting data about facets */
            NodeList facets = DOM.selectNodeList(dom, "//facet");
            for (int i = 0; i < facets.getLength(); i++) {
                Node facet = facets.item(i);
                String facetName =
                      facet.getAttributes().getNamedItem("name").getNodeValue();
                NodeList tags = DOM.selectNodeList(facet, "//tag");
                for (int j = 0; j < tags.getLength(); j++) {
                    NamedNodeMap tag = tags.item(j).getAttributes();
                    String tagName = tag.getNamedItem("name").getNodeValue();
                    String tagCount = tag.getNamedItem("addedobjects").getNodeValue();
                    status.put("facets", facetName + ":"+tagName, tagCount);
                }
            }
        } catch (IOException e) {
            status.put("searcher", "status", "ERROR: " + e.getMessage());
        }
    }

    private void collectSuggestStats(Status status) {
        try {
            /* Most popular suggestions in last 24h */
            Request req = new Request();
            req.put(SuggestKeys.SEARCH_RECENT, 60*60*24);
            req.put(SuggestKeys.SEARCH_MAX_RESULTS, 20);

            long responseTime = System.currentTimeMillis();
            ResponseCollection resp = getSuggestClient().search(req);
            responseTime = System.currentTimeMillis() - responseTime;
            status.put("suggest", "status", "OK");
            status.put("suggest", "responseTime", Long.toString(responseTime));

            Document dom = DOM.stringToDOM(resp.toXML());

            /* Parse interesting data about facets */
            NodeList suggestions = DOM.selectNodeList(dom, "//suggestion");
            for (int i = 0; i < suggestions.getLength(); i++) {
                Node suggestion = suggestions.item(i);
                String sugName = suggestion.getTextContent();
                String queryCount = suggestion.getAttributes()
                                     .getNamedItem("queryCount").getNodeValue();

                // Store suggestions indexed by count to get correct sorting
                status.put("queryCount",
                           numberFormat.format(Long.valueOf(queryCount)),
                           XMLUtil.encode(sugName));
                log.debug("");
            }
        } catch (IOException e) {
            status.put("suggest", "status", "ERROR: " + e.getMessage());
        }
    }

    private class Status {
        Map<String, List<Pair<String,String>>> groups =
                               new HashMap<String, List<Pair<String,String>>>();

        Date date;

        public Status() {
            date = new Date(System.currentTimeMillis());
        }

        public void put(String group, String name, String value) {
            if (!groups.containsKey(group)) {
                groups.put(group, new LinkedList<Pair<String,String>>());
            }
            groups.get(group).add(new Pair<String,String>(name, value));
        }

        public String toXML() {
            StringBuilder buf = new StringBuilder();
            buf.append(DOM.XML_HEADER)
               .append("\n")
               .append("<status datetime=\"")
               .append(dateFormat.format(date))
               .append("\">\n");
            SortedSet<String> groupNames = new TreeSet<String>(groups.keySet());
            for (String groupName : groupNames) {
                List<Pair<String,String>> group = groups.get(groupName);
                Collections.sort(group);
                buf.append("  <group name=\"")
                   .append(XMLUtil.encode(groupName))
                   .append("\">\n");
                for (Pair<String,String> prop : group) {
                    // Note: In order to allow services to return rich
                    //       status messages the body of the response is assumed
                    //       to be valid XML, thus is not escaped
                    buf.append("    <property name=\"")
                       .append(XMLUtil.encode(prop.getKey()))
                       .append("\">\n")
                       .append("      ")
                       .append(prop.getValue()) //Body is XML - don't escaped it
                       .append("\n    </property>\n");
                }
                buf.append("  </group>\n");
            }
            buf.append("</status>\n");

            return buf.toString();
        }

    }
}




