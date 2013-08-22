package dk.statsbiblioteket.summa.support.embeddedsolr;

import static org.junit.Assert.assertEquals;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.Before;
import org.junit.Test;

import dk.statsbiblioteket.summa.common.configuration.Resolver;

public class VoidTransliterationTest {

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
	public void testVoidTransliteration() throws Exception {

		//the document contains Tho?m:as which is mapped to Tho)m:as
		String[] files = new String[]{
				"support/solr_test_documents/void_transliteration_doc.txt",
		
		};
		SolrServerUnitTestUtil.indexFiles(files);
		SolrQuery query = new SolrQuery("Thomas");
		QueryResponse response = solrServer.query(query);
		assertEquals(1L, response.getResults().getNumFound());

		query = new SolrQuery("Tho)m:as");
		response = solrServer.query(query);
		assertEquals(1L, response.getResults().getNumFound());				
	}

}