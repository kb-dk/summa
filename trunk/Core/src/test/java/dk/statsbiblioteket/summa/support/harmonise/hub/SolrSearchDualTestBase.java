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
package dk.statsbiblioteket.summa.support.harmonise.hub;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.index.IndexController;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.support.embeddedsolr.EmbeddedJettyWithSolrServer;
import dk.statsbiblioteket.summa.support.solr.SolrManipulator;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SolrSearchDualTestBase extends TestCase {
    private static Log log = LogFactory.getLog(SolrSearchDualTestBase.class);

    public static final String SOLR_HOME0 = "harmonise/hub/solr_home_hub_0";
    public static final String SOLR_HOME1 = "harmonise/hub/solr_home_hub_1";

    protected EmbeddedJettyWithSolrServer server0;
    protected EmbeddedJettyWithSolrServer server1;
    protected SolrServer solrServer0;
    protected SolrServer solrServer1;
    
    @Override
    public void setUp() throws Exception {
        log.info("Starting and clearing Solr test servers");
        System.setProperty("basedir", ".");
        server0 = new EmbeddedJettyWithSolrServer(
                SOLR_HOME0, EmbeddedJettyWithSolrServer.DEFAULT_CONTEXT, EmbeddedJettyWithSolrServer.DEFAULT_PORT);
        server0.run();
        solrServer0 = new HttpSolrServer(server0.getServerUrl());
        solrServer0.deleteByQuery("*:*");
        server1 = new EmbeddedJettyWithSolrServer(
                SOLR_HOME1, EmbeddedJettyWithSolrServer.DEFAULT_CONTEXT, EmbeddedJettyWithSolrServer.DEFAULT_PORT+1);
        server1.run();
        solrServer1 = new HttpSolrServer(server1.getServerUrl());
        solrServer1.deleteByQuery("*:*");
        log.debug("Dual Solr backends ready at '" + server0.getServerUrl() + "' and '" + server1.getServerUrl() + "'");
    }

    @Override
    public void tearDown() throws Exception {
        server0.stopSolr();
        server1.stopSolr();
        log.info("Stopped dual Solr backends");
    }

    protected void ingest(int backend, String field, List<String> terms) throws IOException {
        if (backend != 0 && backend != 1) {
            throw new IllegalArgumentException("The only valid backends are 0 and 1");
        }
        ObjectFilter data = getDataProvider(field, terms);
        ObjectFilter indexer = getIndexer(EmbeddedJettyWithSolrServer.DEFAULT_PORT + backend);
        indexer.setSource(data);
        //noinspection StatementWithEmptyBody
        while (indexer.pump());
        indexer.close(true);
    }

    // content: "myfield1:term", "myfield2:some other terms"...
    protected void ingestMulti(int backend, List<String>... content) throws IOException {
        if (backend != 0 && backend != 1) {
            throw new IllegalArgumentException("The only valid backends are 0 and 1");
        }
        ObjectFilter data = getDataProviderMulti(content);
        ObjectFilter indexer = getIndexer(EmbeddedJettyWithSolrServer.DEFAULT_PORT + backend);
        indexer.setSource(data);
        //noinspection StatementWithEmptyBody
        while (indexer.pump());
        indexer.close(true);
    }

    // content: "myfield1:term", "myfield2:some other terms"...
    private ObjectFilter getDataProviderMulti(List<String>[] content) throws UnsupportedEncodingException {
        List<Payload> samples = new ArrayList<>(content.length);
        for (int i = 0 ; i < content.length ; i++) {
            String c =
                    "<doc><field name=\"recordID\">doc" + i + "</field>\n"
                    + "   <field name=\"recordBase\">myBase</field>\n";
            for (String pair: content[i]) {
                String[] pairT = pair.split(":",2);
                if (pairT.length != 2) {
                    throw new IllegalArgumentException(
                            "The pair '" + pair + "' did not match the pattern 'field:terms'");
                }
                c += "    <field name=\"" + pairT[0] + "\">" + pairT[1] + "</field>\n";
            }
            c += "</doc>\n";
            samples.add(new Payload(new Record("doc" + i, "dummy", c.getBytes("utf-8"))));
        }
        return new PayloadFeederHelper(samples);
    }

    protected ObjectFilter getDataProvider(String field, List<String> terms) throws UnsupportedEncodingException {
        List<Payload> samples = new ArrayList<>(terms.size());
        for (int i = 0 ; i < terms.size() ; i++) {
            samples.add(new Payload(new Record(
                "doc" + i, "dummy",
                ("<doc><field name=\"recordID\">doc" + i + "</field>\n"
                 + "   <field name=\"recordBase\">myBase</field>\n"
                 + "   <field name=\"" + field + "\">" + terms.get(i) + "</field></doc>").getBytes("utf-8"))));
        }
        return new PayloadFeederHelper(samples);
    }

    protected IndexController getIndexer(int port) throws IOException {
        Configuration controllerConf = Configuration.newMemoryBased(
            IndexController.CONF_FILTER_NAME, "testcontroller");
        Configuration manipulatorConf = controllerConf.createSubConfigurations(
            IndexControllerImpl.CONF_MANIPULATORS, 1).get(0);
        manipulatorConf.set(IndexControllerImpl.CONF_MANIPULATOR_CLASS, SolrManipulator.class.getCanonicalName());
        manipulatorConf.set(SolrManipulator.CONF_ID_FIELD, IndexUtils.RECORD_FIELD);
        manipulatorConf.set(SolrManipulator.CONF_SOLR_HOST, "localhost:" + port);
        return new IndexControllerImpl(controllerConf);
    }
}
