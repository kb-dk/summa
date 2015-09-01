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

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;

import java.io.IOException;
import java.util.Arrays;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DocValuesIndexTest extends SolrSearchTestBase {

    public DocValuesIndexTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(DocValuesIndexTest.class);
    }

    public void testIndex() throws SolrServerException, IOException, InterruptedException {
        ingest("mv_index", Arrays.asList("foo", "bar"));
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set(CommonParams.Q, "mv_index:foo");
        QueryResponse response = solrServer.query(params);
        assertEquals("The number of hits should be correct for indexed", 1, response.getResults().getNumFound());
    }

    public void testDVIndex() throws SolrServerException, IOException, InterruptedException {
        ingest("dv_mv_index", Arrays.asList("foo", "bar"));
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set(CommonParams.Q, "dv_mv_index:foo");
        QueryResponse response = solrServer.query(params);
        assertEquals("The number of hits should be correct for DV+indexed", 1, response.getResults().getNumFound());
    }

    public void testDVSansIndex() throws SolrServerException, IOException, InterruptedException {
        ingest("dv_mv_noindex", Arrays.asList("foo", "bar"));
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set(CommonParams.Q, "dv_mv_noindex:foo");
        QueryResponse response = solrServer.query(params);
        assertEquals("The number of hits should be correct for DV not indexed", 0, response.getResults().getNumFound());
    }

}
