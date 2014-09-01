package dk.statsbiblioteket.summa.support.embeddedsolr;

import static org.junit.Assert.assertEquals;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.Before;
import org.junit.Test;

import dk.statsbiblioteket.summa.common.configuration.Resolver;

public class WildCardTest {

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
	public void testWildCard() throws Exception {

		//recordbase:
		//title: ABCDEFG 12345678
		
		String[] files = new String[]{
				"support/solr_test_documents/wildcard_doc.txt",		
		};
		//test title first						
		SolrServerUnitTestUtil.indexFiles(files);
		SolrQuery query = new SolrQuery("title:*");
		QueryResponse response = solrServer.query(query);
	  	assertEquals(1L, response.getResults().getNumFound());
	
    	SolrServerUnitTestUtil.indexFiles(files);
		query = new SolrQuery("title:abc*");
	    response = solrServer.query(query);
	  	assertEquals(1L, response.getResults().getNumFound());

	  	SolrServerUnitTestUtil.indexFiles(files);
		query = new SolrQuery("title:abc*efg");
	    response = solrServer.query(query);
	  	assertEquals(1L, response.getResults().getNumFound());
	  	
	  	SolrServerUnitTestUtil.indexFiles(files);
		query = new SolrQuery("title:ab*fg");
	    response = solrServer.query(query);
	  	assertEquals(1L, response.getResults().getNumFound());
	  	
    	SolrServerUnitTestUtil.indexFiles(files);
		query = new SolrQuery("title:abc*def");//Not found. both * and d
	    response = solrServer.query(query);
	  	assertEquals(0L, response.getResults().getNumFound());
	  	
	 
	  	SolrServerUnitTestUtil.indexFiles(files);
		query = new SolrQuery("title:*ef");//not found missing g
	    response = solrServer.query(query);
	  	assertEquals(0L, response.getResults().getNumFound());
	  	

	  	SolrServerUnitTestUtil.indexFiles(files);
		query = new SolrQuery("title:*efg");
	    response = solrServer.query(query);
	  	assertEquals(1L, response.getResults().getNumFound());
	  	
		SolrServerUnitTestUtil.indexFiles(files);
		query = new SolrQuery("title:abc?efg"); //? replaces d
	    response = solrServer.query(query);
	  	assertEquals(1L, response.getResults().getNumFound());
	  	
	  	SolrServerUnitTestUtil.indexFiles(files);
		query = new SolrQuery("title:abc??fg"); //? replaces d and e
	    response = solrServer.query(query);
	  	assertEquals(1L, response.getResults().getNumFound());
	  	
	  	
	 	SolrServerUnitTestUtil.indexFiles(files);
	    query = new SolrQuery("title:ab?fg"); //? can not replace both c and d
        response = solrServer.query(query);
	 	assertEquals(0L, response.getResults().getNumFound());
	  		  		  	
		
	}

}