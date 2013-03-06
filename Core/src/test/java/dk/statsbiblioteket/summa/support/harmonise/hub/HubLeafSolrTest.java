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
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.SolrParams;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class HubLeafSolrTest extends TestCase {
    private static Log log = LogFactory.getLog(HubLeafSolrTest.class);

    private HubComponent solr;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        solr = new HubLeafSolr(Configuration.newMemoryBased(
                HubComponentImpl.CONF_ID, "solr",
                HubLeafSolr.CONF_URL, "http://mars:57008/sb/sbsolr/"
        ));
    }

    public void testSearch() throws Exception {
        SolrParams query = new SolrQuery("foo");
        System.out.println(solr.search(null, query));
    }
}
