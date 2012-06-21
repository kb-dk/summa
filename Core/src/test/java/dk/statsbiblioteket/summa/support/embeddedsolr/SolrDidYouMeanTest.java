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

		/* DATA
		 <field name="recordId">didyoumeantest</field>
		    <field name="recordBase">aleph</field>
		    <field name="title">Hello world</field>
		    <field name="author_main">Thomas Egense</field>
		    <field name="author_normalised">Thomas Egense, Toke Eskildsen</field>
		    <field name="year">2012</field>
		    <field name="location_normalised">Statsbiblioteket</field>
		*/		
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


		//Test of a sentence
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

	}



}