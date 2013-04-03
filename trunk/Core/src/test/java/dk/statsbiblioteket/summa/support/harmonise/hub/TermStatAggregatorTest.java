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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubComponent;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubComponentImpl;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubFactory;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TermStatAggregatorTest extends SolrSearchDualTestBase {
    private static Log log = LogFactory.getLog(TermStatAggregatorTest.class);

    public void testBasicIngest() throws IOException, SolrServerException {
        log.info("testBasicIngest()");
        ingest(0, "fulltext", Arrays.asList("bar"));
        ingest(1, "fulltext", Arrays.asList("zoo"));

        {
            QueryResponse r0 = solrServer0.query(new SolrQuery("fulltext:bar"));
            assertEquals("There should be hits from Solr 0",
                         1, r0.getResults().getNumFound());
            assertEquals("The hit for solr 0 should be correct",
                         "bar", ((ArrayList)r0.getResults().get(0).getFieldValue("fulltext")).get(0));
        }

        {
            QueryResponse r1 = solrServer1.query(new SolrQuery("fulltext:zoo"));
            assertEquals("There should be hits from Solr 1",
                         1, r1.getResults().getNumFound());
            assertEquals("The hit for solr 0 should be correct",
                         "zoo", ((ArrayList) r1.getResults().get(0).getFieldValue("fulltext")).get(0));
        }

        {
            QueryResponse r1f = solrServer1.query(new SolrQuery("fulltext:bar"));
            assertEquals("There should be no hits from Solr 1 when searching for 'bar'",
                         0, r1f.getResults().getNumFound());
        }
    }

    public void testDocumentMerge() throws Exception {
        log.info("testDocumentMerge()");
        ingest(0, "fulltext", Arrays.asList("bar", "moo"));
        ingest(1, "fulltext", Arrays.asList("zoo", "bam"));

        HubComponent hub = getHub();
        QueryResponse response = hub.search(null, new SolrQuery("*:*"));
        assertEquals("The number of merged hits should match", 4, response.getResults().getNumFound());
        for (int i = 0 ; i < 4 ; i++) {
            assertNotNull("There should be a document for hit " + i, response.getResults().get(i));
        }
    }
    
    private HubComponent getHub() throws IOException {
        Configuration hubConf = Configuration.newMemoryBased(
                HubFactory.CONF_COMPONENT, TermStatAggregator.class,
                HubResponseMerger.CONF_MODE, HubResponseMerger.MODE.score
        );
        List<Configuration> subs =  hubConf.createSubConfigurations(HubFactory.CONF_SUB, 2);

        subs.get(0).set(HubFactory.CONF_COMPONENT, SolrLeaf.class);
        subs.get(0).set(HubComponentImpl.CONF_ID, "solr0");
        subs.get(0).set(SolrLeaf.CONF_URL, server0.getServerUrl());

        subs.get(1).set(HubFactory.CONF_COMPONENT, SolrLeaf.class);
        subs.get(1).set(HubComponentImpl.CONF_ID, "solr1");
        subs.get(1).set(SolrLeaf.CONF_URL, server1.getServerUrl());

        return HubFactory.createComponent(hubConf);
    }
}
