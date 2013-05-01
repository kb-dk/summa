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
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;

import java.util.Iterator;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class HubComponentImplTest extends TestCase {

    public void testDefaultValues() throws Exception {
        Configuration conf = Configuration.newMemoryBased(HubComponentImpl.CONF_ID, "dummy");
        Configuration defaults = conf.createSubConfiguration(HubComponentImpl.CONF_DEFAULTS);
        defaults.set("foo", "bar");
        DummyLeaf dummy = new DummyLeaf(conf);

        {
            ModifiableSolrParams params = new ModifiableSolrParams();
            params.add("zoo", "baz");
            dummy.search(null, params);
            SolrParams processed = DummyLeaf.getReceivedParams().get("dummy").get(0);
            assertEquals("Default 'foo' should be correct", "bar", processed.get("foo"));
            assertEquals("Provided 'zoo' should be correct", "baz", processed.get("zoo"));
            DummyLeaf.getReceivedParams().clear();
        }

        {
            ModifiableSolrParams params = new ModifiableSolrParams();
            params.add("foo", "bom");
            dummy.search(null, params);
            SolrParams processed = DummyLeaf.getReceivedParams().get("dummy").get(0);
            assertEquals("Overwritten 'foo' should be correct", "bom", processed.get("foo"));
            DummyLeaf.getReceivedParams().clear();
        }

        {
            ModifiableSolrParams params = new ModifiableSolrParams();
            dummy.search(null, params);
            SolrParams processed = DummyLeaf.getReceivedParams().get("dummy").get(0);
            assertEquals("Old default 'foo' should be correct", "bar", processed.get("foo"));
            DummyLeaf.getReceivedParams().clear();
        }

    }
}
