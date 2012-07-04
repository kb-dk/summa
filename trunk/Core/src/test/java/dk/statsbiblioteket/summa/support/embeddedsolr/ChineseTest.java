package dk.statsbiblioteket.summa.support.embeddedsolr;

import static org.junit.Assert.*;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.Before;
import org.junit.Test;

import dk.statsbiblioteket.summa.common.configuration.Resolver;

public class ChineseTest {

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
	public void testChineseCharacters() throws Exception {

		/*
	   title: Her er noget kinesisk 一般論述研究 som skal fremsøges
	    */
		String[] files = new String[]{
				"support/solr_test_documents/chinese_test_doc.txt",
		
		};
		SolrServerUnitTestUtil.indexFiles(files);
		Thread.sleep(1000000000000L);	
		SolrQuery query = new SolrQuery("一般論述研究");
 		
		QueryResponse response = solrServer.query(query);
	  	assertEquals(1L, response.getResults().getNumFound());
	  
	  	
	  	
	}

}