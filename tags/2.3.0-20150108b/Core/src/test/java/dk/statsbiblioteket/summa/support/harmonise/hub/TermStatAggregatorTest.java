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
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CommonParams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TermStatAggregatorTest extends SolrSearchDualTestBase {
    private static Log log = LogFactory.getLog(TermStatAggregatorTest.class);

    // TODO: Test force top X

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

    public void testMissingSearcher() throws Exception {
        log.info("testMissingSearcher()");
        ingest(0, "fulltext", Arrays.asList("bar", "moo"));
        HubComponent hub = getHubMissing(HubResponseMerger.SEARCH_MODE, HubResponseMerger.MODE.score.toString());
        try {
            QueryResponse response = hub.search(null, new SolrQuery("*:*"));
            fail("Got a response instead of an exception for a missing component:\n" + response);
        } catch (ExecutionException e) {
            log.info("Got an expected exception when querying a missing searcher", e);
        }
    }

    public void testDocumentTrim() throws Exception {
        log.info("testDocumentTrim()");
        ingest(0, "fulltext", Arrays.asList("bar", "moo"));
        ingest(1, "fulltext", Arrays.asList("zoo", "bam"));

        HubComponent hub = getHub(HubResponseMerger.SEARCH_MODE, HubResponseMerger.MODE.score.toString());
        SolrQuery request = new SolrQuery("*:*");
        request.set(CommonParams.ROWS, 3);
        QueryResponse response = hub.search(null, request);
        assertEquals("The number of hits should be unchanged", 4, response.getResults().getNumFound());
        assertEquals("The number of returned documents should be trimmed", 3, response.getResults().size());
    }

    public void testDocumentMergeScore() throws Exception {
        log.info("testDocumentMergeScoreIgnoreOrder()");
        ingestMulti(0,
                    Arrays.asList("fulltext:bar bar zoo", "mykey:bar bar zoo"),
                    Arrays.asList("fulltext:bar bar", "mykey:bar bar"));
        ingestMulti(1,
                    Arrays.asList("fulltext:bam", "mykey:bam"),
                    Arrays.asList("fulltext:bar bam", "mykey:bar bam moo"));

        HubComponent hub = getHub(HubResponseMerger.SEARCH_MODE, HubResponseMerger.MODE.score.toString());
        SolrQuery request = new SolrQuery("fulltext:bar");
        QueryResponse response = hub.search(null, request);
        //dumpDocuments(response);
        // "bar bam moo" comes first as that Solr instance only has a single 'bar'-term in the whole index
        assertDocFieldOrder("Sorting by score should work", response, "mykey", "bar bam moo", "bar bar", "bar bar zoo");
        assertFalse("The maxScore should be different from 1.0",
                    Math.abs(response.getResults().getMaxScore() - 1.0) < 0.001);
    }

    public void testDocumentMergeInterleave() throws Exception {
        log.info("testDocumentMergeScoreIgnoreOrder()");
        ingestMulti(0,
                    Arrays.asList("fulltext:bar bar zoo", "mykey:a0"),
                    Arrays.asList("fulltext:bar bar", "mykey:a1"));
        ingestMulti(1,
                    Arrays.asList("fulltext:bam", "mykey:b0"),
                    Arrays.asList("fulltext:bar bam", "mykey:b1"));

        {
            HubComponent hub = getHub(
                    HubResponseMerger.CONF_MODE, HubResponseMerger.MODE.interleave.toString(),
                    HubResponseMerger.CONF_ORDER, "solr0, solr1");
            SolrQuery request = new SolrQuery("*:*");
            QueryResponse response = hub.search(null, request);
            assertDocFieldOrder("Sorting by interleaving should work", response, "mykey",
                                "a0", "b0", "a1", "b1");
        }
    }

    public void testDocumentMergeConcatenate() throws Exception {
        log.info("testDocumentMergeScoreIgnoreOrder()");
        ingestMulti(0,
                    Arrays.asList("fulltext:bar bar zoo", "mykey:a0"),
                    Arrays.asList("fulltext:bar bar", "mykey:a1"));
        ingestMulti(1,
                    Arrays.asList("fulltext:bam", "mykey:b0"),
                    Arrays.asList("fulltext:bar bam", "mykey:b1"));

        {
            HubComponent hub = getHub(
                    HubResponseMerger.CONF_MODE, HubResponseMerger.MODE.concatenate.toString(),
                    HubResponseMerger.SEARCH_ORDER, "solr0, solr1");
            SolrQuery request = new SolrQuery("*:*");
            QueryResponse response = hub.search(null, request);
            assertDocFieldOrder("Sorting by concatenating should work", response, "mykey",
                                "a0", "a1", "b0", "b1");
        }

        {
            HubComponent hub = getHub(
                    HubResponseMerger.CONF_MODE, HubResponseMerger.MODE.concatenate.toString(),
                    HubResponseMerger.CONF_ORDER, "solr0, solr1");
            SolrQuery request = new SolrQuery("*:*");
            request.set(HubResponseMerger.SEARCH_ORDER, "solr1, solr0");
            QueryResponse response = hub.search(null, request);
            assertDocFieldOrder("Sorting by concatenating with search time order should work", response, "mykey",
                                "b0", "b1", "a0", "a1");
        }
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

    public void testFacetMergePopularity() throws Exception {
        log.info("testFacetMergePopularity()");
        ingestMulti(0,
               Arrays.asList("fulltext:bar", "fulltext:baz"),
               Arrays.asList("fulltext:zoo")
        );
        ingestMulti(1,
               Arrays.asList("fulltext:bar"),
               Arrays.asList("fulltext:bar", "fulltext:zoo")
        );

        HubComponent hub = getHub();
        SolrQuery request = new SolrQuery("*:*");
        request.setFacet(true);
        request.setFacetSort("count");
        request.set("facet.field", "fulltext");
        QueryResponse response = hub.search(null, request);
        //dumpDocuments(response);
        // "bar bam moo" comes first as that Solr instance only has a single 'bar'-term in the whole index
        assertFacetTermOrder("Default (popularity) faceting", response, "fulltext",
                             "bar 3", "zoo 2", "baz 1");
    }

    private void assertFacetTermOrder(String message, QueryResponse response, String field, String... expected) {
        FacetField ff = response.getFacetField(field);
        if (ff == null) {
            fail(message + ". No facet field '" + field + "' in response\n" + response);
        }
        assertEquals(message + ". The number of terms in the facet '" + field + "' should be correct",
                     expected.length, ff.getValues().size());
        for (int i = 0 ; i < expected.length ; i++) {
            String actual = ff.getValues().get(i).getName() + " " + ff.getValues().get(i).getCount();
            assertEquals(message + ". Facet term #" + i + " for field '" + field + "' should be correct",
                         expected[i], actual);
        }
    }


    private void assertDocFieldOrder(String message, QueryResponse response, String field, String... expected) {
        List<String> extracted = new ArrayList<>(response.getResults().size());
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
        List<String> hp = new ArrayList<>(Arrays.asList(hubParams));
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

    private HubComponent getHubMissing(String... hubParams) throws IOException {
        List<String> hp = new ArrayList<>(Arrays.asList(hubParams));
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
        subs.get(1).set(SolrLeaf.CONF_URL, server1.getServerUrl() + "missing");

        return HubFactory.createComponent(hubConf);
    }
}
