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
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.index.IndexController;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.support.embeddedsolr.EmbeddedJettyWithSolrServer;
import dk.statsbiblioteket.summa.support.embeddedsolr.SolrServerUnitTestUtil;
import dk.statsbiblioteket.summa.support.solr.SolrManipulator;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SolrSearchDualTestBase extends TestCase {
    private static Log log = LogFactory.getLog(SolrSearchDualTestBase.class);

    public static final String SOLR_HOME0 = Resolver.getFile("harmonise/hub/solr_home_hub_0").getAbsolutePath();
    public static final String SOLR_HOME1 = Resolver.getFile("harmonise/hub/solr_home_hub_1").getAbsolutePath();

    protected EmbeddedJettyWithSolrServer server0 = null;
    protected EmbeddedJettyWithSolrServer server1 = null;
    protected SolrServer solrServer0 = null;
    protected SolrServer solrServer1 = null;
    
    @Override
    public void setUp() throws Exception {
        log.info("Starting and clearing Solr test servers");
        System.setProperty("basedir", ".");
        setupHomes();
        server0 = new EmbeddedJettyWithSolrServer(
                SOLR_HOME0, EmbeddedJettyWithSolrServer.DEFAULT_CONTEXT, EmbeddedJettyWithSolrServer.DEFAULT_PORT+1);
        server0.run();
        solrServer0 = new HttpSolrServer(server0.getServerUrl());
        solrServer0.deleteByQuery("*:*");
        server1 = new EmbeddedJettyWithSolrServer(
                SOLR_HOME1, EmbeddedJettyWithSolrServer.DEFAULT_CONTEXT, EmbeddedJettyWithSolrServer.DEFAULT_PORT+2);
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

    private void setupHomes() throws Exception {
        if (server0 != null) {
            server0.stopSolr();
        }
        if (server1 != null) {
            server1.stopSolr();
        }
        resetHome(SOLR_HOME0);
        resetHome(SOLR_HOME1);
    }

    private void resetHome(String solrHome) throws IOException {
        File home = new File(solrHome);
        if (!home.exists()) {
            fail("The solr_home folder " + solrHome + " does not exist");
        }
        File testHome = new File(solrHome + SolrServerUnitTestUtil.SOLR_HOME_SUFFIX);
        if (testHome.exists()) {
            Files.delete(testHome);
        }
        if (testHome.exists()) {
            fail("Unable to remove old test-home " + testHome);
        }
        Files.copy(home, testHome, false);
        if (!testHome.exists()) {
            fail("Unable to copy folder " + home + " to " + testHome);
        }
        log.debug("Prepared Solr home " + testHome);
        assertTrue("There should be at least 2 files in " + testHome, testHome.listFiles().length > 1);
    }

    protected void ingest(int backend, String field, List<String> terms) throws IOException {
        if (backend != 0 && backend != 1) {
            throw new IllegalArgumentException("The only valid backends are 0 and 1");
        }
        ObjectFilter data = getDataProvider(backend, field, terms);
        ObjectFilter indexer = getIndexer(EmbeddedJettyWithSolrServer.DEFAULT_PORT + 1 + backend);
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
        ObjectFilter indexer = getIndexer(EmbeddedJettyWithSolrServer.DEFAULT_PORT + 1 + backend);
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
            samples.add(new Payload(new Record("doc" + i, "dummy", c.getBytes(StandardCharsets.UTF_8))));
        }
        return new PayloadFeederHelper(samples);
    }

    private Random random = new Random(88);
    protected ObjectFilter getDataProvider(int backend, String field, List<String> terms) throws UnsupportedEncodingException {
        List<Payload> samples = new ArrayList<>(terms.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0 ; i < terms.size() ; i++) {
            sb.setLength(0);
            double boost = 0.5 + random.nextDouble()*10;

            char c = (char) ('a' + random.nextInt(10));
            for (int token = random.nextInt(terms.size()*2) + 1 ; token >= 0 ; token--) {
                if (sb.length() != 0) {
                    sb.append(" ");
                }
                sb.append("r").append(c);
            }
            samples.add(new Payload(new Record(
                    "doc" + i, "dummy",
                    ("<doc boost=\"" + boost + "\"><field name=\"recordID\">doc" + i + "</field>\n"
                     + "   <field name=\"recordBase\">backend" + backend + "</field>\n"
                     + "   <field name=\"fulltext\">" + sb.toString() + " c" + i + "</field>\n"
                     + "   <field name=\"" + field + "\">" + terms.get(i) + "</field></doc>").getBytes(StandardCharsets.UTF_8))));
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
