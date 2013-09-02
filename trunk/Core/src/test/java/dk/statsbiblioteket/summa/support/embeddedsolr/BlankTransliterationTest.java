package dk.statsbiblioteket.summa.support.embeddedsolr;

import static org.junit.Assert.assertEquals;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.Before;
import org.junit.Test;

import dk.statsbiblioteket.summa.common.configuration.Resolver;

public class BlankTransliterationTest {

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
	public void testBlankTransliteration() throws Exception {

		//the document contains "Thomas-Egense ¼" which is mapped to "Thomas Egense 1 4" 
		String[] files = new String[]{
				"support/solr_test_documents/blank_transliteration_doc.txt",
		
		};
		SolrServerUnitTestUtil.indexFiles(files);
		SolrQuery query = new SolrQuery("Thomas Egense");
		QueryResponse response = solrServer.query(query);
		assertEquals(1L, response.getResults().getNumFound());

		query = new SolrQuery("1 4");
		response = solrServer.query(query);
		assertEquals(1L, response.getResults().getNumFound());				
	
		query = new SolrQuery("2 3"); //No results
		response = solrServer.query(query);
		assertEquals(0L, response.getResults().getNumFound());
	
	
		//testing abc'def
		query = new SolrQuery("abc'def"); 
        response = solrServer.query(query);
        assertEquals(1L, response.getResults().getNumFound());
	        
        query = new SolrQuery("abc def"); 
        response = solrServer.query(query);
        assertEquals(1L, response.getResults().getNumFound());
    
        query = new SolrQuery("abc"); 
        response = solrServer.query(query);
        assertEquals(1L, response.getResults().getNumFound());
        
        query = new SolrQuery("def"); 
        response = solrServer.query(query);
        assertEquals(1L, response.getResults().getNumFound());
	   
	
	}

}