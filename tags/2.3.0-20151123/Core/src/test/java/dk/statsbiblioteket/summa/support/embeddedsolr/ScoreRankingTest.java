package dk.statsbiblioteket.summa.support.embeddedsolr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.junit.Before;
import org.junit.Test;

import dk.statsbiblioteket.summa.common.configuration.Resolver;

public class ScoreRankingTest {

    String solrHome = "support/solr_home_default"; //data-dir (index) will be created here.
    EmbeddedJettyWithSolrServer server;
    SolrServer solrServer;


    @Before
    public void setUp() throws Exception {
        System.setProperty("basedir", "."); //for logback
        server =  new EmbeddedJettyWithSolrServer(Resolver.getFile(solrHome).toString());
        server.run();
        solrServer = new HttpSolrServer(server.getServerUrl());
    }

    /**
     * Solr 4.0.0 contains a bug where multiple unique fields that are copied to the same field (via the copyField
     * directive), results in the destination field having the boost assigned multiple times.
     * See https://issues.apache.org/jira/browse/SOLR-3875 for details.
     * @throws Exception if the test failed.
     */
    @Test
    public void testScoreRanking() throws Exception {

        String[] files = new String[]{
            "support/solr_test_documents/score_ranking_doc.txt"
        };
        SolrServerUnitTestUtil.indexFiles(files);
        SolrServerUnitTestUtil.indexDocuments(generateSamples(10, 87));

        SolrQuery query = new SolrQuery("\"Thomas og Toke\"");
        QueryResponse response = solrServer.query(query);
        assertEquals(1L, response.getResults().getNumFound());
        SolrDocument solrDocument = response.getResults().get(0);
        Float score= (Float) solrDocument.getFieldValue("score");
        assertTrue("Insane boost bug not fixed, score=" + score, score < 1000);

       //Thread.sleep(1000000000000000L);
    }

    public String[] generateSamples(int numSamples, int seed) {
        Random random = new Random(seed);
        String[] docs = new String[numSamples];
        for (int i = 0 ; i < numSamples ; i++) {
            docs[i] =
                "<doc>\n"
                + "    <field name=\"recordID\">doc" + i + "</field>\n"
                + "    <field name=\"recordBase\">aleph</field>\n"
                + "    <field name=\"title\">Matematik " + randomString(random) + "</field>\n"
                + "    <field name=\"author_main\">" + randomString(random) + " " + randomString(random) + "</field>\n"
                + "    <field name=\"author_normalised\">Thomas Egense</field>\n"
                + "    <field name=\"year\">" + (1990 + i) + "</field>\n"
                + "    <field name=\"location_normalised\">" + randomString(random) + "</field>\n"
                + "</doc>";
        }
        return docs;
    }

    private String randomString(Random random) {
        int size = random.nextInt(20);
        String str = "";
        for (int i = 0 ; i < size ; i++) {
            str += (char)(97 + random.nextInt(27));
        }
        return str;
    }

}