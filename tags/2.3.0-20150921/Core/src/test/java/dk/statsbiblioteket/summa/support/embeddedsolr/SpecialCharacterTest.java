package dk.statsbiblioteket.summa.support.embeddedsolr;

import static org.junit.Assert.assertEquals;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.Before;
import org.junit.Test;

import dk.statsbiblioteket.summa.common.configuration.Resolver;

public class SpecialCharacterTest {

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

	@Test
	public void testSpecialCharacters() throws Exception {

		/*
	      doc 1: title:"ABCD-EFGH" (notice analyzer will make it abcd efgh)
	      doc 2: title:"ABCD"
	    */
		String[] files = new String[]{
				"support/solr_test_documents/special_character_test_doc1.txt",
				"support/solr_test_documents/special_character_test_doc2.txt",
		};
		SolrServerUnitTestUtil.indexFiles(files);
		
		
		//TODO: FIX. Seems summa handles the '-' character if there is no whitespace before
		//We only want to find doc1
		SolrQuery query = new SolrQuery("abcd-efgh");
 		
		QueryResponse response = solrServer.query(query);
	  	assertEquals(1L, response.getResults().getNumFound());
	  
	  	
	  	
	}

}