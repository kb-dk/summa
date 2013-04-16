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
import dk.statsbiblioteket.summa.support.harmonise.hub.SolrSearchDualTestBase;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubComponent;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubFactory;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrQuery;

import javax.naming.InitialContext;
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
        ingestSimpleCorpus();
    }

    public void testHub() throws Exception {
        HubComponent hub = HubFactory.createComponent(Configuration.load("hub_configuration.xml"));
        System.out.println("Search for '*:*': " + hub.search(null, new SolrQuery("*:*")));
    }

    public void testWebSearch() throws Exception {
        InitialContext context = new InitialContext();
        //context.bind("java:comp/env/confLocation", "goat");
        HttpServer server;
        try {
            server = HttpServerFactory.create(ADDRESS);
        } catch (BindException e) {
            throw (BindException)new BindException("Unable to bind to '" + ADDRESS + "'").initCause(e);
        }
        server.start();

        System.out.println("Server running");
        System.out.println("Visit: " + ADDRESS);
        System.out.println("Hit return to stop...");
        System.in.read();
        System.out.println("Stopping server");
        server.stop(0);
        System.out.println("Server stopped");

    }

    private void ingestSimpleCorpus() throws IOException {
        ingest(0, "fulltext", Arrays.asList("bar", "zoo"));
        ingest(1, "fulltext", Arrays.asList("foo", "baz"));
    }
}
