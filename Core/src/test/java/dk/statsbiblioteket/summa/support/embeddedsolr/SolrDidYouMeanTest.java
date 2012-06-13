package dk.statsbiblioteket.summa.support.embeddedsolr;

import static org.junit.Assert.*;

import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse.Suggestion;
import org.junit.Before;
import org.junit.Test;

import dk.statsbiblioteket.summa.common.configuration.Resolver;

public class SolrDidYouMeanTest {

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
	public void testDidYouMean() throws Exception {

		//Data:
		//3 books by Thomas Egense, 1 located at Matematik, 2 at Statsbiblioteket. 2 from 2011, 1 from 2012
		//2 books by Toke Eskildsen, 2 located at Statsbiblioteket, 1 from 2012, 1 from 2010
		//Facets defined for year,location_normalised,author_normalised
		String[] files = new String[]{
				"support/solr_test_documents/didyoumeantest.xml",    		
		};

		SolrServerUnitTestUtil.indexFiles(files);

		//Test of a single word. Missing prefix
		// gense -> egense 
		SolrQuery query = new SolrQuery("gense");
		query.setParam("spellcheck","true");
		query.setParam("spellcheck.dictionary","summa_spell");        
		query.setParam("spellcheck.count","5");

		QueryResponse response = solrServer.query(query);
		Map<String, Suggestion> suggestionMap = response.getSpellCheckResponse().getSuggestionMap();
		assertTrue(suggestionMap.size() == 1);                

		Suggestion suggestion = response.getSpellCheckResponse().getSuggestions().get(0);
		assertEquals("gense", suggestion.getToken());
		assertTrue(suggestion.getAlternatives().contains("egense"));

		//Single word, midde part wrong
		// hwllo -> hello 
		query = new SolrQuery("hwllo");
		query.setParam("spellcheck","true");
		query.setParam("spellcheck.dictionary","summa_spell");         
		query.setParam("spellcheck.count","5");

		response = solrServer.query(query);
		suggestionMap = response.getSpellCheckResponse().getSuggestionMap();
		assertTrue(suggestionMap.size() == 1);                

		suggestion = response.getSpellCheckResponse().getSuggestions().get(0);
		assertEquals("hwllo", suggestion.getToken());
		assertTrue(suggestion.getAlternatives().contains("hello"));


		//Test of a setentence
		// thomas exense -> thomas egense 
		query = new SolrQuery("thomas exense"); 
		query.setParam("spellcheck","true");
		query.setParam("spellcheck.dictionary","summa_spell");        
		query.setParam("spellcheck.extendedResults","true");
		query.setParam("spellcheck.collateExtendedResults","true");
		query.setParam("spellcheck.maxCollationTries","5");
		query.setParam("spellcheck.collate","true");
		query.setParam("spellcheck.count","5");

		//String httpResponse = SolrServerUnitTestUtil.callURL(server.getServerUrl()+"/select/?spellcheck=true&spellcheck.dictionary=summa_spell&spellcheck.extendedResults=true&spellcheck.maxCollationsTries=5&spellcheck.collate=true&spellcheck.collateExtendedResults=true&spellcheck.count=5&q=Thomas%20Exense");
		//System.out.println(httpResponse);

		response = solrServer.query(query);
		suggestionMap = response.getSpellCheckResponse().getSuggestionMap();
		//This method gives the modified query. Very usefull
		String collatedResult = response.getSpellCheckResponse().getCollatedResult();                
		assertEquals("thomas egense",collatedResult);




		//  Direct REST call. /edismax is defined to search in all summa-fields with boosts        
		//  String httpResponse = SolrServerUnitTestUtil.callURL(server.getServerUrl()+"/select/?spellcheck=true&spellcheck.dictionary=summa_spell&spellcheck.extendedResults=true&spellcheck.maxCollationsTries=5&spellcheck.collate=true&spellcheck.collateExtendedResults=true&spellcheck.count=5&q=gense");
		//  System.out.println(httpResponse);

	}



}