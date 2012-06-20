package dk.statsbiblioteket.summa.support.embeddedsolr;

import static org.junit.Assert.*;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.Before;
import org.junit.Test;

import dk.statsbiblioteket.summa.common.configuration.Resolver;

public class NumberTransliterationTest {

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
	public void testNumberTransliteration() throws Exception {

		//the document contains isbn "X978-3-16-148410-0" which is mapped to "x9783161484100"" 
		//also numbers-field are mapped to isbn (and other fields)
		String[] files = new String[]{
				"support/solr_test_documents/number_transliteration_doc.txt",
		
		};
		SolrServerUnitTestUtil.indexFiles(files);
		SolrQuery query = new SolrQuery("numbers:x9783161484100");
		QueryResponse response = solrServer.query(query);
		assertEquals(1L, response.getResults().getNumFound());

		query = new SolrQuery("numbers:X978-3-16-148410-0");
		response = solrServer.query(query);
		assertEquals(1L, response.getResults().getNumFound());				
	
	}

}