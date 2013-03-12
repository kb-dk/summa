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
package dk.statsbiblioteket.summa.support.harmonise.hub.core;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.support.harmonise.hub.DummyLeaf;
import dk.statsbiblioteket.summa.support.harmonise.hub.TermStatAggregator;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrQuery;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class HubFactoryTest extends TestCase {
    private static Log log = LogFactory.getLog(HubFactoryTest.class);

    public void testCreateDirectComponent() throws Exception {
        HubComponent dummy = HubFactory.createComponent(Configuration.newMemoryBased(
                HubFactory.CONF_COMPONENT, DummyLeaf.class,
                HubComponentImpl.CONF_ID, "myDummy"
        ));
        dummy.search(null, new SolrQuery("foo"));
        dummy.search(null, new SolrQuery("bar"));
        assertEquals("The search on the direct dummy should pass",
                     2, DummyLeaf.getReceivedParams().get("myDummy").size());
    }

    public void testCreateComponentUnderHub() throws Exception {
        Configuration hubConf = Configuration.newMemoryBased(
                HubFactory.CONF_COMPONENT, TermStatAggregator.class
        );
        Configuration dummyConf = hubConf.createSubConfigurations(HubFactory.CONF_SUB, 1).get(0);
        dummyConf.set(HubFactory.CONF_COMPONENT, DummyLeaf.class);
        dummyConf.set(HubComponentImpl.CONF_ID, "dummyUnder");
        HubComponent dummy = HubFactory.createComponent(hubConf);

        dummy.search(null, new SolrQuery("foo"));
        assertEquals("The first search on the direct dummy should pass",
                     1, DummyLeaf.getReceivedParams().get("dummyUnder").size());
        dummy.search(null, new SolrQuery("bar"));
        assertEquals("The second search on the direct dummy should pass",
                     2, DummyLeaf.getReceivedParams().get("dummyUnder").size());
    }
}
