package dk.statsbiblioteket.summa.support.embeddedsolr;

import static org.junit.Assert.*;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.Before;
import org.junit.Test;

import dk.statsbiblioteket.summa.common.configuration.Resolver;

public class KeyFieldTest {

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
	public void testKeywordField() throws Exception {

		//field recordId and recordBase is fieldtype:key	
		
		//recordId:sb_123456789
		String[] files = new String[]{
				"support/solr_test_documents/keyfieldtest_doc.txt",
		
		};
		SolrServerUnitTestUtil.indexFiles(files);
		SolrQuery query = new SolrQuery("recordId:sb_123456789");
		QueryResponse response = solrServer.query(query);
	  	assertEquals(1L, response.getResults().getNumFound());


	  	query = new SolrQuery("recordId:sb 123456789");
		response = solrServer.query(query);
		assertEquals(0L, response.getResults().getNumFound()); 
	  	
		
		query = new SolrQuery("recordId:sb");
		response = solrServer.query(query);
		assertEquals(0L, response.getResults().getNumFound()); 
	  	
		
		query = new SolrQuery("recordId:123456789");
		response = solrServer.query(query);
		assertEquals(0L, response.getResults().getNumFound());
		
	
		
	}

}