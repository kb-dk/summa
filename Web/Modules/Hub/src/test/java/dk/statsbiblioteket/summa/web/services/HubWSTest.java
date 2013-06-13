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

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.support.harmonise.hub.SolrLeaf;
import dk.statsbiblioteket.summa.support.harmonise.hub.SolrSearchDualTestBase;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubComponent;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubFactory;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.QueryResponse;

import java.io.IOException;
import java.net.BindException;
import java.util.Arrays;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class HubWSTest extends SolrSearchDualTestBase {
    private static Log log = LogFactory.getLog(HubWSTest.class);

    public static final String ADDRESS= "http://localhost:9999/";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ingestSimpleCorpus();
    }

    // Verifies that the Hub can be created successfully from the configuration
    public void testHub() throws Exception {
        HubComponent hub = HubFactory.createComponent(Configuration.load("hub_configuration.xml"));
//        System.out.println("Search for '*:*': " + hub.search(null, new SolrQuery("*:*")));
        QueryResponse response = hub.search(null, new SolrQuery("*:*"));
        assertEquals("The hub response should contain the correct number of documents",
                     4, response.getResults().size());
    }

    public void testHubMissingSearcher() throws Exception {
        HubComponent hub = HubFactory.createComponent(Configuration.load("hub_configuration_faulty.xml"));
//        System.out.println("Search for '*:*': " + hub.search(null, new SolrQuery("*:*")));
        QueryResponse response = hub.search(null, new SolrQuery("*:*"));
        assertEquals("The hub response should contain the correct number of documents",
                     4, response.getResults().size());
    }

    // Creates the same hub as testHub, but exposed as a web service
    public void testWebSearch() throws Exception {
 //       InitialContext context = new InitialContext();
        //context.bind("java:comp/env/confLocation", "goat");
        HttpServer server;
        try {
            server = HttpServerFactory.create(ADDRESS);
        } catch (BindException e) {
            throw (BindException)new BindException("Unable to bind to '" + ADDRESS + "'").initCause(e);
        }
        server.start();
        log.info("Server running at " + ADDRESS + "hub. Performing test search");
        try {
            SolrQuery query = new SolrQuery("*:*");
            query.set("qt", ""); // Just plain /hub

            QueryResponse response = restSearch(query);
            assertEquals("The web service response should contain the correct number of documents",
                         4, response.getResults().size());
            //System.out.println("Search for '*:*': " + HubComponentImpl.toXML(query, restSearch(query)));
        } finally {
            server.stop(0);
        }
        /*
        System.out.println("Server running");
        System.out.println("Visit: " + ADDRESS);
        System.out.println("Hit return to stop...");
        System.in.read();
        System.out.println("Stopping server");
        server.stop(0);
        System.out.println("Server stopped");
          */
    }

    public void testWebSearchKeepRunning() throws Exception {
        HttpServer server;
        try {
            server = HttpServerFactory.create(ADDRESS);
        } catch (BindException e) {
            throw (BindException)new BindException("Unable to bind to '" + ADDRESS + "'").initCause(e);
        }
        server.start();
        System.out.println("Hit return to stop server at " + ADDRESS);
        System.in.read();
        server.stop(0);
    }

    private QueryResponse restSearch(SolrQuery query) throws SolrServerException {
        SolrLeaf solr = new SolrLeaf(Configuration.newMemoryBased(
                SolrLeaf.CONF_ID, "tmp",
                SolrLeaf.CONF_URL, ADDRESS + "hub",
                SolrLeaf.CONF_PROTOCOL, SolrLeaf.PROTOCOL.xml.toString()
        ));
        solr.getSolrServer().setParser(new XMLResponseParser());
        return solr.search(query);
    }

    private void ingestSimpleCorpus() throws IOException {
        ingest(0, "fulltext", Arrays.asList("bar", "zoo"));
        ingest(1, "fulltext", Arrays.asList("foo", "baz"));
    }
}
