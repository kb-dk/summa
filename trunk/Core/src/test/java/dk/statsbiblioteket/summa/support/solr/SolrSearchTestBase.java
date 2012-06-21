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
package dk.statsbiblioteket.summa.support.solr;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.index.IndexController;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.support.embeddedsolr.EmbeddedJettyWithSolrServer;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Super class for unit testing of Solr search. Handles setup logic and sample data indexing.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SolrSearchTestBase extends TestCase {
    public static final String SOLR_HOME = "support/solr_home1"; //data-dir (index) will be created here.

    protected EmbeddedJettyWithSolrServer server = null;
    protected SolrServer solrServer;

    public SolrSearchTestBase(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("basedir", ".");
        // TODO: Clear existing data
        server = new EmbeddedJettyWithSolrServer(SOLR_HOME);
        server.run();
        solrServer = new HttpSolrServer(server.getServerUrl());
        solrServer.deleteByQuery("*:*");
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        server.stopSolr();
    }

    protected void ingest(List<String> terms) throws IOException {
        ingest("lti", terms);
    }
    protected void ingest(String field, List<String> terms) throws IOException {
        ObjectFilter data = getDataProvider(field, terms);
        ObjectFilter indexer = getIndexer();
        indexer.setSource(data);
        //noinspection StatementWithEmptyBody
        while (indexer.pump());
        indexer.close(true);
    }

    protected ObjectFilter getDataProvider(String field, List<String> terms) throws UnsupportedEncodingException {
        List<Payload> samples = new ArrayList<Payload>(terms.size());
        for (int i = 0 ; i < terms.size() ; i++) {
            samples.add(new Payload(new Record(
                "doc" + i, "dummy",
                ("<doc><field name=\"recordId\">doc" + i + "</field>\n"
                 + "   <field name=\"recordBase\">myBase</field>\n"
                 + "   <field name=\"" + field + "\">" + terms.get(i) + "</field></doc>").getBytes("utf-8"))));
        }
        return new PayloadFeederHelper(samples);
    }

    protected IndexController getIndexer() throws IOException {
        Configuration controllerConf = Configuration.newMemoryBased(
            IndexController.CONF_FILTER_NAME, "testcontroller");
        Configuration manipulatorConf = controllerConf.createSubConfigurations(
            IndexControllerImpl.CONF_MANIPULATORS, 1).get(0);
        manipulatorConf.set(IndexControllerImpl.CONF_MANIPULATOR_CLASS, SolrManipulator.class.getCanonicalName());
        manipulatorConf.set(SolrManipulator.CONF_ID_FIELD, "recordId"); // 'id' is the default ID field for Solr
        return new IndexControllerImpl(controllerConf);
    }
}
