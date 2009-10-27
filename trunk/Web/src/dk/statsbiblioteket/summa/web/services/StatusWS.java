/*
 * The Summa project.
 * Copyright (C) 2005-2009  The State and University Library
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
import dk.statsbiblioteket.summa.storage.api.StorageReaderClient;
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
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
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
            long responseTime = System.currentTimeMillis();
            long timestamp = getStorageClient().getModificationTime(null);
            responseTime = System.currentTimeMillis() - responseTime;
            String datetime = dateFormat.format(new Date(timestamp));
            status.put("storage", "lastUpdate", datetime);
            status.put("storage", "status", "OK");
            status.put("storage", "responseTime", Long.toString(responseTime));
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
            }
        } catch (IOException e) {
            status.put("suggest", "status", "ERROR: " + e.getMessage());
        }
    }

    private static class Status {

        Map<String, Map<String,String>> groups =
                                     new HashMap<String, Map<String,String>>();

        public void put(String group, String name, String value) {
            if (!groups.containsKey(group)) {
                groups.put(group, new HashMap<String,String>());
            }
            groups.get(group).put(name, value);
        }

        public String toXML() {
            StringBuilder buf = new StringBuilder();
            buf.append(DOM.XML_HEADER);
            buf.append("\n");
            buf.append("<status>\n");
            SortedSet<String> groupNames = new TreeSet<String>(groups.keySet());
            for (String groupName : groupNames) {
                Map<String,String> group = groups.get(groupName);
                buf.append("  <group name=\"")
                   .append(groupName)
                   .append("\">\n");
                SortedSet<String> groupProperties =
                                    new TreeSet<String>(group.keySet());
                for (String name : groupProperties) {
                    buf.append("    <property name=\"")
                       .append(name)
                       .append("\">\n")
                       .append("      ")
                       .append(group.get(name))
                       .append("\n    </property>\n");
                }
                buf.append("  </group>\n");
            }
            buf.append("</status>\n");

            return buf.toString();
        }

    }
}



