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

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

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

}
