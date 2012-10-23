package dk.statsbiblioteket.summa.support.embeddedsolr;

import static org.junit.Assert.*;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.junit.Before;
import org.junit.Test;

import dk.statsbiblioteket.summa.common.configuration.Resolver;

public class ScoreRankingTest {

	String solrHome = "support/solr_home1"; //data-dir (index) will be created here.  
	EmbeddedJettyWithSolrServer server;
	SolrServer solrServer;


	@Before
	public void setUp() throws Exception {
		System.setProperty("basedir", "."); //for logback
		server =  new EmbeddedJettyWithSolrServer(Resolver.getFile(solrHome).toString());
		server.run();
		solrServer = new HttpSolrServer(server.getServerUrl());		
	}

	@Test
	public void testScoreRanking() throws Exception {
		
		String[] files = new String[]{
				"support/solr_test_documents/score_ranking_doc.txt"
		
		};
		SolrServerUnitTestUtil.indexFiles(files);

	    SolrQuery query = new SolrQuery("\"Thomas og Toke\"");
		QueryResponse response = solrServer.query(query);
		assertEquals(1L, response.getResults().getNumFound());
		SolrDocument solrDocument = response.getResults().get(0);
		Float score= (Float) solrDocument.getFieldValue("score");
		assertTrue("Insane boost bug not fixed, score="+score, score < 10000000);
        System.out.println("Got score: " + score);
		
		
		
	//Thread.sleep(1000000000000000L);	
		
	}

}