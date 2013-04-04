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
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

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

    public void testDocumentMergeScoreIgnoreOrder() throws Exception {
        log.info("testDocumentMergeScoreIgnoreOrder()");
        ingest(0, "fulltext", Arrays.asList("bar", "moo"));
        ingest(1, "fulltext", Arrays.asList("zoo", "bam"));

        HubComponent hub = getHub(HubResponseMerger.SEARCH_MODE, HubResponseMerger.MODE.score.toString());
        QueryResponse response = hub.search(null, new SolrQuery("*:*"));
        assertEquals("The number of merged hits should match", 4, response.getResults().getNumFound());
        for (int i = 0 ; i < 4 ; i++) {
            assertNotNull("There should be a document for hit " + i, response.getResults().get(i));
        }
    }
    
    public void testDocumentMergeScore() throws Exception {
        log.info("testDocumentMergeScoreIgnoreOrder()");
        ingest(0,
               Arrays.asList("fulltext:bar bar zoo", "mykey:bar bar zoo"),
               Arrays.asList("fulltext:bar bar", "mykey:bar bar"));
        ingest(1,
               Arrays.asList("fulltext:bam", "mykey:bam"),
               Arrays.asList("fulltext:bar bam", "mykey:bar bam moo"));

        HubComponent hub = getHub(HubResponseMerger.SEARCH_MODE, HubResponseMerger.MODE.score.toString());
        SolrQuery request = new SolrQuery("fulltext:bar");
        QueryResponse response = hub.search(null, request);
        //dumpDocuments(response);
        // "bar bam moo" comes first as that Solr instance only has a single 'bar'-term in the whole index
        assertDocFieldOrder("Sorting by score should work", response, "mykey", "bar bam moo", "bar bar", "bar bar zoo");
    }

    public void testDocumentMergeDefaultIgnoreOrder() throws Exception {
        log.info("testDocumentMergeDefaultIgnoreOrder()");
        ingest(0, "mykey", Arrays.asList("bar", "moo"));
        ingest(1, "mykey", Arrays.asList("zoo", "bam"));

        HubComponent hub = getHub(HubResponseMerger.SEARCH_MODE, HubResponseMerger.MODE.standard.toString());
        SolrQuery request = new SolrQuery("*:*");
        {
            request.setSortField("mykey", SolrQuery.ORDER.asc);
            QueryResponse response = hub.search(null, request);
            assertDocFieldOrder("Sorting by 'mykey asc' should work", response, "mykey", "bam", "bar", "moo", "zoo");
        }
        {
            request.setSortField("mykey", SolrQuery.ORDER.desc);
            QueryResponse response = hub.search(null, request);
            assertDocFieldOrder("Sorting by 'mykey desc' should work", response, "mykey", "zoo", "moo", "bar", "bam");
        }
    }

    private void assertDocFieldOrder(String message, QueryResponse response, String field, String... expected) {
        List<String> extracted = new ArrayList<String>(response.getResults().size());
        for (SolrDocument doc: response.getResults()) {
            String value = (String) doc.getFieldValue(field);
            if (value != null) {
                extracted.add(value);
            }
        }
        assertEquals(message, Strings.join(expected), Strings.join(extracted));
    }

    private void dumpDocuments(QueryResponse response) {
        int docCount = 0;
        for (SolrDocument doc: response.getResults()) {
            System.out.println("Document " + docCount++);
            for (String field: doc.getFieldNames()) {
                for (Object value: doc.getFieldValues(field)) {
                    System.out.println("  " + field + ":" + value);
                }
            }
        }
    }

    private HubComponent getHub(String... hubParams) throws IOException {
        List<String> hp = new ArrayList<String>(Arrays.asList(hubParams));
        hp.add(HubFactory.CONF_COMPONENT);
        hp.add(TermStatAggregator.class.getCanonicalName());
        String[] hpa = new String[hp.size()];
        hp.toArray(hpa);

        Configuration hubConf = Configuration.newMemoryBased(hpa);
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
