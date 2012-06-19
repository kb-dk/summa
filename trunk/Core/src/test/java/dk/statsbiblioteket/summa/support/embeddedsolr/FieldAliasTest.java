package dk.statsbiblioteket.summa.support.embeddedsolr;

import static org.junit.Assert.*;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.Before;
import org.junit.Test;

import dk.statsbiblioteket.summa.common.configuration.Resolver;

public class FieldAliasTest {

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
	public void testFieldAlias() throws Exception {

		//title field of the dokument: "Sjov med fraktaler"
		//'title'field also has alias: 'titel' in solr.config edismax-requesthandler
		String[] files = new String[]{
				"support/solr_test_documents/field_aliastest_doc.txt",		
		};
		SolrServerUnitTestUtil.indexFiles(files);
	//Thread.sleep(1000000000L);	
					
		SolrQuery query = new SolrQuery("title:\"Sjov med fraktaler\""); //original field
		QueryResponse response = solrServer.query(query);
		assertEquals(1L, response.getResults().getNumFound());

	
         query = new SolrQuery("location_normalised:\"Sjov med fraktaler\""); //existing field, but not with the content
         response = solrServer.query(query);
		 assertEquals(0L, response.getResults().getNumFound());
				
		query = new SolrQuery("titel:\"Sjov med fraktaler\""); //alias field		
		response = solrServer.query(query);
		assertEquals(1L, response.getResults().getNumFound());
		
		//alias groups. html_header -> (html_h1,html_2)
		//html_h1=>Recurssion
		//html_h2=>Gentagelse
		
		
		query = new SolrQuery("html_header:Recurssion"); //alias field		
		response = solrServer.query(query);
		assertEquals(1L, response.getResults().getNumFound());
		
		query = new SolrQuery("html_header:Gentagelse"); //alias field		
		response = solrServer.query(query);
		assertEquals(1L, response.getResults().getNumFound());
		
		
		query = new SolrQuery("html_h1:recurssion"); //field		
		response = solrServer.query(query);
		assertEquals(1L, response.getResults().getNumFound());
		
		//FAILS!
		//TODO! Fix default SOLR behaviour. Non-existing fields causes SOLR to go into "parse-error" mode and make a fixed query
		//Only happens if the non-existing field is given a phrase-query
		query = new SolrQuery("xtitel:\"Sjov med fraktaler\""); //non existing field, also no alias for it.
		response = solrServer.query(query);
		assertEquals(0L, response.getResults().getNumFound()); //zero results
		
		
	}

}