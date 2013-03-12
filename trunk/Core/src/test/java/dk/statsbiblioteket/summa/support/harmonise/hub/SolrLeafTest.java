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
import dk.statsbiblioteket.summa.support.solr.SolrSearchTestBase;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;

import java.util.Arrays;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SolrLeafTest extends SolrSearchTestBase {
    private static Log log = LogFactory.getLog(SolrLeafTest.class);

    private HubComponent solr;

    public SolrLeafTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        solr = new SolrLeaf(Configuration.newMemoryBased(
                HubComponentImpl.CONF_ID, "solr",
                SolrLeaf.CONF_URL, server.getServerUrl()
        ));
    }

    public void testSearch() throws Exception {
        ingest("lti", Arrays.asList("foo", "fii"));
        SolrParams query = new SolrQuery("lti:f*");
        QueryResponse response = solr.search(null, query);
        assertEquals("The number of hit should be correct in\n" + response,
                     2, response.getResults().getNumFound());
    }


}
